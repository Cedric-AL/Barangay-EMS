package barangayems.backend;

import com.google.gson.annotations.SerializedName;

public class EvacuationCenter {
    @SerializedName("center_id") public long id;
    public String name, location, facilities;
    @SerializedName("max_capacity") public int capacity;
    @SerializedName("current_occupants") public int occupants;

    public int available() { return Math.max(0, capacity - occupants); }
    public String status() {
        return occupants >= capacity ? "Full" : occupants >= capacity * .9 ? "Near Capacity" : "Available";
    }
    @Override public String toString() { return name + " (#" + id + ")"; }
}
