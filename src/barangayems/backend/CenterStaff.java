package barangayems.backend;

/** Profile returned after Auth and server-owned role verification. */
public final class CenterStaff extends User {
    @Override public boolean isOfficial() { return false; }
}
