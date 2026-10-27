# Allocation planning

See REQUIREMENTS_PLANNING.md for installation and the current saved-rule workflow.
Apply migrations 09 and 10 before launching this version.

Officials configure item/unit supply rules under Resources > Supply Rules. On Allocation,
select a destination and source resource, then choose Load Saved Rule for Selected Resource.
Click Calculate and Use Suggested Quantity, review the result, and confirm distribution.
Temporary changes to rate, days or group do not overwrite the saved rule.

Required = ceil(registered people covered x daily rate x days).
Additional need = max(0, required - matching destination remaining stock).
Suggested transfer = min(additional need, source remaining stock).
Unmet need = additional need - suggested transfer.

For 20 people, 2 packs per person/day and 3 days, required stock is 120 packs.
With 10 packs at the destination and 90 at the source, transfer 90 and unmet need is 20.

Rates are entered by officials, not assumed standards. Rules apply per item/unit to
every active center and can cover all evacuees or one recorded vulnerability category.
Inventory names and units match ignoring case and outer spaces; no unit conversion is
performed. Avoid double counting shared stock across separate temporary group plans.
Estimates use the loaded snapshot; refresh when other staff have made changes.

Dashboard and Demand Shortages reports use saved rules and count missing inventory as
zero. Low Stock remains a separate reserve threshold. Distributions are validated by the
existing database RPC and recorded in allocation history.
