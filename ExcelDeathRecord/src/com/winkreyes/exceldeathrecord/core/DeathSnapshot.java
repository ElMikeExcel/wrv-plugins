package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * The pure builder the Bukkit adapter feeds: raw harvested
 * values in, a complete DeathRecord out. Item byte payloads
 * arrive as raw bytes (the adapter's ItemStack.serializeAsBytes
 * output) and are Base64'd here so the encoding choice is
 * unit-tested too.
 *
 * The design rule this class exists to honor (owner's hard
 * requirement, Oct 22, 2026): the snapshot is built ONLY from
 * the death event's own data — the event drop list plus the
 * inventory / armor / offhand state read at the event — NEVER
 * from item entities in the world. A lava death's drops burn a
 * tick after they land; a void death's drops never land at all.
 * Neither matters here: by the time the event fires, everything
 * the player had is already in this record, byte-exact.
 */
public final class DeathSnapshot {

    /** A raw harvested item: the summary fields plus the exact
     *  serialized bytes (may be null when the adapter's
     *  serialization failed — the summary still records the item
     *  existed, and the entry's bytes stay empty, never a guess). */
    public static final class RawItem {
        public String slot = "";
        public String id = "";
        public int count = 0;
        public int damage = -1;
        public java.util.Map<String, Integer> enchants = new java.util.LinkedHashMap<>();
        public String customName = "";
        public byte[] bytes = null;
    }

    private DeathSnapshot() {}

    public static DeathRecord build(
            String playerName, String playerUuid, long epochMillis, String timeText,
            String world, double x, double y, double z, String gamemode,
            String deathMessage, String damageCause, String killer,
            int xpLevel, int totalExperience, int droppedExp, boolean keepInventory,
            List<RawItem> drops, List<RawItem> inventory) {
        DeathRecord r = new DeathRecord();
        r.playerName = nz(playerName);
        r.playerUuid = nz(playerUuid);
        r.epochMillis = epochMillis;
        r.timeText = nz(timeText);
        r.world = nz(world);
        r.x = x; r.y = y; r.z = z;
        r.gamemode = nz(gamemode);
        r.deathMessage = nz(deathMessage);
        r.damageCause = nz(damageCause);
        r.killer = killer == null || killer.isEmpty() ? "none" : killer;
        r.xpLevel = xpLevel;
        r.totalExperience = totalExperience;
        r.droppedExp = droppedExp;
        r.keepInventory = keepInventory;
        r.drops = convert(drops);
        r.inventory = convert(inventory);
        return r;
    }

    private static List<DeathRecord.ItemEntry> convert(List<RawItem> raw) {
        List<DeathRecord.ItemEntry> out = new ArrayList<>();
        if (raw == null) return out;
        for (RawItem ri : raw) {
            DeathRecord.ItemEntry e = new DeathRecord.ItemEntry();
            e.slot = nz(ri.slot);
            e.id = nz(ri.id);
            e.count = ri.count;
            e.damage = ri.damage;
            if (ri.enchants != null) e.enchants.putAll(ri.enchants);
            e.customName = nz(ri.customName);
            e.bytesB64 = ri.bytes == null ? "" : Base64.getEncoder().encodeToString(ri.bytes);
            out.add(e);
        }
        return out;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
