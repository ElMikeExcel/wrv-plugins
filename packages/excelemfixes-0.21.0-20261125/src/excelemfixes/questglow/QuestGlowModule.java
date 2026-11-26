package excelemfixes.questglow;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import com.magmaguy.elitemobs.api.NPCEntityRemoveEvent;
import com.magmaguy.elitemobs.api.NPCEntitySpawnEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fix module 17: quest-giver glow (Jester's request, Nov 24,
 * 2026; Mike's chosen shape the same night).
 *
 * THE PROBLEM: EliteMobs quest NPCs are invisible as quest
 * givers until a player walks up and right-clicks them — in a
 * big quest world there is no way to tell, from any distance,
 * which of the many NPCs actually has a quest for YOU. Players
 * resort to clicking every NPC they pass.
 *
 * THE FIX: on a gentle refresh, work out — from EliteMobs' own
 * data, through its own accept check — which NPCs have a quest
 * THIS player can pick up right now, and light exactly those
 * NPCs, for that player only, as a per-viewer metadata packet
 * (see GlowPackets for why a packet, and why it cannot leak:
 * the glow exists only in the entitled viewer's client data).
 * A second color marks an NPC the player is carrying a
 * finished quest back to. A third color (0.17.0, the
 * dialog-target fix) marks the NPC a carried quest's
 * outstanding DIALOG objective sends the player to talk to —
 * the state Jester's Nov 25, 2026 report exposed as missing:
 * he carried a quest whose only objective was talking to
 * King Nemian, and Nemian could not glow in either older
 * state (every quest Nemian offers was chain-locked for him,
 * and the turn-in light for that very quest can only switch
 * on after the talk is already over). The objective state
 * extends the NPC universe to dialog targets of carried
 * quests: those NPCs are ordinary EliteMobs NPCs and resolve
 * through the same spawn-event / registry-harvest /
 * anchor-adoption machinery as givers and turn-ins. NPCs
 * EliteMobs itself does not track stay unlit — the same
 * truthful boundary as the older states. Nothing is written to EliteMobs, no
 * entity is really glowing server-side, and a player with
 * nothing to pick up sees a completely plain world.
 *
 * Data-driven and expansion-generic: NPCs, their quest lists,
 * turn-in links, accept permissions and lockouts all come from
 * EliteMobs' loaded registries at runtime; the module knows no
 * quest, NPC or place by name. EliteMobs' dynamic quest-giver
 * NPCs (the ones that generate offers on the spot when talked
 * to) carry no quest list in EliteMobs' data, so there is
 * nothing truthful to light for them — they never glow here.
 *
 * Identification: an EliteMobs NPC body is a plain villager
 * with no Bukkit custom name, and its floating nameplate is a
 * per-player packet fake with no server-side entity (both
 * mirror-proven, Nov 24, 2026), so neither can be matched
 * against. The module instead learns identity from EliteMobs
 * itself, three ways: its NPC spawn/remove events; a periodic
 * harvest of its spawned-NPC registry (for the NPCs already
 * standing when the module enabled — EliteMobs loads its hub
 * world at boot, greeters included); and, as a last backstop,
 * adopting an anchored NPC from the post its data names,
 * where EliteMobs quest NPCs stand.
 */
public final class QuestGlowModule implements FixModule, Listener {

    private static final String ACCEPT_TEAM = "qgaccept";
    private static final String TURNIN_TEAM = "qgturnin";
    private static final String OBJECTIVE_TEAM = "qgobjective";

    private ModuleContext ctx;
    private EmQuestGlowData data;
    private BukkitTask task;

    private int refreshTicks = 40;
    private double viewRadius = 64;
    private double anchorRadius = 8;
    private NamedTextColor acceptColor = NamedTextColor.GOLD;
    private NamedTextColor turnInColor = NamedTextColor.GREEN;
    private NamedTextColor objectiveColor = NamedTextColor.AQUA;
    private boolean objectiveEnabled = true;
    private boolean debug;

    private boolean bridgeDown;
    private int consecutiveFailures;
    private boolean censusLogged;
    private int refreshCount;

    /** What we have lit, per viewer: player -> entity -> state. */
    private final Map<UUID, Map<UUID, GlowState>> sent = new HashMap<>();

    /**
     * Live NPC identity, learned from EliteMobs' own spawn
     * events (and the anchor-adoption backstop below): entity
     * UUID -> the NPC config filename it belongs to. This map
     * is the module's only identification source — an EliteMobs
     * NPC body is a plain villager with no Bukkit custom name,
     * and its floating nameplate is a per-player packet fake
     * that does not exist server-side (both mirror-proven,
     * Nov 24, 2026), so neither can be matched against. The
     * spawn event is EliteMobs saying "this entity is this
     * NPC" itself.
     */
    private final Map<UUID, String> liveNpcFiles = new HashMap<>();

    @Override
    public String id() {
        return "quest-giver-glow";
    }

    @Override
    public String displayName() {
        return "Quest-Giver Glow";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
            ctx.logger().warning("Quest-Giver Glow: EliteMobs not "
                    + "found — the module idles until EliteMobs is "
                    + "present.");
            return;
        }
        data = new EmQuestGlowData(ctx.logger());
        loadSettings();
        if (!GlowPackets.available()) {
            bridgeDown = true;
            ctx.logger().warning("Quest-Giver Glow: per-viewer "
                    + "glow packets are unavailable on this server "
                    + "build — the module idles (no glow, no "
                    + "errors).");
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        startTask();
        Map<String, String> turnIns = data.turnInByQuestFile();
        List<QuestGlowCore.NpcInfo> npcs = data.npcs(
                new HashSet<>(turnIns.values()),
                objectiveEnabled ? data.dialogTargets() : null);
        long givers = npcs.stream()
                .filter(n -> !n.questFiles().isEmpty()).count();
        ctx.logger().info("Quest-Giver Glow ready — " + givers
                + " quest-giver NPCs and " + npcs.size()
                + " glow-capable NPCs in EliteMobs' data; a quest "
                + "NPC lights up only for a player it has a quest "
                + "for.");
    }

    @Override
    public void onDisable() {
        stopTask();
        HandlerList.unregisterAll(this);
        // Take every light back off before we go: a viewer must
        // never be left with a glow we are no longer refreshing.
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<UUID, GlowState> mine = sent.remove(player.getUniqueId());
            if (mine == null) continue;
            for (Map.Entry<UUID, GlowState> e : mine.entrySet()) {
                Entity entity = Bukkit.getEntity(e.getKey());
                if (entity != null) {
                    GlowPackets.setGlowing(player, entity, false);
                }
                teamRemove(player, e.getKey(), e.getValue());
            }
        }
        sent.clear();
        liveNpcFiles.clear();
    }

    @Override
    public void reload() {
        if (data == null) return;
        loadSettings();
        startTask();
    }

    private void loadSettings() {
        ConfigurationSection c = ctx.moduleConfig();
        if (c == null) return;
        refreshTicks = Math.max(10, Math.min(200,
                c.getInt("refresh-ticks", refreshTicks)));
        viewRadius = Math.max(16, Math.min(160,
                c.getDouble("view-radius", viewRadius)));
        anchorRadius = Math.max(4, Math.min(64,
                c.getDouble("anchor-radius", anchorRadius)));
        acceptColor = parseColor(c.getString("accept-color"),
                NamedTextColor.GOLD);
        turnInColor = parseColor(c.getString("turn-in-color"),
                NamedTextColor.GREEN);
        objectiveColor = parseColor(c.getString("objective-color"),
                NamedTextColor.AQUA);
        objectiveEnabled = c.getBoolean("objective-enabled", true);
        debug = c.getBoolean("debug", false);
    }

    private static NamedTextColor parseColor(String raw,
                                             NamedTextColor fallback) {
        if (raw == null) return fallback;
        NamedTextColor color = NamedTextColor.NAMES.value(
                raw.trim().toLowerCase(java.util.Locale.ROOT));
        return color == null ? fallback : color;
    }

    private void startTask() {
        stopTask();
        if (bridgeDown) return;
        task = Bukkit.getScheduler().runTaskTimer(
                ctx.plugin(), this::refresh,
                refreshTicks, refreshTicks);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    // ------------------------------------------------------------ refresh

    private void refresh() {
        if (bridgeDown || data == null) return;
        // Keep the identity map topped up from EliteMobs'
        // own registry of spawned NPCs (the pre-enable
        // greeters); spawn events and post adoption cover
        // the rest. Every 15th refresh is often enough —
        // identity changes on chunk timescales, not ticks.
        if (++refreshCount % 15 == 1) {
            try {
                Map<UUID, String> harvested = data.harvestLiveNpcs();
                liveNpcFiles.putAll(harvested);
                if (debug) {
                    ctx.logger().info("Quest-Giver Glow debug: "
                            + "harvested " + harvested.size()
                            + " live NPCs: " + harvested);
                }
            } catch (Throwable ignored) {
                // The bridge already logged it; events and
                // adoption still identify what they can.
            }
        }
        Map<String, String> turnInByQuest = data.turnInByQuestFile();
        List<QuestGlowCore.NpcInfo> npcs = data.npcs(
                new HashSet<>(turnInByQuest.values()),
                objectiveEnabled ? data.dialogTargets() : null);
        if (!censusLogged && !npcs.isEmpty()) {
            // EliteMobs' registries can populate seconds after
            // this module enables, so the enable-time census
            // may read 0; log the real count at the first read
            // that sees the data.
            censusLogged = true;
            long givers = npcs.stream()
                    .filter(n -> !n.questFiles().isEmpty()).count();
            ctx.logger().info("Quest-Giver Glow census — "
                    + givers + " quest-giver NPCs and "
                    + npcs.size() + " glow-capable NPCs in "
                    + "EliteMobs' data.");
            if (debug) {
                StringBuilder sb = new StringBuilder(
                        "Quest-Giver Glow debug: glow-capable "
                                + "NPCs:");
                for (QuestGlowCore.NpcInfo npc : npcs) {
                    sb.append(' ').append(npc.npcFile())
                            .append("(quests=")
                            .append(npc.questFiles().size())
                            .append(",anchors=")
                            .append(npc.anchors().size())
                            .append(')');
                }
                ctx.logger().info(sb.toString());
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                refreshPlayer(player, npcs, turnInByQuest);
            } catch (Throwable t) {
                if (debug) {
                    ctx.logger().warning("Quest-Giver Glow: refresh "
                            + "failed for " + player.getName() + ": " + t);
                }
            }
        }
    }

    private void refreshPlayer(Player player,
                               List<QuestGlowCore.NpcInfo> npcs,
                               Map<String, String> turnInByQuest) {
        Map<String, Boolean> active = data.activeQuests(player);
        Set<String> available =
                data.availableQuests(player, active.keySet());
        Map<String, String> myTurnIns = new HashMap<>();
        for (Map.Entry<String, Boolean> e : active.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                String target = turnInByQuest.get(e.getKey());
                if (target != null) myTurnIns.put(e.getKey(), target);
            }
        }

        Set<String> myObjectiveTargets = objectiveEnabled
                ? data.objectiveTargets(player) : Set.of();
        Location ploc = player.getLocation();
        Map<UUID, GlowState> desired = new HashMap<>();
        Map<UUID, Entity> desiredEntities = new HashMap<>();
        for (QuestGlowCore.NpcInfo npc : npcs) {
            GlowState state = QuestGlowCore.stateFor(
                    npc, available, myTurnIns, myObjectiveTargets);
            if (state == GlowState.NONE) continue;
            List<Entity> bodies = liveBodies(player, npc);
            for (Entity entity : bodies) {
                // One entity claimed by two NPC entries (a
                // shared post) shows the stronger state:
                // TURNIN > OBJECTIVE > ACCEPT.
                desired.merge(entity.getUniqueId(), state,
                        QuestGlowCore::stronger);
                desiredEntities.put(entity.getUniqueId(), entity);
            }
            if (debug) {
                ctx.logger().info("Quest-Giver Glow debug: "
                        + player.getName() + " <- "
                        + npc.npcFile() + " = " + state
                        + (bodies.isEmpty()
                                ? " (no live entity found)"
                                : " (entities " + bodies.size()
                                        + ")"));
            }
        }
        if (debug) {
            ctx.logger().info("Quest-Giver Glow debug: "
                    + player.getName() + " has "
                    + available.size() + " available quest(s): "
                    + available);
        }
        applyDiff(player, desired, desiredEntities);
    }

    /**
     * The live bodies for one NPC that this player should see
     * lit: the entities EliteMobs' spawn events (or the anchor
     * backstop) identified as this NPC, in the player's world
     * and inside the view radius of the player. Stale identity
     * entries (despawned entities) are pruned as they are met.
     */
    private List<Entity> liveBodies(Player player,
                                    QuestGlowCore.NpcInfo npc) {
        List<Entity> out = new ArrayList<>();
        Location ploc = player.getLocation();
        Iterator<Map.Entry<UUID, String>> it =
                liveNpcFiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, String> e = it.next();
            if (!e.getValue().equals(npc.npcFile())) continue;
            Entity entity = Bukkit.getEntity(e.getKey());
            if (entity == null || entity.isDead()) {
                it.remove();
                continue;
            }
            if (!entity.getWorld().equals(ploc.getWorld())) {
                continue;
            }
            Location eloc = entity.getLocation();
            if (QuestGlowCore.within(
                    ploc.getX() - eloc.getX(),
                    ploc.getY() - eloc.getY(),
                    ploc.getZ() - eloc.getZ(), viewRadius)) {
                out.add(entity);
            }
        }
        if (out.isEmpty()) {
            Entity adopted = adoptAtAnchor(player, npc);
            if (adopted != null) out.add(adopted);
        }
        return out;
    }

    /**
     * The boot/reload backstop: an NPC whose spawn this
     * module missed (its chunk was already loaded when the
     * module enabled) is adopted from its post — the living
     * entity standing within the anchor radius of the home
     * spot EliteMobs' data gives it. EliteMobs quest NPCs
     * stand at their posts (and EliteMobs itself removes
     * duplicates), so the body on the post IS the NPC; the
     * adoption is recorded in the same identity map the spawn
     * events feed, so it is paid for once, not every refresh.
     * Anchorless NPCs have no post to adopt from; their
     * chunks load as players approach, which is when their
     * spawn events fire.
     */
    private Entity adoptAtAnchor(Player player,
                                 QuestGlowCore.NpcInfo npc) {
        String worldName = player.getWorld().getName();
        for (QuestGlowCore.Anchor anchor : npc.anchors()) {
            if (!anchor.world().equals(worldName)) continue;
            Location ploc = player.getLocation();
            if (!QuestGlowCore.within(
                    ploc.getX() - anchor.x(),
                    ploc.getY() - anchor.y(),
                    ploc.getZ() - anchor.z(), viewRadius)) {
                continue;
            }
            Location spot = new Location(player.getWorld(),
                    anchor.x(), anchor.y(), anchor.z());
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;
            for (Entity e : player.getWorld().getNearbyEntities(
                    spot, anchorRadius, anchorRadius, anchorRadius)) {
                if (!(e instanceof LivingEntity living)) continue;
                if (e instanceof Player) continue;
                if (liveNpcFiles.containsKey(e.getUniqueId())) {
                    continue;
                }
                double dist = e.getLocation().distanceSquared(spot);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = living;
                }
            }
            if (best != null) {
                liveNpcFiles.put(best.getUniqueId(), npc.npcFile());
                if (debug) {
                    ctx.logger().info("Quest-Giver Glow debug: "
                            + "adopted the body on " + npc.npcFile()
                            + "'s post as that NPC (its spawn "
                            + "predates this module's enable).");
                }
                return best;
            }
        }
        return null;
    }

    // ------------------------------------------------------------ NPC identity events

    @EventHandler
    public void onNpcSpawn(NPCEntitySpawnEvent event) {
        if (data == null) return;
        try {
            String file = data.filenameFor(
                    event.getNPCEntity().getNPCsConfigFields());
            if (file != null) {
                liveNpcFiles.put(
                        event.getLivingEntity().getUniqueId(), file);
            }
        } catch (Throwable ignored) {
            // An unresolvable spawn is simply never lit.
        }
    }

    @EventHandler
    public void onNpcRemove(NPCEntityRemoveEvent event) {
        liveNpcFiles.remove(event.getLivingEntity().getUniqueId());
    }

    // ------------------------------------------------------------ applying

    private void applyDiff(Player player,
                           Map<UUID, GlowState> desired,
                           Map<UUID, Entity> desiredEntities) {
        Map<UUID, GlowState> mine = sent.computeIfAbsent(
                player.getUniqueId(), k -> new HashMap<>());

        Iterator<Map.Entry<UUID, GlowState>> it =
                mine.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, GlowState> e = it.next();
            GlowState want = desired.get(e.getKey());
            if (want == e.getValue()) continue;
            teamRemove(player, e.getKey(), e.getValue());
            if (want == null) {
                Entity entity = Bukkit.getEntity(e.getKey());
                if (entity != null) {
                    GlowPackets.setGlowing(player, entity, false);
                }
                it.remove();
            }
        }

        for (Map.Entry<UUID, GlowState> e : desired.entrySet()) {
            GlowState already = mine.get(e.getKey());
            if (already == e.getValue()) continue;
            Entity entity = desiredEntities.get(e.getKey());
            if (entity == null) continue;
            if (already == null) {
                if (!GlowPackets.setGlowing(player, entity, true)) {
                    notePacketFailure();
                    continue;
                }
                notePacketSuccess();
            }
            teamAdd(player, e.getKey(), e.getValue());
            mine.put(e.getKey(), e.getValue());
        }

        if (mine.isEmpty()) sent.remove(player.getUniqueId());
    }

    private void notePacketFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= 3 && !bridgeDown) {
            bridgeDown = true;
            stopTask();
            ctx.logger().warning("Quest-Giver Glow: glow packets "
                    + "are failing on this server build — the "
                    + "module idles for this session (no glow, "
                    + "no errors).");
            // Leave nothing lit behind us.
            onDisableCleanupOnly();
        }
    }

    private void notePacketSuccess() {
        consecutiveFailures = 0;
    }

    /** Clears sent state without touching the scheduler (the
     *  failure path already stopped the task). */
    private void onDisableCleanupOnly() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<UUID, GlowState> mine = sent.remove(player.getUniqueId());
            if (mine == null) continue;
            for (Map.Entry<UUID, GlowState> e : mine.entrySet()) {
                Entity entity = Bukkit.getEntity(e.getKey());
                if (entity != null) {
                    GlowPackets.setGlowing(player, entity, false);
                }
                teamRemove(player, e.getKey(), e.getValue());
            }
        }
        sent.clear();
    }

    // ------------------------------------------------------------ teams

    private void teamAdd(Player player, UUID entityId,
                         GlowState state) {
        try {
            Team team = team(player.getScoreboard(), state);
            if (team == null) return;
            String entry = entityId.toString();
            if (!team.hasEntry(entry)) team.addEntry(entry);
        } catch (Throwable ignored) {
            // Color is best-effort: the glow bit is the feature,
            // and it never depends on the scoreboard.
        }
    }

    private void teamRemove(Player player, UUID entityId,
                            GlowState state) {
        try {
            Scoreboard board = player.getScoreboard();
            if (board == null) return;
            Team team = board.getTeam(teamName(state));
            if (team == null) return;
            String entry = entityId.toString();
            if (team.hasEntry(entry)) team.removeEntry(entry);
        } catch (Throwable ignored) {
            // See teamAdd.
        }
    }

    private Team team(Scoreboard board, GlowState state) {
        if (board == null) return null;
        String name = teamName(state);
        Team team = board.getTeam(name);
        if (team == null) team = board.registerNewTeam(name);
        team.color(switch (state) {
            case TURNIN -> turnInColor;
            case OBJECTIVE -> objectiveColor;
            default -> acceptColor;
        });
        return team;
    }

    private static String teamName(GlowState state) {
        return switch (state) {
            case TURNIN -> TURNIN_TEAM;
            case OBJECTIVE -> OBJECTIVE_TEAM;
            default -> ACCEPT_TEAM;
        };
    }

    // ------------------------------------------------------------ events

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // The viewer's client is gone, so its glow state dies
        // with the session; only our bookkeeping needs clearing.
        sent.remove(event.getPlayer().getUniqueId());
    }
}
