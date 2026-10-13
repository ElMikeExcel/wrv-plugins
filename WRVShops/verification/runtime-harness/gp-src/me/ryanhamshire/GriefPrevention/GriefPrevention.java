package me.ryanhamshire.GriefPrevention;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
/** Original test fixture. No real claims, player data, permissions, or GP implementation. */
public final class GriefPrevention extends JavaPlugin {
    public DataStore dataStore = new DataStore();
    public static final class DataStore {
        public Claim defaultClaim = new Claim(UUID.fromString("667cf2d7-0000-4000-8000-000000000001"), 100L);
        public final Map<String, Claim> claims = new HashMap<>();
        public boolean failLookup;
        public int calls;
        public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cached) {
            if (!ignoreHeight) throw new AssertionError("Ownership lookup must include vertical claim protection");
            if (failLookup) throw new IllegalStateException("Injected claim API failure");
            calls++;
            String key = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            return claims.containsKey(key) ? claims.get(key) : defaultClaim;
        }
    }
}
