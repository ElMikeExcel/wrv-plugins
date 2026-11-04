package com.winkreyesvalley.excelmagiclevel;

import com.winkreyesvalley.excelmagiclevel.adapter.magic.BoostApplier;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.CastXpListener;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.MageLoadListener;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.MagicBridge;
import com.winkreyesvalley.excelmagiclevel.adapter.magic.TemplateDriftCheck;
import com.winkreyesvalley.excelmagiclevel.adapter.mcmmo.McmmoBridge;
import com.winkreyesvalley.excelmagiclevel.adapter.mcmmo.NoOpMcmmoBridge;
import com.winkreyesvalley.excelmagiclevel.adapter.papi.MagicLevelExpansion;
import com.winkreyesvalley.excelmagiclevel.adapter.together.BukkitServicePartyLookup;
import com.winkreyesvalley.excelmagiclevel.adapter.together.NoOpPartyLookup;
import com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup;
import com.winkreyesvalley.excelmagiclevel.adapter.together.ProximityPartyLookup;
import com.winkreyesvalley.excelmagiclevel.adapter.weekend.ClockWeekendSignal;
import com.winkreyesvalley.excelmagiclevel.adapter.weekend.WeekendSignal;
import com.winkreyesvalley.excelmagiclevel.core.AntiAbuseWindows;
import com.winkreyesvalley.excelmagiclevel.core.Boost;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.Percents;
import com.winkreyesvalley.excelmagiclevel.core.RewardTable;
import com.winkreyesvalley.excelmagiclevel.core.WeightTable;
import com.winkreyesvalley.excelmagiclevel.store.PlayerStore;
import com.winkreyesvalley.excelmagiclevel.surface.FeedbackService;
import com.winkreyesvalley.excelmagiclevel.surface.MagicLevelCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * ExcelMagicLevel — casts earn magic XP; a derived Magic Level grants
 * small soft-power boosts (mana pool / regen / cast cost) through Magic's
 * own modifier system. Path and seals decide WHAT you can cast; Magic
 * Level decides HOW STRONG your casting is. It never locks or unlocks a
 * spell, and it never touches SP, earn rates, or path data (SPEC §1).
 *
 * <p>Built to the house laws: every balance value is a config key
 * (adjustability), the frequently-edited tables are data (modularity),
 * and every neighbour — Magic, ExcelTogether, mcMMO, PlaceholderAPI —
 * sits behind a replaceable adapter that fails soft (house build style).
 * The mcMMO adapter is a deliberate no-op in v1 (SPEC §9): no mcMMO code
 * path exists in the loop, so no mcMMO update can break this plugin.</p>
 *
 * <p>Excel — Wink Reyes Valley.</p>
 */
public final class ExcelMagicLevelPlugin extends JavaPlugin implements Listener {
    private PlayerStore store;
    private AntiAbuseWindows windows;
    private FeedbackService feedback;

    @SuppressWarnings("unused") // held for /magiclevel party introspection; wiring lives in onEnable
    private PartyLookup partyLookup;
    @SuppressWarnings("unused")
    private McmmoBridge mcmmoBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        // --- core tables, all from config (adjustability law) ---
        Curve curve = readCurve(config);
        RewardTable rewards = readRewards(config);
        WeightTable weights = readWeights(config);
        String prefix = config.getString("modifiers.key-prefix", "wrv_magiclevel_");
        boolean verbose = config.getBoolean("debug.verbose", false);

        // --- store (SPEC §7.1) ---
        store = new PlayerStore(new File(getDataFolder(), "players.yml"),
                message -> getLogger().warning(message));
        store.load();

        // --- adapters (house build style: replaceable, fail-soft) ---
        mcmmoBridge = new NoOpMcmmoBridge();      // v1 by design (SPEC §9)

        // --- party share source (SPEC §4.5/§0.3): a registered PartyLookup
        // service wins; until ExcelTogether publishes one (0.4.3 does not —
        // see TOGETHER-API-NOTE.md), the config-gated PROXIMITY fallback
        // runs, labelled everywhere the mode is shown. ---
        boolean shareEnabled = config.getBoolean("share.enabled", true);
        int sharePercent = config.getInt("share.percent", 100);
        double shareRadius = config.getDouble("share.radius", 50.0);
        String shareMode;
        if (!shareEnabled) {
            partyLookup = new NoOpPartyLookup();
            shareMode = "off";
        } else {
            BukkitServicePartyLookup serviceLookup =
                    new BukkitServicePartyLookup(getServer().getServicesManager(), getLogger());
            if (serviceLookup.available()) {
                partyLookup = serviceLookup;
                shareMode = "party";
            } else if (config.getBoolean("share.fallback-proximity", true)) {
                partyLookup = new ProximityPartyLookup(shareRadius);
                shareMode = "proximity";
                getLogger().warning("ExcelMagicLevel: share is ON but no PartyLookup service is registered "
                        + "(ExcelTogether 0.4.3 publishes none — see TOGETHER-API-NOTE.md); running the "
                        + "labelled PROXIMITY fallback: copies go to every player within " + shareRadius
                        + " blocks in the same world.");
            } else {
                partyLookup = new NoOpPartyLookup();
                shareMode = "off-no-service";
                getLogger().warning("ExcelMagicLevel: share is ON but no PartyLookup service is registered "
                        + "and share.fallback-proximity is false — share copies are OFF.");
            }
        }
        String shareStatus = switch (shareMode) {
            case "party" -> "on — party members within " + trimRadius(shareRadius)
                    + " blocks in your world get " + copyPhrase(sharePercent) + " of your Magic XP";
            case "proximity" -> "on (proximity fallback) — every player within " + trimRadius(shareRadius)
                    + " blocks in your world gets " + copyPhrase(sharePercent)
                    + " of your Magic XP; party-only share waits on ExcelTogether's PartyLookup service";
            case "off-no-service" -> "off — no party service is registered and the proximity fallback is disabled";
            default -> "off";
        };
        WeekendSignal weekendSignal = readWeekendSignal(config);
        MagicBridge magic = new MagicBridge();
        if (!magic.connect()) {
            // Magic is the substrate — the one correct hard edge (SPEC §9.2).
            getLogger().severe("ExcelMagicLevel: Magic's API controller is unreachable — disabling. "
                    + "A magic level with no Magic is meaningless.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BoostApplier boosts = new BoostApplier(prefix, curve);
        feedback = new FeedbackService(store, config.getInt("feedback.chunk", 10));
        windows = new AntiAbuseWindows();
        AwardService awards = new AwardService(getLogger(), store, curve, rewards, windows,
                weekendSignal, config.getDouble("bonuses.weekend-rate", 1.2),
                config.getLong("xp.earn-window-seconds", 300) * 1000L,
                config.getLong("xp.target-window-seconds", 30) * 1000L,
                config.getDouble("xp.target-repeat-scale", 0.5),
                magic, boosts, feedback,
                partyLookup, shareEnabled && !"off-no-service".equals(shareMode), sharePercent, shareRadius);

        // --- listeners ---
        getServer().getPluginManager().registerEvents(
                new CastXpListener(getLogger(), awards, weights,
                        config.getString("debug.throw-on-spell-key", ""), verbose), this);
        getServer().getPluginManager().registerEvents(
                new MageLoadListener(getLogger(), store, curve, boosts, verbose), this);
        getServer().getPluginManager().registerEvents(this, this);

        // --- command ---
        MagicLevelCommand command = new MagicLevelCommand(this, store, curve, rewards, magic, boosts, shareStatus);
        PluginCommand pluginCommand = getCommand("magiclevel");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // --- PlaceholderAPI expansion (SPEC §6.1): only when PAPI is
        // present; a failure here skips the expansion and nothing else
        // (adapter law, SPEC §9.2). ---
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                boolean registered = new MagicLevelExpansion(store, curve, rewards,
                        getPluginMeta().getVersion()).register();
                getLogger().info("ExcelMagicLevel: PlaceholderAPI expansion "
                        + (registered ? "registered (%excelmagiclevel_*%)." : "registration declined by PAPI."));
            } catch (Throwable t) {
                getLogger().log(Level.WARNING,
                        "ExcelMagicLevel: PlaceholderAPI expansion failed to register — placeholders are off, "
                                + "everything else is unaffected", t);
            }
        }

        // --- debounced saves (SPEC §7.1: 5 s debounce) ---
        getServer().getScheduler().runTaskTimer(this,
                () -> store.saveIfDue(System.currentTimeMillis(), 5000L), 20L, 20L);

        // --- enable-time drift check: warns, never blocks (SPEC §7.2) ---
        List<String> drift = TemplateDriftCheck.check(magic.controller(), prefix, curve, rewards);
        for (String warning : drift) {
            getLogger().warning("ExcelMagicLevel drift check: " + warning);
        }
        if (!config.getBoolean("xp.bosstrial-enabled", true)) {
            getLogger().warning("ExcelMagicLevel: xp.bosstrial-enabled is false, but detecting a boss trial "
                    + "needs a BossTrial adapter this build does not have — casts will pay XP normally. "
                    + "The settled default is true (SPEC 4.3).");
        }

        Boost cap = rewards.boostAt(curve.maxLevel());
        getLogger().info("ExcelMagicLevel " + getPluginMeta().getVersion() + " awake: " + curve.maxLevel()
                + " Magic Levels, " + curve.totalToCap() + " XP to cap, cap boosts mana pool +"
                + Percents.format(cap.manaMaxBoost()) + "% / regen +" + Percents.format(cap.manaRegenerationBoost())
                + "% / cast cost -" + Percents.format(cap.costReduction()) + "%"
                + "; share: " + shareMode
                + (drift.isEmpty() ? "; modifier templates in sync." : "; " + drift.size() + " drift warning(s) above."));
    }

    private static String copyPhrase(int percent) {
        return percent >= 100 ? "a full copy" : ("a " + percent + "% copy");
    }

    private static String trimRadius(double radius) {
        return radius == Math.floor(radius) ? String.valueOf((long) radius) : String.valueOf(radius);
    }

    @Override
    public void onDisable() {
        if (store != null) {
            try {
                store.saveNow();
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "ExcelMagicLevel: final players.yml save failed", ex);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Hygiene only: windows are in-memory by design (SPEC §7.1) and a
        // relog resetting them is the accepted, named seam.
        if (windows != null) windows.forget(event.getPlayer().getUniqueId());
        if (feedback != null) feedback.forget(event.getPlayer().getUniqueId());
    }

    // --- config readers: config is the ONLY source of balance truth ---

    private Curve readCurve(FileConfiguration config) {
        Map<Integer, Long> overrides = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("curve.overrides");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    overrides.put(Integer.parseInt(key), section.getLong(key));
                } catch (NumberFormatException ex) {
                    getLogger().warning("ExcelMagicLevel: ignoring non-numeric curve override key '" + key + "'");
                }
            }
        }
        return new Curve(
                config.getInt("curve.max-level", 20),
                config.getDouble("curve.base", 100.0),
                config.getDouble("curve.exponent", 1.5),
                config.getLong("curve.round-to", 10),
                overrides);
    }

    private RewardTable readRewards(FileConfiguration config) {
        Boost perLevel = new Boost(
                config.getDouble("rewards.per-level.mana-max-boost", 0.0125),
                config.getDouble("rewards.per-level.mana-regeneration-boost", 0.0125),
                config.getDouble("rewards.per-level.cost-reduction", 0.005));
        Set<Integer> milestones = new HashSet<>(config.getIntegerList("rewards.milestones"));
        return new RewardTable(perLevel, milestones);
    }

    private WeightTable readWeights(FileConfiguration config) {
        Map<String, Integer> weights = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("weights");
        int defaultWeight = 4;
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (key.equals("default")) {
                    defaultWeight = section.getInt(key, 4);
                } else {
                    weights.put(key, section.getInt(key, 0));
                }
            }
        }
        return new WeightTable(weights, defaultWeight);
    }

    private WeekendSignal readWeekendSignal(FileConfiguration config) {
        ZoneId zone;
        try {
            zone = ZoneId.of(config.getString("bonuses.weekend-zone", "America/Chicago"));
        } catch (Exception ex) {
            getLogger().warning("ExcelMagicLevel: bad bonuses.weekend-zone — falling back to America/Chicago");
            zone = ZoneId.of("America/Chicago");
        }
        Set<DayOfWeek> days = new HashSet<>();
        for (String day : config.getStringList("bonuses.weekend-days")) {
            try {
                days.add(DayOfWeek.valueOf(day.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("ExcelMagicLevel: ignoring unknown weekend day '" + day + "'");
            }
        }
        if (days.isEmpty()) {
            days = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }
        return new ClockWeekendSignal(zone, days);
    }
}
