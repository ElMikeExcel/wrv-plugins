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
    private StyleHook style;
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
            try {
                style = new StyleHook(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic);
                style.loadSettings(getConfig().getConfigurationSection("wand-style"));
                style.loadPrefs();
                getServer().getPluginManager().registerEvents(style, this);
            } catch (Throwable t) {
                style = null;
                getLogger().warning("Wand style switch off (" + t + ").");
            }
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
        getLogger().info(style == null ? "Wand style switch OFF" : "Wand style switch " + (style.enabled() ? "ON" : "OFF")
                + " (default " + style.prefs().defaultStyle().key() + ", " + style.prefs().size() + " player choice(s) saved)");
        if (style != null) {
            // Players already online (plugin reload or late enable): put their style on now.
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try { style.apply(p); } catch (Throwable t) { getLogger().warning("Wand style error for " + p.getName() + ": " + t); }
                }
            }, 40L);
        }
    }

    @Override
    public void onDisable() {
        hook = null;
        style = null;
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
    /** Bars style for this player? True when the style switch is missing (0.1.0 behaviour). */
    boolean barsStyle(Player p) {
        StyleHook s = style;
        if (s == null || !s.enabled()) return true;
        return s.styleOf(p) == com.winkreyes.excelwandbars.core.WandStyle.BARS;
    }
    StyleHook styleHook() { return style; }

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
        if (name.equals("wandstyle")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only. Admins: /wandbars style <player> [classic|bars|clear]");
                return true;
            }
            if (style == null || !style.enabled()) {
                p.sendMessage(ChatColor.GRAY + "Wand controls can't be changed right now.");
                return true;
            }
            if (args.length == 0) { style.openChooser(p); return true; }
            com.winkreyes.excelwandbars.core.WandStyle pick = args[0].equalsIgnoreCase("switch")
                    ? style.styleOf(p).other() : com.winkreyes.excelwandbars.core.WandStyle.parse(args[0]);
            if (pick == null) {
                p.sendMessage(ChatColor.YELLOW + "Use /wandstyle classic or /wandstyle bars (or just /wandstyle to pick).");
                return true;
            }
            for (String line : style.choose(p, pick)) p.sendMessage(ChatColor.YELLOW + line);
            return true;
        }
        if (name.equals("wandbars")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                if (style != null) { style.loadSettings(getConfig().getConfigurationSection("wand-style")); style.loadPrefs(); }
                sender.sendMessage("ExcelWandBars settings reloaded.");
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("style")) {
                // Admin: /wandbars style <player> [classic|bars|clear|apply|unset]
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || style == null) { sender.sendMessage("No such player (or style switch off)."); return true; }
                if (args.length > 2) {
                    String a = args[2].toLowerCase(Locale.ROOT);
                    if (a.equals("clear")) {
                        style.prefs().clear(t.getUniqueId());
                        style.persist();
                        sender.sendMessage("style " + t.getName() + ": choice cleared, default " + style.prefs().defaultStyle().key() + " -> " + style.apply(t));
                        return true;
                    }
                    if (a.equals("apply")) { sender.sendMessage("style " + t.getName() + ": " + style.styleOf(t).key() + " -> " + style.apply(t)); return true; }
                    if (a.equals("unset")) { sender.sendMessage("style " + t.getName() + ": removed " + style.clearOverrides(t) + " wand override(s)"); return true; }
                    com.winkreyes.excelwandbars.core.WandStyle pick = com.winkreyes.excelwandbars.core.WandStyle.parse(a);
                    if (pick == null) { sender.sendMessage("Use classic, bars, clear, apply or unset."); return true; }
                    style.choose(t, pick);
                }
                sender.sendMessage("style " + t.getName() + ": " + style.styleOf(t).key()
                        + (style.prefs().hasChoice(t.getUniqueId()) ? " (picked)" : " (default)") + " wand " + style.describe(t));
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
                    + ", compass guard " + (compassGuard != null ? "ON, trimmed " + compassGuard.trimmed() : "OFF")
                    + ", wand style " + (style == null ? "OFF" : (style.enabled() ? "ON" : "OFF") + " default " + style.prefs().defaultStyle().key()
                    + " saved " + style.prefs().size() + " applied " + style.applied()));
            return true;
        }
        return false;
    }
}
