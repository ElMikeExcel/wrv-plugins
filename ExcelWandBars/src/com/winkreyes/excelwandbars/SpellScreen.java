package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.SpellInventoryEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.ProgressGate;
import com.winkreyes.excelwandbars.core.ProgressText;
import com.winkreyes.excelwandbars.core.SpellFacts;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.2.2 "spell screen":
 *  1. Bedrock players on Classic (Magic chest mode): when Magic is about to open
 *     its chest spell list (SpellInventoryEvent, opening), cancel it and open
 *     the Spell Book form instead (with "Use this spell"). Geyser shows Magic's
 *     45-slot chest as a 54-slot double chest with 9 barrier blocks, which is
 *     what players saw. Java players and Sneak bars (inventory mode) are never
 *     touched. If the form cannot be sent, nothing is cancelled and Magic's
 *     chest opens as before.
 *  2. One throttled action-bar line after a counted cast ("Boon: 12/40 casts
 *     to Rank 2"). Skipped while an ExcelAttunement stance is on, so its
 *     countdown line is never covered. The XP bar (Magic's mana) is not touched.
 * Reads Magic only through its API events and Wand/Mage methods.
 */
final class SpellScreen implements Listener {
    private final ExcelWandBars plugin;
    private final MagicHook hook;
    private final ProgressGate gate = new ProgressGate();
    private boolean bedrockForm = true;
    private long debounceMs = 600;
    private final Map<UUID, Long> lastOpen = new HashMap<>();
    private final Map<UUID, String> lastLine = new HashMap<>();
    private long intercepted = 0, chestsLeft = 0, lines = 0;

    SpellScreen(ExcelWandBars plugin, MagicHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    void loadSettings(ConfigurationSection screen, ConfigurationSection progress) {
        bedrockForm = screen == null || screen.getBoolean("bedrock-form", true);
        debounceMs = screen == null ? 600 : Math.max(0, screen.getLong("debounce-ms", 600));
        gate.enabled = progress == null || progress.getBoolean("enabled", true);
        gate.minGapMs = progress == null ? 4000 : Math.max(0, (long) (progress.getDouble("min-gap-seconds", 4) * 1000));
        gate.skipDuringStance = progress == null || progress.getBoolean("skip-during-stance", true);
    }

    boolean bedrockForm() { return bedrockForm; }
    ProgressGate gate() { return gate; }
    long intercepted() { return intercepted; }
    long chestsLeft() { return chestsLeft; }
    long lines() { return lines; }
    String lastLine(Player p) { return lastLine.get(p.getUniqueId()); }

    // ------------------------------------------------------------------ 1. Bedrock Classic: form instead of chest

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpellInventory(SpellInventoryEvent e) {
        if (!e.isOpening()) return;
        Mage mage = e.getMage();
        Player p = mage == null ? null : mage.getPlayer();
        if (p == null || plugin.skipWorld(p.getWorld())) return;
        if (!plugin.isBedrock(p)) {
            // Java: Magic's chest is unchanged; the first-use tip points to /spellbook.
            SpellTipHook st = plugin.spellTip();
            // 5 ticks later, so a wand tip shown by the same press counts first (one tip at a time).
            if (st != null && plugin.spellBookEnabled()) Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) st.onJavaList(p); }, 5L);
            return;
        }
        if (!bedrockForm || !plugin.spellBookEnabled()) return;
        Wand wand = mage.getActiveWand();
        if (!(wand instanceof com.elmakers.mine.bukkit.wand.Wand w) || w.getMode() != com.elmakers.mine.bukkit.wand.WandMode.CHEST) return;
        long now = System.currentTimeMillis();
        Long last = lastOpen.get(p.getUniqueId());
        if (last != null && now - last >= 0 && now - last < debounceMs) {
            e.setCancelled(true); // the same press arriving twice: one Spell Book is enough
            return;
        }
        boolean shown;
        try {
            shown = hook.openSpellBook(p, null, true);
        } catch (Throwable t) {
            plugin.getLogger().warning("Spell screen error for " + p.getName() + ": " + t);
            shown = false;
        }
        if (shown) {
            lastOpen.put(p.getUniqueId(), now);
            lastForm.put(p.getUniqueId(), now);
            e.setCancelled(true);
            intercepted++;
            if (plugin.debug()) plugin.getLogger().info(p.getName() + " spell list -> Spell Book form (chest cancelled)");
        } else {
            chestsLeft++;
            if (plugin.debug()) plugin.getLogger().info(p.getName() + " spell list: form not sent, Magic chest left as is");
        }
    }

    private final java.util.Map<java.util.UUID, Long> lastForm = new java.util.HashMap<>();
    /** The Spell Book form replaced the chest for this player in the last ms milliseconds (one form at a time on Bedrock). */
    boolean formWithin(java.util.UUID id, long ms) { Long t = lastForm.get(id); return t != null && System.currentTimeMillis() - t < ms; }

    // ------------------------------------------------------------------ 2. progress line after a counted cast

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(CastEvent e) {
        if (!gate.enabled) return;
        try {
            if (e.getSpellResult() == null || !e.getSpellResult().isSuccess()) return; // only successful casts count
            Mage mage = e.getMage();
            Player p = mage == null ? null : mage.getPlayer();
            Spell spell = e.getSpell();
            if (p == null || spell == null || spell.getSpellKey() == null) return;
            if (plugin.skipWorld(p.getWorld())) return;
            String key = hook.baseKey(spell.getSpellKey().getBaseKey());
            // Magic adds the cast (and runs any rank-up) right after this event: read on the next tick.
            Bukkit.getScheduler().runTask(plugin, () -> afterCast(p, mage, key));
        } catch (Throwable t) {
            if (plugin.debug()) plugin.getLogger().info("progress line: " + t);
        }
    }

    String afterCast(Player p, Mage mage, String key) {
        if (!p.isOnline()) return "offline";
        Wand wand = mage.getActiveWand();
        if (wand == null) return "no-wand";
        SpellFacts f = hook.facts(wand, mage, key, 0);
        if (f == null) return "not-on-wand";
        boolean stance = inStance(p);
        ProgressGate.Result r = gate.decide(p.getUniqueId(), key, f.rank, f.castCount, f.pathOk ? f.nextRankCasts : 0, stance, System.currentTimeMillis());
        String line = null;
        if (r == ProgressGate.Result.RANKED_UP) line = ProgressText.rankedUp(f.name, f.rank);
        else if (r == ProgressGate.Result.SHOW) line = ProgressText.line(f);
        if (plugin.debug()) plugin.getLogger().info(p.getName() + " cast " + key + " r" + f.rank + " " + f.castCount + "/" + f.nextRankCasts + " -> " + r + (line == null ? "" : " '" + line + "'"));
        if (line == null) return r.name();
        p.sendActionBar(Component.text(line, r == ProgressGate.Result.RANKED_UP ? NamedTextColor.GOLD : NamedTextColor.GREEN));
        lastLine.put(p.getUniqueId(), line);
        lines++;
        SpellTipHook st = plugin.spellTip();
        if (st != null) st.onProgressShown(p);
        return r.name() + " " + line;
    }

    // ExcelAttunement (optional, by reflection): skip our line while a stance countdown is on.
    private Method stanceMethod;
    private Plugin stancePlugin;

    boolean inStance(Player p) {
        try {
            Plugin a = Bukkit.getPluginManager().getPlugin("ExcelAttunement");
            if (a == null || !a.isEnabled()) return false;
            if (a != stancePlugin) { stancePlugin = a; stanceMethod = a.getClass().getMethod("activeStance", Player.class); }
            Object st = stanceMethod.invoke(a, p);
            return st instanceof Enum<?> en && !en.name().equals("NONE");
        } catch (Throwable t) {
            return false; // older ExcelAttunement without activeStance: send the line once, its stance line wins next second
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastOpen.remove(id);
        lastLine.remove(id);
        gate.forget(id);
    }
}

