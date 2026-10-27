package barangayems.backend;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/** Consistent operational snapshot returned by the ems_snapshot RPC. */
public class DataSnapshot {
    public List<SupplyRule> supplyRules = new ArrayList<>();
    public List<EvacuationCenter> centers = new ArrayList<>();
    public List<Evacuee> people = new ArrayList<>();
    public List<Resource> supplies = new ArrayList<>();
    public List<AllocationRecord> allocations = new ArrayList<>();
    public List<TransferRecord> transfers = new ArrayList<>();

    public String centerName(Long id) {
        if (id != null) {
            for (EvacuationCenter center : centers) {
                if (center.id == id.longValue()) return center.name;
            }
        }
        return "Unassigned / unavailable";
    }

    public Resource resource(long id) {
        for (Resource resource : supplies) if (resource.id == id) return resource;
        return null;
    }

    public void resolveLabels() {
        for (Evacuee person : people) person.center = centerName(person.centerId);
        for (Resource resource : supplies) {
            resource.center = centerName(resource.centerId);
            if (resource.unit == null) resource.unit = "";
        }
    }

    public java.util.List<EvacuationCenter> activeCenters() {
        return centers.stream().filter(c -> !c.archived).toList();
    }

    public int occupants() {
        return centers.stream().mapToInt(center -> center.occupants).sum();
    }

    public int capacity() {
        return activeCenters().stream().mapToInt(center -> center.capacity).sum();
    }

    public static class AllocationRecord {
        @SerializedName("allocation_id") public long id;
        @SerializedName("resource_id") public long resourceId;
        @SerializedName("center_id") public long centerId;
        public int quantity;
        @SerializedName("allocation_date") public String date;
    }

    public static class TransferRecord {
        @SerializedName("evacuee_id") public long personId;
        @SerializedName("from_center_id") public Long fromId;
        @SerializedName("to_center_id") public long toId;
        @SerializedName("transfer_date") public String date;
        public String reason;
    }
}
