package excelemfixes.mcmmo;

/**
 * Kill-award pricing for the mcMMO XP Connector: the award for
 * killing an EliteMobs entity is {@code rate(rank) x elite level},
 * never a function of the entity's health pool. That is the whole
 * point of the rebalance this module implements — mcMMO's native
 * pricing pays per point of damage, so an EliteMobs-sized pool
 * (millions of HP) pays millions of XP; pricing the kill instead
 * of the damage makes the pool irrelevant.
 *
 * <p>Rank classes are EliteMobs' own class-loot taxonomy for
 * custom bosses (TRASH / MINIBOSS / BOSS), plus NATURAL for
 * natural elites, which carry no class-loot rank in EliteMobs
 * (class loot is boss-only) and so get their own class here.
 *
 * <p>Fail-low discipline: an unreadable or unknown rank prices
 * at TRASH, and an unreadable level (level &lt;= 0) prices at 0.
 * An award is a bonus channel; uncertainty never prices it up.
 */
public final class AwardRates {

    /** The award classes, priced by {@link Rates}. */
    public enum RankClass {
        BOSS, MINIBOSS, TRASH, NATURAL
    }

    /** The four per-level rates (XP per elite level). */
    public record Rates(int boss, int miniboss, int natural, int trash) {

        /** The approved design defaults. */
        public static Rates defaults() {
            return new Rates(100, 40, 20, 10);
        }

        public int rateFor(RankClass rankClass) {
            return switch (rankClass) {
                case BOSS -> boss;
                case MINIBOSS -> miniboss;
                case NATURAL -> natural;
                case TRASH -> trash;
            };
        }
    }

    private AwardRates() {
    }

    /**
     * Maps an EliteMobs class-loot rank name (as returned by
     * EliteMobs' own rank resolver) to a rank class. Anything
     * unrecognised — including null — prices at TRASH (fail-low).
     * NATURAL is never produced here: it is the caller's class
     * for entities that are not custom bosses at all.
     */
    public static RankClass fromEmRankName(String rankName) {
        if (rankName == null) return RankClass.TRASH;
        return switch (rankName.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "BOSS" -> RankClass.BOSS;
            case "MINIBOSS" -> RankClass.MINIBOSS;
            default -> RankClass.TRASH;
        };
    }

    /** The award for one kill: rate x level; level &lt;= 0 pays 0. */
    public static int award(RankClass rankClass, int level, Rates rates) {
        if (rankClass == null || rates == null || level <= 0) return 0;
        return rates.rateFor(rankClass) * level;
    }
}
