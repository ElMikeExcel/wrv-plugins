package com.winkreyes.excelattunement;

import com.winkreyes.excelattunement.core.WardNotice;
import com.winkreyes.excelattunement.core.WardRules;
import com.winkreyes.excelattunement.core.WardWorlds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 0.1.5 guild ward (Mike, Oct 24, 2026: retire Magic inside the
 * EliteMobs worlds, heals included; the EliteMobs classes own combat
 * there). No Magic imports here; MagicBridge does the Magic side
 * (PreCastEvent / WandPreActivateEvent cancels, stopping running
 * spells) and hands its stopper in through {@link #setMagicStopper}.
 *
 * EliteMobs (MagmaGuy) is only READ: its public static
 * com.magmaguy.elitemobs.dungeons.EliteMobsWorld.isEliteMobsWorld(UUID)
 * is looked up once by reflection (present in 10.9.8). If it is
 * missing, moved or throws, the ward falls back to the config name
 * rules alone and logs that once. No EliteMobs file is touched.
 */
final class WorldWard implements Listener {
    static final String BYPASS = "excelattunement.ward.bypass";
    private static final String EM_WORLD_CLASS = "com.magmaguy.elitemobs.dungeons.EliteMobsWorld";

    private final ExcelAttunement plugin;
    private final boolean enabled;
    private final boolean useRegistry;
    private final boolean blockWand;
    private final boolean stopActive;
    private final boolean stripBuffs;
    private final Set<String> stripTypes;
    private final long stripMaxSeconds;
    private final boolean blockTeleports;
    private final List<String> stackPrefixes;
    private final String notice;
    private final long noticeCooldown;
    private final WardWorlds worlds;
    private final Map<UUID, WardNotice> notices = new HashMap<>();
    private Method emIsWorld;          // null = registry not available
    private boolean registryWarned;
    private Consumer<Player> magicStopper = p -> { };
    private int castsBlocked, wandsBlocked, teleportsBlocked, buffsStripped, entries;

    WorldWard(ExcelAttunement plugin, ConfigurationSection c) {
        this.plugin = plugin;
        this.enabled = c == null || c.getBoolean("enabled", true);
        this.useRegistry = c == null || c.getBoolean("use-elitemobs-registry", true);
        this.blockWand = c == null || c.getBoolean("block-wand", true);
        this.stopActive = c == null || c.getBoolean("stop-active-spells", true);
        this.stripBuffs = c == null || c.getBoolean("strip-buffs", true);
        this.stripTypes = WardRules.normalizeAll(listOr(c, "strip-buff-types",
                List.of("resistance", "absorption", "regeneration")));
        this.stripMaxSeconds = c == null ? 15L : c.getLong("strip-max-seconds", 15L);
        this.blockTeleports = c == null || c.getBoolean("block-magic-teleport-in", true);
        this.stackPrefixes = listOr(c, "magic-class-prefixes", List.of("com.elmakers.mine.bukkit."));
        this.notice = c == null ? DEFAULT_NOTICE : c.getString("notice", DEFAULT_NOTICE);
        this.noticeCooldown = c == null ? 10L : c.getLong("notice-cooldown-seconds", 10L);
        this.worlds = new WardWorlds(listOr(c, "world-prefixes", List.of("em_")),
                listOr(c, "worlds", List.of("em_adventurers_guild", "GoblinKingdom_vf")));
        if (enabled && useRegistry) hookRegistry();
    }

    static final String DEFAULT_NOTICE = "The guild's halls are warded. Magic falls silent inside.";

    private static List<String> listOr(ConfigurationSection c, String key, List<String> def) {
        if (c == null || !c.isList(key)) return new ArrayList<>(def);
        return new ArrayList<>(c.getStringList(key));
    }

    private void hookRegistry() {
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null) {
            plugin.getLogger().info("Guild ward: EliteMobs not present, using the world-name rules only.");
            return;
        }
        try {
            Class<?> k = Class.forName(EM_WORLD_CLASS, false, em.getClass().getClassLoader());
            Method m = k.getMethod("isEliteMobsWorld", UUID.class);
            if (m.getReturnType() != boolean.class) throw new NoSuchMethodException("unexpected return type");
            emIsWorld = m;
            plugin.getLogger().info("Guild ward: EliteMobs world registry found (read-only), plus the world-name rules.");
        } catch (Throwable t) {
            emIsWorld = null;
            plugin.getLogger().warning("Guild ward: EliteMobs world registry not found (" + t.getClass().getSimpleName()
                    + "), using the world-name rules only.");
        }
    }

    void setMagicStopper(Consumer<Player> stopper) {
        this.magicStopper = stopper;
    }

    boolean enabled() { return enabled; }

    boolean blockWand() { return enabled && blockWand; }

    /** EliteMobs' answer for this world, or null when it cannot be asked. */
    private Boolean registry(World w) {
        if (emIsWorld == null || w == null) return null;
        try {
            return (Boolean) emIsWorld.invoke(null, w.getUID());
        } catch (Throwable t) {
            if (!registryWarned) {
                registryWarned = true;
                plugin.getLogger().warning("Guild ward: EliteMobs registry lookup failed (" + t.getClass().getSimpleName()
                        + "), using the world-name rules.");
            }
            return null;
        }
    }

    /** Warded = Magic is silent here (and attunement is ignored), whatever the player. */
    boolean isWarded(World w) {
        if (!enabled || w == null) return false;
        return worlds.isWarded(w.getName(), registry(w));
    }

    WardWorlds.Reason reason(World w) {
        if (w == null) return WardWorlds.Reason.NONE;
        return worlds.reason(w.getName(), registry(w));
    }

    boolean bypass(Player p) {
        return p.hasPermission(BYPASS);
    }

    /** MagicBridge asks: block this player's cast here? */
    boolean blocksCast(Player p) {
        return WardRules.blockCast(enabled, isWarded(p.getWorld()), bypass(p));
    }

    void onCastBlocked(Player p, String key) {
        castsBlocked++;
        if (plugin.debug()) plugin.getLogger().info("WARD blocked cast '" + key + "' for " + p.getName() + " in " + p.getWorld().getName());
        if (noticeOf(p).take(plugin.now())) sendNotice(p, false);
    }

    void onWandBlocked(Player p) {
        wandsBlocked++;
        // Magic re-tries activating a held wand every tick, so this fires
        // often: log it at most once per 10s per player (debug only).
        if (plugin.debug() && wandLogs.computeIfAbsent(p.getUniqueId(), id -> new WardNotice(10)).take(plugin.now()))
            plugin.getLogger().info("WARD kept wand inactive for " + p.getName() + " in " + p.getWorld().getName());
    }

    private final java.util.Map<java.util.UUID, WardNotice> wandLogs = new java.util.HashMap<>();

    private WardNotice noticeOf(Player p) {
        return notices.computeIfAbsent(p.getUniqueId(), id -> new WardNotice(noticeCooldown));
    }

    private void sendNotice(Player p, boolean chat) {
        if (notice == null || notice.isBlank()) return;
        p.sendActionBar(Component.text(notice, NamedTextColor.LIGHT_PURPLE));
        if (chat) p.sendMessage(Component.text(notice, NamedTextColor.LIGHT_PURPLE));
    }

    // ---------- entering a warded world ----------

    /**
     * Arrival in a warded world: attunement stance and weapon lock clear
     * for EVERYONE (attunement is ignored there); for non-bypass players
     * running Magic spells stop, the wand closes, short Magic-type buffs
     * are stripped, and the ward line shows once in chat.
     */
    void onEnter(Player p) {
        if (!p.isOnline() || !isWarded(p.getWorld())) return;
        entries++;
        plugin.clearForWard(p);
        if (bypass(p)) return;
        if (stopActive) {
            try {
                magicStopper.accept(p);
            } catch (Throwable t) {
                plugin.getLogger().warning("Guild ward: stopping Magic for " + p.getName() + " failed: " + t);
            }
        }
        if (stripBuffs) {
            List<PotionEffect> strip = new ArrayList<>();
            for (PotionEffect e : p.getActivePotionEffects()) {
                if (WardRules.shouldStrip(e.getType().getKey().toString(), e.getDuration(), stripTypes, stripMaxSeconds)) {
                    strip.add(e);
                }
            }
            for (PotionEffect e : strip) {
                p.removePotionEffect(e.getType());
                buffsStripped++;
                if (plugin.debug()) plugin.getLogger().info("WARD stripped " + e.getType().getKey() + " ("
                        + (e.getDuration() / 20) + "s left) from " + p.getName());
            }
        }
        noticeOf(p).force(plugin.now());
        sendNotice(p, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        onEnter(p);
        // Magic re-checks the wand a tick after a world change; run the
        // stop once more then (WandPreActivateEvent keeps it closed anyway).
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && isWarded(p.getWorld()) && !bypass(p) && stopActive) {
                try { magicStopper.accept(p); } catch (Throwable ignored) { }
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> onEnter(p), 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        // Same-world respawns fire no world change; check after the move.
        Bukkit.getScheduler().runTaskLater(plugin, () -> onEnter(p), 2L);
    }

    /**
     * No Magic teleports INTO a warded world (Recall to a death point or
     * marker inside the guild, a Warp spell). Magic teleports
     * synchronously inside its action (CastContext.teleport ->
     * entity.teleport, Magic 11.2.5 source), so a Magic class is on the
     * call stack; EliteMobs' own teleports and /ag have none. Recall OUT
     * of a warded world never starts: the cast is blocked there.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled || !blockTeleports || event.getTo() == null) return;
        World to = event.getTo().getWorld();
        if (!isWarded(to)) return;
        Player p = event.getPlayer();
        boolean magic = magicOnStack();
        if (WardRules.blockTeleport(enabled, blockTeleports, true, magic, bypass(p))) {
            event.setCancelled(true);
            teleportsBlocked++;
            plugin.getLogger().info("Guild ward: blocked a Magic teleport of " + p.getName() + " into " + to.getName() + ".");
            p.sendMessage(Component.text("The guild's wards turn your spell aside.", NamedTextColor.LIGHT_PURPLE));
        }
    }

    private boolean magicOnStack() {
        List<String> names = StackWalker.getInstance().walk(s -> s.map(StackWalker.StackFrame::getClassName).toList());
        return WardRules.anyFrameMatches(names, stackPrefixes);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        notices.remove(event.getPlayer().getUniqueId());
        wandLogs.remove(event.getPlayer().getUniqueId());
    }

    // ---------- /attunement ward ----------

    String status(World here) {
        StringBuilder b = new StringBuilder("Guild ward: ").append(enabled ? "ON" : "OFF")
                .append(", EliteMobs registry ").append(emIsWorld != null ? "found" : "not used")
                .append(", prefixes ").append(worlds.prefixes()).append(", worlds ").append(worlds.names())
                .append(", wand ").append(blockWand ? "closed" : "open")
                .append(", strip ").append(stripBuffs ? stripTypes + " <=" + stripMaxSeconds + "s" : "off")
                .append(", Magic teleports in ").append(blockTeleports ? "blocked" : "allowed").append('.');
        List<String> warded = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) if (isWarded(w)) warded.add(w.getName() + " (" + reason(w) + ")");
        b.append("\n  Warded worlds loaded: ").append(warded.isEmpty() ? "none" : String.join(", ", warded));
        if (here != null) b.append("\n  This world (").append(here.getName()).append("): ").append(reason(here));
        b.append("\n  Since start: ").append(castsBlocked).append(" casts blocked, ").append(wandsBlocked)
                .append(" wand opens blocked, ").append(teleportsBlocked).append(" teleports blocked, ")
                .append(buffsStripped).append(" buffs stripped, ").append(entries).append(" entries.");
        return b.toString();
    }
}
