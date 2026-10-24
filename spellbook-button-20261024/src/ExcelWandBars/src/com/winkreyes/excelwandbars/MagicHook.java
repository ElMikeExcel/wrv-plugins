package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.magic.Messages;
import com.elmakers.mine.bukkit.api.spell.CastingCost;
import com.elmakers.mine.bukkit.api.spell.Spell;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.BarText;
import com.winkreyes.excelwandbars.core.FlipGate;
import com.winkreyes.excelwandbars.core.SpellFacts;
import com.winkreyes.excelwandbars.core.SpellInfoText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The only class that touches Magic (elmakers) 11.2.5. API calls used, all
 * verified against the 11.2.5 jar bytecode:
 *  - MagicAPI.getMage(Player), Mage.getActiveWand()
 *  - api Wand.isInventoryOpen(), cycleHotbar(), getCurrentHotbar(), getSpells(), getSpell(key, mage)
 *  - impl com.elmakers.mine.bukkit.wand.Wand.getMode() / getHotbars() (public, not on the api interface)
 *  - MagicAPI.getSpell(ItemStack) for the spell key of a bar icon
 * Wand.cycleHotbar(1) saves the open inventory, moves to (current+1) % bars,
 * redraws the bar and updates the active spell; it never touches the
 * player's stored real inventory, so it cannot duplicate or drop items.
 */
final class MagicHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final FlipGate gate = new FlipGate();
    private final BedrockForms forms;
    private long flips = 0;

    MagicHook(ExcelWandBars plugin, Plugin magic) {
        this.plugin = plugin;
        if (!(magic instanceof MagicAPI m)) throw new IllegalStateException("Magic plugin is not a MagicAPI");
        this.api = m;
        this.forms = new BedrockForms(plugin.getLogger());
        // Fail fast at enable if the impl class we rely on is missing (wrong Magic version).
        com.elmakers.mine.bukkit.wand.WandMode.valueOf("INVENTORY");
    }

    long flips() { return flips; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // only the press, never the release
        try {
            handleSneak(event.getPlayer(), true);
        } catch (Throwable t) {
            plugin.getLogger().warning("Sneak flip error for " + event.getPlayer().getName() + ": " + t);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gate.forget(event.getPlayer().getUniqueId());
    }

    FlipGate.Result handleSneak(Player player, boolean sneakStart) {
        FlipGate.State s = new FlipGate.State();
        s.sneakStart = sneakStart;
        Mage mage = api.getMage(player);
        Wand wand = mage == null ? null : mage.getActiveWand();
        s.hasActiveWand = wand != null;
        s.barsStyle = plugin.barsStyle(player);
        if (wand instanceof com.elmakers.mine.bukkit.wand.Wand w) {
            s.inventoryMode = w.getMode() == com.elmakers.mine.bukkit.wand.WandMode.INVENTORY;
            s.barCount = w.getHotbars() == null ? 0 : w.getHotbars().size();
        }
        s.barOpen = wand != null && wand.isInventoryOpen();
        s.skipWorld = plugin.skipWorld(player.getWorld());
        s.inVehicle = player.isInsideVehicle();
        s.swimming = player.isSwimming();
        s.gliding = player.isGliding();
        s.flying = player.isFlying();
        FlipGate.Result r = gate.decide(player.getUniqueId(), s, plugin.flipOptions(), System.currentTimeMillis());
        if (plugin.debug()) plugin.getLogger().info(player.getName() + " sneak -> " + r);
        if (r != FlipGate.Result.FLIP) return r;
        wand.cycleHotbar();
        flips++;
        if (plugin.showBarLine() && wand instanceof com.elmakers.mine.bukkit.wand.Wand w) {
            // Next tick, so this line lands after Magic's own "$spell" action-bar update.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                Wand now = mage.getActiveWand();
                if (now != wand || !now.isInventoryOpen()) return;
                String line = BarText.line(w.getCurrentHotbar(), w.getHotbars().size(), barNames(w, mage, w.getCurrentHotbar()));
                player.sendActionBar(Component.text(line, NamedTextColor.AQUA));
            });
        }
        return r;
    }

    private String keyOf(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        try {
            if (!api.isSpell(item)) return null;
            String k = api.getSpell(item);
            return k == null || k.isEmpty() ? null : k;
        } catch (Throwable t) { return null; }
    }

    private static String base(String key) {
        int i = key.indexOf('|');
        return i < 0 ? key : key.substring(0, i);
    }

    private List<String> barNames(com.elmakers.mine.bukkit.wand.Wand w, Mage mage, int bar) {
        List<String> names = new ArrayList<>();
        List<com.elmakers.mine.bukkit.wand.WandInventory> bars = w.getHotbars();
        if (bars == null || bar < 0 || bar >= bars.size()) return names;
        for (ItemStack it : bars.get(bar).getContents()) {
            String k = keyOf(it);
            if (k == null) continue;
            Spell s = w.getSpell(base(k), mage);
            names.add(s != null ? SpellInfoText.strip(s.getName()) : base(k));
        }
        return names;
    }

    /** Spell keys of the wand in bar order, with their 1-based bar (0 = not on a bar). */
    Map<String, Integer> orderedSpells(Wand wand) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (wand instanceof com.elmakers.mine.bukkit.wand.Wand w && w.getHotbars() != null) {
            List<com.elmakers.mine.bukkit.wand.WandInventory> bars = w.getHotbars();
            for (int b = 0; b < bars.size(); b++) {
                for (ItemStack it : bars.get(b).getContents()) {
                    String k = keyOf(it);
                    if (k != null) out.putIfAbsent(base(k), b + 1);
                }
            }
        }
        Collection<String> all = wand.getSpells();
        if (all != null) for (String k : all) if (k != null && !k.isEmpty()) out.putIfAbsent(base(k), 0);
        return out;
    }

    SpellFacts facts(Wand wand, Mage mage, String key, int bar) {
        Spell s = wand.getSpell(key, mage);
        if (s == null) return null;
        MageController c = api.getController();
        Messages msg = c.getMessages();
        SpellFacts f = new SpellFacts();
        f.key = key;
        f.bar = bar;
        f.name = SpellInfoText.strip(s.getName());
        f.description = SpellInfoText.strip(s.getDescription());
        f.rank = Math.max(1, s.getSpellKey().getLevel());
        int max = f.rank;
        for (int k = f.rank + 1; k <= 20; k++) {
            if (c.getSpellTemplate(base(key) + "|" + k) == null) break;
            max = k;
        }
        f.maxRank = max;
        addCosts(s.getCosts(), wand, msg, f.costs);
        addCosts(s.getActiveCosts(), wand, msg, f.activeCosts);
        f.cooldownMs = Math.max(0, s.getCooldown());
        f.range = s.getRange();
        ConfigurationSection params = s.getSpellParameters();
        String target = params == null ? null : params.getString("target");
        f.selfOnly = target != null && target.equalsIgnoreCase("self");
        // Magic's isToggleable() is true for nearly every spell ("may be cancelled"),
        // so "toggle" here means a sustained spell: it has a per-second active cost.
        f.toggle = !f.activeCosts.isEmpty();
        f.stopsManaRegen = s.disableManaRegenerationWhenActive();
        f.durationMs = Math.max(0, s.getDuration());
        f.nextRankCasts = Math.max(0, s.getRequiredUpgradeCasts());
        f.nextRankPath = s.getRequiredUpgradePath() == null ? "" : s.getRequiredUpgradePath();
        f.castCount = Math.max(0, s.getCastCount());
        return f;
    }

    private static void addCosts(Collection<CastingCost> costs, Wand wand, Messages msg, List<String> out) {
        if (costs == null) return;
        for (CastingCost cc : costs) {
            if (cc == null) continue;
            int mana = cc.getMana(wand);
            if (mana > 0) { out.add(mana + " Mana"); continue; }
            if (cc.isEmpty(wand)) continue;
            String d = SpellInfoText.strip(cc.getFullDescription(msg, wand));
            if (!d.isEmpty()) out.add(d);
        }
    }

    /**
     * 0.2.1: the wand the spell book reads. The held (active) wand first, as in
     * 0.2.0. If none is held (e.g. the player tapped Spellbook in the compass
     * menu, so the compass is in hand) the first wand in the hotbar, then the
     * rest of the inventory, then the offhand, that this player may use.
     * Magic's getWand(item) builds a Wand around the item and may write to it,
     * so it only ever gets a CLONE: the real item is never touched.
     */
    Wand bookWand(Player p, Mage mage) {
        Wand held = mage == null ? null : mage.getActiveWand();
        if (held != null) return held;
        if (mage == null) return null;
        Wand firstUsable = null;
        List<ItemStack> look = new ArrayList<>();
        for (ItemStack it : p.getInventory().getStorageContents()) look.add(it);
        look.add(p.getInventory().getItemInOffHand());
        for (ItemStack it : look) {
            if (it == null || it.getType().isAir()) continue;
            try {
                if (!api.isWand(it)) continue;
                Wand w = api.getWand(it.clone());
                if (w == null || !w.canUse(p)) continue;
                Collection<String> sp = w.getSpells();
                if (sp != null && !sp.isEmpty()) return w;
                if (firstUsable == null) firstUsable = w;
            } catch (Throwable t) {
                if (plugin.debug()) plugin.getLogger().info("spell book: skipped an item (" + t + ")");
            }
        }
        return firstUsable;
    }

    private List<SpellFacts> book(Player p) {
        Mage mage = api.getMage(p);
        Wand wand = bookWand(p, mage);
        List<SpellFacts> out = new ArrayList<>();
        if (wand == null) return null;
        for (Map.Entry<String, Integer> e : orderedSpells(wand).entrySet()) {
            SpellFacts f = facts(wand, mage, e.getKey(), e.getValue());
            if (f != null) out.add(f);
        }
        return out;
    }

    /** /spellinfo [spell]. Reads only; never changes the wand, the bar or the inventory. */
    void openSpellBook(Player p, String only) {
        List<SpellFacts> book = book(p);
        if (book == null) {
            p.sendMessage(Component.text("No Magic wand found. Keep your wand in your inventory (take it out of your Utility Pouch if it's in there), then try /spellbook again.", NamedTextColor.GRAY));
            return;
        }
        if (book.isEmpty()) {
            p.sendMessage(Component.text("This wand has no spells yet.", NamedTextColor.GRAY));
            return;
        }
        SpellFacts pick = null;
        if (only != null) {
            String q = only.toLowerCase(Locale.ROOT);
            for (SpellFacts f : book) if (f.key.equalsIgnoreCase(q) || f.name.equalsIgnoreCase(only)) { pick = f; break; }
        }
        boolean bedrock = BedrockForms.isBedrock(p);
        if (bedrock) {
            if (pick != null) { sendDetailForm(p, pick); return; }
            if (sendListForm(p, book)) return;
            // form failed: fall through to chat
        }
        if (pick != null) {
            for (String line : SpellInfoText.lines(pick)) p.sendMessage(Component.text(line, NamedTextColor.YELLOW));
            return;
        }
        p.sendMessage(Component.text("Spell Book (hover or click a spell)", NamedTextColor.GOLD));
        boolean bars = plugin.barsStyle(p);
        for (SpellFacts f : book) {
            String label = (bars && f.bar > 0 ? "Bar " + f.bar + ": " : "") + SpellInfoText.button(f);
            Component hover = Component.text(String.join("\n", SpellInfoText.lines(f)));
            p.sendMessage(Component.text(" " + label, NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(hover))
                    .clickEvent(ClickEvent.runCommand("/spellinfo " + f.key)));
        }
        if (plugin.styleHook() != null && plugin.styleHook().enabled()) {
            p.sendMessage(Component.text(" [Wand controls: " + (bars ? "Sneak bars" : "Classic") + " - click to change]", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/wandstyle")));
        }
    }

    private boolean sendListForm(Player p, List<SpellFacts> book) {
        List<String> buttons = new ArrayList<>();
        boolean bars = plugin.barsStyle(p);
        for (SpellFacts f : book) buttons.add((bars && f.bar > 0 ? "Bar " + f.bar + ": " : "") + SpellInfoText.button(f));
        String content = "Tap a spell to read what it does, its cost and its cooldown.\n"
                + (bars ? "Sneak once with the bar open to flip to the next bar."
                        : "Right click picks the next spell. Sneak + right click opens the spell list.");
        boolean styleButton = plugin.styleHook() != null && plugin.styleHook().enabled();
        if (styleButton) buttons.add("Wand controls: " + (bars ? "Sneak bars" : "Classic") + " (change)");
        return forms.simple(p, "Spell Book", content, buttons, id -> {
            if (styleButton && id == book.size()) {
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && plugin.styleHook() != null) plugin.styleHook().openChooser(p); });
                return;
            }
            if (id < 0 || id >= book.size()) return;
            SpellFacts f = book.get(id);
            Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) sendDetailForm(p, f); });
        });
    }

    private void sendDetailForm(Player p, SpellFacts f) {
        String content = String.join("\n\n", SpellInfoText.lines(f).subList(1, SpellInfoText.lines(f).size()));
        boolean sent = forms.simple(p, f.name + " (" + SpellInfoText.rankText(f) + ")", content, List.of("Back to Spell Book", "Close"), id -> {
            if (id == 0) Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) openSpellBook(p, null); });
        });
        if (!sent) for (String line : SpellInfoText.lines(f)) p.sendMessage(Component.text(line, NamedTextColor.YELLOW));
    }

    /** Console view for the rig test and for admins: bar state plus each spell's summary. */
    List<String> describeForConsole(Player p) {
        List<String> out = new ArrayList<>();
        Mage mage = api.getMage(p);
        Wand wand = mage == null ? null : mage.getActiveWand();
        if (wand == null) { out.add(p.getName() + ": no active wand"); return out; }
        String mode = "?";
        int count = -1;
        if (wand instanceof com.elmakers.mine.bukkit.wand.Wand w) {
            mode = String.valueOf(w.getMode());
            count = w.getHotbars() == null ? 0 : w.getHotbars().size();
        }
        out.add(p.getName() + ": wand " + SpellInfoText.strip(wand.getName()) + " mode " + mode + " open " + wand.isInventoryOpen()
                + " bar " + (wand.getCurrentHotbar() + 1) + "/" + count + " active " + wand.getActiveSpellKey());
        if (wand instanceof com.elmakers.mine.bukkit.wand.Wand w) {
            for (int b = 0; b < count; b++) out.add("  " + BarText.line(b, count, barNames(w, mage, b)));
        }
        for (SpellFacts f : book(p)) out.add("  [" + f.key + "] " + SpellInfoText.button(f) + " :: " + SpellInfoText.summary(f)
                + (f.toggle ? " | toggle" : "") + (f.stopsManaRegen ? " | no regen" : "")
                + (f.nextRankCasts > 0 ? " | next rank " + f.nextRankCasts + " casts " + f.nextRankPath : "")
                + (f.description.isEmpty() ? " | NO DESCRIPTION" : " | " + f.description));
        return out;
    }
}
