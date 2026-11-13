package com.winkreyes.excelskillspanel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure composition of an mcMMO skill row from raw API numbers —
 * the arithmetic the provider-mcmmo module performs, kept
 * Bukkit-free so the harness proves it without a server.
 *
 * Inputs are mcMMO ExperienceAPI semantics (verified against the
 * WRV fork's source): {@code xpInto} = XP earned into the current
 * level (getXP), {@code xpSpan} = the level's total XP span
 * (getXPToNextLevel), level from getLevel.
 *
 * 0.2.0 (Item 8, decision-log D32): the detail view's tail is the
 * skill's verified perk ladder (PerkLadder), supplied by the
 * provider ONLY after the live mcMMO data has been verified
 * against the verified table for that skill. The row builders
 * that take no ladder keep the standing website pointer — that
 * pointer is the fail-soft shape for an unverified skill, never
 * a placeholder for an invented ladder.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SkillMath {
    private SkillMath() {}

    /** The standing last line of every mcMMO skill detail view. */
    public static final String PERK_POINTER =
            "Full perk ladder: Skill Trees page on the website";

    public static PanelModel.SkillRow mcmmoRow(String id, String name, String iconKey,
                                               int level, int xpInto, int xpSpan,
                                               String rankLabel) {
        return mcmmoRow(id, name, iconKey, level, xpInto, xpSpan, rankLabel,
                List.of(PERK_POINTER));
    }

    /** 0.2.0: a row whose detail tail is {@code detailTail} — the
     *  verified perk ladder lines for this skill at this level,
     *  or the pointer line when the ladder did not verify. */
    public static PanelModel.SkillRow mcmmoRow(String id, String name, String iconKey,
                                               int level, int xpInto, int xpSpan,
                                               String rankLabel, List<String> detailTail) {
        PanelModel.BarData bar = new PanelModel.BarData(xpInto, xpSpan);
        List<String> detail = new ArrayList<>();
        detail.add("Level: " + level);
        detail.add(bar.line());
        detail.add("XP into this level: " + PanelModel.fmt(Math.max(0, xpInto)));
        if (bar.capped()) {
            detail.add("XP to next level: \u2014 (at the cap)");
        } else {
            detail.add("XP to next level: "
                    + PanelModel.fmt(Math.max(0, (long) xpSpan - xpInto)));
        }
        if (rankLabel != null && !rankLabel.isBlank()) {
            detail.add("Rank: " + rankLabel);
        }
        detail.addAll(detailTail);
        return new PanelModel.SkillRow(id, name, iconKey, level, bar, rankLabel, detail);
    }

    /** A child-skill row (Salvage, Smelting): a level but no XP
     *  and no bar — mcMMO derives a child skill's level from its
     *  parent skills, and its XP API reads throw
     *  UnsupportedOperationException ("Child skills do not have
     *  XP"), proven on the build rig. The detail view says
     *  exactly that instead of inventing a bar. */
    public static PanelModel.SkillRow childRow(String id, String name, String iconKey,
                                               int level, String rankLabel) {
        return childRow(id, name, iconKey, level, rankLabel, List.of(PERK_POINTER));
    }

    /** 0.2.0 (D32): a child row with a detail tail. The child
     *  explainer lines stay ABOVE the ladder; the ladder's earned
     *  state is computed at the child's DERIVED level, exactly as
     *  RankUtils.getRank reads it (LADDER-DATA §4). */
    public static PanelModel.SkillRow childRow(String id, String name, String iconKey,
                                               int level, String rankLabel,
                                               List<String> detailTail) {
        List<String> detail = new ArrayList<>();
        detail.add("Level: " + level);
        detail.add("Child skill \u2014 its level comes from its parent skills.");
        detail.add("It earns no XP of its own, so there is no progress bar.");
        if (rankLabel != null && !rankLabel.isBlank()) {
            detail.add("Rank: " + rankLabel);
        }
        detail.addAll(detailTail);
        return new PanelModel.SkillRow(id, name, iconKey, level, null, rankLabel, detail);
    }

    /** The mcMMO header block: Power Level as ONE line.
     *
     *  0.1.1 (Mike, Nov 13, 2026, live Bedrock pass): 0.1.0 carried
     *  the title "Power Level" AND a first line "Power Level: N" —
     *  the Bedrock body prints title then lines, so both printed,
     *  one above the other. The value now lives in the block's
     *  title itself ("Power Level 579") and the block carries no
     *  lines, so every surface shows it exactly once: the Bedrock
     *  body prints the title line alone, the chat fallback prints
     *  it once, and the Java chest shows it as the header item's
     *  name with no duplicating lore line.
     *
     *  The cap line is GONE entirely (Mike, same pass): the WRV
     *  fork's getPowerLevelCap() returns Integer.MAX_VALUE when
     *  uncapped, so the line printed "Power Level cap:
     *  2,147,483,647" — pure noise. The {@code powerLevelCap}
     *  parameter is retained (the provider still passes the API
     *  read) but is deliberately never rendered. */
    public static PanelModel.HeaderBlock powerHeader(int powerLevel, int powerLevelCap) {
        return new PanelModel.HeaderBlock("power", "Power Level " + PanelModel.fmt(powerLevel),
                "EXPERIENCE_BOTTLE", List.of(), null, false);
    }
}
