package com.winkreyes.excelmagicgate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ExcelMagicGate: the automation bridge for the WRV Magic rollout
 * (Route Sheet WRV-070).
 *
 * Magic (by NathanWolf) releases in five stages as the Adventurers Guild
 * seals fall. This plugin watches ExcelGuildGate's broken-seal count
 * through its public GuildGateAPI (read by reflection, the consumption
 * pattern that API documents), and when stage N becomes eligible it:
 *   1. backs up the live Magic config set,
 *   2. swaps in stage N's curated packet files (shipped in this
 *      plugin's data folder, stages/stage0..stage5),
 *   3. reloads Magic via its own /magic reload command,
 *   4. runs stage N's LuckPerms grant list verbatim as console commands,
 *   5. persists appliedStage, and announces the unlock.
 * Any failure before step 5 leaves appliedStage untouched and is
 * retried on the next watch tick.
 *
 * Stages only ever move UP. If the seal count drops below the applied
 * stage (a /guildgate revoke), the bridge warns the console and online
 * operators and changes nothing: grants and configs are never removed
 * automatically.
 *
 * Stage 0 is the dormant pre-Seal-1 state: the Phase 1 packet's config
 * with the rank wands removed and an empty grant list. A fresh install
 * sits at stage 0 until the first seal falls.
 *
 * v0.1.0: initial release. NOT installed live - gated on the WRV-070
 * Phase 0 player trial (see INSTALL-RUNBOOK.md).
 */
public class ExcelMagicGate extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    /** The six Magic config files the bridge manages. Nothing else in
     *  plugins/Magic (mage data, worlds, generated files) is ever touched. */
    static final List<String> MAGIC_FILES = List.of(
            "config.yml", "spells.yml", "paths.yml", "wands.yml", "classes.yml", "messages.yml");
    private static final String GRANTS_FILE = "grants.txt";

    private BridgeState state;
    private boolean applying = false;
    private Method guildGateSealsMethod; // cached GuildGateAPI.sealsBrokenCount

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        extractStages();
        boolean freshState = !stateFile().exists();
        state = loadState();

        if (freshState && state.appliedStage() == 0) {
            seedStageZeroIfMagicMissing();
        }
        canaryCheck();

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("magicgate") != null) {
            getCommand("magicgate").setExecutor(this);
            getCommand("magicgate").setTabCompleter(this);
        }

        long pollTicks = Math.max(5, getConfig().getInt("poll-seconds", 60)) * 20L;
        getServer().getScheduler().runTaskTimer(this, this::evaluate, 100L, pollTicks);

        if (sealSourceAvailable()) {
            getLogger().info("ExcelMagicGate enabled: applied stage " + state.appliedStage()
                    + " of 5, watching ExcelGuildGate seals every "
                    + getConfig().getInt("poll-seconds", 60) + "s"
                    + (getConfig().getBoolean("enabled", true) ? "" : " (master switch OFF)"));
        } else {
            getLogger().warning("ExcelMagicGate enabled: ExcelGuildGate not found "
                    + "- seal watch idle, no stage will ever become eligible.");
        }
    }

    @Override
    public void onDisable() {
        if (state != null) {
            saveState();
        }
    }

    // ------------------------------------------------------------------
    // Seal watch
    // ------------------------------------------------------------------

    /** One watch evaluation: observe seals, then act on the plan. */
    private void evaluate() {
        if (applying) {
            return;
        }
        Integer seals = readSeals();
        if (seals != null && seals != state.lastKnownSeals()) {
            getLogger().info("GuildGate seals observed: " + seals + " of 5"
                    + (state.lastKnownSeals() >= 0 ? " (was " + state.lastKnownSeals() + ")" : ""));
            state.setLastKnownSeals(seals);
            saveState();
        }
        StagePlan.Decision decision = StagePlan.decide(
                getConfig().getBoolean("enabled", true),
                seals != null,
                seals == null ? 0 : seals,
                state.appliedStage());
        switch (decision.action()) {
            case APPLY -> doApply(decision.targetStage(), "seal watch");
            case WARN_DOWNGRADE -> {
                if (state.downgradeWarnedFor() != state.appliedStage()) {
                    state.setDowngradeWarnedFor(state.appliedStage());
                    saveState();
                    String line = "GuildGate seal count (" + seals + ") has dropped below the applied "
                            + "Magic stage (" + state.appliedStage() + "). Per policy the bridge does NOT "
                            + "remove grants or downgrade configs automatically - stage "
                            + state.appliedStage() + " stays live until an operator acts.";
                    getLogger().warning(line);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("excelmagicgate.admin")) {
                            p.sendMessage(color("&c[MagicGate] &7" + line));
                        }
                    }
                }
            }
            default -> {
                // NONE / UNAVAILABLE: nothing to do this tick.
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // A join is a cheap extra watch tick (an admin may have granted a
        // seal from the panel between polls). No per-player state is kept
        // here - join catch-up messaging lives in ExcelGuildGate itself.
        evaluate();
    }

    /** GuildGate's broken-seal count, or null when the source is unavailable. */
    private Integer readSeals() {
        Plugin gg = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
        if (gg == null || !gg.isEnabled()) {
            return null;
        }
        try {
            if (guildGateSealsMethod == null) {
                Class<?> api = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                        true, gg.getClass().getClassLoader());
                guildGateSealsMethod = api.getMethod("sealsBrokenCount");
            }
            return (Integer) guildGateSealsMethod.invoke(null);
        } catch (Exception e) {
            getLogger().warning("Could not read GuildGateAPI.sealsBrokenCount(): " + e.getMessage());
            guildGateSealsMethod = null;
            return null;
        }
    }

    private boolean sealSourceAvailable() {
        return readSeals() != null;
    }

    // ------------------------------------------------------------------
    // Stage apply
    // ------------------------------------------------------------------

    private void doApply(int stage, String source) {
        if (applying) {
            getLogger().warning("Apply of stage " + stage + " skipped: another apply is running.");
            return;
        }
        applying = true;
        try {
            getLogger().info("Applying Magic stage " + stage + " (source: " + source
                    + ", previously applied: " + state.appliedStage() + ")");
            StageApplier.Result result = StageApplier.apply(new ApplySteps(stage),
                    msg -> getLogger().warning(msg));
            if (result.success()) {
                state.recordApply(stage, "OK (" + source + ")");
                saveState();
                getLogger().info("Magic stage " + stage + " applied successfully.");
            } else {
                state.recordApply(stage, "FAILED at " + result.failedStep() + ": " + result.error());
                saveState();
                getLogger().severe("Magic stage " + stage + " apply FAILED at step '"
                        + result.failedStep() + "': " + result.error()
                        + " - appliedStage remains " + state.appliedStage()
                        + "; nothing was persisted and the apply will retry on the next watch tick.");
            }
        } finally {
            applying = false;
        }
    }

    /** Bukkit-backed apply steps for one stage. */
    private final class ApplySteps implements StageApplier.Steps {
        private final int stage;
        private GrantList.Parsed grants;

        ApplySteps(int stage) {
            this.stage = stage;
        }

        @Override
        public void backup() throws Exception {
            File magic = magicFolder();
            File dir = new File(new File(getDataFolder(), "backups"),
                    new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date())
                            + "-from-stage" + state.appliedStage());
            if (!dir.mkdirs() && !dir.isDirectory()) {
                throw new IOException("could not create backup dir " + dir);
            }
            for (String name : MAGIC_FILES) {
                File src = new File(magic, name);
                if (src.isFile()) {
                    Files.copy(src.toPath(), new File(dir, name).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            getLogger().info("Backed up live Magic config set to " + dir.getName());
        }

        @Override
        public void swapConfig() throws Exception {
            File magic = magicFolder();
            if (!magic.isDirectory() && !magic.mkdirs()) {
                throw new IOException("could not create " + magic);
            }
            File stageDir = stageDir(stage);
            for (String name : MAGIC_FILES) {
                File src = new File(stageDir, name);
                if (!src.isFile()) {
                    throw new IOException("stage file missing: " + src);
                }
                Files.copy(src.toPath(), new File(magic, name).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            getLogger().info("Swapped Magic config set to stage " + stage + " packet files.");
        }

        @Override
        public void reloadMagic() throws Exception {
            Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
            if (magic == null || !magic.isEnabled()) {
                throw new IllegalStateException("Magic plugin is not installed/enabled");
            }
            // Magic's real reload path (upstream source, Magic 11.2.5):
            // /magic reload -> MagicController.updateConfiguration ->
            // loadConfiguration(), the same full load used on enable
            // (spells, paths, wands, classes). It runs as a load task and
            // logs its "Loaded N spells" lines when done.
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magic reload")) {
                throw new IllegalStateException("'magic reload' command was not accepted");
            }
            getLogger().info("Magic reload dispatched.");
        }

        @Override
        public void runGrants() throws Exception {
            File grantsFile = new File(stageDir(stage), GRANTS_FILE);
            List<String> lines = grantsFile.isFile()
                    ? Files.readAllLines(grantsFile.toPath())
                    : List.of();
            grants = GrantList.parse(lines);
            if (grants.size() == 0 && grants.preCommands().isEmpty()) {
                getLogger().info("Stage " + stage + " grant list is empty - nothing to run.");
                return;
            }
            Plugin lp = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (lp == null || !lp.isEnabled()) {
                throw new IllegalStateException("LuckPerms is not installed/enabled but stage "
                        + stage + " has " + grants.size() + " grants");
            }
            for (String pre : grants.preCommands()) {
                dispatchChecked(pre);
            }
            for (String cmd : grants.commands()) {
                dispatchChecked(cmd);
            }
            getLogger().info("Ran " + grants.size() + " LuckPerms grant commands for stage " + stage + ".");
        }

        private void dispatchChecked(String cmd) throws Exception {
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)) {
                throw new IllegalStateException("console command was not accepted: " + cmd);
            }
        }

        @Override
        public void persist() {
            state.setAppliedStage(stage);
            saveState();
        }

        @Override
        public void announce() {
            StageMessages.StageMessage m = StageMessages.resolve(stage,
                    configScalars(stage), configLists(stage));
            if (m == null) {
                return; // stage 0 has no announcement
            }
            String title = fill(m.title(), stage, m);
            String subtitle = fill(m.subtitle(), stage, m);
            String sound = getConfig().getString("announce.sound", "ui.toast.challenge_complete");
            float volume = (float) getConfig().getDouble("announce.volume", 1.0);
            float pitch = (float) getConfig().getDouble("announce.pitch", 1.0);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(color(title), color(subtitle), 10, 70, 20);
                if (sound != null && !sound.isBlank()) {
                    p.playSound(p.getLocation(), sound, volume, pitch);
                }
            }
            for (String line : m.broadcast()) {
                Bukkit.broadcastMessage(color(fill(line, stage, m)));
            }
            getLogger().info("Announced Magic stage " + stage + ": " + m.name());
        }

        private String fill(String text, int stage, StageMessages.StageMessage m) {
            return text.replace("{stage}", String.valueOf(stage))
                    .replace("{name}", m.name())
                    .replace("{unlocks}", m.unlocks());
        }
    }

    private Map<String, String> configScalars(int stage) {
        Map<String, String> out = new HashMap<>();
        ConfigurationSection sec = getConfig().getConfigurationSection("messages.stages." + stage);
        if (sec != null) {
            for (String key : List.of("name", "title", "subtitle", "unlocks")) {
                String v = sec.getString(key);
                if (v != null) {
                    out.put(key, v);
                }
            }
        }
        return out;
    }

    private Map<String, List<String>> configLists(int stage) {
        Map<String, List<String>> out = new HashMap<>();
        ConfigurationSection sec = getConfig().getConfigurationSection("messages.stages." + stage);
        if (sec != null) {
            List<String> lines = sec.getStringList("broadcast");
            if (!lines.isEmpty()) {
                out.put("broadcast", lines);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Stage files, seeding, persistence
    // ------------------------------------------------------------------

    private File magicFolder() {
        return new File(getDataFolder().getParent(), "Magic");
    }

    private File stageDir(int stage) {
        return new File(new File(getDataFolder(), "stages"), "stage" + stage);
    }

    /** Extract the bundled stage packets into the data folder (missing files only). */
    private void extractStages() {
        List<String> files = new ArrayList<>(MAGIC_FILES);
        files.add(GRANTS_FILE);
        for (int stage = 0; stage <= StagePlan.MAX_STAGE; stage++) {
            File dir = stageDir(stage);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                getLogger().warning("Could not create stage dir " + dir);
                continue;
            }
            for (String name : files) {
                File target = new File(dir, name);
                if (target.isFile()) {
                    continue;
                }
                String resource = "stages/stage" + stage + "/" + name;
                try (InputStream in = getResource(resource)) {
                    if (in == null) {
                        getLogger().warning("Bundled resource missing from jar: " + resource);
                        continue;
                    }
                    Files.copy(in, target.toPath());
                } catch (IOException e) {
                    getLogger().warning("Could not extract " + resource + ": " + e.getMessage());
                }
            }
        }
    }

    /** Fresh install: if Magic has no config set at all, seed the dormant stage 0 set. */
    private void seedStageZeroIfMagicMissing() {
        File magic = magicFolder();
        boolean anyPresent = false;
        for (String name : MAGIC_FILES) {
            if (new File(magic, name).isFile()) {
                anyPresent = true;
                break;
            }
        }
        if (anyPresent) {
            return;
        }
        try {
            if (!magic.isDirectory() && !magic.mkdirs()) {
                getLogger().warning("Could not create " + magic + " to seed stage 0.");
                return;
            }
            File stage0 = stageDir(0);
            for (String name : MAGIC_FILES) {
                File src = new File(stage0, name);
                if (src.isFile()) {
                    Files.copy(src.toPath(), new File(magic, name).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            getLogger().info("Seeded the dormant stage 0 Magic config set "
                    + "(Phase 1 packet files, rank wands removed, no grants).");
        } catch (IOException e) {
            getLogger().warning("Stage 0 seed failed: " + e.getMessage());
        }
    }

    /**
     * Packet canary: every WRV packet's spells.yml defines our custom
     * Storm Shackle spell. At stage 0 a live Magic config without it is
     * not a WRV packet (most likely Magic's own defaults from an
     * unseeded first boot) - warn loudly, clobber nothing. The stage 1
     * apply replaces all six files anyway.
     */
    private void canaryCheck() {
        if (state.appliedStage() != 0) {
            return;
        }
        File spells = new File(magicFolder(), "spells.yml");
        if (!spells.isFile()) {
            return;
        }
        try {
            String text = Files.readString(spells.toPath());
            if (!text.contains("stormshackle")) {
                getLogger().severe("Live Magic spells.yml does NOT contain 'stormshackle': "
                        + "the running Magic config is not a WRV packet. Pre-seed stage 0 "
                        + "per INSTALL-RUNBOOK.md before the first seal falls. The bridge "
                        + "will repair this automatically when stage 1 applies.");
            }
        } catch (IOException e) {
            getLogger().warning("Canary check could not read spells.yml: " + e.getMessage());
        }
    }

    private File stateFile() {
        return new File(getDataFolder(), "state.yml");
    }

    private BridgeState loadState() {
        File file = stateFile();
        if (!file.isFile()) {
            return new BridgeState();
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            Map<String, Object> m = new HashMap<>();
            for (String key : y.getKeys(false)) {
                m.put(key, y.get(key));
            }
            return BridgeState.fromMap(m);
        } catch (Exception e) {
            getLogger().warning("state.yml could not be read (" + e.getMessage()
                    + "). Starting from stage 0 state.");
            return new BridgeState();
        }
    }

    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Object> e : state.toMap().entrySet()) {
            y.set(e.getKey(), e.getValue());
        }
        try {
            y.save(stateFile());
        } catch (IOException e) {
            getLogger().warning("Could not save state.yml: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("magicgate")) {
            return false;
        }
        if (!sender.hasPermission("excelmagicgate.admin")) {
            sender.sendMessage(color("&cYou do not have permission to use /magicgate."));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendStatus(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("apply")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cUsage: /magicgate apply <stage 0-5>"));
                return true;
            }
            int stage;
            try {
                stage = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(color("&cNot a stage number: &f" + args[1]));
                return true;
            }
            if (stage < 0 || stage > StagePlan.MAX_STAGE) {
                sender.sendMessage(color("&cStage must be 0-5."));
                return true;
            }
            if (stage < state.appliedStage()) {
                sender.sendMessage(color("&cRefused: stages only move up (applied is "
                        + state.appliedStage() + "). For a manual rollback use the packet "
                        + "runbooks - the bridge never downgrades."));
                return true;
            }
            sender.sendMessage(color("&7Applying Magic stage " + stage + " manually..."));
            doApply(stage, "manual /magicgate apply by " + sender.getName());
            sender.sendMessage(color(state.appliedStage() == stage
                    ? "&aStage " + stage + " is now applied."
                    : "&cApply did not complete - see the console log for the failed step."));
            return true;
        }
        sender.sendMessage(color("&6/magicgate &7- Magic stage bridge admin"));
        sender.sendMessage(color("&e/magicgate status &7- seals, applied stage, per-stage state"));
        sender.sendMessage(color("&e/magicgate apply <stage> &7- run the apply path by hand (stages only move up)"));
        return true;
    }

    private void sendStatus(CommandSender sender) {
        Integer seals = readSeals();
        boolean enabled = getConfig().getBoolean("enabled", true);
        sender.sendMessage(color("&6— ExcelMagicGate: status —"));
        sender.sendMessage(color("&7Master switch: &f" + (enabled ? "ON" : "OFF (watch inert)")
                + " &7| Seal source: &f" + (seals != null ? "ExcelGuildGate API" : "UNAVAILABLE")
                + " &7| Seals fallen: &f" + (seals != null ? seals + " of 5" : "unknown")));
        sender.sendMessage(color("&7Applied Magic stage: &f" + state.appliedStage() + " of 5"
                + (state.lastApplyStage() >= 0
                        ? " &7| Last apply: stage " + state.lastApplyStage() + " -> " + state.lastApplyResult()
                        : "")));
        for (int stage = 0; stage <= StagePlan.MAX_STAGE; stage++) {
            StageMessages.StageMessage m = StageMessages.defaultsFor(stage);
            String name = stage == 0 ? "Dormant (pre-Seal-1)" : (m == null ? "" : m.name());
            String status;
            if (stage < state.appliedStage()) {
                status = "&7applied earlier";
            } else if (stage == state.appliedStage()) {
                status = "&aAPPLIED";
            } else if (seals != null && stage <= seals) {
                status = "&eeligible - will apply";
            } else {
                status = "&8locked (needs " + stage + " seals)";
            }
            sender.sendMessage(color(" &7Stage " + stage + " &f" + name + " &7- " + status));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("magicgate")) {
            return null;
        }
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("status", "apply")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("apply")) {
            for (int i = 0; i <= StagePlan.MAX_STAGE; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(args[1])) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
