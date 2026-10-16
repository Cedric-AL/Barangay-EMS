package barangayems.backend;

import com.google.gson.*;
import java.util.Objects;

/** Application operations. All mutations use guarded database functions, never direct table writes. */
public final class DataManager {
    private final SupabaseClient client;
    private final Gson json = new Gson();
    private volatile User user;
    public DataManager(SupabaseConfig config) { client = new SupabaseClient(config); }
    public User user() { return user; }
    public User login(String email, char[] password, String expectedRole) throws Exception {
        try {
            String authId = client.signIn(email, password);
            java.util.UUID.fromString(authId);
            JsonArray rows = client.get("users?auth_user_id=eq." + authId + "&select=user_id,auth_user_id,username,full_name,role,is_active").getAsJsonArray();
            if (rows.size() != 1) {
                JsonElement status = client.rpc("ems_application_status", new JsonObject());
                String message = status.isJsonNull() ? "This login has no linked Barangay-EMS profile. Contact your official."
                    : switch (status.getAsString()) {
                        case "Pending" -> "Your staff application is pending Barangay Official approval.";
                        case "Rejected" -> "Your staff application was rejected. Contact your Barangay Official.";
                        case "Deactivated" -> "Your staff access is deactivated. Contact your Barangay Official.";
                        default -> "Your profile is unavailable. Contact your Barangay Official.";
                    };
                throw new IllegalArgumentException(message);
            }
            if (!rows.get(0).getAsJsonObject().get("is_active").getAsBoolean())
                throw new IllegalArgumentException("Your staff access is deactivated. Contact your Barangay Official.");
            String storedRole = rows.get(0).getAsJsonObject().get("role").getAsString();
            Class<? extends User> profileType = switch (storedRole) {
                case "Barangay Official" -> BarangayOfficial.class;
                case "Center Staff" -> CenterStaff.class;
                default -> throw new IllegalArgumentException("Unsupported account role.");
            };
            User candidate = json.fromJson(rows.get(0), profileType);
            if (!Objects.equals(candidate.role, expectedRole)) throw new IllegalArgumentException("The selected role does not match your registered account role.");
            user = candidate; return candidate;
        } catch (Exception e) { user = null; client.clear(); throw e; }
    }
    public void logout() throws Exception { try { client.signOut(); } finally { user = null; } }
    public void clear() { client.clear(); user = null; }
    public StaffApplication[] staffApplications() throws Exception {
        return json.fromJson(client.rpc("ems_staff_applications", new JsonObject()), StaffApplication[].class);
    }
    public void reviewStaff(String authId, String action, Long centerId, String note) throws Exception {
        client.rpc("ems_review_staff", object("p_auth_id", authId, "p_action", action, "p_center_id", centerId, "p_note", note));
    }
    public DataSnapshot load() throws Exception {
        DataSnapshot s = json.fromJson(client.rpc("ems_snapshot", new JsonObject()), DataSnapshot.class);
        if (s == null || s.centers == null || s.people == null || s.supplies == null || s.allocations == null || s.transfers == null)
            throw new IllegalStateException("Incomplete database snapshot. Apply database/02_integration.sql first.");
        s.resolveLabels(); return s;
    }
    private JsonObject object(Object... fields) {
        JsonObject result = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) result.add((String) fields[i], json.toJsonTree(fields[i+1]));
        return result;
    }
    public void saveCenter(Long id, String name, String location, int capacity, String facilities) throws Exception {
        client.rpc("ems_save_center", object("p_id", id, "p_name", name, "p_location", location, "p_capacity", capacity, "p_facilities", facilities));
    }
    public void deleteCenter(long id) throws Exception { client.rpc("ems_delete_center", object("p_id", id)); }
    public void register(String name, int age, String household, String contact, long centerId, String needs, String category, boolean distinct) throws Exception {
        client.rpc("ems_register_evacuee", object("p_name", name, "p_age", age, "p_household", household, "p_contact", contact,
                "p_center_id", centerId, "p_needs", needs, "p_category", category, "p_allow_duplicate", distinct));
    }
    public void transfer(long id, long toId, String reason) throws Exception {
        client.rpc("ems_transfer_evacuee", object("p_id", id, "p_to_center_id", toId, "p_reason", reason));
    }
    public void updateEvacuee(long id, String name, int age, String household, String contact, String needs, String category) throws Exception {
        client.rpc("ems_update_evacuee", object("p_id", id, "p_name", name, "p_age", age, "p_household", household,
                "p_contact", contact, "p_needs", needs, "p_category", category));
    }
    public void deleteEvacuee(long id) throws Exception {
        client.rpc("ems_delete_evacuee", object("p_id", id));
    }
    public void deleteEvacuees(long[] ids) throws Exception {
        client.rpc("ems_delete_evacuees", object("p_ids", ids));
    }
    public void saveResource(Long id, long centerId, String name, String unit, int quantity, int used, int minimum) throws Exception {
        client.rpc("ems_save_resource", object("p_id", id, "p_center_id", centerId, "p_name", name, "p_unit", unit,
                "p_quantity", quantity, "p_used", used, "p_minimum", minimum));
    }
    public void allocate(long resourceId, long centerId, int quantity) throws Exception {
        client.rpc("ems_allocate_resource", object("p_resource_id", resourceId, "p_to_center_id", centerId, "p_quantity", quantity));
    }
}
