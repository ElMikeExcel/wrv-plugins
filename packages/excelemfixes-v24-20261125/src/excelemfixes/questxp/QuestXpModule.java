package excelemfixes.questxp;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The quest-xp module (module id quest-xp): pays EliteMobs
 * skill XP for completing EliteMobs quests — the fold of
 * the standalone ExcelEMQuestXP 1.1.0 into this pack (Mike's
 * ruling D244, Nov 25, 2026). Behavior is 1.1.0's, ported
 * class for class; the /questxp command name, its syntax,
 * its messages and its popup content are exactly the
 * standalone's.
 *
 * WHY THIS MODULE EXISTS (player feedback — the REASON):
 * on Nov 19, 2026 a player (Jester) asked why completing a
 * quest "does not boost your exp — is it possible for that
 * to be an award for most of the quests?", because doing
 * quests felt pointless next to killing elites. He was
 * right about the gap: EliteMobs' QuestReward pays items +
 * currency only — there is NO native route for a quest to
 * pay skill XP in EliteMobs 10.9.x (verified by the W3
 * investigation, Nov 19, 2026). Mike's ruling D136 the same
 * day: quests pay EliteMobs XP, scaled to the quest. The
 * standalone bridge shipped Nov 20, 2026 (1.0.0; 1.0.1
 * added exactly-once backpay for quests completed before
 * the bridge existed; 1.1.0, under ruling D159, let each
 * player choose where their quest XP goes). Marvin praised
 * it in play — quests paying XP is why they feel worth
 * doing. When the Nov 25, 2026 consolidation audit found
 * the bridge still lived outside this pack, Mike ruled
 * (D244) to fold it in as this module and retire the
 * standalone jar.
 *
 * WHAT IT DOES: listens to EliteMobs' public
 * QuestCompleteEvent and grants the quest's table amount
 * (quest-xp/quest-xp.yml; 112 quests, 130,355 total XP;
 * sizing K x tier^2) into the player's chosen weapon skill
 * — their pinned skill (D159), or their highest weapon
 * skill by default — plus Armor at 1/3, through
 * EliteMobs' own PlayerData calls (see EmQuestXpBridge).
 * The /questxp backpay command pays the staged backlog
 * (quest-xp/backpay.yml) for quests completed before the
 * system existed, guarded by the persisted exactly-once
 * paid ledger (quest-xp/backpay-paid.yml).
 *
 * STATE + MIGRATION (the exactly-once core): all module
 * state lives in plugins/ExcelEMFixes/quest-xp/ — the
 * folder the install packet fills by copying the ENTIRE
 * live plugins/ExcelEMQuestXP/ folder byte-verified
 * BEFORE the first 0.22.0 boot (the same pattern as the
 * 1.1.0 rename migration). The module reads its table,
 * backpay table, paid ledger, target pins and grants.log
 * from that folder only, so a quest already in the paid
 * ledger can never pay again under the module: the ledger
 * is loaded at enable, re-read from disk at the start of
 * every backpay run, and written through to disk after
 * EVERY grant. A paid file that exists but cannot be
 * parsed DISABLES backpay (never read as "nothing paid").
 * The standalone jar is removed in the same install window
 * before the boot, so the two grant listeners never run
 * together.
 *
 * POPUP: 1.1.0's popup was its own code (a Java chest GUI +
 * its own Cumulus reflection bridge). The fold re-expresses
 * its content and order through the pack's shared renderer
 * (QuestXpView), so Java chest GUI / Bedrock form parity
 * comes from the renderer, per the build brief.
 */
public final class QuestXpModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;
    private volatile QuestXpTable table = new QuestXpTable(Map.of(), Map.of());
    private volatile BackpayTable backpayTable = BackpayTable.empty();
    private volatile BackpayLedger backpayLedger = new BackpayLedger();
    /** True when backpay-paid.yml exists but could not be parsed: backpay refuses to pay. */
    private volatile boolean backpayLedgerBroken;
    private volatile TargetStore targetStore = new TargetStore();
    private final GrantPolicy.ClaimLedger claimLedger = new GrantPolicy.ClaimLedger();
    private QuestXpListener listener;
    private boolean active;
    private File moduleDir;
    private File grantsLog;
    private File backpayFile;
    private File paidFile;
    private File targetsFile;

    @Override
    public String id() {
        return "quest-xp";
    }

    @Override
    public String displayName() {
        return "Quest XP";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        moduleDir = new File(ctx.plugin().getDataFolder(), "quest-xp");
        grantsLog = new File(moduleDir, "grants.log");
        backpayFile = new File(moduleDir, "backpay.yml");
        paidFile = new File(moduleDir, "backpay-paid.yml");
        targetsFile = new File(moduleDir, "targets.yml");
        ensureBundled("quest-xp.yml");
        ensureBundled("backpay.yml");
        if (!loadTable()) {
            ctx.logger().severe("Quest XP: quest-xp.yml failed to load — "
                    + "the quest-xp module idles (no listener, no grants) "
                    + "until the file is repaired and /emfixes reload runs. "
                    + "The rest of the pack is unaffected.");
            return;
        }
        loadBackpay();
        loadTargets();
        ctx.renderer().registerHandler("questxp", this::handleAction);
        ctx.registerCommand("questxp", List.of(),
                "/questxp target [highest|<skill>] | /questxp menu "
                        + "| /questxp reload | /questxp backpay <player>", this);
        active = emPresent() && bridgeProbes();
        if (active) {
            listener = new QuestXpListener(this, () -> table, claimLedger,
                    ctx.logger());
            Bukkit.getPluginManager().registerEvents(listener, ctx.plugin());
            ctx.logger().info("Quest XP ready — quests in table: " + table.size()
                    + "; total XP across table: " + table.totalXp()
                    + "; backpay players: " + backpayTable.playerCount()
                    + "; backpay already paid: " + backpayLedger.paidCountTotal()
                    + "; XP targets pinned: " + targetStore.size()
                    + "; pinned EliteMobs " + EmQuestXpBridge.EM_PIN + ".");
        } else {
            ctx.logger().warning("Quest XP: EliteMobs did not answer its "
                    + "probe classes (pinned EliteMobs "
                    + EmQuestXpBridge.EM_PIN + "); the module idles (no "
                    + "listener) until EliteMobs is present and its API "
                    + "responds. The /questxp command still answers.");
        }
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (ctx != null) ctx.renderer().unregisterHandler("questxp");
        active = false;
    }

    @Override
    public void reload() {
        if (ctx == null) return;
        loadTable();
        loadBackpay();
        loadTargets();
    }

    // ------------------------------------------------------------ data files

    /** Copies a bundled default into the module folder when
     *  no file is there yet (the install migration places the
     *  live files first; this only covers a fresh install). */
    private void ensureBundled(String name) {
        File out = new File(moduleDir, name);
        if (out.isFile()) return;
        try (InputStream in = ctx.plugin().getResource(name)) {
            if (in == null) {
                ctx.logger().warning("Quest XP: bundled " + name + " missing "
                        + "from the jar; the module folder has none either.");
                return;
            }
            moduleDir.mkdirs();
            Files.copy(in, out.toPath());
        } catch (IOException e) {
            ctx.logger().log(Level.WARNING,
                    "Quest XP: could not write bundled " + name, e);
        }
    }

    /**
     * (Re)loads targets.yml. A load failure never disables
     * the module: targets are a player preference, and the
     * safe fallback for a broken preference file is HIGHEST
     * for everyone (the pre-1.1.0 behavior), logged loudly.
     * (1.1.0's loadTargets, ported.)
     */
    public void loadTargets() {
        try {
            targetStore = TargetStore.load(targetsFile,
                    EmQuestXpBridge.weaponSkillNames());
            if (targetStore.skipped() > 0) {
                ctx.logger().warning("targets.yml: skipped " + targetStore.skipped()
                        + " malformed entr(ies) (those players fall back to HIGHEST).");
            }
            ctx.logger().info("XP targets loaded: " + targetStore.size() + " player(s) pinned.");
        } catch (Throwable t) {
            targetStore = new TargetStore();
            ctx.logger().log(Level.SEVERE, "Failed to load targets.yml — every player "
                    + "falls back to HIGHEST until the file is repaired: " + t, t);
        }
    }

    private void saveTargets() {
        try {
            moduleDir.mkdirs();
            targetStore.save(targetsFile);
        } catch (IOException e) {
            ctx.logger().warning("Quest XP: could not save targets.yml: " + e);
        }
    }

    /** The player's pinned weapon skill enum name, or null
     *  when they are on HIGHEST (auto). */
    public String pinnedSkillName(UUID uuid) {
        String name = targetStore.pinnedSkill(uuid);
        if (name == null) {
            return null;
        }
        return EmQuestXpBridge.isSkill(name) ? name : null;
    }

    /** Human line naming a player's current target, for /questxp target and the menu. */
    public String targetDescription(UUID uuid) {
        String pinned = pinnedSkillName(uuid);
        return pinned == null
                ? "Highest weapon skill (auto)"
                : EmQuestXpBridge.displayName(pinned) + " (pinned)";
    }

    /**
     * Applies a target choice from any door (typed command,
     * menu): persists it immediately and confirms in chat.
     * (1.1.0's applyTargetChoice, ported; the skill arrives
     * as its enum name.)
     *
     * @param skillEnumName the skill to pin, or null for HIGHEST (auto)
     */
    public void applyTargetChoice(Player player, String skillEnumName) {
        if (skillEnumName == null) {
            targetStore.clearPin(player.getUniqueId());
            saveTargets();
            player.sendMessage(ChatColor.GREEN + "Quest XP target: "
                    + ChatColor.BOLD + "Highest weapon skill (auto)" + ChatColor.RESET
                    + ChatColor.GREEN + " — quest XP pays your highest weapon skill, "
                    + "plus Armor at 1/3 as always.");
        } else {
            targetStore.setPin(player.getUniqueId(), skillEnumName);
            saveTargets();
            String display = EmQuestXpBridge.displayName(skillEnumName);
            player.sendMessage(ChatColor.GREEN + "Quest XP target pinned to "
                    + ChatColor.BOLD + display + ChatColor.RESET
                    + ChatColor.GREEN + " — quest XP now pays " + display
                    + ", plus Armor at 1/3 as always. "
                    + ChatColor.GRAY + "/questxp target highest" + ChatColor.GREEN
                    + " switches back to auto.");
        }
        ctx.logger().info("Quest XP target set: " + player.getName() + " -> "
                + (skillEnumName == null ? "highest" : "pinned:" + skillEnumName));
    }

    /** (Re)loads quest-xp.yml from the module folder. Returns false on failure.
     * Parsed with SnakeYAML directly, NOT Bukkit's YamlConfiguration:
     * Bukkit splits config keys on '.', so quest display names ending in
     * "..." ("Strangers in need...", "Into the next hole...") throw
     * "Cannot set to an empty path" under Bukkit's loader. SnakeYAML reads
     * the keys verbatim. (1.1.0's loadTable, ported.) */
    public boolean loadTable() {
        try {
            File file = new File(moduleDir, "quest-xp.yml");
            Map<String, Object> yaml;
            try (java.io.Reader reader = Files.newBufferedReader(file.toPath(),
                    StandardCharsets.UTF_8)) {
                yaml = new org.yaml.snakeyaml.Yaml().load(reader);
            }
            if (yaml == null) {
                ctx.logger().warning("quest-xp.yml is empty — table is empty.");
                return false;
            }
            Map<String, Long> byFile = readLongMap(yaml.get("quests"));
            Map<String, Long> byName = readLongMap(yaml.get("names"));
            if (byFile.isEmpty()) {
                ctx.logger().warning("quest-xp.yml has no quests section — table is empty.");
                return false;
            }
            table = new QuestXpTable(byFile, byName);
            ctx.logger().info("Quest XP table loaded: " + byFile.size() + " quests, "
                    + table.totalXp() + " total XP, " + byName.size() + " name fallbacks.");
            return true;
        } catch (Throwable t) {
            ctx.logger().log(Level.SEVERE, "Failed to load quest-xp.yml: " + t, t);
            return false;
        }
    }

    /**
     * (Re)loads backpay.yml and backpay-paid.yml from the
     * module folder. A missing backpay.yml means no backpay
     * is staged (empty table, not an error). A
     * present-but-corrupt backpay-paid.yml sets the broken
     * flag: the backpay command refuses to pay until the
     * file is repaired — a corrupt ledger must never be read
     * as "nothing paid yet". (1.1.0's loadBackpay, ported.)
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
                ctx.logger().warning("backpay.yml: skipped " + backpayTable.skipped()
                        + " malformed player entr(ies).");
            }
            ctx.logger().info("Backpay table loaded: " + backpayTable.playerCount()
                    + " players.");
        } catch (Throwable t) {
            ctx.logger().log(Level.SEVERE, "Failed to load backpay.yml (no backpay staged): " + t, t);
            backpayTable = BackpayTable.empty();
        }
        try {
            backpayLedger = BackpayLedger.load(paidFile);
            ctx.logger().info("Backpay paid ledger loaded: "
                    + backpayLedger.paidCountTotal() + " quest(s) already paid.");
        } catch (IOException e) {
            backpayLedgerBroken = true;
            backpayLedger = new BackpayLedger();
            ctx.logger().severe("backpay-paid.yml exists but cannot be parsed: " + e
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

    // --------------------------------------------------------------- grants

    /** The live-completion grant (the listener's half of the
     *  shared grant path). */
    void grantLive(Player player, UUID uuid, String filename,
                   String questName, long xp, String matchedBy) {
        EmQuestXpBridge.grant(player, uuid, filename, questName, xp,
                matchedBy, false, pinnedSkillName(uuid), ctx.logger(),
                this::audit);
    }

    /** Appends one audit line per grant to grants.log
     *  (1.1.0's logGrant / logBackpayGrant, ported — one
     *  method, the BACKPAY tag carried by the flag). */
    private synchronized void audit(Player player, String filename,
                                    String questName, String matchedBy,
                                    String skillName, long xp, long armorXp,
                                    int weaponBefore, int weaponAfter,
                                    int armorBefore, int armorAfter,
                                    String targetTag, boolean backpay) {
        appendGrantsLog(OffsetDateTime.now(ZoneId.of("America/Chicago"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                + (backpay ? " | BACKPAY" : "")
                + " | " + player.getName() + " | " + player.getUniqueId()
                + " | " + filename + " | " + questName + " | matched=" + matchedBy
                + " | target=" + targetTag
                + " | +" + xp + " XP to " + skillName + " (level " + weaponBefore + " -> "
                + weaponAfter + ") | +" + armorXp + " Armor XP (level " + armorBefore
                + " -> " + armorAfter + ")\n");
    }

    private void appendGrantsLog(String line) {
        try {
            moduleDir.mkdirs();
            Files.writeString(grantsLog.toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            ctx.logger().warning("Quest XP: could not append to grants.log: " + e);
        }
    }

    // -------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
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
        if (args.length == 0 && sender instanceof Player) {
            // Bare /questxp in game opens the target popup (D159).
            handleMenu(sender);
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "ExcelEMFixes quest-xp module "
                + "(formerly ExcelEMQuestXP 1.1.0) — " + table.size()
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
     * (1.1.0's handleTarget, ported verbatim.)
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
            String[][] skillTable = EmQuestXpBridge.weaponSkillTable();
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
                ctx.logger().info("Quest XP target refused for " + player.getName()
                        + ": unknown skill '" + input + "'.");
                return;
            }
            applyTargetChoice(player, resolved);
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
        if (!active) {
            player.sendMessage(ChatColor.GOLD + "Quest XP target menu could not open. "
                    + "Use " + ChatColor.BOLD + "/questxp target <skill>" + ChatColor.RESET
                    + ChatColor.GOLD + " or " + ChatColor.BOLD + "/questxp target highest"
                    + ChatColor.RESET + ChatColor.GOLD + " instead.");
            return;
        }
        ctx.renderer().openRoot(player, QuestXpView.view(
                pinnedSkillName(player.getUniqueId()), skillRows(player.getUniqueId())));
    }

    /** Menu actions: questxp:open | questxp:highest | questxp:set:<ENUM>.
     *  A pick applies immediately, confirms in chat (inside
     *  applyTargetChoice) and closes — 1.1.0's popup behavior. */
    private void handleAction(Player player, String action) {
        if (!active) return;
        if (QuestXpView.isOpen(action)) {
            ctx.renderer().open(player, QuestXpView.view(
                    pinnedSkillName(player.getUniqueId()),
                    skillRows(player.getUniqueId())));
            return;
        }
        if (QuestXpView.isHighest(action)) {
            applyTargetChoice(player, null);
            ctx.renderer().dispatch(player, "core:close");
            return;
        }
        String skill = QuestXpView.parseSet(action);
        if (skill != null) {
            if (EmQuestXpBridge.isSkill(skill)) {
                applyTargetChoice(player, skill);
            } else {
                ctx.logger().warning("Quest XP menu: stale skill action " + action);
            }
            ctx.renderer().dispatch(player, "core:close");
        }
    }

    private List<QuestXpView.SkillRow> skillRows(UUID uuid) {
        String[][] table = EmQuestXpBridge.weaponSkillTable();
        java.util.ArrayList<QuestXpView.SkillRow> rows = new java.util.ArrayList<>();
        for (int i = 0; i < table[0].length; i++) {
            rows.add(new QuestXpView.SkillRow(table[0][i], table[1][i],
                    EmQuestXpBridge.skillLevel(uuid, table[0][i]),
                    EmQuestXpBridge.skillXp(uuid, table[0][i])));
        }
        return rows;
    }

    // -------------------------------------------------------------- backpay

    /**
     * Pays one player's staged backpay, in order, with the
     * guards from the 1.0.1 build record, all-or-nothing per
     * player at the guard stage: (1) a backpay entry must
     * exist for the player (by UUID or staged name); (2) the
     * paid ledger must be readable (a corrupt ledger refuses
     * outright); (3) the player must be ONLINE with EliteMobs
     * data loaded — addSkillXP silently returns 0 for
     * unloaded data, so an offline pay would silently grant
     * nothing while LOOKING paid if the ledger were marked
     * (this check runs BEFORE the counter guard: a player
     * with no database row at all makes getQuestsCompleted
     * throw NPE, so the guard only ever runs for a loaded
     * player, whose row exists); (4) the live
     * QuestsCompleted counter must equal the staged snapshot.
     * Each quest is then granted through the shared grant
     * path and written to backpay-paid.yml immediately
     * (write-through per quest). Already-paid quests grant 0
     * and are reported, never re-paid. (1.1.0's runBackpay,
     * ported verbatim in behavior.)
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
            ctx.logger().warning("Backpay refused for '" + target + "': no entry in backpay.yml.");
            return;
        }
        UUID uuid = entry.uuid();
        String name = entry.name();

        if (backpayLedgerBroken) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": backpay-paid.yml is corrupt/unparseable. Repair the file, then "
                    + "/questxp reload. Nothing was paid.");
            ctx.logger().severe("Backpay refused for " + name + ": paid ledger is broken.");
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
            ctx.logger().severe("Backpay refused for " + name + ": paid ledger unreadable: " + e);
            return;
        }

        // Offline grants silently pay 0 (addSkillXP returns 0 when data is
        // not loaded) — refuse instead of paying nothing. This runs BEFORE
        // the counter guard below: getQuestsCompleted throws NPE for a
        // player with no database row (never joined), so the guard only
        // ever runs for a loaded player, whose row exists.
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !EmQuestXpBridge.isDataLoaded(uuid)) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": the player must be ONLINE (EliteMobs data loaded). Offline "
                    + "grants silently pay 0 XP. Nothing was paid.");
            ctx.logger().warning("Backpay refused for " + name
                    + ": player not online / data not loaded.");
            return;
        }

        // Pre-pay guard: live QuestsCompleted counter must match the snapshot.
        int liveCounter;
        try {
            liveCounter = EmQuestXpBridge.questsCompleted(uuid);
        } catch (Throwable t) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": could not verify the live QuestsCompleted counter (" + t
                    + "). Re-pull player_data.db and recompute before paying. Nothing was paid.");
            ctx.logger().severe("Backpay refused for " + name
                    + ": getQuestsCompleted failed: " + t);
            return;
        }
        if (!BackpayTable.counterMatches(liveCounter, entry.questsCompleted())) {
            sender.sendMessage(ChatColor.RED + "Backpay REFUSED for " + name
                    + ": live QuestsCompleted is " + liveCounter + ", but backpay.yml "
                    + "snapshots " + entry.questsCompleted() + ". The player completed "
                    + "more quests since staging — re-pull player_data.db and recompute "
                    + "the table before paying. Nothing was paid.");
            ctx.logger().warning("Backpay refused for " + name + ": counter moved (live "
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
                ctx.logger().info("Backpay already paid: " + name + " / " + file + " — skipping.");
                continue;
            }
            if (xp <= 0 || tableXp(file) <= 0) {
                // Configured zero (test_quest / xmas_quest_0): grant 0 by design.
                zeroSkipped++;
                ctx.logger().info("Backpay 0 XP (configured): " + name + " / " + file + " — skipping.");
                continue;
            }
            long armor = GrantPolicy.armorXp(xp);
            try {
                EmQuestXpBridge.Outcome outcome = EmQuestXpBridge.grant(
                        player, uuid, file, questDisplayName(file), xp,
                        "backpay:" + file, true, pinnedSkillName(uuid),
                        ctx.logger(), this::audit);
                skillPaid = outcome.skillName();
                // Write-through AFTER every grant: a crash mid-run can never double-pay.
                backpayLedger.markPaid(uuid, file, xp, armor, stampNow());
                backpayLedger.save(paidFile);
                claimLedger.tryClaim(uuid, "backpay:" + file);
                paidNow++;
                weaponPaid += xp;
                armorPaid += armor;
            } catch (Throwable t) {
                sender.sendMessage(ChatColor.RED + "Backpay STOPPED for " + name
                        + " at " + file + ": grant failed (" + t + "). Quests before this "
                        + "one are paid and recorded; this quest and later ones were NOT paid. "
                        + "See the console; fix the cause, then re-run the command (paid "
                        + "quests are skipped automatically).");
                ctx.logger().severe("Backpay grant FAILED for " + name + " / " + file + ": " + t);
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
        ctx.logger().info(summary);
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

    // -------------------------------------------------------------- helpers

    private boolean emPresent() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        return em != null && em.isEnabled();
    }

    private boolean bridgeProbes() {
        try {
            return EmQuestXpBridge.probe();
        } catch (Throwable t) {
            return false;
        }
    }

    // Test/rig hooks.
    QuestXpTable table() {
        return table;
    }

    BackpayLedger backpayLedger() {
        return backpayLedger;
    }

    TargetStore targetStore() {
        return targetStore;
    }

    boolean active() {
        return active;
    }
}
