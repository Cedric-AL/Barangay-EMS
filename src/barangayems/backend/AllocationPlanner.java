package barangayems.backend;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Advisory demand calculation; inventory transfers remain guarded by the database. */
public final class AllocationPlanner {
    public static final String ALL = "All evacuees";
    private AllocationPlanner() {}

    public record Plan(int people, long required, long onHand, long deficit, int suggested, long unmet) {}

    public static Plan calculate(DataSnapshot snapshot, EvacuationCenter destination, Resource source,
                                 String group, BigDecimal dailyRate, int days) {
        if (destination == null || source == null)
            throw new IllegalArgumentException("Select a destination and source resource.");
        if (destination.archived) throw new IllegalArgumentException("Choose an active destination center.");
        if (destination.id == source.centerId)
            throw new IllegalArgumentException("Choose a destination different from the source center.");
        if (group == null || dailyRate == null || dailyRate.signum() <= 0 || days < 1)
            throw new IllegalArgumentException("Enter a positive daily rate and number of days.");
        int people = 0;
        for (Evacuee person : snapshot.people)
            if (person.centerId != null && person.centerId == destination.id
                    && (ALL.equals(group) || group.equals(person.category))) people++;
        long required;
        try {
            required = dailyRate.multiply(BigDecimal.valueOf(people)).multiply(BigDecimal.valueOf(days))
                    .setScale(0, RoundingMode.CEILING).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Calculated requirement is too large. Reduce the rate or days.");
        }
        long stock = 0;
        for (Resource item : snapshot.supplies)
            if (item.centerId == destination.id
                    && same(item.name, source.name) && same(item.unit, source.unit))
                stock += Math.max(0, item.remaining());
        long onHand = stock;
        long deficit = Math.max(0L, required - onHand);
        int suggested = (int)Math.min(deficit, Math.max(0, source.remaining()));
        return new Plan(people, required, onHand, deficit, suggested, deficit - suggested);
    }

    private static boolean same(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
