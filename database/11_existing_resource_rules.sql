-- Apply after migration 10. Existing rules are preserved.
BEGIN;
CREATE OR REPLACE FUNCTION public.ems_save_supply_rule(p_name TEXT,p_unit TEXT,p_group TEXT,p_rate NUMERIC,p_days INT,p_enabled BOOLEAN)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_actor BIGINT;
BEGIN
    v_actor:=ems_private.require_user(TRUE);
    IF p_rate IS NULL OR p_rate<=0 OR p_rate>100000 OR p_rate<>round(p_rate,4)
        OR p_days IS NULL OR p_days NOT BETWEEN 1 AND 365 OR p_enabled IS NULL
        OR p_name IS NULL OR length(btrim(p_name)) NOT BETWEEN 1 AND 100
        OR p_unit IS NULL OR length(btrim(p_unit)) NOT BETWEEN 1 AND 30
        OR p_group IS NULL OR p_group NOT IN ('All evacuees','Child','Senior Citizen','Person with Disability','Pregnant','Other') THEN
        RAISE EXCEPTION 'Enter a valid item, unit, people group, positive daily rate (up to four decimals) and coverage days (1-365).';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM public.resources r
        WHERE lower(btrim(r.resource_type))=lower(btrim(p_name))
          AND lower(btrim(r.unit))=lower(btrim(p_unit))) THEN
        RAISE EXCEPTION 'Choose an existing inventory item and unit. Add the resource first.';
    END IF;
    INSERT INTO public.supply_rules(resource_type,unit,target_group,daily_rate,coverage_days,enabled,updated_by)
    VALUES(btrim(p_name),btrim(p_unit),p_group,p_rate,p_days,p_enabled,v_actor)
    ON CONFLICT(lower(btrim(resource_type)),lower(btrim(unit))) DO UPDATE
    SET target_group=excluded.target_group,daily_rate=excluded.daily_rate,coverage_days=excluded.coverage_days,
        enabled=excluded.enabled,updated_by=excluded.updated_by,updated_at=now();
END $$;

NOTIFY pgrst,'reload schema';
COMMIT;
