package com.winkreyes.excelwandbars;

import com.winkreyes.excelwandbars.core.SpellTip;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 0.2.2 first-use tip for the Spell Book screen and the progress line
 * (core.SpellTip). Repeats until OK, at most once a minute.
 * Fires when a player first meets the feature: the Spell Book opens, or a
 * progress line shows. If ExcelTips is running, the tip is handed to it
 * (registered as "spellscreen", until OK) and this plugin does not show its
 * own copy, so a player never gets both. ExcelTips is never required.
 */
final class SpellTipHook implements Listener {
    private final ExcelWandBars plugin;
    private final SpellTip gate = new SpellTip();
    private final File file;
    private long shown = 0, handed = 0;

    SpellTipHook(ExcelWandBars plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "spellscreen-tips.yml");
    }

    SpellTip gate() { return gate; }
    long shown() { return shown; }
    long handed() { return handed; }

    void loadSettings(ConfigurationSection c) {
        gate.enabled = c == null || c.getBoolean("enabled", true);
        gate.maxShows = c == null ? 0 : Math.max(0, c.getInt("max-shows", 0));
        gate.showGapMs = c == null ? 60_000 : Math.max(0, (long) (c.getDouble("show-gap-seconds", 60) * 1000));
    }

    void load() {
        gate.clearAll();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("players");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            try { gate.put(UUID.fromString(k), s.getInt(k + ".shows", 0), s.getBoolean(k + ".ack", false)); }
            catch (IllegalArgumentException ignored) {}
        }
        gate.saved();
    }

    void save() {
        if (!gate.dirty()) return;
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of("ExcelWandBars 0.2.2 Spell Book tip (written by the plugin).",
                "shows = times the tip showed, ack = player said OK (then it never shows again)."));
        Map<String, UUID> sorted = new TreeMap<>();
        for (UUID id : gate.known()) sorted.put(id.toString(), id);
        for (Map.Entry<String, UUID> e : sorted.entrySet()) {
            y.set("players." + e.getKey() + ".shows", gate.shows(e.getValue()));
            y.set("players." + e.getKey() + ".ack", gate.acked(e.getValue()));
        }
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "spellscreen-tips.yml.tmp");
            y.save(tmp);
            if (!tmp.renameTo(file)) java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            gate.saved();
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save spellscreen-tips.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ ExcelTips bridge (reflection, optional)

    private Class<?> tipsApi() {
        Plugin p = Bukkit.getPluginManager().getPlugin("ExcelTips");
        if (p == null || !p.isEnabled()) return null;
        try { return Class.forName("com.winkreyes.exceltips.ExcelTipsAPI", true, p.getClass().getClassLoader()); }
        catch (Throwable t) { return null; }
    }

    /** True when ExcelTips owns this tip right now (registers it on first use, again after an ExcelTips reload). */
    boolean tipsOwns() {
        if (!gate.enabled) return false;
        Class<?> c = tipsApi();
        if (c == null) return false;
        try {
            if (!(Boolean) c.getMethod("available").invoke(null)) return false;
            if ((Boolean) c.getMethod("hasFeature", String.class).invoke(null, SpellTip.FEATURE)) return true;
            Object ok = c.getMethod("register", Plugin.class, String.class, String.class, List.class, List.class, int.class)
                    .invoke(null, plugin, SpellTip.FEATURE, SpellTip.TITLE, SpellTip.body(false), SpellTip.body(true), gate.maxShows);
            return Boolean.TRUE.equals(ok);
        } catch (Throwable t) { return false; }
    }

    private boolean tipsFinished(UUID id) {
        Class<?> c = tipsApi();
        if (c == null) return false;
        try { return (Boolean) c.getMethod("isFinished", UUID.class, String.class).invoke(null, id, SpellTip.FEATURE); }
        catch (Throwable t) { return false; }
    }

    private void tipsAck(Player p) {
        Class<?> c = tipsApi();
        if (c == null) return;
        try { c.getMethod("acknowledge", Player.class, String.class).invoke(null, p, SpellTip.FEATURE); } catch (Throwable ignored) {}
    }

    private void tipsFire(Player p) {
        Class<?> c = tipsApi();
        if (c == null) return;
        try { c.getMethod("tip", Player.class, String.class).invoke(null, p, SpellTip.FEATURE); handed++; } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------------ triggers

    /** Should the Bedrock Spell Book carry the tip text and its OK button? */
    boolean pendingInBook(Player p) {
        if (!gate.enabled) return false;
        if (tipsOwns()) return !tipsFinished(p.getUniqueId());
        return gate.pending(p.getUniqueId());
    }

    /** inForm: the Bedrock Spell Book form already carries the tip and its OK button, so no chat copy. */
    void onBookOpened(Player p, boolean inForm) { trigger(p, inForm); }
    /** Java: Magic's spell chest opened (sneak + right click). The chat tip points to /spellbook. */
    void onJavaList(Player p) {
        TipHook wt = plugin.tips();
        if (wt != null && wt.shownWithin(p.getUniqueId(), 10_000)) return;
        trigger(p, false);
    }
    void onProgressShown(Player p) {
        // Never two tips at once: right after the wand tip, wait for the next progress line.
        TipHook wt = plugin.tips();
        if (wt != null && wt.shownWithin(p.getUniqueId(), 10_000)) return;
        trigger(p, false);
    }

    private void trigger(Player p, boolean inForm) {
        if (plugin.skipWorld(p.getWorld())) return;
        if (tipsOwns()) { if (!inForm) tipsFire(p); return; }
        SpellTip.Show r = gate.onTrigger(p.getUniqueId(), System.currentTimeMillis());
        if (plugin.debug()) plugin.getLogger().info(p.getName() + " spell tip -> " + r + " (" + gate.shows(p.getUniqueId()) + ")" + (inForm ? " in form" : ""));
        if (r != SpellTip.Show.SHOW) return;
        save();
        if (inForm) { shown++; return; }
        // Next tick, after the book or the cast message.
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) show(p); });
    }

    void show(Player p) {
        boolean bedrock = plugin.isBedrock(p);
        List<String> lines = SpellTip.lines(bedrock);
        shown++;
        for (int i = 0; i < lines.size() - 1; i++) p.sendMessage(Component.text((i == 0 ? "[Tip] " : "") + lines.get(i), i == 0 ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
        if (bedrock) {
            p.sendMessage(Component.text(lines.get(lines.size() - 1), NamedTextColor.YELLOW));
            return;
        }
        p.sendMessage(Component.text("Click ", NamedTextColor.YELLOW)
                .append(Component.text("[OK]", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text("Hide this tip")))
                        .clickEvent(ClickEvent.runCommand("/spellbook ok")))
                .append(Component.text(" to hide this tip.", NamedTextColor.YELLOW)));
    }

    /** /spellbook ok, the Java [OK], or the OK button in the Bedrock Spell Book. */
    boolean ack(Player p, boolean say) {
        tipsAck(p);
        boolean fresh = gate.ack(p.getUniqueId());
        if (fresh) save();
        if (say) p.sendMessage(Component.text(SpellTip.DONE_ACK, NamedTextColor.GREEN));
        return fresh;
    }

    void reset(UUID id) { gate.reset(id); save(); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { gate.endSession(e.getPlayer().getUniqueId()); }
}
