-- Apply after migration 08 using the Supabase SQL Editor.
BEGIN;
ALTER TABLE public.evacuation_centers ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE OR REPLACE FUNCTION public.ems_archive_center(p_id BIGINT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_count INT;
BEGIN
    PERFORM ems_private.require_user(TRUE);
    SELECT current_occupants INTO v_count FROM public.evacuation_centers WHERE center_id=p_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Center no longer exists.'; END IF;
    IF v_count > 0 OR EXISTS(SELECT 1 FROM public.evacuees WHERE center_id=p_id) THEN
        RAISE EXCEPTION 'Transfer all occupants before archiving this center.';
    END IF;
    UPDATE public.evacuation_centers SET archived=TRUE WHERE center_id=p_id;
END $$;
REVOKE ALL ON FUNCTION public.ems_archive_center(BIGINT) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.ems_archive_center(BIGINT) TO authenticated;

-- Serialize incoming assignments with archiving, including requests from older clients.
CREATE OR REPLACE FUNCTION public.ems_check_active_center()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE v_archived BOOLEAN;
BEGIN
    IF TG_OP='UPDATE' THEN
        IF NEW.center_id IS NOT DISTINCT FROM OLD.center_id THEN RETURN NEW; END IF;
    END IF;
    IF NEW.center_id IS NULL THEN RETURN NEW; END IF;
    SELECT archived INTO v_archived FROM public.evacuation_centers WHERE center_id=NEW.center_id FOR SHARE;
    IF v_archived THEN RAISE EXCEPTION 'This center is archived. Choose an active center.'; END IF;
    RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION public.ems_check_active_center() FROM PUBLIC,anon,authenticated;
DROP TRIGGER IF EXISTS ems_active_evacuee_center ON public.evacuees;
CREATE TRIGGER ems_active_evacuee_center BEFORE INSERT OR UPDATE OF center_id ON public.evacuees
FOR EACH ROW EXECUTE FUNCTION public.ems_check_active_center();
DROP TRIGGER IF EXISTS ems_active_resource_center ON public.resources;
CREATE TRIGGER ems_active_resource_center BEFORE INSERT OR UPDATE OF center_id ON public.resources
FOR EACH ROW EXECUTE FUNCTION public.ems_check_active_center();
DROP TRIGGER IF EXISTS ems_active_allocation_center ON public.resource_allocations;
CREATE TRIGGER ems_active_allocation_center BEFORE INSERT OR UPDATE OF center_id ON public.resource_allocations
FOR EACH ROW EXECUTE FUNCTION public.ems_check_active_center();
NOTIFY pgrst,'reload schema';
COMMIT;
