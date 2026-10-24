package com.winkreyes.exceldeathrecord;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * 0.3.0: hands the Lost &amp; Found first-use tip to ExcelTips when
 * ExcelTips is running, so a player never gets two copies. Everything is
 * reflection: ExcelDeathRecord has no compile or load dependency on
 * ExcelTips and works the same without it (its own tip screen shows).
 *
 * With ExcelTips: the tip is registered as "lostfound" (shows until OK,
 * maxShows 0), each /lostfound open calls tip(player, "lostfound"), and an
 * OK given through ExcelTips ([OK] in chat, the Bedrock pop-up, /tips ok)
 * is copied into our own player state through ExcelTipsAckEvent, so the
 * tip stays hidden even if ExcelTips is removed later. An OK given through
 * our own command (/lostfound ok) is passed to ExcelTips the other way.
 */
final class TipsBridge {

    static final String FEATURE = "lostfound";
    private static final String API = "com.winkreyes.exceltips.ExcelTipsAPI";
    private static final String ACK_EVENT = "com.winkreyes.exceltips.ExcelTipsAckEvent";

    private final ExcelDeathRecord plugin;
    private final Listener ackListener = new Listener() { };
    private Class<?> api;
    private boolean registered;
    private boolean listening;

    TipsBridge(ExcelDeathRecord plugin) { this.plugin = plugin; }

    private Class<?> api() {
        Plugin t = Bukkit.getPluginManager().getPlugin("ExcelTips");
        if (t == null || !t.isEnabled()) { api = null; return null; }
        try {
            Class<?> c = Class.forName(API, true, t.getClass().getClassLoader());
            Object ok = c.getMethod("available").invoke(null);
            api = Boolean.TRUE.equals(ok) ? c : null;
        } catch (Throwable e) { api = null; }
        return api;
    }

    /** ExcelTips is running and our tip is registered (or tips.yml has a lostfound entry). */
    boolean active() {
        Class<?> c = api();
        if (c == null) { registered = false; return false; }
        try { return Boolean.TRUE.equals(c.getMethod("hasFeature", String.class).invoke(null, FEATURE)); }
        catch (Throwable e) { return false; }
    }

    /** Register (or re-register after ExcelTips reloads) the tip and the OK listener. Safe to call often. */
    void hook(List<String> java, List<String> bedrock, BiConsumer<UUID, String> onAck) {
        Class<?> c = api();
        if (c == null) { registered = false; return; }
        try {
            Method reg = c.getMethod("register", Plugin.class, String.class, String.class, List.class, List.class, int.class);
            registered = Boolean.TRUE.equals(reg.invoke(null, plugin, FEATURE, "Lost & Found", java, bedrock, 0));
        } catch (Throwable e) {
            registered = false;
            plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: ExcelTips register failed: " + e);
        }
        if (!listening) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Event> ev = (Class<? extends Event>) Class.forName(ACK_EVENT, true, c.getClassLoader());
                Method feature = ev.getMethod("getFeature");
                Method player = ev.getMethod("getPlayerId");
                Method source = ev.getMethod("getSource");
                Bukkit.getPluginManager().registerEvent(ev, ackListener, EventPriority.MONITOR, (l, e) -> {
                    if (!ev.isInstance(e)) return;
                    try {
                        if (!FEATURE.equals(feature.invoke(e))) return;
                        onAck.accept((UUID) player.invoke(e), String.valueOf(source.invoke(e)));
                    } catch (Throwable t) { /* never break ExcelTips' event */ }
                }, plugin);
                listening = true;
            } catch (Throwable e) {
                plugin.getLogger().warning("ExcelDeathRecord [Lost & Found]: ExcelTips OK listener failed: " + e);
            }
        }
        plugin.getLogger().info("ExcelDeathRecord [Lost & Found]: first-use tip handed to ExcelTips (" + (registered ? "registered" : "tips.yml entry or not usable") + ").");
    }

    /** ExcelTips went away: forget the listener so a later hook re-registers it. */
    void unhook() {
        HandlerList.unregisterAll(ackListener);
        listening = false;
        registered = false;
        api = null;
    }

    /** One use; ExcelTips decides whether to show it now. */
    boolean tip(Player p) {
        Class<?> c = api;
        if (c == null) return false;
        try { return Boolean.TRUE.equals(c.getMethod("tip", Player.class, String.class).invoke(null, p, FEATURE)); }
        catch (Throwable e) { return false; }
    }

    boolean isAcknowledged(UUID id) {
        Class<?> c = api;
        if (c == null) return false;
        try { return Boolean.TRUE.equals(c.getMethod("isAcknowledged", UUID.class, String.class).invoke(null, id, FEATURE)); }
        catch (Throwable e) { return false; }
    }

    /** Our own OK (typed /lostfound ok, or an older state file) tells ExcelTips too. */
    void acknowledge(Player p) {
        Class<?> c = api;
        if (c == null) return;
        try { c.getMethod("acknowledge", Player.class, String.class).invoke(null, p, FEATURE); }
        catch (Throwable e) { /* ignore */ }
    }
}
