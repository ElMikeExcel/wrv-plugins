package excelemfixes.casterxp;

/**
 * The caster-xp-sharing module's pure rules: no Bukkit and no
 * EliteMobs types live here, so the sharing arithmetic is
 * unit-testable on a plain JVM, the pack's standing pattern.
 *
 * The background, in one paragraph: EliteMobs' Advanced
 * Combat System gives every class form two "foundation
 * skills" and sets the class level to the LOWER of the two
 * (its FoundationSkillPair limiting rule). For Spellcaster
 * the pair is WANDS and STAVES, and the two skills earn XP
 * only from their own weapon — a wand main earns zero staff
 * XP — so the class pauses at the lagging skill's level and
 * EliteMobs discards ("does not bank") the overflow class
 * XP. EliteMobs exposes no setting for any of this (alpha,
 * fixed baseline). This module cannot edit EliteMobs'
 * formula; it grants offsetting skill XP into the lagging
 * track through EliteMobs' own public PlayerData calls, per
 * the mode the player picked.
 */
public final class CasterXpMath {

    private CasterXpMath() {}

    /** Skill type names, spelled as EliteMobs' SkillType
     *  enum spells them (the bridge translates). */
    public static final String WANDS = "WANDS";
    public static final String STAVES = "STAVES";

    /** How a player's caster XP is shared between the two
     *  foundation tracks. */
    public enum Mode {
        /** EliteMobs default: no intervention at all. */
        STOCK,
        /** Each gain is split across both tracks (the gained
         *  track keeps the ceiling half, the other track gets
         *  the floor half — the total is preserved). */
        SHARED,
        /** Gains on the leading track also grant the same
         *  amount into the lagging track, so the gap never
         *  widens and the class minimum rises with the
         *  player's main weapon. */
        CARRY;

        /** Parses a config / command / stored value; unknown
         *  values fall back to STOCK (never intervene on a
         *  value we do not understand). */
        public static Mode parse(String text) {
            if (text == null) return STOCK;
            try {
                return Mode.valueOf(text.strip().toUpperCase());
            } catch (IllegalArgumentException e) {
                return STOCK;
            }
        }
    }

    /** True for the two caster foundation skills. */
    public static boolean isCasterSkill(String typeName) {
        return WANDS.equals(typeName) || STAVES.equals(typeName);
    }

    /** The other half of the caster pair. */
    public static String otherSkill(String typeName) {
        return WANDS.equals(typeName) ? STAVES : WANDS;
    }

    // ------------------------------------------------------------ SHARED

    /**
     * SHARED split of one gain: {kept on the gained track,
     * granted to the other track}. The gained track keeps
     * the ceiling half so a 1-XP gain is never halved into
     * nothing; the two parts always sum to the gain (the
     * mode is XP-neutral — it redistributes, it does not
     * create or destroy).
     */
    public static long[] splitGain(long gain) {
        if (gain <= 0) return new long[]{0, 0};
        long toOther = gain / 2;
        return new long[]{gain - toOther, toOther};
    }

    // ------------------------------------------------------------- CARRY

    /**
     * CARRY mirror for one gain on one track: how much XP to
     * grant into the OTHER track so the pre-gain gap never
     * widens. The gap is measured as (gained track − other
     * track); only the gain's contribution to a POSITIVE gap
     * is mirrored:
     *   - gain on the leading track: the whole gain mirrors.
     *   - gain on the lagging track: nothing mirrors (the
     *     gain only narrows the gap — the class minimum is
     *     already rising on its own).
     *   - gain on the lagging track that overshoots into the
     *     lead: only the overshoot mirrors, leaving the two
     *     tracks tied.
     * Gains never shrink a track, so the mirror is never
     * negative.
     */
    public static long carryMirror(long gainedBefore, long otherBefore,
                                   long gain) {
        if (gain <= 0) return 0;
        long gapBefore = gainedBefore - otherBefore;
        long gapAfter = gapBefore + gain;
        long widening = Math.max(0L, gapAfter) - Math.max(0L, gapBefore);
        return Math.max(0L, widening);
    }

    // ---------------------------------------------------------- catch-up

    /** A one-time catch-up plan: which track lags and by how
     *  much. {@code lagging} is null when the tracks are
     *  already equal (amount 0). */
    public record CatchUp(String lagging, long amount) {}

    /**
     * The catch-up that closes an existing gap: raise the
     * lagging track to the leading track's total XP.
     */
    public static CatchUp catchUp(long wandsXp, long stavesXp) {
        if (wandsXp == stavesXp) return new CatchUp(null, 0);
        return wandsXp > stavesXp
                ? new CatchUp(STAVES, wandsXp - stavesXp)
                : new CatchUp(WANDS, stavesXp - wandsXp);
    }
}
