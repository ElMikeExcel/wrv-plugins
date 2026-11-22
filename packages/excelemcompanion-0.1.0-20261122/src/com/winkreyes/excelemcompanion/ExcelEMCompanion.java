package com.winkreyes.excelemcompanion;

import com.winkreyes.excelemcompanion.shared.MenuRenderer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * ExcelEMCompanion — one plugin, open module framework (D186).
 *
 * The core owns exactly three things: the shared menu renderer
 * (Java chest GUI + Bedrock Cumulus forms, one content model), the
 * module lifecycle driven by config toggles
 * (modules.<id>.enabled), and dynamic command registration so a
 * disabled module's commands do not exist. Every feature lives in
 * its module package:
 *   drop-guide       (/drops)    — what does THIS boss drop (D185)
 *   progress-tracker (/progress) — a player's own progress record
 *
 * The only command declared in plugin.yml is /companion itself
 * (reload). Module commands are registered at runtime, per module.
 */
public class ExcelEMCompanion extends JavaPlugin implements CommandExecutor {

    public static final String VERSION = "0.1.0";

    private MenuRenderer renderer;
    private final Map<String, CompanionModule> enabledModules =
            new LinkedHashMap<>();
    private final Map<String, List<PluginCommand>> moduleCommands =
            new HashMap<>();
    private final Map<String, Ctx> contexts = new HashMap<>();

    /** Per-module ModuleContext implementation. */
    private final class Ctx implements ModuleContext {
        private final CompanionModule module;

        Ctx(CompanionModule module) {
            this.module = module;
        }

        @Override
        public JavaPlugin plugin() {
            return ExcelEMCompanion.this;
        }

        @Override
        public java.util.logging.Logger logger() {
            return getLogger();
        }

        @Override
        public MenuRenderer renderer() {
            return renderer;
        }

        @Override
        public ConfigurationSection moduleConfig() {
            return getConfig().getConfigurationSection(
                    "modules." + module.id());
        }

        @Override
        public void registerCommand(String name, List<String> aliases,
                                    String usage, CommandExecutor executor) {
            PluginCommand cmd = newPluginCommand(name);
            if (aliases != null && !aliases.isEmpty()) {
                cmd.setAliases(new ArrayList<>(aliases));
            }
            if (usage != null) cmd.setUsage(usage);
            cmd.setExecutor(executor);
            Bukkit.getCommandMap().register("excelemcompanion", cmd);
            moduleCommands.computeIfAbsent(module.id(),
                    k -> new ArrayList<>()).add(cmd);
        }
    }

    private PluginCommand newPluginCommand(String name) {
        try {
            Constructor<PluginCommand> ctor =
                    PluginCommand.class.getDeclaredConstructor(
                            String.class, org.bukkit.plugin.Plugin.class);
            ctor.setAccessible(true);
            return ctor.newInstance(name, this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "cannot create dynamic command " + name, e);
        }
    }

    private void unregisterModuleCommands(String moduleId) {
        List<PluginCommand> cmds = moduleCommands.remove(moduleId);
        if (cmds == null || cmds.isEmpty()) return;
        try {
            Field known = SimpleCommandMap.class
                    .getDeclaredField("knownCommands");
            known.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands =
                    (Map<String, Command>) known.get(Bukkit.getCommandMap());
            for (PluginCommand cmd : cmds) {
                knownCommands.remove(cmd.getName());
                for (String alias : cmd.getAliases()) {
                    knownCommands.remove(alias);
                }
                cmd.unregister(Bukkit.getCommandMap());
            }
        } catch (ReflectiveOperationException e) {
            getLogger().log(Level.WARNING,
                    "command unregister cleanup failed for " + moduleId, e);
        }
    }

    // ------------------------------------------------------------ lifecycle

    @Override
    public void onEnable() {
        saveDefaultConfig();
        renderer = new MenuRenderer(this);
        Bukkit.getPluginManager().registerEvents(renderer, this);
        PluginCommand core = getCommand("companion");
        if (core != null) core.setExecutor(this);
        syncModules();
        getLogger().info("ExcelEMCompanion " + VERSION + " ready — modules: "
                + enabledModules.keySet());
    }

    @Override
    public void onDisable() {
        for (String id : new ArrayList<>(enabledModules.keySet())) {
            disableModule(id);
        }
    }

    /** Enables/disables modules to match the config toggles. */
    private void syncModules() {
        for (CompanionModule module : ModuleRegistry.all()) {
            boolean want = getConfig().getBoolean(
                    "modules." + module.id() + ".enabled", true);
            boolean have = enabledModules.containsKey(module.id());
            if (want && !have) {
                enableModule(module);
            } else if (!want && have) {
                disableModule(module.id());
            } else if (want) {
                try {
                    module.reload();
                } catch (Throwable t) {
                    getLogger().log(Level.WARNING,
                            "module " + module.id() + " reload failed", t);
                }
            }
        }
    }

    private void enableModule(CompanionModule module) {
        Ctx ctx = new Ctx(module);
        contexts.put(module.id(), ctx);
        try {
            module.onEnable(ctx);
            enabledModules.put(module.id(), module);
            getLogger().info("module enabled: " + module.displayName()
                    + " (" + module.id() + ")");
        } catch (Throwable t) {
            unregisterModuleCommands(module.id());
            contexts.remove(module.id());
            getLogger().log(Level.SEVERE,
                    "module " + module.id() + " failed to enable", t);
        }
    }

    private void disableModule(String moduleId) {
        CompanionModule module = enabledModules.remove(moduleId);
        if (module == null) return;
        try {
            module.onDisable();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING,
                    "module " + moduleId + " disable failed", t);
        }
        unregisterModuleCommands(moduleId);
        contexts.remove(moduleId);
        getLogger().info("module disabled: " + moduleId);
    }

    // ------------------------------------------------------------- /companion

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("excelemcompanion.admin")) {
                sender.sendMessage("§cYou do not have permission to reload the companion.");
                return true;
            }
            reloadConfig();
            syncModules();
            sender.sendMessage("§aExcelEMCompanion reloaded. Modules: "
                    + enabledModules.keySet());
            return true;
        }
        sender.sendMessage("§eExcelEMCompanion " + VERSION
                + " — modules: " + enabledModules.keySet());
        return true;
    }

    /** Test/rig hook: ids of the currently enabled modules. */
    public java.util.Set<String> enabledModuleIds() {
        return java.util.Collections.unmodifiableSet(enabledModules.keySet());
    }

    /** Test/rig hook: the shared renderer. */
    public MenuRenderer renderer() {
        return renderer;
    }

    /** Test/rig hook: an enabled module instance by id, or null. */
    public CompanionModule module(String id) {
        return enabledModules.get(id);
    }
}
