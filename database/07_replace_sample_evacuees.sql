-- MANUAL DATA REPLACEMENT: removes ALL current evacuees, then inserts 24
-- fictional residents with realistic names. Centers, resources and accounts remain.
-- Export public.evacuees first if you need to retain the current records.
-- Refuses to erase any existing transfer history. All changes are transactional.
BEGIN;
LOCK TABLE public.evacuees,public.evacuation_centers,public.evacuee_transfers IN SHARE ROW EXCLUSIVE MODE;
DO $$
DECLARE v_center BIGINT; v_name TEXT; v_index BIGINT;
BEGIN
    IF EXISTS(SELECT 1 FROM public.evacuee_transfers) THEN
        RAISE EXCEPTION 'Existing transfer history must be retained. No records replaced; contact the developer to plan archival.';
    END IF;
    IF (SELECT coalesce(sum(max_capacity),0) FROM public.evacuation_centers)<24 THEN
        RAISE EXCEPTION 'At least 24 total center places are required. No records replaced.';
    END IF;
    DELETE FROM public.evacuees;
    UPDATE public.evacuation_centers SET current_occupants=0;
    FOR v_name,v_index IN
        SELECT person,ordinality FROM unnest(ARRAY[
            'Maria Santos','Jose Santos','Ana Reyes','Miguel Reyes',
            'Elena Cruz','Rafael Cruz','Sofia Garcia','Daniel Garcia',
            'Isabel Mendoza','Gabriel Mendoza','Teresa Ramos','Antonio Ramos',
            'Lucia Flores','Carlos Flores','Patricia Aquino','Luis Aquino',
            'Carmen Navarro','Roberto Navarro','Angela Torres','Paolo Torres',
            'Rosa Villanueva','Manuel Villanueva','Andrea Castillo','Marco Castillo'
        ]) WITH ORDINALITY AS names(person,ordinality)
    LOOP
        SELECT center_id INTO v_center FROM public.evacuation_centers
        WHERE current_occupants<max_capacity
        ORDER BY current_occupants::NUMERIC/max_capacity,center_id LIMIT 1;
        INSERT INTO public.evacuees(full_name,age,household,contact,center_id,special_needs,vulnerability_category)
        VALUES(v_name,
            CASE v_index%6 WHEN 0 THEN 68 WHEN 1 THEN 35 WHEN 2 THEN 12 WHEN 3 THEN 42 WHEN 4 THEN 9 ELSE 27 END,
            'Household '||lpad(((v_index+1)/2)::TEXT,2,'0'),NULL,v_center,
            CASE WHEN v_index%6=0 THEN 'Mobility assistance' ELSE 'None reported' END,
            CASE WHEN v_index%6=0 THEN 'Senior Citizen' WHEN v_index%6 IN (2,4) THEN 'Child' ELSE 'None' END);
        UPDATE public.evacuation_centers SET current_occupants=current_occupants+1 WHERE center_id=v_center;
    END LOOP;
END $$;
COMMIT;
SELECT full_name,age,household,center_id FROM public.evacuees ORDER BY evacuee_id;
