package excelemfixes.witherskulls;

/**
 * The whole drop decision as one pure table, so every edge case in
 * the spec is a test row instead of a hope: the Bukkit glue gathers
 * the facts (entity type, the EliteMobs seam's answer, the killer,
 * the drops list, the killer's Looting, a roll) and this class
 * returns the verdict. Verdict order matters and is pinned by the
 * tests — cheap structural rejections first, the roll last.
 */
public final class TopUpDecider {

    public enum Verdict {
        /** Add exactly one skull to the drops. */
        ADD_SKULL,
        /** Plugin switched off in config. */
        DISABLED,
        /** The dead entity is not a wither skeleton. */
        NOT_WITHER_SKELETON,
        /** Wither skeleton, but not an EliteMobs elite. */
        NOT_ELITE,
        /** No player is credited with the kill (environment, a
         *  dispenser projectile, another mob...). */
        NOT_PLAYER_KILL,
        /** Vanilla already put a skull in the drops — never two. */
        SKULL_ALREADY_PRESENT,
        /** Target at or below vanilla's effective chance for this
         *  kill; the plugin only ever adds. */
        TARGET_NOT_ABOVE_VANILLA,
        /** The top-up roll missed. */
        ROLL_MISS
    }

    private TopUpDecider() {
    }

    /**
     * @param rollPercent this kill's top-up roll, drawn uniformly
     *                    from [0, 100) by the caller (production:
     *                    ThreadLocalRandom; tests: pinned values)
     */
    public static Verdict decide(boolean enabled,
                                 boolean witherSkeleton,
                                 boolean elite,
                                 boolean playerKill,
                                 boolean skullAlreadyPresent,
                                 double targetPercent,
                                 int lootingLevel,
                                 double rollPercent) {
        if (!enabled) return Verdict.DISABLED;
        if (!witherSkeleton) return Verdict.NOT_WITHER_SKELETON;
        if (!elite) return Verdict.NOT_ELITE;
        if (!playerKill) return Verdict.NOT_PLAYER_KILL;
        if (skullAlreadyPresent) return Verdict.SKULL_ALREADY_PRESENT;
        double topUp = SkullMath.topUpChancePercent(targetPercent, lootingLevel);
        if (topUp <= 0.0) return Verdict.TARGET_NOT_ABOVE_VANILLA;
        if (!SkullMath.rollSucceeds(topUp, rollPercent)) return Verdict.ROLL_MISS;
        return Verdict.ADD_SKULL;
    }
}
