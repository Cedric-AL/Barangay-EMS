-- Run after the existing integration and staff-registration migrations.
BEGIN;
CREATE OR REPLACE FUNCTION public.ems_update_evacuee(p_id BIGINT,p_name TEXT,p_age INT,p_household TEXT,p_contact TEXT,p_needs TEXT,p_category TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    PERFORM ems_private.require_user();
    IF nullif(btrim(p_name),'') IS NULL OR length(btrim(p_name))>150 OR p_age IS NULL OR p_age<0 OR p_age>120
       OR nullif(btrim(p_household),'') IS NULL OR length(btrim(p_household))>100 THEN
        RAISE EXCEPTION 'Enter a valid name, age (0-120), and household.';
    END IF;
    IF nullif(btrim(p_contact),'') IS NOT NULL AND btrim(p_contact) !~ '^[+0-9 ()-]{7,20}$' THEN
        RAISE EXCEPTION 'Enter a valid contact number, or leave it blank.';
    END IF;
    IF p_category IS NULL OR p_category NOT IN ('None','Child','Senior Citizen','Person with Disability','Pregnant','Other') THEN
        RAISE EXCEPTION 'Choose a valid vulnerability category.';
    END IF;
    -- Same normalized-name lock as registration, to serialize duplicate checks.
    PERFORM pg_advisory_xact_lock(hashtextextended(lower(btrim(p_name)),0));
    PERFORM 1 FROM public.evacuees WHERE evacuee_id=p_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Evacuee no longer exists. Refresh the screen.'; END IF;
    IF EXISTS(SELECT 1 FROM public.evacuees e WHERE e.evacuee_id<>p_id
        AND lower(btrim(e.full_name))=lower(btrim(p_name)) AND e.age=p_age
        AND (lower(btrim(e.household))=lower(btrim(p_household))
             OR (nullif(btrim(p_contact),'') IS NOT NULL AND btrim(e.contact)=btrim(p_contact)))) THEN
        RAISE EXCEPTION 'Another evacuee has matching identifying details. Review the existing record before saving.';
    END IF;
    UPDATE public.evacuees SET full_name=btrim(p_name),age=p_age,household=btrim(p_household),
        contact=nullif(btrim(p_contact),''),special_needs=p_needs,vulnerability_category=p_category
    WHERE evacuee_id=p_id;
    -- Center assignment and occupancy are intentionally unchanged by detail edits.
END $$;

CREATE OR REPLACE FUNCTION public.ems_delete_evacuee(p_id BIGINT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_center BIGINT;
BEGIN
    PERFORM ems_private.require_user(TRUE);
    -- Match transfer's person-before-center locking order.
    SELECT center_id INTO v_center FROM public.evacuees WHERE evacuee_id=p_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Evacuee no longer exists. Refresh the screen.'; END IF;
    IF EXISTS(SELECT 1 FROM public.evacuee_transfers WHERE evacuee_id=p_id) THEN
        RAISE EXCEPTION 'This evacuee has transfer history and cannot be permanently deleted.';
    END IF;
    IF v_center IS NOT NULL THEN
        PERFORM 1 FROM public.evacuation_centers WHERE center_id=v_center FOR UPDATE;
        UPDATE public.evacuation_centers SET current_occupants=current_occupants-1
        WHERE center_id=v_center AND current_occupants>0;
        IF NOT FOUND THEN RAISE EXCEPTION 'Center occupancy is inconsistent. Contact the administrator.'; END IF;
    END IF;
    DELETE FROM public.evacuees WHERE evacuee_id=p_id;
END $$;
REVOKE ALL ON FUNCTION public.ems_update_evacuee(BIGINT,TEXT,INT,TEXT,TEXT,TEXT,TEXT),public.ems_delete_evacuee(BIGINT) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.ems_update_evacuee(BIGINT,TEXT,INT,TEXT,TEXT,TEXT,TEXT),public.ems_delete_evacuee(BIGINT) TO authenticated;
NOTIFY pgrst,'reload schema';
COMMIT;
