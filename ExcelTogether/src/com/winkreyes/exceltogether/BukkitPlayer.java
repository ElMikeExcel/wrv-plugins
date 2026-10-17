package com.winkreyes.exceltogether;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Wraps a Bukkit Player for the party logic. Must be used on the main thread. */
public final class BukkitPlayer implements PlayerView {
    private final Player p; private final boolean bedrock;
    public BukkitPlayer(Player p) { this.p = p; this.bedrock = GeyserForms.isBedrock(p); }
    @Override public UUID id() { return p.getUniqueId(); }
    @Override public String realName() { return p.getName(); }
    @Override public String displayName() {
        try { return PlainTextComponentSerializer.plainText().serialize(p.displayName()); } catch (Throwable t) { return p.getName(); }
    }
    @Override public boolean bedrock() { return bedrock; }
    @Override public void say(String line) { p.sendMessage(Component.text(line, NamedTextColor.YELLOW)); }
    @Override public void actionBar(String t) { p.sendActionBar(Component.text(t, NamedTextColor.GREEN)); }
    @Override public void say(Panel panel) {
        for (Panel.Line l : panel.lines) {
            Component c = Component.empty();
            for (Panel.Seg s : l.segs) {
                Component t = Component.text(s.text, NamedTextColor.YELLOW);
                if (s.clickable()) {
                    t = Component.text(s.text, NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(Component.text(s.hover == null ? "Click" : "Click: " + s.hover)));
                    t = s.run != null ? t.clickEvent(ClickEvent.runCommand(s.run)) : t.clickEvent(ClickEvent.suggestCommand(s.suggest));
                }
                c = c.append(t);
            }
            p.sendMessage(c);
        }
    }
}
