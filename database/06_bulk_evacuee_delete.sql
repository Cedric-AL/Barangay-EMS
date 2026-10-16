-- Apply AFTER 05_evacuee_edit_delete.sql. Adds an official-only atomic bulk delete.
BEGIN;
CREATE OR REPLACE FUNCTION public.ems_delete_evacuees(p_ids BIGINT[])
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_id BIGINT; v_count BIGINT;
BEGIN
    PERFORM ems_private.require_user(TRUE);
    IF p_ids IS NULL OR cardinality(p_ids)=0 OR array_position(p_ids,NULL) IS NOT NULL THEN
        RAISE EXCEPTION 'Select at least one valid evacuee.';
    END IF;
    -- Lock persons first and all affected centers in ascending order before deleting.
    PERFORM 1 FROM public.evacuees WHERE evacuee_id=ANY(p_ids) ORDER BY evacuee_id FOR UPDATE;
    SELECT count(*) INTO v_count FROM public.evacuees WHERE evacuee_id=ANY(p_ids);
    IF v_count<>(SELECT count(DISTINCT id) FROM unnest(p_ids) AS ids(id)) THEN
        RAISE EXCEPTION 'A selected evacuee no longer exists. Refresh before deleting.';
    END IF;
    IF EXISTS(SELECT 1 FROM public.evacuee_transfers WHERE evacuee_id=ANY(p_ids)) THEN
        RAISE EXCEPTION 'A selected evacuee has transfer history. No records were deleted.';
    END IF;
    PERFORM 1 FROM public.evacuation_centers
    WHERE center_id IN (SELECT center_id FROM public.evacuees WHERE evacuee_id=ANY(p_ids))
    ORDER BY center_id FOR UPDATE;
    FOR v_id IN SELECT DISTINCT id FROM unnest(p_ids) AS ids(id) ORDER BY id LOOP
        PERFORM public.ems_delete_evacuee(v_id);
    END LOOP;
END $$;
REVOKE ALL ON FUNCTION public.ems_delete_evacuees(BIGINT[]) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.ems_delete_evacuees(BIGINT[]) TO authenticated;
NOTIFY pgrst,'reload schema';
COMMIT;
