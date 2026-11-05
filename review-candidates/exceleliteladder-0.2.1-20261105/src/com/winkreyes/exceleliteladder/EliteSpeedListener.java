package com.winkreyes.exceleliteladder;

import com.magmaguy.elitemobs.api.EliteMobEnterCombatEvent;
import com.magmaguy.elitemobs.api.EliteMobSpawnEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.winkreyes.exceleliteladder.core.EliteSpeedSettings;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * ExcelEliteLadder 0.2.1 — applies the elite movement-speed buff (a Speed
 * potion EFFECT, EliteMobs' own {@code movement_speed} channel).
 *
 * This class is loaded and registered ONLY when EliteMobs is enabled
 * (EliteMobs is a softdepend; with EM absent the plugin behaves exactly as
 * 0.2.0 minus this listener — no class loading, no errors).
 *
 * Triggers:
 *   1. EliteMobSpawnEvent — 1 tick later (mirrors EM's own run_after(1), so
 *      EM finishes its spawn setup first), natural elites only.
 *   2. EliteMobEnterCombatEvent — re-assert: a natural elite that somehow
 *      lost the effect (milk-bucket equivalents, other plugins) regains it
 *      when it next enters combat. Event-driven, zero polling.
 *
 * There is deliberately NO chunk-load catch-up trigger: EliteMobs 10.9.12
 * exposes no registry lookup that resolves a Bukkit Entity back to an
 * EliteEntity (verified against the jar at build — no EliteEntityManager,
 * no static registry, EliteMindService only accepts an EliteEntity you
 * already hold). Toggling the feature on therefore covers new spawns and
 * combat re-asserts; already-alive elites pick the buff up at their next
 * combat. Recorded in CHANGELOG-0.2.1.md.
 *
 * An elite that already HAS a Speed effect is never touched: EM's own
 * movement_speed power (Speed II) outranks ours and must not be downgraded,
 * and a player-splashed Speed must not be overwritten either.
 */
public final class EliteSpeedListener implements Listener {

    private final ExcelEliteLadder plugin;
    private final Supplier<EliteSpeedSettings> settings;

    public EliteSpeedListener(ExcelEliteLadder plugin, Supplier<EliteSpeedSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEliteSpawn(EliteMobSpawnEvent e) {
        EliteEntity elite = e.getEliteMobEntity();
        LivingEntity living = e.getEntity();
        if (elite == null || living == null) {
            return;
        }
        if (!settings.get().shouldBuff(true, elite.isNaturalEntity())) {
            return;
        }
        // One tick later, like EM's own power: let EM finish its spawn setup,
        // then re-check the mob is still there before touching it.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> apply(living, "spawn"), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnterCombat(EliteMobEnterCombatEvent e) {
        EliteEntity elite = e.getEliteMobEntity();
        if (elite == null) {
            return;
        }
        LivingEntity living = elite.getLivingEntity();
        if (living == null) {
            return;
        }
        if (!settings.get().shouldBuff(true, elite.isNaturalEntity())) {
            return;
        }
        apply(living, "combat re-assert");
    }

    private void apply(LivingEntity living, String why) {
        EliteSpeedSettings s = settings.get();
        if (!s.enabled() || !living.isValid() || living.isDead()) {
            return;
        }
        if (living.hasPotionEffect(PotionEffectType.SPEED)) {
            return; // never downgrade or overwrite an existing Speed effect
        }
        living.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, s.durationTicks(), s.amplifier(),
                EliteSpeedSettings.EFFECT_AMBIENT, EliteSpeedSettings.EFFECT_PARTICLES,
                EliteSpeedSettings.EFFECT_ICON));
        Location loc = living.getLocation();
        plugin.getLogger().info("Elite speed applied (" + why + "): " + living.getType()
                + " at " + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + " — " + s.describe() + ".");
    }
}
