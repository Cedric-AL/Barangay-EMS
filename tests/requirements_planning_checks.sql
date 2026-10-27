-- DISPOSABLE TEST DATABASE ONLY; apply existing migrations through 10 first.
-- Run as test DB administrator with ON_ERROR_STOP; fixtures roll back.
BEGIN;
INSERT INTO auth.users(id,email) VALUES
('00000000-0000-4000-8000-000000000101','planning-official@example.test'),
('00000000-0000-4000-8000-000000000102','planning-staff@example.test');
INSERT INTO public.users(username,full_name,role,auth_user_id) VALUES
('planning-official','Planning Official','Barangay Official','00000000-0000-4000-8000-000000000101'),
('planning-staff','Planning Staff','Center Staff','00000000-0000-4000-8000-000000000102');
SELECT set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000101',true);
SET LOCAL ROLE authenticated;
DO $$
DECLARE c BIGINT; other_center BIGINT; person BIGINT; snapshot JSONB; rejected BOOLEAN; resource BIGINT;
BEGIN
    c:=public.ems_save_center_profile(NULL,'Planning Fixture A','Test only',10,'Ramp',ARRAY['Wheelchair access'],TRUE);
    other_center:=public.ems_save_center_profile(NULL,'Planning Fixture B','Test only',10,'',ARRAY[]::TEXT[],FALSE);
    person:=public.ems_register_evacuee_planned('Planning Person',25,'Fixture family','',c,'Human review','None',FALSE,ARRAY['Wheelchair access']);
    snapshot:=public.ems_snapshot();
    IF NOT EXISTS(SELECT 1 FROM jsonb_array_elements(snapshot->'people') p WHERE (p->>'evacuee_id')::BIGINT=person
       AND p->'requirements' @> '["Wheelchair access"]') THEN RAISE EXCEPTION 'Requirements not persisted'; END IF;
    PERFORM public.ems_update_evacuee_planned(person,'Planning Person',25,'Fixture family','','','None',ARRAY['Accessible toilets']);
    PERFORM public.ems_transfer_evacuee(person,other_center,'Fixture transfer');
    snapshot:=public.ems_snapshot();
    IF NOT EXISTS(SELECT 1 FROM jsonb_array_elements(snapshot->'people') p WHERE (p->>'evacuee_id')::BIGINT=person
       AND p->'requirements' @> '["Accessible toilets"]' AND (p->>'center_id')::BIGINT=other_center)
       THEN RAISE EXCEPTION 'Transfer did not retain requirements'; END IF;
    PERFORM public.ems_save_supply_rule('Planning packs','packs','All evacuees',2,3,TRUE);
    PERFORM public.ems_save_supply_rule(' planning PACKS ',' PACKS ','Child',0.5,2,TRUE);
    snapshot:=public.ems_snapshot();
    IF (SELECT count(*) FROM jsonb_array_elements(snapshot->'supplyRules') r WHERE lower(btrim(r->>'resource_type'))='planning packs')<>1
       THEN RAISE EXCEPTION 'Rule upsert created duplicates'; END IF;
    IF NOT EXISTS(SELECT 1 FROM jsonb_array_elements(snapshot->'supplyRules') r WHERE r->>'target_group'='Child' AND (r->>'daily_rate')::NUMERIC=0.5)
       THEN RAISE EXCEPTION 'Rule not updated'; END IF;
    rejected:=FALSE;
    BEGIN PERFORM public.ems_save_supply_rule('Bad','packs','All evacuees',0,1,TRUE);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Zero rate accepted'; END IF;
    rejected:=FALSE;
    BEGIN PERFORM public.ems_save_center_profile(NULL,'Invalid Profile','Test only',10,'',ARRAY['Invented feature'],TRUE);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Invalid feature accepted'; END IF;
    resource:=public.ems_save_resource(NULL,c,'Planning stock','packs',20,0,5);
    PERFORM public.ems_allocate_resource(resource,other_center,5);
    PERFORM public.ems_archive_center(c);
    rejected:=FALSE;
    BEGIN PERFORM public.ems_register_evacuee_planned('Archived fixture',24,'Other fixture','',c,'','None',FALSE,ARRAY[]::TEXT[]);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Archived registration accepted'; END IF;
    rejected:=FALSE;
    BEGIN PERFORM public.ems_allocate_resource((SELECT resource_id FROM public.resources WHERE center_id=other_center AND resource_type='Planning stock'),c,1);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Archived allocation accepted'; END IF;
END $$;
SELECT set_config('request.jwt.claim.sub','00000000-0000-4000-8000-000000000102',true);
DO $$
DECLARE rejected BOOLEAN:=FALSE;
BEGIN
    BEGIN PERFORM public.ems_save_supply_rule('Forbidden','packs','All evacuees',1,1,TRUE);
    EXCEPTION WHEN insufficient_privilege THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Staff can change supply rules'; END IF;
    IF public.ems_snapshot()->'supplyRules' IS NULL THEN RAISE EXCEPTION 'Staff cannot view planning rules'; END IF;
END $$;
RESET ROLE;
ROLLBACK;
