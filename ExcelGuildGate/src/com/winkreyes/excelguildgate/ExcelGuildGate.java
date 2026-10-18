package com.winkreyes.excelguildgate;

import com.winkreyes.guildgate.api.GuildGateAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.Raid;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExcelGuildGate: "The Road to the Guild".
 *
 * The Adventurers Guild (EliteMobs, world em_adventurers_guild, reached via
 * /ag) stays locked until the family collectively breaks five seals:
 * ender_dragon, wither, elder_guardian, warden, raid. Progress is
 * server-wide and persisted in state.yml across restarts. When every
 * enabled seal is broken the guild unlocks permanently.
 *
 * The gate only intercepts the /ag command (and its alias) and teleports
 * INTO the guild world. The guild world itself, EliteMobs and its NPCs are
 * never touched; this plugin works with EliteMobs entirely absent.
 *
 * v0.1.0: initial release.
 * v0.2.0: join catch-up. Every player's seen-stage (the highest fallen-seal
 * count they have been told about) is tracked in state.yml; a player who
 * was away when seals fell gets ONE combined personal catch-up message on
 * their next join, listing each missed beat (see CatchUpTracker for the
 * exact rules, including the silent first-sighting start and the revoke
 * clamp). /guildgate catchup shows and resets a player's mark.
 *
 * v0.3.0: narration. On the celebrated fall path, a recorded narrator
 * voice line (a custom sound from the server resource pack,
 * wrv:guildgate.seal&lt;N&gt;) plays for everyone online, followed by a
 * short staggered guide for the stage that just opened (see NarrationGuide
 * for the plan and its config semantics). The join catch-up additionally
 * replays the voice line for the returner's highest missed stage, to that
 * player only. Narration is garnish, never load-bearing: every dispatch
 * is guarded so a missing pack or an unknown sound can never break a
 * seal fall, and players without the pack still get the text.
 */
public class ExcelGuildGate extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    static final String GUILD_WORLD = "em_adventurers_guild";
    static final List<String> SEAL_ORDER = List.of(
            "ender_dragon", "wither", "elder_guardian", "warden", "raid");
    private static final long GATE_NOTICE_THROTTLE_MS = 5000L;

    private record SealDef(String id, boolean enabled, String name, String hint, String flavor) {}

    /** One catch-up beat: the Nth fallen seal (1-based) and what it carries. */
    private record BeatDef(String name, String line) {}

    /**
     * Built-in beat wording, used for any beat the config does not define.
     * The beats key on the fallen-seal COUNT, not on which boss fell: the
     * Nth seal down, in any order, carries the Nth beat (the same five
     * stages the Magic rollout unlocks behind the seals).
     */
    private static final String[][] BEAT_FALLBACKS = {
            {"The Healer",
                    "&aThe First Seal fell &7- the Adventurers Guild stirs, and &aThe Healer's"
                            + " &7magic returns to the valley."},
            {"The Pathfinder",
                    "&aThe Second Seal fell &7- &aThe Pathfinder's &7magic returns: light for"
                            + " dark places, keen sight, and the getting-around arts."},
            {"The Wonder",
                    "&aThe Third Seal fell &7- &aThe Wonder &7returns: lightning answers the"
                            + " call again, and the wild elements stir."},
            {"The Masons",
                    "&aThe Fourth Seal fell &7- &aThe Masons' &7magic returns: spells that"
                            + " place, move, and shape stone and timber."},
            {"The Guild Opens",
                    "&6The Fifth Seal fell &7- &6the Adventurers Guild is OPEN&7, and the last"
                            + " schools of magic return with it. Type &f/ag &7to visit!"},
    };

    private final Map<String, SealDef> defs = new LinkedHashMap<>();
    private final List<BeatDef> beats = new ArrayList<>();
    private final CatchUpTracker catchUp = new CatchUpTracker();
    private NarrationGuide narration = NarrationGuide.defaults(true, true, 2L);
    private SealState state;
    private boolean gateEnabled = true;
    private boolean catchUpEnabled = true;
    private final Map<UUID, Long> lastGateNotice = new HashMap<>();
    private GuildGateExpansion expansion;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDefs();
        loadBeats();
        Set<String> enabled = new LinkedHashSet<>();
        for (SealDef d : defs.values()) {
            if (d.enabled()) {
                enabled.add(d.id());
            }
        }
        state = new SealState(new LinkedHashSet<>(defs.keySet()), enabled);
        loadState();
        gateEnabled = getConfig().getBoolean("gateEnabled", true);
        catchUpEnabled = getConfig().getBoolean("catchup.enabled", true);
        narration = loadNarration();
        GuildGateAPI.bind(state);

        // PlaceholderAPI hook (softdepend): powers the DeluxeMenus
        // locked/open guild button variants via %guildgate_unlocked%.
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                expansion = new GuildGateExpansion(this);
                expansion.register();
                getLogger().info("Hooked into PlaceholderAPI (%guildgate_unlocked%, "
                        + "%guildgate_seals_broken%, %guildgate_seals_total%).");
            } catch (Exception e) {
                getLogger().warning("PlaceholderAPI hook failed: " + e.getMessage());
                expansion = null;
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("seals") != null) {
            getCommand("seals").setExecutor(this);
        }
        if (getCommand("guildgate") != null) {
            getCommand("guildgate").setExecutor(this);
            getCommand("guildgate").setTabCompleter(this);
        }
        getLogger().info("ExcelGuildGate enabled: " + state.brokenCount() + " of "
                + state.requiredCount() + " seals broken, guild "
                + (state.isUnlocked() ? "OPEN" : "LOCKED")
                + (gateEnabled ? "" : " (gate disabled in config)"));
        getLogger().info("Join catch-up: " + (catchUpEnabled ? "on" : "off")
                + ", " + catchUp.trackedCount() + " players tracked.");
        getLogger().info("Narration: " + (narration.enabled()
                ? "on, catch-up replay " + (narration.catchupReplay() ? "on" : "off")
                        + ", guide delay " + narration.guideLineDelaySeconds() + "s"
                        + " (pack sounds " + NarrationGuide.SOUND_PREFIX + "1.."
                        + NarrationGuide.STAGE_COUNT + ")"
                : "off"));
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Exception ignored) {
                // PlaceholderAPI already gone; nothing to clean up.
            }
            expansion = null;
        }
        GuildGateAPI.unbind();
    }

    private void loadDefs() {
        defs.clear();
        Map<String, String[]> fallbacks = Map.of(
                "ender_dragon", new String[]{"The Ender Dragon",
                        "Waits in the End, past the stronghold portal.",
                        "The Ender Dragon has fallen! The End is free."},
                "wither", new String[]{"The Wither",
                        "Summon it somewhere far from anything you love.",
                        "The Wither has fallen! The sky is quiet again."},
                "elder_guardian", new String[]{"An Elder Guardian",
                        "Guards an ocean monument. Bring milk for the mining fatigue.",
                        "An Elder Guardian has fallen! The monument treasure is unguarded."},
                "warden", new String[]{"The Warden",
                        "Sleeps in the deep dark and hunts by sound. Sneak, or bring everyone.",
                        "The Warden has fallen! The deep dark feels safer tonight."},
                "raid", new String[]{"A Raid Victory",
                        "Bad omen, then a village. Hold the line until the horn sounds victory.",
                        "The raid is broken! The village celebrates its heroes."});
        for (String id : SEAL_ORDER) {
            String base = "seals." + id + ".";
            String[] fb = fallbacks.get(id);
            boolean enabled = getConfig().getBoolean(base + "enabled", true);
            String name = getConfig().getString(base + "name", fb[0]);
            String hint = getConfig().getString(base + "hint", fb[1]);
            String flavor = getConfig().getString(base + "flavor", fb[2]);
            defs.put(id, new SealDef(id, enabled, name, hint, flavor));
        }
    }

    /**
     * Catch-up beat wording: the config's catchup.beats list (name + line
     * per beat, in order) over the built-in defaults. A missing or partial
     * config (including the live 0.1.0 config, which has no catchup section
     * at all) falls back per beat, so the feature works before anyone
     * bothers merging the new config in.
     */
    private void loadBeats() {
        beats.clear();
        List<Map<?, ?>> raw = getConfig().getMapList("catchup.beats");
        for (int i = 0; i < BEAT_FALLBACKS.length; i++) {
            String name = BEAT_FALLBACKS[i][0];
            String line = BEAT_FALLBACKS[i][1];
            if (raw != null && i < raw.size() && raw.get(i) != null) {
                Object n = raw.get(i).get("name");
                Object l = raw.get(i).get("line");
                if (n != null && !String.valueOf(n).isBlank()) {
                    name = String.valueOf(n);
                }
                if (l != null && !String.valueOf(l).isBlank()) {
                    line = String.valueOf(l);
                }
            }
            beats.add(new BeatDef(name, line));
        }
    }

    /**
     * Narration plan (v0.3.0): the narration section of the config parsed
     * into a NarrationGuide. Per stage, an entry may carry a "sound"
     * override (blank silences that stage's voice line) and a "lines"
     * list that replaces the stage's guide wholesale. A config with no
     * narration section at all (including the live 0.1.0 config) yields
     * the built-in defaults, exactly like the catch-up beats.
     */
    private NarrationGuide loadNarration() {
        boolean enabled = getConfig().getBoolean("narration.enabled", true);
        boolean replay = getConfig().getBoolean("narration.catchupReplay", true);
        long delay = getConfig().getLong("narration.guideLineDelaySeconds", 2L);
        List<String> soundOverrides = new ArrayList<>();
        List<List<String>> guideOverrides = new ArrayList<>();
        List<Map<?, ?>> raw = getConfig().getMapList("narration.guides");
        for (int i = 0; i < NarrationGuide.STAGE_COUNT; i++) {
            String sound = null;
            List<String> lines = null;
            if (raw != null && i < raw.size() && raw.get(i) != null) {
                Object s = raw.get(i).get("sound");
                if (s != null) {
                    sound = String.valueOf(s);
                }
                Object l = raw.get(i).get("lines");
                if (l instanceof List<?> list) {
                    lines = new ArrayList<>();
                    for (Object item : list) {
                        lines.add(item == null ? null : String.valueOf(item));
                    }
                }
            }
            soundOverrides.add(sound);
            guideOverrides.add(lines);
        }
        return NarrationGuide.build(enabled, replay, delay, soundOverrides, guideOverrides);
    }

    // ------------------------------------------------------------------
    // Persistence (state.yml)
    // ------------------------------------------------------------------

    private File stateFile() {
        return new File(getDataFolder(), "state.yml");
    }

    private void loadState() {
        File file = stateFile();
        if (!file.exists()) {
            getLogger().info("No state.yml yet: starting the Road to the Guild fresh.");
            return;
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            Map<String, SealState.SealRecord> restored = new HashMap<>();
            ConfigurationSection sec = y.getConfigurationSection("seals");
            if (sec != null) {
                for (String id : sec.getKeys(false)) {
                    if (!defs.containsKey(id)) {
                        continue;
                    }
                    if (sec.getBoolean(id + ".broken", false)) {
                        restored.put(id, new SealState.SealRecord(
                                sec.getLong(id + ".timestamp", 0L),
                                sec.getString(id + ".killer")));
                    }
                }
            }
            state.restore(restored, y.getLong("unlockTimestamp", 0L));
            // Join catch-up records (v0.2.0). A 0.1.0 state file has no
            // "seen" section at all: everyone simply starts untracked,
            // which the join path treats as a silent start at the current
            // stage. Bad entries are skipped one by one.
            Map<UUID, Integer> seenMap = new HashMap<>();
            ConfigurationSection seenSec = y.getConfigurationSection("seen");
            if (seenSec != null) {
                for (String key : seenSec.getKeys(false)) {
                    try {
                        int stage = seenSec.getInt(key, -1);
                        if (stage >= 0) {
                            seenMap.put(UUID.fromString(key), stage);
                        }
                    } catch (IllegalArgumentException ignored) {
                        getLogger().warning("state.yml: skipping bad catch-up entry '" + key + "'.");
                    }
                }
            }
            catchUp.restore(seenMap);
        } catch (Exception e) {
            getLogger().warning("state.yml could not be read (" + e.getMessage()
                    + "). Starting fresh: no seals are marked broken.");
        }
    }

    private void saveState() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("unlocked", state.isUnlocked());
        y.set("unlockTimestamp", state.unlockTimestampMillis());
        for (String id : SEAL_ORDER) {
            SealState.SealRecord rec = state.record(id);
            y.set("seals." + id + ".broken", rec != null);
            if (rec != null) {
                y.set("seals." + id + ".timestamp", rec.timestampMillis());
                if (rec.killer() != null) {
                    y.set("seals." + id + ".killer", rec.killer());
                }
            }
        }
        // Join catch-up records (v0.2.0): additive section only, so a
        // rolled-back 0.1.0 jar reads this file exactly as before (it
        // ignores unknown top-level keys). Sorted for a stable file.
        Map<UUID, Integer> seenMap = catchUp.snapshot();
        List<UUID> ids = new ArrayList<>(seenMap.keySet());
        ids.sort(Comparator.comparing(UUID::toString));
        for (UUID id : ids) {
            y.set("seen." + id, seenMap.get(id));
        }
        try {
            y.save(stateFile());
        } catch (IOException e) {
            getLogger().warning("Could not save state.yml: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Seal crediting
    // ------------------------------------------------------------------

    /**
     * The one path every seal break goes through. Persists immediately;
     * broadcasts the celebration for real kills (celebrate=true) and the
     * unlock when the set completes. Duplicate credits are no-ops.
     */
    private void creditSeal(String sealId, String killerName, boolean celebrate) {
        boolean wasUnlocked = state.isUnlocked();
        SealState.CreditResult result = state.credit(sealId, killerName, System.currentTimeMillis());
        if (result != SealState.CreditResult.CREDITED) {
            return;
        }
        // Everyone online is standing here for this fall (this is the
        // announced path; admin grant/open stay quiet and do NOT mark
        // anyone told). Advance their seen-stage with the moment, in the
        // same save as the seal itself.
        List<UUID> online = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            online.add(p.getUniqueId());
        }
        catchUp.advanceOnline(online, state.brokenCount());
        saveState();
        getLogger().info("Seal broken: " + sealId
                + (killerName != null ? " (killer: " + killerName + ")" : "")
                + " - " + state.brokenCount() + " of " + state.requiredCount() + " seals broken.");
        if (celebrate) {
            SealDef def = defs.get(sealId);
            Bukkit.broadcastMessage(color(msg("sealBrokenLine1")
                    .replace("{flavor}", def.flavor())
                    .replace("{name}", def.name())));
            Bukkit.broadcastMessage(color(msg("sealBrokenLine2")
                    .replace("{broken}", String.valueOf(state.brokenCount()))
                    .replace("{total}", String.valueOf(state.requiredCount()))));
            // The narrator speaks for the stage this fall just opened
            // (v0.3.0): voice line first, then the staggered guide.
            runNarrationMoment(state.brokenCount());
        }
        if (!wasUnlocked && state.isUnlocked()) {
            getLogger().info("All seals broken: the Adventurers Guild is now OPEN.");
            if (celebrate) {
                Bukkit.broadcastMessage(color(msg("unlockLine1")));
                Bukkit.broadcastMessage(color(msg("unlockLine2")));
            }
        }
    }

    // ------------------------------------------------------------------
    // Detection
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String sealId = null;
        if (entity instanceof EnderDragon) {
            sealId = "ender_dragon";
        } else if (entity instanceof Wither) {
            sealId = "wither";
        } else if (entity instanceof ElderGuardian) {
            sealId = "elder_guardian";
        } else if (entity instanceof Warden) {
            sealId = "warden";
        }
        if (sealId == null) {
            return;
        }
        Player killer = entity.getKiller();
        // Forgiving by design, but never credit with nobody around: a seal
        // counts if a player landed the kill OR any player was online.
        if (killer == null && Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        creditSeal(sealId, killer != null ? killer.getName() : null, true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidFinish(RaidFinishEvent event) {
        if (event.getRaid() == null || event.getRaid().getStatus() != Raid.RaidStatus.VICTORY) {
            return;
        }
        String winner = null;
        if (event.getWinners() != null && !event.getWinners().isEmpty()) {
            winner = event.getWinners().get(0).getName();
        }
        creditSeal("raid", winner, true);
    }

    // ------------------------------------------------------------------
    // Join catch-up (v0.2.0)
    // ------------------------------------------------------------------

    /**
     * A few seconds after a player joins (so the message is not buried in
     * the join spam), settle their seen-stage against the campaign: send
     * the ONE combined catch-up if they missed any beats, start them
     * silently if they are new, clamp silently if a revoke dropped the
     * stage under them. The exact rules live in CatchUpTracker.
     *
     * If a seal falls while the delayed check is still pending, the fall
     * path advances every online player (this one included) first, so the
     * check then finds nothing to say: they heard it live.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!catchUpEnabled) {
            return;
        }
        Player player = event.getPlayer();
        long delaySeconds = Math.max(0L, getConfig().getLong("catchup.joinDelaySeconds", 5L));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            int current = state.brokenCount();
            CatchUpTracker.JoinResult result = catchUp.evaluateJoin(player.getUniqueId(), current);
            if (result.changed()) {
                saveState();
            }
            if (!result.missedBeats().isEmpty()) {
                sendCatchUp(player, result.missedBeats());
                // The narrator replays the voice line for the highest
                // missed stage, to this player only (v0.3.0). evaluateJoin
                // has already advanced the seen-stage, so this join is the
                // one and only delivery.
                narration.replaySound(result.missedBeats()).ifPresent(sound -> {
                    playNarrationSound(player, sound);
                    getLogger().info("Narration catch-up replay: " + sound
                            + " for " + player.getName() + ".");
                });
            }
        }, delaySeconds * 20L);
    }

    // ------------------------------------------------------------------
    // Narration (v0.3.0)
    // ------------------------------------------------------------------

    /**
     * The narrator's moment for a stage that just opened: the stage's
     * voice line plays for everyone online, then the stage's guide lines
     * are broadcast one at a time, staggered by the configured delay.
     *
     * Everything here is guarded: narration is garnish on the seal fall,
     * never load-bearing. A missing resource pack is invisible server-side
     * (the client simply has no such sound and stays silent), and any
     * unexpected failure is caught and logged so the fall path completes
     * exactly as it did before 0.3.0.
     */
    private void runNarrationMoment(int stage) {
        if (!narration.enabled()) {
            return;
        }
        try {
            narration.momentSound(stage).ifPresent(sound -> {
                int targets = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    playNarrationSound(p, sound);
                    targets++;
                }
                getLogger().info("Narration: voice line " + sound + " dispatched to "
                        + targets + " online player(s) (stage " + stage + ").");
            });
            List<String> guide = narration.guideLines(stage);
            long stepTicks = narration.guideLineDelaySeconds() * 20L;
            for (int i = 0; i < guide.size(); i++) {
                final String line = guide.get(i);
                final int index = i + 1;
                final int total = guide.size();
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Bukkit.broadcastMessage(color(line));
                    getLogger().info("Narration guide (stage " + stage + ", line "
                            + index + " of " + total + ") sent.");
                }, stepTicks * (i + 1));
            }
        } catch (Exception e) {
            getLogger().warning("Narration moment failed (stage " + stage + "): "
                    + e.getMessage());
        }
    }

    /**
     * Send one custom sound to one player. The string-keyed playsound is
     * a client-side lookup: an unknown or unloaded sound name can never
     * throw here in practice, but the guard keeps the promise that
     * narration cannot break a seal fall or a join.
     */
    private void playNarrationSound(Player player, String sound) {
        try {
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, 1.0f, 1.0f);
        } catch (Exception e) {
            getLogger().fine("Narration sound " + sound + " could not be sent to "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    /** The combined catch-up: one header, then one line per missed beat. */
    private void sendCatchUp(Player player, List<Integer> missedBeats) {
        player.sendMessage(color(msg("catchupHeader")
                .replace("{broken}", String.valueOf(state.brokenCount()))
                .replace("{total}", String.valueOf(state.requiredCount()))));
        for (int beat : missedBeats) {
            if (beat >= 1 && beat <= beats.size()) {
                player.sendMessage(color(beats.get(beat - 1).line()));
            }
        }
        getLogger().info("Catch-up sent to " + player.getName()
                + ": beats " + missedBeats + " (stage " + state.brokenCount() + ").");
    }

    // ------------------------------------------------------------------
    // The gate
    // ------------------------------------------------------------------

    private boolean gateActive() {
        return gateEnabled && !state.isUnlocked();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!gateActive()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("excelguildgate.bypass")) {
            return;
        }
        String body = event.getMessage();
        if (body.startsWith("/")) {
            body = body.substring(1);
        }
        String token = body.split("\\s+")[0].toLowerCase(Locale.ROOT);
        int colon = token.lastIndexOf(':');
        if (colon >= 0) {
            token = token.substring(colon + 1);
        }
        if (token.equals("ag") || token.equals("adventurersguild")) {
            event.setCancelled(true);
            notifyLocked(player, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!gateActive()) {
            return;
        }
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }
        if (!GUILD_WORLD.equals(to.getWorld().getName())) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("excelguildgate.bypass")) {
            return;
        }
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long last = lastGateNotice.get(player.getUniqueId());
        if (last == null || now - last >= GATE_NOTICE_THROTTLE_MS) {
            lastGateNotice.put(player.getUniqueId(), now);
            notifyLocked(player, false);
        }
    }

    /**
     * Locked-gate feedback: the one-line notice, then the quest view.
     *
     * openView=true (the /ag command path) opens the full quest view, the
     * exact renderer /seals uses: a Cumulus form for Bedrock players, the
     * chat panel for Java. If a form cannot be opened in the player's
     * current state (Geyser hiccup, form send failure), showSeals falls
     * back to the chat panel on its own, so the player always gets the
     * quest either way.
     *
     * openView=false (the teleport path) sends the chat panel only: a
     * teleport event can refire repeatedly (portals), and popping a form
     * from inside one is unreliable, so the notice plus panel is the safe
     * channel there. This path is throttled per player.
     */
    private void notifyLocked(Player player, boolean openView) {
        player.sendMessage(color(msg("lockedCommandMessage")));
        if (openView) {
            showSeals(player);
        } else {
            sendQuestPanel(player);
        }
    }

    // ------------------------------------------------------------------
    // Presentation: /seals quest page
    // ------------------------------------------------------------------

    /** Colored chat lines (Java panel and fallbacks). */
    private List<String> questLines() {
        List<String> lines = new ArrayList<>();
        lines.add(color(msg("lockedHeader")));
        for (SealDef def : defs.values()) {
            if (!def.enabled()) {
                continue;
            }
            if (state.isBroken(def.id())) {
                lines.add(color("&a✔ " + def.name()));
            } else {
                lines.add(color("&7✘ " + def.name() + ": &8" + def.hint()));
            }
        }
        if (state.isUnlocked()) {
            lines.add(color(msg("questFooterOpen")));
        } else {
            lines.add(color(msg("progressLine")
                    .replace("{broken}", String.valueOf(state.brokenCount()))
                    .replace("{total}", String.valueOf(state.requiredCount()))));
            lines.add(color(msg("questFooterLocked")));
        }
        return lines;
    }

    /** Plain-text lines for the Bedrock form body (no color codes). */
    private List<String> questLinesPlain() {
        List<String> lines = new ArrayList<>();
        for (SealDef def : defs.values()) {
            if (!def.enabled()) {
                continue;
            }
            if (state.isBroken(def.id())) {
                lines.add("✔ " + def.name() + " - broken");
            } else {
                lines.add("✘ " + def.name() + ": " + def.hint());
            }
        }
        if (state.isUnlocked()) {
            lines.add("");
            lines.add("The guild is open! Type /ag to visit.");
        } else {
            lines.add("");
            lines.add("Progress: " + state.brokenCount() + " of " + state.requiredCount()
                    + " seals broken.");
            lines.add("The guild opens when every seal is broken.");
        }
        return lines;
    }

    private void sendQuestPanel(Player player) {
        for (String line : questLines()) {
            player.sendMessage(line);
        }
    }

    private void showSeals(Player player) {
        if (GateForms.isBedrockPlayer(player, getLogger())) {
            String content = String.join("\n", questLinesPlain());
            boolean sent = GateForms.sendSimpleForm(player, msg("questTitle"), content,
                    new String[]{"Close"}, id -> { /* Close button: nothing to do */ },
                    getLogger());
            if (sent) {
                return;
            }
        }
        sendQuestPanel(player);
    }

    // ------------------------------------------------------------------
    // Commands
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("seals")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use /seals.");
                return true;
            }
            showSeals((Player) sender);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("guildgate")) {
            return guildGate(sender, args);
        }
        return false;
    }

    private boolean guildGate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("excelguildgate.admin")) {
            sender.sendMessage(color("&cYou do not have permission to use /guildgate."));
            return true;
        }
        if (args.length == 0) {
            sendAdminUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status":
                sendStatus(sender);
                return true;
            case "grant":
            case "revoke": {
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /guildgate " + args[0].toLowerCase(Locale.ROOT)
                            + " <seal>. Valid seals: " + String.join(", ", SEAL_ORDER)));
                    return true;
                }
                String sealId = args[1].toLowerCase(Locale.ROOT);
                if (!state.isKnown(sealId)) {
                    sender.sendMessage(color("&cUnknown seal '" + args[1]
                            + "'. Valid seals: " + String.join(", ", SEAL_ORDER)));
                    return true;
                }
                boolean changed;
                if (args[0].equalsIgnoreCase("grant")) {
                    boolean wasUnlocked = state.isUnlocked();
                    changed = state.grant(sealId, System.currentTimeMillis());
                    if (changed) {
                        saveState();
                        getLogger().info("[GuildGate] " + sender.getName() + " granted seal " + sealId
                                + " (" + state.brokenCount() + " of " + state.requiredCount() + ").");
                        if (!wasUnlocked && state.isUnlocked()) {
                            getLogger().info("All seals broken: the Adventurers Guild is now OPEN.");
                        }
                    }
                    sender.sendMessage(color(changed
                            ? "&aSeal granted: &f" + defs.get(sealId).name()
                            : "&7Seal already broken: &f" + defs.get(sealId).name()));
                } else {
                    changed = state.revoke(sealId);
                    if (changed) {
                        saveState();
                        getLogger().info("[GuildGate] " + sender.getName() + " revoked seal " + sealId
                                + " (" + state.brokenCount() + " of " + state.requiredCount() + ").");
                    }
                    sender.sendMessage(color(changed
                            ? "&aSeal revoked: &f" + defs.get(sealId).name() + " &7(the guild relocks if it was open)"
                            : "&7Seal was not broken: &f" + defs.get(sealId).name()));
                }
                sender.sendMessage(color("&7Guild is now &f" + (state.isUnlocked() ? "OPEN" : "LOCKED")
                        + " &7(" + state.brokenCount() + " of " + state.requiredCount() + " seals broken)."));
                return true;
            }
            case "reset": {
                state.reset();
                saveState();
                getLogger().info("[GuildGate] " + sender.getName()
                        + " reset the Road to the Guild. All seals restored, guild LOCKED.");
                sender.sendMessage(color("&aRoad to the Guild reset. All seals restored, guild is LOCKED."));
                return true;
            }
            case "open": {
                boolean changed = state.openAll(System.currentTimeMillis());
                saveState();
                getLogger().info("[GuildGate] " + sender.getName()
                        + " opened the guild with /guildgate open. All seals marked broken.");
                sender.sendMessage(color(changed
                        ? "&aAll seals marked broken. The guild is OPEN."
                        : "&7The guild was already open."));
                return true;
            }
            case "catchup":
                return guildGateCatchup(sender, args);
            case "narrate":
                return guildGateNarrate(sender, args);
            default:
                sendAdminUsage(sender);
                return true;
        }
    }

    /**
     * /guildgate narrate &lt;1-5&gt;: preview a stage's narration moment
     * (voice line + staggered guide) for everyone online, WITHOUT touching
     * the campaign: no seal is credited and no seen-stage moves. This is
     * how the narrator gets auditioned and how a stage's guide wording is
     * checked in game; the real moment fires on the celebrated fall path.
     * Console-usable (with nobody online, the guide still broadcasts and
     * the sound dispatch is logged at 0 targets).
     */
    private boolean guildGateNarrate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /guildgate narrate <stage 1-"
                    + NarrationGuide.STAGE_COUNT + ">"));
            return true;
        }
        int stage;
        try {
            stage = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cUsage: /guildgate narrate <stage 1-"
                    + NarrationGuide.STAGE_COUNT + ">"));
            return true;
        }
        if (stage < 1 || stage > NarrationGuide.STAGE_COUNT) {
            sender.sendMessage(color("&cStage must be 1-" + NarrationGuide.STAGE_COUNT + "."));
            return true;
        }
        if (!narration.enabled()) {
            sender.sendMessage(color("&7Narration is off (narration.enabled is false);"
                    + " nothing to preview."));
            return true;
        }
        getLogger().info("[GuildGate] " + sender.getName()
                + " triggered a narration preview for stage " + stage + ".");
        runNarrationMoment(stage);
        sender.sendMessage(color("&aNarration preview fired for stage &f" + stage
                + " &a(voice line + guide, everyone online). Campaign state untouched."));
        return true;
    }

    /**
     * /guildgate catchup &lt;player&gt; [reset]: show a player's seen-stage,
     * or set it to 0 so their next join replays every catch-up beat up to
     * the current stage. Console-usable (the sender only needs the admin
     * permission the whole command already requires).
     */
    private boolean guildGateCatchup(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /guildgate catchup <player> [reset]"));
            return true;
        }
        OfflinePlayer target = resolveKnownPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(color("&cNo player named '" + args[1]
                    + "' has been seen on this server."));
            return true;
        }
        String display = target.getName() != null ? target.getName() : args[1];
        int current = state.brokenCount();
        if (args.length >= 3) {
            if (!args[2].equalsIgnoreCase("reset")) {
                sender.sendMessage(color("&cUsage: /guildgate catchup <player> [reset]"));
                return true;
            }
            if (catchUp.setSeen(target.getUniqueId(), 0)) {
                saveState();
            }
            getLogger().info("[GuildGate] " + sender.getName() + " reset catch-up for "
                    + display + " to stage 0.");
            sender.sendMessage(color("&aCatch-up reset for &f" + display
                    + "&a: their next join replays "
                    + (current == 0
                            ? "nothing (the campaign is at stage 0)."
                            : "every beat up to stage &f" + current + "&a.")));
            return true;
        }
        Integer seenStage = catchUp.seenStage(target.getUniqueId());
        if (seenStage == null) {
            sender.sendMessage(color("&e" + display + " &7has no catch-up record yet; they start"
                    + " silently at the current stage (&f" + current + "&7)."));
        } else if (seenStage < current) {
            sender.sendMessage(color("&e" + display + " &7has been told through stage &f" + seenStage
                    + " &7of &f" + current + "&7; the missed beats arrive on their next join."));
        } else if (seenStage > current) {
            sender.sendMessage(color("&e" + display + " &7has been told through stage &f" + seenStage
                    + " &7, past the current &f" + current + " &7(a revoke dropped it); the mark"
                    + " clamps silently on their next join."));
        } else {
            sender.sendMessage(color("&e" + display + " &7has been told through stage &f" + seenStage
                    + " &7of &f" + current + "&7. Fully caught up."));
        }
        return true;
    }

    /**
     * Resolve a player name for the catch-up admin commands: an online
     * player first, then anyone who has played here before. A name that
     * has never been seen resolves to null (no phantom records under
     * made-up UUIDs).
     *
     * The last-resort route reads the server's own usercache.json:
     * current Paper does not enumerate cache-only players through
     * getOfflinePlayers(), and hasPlayedBefore() misses them too
     * (scratch-verified on Paper 26.2), while the vanilla whitelist —
     * which reads the same cache — resolves them fine. A cache entry is
     * only accepted with proof the player was actually here: a
     * playerdata or stats file, or an existing catch-up record.
     */
    private OfflinePlayer resolveKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() != null && p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        OfflinePlayer named = Bukkit.getOfflinePlayer(name);
        if (named.hasPlayedBefore() || named.isOnline()) {
            return named;
        }
        UUID cached = lookupUserCache(name);
        if (cached != null && (catchUp.seenStage(cached) != null || hasPlayedHere(cached))) {
            return Bukkit.getOfflinePlayer(cached);
        }
        return null;
    }

    private static final Pattern CACHE_NAME =
            Pattern.compile("\"name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern CACHE_UUID =
            Pattern.compile("\"uuid\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");

    /** Name to UUID from the server root's usercache.json, or null. */
    private UUID lookupUserCache(String name) {
        try {
            File cacheFile = new File(Bukkit.getWorldContainer(), "usercache.json");
            if (!cacheFile.isFile()) {
                return null;
            }
            String json = Files.readString(cacheFile.toPath());
            for (String entry : json.split("\\},\\s*\\{")) {
                Matcher nm = CACHE_NAME.matcher(entry);
                Matcher um = CACHE_UUID.matcher(entry);
                if (nm.find() && um.find()
                        && nm.group(1).equalsIgnoreCase(name)) {
                    return UUID.fromString(um.group(1));
                }
            }
        } catch (Exception e) {
            getLogger().fine("usercache.json lookup failed: " + e.getMessage());
        }
        return null;
    }

    /** True when a playerdata or stats file exists for the UUID. */
    private boolean hasPlayedHere(UUID id) {
        if (Bukkit.getWorlds().isEmpty()) {
            return false;
        }
        World first = Bukkit.getWorlds().get(0);
        // Playerdata lives at the level root (world container + world
        // name) in both the classic layout and Paper 26.2's
        // dimension-folder layout, where getWorldFolder() points into
        // dimensions/... instead; check both bases.
        File levelRoot = new File(Bukkit.getWorldContainer(), first.getName());
        for (File base : new File[]{levelRoot, first.getWorldFolder()}) {
            if (base != null && (new File(base, "playerdata/" + id + ".dat").isFile()
                    || new File(base, "stats/" + id + ".json").isFile())) {
                return true;
            }
        }
        return false;
    }

    private void sendAdminUsage(CommandSender sender) {
        sender.sendMessage(color("&6/guildgate &7- Road to the Guild admin"));
        sender.sendMessage(color("&e/guildgate status &7- seals and lock state"));
        sender.sendMessage(color("&e/guildgate grant <seal> &7- mark a seal broken"));
        sender.sendMessage(color("&e/guildgate revoke <seal> &7- un-break a seal"));
        sender.sendMessage(color("&e/guildgate reset &7- clear all seals and relock"));
        sender.sendMessage(color("&e/guildgate open &7- break all seals and open the guild (quiet)"));
        sender.sendMessage(color("&e/guildgate catchup <player> &7- show a player's catch-up stage"));
        sender.sendMessage(color("&e/guildgate catchup <player> reset &7- replay their catch-ups next join"));
        sender.sendMessage(color("&e/guildgate narrate <1-5> &7- preview a stage's narration (no state change)"));
        sender.sendMessage(color("&7Valid seals: &f" + String.join(", ", SEAL_ORDER)));
    }

    private void sendStatus(CommandSender sender) {
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, h:mm a", Locale.US);
        sender.sendMessage(color("&6— Road to the Guild: status —"));
        sender.sendMessage(color("&7Gate: &f" + (gateEnabled ? "enabled" : "DISABLED in config")
                + " &7| Guild: &f" + (state.isUnlocked() ? "OPEN" : "LOCKED")
                + " &7| Progress: &f" + state.brokenCount() + " of " + state.requiredCount()));
        if (state.isUnlocked() && state.unlockTimestampMillis() > 0L) {
            sender.sendMessage(color("&7Unlocked: &f" + fmt.format(new Date(state.unlockTimestampMillis()))));
        }
        for (SealDef def : defs.values()) {
            SealState.SealRecord rec = state.record(def.id());
            StringBuilder line = new StringBuilder();
            line.append(rec != null ? "&a✔ " : "&7✘ ");
            line.append(def.name()).append(" &8(").append(def.id()).append(")");
            if (!def.enabled()) {
                line.append(" &7- disabled in config, not required");
            }
            if (rec != null) {
                line.append(" &7- ").append(fmt.format(new Date(rec.timestampMillis())));
                if (rec.killer() != null) {
                    line.append(" by ").append(rec.killer());
                }
            }
            sender.sendMessage(color(line.toString()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("guildgate")) {
            return null;
        }
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : List.of("status", "grant", "revoke", "reset", "open", "catchup", "narrate")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("narrate")) {
            for (int s = 1; s <= NarrationGuide.STAGE_COUNT; s++) {
                if (String.valueOf(s).startsWith(args[1])) {
                    out.add(String.valueOf(s));
                }
            }
        } else if (args.length == 2
                && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke"))) {
            for (String id : SEAL_ORDER) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("catchup")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT)
                        .startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("catchup")) {
            if ("reset".startsWith(args[2].toLowerCase(Locale.ROOT))) {
                out.add("reset");
            }
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String msg(String key) {
        String fallback = switch (key) {
            case "questTitle" -> "The Road to the Guild";
            case "lockedCommandMessage" -> "&cThe Adventurers Guild is still sealed.";
            case "lockedHeader" -> "&6— The Road to the Guild —";
            case "sealBrokenLine1" -> "&6Seal broken: &f{flavor}";
            case "sealBrokenLine2" -> "&7Road to the Guild: &a{broken} of {total} &7seals broken.";
            case "unlockLine1" -> "&6&lThe Road to the Guild is complete!";
            case "unlockLine2" -> "&eEvery seal is broken. The Adventurers Guild is open! Type /ag to visit.";
            case "progressLine" -> "&7Progress: &a{broken} of {total} &7seals broken.";
            case "catchupHeader" -> "&6&lWhile you were away:";
            case "questFooterLocked" -> "&7The guild opens when every seal is broken.";
            case "questFooterOpen" -> "&aThe guild is open! &7Type &f/ag &7to visit.";
            default -> "";
        };
        return getConfig().getString("messages." + key, fallback);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
