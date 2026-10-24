package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The /deathrecord burned lookup logic: ordering and the exact
 * lines the command prints for Burn Ledger records. Pure, like
 * RecordIndex, so the whole surface is unit-tested off-server.
 */
public final class BurnIndex {

    /** One stored destruction record: file name + parsed record. */
    public static final class Entry {
        public final String fileName;
        public final DestructionRecord record;
        /** Owner folder under burned/ (uuid or "unknown"); set by the store. */
        public String folder = "";
        public Entry(String fileName, DestructionRecord record) {
            this.fileName = fileName;
            this.record = record;
        }
    }

    private BurnIndex() {}

    /** Newest first: by instant; same-second ties put the
     *  suffixed ("-2", later) write first — by name length then
     *  reverse name, the RecordIndex tiebreak. */
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

    /** "2026-10-22 21:25:00 — minecraft:diamond_sword x1 destroyed by
     *  LAVA at minecraft:the_nether 100.5, 32.0, -40.5 (owner:
     *  .BedrockAlex, after 42s in the world)" — owner/age segments
     *  appear only when known. */
    public static String summaryLine(DestructionRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.timeText).append(" — ").append(itemBrief(r.item))
          .append(" destroyed by ").append(r.cause)
          .append(" at ").append(r.world).append(' ')
          .append(fmt(r.x)).append(", ").append(fmt(r.y)).append(", ").append(fmt(r.z));
        sb.append(" (owner: ").append(r.ownerName.isEmpty() ? "unknown" : r.ownerName);
        if (r.ageSeconds >= 0) sb.append(", after ").append(r.ageSeconds).append("s in the world");
        sb.append(')');
        sb.append(RecordIndex.restoredNote(r.item));
        return sb.toString();
    }

    /** "minecraft:diamond_sword x1 damage=394 enchants: ..." — the
     *  item part of the summary, shared with the console line. */
    public static String itemBrief(DeathRecord.ItemEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.id).append(" x").append(e.count);
        if (e.damage >= 0) sb.append(" damage=").append(e.damage);
        if (!e.enchants.isEmpty()) sb.append(" enchants: ").append(RecordFormat.packEnchants(e.enchants));
        if (!e.customName.isEmpty()) sb.append(" name=\"").append(e.customName).append('"');
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
