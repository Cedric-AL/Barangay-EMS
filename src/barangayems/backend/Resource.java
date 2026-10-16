package barangayems.backend;

import com.google.gson.annotations.SerializedName;

public class Resource {
    @SerializedName("resource_id") public long id;
    @SerializedName("center_id") public long centerId;
    @SerializedName("resource_type") public String name;
    public String unit;
    @SerializedName("quantity") public int available;
    @SerializedName("used_quantity") public int used;
    @SerializedName("minimum_stock") public int required;
    public transient String center;

    public int remaining() { return available - used; }
    public int shortage() { return Math.max(0, required - remaining()); }
    @Override public String toString() { return center + " / " + name + " (#" + id + ")"; }
}
