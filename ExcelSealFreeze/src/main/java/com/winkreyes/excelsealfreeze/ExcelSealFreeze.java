package com.winkreyes.excelsealfreeze;

import com.winkreyes.excelsealfreeze.core.BaselineStore;
import com.winkreyes.excelsealfreeze.core.ClampEngine;
import com.winkreyes.excelsealfreeze.core.Progression;
import com.winkreyes.excelsealfreeze.core.SealFlag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

/**
 * ExcelSealFreeze 0.1.0 - holds EliteMobs progression at the post-wipe
 * baseline until the guild's fifth seal breaks (Mike's law, Nov 14 2026).
 *
 * Design invariants:
 *  - Play is never blocked. This plugin only edits progression columns in
 *    EliteMobs' SQLite database, from async tasks, on a cadence plus
 *    join/quit. It never cancels an event and never touches the main thread
 *    with database work.
 *  - Clamp-only-down: values are restored TO the arm-time baseline, never
 *    below it; Deaths / gambling / quest state are never read or written.
 *  - The unlock is read from ExcelGuildGate's state.yml ("unlocked: true",
 *    written by GuildGate when every enabled seal is broken). The moment it
 *    reads true, the freeze disarms itself permanently.
 *  - Fail-safe: a missing/unreachable EM database, a missing baseline or an
 *    unreadable seal file only ever logs and skips the cycle (seal file
 *    unreadable = still locked). Nothing here can corrupt EM's data: clamp
 *    cycles run in a single transaction and roll back whole on any failure.
 *  - File writes fsync the FILE only, never the containing directory
 *    (directory fsync is what breaks Excel plugins on Windows - P-FSYNC-1).
 */
public final class ExcelSealFreeze extends JavaPlugin implements Listener, CommandExecutor {

    private volatile boolean armed;
    private volatile long armedAtMillis;
    private volatile Map<String, Progression.ProgRow> baseline;
    private volatile String lastCycleSummary = "no cycle yet";
    private volatile String lastError;
    private BukkitTask cycleTask;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadOwnState();
        if (getCommand("sealfreeze") != null) {
            getCommand("sealfreeze").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        if (armed) {
            if (baseline == null) {
                // Armed with no readable baseline: never clamp blind.
                armed = false;
                saveOwnState("baseline-missing-on-boot");
                getLogger().severe("ExcelSealFreeze was armed but baseline.txt is missing/unreadable. "
                        + "DISARMED rather than clamp without a baseline. Re-run /sealfreeze arm.");
            } else {
                startCycleTask();
                getLogger().info("ExcelSealFreeze resumed ARMED (baseline of "
                        + baseline.size() + " players, armed at " + armedAtMillis + ").");
            }
        }
        getLogger().info("ExcelSealFreeze enabled. Armed=" + armed
                + ", sealUnlocked=" + readSealUnlocked()
                + ", sealsBroken=" + readSealsBroken() + ".");
    }

    @Override
    public void onDisable() {
        if (cycleTask != null) cycleTask.cancel();
    }

    // ------------------------------------------------------------------
    // Persistence (own files only)
    // ------------------------------------------------------------------

    private File baselineFile() {
        return new File(getDataFolder(), "baseline.txt");
    }

    private File ownStateFile() {
        return new File(getDataFolder(), "state.yml");
    }

    /** Write text and fsync the FILE (never the directory - see class doc). */
    private static void writeFileSynced(File file, String text) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try (FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            ch.force(false);
        }
    }

    private void loadOwnState() {
        File f = ownStateFile();
        if (!f.exists()) {
            armed = false;
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        armed = y.getBoolean("armed", false);
        armedAtMillis = y.getLong("armedAt", 0L);
        if (armed && baselineFile().exists()) {
            try {
                baseline = BaselineStore.parse(Files.readString(baselineFile().toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                getLogger().warning("Could not read baseline.txt: " + e.getMessage());
                baseline = null;
            }
        }
    }

    private void saveOwnState(String disarmReason) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("armed", armed);
        y.set("armedAt", armedAtMillis);
        if (disarmReason != null) y.set("lastDisarmReason", disarmReason);
        try {
            writeFileSynced(ownStateFile(), y.saveToString());
        } catch (IOException e) {
            getLogger().warning("Could not save state.yml: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Paths and connections
    // ------------------------------------------------------------------

    private File pluginsDir() {
        return getDataFolder().getParentFile();
    }

    private File emDbFile() {
        String rel = getConfig().getString("em-db-path", "EliteMobs/data/player_data.db");
        return new File(pluginsDir(), rel);
    }

    private File sealStateFile() {
        String rel = getConfig().getString("seal-state-path", "ExcelGuildGate/state.yml");
        return new File(pluginsDir(), rel);
    }

    private Connection openEmDb() throws Exception {
        File db = emDbFile();
        if (!db.exists()) throw new IOException("EM database not found at " + db.getPath());
        Class.forName("org.sqlite.JDBC");
        Properties props = new Properties();
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath(), props);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA busy_timeout = " + Math.max(250, getConfig().getInt("busy-timeout-ms", 2000)));
        }
        return c;
    }

    private boolean readSealUnlocked() {
        try {
            File f = sealStateFile();
            if (!f.exists()) return false;
            return SealFlag.isUnlocked(Files.readString(f.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Unreadable seal file = still locked (safe default for the flag).
            return false;
        }
    }

    private int readSealsBroken() {
        try {
            File f = sealStateFile();
            if (!f.exists()) return -1;
            return SealFlag.brokenCount(Files.readString(f.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return -1;
        }
    }

    // ------------------------------------------------------------------
    // Clamp cycles (always async, always fail-safe)
    // ------------------------------------------------------------------

    private void startCycleTask() {
        if (cycleTask != null) return;
        long periodTicks = Math.max(20L, getConfig().getLong("clamp-interval-seconds", 60L) * 20L);
        cycleTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this::runCycle, 100L, periodTicks);
    }

    private void stopCycleTask() {
        if (cycleTask != null) {
            cycleTask.cancel();
            cycleTask = null;
        }
    }

    /** One full clamp cycle. Never throws. */
    private void runCycle() {
        if (!armed) return;
        try {
            if (readSealUnlocked()) {
                disarmInternal("seal-5-broken");
                getLogger().info("ExcelSealFreeze: GuildGate state reads UNLOCKED - the fifth seal has broken. "
                        + "Freeze lifted automatically; EM progression accrues normally from here.");
                return;
            }
            Map<String, Progression.ProgRow> base = baseline;
            if (base == null) {
                lastError = "baseline missing during cycle";
                getLogger().warning("Clamp cycle skipped: baseline not loaded.");
                return;
            }
            try (Connection c = openEmDb()) {
                ClampEngine.Report report = ClampEngine.clampAll(c, base);
                lastCycleSummary = report + " at " + System.currentTimeMillis();
                lastError = null;
                if (report.rowsClamped > 0) {
                    getLogger().info("Clamp cycle: " + report + " (gains returned to the freeze baseline).");
                }
            }
        } catch (Exception e) {
            // Fail-safe: log and skip. EM's database is never left half-written
            // (ClampEngine rolls its transaction back) and the server is untouched.
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            getLogger().warning("Clamp cycle skipped (EM database unreachable or busy): " + lastError);
        }
    }

    /** Single-player clamp used on join/quit. Never throws. */
    private void runSingleClamp(String uuid) {
        if (!armed) return;
        try {
            if (readSealUnlocked()) {
                runCycle();
                return;
            }
            Map<String, Progression.ProgRow> base = baseline;
            if (base == null) return;
            try (Connection c = openEmDb()) {
                ClampEngine.clampOne(c, base, uuid);
            }
        } catch (Exception e) {
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            getLogger().warning("Single clamp skipped for " + uuid + ": " + lastError);
        }
    }

    private void disarmInternal(String reason) {
        armed = false;
        stopCycleTask();
        saveOwnState(reason);
    }

    // ------------------------------------------------------------------
    // Join / quit enforcement (delayed so EM's own load/save settles first)
    // ------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!armed) return;
        String uuid = event.getPlayer().getUniqueId().toString();
        getServer().getScheduler().runTaskLaterAsynchronously(this, () -> runSingleClamp(uuid), 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!armed) return;
        String uuid = event.getPlayer().getUniqueId().toString();
        getServer().getScheduler().runTaskLaterAsynchronously(this, () -> runSingleClamp(uuid), 60L);
    }

    // ------------------------------------------------------------------
    // Command
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "status" -> {
                sender.sendMessage("ExcelSealFreeze status:");
                sender.sendMessage("  armed: " + armed + (armed ? " (since " + armedAtMillis + ")" : ""));
                sender.sendMessage("  seal unlocked (GuildGate state): " + readSealUnlocked()
                        + ", seals broken: " + readSealsBroken());
                sender.sendMessage("  baseline players: " + (baseline == null ? "none loaded" : baseline.size()));
                sender.sendMessage("  last cycle: " + lastCycleSummary);
                sender.sendMessage("  last error: " + (lastError == null ? "none" : lastError));
            }
            case "arm" -> {
                if (armed) {
                    sender.sendMessage("Already armed. Disarm first if you need a fresh baseline.");
                    return true;
                }
                if (readSealUnlocked()) {
                    sender.sendMessage("GuildGate already reads UNLOCKED - the fifth seal has broken; "
                            + "there is nothing left to freeze.");
                    return true;
                }
                sender.sendMessage("Arming: snapshotting the EM progression baseline...");
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    try (Connection c = openEmDb()) {
                        Map<String, Progression.ProgRow> snapshot = ClampEngine.readAll(c);
                        long now = System.currentTimeMillis();
                        writeFileSynced(baselineFile(), BaselineStore.serialize(snapshot, now));
                        baseline = snapshot;
                        armedAtMillis = now;
                        armed = true;
                        saveOwnState(null);
                        startCycleTask();
                        getLogger().info("ExcelSealFreeze ARMED by " + sender.getName()
                                + ": baseline snapshot of " + snapshot.size() + " players written.");
                        sender.sendMessage("ExcelSealFreeze ARMED - baseline of " + snapshot.size()
                                + " players; progression is frozen until the fifth seal breaks.");
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Arm failed - NOT armed.", e);
                        sender.sendMessage("Arm FAILED (not armed): " + e.getMessage());
                    }
                });
            }
            case "disarm" -> {
                if (!armed) {
                    sender.sendMessage("Not armed.");
                    return true;
                }
                disarmInternal("manual-by-" + sender.getName());
                getLogger().info("ExcelSealFreeze DISARMED manually by " + sender.getName() + ".");
                sender.sendMessage("ExcelSealFreeze disarmed. EM progression accrues normally.");
            }
            case "clampnow" -> {
                if (!armed) {
                    sender.sendMessage("Not armed.");
                    return true;
                }
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    runCycle();
                    sender.sendMessage("Clamp cycle run. Last cycle: " + lastCycleSummary
                            + (lastError == null ? "" : " | error: " + lastError));
                });
            }
            default -> sender.sendMessage("Usage: /sealfreeze [status|arm|disarm|clampnow]");
        }
        return true;
    }
}
