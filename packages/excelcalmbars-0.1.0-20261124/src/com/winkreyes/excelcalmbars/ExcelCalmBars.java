package com.winkreyes.excelcalmbars;

import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelCalmBars — holds XP-gain boss bars back while a player is in
 * combat, releasing them once the player has been out of combat for
 * the configured quiet period (Mike's ruling, Nov 24, 2026: 30 s,
 * config value; tracker row 92 — Marvin's report that the stacked
 * Jobs / mcMMO / EliteMobs XP bars block his view in back-to-back
 * fights).
 *
 * How it works:
 * - Combat is tracked from damage events (dealt or taken), the same
 *   time-since-last-damage pattern as the fleet's out-of-combat heal.
 * - A per-connection packet gate (see GateHandler + BossBridge)
 *   watches clientbound boss-bar packets. A bar is held ONLY when
 *   its title positively matches a configured XP-bar pattern
 *   (Jobs / mcMMO / EliteMobs skill bars) — real boss bars, quest
 *   bars, the ExcelDeathProtection status bar, and anything
 *   unidentifiable always pass. REMOVE packets always pass.
 * - When combat starts, XP bars already on screen are retracted so
 *   the view is clear for the fight itself, not just for later pops.
 * - Held bars are released at the end of the quiet period, or at
 *   the max-hold cap, or immediately on death / world change /
 *   logout / plugin disable — a held bar is never lost. Every
 *   failure path fails OPEN: bars show, exactly as without the
 *   plugin.
 */
public final class ExcelCalmBars extends JavaPlugin
        implements Listener {

    static final String HANDLER_NAME = "excelcalmbars_gate";
    private static final int MAX_RAW_PACKETS = 32;

    private volatile boolean gateEnabled;
    private volatile long quietMillis;
    private volatile long maxHoldMillis;
    private volatile boolean debug;
    private volatile BarPatterns patterns;

    private final Map<UUID, PlayerBars> states =
            new ConcurrentHashMap<>();
    private final Map<UUID, Channel> channels =
            new ConcurrentHashMap<>();

    // ------------------------------------------------------------ life

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        if (!BossBridge.available()) {
            getLogger().warning("Boss-bar packet bridge unavailable "
                    + "on this server build — ExcelCalmBars is idle "
                    + "(fail-open: all bars pass through).");
        }
        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            states.put(player.getUniqueId(),
                    new PlayerBars(player.getUniqueId()));
            inject(player);
        }
        getServer().getScheduler().runTaskTimer(
                this, this::tickFlush, 5L, 5L);
        getLogger().info("ExcelCalmBars 0.1.0 ready — quiet period "
                + (quietMillis / 1000) + "s, max hold "
                + (maxHoldMillis / 1000) + "s, bridge "
                + (BossBridge.available() ? "ok" : "UNAVAILABLE"));
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerBars bars = states.get(player.getUniqueId());
            if (bars != null) flushAll(player, bars, "disable");
            uninject(player.getUniqueId());
        }
        states.clear();
        channels.clear();
    }

    private void loadSettings() {
        gateEnabled = getConfig().getBoolean("enabled", true);
        quietMillis = Math.max(1,
                getConfig().getLong("quiet-period-seconds", 30)) * 1000L;
        maxHoldMillis = Math.max(5,
                getConfig().getLong("max-hold-seconds", 120)) * 1000L;
        debug = getConfig().getBoolean("debug-logging", false);
        patterns = new BarPatterns(getConfig());
    }

    // ------------------------------------------------------------ gate

    /**
     * The gate decision, called from Netty event-loop threads.
     * Returns true when the packet is held back. Every failure and
     * every unidentifiable case returns false (fail open).
     */
    private final java.util.concurrent.atomic.AtomicBoolean
            tryHoldErrorLogged =
            new java.util.concurrent.atomic.AtomicBoolean();

    public boolean tryHold(PlayerBars bars, Object packet) {
        synchronized (bars) {
            bars.seenPackets++;
        }
        if (!gateEnabled || !BossBridge.available()) return false;
        try {
            String type = BossBridge.opType(packet);
            UUID barId = BossBridge.barId(packet);
            synchronized (bars) {
                if ("REMOVE".equals(type)) {
                    bars.bars.remove(barId);
                    return false; // REMOVE always passes
                }
                boolean fresh = !bars.bars.containsKey(barId);
                BarState state = bars.bars.computeIfAbsent(
                        barId, BarState::new);
                BossBridge.merge(packet, type, state);
                long now = System.currentTimeMillis();
                boolean holding = bars.lastCombatMillis > 0
                        && now - bars.lastCombatMillis < quietMillis;
                if (holding && patterns.isXpBar(state.plainTitle)) {
                    if (!state.held) {
                        state.held = true;
                        state.heldSinceMillis = now;
                    }
                    if (state.rawPackets.size() < MAX_RAW_PACKETS) {
                        state.rawPackets.add(packet);
                    }
                    debug("held " + type + " '" + state.plainTitle
                            + "' for " + bars.playerId);
                    return true;
                }
                if ("ADD".equals(type)) {
                    state.clientHas = true;
                } else if (fresh) {
                    // First contact is an update for a bar this
                    // connection never showed us being added (e.g.
                    // the join-time sync that predates injection):
                    // the server only updates bars the client has.
                    state.clientHas = true;
                }
                return false;
            }
        } catch (Throwable t) {
            if (tryHoldErrorLogged.compareAndSet(false, true)) {
                getLogger().warning("[calmbars] tryHold failed open "
                        + "(further failures silent): " + t);
            }
            return false; // fail open
        }
    }

    /** Marks combat for a player and retracts any XP bars already
     *  on their screen, so the fight itself starts with a clear
     *  view. Called on the main thread from the damage listener. */
    private void markCombat(Player player) {
        PlayerBars bars = states.get(player.getUniqueId());
        if (bars == null) return;
        long now = System.currentTimeMillis();
        synchronized (bars) {
            bars.lastCombatMillis = now;
            if (!gateEnabled || !BossBridge.available()) return;
            for (BarState state : bars.bars.values()) {
                if (!state.clientHas || state.held) continue;
                if (!patterns.isXpBar(state.plainTitle)) continue;
                Object remove = BossBridge.buildRemove(state.id);
                if (remove != null && BossBridge.send(player, remove)) {
                    state.clientHas = false;
                    state.held = true;
                    state.heldSinceMillis = now;
                    debug("retracted '" + state.plainTitle
                            + "' for " + player.getName());
                }
                // If the retract send fails the bar simply stays
                // visible — fail-open, no state change.
            }
        }
    }

    /** Releases one held bar to the client. Caller holds the
     *  PlayerBars lock. */
    private void flushBar(Player player, BarState state, String why) {
        List<Object> toSend = new ArrayList<>();
        if (!state.clientHas) {
            Object add = BossBridge.buildAdd(state);
            if (add != null) {
                toSend.add(add);
            } else {
                toSend.addAll(state.rawPackets);
            }
        } else {
            List<Object> updates = BossBridge.buildUpdates(state);
            if (updates != null) {
                toSend.addAll(updates);
            } else {
                toSend.addAll(state.rawPackets);
            }
        }
        for (Object packet : toSend) {
            BossBridge.send(player, packet);
        }
        debug("released '" + state.plainTitle + "' to "
                + player.getName() + " (" + why + ", "
                + toSend.size() + " packets)");
        state.held = false;
        state.heldSinceMillis = 0L;
        state.rawPackets.clear();
        state.clientHas = true;
    }

    private void flushAll(Player player, PlayerBars bars, String why) {
        synchronized (bars) {
            for (BarState state : bars.bars.values()) {
                if (state.held) flushBar(player, state, why);
            }
        }
    }

    /** Periodic release driver: quiet-period end and max-hold cap. */
    private void tickFlush() {
        if (!gateEnabled || !BossBridge.available()) return;
        long now = System.currentTimeMillis();
        for (PlayerBars bars : states.values()) {
            Player player = Bukkit.getPlayer(bars.playerId);
            if (player == null) continue;
            synchronized (bars) {
                boolean quietOver = bars.lastCombatMillis == 0
                        || now - bars.lastCombatMillis >= quietMillis;
                for (BarState state : bars.bars.values()) {
                    if (!state.held) continue;
                    boolean capped = state.heldSinceMillis > 0
                            && now - state.heldSinceMillis
                                    >= maxHoldMillis;
                    if (quietOver || capped) {
                        flushBar(player, state,
                                capped && !quietOver
                                        ? "max-hold cap"
                                        : "quiet period over");
                    }
                }
            }
        }
    }

    // ------------------------------------------------------- injection

    private void inject(Player player) {
        Channel channel = BossBridge.channelOf(player);
        if (channel == null) {
            // Channel not ready yet (very early join): one retry.
            getServer().getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) injectNow(player);
            }, 20L);
            return;
        }
        injectNow(player);
    }

    private void injectNow(Player player) {
        PlayerBars bars = states.computeIfAbsent(
                player.getUniqueId(),
                id -> new PlayerBars(id));
        Channel channel = BossBridge.channelOf(player);
        if (channel == null) return;
        channels.put(player.getUniqueId(), channel);
        Object connection = BossBridge.connectionOf(player);
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(HANDLER_NAME) != null) return;
                GateHandler handler = new GateHandler(this, bars);
                // Anchor: the pipeline entry holding the server's
                // Connection object (the packet handler). Outbound
                // writes start there and flow toward the head, so the
                // gate must sit on the head side of it — addBefore.
                // Fallbacks keep the same head-side property: before
                // "packet_handler", else before the packet encoder.
                String anchor = null;
                for (java.util.Map.Entry<String, io.netty.channel.ChannelHandler> e
                        : channel.pipeline()) {
                    Object h = e.getValue();
                    if (h == connection
                            || h.getClass().getName().equals(
                                    "net.minecraft.network.Connection")) {
                        anchor = e.getKey();
                        break;
                    }
                }
                if (anchor == null
                        && channel.pipeline().get("packet_handler") != null) {
                    anchor = "packet_handler";
                }
                if (anchor == null
                        && channel.pipeline().get("encoder") != null) {
                    anchor = "encoder";
                }
                if (anchor != null) {
                    channel.pipeline().addBefore(anchor, HANDLER_NAME, handler);
                } else {
                    channel.pipeline().addLast(HANDLER_NAME, handler);
                }
                getLogger().info("[calmbars] gate injected for "
                        + player.getName() + " before "
                        + (anchor == null ? "<tail>" : anchor)
                        + " — pipeline: " + channel.pipeline().names());
            } catch (Throwable t) {
                // Fail-open (the player simply has no gate), but LOUD:
                // a silently ungated player is indistinguishable from
                // a working one in every other signal.
                getLogger().warning("[calmbars] gate injection failed for "
                        + player.getName() + " (fail-open): " + t);
            }
        });
    }

    private void uninject(UUID playerId) {
        Channel channel = channels.remove(playerId);
        if (channel == null) return;
        channel.eventLoop().execute(() -> {
            try {
                if (channel.pipeline().get(HANDLER_NAME) != null) {
                    channel.pipeline().remove(HANDLER_NAME);
                }
            } catch (Throwable t) {
                // connection already gone — nothing to clean
            }
        });
    }

    // ---------------------------------------------------------- events

    @EventHandler(priority = EventPriority.MONITOR,
            ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            markCombat(damager);
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            markCombat(shooter);
        }
        if (event.getEntity() instanceof Player victim) {
            markCombat(victim);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        states.put(event.getPlayer().getUniqueId(),
                new PlayerBars(event.getPlayer().getUniqueId()));
        inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
        uninject(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerBars bars = states.get(player.getUniqueId());
        if (bars == null) return;
        synchronized (bars) {
            bars.lastCombatMillis = 0L;
        }
        flushAll(player, bars, "death");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerBars bars = states.get(player.getUniqueId());
        if (bars == null) return;
        synchronized (bars) {
            bars.lastCombatMillis = 0L;
        }
        flushAll(player, bars, "world change");
    }

    // --------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("excelcalmbars.admin")) {
                sender.sendMessage("No permission.");
                return true;
            }
            reloadConfig();
            loadSettings();
            sender.sendMessage("ExcelCalmBars reloaded — quiet period "
                    + (quietMillis / 1000) + "s, max hold "
                    + (maxHoldMillis / 1000) + "s, gate "
                    + (gateEnabled ? "on" : "off") + ".");
            return true;
        }
        if (sender instanceof Player player) {
            PlayerBars bars = states.get(player.getUniqueId());
            if (args.length == 1
                    && args[0].equalsIgnoreCase("flush")) {
                if (bars != null) {
                    flushAll(player, bars, "command");
                    sender.sendMessage(
                            "Held bars released.");
                }
                return true;
            }
            // status (default)
            if (bars == null) {
                sender.sendMessage("ExcelCalmBars: no state yet.");
                return true;
            }
            synchronized (bars) {
                long held = bars.bars.values().stream()
                        .filter(s -> s.held).count();
                long sinceCombat = bars.lastCombatMillis == 0
                        ? -1
                        : (System.currentTimeMillis()
                                - bars.lastCombatMillis) / 1000;
                sender.sendMessage("ExcelCalmBars — bridge "
                        + (BossBridge.available() ? "ok" : "DOWN")
                        + ", gate " + (gateEnabled ? "on" : "off")
                        + ", boss packets seen: " + bars.seenPackets
                        + ", bars tracked: " + bars.bars.size()
                        + ", held: " + held
                        + ", last combat: "
                        + (sinceCombat < 0 ? "none"
                                            : sinceCombat + "s ago")
                        + ", quiet period: " + (quietMillis / 1000)
                        + "s.");
            }
            return true;
        }
        sender.sendMessage(
                "ExcelCalmBars — use /calmbars reload from console.");
        return true;
    }

    private void debug(String message) {
        if (debug) getLogger().info("[calmbars] " + message);
    }
}
