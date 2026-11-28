package exceltestmode;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelTestMode — a damage floor for proof and test
 * sessions. A FLEET TESTING TOOL, not a gameplay
 * feature: it exists so a staged test character can
 * be measured under real combat without dying
 * mid-measurement. Plain Bukkit/Paper plugin; no
 * EliteMobs dependency of any kind.
 *
 * HISTORY: this code shipped for one evening as the
 * test-mode module inside ExcelEMFixes v31 (Nov 27,
 * 2026). Mike's ruling the same night: ExcelEMFixes
 * is the EliteMobs fix pack, and test mode fixes
 * nothing — it is a testing tool, so it does not
 * belong in the fix pack. ExcelEMFixes v32 removed
 * the module; this standalone plugin is its home
 * from v1 on. Behavior is identical to the module's.
 *
 * WHY IT EXISTS (the creation reason): on Nov 27,
 * 2026, an EliteMobs live-proof session staged the
 * server owner's own character against a boss to
 * measure the boss's combat-end heal; the character
 * DIED mid-test (10:42 PM), the session lane was
 * then cut by a scheduler timeout, and the account
 * had to be restored from baselines before it was
 * safe to use again. Mike's proposal the same
 * night: "create a test mode where I don't take as
 * much damage or maybe can't die but still take
 * damage while testing." A proof session's whole
 * value is real numbers — real hits, real combat
 * state, real heal timings — so the mode must not
 * soften the fight; it may only refuse the death.
 *
 * WHAT IT DOES: for each player on the list,
 * incoming damage registers FULLY and normally —
 * the damage event fires unmodified, game combat
 * state, aggro and heal mechanics engage exactly
 * as stock, outgoing damage is untouched — except
 * that the portion of a hit which would carry the
 * player below the configured health floor
 * (shipped 2.0 HP = one heart) is trimmed, so the
 * hit lands them exactly ON the floor. A listed
 * player can be hurt, never killed. A player
 * already at or below the floor takes 0-damage
 * hits (the events still fire; nothing is ever
 * cancelled) until they heal back above it.
 *
 * MECHANISM — the TrainingWheels shield bones (the
 * D208 per-player mitigation precedent), reused on
 * purpose: a per-player list plus a victim-axis
 * EntityDamageEvent handler that writes the
 * event's own damage, never a cancel, never a side
 * channel, so health, death, logs and every other
 * reader see one consistent number. Two deliberate
 * divergences from the shield's shape, both
 * consequences of the job: (1) the adjustment is a
 * FLOOR CLAMP, not a factor — damage that fits
 * above the floor passes byte-for-byte untouched,
 * only the lethal excess is trimmed (a percent
 * reduction was rejected: heal and combat proofs
 * measure real damage numbers, and scaling every
 * hit would corrupt the measurement); (2) the
 * handler runs at HIGHEST, not HIGH — the floor
 * must price the FINAL hit, after armour and
 * resistance have settled it, and before MONITOR
 * observers read the settled number. There is no
 * world-scope gate: the list is explicit operator
 * intent, toggled per player from the console —
 * the list IS the scope. VOID and SUICIDE are
 * excluded by shipped default (no floor holds the
 * void; /kill is an operator's own act),
 * configurable like the rest.
 *
 * CONTROL: /testmode &lt;player&gt; on|off toggles
 * a player (console or exceltestmode.admin),
 * /testmode status lists the floor and the listed
 * players, /testmode &lt;player&gt; reads one
 * player. Toggles write through to config.yml (the
 * players list, stored as UUIDs) so the list
 * survives restarts and stays the one source of
 * truth, and every toggle logs one line: player,
 * on/off, the floor in force, an ISO timestamp,
 * and who issued it. The plugin ships OFF with an
 * empty list (enabled: false): nobody is protected
 * until an operator enables it and lists them, and
 * removing the last player (or setting enabled:
 * false and restarting) returns everyone to stock
 * behavior exactly.
 */
public final class ExcelTestMode extends JavaPlugin
        implements Listener, CommandExecutor {

    private boolean active;

    private volatile double floorHp = TestModeMath.DEFAULT_FLOOR_HP;
    private Set<String> excludedCauses = Set.of("VOID", "SUICIDE");
    private volatile boolean debug;
    private final Set<UUID> testPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getConfig().getBoolean("enabled", false)) {
            getLogger().info("ExcelTestMode present but DISABLED "
                    + "(enabled: false in config.yml) - nobody is "
                    + "protected. This is a fleet testing tool, not "
                    + "a gameplay feature; enable it only for a test "
                    + "session.");
            return;
        }
        loadSettings();
        active = true;
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("testmode") != null) {
            getCommand("testmode").setExecutor(this);
        }
        getLogger().info("Test Mode ready - listed players take full damage "
                + "but cannot drop below " + fmt(floorHp) + " HP ("
                + testPlayers.size() + " player(s) listed). "
                + "Toggle with /testmode <player> on|off.");
    }

    @Override
    public void onDisable() {
        active = false;
        HandlerList.unregisterAll((Listener) this);
    }

    private void loadSettings() {
        floorHp = TestModeMath.sanitizeFloor(
                getConfig().getDouble("floor-hp", TestModeMath.DEFAULT_FLOOR_HP));
        excludedCauses = upperSet(getConfig().getStringList("excluded-causes"),
                Set.of("VOID", "SUICIDE"));
        debug = getConfig().getBoolean("debug", false);
        Set<UUID> loaded = new HashSet<>();
        for (String entry : getConfig().getStringList("players")) {
            UUID uuid = resolveEntry(entry);
            if (uuid != null) loaded.add(uuid);
        }
        testPlayers.clear();
        testPlayers.addAll(loaded);
    }

    private static Set<String> upperSet(List<String> values, Set<String> fallback) {
        if (values == null || values.isEmpty()) return fallback;
        Set<String> out = new HashSet<>();
        for (String v : values) {
            if (v != null && !v.isBlank()) out.add(v.trim().toUpperCase(Locale.ROOT));
        }
        return out.isEmpty() ? fallback : out;
    }

    /** A config list entry: a UUID string, or a player name. */
    private static UUID resolveEntry(String entry) {
        if (entry == null || entry.isBlank()) return null;
        String trimmed = entry.trim();
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException notUuid) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(trimmed);
            return offline == null ? null : offline.getUniqueId();
        }
    }

    // ------------------------------------------------------------------
    // The floor: victim-axis clamp at HIGHEST (TrainingWheels shape)
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!active) return;
        try {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!testPlayers.contains(player.getUniqueId())) return;
            GameMode mode = player.getGameMode();
            if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) return;
            if (excludedCauses.contains(event.getCause().name())) return;
            double health = player.getHealth();
            if (health <= 0.0) return;
            double absorption = player.getAbsorptionAmount();
            double finalDamage = event.getFinalDamage();
            if (!TestModeMath.wouldBreakFloor(health, absorption,
                    finalDamage, floorHp)) {
                return;
            }
            double allowed = TestModeMath.allowedDamage(health, absorption, floorHp);
            event.setDamage(TestModeMath.scaledBaseDamage(
                    event.getDamage(), finalDamage, allowed));
            // One settle pass: a non-proportional modifier can
            // leave the settled final a hair above the allowance;
            // scale once more on the settled ratio.
            double settled = event.getFinalDamage();
            if (settled > allowed) {
                event.setDamage(TestModeMath.scaledBaseDamage(
                        event.getDamage(), settled, allowed));
            }
            if (debug) {
                getLogger().info("TEST-MODE clamp player=" + player.getName()
                        + " cause=" + event.getCause()
                        + " finalBefore=" + fmt(finalDamage)
                        + " allowed=" + fmt(allowed)
                        + " floor=" + fmt(floorHp)
                        + " world=" + player.getWorld().getName());
            }
        } catch (Throwable failure) {
            logThrottled(failure);
        }
    }

    // ------------------------------------------------------------------
    // /testmode — console / operator control
    // ------------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (sender instanceof Player
                && !sender.hasPermission("exceltestmode.admin")) {
            sender.sendMessage("§cYou do not have permission to use test mode.");
            return true;
        }
        if (args.length == 0
                || (args.length == 1 && args[0].equalsIgnoreCase("status"))) {
            sendStatus(sender);
            return true;
        }
        if (args.length == 1) {
            sendPlayerStatus(sender, args[0]);
            return true;
        }
        if (args.length == 2) {
            String verb = args[1].toLowerCase(Locale.ROOT);
            if (verb.equals("on") || verb.equals("off")) {
                toggle(sender, args[0], verb.equals("on"));
                return true;
            }
        }
        sender.sendMessage("§eUsage: /testmode <player> on|off"
                + " — /testmode status — /testmode <player>");
        return true;
    }

    private void toggle(CommandSender sender, String nameArg, boolean on) {
        OfflinePlayer target = resolveTarget(nameArg);
        if (target == null || target.getName() == null) {
            sender.sendMessage("§cTest Mode: no such player '" + nameArg + "'.");
            return;
        }
        UUID uuid = target.getUniqueId();
        boolean changed = on ? testPlayers.add(uuid) : testPlayers.remove(uuid);
        if (!changed) {
            sender.sendMessage("§eTest Mode: " + target.getName()
                    + " is already " + (on ? "ON" : "OFF") + ".");
            return;
        }
        persistPlayers();
        getLogger().info("TEST-MODE " + (on ? "ON" : "OFF")
                + ": player=" + target.getName()
                + " uuid=" + uuid
                + " floor=" + fmt(floorHp) + " HP"
                + " at=" + Instant.now()
                + " by=" + sender.getName());
        sender.sendMessage("§aTest Mode " + (on ? "ON" : "OFF") + " for "
                + target.getName() + " (floor " + fmt(floorHp) + " HP).");
        Player online = target.getPlayer();
        if (online != null && online != sender) {
            online.sendMessage(on
                    ? "§eTest Mode is ON for you: you take full damage "
                            + "but cannot drop below " + fmt(floorHp) + " HP."
                    : "§eTest Mode is OFF for you: damage behaves as stock.");
        }
    }

    /** A command argument: a UUID string, an online name, or a known name. */
    private static OfflinePlayer resolveTarget(String arg) {
        if (arg == null || arg.isBlank()) return null;
        String trimmed = arg.trim();
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(trimmed));
        } catch (IllegalArgumentException notUuid) {
            Player online = Bukkit.getPlayerExact(trimmed);
            if (online != null) return online;
            return Bukkit.getOfflinePlayer(trimmed);
        }
    }

    /** Write-through: the config list is the one source of truth. */
    private void persistPlayers() {
        try {
            List<String> entries = new ArrayList<>();
            for (UUID uuid : testPlayers) entries.add(uuid.toString());
            entries.sort(String::compareTo);
            getConfig().set("players", entries);
            saveConfig();
        } catch (Throwable failure) {
            getLogger().warning("Test Mode: toggle applied in memory but "
                    + "the config write-through failed (" + failure + "); "
                    + "the list will revert on restart.");
        }
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("§eTest Mode — floor " + fmt(floorHp) + " HP, "
                + testPlayers.size() + " player(s) listed.");
        for (UUID uuid : testPlayers) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName() != null ? offline.getName() : uuid.toString();
            sender.sendMessage("§7- " + name + " (" + uuid + ")"
                    + (offline.isOnline() ? " — online now" : ""));
        }
        if (testPlayers.isEmpty()) {
            sender.sendMessage("§7Nobody is protected. "
                    + "/testmode <player> on lists a player.");
        }
    }

    private void sendPlayerStatus(CommandSender sender, String nameArg) {
        OfflinePlayer target = resolveTarget(nameArg);
        if (target == null || target.getName() == null) {
            sender.sendMessage("§cTest Mode: no such player '" + nameArg + "'.");
            return;
        }
        boolean on = testPlayers.contains(target.getUniqueId());
        sender.sendMessage("§eTest Mode: " + target.getName() + " is "
                + (on ? "ON (floor " + fmt(floorHp) + " HP)" : "OFF (not listed)")
                + ".");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private long lastFailureLogNanos;

    private void logThrottled(Throwable failure) {
        long now = System.nanoTime();
        if (now - lastFailureLogNanos < 60_000_000_000L) return;
        lastFailureLogNanos = now;
        getLogger().warning("Test Mode: failure (" + failure.getClass().getSimpleName()
                + ": " + failure.getMessage() + "); plugin continues.");
    }
}
