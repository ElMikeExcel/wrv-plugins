package com.winkreyes.excelemfoodheal;

import com.magmaguy.elitemobs.dungeons.EliteMobsWorld;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelEMFoodHeal 0.2.0 (D162 + D184): in EliteMobs worlds, eating
 * food grants +1% of max health instantly plus a heal budget
 * totalling clamp(saturation x 0.75, 1%, 10%) of max health,
 * delivered at 0.5%/s while out of combat. The budget waits during
 * combat (20 s window, mirroring ACS), expires 90 s after the meal,
 * and is replaced - never stacked - by re-eating. Self-only
 * healing; no cleanse/shield/absorption/DR. EliteMobs' own flat
 * ACS food heal is not touched and still applies.
 *
 * <p>D184 adds a base out-of-combat regen bonus (default +1%/s,
 * config {@code ooc-bonus-enabled} / {@code
 * ooc-bonus-percent-per-second}): EliteMobs' ACS out-of-combat
 * trickle is a hardcoded 1%/s with no config in 10.9.14, so this
 * bonus supplies the second 1%/s under the same conditions (EM
 * world, out of combat on the same 20 s tracking), for a total
 * base rate of 2%/s. No food required; food budgets stack on top.
 *
 * <p>World gate: EliteMobs' registry
 * ({@code EliteMobsWorld.isEliteMobsWorld}) OR the config world
 * list. Max health is the player's live MAX_HEALTH attribute, so
 * the % follows the real (ACS-raised) pool. Server-side only -
 * identical for Java and Bedrock; no commands, no UI.
 */
public final class ExcelEMFoodHeal extends JavaPlugin implements Listener {

    private HealEngine engine;
    private boolean debug;
    private boolean bonusEnabled;
    private double bonusPercentPerSecond;
    private final Set<UUID> bonusActive = new HashSet<>();
    private double saturationMultiplier;
    private double minTotalPercent;
    private double maxTotalPercent;
    private Set<String> configWorlds = Set.of();
    private Map<Material, Double> saturationOverrides = Map.of();
    private boolean eliteMobsPresent;
    private boolean eliteMobsLinkFailed;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug", true);
        bonusEnabled = getConfig().getBoolean("ooc-bonus-enabled", true);
        bonusPercentPerSecond = getConfig().getDouble("ooc-bonus-percent-per-second", 1.0);
        saturationMultiplier = getConfig().getDouble("saturation-multiplier", 0.75);
        minTotalPercent = getConfig().getDouble("min-total-percent", 1.0);
        maxTotalPercent = getConfig().getDouble("max-total-percent", 10.0);
        configWorlds = new HashSet<>(getConfig().getStringList("worlds"));
        saturationOverrides = readSaturationOverrides();
        HealEngine.Settings settings = new HealEngine.Settings(
                getConfig().getDouble("instant-percent", 1.0),
                getConfig().getDouble("rate-percent-per-second", 0.5),
                getConfig().getLong("combat-window-seconds", 20) * 1000L,
                getConfig().getLong("budget-expiry-seconds", 90) * 1000L);
        engine = new HealEngine(settings);
        eliteMobsPresent = getServer().getPluginManager().getPlugin("EliteMobs") != null;
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("ExcelEMFoodHeal enabled: food heal-over-time active in EliteMobs worlds (debug=" + debug + ")."
                + " Out-of-combat bonus (D184): " + (bonusEnabled
                        ? "+" + fmt(bonusPercentPerSecond) + "%/s (total base 2%/s with EliteMobs' own trickle)."
                        : "disabled by config."));
        if (!eliteMobsPresent) {
            getLogger().warning("EliteMobs not detected: world gate runs on the config world list only.");
        }
    }

    private Map<Material, Double> readSaturationOverrides() {
        Map<Material, Double> out = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("food-saturation");
        if (section == null) return out;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                getLogger().warning("food-saturation override names an unknown material: " + key);
                continue;
            }
            out.put(material, section.getDouble(key));
        }
        return out;
    }

    // ------------------------------------------------------------ world gate

    private boolean isEmWorld(World world) {
        boolean registrySays = false;
        if (eliteMobsPresent && !eliteMobsLinkFailed) {
            try {
                registrySays = EliteMobsWorld.isEliteMobsWorld(world.getUID());
            } catch (Throwable t) {
                eliteMobsLinkFailed = true;
                getLogger().warning("EliteMobs world registry call failed (" + t + "); "
                        + "falling back to the config world list only.");
            }
        }
        return WorldGate.isEliteMobsWorld(registrySays, configWorlds.contains(world.getName()));
    }

    // ------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material food = event.getItem().getType();
        if (!food.isEdible()) return;
        if (!isEmWorld(player.getWorld())) return;
        Double saturation = saturationOverrides.get(food);
        if (saturation == null) saturation = FoodTable.vanillaSaturation(food);
        if (saturation == null) return; // unknown/custom food: no budget in v1

        double totalPercent = FoodTable.totalPercent(saturation, saturationMultiplier, minTotalPercent, maxTotalPercent);
        long now = System.currentTimeMillis();
        double instantPercent = engine.startMeal(player.getUniqueId(), food.name(), totalPercent, now);
        double deliveredHp = deliver(player, instantPercent);
        double maxHp = maxHealth(player);
        boolean finished = engine.reportDelivered(player.getUniqueId(),
                maxHp > 0 ? deliveredHp / maxHp * 100.0 : 0.0);
        debug("meal player=" + player.getName() + " food=" + food.name()
                + " budget=" + fmt(totalPercent) + "% instant=" + fmt(deliveredHp) + "HP"
                + " remaining=" + fmt(engine.remainingPercent(player.getUniqueId())) + "%"
                + " world=" + player.getWorld().getName());
        if (finished) {
            debug("complete player=" + player.getName() + " food=" + food.name() + " (instant only)");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();
        if (event.getEntity() instanceof Player victim) {
            engine.markCombat(victim.getUniqueId(), now);
        }
        UUID damagerUuid = null;
        if (event.getDamager() instanceof Player damager) {
            damagerUuid = damager.getUniqueId();
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            damagerUuid = shooter.getUniqueId();
        }
        if (damagerUuid != null) {
            engine.markCombat(damagerUuid, now);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        engine.forget(event.getEntity().getUniqueId());
        bonusActive.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        engine.forget(event.getPlayer().getUniqueId());
        bonusActive.remove(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------ tick + delivery

    private void tick() {
        long now = System.currentTimeMillis();
        for (UUID uuid : engine.activePlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            String foodName = engine.mealFood(uuid); // capture now: tick/report may remove the meal
            HealEngine.TickDecision decision = engine.tick(uuid, now);
            switch (decision.kind()) {
                case PAUSED -> debug("pause player=" + player.getName()
                        + " food=" + foodName
                        + " remaining=" + fmt(decision.remainingPercent()) + "%");
                case EXPIRED -> debug("expire player=" + player.getName()
                        + " food=" + foodName
                        + " undelivered=" + fmt(decision.remainingPercent()) + "%");
                case DELIVER -> {
                    if (decision.resumed()) {
                        debug("resume player=" + player.getName()
                                + " food=" + foodName
                                + " remaining=" + fmt(decision.remainingPercent()) + "%");
                    }
                    double deliveredHp = deliver(player, decision.percentToDeliver());
                    double maxHp = maxHealth(player);
                    if (deliveredHp > 0) {
                        boolean finished = engine.reportDelivered(uuid, deliveredHp / maxHp * 100.0);
                        if (finished) {
                            debug("complete player=" + player.getName()
                                    + " food=" + foodName);
                        }
                    }
                }
                case WAIT -> { /* budget waiting (combat or full health): nothing to log */ }
            }
        }
        tickBonus(now);
    }

    /**
     * D184 base out-of-combat regen bonus: once per second, every
     * player in an EliteMobs world who is out of combat (same 20 s
     * tracking the food budgets use) gains the configured bonus %
     * of max health — no meal required. Runs after the meal loop so
     * a tick's food delivery lands first; both layers clamp to
     * missing health inside {@link #deliver}. The transition into
     * bonus delivery is debug-logged once per stretch, so the live
     * proof has a log witness.
     */
    private void tickBonus(long now) {
        if (!bonusEnabled || bonusPercentPerSecond <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            double percent = BonusPolicy.tickPercent(bonusEnabled, bonusPercentPerSecond,
                    isEmWorld(player.getWorld()), engine.inCombat(uuid, now));
            if (percent <= 0 || player.isDead()) {
                bonusActive.remove(uuid);
                continue;
            }
            double deliveredHp = deliver(player, percent);
            if (deliveredHp > 0) {
                if (bonusActive.add(uuid)) {
                    debug("bonus start player=" + player.getName()
                            + " rate=" + fmt(percent) + "%/s"
                            + " world=" + player.getWorld().getName());
                }
            } else {
                // At full health: nothing to deliver, stretch is over.
                bonusActive.remove(uuid);
            }
        }
    }

    /**
     * Heal the player by percent of their current max health, clamped
     * to missing health. Returns HP actually delivered (0 at full
     * health or while dead).
     */
    private double deliver(Player player, double percent) {
        if (percent <= 0 || player.isDead()) return 0.0;
        double maxHp = maxHealth(player);
        if (maxHp <= 0) return 0.0;
        double missing = maxHp - player.getHealth();
        if (missing <= 0) return 0.0;
        double amount = Math.min(percent / 100.0 * maxHp, missing);
        if (amount <= 0) return 0.0;
        player.setHealth(player.getHealth() + amount);
        return amount;
    }

    private double maxHealth(Player player) {
        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 0.0 : attribute.getValue();
    }

    private void debug(String message) {
        if (debug) getLogger().info("FOODHEAL " + message);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
