# Edit and delete evacuee records

Run all of `database/05_evacuee_edit_delete.sql` in Supabase SQL Editor after the existing migrations. It can be rerun. Restart the application after updating the Java files.

Select a row (including in a sorted or searched table), click **Edit Selected**, change the name or other details, and click **Save Changes**. **Cancel Edit / Clear** returns to registration. The selected record ID is retained while editing; changing table selection does not silently change the record being edited. Center assignment is locked during detail edits; use the existing transfer flow for relocation.

Active linked staff and officials can edit details. Only officials can use **Delete Selected**. Deletion asks for confirmation, reduces occupancy in the same transaction, and lets the existing trigger update center status. Records with transfer history cannot be permanently deleted. This feature does not implement departure/archive tracking.

## Verify against Supabase

1. Register a disposable evacuee and note the center occupancy.
2. Search or sort the list, select that record, edit its name and age, and save. Confirm the same record changed and occupancy did not change.
3. Cancel another edit; confirm registration mode returns without a database change.
4. As staff, confirm Delete is absent. A direct call to `ems_delete_evacuee` using a staff session must also be denied.
5. As an official, cancel a delete confirmation; confirm nothing changes. Then delete the disposable record and confirm occupancy decreases by one.
6. Confirm a record with transfer history cannot be deleted and occupancy remains unchanged after that rejection.
7. Refresh or restart to verify persistence. Try an invalid age and a duplicate identifying match; saving should be rejected.

Java compilation is checked locally. The live migration and permission/occupancy scenarios require Supabase verification; no live records are modified by local compilation.
