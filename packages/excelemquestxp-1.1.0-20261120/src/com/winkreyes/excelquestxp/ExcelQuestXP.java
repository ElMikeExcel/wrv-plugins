package com.winkreyes.excelquestxp;

import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.skills.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * ExcelEMQuestXP 1.1.0 — EliteMobs quest-XP bridge (D136) + backpay
 * command (D136 backpay directive) + player-chosen XP target (D159).
 * Renamed from ExcelQuestXP under the D160 naming law (EliteMobs-related
 * plugins carry the ExcelEM prefix); the {@code /questxp} command and
 * the Java package are unchanged.
 *
 * <p>EliteMobs quests pay no XP natively (QuestReward handles items +
 * currency only; there is no add-XP command). This plugin listens to the
 * public QuestCompleteEvent and grants EliteMobs skill XP from its own
 * quest-xp.yml table (112 authored quests, D-W3-2 sizing), plus Armor at
 * 1/3 (D-W3-1). Version 1.0.1 adds the op-only
 * {@code /questxp backpay &lt;player&gt;} command, which pays the staged
 * backlog for quests completed BEFORE the bridge was installed, from
 * backpay.yml, with a persisted exactly-once ledger (backpay-paid.yml).
 * Version 1.1.0 (D159) lets each player choose the grant target — a
 * pinned weapon skill, or the default HIGHEST (the pre-1.1.0 rule,
 * unchanged) — via {@code /questxp target} and the {@code /questxp menu}
 * popup (Java chest GUI / Bedrock form), persisted in targets.yml.
 *
 * <p>Internal-API coupling (see BUILD-RECORD.md): the grant uses
 * {@code PlayerData.addSkillXP} and friends from EliteMobs' internal
 * packages, pinned to EliteMobs 10.9.14 (live file named 10.9.10). The enable-time probe below fails
 * the plugin cleanly (disabled, logged) instead of half-running if those
 * classes are absent — e.g. after an EliteMobs upgrade that refactors them.
 */
public final class ExcelQuestXP extends JavaPlugin {

    private static final String[] PROBE_CLASSES = {
            "com.magmaguy.elitemobs.playerdata.database.PlayerData",
            "com.magmaguy.elitemobs.api.QuestCompleteEvent",
            "com.magmaguy.elitemobs.quests.CustomQuest",
            "com.magmaguy.elitemobs.skills.SkillType",
            "com.magmaguy.elitemobs.skills.SkillXPCalculator",
            "com.magmaguy.elitemobs.skills.CombatLevelDisplay",
            "com.magmaguy.elitemobs.skills.ArmorSkillHealthBonus",
    };

    private volatile QuestXpTable table = new QuestXpTable(Map.of(), Map.of());
    private volatile BackpayTable backpayTable = BackpayTable.empty();
    private volatile BackpayLedger backpayLedger = new BackpayLedger();
    /** True when backpay-paid.yml exists but could not be parsed: backpay refuses to pay. */
    private volatile boolean backpayLedgerBroken;
    private volatile TargetStore targetStore = new TargetStore();
    private final GrantPolicy.ClaimLedger ledger = new GrantPolicy.ClaimLedger();
    private TargetMenu targetMenu;
    private File grantsLog;
    private File backpayFile;
    private File paidFile;
    private File targetsFile;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
            getLogger().severe("EliteMobs is not installed — ExcelEMQuestXP disables itself.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        for (String probe : PROBE_CLASSES) {
            try {
                Class.forName(probe, false, getClass().getClassLoader());
            } catch (Throwable t) {
                getLogger().severe("EliteMobs API class missing (" + probe + ") — this build "
                        + "is pinned to EliteMobs " + BuildInfo.EM_PIN + ". ExcelEMQuestXP "
                        + "disables itself rather than half-running. Re-verify against the "
                        + "installed EliteMobs version. Cause: " + t);
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        saveResource("quest-xp.yml", false);
        saveResource("backpay.yml", false);
        grantsLog = new File(getDataFolder(), "grants.log");
        backpayFile = new File(getDataFolder(), "backpay.yml");
        paidFile = new File(getDataFolder(), "backpay-paid.yml");
        targetsFile = new File(getDataFolder(), "targets.yml");
        if (!loadTable()) {
            getLogger().severe("quest-xp.yml failed to load — ExcelEMQuestXP disables itself.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        loadBackpay();
        loadTargets();
        getServer().getPluginManager().registerEvents(
                new QuestXpListener(this, () -> table, ledger), this);
        targetMenu = new TargetMenu(this);
        getServer().getPluginManager().registerEvents(targetMenu, this);
        getLogger().info("ExcelEMQuestXP " + BuildInfo.VERSION + " ready — build "
                + BuildInfo.BUILD_ID + "; quests in table: " + table.size()
                + "; total XP across table: " + table.totalXp()
                + "; backpay players: " + backpayTable.playerCount()
                + "; backpay already paid: " + backpayLedger.paidCountTotal()
                + "; XP targets pinned: " + targetStore.size()
                + "; pinned EliteMobs " + BuildInfo.EM_PIN + ".");
    }

    /**
     * (Re)loads targets.yml. A load failure never disables the plugin:
     * targets are a player preference, and the safe fallback for a broken
     * preference file is HIGHEST for everyone (the pre-1.1.0 behavior),
     * logged loudly.
     */
    public void loadTargets() {
        try {
            targetStore = TargetStore.load(targetsFile, weaponSkillNames());
            if (targetStore.skipped() > 0) {
                getLogger().warning("targets.yml: skipped " + targetStore.skipped()
                        + " malformed entr(ies) (those players fall back to HIGHEST).");
            }
            getLogger().info("XP targets loaded: " + targetStore.size() + " player(s) pinned.");
        } catch (Throwable t) {
            targetStore = new TargetStore();
            getLogger().log(Level.SEVERE, "Failed to load targets.yml — every player "
                    + "falls back to HIGHEST until the file is repaired: " + t, t);
        }
    }

    private void saveTargets() {
        try {
            targetStore.save(targetsFile);
        } catch (IOException e) {
            getLogger().warning("Quest XP: could not save targets.yml: " + e);
        }
    }

    /** Live weapon-skill enum names (EliteMobs order), as a set. */
    public static Set<String> weaponSkillNames() {
        Set<String> names = new HashSet<>();
        for (SkillType skill : SkillType.getWeaponSkills()) {
            names.add(skill.name());
        }
        return names;
    }

    /** Live weapon-skill enum names + display names, EliteMobs order (TargetPolicy input). */
    public static String[][] weaponSkillTable() {
        SkillType[] weapons = SkillType.getWeaponSkills();
        String[] enumNames = new String[weapons.length];
        String[] displayNames = new String[weapons.length];
        for (int i = 0; i < weapons.length; i++) {
            enumNames[i] = weapons[i].name();
            displayNames[i] = weapons[i].getDisplayName();
        }
        return new String[][]{enumNames, displayNames};
    }

    /** The player's pinned weapon skill, or null when they are on HIGHEST (auto). */
    public SkillType pinnedSkill(UUID uuid) {
        String name = targetStore.pinnedSkill(uuid);
        if (name == null) {
            return null;
        }
        try {
            return SkillType.valueOf(name);
        } catch (IllegalArgumentException stale) {
            return null;
        }
    }

    /** Human line naming a player's current target, for /questxp target and the menu. */
    public String targetDescription(UUID uuid) {
        SkillType pinned = pinnedSkill(uuid);
        return pinned == null
                ? "Highest weapon skill (auto)"
                : pinned.getDisplayName() + " (pinned)";
    }

    /**
     * Applies a target choice from any door (typed command, Java menu,
     * Bedrock form): persists it immediately and confirms in chat.
     *
     * @param skill the skill to pin, or null for HIGHEST (auto)
     */
    public void applyTargetChoice(Player player, SkillType skill) {
        if (skill == null) {
            targetStore.clearPin(player.getUniqueId());
            saveTargets();
            player.sendMessage(ChatColor.GREEN + "Quest XP target: "
                    + ChatColor.BOLD + "Highest weapon skill (auto)" + ChatColor.RESET
                    + ChatColor.GREEN + " — quest XP pays your highest weapon skill, "
                    + "plus Armor at 1/3 as always.");
        } else {
            targetStore.setPin(player.getUniqueId(), skill.name());
            saveTargets();
            player.sendMessage(ChatColor.GREEN + "Quest XP target pinned to "
                    + ChatColor.BOLD + skill.getDisplayName() + ChatColor.RESET
                    + ChatColor.GREEN + " — quest XP now pays " + skill.getDisplayName()
                    + ", plus Armor at 1/3 as always. "
                    + ChatColor.GRAY + "/questxp target highest" + ChatColor.GREEN
                    + " switches back to auto.");
        }
        getLogger().info("Quest XP target set: " + player.getName() + " -> "
                + (skill == null ? "highest" : "pinned:" + skill.name()));
    }

    /** (Re)loads quest-xp.yml from the data folder. Returns false on failure.
     * Parsed with SnakeYAML directly, NOT Bukkit's YamlConfiguration:
     * Bukkit splits config keys on '.', so quest display names ending in
     * "..." ("Strangers in need...", "Into the next hole...") throw
     * "Cannot set to an empty path" under Bukkit's loader. SnakeYAML reads
     * the keys verbatim. */
    public boolean loadTable() {
        try {
            File file = new File(getDataFolder(), "quest-xp.yml");
            Map<String, Object> yaml;
            try (java.io.Reader reader = Files.newBufferedReader(file.toPath(),
                    StandardCharsets.UTF_8)) {
                yaml = new org.yaml.snakeyaml.Yaml().load(reader);
            }
            if (yaml == null) {
                getLogger().warning("quest-xp.yml is empty — table is empty.");
                return false;
            }
            Map<String, Long> byFile = readLongMap(yaml.get("quests"));
            Map<String, Long> byName = readLongMap(yaml.get("names"));
            if (byFile.isEmpty()) {
                getLogger().warning("quest-xp.yml has no quests section — table is empty.");
                return false;
            }
            table = new QuestXpTable(byFile, byName);
            getLogger().info("Quest XP table loaded: " + byFile.size() + " quests, "
                    + table.totalXp() + " total XP, " + byName.size() + " name fallbacks.");
            return true;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to load quest-xp.yml: " + t, t);
            return false;
        }
    }

    /**
     * (Re)loads backpay.yml and backpay-paid.yml from the data folder.
     * A missing backpay.yml means no backpay is staged (empty table, not an
     * error). A present-but-corrupt backpay-paid.yml sets the broken flag:
     * the backpay command refuses to pay until the file is repaired — a
     * corrupt ledger must never be read as "nothing paid yet".
     */
    public void loadBackpay() {
        backpayLedgerBroken = false;
        try {
            Map<String, Object> yaml;
            try (java.io.Reader reader = Files.newBufferedReader(backpayFile.toPath(),
                    StandardCharsets.UTF_8)) {
                yaml = new org.yaml.snakeyaml.Yaml().load(reader);
            }
            backpayTable = BackpayTable.parse(yaml);
            if (backpayTable.skipped() > 0) {
                getLogger().warning("backpay.yml: skipped " + backpayTable.skipped()
                        + " malformed player entr(ies).");
            }
            getLogger().info("Backpay table loaded: " + backpayTable.playerCount()
                    + " players.");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to load backpay.yml (no backpay staged): " + t, t);
            backpayTable = BackpayTable.empty();
        }
        try {
            backpayLedger = BackpayLedger.load(paidFile);
            getLogger().info("Backpay paid ledger loaded: "
                    + backpayLedger.paidCountTotal() + " quest(s) already paid.");
        } catch (IOException e) {
            backpayLedgerBroken = true;
            backpayLedger = new BackpayLedger();
            getLogger().severe("backpay-paid.yml exists but cannot be parsed: " + e
                    + " — /questxp backpay is DISABLED until the file is repaired. "
                    + "Live quest grants are unaffected.");
        }
    }

    private static Map<String, Long> readLongMap(Object section) {
        Map<String, Long> out = new HashMap<>();
        if (section instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() instanceof String key && e.getValue() instanceof Number n) {
                    out.put(key, n.longValue());
                }
            }
        }
        return out;
    }

    /** Appends one audit line per grant (player, quest, skill, amounts, levels, target). */
    public synchronized void logGrant(org.bukkit.entity.Player player, String filename,
                                      String questName, String matchedBy, String skillName,
                                      long xp, long armorXp, int weaponBefore, int weaponAfter,
                                      int armorBefore, int armorAfter, String targetTag) {
        appendGrantsLog(OffsetDateTime.now(ZoneId.of("America/Chicago"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + " | " + player.getName() + " | " + player.getUniqueId()
                + " | " + filename + " | " + questName + " | matched=" + matchedBy
                + " | target=" + targetTag
                + " | +" + xp + " XP to " + skillName + " (level " + weaponBefore + " -> "
                + weaponAfter + ") | +" + armorXp + " Armor XP (level " + armorBefore
                + " -> " + armorAfter + ")\n");
    }

    /** Appends one BACKPAY-tagged audit line per backpay grant (same file, grants.log). */
    public synchronized void logBackpayGrant(org.bukkit.entity.Player player, String filename,
                                             String questName, String matchedBy, String skillName,
                                             long xp, long armorXp, int weaponBefore, int weaponAfter,
                                             int armorBefore, int armorAfter, String targetTag) {
        appendGrantsLog(OffsetDateTime.now(ZoneId.of("America/Chicago"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + " | BACKPAY | " + player.getName() + " | " + player.getUniqueId()
                + " | " + filename + " | " + questName + " | matched=" + matchedBy
                + " | target=" + targetTag
                + " | +" + xp + " XP to " + skillName + " (level " + weaponBefore + " -> "
                + weaponAfter + ") | +" + armorXp + " Armor XP (level " + armorBefore
                + " -> " + armorAfter + ")\n");
    }

    private void appendGrantsLog(String line) {
        try {
            Files.writeString(grantsLog.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            getLogger().warning("Quest XP: could not append to grants.log: " + e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("questxp")) {
            return false;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("target")) {
            handleTarget(sender, args);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("menu")) {
            handleMenu(sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("excelquestxp.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                return true;
            }
            boolean ok = loadTable();
            loadBackpay();
            loadTargets();
            if (ok) {
                sender.sendMessage(ChatColor.GREEN + "Quest XP table reloaded: "
                        + table.size() + " quests, " + table.totalXp() + " total XP. "
                        + "Backpay table: " + backpayTable.playerCount() + " players, "
                        + backpayLedger.paidCountTotal() + " quest(s) already paid. "
                        + "XP targets: " + targetStore.size() + " player(s) pinned.");
            } else {
                sender.sendMessage(ChatColor.RED + "Quest XP table reload FAILED — "
                        + "previous table kept. See the console.");
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("backpay")) {
            if (!sender.hasPermission("excelquestxp.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                return true;
            }
            runBackpay(sender, args[1]);
            return true;
        }
        if (args.length == 0 && sender instanceof Player player) {
            // Bare /questxp in game opens the target popup (D159).
            handleMenu(sender);
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "ExcelEMQuestXP " + BuildInfo.VERSION
                + " (build " + BuildInfo.BUILD_ID + ") — " + table.size()
                + " quests, " + table.totalXp() + " total XP. Backpay: "
                + backpayTable.playerCount() + " players staged, "
                + backpayLedger.paidCountTotal() + " quest(s) already paid. "
                + "Usage: /questxp target [highest|<skill>] | /questxp menu "
                + "| /questxp reload | /questxp backpay <player>");
        return true;
    }

    /**
     * /questxp target — show or set the sender's own XP target (D159).
     * Player-only: the target is a per-player choice, and the console has
     * no skills to pay. Permission excelquestxp.target (default true).
     */
    private void handleTarget(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.GOLD + "The quest XP target is a per-player "
                    + "setting — run /questxp target in game. Console commands: "
                    + "/questxp reload | /questxp backpay <player>");
            return;
        }
        if (!sender.hasPermission("excelquestxp.target")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return;
        }
        if (args.length == 1) {
            player.sendMessage(ChatColor.GOLD + "Your quest XP target: "
                    + ChatColor.BOLD + targetDescription(player.getUniqueId())
                    + ChatColor.RESET + ChatColor.GOLD + ". Change it with "
                    + ChatColor.BOLD + "/questxp target <skill>" + ChatColor.RESET
                    + ChatColor.GOLD + ", " + ChatColor.BOLD + "/questxp target highest"
                    + ChatColor.RESET + ChatColor.GOLD + ", or the popup: "
                    + ChatColor.BOLD + "/questxp menu" + ChatColor.RESET + ChatColor.GOLD + ".");
            return;
        }
        if (args.length == 2) {
            String input = args[1];
            if (TargetPolicy.isHighestWord(input)) {
                applyTargetChoice(player, null);
                return;
            }
            String[][] skillTable = weaponSkillTable();
            String resolved = TargetPolicy.resolveSkill(input, skillTable[0], skillTable[1]);
            if (resolved == null) {
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < skillTable[1].length; i++) {
                    if (i > 0) {
                        names.append(", ");
                    }
                    names.append(skillTable[1][i]);
                }
                player.sendMessage(ChatColor.RED + "Unknown skill '" + input + "'. "
                        + "Pick one of: Highest (auto), " + names + " — or open "
                        + "/questxp menu and tap your skill.");
                getLogger().info("Quest XP target refused for " + player.getName()
                        + ": unknown skill '" + input + "'.");
                return;
            }
            applyTargetChoice(player, SkillType.valueOf(resolved));
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Usage: /questxp target | "
                + "/questxp target highest | /questxp target <skill> | /questxp menu");
    }

    /** /questxp menu (and bare /questxp) — the target popup. Player-only. */
    private void handleMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.GOLD + "The quest XP target menu is in-game "
                    + "only. Console commands: /questxp reload | /questxp backpay <player>");
            return;
        }
        if (!sender.hasPermission("excelquestxp.target")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return;
        }
        if (targetMenu != null) {
            targetMenu.open(player);
        }
    }

    /**
     * Pays one player's staged backpay, in order, with the guards from
     * BACKPAY-RECORD.md §4(b), all-or-nothing per player at the guard stage:
     * <ol>
     *   <li>A backpay entry must exist for the player (by UUID or staged name).</li>
     *   <li>The paid ledger must be readable (a corrupt ledger refuses outright).</li>
     *   <li>The player must be ONLINE with EliteMobs data loaded:
     *       {@code PlayerData.addSkillXP} silently returns 0 for unloaded
     *       data (bytecode-proven), so an offline pay would silently grant
     *       nothing while LOOKING paid if the ledger were marked. This check
     *       runs BEFORE the counter guard: a player with no database row at
     *       all (never joined) makes {@code PlayerData.getQuestsCompleted}
     *       throw a NullPointerException (it unboxes a null
     *       {@code getDatabaseInteger}), so the guard only ever runs for a
     *       loaded player, whose row exists.</li>
     *   <li>The live {@code QuestsCompleted} counter must equal the staged
     *       snapshot — a moved counter means recompute from a fresh pull.</li>
     * </ol>
     * Each quest is then granted through the shared {@link SkillGrant} path
     * and written to backpay-paid.yml immediately (write-through per quest).
     * Already-paid quests grant 0 and are reported, never re-paid.
     */
    private void runBackpay(CommandSender sender, String target) {
        // Resolve the target: UUID first, then staged name, then an online player's name.
        BackpayTable.Entry entry = null;
        try {
            entry = backpayTable.forUuid(UUID.fromString(target.trim()));
        } catch (IllegalArgumentException notUuid) {
            entry = backpayTable.forName(target.trim());
        }
        if (entry == null) {
            Player online = Bukkit.getPlayerExact(target);
            if (online != null) {
                entry = backpayTable.forUuid(online.getUniqueId());
            }
        }
        if (entry == null) {
            sender.sendMessage(ChatColor.RED + "Backpay: no backpay entry for '" + target
                    + "'. Check the name/UUID and backpay.yml.");
            getLogger().warning("Backpay refused for '" + target + "': no entry in backpay.yml.");
            return;
        }
        UUID uuid = entry.uuid();
        String name = entry.name();

        if (backpayLedgerBroken) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": backpay-paid.yml is corrupt/unparseable. Repair the file, then "
                    + "/questxp reload. Nothing was paid.");
            getLogger().severe("Backpay refused for " + name + ": paid ledger is broken.");
            return;
        }
        // Re-read the paid ledger from disk so a hand-corrected file is honored
        // and a stale in-memory copy can never double-pay.
        try {
            backpayLedger = BackpayLedger.load(paidFile);
        } catch (IOException e) {
            backpayLedgerBroken = true;
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": backpay-paid.yml could not be read (" + e.getMessage() + "). "
                    + "Nothing was paid.");
            getLogger().severe("Backpay refused for " + name + ": paid ledger unreadable: " + e);
            return;
        }

        // Offline grants silently pay 0 (addSkillXP returns 0 when data is
        // not loaded) — refuse instead of paying nothing. This runs BEFORE
        // the counter guard below: getQuestsCompleted throws NPE for a
        // player with no database row (never joined), so the guard only
        // ever runs for a loaded player, whose row exists.
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !PlayerData.isDataLoaded(uuid)) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": the player must be ONLINE (EliteMobs data loaded). Offline "
                    + "grants silently pay 0 XP. Nothing was paid.");
            getLogger().warning("Backpay refused for " + name
                    + ": player not online / data not loaded.");
            return;
        }

        // Pre-pay guard: live QuestsCompleted counter must match the snapshot.
        int liveCounter;
        try {
            liveCounter = PlayerData.getQuestsCompleted(uuid);
        } catch (Throwable t) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": could not verify the live QuestsCompleted counter (" + t
                    + "). Re-pull player_data.db and recompute before paying. Nothing was paid.");
            getLogger().severe("Backpay refused for " + name
                    + ": getQuestsCompleted failed: " + t);
            return;
        }
        if (!BackpayTable.counterMatches(liveCounter, entry.questsCompleted())) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": live QuestsCompleted is " + liveCounter + ", but backpay.yml "
                    + "snapshots " + entry.questsCompleted() + ". The player completed "
                    + "more quests since staging — re-pull player_data.db and recompute "
                    + "the table before paying. Nothing was paid.");
            getLogger().warning("Backpay refused for " + name + ": counter moved (live "
                    + liveCounter + " vs snapshot " + entry.questsCompleted() + ").");
            return;
        }

        int paidNow = 0;
        int alreadyPaid = 0;
        int zeroSkipped = 0;
        long weaponPaid = 0;
        long armorPaid = 0;
        String skillPaid = null;
        for (Map.Entry<String, Long> quest : entry.quests().entrySet()) {
            String file = quest.getKey();
            long xp = quest.getValue();
            if (backpayLedger.isPaid(uuid, file)) {
                alreadyPaid++;
                getLogger().info("Backpay already paid: " + name + " / " + file + " — skipping.");
                continue;
            }
            if (xp <= 0 || tableXp(file) <= 0) {
                // Configured zero (test_quest / xmas_quest_0): grant 0 by design.
                zeroSkipped++;
                getLogger().info("Backpay 0 XP (configured): " + name + " / " + file + " — skipping.");
                continue;
            }
            long armor = GrantPolicy.armorXp(xp);
            try {
                SkillGrant.Outcome outcome = SkillGrant.grant(this, player, uuid, file,
                        questDisplayName(file), xp, "backpay:" + file, true);
                skillPaid = outcome.skillName();
                // Write-through AFTER every grant: a crash mid-run can never double-pay.
                backpayLedger.markPaid(uuid, file, xp, armor, stampNow());
                backpayLedger.save(paidFile);
                ledger.tryClaim(uuid, "backpay:" + file);
                paidNow++;
                weaponPaid += xp;
                armorPaid += armor;
            } catch (Throwable t) {
                sender.sendMessage(ChatColor.RED + "Backpay STOPPED for " + name
                        + " at " + file + ": grant failed (" + t + "). Quests before this "
                        + "one are paid and recorded; this quest and later ones were NOT paid. "
                        + "See the console; fix the cause, then re-run the command (paid "
                        + "quests are skipped automatically).");
                getLogger().severe("Backpay grant FAILED for " + name + " / " + file + ": " + t);
                return;
            }
        }

        String summary = "Backpay for " + name + ": paid " + paidNow + " quest(s), +"
                + weaponPaid + " XP to " + (skillPaid == null ? "—" : skillPaid) + ", +"
                + armorPaid + " Armor XP"
                + (alreadyPaid > 0 ? "; " + alreadyPaid + " already paid (0 granted)" : "")
                + (zeroSkipped > 0 ? "; " + zeroSkipped + " zero-XP skipped" : "")
                + ".";
        sender.sendMessage(paidNow > 0 ? ChatColor.GREEN + summary : ChatColor.YELLOW + summary);
        getLogger().info(summary);
    }

    /** Quest-table XP for a backpay file key (bare or primis/-prefixed form). */
    private long tableXp(String file) {
        long xp = table.xpForFile(file);
        if (xp == 0 && !file.startsWith("primis/")) {
            xp = table.xpForFile("primis/" + file);
        }
        return xp;
    }

    private String questDisplayName(String file) {
        return file;
    }

    private static String stampNow() {
        return OffsetDateTime.now(ZoneId.of("America/Chicago"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
