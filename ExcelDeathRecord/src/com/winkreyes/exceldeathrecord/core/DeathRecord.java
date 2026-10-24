package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One death snapshot, as plain data (no Bukkit): everything the
 * Bukkit adapter harvested from a PlayerDeathEvent, in the exact
 * shape RecordFormat writes to disk and reads back.
 *
 * Two item sections, both captured at the moment of death:
 *   drops     — the event's own drop list, exactly what the game
 *               was about to scatter on the ground;
 *   inventory — the player's complete inventory state at death
 *               (storage slots, armor, offhand), slot by slot.
 * With keepInventory off the two mirror each other; with it on,
 * drops is empty and inventory is the record that matters.
 */
public final class DeathRecord {

    /** One recorded item stack. The bytes are the item itself
     *  (Paper ItemStack.serializeAsBytes) as Base64 — lossless:
     *  enchantments, damage, names, every component. The other
     *  fields are the human-readable summary written beside the
     *  bytes so a record can be read with no tooling at all. */
    public static final class ItemEntry {
        public String slot = "";          // "drop-0", "storage-9", "helmet", "offhand", ...
        public String id = "";            // namespaced item id, e.g. minecraft:diamond_sword
        public int count = 0;
        public int damage = -1;           // -1 = not a damageable item
        public Map<String, Integer> enchants = new LinkedHashMap<>();
        public String customName = "";    // "" = no custom name
        public String bytesB64 = "";
        // Restore mark (0.2.0). restoredTimes == 0 means never
        // restored; the other fields describe the LATEST restore.
        public int restoredTimes = 0;
        public String restoredTo = "";
        public String restoredToUuid = "";
        public String restoredBy = "";
        public long restoredEpoch = 0;
        public String restoredAt = "";    // "yyyy-MM-dd HH:mm:ss UTC"

        public boolean isRestored() { return restoredTimes > 0; }

        @Override public boolean equals(Object o) {
            if (!(o instanceof ItemEntry other)) return false;
            return count == other.count && damage == other.damage
                    && Objects.equals(slot, other.slot) && Objects.equals(id, other.id)
                    && Objects.equals(enchants, other.enchants)
                    && Objects.equals(customName, other.customName)
                    && Objects.equals(bytesB64, other.bytesB64)
                    && restoredTimes == other.restoredTimes && restoredEpoch == other.restoredEpoch
                    && Objects.equals(restoredTo, other.restoredTo)
                    && Objects.equals(restoredToUuid, other.restoredToUuid)
                    && Objects.equals(restoredBy, other.restoredBy)
                    && Objects.equals(restoredAt, other.restoredAt);
        }
        @Override public int hashCode() {
            return Objects.hash(slot, id, count, damage, enchants, customName, bytesB64,
                    restoredTimes, restoredTo, restoredToUuid, restoredBy, restoredEpoch, restoredAt);
        }
    }

    public String playerName = "";
    public String playerUuid = "";
    public long epochMillis = 0;
    public String timeText = "";          // "yyyy-MM-dd HH:mm:ss", server-local
    public String world = "";             // world key, e.g. minecraft:overworld
    public double x, y, z;
    public String gamemode = "";
    public String deathMessage = "";
    public String damageCause = "";       // DamageCause enum name, "" if none was recorded
    public String killer = "none";        // "Name (player)", "Zombie (minecraft:zombie)", "none"
    public int xpLevel = 0;
    public int totalExperience = 0;
    public int droppedExp = 0;
    public boolean keepInventory = false;
    // XP restore mark (0.2.0); xpRestoredTimes == 0 = never.
    public int xpRestoredTimes = 0;
    public String xpRestoredTo = "";
    public String xpRestoredBy = "";
    public String xpRestoredAt = "";
    public List<ItemEntry> drops = new ArrayList<>();
    public List<ItemEntry> inventory = new ArrayList<>();

    @Override public boolean equals(Object o) {
        if (!(o instanceof DeathRecord other)) return false;
        return epochMillis == other.epochMillis
                && Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0
                && Double.compare(z, other.z) == 0
                && xpLevel == other.xpLevel && totalExperience == other.totalExperience
                && droppedExp == other.droppedExp && keepInventory == other.keepInventory
                && Objects.equals(playerName, other.playerName)
                && Objects.equals(playerUuid, other.playerUuid)
                && Objects.equals(timeText, other.timeText)
                && Objects.equals(world, other.world)
                && Objects.equals(gamemode, other.gamemode)
                && Objects.equals(deathMessage, other.deathMessage)
                && Objects.equals(damageCause, other.damageCause)
                && Objects.equals(killer, other.killer)
                && xpRestoredTimes == other.xpRestoredTimes
                && Objects.equals(xpRestoredTo, other.xpRestoredTo)
                && Objects.equals(xpRestoredBy, other.xpRestoredBy)
                && Objects.equals(xpRestoredAt, other.xpRestoredAt)
                && Objects.equals(drops, other.drops)
                && Objects.equals(inventory, other.inventory);
    }
    @Override public int hashCode() {
        return Objects.hash(playerName, playerUuid, epochMillis, timeText, world, x, y, z,
                gamemode, deathMessage, damageCause, killer, xpLevel, totalExperience,
                droppedExp, keepInventory, xpRestoredTimes, xpRestoredTo, xpRestoredBy, xpRestoredAt, drops, inventory);
    }
}
