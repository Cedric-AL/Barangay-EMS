# Archive centers

Apply database/09_archive_centers.sql in Supabase SQL Editor after existing migrations,
then restart with run.cmd. The app alone cannot install this database function.

Officials can select an empty center and click Archive. Centers remain visible with
Archived status in the center list and history retains their names and records.
Archived centers are excluded from destination choices, active capacity, and dashboard
center totals. Their remaining inventory is preserved and can be distributed out to
an active center. Incoming evacuees and allocations are rejected by database triggers.
Existing staff assignments are retained; this feature does not deactivate staff accounts.
Archive is distinct from permanent Delete, which may still reject linked records.
No automatic restoration workflow is included.
