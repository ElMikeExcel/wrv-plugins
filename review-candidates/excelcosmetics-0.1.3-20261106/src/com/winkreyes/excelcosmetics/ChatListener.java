package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.ChatFormatter;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * The painter. Chat is painted server-side from per-player state,
 * so Java and Bedrock (whose chat fires the same AsyncChatEvent
 * through Geyser) see the identical line — parity by construction.
 *
 * When a player has no appearance and token replacement is off,
 * the event is left untouched and vanilla chat (signed, default
 * renderer) flows exactly as before.
 */
final class ChatListener implements Listener {

    private final ExcelCosmetics plugin;
    private final boolean rendererEnabled;
    private final boolean tokenReplacement;

    ChatListener(ExcelCosmetics plugin, boolean rendererEnabled, boolean tokenReplacement) {
        this.plugin = plugin;
        this.rendererEnabled = rendererEnabled;
        this.tokenReplacement = tokenReplacement;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!rendererEnabled) return;
        Catalog catalog = plugin.catalog();
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        PlayerState state = plugin.store().peek(player.getUniqueId());
        PlayerState.Appearance appearance = state != null
                ? state.appearance(catalog, now) : PlayerState.Appearance.PLAIN;
        boolean premium = tokenReplacement && state != null && catalog.premiumTokensEnabled()
                && state.hasAnyActive(Kind.EMOTES, now);
        if (appearance.isPlain() && !tokenReplacement) return;

        String displayName = PlainTextComponentSerializer.plainText().serialize(player.displayName());
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        ChatFormatter.Plan plan = ChatFormatter.format(catalog, appearance, displayName, rawMessage, premium);
        if (!tokenReplacement) {
            // The formatter always rewrites basic tokens; with the
            // toggle off the message text must pass through raw.
            plan = new ChatFormatter.Plan(plan.name(),
                    List.of(ChatFormatter.Segment.plain(rawMessage, appearance.rgb())));
        }
        ChatFormatter.Plan finalPlan = plan;
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            List<Component> parts = new ArrayList<>();
            parts.add(toComponent(finalPlan.name()));
            parts.add(Component.text(": "));
            parts.add(toComponent(finalPlan.message()));
            return Component.text("").children(parts);
        });
    }

    private Component toComponent(List<ChatFormatter.Segment> segments) {
        List<Component> parts = new ArrayList<>();
        for (ChatFormatter.Segment seg : segments) {
            Component c = Component.text(seg.text());
            if (seg.rgb() != null) c = c.color(TextColor.color(seg.rgb()));
            if (seg.bold()) c = c.decorate(TextDecoration.BOLD);
            if (seg.italic()) c = c.decorate(TextDecoration.ITALIC);
            if (seg.obfuscated()) c = c.decorate(TextDecoration.OBFUSCATED);
            parts.add(c);
        }
        return Component.text("").children(parts);
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
