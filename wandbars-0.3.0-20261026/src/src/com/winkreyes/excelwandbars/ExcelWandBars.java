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
    private TipHook tips;
    private SpellScreen screen;
    private SpellTipHook spellTip;
    private TapBarHook tapBar;
    /** Rank-up by casting (2026-10-26). */
    private RankUpHook rankUp;
    private WandSlotHook wandSlot;
    /** 0.2.9: each player's own spell bar. */
    private LoadoutHook loadouts;
    private String geyserTap = "off";
    /** Admin test aid (/wandbars simbedrock): treat these online Java players as Bedrock. Memory only, cleared on quit. */
    private final java.util.Set<java.util.UUID> simBedrock = new java.util.HashSet<>();
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
            if (style != null) {
                try {
                    // 0.2.4: Tap bar / Tap to cycle (original) open on hold, item-safe close.
                    tapBar = new TapBarHook(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic);
                    tapBar.loadSettings(getConfig().getConfigurationSection("tap-bar"));
                    getServer().getPluginManager().registerEvents(tapBar, this);
                    tapBar.start();
                } catch (Throwable t) {
                    tapBar = null;
                    getLogger().warning("Tap bar opener off (" + t + ").");
                }
                try {
                    // 0.2.7: the wand lives in the last hotbar slot (Tap bar / Tap to pick by default).
                    wandSlot = new WandSlotHook(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic);
                    wandSlot.loadSettings(getConfig().getConfigurationSection("wand-slot"));
                    getServer().getPluginManager().registerEvents(wandSlot, this);
                    wandSlot.start();
                } catch (Throwable t) {
                    wandSlot = null;
                    getLogger().warning("Wand slot off (" + t + ").");
                }
                if (tapBar != null) installGeyserTap();
            }
            try {
                tips = new TipHook(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic);
                tips.loadSettings(getConfig().getConfigurationSection("wand-tip"));
                tips.load();
                getServer().getPluginManager().registerEvents(tips, this);
                getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) { simBedrock.remove(e.getPlayer().getUniqueId()); if (style != null) style.forgetChooser(e.getPlayer()); }
                }, this);
            } catch (Throwable t) {
                tips = null;
                getLogger().warning("Wand tip off (" + t + ").");
            }
            try {
                // 0.2.2: Bedrock Classic spell list as a form, progress line, Spell Book tip.
                spellTip = new SpellTipHook(this);
                spellTip.loadSettings(getConfig().getConfigurationSection("spell-tip"));
                spellTip.load();
                getServer().getPluginManager().registerEvents(spellTip, this);
                screen = new SpellScreen(this, hook);
                screen.loadSettings(getConfig().getConfigurationSection("spell-screen"), getConfig().getConfigurationSection("progress-line"));
                getServer().getPluginManager().registerEvents(screen, this);
            } catch (Throwable t) {
                screen = null;
                getLogger().warning("Spell screen off (" + t + ").");
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
            try {
                // 0.2.9: each player's own spell bar (bar-loadout.enabled: false = 0.2.8 paging).
                loadouts = new LoadoutHook(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic);
                loadouts.loadSettings(getConfig().getConfigurationSection("bar-loadout"));
                loadouts.load();
                getServer().getPluginManager().registerEvents(loadouts, this);
                getLogger().info("Own spell bar per player " + loadouts.summary() + ".");
            } catch (Throwable t) {
                loadouts = null;
                getLogger().warning("Own spell bar off (" + t + "), every spell on the bar in pages as in 0.2.8.");
            }
            try {
                // Rank-up by casting (2026-10-26): off until rank-up-casting.enabled is true (reloadable).
                rankUp = new RankUpHook(this, (com.elmakers.mine.bukkit.api.magic.MagicAPI) magic);
                rankUp.loadSettings(getConfig().getConfigurationSection("rank-up-casting"));
                rankUp.load();
                getServer().getPluginManager().registerEvents(rankUp, this);
                getServer().getScheduler().runTaskTimer(this, () -> { if (rankUp != null) rankUp.save(); }, 6000L, 6000L);
                getLogger().info("Rank-up by casting " + (rankUp.enabled ? "ON" : "off (rank-up-casting.enabled: false)") + ".");
            } catch (Throwable t) {
                rankUp = null;
                getLogger().warning("Rank-up by casting off (" + t + ").");
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
                + " (default " + style.prefs().defaultStyle().key() + ", " + style.prefs().size() + " player choice(s) saved"
                + ", sneak bars " + (style.barsBedrockOnly() ? "Bedrock only" : "everyone") + ", " + style.pagesSummary() + ")");
        getLogger().info(tapBar == null ? "Tap bar opener OFF" : "Tap bar / Tap to pick (original): " + tapBar.summary());
        getLogger().info(wandSlot == null ? "Wand slot OFF" : "Wand slot " + wandSlot.summary());
        getLogger().info(screen == null ? "Spell screen OFF" : "Spell screen: Bedrock Classic spell list as a form " + (screen.bedrockForm() ? "ON" : "OFF")
                + ", progress line " + (screen.gate().enabled ? "ON (min gap " + screen.gate().minGapMs + " ms, skip during stance " + screen.gate().skipDuringStance + ")" : "OFF")
                + ", Spell Book tip " + (spellTip == null || !spellTip.gate().enabled ? "OFF" : "ON (until OK, " + spellTip.gate().known().size() + " player(s) tracked)"));
        getLogger().info(tips == null ? "Wand tip OFF" : "Wand tip " + (tips.gate().enabled ? "ON" : "OFF")
                + " (first " + tips.gate().maxUses + " wand uses until OK, " + tips.gate().known().size() + " player(s) tracked)");
        if (style != null) {
            // Players already online (plugin reload or late enable): put their style on now.
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try { style.apply(p); } catch (Throwable t) { getLogger().warning("Wand style error for " + p.getName() + ": " + t); }
                }
            }, 40L);
        }
        // 0.3.0: after Magic has loaded its wands (5 s): Spell Book group check.
        Bukkit.getScheduler().runTaskLater(this, this::checkGroups, 100L);
    }

    @Override
    public void onDisable() {
        if (!"off".equals(geyserTap)) {
            try { Object r = Class.forName("com.winkreyes.excelwandbars.GeyserTapHook").getMethod("uninstall").invoke(null); getLogger().info("Geyser wand tap: " + r); }
            catch (Throwable ignored) { }
            geyserTap = "off";
        }
        if (wandSlot != null) { try { wandSlot.stop(); } catch (Throwable ignored) { } }
        wandSlot = null;
        if (tapBar != null) { try { tapBar.stop(); } catch (Throwable ignored) { } }
        tapBar = null;
        if (tips != null) tips.save();
        if (spellTip != null) spellTip.save();
        if (rankUp != null) rankUp.save();
        rankUp = null;
        if (loadouts != null) loadouts.save();
        loadouts = null;
        tips = null;
        spellTip = null;
        screen = null;
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
        spellBookGroups = c.getBoolean("spell-book.groups", true);
        showLocked = c.getBoolean("spell-book.show-locked", true);
        loadGroups();
        debug = c.getBoolean("debug", false);
        skipWorlds.clear();
        for (String w : c.getStringList("skip-worlds")) skipWorlds.add(w.toLowerCase(Locale.ROOT));
        skipPrefixes.clear();
        for (String w : c.getStringList("skip-world-prefixes")) skipPrefixes.add(w.toLowerCase(Locale.ROOT));
        rankNames.clear();
        org.bukkit.configuration.ConfigurationSection rn = c.getConfigurationSection("rank-names");
        if (rn != null) for (String k : rn.getKeys(false)) { String v = rn.getString(k, ""); if (v != null && !v.isBlank()) rankNames.put(k.toLowerCase(Locale.ROOT), v.trim()); }
        else rankNames.put("master", "Master");
        rankUpHow.clear();
        org.bukkit.configuration.ConfigurationSection ru = c.getConfigurationSection("spell-info.rank-up");
        if (ru != null) for (String k : ru.getKeys(false)) rankUpHow.put(k.toLowerCase(Locale.ROOT), ru.getString(k, ""));
    }

    /** Spell info 2026-10-26: spell-info.rank-up text, per wand rank key, then "default". {rank} = rank name. Reloadable. */
    private final java.util.Map<String, String> rankUpHow = new java.util.HashMap<>();

    static final String DEFAULT_RANK_UP = "{rank} rank is not open yet. Watch What's New.";

    String rankUpHow(String rankKey, String rankName) {
        String t = rankKey == null ? null : rankUpHow.get(rankKey.toLowerCase(Locale.ROOT));
        if (t == null) t = rankUpHow.get("default");
        if (t == null) t = DEFAULT_RANK_UP; // section missing from an older config.yml
        if (t == null) return "";
        return t.replace("{rank}", rankName == null ? "" : rankName).replace("\u2014", ",").trim();
    }

    FlipGate.Options flipOptions() { return flipOptions; }
    boolean showBarLine() { return showBarLine; }
    boolean debug() { return debug; }
    boolean spellBookEnabled() { return spellBookEnabled; }

    // ------------------------------------------------------------------ 0.3.0 Spell Book groups
    private boolean spellBookGroups = true, showLocked = true;
    private com.winkreyes.excelwandbars.core.SpellbookGroups groups = new com.winkreyes.excelwandbars.core.SpellbookGroups();
    private String groupsSource = "none";
    boolean spellBookGroups() { return spellBookGroups; }
    boolean showLocked() { return showLocked; }
    com.winkreyes.excelwandbars.core.SpellbookGroups spellbookGroups() { return groups; }

    /** spellbook.yml in the plugin folder (written from the jar when missing). Broken or empty: the jar copy, with a warning. */
    private void loadGroups() {
        com.winkreyes.excelwandbars.core.SpellbookGroups g = null;
        try {
            java.io.File f = new java.io.File(getDataFolder(), "spellbook.yml");
            if (!f.exists()) saveResource("spellbook.yml", false);
            org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
            y.load(f);
            g = com.winkreyes.excelwandbars.core.SpellbookGroups.parse(asMap(y));
            for (String w : g.problems()) getLogger().warning("spellbook.yml: " + w);
            if (g.groups().isEmpty()) { getLogger().warning("spellbook.yml has no groups, using the groups built into the jar."); g = null; }
            else groupsSource = "spellbook.yml";
        } catch (Throwable t) {
            getLogger().warning("spellbook.yml could not be read (" + t.getClass().getSimpleName() + ": " + t.getMessage() + "), using the groups built into the jar.");
            g = null;
        }
        if (g == null) {
            try (java.io.InputStream in = getResource("spellbook.yml")) {
                org.bukkit.configuration.file.YamlConfiguration y = new org.bukkit.configuration.file.YamlConfiguration();
                if (in != null) y.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                g = com.winkreyes.excelwandbars.core.SpellbookGroups.parse(asMap(y));
                groupsSource = "jar";
            } catch (Throwable t) {
                g = new com.winkreyes.excelwandbars.core.SpellbookGroups();
                groupsSource = "none";
            }
        }
        groups = g;
        for (String d : g.duplicates()) getLogger().warning("spellbook.yml: " + d);
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> asMap(org.bukkit.configuration.ConfigurationSection y) {
        Object o = MagicHook.plain(y);
        return o instanceof java.util.Map ? (java.util.Map<String, Object>) o : new java.util.HashMap<>();
    }

    /** Boot (5 s after start) and /wandbars reload: rank-wand spells that are in no group go to "Other". */
    void checkGroups() {
        if (hook == null) return;
        List<String> spells = hook.ladderSpells();
        List<String> loose = groups.unlisted(spells);
        getLogger().info("Spell Book groups " + (spellBookGroups ? "ON" : "OFF") + " (" + groupsSource + ", " + groups.groups().size()
                + " groups, " + groups.spellCount() + " spells listed, " + spells.size() + " on the rank wands now), locked spells "
                + (showLocked ? "shown" : "hidden") + ".");
        if (!loose.isEmpty()) getLogger().warning("Spell Book: " + loose.size() + " spell(s) on the rank wands are in no group, shown under \""
                + groups.otherName() + "\": " + String.join(", ", loose) + ". Add them to spellbook.yml, then /wandbars reload.");
    }
    /** Bars style for this player? True when the style switch is missing (0.1.0 behaviour). */
    boolean barsStyle(Player p) {
        StyleHook s = style;
        if (s == null || !s.enabled()) return true;
        // 0.2.4: every inventory-mode style (Sneak bars, Tap to cycle (original), Tap bar) has bars to flip.
        return s.styleOf(p).inventory();
    }
    TapBarHook tapBar() { return tapBar; }
    RankUpHook rankUp() { return rankUp; }
    LoadoutHook loadouts() { return loadouts; }
    /** 0.2.8: player-facing rank names (config rank-names). Magic's template and path keys never change. */
    private final java.util.Map<String, String> rankNames = new java.util.HashMap<>();
    String rankDisplay(String key, String magicName) {
        String v = key == null ? null : rankNames.get(key.toLowerCase(Locale.ROOT));
        if (v != null) return v;
        return magicName == null ? "" : magicName;
    }
    WandSlotHook wandSlot() { return wandSlot; }

    /**
     * 0.2.7: Bedrock "tap the wand" (Tap bar). Geyser does not forward a tap on the slot that is
     * already selected, so GeyserTapHook notes it inside Geyser and we act on the main thread.
     * Loaded by name only when Geyser-Spigot runs; tap-bar.tap-wand: off makes every note a no-op.
     */
    private int geyserRetries = 0;

    private void installGeyserTap() {
        Plugin g = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (g == null || !g.isEnabled()) { geyserTap = "off"; tapBar.setTapWandSource("no Geyser"); return; }
        try {
            final ExcelWandBars self = this;
            java.util.function.BiConsumer<java.util.UUID, Integer> cb = (id, slot) -> {
                try {
                    if (!self.isEnabled()) return;
                    Bukkit.getScheduler().runTask(self, () -> {
                        Player p = Bukkit.getPlayer(id);
                        TapBarHook tb = tapBar;
                        if (p != null && tb != null) {
                            String r = tb.onWandTap(p, slot);
                            if (debug) getLogger().info("Wand tap from " + p.getName() + " slot " + (slot + 1) + ": " + r);
                        }
                    });
                } catch (Throwable ignored) { }
            };
            Object r = Class.forName("com.winkreyes.excelwandbars.GeyserTapHook")
                    .getMethod("install", java.util.function.BiConsumer.class).invoke(null, cb);
            geyserTap = String.valueOf(r);
            tapBar.setTapWandSource("Geyser " + geyserTap);
            // Geyser fills its translator table when it starts, after the server has loaded the
            // plugins, so on boot the first try finds none. Try again once a second for a minute.
            if ("no Geyser translator".equals(geyserTap) && geyserRetries++ < 60) {
                Bukkit.getScheduler().runTaskLater(this, () -> { if (isEnabled() && tapBar != null) installGeyserTap(); }, 20L);
            } else if ("installed".equals(geyserTap)) {
                getLogger().info("Tap bar tap the wand: Geyser hook installed.");
            } else if ("no Geyser translator".equals(geyserTap)) {
                getLogger().warning("Tap bar tap the wand off: Geyser never loaded its translators. Items back still works: the empty slot, or press and hold the wand slot.");
            }
        } catch (Throwable t) {
            geyserTap = "off";
            tapBar.setTapWandSource("Geyser hook failed: " + t.getClass().getSimpleName());
            getLogger().warning("Geyser wand tap off (" + t + "). Items back still works: the empty slot, or press and hold the wand slot.");
        }
    }
    MagicHook magicHook() { return hook; }
    StyleHook styleHook() { return style; }
    TipHook tips() { return tips; }
    SpellTipHook spellTip() { return spellTip; }
    SpellScreen spellScreen() { return screen; }
    /** On the admin test list only (no real Geyser connection). */
    boolean simulatedBedrock(Player p) { return simBedrock.contains(p.getUniqueId()) && !BedrockForms.isBedrock(p); }

    /** Bedrock (Geyser / Floodgate) player? Plus the admin test list. */
    boolean isBedrock(Player p) {
        return simBedrock.contains(p.getUniqueId()) || BedrockForms.isBedrock(p);
    }

    boolean skipWorld(World w) {
        if (w == null) return false;
        String n = w.getName().toLowerCase(Locale.ROOT);
        if (skipWorlds.contains(n)) return true;
        for (String p : skipPrefixes) if (!p.isEmpty() && n.startsWith(p)) return true;
        return false;
    }

    /** 1-based page word to a 0-based page (bad or missing = first page). */
    static int pageArg(String[] args, int i) {
        if (args.length <= i) return 0;
        try { return Math.max(0, Integer.parseInt(args[i].trim()) - 1); } catch (NumberFormatException e) { return 0; }
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
            if (args.length > 0 && (args[0].equalsIgnoreCase("ok") || args[0].equalsIgnoreCase("gotit"))) {
                if (spellTip != null) spellTip.ack(p, true);
                return true;
            }
            // 0.3.0: /spellbook group <name> [page], /spellbook all [page], /spellbook locked <spell>.
            // ("group", "all" and "locked" are not Magic spell keys; a group named like a spell, e.g.
            // Healing and the healing spell, needs the word "group".)
            if (spellBookGroups && args.length > 0) {
                String a0 = args[0].toLowerCase(Locale.ROOT);
                if (a0.equals("group") && args.length > 1) { hook.openGroup(p, args[1], pageArg(args, 2)); return true; }
                if (a0.equals("all")) { hook.openGroup(p, com.winkreyes.excelwandbars.core.SpellbookText.ALL, pageArg(args, 1)); return true; }
                if (a0.equals("locked") && args.length > 1) { hook.openLocked(p, args[1]); return true; }
                if (a0.equals("group")) { hook.openSpellBook(p, null); return true; }
            }
            hook.openSpellBook(p, args.length > 0 ? args[0] : null);
            return true;
        }
        if (name.equals("spellbar")) {
            // 0.2.9: change your own spell bar (Bedrock form or Java clickable chat).
            if (!(sender instanceof Player p)) { sender.sendMessage("Players only. Admins: /wandbars loadout <player>"); return true; }
            if (loadouts == null || hook == null) { p.sendMessage(ChatColor.GRAY + com.winkreyes.excelwandbars.core.LoadoutText.OFF); return true; }
            loadouts.handle(p, args);
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
            if (args[0].equalsIgnoreCase("ok") || args[0].equalsIgnoreCase("gotit")) {
                if (tips != null) tips.ack(p);
                p.sendMessage(ChatColor.GREEN + com.winkreyes.excelwandbars.core.TipGate.DONE_ACK);
                return true;
            }
            com.winkreyes.excelwandbars.core.WandStyle pick = args[0].equalsIgnoreCase("switch")
                    ? style.styleOf(p).other() : com.winkreyes.excelwandbars.core.WandStyle.parse(args[0]);
            if (pick == null) {
                p.sendMessage(ChatColor.YELLOW + (style.barsAllowed(p)
                        ? "Use /wandstyle classic, sneakbars, cycle, original or tapbar (or just /wandstyle to pick)."
                        : "Use /wandstyle classic, cycle, original or tapbar (or just /wandstyle to pick)."));
                return true;
            }
            for (String line : style.choose(p, pick)) p.sendMessage(ChatColor.YELLOW + line);
            return true;
        }
        if (name.equals("wandbars")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                loadSettings();
                if (style != null) { style.loadSettings(getConfig().getConfigurationSection("wand-style")); style.loadPrefs(); }
                if (tips != null) { tips.save(); tips.loadSettings(getConfig().getConfigurationSection("wand-tip")); tips.load(); }
                if (spellTip != null) { spellTip.save(); spellTip.loadSettings(getConfig().getConfigurationSection("spell-tip")); spellTip.load(); }
                if (tapBar != null) tapBar.loadSettings(getConfig().getConfigurationSection("tap-bar"));
                if (wandSlot != null) wandSlot.loadSettings(getConfig().getConfigurationSection("wand-slot"));
                if (screen != null) screen.loadSettings(getConfig().getConfigurationSection("spell-screen"), getConfig().getConfigurationSection("progress-line"));
                if (rankUp != null) { rankUp.save(); rankUp.loadSettings(getConfig().getConfigurationSection("rank-up-casting")); }
                if (loadouts != null) { loadouts.loadSettings(getConfig().getConfigurationSection("bar-loadout")); loadouts.save(); }
                checkGroups();
                sender.sendMessage("ExcelWandBars settings reloaded." + (rankUp != null ? " Rank-up by casting: " + (rankUp.enabled ? "ON" : "off") + "." : "")
                        + (loadouts != null ? " Own spell bar: " + loadouts.summary() + "." : "")
                        + " Spell Book groups: " + (spellBookGroups ? groups.groups().size() + " (" + groupsSource + ")" : "off") + ".");
                if (style != null) for (Player op : Bukkit.getOnlinePlayers()) Bukkit.getScheduler().runTaskLater(this, () -> { if (op.isOnline() && style != null) style.applyNow(op, "reload"); }, 2L);
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("groups")) {
                // 0.3.0 admin/rig: /wandbars groups [player] = group check, plus that player's Spell Book counts.
                checkGroups();
                sender.sendMessage("Spell Book groups " + (spellBookGroups ? "ON" : "OFF") + " from " + groupsSource + ": " + groups.groups().size()
                        + " groups, " + groups.spellCount() + " spells. Locked spells " + (showLocked ? "shown" : "hidden") + ".");
                if (args.length > 1 && hook != null) {
                    Player t = Bukkit.getPlayerExact(args[1]);
                    if (t == null) { sender.sendMessage("Not online: " + args[1]); return true; }
                    for (String line : hook.describeGroups(t)) sender.sendMessage(line);
                }
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("loadout")) {
                // 0.2.9 admin/rig: /wandbars loadout <player> [show | reseed | do <spellbar args...>]
                if (loadouts == null) { sender.sendMessage("Own spell bar is not loaded."); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage("Player not online: " + args[1]); return true; }
                String a = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "show";
                if (a.equals("reseed")) sender.sendMessage("reseed: " + loadouts.reseed(t));
                else if (a.equals("do")) sender.sendMessage("do: " + loadouts.handle(t, java.util.Arrays.copyOfRange(args, 3, args.length)));
                else if (a.equals("save")) { loadouts.save(); sender.sendMessage("saved"); }
                for (String l : loadouts.describe(t)) sender.sendMessage(l);
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("rankup")) {
                // Admin/console: /wandbars rankup <player> [status | check | addsp <n>]
                if (rankUp == null) { sender.sendMessage("Rank-up by casting is not loaded."); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage("Player not online: " + args[1]); return true; }
                String a = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "status";
                if (a.equals("addsp") && args.length > 3) {
                    com.elmakers.mine.bukkit.api.magic.Mage m = ((com.elmakers.mine.bukkit.api.magic.MagicAPI) Bukkit.getPluginManager().getPlugin("Magic")).getController().getMage(t);
                    m.addCurrency("sp", Double.parseDouble(args[3]), true);
                    sender.sendMessage("sp now " + m.getCurrency("sp"));
                } else if (a.equals("check")) {
                    sender.sendMessage("check: " + rankUp.forceCheck(t));
                }
                for (String l : rankUp.status(t)) sender.sendMessage(l);
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("spelldump")) {
                // Admin/console: /wandbars spelldump <player>  or  /wandbars spelldump template <key>
                if (hook == null) { sender.sendMessage("Magic hook is off."); return true; }
                if (args[1].equalsIgnoreCase("template") && args.length > 2) {
                    for (String l : hook.dumpTemplate(args[2])) sender.sendMessage(l);
                    return true;
                }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage("Player not online: " + args[1]); return true; }
                for (String l : hook.dump(t)) sender.sendMessage(l);
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
                    if (pick == null) { sender.sendMessage("Use classic, bars, cycle, original, tapbar, clear, apply or unset."); return true; }
                    style.choose(t, pick);
                }
                sender.sendMessage("style " + t.getName() + ": " + style.styleOf(t).key()
                        + (style.prefs().hasChoice(t.getUniqueId()) ? " (picked)" : " (default)") + " wand " + style.describe(t));
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("chooser")) {
                // 0.2.3 admin/rig: build the Wand controls chooser for a player and print its buttons and text.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || style == null) { sender.sendMessage("No such player (or style switch off)."); return true; }
                style.openChooser(t);
                java.util.Map.Entry<java.util.List<String>, String> c = style.lastChooser(t);
                if (c == null) { sender.sendMessage("chooser " + t.getName() + ": java chat chooser (no form)"); return true; }
                sender.sendMessage("chooser " + t.getName() + ": buttons=[" + String.join(" ;; ", c.getKey()) + "]");
                for (String line : c.getValue().split("\n")) if (!line.isBlank()) sender.sendMessage("  | " + line);
                return true;
            }
            if (args.length > 2 && args[0].equalsIgnoreCase("choosertap")) {
                // 0.2.3 admin/rig: run the last chooser's button N handler (0-based) as if tapped.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || style == null) { sender.sendMessage("No such player (or style switch off)."); return true; }
                int n;
                try { n = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage("choosertap: number"); return true; }
                sender.sendMessage("choosertap " + t.getName() + " " + n + ": " + (style.chooserTap(t, n) ? "ran" : "no chooser"));
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("tapbar")) {
                // 0.2.4 admin/rig: /wandbars tapbar <player> [open|lore] : state, last cast, or run the hold opener.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || tapBar == null) { sender.sendMessage("No such player (or tap bar off)."); return true; }
                String r = args.length > 2 && args[2].equalsIgnoreCase("open") ? " open -> " + tapBar.tryOpen(t, "admin")
                        : args.length > 2 && args[2].equalsIgnoreCase("lore") ? " lore -> " + tapBar.applyLore(t) : "";
                com.elmakers.mine.bukkit.api.magic.Mage m = ((com.elmakers.mine.bukkit.api.magic.MagicAPI) Bukkit.getPluginManager().getPlugin("Magic")).getMage(t);
                com.elmakers.mine.bukkit.api.wand.Wand w = m == null ? null : m.getActiveWand();
                sender.sendMessage("tapbar " + t.getName() + ":" + r + " wand " + (w == null ? "none" : "open=" + w.isInventoryOpen() + " active=" + w.getActiveSpellKey())
                        + " held=" + t.getInventory().getHeldItemSlot() + " lastcast " + tapBar.lastCast(t) + " | " + tapBar.counters() + " | hold " + tapBar.lastHold(t));
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("wandslot")) {
                // 0.2.7 admin/rig: /wandbars wandslot <player> : run the wand slot check once now.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || wandSlot == null) { sender.sendMessage("No such player (or wand slot off)."); return true; }
                sender.sendMessage("wandslot " + t.getName() + ": " + wandSlot.check(t, "admin") + " held=" + t.getInventory().getHeldItemSlot() + " | " + wandSlot.counters());
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("wandtap")) {
                // 0.2.7 admin/rig: /wandbars wandtap <player> : act as if they tapped the selected slot (guard applies).
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || tapBar == null) { sender.sendMessage("No such player (or tap bar off)."); return true; }
                sender.sendMessage("wandtap " + t.getName() + ": " + tapBar.onWandTap(t, t.getInventory().getHeldItemSlot()) + " (geyser " + geyserTap + ")");
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("simbedrock")) {
                // Admin test aid: /wandbars simbedrock <player> [on|off]. Memory only; cleared when they leave.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage("No such player."); return true; }
                boolean on = args.length > 2 ? args[2].equalsIgnoreCase("on") : !simBedrock.contains(t.getUniqueId());
                if (on) simBedrock.add(t.getUniqueId()); else simBedrock.remove(t.getUniqueId());
                String r = style == null ? "-" : String.valueOf(style.apply(t));
                sender.sendMessage("simbedrock " + t.getName() + ": " + (on ? "on" : "off") + " (bedrock=" + isBedrock(t) + ", real=" + BedrockForms.isBedrock(t) + ") apply " + r);
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("lastform")) {
                // Admin/rig: the last Spell Book form built for a player (title, buttons, text).
                Player t = Bukkit.getPlayerExact(args[1]);
                MagicHook.LastForm lf = t == null || hook == null ? null : hook.lastForm(t);
                if (lf == null) { sender.sendMessage("lastform: none"); return true; }
                sender.sendMessage("lastform " + t.getName() + ": title=" + lf.title + " buttons=[" + String.join(" ;; ", lf.buttons) + "]");
                for (String line : lf.content.split("\n")) if (!line.isBlank()) sender.sendMessage("  | " + line);
                return true;
            }
            if (args.length > 2 && args[0].equalsIgnoreCase("formtap")) {
                // Admin/rig: run the last form's button handler as if the player tapped button N (0-based).
                Player t = Bukkit.getPlayerExact(args[1]);
                MagicHook.LastForm lf = t == null || hook == null ? null : hook.lastForm(t);
                if (lf == null) { sender.sendMessage("formtap: no form"); return true; }
                int n;
                try { n = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { sender.sendMessage("formtap: number"); return true; }
                sender.sendMessage("formtap " + t.getName() + ": " + (n >= 0 && n < lf.buttons.size() ? lf.buttons.get(n) : "closed (" + n + ")"));
                lf.cb.accept(n);
                return true;
            }
            if (args.length > 2 && args[0].equalsIgnoreCase("use")) {
                // Admin/rig: the same path as the "Use this spell" button.
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || hook == null) { sender.sendMessage("No such player (or Magic missing)."); return true; }
                sender.sendMessage("use " + t.getName() + " " + args[2] + ": " + hook.useSpell(t, args[2]));
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("progress")) {
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || screen == null) { sender.sendMessage("No such player (or spell screen off)."); return true; }
                sender.sendMessage("progress " + t.getName() + ": last line '" + screen.lastLine(t) + "' stance " + screen.inStance(t) + " lines " + screen.lines());
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("spelltip")) {
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || spellTip == null) { sender.sendMessage("No such player (or spell tip off)."); return true; }
                if (args.length > 2 && args[2].equalsIgnoreCase("reset")) spellTip.reset(t.getUniqueId());
                java.util.UUID id = t.getUniqueId();
                sender.sendMessage("spelltip " + t.getName() + ": shows " + spellTip.gate().shows(id) + " ack " + spellTip.gate().acked(id)
                        + " exceltips " + spellTip.tipsOwns() + " pending " + spellTip.pendingInBook(t));
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("tip")) {
                // Admin: /wandbars tip <player> [reset]
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null || tips == null) { sender.sendMessage("No such player (or wand tip off)."); return true; }
                if (args.length > 2 && args[2].equalsIgnoreCase("reset")) tips.reset(t.getUniqueId());
                java.util.UUID id = t.getUniqueId();
                sender.sendMessage("tip " + t.getName() + ": uses " + tips.gate().uses(id) + "/" + tips.gate().maxUses
                        + " ack " + tips.gate().acked(id) + " popup-this-session " + tips.gate().formSentThisSession(id)
                        + " bedrock " + isBedrock(t));
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
                    + " saved " + style.prefs().size() + " applied " + style.applied())
                    + ", wand tip " + (tips == null ? "OFF" : "shown " + tips.shown() + " popups " + tips.formsSent())
                    + ", spell screen " + (screen == null ? "OFF" : (screen.bedrockForm() ? "ON" : "OFF") + " forms " + screen.intercepted() + " chest-left " + screen.chestsLeft()
                    + " progress lines " + screen.lines() + " uses " + hook.uses())
                    + ", spell tip " + (spellTip == null ? "OFF" : "shown " + spellTip.shown() + " handed " + spellTip.handed())
                    + ", tap bar " + (tapBar == null ? "OFF" : tapBar.counters())
                    + ", " + (wandSlot == null ? "wand slot OFF" : wandSlot.counters()) + ", geyser wand tap " + geyserTap
                    + ", simbedrock " + simBedrock.size());
            return true;
        }
        return false;
    }
}
