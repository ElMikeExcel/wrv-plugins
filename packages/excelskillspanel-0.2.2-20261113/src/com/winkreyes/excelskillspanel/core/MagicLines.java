package com.winkreyes.excelskillspanel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure composition of the WRV Magic header block (DESIGN §7.2/§7.3
 * as superseded by the run-273 verification + Mike's Q2 FULL
 * HEADER ruling): the provider fills a {@link MagicData} DTO from
 * verified reads only — every field is nullable, and a null field
 * OMITS its line. The panel never fakes a header field: a read
 * that fails simply does not appear.
 *
 * Sources per field (all verified in MAGIC-HEADER-VERIFICATION):
 * - level / xpInto / xpToNext / totalXp / manaBoostPct: the
 *   ExcelMagicLevel PlaceholderAPI surface.
 * - rankName / rankKey / nextRankName: the Magic API — the
 *   CARRIED wand's path (0.2.2: MagicApiReads resolves the wand
 *   from the player's carried items, held first, highest path
 *   when several are carried; the class path is the last resort
 *   when the player owns no wand at all). 0.2.1 trusted only
 *   Mage.getActiveWand() and fell back to the class base path
 *   the moment no wand was in hand — the Item 13 bug
 *   (DIAGNOSIS.md). No Class line renders in 0.2.2 (Item 12:
 *   everyone is a mage).
 * - mana / manaMax / manaRegen: the SAME resolved wand — its
 *   live current mana, and its frame max/regen raised to the
 *   EFFECTIVE pool by the Magic-Level boosts (MagicLines.
 *   effectiveManaMax/effectiveManaRegen: frame x (1 + boost),
 *   truncated as Magic truncates — the live wand's mana is
 *   synced to that effective pool, so the frame alone would
 *   understate it: 150 frame reads 154 at level 136, 150 at
 *   Mike's level 26). Rank/next come from the WRV order, never
 *   Magic's getNextPath() (its master name is "Wizard"). The
 *   mage-effective reads survive only as the no-wand fallback,
 *   already effective there.
 * - regenBoostPct / costReductionPct: ExcelMagicLevel's reward
 *   table applied to the level (per-level fractions from its own
 *   live config x (level - 1) — RewardTable.boostAt's formula).
 * - creditedCasts: MagicGate's ledger (EarnedStore.creditedTotal
 *   — the rank-driving count, 1,215 for Mike; the old
 *   ExcelMagicLevel totalCasts, 694, is a different counter and
 *   is never read again). Labelled "Credited casts".
 * - sealsBroken: the GuildGate reflective read (GuildGateReads
 *   pattern), rendered "Guild seals: N of 5".
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MagicLines {
    private MagicLines() {}

    /** ExcelMagicLevel's canonical explainer (MagicLevelCommand.
     *  EXPLAINER, verbatim) — the Magic detail view's lead text. */
    public static final String EXPLAINER =
            "Your path and the seals decide what you can cast. Your Magic Level decides how strong "
            + "your casting is \u2014 a bigger mana pool, faster regen, cheaper casts. "
            + "It never locks or unlocks a spell.";

    /** Every Magic header input; null = the read was unavailable
     *  and the line is omitted. */
    public record MagicData(
            Integer level, Long xpInto, Long xpToNext, Long totalXp,
            String rankName, String rankKey, String nextRankName,
            Float mana, Integer manaMax, Integer manaRegen,
            String manaBoostPct, String regenBoostPct, String costReductionPct,
            Long creditedCasts, Integer sealsBroken) {}

    /** Magic XP bar: span = into + to-next. ExcelMagicLevel's
     *  surface reports to-next as 0 exactly at the level cap
     *  (the expansion's own contract), so to-next 0 maps to
     *  BarData's capped sentinel — full bar, MAX label — rather
     *  than a fake "into / into" span. Null when the level
     *  surface is absent. */
    public static PanelModel.BarData magicBar(MagicData d) {
        if (d.level() == null || d.xpInto() == null || d.xpToNext() == null) return null;
        if (d.xpToNext() == 0) return new PanelModel.BarData(d.xpInto(), 0);
        return new PanelModel.BarData(d.xpInto(), d.xpInto() + d.xpToNext());
    }

    /** The boosts line, or null when no boost surface read. At
     *  Magic Level 1 there are no boosts yet (ExcelMagicLevel's
     *  own wording); from level 2 the verified percentages show.
     *  Every part is re-rendered through {@link #displayPct} so
     *  all three boosts share the panel's 2-decimal display
     *  rounding (0.1.1) — including the mana-pool part, which
     *  arrives as a PlaceholderAPI string already formatted by
     *  ExcelMagicLevel at 4 decimals. */
    public static String boostsLine(MagicData d) {
        if (d.level() == null) return null;
        if (d.level() < 2) return "Boosts: none yet \u2014 they start at Magic Level 2";
        List<String> parts = new ArrayList<>();
        if (d.manaBoostPct() != null) parts.add("mana pool +" + displayPct(d.manaBoostPct()) + "%");
        if (d.regenBoostPct() != null) parts.add("mana regen +" + displayPct(d.regenBoostPct()) + "%");
        if (d.costReductionPct() != null) parts.add("cast cost -" + displayPct(d.costReductionPct()) + "%");
        if (parts.isEmpty()) return null;
        return "Boosts: " + String.join(", ", parts);
    }

    /** The header block's lines, in display order. */
    public static List<String> headerLines(MagicData d) {
        List<String> lines = new ArrayList<>();
        if (d.level() != null) {
            StringBuilder lvl = new StringBuilder("Magic Level " + d.level());
            if (d.totalXp() != null) lvl.append(" \u2014 ").append(PanelModel.fmt(d.totalXp())).append(" XP total");
            lines.add(lvl.toString());
        }
        if (d.rankName() != null && !d.rankName().isBlank()) {
            lines.add("Rank: " + d.rankName());
        } else if (d.rankKey() != null && !d.rankKey().isBlank()) {
            lines.add("Rank: " + d.rankKey());
        }
        if (d.nextRankName() != null && !d.nextRankName().isBlank()) {
            lines.add("Next rank: " + d.nextRankName());
        }
        // 0.2.2 (Item 12 — Mike, Nov 13, 2026: "Maybe remove the
        // class mage line since everyone is technically a mage"):
        // no "Class: Mage" line renders on any surface. The field
        // is gone from the record, not merely nulled — there is
        // nothing left to emit.
        if (d.mana() != null && d.manaMax() != null) {
            lines.add("Mana: " + Math.round(d.mana()) + " / " + PanelModel.fmt(d.manaMax()));
        }
        if (d.manaRegen() != null) {
            lines.add("Mana regen: " + d.manaRegen());
        }
        String boosts = boostsLine(d);
        if (boosts != null) lines.add(boosts);
        if (d.creditedCasts() != null) {
            lines.add("Credited casts: " + PanelModel.fmt(d.creditedCasts()));
        }
        if (d.sealsBroken() != null) {
            lines.add("Guild seals: " + d.sealsBroken() + " of 5");
        }
        return List.copyOf(lines);
    }

    /** The header block, or null when nothing at all read (no
     *  Magic block rather than an empty one).
     *
     *  0.2.1 (menu programme Item 9a \u2014 Mike, Nov 13, 2026:
     *  "No need for the one line that says magic. Magic level
     *  under it already gives it its title."): the block no
     *  longer carries a standalone "Magic" title line. Exactly
     *  the 0.1.1 Power Level fix, one block over: the block's
     *  FIRST header line (normally "Magic Level N \u2014 X XP
     *  total") becomes the block's title \u2014 the Java header
     *  item's name and the first body line on Bedrock \u2014 and
     *  the remaining lines follow it, so the full header content
     *  renders exactly once on every surface with no bare
     *  "Magic" line above it. {@link #headerLines} itself is
     *  unchanged: the Magic DETAIL view still composes from the
     *  full line list. */
    public static PanelModel.HeaderBlock headerBlock(MagicData d) {
        List<String> lines = headerLines(d);
        if (lines.isEmpty()) return null;
        return new PanelModel.HeaderBlock("magic", lines.get(0), "BLAZE_ROD",
                lines.subList(1, lines.size()), magicBar(d), true);
    }

    /** The Magic detail view: the canonical explainer, then every
     *  header line, then the bar numbers line. */
    public static List<String> detailLines(MagicData d) {
        List<String> lines = headerLines(d);
        if (lines.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        out.add(EXPLAINER);
        out.add("");
        out.addAll(lines);
        PanelModel.BarData bar = magicBar(d);
        if (bar != null) out.add("Magic XP: " + bar.line());
        return List.copyOf(out);
    }

    /** Effective mana-pool max from a wand FRAME plus the
     *  Magic-Level mana-pool boost: Magic keeps the live wand's
     *  mana synced to the EFFECTIVE pool (frame x (1 + boost)),
     *  so the frame alone understates the pool the player
     *  actually has. Rig-proven at level 136 (boost 3.2095%):
     *  frame 150 -> 154 (150 x 1.032095 = 154.8, truncated the
     *  way Magic's own int cast truncates); the no-wand class
     *  base reads the same way, 50 -> 51. At Mike's level 26
     *  (boost 0.5943%) the same formula gives 150 -> 150 and
     *  60 -> 60, so his "150 mana" pin is unchanged. A boost
     *  that does not parse (or is absent) returns the frame
     *  unchanged — the panel shows the frame rather than
     *  guessing a boost. Never call this on a value that is
     *  already effective (the no-wand mage reads): that would
     *  boost twice. */
    public static int effectiveManaMax(int frame, String manaBoostPct) {
        if (manaBoostPct == null) return frame;
        try {
            double pct = new java.math.BigDecimal(manaBoostPct.strip()).doubleValue();
            return (int) (frame * (1.0 + pct / 100.0));
        } catch (NumberFormatException e) {
            return frame;
        }
    }

    /** Effective mana regen from a wand FRAME plus the
     *  cumulative regen-boost FRACTION (per-level fraction x
     *  (level - 1), the same figure the Boosts line renders):
     *  frame 25 at level 136 (fraction 0.032095) -> 25
     *  (25 x 1.032095 = 25.8, truncated); at Mike's level 26
     *  (fraction 0.005943) -> 25. Same no-double-boost rule as
     *  {@link #effectiveManaMax}. */
    public static int effectiveManaRegen(int frame, double regenBoostFraction) {
        return (int) (frame * (1.0 + regenBoostFraction));
    }

    /** Percent formatting for a boost FRACTION (ExcelMagicLevel's
     *  Boost record stores fractions — 0.0125 = +1.25%; verified
     *  against its Boost / Percents / RewardTable sources).
     *
     *  0.1.0 used ExcelMagicLevel's own Percents.format verbatim
     *  (scale 4). On the live 1000-level curve the per-level
     *  fractions are cap-total / 999 (mana 0.0002377...), so at
     *  Mike's Magic Level 26 the cumulative boosts are 0.5943%
     *  and 0.2377% — already PERCENT numbers, but printed with
     *  4-decimal noise that reads like a raw fraction (Mike,
     *  Nov 13, 2026). 0.1.1 renders the panel's display at
     *  scale 2, trailing zeros stripped: 0.5943 -&gt; "0.59",
     *  0.2377 -&gt; "0.24", 13.75 -&gt; "13.75". Whole-percent
     *  rounding was considered and rejected: it would print
     *  0.59% as 1% (+68% error) and 0.24% as 0% (a real cost
     *  reduction shown as none). Scale 2 never zeroes a real
     *  boost on the shipped curve (the smallest step, level 2,
     *  renders 0.02% / 0.01%). /magiclevel's chat output keeps
     *  ExcelMagicLevel's own 4-decimal format, unchanged — the
     *  rounding is this panel's display choice only. */
    public static String percentOf(double fraction) {
        return java.math.BigDecimal.valueOf(fraction)
                .multiply(java.math.BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    /** Re-render a percent NUMBER that arrives already formatted
     *  (the %excelmagiclevel_mana_boost% PlaceholderAPI string,
     *  produced by ExcelMagicLevel's Percents.format at scale 4)
     *  at the panel's scale-2 display rounding, so every boost
     *  on the line rounds the same way. A value that does not
     *  parse as a number is returned unchanged — the line shows
     *  what the provider said rather than hiding the boost. */
    public static String displayPct(String percentNumber) {
        if (percentNumber == null) return null;
        try {
            return new java.math.BigDecimal(percentNumber.strip())
                    .setScale(2, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .toPlainString();
        } catch (NumberFormatException e) {
            return percentNumber;
        }
    }
}
