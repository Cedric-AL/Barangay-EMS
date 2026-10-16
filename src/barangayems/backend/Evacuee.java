package barangayems.backend;

import com.google.gson.annotations.SerializedName;

public class Evacuee {
    @SerializedName("evacuee_id") public long id;
    @SerializedName("center_id") public Long centerId;
    @SerializedName("full_name") public String name;
    public int age;
    public String household, contact;
    @SerializedName("special_needs") public String needs;
    @SerializedName("vulnerability_category") public String category;
    public transient String center;
}
