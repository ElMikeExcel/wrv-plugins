package com.winkreyes.excelguide;

import com.winkreyes.excelguide.core.GuideEngine;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Feeds player chat into the GuideEngine and delivers its replies.
 * Handles the three doors: /ask is a command (main class); here, a
 * chat line starting with the persona summon ("Guide," / "Guide " /
 * "Valley Guide,") is the SUMMON door and every other public chat line
 * is the AMBIENT door. Also tracks recent damage per player for the
 * combat-quiet rule.
 */
public final class ChatListener implements Listener {

    private final ExcelGuide plugin;
    private final Map<UUID, Long> lastDamageAt = new ConcurrentHashMap<>();

    public ChatListener(ExcelGuide plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            lastDamageAt.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("excelguide.ask")) {
            return;
        }
        String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
        GuideEngine engine = plugin.engine();
        if (engine == null) {
            return;
        }
        GuideEngine.Door door = GuideEngine.Door.AMBIENT;
        String question = raw;
        String summonRest = summonRemainder(raw);
        if (summonRest != null) {
            if (summonRest.isBlank()) {
                return; // "Guide?" alone - no question to answer
            }
            door = GuideEngine.Door.SUMMON;
            question = summonRest;
        }
        boolean recentlyDamaged = false;
        Long dmg = lastDamageAt.get(player.getUniqueId());
        if (dmg != null) {
            recentlyDamaged = System.currentTimeMillis() - dmg
                    < plugin.config().combatQuietSeconds * 1000L;
        }
        List<String> onlineNames = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            onlineNames.add(p.getName());
        }
        List<GuideEngine.Reply> replies = engine.handle(
                new GuideEngine.PlayerRef(player.getUniqueId(), player.getName()),
                question, door, onlineNames, recentlyDamaged,
                asyncReplies -> {
                    // An AI answer (or its fallback non-answer) is
                    // ready: deliver on the main thread like any reply.
                    if (!asyncReplies.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> deliver(player, asyncReplies));
                    }
                });
        if (!replies.isEmpty()) {
            // Deliver on the main thread, right after the chat line.
            Bukkit.getScheduler().runTask(plugin, () -> deliver(player, replies));
        }
    }

    private void deliver(Player asker, List<GuideEngine.Reply> replies) {
        // 0.3.0: one delivery path for both doors (it also shows the
        // first-use tip after an "A player said" answer).
        plugin.deliver(asker, replies);
    }

    /**
     * If the line summons the Guide, returns the remainder after the
     * summon prefix ("Guide, what's..." -&gt; "what's..."); else null.
     * Matches: "guide", "valley guide" followed by a comma, colon, or
     * whitespace, case-insensitive.
     */
    static String summonRemainder(String raw) {
        if (raw == null) {
            return null;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        for (String prefix : new String[]{"valley guide", "guide"}) {
            if (lower.startsWith(prefix)) {
                String rest = raw.substring(prefix.length());
                if (!rest.isEmpty() && (rest.charAt(0) == ',' || rest.charAt(0) == ':'
                        || Character.isWhitespace(rest.charAt(0)))) {
                    return rest.replaceFirst("^[,\\:\\s]+", "");
                }
            }
        }
        return null;
    }
}
