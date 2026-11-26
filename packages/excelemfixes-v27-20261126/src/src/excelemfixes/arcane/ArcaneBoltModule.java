package excelemfixes.arcane;

import com.magmaguy.elitemobs.advancedcombat.abilities.AbilityEffect;
import com.magmaguy.elitemobs.advancedcombat.abilities.ClassAbilityAffectEliteEvent;
import com.magmaguy.elitemobs.api.EliteMobDamagedByPlayerEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Fix module: class-ability damage gear scaling (upstream EliteMobs
 * ACS gap; player-visible symptom: Arcane Bolt and the other class
 * abilities fall behind basic attacks as the caster's gear improves).
 *
 * WHAT IS ACTUALLY BROKEN (measured on EliteMobs 10.9.14, Nov 2026):
 * ability damage is computed with the weapon/gear adjustment
 * hardcoded to 1.0 (AdvancedDamageScaling.classAbility), so a
 * signature ability gains nothing from the caster's gear while
 * weapon hits do. Upstream agrees this is a gap and is designing
 * gear scaling for abilities (EliteMobs issue #243); until it
 * ships, this module adds the missing scale:
 *
 *     extra = dealt x (ability-damage-scale - 1)
 *
 * where "dealt" is the final amount on EliteMobs' own damage event
 * for the hit - i.e. the damage after everything EliteMobs itself
 * applied, exactly the number the player sees the elite lose.
 *
 * WHAT IS *NOT* BROKEN ANY MORE (the 0.11.0 correction): the
 * original version of this module (0.1.0-0.10.0) also re-applied the
 * caster's timed outgoing-damage buffs (Spell Echo), based on a
 * November 2026 decompile reading that ability damage bypassed
 * EliteMobs' damage events entirely. That reading was wrong.
 * Measured on 10.9.14 with real casts: ability damage DOES flow
 * through EliteMobDamagedByPlayerEvent, and EliteMobs applies
 * Spell Echo inside it (a 1.154x modifier landed as exactly 1.154x
 * damage, tagged with the buff as its source). Upstream's author
 * reported the same from his own testing (issue #243). Re-applying
 * the buff here would count it twice, so the buff half is removed:
 * this module never touches buffs.
 *
 * TRIGGER (also new in 0.11.0): the original trigger observed the
 * ability's carrier projectile hitting the elite
 * (ProjectileHitEvent). On servers running FreeMinecraftModels -
 * which the mage class requires - EliteMobs delivers projectile
 * impacts through its modeled-impact path and the Bukkit
 * projectile-hit event never fires, so the old module silently
 * never triggered there. The module now pairs EliteMobs' own
 * events instead: ClassAbilityAffectEliteEvent (fires immediately
 * before an ability's damage step) marks the (caster, elite) pair,
 * and the matching EliteMobDamagedByPlayerEvent - custom damage
 * only, so weapon hits can never qualify - supplies the hit.
 *
 * KNOWN LIMITATION (unchanged, and called out by upstream): the
 * top-up is a direct health/absorption adjustment. It fires no
 * damage event, so the top-up share earns no damage credit - no
 * loot share and no threat - inside EliteMobs' accounting; kill
 * credit stays with the ability's own damage. Dealing the top-up
 * as real event damage instead would send it back through
 * EliteMobs' weapon-damage formula and timed modifiers, which
 * would distort it (and could re-apply buffs a second time), so
 * the direct adjustment stays, documented.
 *
 * Scope: ability damage steps against elites. Non-elite targets
 * are untouched.
 */
public final class ArcaneBoltModule implements FixModule, Listener {

    /** Ticks an affect-event mark stays valid for its damage event.
     *  The damage step follows the affect event within a tick or
     *  two; 10 ticks (half a second) leaves generous slack for
     *  multi-step abilities without letting a stale mark linger. */
    private static final int PAIRING_WINDOW_TICKS = 10;

    private ModuleContext ctx;
    private boolean active;

    private double abilityDamageScale = 1.0;
    private double maxExtraFractionOfMaxHealth = 1.0;
    private boolean debugLog;
    private boolean buffKeyWasEnabled;

    private final AbilityHitPairing pairing = new AbilityHitPairing(PAIRING_WINDOW_TICKS);

    @Override
    public String id() {
        return "arcane-bolt";
    }

    @Override
    public String displayName() {
        return "Arcane Bolt Balance";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        Plugin eliteMobsPlugin = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (eliteMobsPlugin == null || !eliteMobsPlugin.isEnabled()) {
            ctx.logger().warning("Arcane Bolt Balance: EliteMobs not "
                    + "found or not enabled; module is inert.");
            return;
        }
        active = true;
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        ctx.logger().info("Arcane Bolt Balance ready - ability damage scale "
                + abilityDamageScale + " (gear-gap scale only; EliteMobs "
                + "applies timed buffs like Spell Echo itself).");
        if (buffKeyWasEnabled) {
            ctx.logger().warning("Arcane Bolt Balance: the config key "
                    + "buff-application-enabled is deprecated and ignored. "
                    + "EliteMobs applies timed outgoing-damage buffs to "
                    + "ability damage itself (measured; upstream EliteMobs "
                    + "issue #243), so this module no longer re-applies "
                    + "them - leaving the key on changes nothing.");
        }
    }

    @Override
    public void onDisable() {
        active = false;
        HandlerList.unregisterAll(this);
        pairing.clear();
    }

    @Override
    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        ConfigurationSection section = ctx.moduleConfig();
        if (section == null) return;
        abilityDamageScale = section.getDouble("ability-damage-scale", abilityDamageScale);
        maxExtraFractionOfMaxHealth = section.getDouble(
                "max-extra-fraction-of-max-health", maxExtraFractionOfMaxHealth);
        debugLog = section.getBoolean("debug-log", debugLog);
        // Deprecated key, read only so an old config that still
        // enables it gets the one-time warning at startup.
        buffKeyWasEnabled = section.getBoolean("buff-application-enabled", false);
    }

    // ------------------------------------------------------------------
    // Hit observation: pair EliteMobs' own ability events
    // ------------------------------------------------------------------

    /**
     * EliteMobs fires this immediately before an ability's effect
     * lands on an elite. A DAMAGE effect means a damage step from
     * this caster is about to hit this elite; mark the pair.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onAffectElite(ClassAbilityAffectEliteEvent event) {
        if (!active) return;
        try {
            if (event.getEffect() != AbilityEffect.DAMAGE) return;
            EliteEntity elite = event.getEliteEntity();
            if (elite == null || elite.getLivingEntity() == null) return;
            pairing.mark(event.getCaster().getUniqueId(),
                    elite.getLivingEntity().getUniqueId(),
                    Bukkit.getCurrentTick());
        } catch (Throwable failure) {
            logThrottled("affect observation", failure);
        }
    }

    /**
     * The ability's damage step itself. Custom damage + a live pair
     * mark identifies it as the marked cast's hit; the event's final
     * damage is the dealt amount (it already includes EliteMobs' own
     * buff application). The top-up lands next tick, after the hit's
     * own health change has settled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEliteDamaged(EliteMobDamagedByPlayerEvent event) {
        if (!active) return;
        try {
            if (!event.isCustomDamage()) return;
            EliteEntity elite = event.getEliteMobEntity();
            if (elite == null) return;
            LivingEntity target = elite.getLivingEntity();
            if (target == null) return;
            Player caster = event.getPlayer();
            if (caster == null) return;
            if (!pairing.isAuthorized(caster.getUniqueId(),
                    target.getUniqueId(), Bukkit.getCurrentTick())) return;
            double dealt = event.getDamage();
            if (!(dealt > 0.0)) return;
            UUID casterId = caster.getUniqueId();
            Bukkit.getScheduler().runTask(ctx.plugin(),
                    () -> applyCompensation(casterId, target, dealt));
        } catch (Throwable failure) {
            logThrottled("damage observation", failure);
        }
    }

    // ------------------------------------------------------------------
    // Compensation
    // ------------------------------------------------------------------

    /**
     * Applies the top-up for one measured ability hit and returns the
     * extra damage dealt. Public so test drivers can exercise the
     * exact production path.
     */
    public double applyCompensation(UUID casterId, LivingEntity target, double dealt) {
        if (!active || target == null || target.isDead()) return 0.0;
        double extra = CompensationMath.extraDamage(dealt, abilityDamageScale);
        if (extra <= 0.0) return 0.0;
        double maxHealth = maxHealth(target);
        if (maxHealth > 0.0) {
            extra = Math.min(extra, maxHealth * Math.max(0.0, maxExtraFractionOfMaxHealth));
        }
        extra = Math.min(extra, effectiveHp(target));
        if (extra <= 0.0) return 0.0;
        double[] split = CompensationMath.applySplit(
                target.getAbsorptionAmount(), target.getHealth(), extra);
        target.setAbsorptionAmount(split[0]);
        target.setHealth(split[1]);
        if (debugLog) {
            ctx.logger().info(String.format(
                    "ARCANE-BOLT dealt=%.2f scale=%.2f extra=%.2f elite=%s caster=%s",
                    dealt, abilityDamageScale, extra,
                    target.getUniqueId(), casterId));
        }
        return extra;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    static double effectiveHp(LivingEntity entity) {
        return Math.max(0.0, entity.getHealth()) + Math.max(0.0, entity.getAbsorptionAmount());
    }

    private static double maxHealth(LivingEntity entity) {
        var attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 0.0 : attribute.getValue();
    }

    private long lastFailureLogNanos;

    private void logThrottled(String where, Throwable failure) {
        long now = System.nanoTime();
        if (now - lastFailureLogNanos < 60_000_000_000L) return;
        lastFailureLogNanos = now;
        ctx.logger().warning("Arcane Bolt Balance: failure during " + where + " ("
                + failure.getClass().getSimpleName() + ": " + failure.getMessage()
                + "); module continues.");
    }
}
