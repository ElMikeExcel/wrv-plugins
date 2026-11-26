package excelemfixes.meleemitigation;

import com.magmaguy.elitemobs.dungeons.EliteMobsWorld;
import com.magmaguy.elitemobs.skills.WeaponIdentityResolver;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.meleepremium.MeleeScope;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Module: melee mitigation (design ruling D206, Nov 23, 2026;
 * module home assigned by D198 — a balance module living in
 * the box, not a defect fix).
 *
 * WHAT IT DOES, in plain language: a player who is holding a
 * melee weapon takes less damage. The weapon in hand is the
 * whole test — this is NOT tied to any class, skill, or
 * player list. Standing in melee range is where the damage
 * lands, so the build that has to stand there gets a flat
 * percentage off the damage it takes (shipped 15%). An item
 * in the OFF hand adds a small extra slice, split by item:
 * a shield or shield-like defensive item adds 7%, a totem
 * adds only 2% (a totem already does so much else). The
 * off-hand slice pays ONLY while the main hand holds a
 * melee weapon — sword + totem earns the 2%, wand + totem
 * earns nothing, because a wand is not a melee weapon. While
 * an off-hand slice is active, the off-hand item itself says
 * so in its lore ("7% mitigation activated" / "2%
 * mitigation activated"); when it stops being active the
 * line is gone. There is NO lifesteal and NO healing in
 * this module — it only ever makes an incoming hit smaller;
 * healing stays the healer's job.
 *
 * MECHANISM — the TrainingWheels scaling bones, reused on
 * purpose: ExcelTrainingWheels' kid shield scales damage on
 * the VICTIM axis at HIGH priority (ShieldRule.scaled:
 * damage x factor, never a cancel, never a side channel).
 * This module does exactly the same shape — a HIGH-priority
 * EntityDamageEvent handler for player victims that
 * multiplies the event's final damage by (1 - total
 * mitigation) — with the protected-player LIST replaced by
 * the weapon-in-hand test. Because the scaling writes the
 * event's own damage, every downstream system (health,
 * death, logs, other plugins reading the event after HIGH)
 * sees the one mitigated number. It stacks multiplicatively
 * with TrainingWheels where both apply (a shielded listed
 * player holding a sword gets both factors, each doing its
 * own job), and with armour/resistance in the usual vanilla
 * order — HIGH runs after armour's modifiers, so mitigation
 * prices the hit the player would actually have taken.
 *
 * SCOPE: EliteMobs-scope worlds — the pack's standard rule
 * (EliteMobs' own world registry, the config worlds list,
 * or the "em_" name prefix), with an apply-in-all-worlds
 * config switch for servers that want it everywhere. Damage
 * causes can be excluded by config (shipped: VOID and
 * SUICIDE are never mitigated — no weapon parries the void).
 *
 * THE STAFF TRAP: EliteMobs builds its staves on the
 * WOODEN_SPEAR material, so a material-only test would call
 * a staff a spear. The classifier therefore asks EliteMobs'
 * own WeaponIdentityResolver first: a magic weapon (wand or
 * staff) is never a melee weapon, whatever its material.
 * See MeleeWeaponClassifier for the full audit behind the
 * melee material lists.
 */
public final class MeleeMitigationModule implements FixModule, Listener {

    private ModuleContext ctx;
    private boolean active;
    private BukkitTask indicatorTask;

    private double basePercent = MitigationMath.DEFAULT_BASE_PERCENT;
    private double totemPercent = MitigationMath.DEFAULT_TOTEM_PERCENT;
    private double shieldPercent = MitigationMath.DEFAULT_SHIELD_PERCENT;
    private double maxTotalPercent = MitigationMath.DEFAULT_MAX_TOTAL_PERCENT;
    private Set<String> meleeMaterials = MeleeWeaponClassifier.DEFAULT_MELEE_MATERIALS;
    private Set<String> meleeSuffixes = MeleeWeaponClassifier.DEFAULT_MELEE_SUFFIXES;
    private Set<String> excludedMaterials = Set.of();
    private Set<String> totemMaterials = OffHandClassifier.DEFAULT_TOTEM_MATERIALS;
    private Set<String> shieldLikeMaterials = OffHandClassifier.DEFAULT_SHIELD_LIKE_MATERIALS;
    private Set<String> excludedCauses = Set.of("VOID", "SUICIDE");
    private Set<String> configWorlds = Set.of();
    private String worldPrefix = "em_";
    private boolean allWorlds;
    private long indicatorTicks = 10L;
    private boolean debug;

    private boolean eliteMobsPresent;
    private boolean registryLinkFailed;

    @Override
    public String id() {
        return "melee-mitigation";
    }

    @Override
    public String displayName() {
        return "Melee Mitigation";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        eliteMobsPresent = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        active = true;
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        indicatorTask = Bukkit.getScheduler().runTaskTimer(ctx.plugin(),
                this::refreshAllIndicators, indicatorTicks, indicatorTicks);
        ctx.logger().info("Melee Mitigation ready - players holding a melee weapon "
                + "take " + IndicatorText.formatPercent(basePercent) + "% less damage"
                + " (off hand: shield-like +" + IndicatorText.formatPercent(shieldPercent)
                + "%, totem +" + IndicatorText.formatPercent(totemPercent)
                + "%, off-hand bonus only with a melee weapon in the main hand).");
        if (!eliteMobsPresent) {
            ctx.logger().warning("EliteMobs not detected: magic-weapon identity and the "
                    + "world registry are unavailable; classification falls back to "
                    + "materials and world scope to the config list/prefix.");
        }
    }

    @Override
    public void onDisable() {
        active = false;
        if (indicatorTask != null) {
            indicatorTask.cancel();
            indicatorTask = null;
        }
        HandlerList.unregisterAll(this);
        // Leave no indicator lines behind on online players.
        for (Player player : Bukkit.getOnlinePlayers()) {
            reconcileIndicator(player, null);
        }
    }

    @Override
    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        org.bukkit.configuration.ConfigurationSection cfg = ctx.moduleConfig();
        if (cfg == null) return;
        basePercent = MitigationMath.sanitizePercent(
                cfg.getDouble("base-percent", MitigationMath.DEFAULT_BASE_PERCENT),
                MitigationMath.DEFAULT_BASE_PERCENT);
        totemPercent = MitigationMath.sanitizePercent(
                cfg.getDouble("totem-bonus-percent", MitigationMath.DEFAULT_TOTEM_PERCENT),
                MitigationMath.DEFAULT_TOTEM_PERCENT);
        shieldPercent = MitigationMath.sanitizePercent(
                cfg.getDouble("shield-bonus-percent", MitigationMath.DEFAULT_SHIELD_PERCENT),
                MitigationMath.DEFAULT_SHIELD_PERCENT);
        maxTotalPercent = MitigationMath.sanitizeCap(
                cfg.getDouble("max-total-percent", MitigationMath.DEFAULT_MAX_TOTAL_PERCENT));
        meleeMaterials = upperSet(cfg.getStringList("melee-materials"),
                MeleeWeaponClassifier.DEFAULT_MELEE_MATERIALS);
        meleeSuffixes = upperSet(cfg.getStringList("melee-material-suffixes"),
                MeleeWeaponClassifier.DEFAULT_MELEE_SUFFIXES);
        excludedMaterials = upperSet(cfg.getStringList("excluded-materials"), Set.of());
        totemMaterials = upperSet(cfg.getStringList("totem-materials"),
                OffHandClassifier.DEFAULT_TOTEM_MATERIALS);
        shieldLikeMaterials = upperSet(cfg.getStringList("shield-like-materials"),
                OffHandClassifier.DEFAULT_SHIELD_LIKE_MATERIALS);
        excludedCauses = upperSet(cfg.getStringList("excluded-causes"),
                Set.of("VOID", "SUICIDE"));
        configWorlds = new HashSet<>(cfg.getStringList("worlds"));
        worldPrefix = cfg.getString("world-prefix", "em_");
        allWorlds = cfg.getBoolean("apply-in-all-worlds", false);
        indicatorTicks = Math.max(1L, cfg.getLong("indicator-check-ticks", 10L));
        debug = cfg.getBoolean("debug", false);
    }

    private static Set<String> upperSet(List<String> values, Set<String> fallback) {
        if (values == null || values.isEmpty()) return fallback;
        Set<String> out = new HashSet<>();
        for (String v : values) {
            if (v != null && !v.isBlank()) out.add(v.trim().toUpperCase(Locale.ROOT));
        }
        return out.isEmpty() ? fallback : out;
    }

    // ------------------------------------------------------------------
    // Classification helpers
    // ------------------------------------------------------------------

    private boolean isWorldInScope(World world) {
        if (allWorlds) return true;
        if (world == null) return false;
        boolean registrySays = false;
        if (eliteMobsPresent && !registryLinkFailed) {
            try {
                registrySays = EliteMobsWorld.isEliteMobsWorld(world.getUID());
            } catch (Throwable t) {
                registryLinkFailed = true;
                ctx.logger().warning("Melee Mitigation: EliteMobs world registry call "
                        + "failed (" + t + "); world scope falls back to the "
                        + "config list/prefix.");
            }
        }
        return MeleeScope.isWorldInScope(registrySays,
                configWorlds.contains(world.getName()),
                world.getName(), worldPrefix);
    }

    private boolean isMagicWeapon(ItemStack item) {
        if (item == null || !eliteMobsPresent) return false;
        try {
            return WeaponIdentityResolver.isMagicWeapon(item);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean mainHandIsMelee(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == null) return false;
        return MeleeWeaponClassifier.isMeleeWeapon(main.getType().name(),
                isMagicWeapon(main), meleeMaterials, meleeSuffixes, excludedMaterials);
    }

    private OffHandClassifier.Kind offHandKind(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off == null) return OffHandClassifier.Kind.NONE;
        return OffHandClassifier.kindOf(off.getType().name(),
                totemMaterials, shieldLikeMaterials);
    }

    /** Total mitigation for this player right now (0 when the weapon test fails). */
    private double totalFor(Player player) {
        if (!mainHandIsMelee(player)) return 0.0;
        double bonus = OffHandClassifier.bonusFor(offHandKind(player),
                totemPercent, shieldPercent);
        return MitigationMath.totalPercent(basePercent, bonus, maxTotalPercent);
    }

    // ------------------------------------------------------------------
    // Damage taken: victim-axis scaling at HIGH (TrainingWheels shape)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!active) return;
        try {
            if (!(event.getEntity() instanceof Player player)) return;
            if (excludedCauses.contains(event.getCause().name())) return;
            if (!isWorldInScope(player.getWorld())) return;
            double total = totalFor(player);
            if (total <= 0.0) return;
            double before = event.getDamage();
            double after = MitigationMath.mitigatedDamage(before, total);
            if (after < before) {
                event.setDamage(after);
                if (debug) {
                    ctx.logger().info("MELEE-MITIGATION player=" + player.getName()
                            + " cause=" + event.getCause()
                            + " before=" + fmt(before) + " after=" + fmt(after)
                            + " total=" + IndicatorText.formatPercent(total) + "%"
                            + " world=" + player.getWorld().getName());
                }
            }
        } catch (Throwable failure) {
            logThrottled(failure);
        }
    }

    // ------------------------------------------------------------------
    // On-item indicator for the off-hand bonus
    // ------------------------------------------------------------------

    private void refreshAllIndicators() {
        if (!active) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                String desired = null;
                if (isWorldInScope(player.getWorld()) && mainHandIsMelee(player)) {
                    OffHandClassifier.Kind kind = offHandKind(player);
                    double bonus = OffHandClassifier.bonusFor(kind,
                            totemPercent, shieldPercent);
                    if (kind != OffHandClassifier.Kind.NONE && bonus > 0.0) {
                        desired = IndicatorText.lineFor(bonus);
                    }
                }
                reconcileIndicator(player, desired);
            } catch (Throwable failure) {
                logThrottled(failure);
            }
        }
    }

    /** Adds/removes exactly this module's lore line on the off-hand item. */
    private void reconcileIndicator(Player player, String desiredLine) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off == null || off.getType().isAir()) return;
        if (!off.hasItemMeta()) {
            if (desiredLine == null) return;
        }
        ItemMeta meta = off.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? meta.getLore() : null;
        List<String> next = IndicatorText.reconcile(lore, desiredLine);
        if (next == null) return;
        meta.setLore(next.isEmpty() ? null : next);
        off.setItemMeta(meta);
        player.getInventory().setItemInOffHand(off);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private long lastFailureLogNanos;

    private void logThrottled(Throwable failure) {
        long now = System.nanoTime();
        if (now - lastFailureLogNanos < 60_000_000_000L) return;
        lastFailureLogNanos = now;
        ctx.logger().warning("Melee Mitigation: failure (" + failure.getClass().getSimpleName()
                + ": " + failure.getMessage() + "); module continues.");
    }
}
