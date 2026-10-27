package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the spell book shows about one spell, read from Magic's live
 * spell templates (so it always matches spells.yml at the current seal).
 */
public final class SpellFacts {
    public String key = "";
    public String name = "";
    public String description = "";
    public int rank = 1;
    public int maxRank = 1;
    public int bar = 0;                       // 1-based bar, 0 = not on a bar
    public final List<String> costs = new ArrayList<>();        // e.g. "2 Mana"
    public final List<String> activeCosts = new ArrayList<>();  // e.g. "1 Mana"
    public long cooldownMs = 0;
    public double range = 0;
    public boolean selfOnly = false;
    public boolean toggle = false;
    public boolean stopsManaRegen = false;
    public long durationMs = 0;
    public long nextRankCasts = 0;            // casts needed for the next rank (0 = unknown/none)
    public long castCount = 0;                // this player's casts of the spell
    public String nextRankPath = "";          // e.g. "student"
    /** 0.2.2: the wand's current rank already allows the next spell rank (no "opens at X rank" note). */
    public boolean pathOk = true;
    /** 0.2.2: the wand has no Magic path, so casting never ranks this spell up; its next rank comes with the next wand rank. */
    public boolean rankByWand = false;
    /** 0.2.2: this is the wand's active spell right now (the one a cast uses). */
    public boolean active = false;

    // Spell info 2026-10-26: real effects and how the next rank is reached.
    /** What this rank does, in plain words, from the spell's own config (SpellEffects.lines). */
    public final List<String> effects = new ArrayList<>();
    /** Magic cooldown_mage after the cast (0 = none, or the spell skips it). Already cut by the wand's cooldown reduction. */
    public long mageCooldownMs = 0;
    /** Pathless rank wands: the first wand rank that raises this spell ("Student"), "" = none. */
    public String nextWandRank = "";
    /** The spell rank that wand rank gives (0 = none). */
    public int nextWandRankLevel = 0;
    /** What changes at that rank (effect, cost, cooldown or range lines that differ). */
    public final List<String> nextChanges = new ArrayList<>();
    /** How a player reaches nextWandRank (config text, reloadable). "" = say nothing. */
    public String rankUpHow = "";
}

