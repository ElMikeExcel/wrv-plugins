package com.winkreyes.leaderboard.paper;

import com.winkreyes.leaderboard.ExcelLeaderboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** The Java presentation: a clickable chat panel in the same compact
 * design language as the Bedrock form. /leaderboard prints one line per
 * board (click runs "/leaderboard board <key>"); a board prints its top
 * 10 as clean text with the viewer's rank pinned in green and a
 * clickable back line. All strings are built by BoardPresentation; this
 * class only colours and sends them. */
public final class ChatBoardMenu {

    private final ExcelLeaderboard plugin;

    public ChatBoardMenu(ExcelLeaderboard plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        for (BoardPresentation.ChatLine line : BoardPresentation.mainChatLines(plugin.snapshot().boards())) {
            player.sendMessage(render(line));
        }
    }

    public void openBoard(Player player, Category category) {
        ExcelLeaderboard.Snapshot snapshot = plugin.snapshot();
        for (BoardPresentation.ChatLine line
                : BoardPresentation.boardChatLines(snapshot.boards(), category,
                        player.getUniqueId(), snapshot.detailsFor(category))) {
            player.sendMessage(render(line));
        }
    }

    private static Component render(BoardPresentation.ChatLine line) {
        Component component = switch (line.kind()) {
            case HEADER -> Component.text(line.text(), NamedTextColor.GOLD);
            case ENTRY -> entryComponent(line.text());
            case RANK -> rankComponent(line.text());
            case OWN_RANK -> Component.text(line.text(), NamedTextColor.GREEN);
            case BACK -> Component.text(line.text(), NamedTextColor.AQUA);
            case NOTE -> Component.text(line.text(), NamedTextColor.GRAY);
        };
        if (line.clickCommand() != null) {
            component = component.clickEvent(ClickEvent.runCommand(line.clickCommand()));
        }
        if (line.hoverText() != null) {
            component = component.hoverEvent(HoverEvent.showText(Component.text(line.hoverText())));
        }
        return component;
    }

    /** "» Top Miner — Name · 12,340": aqua arrow, white title, gray
     * leader tail. The "» " prefix and " — " separator are guaranteed
     * by the builder; without a leader the tail is absent. */
    private static Component entryComponent(String text) {
        String body = text.startsWith("» ") ? text.substring(2) : text;
        int split = body.indexOf(" — ");
        Component component = Component.text("» ", NamedTextColor.AQUA);
        if (split < 0) {
            return component.append(Component.text(body, NamedTextColor.WHITE));
        }
        return component
                .append(Component.text(body.substring(0, split), NamedTextColor.WHITE))
                .append(Component.text(body.substring(split), NamedTextColor.GRAY));
    }

    /** "#1 Name · 12,340": the rank token gray, the rest white. */
    private static Component rankComponent(String text) {
        int split = text.indexOf(' ');
        if (split < 0) {
            return Component.text(text, NamedTextColor.WHITE);
        }
        return Component.text(text.substring(0, split), NamedTextColor.GRAY)
                .append(Component.text(text.substring(split), NamedTextColor.WHITE));
    }
}
