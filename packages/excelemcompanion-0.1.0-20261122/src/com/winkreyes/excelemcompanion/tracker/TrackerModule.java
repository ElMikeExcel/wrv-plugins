package com.winkreyes.excelemcompanion.tracker;

import com.winkreyes.excelemcompanion.CompanionModule;
import com.winkreyes.excelemcompanion.ModuleContext;
import com.winkreyes.excelemcompanion.shared.GeyserForms;
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
 * ExcelProtect records (aggregated off-thread and cached).
 */
public final class TrackerModule implements CompanionModule, CommandExecutor {

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
        protectDir = new File(ctx.plugin().getDataFolder().getParentFile(),
                "ExcelProtect");
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
                            parseInt(parts, 2), pageSize));
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
     * The player's ExcelProtect kill counts, or null while the
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
