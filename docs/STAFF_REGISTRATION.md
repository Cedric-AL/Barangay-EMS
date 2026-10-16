# Enable staff registration and approval

**Current classroom setup:** apply `database/08_official_approval_only.sql` last and follow [Official approval only](OFFICIAL_APPROVAL_ONLY.md). It supersedes the email-confirmation requirements below; official approval and center assignment remain required.

## Your existing Supabase project

1. Keep the existing schema, integration, and linked official account. Do not rerun `01_schema.sql` or the account-linking example.
2. In Supabase SQL Editor, run the entire `database/04_staff_registration.sql` once as administrator. It runs in a transaction. If it reports an error, stop and share the error rather than running individual fragments.
3. In Authentication settings, allow new users to sign up and enable the Email provider. Email confirmation is recommended; configure a working confirmation redirect URL and mail delivery for your project. Do not disable confirmation just to bypass an error.
4. Restart the Java application from the folder containing `pom.xml`. The existing local Supabase configuration remains in use.

## Try the workflow

1. On the login screen choose **Register as Center Staff**, using a new email you control. Submit name, username, email, and a password of at least eight characters (your project's Auth policy may require more).
2. If confirmation is enabled, confirm the email using the message from Supabase. Signup does not open the operational dashboard.
3. Before approval, logging in as Center Staff must show a pending-approval message. The Auth account exists, but there is no operational `public.users` profile.
4. Log in with your existing Barangay Official account. Open **Staff Applications** and Refresh. Confirm the applicant's identity outside the app, select their row and an evacuation center, and Approve. Email confirmation is required by the approval function.
5. The applicant can now log in as Center Staff. Verify that they cannot manage centers or access the staff approval queue.
6. Test Reject on a second pending application; a reason is required. Test Deactivate on approved staff, including with a staff session already open: its next operational read/write must fail. A deactivated staff account can be reapproved with a selected center.

Existing staff remain active and appear as Approved. Existing assignments can initially be empty; select a center when reapproving. Official accounts cannot be created or promoted by signup or this screen. Duplicate usernames are detected during approval; an administrator must resolve collisions in the application record. Rejected requests cannot be approved from this screen.

## Access boundaries

Applicants cannot insert/update their application, approve themselves, assign roles, or read other applications through the Data API. A database trigger records signup metadata as an application only. Official-only functions perform reviews, fix the role to Center Staff, and record reviewer, time, and reason. Pending/rejected applicants have no operational profile. Active membership is checked by the existing read policies and write functions; deactivation therefore also blocks previously issued sessions from operational data.

Center assignment records staff placement. Approved staff retain the existing barangay-wide operational access model from `02_integration.sql`; this change does not introduce per-center record isolation. Center CRUD remains official-only. Apply all operational permissions through the database, not just UI controls.

## Verification

Compile all Java sources with Java 17+ and Gson. `tests/StaffRegistrationChecks.java` is a standalone local HTTP fixture test, not a live Supabase test. Compile with `--add-modules jdk.httpserver` and run with the same module enabled. `tests/staff_registration_checks.sql` verifies database permissions and lifecycle in a disposable PostgreSQL database; it is not a production setup script. The manual workflow above is still required against your Supabase project's Auth/email configuration.

References: [Supabase user data and signup triggers](https://supabase.com/docs/guides/auth/managing-user-data), [signup and email confirmation behavior](https://supabase.com/docs/reference/javascript/auth-signup).
