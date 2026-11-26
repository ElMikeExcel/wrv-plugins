package excelemfixes.meleepremium;

import com.magmaguy.elitemobs.api.EliteMobDamagedByPlayerEvent;
import com.magmaguy.elitemobs.dungeons.EliteMobsWorld;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.skills.WeaponIdentityResolver;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Module: melee premium (design ruling D195, Nov 22, 2026;
 * module home assigned by D198 — a balance module living in the
 * box, not a defect fix).
 *
 * THE DESIGN GAP: in EliteMobs worlds, a melee fighter pays for
 * every hit twice — once in the swing, and again in the contact
 * damage eaten at point-blank range, which the server's elite
 * ladder scales UP (natural elites deal x1.94, regional bosses
 * x2.0) while a ranged or magic fighter kites the same mob for
 * free. EliteMobs' damage multipliers are weapon-neutral, so
 * nothing compensates the melee fighter for standing in the
 * fire. The reference server's standing balance ruling (its
 * Magic-system ruling of Nov 8, 2026, mirrored here): sword
 * sits slightly ABOVE magic single-target, as a melee-range
 * risk premium. This module is that premium for EliteMobs
 * content: direct melee hits by players in EliteMobs-scope
 * worlds deal x1.15 (config: damage-multiplier).
 *
 * MECHANISM — chosen by rig A/B/C evidence on the exact stack
 * (Paper 26.2 + EliteMobs 10.9.10; see the build record):
 *  - A Bukkit-level pre-modifier (scaling the raw
 *    EntityDamageByEntityEvent before EliteMobs sees it) does
 *    NOT survive: EliteMobs' player-to-elite formula recomputes
 *    damage from the attacker's gear and levels and overwrites
 *    the Bukkit event's damage with its own result, so the
 *    premium either vanishes or lands outside the ledger.
 *  - A post-pipeline top-up (observe the HP drop, subtract the
 *    extra 15% directly, the arcane-bolt pattern) moves the HP
 *    but NOT EliteMobs' damager ledger: the premium damage is
 *    invisible to weapon XP, loot shares and kill credit —
 *    the ledger would keep pricing the un-premiumed hit.
 *  - THIS MODULE scales the damage INSIDE EliteMobs' own
 *    pipeline: it listens to EliteMobDamagedByPlayerEvent —
 *    which EliteMobs fires mid-handler, AFTER its formula and
 *    timed-modifier stages and BEFORE it reads the damage back
 *    to (a) credit the damager ledger and (b) write the final
 *    Bukkit damage — and multiplies the event's damage by the
 *    configured factor. One number flows everywhere: HP loss,
 *    damager ledger, weapon/class XP and loot shares all price
 *    the premiumed hit, exactly as if EliteMobs had computed
 *    it. No events are re-fired, no formula re-enters, and a
 *    killing blow is credited by the same single hit.
 * Non-elite targets in scope worlds have no EliteMobs pipeline
 * (their damage is stock vanilla); for those the module scales
 * the Bukkit event's BASE damage at HIGHEST — the vanilla-
 * consistent shape (defenses then apply to the stronger hit) —
 * skipping any target EliteMobs tracks as an elite, which the
 * API path already handled.
 *
 * INTERACTION WITH THE ELITE LADDER: the ladder scales MOB
 * damage dealt and MOB damage taken (MobCombatSettings, via
 * EliteMobs' own formula/config multipliers). The premium is a
 * PLAYER-side factor applied once, after EliteMobs' computed
 * damage (which already includes the ladder's damage-taken
 * factor for elites): total = EM formula x ladder-taken x
 * 1.15. It never re-enters the formula, never stacks with
 * itself (elite targets are skipped by the Bukkit path; the
 * API event fires once per hit), and changes nothing about
 * mob damage dealt.
 *
 * Scope: direct melee hits only (see MeleeClassifier — no
 * projectiles, no magic weapons, no sweeps, no custom damage),
 * in EliteMobs-scope worlds only (registry, config list, or
 * the "em_" name prefix). The overworld is never touched,
 * including elites that appear there.
 */
public final class MeleePremiumModule implements FixModule, Listener {

    private ModuleContext ctx;
    private boolean active;

    private double multiplier = MeleePremiumMath.DEFAULT_MULTIPLIER;
    private Set<String> configWorlds = Set.of();
    private String worldPrefix = "em_";
    private boolean debug;

    private boolean eliteMobsPresent;
    private boolean registryLinkFailed;

    @Override
    public String id() {
        return "melee-premium";
    }

    @Override
    public String displayName() {
        return "Melee Premium";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        eliteMobsPresent = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        active = true;
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        ctx.logger().info("Melee Premium ready - direct melee hits by players in "
                + "EliteMobs-scope worlds deal x" + fmt(multiplier)
                + " (elite hits scaled inside EliteMobs' pipeline so the damager "
                + "ledger and weapon XP price the same number).");
        if (!eliteMobsPresent) {
            ctx.logger().warning("EliteMobs not detected: the pipeline path and the "
                    + "world registry are unavailable; only non-elite hits in "
                    + "list/prefix worlds are premiumed.");
        }
    }

    @Override
    public void onDisable() {
        active = false;
        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        org.bukkit.configuration.ConfigurationSection cfg = ctx.moduleConfig();
        multiplier = MeleePremiumMath.sanitizeMultiplier(
                cfg == null ? MeleePremiumMath.DEFAULT_MULTIPLIER
                        : cfg.getDouble("damage-multiplier",
                                MeleePremiumMath.DEFAULT_MULTIPLIER));
        configWorlds = cfg == null ? Set.of() : new HashSet<>(cfg.getStringList("worlds"));
        worldPrefix = cfg == null ? "em_" : cfg.getString("world-prefix", "em_");
        debug = cfg != null && cfg.getBoolean("debug", false);
    }

    // ------------------------------------------------------------------
    // Scope
    // ------------------------------------------------------------------

    private boolean isWorldInScope(World world) {
        if (world == null) return false;
        boolean registrySays = false;
        if (eliteMobsPresent && !registryLinkFailed) {
            try {
                registrySays = EliteMobsWorld.isEliteMobsWorld(world.getUID());
            } catch (Throwable t) {
                registryLinkFailed = true;
                ctx.logger().warning("Melee Premium: EliteMobs world registry call "
                        + "failed (" + t + "); world scope falls back to the "
                        + "config list/prefix.");
            }
        }
        return MeleeScope.isWorldInScope(registrySays,
                configWorlds.contains(world.getName()),
                world.getName(), worldPrefix);
    }

    private boolean isMagicWeapon(Player player) {
        try {
            return WeaponIdentityResolver.isMagicWeapon(
                    player.getInventory().getItemInMainHand());
        } catch (Throwable ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Elite path: scale inside EliteMobs' own pipeline
    // ------------------------------------------------------------------

    /**
     * EliteMobs fires this event mid-handler: its damage formula
     * and timed modifiers have already run, and the value read
     * back from it afterwards is what EliteMobs credits in its
     * damager ledger (weapon/class XP, loot shares) and writes
     * as the hit's final Bukkit damage. Scaling here is the one
     * point where a single number flows to HP, ledger and XP.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEliteDamagedByPlayer(EliteMobDamagedByPlayerEvent event) {
        if (!active) return;
        try {
            EntityDamageByEntityEvent edbe = event.getEntityDamageByEntityEvent();
            if (edbe == null) return;
            Player player = event.getPlayer();
            if (player == null) return;
            EliteEntity elite = event.getEliteMobEntity();
            LivingEntity target = elite == null ? null : elite.getLivingEntity();
            if (target == null) return;
            if (!isWorldInScope(target.getWorld())) return;
            MeleeClassifier.DamagerKind kind =
                    edbe.getDamager() instanceof Player
                            ? MeleeClassifier.DamagerKind.DIRECT_PLAYER
                            : edbe.getDamager() instanceof Projectile
                                    ? MeleeClassifier.DamagerKind.PROJECTILE
                                    : MeleeClassifier.DamagerKind.OTHER;
            boolean melee = MeleeClassifier.isPremiumHit(
                    edbe.getCause().name(), kind, event.isRangedAttack(),
                    isMagicWeapon(player), event.getMagicHit() != null,
                    event.isCustomDamage());
            if (!melee) return;
            double before = event.getDamage();
            double after = MeleePremiumMath.premiumDamage(multiplier, before);
            if (after > before) {
                event.setDamage(after);
                debug("elite player=" + player.getName()
                        + " target=" + target.getType()
                        + " before=" + fmt(before) + " after=" + fmt(after)
                        + " world=" + target.getWorld().getName());
            }
        } catch (Throwable failure) {
            logThrottled("pipeline scaling", failure);
        }
    }

    // ------------------------------------------------------------------
    // Non-elite path: scale the vanilla hit in scope worlds
    // ------------------------------------------------------------------

    /**
     * Non-elite targets in scope worlds take stock vanilla
     * damage (EliteMobs' pipeline never sees them). Scale the
     * event's BASE damage at HIGHEST so defenses apply to the
     * stronger hit, exactly as they would to a stronger weapon.
     * Elite targets are skipped — the pipeline path above owns
     * them, so no hit is ever premiumed twice.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMeleeDamage(EntityDamageByEntityEvent event) {
        if (!active) return;
        try {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
            if (!(event.getEntity() instanceof LivingEntity target)) return;
            if (!(event.getDamager() instanceof Player player)) return;
            if (!isWorldInScope(target.getWorld())) return;
            if (eliteMobsPresent) {
                try {
                    if (EntityTracker.getEliteMobEntity(target) != null) return;
                } catch (Throwable ignored) {
                    // Tracker unavailable: fall through — the API
                    // path keys on the same tracker, so a target
                    // the tracker cannot see was not premiumed.
                }
            }
            boolean melee = MeleeClassifier.isPremiumHit(
                    event.getCause().name(),
                    MeleeClassifier.DamagerKind.DIRECT_PLAYER,
                    false, isMagicWeapon(player), false, false);
            if (!melee) return;
            double baseBefore = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
            if (!(baseBefore > 0.0)) return;
            double baseAfter = MeleePremiumMath.premiumDamage(multiplier, baseBefore);
            if (baseAfter > baseBefore) {
                event.setDamage(EntityDamageEvent.DamageModifier.BASE, baseAfter);
                debug("non-elite player=" + player.getName()
                        + " target=" + target.getType()
                        + " baseBefore=" + fmt(baseBefore)
                        + " baseAfter=" + fmt(baseAfter)
                        + " world=" + target.getWorld().getName());
            }
        } catch (Throwable failure) {
            logThrottled("vanilla scaling", failure);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void debug(String message) {
        if (debug) ctx.logger().info("MELEE-PREMIUM " + message);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private long lastFailureLogNanos;

    private void logThrottled(String where, Throwable failure) {
        long now = System.nanoTime();
        if (now - lastFailureLogNanos < 60_000_000_000L) return;
        lastFailureLogNanos = now;
        ctx.logger().warning("Melee Premium: failure during " + where + " ("
                + failure.getClass().getSimpleName() + ": " + failure.getMessage()
                + "); module continues.");
    }
}
