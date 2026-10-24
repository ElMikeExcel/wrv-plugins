package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The /deathrecord lookup logic over stored snapshots: ordering,
 * 1-based picking, and the exact text lines the command prints.
 * Kept pure (records + file names in, strings out) so the whole
 * surface is unit-tested off-server.
 */
public final class RecordIndex {

    /** One stored snapshot: its file name and its parsed record. */
    public static final class Entry {
        public final String fileName;
        public final DeathRecord record;
        public Entry(String fileName, DeathRecord record) {
            this.fileName = fileName;
            this.record = record;
        }
    }

    private RecordIndex() {}

    /** Newest first: by death instant; ties (same-second files)
     *  put the suffixed name first — "-2" is the later write of
     *  the same second. Plain lexicographic order cannot express
     *  that ('-' sorts before '.'), so ties break on name length
     *  (the suffixed name is longer), then reverse name. */
    public static List<Entry> newestFirst(List<Entry> entries) {
        List<Entry> out = new ArrayList<>(entries);
        out.sort((a, b) -> {
            int c = Long.compare(b.record.epochMillis, a.record.epochMillis);
            if (c != 0) return c;
            c = Integer.compare(b.fileName.length(), a.fileName.length());
            if (c != 0) return c;
            return b.fileName.compareTo(a.fileName);
        });
        return out;
    }

    /** The n-th most recent entry (1-based) of a newest-first
     *  list, or null when n is out of range. */
    public static Entry pick(List<Entry> newestFirst, int n) {
        if (n < 1 || n > newestFirst.size()) return null;
        return newestFirst.get(n - 1);
    }

    /** "2026-10-22 21:24:04 — ENTITY_ATTACK by Zombie (minecraft:zombie)
     *  at minecraft:overworld -1280.5, 71.0, -300.2 (8 dropped, 8 carried)" */
    public static String summaryLine(DeathRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.timeText).append(" — ");
        sb.append(r.damageCause.isEmpty() ? "unknown cause" : r.damageCause);
        if (!"none".equals(r.killer)) sb.append(" by ").append(r.killer);
        sb.append(" at ").append(r.world).append(' ')
          .append(fmt(r.x)).append(", ").append(fmt(r.y)).append(", ").append(fmt(r.z));
        sb.append(" (").append(r.drops.size()).append(" dropped, ")
          .append(r.inventory.size()).append(" carried)");
        return sb.toString();
    }

    /** "storage-0: minecraft:diamond_sword x1 damage=394
     *  enchants: minecraft:sharpness=4, minecraft:fire_aspect=2
     *  name=\"Rig Blade\"" — segments appear only when present. */
    public static String itemLine(DeathRecord.ItemEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.slot).append(": ").append(e.id).append(" x").append(e.count);
        if (e.damage >= 0) sb.append(" damage=").append(e.damage);
        if (!e.enchants.isEmpty()) sb.append(" enchants: ").append(RecordFormat.packEnchants(e.enchants));
        if (!e.customName.isEmpty()) sb.append(" name=\"").append(e.customName).append('"');
        return sb.toString();
    }

    /** Item line with its restore number and any restore mark:
     *  "#3 drop-2: minecraft:netherite_shovel x1 ... [restored to Sam 2026-10-24 01:30:00 UTC by Op]" */
    public static String numberedItemLine(int number, DeathRecord.ItemEntry e) {
        return "#" + number + " " + itemLine(e) + restoredNote(e);
    }

    /** " [restored to Sam 2026-10-24 01:30:00 UTC by Op]" ("" if never restored). */
    public static String restoredNote(DeathRecord.ItemEntry e) {
        if (!e.isRestored()) return "";
        return " [restored to " + e.restoredTo + " " + e.restoredAt
                + (e.restoredBy.isEmpty() ? "" : " by " + e.restoredBy)
                + (e.restoredTimes > 1 ? ", " + e.restoredTimes + " times" : "") + "]";
    }

    /** The numbered (restorable) list of a death: what actually
     *  dropped. With keepInventory (or another plugin keeping the
     *  items) nothing dropped, and the carried list is offered
     *  instead — only with force, since the player may still have it. */
    public static List<DeathRecord.ItemEntry> restorable(DeathRecord r) {
        return r.drops.isEmpty() ? r.inventory : r.drops;
    }

    public static boolean restorableIsCarried(DeathRecord r) {
        return r.drops.isEmpty() && !r.inventory.isEmpty();
    }

    /** 1-based page slice (size per page); empty when past the end. */
    public static <T> List<T> page(List<T> all, int page, int size) {
        int from = (Math.max(1, page) - 1) * size;
        if (from >= all.size()) return List.of();
        return all.subList(from, Math.min(all.size(), from + size));
    }

    public static int pageCount(int total, int size) {
        return Math.max(1, (total + size - 1) / size);
    }

    /** "Typed: /deathrecord restore Alex 1 3 to Sam (1 = newest death, 3 = item number)" */
    public static String typedHint(String player, int deathNumber, String items, String target) {
        StringBuilder sb = new StringBuilder("Typed: /deathrecord restore ").append(player);
        if (deathNumber > 0) sb.append(' ').append(deathNumber);
        if (items != null && !items.isEmpty()) sb.append(' ').append(items);
        if (target != null && !target.isEmpty() && !target.equalsIgnoreCase(player)) sb.append(" to ").append(target);
        if (deathNumber > 0) {
            sb.append(" (").append(deathNumber).append(" = ").append(deathNumber == 1 ? "newest death" : "death number, 1 is newest");
            if (items != null && !items.isEmpty()) {
                sb.append(", ").append(items).append(items.equals("all") ? " = every item" : " = item number" + (items.matches("\\d+") ? "" : "s"));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
