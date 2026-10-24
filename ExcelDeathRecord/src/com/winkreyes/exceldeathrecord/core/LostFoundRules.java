package com.winkreyes.exceldeathrecord.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 0.3.0: the Lost &amp; Found rules, pure so every edge is unit-tested:
 * which Burn Ledger entries a player may recover, the cooldown, and
 * the price (the free first recovery vs the normal price).
 */
public final class LostFoundRules {

    /** The config values the rules need. */
    public static final class Settings {
        public double price = 16000;
        public boolean freeEnabled = true;
        public double freeThreshold = 10000;       // free only when balance is UNDER this
        public boolean freeOnlyOnFirst = true;     // free only if no earlier recovery at all
        public long windowMillis = 7L * 24 * 3600 * 1000;
        public long cooldownMillis = 24L * 3600 * 1000;
    }

    /** The price decision for one confirm. */
    public static final class Price {
        public final double amount;
        public final boolean free;
        Price(double amount, boolean free) { this.amount = amount; this.free = free; }
        @Override public boolean equals(Object o) {
            return o instanceof Price p && p.free == free && Double.compare(p.amount, amount) == 0;
        }
        @Override public int hashCode() { return Double.hashCode(amount) * 31 + (free ? 1 : 0); }
        @Override public String toString() { return priceText(this); }
    }

    private LostFoundRules() {}

    /**
     * Why this entry can't be recovered by this player, or null when
     * it can. folder is the owner folder under burned/ (uuid or
     * "unknown"). Unknown-owner entries are never self-service.
     */
    public static String ineligible(DestructionRecord r, String folder, String playerUuid, long now, Settings s) {
        if (r == null) return "That entry could not be read.";
        if (playerUuid == null || playerUuid.isEmpty()) return "Unknown player.";
        if (r.ownerUuid == null || r.ownerUuid.isEmpty() || "unknown".equals(folder)) return "That item has no known owner.";
        if (!r.ownerUuid.equalsIgnoreCase(playerUuid) || (folder != null && !folder.isEmpty() && !folder.equalsIgnoreCase(playerUuid))) {
            return "That item belongs to someone else.";
        }
        if (r.epochMillis <= 0 || now - r.epochMillis > s.windowMillis) return "That item was lost more than " + daysText(s.windowMillis) + " ago.";
        if (r.epochMillis > now + 60_000L) return "That entry has a time in the future.";
        if (r.item == null || r.item.bytesB64 == null || r.item.bytesB64.isEmpty()) return "That entry has no saved exact copy.";
        if (r.item.isRestored()) return "That item was already brought back.";
        return null;
    }

    /** Milliseconds left on the cooldown (0 = ready). */
    public static long cooldownLeft(LostFoundState st, long now, Settings s) {
        if (st == null || st.lastRecoveryEpoch <= 0 || s.cooldownMillis <= 0) return 0;
        long left = st.lastRecoveryEpoch + s.cooldownMillis - now;
        return Math.max(0, left);
    }

    /** True when this confirm would be the free one. */
    public static boolean freeApplies(LostFoundState st, double balance, Settings s) {
        if (!s.freeEnabled) return false;
        if (st != null && st.freeUsed) return false;
        if (s.freeOnlyOnFirst && st != null && st.recoveries > 0) return false;
        return balance < s.freeThreshold;
    }

    public static Price decide(LostFoundState st, double balance, Settings s) {
        return freeApplies(st, balance, s) ? new Price(0, true) : new Price(s.price, false);
    }

    /** "Free (first recovery)" or "$16,000". */
    public static String priceText(Price p) {
        return p.free ? "Free (first recovery)" : money(p.amount);
    }

    /** "$16,000" for whole amounts, "$9,999.99" otherwise. */
    public static String money(double v) {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
        double cents = Math.round(v * 100.0) / 100.0;
        boolean whole = Math.abs(cents - Math.rint(cents)) < 1e-9;
        DecimalFormat f = new DecimalFormat(whole ? "#,##0" : "#,##0.00", sym);
        return (cents < 0 ? "-$" : "$") + f.format(Math.abs(cents));
    }

    /** "5h 12m", "12m", "less than a minute". */
    public static String durationText(long ms) {
        long mins = (ms + 59_999) / 60_000;
        if (mins <= 0) return "less than a minute";
        long h = mins / 60, m = mins % 60;
        if (h == 0) return m + "m";
        return m == 0 ? h + "h" : h + "h " + m + "m";
    }

    public static String daysText(long ms) {
        long d = ms / (24L * 3600 * 1000);
        if (d * 24L * 3600 * 1000 == ms) return d + (d == 1 ? " day" : " days");
        long h = ms / (3600L * 1000);
        return h + (h == 1 ? " hour" : " hours");
    }

    /**
     * The menu order: valuable items (enchanted, named, or a listed
     * type) first, newest first inside each group, capped at max.
     * Keeps a lava pit full of cobblestone from hiding a sword.
     */
    public static List<BurnIndex.Entry> menuOrder(List<BurnIndex.Entry> eligible, TypeGlob types, int max) {
        List<BurnIndex.Entry> out = new ArrayList<>(BurnIndex.newestFirst(eligible));
        out.sort((a, b) -> Boolean.compare(valuable(b.record.item, types), valuable(a.record.item, types)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    public static boolean valuable(DeathRecord.ItemEntry e, TypeGlob types) {
        return Attribution.isValuable(e) || (types != null && types.matches(e.id));
    }

    /** What a cause looks like in a player-facing line. */
    public static String causeWords(String cause) {
        if (cause == null) return "was lost";
        return switch (cause) {
            case "LAVA" -> "burned in lava";
            case "FIRE" -> "burned up";
            case "VOID" -> "fell into the void";
            case "DESPAWN" -> "despawned";
            default -> "was destroyed";
        };
    }
}
