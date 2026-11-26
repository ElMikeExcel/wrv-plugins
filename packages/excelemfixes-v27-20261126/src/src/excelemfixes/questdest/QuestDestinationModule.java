package excelemfixes.questdest;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Fix module 1: quest-tracker destinations (upstream EliteMobs
 * issue #241). EM's tracker shows no destination for quests whose
 * target boss lives in an instanced dungeon, because the tracker
 * never loads an instanced boss's spawn locations into a form it
 * can present. That code is inside EliteMobs and cannot be
 * repaired from outside, so this module gives the player the
 * destination itself: /questwhere lists the player's own active
 * tracker-blank quests and, per quest, the place, the entry
 * wormhole's coordinates and step-by-step directions.
 *
 * Information only: no teleports, no /warps — travel stays stock
 * EliteMobs (walking + EM's own wormholes), per design law. The
 * glow guidance (the server's existing glow approach, reused from
 * a GLOWING potion effect) only lights the destination for a short
 * while; it never moves anyone.
 *
 * Command surface (also the integration point for menus — a
 * future companion progress entry can run these on a
 * click, for the player's own quests):
 *   /questwhere                    the menu
 *   /questwhere show  <quest file> the directions screen
 *   /questwhere guide <quest file> destination in chat + the glow
 */
public final class QuestDestinationModule
        implements FixModule, CommandExecutor, Listener {

    private ModuleContext ctx;
    private QuestWhereMenus menus;
    private GlowGuide glow;
    private boolean emAvailable;
    private boolean joinHint = true;
    private boolean deriveDialog = true;
    private boolean deriveFetch = true;
    private boolean deriveTurnIn = true;
    private TrackerBlankScan.Scan lastScan;
    private final excelemfixes.api.QuestDestinationApi
            .DialogDestinationProvider dialogProvider =
            this::resolveDestination;

    @Override
    public String id() {
        return "quest-destinations";
    }

    @Override
    public String displayName() {
        return "Quest Destinations";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        if (!loadDestinations()) {
            throw new IllegalStateException(
                    "quest-destinations table failed to load");
        }
        emAvailable = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        if (!emAvailable) {
            ctx.logger().warning("Quest Destinations: EliteMobs not "
                    + "found — /questwhere will show its unavailable "
                    + "panel.");
        }
        glow = new GlowGuide(ctx.plugin(), ctx.logger());
        loadGlowSettings();
        glow.start();
        excelemfixes.api.GlowApi.register(glow);
        runCoverageScan();
        ctx.renderer().registerHandler("questwhere", this::handleAction);
        ctx.registerCommand("questwhere", List.of("questdest"),
                "/questwhere [guide|show <quest> | glow <world> "
                        + "<x> <y> <z> [entity]]", this);
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        ctx.logger().info("Quest Destinations ready — "
                + menus.destinations().all().size()
                + " tracker-blank quests covered.");
    }

    @Override
    public void onDisable() {
        if (ctx != null) ctx.renderer().unregisterHandler("questwhere");
        excelemfixes.api.QuestDestinationApi.unregisterDialogProvider(
                dialogProvider);
        if (menus != null) {
            excelemfixes.api.QuestDestinationApi.unregister(
                    menus.destinations());
        }
        HandlerList.unregisterAll(this);
        if (glow != null) {
            excelemfixes.api.GlowApi.unregister(glow);
            glow.stop();
        }
    }

    @Override
    public void reload() {
        loadSettings();
        loadDestinations();
        if (glow != null) {
            loadGlowSettings();
            glow.start();
        }
        runCoverageScan();
    }

    /**
     * Expansion-generic coverage: scan EliteMobs' own quest/boss
     * data for the tracker-blank defect class and report any
     * quest the destination table does not cover yet. A future
     * pack's blank quests announce themselves here by filename
     * the day the pack lands; covering one is a data row, never
     * a code change.
     */
    private void runCoverageScan() {
        if (!emAvailable) return;
        try {
            org.bukkit.plugin.Plugin em =
                    Bukkit.getPluginManager().getPlugin("EliteMobs");
            if (em == null) return;
            java.util.Set<String> covered = new java.util.HashSet<>();
            for (QuestDestinations.Destination d : menus.destinations().all()) {
                covered.add(d.questFile());
            }
            lastScan = TrackerBlankScan.scan(em.getDataFolder(), covered,
                    menus.destinations().ignoredFiles());
            ctx.logger().info("Tracker-blank coverage: "
                    + (lastScan.trackerBlankQuests().size()
                            - lastScan.uncoveredQuests().size())
                    + " of " + lastScan.trackerBlankQuests().size()
                    + " covered (scanned " + lastScan.questsScanned()
                    + " quests, " + lastScan.bossesScanned() + " bosses).");
            for (String file : lastScan.uncoveredQuests()) {
                ctx.logger().warning("UNCOVERED tracker-blank quest "
                        + file + " — add a row to quest-destinations.yml "
                        + "to cover it (data only, no code change).");
            }
        } catch (Throwable t) {
            ctx.logger().warning("Coverage scan failed: " + t);
        }
    }

    private void loadSettings() {
        if (ctx.moduleConfig() != null) {
            joinHint = ctx.moduleConfig().getBoolean("join-hint", true);
            deriveDialog = ctx.moduleConfig().getBoolean(
                    "derive-dialog-destinations", true);
            deriveFetch = ctx.moduleConfig().getBoolean(
                    "derive-fetch-destinations", true);
            deriveTurnIn = ctx.moduleConfig().getBoolean(
                    "derive-turn-in-destinations", true);
        }
    }

    /**
     * The player-aware destination resolution the destination
     * API serves — the ONE resolution every guidance surface
     * reads (the quest beacon through the API, /questwhere and
     * the glow guide directly), so a phase change made here
     * moves all three together.
     *
     * TURN-IN PHASE FIRST (v27, FIX-LIST B2): when the
     * player's own copy of the quest has ALL of its objectives
     * complete, the destination switches to the turn-in NPC's
     * spawn (the quest file's turnInNPC, else the giver) —
     * ahead of the table row, because a row encodes where the
     * quest's objectives happen and that place is spent once
     * no objective remains. When the turn-in NPC cannot be
     * resolved, the switch stays silent and the resolution
     * below stands (never a guessed destination).
     *
     * OBJECTIVE PHASES: table rows keep precedence whenever a
     * row exists (0.19.0 rule 1, unchanged). Only when a quest
     * has NO table row is its destination derived, and the
     * derivation keys on the quest's CURRENT incomplete
     * objective — the first incomplete objective of the
     * player's own copy, read fresh on every resolution, so a
     * derivation can never shadow another phase's channel:
     *
     * - DIALOG (0.19.0): the objective's target NPC spawn.
     * - CLASS_UNLOCK (v26, FIX-LIST B6 parity): the trainer
     *   NPC's spawn, derived exactly like DIALOG — a future
     *   quest pairing CLASS_UNLOCK with a different NPC than
     *   its dialog target no longer holes silently.
     * - FETCH_ITEM (v26, FIX-LIST B1): the objective item's
     *   source in EliteMobs' own data — an open-world
     *   dropper's spawn centroid, an instance entry for an
     *   instance-only dropper, or a treasure chest holding
     *   the item (the pure FetchDerivation policy).
     *
     * Each derivation has its own toggle; with all off, or
     * EliteMobs absent, this is exactly the pre-0.19.0
     * table-only lookup.
     */
    private QuestDestinations.Destination resolveDestination(
            String questFile, UUID uuid) {
        if (emAvailable && uuid != null && deriveTurnIn) {
            EmTurnInData.TurnInState turnIn =
                    EmTurnInData.turnInState(uuid, questFile, ctx.logger());
            if (turnIn.allComplete() && turnIn.npcFile() != null) {
                DialogDerivation.NpcSpawn spawn = EmDialogData.spawnFor(
                        turnIn.npcFile(), ctx.logger());
                QuestDestinations.Destination switched =
                        TurnInDerivation.resolve(true, true, questFile,
                                questNameFor(uuid, questFile), spawn)
                                .destination();
                if (switched != null) return switched;
            }
        }
        QuestDestinations.Destination row =
                menus == null ? null : menus.destinations().byFile(questFile);
        if (row != null) return row;
        if (!emAvailable || uuid == null
                || (!deriveDialog && !deriveFetch)) {
            return null;
        }
        EmFetchData.CurrentObjective current =
                EmFetchData.currentObjective(uuid, questFile, ctx.logger());
        switch (current.kind()) {
            case DIALOG, CLASS_UNLOCK -> {
                if (!deriveDialog || current.npcFile() == null) return null;
                DialogDerivation.NpcSpawn spawn =
                        EmDialogData.spawnFor(current.npcFile(), ctx.logger());
                if (spawn == null) return null;
                return DialogDerivation.derivedDestination(
                        questFile, questNameFor(uuid, questFile), spawn);
            }
            case FETCH_ITEM -> {
                if (!deriveFetch || current.itemFile() == null) return null;
                org.bukkit.plugin.Plugin em =
                        Bukkit.getPluginManager().getPlugin("EliteMobs");
                java.io.File emFolder = em == null ? null : em.getDataFolder();
                return FetchDerivation.resolve(
                        questFile, questNameFor(uuid, questFile),
                        current.itemFile(), current.itemName(),
                        EmFetchData.droppersFor(
                                current.itemFile(), ctx.logger()),
                        EmFetchData.chestsFor(
                                current.itemFile(), ctx.logger()),
                        world -> EmFetchData.entryFor(
                                world, emFolder, ctx.logger()))
                        .destination();
            }
            default -> {
                return null;
            }
        }
    }

    /** The carried quest's display name, for a derived row. */
    private String questNameFor(UUID uuid, String questFile) {
        for (QuestDestinations.ActiveQuest q
                : EmQuestData.read(uuid, ctx.logger())) {
            if (q.file().equals(questFile)) return q.name();
        }
        return null;
    }

    private void loadGlowSettings() {
        boolean enabled = true;
        int seconds = 90;
        int radius = 32;
        if (ctx.moduleConfig() != null) {
            org.bukkit.configuration.ConfigurationSection g =
                    ctx.moduleConfig().getConfigurationSection("glow");
            if (g != null) {
                enabled = g.getBoolean("enabled", true);
                seconds = Math.max(10, Math.min(600,
                        g.getInt("duration-seconds", 90)));
                radius = Math.max(8, Math.min(128,
                        g.getInt("search-radius", 32)));
            }
        }
        glow.configure(enabled, seconds, radius);
    }

    private boolean loadDestinations() {
        File external = new File(ctx.plugin().getDataFolder(),
                "quest-destinations.yml");
        try {
            QuestDestinations table;
            if (external.isFile()) {
                try (InputStream in = new FileInputStream(external)) {
                    table = QuestDestinations.load(new InputStreamReader(
                            in, StandardCharsets.UTF_8));
                }
                ctx.logger().info("Quest Destinations: data-folder "
                        + "quest-destinations.yml in use");
            } else {
                try (InputStream in =
                             ctx.plugin().getResource("quest-destinations.yml")) {
                    if (in == null) {
                        ctx.logger().severe("Quest Destinations: bundled "
                                + "quest-destinations.yml missing");
                        return false;
                    }
                    table = QuestDestinations.load(new InputStreamReader(
                            in, StandardCharsets.UTF_8));
                }
            }
            menus = new QuestWhereMenus(table);
            excelemfixes.api.QuestDestinationApi.register(table);
            excelemfixes.api.QuestDestinationApi.registerDialogProvider(
                    dialogProvider);
            if (table.all().isEmpty()) {
                ctx.logger().info("Quest Destinations: the table is "
                        + "empty — add rows for your tracker-blank "
                        + "quests to quest-destinations.yml in the "
                        + "plugin's data folder (the coverage scan "
                        + "lists them by filename).");
            }
            return true;
        } catch (Exception e) {
            ctx.logger().severe("Quest Destinations: table load failed: " + e);
            return false;
        }
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Quest destinations are for players.");
            return true;
        }
        if (args.length == 0) {
            ctx.renderer().openRoot(player,
                    menus.rootView(activeQuests(player), emAvailable));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("show")) {
            QuestDestinations.Destination d =
                    resolveDestination(args[1], player.getUniqueId());
            if (d == null) {
                player.sendMessage("§7That quest isn't one the EliteMobs "
                        + "tracker leaves blank.");
            } else {
                ctx.renderer().openRoot(player, menus.questView(d,
                        glow.enabled(), glow.durationSeconds()));
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("guide")) {
            guide(player, args[1]);
            return true;
        }
        if (args.length >= 5 && args[0].equalsIgnoreCase("glow")) {
            glowAt(player, args);
            return true;
        }
        player.sendMessage("§7Usage: /questwhere [guide|show <quest> | "
                + "glow <world> <x> <y> <z> [entity name]]");
        return true;
    }

    /**
     * The general glow surface any menu can call (a companion
     * drop guide's boss/place screens are the designed caller):
     *   /questwhere glow <world> <x> <y> <z> [entity name]
     * With an entity name, that entity near the point glows (a
     * marker at the point stands in when it is not there);
     * without one, the point itself gets the marker.
     */
    private void glowAt(Player player, String[] args) {
        double x, y, z;
        try {
            x = Double.parseDouble(args[2]);
            y = Double.parseDouble(args[3]);
            z = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage("§7Those coordinates are not numbers.");
            return;
        }
        String world = args[1];
        if (args.length == 5) {
            if (glow.glowMarkerAt(player, world, x, y, z, "Destination")) {
                player.sendMessage("§eThe destination is glowing for "
                        + glow.durationSeconds() + " seconds.");
            } else {
                player.sendMessage("§7The glow could not be lit — that "
                        + "world is not loaded, or glow guidance is off.");
            }
            return;
        }
        StringBuilder name = new StringBuilder();
        for (int i = 5; i < args.length; i++) {
            if (i > 5) name.append(' ');
            name.append(args[i]);
        }
        GlowGuide.Outcome outcome =
                glow.glowEntityAt(player, world, x, y, z, name.toString());
        switch (outcome.kind()) {
            case ENTITY -> player.sendMessage("§e" + outcome.name()
                    + " is glowing for " + glow.durationSeconds()
                    + " seconds — look for the glow.");
            case MARKER -> player.sendMessage("§e" + name + " is not "
                    + "there right now, so the spot is glowing for "
                    + glow.durationSeconds() + " seconds instead.");
            case WORLD_MISSING -> player.sendMessage("§7The glow could "
                    + "not be lit — that world is not loaded right now.");
            case DISABLED -> player.sendMessage("§7Glow guidance is "
                    + "turned off.");
        }
    }

    // ---------------------------------------------------------- navigation

    /** Actions: questwhere:quest:<file> | questwhere:guide:<file>. */
    private void handleAction(Player player, String action) {
        String[] parts = action.split(":", 3);
        if (parts.length < 3) return;
        switch (parts[1]) {
            case "quest" -> {
                QuestDestinations.Destination d =
                        resolveDestination(parts[2],
                                player.getUniqueId());
                if (d != null) {
                    ctx.renderer().open(player, menus.questView(d,
                            glow.enabled(), glow.durationSeconds()));
                }
            }
            case "guide" -> {
                guide(player, parts[2]);
                // Close the menu so the player sees the chat
                // directions — and the glow itself.
                player.closeInventory();
            }
            default -> { /* unknown sub-action: ignore */ }
        }
    }

    // -------------------------------------------------------------- guidance

    /**
     * The re-triggerable guidance: the destination in chat plus
     * the glow. Only for the player's OWN active quests — nobody
     * lights destinations for quests they are not on.
     */
    private void guide(Player player, String questFile) {
        QuestDestinations.Destination d =
                resolveDestination(questFile, player.getUniqueId());
        if (d == null) {
            player.sendMessage("§7That quest isn't one the EliteMobs "
                    + "tracker leaves blank.");
            return;
        }
        boolean active = activeQuests(player).stream()
                .anyMatch(q -> q.file().equals(questFile));
        if (!active) {
            player.sendMessage("§7You don't have \"" + d.questName()
                    + "\" active right now.");
            return;
        }
        player.sendMessage("§a" + d.questName() + " §7— " + d.place());
        player.sendMessage("§fTarget: §7" + d.target());
        player.sendMessage("§fEntry: §7" + d.entry() + " at §f"
                + d.coords() + " §7(" + d.worldLabel() + ")");
        for (String step : d.directions()) {
            player.sendMessage("§7" + step);
        }
        GlowGuide.Outcome outcome = glow.guide(player, d);
        switch (outcome.kind()) {
            case ENTITY -> player.sendMessage("§e" + outcome.name()
                    + " is glowing for " + glow.durationSeconds()
                    + " seconds — look for the glow.");
            case MARKER -> player.sendMessage("§eThe " + d.entry()
                    + " is glowing for " + glow.durationSeconds()
                    + " seconds — look for the glow at the coordinates "
                    + "above.");
            case WORLD_MISSING -> player.sendMessage("§7The glow could "
                    + "not be lit — that destination's world is not "
                    + "loaded right now. The directions above still "
                    + "stand.");
            case DISABLED -> player.sendMessage("§7Glow guidance is "
                    + "turned off; the directions above are the way.");
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (glow != null) glow.clearPlayer(event.getPlayer().getUniqueId());
    }

    // ------------------------------------------------------------ join hint

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!joinHint || !emAvailable) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> {
            if (!player.isOnline()) return;
            List<QuestDestinations.Destination> fixed =
                    menus.destinations().forActive(
                            activeQuests(player).stream()
                                    .map(QuestDestinations.ActiveQuest::file)
                                    .toList());
            if (fixed.isEmpty()) return;
            String what = fixed.size() == 1
                    ? "Your quest \"" + fixed.get(0).questName() + "\" has"
                    : fixed.size() + " of your quests have";
            player.sendMessage("§e[ExcelEMFixes] §f" + what
                    + " no destination in the EliteMobs tracker. "
                    + "Run §a/questwhere§f for directions.");
        }, 60L);
    }

    // ---------------------------------------------------------------- data

    private List<QuestDestinations.ActiveQuest> activeQuests(Player player) {
        return emAvailable
                ? EmQuestData.read(player.getUniqueId(), ctx.logger())
                : List.of();
    }

    /** Test/rig hooks. */
    public QuestWhereMenus menus() {
        return menus;
    }

    public GlowGuide glow() {
        return glow;
    }

    public TrackerBlankScan.Scan scan() {
        return lastScan;
    }

    public QuestDestinations destinations() {
        return menus == null ? null : menus.destinations();
    }

    public List<QuestDestinations.ActiveQuest> activeQuestsFor(UUID uuid) {
        return emAvailable
                ? EmQuestData.read(uuid, ctx.logger())
                : List.of();
    }
}
