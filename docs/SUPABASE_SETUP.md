# Supabase setup for Barangay-EMS

For the current classroom workflow without confirmation emails, apply `database/08_official_approval_only.sql` after migration 04 and follow [Official approval only](OFFICIAL_APPROVAL_ONLY.md). Disable Confirm Email in Supabase Authentication settings.

This integrated build uses Supabase Auth for passwords and the Supabase Data API/RPC layer for application data. It does **not** connect with a database password or JDBC.

## 1. Check the six base tables

In Supabase SQL Editor, run:

```sql
select table_name
from information_schema.tables
where table_schema='public'
  and table_name in ('users','evacuation_centers','evacuees','resources','resource_allocations','evacuee_transfers')
order by table_name;
```

If all six exist, **do not rerun `database/01_schema.sql`**. If the project is empty, run `01_schema.sql` once.

## 2. Apply the missing integration layer

Run the entire `database/02_integration.sql` in SQL Editor as the project administrator.

It adds the Auth UUID link, RLS read policies, guarded write RPCs, automatic center status updates, duplicate/resource protections, transactional transfers/allocation, and the `ems_snapshot()` RPC used by Java.

The migration intentionally stops if stored center occupant counters disagree with the actual evacuee rows or if duplicate resource identities already exist. Fix those data conflicts first instead of allowing the migration to guess which data is correct.

## 3. Create and link a login

1. In **Authentication > Users**, create an email/password user and confirm the email if your project requires confirmation.
2. Open `database/03_link_user.sql.example`.
3. Replace `YOUR_LOGIN_EMAIL`, `YOUR_USERNAME`, and `YOUR_FULL_NAME`.
4. Pick either `Barangay Official` or `Center Staff`.
5. Run the edited SQL in SQL Editor.

Passwords stay in Supabase Auth. Never add a password column to `public.users`.

## 4. Add the local public API key

Copy:

`config/supabase.properties.example` → `config/supabase.properties`

Then replace the placeholder with the **publishable key** (or legacy **anon** key):

```properties
supabase.url=https://zxqeienskrnthxmcreeo.supabase.co
supabase.key=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Do not use a `service_role` or `sb_secret_...` key in the desktop application. The real properties file is ignored by Git.

## 5. Run

Before running this version, apply `database/04_staff_registration.sql` once after the integration migration. See [Staff registration setup](STAFF_REGISTRATION.md) for signup settings and the approval workflow. Existing linked accounts remain active.

Use Java 17+.

- VS Code: open the folder containing `pom.xml`, install Extension Pack for Java, then run `src/barangayems/Main.java`.
- Maven: `mvn compile exec:java`
- Windows helper: `run.cmd`
- macOS/Linux helper: `sh run.sh`

## 6. Verify end-to-end behavior

1. Log in with the linked Supabase account and matching role.
2. As a Barangay Official, add two centers.
3. Register a resident at the first center and verify occupancy changes.
4. Register matching identifying details against the second center and test the transfer flow.
5. Add a resource to the first center, allocate part to the second center, and verify both inventories plus allocation history.
6. Try allocating more stock than remains and transferring into a full center; both must be rejected.
7. Log in as Center Staff and verify center management is blocked while registration/resource operations still work.
8. Restart the app and confirm records persist.
