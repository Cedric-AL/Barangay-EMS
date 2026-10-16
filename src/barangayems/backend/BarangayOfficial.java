package barangayems.backend;

/** Profile returned after Auth and server-owned role verification. */
public final class BarangayOfficial extends User {
    @Override public boolean isOfficial() { return true; }
}
