package barangayems.backend;

import com.google.gson.annotations.SerializedName;

public final class StaffApplication {
    @SerializedName("auth_user_id") public String authId;
    @SerializedName("full_name") public String fullName;
    @SerializedName("center_name") public String centerName;
    @SerializedName("email_confirmed") public boolean emailConfirmed;
    @SerializedName("review_note") public String reviewNote;
    public String username, email, status;
}
