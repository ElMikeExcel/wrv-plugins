package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.winkreyes.excelwandbars.core.TipGate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 0.2.1 onboarding tip about the two wand styles (see core.TipGate).
 * Counts a "wand use" when a player left or right clicks with a Magic
 * (elmakers) wand in the main hand. Read only towards Magic: it never
 * changes the wand, the bar or the inventory. Stored in tips.yml.
 */
final class TipHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final TipGate gate = new TipGate();
    private final BedrockForms forms;
    private final File file;
    private final Map<UUID, Long> lastCombat = new HashMap<>();
    private long shown = 0, formsSent = 0;

    TipHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.forms = new BedrockForms(plugin.getLogger());
        this.file = new File(plugin.getDataFolder(), "tips.yml");
    }

    TipGate gate() { return gate; }
    long shown() { return shown; }
    long formsSent() { return formsSent; }

    void loadSettings(ConfigurationSection c) {
        gate.enabled = c == null || c.getBoolean("enabled", true);
        gate.maxUses = c == null ? 15 : Math.max(0, c.getInt("max-uses", 15));
        gate.useGapMs = c == null ? 2000 : Math.max(0, c.getLong("use-gap-ms", 2000));
        gate.combatQuietMs = c == null ? 10_000 : Math.max(0, c.getLong("bedrock-popup-combat-quiet-ms", 10_000));
    }

    void load() {
        gate.clearAll();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("players");
        if (s == null) return;
        int bad = 0;
        for (String k : s.getKeys(false)) {
            try {
                gate.put(UUID.fromString(k), s.getInt(k + ".uses", 0), s.getBoolean(k + ".ack", false));
            } catch (IllegalArgumentException ex) { bad++; }
        }
        gate.saved();
        if (bad > 0) plugin.getLogger().warning("tips.yml: " + bad + " unreadable line(s) skipped");
    }

    void save() {
        if (!gate.dirty()) return;
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of(
                "ExcelWandBars wand tip progress (written by the plugin).",
                "uses = wand uses that showed the tip (stops at max-uses), ack = player said OK."));
        Map<String, UUID> sorted = new TreeMap<>();
        for (UUID id : gate.known()) sorted.put(id.toString(), id);
        for (Map.Entry<String, UUID> e : sorted.entrySet()) {
            y.set("players." + e.getKey() + ".uses", gate.uses(e.getValue()));
            y.set("players." + e.getKey() + ".ack", gate.acked(e.getValue()));
        }
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "tips.yml.tmp");
            y.save(tmp);
            if (!tmp.renameTo(file)) java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            gate.saved();
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save tips.yml: " + ex.getMessage());
        }
    }

    /** /wandstyle ok, the Java [OK], the Bedrock OK button, or picking a style. */
    boolean ack(Player p) {
        boolean fresh = gate.ack(p.getUniqueId());
        if (fresh) save();
        return fresh;
    }

    void reset(UUID id) { gate.reset(id); save(); }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        // Not ignoreCancelled: clicks in the air arrive already "cancelled".
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) return;
        Player p = e.getPlayer();
        ItemStack it = e.getItem();
        if (it == null || it.getType().isAir()) return;
        try {
            if (!api.isWand(it)) return;
        } catch (Throwable t) { return; }
        if (plugin.skipWorld(p.getWorld())) return;
        TipGate.Show r = gate.onUse(p.getUniqueId(), System.currentTimeMillis());
        if (plugin.debug()) plugin.getLogger().info(p.getName() + " wand use -> tip " + r + " (" + gate.uses(p.getUniqueId()) + "/" + gate.maxUses + ")");
        if (r != TipGate.Show.SHOW) return;
        save();
        // Next tick, after Magic's own click handling and messages.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            // 0.2.2: this press opened the Bedrock Spell Book form (it carries its own tip); no second form/tip on top.
            SpellScreen ss = plugin.spellScreen();
            if (ss != null && ss.formWithin(p.getUniqueId(), 1500)) {
                if (plugin.debug()) plugin.getLogger().info(p.getName() + " wand tip held back: Spell Book form just opened");
                return;
            }
            show(p);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        long now = System.currentTimeMillis();
        if (e.getEntity() instanceof Player v) lastCombat.put(v.getUniqueId(), now);
        if (e instanceof EntityDamageByEntityEvent d) {
            Entity src = d.getDamager();
            if (src instanceof Projectile pr && pr.getShooter() instanceof Entity sh) src = sh;
            if (src instanceof Player a) lastCombat.put(a.getUniqueId(), now);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        gate.endSession(id);
        lastCombat.remove(id);
        lastShownAt.remove(id);
    }

    // ------------------------------------------------------------------ showing

    private final Map<UUID, Long> lastShownAt = new HashMap<>();
    /** 0.2.2: the wand tip showed to this player in the last ms milliseconds (so other tips wait). */
    boolean shownWithin(UUID id, long ms) { Long t = lastShownAt.get(id); return t != null && System.currentTimeMillis() - t < ms; }

    void show(Player p) {
        lastShownAt.put(p.getUniqueId(), System.currentTimeMillis());
        boolean bedrock = plugin.isBedrock(p);
        List<String> lines = TipGate.lines(bedrock);
        shown++;
        for (int i = 0; i < lines.size() - 1; i++) {
            p.sendMessage(Component.text(lines.get(i), i == 0 ? NamedTextColor.GOLD : NamedTextColor.YELLOW));
        }
        if (!bedrock) {
            p.sendMessage(Component.text("Click ", NamedTextColor.YELLOW)
                    .append(Component.text("[OK]", NamedTextColor.GREEN)
                            .hoverEvent(HoverEvent.showText(Component.text("Hide this tip")))
                            .clickEvent(ClickEvent.runCommand("/wandstyle ok")))
                    .append(Component.text(" to hide this tip.", NamedTextColor.YELLOW)));
            return;
        }
        p.sendMessage(Component.text(lines.get(lines.size() - 1), NamedTextColor.YELLOW));
        UUID id = p.getUniqueId();
        Long lc = lastCombat.get(id);
        if (!gate.takeForm(id, System.currentTimeMillis(), lc == null ? 0 : lc)) return;
        // A moment later, so the pop-up does not land in the middle of the click.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || gate.acked(id)) return;
            boolean sent = forms.simple(p, TipGate.TITLE, TipGate.formContent(), List.of(TipGate.OK_BUTTON), btn -> {
                if (btn != 0) return; // closed with X: not an OK, the chat tip keeps coming
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (p.isOnline() && ack(p)) p.sendMessage(Component.text(TipGate.DONE_ACK, NamedTextColor.GREEN));
                });
            });
            if (sent) formsSent++;
            if (plugin.debug() || !sent) plugin.getLogger().info("Wand tip pop-up for " + p.getName() + ": " + (sent ? "sent" : "not sent (no Geyser form connection)"));
        }, 20L);
    }
}
