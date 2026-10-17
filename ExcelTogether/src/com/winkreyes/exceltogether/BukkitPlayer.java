package com.winkreyes.exceltogether;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
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
    @Override public void actionBarDim(String t) { p.sendActionBar(Component.text(t, NamedTextColor.GRAY)); }
    /** WRV-068 cue: a small, quick title flash (0.15s in, 0.9s hold, 0.35s out), green for ON, gray for OFF. */
    @Override public void titleFlash(String t, boolean on) {
        p.showTitle(Title.title(Component.text(t, on ? NamedTextColor.GREEN : NamedTextColor.GRAY), Component.empty(),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(900), Duration.ofMillis(350))));
    }
    /** WRV-068 cue: one soft orb ping when the bonus turns on. Nothing when it turns off. */
    @Override public void cueSound() { p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.3f); }
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
