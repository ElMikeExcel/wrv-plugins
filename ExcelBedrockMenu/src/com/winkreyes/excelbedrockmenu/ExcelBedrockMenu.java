package com.winkreyes.excelbedrockmenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /menu that actually works on Bedrock.
 * Geyser forms go through the reflection-based GeyserForms helper (no
 * compile-time Geyser dependency); falls back to the DeluxeMenus chest GUI
 * for Java players.
 *
 * v1.5.0: full rename of BedrockMenu 1.4.2-menu.1 to ExcelBedrockMenu
 * (package + main class + plugin name). No behavior changes. The button set
 * is the 1.4.2 one (includes the "Spawn Shop" button added by the 1.4.2 ASM
 * patch). On first enable, any data folder left by the old BedrockMenu
 * plugin is copied across so its config is preserved.
 *
 * v1.5.1: added the "Adventurers Guild" button, right after the shop
 * button (mirroring the Java menu). It runs /ag as the player via the same
 * performCommand path as the other command buttons. Nothing else changed.
 *
 * v1.5.3: added the "Road to the Guild" button, immediately before the
 * "Adventurers Guild" button (the quest sits right next to the door it
 * unlocks). It runs /seals (ExcelGuildGate) as the player via the same
 * performCommand path. The "Adventurers Guild" button label is dynamic:
 * while ExcelGuildGate reports the guild locked, the menu shows
 * "🔒 Adventurers Guild" (computed at menu-open time via reflection on
 * GuildGateAPI; the stored BUTTONS array keeps the plain label, and any
 * lookup failure falls back to it). Dispatch stays /ag in both states;
 * the gate itself opens the quest view when tapped while locked.
 * (1.5.2 was an unshipped intermediate build with only the Road button.)
 *
 * v1.5.4: added two buttons after "Server Info" and two commands to back
 * them. "Server Commands" runs /servercommands: a curated, player-facing
 * list of the server's commands (the CATALOG below), grouped. Bedrock
 * players get a Cumulus group picker, then a per-group form whose content
 * lists every command in the group and whose buttons run the group's
 * bare-safe commands; Java players get a compact chat panel. Commands
 * that need typed arguments are display-only everywhere (their catalogue
 * entry has a null run string). "Server Website" runs /serverwebsite:
 * Bedrock players get a form spelling out the website address (Bedrock
 * cannot open a browser from in game) plus a chat copy; Java players get
 * the address as a clickable chat link. Everything lives in this class on
 * purpose: the jar keeps the exact 1.5.3 entry set, only this class and
 * plugin.yml change.
 */
public class ExcelBedrockMenu extends JavaPlugin implements CommandExecutor {

    private static final String[] BUTTONS = {
        "Spawn", "Teleport to a player", "Spawn Shop", "Road to the Guild", "Adventurers Guild",
        "Jobs & Money", "mcMMO Stats", "Leaderboard", "Server Info", "Server Commands",
        "Server Website", "Auto-open on join"
    };
    private static final String[] COMMANDS = {
        "spawn", "tpmenu", "shop", "seals", "ag", "jobs browse",
        "mcstats", "leaderboard", "motd", "servercommands", "serverwebsite", "menujoin"
    };
    // Village spawn coordinates (fallback if Essentials spawn lookup fails)
    private static final double SPAWN_X = -784;
    private static final double SPAWN_Y = 70;
    private static final double SPAWN_Z = -272;

    static final String WEBSITE_URL = "https://elmikeexcel.github.io/winkreyesvalley/";
    static final String WEBSITE_DISPLAY = "elmikeexcel.github.io/winkreyesvalley";

    // ------------------------------------------------------------------
    // Server command catalogue (v1.5.4)
    //
    // Curated from the plugin.yml of every installed plugin plus this
    // server's own menu commands. Player commands only: anything admin,
    // moderation, economy-admin or setup is deliberately absent (the test
    // suite asserts a denylist). Entry shape: {display, run, description}.
    // "run" is the exact string handed to performCommand when the entry is
    // activated; null means display-only (the command needs typed words
    // after it, or running it bare would be a mistake). The full curation
    // record, including everything left out and why, ships with the
    // install packet as COMMANDS-CATALOG.md.
    // ------------------------------------------------------------------
    private static final String[] CATALOG_GROUPS = {
        "Getting Around", "Money & Jobs", "Skills & Levels", "Shops & Trading",
        "Claims & Land", "Guild & Quests", "Server Info"
    };
    private static final String[][][] CATALOG = {
        { // Getting Around
            {"/menu", "menu", "Open the server menu"},
            {"/spawn", "spawn", "Teleport back to the village spawn"},
            {"/tpmenu", "tpmenu", "Pick a player to send a teleport request to"},
            {"/tpa <player>", null, "Ask to teleport to a player"},
            {"/tpaccept", "tpaccept", "Accept a request (/tpdeny says no)"},
            {"/sethome", "sethome", "Set your home where you are standing"},
            {"/home", "home", "Teleport to your home"},
            {"/warps", "warps", "List the server warps (/warp <name> to go)"},
        },
        { // Money & Jobs
            {"/balance", "balance", "See how much money you have"},
            {"/pay <player> <amount>", null, "Send money to another player"},
            {"/balancetop", "balancetop", "The richest players on the server"},
            {"/sell hand", "sell hand", "Sell the stack in your hand to the server"},
            {"/jobs", "jobs browse", "Browse jobs and join one"},
            {"/jobs stats", "jobs stats", "Your job levels, XP and pay rates"},
            {"/jobs join <job>", null, "Join a job by name (or browse with /jobs)"},
        },
        { // Skills & Levels
            {"/mcstats", "mcstats", "Your mcMMO skill levels"},
            {"/mctop", "mctop", "The top mcMMO players"},
            {"/<skill name>", null, "Inspect one skill: /mining, /swords, /axes, /fishing and more"},
            {"/party", "party", "Your mcMMO party: members, chat and invites"},
            {"/leaderboard", "leaderboard", "Top players on every board"},
        },
        { // Shops & Trading
            {"/shop", "shop", "Open the Spawn Shop"},
            {"/excelshop setup", "excelshop setup", "Start the guided setup for your own chest shop"},
            {"/excelshop manage", "excelshop manage", "Manage your own chest shop"},
            {"/deathprotection status", "deathprotection status", "See if your items are protected from death"},
            {"/mobveil status", "mobveil status", "See how much Mob Veil time you have left"},
        },
        { // Claims & Land
            {"/claim", "claim", "Claim the land around you"},
            {"/claimslist", "claimslist", "Your claim blocks and where your claims are"},
            {"/trust <player>", null, "Give a player full access to your claims"},
            {"/extendclaim <blocks>", null, "Grow the claim you are standing in"},
            {"/buyclaimblocks <amount>", null, "Buy more claim blocks with server money"},
            {"/trapped", "trapped", "Escape if you are stuck inside a claim"},
            {"/abandonclaim", null, "Delete the claim you stand in (no undo)"},
        },
        { // Guild & Quests
            {"/ag", "ag", "Teleport to the Adventurers Guild"},
            {"/seals", "seals", "Road to the Guild: the five seals quest (also /road)"},
            {"/em", "em", "EliteMobs: your elite level, gear skills and menus"},
            {"/em party", null, "Dungeon parties: add create, invite or ready"},
        },
        { // Server Info
            {"/motd", "motd", "Server info and news"},
            {"/list", "list", "See who is online right now"},
            {"/msg <player> <message>", null, "Send a private message (/r replies)"},
            {"/playermap", "playermap", "How player maps work and who is on them"},
            {"/compass", "compass", "Get a new server menu compass"},
            {"/menujoin", "menujoin", "Turn the auto-open menu on or off"},
            {"/servercommands", "servercommands", "This command list"},
            {"/serverwebsite", "serverwebsite", "Get the server website address"},
        },
    };

    static int catalogGroupCount() {
        return CATALOG_GROUPS.length;
    }

    static String catalogGroupName(int g) {
        return CATALOG_GROUPS[g];
    }

    static int catalogEntryCount(int g) {
        return CATALOG[g].length;
    }

    static String catalogDisplay(int g, int i) {
        return CATALOG[g][i][0];
    }

    /** Exact performCommand string for an entry, or null if display-only. */
    static String catalogRun(int g, int i) {
        return CATALOG[g][i][1];
    }

    static String catalogDescription(int g, int i) {
        return CATALOG[g][i][2];
    }

    static int catalogTotalEntries() {
        int total = 0;
        for (String[][] group : CATALOG) {
            total += group.length;
        }
        return total;
    }

    /**
     * Resolves a /servercommands argument to a group index: a 1-based
     * group number, or a case-insensitive prefix of the group name
     * ("claims" for Claims & Land). -1 when nothing matches.
     */
    static int catalogGroupIndexFor(String arg) {
        if (arg == null) {
            return -1;
        }
        String wanted = arg.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) {
            return -1;
        }
        try {
            int n = Integer.parseInt(wanted);
            return (n >= 1 && n <= CATALOG_GROUPS.length) ? n - 1 : -1;
        } catch (NumberFormatException ignored) {
            // not a number; fall through to name matching
        }
        for (int g = 0; g < CATALOG_GROUPS.length; g++) {
            if (CATALOG_GROUPS[g].toLowerCase(Locale.ROOT).startsWith(wanted)) {
                return g;
            }
        }
        return -1;
    }

    /** Plain-text lines for a group's Bedrock form body. */
    static String[] catalogGroupFormLines(int g) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < CATALOG[g].length; i++) {
            lines.add(catalogDisplay(g, i) + " - " + catalogDescription(g, i));
        }
        lines.add("");
        lines.add("Buttons below run the command. Lines with <words> in them "
                + "need typing in chat instead.");
        return lines.toArray(new String[0]);
    }

    /** Button labels for a group's form: the runnable entries, in order. */
    static String[] catalogGroupFormButtons(int g) {
        List<String> buttons = new ArrayList<>();
        for (int i = 0; i < CATALOG[g].length; i++) {
            if (catalogRun(g, i) != null) {
                buttons.add(catalogDisplay(g, i));
            }
        }
        return buttons.toArray(new String[0]);
    }

    /** Run string behind a group's form button index, or null if invalid. */
    static String catalogRunForButton(int g, int button) {
        int seen = -1;
        for (int i = 0; i < CATALOG[g].length; i++) {
            if (catalogRun(g, i) != null && ++seen == button) {
                return catalogRun(g, i);
            }
        }
        return null;
    }

    /**
     * Compact chat panel lines (same design language as the
     * ExcelLeaderboard 1.2 chat panels: gold headers, aqua markers,
     * yellow commands, gray descriptions). group -1 prints every group.
     */
    static String[] commandsPanelLines(int group) {
        List<String> lines = new ArrayList<>();
        if (group >= 0) {
            lines.add("\u00a76\u2014 Server Commands: " + CATALOG_GROUPS[group] + " \u2014");
            addPanelGroup(lines, group);
            lines.add("\u00a77/servercommands shows every group.");
            return lines.toArray(new String[0]);
        }
        lines.add("\u00a76\u2014 Server Commands \u2014");
        for (int g = 0; g < CATALOG_GROUPS.length; g++) {
            addPanelGroup(lines, g);
        }
        lines.add("\u00a77That is the whole list. In the menu, tap Server Commands "
                + "for tap-to-run buttons.");
        return lines.toArray(new String[0]);
    }

    private static void addPanelGroup(List<String> lines, int g) {
        lines.add("\u00a76" + CATALOG_GROUPS[g]);
        for (int i = 0; i < CATALOG[g].length; i++) {
            lines.add("\u00a7b\u00bb \u00a7e" + catalogDisplay(g, i)
                    + " \u00a77- " + catalogDescription(g, i));
        }
    }

    @Override
    public void onEnable() {
        migrateLegacyDataFolder("BedrockMenu");
        getCommand("menu").setExecutor(this);
        getCommand("servercommands").setExecutor(this);
        getCommand("serverwebsite").setExecutor(this);
        TeleportMenu tp = new TeleportMenu(this);
        getCommand("tpmenu").setExecutor(tp);
        getServer().getPluginManager().registerEvents(tp, this);
        getLogger().info("ExcelBedrockMenu enabled: /menu uses native Bedrock dialogs.");
    }

    /**
     * One-time rename migration: if our data folder has no files yet and the
     * legacy plugin's data folder (plugins/<legacyName>) exists, copy its
     * contents across so no config or data is lost in the rename.
     */
    private void migrateLegacyDataFolder(String legacyName) {
        try {
            File newDir = getDataFolder();
            File legacyDir = new File(newDir.getParentFile(), legacyName);
            if (!legacyDir.isDirectory() || legacyDir.equals(newDir)) {
                return;
            }
            if (newDir.isDirectory()) {
                try (var stream = Files.walk(newDir.toPath())) {
                    if (stream.anyMatch(p -> !p.equals(newDir.toPath()))) {
                        return; // new folder already has content; never overwrite
                    }
                }
            }
            final Path from = legacyDir.toPath();
            final Path to = newDir.toPath();
            try (var stream = Files.walk(from)) {
                stream.forEach(src -> {
                    try {
                        Path dst = to.resolve(from.relativize(src));
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dst);
                        } else {
                            Files.createDirectories(dst.getParent());
                            Files.copy(src, dst);
                        }
                    } catch (IOException e) {
                        getLogger().warning("Data migration copy failed for " + src + ": " + e.getMessage());
                    }
                });
            }
            getLogger().info("Migrated legacy data folder plugins/" + legacyName + " -> plugins/" + newDir.getName());
        } catch (Exception e) {
            getLogger().warning("Legacy data migration (" + legacyName + ") failed: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        if (name.equals("servercommands")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use /servercommands.");
                return true;
            }
            Player player = (Player) sender;
            if (args.length > 0) {
                int g = catalogGroupIndexFor(args[0]);
                if (g >= 0) {
                    sendCommandsPanel(player, g);
                } else {
                    player.sendMessage("\u00a7cUnknown group '" + args[0]
                            + "'.\u00a77 Groups: " + String.join(", ", CATALOG_GROUPS));
                }
                return true;
            }
            openCommands(player);
            return true;
        }
        if (name.equals("serverwebsite")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use /serverwebsite.");
                return true;
            }
            showWebsite((Player) sender);
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /menu.");
            return true;
        }
        openMenu((Player) sender);
        return true;
    }

    private void openMenu(Player player) {
        String[] labels = BUTTONS.clone();
        if (isGuildLocked()) {
            for (int i = 0; i < labels.length; i++) {
                if (COMMANDS[i].equals("ag")) {
                    labels[i] = "🔒 " + labels[i];
                    break;
                }
            }
        }
        boolean sent = GeyserForms.sendSimpleForm(player, "Server Menu",
                "What would you like to do?", labels,
                id -> {
                    if (id < 0 || id >= COMMANDS.length || !player.isOnline()) {
                        return;
                    }
                    String command = COMMANDS[id];
                    Bukkit.getScheduler().runTask(this, () -> runAction(player, command));
                },
                getLogger());
        if (sent) {
            return;
        }
        // Fallback for Java players (or if the form failed)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "dm open server_menu " + player.getName());
    }

    /**
     * /servercommands with no argument: Bedrock players get the group
     * picker form (the /seals pattern: forms first, chat panel as the
     * fallback); everyone else gets the full chat panel.
     */
    private void openCommands(Player player) {
        if (GeyserForms.isBedrockPlayer(player, getLogger())) {
            boolean sent = GeyserForms.sendSimpleForm(player, "Server Commands",
                    "Every command you can use, by group. Pick a group:",
                    CATALOG_GROUPS.clone(),
                    id -> {
                        if (id < 0 || id >= CATALOG_GROUPS.length || !player.isOnline()) {
                            return;
                        }
                        Bukkit.getScheduler().runTask(this, () -> openCommandsGroup(player, id));
                    },
                    getLogger());
            if (sent) {
                return;
            }
        }
        sendCommandsPanel(player, -1);
    }

    private void openCommandsGroup(Player player, int g) {
        String[] buttons = catalogGroupFormButtons(g);
        boolean sent = GeyserForms.sendSimpleForm(player, "Server Commands",
                String.join("\n", catalogGroupFormLines(g)), buttons,
                id -> {
                    if (id < 0 || id >= buttons.length || !player.isOnline()) {
                        return;
                    }
                    String run = catalogRunForButton(g, id);
                    if (run != null) {
                        Bukkit.getScheduler().runTask(this, () -> player.performCommand(run));
                    }
                },
                getLogger());
        if (!sent) {
            sendCommandsPanel(player, g);
        }
    }

    private void sendCommandsPanel(Player player, int group) {
        for (String line : commandsPanelLines(group)) {
            player.sendMessage(line);
        }
    }

    /**
     * /serverwebsite: Bedrock players get a form spelling the address out
     * (Bedrock cannot open a browser from in game) and a plain chat copy;
     * Java players get the address as a clickable chat link.
     */
    private void showWebsite(Player player) {
        if (GeyserForms.isBedrockPlayer(player, getLogger())) {
            boolean sent = GeyserForms.sendSimpleForm(player, "Server Website",
                    "The server website has the guide, the full command list and "
                            + "server news.\n\nType this into your phone or computer "
                            + "browser:\n\n" + WEBSITE_DISPLAY + "\n\nBedrock cannot "
                            + "open a browser from in game, so the address is in your "
                            + "chat too.",
                    new String[]{"Close"}, id -> { /* Close: nothing to do */ },
                    getLogger());
            player.sendMessage("\u00a7aServer Website: \u00a7b" + WEBSITE_URL);
            if (sent) {
                return;
            }
            // Form failed: the chat copy above is the whole answer.
            return;
        }
        player.sendMessage(Component.text("\u00bb Server Website: ", NamedTextColor.AQUA)
                .append(Component.text(WEBSITE_DISPLAY, NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(WEBSITE_URL))
                        .hoverEvent(HoverEvent.showText(Component.text("Open the server website")))));
        player.sendMessage("\u00a77The guide, the command list and server news live there.");
    }

    /**
     * True while ExcelGuildGate is installed and reports the guild locked.
     * Reflection against GuildGateAPI in the GuildGate plugin's own
     * classloader (no hard dependency, same pattern as GeyserForms). Any
     * failure at all (plugin absent, API missing, anything) reads as
     * "not locked", so the button simply keeps its plain label.
     */
    private boolean isGuildLocked() {
        try {
            Plugin guildGate = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (guildGate == null || !guildGate.isEnabled()) {
                return false;
            }
            ClassLoader loader = guildGate.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                    true, loader);
            Object unlocked = apiClass.getMethod("isUnlocked").invoke(null);
            return !Boolean.TRUE.equals(unlocked);
        } catch (Exception e) {
            return false;
        }
    }

    private void runAction(Player player, String command) {
        switch (command) {
            case "spawn":
                teleportToSpawn(player);
                break;
            case "tpmenu":
                TeleportMenu.open(this, player);
                break;
            default:
                player.performCommand(command);
                break;
        }
    }

    /**
     * Teleports the player to spawn without relying on the /spawn command.
     * Tries EssentialsXSpawn's spawn location first, falls back to the
     * hardcoded village coordinates.
     */
    private void teleportToSpawn(Player player) {
        try {
            // Try EssentialsXSpawn via reflection
            Plugin essentialsSpawn = Bukkit.getPluginManager().getPlugin("EssentialsXSpawn");
            if (essentialsSpawn == null) {
                essentialsSpawn = Bukkit.getPluginManager().getPlugin("EssentialsSpawn");
            }
            if (essentialsSpawn != null) {
                // EssentialsXSpawn stores spawn; get it via the Essentials API
                Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essentials != null) {
                    ClassLoader essLoader = essentials.getClass().getClassLoader();
                    Class<?> essClass = Class.forName("com.earth2me.essentials.Essentials",
                            true, essLoader);
                    Object essApi = essClass.getMethod("getInstance").invoke(null);
                    Object spawn = essClass.getMethod("getSpawn").invoke(essApi);
                    if (spawn != null) {
                        // Location is from Bukkit API, use it directly
                        org.bukkit.Location loc = (org.bukkit.Location) spawn;
                        player.teleport(loc);
                        getLogger().info("Teleported " + player.getName() + " to Essentials spawn.");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Essentials spawn lookup failed: " + e.getMessage()
                    + ", using village coordinates.");
        }
        // Fallback: village coordinates
        org.bukkit.World world = player.getWorld();
        org.bukkit.Location village = new org.bukkit.Location(world, SPAWN_X, SPAWN_Y, SPAWN_Z);
        // Find safe Y (highest block at x,z)
        int safeY = world.getHighestBlockYAt((int) SPAWN_X, (int) SPAWN_Z) + 1;
        village.setY(safeY);
        player.teleport(village);
        getLogger().info("Teleported " + player.getName() + " to village fallback.");
    }
}
