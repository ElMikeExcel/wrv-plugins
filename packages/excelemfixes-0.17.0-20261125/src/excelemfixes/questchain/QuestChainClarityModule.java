package excelemfixes.questchain;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import excelemfixes.shared.TextUtil;
import com.magmaguy.elitemobs.api.NPCEntityRemoveEvent;
import com.magmaguy.elitemobs.api.NPCEntitySpawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fix module 22: quest-chain clarity (Jester's report and
 * Mike's direction, Nov 24, 2026).
 *
 * THE PROBLEM: when a player clicks an EliteMobs quest NPC
 * whose quest sits behind earlier quests, EliteMobs answers
 * with one generic red line — "This NPC has some quest(s) you
 * can't accept yet! Finish the quests that come before them
 * first." — and nothing else. The player is told a chain
 * exists but never which link they are missing: Jester found
 * Alanus Secundas at the Pontis Groves lighthouse, got that
 * line, and had no way to learn that the road there runs
 * through King Nemian's quests in the Groves. He had to ask
 * a human.
 *
 * THE FIX: when a click on an EliteMobs quest NPC leaves a
 * quest chain-locked for this player, the player additionally
 * gets one specific line per locked chain naming the NEXT
 * quest they can actually go and do — the deepest link of
 * the chain they have not finished — with the NPC who gives
 * it and the place the quest itself names, when the content
 * provides one: "Locked: finish 'Population Control!' first
 * — King Nemian gives it."
 *
 * DETECTION ROUTE (and why): EliteMobs' api/ events cover
 * quest accepts, completions and progression, but no event
 * fires when a quest is REFUSED — the refusal is just a chat
 * message inside QuestInteractionHandler. So this module
 * listens to the same click EliteMobs does
 * (PlayerInteractEntityEvent, main hand, MONITOR priority —
 * it observes after EliteMobs has answered and never alters
 * the event, the click, or EliteMobs' own message), works
 * out which NPC was clicked the same way quest-giver-glow
 * does (EliteMobs' own spawn/remove events plus a harvest of
 * its spawned-NPC registry), and replicates only the
 * accept-permission segment of EliteMobs' own accept check
 * from its loaded quest data (see QuestChainCore for the
 * exact semantics). A quest EliteMobs refuses for any other
 * reason — already finished and on lockout, already carried
 * — earns no line here: the module explains chain locks,
 * nothing else.
 *
 * One hard-won detail: EliteMobs CANCELS this event for its
 * NPCs (that is how it keeps the vanilla villager trade
 * screen from opening), so the listener must NOT use
 * ignoreCancelled — with it, the handler never runs at all
 * (proven on the mirror, Nov 24, 2026: EliteMobs' generic
 * line arrived, this module stayed silent, until the flag
 * came off). Observing a cancelled click is safe here: the
 * module only reads and messages, it never un-cancels or
 * otherwise touches the event.
 *
 * Data-driven and expansion-generic: quests, chains, givers
 * and places all come from EliteMobs' loaded registries at
 * runtime; the module knows no quest, NPC or place by name.
 * With the module off, nothing changes at all.
 */
public final class QuestChainClarityModule implements FixModule, Listener {

    private ModuleContext ctx;
    private EmQuestChainData data;
    private BukkitTask task;

    private int cooldownSeconds = 30;
    private int maxLines = 3;
    private boolean debug;

    private volatile QuestChainCore index =
            new QuestChainCore(List.of(), Map.of(), Map.of());
    private boolean censusLogged;

    /**
     * Live NPC identity, learned from EliteMobs' own spawn
     * events and its spawned-NPC registry: entity UUID -> the
     * NPC config filename it belongs to. An EliteMobs NPC body
     * carries no server-side name to match against (its
     * nameplate is a per-player packet fake), so EliteMobs
     * itself is the only truthful source — the same
     * identification as quest-giver-glow.
     */
    private final Map<UUID, String> liveNpcFiles = new HashMap<>();

    /** Last explanation time per player + NPC, epoch millis. */
    private final Map<UUID, Map<String, Long>> lastTold = new HashMap<>();

    @Override
    public String id() {
        return "quest-chain-clarity";
    }

    @Override
    public String displayName() {
        return "Quest Chain Clarity";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
            ctx.logger().warning("Quest Chain Clarity: EliteMobs "
                    + "not found — the module idles until "
                    + "EliteMobs is present.");
            return;
        }
        data = new EmQuestChainData(ctx.logger());
        loadSettings();
        index = data.snapshot();
        liveNpcFiles.putAll(data.harvestLiveNpcs());
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        startTask();
        ctx.logger().info("Quest Chain Clarity ready — "
                + index.questCount() + " quests indexed; a "
                + "chain-locked quest NPC now names the next "
                + "quest to finish.");
    }

    @Override
    public void onDisable() {
        stopTask();
        HandlerList.unregisterAll(this);
        liveNpcFiles.clear();
        lastTold.clear();
    }

    @Override
    public void reload() {
        if (data == null) return;
        loadSettings();
        index = data.snapshot();
        liveNpcFiles.putAll(data.harvestLiveNpcs());
    }

    private void loadSettings() {
        ConfigurationSection c = ctx.moduleConfig();
        if (c == null) return;
        cooldownSeconds = Math.max(0, Math.min(300,
                c.getInt("cooldown-seconds", cooldownSeconds)));
        maxLines = Math.max(1, Math.min(5,
                c.getInt("max-lines", maxLines)));
        debug = c.getBoolean("debug", false);
    }

    private void startTask() {
        stopTask();
        // Keep the chain index and the NPC identity map fresh:
        // EliteMobs populates its registries seconds after
        // boot, content can be reloaded at runtime, and NPCs
        // spawn as chunks load.
        task = Bukkit.getScheduler().runTaskTimer(
                ctx.plugin(), this::refresh, 600L, 600L);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void refresh() {
        if (data == null) return;
        try {
            liveNpcFiles.putAll(data.harvestLiveNpcs());
            QuestChainCore fresh = data.snapshot();
            if (fresh.questCount() > 0) {
                index = fresh;
                if (!censusLogged) {
                    censusLogged = true;
                    ctx.logger().info("Quest Chain Clarity "
                            + "census — " + fresh.questCount()
                            + " quests indexed from EliteMobs' "
                            + "data.");
                }
            }
        } catch (Throwable ignored) {
            // The bridge already logged it; the last good
            // index keeps serving.
        }
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
            // An unresolvable spawn is simply never explained.
        }
    }

    @EventHandler
    public void onNpcRemove(NPCEntityRemoveEvent event) {
        liveNpcFiles.remove(event.getLivingEntity().getUniqueId());
    }

    // ------------------------------------------------------------ the click

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (data == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();
        String npcFile = liveNpcFiles.get(clicked.getUniqueId());
        if (npcFile == null) {
            // The NPC may have been standing since before this
            // module enabled and its chunk never reloaded: one
            // harvest retry before giving up on the click.
            liveNpcFiles.putAll(data.harvestLiveNpcs());
            npcFile = liveNpcFiles.get(clicked.getUniqueId());
            if (npcFile == null) {
                if (debug) {
                    ctx.logger().info("Quest Chain Clarity "
                            + "debug: click by "
                            + player.getName()
                            + " on unidentified entity "
                            + clicked.getType() + " "
                            + clicked.getUniqueId());
                }
                return;
            }
        }
        try {
            explain(player, npcFile);
        } catch (Throwable t) {
            if (debug) {
                ctx.logger().warning("Quest Chain Clarity: "
                        + "explain failed for " + player.getName()
                        + " at " + npcFile + ": " + t);
            }
        }
    }

    private void explain(Player player, String npcFile) {
        QuestChainCore core = index;
        List<QuestChainCore.QuestInfo> quests =
                core.questsForNpc(npcFile);
        if (quests.isEmpty()) return;
        if (!data.playerDataReady(player)) return;
        Set<String> active = data.activeQuestFiles(player);
        boolean anyCandidate = false;
        for (QuestChainCore.QuestInfo q : quests) {
            if (!active.contains(q.file())) {
                anyCandidate = true;
                break;
            }
        }
        if (!anyCandidate) return;
        Set<String> held = data.heldPerms(player, core.allAcceptPerms());
        if (held == null) return;
        List<QuestChainCore.NextStep> steps =
                core.nextSteps(npcFile, held, active, maxLines);
        if (steps.isEmpty()) {
            if (debug) {
                ctx.logger().info("Quest Chain Clarity debug: "
                        + player.getName() + " at " + npcFile
                        + " -> no steps (quests="
                        + quests.size() + ", held=" + held
                        + ", active=" + active + ")");
            }
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Long> mine = lastTold.computeIfAbsent(
                player.getUniqueId(), k -> new HashMap<>());
        Long last = mine.get(npcFile);
        if (last != null
                && now - last < cooldownSeconds * 1000L) {
            return;
        }
        mine.put(npcFile, now);

        for (QuestChainCore.NextStep step : steps) {
            player.sendMessage(TextUtil.component(
                    QuestChainCore.formatLine(step)));
        }
        if (debug) {
            ctx.logger().info("Quest Chain Clarity debug: "
                    + player.getName() + " at " + npcFile
                    + " -> " + steps);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTold.remove(event.getPlayer().getUniqueId());
    }
}
