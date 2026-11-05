package com.winkreyes.exceleliteladder;

import com.winkreyes.exceleliteladder.core.EliteSpeedSettings;
import com.winkreyes.exceleliteladder.core.FileApplier;
import com.winkreyes.exceleliteladder.core.LadderConfig;
import com.winkreyes.exceleliteladder.core.LadderConfigException;
import com.winkreyes.exceleliteladder.core.LadderException;
import com.winkreyes.exceleliteladder.core.LadderStep;
import com.winkreyes.exceleliteladder.core.LadderTable;
import com.winkreyes.exceleliteladder.core.MobCombatFile;
import com.winkreyes.exceleliteladder.core.StepPlan;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelEliteLadder: applies the Elite Seal Ladder to EliteMobs' MobCombatSettings.yml when
 * ExcelGuildGate seals break. Up only. Waits for an empty server, then dated .bak, write,
 * verify (restore on failure) and `em reload`.
 */
public class ExcelEliteLadder extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    static final String TEST_FAULT_PROPERTY = "exceleliteladder.rigTestFault";

    private int appliedStep;
    private int lastKnownSeals = -1;
    private String lastResult = "none yet";
    private String lastApplyAt = "never";
    private String lastBackup = "none";
    private boolean failedLatch = false;
    private boolean applying = false;
    private String pendingNote = "none";
    private Method sealsMethod;
    private final Set<String> loggedOnce = new HashSet<>();

    // The active ladder table (0.2.0): loaded from ladder.yml under strict
    // validation; the built-in default table until/unless a config table loads.
    // volatile: swapped wholesale by /eliteladder reload, read by the watch tick.
    private volatile LadderTable table = LadderTable.builtin();
    private volatile String tableSource = "built-in default";
    private volatile String tableLoadNote = "not loaded yet";
    private volatile String lastLadderDefect = null;

    // Elite speed buff (0.2.1): settings from config.yml's elite-speed section,
    // re-read at enable and on /eliteladder reload. The listener itself is
    // registered only when EliteMobs is enabled (softdepend class safety).
    private volatile EliteSpeedSettings speedSettings = EliteSpeedSettings.defaults();
    private boolean speedListenerActive = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSpeedSettings();
        loadLadder(true);
        loadState();
        String fault = System.getProperty(TEST_FAULT_PROPERTY);
        if (fault != null) {
            getLogger().warning("RIG TEST FAULT ACTIVE (" + TEST_FAULT_PROPERTY + "=" + fault + "). Never set this on a real server.");
        }
        getServer().getPluginManager().registerEvents(this, this);
        Plugin emForSpeed = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (emForSpeed != null && emForSpeed.isEnabled()) {
            getServer().getPluginManager().registerEvents(new EliteSpeedListener(this, () -> speedSettings), this);
            speedListenerActive = true;
        }
        if (getCommand("eliteladder") != null) {
            getCommand("eliteladder").setExecutor(this);
            getCommand("eliteladder").setTabCompleter(this);
        }
        long period = Math.max(5, getConfig().getInt("poll-seconds", 30)) * 20L;
        getServer().getScheduler().runTaskTimer(this, this::evaluate, 100L, period);
        getLogger().info("ExcelEliteLadder enabled: applied step " + stepText(appliedStep)
                + ", ladder table: " + tableSource
                + ", watching ExcelGuildGate seals every " + getConfig().getInt("poll-seconds", 30) + "s"
                + ", reload with players online " + (allowWithPlayers() ? "ALLOWED" : "off (waits for an empty server)")
                + ", elite speed: " + speedSettings.bootText()
                + (speedSettings.enabled() && !speedListenerActive ? " (listener inactive: EliteMobs not enabled)" : "")
                + (getConfig().getBoolean("enabled", true) ? "" : " (master switch OFF)"));
    }

    // ---------- elite speed (config.yml elite-speed section, 0.2.1) ----------

    /**
     * Reads the elite-speed settings from config.yml. Leaf reads (not a
     * section read) so keys missing from an older live config.yml still fall
     * back to the bundled defaults; junk values fall back too, each named in
     * a warning — the house posture for the plugin's own config file.
     */
    private void loadSpeedSettings() {
        List<String> warnings = new ArrayList<>();
        speedSettings = EliteSpeedSettings.parse(
                getConfig().get("elite-speed.enabled"),
                getConfig().get("elite-speed.amplifier"),
                getConfig().get("elite-speed.duration-ticks"),
                warnings);
        for (String w : warnings) {
            getLogger().warning("Config: " + w);
        }
    }

    @Override
    public void onDisable() {
        saveState();
    }

    private boolean allowWithPlayers() {
        return getConfig().getBoolean("allow-reload-with-players-online", false);
    }

    private static String stepText(int n) {
        return n < 1 ? "none (no verified baseline)" : "S" + n;
    }

    // ---------- ladder table (ladder.yml, 0.2.0) ----------

    private File ladderFile() {
        return new File(getDataFolder(), "ladder.yml");
    }

    /**
     * Loads (or reloads) the ladder table from ladder.yml under LadderConfig's
     * strict validation. Discipline (Mike's 2.0 trade, Nov 5, 2026): ANY defect
     * refuses the new table whole — on first boot the built-in default table
     * (the shipped values; since 0.2.1, the Nov 5, 2026 retune) runs instead;
     * on reload the last-good table keeps running. A missing file is generated
     * from the bundled default, so a fresh install needs no manual placement.
     */
    private void loadLadder(boolean initial) {
        String when = initial ? "at boot" : "on reload";
        File f = ladderFile();
        boolean generated = false;
        if (!f.exists()) {
            try {
                saveResource("ladder.yml", false);
                generated = true;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                refuseLadder(when, initial, "ladder.yml is missing and the bundled default could not be generated (" + ex.getMessage() + ")");
                return;
            }
        }
        if (!f.exists()) {
            refuseLadder(when, initial, "ladder.yml is missing and the bundled default could not be generated");
            return;
        }
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.load(f);
            Map<String, Object> root = new LinkedHashMap<>();
            ConfigurationSection sec = y.getConfigurationSection(LadderConfig.SECTION);
            root.put(LadderConfig.SECTION, sec == null ? null : sectionToMap(sec));
            LadderTable parsed = LadderConfig.parseRoot(root);
            table = parsed;
            tableSource = "ladder.yml (config)" + (generated ? " — default generated on first boot" : "");
            lastLadderDefect = null;
            tableLoadNote = (initial ? "loaded from ladder.yml at boot" : "reloaded from ladder.yml (accepted)")
                    + (generated ? "; the file did not exist and was generated from the bundled default" : "");
            if (!initial) {
                getLogger().info("Ladder table reloaded from ladder.yml and is now live ("
                        + table.step(LadderTable.MAX_STEP).label() + ": " + table.step(LadderTable.MAX_STEP).summary() + ").");
            }
        } catch (LadderConfigException ex) {
            refuseLadder(when, initial, ex.getMessage());
        } catch (InvalidConfigurationException ex) {
            refuseLadder(when, initial, "ladder.yml is not valid YAML: " + oneLine(ex.getMessage()));
        } catch (IOException ex) {
            refuseLadder(when, initial, "ladder.yml could not be read: " + ex.getMessage());
        }
    }

    /** Refusal path: keep last-good (or the built-in default at boot), name the defect loudly. */
    private void refuseLadder(String when, boolean initial, String defect) {
        if (initial) {
            table = LadderTable.builtin();
            tableSource = "built-in default";
        }
        lastLadderDefect = defect;
        tableLoadNote = "REFUSED " + when + ": " + defect
                + (initial ? " — running on the built-in default table (the Nov 5, 2026 retuned values)"
                           : " — previous table kept (" + tableSource + ")");
        getLogger().warning("Ladder table REFUSED " + when + ": " + defect
                + (initial ? ". Running on the built-in default table (the Nov 5, 2026 retuned table). Fix ladder.yml and run /eliteladder reload."
                           : ". The previous table stays in force; nothing was changed. Fix ladder.yml and run /eliteladder reload again."));
    }

    private static Map<String, Object> sectionToMap(ConfigurationSection sec) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String k : sec.getKeys(false)) {
            Object v = sec.get(k);
            if (v instanceof ConfigurationSection sub) {
                v = sectionToMap(sub);
            }
            out.put(k, v);
        }
        return out;
    }

    private static String oneLine(String s) {
        if (s == null) return "unknown parse error";
        int i = s.indexOf('\n');
        return (i >= 0 ? s.substring(0, i) : s).trim();
    }

    // ---------- seal source ----------

    private Integer readSeals() {
        Plugin gg = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
        if (gg == null || !gg.isEnabled()) {
            return null;
        }
        try {
            if (sealsMethod == null) {
                Class<?> c = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI", true, gg.getClass().getClassLoader());
                sealsMethod = c.getMethod("sealsBrokenCount");
            }
            return (Integer) sealsMethod.invoke(null);
        } catch (Exception ex) {
            once("seals-read-" + ex.getClass().getSimpleName(), () -> getLogger().warning("Could not read GuildGateAPI.sealsBrokenCount(): " + ex));
            sealsMethod = null;
            return null;
        }
    }

    // ---------- files ----------

    private Path mobCombatFile() {
        return new File(new File(getDataFolder().getParentFile(), "EliteMobs"), "MobCombatSettings.yml").toPath();
    }

    private Path backupDir() {
        return new File(getDataFolder(), "backups").toPath();
    }

    private Map<String, Double> readLive() throws IOException {
        return MobCombatFile.read(new String(Files.readAllBytes(mobCombatFile()), StandardCharsets.UTF_8));
    }

    /** Adopt the live file as the baseline when it equals a ladder step. */
    private void tryBaseline() {
        Map<String, Double> live;
        try {
            live = readLive();
        } catch (IOException | LadderException ex) {
            String why = "cannot read live MobCombatSettings.yml: " + ex.getMessage();
            pendingNote = "REFUSED: " + why;
            once("baseline-" + why, () -> getLogger().warning("No baseline, nothing will be applied: " + why));
            return;
        }
        LadderTable active = table;
        int k = active.matchStep(live);
        if (k >= LadderTable.MIN_STEP) {
            appliedStep = k;
            lastResult = "baseline adopted: live file equals S" + k + "-norm";
            saveState();
            getLogger().info("Baseline adopted: live MobCombatSettings.yml equals ladder S" + k + "-norm (" + active.step(k).summary() + ").");
            return;
        }
        String why = LadderTable.describeOffLadder(live, active.step(1));
        pendingNote = "REFUSED: " + why + ". Install the S1-norm file first.";
        once("baseline-" + why, () -> getLogger().warning("No baseline, nothing will be applied: " + why
                + ". Install the S1-norm MobCombatSettings.yml (sha ff017c5d...) and the ladder starts from there."));
    }

    // ---------- watch ----------

    private void evaluate() {
        if (applying) {
            return;
        }
        Integer seals = readSeals();
        if (seals != null && seals != lastKnownSeals) {
            getLogger().info("GuildGate seals observed: " + seals + " of 5" + (lastKnownSeals >= 0 ? " (was " + lastKnownSeals + ")" : ""));
            lastKnownSeals = seals;
            saveState();
        }
        if (appliedStep < LadderTable.MIN_STEP) {
            tryBaseline();
        }
        int online = Bukkit.getOnlinePlayers().size();
        StepPlan.Decision d = StepPlan.decide(getConfig().getBoolean("enabled", true), seals, appliedStep, online, allowWithPlayers());
        switch (d.action()) {
            case DISABLED -> pendingNote = "master switch off";
            case NO_SEAL_SOURCE -> {
                pendingNote = "ExcelGuildGate not available";
                once("no-gg", () -> getLogger().warning("ExcelGuildGate not found or not enabled: seal watch idle."));
            }
            case NO_BASELINE -> {
                // pendingNote set by tryBaseline
            }
            case UP_TO_DATE -> pendingNote = "none";
            case WARN_DOWNGRADE -> {
                pendingNote = "none (seal count " + seals + " is below applied S" + appliedStep + "; no step-down)";
                once("down-" + appliedStep + "-" + seals, () -> {
                    String m = "GuildGate seal count (" + seals + ") is below the applied elite step (S" + appliedStep
                            + "). Per policy the ladder never steps down by itself; S" + appliedStep + " stays live until an operator acts.";
                    getLogger().warning(m);
                    tellAdmins("&c[EliteLadder] &7" + m);
                });
            }
            case WAIT_FOR_EMPTY -> {
                pendingNote = "S" + d.target() + " waiting for an empty server (" + online + " online)";
                once("wait-" + d.target(), () -> getLogger().info("Seal " + seals + " is broken: elite step S" + d.target()
                        + " is pending and will apply when the server is empty (" + online + " online now)."));
            }
            case APPLY -> {
                if (failedLatch) {
                    pendingNote = "S" + d.target() + " BLOCKED after a failed apply; check the log, then /eliteladder retry";
                    once("latch-" + d.target(), () -> getLogger().severe("S" + d.target() + " not applied: a previous apply failed. Fix the cause, then /eliteladder retry."));
                    return;
                }
                doApply(d.target(), online);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // The quitting player still counts during this event; look again one second later.
        getServer().getScheduler().runTaskLater(this, this::evaluate, 20L);
    }

    // ---------- apply ----------

    private void doApply(int target, int online) {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) {
            pendingNote = "S" + target + " waiting: EliteMobs is not enabled";
            once("no-em-" + target, () -> getLogger().warning("S" + target + " pending: EliteMobs is not enabled, nothing written."));
            return;
        }
        applying = true;
        int from = appliedStep;
        LadderTable active = table; // one consistent table for the whole apply
        LadderStep to = active.step(target);
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
            FileApplier.Result r = FileApplier.apply(mobCombatFile(), backupDir(), active, from, to, stamp, this::yamlCheck, faults());
            if (!r.ok()) {
                if (r.error() != null && r.error().startsWith("REFUSED")) {
                    lastResult = r.error();
                    pendingNote = "S" + target + " " + r.error();
                    once("refuse-" + target + "-" + r.error(), () -> {
                        getLogger().warning("S" + target + " not applied, " + r.error() + ". Nothing was changed.");
                        tellAdmins("&c[EliteLadder] &7S" + target + " not applied: live file is not the expected step. See console.");
                    });
                } else {
                    failedLatch = true;
                    lastResult = "FAILED S" + target + ": " + r.error();
                    pendingNote = "S" + target + " BLOCKED after a failed apply; /eliteladder retry";
                    getLogger().severe("S" + target + " apply FAILED: " + r.error() + ". File sha now " + r.shaAfter()
                            + " (before " + r.shaBefore() + "). Applied step stays S" + from + "; no reload was run.");
                    tellAdmins("&c[EliteLadder] &7S" + target + " apply failed and was rolled back. See console.");
                }
                saveState();
                return;
            }
            appliedStep = target;
            lastApplyAt = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a z", Locale.US).format(new Date());
            lastBackup = r.backup() == null ? "none (file already had S" + target + " values)" : r.backup().getFileName().toString();
            lastResult = "OK S" + from + " -> S" + target + " (sha " + r.shaAfter() + ")";
            pendingNote = "none";
            saveState();
            String reload = getConfig().getString("reload-command", "em reload");
            String line = "EliteLadder applied S" + target + " (from S" + from + ", seals " + lastKnownSeals + ", " + online
                    + " online): " + to.summary() + ". Backup " + lastBackup + ". File sha " + r.shaAfter() + ". Running '" + reload + "'.";
            getLogger().info(line);
            Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[EliteLadder] " + ChatColor.GREEN + "Elite step S" + target
                    + " applied (" + to.summary() + ").");
            appendLog(line);
            if (online > 0) {
                tellAdmins("&6[EliteLadder] &aElite step S" + target + " applied with players online (flag on). Reloading EliteMobs.");
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reload);
            long delay = Math.max(5, getConfig().getInt("post-reload-check-seconds", 20)) * 20L;
            getServer().getScheduler().runTaskLater(this, () -> postReloadCheck(to, r.shaAfter()), delay);
        } finally {
            applying = false;
        }
    }

    private FileApplier.Faults faults() {
        String f = System.getProperty(TEST_FAULT_PROPERTY);
        if ("beforeMove".equals(f)) {
            return new FileApplier.Faults() {
                @Override
                public void beforeMove(Path temp) throws IOException {
                    throw new IOException("rig test fault: write failed before move");
                }
            };
        }
        if ("afterMove".equals(f)) {
            return new FileApplier.Faults() {
                @Override
                public void afterMove(Path target) throws IOException {
                    byte[] b = Files.readAllBytes(target);
                    Files.write(target, java.util.Arrays.copyOf(b, b.length / 2));
                }
            };
        }
        return FileApplier.NONE;
    }

    /** Full YAML parse of the written text; top-level key set must be unchanged. */
    private String yamlCheck(String[] beforeAfter) {
        try {
            YamlConfiguration a = new YamlConfiguration();
            a.loadFromString(beforeAfter[0]);
            YamlConfiguration b = new YamlConfiguration();
            b.loadFromString(beforeAfter[1]);
            if (!a.getKeys(false).equals(b.getKeys(false))) {
                return "top-level key set changed";
            }
            return null;
        } catch (InvalidConfigurationException ex) {
            return "YAML parse failed: " + ex.getMessage();
        }
    }

    private void postReloadCheck(LadderStep to, String shaWritten) {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        try {
            Map<String, Double> live = readLive();
            String sha = FileApplier.shaOf(mobCombatFile());
            boolean ok = LadderTable.matches(live, to);
            String m = "Post-reload check S" + to.step() + ": EliteMobs " + (em != null && em.isEnabled() ? "enabled" : "NOT enabled")
                    + ", file values " + (ok ? "match" : "DIFFER: " + LadderTable.differences(live, to))
                    + ", sha " + sha + (sha.equals(shaWritten) ? " (unchanged by EliteMobs)" : " (EliteMobs rewrote the file; values checked above)");
            if (ok && em != null && em.isEnabled()) {
                getLogger().info(m);
            } else {
                getLogger().severe(m);
                lastResult = "S" + to.step() + " written but post-reload check failed: " + m;
                saveState();
            }
            appendLog(m);
        } catch (IOException | LadderException ex) {
            getLogger().severe("Post-reload check S" + to.step() + " could not read the file: " + ex.getMessage());
        }
    }

    private void appendLog(String line) {
        try {
            Files.createDirectories(getDataFolder().toPath());
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date());
            Files.writeString(new File(getDataFolder(), "apply-log.txt").toPath(), ts + " " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            getLogger().warning("Could not append apply-log.txt: " + ex.getMessage());
        }
    }

    private void tellAdmins(String msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("exceleliteladder.admin")) {
                p.sendMessage(color(msg));
            }
        }
    }

    private void once(String key, Runnable r) {
        if (loggedOnce.add(key)) {
            r.run();
        }
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // ---------- state ----------

    private File stateFile() {
        return new File(getDataFolder(), "state.yml");
    }

    private void loadState() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(stateFile());
        appliedStep = y.getInt("appliedStep", 0);
        lastKnownSeals = y.getInt("lastKnownSeals", -1);
        lastResult = y.getString("lastResult", "none yet");
        lastApplyAt = y.getString("lastApplyAt", "never");
        lastBackup = y.getString("lastBackup", "none");
        failedLatch = y.getBoolean("failedLatch", false);
    }

    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("appliedStep", appliedStep);
        y.set("lastKnownSeals", lastKnownSeals);
        y.set("lastResult", lastResult);
        y.set("lastApplyAt", lastApplyAt);
        y.set("lastBackup", lastBackup);
        y.set("failedLatch", failedLatch);
        try {
            getDataFolder().mkdirs();
            y.save(stateFile());
        } catch (IOException ex) {
            getLogger().warning("Could not save state.yml: " + ex.getMessage());
        }
    }

    // ---------- command ----------

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> {
                Integer seals = readSeals();
                LadderTable active = table;
                String liveText;
                try {
                    Map<String, Double> live = readLive();
                    int k = active.matchStep(live);
                    liveText = k > 0 ? "S" + k + "-norm" : "OFF LADDER (" + LadderTable.describeOffLadder(live, active.step(Math.max(1, appliedStep))) + ")";
                } catch (IOException | LadderException ex) {
                    liveText = "unreadable (" + ex.getMessage() + ")";
                }
                s.sendMessage(color("&6EliteLadder &7status"));
                s.sendMessage(color("&7Current seals: &f" + (seals == null ? "unknown (ExcelGuildGate missing)" : seals + " of 5")));
                s.sendMessage(color("&7Applied step: &f" + stepText(appliedStep)));
                s.sendMessage(color("&7Ladder table: &f" + tableSource));
                s.sendMessage(color("&7Table load: &f" + tableLoadNote));
                for (LadderStep st : active.steps()) {
                    s.sendMessage(color("&7  " + st.label() + ": &f" + st.summary()));
                }
                s.sendMessage(color("&7Live file: &f" + liveText));
                s.sendMessage(color("&7Elite speed: &f" + speedSettings.describe()
                        + (speedSettings.enabled() && !speedListenerActive ? " (listener inactive: EliteMobs not enabled)" : "")));
                s.sendMessage(color("&7Pending: &f" + pendingNote));
                s.sendMessage(color("&7Last result: &f" + lastResult));
                s.sendMessage(color("&7Last apply: &f" + lastApplyAt + " &7backup &f" + lastBackup));
                s.sendMessage(color("&7Reload with players online: &f" + (allowWithPlayers() ? "ALLOWED" : "off")
                        + " &7| players online: &f" + Bukkit.getOnlinePlayers().size()));
                return true;
            }
            case "retry" -> {
                failedLatch = false;
                loggedOnce.clear();
                saveState();
                s.sendMessage(color("&aFailure latch cleared. Checking now."));
                evaluate();
                return true;
            }
            case "check" -> {
                loggedOnce.clear();
                evaluate();
                s.sendMessage(color("&7Checked. Pending: &f" + pendingNote));
                return true;
            }
            case "reload" -> {
                reloadConfig();
                loadSpeedSettings();
                loadLadder(false);
                if (lastLadderDefect != null) {
                    s.sendMessage(color("&c[EliteLadder] &7ladder.yml REFUSED: " + lastLadderDefect
                            + ". Previous table kept (" + tableSource + "); nothing was changed."));
                } else {
                    s.sendMessage(color("&a[EliteLadder] &7Ladder table reloaded from ladder.yml and is now live:"));
                    for (LadderStep st : table.steps()) {
                        s.sendMessage(color("&7  " + st.label() + ": &f" + st.summary()));
                    }
                    evaluate();
                    s.sendMessage(color("&7Checked against the new table. Pending: &f" + pendingNote));
                }
                return true;
            }
            default -> {
                s.sendMessage(color("&7Usage: /eliteladder status | check | retry | reload"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String o : List.of("status", "check", "retry", "reload")) {
                if (o.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(o);
                }
            }
        }
        return out;
    }
}
