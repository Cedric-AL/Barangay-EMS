-- TEST DATABASE ONLY. Requires 01_schema.sql and 02_integration.sql already applied.
-- The setup/bootstrap uses a disposable auth.users stub in tests/postgres_bootstrap.sql.
-- Run with psql -v ON_ERROR_STOP=1. All test records roll back.
BEGIN;
INSERT INTO auth.users(id,email) VALUES
('11111111-1111-4111-8111-111111111111','official@example.test'),
('22222222-2222-4222-8222-222222222222','staff@example.test');
INSERT INTO public.users(username,full_name,role,auth_user_id) VALUES
('fixture-official','Fixture Official','Barangay Official','11111111-1111-4111-8111-111111111111'),
('fixture-staff','Fixture Staff','Center Staff','22222222-2222-4222-8222-222222222222');
SELECT set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',true);
SET LOCAL ROLE authenticated;
DO $$
DECLARE c1 BIGINT; c2 BIGINT; c3 BIGINT; e BIGINT; r BIGINT; v_before BIGINT; v_after BIGINT; rejected BOOLEAN;
BEGIN
    c1:=public.ems_save_center(NULL,'Fixture Hall','Purok 1',2,'Ramp');
    c2:=public.ems_save_center(NULL,'Fixture Court','Purok 2',1,'Toilets');
    c3:=public.ems_save_center(NULL,'Fixture Empty','Purok 3',1,'');
    e:=public.ems_register_evacuee('Fixture Person',24,'Fixture Family','09171234567',c1,'None','None',FALSE);
    IF (SELECT current_occupants FROM public.evacuation_centers WHERE center_id=c1)<>1 THEN RAISE EXCEPTION 'Registration failed occupancy assertion'; END IF;
    rejected:=FALSE;
    BEGIN PERFORM public.ems_register_evacuee('Fixture Person',24,'Fixture Family','09171234567',c1,'None','None',FALSE);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Duplicate review was bypassed'; END IF;
    PERFORM public.ems_transfer_evacuee(e,c2,'Confirmed move');
    IF (SELECT current_occupants FROM public.evacuation_centers WHERE center_id=c1)<>0
       OR (SELECT current_occupants FROM public.evacuation_centers WHERE center_id=c2)<>1
       OR (SELECT status FROM public.evacuation_centers WHERE center_id=c2)<>'Full'
       OR NOT EXISTS(SELECT 1 FROM public.evacuee_transfers WHERE evacuee_id=e AND from_center_id=c1 AND to_center_id=c2 AND transferred_by IS NOT NULL)
       THEN RAISE EXCEPTION 'Transfer counters/status/audit assertion failed'; END IF;
    rejected:=FALSE;
    BEGIN PERFORM public.ems_register_evacuee('Second Person',30,'Other Family','',c2,'None','None',FALSE);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Full center accepted resident'; END IF;
    r:=public.ems_save_resource(NULL,c1,'Fixture Water','Liters',100,20,90);
    SELECT sum(quantity-used_quantity) INTO v_before FROM public.resources WHERE center_id IN(c1,c2);
    PERFORM public.ems_allocate_resource(r,c2,30);
    SELECT sum(quantity-used_quantity) INTO v_after FROM public.resources WHERE center_id IN(c1,c2);
    IF v_before<>v_after OR (SELECT used_quantity FROM public.resources WHERE resource_id=r)<>50
       OR NOT EXISTS(SELECT 1 FROM public.resource_allocations WHERE resource_id=r AND center_id=c2 AND quantity=30 AND allocated_by IS NOT NULL)
       THEN RAISE EXCEPTION 'Allocation conservation/audit assertion failed'; END IF;
    rejected:=FALSE;
    BEGIN PERFORM public.ems_allocate_resource(r,c2,999);
    EXCEPTION WHEN raise_exception THEN rejected:=TRUE; END;
    IF NOT rejected OR (SELECT used_quantity FROM public.resources WHERE resource_id=r)<>50 THEN RAISE EXCEPTION 'Over-allocation was not rolled back'; END IF;
    rejected:=FALSE;
    BEGIN INSERT INTO public.evacuees(full_name,age,center_id) VALUES('Bypass',20,c1);
    EXCEPTION WHEN insufficient_privilege THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Direct table write unexpectedly allowed'; END IF;
    PERFORM public.ems_delete_center(c3);
    IF EXISTS(SELECT 1 FROM public.evacuation_centers WHERE center_id=c3) THEN RAISE EXCEPTION 'Delete failed'; END IF;
    IF jsonb_array_length(public.ems_snapshot()->'people')<1 THEN RAISE EXCEPTION 'Snapshot missing residents'; END IF;
END $$;
SELECT set_config('request.jwt.claim.sub','22222222-2222-4222-8222-222222222222',true);
DO $$
DECLARE rejected BOOLEAN:=FALSE;
BEGIN
    BEGIN PERFORM public.ems_save_center(NULL,'Forbidden','No',1,'');
    EXCEPTION WHEN insufficient_privilege THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Staff gained center-management permission'; END IF;
END $$;
SELECT set_config('request.jwt.claim.sub','33333333-3333-4333-8333-333333333333',true);
DO $$
DECLARE rejected BOOLEAN:=FALSE;
BEGIN
    BEGIN PERFORM public.ems_snapshot(); EXCEPTION WHEN insufficient_privilege THEN rejected:=TRUE; END;
    IF NOT rejected THEN RAISE EXCEPTION 'Unlinked account accessed operational snapshot'; END IF;
END $$;
RESET ROLE;
ROLLBACK;
