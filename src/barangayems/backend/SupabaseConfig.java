package barangayems.backend;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.Properties;
import com.google.gson.JsonParser;

public record SupabaseConfig(String url, String key) {
    public SupabaseConfig {
        url = url == null ? "" : url.trim().replaceAll("/+$", "");
        key = key == null ? "" : key.trim();
        if (key.isEmpty() || key.startsWith("PASTE_"))
            throw new IllegalArgumentException("Copy config/supabase.properties.example to config/supabase.properties and enter your publishable or anon key.");
        URI uri;
        try { uri = URI.create(url); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid Supabase project URL."); }
        boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || local && "http".equals(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || !uri.getPath().isEmpty())
            throw new IllegalArgumentException("Use the HTTPS project API URL, without a dashboard path.");
        if (key.startsWith("sb_secret_")) throw new IllegalArgumentException("Secret keys cannot be used in this desktop client.");
        if (key.startsWith("eyJ")) {
            try {
                String payload = new String(Base64.getUrlDecoder().decode(key.split("\\.")[1]), StandardCharsets.UTF_8);
                if (!"anon".equals(JsonParser.parseString(payload).getAsJsonObject().get("role").getAsString()))
                    throw new IllegalArgumentException("Use a legacy anon key, not a privileged JWT key.");
            } catch (RuntimeException e) { throw new IllegalArgumentException("Use a publishable key or a valid legacy anon key."); }
        }
    }
    public static SupabaseConfig load() throws IOException {
        Properties p = new Properties();
        Path path = Paths.get("config", "supabase.properties");
        if (Files.exists(path)) try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(r); }
        String url = System.getenv("SUPABASE_URL");
        String key = System.getenv("SUPABASE_PUBLISHABLE_KEY");
        if (key == null || key.isBlank()) key = System.getenv("SUPABASE_ANON_KEY");
        return new SupabaseConfig(url == null ? p.getProperty("supabase.url", "https://zxqeienskrnthxmcreeo.supabase.co") : url,
                key == null ? p.getProperty("supabase.key", "") : key);
    }
}
