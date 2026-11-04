// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.api.ExcelMenuApi;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.registry.RegistryConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ExcelMenu 2.0 — ONE menu plugin for Java + Bedrock (DESIGN-2.0).
 *
 * This is the Phase 1 build: it replaces ExcelMenu 0.1.8 in its own
 * slot (same plugin name, same data folder) and carries BOTH menu
 * systems during the switchover:
 *
 * <ul>
 *   <li><b>The registry</b> (Phase 0's core): entry files + layout
 *       definitions, resolved per viewer per open and rendered by
 *       the two thin renderers — the Java chest GUI and the Bedrock
 *       Cumulus form (flat = the native compass form's shape;
 *       grouped = the commands catalogue's shape). server_menu and
 *       commands_menu are registry-owned from this build on.</li>
 *   <li><b>The definition engine</b> (0.1.8, ported): every
 *       unconverted menu (shops, skills, teleport confirms, basics)
 *       keeps loading and rendering exactly as before, Bedrock forms
 *       included — now through the same unified platform bridge.</li>
 * </ul>
 *
 * What it deliberately does NOT do, per the switchover sequence
 * (DESIGN-2.0 §3.3): it does not register /menu, /servercommands,
 * /serverwebsite, /tpmenu or /childproofmenu — those stay with
 * ExcelBedrockMenu 1.5.27 until the Phase 2 unification window, so
 * ExcelBedrockMenu's Java routing ("dm open server_menu") lands on
 * this build's registry renderer without knowing anything changed,
 * and its Bedrock native form is still the hardcoded one. The /menu
 * contract ExcelMenuJoin depends on (one player command, no args,
 * opens the Server Menu) is preserved by not touching it.
 */
public final class ExcelMenu extends JavaPlugin {

    /** Registry entries extracted on first run (fill-missing). */
    static final String[] BUNDLED_ENTRIES = {
            // server_menu (27 — halloween_scares added Nov 1, 2026: it was
            // in the jar's entries/ resources but missing from this extraction
            // list, so first boot extracted 82 of 83 and the registry read 82)
            "aimassist", "ask", "autojoin", "autosort", "boss_rewards", "commands",
            "cosmetics", "guild", "halloween", "halloween_scares", "info", "jobs", "leaderboard",
            "lostfound", "makersmark_settings", "party", "shop", "silkwarnings",
            "skills", "spawn", "spellbook", "tpmenu", "trash", "utility_pouch",
            "wandstyle", "website", "whatsnew",
            // commands_menu catalogue (56)
            "ga_menu", "ga_spawn", "ga_tpmenu", "ga_tpa_player", "ga_tpaccept",
            "ga_warps", "mj_balance", "mj_pay_player_amount", "mj_balancetop",
            "mj_jobs", "mj_jobs_stats", "mj_jobs_join_job", "sl_mcstats",
            "sl_mctop", "sl_skill_name", "sl_party", "sl_leaderboard",
            "st_shop", "st_cosmetics", "st_excelshop_setup", "st_excelshop_manage",
            "st_deathprotection_status", "st_mobveil_status", "st_buildmode",
            "cl_claim", "cl_claimslist", "cl_trust_player", "cl_childproofmenu",
            "cl_unchildproof_player", "cl_extendclaim_blocks",
            "cl_buyclaimblocks_amount", "cl_trapped", "cl_abandonclaim",
            "gq_ag", "gq_seals", "gq_em", "gq_em_party",
            "si_motd", "si_list", "si_msg_player_message", "si_playermap",
            "si_compass", "si_pouch", "si_menujoin", "si_servercommands",
            "si_serverwebsite", "si_whatsnew", "si_emotes",
            "fh_chunks", "fh_places", "fh_aimassist", "fh_treecleanup",
            "yi_lostfound", "yi_trash", "yi_mark", "yi_ask",
    };

    /** Layout definitions extracted on first run (fill-missing). */
    static final String[] BUNDLED_LAYOUTS = { "server_menu", "commands_menu" };

    /** Unconverted definitions extracted on first run (fill-missing).
     *  server_menu / commands_menu / commands_menu_page2 are NOT
     *  here: the first two are registry-owned now, the third is
     *  retired by the catalogue conversion. */
    static final String[] BUNDLED_MENUS = {
            "basics_menu", "makersmark_settings", "skills_perks",
            "spawnshop", "spawnshop_building", "spawnshop_cosmetics_colors",
            "spawnshop_cosmetics_sets", "spawnshop_enchanting", "spawnshop_food",
            "spawnshop_potions_protection", "spawnshop_safe_blocks",
            "spawnshop_travel_animals", "teleport_confirm_guild",
            "teleport_confirm_home", "teleport_confirm_spawn",
            "teleport_confirm_spawncmd",
    };

    private EntryService entryService;
    private PlatformBridge bridge;
    private MenuService menuService;
    private RegistryMenus registryMenus;
    private FormService formService;
    private Map<String, List<String>> lastMenuFailures = Map.of();
    private final List<String> registeredCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        entryService = new EntryService(this);
        entryService.configure(readConfig());
        bridge = new PlatformBridge(this);
        menuService = new MenuService(this, bridge);
        formService = new FormService(this, bridge, entryService,
                menuService.dispatcher());
        registryMenus = new RegistryMenus(this, bridge, entryService,
                menuService.dispatcher(), formService);
        formService.setChestFallback((player, menuId) ->
                registryMenus.openChest(player, menuId));
        formService.setTtlSeconds(getConfig().getLong("forms.session-ttl-seconds", 300L));
        menuService.setRegistryMenus(registryMenus);

        extractBundled("entries", BUNDLED_ENTRIES);
        extractBundled("layouts", BUNDLED_LAYOUTS);
        extractBundled("menus", BUNDLED_MENUS);

        entryService.reload(null);
        registryMenus.reloadLayouts(null);
        reloadDefinitions(null);
        bridge.selfCheck();

        getServer().getPluginManager().registerEvents(new MenuListener(menuService), this);
        getServer().getPluginManager().registerEvents(
                registryMenus.new ClickListener(), this);
        getServer().getPluginManager().registerEvents(formService, this);

        MenuCommands commands = new MenuCommands(this, menuService, entryService,
                registryMenus, bridge);
        if (getCommand("excelmenu") != null) getCommand("excelmenu").setExecutor(commands);
        if (getCommand("dm") != null) getCommand("dm").setExecutor(commands);
        getServer().getServicesManager().register(ExcelMenuApi.class, entryService, this,
                ServicePriority.Normal);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getScheduler().runTaskTimer(this, this::refreshTick, 20L, 20L);

        getLogger().info("ExcelMenu " + getPluginMeta().getVersion() + " enabled (Phase 1:"
                + " registry + definitions in the ExcelMenu slot). "
                + entryService.entries().size() + " entries, "
                + registryMenus.layoutIds().size() + " registry menus, "
                + menuService.menus().size() + " definition menus live. "
                + "/menu and the /menu family remain with ExcelBedrockMenu"
                + " until the Phase 2 unification (DESIGN-2.0 §3.3).");
    }

    @Override
    public void onDisable() {
        if (menuService != null) menuService.retire();
        if (formService != null) formService.retire();
        if (entryService != null) {
            getServer().getServicesManager().unregister(ExcelMenuApi.class, entryService);
        }
        unregisterMenuCommands();
    }

    // ---------- loading ----------

    /** (Re)load the definition set with the registry exclusions. */
    void reloadDefinitions(CommandSender sender) {
        File dir = new File(getDataFolder(), "menus");
        Set<String> exclusions = new HashSet<>(registryMenus.layoutIds());
        exclusions.addAll(getConfig().getStringList("registry.retired-definitions"));
        MenuService.LoadOutcome outcome = menuService.loadAll(dir.toPath(), exclusions,
                registryMenus.layoutIds());
        if (outcome.loaded().isEmpty() && !menuService.menus().isEmpty()) {
            getLogger().severe("ExcelMenu definition reload REJECTED: the new set loaded"
                    + " zero menus; the previous " + menuService.menus().size()
                    + " stay live.");
            if (sender != null) {
                sender.sendMessage("§cExcelMenu reload rejected — new definition set is"
                        + " empty; previous menus stay live. See console.");
            }
            return;
        }
        lastMenuFailures = outcome.failed();
        menuService.installSet(outcome);
        registerMenuCommands();
        if (sender != null) {
            sender.sendMessage("§aExcelMenu definitions reloaded: " + outcome.loaded().size()
                    + " menus, " + outcome.failed().size() + " failed.");
        }
    }

    /** The full reload behind /excelmenu reload and /dm reload:
     *  config, registry (+ diff), layouts, definitions. */
    void reloadAll(CommandSender sender) {
        entryService.configure(rereadConfig());
        formService.setTtlSeconds(getConfig().getLong("forms.session-ttl-seconds", 300L));
        entryService.reload(sender);
        registryMenus.reloadLayouts(sender);
        reloadDefinitions(sender);
    }

    Map<String, List<String>> lastMenuFailures() {
        return lastMenuFailures;
    }

    private void extractBundled(String folder, String[] names) {
        File dir = new File(getDataFolder(), folder);
        if (!dir.exists() && !dir.mkdirs()) {
            getLogger().severe("ExcelMenu: cannot create " + dir);
            return;
        }
        List<String> missing = new ArrayList<>();
        for (String name : names) {
            File f = new File(dir, name + ".yml");
            if (!f.exists()) {
                try {
                    saveResource(folder + "/" + name + ".yml", false);
                } catch (IllegalArgumentException e) {
                    missing.add(name);
                }
            }
        }
        if (!missing.isEmpty()) {
            getLogger().severe("ExcelMenu: bundled " + folder + " missing from jar: "
                    + String.join(", ", missing));
        }
    }

    // ---------- config ----------

    RegistryConfig rereadConfig() {
        reloadConfig();
        return readConfig();
    }

    private RegistryConfig readConfig() {
        FileConfiguration cfg = getConfig();
        String message = cfg.getString("registry.unavailable-message",
                RegistryConfig.DEFAULT_UNAVAILABLE);
        Map<String, List<String>> groupOrder = new LinkedHashMap<>();
        if (cfg.isConfigurationSection("groups")) {
            for (String menu : cfg.getConfigurationSection("groups").getKeys(false)) {
                groupOrder.put(menu, cfg.getStringList("groups." + menu));
            }
        }
        Map<String, String> groupColors = new LinkedHashMap<>();
        if (cfg.isConfigurationSection("group-colors")) {
            for (String group : cfg.getConfigurationSection("group-colors").getKeys(false)) {
                groupColors.put(group, cfg.getString("group-colors." + group, ""));
            }
        }
        String zoneName = cfg.getString("registry.zone", "America/Chicago");
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneName);
        } catch (Exception e) {
            getLogger().warning("ExcelMenu: unknown registry.zone '" + zoneName
                    + "' — falling back to America/Chicago.");
            zone = RegistryConfig.SERVER_ZONE;
        }
        return new RegistryConfig(zone, message, groupOrder, groupColors);
    }

    // ---------- dynamic open commands (/shop, /basicsmenu) ----------

    private void registerMenuCommands() {
        unregisterMenuCommands();
        CommandMap map = getServer().getCommandMap();
        Map<String, String> wanted = new HashMap<>();
        for (MenuDef def : menuService.menus().values()) {
            if (def.command != null && !def.command.isBlank()) {
                wanted.put(def.command, def.id);
            }
        }
        for (Map.Entry<String, String> e : wanted.entrySet()) {
            map.register("excelmenu",
                    new MenuCommands.MenuOpenCommand(menuService, e.getValue(), e.getKey()));
            registeredCommands.add(e.getKey());
            getLogger().info("ExcelMenu: registered command /" + e.getKey()
                    + " -> " + e.getValue());
        }
    }

    private void unregisterMenuCommands() {
        if (registeredCommands.isEmpty()) return;
        Map<String, org.bukkit.command.Command> known =
                getServer().getCommandMap().getKnownCommands();
        for (String name : registeredCommands) {
            known.remove(name);
            known.remove("excelmenu:" + name);
        }
        registeredCommands.clear();
    }

    // ---------- placeholder refresh tick (definition views) ----------

    private void refreshTick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder =
                    player.getOpenInventory().getTopInventory().getHolder();
            if (!(holder instanceof MenuHolder mh)) continue;
            MenuDef def = menuService.menu(mh.menuId());
            if (def == null || def.updateInterval <= 0) continue;
            if (now - mh.lastRenderMillis() < def.updateInterval * 1000L) continue;
            menuService.fillInventory(player, def, mh.getInventory(), mh, true);
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
