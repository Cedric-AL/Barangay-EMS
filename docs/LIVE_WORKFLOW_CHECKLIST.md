# Live verification - test database only

Use an isolated Supabase test project with existing migrations through 10 applied.
Keep production data untouched. Record tester, date, expected result, actual result,
and screenshots for the paper. These checks have not been run against live Supabase.

1. As official, save a center with wheelchair access and assessed facilities. Refresh
   or restart; the checklist must persist. Staff must not be able to change it.
2. Register someone requiring wheelchair access: matching center shows matched;
   assessed center without the feature shows missing; unassessed center shows unknown.
   Cancel the warning: no evacuee should be saved. Confirm: record should persist.
3. Edit the person's required facilities, refresh, and inspect the checklist. Use
   Transfer Selected: the same saved needs must appear in the comparison. Cancel
   leaves occupancy unchanged; confirm updates both centers and transfer history.
4. Fill a small center to capacity. Further registration or transfer must fail, even
   if facility requirements match. Archive an empty center with history: retain its
   records and exclude it from destination choices. Older-client incoming RPCs must fail.
5. As official, save a rule: 2 packs/person/day, 3 days, all evacuees. Refresh/restart
   and confirm persistence. Staff must be unable to save rules through UI or RPC.
6. For 20 people and 10 remaining packs, expected demand is 120 and shortage 110.
   Check Dashboard, Resources demand dialog, and exported Demand Shortages report.
   No inventory row must still show a shortage of 120. An item with different units
   must not reduce this shortage. A disabled rule must disappear from assessments.
7. Load the saved rule on Allocation with a different source center. With 90 source
   packs, expected suggestion is 90 and unmet need 20. Complete transfer and refresh:
   destination stock should be 100, source stock 0, demand shortage 20. Review history.
8. Change the temporary allocation rate: saved rule and dashboard/report results must
   remain unchanged. Edit the saved rule as official: all views must use the new rule
   after refresh. Verify target group counts and fractional round-up.
9. Two logged-in users simultaneously request 70 packs each from the same source
   holding 100. At most one may succeed; source remaining must never be negative.
   Refresh both clients. Verify combined stock conservation and one successful audit.
10. Simultaneously archive an empty destination and submit an incoming registration
    or allocation. One operation must fail if they conflict; no archived center may
    gain occupants or incoming allocations. Resolve any deadlock by refreshing before
    retrying; verify neither operation was partially committed.

Do not mark these tests passed without observed results.
