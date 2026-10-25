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
 * v1.5.8: teleport buttons ask first (Mike's standing standard, set
 * when players kept teleporting by accident on stray Spawn taps).
 * Every menu action that immediately teleports the player now opens a
 * Cumulus ModalForm ("Teleport to <destination>?" / "Yes, teleport" /
 * "Cancel") and only an explicit Yes runs the exact action the button
 * used through 1.5.7; Cancel and closing the form do nothing. Covered:
 * the Spawn button, the Adventurers Guild button (only while the guild
 * is unlocked; while sealed /ag opens the seals quest page instead,
 * which is not a teleport), and the /servercommands catalogue entries
 * that teleport the tapper (/spawn, /home, /ag). Buttons that do not
 * teleport are untouched.
 *
 * v1.5.7: new "Party" button (opens ExcelTogether's /excelparty menu).
 * v1.5.6: Spawn button always targets the overworld (see teleportToSpawn).
 * Village fallback X/Y/Z are read from config.yml (spawn.x/y/z).
 *
 * v1.5.5: removed the "Road to the Guild" button (it duplicated the locked
 * "Adventurers Guild" tap, which already runs /seals) and replaced the
 * emoji lock prefix (1.5.6: no lock text at all, label identical to unlocked): Bedrock has no glyph for
 * U+1F512, so it drew a box and switched the whole label to a smaller
 * fallback font. All labels are now plain ASCII, one font and size.
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
 *
 * v1.5.10: the Server Website answer leads with the SHORT link
 * (tinyurl.com/winkreyesvalley) and keeps the full address under it --
 * on the Bedrock form AND in its chat copy, and on the Java clickable
 * answer too. Mike's ask (Oct 18, 2026, ~9:58 PM CDT): the short one is
 * far easier to remember and to type on a phone. The form body and the
 * chat lines are static helpers (websiteFormContent / websiteChatLines)
 * so the test suite can pin both addresses and their order.
 *
 * v1.5.11: new "Utility Pouch" button, between "Server Website" and
 * "Auto-open on join". It runs /pouch (ExcelPouch) as the player via the
 * same performCommand path as the other command buttons -- platform
 * parity with the Java DeluxeMenus server_menu, which gained its own
 * Pouch button with the ExcelPouch 0.1.0 install (Mike's parity rule:
 * Java and Bedrock are treated the same). /pouch is not a teleport, so
 * no confirmation step. Nothing else changed.
 *
 * v1.5.12: new "Skills & Perks" button, immediately after "mcMMO
 * Stats" (skills-perks/MENU-SPEC.md section 2, specced Oct 17, 2026
 * and never built until now; the parity audit WRV-084 found the gap).
 * It opens a Cumulus content form carrying the spec's exact copy --
 * the mcMMO ladders in brief, the check commands, and the Magic line
 * with the LIVE seal count read from ExcelGuildGate (the same
 * reflective read the Adventurers Guild button's lock state uses) --
 * plus a "Skill Trees on the Website" button that opens a follow-on
 * form spelling out the website address, the Server Website pattern.
 * Java has had its Skills & Perks menu since Oct 17; this is the
 * Bedrock half. The button runs no command and teleports nowhere, so
 * no confirmation step. Nothing else changed.
 *
 * v1.5.13: /pouch joins the /servercommands catalogue (Server Info
 * group, immediately after /compass). The Utility Pouch main-menu
 * button has existed since v1.5.11, but the command itself was
 * missing from the catalogue on both platforms -- a player browsing
 * Server Commands found no pouch anywhere (found live by Mike,
 * Oct 21, 2026). The entry is runnable: it runs /pouch as the player
 * through the same catalogue path as /compass, and, not being a
 * teleport, has no confirmation step. The bundled commands_menu.yml
 * mirror gains the matching si_pouch entry. Nothing else changed.
 *
 * v1.5.14: the Utility Pouch main-menu button moves up: index 12
 * (13th of 14, below Server Website) -> index 3, in the top
 * daily-driver cluster behind Spawn / Teleport to a player / Party
 * (Mike, Oct 21, 2026: the pouch is something people use quite
 * often to store their stuff away, so it should not sit near the
 * bottom of the menu). Every other button keeps its 1.5.13
 * relative order; the catalogue, both yml mirrors and all other
 * behavior are unchanged.
 *
 * v1.5.18: new "Spellbook" button at index 4, right after "Utility
 * Pouch" (Mike, Oct 24, 2026: a Spellbook button on the compass). It runs
 * /spellinfo as the player: ExcelWandBars' read-only Magic (elmakers)
 * spell book for the player's own wand (Bedrock form, Java chat). If
 * ExcelWandBars is not loaded the tap says so instead of an unknown
 * command. Not a teleport, so no confirm step. Every other button keeps
 * its 1.5.17 relative order.
 *
 * v1.5.18 also: "Auto-sort my chests: on/off" just before "Auto-open on
 * join" (the other per-player switch). It runs /autosort as the player
 * (AutoSort by nothub, Modrinth): a toggle, so the same tap turns it on or
 * off and AutoSort's own chat line says which. Missing AutoSort gives a
 * message instead of an unknown command.
 *
 * v1.5.18 also: "Wand controls: Classic or Sneak bars" just before the
 * Auto-sort switch. It runs /wandstyle as the player (ExcelWandBars 0.2.1):
 * a Bedrock form with two buttons, Classic (right click picks the next
 * spell, the default) or Sneak bars (sneak flips the Magic (elmakers) spell
 * bar). No typing. This menu is Bedrock only, so Java players never see it
 * (Sneak bars is Bedrock only). Missing ExcelWandBars gives a message.
 *
 * v1.5.19 (What's New build, Oct 24, 2026, Mike approved building it on
 * the test server): new "What's New" button right after "Server Website"
 * (the help/news cluster). It runs /whatsnew as the player (ExcelWhatsNew
 * 0.1.0): the latest server news and new commands in a Bedrock form with
 * an OK button. Not a teleport, so no confirm step. Missing ExcelWhatsNew
 * gives a message instead of an unknown command. The Server Info group of
 * the Server Commands list gains a runnable /whatsnew row. Every other
 * button keeps its 1.5.18 relative order. Built on the batch 1.5.18 source
 * (ee3d00f0), NOT on the EliteMobs-fallback follow-up 1.5.19 (cb8bf9ec).
 *
 * v1.5.20 (Oct 25, 2026, Mike: match the Java compass): two new buttons
 * right after "Leaderboard", the end of the Bedrock "Me" cluster (Jobs &
 * Money, mcMMO Stats, Skills & Perks, Leaderboard), in the Java "Me" row
 * order: "Lost & Found" (runs /lostfound as the player, ExcelDeathRecord
 * 0.3.0, which shows the price before paying) then "Trash Bin" (runs /trash
 * as the player, ExcelTrash 0.2.0). Same labels and the same commands as
 * the Java server_menu.yml. Neither is a teleport, so no confirm step. A
 * missing plugin gives a message instead of an unknown command, and a
 * player without the command's own permission (exceldeathrecord.lostfound,
 * exceltrash.use, both default true) gets a plain message. This form has
 * no button images, so there are no icons. Built on the live
 * 1.5.19-whatsnew source (178b03b7). Every other button keeps its
 * 1.5.19-whatsnew relative order and behavior.
 */
public class ExcelBedrockMenu extends JavaPlugin implements CommandExecutor {

    private static final String[] BUTTONS = {
        "Spawn", "Teleport to a player", "Party", "Utility Pouch", "Spellbook", "Excel Shop", "Adventurers Guild",
        "Jobs & Money", "mcMMO Stats", "Skills & Perks", "Leaderboard", "Lost & Found", "Trash Bin", "Server Info",
        "Server Commands", "Server Website", "What's New", "Wand controls: Classic or Sneak bars",
        "Auto-sort my chests: on/off", "Auto-open on join"
    };
    private static final String[] COMMANDS = {
        "spawn", "tpmenu", "excelparty", "pouch", "spellinfo", "shop", "ag", "jobs browse",
        "mcstats", "skillsperks", "leaderboard", "lostfound", "trash", "motd", "servercommands", "serverwebsite", "whatsnew", "wandstyle", "autosort", "menujoin"
    };
    // Village spawn coordinates (fallback if Essentials spawn lookup fails)
    private static final double SPAWN_X = -784;
    private static final double SPAWN_Y = 70;
    private static final double SPAWN_Z = -272;

    /** Label for the guild button while sealed: identical to unlocked (plain ASCII, no Locked text). */
    static String lockedLabel(String base) {
        return base;
    }

    /** True if a world may be used as the Spawn button target. */
    static boolean isSpawnWorld(boolean normalEnvironment, String name) {
        return normalEnvironment && !"em_adventurers_guild".equals(name);
    }

    // ------------------------------------------------------------------
    // Teleport confirm (v1.5.8, Mike's standing menu standard)
    //
    // Menu buttons teleported on a single tap and players kept hitting
    // them by accident. Every action that immediately teleports the
    // player now opens a Cumulus ModalForm first
    // (GeyserForms.sendModalForm, the same helper the teleport-request
    // prompt in TeleportMenu uses) and only an explicit "Yes, teleport"
    // runs the exact action the button used through 1.5.7. "Cancel",
    // and closing the form without choosing, both do nothing: the
    // modal helper only reports valid results, so a closed form never
    // calls back.
    //
    // Covered commands: "spawn" (main-menu Spawn button and the
    // catalogue /spawn entry), "home" (catalogue /home entry) and "ag"
    // (Adventurers Guild button and catalogue entry) -- but "ag" only
    // while the guild is unlocked: while sealed, /ag opens the seals
    // quest page, which is not a teleport and gets no confirm.
    // Deliberately NOT covered: "tpaccept" (it answers a teleport
    // request the other player started; that request flow is itself
    // the consent step) and "trapped" (GriefPrevention only honors it
    // when the player is genuinely trapped in a claim, so a stray tap
    // anywhere else is a no-op, and it is an emergency escape).
    // ------------------------------------------------------------------
    static final String CONFIRM_YES = "Yes, teleport";
    static final String CONFIRM_NO = "Cancel";

    /**
     * Confirm form title for a command, or null when the command does
     * not immediately teleport the player (and so needs no confirm).
     * guildLocked only matters for "ag" (see the block above).
     */
    static String teleportConfirmTitle(String command, boolean guildLocked) {
        switch (command) {
            case "spawn":
                return "Teleport to spawn?";
            case "ag":
                return guildLocked ? null : "Teleport to the Adventurers Guild?";
            case "home":
                return "Teleport home?";
            default:
                return null;
        }
    }

    /** Confirm form body line for a command ("" when it needs no confirm). */
    static String teleportConfirmContent(String command) {
        switch (command) {
            case "spawn":
                return "This takes you back to the village spawn right away.";
            case "ag":
                return "This takes you to the Adventurers Guild right away.";
            case "home":
                return "This takes you home right away.";
            default:
                return "";
        }
    }

    /** True for menu commands that must ask before they run (v1.5.8). */
    static boolean requiresConfirm(String command, boolean guildLocked) {
        return teleportConfirmTitle(command, guildLocked) != null;
    }

    /**
     * Confirm outcome: only an explicit first-button choice teleports.
     * A null choice (the form was closed without choosing) never does.
     */
    static boolean confirmChoiceTeleports(Boolean firstButtonClicked) {
        return Boolean.TRUE.equals(firstButtonClicked);
    }

    static final String WEBSITE_URL = "https://elmikeexcel.github.io/winkreyesvalley/";
    static final String WEBSITE_DISPLAY = "elmikeexcel.github.io/winkreyesvalley";
    static final String WEBSITE_SHORT_URL = "https://tinyurl.com/winkreyesvalley";
    static final String WEBSITE_SHORT_DISPLAY = "tinyurl.com/winkreyesvalley";

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
            {"/warps", "warps", "List the server warps (/warp <name> to go)"},
        },
        { // Money & Jobs
            {"/balance", "balance", "See how much money you have"},
            {"/pay <player> <amount>", null, "Send money to another player"},
            {"/balancetop", "balancetop", "The richest players on the server"},
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
            {"/shop", "shop", "Open the Excel Shop"},
            {"/excelshop setup", "excelshop setup", "Start the guided setup for your own chest shop"},
            {"/excelshop manage", "excelshop manage", "Manage your own chest shop"},
            {"/deathprotection status", "deathprotection status", "See if your items are protected from death"},
            {"/mobveil status", "mobveil status", "See how much Mob Veil time you have left"},
        },
        { // Claims & Land
            {"/claim", "claim", "Claim the land around you"},
            {"/claimslist", "claimslist", "Your claim blocks and where your claims are"},
            {"/trust <player>", null, "Give a player full access to your claims"},
            {"/childproofmenu", "childproofmenu", "Pick a player: doors and buttons only in this claim"},
            {"/unchildproof <player>", null, "Remove that player from the claim you stand in"},
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
            {"/pouch", "pouch", "Park your menu compass and Magic wand; contents survive death"},
            {"/menujoin", "menujoin", "Turn the auto-open menu on or off"},
            {"/servercommands", "servercommands", "This command list"},
            {"/serverwebsite", "serverwebsite", "Get the server website address"},
            {"/whatsnew", "whatsnew", "What's new on the server, with the new commands (also /news)"},
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
        saveDefaultConfig();
        getCommand("menu").setExecutor(this);
        getCommand("servercommands").setExecutor(this);
        getCommand("serverwebsite").setExecutor(this);
        TeleportMenu tp = new TeleportMenu(this);
        getCommand("tpmenu").setExecutor(tp);
        getCommand("childproofmenu").setExecutor(new ChildproofMenu(this));
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
                    labels[i] = lockedLabel(labels[i]);
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
                        Bukkit.getScheduler().runTask(this, () -> runCatalogEntry(player, run));
                    }
                },
                getLogger());
        if (!sent) {
            sendCommandsPanel(player, g);
        }
    }

    /**
     * v1.5.8: catalogue entries that immediately teleport the tapper
     * (/spawn, /home, /ag) go through the same confirm as the main-menu
     * buttons; every other entry runs exactly as it did through 1.5.7.
     * A confirmed catalogue /spawn still runs the player's own /spawn
     * command, as the catalogue always has (the main-menu Spawn button
     * is the one with the console-dispatch + village fallback path).
     */
    private void runCatalogEntry(Player player, String run) {
        if (requiresConfirm(run, isGuildLocked())) {
            confirmTeleport(player, run, () -> player.performCommand(run));
        } else {
            player.performCommand(run);
        }
    }

    private void sendCommandsPanel(Player player, int group) {
        for (String line : commandsPanelLines(group)) {
            player.sendMessage(line);
        }
    }

    /**
     * Bedrock website form body (v1.5.10): the short link first (easy to
     * remember and to type on a phone), the full address under it.
     */
    static String websiteFormContent() {
        return "The server website has the guide, the full command list and "
                + "server news.\n\nType one of these into your phone or computer "
                + "browser:\n\nShort and easy to type:\n" + WEBSITE_SHORT_DISPLAY
                + "\n\nFull address:\n" + WEBSITE_DISPLAY + "\n\nBedrock cannot "
                + "open a browser from in game, so both addresses are in your "
                + "chat too.";
    }

    /** The chat copy that mirrors the form: short link, then full address. */
    static String[] websiteChatLines() {
        return new String[]{
            "\u00a7aServer Website: \u00a7b" + WEBSITE_SHORT_URL,
            "\u00a77Full address: \u00a7b" + WEBSITE_URL
        };
    }

    // ------------------------------------------------------------------
    // Skills & Perks (v1.5.12; skills-perks/MENU-SPEC.md section 2)
    //
    // The Bedrock half of the Skills & Perks surface Java got Oct 17,
    // 2026. The body copy is the spec's, verbatim; only the seal count
    // is live data (read from ExcelGuildGate at menu-open time, the
    // same reflective read the Adventurers Guild button's lock state
    // uses). The numbers in the copy (19 skills, 83 perks, levels
    // 1-1000) are copy, not a live mcMMO integration -- the spec says
    // so, and skills-perks/INVENTORY.md is their source of truth.
    // ------------------------------------------------------------------
    static final String[] SKILLS_PERKS_BUTTONS = {"Skill Trees on the Website", "Close"};

    /**
     * Skills & Perks form body: the MENU-SPEC section 2 copy, verbatim,
     * with the live broken-seal count in the Magic line (never a
     * hard-coded number).
     */
    static String skillsPerksContent(int sealsBroken) {
        return "mcMMO skills are live now: 19 skills and 83 perks, and levels run from "
                + "1 to 1,000. The game announces each new perk in chat the moment you "
                + "unlock it.\n\nCheck yours any time:\n/mcstats shows all your levels.\n"
                + "Typing a skill's name (like /smelting) shows that skill's own page.\n"
                + "/mctop shows the leaderboards.\n\nMagic is sealed for now. It arrives "
                + "as the Adventurers Guild seals fall (" + sealsBroken + " of 5 today).";
    }

    /**
     * Follow-on form body behind the "Skill Trees on the Website"
     * button: the address as plain text (Bedrock cannot open links),
     * per the spec.
     */
    static String skillsPerksWebsiteContent() {
        return WEBSITE_DISPLAY + "\n\nOpen it and tap Skill Trees \u2014 every ladder is there.";
    }

    /**
     * /serverwebsite: Bedrock players get a form spelling the addresses out
     * (Bedrock cannot open a browser from in game) and both addresses as a
     * plain chat copy; Java players get both addresses as clickable chat
     * links.
     */
    private void showWebsite(Player player) {
        if (GeyserForms.isBedrockPlayer(player, getLogger())) {
            boolean sent = GeyserForms.sendSimpleForm(player, "Server Website",
                    websiteFormContent(),
                    new String[]{"Close"}, id -> { /* Close: nothing to do */ },
                    getLogger());
            for (String line : websiteChatLines()) {
                player.sendMessage(line);
            }
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
        player.sendMessage(Component.text("\u00bb Short link: ", NamedTextColor.AQUA)
                .append(Component.text(WEBSITE_SHORT_DISPLAY, NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(WEBSITE_SHORT_URL))
                        .hoverEvent(HoverEvent.showText(Component.text("Open the server website")))));
        player.sendMessage("\u00a77The guide, the command list and server news live there.");
    }

    /**
     * v1.5.12: the Skills & Perks form. Bedrock players get the content
     * form; its first button opens the Skill Trees website follow-on.
     * If the form cannot be sent at all, the same copy goes to chat so
     * the button never dead-ends (the /servercommands chat-panel
     * fallback pattern).
     */
    private void openSkillsPerks(Player player) {
        boolean sent = GeyserForms.sendSimpleForm(player, "Skills & Perks",
                skillsPerksContent(sealsBrokenCount()),
                SKILLS_PERKS_BUTTONS.clone(),
                id -> {
                    if (id != 0 || !player.isOnline()) {
                        return; // "Close", or the form was closed: nothing to do
                    }
                    Bukkit.getScheduler().runTask(this, () -> openSkillsPerksWebsite(player));
                },
                getLogger());
        if (!sent) {
            for (String line : skillsPerksContent(sealsBrokenCount()).split("\n")) {
                player.sendMessage(line);
            }
        }
    }

    /** The follow-on form: the website address as plain text. */
    private void openSkillsPerksWebsite(Player player) {
        boolean sent = GeyserForms.sendSimpleForm(player, "Skill Trees on the Website",
                skillsPerksWebsiteContent(),
                new String[]{"Close"}, id -> { /* Close: nothing to do */ },
                getLogger());
        if (!sent) {
            player.sendMessage(skillsPerksWebsiteContent());
        }
    }

    /**
     * Live count of broken guild seals, for the Skills & Perks Magic
     * line ("N of 5 today"). Reflection against GuildGateAPI in the
     * GuildGate plugin's own classloader -- the same read the
     * Adventurers Guild button's lock state uses (isGuildLocked),
     * against the API's count method. Any failure at all (plugin
     * absent, API missing, anything) falls back to the guild's own
     * lock state: 5 once the guild stands open, 0 while it is sealed
     * or unknown, so the line never claims seals that have not fallen.
     */
    private int sealsBrokenCount() {
        try {
            Plugin guildGate = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (guildGate == null || !guildGate.isEnabled()) {
                return 0;
            }
            ClassLoader loader = guildGate.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                    true, loader);
            Object count = apiClass.getMethod("sealsBrokenCount").invoke(null);
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
            return 0;
        } catch (Exception e) {
            return isGuildLocked() ? 0 : 5;
        }
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
        if (requiresConfirm(command, isGuildLocked())) {
            confirmTeleport(player, command, () -> runConfirmedAction(player, command));
            return;
        }
        switch (command) {
            case "skillsperks":
                // v1.5.12: opens the Skills & Perks content form; runs
                // no command and teleports nowhere (no confirm step).
                openSkillsPerks(player);
                break;
            case "tpmenu":
                TeleportMenu.open(this, player);
                break;
            case "autosort":
                // v1.5.18: AutoSort (nothub) owns /autosort; it is a toggle.
                if (Bukkit.getPluginCommand("autosort") == null) {
                    player.sendMessage("\u00a7eAuto-sort isn't available right now. Ask an adult to check the server.");
                } else {
                    player.performCommand("autosort");
                }
                break;
            case "wandstyle":
                // v1.5.18: ExcelWandBars 0.2.1 owns /wandstyle (Classic or Sneak bars form).
                if (Bukkit.getPluginCommand("wandstyle") == null) {
                    player.sendMessage("\u00a7eWand controls aren't available right now. Ask an adult to check the server.");
                } else {
                    player.performCommand("wandstyle");
                }
                break;
            case "spellinfo":
                // v1.5.18: ExcelWandBars owns /spellinfo (aliases spellbook, wandspells).
                if (Bukkit.getPluginCommand("spellinfo") == null) {
                    player.sendMessage("\u00a7eThe Spellbook isn't available right now. Ask an adult to check the server.");
                } else {
                    player.performCommand("spellinfo");
                }
                break;
            case "whatsnew":
                // v1.5.19: ExcelWhatsNew 0.1.0 owns /whatsnew (alias /news).
                if (Bukkit.getPluginCommand("whatsnew") == null) {
                    player.sendMessage("\u00a7eThe news isn't available right now. Ask an adult to check the server.");
                } else {
                    player.performCommand("whatsnew");
                }
                break;
            case "lostfound":
                // v1.5.20: ExcelDeathRecord 0.3.0 owns /lostfound (alias /recover).
                runGuarded(player, "lostfound",
                        "\u00a7eLost & Found isn't available right now. Ask an adult to check the server.",
                        "\u00a7eYou can't use Lost & Found right now. Ask an adult to check the server.");
                break;
            case "trash":
                // v1.5.20: ExcelTrash 0.2.0 owns /trash (alias /bin).
                runGuarded(player, "trash",
                        "\u00a7eThe Trash Bin isn't available right now. Ask an adult to check the server.",
                        "\u00a7eYou can't use the Trash Bin right now. Ask an adult to check the server.");
                break;
            case "excelparty":
                // v1.5.7: ExcelTogether 0.2.0 owns this command. Not /party: mcMMO has that one.
                if (Bukkit.getPluginCommand("excelparty") == null) {
                    player.sendMessage("\u00a7eParties aren't available right now. Ask an adult to check the server.");
                } else {
                    player.performCommand("excelparty");
                }
                break;
            default:
                player.performCommand(command);
                break;
        }
    }

    /**
     * v1.5.20: run a command owned by another plugin as the player. Missing
     * command: the "missing" message. Player lacks the command's own
     * permission node: the "denied" message. Otherwise performCommand, the
     * same path the Java menu's [player] click line uses.
     */
    private void runGuarded(Player player, String label, String missing, String denied) {
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand(label);
        switch (guardOutcome(cmd != null, cmd == null || cmd.testPermissionSilent(player))) {
            case 0:
                player.sendMessage(missing);
                break;
            case 1:
                player.sendMessage(denied);
                break;
            default:
                player.performCommand(label);
                break;
        }
    }

    /** 0 = command missing, 1 = no permission, 2 = run it. */
    static int guardOutcome(boolean commandExists, boolean hasPermission) {
        if (!commandExists) return 0;
        if (!hasPermission) return 1;
        return 2;
    }

    /** True when the console /spawn did not work and the village fallback must run. */
    static boolean needsSpawnFallback(boolean commandDispatched, String worldNameAfter) {
        return !commandDispatched || "em_adventurers_guild".equals(worldNameAfter);
    }

    /** The action a confirmed teleport button runs: the exact 1.5.7 behavior. */
    private void runConfirmedAction(Player player, String command) {
        if ("spawn".equals(command)) {
            teleportToSpawn(player);
        } else {
            player.performCommand(command);
        }
    }

    /**
     * v1.5.8: the teleport confirm step (see the Teleport confirm block
     * above). The action only runs on an explicit Yes, exactly once per
     * form answer. If the form cannot be sent at all (Geyser hiccup
     * after the menu itself worked), fall back to the pre-1.5.8
     * immediate action so the button never dead-ends.
     */
    private void confirmTeleport(Player player, String command, Runnable action) {
        boolean sent = false;
        try {
            sent = GeyserForms.sendModalForm(player,
                    teleportConfirmTitle(command, isGuildLocked()),
                    teleportConfirmContent(command), CONFIRM_YES, CONFIRM_NO,
                    first -> {
                        if (confirmChoiceTeleports(first) && player.isOnline()) {
                            Bukkit.getScheduler().runTask(this, action);
                        }
                    },
                    getLogger());
        } catch (Throwable t) {
            getLogger().warning("Teleport confirm form failed for " + player.getName() + ": " + t);
            sent = false;
        }
        if (!sent && player.isOnline()) {
            action.run();
        }
    }

    /**
     * v1.5.6 (final2): runs the real EssentialsX spawn for the player from the
     * console ("spawn <name>"). If the command is unavailable, or one tick later
     * the player is still in em_adventurers_guild, falls back to the overworld
     * village coordinates (config spawn.x/spawn.z).
     */
    private void teleportToSpawn(Player player) {
        boolean ok = false;
        try {
            ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + player.getName());
        } catch (Exception ignored) {
            ok = false;
        }
        final boolean dispatched = ok;
        if (!dispatched) {
            villageFallback(player);
            return;
        }
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline() && needsSpawnFallback(true, player.getWorld().getName())) {
                villageFallback(player);
            }
        });
    }

    private void villageFallback(Player player) {
        org.bukkit.World overworld = null;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            if (isSpawnWorld(w.getEnvironment() == org.bukkit.World.Environment.NORMAL, w.getName())) {
                overworld = w;
                break;
            }
        }
        if (overworld == null) {
            player.sendMessage("Spawn is unavailable right now. Ask an admin.");
            return;
        }
        double vx = getConfig().getDouble("spawn.x", SPAWN_X);
        double vz = getConfig().getDouble("spawn.z", SPAWN_Z);
        org.bukkit.Location village = new org.bukkit.Location(overworld, vx, SPAWN_Y, vz);
        village.setY(overworld.getHighestBlockYAt((int) vx, (int) vz) + 1);
        player.teleport(village);
    }
}
