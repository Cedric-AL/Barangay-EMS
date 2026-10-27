package barangayems.backend;

import java.util.*;

/** One demand estimate per active center and enabled item/unit rule, including missing inventory. */
public final class SupplyDemand {
    private SupplyDemand() {}
    public record Row(EvacuationCenter center, SupplyRule rule, AllocationPlanner.Plan plan) {}
    public static List<Row> rows(DataSnapshot snapshot) {
        List<Row> rows = new ArrayList<>();
        for (EvacuationCenter center : snapshot.activeCenters())
            for (SupplyRule rule : snapshot.supplyRules) if (rule.enabled) {
                Resource item = new Resource(); item.centerId = Long.MIN_VALUE;
                item.name = rule.name; item.unit = rule.unit;
                rows.add(new Row(center, rule, AllocationPlanner.calculate(snapshot,center,item,rule.group,rule.dailyRate,rule.days)));
            }
        return rows;
    }
    public static String report(DataSnapshot snapshot) {
        StringBuilder result = new StringBuilder("DEMAND SHORTAGES (saved planning rules)\n");
        if (snapshot.supplyRules.stream().noneMatch(r -> r.enabled))
            return result.append("No enabled rules. Officials can configure Supply Rules in Resources.\n").toString();
        for (Row row : rows(snapshot)) result.append(row.center.name).append(" / ").append(row.rule.name)
                .append(" | Group: ").append(row.rule.group).append(" | People: ").append(row.plan.people())
                .append("\n  Rate: ").append(row.rule.dailyRate).append(" ").append(row.rule.unit)
                .append(" per person/day | Days: ").append(row.rule.days)
                .append("\n  Required: ").append(row.plan.required()).append(" | Remaining: ").append(row.plan.onHand())
                .append(" | Demand shortage: ").append(row.plan.deficit()).append("\n\n");
        return result.toString();
    }
}
