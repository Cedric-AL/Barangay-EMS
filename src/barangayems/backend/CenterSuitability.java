package barangayems.backend;

import java.util.*;

/** Recorded facilities support human assessment; they do not certify safety. */
public final class CenterSuitability {
    public static final String[] FEATURES = {"Wheelchair access", "Accessible toilets", "First-aid area", "Child-friendly space", "Private care area"};
    private CenterSuitability() {}
    public record Assessment(String status, String details, boolean warning) {}
    public static Assessment assess(EvacuationCenter center, String[] requirements) {
        if (center == null) return new Assessment("Choose a center", "No center selected.", true);
        String summary = "Available spaces: " + center.available() + " / " + center.capacity
                + "\nRecorded facilities: " + (center.facilityFeatures == null ? "Not assessed" : String.join(", ", center.facilityFeatures))
                + "\nFacility notes: " + Objects.toString(center.facilities, "None");
        if (center.archived || center.available() == 0)
            return new Assessment(center.archived ? "Archived center" : "Center full", summary, true);
        if (!center.facilityProfileComplete)
            return new Assessment("Facility information incomplete", summary, true);
        Set<String> available = new HashSet<>(Arrays.asList(center.facilityFeatures == null ? new String[0] : center.facilityFeatures));
        List<String> missing = new ArrayList<>();
        for (String need : requirements == null ? new String[0] : requirements) if (!available.contains(need)) missing.add(need);
        if (!missing.isEmpty()) return new Assessment("Some requirements missing", summary + "\nMissing: " + String.join(", ",missing), true);
        return new Assessment(requirements == null || requirements.length == 0 ? "No facility requirements selected" : "Requirements matched", summary, false);
    }
}
