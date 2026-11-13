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
 * This is the Phase 2 build — the unification (DESIGN-2.0 §3.3):
 * ExcelBedrockMenu is retired and everything it owned lives here.
 *
 * <ul>
 *   <li><b>The registry</b> (Phase 0's core): entry files + layout
 *       definitions, resolved per viewer per open and rendered by
 *       the two thin renderers — the Java chest GUI and the Bedrock
 *       Cumulus form (flat = the native compass form's shape;
 *       grouped = the commands catalogue's shape). server_menu and
 *       commands_menu are registry-owned.</li>
 *   <li><b>The definition engine</b> (0.1.8, ported): every
 *       unconverted menu (shops, skills, basics) keeps loading and
 *       rendering exactly as before, Bedrock forms included —
 *       through the same unified platform bridge. The four
 *       teleport_confirm_* definitions retired in this phase: their
 *       ids route to the confirmation-gate service instead.</li>
 *   <li><b>The /menu family</b> (ported from ExcelBedrockMenu
 *       1.5.28/1.5.29 as a shared core + thin renderers): /menu,
 *       /servercommands (alias /cmds), /serverwebsite, /tpmenu and
 *       /childproofmenu are registered here, and the dynamic
 *       screens — the teleport picker with its direction choice,
 *       the childproof picker, the Ask text box, Skills & Perks,
 *       the website screen — live in the screens classes beside
 *       this one, their pure logic in core.screens.</li>
 * </ul>
 *
 * The /menu contract ExcelMenuJoin depends on (one player command,
 * no args, opens the Server Menu) is preserved letter-for-letter:
 * ExcelMenuJoin still funnels through performCommand("menu"), which
 * now lands on MenuFamilyCommands and opens server_menu through
 * MenuService, exactly as ExcelBedrockMenu's /menu did.
 */
public final class ExcelMenu extends JavaPlugin {

    /** Registry entries extracted on first run (fill-missing). */
    static final String[] BUNDLED_ENTRIES = {
            // server_menu (28 — halloween_scares added Nov 1, 2026: it was
            // in the jar's entries/ resources but missing from this extraction
            // list, so first boot extracted 82 of 83 and the registry read 82;
            // magiclevel folded in at the Phase 2 baseline, Nov 2, 2026: it
            // was placed live as a data file Nov 1 and never bundled.
            // RETIRED in 3.0 (Phase 3, DESIGN §5): the bundled entry and
            // its resource are removed; the live file is deleted in the
            // same window (kept as a .bak) — the door's job moved to the
            // Skills panel's Magic header. Restoring the file revives
            // the door even on this jar (extraction is fill-missing only).
            // BundledEntriesTest pins this list to the resources tree.)
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
     *  retired by the catalogue conversion. The four
     *  teleport_confirm_* definitions are NOT here either since
     *  Phase 2: they retired into the confirmation-gate service
     *  (DESIGN-2.0 §2.4); the bundled config's retired-definitions
     *  list keeps any on-disk copies inert. */
    static final String[] BUNDLED_MENUS = {
            "basics_menu", "makersmark_settings", "skills_perks",
            "spawnshop", "spawnshop_building", "spawnshop_cosmetics_colors",
            "spawnshop_cosmetics_sets", "spawnshop_enchanting", "spawnshop_food",
            "spawnshop_potions_protection", "spawnshop_safe_blocks",
            "spawnshop_travel_animals",
    };

    private EntryService entryService;
    private PlatformBridge bridge;
    private MenuService menuService;
    private RegistryMenus registryMenus;
    private FormService formService;
    private ConfirmService confirmService;
    private PrefsStore prefsStore;
    private MenuEditorForms editorForms;
    private MenuEditorMenus editorMenus;
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
        // 3.0 rearrange: prefs store + both editors (DESIGN §3).
        prefsStore = new PrefsStore(this);
        formService.setPrefsStore(prefsStore);
        registryMenus.setPrefsStore(prefsStore);
        editorForms = new MenuEditorForms(this, bridge, prefsStore, entryService);
        editorForms.bind(registryMenus);
        editorForms.setTtlSeconds(getConfig().getLong("forms.session-ttl-seconds", 300L));
        editorMenus = new MenuEditorMenus(this, prefsStore, entryService);
        editorMenus.bind(registryMenus);
        formService.setEditorHandler((player, layout) -> {
            if (bridge.detect(player)
                    == com.winkreyes.excelmenu.core.platform.Detection.State.BEDROCK) {
                editorForms.openRoot(player);
            } else {
                editorMenus.openRoot(player);
            }
        });
        formService.setTtlSeconds(getConfig().getLong("forms.session-ttl-seconds", 300L));
        menuService.setRegistryMenus(registryMenus);

        // Phase 2: the unified /menu family and the ported screens.
        ConfirmService confirmService = new ConfirmService(this, bridge,
                new SpawnTeleporter(this));
        menuService.setConfirmService(confirmService);
        this.confirmService = confirmService;
        formService.setScreenHook(new ScreenHooks(
                new AskScreen(this, bridge), new SkillsPerksScreen(this, bridge),
                () -> Bukkit.getPluginManager().isPluginEnabled("ExcelSkillsPanel")));
        TeleportScreen teleportScreen = new TeleportScreen(this, bridge);
        ChildproofScreen childproofScreen = new ChildproofScreen(this, bridge);
        MenuFamilyCommands familyCommands = new MenuFamilyCommands(menuService,
                entryService, registryMenus, bridge, new WebsiteScreen(this, bridge));

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
        getServer().getPluginManager().registerEvents(confirmService, this);
        getServer().getPluginManager().registerEvents(teleportScreen, this);
        getServer().getPluginManager().registerEvents(editorMenus, this);
        getServer().getPluginManager().registerEvents(prefsStore, this);

        MenuCommands commands = new MenuCommands(this, menuService, entryService,
                registryMenus, bridge);
        if (getCommand("excelmenu") != null) getCommand("excelmenu").setExecutor(commands);
        if (getCommand("dm") != null) getCommand("dm").setExecutor(commands);
        if (getCommand("menu") != null) getCommand("menu").setExecutor(familyCommands);
        if (getCommand("servercommands") != null) {
            getCommand("servercommands").setExecutor(familyCommands);
        }
        if (getCommand("serverwebsite") != null) {
            getCommand("serverwebsite").setExecutor(familyCommands);
        }
        if (getCommand("tpmenu") != null) getCommand("tpmenu").setExecutor(teleportScreen);
        if (getCommand("childproofmenu") != null) {
            getCommand("childproofmenu").setExecutor(childproofScreen);
        }
        if (getCommand("rearrange") != null) {
            getCommand("rearrange").setExecutor(new RearrangeCommand(
                    entryService, bridge, editorForms, editorMenus));
        }
        getServer().getServicesManager().register(ExcelMenuApi.class, entryService, this,
                ServicePriority.Normal);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getScheduler().runTaskTimer(this, this::refreshTick, 20L, 20L);

        getLogger().info("ExcelMenu " + getPluginMeta().getVersion() + " enabled (Phase 2:"
                + " unified — registry, definitions and the /menu family in one"
                + " plugin; ExcelBedrockMenu is retired). "
                + entryService.entries().size() + " entries, "
                + registryMenus.layoutIds().size() + " registry menus, "
                + menuService.menus().size() + " definition menus live.");
    }

    @Override
    public void onDisable() {
        if (menuService != null) menuService.retire();
        if (formService != null) formService.retire();
        if (confirmService != null) confirmService.retire();
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
        boolean rearrangeEnabled = cfg.getBoolean("rearrange.enabled", true);
        List<String> rearrangeMenus = cfg.getStringList("rearrange.menus");
        String pinColor = cfg.getString("pin-color", "&1");
        return new RegistryConfig(zone, message, groupOrder, groupColors,
                rearrangeEnabled, rearrangeMenus, pinColor);
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
