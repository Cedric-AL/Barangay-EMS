-- Base Barangay-EMS schema. Run only on a new/empty project.
BEGIN;

CREATE TABLE public.users (
    user_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT users_role_check CHECK (role IN ('Barangay Official', 'Center Staff'))
);

CREATE TABLE public.evacuation_centers (
    center_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    max_capacity INT NOT NULL,
    current_occupants INT NOT NULL DEFAULT 0,
    facilities TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'Available',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT center_capacity_check CHECK (max_capacity > 0),
    CONSTRAINT center_occupants_check CHECK (current_occupants >= 0 AND current_occupants <= max_capacity),
    CONSTRAINT center_status_check CHECK (status IN ('Available', 'Near Capacity', 'Full'))
);

CREATE TABLE public.evacuees (
    evacuee_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    age INT NOT NULL,
    household VARCHAR(100),
    contact VARCHAR(30),
    center_id BIGINT,
    special_needs TEXT,
    vulnerability_category VARCHAR(50),
    registration_date TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT evacuee_age_check CHECK (age >= 0),
    CONSTRAINT evacuee_center_fk FOREIGN KEY (center_id) REFERENCES public.evacuation_centers(center_id) ON DELETE SET NULL
);

CREATE TABLE public.resources (
    resource_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    center_id BIGINT NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    used_quantity INT NOT NULL DEFAULT 0,
    minimum_stock INT NOT NULL DEFAULT 0,
    unit VARCHAR(30),
    last_updated TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT resource_quantity_check CHECK (quantity >= 0),
    CONSTRAINT resource_used_quantity_check CHECK (used_quantity >= 0 AND used_quantity <= quantity),
    CONSTRAINT resource_minimum_stock_check CHECK (minimum_stock >= 0),
    CONSTRAINT resource_center_fk FOREIGN KEY (center_id) REFERENCES public.evacuation_centers(center_id) ON DELETE CASCADE
);

CREATE TABLE public.resource_allocations (
    allocation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    allocation_date TIMESTAMPTZ DEFAULT NOW(),
    allocated_by BIGINT,
    CONSTRAINT allocation_quantity_check CHECK (quantity > 0),
    CONSTRAINT allocation_resource_fk FOREIGN KEY (resource_id) REFERENCES public.resources(resource_id) ON DELETE CASCADE,
    CONSTRAINT allocation_center_fk FOREIGN KEY (center_id) REFERENCES public.evacuation_centers(center_id) ON DELETE CASCADE,
    CONSTRAINT allocation_user_fk FOREIGN KEY (allocated_by) REFERENCES public.users(user_id) ON DELETE SET NULL
);

CREATE TABLE public.evacuee_transfers (
    transfer_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    evacuee_id BIGINT NOT NULL,
    from_center_id BIGINT,
    to_center_id BIGINT NOT NULL,
    transfer_date TIMESTAMPTZ DEFAULT NOW(),
    transferred_by BIGINT,
    reason TEXT,
    CONSTRAINT transfer_evacuee_fk FOREIGN KEY (evacuee_id) REFERENCES public.evacuees(evacuee_id) ON DELETE CASCADE,
    CONSTRAINT transfer_from_center_fk FOREIGN KEY (from_center_id) REFERENCES public.evacuation_centers(center_id) ON DELETE SET NULL,
    CONSTRAINT transfer_to_center_fk FOREIGN KEY (to_center_id) REFERENCES public.evacuation_centers(center_id) ON DELETE CASCADE,
    CONSTRAINT transfer_user_fk FOREIGN KEY (transferred_by) REFERENCES public.users(user_id) ON DELETE SET NULL,
    CONSTRAINT different_centers_check CHECK (from_center_id IS NULL OR from_center_id <> to_center_id)
);

CREATE INDEX idx_evacuees_center ON public.evacuees(center_id);
CREATE INDEX idx_evacuees_name ON public.evacuees(full_name);
CREATE INDEX idx_resources_center ON public.resources(center_id);
CREATE INDEX idx_resource_allocations_center ON public.resource_allocations(center_id);
CREATE INDEX idx_resource_allocations_resource ON public.resource_allocations(resource_id);
CREATE INDEX idx_transfers_evacuee ON public.evacuee_transfers(evacuee_id);
COMMIT;
