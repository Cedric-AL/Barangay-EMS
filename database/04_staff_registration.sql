-- Run once AFTER 02_integration.sql, in Supabase SQL Editor as administrator.
-- Existing linked accounts remain active. No Auth accounts are created here.
BEGIN;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS assigned_center_id BIGINT REFERENCES public.evacuation_centers(center_id);

CREATE TABLE public.staff_applications (
    auth_user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL CHECK (length(btrim(username)) BETWEEN 1 AND 50),
    full_name VARCHAR(100) NOT NULL CHECK (length(btrim(full_name)) BETWEEN 1 AND 100),
    status TEXT NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending','Approved','Rejected','Deactivated')),
    assigned_center_id BIGINT REFERENCES public.evacuation_centers(center_id),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT REFERENCES public.users(user_id),
    review_note TEXT
);
ALTER TABLE public.staff_applications ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.staff_applications FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION ems_private.require_user(p_official BOOLEAN DEFAULT FALSE)
RETURNS BIGINT LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_id BIGINT; v_role TEXT;
BEGIN
    SELECT user_id,role INTO v_id,v_role FROM public.users WHERE auth_user_id=auth.uid() AND is_active;
    IF v_id IS NULL THEN RAISE EXCEPTION 'An approved, active Barangay-EMS account is required.' USING ERRCODE='42501'; END IF;
    IF p_official AND v_role<>'Barangay Official' THEN
        RAISE EXCEPTION 'Only Barangay Officials can perform this action.' USING ERRCODE='42501';
    END IF;
    RETURN v_id;
END $$;
CREATE OR REPLACE FUNCTION ems_private.is_member()
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT EXISTS(SELECT 1 FROM public.users WHERE auth_user_id=auth.uid() AND is_active)
$$;

-- Metadata supplies application details ONLY; role/status never come from the client.
-- Trigger runs in the Auth signup transaction, including when confirmation is enabled.
CREATE FUNCTION ems_private.capture_staff_application()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    IF NEW.raw_user_meta_data->>'ems_staff_application' = 'true' THEN
        INSERT INTO public.staff_applications(auth_user_id,username,full_name)
        VALUES(NEW.id,btrim(NEW.raw_user_meta_data->>'username'),btrim(NEW.raw_user_meta_data->>'full_name'));
    END IF;
    RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION ems_private.capture_staff_application() FROM PUBLIC, anon, authenticated;
CREATE TRIGGER ems_staff_signup AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION ems_private.capture_staff_application();

-- Include previously provisioned staff in the management screen without revoking them.
INSERT INTO public.staff_applications(auth_user_id,username,full_name,status,assigned_center_id)
SELECT auth_user_id,username,full_name,'Approved',assigned_center_id FROM public.users
WHERE role='Center Staff' AND auth_user_id IS NOT NULL;

CREATE FUNCTION public.ems_application_status()
RETURNS TEXT LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT status FROM public.staff_applications WHERE auth_user_id=auth.uid()
$$;

CREATE FUNCTION public.ems_staff_applications()
RETURNS JSONB LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    PERFORM ems_private.require_user(TRUE);
    RETURN coalesce((SELECT jsonb_agg(to_jsonb(q) ORDER BY q.submitted_at DESC) FROM (
        SELECT a.*,u.email,u.email_confirmed_at IS NOT NULL AS email_confirmed,c.name AS center_name
        FROM public.staff_applications a JOIN auth.users u ON u.id=a.auth_user_id
        LEFT JOIN public.evacuation_centers c ON c.center_id=a.assigned_center_id
    ) q),'[]'::jsonb);
END $$;

CREATE FUNCTION public.ems_review_staff(p_auth_id UUID,p_action TEXT,p_center_id BIGINT,p_note TEXT)
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
        IF NOT EXISTS(SELECT 1 FROM auth.users WHERE id=p_auth_id AND email_confirmed_at IS NOT NULL) THEN
            RAISE EXCEPTION 'The applicant must confirm their email before approval.';
        END IF;
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
REVOKE ALL ON FUNCTION public.ems_application_status(),public.ems_staff_applications(),public.ems_review_staff(UUID,TEXT,BIGINT,TEXT) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.ems_application_status(),public.ems_staff_applications(),public.ems_review_staff(UUID,TEXT,BIGINT,TEXT) TO authenticated;
NOTIFY pgrst,'reload schema';
COMMIT;
