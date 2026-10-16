# Barangay-EMS — Integrated Swing + Supabase Build

This folder combines the **updated Swing frontend design** with the **Supabase-connected backend** for the Barangay Evacuation Management and Resource Allocation System.

## What was combined

- The newer navy/blue dashboard, sidebar, header, cards, tables, navigation icons, and responsive Swing layout were kept.
- `DemoStore` was removed from the running application.
- Login now uses Supabase Auth email/password.
- Every screen reads a `DataSnapshot` from Supabase.
- Center, evacuee, resource, transfer, and allocation changes use guarded Supabase RPC functions.
- Database calls run through `SwingWorker` so the Swing UI does not freeze during network operations.
- Center Staff can view centers but center CRUD is limited to Barangay Officials in both the UI and database layer.

## Project structure

```text
src/barangayems/frontend/   Swing UI
src/barangayems/backend/    Supabase/Auth/data classes
database/01_schema.sql      Base six-table schema (new DB only)
database/02_integration.sql Required Auth/RLS/RPC integration
database/03_link_user.sql.example
docs/SUPABASE_SETUP.md
config/supabase.properties.example
tests/database_checks.sql
```

## Before running

Staff can now register from the login screen and wait for official approval. Apply `database/04_staff_registration.sql` once to an already integrated database before using this version. See `docs/STAFF_REGISTRATION.md` for setup and validation.

The most important missing piece in the files you originally sent was `database/02_integration.sql`. The Java backend calls RPC names such as `ems_snapshot`, `ems_save_center`, `ems_register_evacuee`, and `ems_allocate_resource`; those functions are not present in `01_schema.sql`. This integrated version supplies that migration.

Follow `docs/SUPABASE_SETUP.md` in order.

## Run

Requires Java 17+ and Gson 2.10.1. Maven will download Gson automatically when internet access is available:

```sh
mvn compile exec:java
```

Or use `run.cmd` / `run.sh`. The helper scripts also support a manually supplied `lib/gson.jar`.

## Security notes

- Never commit `config/supabase.properties`.
- Use only the Supabase publishable/anon public key in the desktop client.
- Never place a service-role/secret key in Java code or local files that will be shared.
- Password verification remains in Supabase Auth; passwords are not stored in `public.users`.
