package excelemfixes.mcmmo;

import com.magmaguy.elitemobs.items.ClassLootCoverage;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import com.magmaguy.elitemobs.tagger.PersistentTagger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;

/**
 * Bridge to EliteMobs' own identity for a Bukkit entity,
 * through EliteMobs' own tagger (the same lookup the
 * jobs-dummy-gate module uses): whether the entity is
 * EliteMobs-tracked, its elite level, its class-loot rank
 * (resolved by EliteMobs' own {@link ClassLootCoverage}, so
 * the module prices ranks exactly the way EliteMobs itself
 * derives them, AUTO rules included), its boss filename, and
 * the two config fields the passive-practice-target rule
 * reads ({@code ai}, {@code damageMultiplier}).
 *
 * <p>Kept in its own class (the pack's bridge pattern) so the
 * EliteMobs internals live in exactly one place. This class is
 * only constructed when the EliteMobs plugin is present. Any
 * failure — internals shifted, fields unreadable — resolves
 * fail-low: untracked, or (for a tracked custom boss) rank
 * TRASH via {@link AwardRates#fromEmRankName}, never a guess
 * upward.
 */
public final class EmIdentity implements ElitePort {

    private final Logger logger;
    private final AtomicBoolean warned = new AtomicBoolean();

    public EmIdentity(Logger logger) {
        this.logger = logger;
    }

    @Override
    public EliteInfo info(Entity entity) {
        if (entity == null) return EliteInfo.untracked();
        try {
            EliteEntity elite = PersistentTagger.getEliteEntity(entity);
            if (elite == null) return EliteInfo.untracked();
            int level = elite.getLevel();
            if (elite instanceof CustomBossEntity boss
                    && boss.getCustomBossesConfigFields() != null) {
                String rankName = null;
                try {
                    Object rank = ClassLootCoverage.rank(boss);
                    if (rank != null) rankName = rank.toString();
                } catch (Throwable ignored) {
                    // Rank unreadable: rankName stays null and the
                    // award prices at TRASH (fail-low).
                }
                return new EliteInfo(true, true, level, rankName,
                        boss.getCustomBossesConfigFields().getFilename(),
                        boss.getCustomBossesConfigFields().isAi(),
                        boss.getCustomBossesConfigFields()
                                .getDamageMultiplier());
            }
            return new EliteInfo(true, false, level, null, null, true, 1.0);
        } catch (Throwable t) {
            if (warned.compareAndSet(false, true)) {
                logger.warning("mcMMO XP Connector: EliteMobs identity "
                        + "probe failed (" + t.getClass().getSimpleName()
                        + "); entities will be treated as untracked until "
                        + "this is resolved.");
            }
            return EliteInfo.untracked();
        }
    }
}
