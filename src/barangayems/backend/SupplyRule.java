package barangayems.backend;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public final class SupplyRule {
    @SerializedName("resource_type") public String name;
    public String unit;
    @SerializedName("target_group") public String group;
    @SerializedName("daily_rate") public BigDecimal dailyRate;
    @SerializedName("coverage_days") public int days;
    public boolean enabled;
    public boolean matches(String item, String units) {
        return name != null && unit != null && item != null && units != null
                && name.trim().equalsIgnoreCase(item.trim()) && unit.trim().equalsIgnoreCase(units.trim());
    }
}
