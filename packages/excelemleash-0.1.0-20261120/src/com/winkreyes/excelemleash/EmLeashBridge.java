package com.winkreyes.excelemleash;

import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.easyminecraftgoals.events.WanderBackToPointEndEvent;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * All EliteMobs-touching code for ExcelEMLeash. This class is only
 * loaded when EliteMobs is present (the main class checks first), so
 * its EM imports never resolve against a server without EliteMobs.
 *
 * Leg A: WanderBackToPointEndEvent is the event EliteMobs fires when
 * a leash return completes (its own Navigation.endLeashReturn listens
 * to it for the full heal). Nothing in EM's leash path ever clears the
 * boss's target — that is the loop Jester reported. On the event we
 * clear exactly what EM's own combat-reset clears, no more:
 *   - the Bukkit target (Mob.setTarget(null)), and
 *   - EliteEntity.clearDamagers(), EM's purpose-built reset bundle
 *     (damagers, aggro/threat, skill damage contributions,
 *     contribution sessions, and the forced-target lease).
 * EM keeps: health (it full-heals on return end, by design), the
 * inCombat flag/watchdog (its own state machine exits combat on its
 * schedule), spawn point, powers, follow range (EM restores it).
 *
 * Leg B: CustomBossesConfig.getCustomBosses() is EM's live registry
 * of loaded boss config objects. CustomBossesConfigFields.setLeashRadius
 * is a bare in-memory field write (bytecode-verified on the pinned
 * 10.9.14 jar; the persist-to-file variant is runtimeSetLeashRadius,
 * deliberately not used) — this plugin never writes boss files.
 * RegionalBossEntity reads the fields value when it spawns, so the
 * floored values govern every future (re)spawn.
 */
public final class EmLeashBridge implements Listener {

    private final ExcelEMLeash plugin;
    private final ExcelEMLeash.Settings settings;
    private boolean appliedOnce = false;

    public EmLeashBridge(ExcelEMLeash plugin, ExcelEMLeash.Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    // ---- Leg A: aggro-clear ----

    @EventHandler
    public void onWanderBackToPointEnd(WanderBackToPointEndEvent event) {
        LivingEntity living = event.getLivingEntity();
        if (living == null) return;
        EliteEntity elite;
        try {
            elite = EntityTracker.getEliteMobEntity(living);
        } catch (Throwable t) {
            plugin.debug("LEASH return end: tracker lookup failed for " + living.getType() + ": " + t);
            return;
        }
        boolean regional = elite instanceof RegionalBossEntity;
        AggroClearPolicy.Decision decision =
                AggroClearPolicy.decide(settings.aggroClearEnabled, elite != null, regional);
        if (decision != AggroClearPolicy.Decision.CLEAR) {
            plugin.debug("LEASH return end for " + living.getType() + " — no clear (" + decision + ")");
            return;
        }
        String bossName = ((RegionalBossEntity) elite).getCustomBossesConfigFields().getFilename();
        if (living instanceof Mob) ((Mob) living).setTarget(null);
        elite.clearDamagers();
        plugin.debug("LEASH aggro cleared: " + bossName + " stays home (hardObjective="
                + event.isHardObjective() + ")");
    }

    // ---- Leg B: radii management ----

    /**
     * Applies the floor + overrides to EM's loaded boss configs.
     * Writes only where the computed target differs from the current
     * value, so repeat passes are silent no-ops unless EM reloaded
     * its configs (or a pack update landed) underneath us.
     */
    public synchronized void applyRadii() {
        Map<String, ? extends CustomBossesConfigFields> bosses = CustomBossesConfig.getCustomBosses();
        if (bosses == null || bosses.isEmpty()) {
            if (!appliedOnce) {
                plugin.getLogger().warning("LEASH radii: EliteMobs boss registry is empty — nothing applied yet.");
            }
            appliedOnce = true;
            return;
        }
        int seen = 0, leashed = 0, floorRaised = 0, overridesInForce = 0, changed = 0;
        for (Map.Entry<String, ? extends CustomBossesConfigFields> entry : bosses.entrySet()) {
            CustomBossesConfigFields fields = entry.getValue();
            if (fields == null) continue;
            seen++;
            String filename = fields.getFilename() != null ? fields.getFilename() : entry.getKey();
            double current = fields.getLeashRadius();
            if (current > 0) leashed++;
            Double override = lookupOverride(filename, entry.getKey());
            if (override != null) overridesInForce++;
            RadiusPolicy.ChangeKind kind = RadiusPolicy.classify(override, current, settings.minimumRadius);
            if (kind != RadiusPolicy.ChangeKind.NONE) {
                double target = RadiusPolicy.effectiveTarget(override, current, settings.minimumRadius);
                fields.setLeashRadius(target);
                changed++;
                if (kind == RadiusPolicy.ChangeKind.FLOOR_RAISE) floorRaised++;
                plugin.debug("LEASH " + filename + ": leashRadius " + trim(current) + " -> " + trim(target)
                        + " (" + (kind == RadiusPolicy.ChangeKind.OVERRIDE ? "override" : "floor") + ")");
            }
        }
        if (!appliedOnce || changed > 0) {
            plugin.getLogger().info("LEASH radii applied: bosses seen " + seen + ", with leash " + leashed
                    + ", floor " + trim(settings.minimumRadius) + ": raised " + floorRaised
                    + ", overrides in force " + overridesInForce + ", values changed " + changed);
        }
        appliedOnce = true;
    }

    private Double lookupOverride(String filename, String mapKey) {
        Double hit = settings.overrides.get(filename);
        if (hit != null) return hit;
        if (mapKey != null && !mapKey.equals(filename)) hit = settings.overrides.get(mapKey);
        if (hit != null) return hit;
        // Tolerate path-prefixed keys on either side.
        String base = filename;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) base = base.substring(slash + 1);
        return settings.overrides.get(base);
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }
}
