package com.winkreyes.excelsummons;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mana-reserve upkeep for combat summons, pure and unit-tested
 * (ExcelSummons 0.1.2, Mike's ruling, Nov 8, 2026: summons should
 * "take a percentage of the player's mana bar as payment so that
 * way people aren't endlessly summoning a bunch of freaking stuff").
 *
 * The model is the server's own toggle pattern (Boon −50%, Cloak
 * −75%, Force −50% max-mana reserve) extended to summons: while a
 * combat summon lives, a percentage of its caster's mana pool is
 * held — unavailable — and released the moment the summon leaves,
 * on EVERY despawn path (expiry, death, dismissal, caster
 * logout/death, chunk re-check), because every one of those paths
 * unregisters the record and the bridge re-derives the reserve
 * from the registry after each mutation.
 *
 * THE MECHANISM IS REAL, and it is Magic's own: a reserve is a
 * Magic modifier template (shipped in the summons packet's
 * modifiers addition, exactly like wrv_boon_reserve) carrying a
 * negative mana_max_boost. Bytecode-verified against Magic 11.2.5:
 * Mage.addPassiveEffects SUMS every active source's mana_max_boost
 * into one field, and CasterProperties.updateMaxMana multiplies
 * the pool by (1 + that sum) — so reserves stack ADDITIVELY with
 * the toggle reserves and with each other, scale automatically
 * with MagicGate's rank pools (a percentage of whatever pool the
 * mage currently has), and show wherever Magic shows modifiers
 * and the reduced effective max. Two engine facts shape the rules
 * below: (1) addModifier refuses a second copy of a key already
 * held without duration, so two summons of one family are carried
 * by a count-tiered key (…_archer, …_archer2); (2) NO clamp on the
 * summed boost exists in the engine — a pool could in principle be
 * multiplied to zero or below — so the bridge enforces both a
 * ceiling on its own total and a floor on the mage's remaining
 * usable fraction, computed from the mage's LIVE numbers at
 * PreCast (a player running Cloak has less headroom, and the
 * reserve respects that instead of discovering it).
 *
 * Scope: 0.1.2 was COMBAT branch only. 0.1.4 (FIX-PACKET-DESIGN
 * §2, Mike, Nov 10, 2026, 10:08 AM — "companions hold mana too")
 * widens the scope to EVERY family with a configured percent > 0:
 * combat (archer 10, bruiser 15, guardian 25, revenant 20,
 * ghast 25), wolf 10, familiar 5, cat 5, mount 10. All holds —
 * combat, companion, and mount — sum toward the one 75% ceiling;
 * the mount is excluded from the §3 COUNT cap, never from the
 * reserve total. The scope is read from the percent table itself
 * (percent > 0), never from a branch filter, so the apply logic,
 * the PreCast gate, and the list lines can never drift apart.
 *
 * 0.1.6 (Mike's ruling, Nov 12, 2026, ~3:08 PM CST — "If they are
 * original non summonsed Pets they shouldn't hold mana. Someone
 * took the time to tame something and or breed them"): ADOPTED-
 * origin records hold NO mana, permanently — every derivation in
 * this class (counts, raw total, desired modifiers) skips them,
 * so a hand-tamed or bred animal contributes 0% at PreCast, at
 * sync, and in the list lines alike. CAST records keep the rates
 * above. Adopted records still count toward the §3 count cap
 * (CapPolicy is untouched); only the reserve is exempt.
 *
 * 0.1.6 also closes the re-derivation hole behind Jester's
 * Nov 12, 2026 report (his all-adopted menagerie held 85% of a
 * 200 pool): the ceiling + floor used to be enforced at PreCast
 * ONLY, while the login/sync re-derivation (desiredModifiers)
 * applied the raw sum. desiredModifiers now CLAMPS: holds are
 * applied per record in registry order, a record is included
 * only while the running total stays within BOTH the ceiling
 * and the floor-derived cap (100 − floor), and a record that
 * would pass the cap is skipped — it stays registered and
 * alive, it simply holds nothing, and a smaller later record
 * that still fits is still included. The applied total can
 * therefore never pass the ceiling nor eat the floor, on any
 * path — cast, login, or drift sweep.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ReservePolicy {

    private ReservePolicy() {}

    /** Modifier key prefix; the full key set ships as templates in
     *  the summons packet's modifiers addition. */
    public static final String KEY_PREFIX = "wrv_summon_reserve_";

    /** The shipped config defaults (config.yml
     *  reserve-ceiling-percent / reserve-floor-percent). The
     *  three-argument derivations below clamp at these values;
     *  the listener passes the live config values explicitly. */
    public static final int DEFAULT_CEILING_PERCENT = 75;
    public static final int DEFAULT_FLOOR_PERCENT = 20;

    /** 0.1.6: only a CAST-origin record can hold reserve. An
     *  ADOPTED record (a hand-tamed / bred animal the sweep
     *  registered) holds nothing, permanently (Mike's ruling,
     *  Nov 12, 2026). Every derivation funnels through here so
     *  the exemption can never apply on one path and not another. */
    private static boolean holdsReserve(SummonRecord rec) {
        return rec != null && rec.origin() == SummonRecord.Origin.CAST;
    }

    /** The most the applied total may reach under a ceiling +
     *  floor pair: the ceiling itself, and never so much that
     *  less than the floor of the pool stays usable. */
    private static int appliedCap(int ceilingPercent, int floorPercent) {
        int byCeiling = Math.max(0, ceilingPercent);
        int byFloor = Math.max(0, 100 - Math.max(0, floorPercent));
        return Math.min(byCeiling, byFloor);
    }

    /** Percent of the caster's pool one living summon of this family
     *  holds; 0 for anything outside the combat branch / unknown. */
    public static int percentFor(Map<String, Integer> percents, String baseSpellKey) {
        if (percents == null || baseSpellKey == null) return 0;
        Integer p = percents.get(baseSpellKey);
        return p == null ? 0 : Math.max(0, p);
    }

    /** Total reserve percent an owner's living CAST summons would
     *  hold with NO clamp applied — any CAST record whose family
     *  has a configured percent > 0 (§2.1a) — derived from the
     *  registry. ADOPTED records contribute 0 (0.1.6, Mike's
     *  ruling). This is the RAW cast total: it may exceed the
     *  ceiling for a grandfathered set, which is exactly why the
     *  applied state is desiredModifiers / appliedPercent, never
     *  this number. Kept as the registry truth the tests and the
     *  diagnosis read; the PreCast gate reads appliedPercent so
     *  its "already held" number is the clamped, applied one. */
    public static int totalPercent(SummonRegistry reg, UUID owner,
                                   Map<String, Integer> percents) {
        int total = 0;
        for (SummonRecord rec : reg.ofOwner(owner)) {
            if (!holdsReserve(rec)) continue;
            total += percentFor(percents, rec.spellKey());
        }
        return total;
    }

    /** Living counts per reserve-bearing family for one owner
     *  (CAST records of families with a nonzero reserve percent
     *  only, any branch — §2.1b; ADOPTED records excluded, 0.1.6),
     *  in registry order. Renamed from 0.1.2's combatCounts
     *  when the scope widened past the combat branch. NOTE: these
     *  are the UNCLAMPED cast counts; the modifier state the mage
     *  actually carries is desiredModifiers, which clamps. */
    public static Map<String, Integer> reserveCounts(SummonRegistry reg, UUID owner,
                                                     Map<String, Integer> percents) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SummonRecord rec : reg.ofOwner(owner)) {
            if (!holdsReserve(rec)) continue;
            if (percentFor(percents, rec.spellKey()) <= 0) continue;
            counts.merge(rec.spellKey(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * The modifier key carrying {@code count} living summons of one
     * family: count 1 is the bare key, count 2+ suffixes the count
     * (…_archer2). The packet's modifiers addition defines exactly
     * the keys the family caps can produce (archer/bruiser: 1 and 2;
     * guardian/revenant/ghast: 1).
     */
    public static String modifierKey(String baseSpellKey, int count) {
        if (baseSpellKey == null || count <= 0) return null;
        return count == 1 ? KEY_PREFIX + baseSpellKey
                          : KEY_PREFIX + baseSpellKey + count;
    }

    /** The full desired modifier state for one owner: key → the
     *  total percent that key stands for, clamped at the shipped
     *  default ceiling + floor. The listener diffs this against
     *  the mage's held modifiers on every sync. */
    public static Map<String, Integer> desiredModifiers(SummonRegistry reg, UUID owner,
                                                        Map<String, Integer> percents) {
        return desiredModifiers(reg, owner, percents,
                DEFAULT_CEILING_PERCENT, DEFAULT_FLOOR_PERCENT);
    }

    /**
     * The desired modifier state under a live ceiling + floor
     * (0.1.6): CAST records are walked in registry order and a
     * record's hold is included only while the running applied
     * total stays within appliedCap(ceiling, floor); a record
     * that would pass the cap is skipped (it stays registered —
     * it simply holds nothing) and a smaller later record that
     * still fits is still included. ADOPTED records never enter
     * the walk (0.1.6 exemption). Included counts per family are
     * then carried by the count-tiered modifier keys exactly as
     * before, so a clamped set's keys are always a subset of the
     * keys the unclamped set could produce.
     */
    public static Map<String, Integer> desiredModifiers(SummonRegistry reg, UUID owner,
                                                        Map<String, Integer> percents,
                                                        int ceilingPercent, int floorPercent) {
        int cap = appliedCap(ceilingPercent, floorPercent);
        Map<String, Integer> included = new LinkedHashMap<>();
        int running = 0;
        for (SummonRecord rec : reg.ofOwner(owner)) {
            if (!holdsReserve(rec)) continue;
            int pct = percentFor(percents, rec.spellKey());
            if (pct <= 0) continue;
            if (running + pct > cap) continue; // over the cap: holds nothing
            included.merge(rec.spellKey(), 1, Integer::sum);
            running += pct;
        }
        Map<String, Integer> desired = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : included.entrySet()) {
            String key = modifierKey(e.getKey(), e.getValue());
            if (key != null) {
                desired.put(key, percentFor(percents, e.getKey()) * e.getValue());
            }
        }
        return desired;
    }

    /** The APPLIED total percent for one owner: the sum the mage
     *  actually carries after the adopted exemption and the
     *  ceiling + floor clamp — desiredModifiers' values summed.
     *  Never above the ceiling, never eating the floor. */
    public static int appliedPercent(SummonRegistry reg, UUID owner,
                                     Map<String, Integer> percents,
                                     int ceilingPercent, int floorPercent) {
        int total = 0;
        for (int v : desiredModifiers(reg, owner, percents,
                ceilingPercent, floorPercent).values()) {
            total += v;
        }
        return total;
    }

    /** The applied total at the shipped default ceiling + floor. */
    public static int appliedPercent(SummonRegistry reg, UUID owner,
                                     Map<String, Integer> percents) {
        return appliedPercent(reg, owner, percents,
                DEFAULT_CEILING_PERCENT, DEFAULT_FLOOR_PERCENT);
    }

    /**
     * Whether ONE record actually holds reserve in the applied
     * state (0.1.6): the clamp walk replayed in registry order up
     * to the record — false for adopted records, for families
     * with no configured percent, and for cast records the clamp
     * skipped. The list lines read this so a summon that holds
     * nothing never claims a hold it does not carry.
     */
    public static boolean recordHolds(SummonRegistry reg, UUID owner, UUID entityId,
                                      Map<String, Integer> percents,
                                      int ceilingPercent, int floorPercent) {
        if (entityId == null) return false;
        int cap = appliedCap(ceilingPercent, floorPercent);
        int running = 0;
        for (SummonRecord rec : reg.ofOwner(owner)) {
            boolean target = entityId.equals(rec.entityId());
            if (!holdsReserve(rec)) {
                if (target) return false;
                continue;
            }
            int pct = percentFor(percents, rec.spellKey());
            if (pct <= 0) {
                if (target) return false;
                continue;
            }
            if (running + pct > cap) {
                if (target) return false;
                continue;
            }
            if (target) return true;
            running += pct;
        }
        return false;
    }

    /** Every modifier key this policy can ever ask for (the listener
     *  sweeps the mage's held keys against the desired set using
     *  this list, so a stale key from a changed count is removed). */
    public static java.util.List<String> allKeys(Map<String, Integer> percents,
                                                 Map<String, Integer> familyCaps) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (percents == null) return keys;
        for (String family : percents.keySet()) {
            int cap = familyCaps == null ? 1 : Math.max(1, familyCaps.getOrDefault(family, 1));
            for (int count = 1; count <= cap; count++) {
                String key = modifierKey(family, count);
                if (key != null) keys.add(key);
            }
        }
        return keys;
    }

    public enum Refusal { NONE, RESERVE_CEILING, RESERVE_FLOOR }

    /**
     * May this caster field one more summon (any branch) holding
     * {@code spellPercent}?
     *
     * CEILING: the summons' own total (current + new) may not pass
     * {@code ceilingPercent} — the full-set band Mike's anti-spam
     * intent prices at roughly a running toggle (default 75).
     * FLOOR: after this summon's hold, at least {@code floorPercent}
     * of the mage's BASE pool must remain usable, measured from the
     * mage's live effective max — so toggles the bridge doesn't own
     * (Cloak's −75% above all) shrink the headroom honestly instead
     * of stacking into a zero-mana lock the engine would not clamp.
     * Both numbers come from the mage at PreCast time; a refusal
     * cancels the cast and spends nothing, like the caps.
     */
    public static Refusal check(int currentPercent, int spellPercent, int ceilingPercent,
                                int effectiveMax, int baseMax, int floorPercent) {
        if (spellPercent <= 0) return Refusal.NONE;
        if (currentPercent + spellPercent > ceilingPercent) {
            return Refusal.RESERVE_CEILING;
        }
        if (baseMax > 0) {
            double usableAfter = effectiveMax - (spellPercent / 100.0) * baseMax;
            if (usableAfter < (floorPercent / 100.0) * baseMax) {
                return Refusal.RESERVE_FLOOR;
            }
        }
        return Refusal.NONE;
    }

    /** The RESERVE_CEILING text (§4.2), with the live numbers:
     *  what this spell would hold, what is already held, and the
     *  ceiling it is measured against. */
    public static String ceilingMessage(String spellDisplayName, int spellPercent,
                                        int currentPercent, int ceilingPercent) {
        return spellDisplayName + " would hold " + spellPercent + "% mana, but your "
                + "summons already hold " + currentPercent + "% of " + ceilingPercent
                + "% allowed. Dismiss a summon first.";
    }

    /** The RESERVE_FLOOR text (§4.2): toggles and summons both
     *  commit mana; the floor is named so the player can act. */
    public static String floorMessage(String spellDisplayName, int floorPercent) {
        return "Too much of your mana is already committed (toggles and summons) — a "
                + spellDisplayName + " would leave you under " + floorPercent
                + "% usable. Free some mana first.";
    }

    /** Full-context refusal message (§4.2). */
    public static String refusalMessage(Refusal r, String baseSpellKey, int spellPercent,
                                        int currentPercent, int ceilingPercent,
                                        int floorPercent) {
        String display = Nameplate.spellDisplayName(baseSpellKey);
        switch (r) {
            case RESERVE_CEILING:
                return ceilingMessage(display, spellPercent, currentPercent, ceilingPercent);
            case RESERVE_FLOOR:
                return floorMessage(display, floorPercent);
            default:
                return "";
        }
    }

    /** Bare refusal message (no numbers). Prefer the full-context
     *  overload — §4 requires the live numbers in the text. */
    public static String refusalMessage(Refusal r, String spellKey) {
        switch (r) {
            case RESERVE_CEILING:
                return "Your summons already hold as much of your mana as they can — "
                        + "dismiss one before calling another.";
            case RESERVE_FLOOR:
                return "Too much of your mana is already committed — "
                        + "you would have too little left to fight with.";
            default:
                return "";
        }
    }
}
