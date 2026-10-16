-- Classroom setup: official approval is required; email verification is waived.
-- Apply after 04_staff_registration.sql. Disable Confirm Email in Auth settings too.
BEGIN;
CREATE OR REPLACE FUNCTION public.ems_review_staff(p_auth_id UUID,p_action TEXT,p_center_id BIGINT,p_note TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_reviewer BIGINT; v_app public.staff_applications; v_user public.users;
BEGIN
    v_reviewer:=ems_private.require_user(TRUE);
    SELECT * INTO v_app FROM public.staff_applications WHERE auth_user_id=p_auth_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Application no longer exists. Refresh.'; END IF;
    SELECT * INTO v_user FROM public.users WHERE auth_user_id=p_auth_id FOR UPDATE;
    IF FOUND AND v_user.role<>'Center Staff' THEN RAISE EXCEPTION 'Official accounts cannot be changed here.'; END IF;
    IF p_action='Approve' THEN
        IF v_app.status NOT IN ('Pending','Deactivated') THEN RAISE EXCEPTION 'Refresh: only pending or deactivated staff can be approved.'; END IF;
PERFORM 1 FROM public.evacuation_centers WHERE center_id=p_center_id FOR KEY SHARE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Select an existing evacuation center.'; END IF;
        IF EXISTS(SELECT 1 FROM public.users WHERE username=v_app.username AND auth_user_id IS DISTINCT FROM p_auth_id) THEN
            RAISE EXCEPTION 'Username is already used. Ask the administrator to resolve the application username.';
        END IF;
        INSERT INTO public.users(username,full_name,role,auth_user_id,is_active,assigned_center_id)
        VALUES(v_app.username,v_app.full_name,'Center Staff',p_auth_id,TRUE,p_center_id)
        ON CONFLICT(auth_user_id) DO UPDATE SET is_active=TRUE,assigned_center_id=excluded.assigned_center_id;
        UPDATE public.staff_applications SET status='Approved',assigned_center_id=p_center_id WHERE auth_user_id=p_auth_id;
    ELSIF p_action='Reject' THEN
        IF v_app.status<>'Pending' THEN RAISE EXCEPTION 'Only pending applications can be rejected.'; END IF;
        IF nullif(btrim(p_note),'') IS NULL THEN RAISE EXCEPTION 'Enter a rejection reason.'; END IF;
        UPDATE public.staff_applications SET status='Rejected' WHERE auth_user_id=p_auth_id;
    ELSIF p_action='Deactivate' THEN
        IF v_app.status<>'Approved' THEN RAISE EXCEPTION 'Only approved staff can be deactivated.'; END IF;
        UPDATE public.users SET is_active=FALSE WHERE auth_user_id=p_auth_id AND role='Center Staff';
        UPDATE public.staff_applications SET status='Deactivated' WHERE auth_user_id=p_auth_id;
    ELSE RAISE EXCEPTION 'Unsupported review action.';
    END IF;
    UPDATE public.staff_applications SET reviewed_at=now(),reviewed_by=v_reviewer,review_note=nullif(btrim(p_note),'')
    WHERE auth_user_id=p_auth_id;
END $$;

REVOKE ALL ON FUNCTION public.ems_review_staff(UUID,TEXT,BIGINT,TEXT) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.ems_review_staff(UUID,TEXT,BIGINT,TEXT) TO authenticated;
-- Allow existing pending/approved staff test accounts to sign in without email verification.
-- This does not approve applications or change passwords.
UPDATE auth.users AS u
SET email_confirmed_at=now()
WHERE u.email_confirmed_at IS NULL
  AND EXISTS (SELECT 1 FROM public.staff_applications a WHERE a.auth_user_id=u.id AND a.status IN ('Pending','Approved'));
NOTIFY pgrst,'reload schema';
COMMIT;

