package barangayems.backend;

import com.google.gson.annotations.SerializedName;

public abstract class User {
    @SerializedName("user_id") public long id;
    @SerializedName("auth_user_id") public String authId;
    public String username, role;
    @SerializedName("full_name") public String fullName;
    public abstract boolean isOfficial();
}
