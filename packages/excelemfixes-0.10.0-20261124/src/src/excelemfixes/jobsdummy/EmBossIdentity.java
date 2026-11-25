package excelemfixes.jobsdummy;

import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.CustomBossEntity;
import com.magmaguy.elitemobs.tagger.PersistentTagger;
import org.bukkit.entity.Entity;

/**
 * Bridge to EliteMobs' own identity for a Bukkit entity: the
 * boss filename of the custom boss it is, or null when the
 * entity is not an EliteMobs custom boss.
 *
 * <p>Resolution goes through EliteMobs' own PersistentTagger —
 * the same lookup EliteMobs itself uses — which reads the elite
 * UUID EliteMobs stamps into the entity's persistent data and
 * finds the tracked EliteEntity. Nothing here looks at entity
 * type: a vanilla husk resolves to null and is never vetoed.
 *
 * <p>Kept in its own class (the pack's bridge pattern) so the
 * EliteMobs internals live in exactly one place; any failure —
 * EliteMobs absent, internals shifted — resolves to null, which
 * the gate reads as "not a listed dummy: let the payment stand".
 */
final class EmBossIdentity {

    private EmBossIdentity() {
    }

    /** The EliteMobs boss filename for this entity, or null. */
    static String bossFilename(Entity entity) {
        if (entity == null) return null;
        try {
            EliteEntity elite = PersistentTagger.getEliteEntity(entity);
            if (elite instanceof CustomBossEntity boss
                    && boss.getCustomBossesConfigFields() != null) {
                return boss.getCustomBossesConfigFields().getFilename();
            }
        } catch (Throwable ignored) {
            // EM internals shifted: no identity, no veto.
        }
        return null;
    }
}
