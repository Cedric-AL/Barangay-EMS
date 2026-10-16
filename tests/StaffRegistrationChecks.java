import barangayems.backend.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/** Local transport/flow checks; makes no requests to Supabase or real accounts. */
public final class StaffRegistrationChecks {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        AtomicReference<String> mode = new AtomicReference<>("Pending");
        AtomicReference<JsonObject> signup = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String response = "{}";
            if (path.equals("/auth/v1/signup")) {
                signup.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
            } else if (path.equals("/auth/v1/token")) {
                response = "{\"access_token\":\"test\",\"refresh_token\":\"test\",\"expires_in\":3600,\"user\":{\"id\":\"11111111-1111-1111-1111-111111111111\"}}";
            } else if (path.equals("/rest/v1/users")) {
                response = mode.get().equals("Pending") || mode.get().equals("Rejected") ? "[]" :
                    "[{\"user_id\":1,\"role\":\"Center Staff\",\"full_name\":\"Test Staff\",\"is_active\":" + !mode.get().equals("Deactivated") + "}]";
            } else if (path.equals("/rest/v1/rpc/ems_application_status")) {
                response = "\"" + mode.get() + "\"";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            SupabaseConfig config = new SupabaseConfig("http://127.0.0.1:" + server.getAddress().getPort(), "sb_publishable_test");
            char[] password = "test-password".toCharArray();
            new SupabaseClient(config).registerStaff("staff@example.test", password, "staff", "Test Staff");
            for (char c : password) check(c == '\0', "Signup password must be cleared");
            JsonObject metadata = signup.get().getAsJsonObject("data");
            check(metadata.get("ems_staff_application").getAsBoolean(), "Signup must identify a staff application");
            check(!metadata.has("role") && !metadata.has("status"), "Client must not request privilege or approval");
            for (String state : new String[]{"Pending", "Rejected", "Deactivated"}) {
                mode.set(state);
                DataManager data = new DataManager(config);
                try {
                    data.login("staff@example.test", "test-password".toCharArray(), "Center Staff");
                    throw new AssertionError(state + " account was admitted");
                } catch (IllegalArgumentException expected) {
                    check(expected.getMessage().toLowerCase().contains(state.toLowerCase()), "Missing status feedback for " + state);
                    check(data.user() == null, "Denied login must clear local profile");
                }
            }
            mode.set("Approved");
            DataManager approved = new DataManager(config);
            check(!approved.login("staff@example.test", "test-password".toCharArray(), "Center Staff").isOfficial(), "Approved staff must remain staff");
            try {
                approved.login("staff@example.test", "test-password".toCharArray(), "Barangay Official");
                throw new AssertionError("Staff selected official role");
            } catch (IllegalArgumentException expected) { check(approved.user() == null, "Role mismatch must clear profile"); }
            System.out.println("PASS: signup payload, password clearing, pending/rejected/deactivated denial, approved staff login, role mismatch.");
        } finally { server.stop(0); }
    }
}
