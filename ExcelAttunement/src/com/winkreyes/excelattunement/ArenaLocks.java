package com.winkreyes.excelattunement;

import com.winkreyes.excelattunement.core.ArenaLock;
import com.winkreyes.excelattunement.core.WardNotice;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntBiFunction;

/**
 * 0.1.6 arena locks (Mike, Oct 24, 2026): some Magic (elmakers)
 * movement spells are sealed near the Ender Dragon while she lives.
 * See core.ArenaLock for the rules; this is the Bukkit side.
 *
 * Dragon state per world (cached per server tick) is ALIVE when ANY of:
 *  - a live (not dead) EnderDragon entity is in the world (a dying
 *    one in her death flight still counts; covers /summon too),
 *  - the world's DragonBattle has a live linked dragon,
 *  - the DragonBattle respawn ritual is running (getRespawnPhase() !=
 *    NONE: the crystal re-summon gap, before the new dragon exists),
 *  - the DragonBattle boss bar is visible (vanilla shows it every tick
 *    while the fight's dragonKilled flag is false, so an unloaded
 *    dragon still counts, and so does her death animation),
 *  - the DragonBattle has never been won (first fight, dragon not
 *    spawned or not loaded yet).
 * A signal that throws is unreadable; with no live signal that makes
 * the state UNKNOWN, which keeps the lock (block-if-dragon-unknown).
 * ExcelBossTrial's two-phase flip heals the SAME dragon entity onto its
 * phase-2 bar (the phase-1 death is cancelled), so the lock never sees
 * a gap there. No other plugin is read or changed.
 *
 * Cast blocking is in MagicBridge (PreCastEvent, LOW): a cancelled
 * PreCastEvent costs no mana and starts no cooldown.
 */
final class ArenaLocks implements Listener {
    /** Kept from the seal-lock draft (same node, same meaning: ignore these locks). */
    static final String BYPASS = "excelattunement.seallock.bypass";

    private final ExcelAttunement plugin;
    private final boolean enabled;
    private final boolean blockIfUnknown;
    private final long noticeCooldown;
    private final int checkTicks;
    private final ArenaLock.Zone zone;
    private final List<ArenaLock.Rule> rules = new ArrayList<>();
    private final boolean rulesFromDefaults;
    /** Per player, per rule (one rule per spell by default): a Glide line does not swallow the Blink one. */
    private final Map<UUID, Map<String, WardNotice>> notices = new HashMap<>();
    private final Map<String, ArenaLock.DragonState> stateCache = new HashMap<>();
    private final Map<String, String> lastLoggedState = new HashMap<>();
    private int stateCacheTick = Integer.MIN_VALUE;
    private ToIntBiFunction<Player, Set<String>> spellStopper = (p, s) -> 0;
    private int castsBlocked, targetsBlocked, stops;
    private boolean unknownWarned;

    ArenaLocks(ExcelAttunement plugin, ConfigurationSection c) {
        this.plugin = plugin;
        this.enabled = c == null || c.getBoolean("enabled", true);
        this.blockIfUnknown = c == null || c.getBoolean("block-if-dragon-unknown", true);
        this.noticeCooldown = c == null ? 3L : c.getLong("notice-cooldown-seconds", 3L);
        this.checkTicks = (int) Math.max(1L, Math.min(40L, c == null ? 5L : c.getLong("check-ticks", 5L)));
        ConfigurationSection z = c == null ? null : c.getConfigurationSection("zone");
        if (z == null) {
            zone = ArenaLock.Zone.defaults();
        } else {
            List<String> envs = z.isSet("environments") ? z.getStringList("environments") : List.of("THE_END");
            zone = new ArenaLock.Zone(new LinkedHashSet<>(z.getStringList("worlds")), new LinkedHashSet<>(envs),
                    z.getDouble("center-x", 0.0), z.getDouble("center-z", 0.0),
                    z.getDouble("radius", ArenaLock.DEFAULT_RADIUS));
        }
        ConfigurationSection rs = c == null || !c.isSet("rules") ? null : c.getConfigurationSection("rules");
        if (rs == null) {
            // Missing section or missing rules key (live config not merged yet) = the shipped default rules.
            rules.addAll(ArenaLock.defaults());
            rulesFromDefaults = true;
        } else {
            rulesFromDefaults = false;
            for (String id : rs.getKeys(false)) {
                ArenaLock.Rule rule;
                if (rs.isConfigurationSection(id)) {
                    ConfigurationSection r = rs.getConfigurationSection(id);
                    if (!r.getBoolean("enabled", true)) continue;
                    List<String> spells = r.isSet("spells") ? r.getStringList("spells") : List.of(id);
                    rule = new ArenaLock.Rule(id, new LinkedHashSet<>(spells),
                            r.getString("message", ArenaLock.defaultMessage(id)),
                            r.getBoolean("stop-running", false), r.getBoolean("protect-targets-in-zone", false));
                } else if (rs.isBoolean(id) && !rs.getBoolean(id)) {
                    continue; // "dash: false" = listed but off
                } else {
                    rule = ArenaLock.simple(id); // "dash: {}" / "dash: true" / "dash:" = one-line add
                }
                if (rule.usable()) rules.add(rule);
                else plugin.getLogger().warning("Arena lock '" + id + "' ignored: no spells.");
            }
        }
    }

    void setSpellStopper(ToIntBiFunction<Player, Set<String>> stopper) {
        this.spellStopper = stopper;
    }

    boolean enabled() { return enabled && !rules.isEmpty(); }

    int checkTicks() { return checkTicks; }

    private static String env(World w) {
        return w == null ? null : w.getEnvironment().name();
    }

    // ---------- dragon state ----------

    ArenaLock.DragonState dragonState(World w) {
        if (w == null) return ArenaLock.DragonState.NONE;
        int tick = Bukkit.getCurrentTick();
        if (tick != stateCacheTick) {
            stateCache.clear();
            stateCacheTick = tick;
        }
        ArenaLock.DragonState s = stateCache.get(w.getName());
        if (s == null) {
            s = readDragonState(w);
            stateCache.put(w.getName(), s);
            String was = lastLoggedState.put(w.getName(), s.name());
            if (plugin.debug() && !s.name().equals(was)) {
                plugin.getLogger().info("ARENALOCK dragon state in " + w.getName() + ": " + s
                        + (was == null ? "" : " (was " + was + ")"));
            }
        }
        return s;
    }

    private ArenaLock.DragonState readDragonState(World w) {
        Boolean entity, linked = Boolean.FALSE, respawning = Boolean.FALSE, bar = Boolean.FALSE, neverWon = Boolean.FALSE;
        try {
            entity = Boolean.FALSE;
            for (EnderDragon d : w.getEntitiesByClass(EnderDragon.class)) {
                if (!d.isDead()) { entity = Boolean.TRUE; break; }
            }
        } catch (Throwable t) {
            entity = null;
            warnUnknown(w, "entity scan", t);
        }
        DragonBattle b;
        try {
            b = w.getEnderDragonBattle();
        } catch (Throwable t) {
            warnUnknown(w, "dragon battle", t);
            return Boolean.TRUE.equals(entity) ? ArenaLock.DragonState.ALIVE : ArenaLock.DragonState.UNKNOWN;
        }
        if (b != null) {
            try {
                EnderDragon d = b.getEnderDragon();
                linked = d != null && !d.isDead();
            } catch (Throwable t) { linked = null; warnUnknown(w, "battle dragon", t); }
            try {
                DragonBattle.RespawnPhase ph = b.getRespawnPhase();
                respawning = ph != null && ph != DragonBattle.RespawnPhase.NONE;
            } catch (Throwable t) { respawning = null; warnUnknown(w, "respawn phase", t); }
            try {
                bar = b.getBossBar() != null && b.getBossBar().isVisible();
            } catch (Throwable t) { bar = null; warnUnknown(w, "boss bar", t); }
            try {
                neverWon = !b.hasBeenPreviouslyKilled();
            } catch (Throwable t) { neverWon = null; warnUnknown(w, "previously killed", t); }
        }
        return ArenaLock.decide(entity, linked, respawning, bar, neverWon);
    }

    private void warnUnknown(World w, String what, Throwable t) {
        if (unknownWarned) return;
        unknownWarned = true;
        plugin.getLogger().warning("Arena locks: cannot read the dragon " + what + " in " + w.getName() + " ("
                + t.getClass().getSimpleName() + "); unreadable means "
                + (blockIfUnknown ? "locked (block-if-dragon-unknown)." : "open (block-if-dragon-unknown: false)."));
    }

    // ---------- zone ----------

    /** Is this location inside an active arena zone right now? */
    boolean activeAt(Location l) {
        if (!enabled || l == null || l.getWorld() == null) return false;
        World w = l.getWorld();
        if (!zone.contains(w.getName(), env(w), l.getX(), l.getZ())) return false; // cheap check first
        return ArenaLock.activeAt(true, zone, w.getName(), env(w), l.getX(), l.getZ(), dragonState(w), blockIfUnknown);
    }

    // ---------- casts ----------

    /** MagicBridge asks: the rule that blocks this caster's cast where they stand, or null. */
    ArenaLock.Rule blocking(Player p, String spellKey) {
        if (!enabled || p == null) return null;
        boolean bypass = p.hasPermission(BYPASS);
        if (bypass) return null;
        // Only look at the dragon when the spell is locked at all.
        if (ArenaLock.firstBlocking(true, rules, spellKey, true, false) == null) return null;
        return ArenaLock.firstBlocking(true, rules, spellKey, activeAt(p.getLocation()), false);
    }

    /** MagicBridge asks: does this spell protect in-zone players from being its target (Lift)? */
    ArenaLock.Rule targetRule(Player caster, String spellKey) {
        if (!enabled || caster == null) return null;
        return ArenaLock.targetRule(true, rules, spellKey, caster.hasPermission(BYPASS));
    }

    /** Is this target protected from that rule's spell right now? */
    boolean protectsTarget(ArenaLock.Rule r, Player caster, Entity target) {
        if (r == null || target == null) return false;
        boolean isPlayer = target instanceof Player;
        return ArenaLock.blocksTarget(r, isPlayer, caster != null && target.equals(caster),
                isPlayer && activeAt(target.getLocation()));
    }

    void onCastBlocked(Player p, String key, ArenaLock.Rule r) {
        castsBlocked++;
        if (plugin.debug()) plugin.getLogger().info("ARENALOCK blocked cast '" + key + "' for " + p.getName()
                + " in " + p.getWorld().getName() + " (rule " + r.id() + ", in the dragon's zone)");
        notice(p, r);
    }

    void onTargetBlocked(Player p, String key, ArenaLock.Rule r, Entity target, String how) {
        targetsBlocked++;
        if (plugin.debug()) plugin.getLogger().info("ARENALOCK blocked target '" + key + "' by " + p.getName()
                + " on " + target.getName() + " (rule " + r.id() + ", target in the dragon's zone, " + how + ")");
        notice(p, r);
    }

    private void notice(Player p, ArenaLock.Rule r) {
        WardNotice n = notices.computeIfAbsent(p.getUniqueId(), id -> new HashMap<>())
                .computeIfAbsent(r.id(), id -> new WardNotice(noticeCooldown));
        if (!r.message().isBlank() && n.take(plugin.now())) {
            Component line = Component.text(r.message(), NamedTextColor.LIGHT_PURPLE);
            p.sendMessage(line);
            p.sendActionBar(line);
            // Magic shows its own "spell was cancelled" on the action bar right after a cancelled cast;
            // re-send one tick later so the arena message is the one that stays visible.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) p.sendActionBar(line);
            }, 1L);
        }
    }

    // ---------- running spells on entry ----------

    /** Stop running stop-running spells (Glide) for a player standing in an active zone. */
    void checkPlayer(Player p) {
        if (!enabled || p == null || !p.isOnline()) return;
        Set<String> keys = ArenaLock.stopOnEntry(true, rules, p.hasPermission(BYPASS));
        if (keys.isEmpty() || !activeAt(p.getLocation())) return;
        try {
            int n = spellStopper.applyAsInt(p, keys);
            stops += n;
        } catch (Throwable t) {
            plugin.getLogger().warning("Arena locks: stopping " + keys + " for " + p.getName() + " failed: " + t);
        }
    }

    /** Every check-ticks: players in a zone world (the zone can switch on under them when a dragon spawns). */
    void tick() {
        if (!enabled()) return;
        if (ArenaLock.stopOnEntry(true, rules, false).isEmpty()) return;
        for (World w : Bukkit.getWorlds()) {
            if (!zone.worldInScope(w.getName(), env(w))) continue;
            for (Player p : w.getPlayers()) checkPlayer(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled()) return;
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkPlayer(p), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!enabled()) return;
        Player p = event.getPlayer();
        checkPlayer(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkPlayer(p), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled()) return;
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkPlayer(p), 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        notices.remove(event.getPlayer().getUniqueId());
    }

    // ---------- status ----------

    String describe() {
        List<String> parts = new ArrayList<>();
        for (ArenaLock.Rule r : rules) {
            parts.add(r.id() + (r.spells().equals(Set.of(r.id())) ? "" : r.spells())
                    + (r.stopRunning() ? " (stopped on entry)" : "")
                    + (r.protectTargets() ? " (+ targets in zone)" : ""));
        }
        String scope = (zone.environments().isEmpty() ? "" : String.join(",", zone.environments()))
                + (zone.worlds().isEmpty() ? "" : (zone.environments().isEmpty() ? "" : " + ") + zone.worlds());
        return "sealed near a live dragon: " + (parts.isEmpty() ? "nothing" : String.join(", ", parts))
                + (rulesFromDefaults ? " [default rules]" : "")
                + "; zone radius " + fmt(zone.radius()) + " around " + fmt(zone.centerX()) + "," + fmt(zone.centerZ())
                + " in " + (scope.isEmpty() ? "no worlds" : scope)
                + "; unknown dragon state = " + (blockIfUnknown ? "locked" : "open");
    }

    String status() {
        StringBuilder sb = new StringBuilder("Arena locks: ").append(enabled ? "ON" : "OFF").append(", ").append(describe());
        for (World w : Bukkit.getWorlds()) {
            if (!zone.worldInScope(w.getName(), env(w))) continue;
            ArenaLock.DragonState s = dragonState(w);
            sb.append("; ").append(w.getName()).append(": dragon ").append(s)
                    .append(enabled && ArenaLock.dragonHolds(s, blockIfUnknown) ? " (zone locked)" : " (zone open)");
        }
        return sb.append(". Since start: ").append(castsBlocked).append(" casts blocked, ").append(targetsBlocked)
                .append(" targets protected, ").append(stops).append(" running spells stopped on entry.").toString();
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? Long.toString((long) d) : Double.toString(d);
    }
}
