/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * First-use tip (Mike's standing rule). If ExcelTips is installed and its tips.yml has the feature,
 * ExcelTips shows it (one tip system, same OK rules). Otherwise the built-in tip shows on each use until
 * the player says OK: a clickable [OK] on Java, /mark ok or an OK pop-up (Floodgate form) on Bedrock.
 */
final class Tips {
    private final ExcelMakersMark plugin;
    private final Map<UUID, Long> lastShown = new HashMap<>();
    private final Map<UUID, Long> lastForm = new HashMap<>();
    private final Map<UUID, Long> pendingForm = new HashMap<>();
    private final Set<UUID> formOpen = new HashSet<>();
    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();

    Tips(ExcelMakersMark plugin) { this.plugin = plugin; }

    // ---- ExcelTips bridge (reflection, no compile dependency) ----
    private Class<?> api() {
        if (!plugin.getConfig().getBoolean("tip.use-exceltips", true)) return null;
        Plugin t = Bukkit.getPluginManager().getPlugin("ExcelTips");
        if (t == null || !t.isEnabled()) return null;
        try { return Class.forName("com.winkreyes.exceltips.ExcelTipsAPI", true, t.getClass().getClassLoader()); }
        catch (Throwable e) { return null; }
    }
    private String feature() { return plugin.getConfig().getString("tip.exceltips-feature", "makersmark"); }

    /** True when ExcelTips is on and has our feature in its tips.yml. */
    boolean viaExcelTips() {
        Class<?> a = api();
        if (a == null) return false;
        try { return (Boolean) a.getMethod("hasFeature", String.class).invoke(null, feature()); }
        catch (Throwable e) { return false; }
    }

    /** One "use" of the feature (a signed craft, or the /mark command). */
    void use(Player p) {
        if (!plugin.getConfig().getBoolean("tip.enabled", true)) return;
        if (viaExcelTips()) {
            try { api().getMethod("tip", Player.class, String.class).invoke(null, p, feature()); } catch (Throwable ignored) { }
            return;
        }
        Prefs.P pr = plugin.prefs().get(p.getUniqueId());
        if (pr.tipOk) return;
        int max = plugin.getConfig().getInt("tip.max-shows", 0);
        if (max > 0 && pr.tipShows >= max) return;
        long now = System.currentTimeMillis();
        long gap = plugin.getConfig().getLong("tip.min-gap-seconds", 20) * 1000L;
        Long last = lastShown.get(p.getUniqueId());
        if (last != null && now - last < gap) return;
        lastShown.put(p.getUniqueId(), now);
        pr.tipShows++;
        plugin.prefs().changed();
        boolean bedrock = Bedrock.isBedrock(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            List<String> lines = plugin.getConfig().getStringList(bedrock ? "tip.bedrock" : "tip.java");
            String prefix = plugin.getConfig().getString("tip.prefix", "&b[Tip] &f");
            for (String l : lines) p.sendMessage(AMP.deserialize(prefix + l));
            if (bedrock) {
                p.sendMessage(AMP.deserialize("&7Type &e/mark ok &7to hide this tip."));
                if (plugin.getConfig().getBoolean("tip.bedrock-form", true)) {
                    pendingForm.put(p.getUniqueId(), System.currentTimeMillis());
                    // A command use (for example from the Bedrock compass) closes no inventory, so also try a few seconds later.
                    Bukkit.getScheduler().runTaskLater(plugin, () -> tryForm(p), 80L);
                }
            } else {
                Component ok = Component.text("[OK]", NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/mark ok"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to hide this tip")));
                p.sendMessage(Component.text("Got it? ", NamedTextColor.GRAY).append(ok)
                        .append(Component.text("  (or type /mark ok)", NamedTextColor.DARK_GRAY)));
            }
        }, 10L);
    }

    /** Bedrock pop-up only at a calm moment: after the crafting or anvil screen closes. */
    void inventoryClosed(Player p) {
        Long since = pendingForm.get(p.getUniqueId());
        if (since == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> tryForm(p), 30L);
    }

    void tryForm(Player p) {
        UUID u = p.getUniqueId();
        Long since = pendingForm.get(u);
        if (since == null || !p.isOnline()) return;
        long now = System.currentTimeMillis();
        if (now - since > 180_000L || plugin.prefs().get(u).tipOk) { pendingForm.remove(u); return; }
        if (p.getOpenInventory().getType() != InventoryType.CRAFTING) return; // another screen is open, wait
        Long lf = lastForm.get(u);
        if (lf != null && now - lf < plugin.getConfig().getLong("tip.bedrock-form-cooldown-seconds", 300) * 1000L) { pendingForm.remove(u); return; }
        if (formOpen.contains(u)) return;
        pendingForm.remove(u);
        lastForm.put(u, now);
        String content = String.join("\n\n", plugin.getConfig().getStringList("tip.bedrock"));
        formOpen.add(u);
        boolean sent = plugin.bedrock().simple(p, "Maker's mark", content, List.of("OK, got it!", "Remind me later"), clicked ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    formOpen.remove(u);
                    if (clicked == 0) ack(p, true);
                }));
        if (!sent) formOpen.remove(u);
    }

    /** /mark ok, the [OK] click, or the form's OK button. */
    boolean ack(Player p, boolean say) {
        boolean was = false;
        Class<?> a = api();
        if (a != null) {
            try { was = (Boolean) a.getMethod("acknowledge", Player.class, String.class).invoke(null, p, feature()); } catch (Throwable ignored) { }
        }
        Prefs.P pr = plugin.prefs().get(p.getUniqueId());
        was |= !pr.tipOk;
        pr.tipOk = true;
        pendingForm.remove(p.getUniqueId());
        plugin.prefs().changed();
        if (say) p.sendMessage(Component.text("Got it! That tip will not show again.", NamedTextColor.GREEN));
        return was;
    }

    void reset(UUID u) {
        Prefs.P pr = plugin.prefs().get(u);
        pr.tipOk = false; pr.tipShows = 0;
        lastShown.remove(u); lastForm.remove(u);
        plugin.prefs().changed();
    }

    void quit(UUID u) { pendingForm.remove(u); formOpen.remove(u); }
}
