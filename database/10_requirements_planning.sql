-- Run AFTER 09_archive_centers.sql in the Supabase SQL Editor.
BEGIN;
DO $$ BEGIN
    IF to_regprocedure('public.ems_archive_center(bigint)') IS NULL THEN
        RAISE EXCEPTION 'Apply database/09_archive_centers.sql before migration 10.';
    END IF;
END $$;
ALTER TABLE public.evacuation_centers
    ADD COLUMN IF NOT EXISTS facility_features TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS facility_profile_complete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.evacuees ADD COLUMN IF NOT EXISTS requirements TEXT[] NOT NULL DEFAULT '{}';

CREATE TABLE IF NOT EXISTS public.supply_rules (
    resource_type TEXT NOT NULL CHECK(length(btrim(resource_type)) BETWEEN 1 AND 100),
    unit TEXT NOT NULL CHECK(length(btrim(unit)) BETWEEN 1 AND 30),
    target_group TEXT NOT NULL CHECK(target_group IN ('All evacuees','Child','Senior Citizen','Person with Disability','Pregnant','Other')),
    daily_rate NUMERIC(10,4) NOT NULL CHECK(daily_rate>0 AND daily_rate<=100000),
    coverage_days INT NOT NULL CHECK(coverage_days BETWEEN 1 AND 365),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by BIGINT REFERENCES public.users(user_id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS supply_rules_item_unit ON public.supply_rules(lower(btrim(resource_type)),lower(btrim(unit)));
ALTER TABLE public.supply_rules ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.supply_rules FROM PUBLIC,anon,authenticated;

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
    INSERT INTO public.supply_rules(resource_type,unit,target_group,daily_rate,coverage_days,enabled,updated_by)
    VALUES(btrim(p_name),btrim(p_unit),p_group,p_rate,p_days,p_enabled,v_actor)
    ON CONFLICT(lower(btrim(resource_type)),lower(btrim(unit))) DO UPDATE
    SET target_group=excluded.target_group,daily_rate=excluded.daily_rate,coverage_days=excluded.coverage_days,
        enabled=excluded.enabled,updated_by=excluded.updated_by,updated_at=now();
END $$;

CREATE OR REPLACE FUNCTION public.ems_validate_features(p_features TEXT[])
RETURNS VOID LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
    IF p_features IS NULL OR array_position(p_features,NULL) IS NOT NULL OR NOT p_features <@ ARRAY[
        'Wheelchair access','Accessible toilets','First-aid area','Child-friendly space','Private care area']::TEXT[] THEN
        RAISE EXCEPTION 'Choose facility requirements from the supported checklist.';
    END IF;
END $$;

-- Each wrapper saves the existing operation and its profile in the same transaction.
CREATE OR REPLACE FUNCTION public.ems_save_center_profile(p_id BIGINT,p_name TEXT,p_location TEXT,p_capacity INT,p_facilities TEXT,p_features TEXT[],p_complete BOOLEAN)
RETURNS BIGINT LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_id BIGINT;
BEGIN
    PERFORM ems_private.require_user(TRUE);
    PERFORM public.ems_validate_features(p_features);
    IF p_complete IS NULL THEN RAISE EXCEPTION 'Specify whether facilities have been assessed.'; END IF;
    v_id:=public.ems_save_center(p_id,p_name,p_location,p_capacity,p_facilities);
    UPDATE public.evacuation_centers SET facility_features=p_features,facility_profile_complete=p_complete WHERE center_id=v_id;
    RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_register_evacuee_planned(p_name TEXT,p_age INT,p_household TEXT,p_contact TEXT,p_center_id BIGINT,p_needs TEXT,p_category TEXT,p_allow_duplicate BOOLEAN,p_requirements TEXT[])
RETURNS BIGINT LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_id BIGINT;
BEGIN
    PERFORM ems_private.require_user();
    PERFORM public.ems_validate_features(p_requirements);
    v_id:=public.ems_register_evacuee(p_name,p_age,p_household,p_contact,p_center_id,p_needs,p_category,p_allow_duplicate);
    UPDATE public.evacuees SET requirements=p_requirements WHERE evacuee_id=v_id;
    RETURN v_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_update_evacuee_planned(p_id BIGINT,p_name TEXT,p_age INT,p_household TEXT,p_contact TEXT,p_needs TEXT,p_category TEXT,p_requirements TEXT[])
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    PERFORM ems_private.require_user();
    PERFORM public.ems_validate_features(p_requirements);
    PERFORM public.ems_update_evacuee(p_id,p_name,p_age,p_household,p_contact,p_needs,p_category);
    UPDATE public.evacuees SET requirements=p_requirements WHERE evacuee_id=p_id;
END $$;

CREATE OR REPLACE FUNCTION public.ems_snapshot()
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER STABLE SET search_path = '' AS $$
BEGIN
    PERFORM ems_private.require_user();
    RETURN jsonb_build_object(
        'centers',COALESCE((SELECT jsonb_agg(to_jsonb(c) ORDER BY c.center_id) FROM public.evacuation_centers c),'[]'::jsonb),
        'people',COALESCE((SELECT jsonb_agg(to_jsonb(e) ORDER BY e.evacuee_id) FROM public.evacuees e),'[]'::jsonb),
        'supplies',COALESCE((SELECT jsonb_agg(to_jsonb(r) ORDER BY r.resource_id) FROM public.resources r),'[]'::jsonb),
        'allocations',COALESCE((SELECT jsonb_agg(to_jsonb(a) ORDER BY a.allocation_id DESC) FROM public.resource_allocations a),'[]'::jsonb),
        'transfers',COALESCE((SELECT jsonb_agg(to_jsonb(t) ORDER BY t.transfer_id DESC) FROM public.evacuee_transfers t),'[]'::jsonb),
        'supplyRules',COALESCE((SELECT jsonb_agg(to_jsonb(s) ORDER BY s.resource_type,s.unit) FROM public.supply_rules s),'[]'::jsonb)
    );
END $$;

REVOKE ALL ON FUNCTION public.ems_validate_features(TEXT[]) FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.ems_save_supply_rule(TEXT,TEXT,TEXT,NUMERIC,INT,BOOLEAN),
 public.ems_save_center_profile(BIGINT,TEXT,TEXT,INT,TEXT,TEXT[],BOOLEAN),
 public.ems_register_evacuee_planned(TEXT,INT,TEXT,TEXT,BIGINT,TEXT,TEXT,BOOLEAN,TEXT[]),
 public.ems_update_evacuee_planned(BIGINT,TEXT,INT,TEXT,TEXT,TEXT,TEXT,TEXT[]),public.ems_snapshot() FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.ems_save_supply_rule(TEXT,TEXT,TEXT,NUMERIC,INT,BOOLEAN),
 public.ems_save_center_profile(BIGINT,TEXT,TEXT,INT,TEXT,TEXT[],BOOLEAN),
 public.ems_register_evacuee_planned(TEXT,INT,TEXT,TEXT,BIGINT,TEXT,TEXT,BOOLEAN,TEXT[]),
 public.ems_update_evacuee_planned(BIGINT,TEXT,INT,TEXT,TEXT,TEXT,TEXT,TEXT[]),public.ems_snapshot() TO authenticated;
NOTIFY pgrst,'reload schema';
COMMIT;
