# Classroom registration without email confirmation

1. In Supabase Authentication, find the Email provider settings and turn **Confirm Email** off. Leave email/password sign-in and new signups enabled. This setting cannot be changed by the application migration SQL.
2. In SQL Editor, run all of `database/08_official_approval_only.sql` after the existing staff migration. It replaces the approval function without changing its official-only permissions or center-assignment requirement.
3. The migration also marks email as confirmed for existing Pending/Approved staff applications that are currently unconfirmed. This is an explicit administrative waiver for testing, not evidence that the person owns the address. Passwords and application approval statuses remain unchanged. Other Auth accounts are not updated.
4. Restart the app, log in as your existing official, select a pending application and center, and Approve. The staff account can then sign in with its existing password.

New applicants still need official approval to access operational data. Pending/rejected and deactivated accounts remain blocked. The email-confirmed column is removed from the app; no emails are required in this classroom flow.

Verification: register a new staff account, try login before approval (must be denied), approve as official without email verification, then log in as staff (must succeed). Repeat with the previously pending account. Confirm rejection, deactivation and staff denial from official review functions still work.

Apply migration 08 last when provisioning a fresh database. Earlier migration 04 and its legacy test file describe the original confirmation-required workflow; 08 intentionally changes that behavior.
