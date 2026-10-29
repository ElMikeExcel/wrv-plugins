// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.MenuDef;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelMenu — the valley's own menu plugin, replacing DeluxeMenus.
 *
 * Why it exists: on 2026-10-20 the cosmetics store would not open
 * for Java players because DeluxeMenus had never registered the two
 * new menu files (their registration step was missed at install)
 * and nothing anywhere said so — Bedrock kept working through a
 * different door, so the failure hid. ExcelMenu loads its own
 * definitions, validates them semantically (every navigation target
 * must exist), and logs a boot self-check naming EVERY menu it
 * loaded; a definition that fails is refused at SEVERE while the
 * rest serve. A dead button should be impossible to miss ever again.
 *
 * One definition renders both platforms: Java gets a chest GUI,
 * Bedrock gets a Cumulus form built from the same visible-item set.
 */
public final class ExcelMenu extends JavaPlugin {

    /** The migration set: bundled definitions extracted on first run. */
    static final String[] BUNDLED = {
            "basics_menu", "commands_menu", "server_menu", "skills_perks",
            "spawnshop", "spawnshop_building", "spawnshop_enchanting",
            "spawnshop_food", "spawnshop_potions_protection",
            "spawnshop_travel_animals", "spawnshop_cosmetics_colors",
            "spawnshop_cosmetics_sets", "teleport_confirm_guild",
            "teleport_confirm_home", "teleport_confirm_spawn",
            "teleport_confirm_spawncmd",
    };

    private MenuService service;
    private Map<String, List<String>> lastFailures = Map.of();
    private final List<String> registeredCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        service = new MenuService(this);
        File menusDir = extractBundledMenus();

        MenuService.LoadOutcome outcome = service.loadAll(menusDir.toPath());
        lastFailures = outcome.failed();
        service.installSet(outcome);
        registerMenuCommands();

        getServer().getPluginManager().registerEvents(new MenuListener(service), this);
        MenuCommands commands = new MenuCommands(this, service, this);
        if (getCommand("excelmenu") != null) getCommand("excelmenu").setExecutor(commands);
        if (getCommand("dm") != null) getCommand("dm").setExecutor(commands);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        // Placeholder refresh for update:true items (the shop balance
        // readouts): re-render those slots on each menu's interval.
        getServer().getScheduler().runTaskTimer(this, this::refreshTick, 20L, 20L);

        getLogger().info("ExcelMenu " + getPluginMeta().getVersion() + " enabled: " + service.menus().size()
                + " menus live, " + lastFailures.size() + " failed."
                + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                        ? " PlaceholderAPI hooked." : " PlaceholderAPI not found — "
                        + "%placeholders% beyond ExcelMenu's own will render literally."));
    }

    @Override
    public void onDisable() {
        unregisterMenuCommands();
    }

    private File extractBundledMenus() {
        File dir = new File(getDataFolder(), "menus");
        if (!dir.exists() && !dir.mkdirs()) {
            getLogger().severe("ExcelMenu: cannot create " + dir);
            return dir;
        }
        for (String name : BUNDLED) {
            File f = new File(dir, name + ".yml");
            if (!f.exists()) {
                try {
                    saveResource("menus/" + name + ".yml", false);
                } catch (IllegalArgumentException e) {
                    getLogger().severe("ExcelMenu: bundled definition missing from jar: "
                            + name + " — " + e.getMessage());
                }
            }
        }
        return dir;
    }

    void reloadMenus(CommandSender sender) {
        File dir = new File(getDataFolder(), "menus");
        MenuService.LoadOutcome outcome = service.loadAll(dir.toPath());
        if (outcome.loaded().isEmpty() && !service.menus().isEmpty()) {
            getLogger().severe("ExcelMenu reload REJECTED: the new set loaded zero menus; "
                    + "the previous " + service.menus().size() + " stay live.");
            sender.sendMessage("§cExcelMenu reload rejected — new set is empty; "
                    + "previous menus stay live. See console.");
            return;
        }
        lastFailures = outcome.failed();
        service.installSet(outcome);
        registerMenuCommands();
        sender.sendMessage("§aExcelMenu reloaded: " + outcome.loaded().size()
                + " menus, " + outcome.failed().size() + " failed.");
    }

    Map<String, List<String>> lastFailures() {
        return lastFailures;
    }

    // ---------- dynamic open commands (/shop, /basicsmenu) ----------

    private void registerMenuCommands() {
        unregisterMenuCommands();
        CommandMap map = getServer().getCommandMap();
        Map<String, String> wanted = new HashMap<>();
        for (MenuDef def : service.menus().values()) {
            if (def.command != null && !def.command.isBlank()) {
                wanted.put(def.command, def.id);
            }
        }
        for (Map.Entry<String, String> e : wanted.entrySet()) {
            map.register("excelmenu",
                    new MenuCommands.MenuOpenCommand(service, e.getValue(), e.getKey()));
            registeredCommands.add(e.getKey());
            getLogger().info("ExcelMenu: registered command /" + e.getKey()
                    + " -> " + e.getValue());
        }
    }

    private void unregisterMenuCommands() {
        if (registeredCommands.isEmpty()) return;
        Map<String, org.bukkit.command.Command> known = getServer().getCommandMap().getKnownCommands();
        for (String name : registeredCommands) {
            known.remove(name);
            known.remove("excelmenu:" + name);
        }
        registeredCommands.clear();
    }

    // ---------- placeholder refresh tick ----------

    private void refreshTick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (!(holder instanceof MenuHolder mh)) continue;
            MenuDef def = service.menu(mh.menuId());
            if (def == null || def.updateInterval <= 0) continue;
            if (now - mh.lastRenderMillis() < def.updateInterval * 1000L) continue;
            service.fillInventory(player, def, mh.getInventory(), mh, true);
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
