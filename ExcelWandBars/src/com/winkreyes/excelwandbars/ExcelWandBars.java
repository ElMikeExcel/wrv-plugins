package com.winkreyes.excelwandbars;

import com.winkreyes.excelwandbars.core.FlipGate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ExcelWandBars: a sneak press flips the open Magic (elmakers) spell bar,
 * and /spellinfo shows WoW-style spell info (Bedrock form or Java chat).
 * Separate plugin; does not touch EliteMobs (MagmaGuy) or Magic's files.
 */
public final class ExcelWandBars extends JavaPlugin {
    private MagicHook hook;
    private CompassGuard compassGuard;
    private final FlipGate.Options flipOptions = new FlipGate.Options();
    private boolean showBarLine = true;
    private boolean debug = false;
    private boolean spellBookEnabled = true;
    private final List<String> skipWorlds = new ArrayList<>();
    private final List<String> skipPrefixes = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) {
            getLogger().warning("Magic not found: sneak bar flip and /spellinfo are OFF.");
            return;
        }
        try {
            hook = new MagicHook(this, magic);
            getServer().getPluginManager().registerEvents(hook, this);
            if (getConfig().getBoolean("compass-guard.enabled", true)) {
                try {
                    compassGuard = new CompassGuard(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic,
                            getConfig().getStringList("compass-guard.keys"));
                    getServer().getPluginManager().registerEvents(compassGuard, this);
                } catch (Throwable t) {
                    compassGuard = null;
                    getLogger().warning("Compass guard off (" + t + ").");
                }
            }
        } catch (Throwable t) {
            hook = null;
            getLogger().severe("Magic API hook failed (" + t.getClass().getSimpleName() + ": " + t.getMessage() + "), plugin idle.");
            return;
        }
        getLogger().info("Sneak bar flip " + (flipOptions.enabled ? "ON" : "OFF")
                + " (debounce " + flipOptions.debounceMs + " ms), spell book " + (spellBookEnabled ? "ON" : "OFF")
                + ", skip worlds " + skipWorlds + " prefixes " + skipPrefixes
                + ", compass guard " + (compassGuard == null ? "OFF" : "ON " + compassGuard.keys()));
    }

    @Override
    public void onDisable() {
        hook = null;
        compassGuard = null;
    }

    void loadSettings() {
        reloadConfig();
        FileConfiguration c = getConfig();
        flipOptions.enabled = c.getBoolean("sneak-flip.enabled", true);
        flipOptions.debounceMs = Math.max(0, c.getLong("sneak-flip.debounce-ms", 250));
        flipOptions.skipVehicle = c.getBoolean("sneak-flip.skip-in-vehicle", true);
        flipOptions.skipSwimming = c.getBoolean("sneak-flip.skip-swimming", true);
        flipOptions.skipGliding = c.getBoolean("sneak-flip.skip-gliding", true);
        flipOptions.skipFlying = c.getBoolean("sneak-flip.skip-flying", true);
        showBarLine = c.getBoolean("sneak-flip.show-bar-line", true);
        spellBookEnabled = c.getBoolean("spell-book.enabled", true);
        debug = c.getBoolean("debug", false);
        skipWorlds.clear();
        for (String w : c.getStringList("skip-worlds")) skipWorlds.add(w.toLowerCase(Locale.ROOT));
        skipPrefixes.clear();
        for (String w : c.getStringList("skip-world-prefixes")) skipPrefixes.add(w.toLowerCase(Locale.ROOT));
    }

    FlipGate.Options flipOptions() { return flipOptions; }
    boolean showBarLine() { return showBarLine; }
    boolean debug() { return debug; }
    boolean spellBookEnabled() { return spellBookEnabled; }

    boolean skipWorld(World w) {
        if (w == null) return false;
        String n = w.getName().toLowerCase(Locale.ROOT);
        if (skipWorlds.contains(n)) return true;
        for (String p : skipPrefixes) if (!p.isEmpty() && n.startsWith(p)) return true;
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("spellinfo")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (hook == null || !spellBookEnabled) {
                p.sendMessage(ChatColor.GRAY + "The spell book is off right now.");
                return true;
            }
            hook.openSpellBook(p, args.length > 0 ? args[0] : null);
            return true;
        }
        if (name.equals("wandbars")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                sender.sendMessage("ExcelWandBars settings reloaded.");
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("flip")) {
                // Admin/test hook: run the same flip path as a sneak press, for a named player.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || hook == null) { sender.sendMessage("No such player (or Magic missing)."); return true; }
                sender.sendMessage("flip " + t.getName() + ": " + hook.handleSneak(t, true));
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("info")) {
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || hook == null) { sender.sendMessage("No such player (or Magic missing)."); return true; }
                for (String line : hook.describeForConsole(t)) sender.sendMessage(line);
                return true;
            }
            sender.sendMessage("ExcelWandBars " + getDescription().getVersion() + ": flip "
                    + (flipOptions.enabled ? "ON" : "OFF") + ", debounce " + flipOptions.debounceMs + " ms, spell book "
                    + (spellBookEnabled ? "ON" : "OFF") + ", Magic hook " + (hook != null ? "OK" : "MISSING")
                    + (hook != null ? ", flips " + hook.flips() : "")
                    + ", compass guard " + (compassGuard != null ? "ON, trimmed " + compassGuard.trimmed() : "OFF"));
            return true;
        }
        return false;
    }
}
