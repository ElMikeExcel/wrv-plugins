package excelemfixes.mcmmo;

import org.bukkit.entity.Entity;

/**
 * The module's window into EliteMobs, behind an interface so
 * the engine never touches an EliteMobs type: the implementing
 * class is only constructed after the module has confirmed the
 * EliteMobs plugin is present (the pack's bridge discipline —
 * a module whose partner plugin is absent must idle without
 * loading the partner's classes).
 */
public interface ElitePort {

    /** What the module needs to know about one entity. */
    record EliteInfo(boolean tracked, boolean customBoss, int level,
                     String rankName, String filename, boolean ai,
                     double damageMultiplier) {

        static EliteInfo untracked() {
            return new EliteInfo(false, false, 0, null, null, true, 1.0);
        }

        /**
         * A passive practice target: a custom boss whose config
         * cannot fight back ({@code ai: false} and
         * {@code damageMultiplier: 0.0}). Structural twin of the
         * training-dummy rule — it covers every pack's practice
         * shapes the day they ship, not just known filenames.
         */
        boolean passivePracticeTarget() {
            return customBoss && !ai && damageMultiplier == 0.0;
        }

        /** The award class for this entity. */
        AwardRates.RankClass rankClass() {
            if (!customBoss) return AwardRates.RankClass.NATURAL;
            return AwardRates.fromEmRankName(rankName);
        }
    }

    /** EliteMobs' identity for this entity; untracked when not EM's. */
    EliteInfo info(Entity entity);
}
