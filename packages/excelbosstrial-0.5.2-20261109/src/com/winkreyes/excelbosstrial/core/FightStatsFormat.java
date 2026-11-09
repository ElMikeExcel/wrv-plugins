/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The post-fight stats surface (0.4.1, backlog ㉚.9): renders
 * one PM-8 fight record for the players who fought it - the
 * automatic pop-up at an encounter close and the
 * /bosstrial stats re-show. Pure and Bukkit-free: the record
 * goes in, chat lines (with &amp; colour codes, the command
 * surface's house style) come out; the adapter colourises,
 * routes Bedrock players to a Floodgate form, and does the
 * record lookup.
 *
 * Space honesty (PM-4): every damage number on this surface
 * is POOL damage - the ledger's space, the same numbers the
 * boss bar showed while the fight ran - and the header line
 * says so, on both renderings. Healing DONE is the record's
 * own claim-matched column (a heal-spell cast matched to its
 * regain, effective amounts only); it ships in this build
 * because the ㉚.10 healing-attribution proof passed in the
 * 0.4.0 code phase (Protect's controlled-heal readback:
 * HoT heal credited 17.0 effective, splash credited as
 * POTION, fight summaries carrying nonzero healing_done) -
 * the design's hard gate on this column is therefore open.
 * ExcelProtect's entity-space figures are a separate
 * accounting and are never blended into these lines.
 *
 * The accounting split (PM-9) is always shown whole: player
 * damage, other damage (mobs and environment on the boss),
 * the pool the boss healed back, and the net the verdict
 * judged on. A spent pool is fully attributable from the
 * players' own screen - the Nov 7 fight's missing 5,470 can
 * not be invisible again.
 */
public final class FightStatsFormat {

    private FightStatsFormat() {
    }

    // ------------------------------------------------------------------
    // Readout math
    // ------------------------------------------------------------------

    /** One player's share of the PARTY's damage, percent.
     *  Zero when the party dealt nothing (a healers-only or
     *  verdict-less record) - never NaN, never negative. */
    public static double shareOfParty(double dealt, double playerDamage) {
        if (Double.isNaN(dealt) || Double.isNaN(playerDamage)
                || dealt <= 0 || playerDamage <= 0) {
            return 0.0;
        }
        return dealt / playerDamage * 100.0;
    }

    /** The viewer's row, or null when they are not in the
     *  record (an admin peeking, a late joiner the ledger
     *  never saw). */
    public static FightTracker.FightRecord.Row rowOf(
            FightTracker.FightRecord rec, UUID viewer) {
        if (rec == null || viewer == null) {
            return null;
        }
        for (FightTracker.FightRecord.Row r : rec.rows) {
            if (r.uuid.equals(viewer)) {
                return r;
            }
        }
        return null;
    }

    /** A close label in words a player reads once and
     *  understands. Unknown labels render as themselves -
     *  honest over pretty. */
    public static String outcomeText(String verdictLabel) {
        if (verdictLabel == null) {
            return "Fight closed";
        }
        switch (verdictLabel) {
            case "CREDIT":
                return "Seal earned";
            case "REPEAT_CREDIT":
                return "Seal earned - repeat clear";
            case "DENY_SHARE":
                return "Seal denied - the party's damage share fell short";
            case "DENY_PRESENCE":
                return "Seal denied - a champion was out of presence range";
            case "DENY_COOLDOWN":
                return "Seal denied - the credit cooldown was running";
            case "DENY_REPEAT_WINDOW":
                return "Seal denied - the repeat window was not open";
            case "CLOSED_NO_VERDICT":
                return "Fight closed with no verdict";
            case "GIVE_UP":
                return "Closed - the boss gave up waiting for a fight";
            case "ADMIN_RESET":
                return "Closed by an admin reset";
            case "SEAL_GRANTED":
                return "Seal granted - the encounter stands down";
            case "SERVER_STOP":
                return "Closed at server stop";
            case "IN_PROGRESS":
                return "Fight still open (mid-fight snapshot)";
            default:
                if (verdictLabel.startsWith("ELDER_PROGRESS")) {
                    return "Elder down - " + verdictLabel.substring(
                            "ELDER_PROGRESS".length()).trim() + " counted";
                }
                return verdictLabel;
        }
    }

    // ------------------------------------------------------------------
    // Renderings
    // ------------------------------------------------------------------

    /**
     * The close pop-up for one participant: the header (which
     * names the number space), the viewer's own line, the
     * party's accounting line, and the door to the full table.
     * A viewer with no row gets the header, the party line
     * and the door - never a fabricated "you" line.
     */
    public static List<String> popupLines(FightTracker.FightRecord rec, UUID viewer) {
        return popupLines(rec, viewer, 1.0);
    }

    /**
     * The pop-up at a display scale (Packet B, Item 35 - the
     * Nov 8, 2026 amendment: the stats pop-up is a player-facing
     * pool-space surface, so its numbers render at the uniform
     * display scale like every other pool figure; percentages
     * and durations are ratios/clocks, never scaled). The
     * unscaled form above is the record's own rendering - the
     * fights/ files and their round-trip keep it byte-for-byte.
     */
    public static List<String> popupLines(FightTracker.FightRecord rec, UUID viewer,
                                          double scale) {
        double sc = DisplayScale.sanitized(scale <= 0 ? 1.0 : scale);
        List<String> out = new ArrayList<>();
        out.add("&6\u2694 " + rec.kind.displayName() + " &7- "
                + outcomeText(rec.verdict));
        out.add(spaceLine());
        FightTracker.FightRecord.Row own = rowOf(rec, viewer);
        if (own != null) {
            out.add("&fYou dealt &a" + num(own.damageDealt, sc) + " &7("
                    + pct(shareOfParty(own.damageDealt, rec.playerDamage))
                    + " of the party's damage) &7- healed &a"
                    + num(own.healingDone, sc) + " &7- took &c"
                    + num(own.damageTaken, sc));
        }
        out.add(partyLine(rec, sc));
        out.add("&7Full table: &f/bosstrial stats");
        return out;
    }

    /**
     * The full table (/bosstrial stats): header naming the
     * space, one line per player in the record's own order
     * (damage dealt descending), the PM-9 split, and the
     * unattributed-healing line when any exists. The viewer's
     * row is marked.
     */
    public static List<String> tableLines(FightTracker.FightRecord rec, UUID viewer) {
        return tableLines(rec, viewer, 1.0);
    }

    /** The full table at a display scale (Packet B - see
     *  popupLines): every damage/healing/pool figure scales;
     *  shares and durations do not. */
    public static List<String> tableLines(FightTracker.FightRecord rec, UUID viewer,
                                          double scale) {
        double sc = DisplayScale.sanitized(scale <= 0 ? 1.0 : scale);
        List<String> out = new ArrayList<>();
        out.add("&6- " + rec.kind.displayName() + " fight record &7- "
                + outcomeText(rec.verdict) + " &6-");
        out.add("&7" + spaceText() + " &7- " + dateText(rec.endedMs)
                + " &7- lasted " + durationText(rec.durationSeconds()));
        out.add("&7Killer: &f" + (rec.killerName != null ? rec.killerName
                : "none recorded") + " &7- standing at the close: &f"
                + standingText(rec));
        int rank = 0;
        for (FightTracker.FightRecord.Row r : rec.rows) {
            rank++;
            boolean own = viewer != null && r.uuid.equals(viewer);
            out.add((own ? "&e\u00bb " : "&7") + "#" + rank + " &f" + r.name
                    + " &7- &a" + num(r.damageDealt, sc) + " dmg &7("
                    + pct(shareOfParty(r.damageDealt, rec.playerDamage)) + ")"
                    + " &7- &a" + num(r.healingDone, sc) + " healed"
                    + " &7- &a" + num(r.healingReceived, sc) + " received"
                    + " &7- &c" + num(r.damageTaken, sc) + " taken");
        }
        if (rec.rows.isEmpty()) {
            out.add("&7No player rows in this record.");
        }
        out.add(splitLine(rec, sc));
        double unattr = rec.unattributedSpellHealing + rec.otherUnattributed();
        if (unattr > 0) {
            out.add("&7Healing with no named source: &f" + num(unattr, sc)
                    + " &7(spell " + num(rec.unattributedSpellHealing, sc)
                    + " - other " + num(rec.otherUnattributed(), sc) + ")");
        }
        if (rec.context != null) {
            out.add("&7Monument: &f" + rec.context);
        }
        return out;
    }

    /** The PM-9 accounting line, shared by both renderings:
     *  players / other / boss healed / net of pool. */
    static String partyLine(FightTracker.FightRecord rec) {
        return partyLine(rec, 1.0);
    }

    static String partyLine(FightTracker.FightRecord rec, double scale) {
        return "&7Party: net &f" + num(rec.netDamage, scale) + " &7of &f"
                + num(rec.pool, scale) + " &7pool - players &f"
                + num(rec.playerDamage, scale) + " &7- other &f"
                + num(rec.otherDamage, scale) + " &7- boss healed &f"
                + num(rec.healedPool, scale) + " &7- " + durationText(rec.durationSeconds());
    }

    /** The split line for the full table: the party line's
     *  numbers plus the verdict's own share figure - player
     *  damage against the pool the gate judges on (it can
     *  pass 100 when the boss healed damage back and the
     *  party dealt it again). */
    static String splitLine(FightTracker.FightRecord rec) {
        return splitLine(rec, 1.0);
    }

    static String splitLine(FightTracker.FightRecord rec, double scale) {
        return "&7The split: players &f" + num(rec.playerDamage, scale)
                + " &7- other &f" + num(rec.otherDamage, scale)
                + " &7- boss healed &f" + num(rec.healedPool, scale)
                + " &7- net &f" + num(rec.netDamage, scale) + " &7of &f"
                + num(rec.pool, scale) + " &7pool - players dealt &f"
                + pct(rec.sharePercent) + " &7of the pool by their own hands";
    }

    private static String spaceLine() {
        return "&7" + spaceText() + ".";
    }

    /** PM-4: the one sentence that names the number space,
     *  carried by both renderings. */
    static String spaceText() {
        return "Every damage number is pool damage - the numbers the boss bar showed";
    }

    private static String standingText(FightTracker.FightRecord rec) {
        if (rec.championsStandingNames.isEmpty()) {
            return "none recorded";
        }
        return String.join(", ", rec.championsStandingNames);
    }

    // ------------------------------------------------------------------
    // Small formatters
    // ------------------------------------------------------------------

    /** The record's own number style (one decimal, always) -
     *  delegated so this surface can never drift from the
     *  fights/ files and the log lines. */
    static String num(double v) {
        return FightTracker.FightRecord.num(v);
    }

    /** A figure at a display scale: the record's own one-decimal
     *  style applied AFTER scaling, so the scaled surface reads
     *  in the same voice as the record (1580.0 at scale 1000
     *  renders "1580000.0"). */
    static String num(double v, double scale) {
        return FightTracker.FightRecord.num(DisplayScale.apply(scale, v));
    }

    static String pct(double v) {
        return num(v) + "%";
    }

    /** 45s / 23m 41s / 1h 3m 5s. */
    public static String durationText(long seconds) {
        long s = Math.max(0L, seconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long r = s % 60;
        if (h > 0) {
            return h + "h " + m + "m " + r + "s";
        }
        if (m > 0) {
            return m + "m " + r + "s";
        }
        return r + "s";
    }

    private static String dateText(long endedMs) {
        return DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(endedMs));
    }

    /** The lines as plain text (a Bedrock form renders no
     *  colour codes): codes stripped, one line per line. */
    public static String plainText(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line.replaceAll("&[0-9a-fk-orA-FK-OR]", ""));
        }
        return sb.toString();
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
