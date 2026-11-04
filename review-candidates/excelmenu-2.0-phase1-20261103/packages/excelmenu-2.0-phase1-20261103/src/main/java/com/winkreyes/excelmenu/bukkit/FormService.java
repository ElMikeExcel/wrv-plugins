// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.render.FormModel;
import com.winkreyes.excelmenu.core.render.FormSession;
import com.winkreyes.excelmenu.core.render.LayoutDef;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * The registry form service: renders resolver output as Cumulus
 * forms through the unified bridge, under the §2.5 session
 * discipline implemented by the core {@link FormSession.Store}
 * (single-use tokens bound to player + menu + world + TTL; closed
 * = cancelled; stale answers dropped).
 *
 * Steps: a FLAT layout opens one form (the native compass form's
 * shape); a GROUPED layout opens a group picker, then the group's
 * form. A failed send falls back to the chest renderer (supplied by
 * RegistryMenus) — a Bedrock player is never stranded without the
 * menu (DESIGN-2.0 §2.2).
 */
final class FormService implements Listener {

    private final JavaPlugin plugin;
    private final PlatformBridge bridge;
    private final EntryService entries;
    private final ActionDispatcher dispatcher;
    private final FormSession.Store sessions = new FormSession.Store();
    private final Map<UUID, Long> generations = new ConcurrentHashMap<>();
    private volatile long ttlMillis = 300_000L;
    /** (player, menuId) -> chest open; set by RegistryMenus. */
    private BiFunction<Player, String, Boolean> chestFallback;

    FormService(JavaPlugin plugin, PlatformBridge bridge, EntryService entries,
                ActionDispatcher dispatcher) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.entries = entries;
        this.dispatcher = dispatcher;
    }

    void setChestFallback(BiFunction<Player, String, Boolean> chestFallback) {
        this.chestFallback = chestFallback;
    }

    void setTtlSeconds(long seconds) {
        this.ttlMillis = Math.max(30L, seconds) * 1000L;
    }

    void retire() {
        sessions.clear();
        generations.clear();
    }

    void invalidate(Player player) {
        sessions.invalidate(player.getUniqueId());
        generations.merge(player.getUniqueId(), 1L, Long::sum);
    }

    boolean hasLiveForm(Player player) {
        return sessions.hasLive(player.getUniqueId());
    }

    // ---------- opening ----------

    /** Open a registry menu as a form. Returns false when the send
     *  failed and no chest fallback could serve either. */
    boolean open(Player player, LayoutDef layout, List<ResolvedEntry> resolved) {
        FormModel.FormPage page;
        String group = null;
        if (layout.form() == LayoutDef.Form.GROUPED) {
            page = FormModel.picker(layout, resolved);
        } else {
            page = FormModel.flat(layout, resolved, entries.config());
        }
        return send(player, layout, resolved, group, page);
    }

    private boolean openGroup(Player player, LayoutDef layout,
                              List<ResolvedEntry> resolved, String group) {
        FormModel.FormPage page = FormModel.group(layout, resolved, group);
        return send(player, layout, resolved, group, page);
    }

    private boolean send(Player player, LayoutDef layout, List<ResolvedEntry> resolved,
                         String group, FormModel.FormPage page) {
        long generation = generations.merge(player.getUniqueId(), 1L, Long::sum);
        FormSession.Token token = sessions.issue(player.getUniqueId(), layout.id(), group,
                player.getWorld().getName(), System.currentTimeMillis(), ttlMillis);
        List<String> labels = page.buttons().stream().map(FormModel.Button::label).toList();
        boolean sent = bridge.sendSimpleForm(player, page.title(), page.content(), labels,
                new PlatformBridge.FormHandler() {
                    @Override
                    public void onSelect(int index) {
                        answer(player, layout, resolved, token, page, index, generation);
                    }

                    @Override
                    public void onClosedOrInvalid() {
                        // Closed = cancelled (§2.5.1): drop the
                        // token, run nothing.
                        if (sessions.hasLive(player.getUniqueId())) {
                            sessions.invalidate(player.getUniqueId());
                        }
                    }
                });
        if (!sent) {
            sessions.invalidate(player.getUniqueId());
            if (chestFallback != null) {
                return chestFallback.apply(player, layout.id());
            }
            return false;
        }
        return true;
    }

    // ---------- answering ----------

    private void answer(Player player, LayoutDef layout, List<ResolvedEntry> resolved,
                        FormSession.Token token, FormModel.FormPage page, int index,
                        long generation) {
        FormSession.Verdict verdict = sessions.answer(player.getUniqueId(), token.id(),
                player.getWorld().getName(), System.currentTimeMillis());
        if (verdict != FormSession.Verdict.OK) {
            plugin.getLogger().fine("ExcelMenu: dropped a stale form answer for "
                    + player.getName() + " (" + verdict + ", menu " + layout.id() + ")");
            return;
        }
        if (index < 0 || index >= page.buttons().size()) return;
        FormModel.Target target = page.buttons().get(index).target();
        if (target instanceof FormModel.Target.GroupTarget g) {
            openGroup(player, layout, resolved, g.group());
            return;
        }
        if (target instanceof FormModel.Target.EntryTarget e) {
            ResolvedEntry entry = null;
            for (ResolvedEntry r : resolved) {
                if (r.id().equals(e.entryId())) { entry = r; break; }
            }
            if (entry == null) return;
            StatsSink.tap(player, layout.id(), entry.id(), index, "bedrock");
            dispatcher.runAll(player, layout.id() + ":" + entry.id(), entry.action(),
                    session(player, layout, token.group(), generation));
        }
    }

    /** The dispatch session for a form-rendered view: close drops
     *  the form, refresh re-opens the same step, delayed lines check
     *  the generation they were issued under. */
    private ActionDispatcher.Session session(Player player, LayoutDef layout, String group,
                                             long generation) {
        return new ActionDispatcher.Session() {
            @Override
            public boolean valid(Player p) {
                return plugin.isEnabled() && p.isOnline()
                        && generations.getOrDefault(p.getUniqueId(), 0L) == generation;
            }

            @Override
            public void close(Player p) {
                invalidate(p);
            }

            @Override
            public void refresh(Player p) {
                List<ResolvedEntry> fresh = entries.entriesFor(layout.id(),
                        new BukkitViewerContext(p, bridge));
                if (group != null) {
                    openGroup(p, layout, fresh, group);
                } else {
                    open(p, layout, fresh);
                }
            }
        };
    }

    // ---------- §2.5 invalidation listeners ----------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invalidate(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // A form answered from another world is stale by the token's
        // own world binding; dropping the live token at the change
        // makes the refusal immediate instead of at-answer.
        sessions.invalidate(event.getPlayer().getUniqueId());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
