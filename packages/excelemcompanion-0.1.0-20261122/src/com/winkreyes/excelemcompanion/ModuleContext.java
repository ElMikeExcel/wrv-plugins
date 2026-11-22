package com.winkreyes.excelemcompanion;

import com.winkreyes.excelemcompanion.shared.MenuRenderer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/** What the core hands a module while it is enabled. */
public interface ModuleContext {

    JavaPlugin plugin();

    Logger logger();

    MenuRenderer renderer();

    /** The module's own config section (modules.<id>), or null. */
    ConfigurationSection moduleConfig();

    /**
     * Registers a command for this module (dynamic registration —
     * module commands are NOT declared in plugin.yml, so a disabled
     * module's commands truly do not exist). Aliases may be empty.
     * The command is unregistered automatically when the module is
     * disabled.
     */
    void registerCommand(String name, java.util.List<String> aliases,
                         String usage, CommandExecutor executor);
}
