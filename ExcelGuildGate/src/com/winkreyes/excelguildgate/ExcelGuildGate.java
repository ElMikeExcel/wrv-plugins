package com.winkreyes.excelguildgate;

import com.winkreyes.guildgate.api.GuildGateAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Raid;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
 */
public class ExcelGuildGate extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    static final String GUILD_WORLD = "em_adventurers_guild";
    static final List<String> SEAL_ORDER = List.of(
            "ender_dragon", "wither", "elder_guardian", "warden", "raid");
    private static final long GATE_NOTICE_THROTTLE_MS = 5000L;

    private record SealDef(String id, boolean enabled, String name, String hint, String flavor) {}

    private final Map<String, SealDef> defs = new LinkedHashMap<>();
    private SealState state;
    private boolean gateEnabled = true;
    private final Map<UUID, Long> lastGateNotice = new HashMap<>();
    private GuildGateExpansion expansion;

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDefs();
        Set<String> enabled = new LinkedHashSet<>();
        for (SealDef d : defs.values()) {
            if (d.enabled()) {
                enabled.add(d.id());
            }
        }
        state = new SealState(new LinkedHashSet<>(defs.keySet()), enabled);
        loadState();
        gateEnabled = getConfig().getBoolean("gateEnabled", true);
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
            default:
                sendAdminUsage(sender);
                return true;
        }
    }

    private void sendAdminUsage(CommandSender sender) {
        sender.sendMessage(color("&6/guildgate &7- Road to the Guild admin"));
        sender.sendMessage(color("&e/guildgate status &7- seals and lock state"));
        sender.sendMessage(color("&e/guildgate grant <seal> &7- mark a seal broken"));
        sender.sendMessage(color("&e/guildgate revoke <seal> &7- un-break a seal"));
        sender.sendMessage(color("&e/guildgate reset &7- clear all seals and relock"));
        sender.sendMessage(color("&e/guildgate open &7- break all seals and open the guild (quiet)"));
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
            for (String sub : List.of("status", "grant", "revoke", "reset", "open")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2
                && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke"))) {
            for (String id : SEAL_ORDER) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
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
