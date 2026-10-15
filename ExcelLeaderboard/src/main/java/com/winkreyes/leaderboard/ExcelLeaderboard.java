package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.AdjustmentLedger;
import com.winkreyes.leaderboard.core.BoardMath;
import com.winkreyes.leaderboard.paper.BoardMenu;
import com.winkreyes.leaderboard.paper.Category;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ExcelLeaderboard extends JavaPlugin {

    /** Immutable view of one refresh: ranked entries per available category. */
    public record Snapshot(Map<Category, List<BoardMath.Entry>> boards) {}

    private volatile Snapshot snapshot = new Snapshot(Map.of());
    private SoftAdapters adapters;
    private AdjustmentLedger ledger = new AdjustmentLedger();
    private BoardMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        adapters = new SoftAdapters(getLogger());
        loadLedger();
        menu = new BoardMenu(this);
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
        menu.openMain(player);
        return true;
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

        for (OfflinePlayer player : players) {
            UUID id = player.getUniqueId();
            String name = player.getName();
            long mined = StatsCollector.minedTotal(player);
            minedByPlayer.put(id, mined);
            raw.get(Category.MINER).add(new BoardMath.Entry(id, name, mined));
            raw.get(Category.BUILDER).add(new BoardMath.Entry(id, name,
                    StatsCollector.placedTotal(player, StatsCollector::isBlockItem)));
            raw.get(Category.PLAYTIME).add(new BoardMath.Entry(id, name, StatsCollector.playtimeTicks(player)));
            raw.get(Category.SLAYER).add(new BoardMath.Entry(id, name, StatsCollector.mobKills(player)));
            raw.get(Category.TRAVELER).add(new BoardMath.Entry(id, name, StatsCollector.distanceTotalCm(player)));
            raw.get(Category.DEATHS).add(new BoardMath.Entry(id, name, StatsCollector.deaths(player)));
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
        snapshot = new Snapshot(Map.copyOf(boards));
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
