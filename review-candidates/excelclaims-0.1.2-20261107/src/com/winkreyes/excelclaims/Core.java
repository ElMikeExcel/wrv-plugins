package com.winkreyes.excelclaims;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelClaims — the engine hub. The facade main class
 * (me.ryanhamshire.GriefPrevention.GriefPrevention) owns the Bukkit
 * lifecycle and delegates everything here; the facade API classes
 * reach the engine through this one object.
 * Excel — Iris / Mike.
 */
public final class Core {

    private final GriefPrevention plugin;
    private final Logger log;
    private final ClaimsConfig config;
    private ClaimStore store;
    private ClaimService service;
    private Visualizer visualizer;
    private ProtectGateway protectSink;

    /** Players (staff) currently ignoring claim protection. */
    public final Set<UUID> ignoringClaims = new HashSet<>();
    /** Shovel modes per player: "subdivision" / "admin" (absent = basic). */
    public final Map<UUID, String> shovelModes = new HashMap<>();
    /** First shovel corner per player, plus the claim being resized (id, or -1). */
    public static final class ShovelState {
        public String world;
        public int x, y, z;
        public long resizingClaimId = -1;
    }
    public final Map<UUID, ShovelState> shovelFirstCorner = new HashMap<>();
    /** Combat tags: player -> epoch millis when the tag expires. */
    public final Map<UUID, Long> combatUntil = new HashMap<>();
    /** Pending /trapped escapes: player -> [world, x, y, z, deadlineMillis]. */
    public final Map<UUID, Object[]> trappedPending = new HashMap<>();

    public Core(GriefPrevention plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.config = new ClaimsConfig();
    }

    private Core(java.util.logging.Logger log, ClaimStore store) {
        this.plugin = null;
        this.log = log;
        this.store = store;
        // One config object shared by engine and store, as in enable().
        this.config = store.config();
        this.service = new ClaimService(this);
    }

    /**
     * Headless test seam: an engine over an already-loaded store, with
     * no Bukkit lifecycle (no listeners, tasks, or data folder). Only
     * the unit tests use this; the live plugin always goes through
     * the JavaPlugin constructor + enable().
     */
    public static Core forTests(java.util.logging.Logger log, ClaimStore store) {
        return new Core(log, store);
    }

    public void enable() {
        plugin.saveDefaultConfig();
        config.load(plugin.getConfig());
        store = new ClaimStore(log, config);
        try {
            store.load(plugin.getDataFolder());
        } catch (IOException e) {
            log.severe("ExcelClaims: could not load the claim store (" + e + ") — claims are NOT protecting until this is fixed");
        }
        service = new ClaimService(this);
        visualizer = new Visualizer(plugin);
        if (config.publishToProtect
                && Bukkit.getPluginManager().getPlugin("ExcelProtect") != null
                && Bukkit.getPluginManager().isPluginEnabled("ExcelProtect")) {
            try {
                protectSink = new ProtectGateway(log);
            } catch (Throwable t) {
                log.warning("ExcelClaims: ExcelProtect present but the record sink did not bind (" + t + ") — continuing without it");
                protectSink = null;
            }
        }
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), plugin);
        Bukkit.getPluginManager().registerEvents(new ToolListener(this), plugin);
        Commands commands = new Commands(this);
        for (String name : Commands.NAMES) {
            if (plugin.getCommand(name) != null) plugin.getCommand(name).setExecutor(commands);
        }
        // Accrual: one pass a minute; each online player earns
        // accruedPerHour/60 per minute (config idle rules applied).
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) accrueMinute(p);
            service.runExpirationIfDue();
        }, 1200L, 1200L);
        log.info("ExcelClaims 0.1.2 enabled: " + store.claimCount() + " claims, "
                + store.allPlayers().size() + " player accounts, build " + BuildInfo.BUILD_ID);
    }

    public void disable() {
        for (PlayerRecord pr : store.allPlayers()) store.savePlayer(pr);
        visualizer.clearAll();
    }

    public void reload() {
        plugin.reloadConfig();
        config.load(plugin.getConfig());
        log.info("ExcelClaims: config reloaded (" + store.claimCount() + " claims held)");
    }

    private void accrueMinute(Player p) {
        PlayerRecord pr = store.playerData(p.getUniqueId());
        pr.lastSeenAt = System.currentTimeMillis();
        int cap = pr.accruedLimitOverride >= 0 ? pr.accruedLimitOverride : config.maxAccrued;
        if (pr.accrued >= cap) return;
        Accrual.accrueMinute(pr, config.accruedPerHour, cap);
        store.savePlayer(pr);
    }

    // ------------------------------------------------------------ accessors

    public GriefPrevention plugin() { return plugin; }
    public Logger log() { return log; }
    public ClaimsConfig config() { return config; }
    public ClaimStore store() { return store; }
    public ClaimService service() { return service; }
    public Visualizer visualizer() { return visualizer; }
    public ProtectGateway protectSink() { return protectSink; }
    public File dataFolder() { return plugin.getDataFolder(); }

    /** The one facade instance for a record (cached on the record itself). */
    public Claim facadeFor(ClaimRecord r) {
        if (r == null) return null;
        if (r.facade == null) r.facade = new Claim(r, this);
        return r.facade;
    }

    public String nameOf(UUID id) {
        if (id == null) return "an administrator";
        try {
            if (Bukkit.getServer() == null) return id.toString();
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String n = op.getName();
            return n != null ? n : id.toString();
        } catch (Throwable t) {
            return id.toString();
        }
    }

    /** Resolve a trust-list entry (UUID string or legacy name) to a UUID, or null. */
    public UUID resolveEntry(String entry) {
        if (entry == null) return null;
        try { return UUID.fromString(entry); } catch (IllegalArgumentException ignored) { }
        try {
            if (Bukkit.getServer() == null) return null;
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry);
            return op.hasPlayedBefore() || op.isOnline() ? op.getUniqueId() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean isIgnoring(Player p) {
        return p != null && (ignoringClaims.contains(p.getUniqueId())
                || p.hasPermission("griefprevention.adminclaims") && ignoringClaims.contains(p.getUniqueId()));
    }

    /** Staff override for checks inside admin claims (and subdivisions of them). */
    public boolean adminOverride(Player p, ClaimRecord claim) {
        if (p == null || claim == null) return false;
        ClaimRecord top = store.topLevel(claim);
        return top != null && top.isAdmin() && p.hasPermission("griefprevention.adminclaims");
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
