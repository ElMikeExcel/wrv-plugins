package com.winkreyes.exceltips;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

import java.util.UUID;

/**
 * Tiny API for other plugins (Excel*). Call it by reflection or with a
 * compile-only dependency; check {@link #available()} first.
 *
 * <pre>
 *   if (ExcelTipsAPI.available() &amp;&amp; ExcelTipsAPI.hasFeature("myfeature")) ExcelTipsAPI.tip(player, "myfeature");
 * </pre>
 * Registering your own tip (ExcelTrash, ExcelMakersMark, ExcelWandBars, ...):
 * <pre>
 *   ExcelTipsAPI.register(this, "makersmark", "Maker's mark", javaLines, bedrockLines, -1);
 *   ...on each use:  ExcelTipsAPI.tip(player, "makersmark");
 *   ...own OK path:  ExcelTipsAPI.acknowledge(player, "makersmark");
 * </pre>
 * A tips.yml entry with the same name overrides the registered words and
 * numbers (so the server owner always has the last say). Registered tips go
 * away when the owning plugin is disabled. Listen to {@link ExcelTipsAckEvent}
 * to hear about OKs given through ExcelTips. All methods are main-thread only.
 */
public final class ExcelTipsAPI {
    private ExcelTipsAPI() {}
    static volatile ExcelTips plugin;

    /** ExcelTips is loaded and enabled. */
    public static boolean available() { ExcelTips p = plugin; return p != null && p.isEnabled(); }

    /** tips.yml has this feature and it is enabled. */
    public static boolean hasFeature(String feature) { ExcelTips p = plugin; return p != null && p.isEnabled() && p.hasFeature(feature); }

    /** One use of a feature. Shows the tip if its rules allow; true when it showed now. */
    public static boolean tip(Player player, String feature) { ExcelTips p = plugin; return p != null && p.isEnabled() && p.fire(player, feature, "api"); }

    /** The player said OK to this feature's tip (for example they picked a wand style). */
    public static boolean acknowledge(Player player, String feature) { ExcelTips p = plugin; return p != null && p.isEnabled() && p.ackQuiet(player.getUniqueId(), feature); }

    /**
     * Register a default tip owned by your plugin (trigger: your tip() calls).
     * java/bedrock: chat lines (bedrock null or empty = same as java). maxShows: -1 = server default,
     * 0 = until OK. Returns true when the tip is usable now. No long dashes in the text.
     */
    public static boolean register(Plugin owner, String feature, String title, List<String> java, List<String> bedrock, int maxShows) {
        ExcelTips p = plugin; return p != null && p.isEnabled() && p.register(owner, feature, title, java, bedrock, maxShows);
    }

    /** Short form: same lines for Java and Bedrock, server default show count. */
    public static boolean register(Plugin owner, String feature, String title, List<String> lines) { return register(owner, feature, title, lines, null, -1); }

    /** Remove a tip your plugin registered (tips.yml entries are not touched). */
    public static boolean unregister(Plugin owner, String feature) { ExcelTips p = plugin; return p != null && p.isEnabled() && p.unregister(owner, feature); }

    /** The player said OK to this tip. */
    public static boolean isAcknowledged(UUID player, String feature) { ExcelTips p = plugin; return p != null && p.book().acked(player, feature); }

    /** The tip will not show again (OK, or it reached its show limit). */
    public static boolean isFinished(UUID player, String feature) { ExcelTips p = plugin; return p != null && p.finished(player, feature); }
}
