-- Barangay-EMS Supabase integration migration.
-- Run as the project/database administrator AFTER the six base tables exist.
BEGIN;

-- Link application profiles to Supabase Auth without ever storing passwords in public.users.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS auth_user_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS users_auth_user_id_uidx
    ON public.users(auth_user_id) WHERE auth_user_id IS NOT NULL;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'users_auth_user_fk' AND conrelid = 'public.users'::regclass
    ) THEN
        ALTER TABLE public.users
            ADD CONSTRAINT users_auth_user_fk
            FOREIGN KEY (auth_user_id) REFERENCES auth.users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Do not silently accept counters that disagree with the actual resident records.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.evacuation_centers c
        LEFT JOIN (
            SELECT center_id, count(*)::int AS n
            FROM public.evacuees
            WHERE center_id IS NOT NULL
            GROUP BY center_id
        ) e ON e.center_id = c.center_id
        WHERE c.current_occupants <> COALESCE(e.n, 0)
    ) THEN
        RAISE EXCEPTION 'Center occupant counters do not match registered evacuees. Reconcile the records before applying 02_integration.sql.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.resources
        GROUP BY center_id, lower(btrim(resource_type)), lower(btrim(COALESCE(unit,'')))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate resource rows exist for the same center/type/unit. Merge them before applying 02_integration.sql.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS resources_center_type_unit_uidx
    ON public.resources(center_id, lower(btrim(resource_type)), lower(btrim(COALESCE(unit,''))));

-- Center status is database-owned so every client sees the same result.
CREATE OR REPLACE FUNCTION public.ems_set_center_status()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    IF NEW.current_occupants >= NEW.max_capacity THEN
        NEW.status := 'Full';
    ELSIF NEW.current_occupants >= CEIL(NEW.max_capacity * 0.90) THEN
        NEW.status := 'Near Capacity';
    ELSE
        NEW.status := 'Available';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER IF EXISTS trg_ems_center_status ON public.evacuation_centers;
CREATE TRIGGER trg_ems_center_status
BEFORE INSERT OR UPDATE OF max_capacity, current_occupants
ON public.evacuation_centers
FOR EACH ROW EXECUTE FUNCTION public.ems_set_center_status();

-- Normalize any existing status values after the trigger is installed.
UPDATE public.evacuation_centers SET current_occupants = current_occupants;

-- Auth/role helpers. They deliberately raise 42501 so the API reports permission failures.
CREATE OR REPLACE FUNCTION public.ems_current_user_id()
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
STABLE
SET search_path = public, auth, pg_temp
AS $$
DECLARE v_user BIGINT;
BEGIN
    SELECT u.user_id INTO v_user
    FROM public.users u
    WHERE u.auth_user_id = auth.uid();
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'This Supabase account is not linked to a Barangay-EMS profile.' USING ERRCODE = '42501';
    END IF;
    RETURN v_user;
END $$;

CREATE OR REPLACE FUNCTION public.ems_require_official()
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
STABLE
SET search_path = public, auth, pg_temp
AS $$
DECLARE v_user BIGINT; v_role TEXT;
BEGIN
    SELECT u.user_id, u.role INTO v_user, v_role
    FROM public.users u
    WHERE u.auth_user_id = auth.uid();
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'This Supabase account is not linked to a Barangay-EMS profile.' USING ERRCODE = '42501';
    END IF;
    IF v_role <> 'Barangay Official' THEN
        RAISE EXCEPTION 'Only Barangay Officials can manage evacuation centers.' USING ERRCODE = '42501';
    END IF;
    RETURN v_user;
END $$;

CREATE OR REPLACE FUNCTION public.ems_save_center(
    p_id BIGINT,
    p_name TEXT,
    p_location TEXT,
    p_capacity INT,
    p_facilities TEXT
) RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE v_id BIGINT; v_occupants INT;
BEGIN
    PERFORM public.ems_require_official();
    p_name := btrim(COALESCE(p_name,''));
    p_location := btrim(COALESCE(p_location,''));
    IF p_name = '' THEN RAISE EXCEPTION 'Center name is required.'; END IF;
    IF p_location = '' THEN RAISE EXCEPTION 'Location is required.'; END IF;
    IF p_capacity IS NULL OR p_capacity < 1 THEN RAISE EXCEPTION 'Capacity must be at least 1.'; END IF;

    PERFORM pg_advisory_xact_lock(hashtext('ems-center:' || lower(p_name)));
    IF EXISTS (
        SELECT 1 FROM public.evacuation_centers
        WHERE lower(btrim(name)) = lower(p_name)
          AND (p_id IS NULL OR center_id <> p_id)
    ) THEN
        RAISE EXCEPTION 'A center with this name already exists.';
    END IF;

    IF p_id IS NULL THEN
        INSERT INTO public.evacuation_centers(name,location,max_capacity,current_occupants,facilities)
        VALUES(p_name,p_location,p_capacity,0,NULLIF(btrim(COALESCE(p_facilities,'')),''))
        RETURNING center_id INTO v_id;
    ELSE
        SELECT current_occupants INTO v_occupants
        FROM public.evacuation_centers WHERE center_id=p_id FOR UPDATE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Evacuation center no longer exists.'; END IF;
        IF p_capacity < v_occupants THEN RAISE EXCEPTION 'Capacity cannot be lower than current occupants.'; END IF;
        UPDATE public.evacuation_centers
        SET name=p_name, location=p_location, max_capacity=p_capacity,
            facilities=NULLIF(btrim(COALESCE(p_facilities,'')),'')
        WHERE center_id=p_id;
        v_id := p_id;
    END IF;
    RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_delete_center(p_id BIGINT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE v_occupants INT;
BEGIN
    PERFORM public.ems_require_official();
    SELECT current_occupants INTO v_occupants
    FROM public.evacuation_centers WHERE center_id=p_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Evacuation center no longer exists.'; END IF;
    IF v_occupants > 0 THEN RAISE EXCEPTION 'Transfer all occupants before deleting this center.'; END IF;
    DELETE FROM public.evacuation_centers WHERE center_id=p_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_register_evacuee(
    p_name TEXT,
    p_age INT,
    p_household TEXT,
    p_contact TEXT,
    p_center_id BIGINT,
    p_needs TEXT,
    p_category TEXT,
    p_allow_duplicate BOOLEAN DEFAULT FALSE
) RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_id BIGINT;
    v_capacity INT;
    v_occupants INT;
    v_name TEXT := lower(regexp_replace(btrim(COALESCE(p_name,'')), '\\s+', ' ', 'g'));
    v_house TEXT := lower(regexp_replace(btrim(COALESCE(p_household,'')), '\\s+', ' ', 'g'));
    v_phone TEXT := regexp_replace(COALESCE(p_contact,''), '[^0-9+]', '', 'g');
BEGIN
    PERFORM public.ems_current_user_id();
    IF v_name = '' THEN RAISE EXCEPTION 'Full name is required.'; END IF;
    IF p_age IS NULL OR p_age < 0 OR p_age > 120 THEN RAISE EXCEPTION 'Age must be between 0 and 120.'; END IF;
    IF v_house = '' THEN RAISE EXCEPTION 'Household is required.'; END IF;

    PERFORM pg_advisory_xact_lock(hashtext('ems-person:' || v_name || ':' || p_age::text));
    SELECT max_capacity,current_occupants INTO v_capacity,v_occupants
    FROM public.evacuation_centers WHERE center_id=p_center_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Selected evacuation center no longer exists.'; END IF;
    IF v_occupants >= v_capacity THEN RAISE EXCEPTION 'The selected center is full. Choose another center.'; END IF;

    IF NOT COALESCE(p_allow_duplicate,FALSE) AND EXISTS (
        SELECT 1 FROM public.evacuees e
        WHERE lower(regexp_replace(btrim(e.full_name), '\\s+', ' ', 'g')) = v_name
          AND e.age = p_age
          AND (
              lower(regexp_replace(btrim(COALESCE(e.household,'')), '\\s+', ' ', 'g')) = v_house
              OR (v_phone <> '' AND regexp_replace(COALESCE(e.contact,''), '[^0-9+]', '', 'g') = v_phone)
          )
    ) THEN
        RAISE EXCEPTION 'Possible duplicate found. Review the existing resident before registering another record.';
    END IF;

    INSERT INTO public.evacuees(full_name,age,household,contact,center_id,special_needs,vulnerability_category)
    VALUES(btrim(p_name),p_age,btrim(p_household),NULLIF(btrim(COALESCE(p_contact,'')),''),p_center_id,
           NULLIF(btrim(COALESCE(p_needs,'')),''),COALESCE(NULLIF(btrim(COALESCE(p_category,'')),''),'None'))
    RETURNING evacuee_id INTO v_id;

    UPDATE public.evacuation_centers
    SET current_occupants=current_occupants+1
    WHERE center_id=p_center_id;
    RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_transfer_evacuee(
    p_id BIGINT,
    p_to_center_id BIGINT,
    p_reason TEXT
) RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_actor BIGINT;
    v_from BIGINT;
    v_capacity INT;
    v_occupants INT;
BEGIN
    v_actor := public.ems_current_user_id();
    SELECT center_id INTO v_from FROM public.evacuees WHERE evacuee_id=p_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Evacuee record no longer exists.'; END IF;
    IF v_from = p_to_center_id THEN RAISE EXCEPTION 'The resident is already assigned to that center.'; END IF;

    -- Lock both center rows in deterministic order to avoid concurrent transfer deadlocks.
    PERFORM 1 FROM public.evacuation_centers
    WHERE center_id IN (v_from,p_to_center_id)
    ORDER BY center_id FOR UPDATE;

    SELECT max_capacity,current_occupants INTO v_capacity,v_occupants
    FROM public.evacuation_centers WHERE center_id=p_to_center_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Destination center no longer exists.'; END IF;
    IF v_occupants >= v_capacity THEN RAISE EXCEPTION 'The selected center is full. Choose another center.'; END IF;

    UPDATE public.evacuees SET center_id=p_to_center_id WHERE evacuee_id=p_id;
    IF v_from IS NOT NULL THEN
        UPDATE public.evacuation_centers
        SET current_occupants=GREATEST(0,current_occupants-1)
        WHERE center_id=v_from;
    END IF;
    UPDATE public.evacuation_centers
    SET current_occupants=current_occupants+1
    WHERE center_id=p_to_center_id;

    INSERT INTO public.evacuee_transfers(evacuee_id,from_center_id,to_center_id,transferred_by,reason)
    VALUES(p_id,v_from,p_to_center_id,v_actor,NULLIF(btrim(COALESCE(p_reason,'')),''));
END $$;

CREATE OR REPLACE FUNCTION public.ems_save_resource(
    p_id BIGINT,
    p_center_id BIGINT,
    p_name TEXT,
    p_unit TEXT,
    p_quantity INT,
    p_used INT,
    p_minimum INT
) RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_id BIGINT;
    v_old_center BIGINT;
    v_old_name TEXT;
    v_old_unit TEXT;
BEGIN
    PERFORM public.ems_current_user_id();
    p_name := btrim(COALESCE(p_name,''));
    p_unit := btrim(COALESCE(p_unit,''));
    IF p_name='' THEN RAISE EXCEPTION 'Item name is required.'; END IF;
    IF p_unit='' THEN RAISE EXCEPTION 'Unit is required.'; END IF;
    IF p_quantity IS NULL OR p_quantity < 0 OR p_used IS NULL OR p_used < 0 OR p_minimum IS NULL OR p_minimum < 0 THEN
        RAISE EXCEPTION 'Resource quantities cannot be negative.';
    END IF;
    IF p_used > p_quantity THEN RAISE EXCEPTION 'Used quantity cannot exceed available stock.'; END IF;
    IF NOT EXISTS(SELECT 1 FROM public.evacuation_centers WHERE center_id=p_center_id) THEN
        RAISE EXCEPTION 'Selected evacuation center no longer exists.';
    END IF;

    PERFORM pg_advisory_xact_lock(hashtext('ems-resource:' || p_center_id::text || ':' || lower(p_name) || ':' || lower(p_unit)));

    IF p_id IS NULL THEN
        IF EXISTS(SELECT 1 FROM public.resources WHERE center_id=p_center_id AND lower(btrim(resource_type))=lower(p_name) AND lower(btrim(COALESCE(unit,'')))=lower(p_unit)) THEN
            RAISE EXCEPTION 'This resource already exists at the selected center. Update its inventory instead.';
        END IF;
        INSERT INTO public.resources(center_id,resource_type,quantity,used_quantity,minimum_stock,unit,last_updated)
        VALUES(p_center_id,p_name,p_quantity,p_used,p_minimum,p_unit,now())
        RETURNING resource_id INTO v_id;
    ELSE
        SELECT center_id,resource_type,COALESCE(unit,'') INTO v_old_center,v_old_name,v_old_unit
        FROM public.resources WHERE resource_id=p_id FOR UPDATE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Resource record no longer exists.'; END IF;
        IF v_old_center <> p_center_id THEN RAISE EXCEPTION 'A resource cannot be moved to another center by editing it. Use Allocation instead.'; END IF;
        IF (lower(btrim(v_old_name)) <> lower(p_name) OR lower(btrim(v_old_unit)) <> lower(p_unit))
           AND EXISTS(SELECT 1 FROM public.resource_allocations WHERE resource_id=p_id) THEN
            RAISE EXCEPTION 'Resources with allocation history cannot be renamed. Create a new resource row instead.';
        END IF;
        IF EXISTS(SELECT 1 FROM public.resources WHERE center_id=p_center_id AND resource_id<>p_id
                  AND lower(btrim(resource_type))=lower(p_name) AND lower(btrim(COALESCE(unit,'')))=lower(p_unit)) THEN
            RAISE EXCEPTION 'This resource already exists at the selected center.';
        END IF;
        UPDATE public.resources
        SET resource_type=p_name,unit=p_unit,quantity=p_quantity,used_quantity=p_used,
            minimum_stock=p_minimum,last_updated=now()
        WHERE resource_id=p_id;
        v_id := p_id;
    END IF;
    RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_allocate_resource(
    p_resource_id BIGINT,
    p_to_center_id BIGINT,
    p_quantity INT
) RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_actor BIGINT;
    v_source_center BIGINT;
    v_name TEXT;
    v_unit TEXT;
    v_total INT;
    v_used INT;
    v_destination_resource BIGINT;
BEGIN
    v_actor := public.ems_current_user_id();
    IF p_quantity IS NULL OR p_quantity <= 0 THEN RAISE EXCEPTION 'Quantity must be at least 1.'; END IF;

    SELECT center_id,resource_type,COALESCE(unit,''),quantity,used_quantity
    INTO v_source_center,v_name,v_unit,v_total,v_used
    FROM public.resources WHERE resource_id=p_resource_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Source resource no longer exists.'; END IF;
    IF p_to_center_id = v_source_center THEN RAISE EXCEPTION 'Destination center must be different from the source center.'; END IF;
    IF NOT EXISTS(SELECT 1 FROM public.evacuation_centers WHERE center_id=p_to_center_id) THEN
        RAISE EXCEPTION 'Destination center no longer exists.';
    END IF;
    IF p_quantity > (v_total-v_used) THEN
        RAISE EXCEPTION 'Not enough remaining stock for this allocation.';
    END IF;

    PERFORM pg_advisory_xact_lock(hashtext('ems-resource:' || p_to_center_id::text || ':' || lower(btrim(v_name)) || ':' || lower(btrim(v_unit))));
    SELECT resource_id INTO v_destination_resource
    FROM public.resources
    WHERE center_id=p_to_center_id
      AND lower(btrim(resource_type))=lower(btrim(v_name))
      AND lower(btrim(COALESCE(unit,'')))=lower(btrim(v_unit))
    FOR UPDATE;

    UPDATE public.resources
    SET used_quantity=used_quantity+p_quantity,last_updated=now()
    WHERE resource_id=p_resource_id;

    IF v_destination_resource IS NULL THEN
        INSERT INTO public.resources(center_id,resource_type,quantity,used_quantity,minimum_stock,unit,last_updated)
        VALUES(p_to_center_id,v_name,p_quantity,0,0,v_unit,now());
    ELSE
        UPDATE public.resources
        SET quantity=quantity+p_quantity,last_updated=now()
        WHERE resource_id=v_destination_resource;
    END IF;

    INSERT INTO public.resource_allocations(resource_id,center_id,quantity,allocated_by)
    VALUES(p_resource_id,p_to_center_id,p_quantity,v_actor);
END $$;

CREATE OR REPLACE FUNCTION public.ems_snapshot()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
STABLE
SET search_path = public, auth, pg_temp
AS $$
BEGIN
    PERFORM public.ems_current_user_id();
    RETURN jsonb_build_object(
        'centers', COALESCE((SELECT jsonb_agg(to_jsonb(c) ORDER BY c.center_id) FROM public.evacuation_centers c), '[]'::jsonb),
        'people', COALESCE((SELECT jsonb_agg(to_jsonb(e) ORDER BY e.evacuee_id) FROM public.evacuees e), '[]'::jsonb),
        'supplies', COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.resource_id) FROM public.resources r), '[]'::jsonb),
        'allocations', COALESCE((SELECT jsonb_agg(to_jsonb(a) ORDER BY a.allocation_id DESC) FROM public.resource_allocations a), '[]'::jsonb),
        'transfers', COALESCE((SELECT jsonb_agg(to_jsonb(t) ORDER BY t.transfer_id DESC) FROM public.evacuee_transfers t), '[]'::jsonb)
    );
END $$;

-- RLS: linked users can read operations; profile rows are self-only.
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.evacuation_centers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.evacuees ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.resources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.resource_allocations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.evacuee_transfers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS ems_users_self_read ON public.users;
CREATE POLICY ems_users_self_read ON public.users
FOR SELECT TO authenticated
USING (auth_user_id = auth.uid());

DROP POLICY IF EXISTS ems_centers_read ON public.evacuation_centers;
CREATE POLICY ems_centers_read ON public.evacuation_centers FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.users u WHERE u.auth_user_id=auth.uid()));
DROP POLICY IF EXISTS ems_evacuees_read ON public.evacuees;
CREATE POLICY ems_evacuees_read ON public.evacuees FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.users u WHERE u.auth_user_id=auth.uid()));
DROP POLICY IF EXISTS ems_resources_read ON public.resources;
CREATE POLICY ems_resources_read ON public.resources FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.users u WHERE u.auth_user_id=auth.uid()));
DROP POLICY IF EXISTS ems_allocations_read ON public.resource_allocations;
CREATE POLICY ems_allocations_read ON public.resource_allocations FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.users u WHERE u.auth_user_id=auth.uid()));
DROP POLICY IF EXISTS ems_transfers_read ON public.evacuee_transfers;
CREATE POLICY ems_transfers_read ON public.evacuee_transfers FOR SELECT TO authenticated
USING (EXISTS (SELECT 1 FROM public.users u WHERE u.auth_user_id=auth.uid()));

GRANT USAGE ON SCHEMA public TO authenticated;
GRANT SELECT ON public.users, public.evacuation_centers, public.evacuees, public.resources,
    public.resource_allocations, public.evacuee_transfers TO authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.users, public.evacuation_centers, public.evacuees,
    public.resources, public.resource_allocations, public.evacuee_transfers FROM anon, authenticated;

REVOKE ALL ON FUNCTION public.ems_current_user_id() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_require_official() FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_save_center(BIGINT,TEXT,TEXT,INT,TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_delete_center(BIGINT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_register_evacuee(TEXT,INT,TEXT,TEXT,BIGINT,TEXT,TEXT,BOOLEAN) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_transfer_evacuee(BIGINT,BIGINT,TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_save_resource(BIGINT,BIGINT,TEXT,TEXT,INT,INT,INT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_allocate_resource(BIGINT,BIGINT,INT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.ems_snapshot() FROM PUBLIC;

GRANT EXECUTE ON FUNCTION public.ems_save_center(BIGINT,TEXT,TEXT,INT,TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.ems_delete_center(BIGINT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.ems_register_evacuee(TEXT,INT,TEXT,TEXT,BIGINT,TEXT,TEXT,BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION public.ems_transfer_evacuee(BIGINT,BIGINT,TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.ems_save_resource(BIGINT,BIGINT,TEXT,TEXT,INT,INT,INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.ems_allocate_resource(BIGINT,BIGINT,INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.ems_snapshot() TO authenticated;

COMMIT;
