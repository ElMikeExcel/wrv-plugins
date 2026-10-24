package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.AdjustmentLedger;
import com.winkreyes.leaderboard.core.BoardMath;
import com.winkreyes.leaderboard.core.VanillaStatsCache;
import com.winkreyes.leaderboard.paper.BedrockBoardForms;
import com.winkreyes.leaderboard.paper.BoardMenu;
import com.winkreyes.leaderboard.paper.Category;
import com.winkreyes.leaderboard.paper.ChatBoardMenu;
import com.winkreyes.leaderboard.paper.SoftAdapters;
import com.winkreyes.leaderboard.paper.StatsCollector;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExcelLeaderboard extends JavaPlugin {

    /** Immutable view of one refresh: ranked entries per available
     * category, plus per-player detail suffixes for boards whose rows
     * show a second number (Land Claimed's "876 left"). */
    public record Snapshot(Map<Category, List<BoardMath.Entry>> boards,
                           Map<Category, Map<UUID, String>> details) {
        public Snapshot(Map<Category, List<BoardMath.Entry>> boards) {
            this(boards, Map.of());
        }

        public Map<UUID, String> detailsFor(Category category) {
            return details.getOrDefault(category, Map.of());
        }
    }

    private volatile Snapshot snapshot = new Snapshot(Map.of());
    private SoftAdapters adapters;
    private AdjustmentLedger ledger = new AdjustmentLedger();
    private BoardMenu menu;
    private BedrockBoardForms forms;
    private ChatBoardMenu chatMenu;
    /** 1.3.1: offline players' vanilla totals, one computation per stats-file version. */
    private final VanillaStatsCache statsCache = new VanillaStatsCache();
    /** 1.3.1: the primary world's player stats folder, resolved once on enable
     * (main thread). Null if it cannot be found: then nothing is cached and
     * the boards behave exactly as 1.3.0. */
    private volatile Path statsDir;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        adapters = new SoftAdapters(getLogger());
        statsDir = locateStatsDir();
        loadLedger();
        menu = new BoardMenu(this);
        forms = new BedrockBoardForms(this);
        chatMenu = new ChatBoardMenu(this);
        getServer().getPluginManager().registerEvents(menu, this);
        long periodTicks = Math.max(20L, getConfig().getLong("cache-seconds", 60) * 20L);
        // First refresh shortly after enable so late-loading soft dependencies
        // are already present when their probes run.
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::refresh, 60L, periodTicks);
        getLogger().info("ExcelLeaderboard enabled; first board refresh in 3 seconds.");
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public BoardMenu menu() {
        return menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("The leaderboard is a player menu; run /leaderboard in game.");
            return true;
        }
        if (!player.hasPermission("excelleaderboard.use")) {
            player.sendMessage("You do not have permission to view the leaderboard.");
            return true;
        }
        // "/leaderboard board <key>" is what the chat menu's lines click;
        // it always prints the text board view. An unknown key prints the
        // main panel again.
        if (args.length >= 1 && args[0].equalsIgnoreCase("board")) {
            Category category = args.length >= 2 ? parseCategory(args[1]) : null;
            if (category == null) {
                chatMenu.openMain(player);
            } else {
                chatMenu.openBoard(player, category);
            }
            return true;
        }
        // Main routing: a detected Bedrock player gets the native form
        // when bedrock-forms is on; if the form cannot be sent, the chest
        // menu is the fallback. Everyone else gets the chat menu.
        if (getConfig().getBoolean("bedrock-forms", true) && forms.isBedrock(player)) {
            if (!forms.openMain(player)) {
                menu.openMain(player);
            }
            return true;
        }
        chatMenu.openMain(player);
        return true;
    }

    /** Category key from the chat menu: the enum name in lowercase
     * (miner, mcmmo, jobs, ...); the id string is accepted too. */
    private static Category parseCategory(String key) {
        if (key == null) {
            return null;
        }
        try {
            return Category.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException notAnEnumName) {
            return Category.byId(key.trim().toLowerCase(Locale.ROOT));
        }
    }

    // ---- Refresh --------------------------------------------------------

    private void refresh() {
        List<OfflinePlayer> players = new ArrayList<>();
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null) {
                players.add(offline);
            }
        }

        Map<UUID, Long> minedByPlayer = new HashMap<>();
        Map<Category, List<BoardMath.Entry>> raw = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            raw.put(category, new ArrayList<>());
        }

        boolean vault = adapters.vaultAvailable();
        boolean jobs = adapters.jobsAvailable();
        boolean elite = adapters.eliteAvailable() && !eliteMobsOnVault();
        boolean claims = adapters.claimsAvailable();
        Map<UUID, String> claimDetails = new HashMap<>();

        for (OfflinePlayer player : players) {
            UUID id = player.getUniqueId();
            String name = player.getName();
            VanillaStatsCache.Totals vt = statsCache.get(id, statsStamp(player),
                    () -> new VanillaStatsCache.Totals(
                            StatsCollector.minedTotal(player),
                            StatsCollector.placedTotal(player, StatsCollector::isBlockItem),
                            StatsCollector.playtimeTicks(player),
                            StatsCollector.mobKills(player),
                            StatsCollector.distanceTotalCm(player),
                            StatsCollector.deaths(player)));
            long mined = vt.mined();
            minedByPlayer.put(id, mined);
            raw.get(Category.MINER).add(new BoardMath.Entry(id, name, mined));
            raw.get(Category.BUILDER).add(new BoardMath.Entry(id, name, vt.placed()));
            raw.get(Category.PLAYTIME).add(new BoardMath.Entry(id, name, vt.playtimeTicks()));
            raw.get(Category.SLAYER).add(new BoardMath.Entry(id, name, vt.mobKills()));
            raw.get(Category.TRAVELER).add(new BoardMath.Entry(id, name, vt.distanceCm()));
            raw.get(Category.DEATHS).add(new BoardMath.Entry(id, name, vt.deaths()));
            if (vault) {
                Double balance = adapters.balance(player);
                if (balance != null) {
                    raw.get(Category.RICHEST).add(new BoardMath.Entry(id, name, Math.round(balance)));
                }
            }
            Long power = adapters.mcMMOPower(player);
            if (power != null) {
                raw.get(Category.MCMMO).add(new BoardMath.Entry(id, name, power));
            }
            if (jobs) {
                Long level = adapters.jobsTotalLevel(player);
                if (level != null) {
                    raw.get(Category.JOBS).add(new BoardMath.Entry(id, name, level));
                }
            }
            if (elite) {
                Long coins = adapters.eliteCoins(player);
                if (coins != null) {
                    raw.get(Category.ELITE).add(new BoardMath.Entry(id, name, coins));
                }
            }
            if (claims) {
                SoftAdapters.RawClaims rawClaims = adapters.claimData(player);
                if (rawClaims != null) {
                    com.winkreyes.leaderboard.core.ClaimBlocksMath.Totals totals =
                            com.winkreyes.leaderboard.core.ClaimBlocksMath.summarize(
                                    rawClaims.accrued(), rawClaims.bonus(), rawClaims.claims());
                    raw.get(Category.CLAIMS).add(new BoardMath.Entry(id, name, totals.claimed()));
                    claimDetails.put(id, BoardMath.formatCount(totals.available()) + " left");
                }
            }
        }

        watchBuildModeSessions(minedByPlayer);

        // Miner applies the Build Mode adjustment; every board is ranked.
        Map<Category, List<BoardMath.Entry>> boards = new EnumMap<>(Category.class);
        for (Map.Entry<Category, List<BoardMath.Entry>> e : raw.entrySet()) {
            List<BoardMath.Entry> entries = e.getValue();
            if (e.getKey() == Category.MINER) {
                List<BoardMath.Entry> adjusted = new ArrayList<>(entries.size());
                for (BoardMath.Entry entry : entries) {
                    adjusted.add(new BoardMath.Entry(entry.player(), entry.name(),
                            BoardMath.adjustedMined(entry.value(), ledger.adjustmentFor(entry.player()))));
                }
                entries = adjusted;
            }
            if (!entries.isEmpty()) {
                boards.put(e.getKey(), BoardMath.rank(entries));
            }
        }
        Map<Category, Map<UUID, String>> details = claimDetails.isEmpty()
                ? Map.of()
                : Map.of(Category.CLAIMS, Map.copyOf(claimDetails));
        snapshot = new Snapshot(Map.copyOf(boards), details);
    }

    /** 1.3.1: cache stamp for one player. Online players are never cached
     * (live stats, in memory). Offline: the stats file's mtime and size. */
    private long statsStamp(OfflinePlayer player) {
        Path dir = statsDir;
        if (dir == null || player.isOnline()) {
            return -1L;
        }
        try {
            Path file = dir.resolve(player.getUniqueId() + ".json");
            if (!Files.isRegularFile(file)) {
                return -1L;
            }
            return VanillaStatsCache.stamp(Files.getLastModifiedTime(file).toMillis(), Files.size(file));
        } catch (IOException | RuntimeException e) {
            return -1L;
        }
    }

    /** Stats live in <world>/players/stats on 26.x and <world>/stats before. */
    private Path locateStatsDir() {
        try {
            if (Bukkit.getWorlds().isEmpty()) {
                return null;
            }
            // Paper 26.x: the world folder is world/dimensions/minecraft/overworld,
            // stats live in world/players/stats (see VanillaStatsCache.findStatsDir).
            Path found = VanillaStatsCache.findStatsDir(Bukkit.getWorlds().get(0).getWorldFolder().toPath());
            if (found != null) {
                getLogger().info("ExcelLeaderboard: offline stats cache reads " + found);
                return found;
            }
        } catch (RuntimeException e) {
            getLogger().fine("Stats folder lookup failed: " + e.getMessage());
        }
        getLogger().info("ExcelLeaderboard: stats folder not found; offline stats are read uncached (1.3.0 behavior).");
        return null;
    }

    /** One-currency rule: when EliteMobs runs on Vault (Mike's decision,
     * Oct 14, 2026), its balance IS the Vault balance and Richest already
     * ranks it, so the Elite Coins board hides itself. */
    private boolean eliteMobsVaultModeLogged = false;

    private boolean eliteMobsOnVault() {
        try {
            Path emSettings = getDataFolder().toPath().getParent()
                    .resolve("EliteMobs").resolve("EconomySettings.yml");
            if (Files.isRegularFile(emSettings)
                    && com.winkreyes.leaderboard.core.VaultModeDetector.useVaultEnabled(
                            Files.readAllLines(emSettings))) {
                if (!eliteMobsVaultModeLogged) {
                    eliteMobsVaultModeLogged = true;
                    getLogger().info("Elite Coins board hidden: EliteMobs runs on Vault,"
                            + " so Richest already covers that balance.");
                }
                return true;
            }
        } catch (Exception e) {
            getLogger().fine("EliteMobs Vault-mode check failed: " + e.getMessage());
        }
        return false;
    }

    // ---- Build Mode session watch ----------------------------------------

    private void watchBuildModeSessions(Map<UUID, Long> minedByPlayer) {
        Path indexDir = getDataFolder().toPath().getParent()
                .resolve("ExcelBuildMode").resolve("active-index");
        if (!Files.isDirectory(indexDir)) {
            return;
        }
        Set<UUID> active = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(indexDir, "*.idx")) {
            for (Path file : stream) {
                try {
                    active.add(UUID.fromString(file.getFileName().toString().replaceFirst("\\.idx$", "")));
                } catch (IllegalArgumentException ignored) {
                    // Not a session file; skip.
                }
            }
        } catch (IOException e) {
            return;
        }
        boolean changed = false;
        for (UUID id : active) {
            Long mined = minedByPlayer.get(id);
            if (mined != null && !ledger.inSession(id)) {
                ledger.noteSessionStart(id, mined);
            }
        }
        for (UUID id : List.copyOf(minedByPlayer.keySet())) {
            if (!active.contains(id) && ledger.inSession(id)) {
                ledger.noteSessionEnd(id, minedByPlayer.get(id));
                changed = true;
            }
        }
        if (changed) {
            saveLedger();
        }
    }

    private Path ledgerFile() {
        return getDataFolder().toPath().resolve("adjustments.txt");
    }

    private void loadLedger() {
        try {
            if (Files.isRegularFile(ledgerFile())) {
                ledger = AdjustmentLedger.deserialize(Files.readString(ledgerFile()));
                getLogger().info("ExcelLeaderboard: loaded Build Mode adjustments for "
                        + ledger.adjustmentsSnapshot().size() + " player(s).");
            }
        } catch (IOException e) {
            getLogger().warning("ExcelLeaderboard: could not read the adjustment ledger: " + e.getMessage());
        }
    }

    private void saveLedger() {
        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.writeString(ledgerFile(), ledger.serialize());
        } catch (IOException e) {
            getLogger().warning("ExcelLeaderboard: could not save the adjustment ledger: " + e.getMessage());
        }
    }
}
