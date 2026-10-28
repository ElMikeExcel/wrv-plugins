package com.winkreyes.teamrewards.encounter;

import java.io.IOException;
import java.util.*;

/** Main-thread encounter adapter boundary. Does not alter combat ledgers or HP scaling. */
public final class TeamAdmission {
    public enum Kind { WITHER, WARDEN, DRAGON, ELDER_TRIO, RAID }
    public enum Mode { SURVIVAL, ADVENTURE, CREATIVE, SPECTATOR }
    public record Point(UUID world, double x, double y, double z) {
        public Point { Objects.requireNonNull(world); if (!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)) throw new IllegalArgumentException("nonfinite location"); }
        public boolean within(Point other, double range) {
            if (!world.equals(other.world)) return false;
            double dx=x-other.x,dy=y-other.y,dz=z-other.z;
            return dx*dx+dy*dy+dz*dz <= range*range;
        }
    }
    public record PlayerView(UUID id, Point position, Mode mode, boolean online, boolean alive) {
        public PlayerView { Objects.requireNonNull(id); Objects.requireNonNull(position); Objects.requireNonNull(mode); }
    }
    public record Encounter(String runId, Kind kind, Set<UUID> components, boolean active,
                            Point center, double configuredRange) {
        public Encounter {
            if (runId==null||runId.isBlank()) throw new IllegalArgumentException("run id");
            Objects.requireNonNull(kind); Objects.requireNonNull(center);
            components=Set.copyOf(components);
            if (components.isEmpty()||!Double.isFinite(configuredRange)||configuredRange<=0) throw new IllegalArgumentException("verified components/range required");
        }
    }
    /** Implementation must durably record membership before returning. Repeated qualification is idempotent. */
    public interface Membership { void qualify(String runId, UUID player) throws IOException; }
    private final Membership membership;
    public TeamAdmission(Membership membership) { this.membership=Objects.requireNonNull(membership); }

    /** Health deliberately absent: late arrivals below 70% remain eligible. No movement/attack/AFK gate. */
    public int observe(Encounter encounter, Collection<PlayerView> players) throws IOException {
        if (!encounter.active()) return 0;
        Set<UUID> seen=new HashSet<>(); int admitted=0;
        for (PlayerView player:players) {
            if (!seen.add(player.id()) || !player.online() || !player.alive()) continue;
            if (player.mode()!=Mode.SURVIVAL && player.mode()!=Mode.ADVENTURE) continue;
            if (!player.position().within(encounter.center(),encounter.configuredRange())) continue;
            membership.qualify(encounter.runId(),player.id()); admitted++;
        }
        return admitted;
    }
    /** Death/offline never revoke already-qualified membership. */
    public void departedOrDied(String runId, UUID player) { Objects.requireNonNull(runId); Objects.requireNonNull(player); }

    /** Stable identity supplied by owning plugin, not guessed from proximity or a display name. */
    public static String runId(Kind kind, UUID world, String ownerGeneration) {
        Objects.requireNonNull(kind); Objects.requireNonNull(world);
        if (ownerGeneration==null || !ownerGeneration.matches("[A-Za-z0-9._:-]{1,160}")) throw new IllegalArgumentException("owner generation");
        return kind.name()+":"+world+":"+ownerGeneration;
    }
}
