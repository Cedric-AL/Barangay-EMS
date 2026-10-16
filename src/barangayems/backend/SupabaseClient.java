package barangayems.backend;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/** Auth and PostgREST transport. Tokens remain in memory; writes are never automatically retried. */
public final class SupabaseClient {
    private static final Gson JSON = new GsonBuilder().serializeNulls().create();
    private final SupabaseConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private String accessToken, refreshToken;
    private Instant expiresAt = Instant.EPOCH;
    public SupabaseClient(SupabaseConfig config) { this.config = config; }

    public synchronized void registerStaff(String email, char[] password, String username, String fullName) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("email", email.trim());
        body.addProperty("password", new String(password));
        JsonObject metadata = new JsonObject();
        metadata.addProperty("ems_staff_application", true);
        metadata.addProperty("username", username.trim());
        metadata.addProperty("full_name", fullName.trim());
        body.add("data", metadata);
        try { send("POST", "/auth/v1/signup", body, false); }
        finally { Arrays.fill(password, '\0'); body.remove("password"); clear(); }
    }

    public synchronized String signIn(String email, char[] password) throws IOException, InterruptedException {
        clear();
        JsonObject body = new JsonObject();
        body.addProperty("email", email.trim()); body.addProperty("password", new String(password));
        try {
            JsonObject response = send("POST", "/auth/v1/token?grant_type=password", body, false).getAsJsonObject();
            acceptSession(response);
            return response.getAsJsonObject("user").get("id").getAsString();
        } finally { Arrays.fill(password, '\0'); body.remove("password"); }
    }
    private void acceptSession(JsonObject session) throws IOException {
        if (!session.has("access_token") || !session.has("refresh_token")) throw new IOException("Supabase did not return a complete login session.");
        accessToken = session.get("access_token").getAsString();
        refreshToken = session.get("refresh_token").getAsString();
        expiresAt = Instant.now().plusSeconds(session.get("expires_in").getAsLong());
    }
    private void ensureSession() throws IOException, InterruptedException {
        if (accessToken == null) throw new IOException("Your session has ended. Please log in again.");
        if (expiresAt.isBefore(Instant.now().plusSeconds(60))) {
            JsonObject body = new JsonObject(); body.addProperty("refresh_token", refreshToken);
            try { acceptSession(send("POST", "/auth/v1/token?grant_type=refresh_token", body, false).getAsJsonObject()); }
            catch (IOException e) { clear(); throw new IOException("Session refresh failed. Please log out and log in again.", e); }
        }
    }
    public synchronized JsonElement get(String path) throws IOException, InterruptedException {
        ensureSession(); return send("GET", "/rest/v1/" + path, null, true);
    }
    public synchronized JsonElement rpc(String function, JsonObject body) throws IOException, InterruptedException {
        ensureSession(); return send("POST", "/rest/v1/rpc/" + function, body, true);
    }
    public synchronized void signOut() throws IOException, InterruptedException {
        try { if (accessToken != null) send("POST", "/auth/v1/logout?scope=local", new JsonObject(), true); }
        finally { clear(); }
    }
    public synchronized void clear() { accessToken = null; refreshToken = null; expiresAt = Instant.EPOCH; }
    private JsonElement send(String method, String path, JsonObject body, boolean authenticated) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(config.url() + path)).timeout(Duration.ofSeconds(30))
                .header("apikey", config.key()).header("Accept", "application/json");
        if (authenticated) request.header("Authorization", "Bearer " + accessToken);
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.toJson(body)));
        HttpResponse<String> response;
        try { response = http.send(request.build(), HttpResponse.BodyHandlers.ofString()); }
        catch (IOException e) {
            throw new IOException(method.equals("GET") ? "Unable to reach Supabase. Check your connection and project URL."
                    : "Supabase could not confirm the request. Refresh and check your records before retrying.", e);
        }
        JsonElement result = JsonNull.INSTANCE;
        try { if (!response.body().isBlank()) result = JsonParser.parseString(response.body()); }
        catch (JsonParseException e) { throw new IOException("Supabase returned an unexpected response (HTTP " + response.statusCode() + ")."); }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = "Request failed (HTTP " + response.statusCode() + ").";
            if (result.isJsonObject()) for (String key : new String[]{"message", "msg", "error_description", "error"})
                if (result.getAsJsonObject().has(key) && result.getAsJsonObject().get(key).isJsonPrimitive()) {
                    message = result.getAsJsonObject().get(key).getAsString(); break;
                }
            if (response.statusCode() == 401 && authenticated) message = "Session rejected. Please log out and log in again.";
            throw new IOException(message);
        }
        return result;
    }
}
