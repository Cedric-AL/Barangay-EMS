# Integration review — issues found and fixes applied

## Blocking issues fixed

1. **Frontend still depended on `DemoStore`.** The newer visual frontend was still session-only and modified lists directly. It now uses `DataSnapshot` plus `DataManager` for all live data.
2. **Login design and backend login were separate.** The login screen now calls Supabase Auth and verifies the stored `users.role` before opening the dashboard.
3. **Missing database integration migration.** The Java backend referenced RPCs that were not in the supplied `01_schema.sql`. A new `database/02_integration.sql` now defines the required RPCs, RLS rules, role checks, Auth mapping, and transaction logic.
4. **Center CRUD permission mismatch.** Center Staff could see the same visual controls in the old frontend even though the intended backend permission model denied center management. The integrated UI disables those controls for staff and the RPC layer independently enforces the rule.
5. **Allocation semantics differed between versions.** The old frontend only incremented a local `used` value. The integrated allocation screen now moves stock transactionally from a source center to a different destination, credits matching destination inventory, and creates an audit record.
6. **Missing `DataSnapshot` source in the upload set.** The compiled class existed, but the usable Java source was not available in the sent source batch. It has been reconstructed to match the compiled fields, Gson mappings, label resolution, and totals.
7. **Build scripts assumed a bundled Gson JAR.** The uploaded files did not include the actual JAR. The new launch scripts use `lib/gson.jar` if present and otherwise fall back to Maven.

## Remaining design limitations

- This is a single-barangay model. Center Staff are not assigned to one specific center.
- Reports are text exports, not PDF/Excel reports.
- There is no resident discharge workflow yet, so occupancy is changed through registration/transfer only.
- There is no real-time subscription; use **Refresh database** to load teammates' latest changes.
- Deleting centers/resources can affect history because the original schema uses cascading foreign keys. For a production/audit-heavy system, use soft deletion or immutable historical labels instead.
