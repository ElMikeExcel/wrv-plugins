package com.winkreyes.exceldeathrecord.core;

import java.util.Objects;

/**
 * One Burn Ledger record: a valuable (or, for lava/fire/void,
 * any) item stack destroyed in the world, with who it belonged
 * to as far as the platform could tell. The item itself rides
 * in a DeathRecord.ItemEntry — the same byte-exact + summary
 * shape the death snapshots use, so one restore tooling fits
 * both ledgers later.
 */
public final class DestructionRecord {

    public String ownerUuid = "";         // "" = owner unknown
    public String ownerName = "";         // "" = owner unknown
    public long epochMillis = 0;
    public String timeText = "";          // "yyyy-MM-dd HH:mm:ss", server-local
    public String cause = "";             // LAVA | FIRE | VOID | DESPAWN
    public String world = "";             // world key, e.g. minecraft:overworld
    public double x, y, z;
    public long ageSeconds = -1;          // time in the world before destruction; -1 = unknown
    public DeathRecord.ItemEntry item = new DeathRecord.ItemEntry();

    @Override public boolean equals(Object o) {
        if (!(o instanceof DestructionRecord other)) return false;
        return epochMillis == other.epochMillis && ageSeconds == other.ageSeconds
                && Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0
                && Objects.equals(ownerUuid, other.ownerUuid)
                && Objects.equals(ownerName, other.ownerName)
                && Objects.equals(timeText, other.timeText)
                && Objects.equals(cause, other.cause)
                && Objects.equals(world, other.world)
                && Objects.equals(item, other.item);
    }
    @Override public int hashCode() {
        return Objects.hash(ownerUuid, ownerName, epochMillis, timeText, cause, world,
                x, y, z, ageSeconds, item);
    }
}
