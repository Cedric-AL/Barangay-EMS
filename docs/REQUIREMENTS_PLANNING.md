# Center suitability and saved supply planning

## Install

Apply database/09_archive_centers.sql if it has not been applied, then apply
database/10_requirements_planning.sql in Supabase SQL Editor. Migration 10 requires
the existing integration, staff registration, and evacuee edit migrations. Both
migrations are transactional. Restart with run.cmd. The new app explicitly reports
that migration 10 is needed if the old snapshot format is returned.

No existing center is assumed assessed: its facility checklist starts incomplete.
Existing facility notes and evacuee special-needs notes remain intact.

## Center suitability

Officials use Evacuation Centers to select recorded facilities and mark the checklist
assessed. An assessed unchecked item means the facility is not recorded as available.
Staff select each person's actual required facilities in Registration; these are saved
with the evacuee and can be edited. Categories alone never imply facility needs.

Registration and Transfer Selected show spaces, facility notes, and the result:
requirements matched, missing requirements, incomplete facility information, or no
facility requirements selected. Missing/unknown results require human confirmation;
full and archived destinations are blocked. The duplicate-match transfer workflow
also checks the existing person's saved requirements. Free-text needs still require
human review. This is an advisory comparison, not certification of safety or suitability.

## Saved supply rules

Resources > Supply Rules lets officials save a positive units-per-person/day rate,
coverage days (1-365), target group, and enabled status. Staff may view rules. Rates
must use the inventory item's units. No default supply standard is asserted.

One rule per item/unit applies to every active center. Saving the same item and unit
updates it; uncheck Enabled to disable it. Item/unit matching ignores outer whitespace
and case. Changing the spelling creates a different rule. Different groups for the same
item are intentionally not added together because they could count shared inventory twice.

Allocation > Load Saved Rule fills the planning inputs. Users may adjust them for a
temporary scenario without changing the saved rule. Dashboard, Resources > Demand
Shortages, and Reports > Demand Shortages use the saved rules, not temporary scenarios.

Demand = ceil(registered people in target group x daily rate x coverage days).
Demand shortage = max(0, demand - matching destination remaining stock).
Missing inventory rows count as zero stock. Rules with no matching stock are still
assessed for every active center. Low Stock reports remain separate reserve-threshold
alerts. No enabled rules means demand is not assessed, not that demand is satisfied.

Estimates cover the next configured number of days from the loaded snapshot. Refresh
when other staff make changes. They are stock targets, not forecasts, reservations,
household receipt tracking, or automatically approved transfers. The existing guarded
allocation RPC validates source stock and records transfers. Archive migration 09
blocks incoming evacuees and allocations to archived centers.

## Verification

Local Java checks cover suitability outcomes, saved-rule serialization, category
counts, round-up, missing inventory, disabled rules, archived centers, and agreement
between allocation and demand reports. Existing allocation and login checks also run.
Use tests/requirements_planning_checks.sql only on a disposable database after all
required migrations. Test fixtures roll back. See LIVE_WORKFLOW_CHECKLIST.md for
manual workflows and concurrent allocation checks. Local checks do not establish
that migrations have been applied or that live Supabase workflows passed.
