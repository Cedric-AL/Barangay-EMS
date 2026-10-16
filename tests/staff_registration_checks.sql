-- DISPOSABLE LOCAL DATABASE ONLY. Requires bootstrap, 01, 02 and 04 migrations.
-- Legacy confirmation-required behavior only: do not run after migration 08.
-- Run with psql -v ON_ERROR_STOP=1; all fixtures roll back.
BEGIN;
INSERT INTO auth.users(id,email,email_confirmed_at) VALUES
('00000000-0000-0000-0000-000000000041','official-check@example.test',now());
INSERT INTO public.users(username,full_name,role,auth_user_id)
VALUES('official-check','Test Official','Barangay Official','00000000-0000-0000-0000-000000000041');
INSERT INTO auth.users(id,email,email_confirmed_at,raw_user_meta_data) VALUES
('00000000-0000-0000-0000-000000000042','staff-check@example.test',now(),
'{"ems_staff_application":true,"username":"staff-check","full_name":"Test Staff","role":"Barangay Official","status":"Approved"}'),
('00000000-0000-0000-0000-000000000043','rejected-check@example.test',null,
'{"ems_staff_application":true,"username":"rejected-check","full_name":"Rejected Staff"}');
INSERT INTO public.evacuation_centers(name,location,max_capacity) VALUES('Staff test center','Test only',10);
SELECT set_config('request.jwt.claim.sub','00000000-0000-0000-0000-000000000042',true);
SET LOCAL ROLE authenticated;
DO $$ BEGIN
    IF public.ems_application_status()<>'Pending' THEN RAISE EXCEPTION 'Signup granted approval'; END IF;
    IF ems_private.is_member() THEN RAISE EXCEPTION 'Pending applicant is a member'; END IF;
    IF EXISTS(SELECT 1 FROM public.evacuation_centers) THEN RAISE EXCEPTION 'Pending applicant can read operations'; END IF;
    BEGIN
        PERFORM public.ems_snapshot();
        RAISE EXCEPTION 'Pending snapshot unexpectedly succeeded';
    EXCEPTION WHEN insufficient_privilege THEN NULL; END;
    BEGIN
        PERFORM public.ems_staff_applications();
        RAISE EXCEPTION 'Applicant can list applications';
    EXCEPTION WHEN insufficient_privilege THEN NULL; END;
    BEGIN
        PERFORM public.ems_review_staff(auth.uid(),'Approve',NULL,NULL);
        RAISE EXCEPTION 'Applicant can approve themselves';
    EXCEPTION WHEN insufficient_privilege THEN NULL; END;
    BEGIN
        UPDATE public.staff_applications SET status='Approved' WHERE auth_user_id=auth.uid();
        RAISE EXCEPTION 'Applicant can directly edit approval';
    EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
RESET ROLE;
SELECT set_config('request.jwt.claim.sub','00000000-0000-0000-0000-000000000041',true);
SET LOCAL ROLE authenticated;
SELECT public.ems_review_staff('00000000-0000-0000-0000-000000000042','Approve',
    (SELECT center_id FROM public.evacuation_centers WHERE name='Staff test center'),'Verified');
DO $$ BEGIN
    BEGIN
        PERFORM public.ems_review_staff('00000000-0000-0000-0000-000000000043','Approve',
            (SELECT center_id FROM public.evacuation_centers WHERE name='Staff test center'),NULL);
        RAISE EXCEPTION 'Unconfirmed applicant was approved';
    EXCEPTION WHEN raise_exception THEN
        IF SQLERRM<>'The applicant must confirm their email before approval.' THEN RAISE; END IF;
    END;
END $$;
SELECT public.ems_review_staff('00000000-0000-0000-0000-000000000043','Reject',NULL,'Not eligible');
RESET ROLE;
SELECT set_config('request.jwt.claim.sub','00000000-0000-0000-0000-000000000042',true);
SET LOCAL ROLE authenticated;
DO $$ BEGIN
    IF NOT ems_private.is_member() THEN RAISE EXCEPTION 'Approved staff denied membership'; END IF;
    IF NOT EXISTS(SELECT 1 FROM public.users WHERE auth_user_id=auth.uid() AND role='Center Staff') THEN
        RAISE EXCEPTION 'Role was not fixed to Center Staff';
    END IF;
    PERFORM public.ems_snapshot();
    BEGIN
        PERFORM public.ems_staff_applications();
        RAISE EXCEPTION 'Approved staff can review applications';
    EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
RESET ROLE;
SELECT set_config('request.jwt.claim.sub','00000000-0000-0000-0000-000000000041',true);
SET LOCAL ROLE authenticated;
SELECT public.ems_review_staff('00000000-0000-0000-0000-000000000042','Deactivate',NULL,'Shift ended');
RESET ROLE;
SELECT set_config('request.jwt.claim.sub','00000000-0000-0000-0000-000000000042',true);
SET LOCAL ROLE authenticated;
DO $$ BEGIN
    IF ems_private.is_member() THEN RAISE EXCEPTION 'Deactivated staff remains a member'; END IF;
    IF EXISTS(SELECT 1 FROM public.evacuation_centers) THEN RAISE EXCEPTION 'Deactivated session can read operations'; END IF;
    BEGIN
        PERFORM public.ems_register_evacuee('Forbidden',20,NULL,NULL,1,NULL,NULL,FALSE);
        RAISE EXCEPTION 'Deactivated session can write operations';
    EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;
RESET ROLE;
ROLLBACK;
