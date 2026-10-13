package me.ryanhamshire.GriefPrevention;
import java.util.UUID;
import org.bukkit.Location;
/** Original minimal fixture implementing only documented GP API surfaces, not GP code. */
public final class Claim {
    public Claim parent;
    public UUID owner;
    public Long id;
    public Location low, high;
    public Claim(UUID owner, long id) { this.owner = owner; this.id = id; }
    public UUID getOwnerID() { return owner; }
    public Long getID() { return id; }
    public Location getLesserBoundaryCorner() { return low; }
    public Location getGreaterBoundaryCorner() { return high; }
    public String allowBuild(Object player, Object material) { throw new AssertionError("Trust/build access must not establish shop ownership"); }
    public String allowContainers(Object player) { throw new AssertionError("Container trust must not establish shop ownership"); }
}
