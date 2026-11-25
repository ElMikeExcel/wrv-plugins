package excelemfixes.tracker;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.shared.GeyserForms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The progress-tracker module: /progress (alias /tracker) opens a
 * player's OWN progress record — quests, dungeons, per-boss kills,
 * combat totals, classes and skills. Denominators come from the
 * generated tracker-catalog.yml (live EliteMobs configs), totals
 * from EliteMobs' own data, per-boss/per-dungeon counts from
 * kill records (aggregated off-thread and cached).
 */
public final class TrackerModule implements FixModule, CommandExecutor {

    private ModuleContext ctx;
    private TrackerMenus menus;
    private boolean emAvailable;
    private File protectDir;
    private int javaPageSize = 45;
    private int bedrockPageSize = 40;

    private record CachedSnapshot(TrackerSnapshot snapshot, long at) {}

    private final Map<UUID, CachedSnapshot> snapshots =
            new ConcurrentHashMap<>();
    private final Map<UUID, AtomicReference<Map<String, Integer>>> killCache =
            new ConcurrentHashMap<>();
    private final Map<UUID, Long> killFingerprints = new ConcurrentHashMap<>();
    private final Map<UUID, Long> killStartedAt = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return "progress-tracker";
    }

    @Override
    public String displayName() {
        return "Progress Tracker";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        if (!loadCatalog()) {
            throw new IllegalStateException("tracker catalog failed to load");
        }
        emAvailable = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        if (!emAvailable) {
            ctx.logger().warning("Progress Tracker: EliteMobs not found — "
                    + "tracker will show its unavailable panel.");
        }
        String recordsFolder = ctx.moduleConfig() == null ? ""
                : ctx.moduleConfig().getString("kill-records-folder", "");
        protectDir = recordsFolder == null || recordsFolder.isBlank() ? null
                : new File(ctx.plugin().getDataFolder().getParentFile(),
                        recordsFolder.strip());
        ctx.renderer().registerHandler("tracker", this::handleAction);
        ctx.registerCommand("progress", List.of("tracker"),
                "/progress", this);
        ctx.logger().info("Progress Tracker ready — "
                + menus.catalog().questTotal() + " quests, "
                + menus.catalog().dungeons().size() + " dungeons, "
                + menus.catalog().places().size() + " places.");
    }

    @Override
    public void onDisable() {
        if (ctx != null) ctx.renderer().unregisterHandler("tracker");
        snapshots.clear();
        killCache.clear();
    }

    @Override
    public void reload() {
        loadSettings();
        loadCatalog();
        snapshots.clear();
        killCache.clear();
    }

    private void loadSettings() {
        if (ctx.moduleConfig() != null) {
            javaPageSize = Math.max(9, Math.min(45,
                    ctx.moduleConfig().getInt("page-size-java", 45)));
            bedrockPageSize = Math.max(10, Math.min(100,
                    ctx.moduleConfig().getInt("page-size-bedrock", 40)));
        }
    }

    private boolean loadCatalog() {
        File external = new File(ctx.plugin().getDataFolder(),
                "tracker-catalog.yml");
        try {
            TrackerCatalog catalog;
            if (external.isFile()) {
                try (InputStream in = new FileInputStream(external)) {
                    catalog = TrackerCatalog.load(new InputStreamReader(in,
                            StandardCharsets.UTF_8));
                }
                ctx.logger().info("Progress Tracker: data-folder tracker-catalog.yml in use");
            } else {
                try (InputStream in =
                             ctx.plugin().getResource("tracker-catalog.yml")) {
                    if (in == null) {
                        ctx.logger().severe("Progress Tracker: bundled tracker-catalog.yml missing");
                        return false;
                    }
                    catalog = TrackerCatalog.load(new InputStreamReader(in,
                            StandardCharsets.UTF_8));
                }
            }
            menus = new TrackerMenus(catalog);
            return true;
        } catch (Exception e) {
            ctx.logger().severe("Progress Tracker: catalog load failed: " + e);
            return false;
        }
    }

    // ------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The progress tracker is for players.");
            return true;
        }
        ctx.renderer().openRoot(player, menus.hubView(snapshot(player)));
        // Warm the kill-count cache so sub-menus open with numbers.
        kills(player);
        return true;
    }

    // ---------------------------------------------------------- navigation

    /** Actions: tracker:quests | tracker:quests-done:<page> |
     *  tracker:quests-open:<page> | tracker:dungeons |
     *  tracker:bosses[:<page>] | tracker:boss-place:<id>:<page> |
     *  tracker:combat | tracker:classes. */
    private void handleAction(Player player, String action) {
        String[] parts = action.split(":");
        if (parts.length < 2) return;
        int pageSize = pageSize(player);
        switch (parts[1]) {
            case "quests" -> ctx.renderer().open(player,
                    menus.questsView(snapshot(player)));
            case "quests-done" -> ctx.renderer().open(player,
                    menus.questListView(snapshot(player), true,
                            parseInt(parts, 2), pageSize));
            case "quests-open" -> ctx.renderer().open(player,
                    menus.questListView(snapshot(player), false,
                            parseInt(parts, 2), pageSize, guideFiles()));
            case "guide" -> fireGuide(player, parts[2]);
            case "dungeons" -> ctx.renderer().open(player,
                    menus.dungeonsView(snapshot(player), kills(player)));
            case "bosses" -> ctx.renderer().open(player,
                    menus.bossPlacesView(parseInt(parts, 2), pageSize));
            case "boss-place" -> ctx.renderer().open(player,
                    menus.bossPlaceView(parts[2], snapshot(player),
                            kills(player), parseInt(parts, 3), pageSize));
            case "combat" -> ctx.renderer().open(player,
                    menus.combatView(snapshot(player)));
            case "classes" -> ctx.renderer().open(player,
                    menus.classesView(snapshot(player)));
            default -> { /* unknown sub-action: ignore */ }
        }
    }

    // ---------------------------------------------------------- glow guide

    /** Quest files the quest-destinations module covers (empty
     *  when that module is off — then no guide buttons render). */
    private java.util.Set<String> guideFiles() {
        excelemfixes.questdest.QuestDestinationModule questModule = questModule();
        if (questModule == null) return java.util.Set.of();
        java.util.Set<String> files = new java.util.HashSet<>();
        for (excelemfixes.questdest.QuestDestinations.Destination d
                : questModule.destinations().all()) {
            files.add(d.questFile());
        }
        return files;
    }

    private excelemfixes.questdest.QuestDestinationModule questModule() {
        if (ctx.plugin() instanceof excelemfixes.ExcelEMFixes core
                && core.module("quest-destinations")
                        instanceof excelemfixes.questdest.QuestDestinationModule qm) {
            return qm;
        }
        return null;
    }

    /** The /progress "guide me" action: destination text via the
     *  quest module's own command surface, then the entry glows
     *  through the shared GlowApi. */
    private void fireGuide(Player player, String questFile) {
        excelemfixes.questdest.QuestDestinationModule questModule = questModule();
        if (questModule == null) return;
        excelemfixes.questdest.QuestDestinations.Destination dest =
                questModule.destinations().byFile(questFile);
        if (dest == null) return;
        player.closeInventory();
        player.performCommand("questwhere show " + questFile);
        if (!excelemfixes.api.GlowApi.isAvailable()) return;
        if (dest.glowIsEntity()) {
            excelemfixes.api.GlowApi.glowEntity(player, dest.world(),
                    dest.x(), dest.y(), dest.z(), dest.glowEntityName());
        } else {
            excelemfixes.api.GlowApi.glowMarker(player, dest.world(),
                    dest.x(), dest.y(), dest.z(), dest.entry());
        }
    }

    // -------------------------------------------------------------- data

    private TrackerSnapshot snapshot(Player player) {
        UUID uuid = player.getUniqueId();
        CachedSnapshot cached = snapshots.get(uuid);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.at() < 30_000) {
            return cached.snapshot();
        }
        TrackerSnapshot snap = emAvailable
                ? EmData.read(uuid, ctx.logger())
                : TrackerSnapshot.unavailable();
        snapshots.put(uuid, new CachedSnapshot(snap, now));
        return snap;
    }

    /**
     * The player's kill-record counts, or null while the
     * (off-thread) aggregation is still running — menus show a
     * "counting" line instead of blocking.
     */
    private Map<String, Integer> kills(Player player) {
        UUID uuid = player.getUniqueId();
        long fingerprint = fingerprint();
        AtomicReference<Map<String, Integer>> ref =
                killCache.computeIfAbsent(uuid, k -> new AtomicReference<>());
        Long started = killStartedAt.get(uuid);
        long startedAt = started == null ? 0L : started;
        boolean fingerprintChanged =
                killFingerprints.getOrDefault(uuid, Long.MIN_VALUE)
                        != fingerprint;
        boolean stale = started == null || fingerprintChanged;
        if (stale && (ref.get() == null
                || System.currentTimeMillis() - startedAt > 60_000)) {
            killStartedAt.put(uuid, System.currentTimeMillis());
            killFingerprints.put(uuid, fingerprint);
            ref.set(null);
            Bukkit.getScheduler().runTaskAsynchronously(ctx.plugin(), () -> {
                Map<String, Integer> result =
                        ProtectKills.aggregate(protectDir, uuid);
                ref.set(result);
            });
        }
        return ref.get();
    }

    private long fingerprint() {
        if (protectDir == null || !protectDir.isDirectory()) return -1;
        File[] files = protectDir.listFiles((dir, name) ->
                name.startsWith("entities-")
                        && (name.endsWith(".jsonl")
                        || name.endsWith(".jsonl.gz")));
        if (files == null) return -1;
        long fp = files.length;
        for (File f : files) {
            fp = fp * 31 + f.length();
            fp = fp * 31 + f.lastModified();
        }
        return fp;
    }

    private int pageSize(Player player) {
        return GeyserForms.isBedrockPlayer(player, ctx.logger())
                ? bedrockPageSize : javaPageSize;
    }

    private static int parseInt(String[] parts, int i) {
        if (i >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Test/rig hooks. */
    public TrackerMenus menus() {
        return menus;
    }

    public Map<String, Integer> aggregateKillsFor(UUID uuid) {
        return ProtectKills.aggregate(protectDir, uuid);
    }
}
