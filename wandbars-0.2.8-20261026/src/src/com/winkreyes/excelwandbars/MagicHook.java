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
        lastForms.remove(event.getPlayer().getUniqueId());
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
        if (r == FlipGate.Result.ONE_BAR && s.barOpen && pagesWord(player) && plugin.showBarLine()) {
            // 0.2.5: crouch always answers. All spells fit on one page: say so instead of doing nothing.
            int n = wand instanceof com.elmakers.mine.bukkit.wand.Wand w1 ? w1.getSpellInventory().size() : 0;
            player.sendActionBar(Component.text("Page 1/1: all " + n + " of your spells are on the bar", NamedTextColor.AQUA));
        }
        if (r != FlipGate.Result.FLIP) return r;
        wand.cycleHotbar();
        flips++;
        if (plugin.showBarLine() && wand instanceof com.elmakers.mine.bukkit.wand.Wand w) {
            // Next tick, so this line lands after Magic's own "$spell" action-bar update.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                Wand now = mage.getActiveWand();
                if (now != wand || !now.isInventoryOpen()) return;
                String line = pagesWord(player)
                        ? com.winkreyes.excelwandbars.core.PageLayout.pageLine(w.getCurrentHotbar(), w.getHotbars().size(), barNames(w, mage, w.getCurrentHotbar()))
                        : BarText.line(w.getCurrentHotbar(), w.getHotbars().size(), barNames(w, mage, w.getCurrentHotbar()));
                player.sendActionBar(Component.text(line, NamedTextColor.AQUA));
            });
        }
        return r;
    }

    /** 0.2.5: Tap bar and Tap to pick (original) call them pages; Sneak bars keeps "Bar". */
    private boolean pagesWord(Player p) {
        StyleHook s = plugin.styleHook();
        if (s == null || !s.enabled()) return false;
        com.winkreyes.excelwandbars.core.WandStyle st = s.styleOf(p);
        return st == com.winkreyes.excelwandbars.core.WandStyle.TAPBAR || st == com.winkreyes.excelwandbars.core.WandStyle.ORIGINAL;
    }

    /** 0.2.5: "Page 1/2: Heal, Boon, ..." for the open bar, or null when it has one page (or is closed). */
    String pageLine(Player p) {
        Mage mage = api.getMage(p);
        Wand wand = mage == null ? null : mage.getActiveWand();
        if (!(wand instanceof com.elmakers.mine.bukkit.wand.Wand w) || !w.isInventoryOpen()) return null;
        int count = w.getHotbars() == null ? 0 : w.getHotbars().size();
        if (count < 2) return null;
        return com.winkreyes.excelwandbars.core.PageLayout.pageLine(w.getCurrentHotbar(), count, barNames(w, mage, w.getCurrentHotbar()))
                + (plugin.isBedrock(p) ? " (crouch: next)" : " (sneak: next)");
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
        ConfigurationSection params = s.getSpellParameters();
        // Spell info 2026-10-26: the real wait, as Magic's BaseSpell.updateCooldown works it out:
        // ceil((1 - wand cooldown_reduction) * cooldown), and the same for cooldown_mage
        // (a pause before the caster's next spell) unless the spell has bypass_mage_cooldown.
        double red = cooldownReduction(wand);
        f.cooldownMs = reduced(Math.max(0, s.getCooldown()), red);
        f.mageCooldownMs = mageCooldown(params, red);
        f.range = s.getRange();
        String target = params == null ? null : params.getString("target");
        f.selfOnly = target != null && target.equalsIgnoreCase("self");
        if ((target != null && target.equalsIgnoreCase("none")) || s.isPassive()) f.range = 0;
        f.effects.addAll(effectsOf(s));
        // Magic's isToggleable() is true for nearly every spell ("may be cancelled"),
        // so "toggle" here means a sustained spell: it has a per-second active cost.
        f.toggle = !f.activeCosts.isEmpty();
        f.stopsManaRegen = s.disableManaRegenerationWhenActive();
        f.durationMs = Math.max(0, s.getDuration());
        f.nextRankCasts = Math.max(0, s.getRequiredUpgradeCasts());
        f.nextRankPath = s.getRequiredUpgradePath() == null ? "" : s.getRequiredUpgradePath();
        f.castCount = Math.max(0, s.getCastCount());
        // 0.2.2: does the wand's current rank already allow the next spell rank?
        f.pathOk = pathAllows(wand, f.nextRankPath);
        f.rankByWand = !f.pathOk && pathless(wand);
        String act = wand.getActiveSpellKey();
        f.active = act != null && base(act).equalsIgnoreCase(base(key));
        if (f.rankByWand && f.rank < f.maxRank) nextByWand(wand, f, red);
        return f;
    }

    // ------------------------------------------------------------------ spell info 2026-10-26

    static double cooldownReduction(Wand wand) {
        try { return Math.max(0, wand.getCooldownReduction()); } catch (Throwable t) { return 0; }
    }

    static long reduced(long ms, double red) {
        if (ms <= 0 || red >= 1) return 0;
        return (long) Math.ceil((1.0f - (float) red) * ms);
    }

    static long mageCooldown(ConfigurationSection params, double red) {
        if (params == null || params.getBoolean("bypass_mage_cooldown", false)) return 0;
        return reduced(Math.max(0, params.getInt("cooldown_mage", 0)), red);
    }

    /** Magic config (sections, lists of sections) as plain maps and lists for the pure core. */
    static Object plain(Object o) {
        if (o instanceof ConfigurationSection cs) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String k : cs.getKeys(false)) m.put(k, plain(cs.get(k)));
            return m;
        }
        if (o instanceof Map<?, ?> mm) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : mm.entrySet()) m.put(String.valueOf(e.getKey()), plain(e.getValue()));
            return m;
        }
        if (o instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object x : l) out.add(plain(x));
            return out;
        }
        return o;
    }

    /** The plain-words effect lines of one spell rank, from its merged Magic config. */
    @SuppressWarnings("unchecked")
    List<String> effectsOf(SpellTemplate t) {
        try {
            ConfigurationSection cfg = t == null ? null : t.getConfiguration();
            if (cfg == null) return new ArrayList<>();
            return com.winkreyes.excelwandbars.core.SpellEffects.lines((Map<String, Object>) plain(cfg));
        } catch (Throwable e) {
            if (plugin.debug()) plugin.getLogger().info("spell effects " + (t == null ? "?" : t.getKey()) + ": " + e);
            return new ArrayList<>();
        }
    }

    /** The spell rank a wand rank (path) gives: Magic's level_spells_to_path rule. */
    int levelAt(String baseKey, int maxLevel, com.elmakers.mine.bukkit.api.wand.WandUpgradePath path) {
        MageController c = api.getController();
        return com.winkreyes.excelwandbars.core.SpellEffects.levelAt(maxLevel,
                lvl -> {
                    SpellTemplate t = c.getSpellTemplate(lvl <= 1 ? baseKey : baseKey + "|" + lvl);
                    return t == null ? null : t.getRequiredUpgradePath();
                },
                need -> path.hasPath(need));
    }

    /** One rank's facts from a template (no player data): effects, cost, cooldown, range. */
    SpellFacts templateFacts(SpellTemplate t, Wand wand, double red) {
        SpellFacts g = new SpellFacts();
        Messages msg = api.getController().getMessages();
        addCosts(t.getCosts(), wand, msg, g.costs);
        addCosts(t.getActiveCosts(), wand, msg, g.activeCosts);
        ConfigurationSection params = t.getSpellParameters();
        g.cooldownMs = reduced(Math.max(0, t.getCooldown()), red);
        g.mageCooldownMs = mageCooldown(params, red);
        g.range = t.getRange();
        String target = params == null ? null : params.getString("target");
        g.selfOnly = target != null && target.equalsIgnoreCase("self");
        if ((target != null && target.equalsIgnoreCase("none")) || t.isPassive()) g.range = 0;
        g.effects.addAll(effectsOf(t));
        return g;
    }

    /**
     * Rank wands (no Magic path): the first higher wand rank that raises this spell,
     * the spell rank it gives, what changes, and the configured "how to get there" text.
     */
    void nextByWand(Wand wand, SpellFacts f, double red) {
        try {
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath path = rankFromTemplate(wand);
            if (path == null) return;
            String b = base(f.key);
            MageController c = api.getController();
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath up = path.getUpgrade();
            for (int i = 0; up != null && i < 8; i++, up = up.getUpgrade()) {
                int lvl = levelAt(b, f.maxRank, up);
                if (lvl <= f.rank) continue;
                SpellTemplate now = c.getSpellTemplate(f.rank <= 1 ? b : b + "|" + f.rank);
                SpellTemplate next = c.getSpellTemplate(lvl <= 1 ? b : b + "|" + lvl);
                if (next == null) return;
                f.nextWandRank = plugin.rankDisplay(up.getKey(), SpellInfoText.strip(up.getName()));
                f.nextWandRankLevel = lvl;
                List<String> was = now == null ? SpellInfoText.rankLines(f) : SpellInfoText.rankLines(templateFacts(now, wand, red));
                f.nextChanges.addAll(com.winkreyes.excelwandbars.core.SpellEffects.changes(was, SpellInfoText.rankLines(templateFacts(next, wand, red))));
                f.rankUpHow = plugin.rankUpHow(up.getKey(), f.nextWandRank);
                return;
            }
        } catch (Throwable t) {
            if (plugin.debug()) plugin.getLogger().info("next rank " + f.key + ": " + t);
        }
    }

    /** Admin/console check: every line each spell on a player's book shows. */
    List<String> dump(Player p) {
        List<String> out = new ArrayList<>();
        List<SpellFacts> book = book(p);
        if (book == null) { out.add("No Magic wand found on " + p.getName()); return out; }
        Wand bw = bookWand(p, api.getMage(p));
        if (bw != null) for (String h : rankHeader(bw, book)) out.add("[header] " + h);
        for (SpellFacts f : book) {
            out.add("== " + f.key);
            for (String l : SpellInfoText.lines(f)) out.add("   " + l);
        }
        return out;
    }

    /** Admin/console check: the effect lines of one spell template key (e.g. mend|3). */
    List<String> dumpTemplate(String key) {
        List<String> out = new ArrayList<>();
        SpellTemplate t = api.getController().getSpellTemplate(key);
        if (t == null) { out.add("No spell template " + key); return out; }
        out.add("== " + key);
        for (String l : effectsOf(t)) out.add("   " + l);
        return out;
    }

    /** True when the wand's rank is (or comes after) the rank a spell upgrade needs. */
    /** 0.2.2: a wand with no Magic path (WRV rank wands: no path, no default_wand_path). Magic never ranks its spells up by casts. */
    static boolean pathless(Wand wand) {
        try { return wand.getPath() == null; } catch (Throwable t) { return false; }
    }

    /** The rank of a pathless rank wand, from its template's level_spells_to_path (then the template key). */
    com.elmakers.mine.bukkit.api.wand.WandUpgradePath rankFromTemplate(Wand wand) {
        try {
            com.elmakers.mine.bukkit.api.wand.WandTemplate t = wand.getTemplate();
            for (int i = 0; t != null && i < 8; i++, t = t.getParent()) {
                String k = t.getConfiguration() == null ? null : t.getConfiguration().getString("level_spells_to_path");
                if (k != null && !k.isBlank() && api.getController().getPath(k) != null) return api.getController().getPath(k);
            }
            String key = wand.getTemplateKey();
            return key == null ? null : api.getController().getPath(key);
        } catch (Throwable t) { return null; }
    }

    static boolean pathAllows(Wand wand, String need) {
        if (need == null || need.isBlank()) return true;
        try {
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath path = wand.getPath();
            return path != null && path.hasPath(need);
        } catch (Throwable t) { return true; }
    }

    /** 0.2.2 rank header: current rank, next rank and what moves the wand up. Never mentions seals. */
    List<String> rankHeader(Wand wand, List<SpellFacts> book) {
        try {
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath path = wand.getPath();
            boolean byWand = false;
            if (path == null) { path = rankFromTemplate(wand); byWand = true; }
            if (path == null) return new ArrayList<>();
            com.elmakers.mine.bukkit.api.wand.WandUpgradePath next = path.getUpgrade();
            boolean byCasts = false;
            if (!byWand && next != null && wand instanceof com.elmakers.mine.bukkit.api.magic.CasterProperties cp) {
                // Same test Magic runs after a spell ranks up (CasterProperties.checkAndUpgrade):
                // nothing left to learn on this rank and every rank spell held. Read only, quiet.
                byCasts = !path.canProgress(cp) && path.checkUpgradeRequirements(cp, true);
            }
            List<String> h = com.winkreyes.excelwandbars.core.ProgressText.header(plugin.rankDisplay(path.getKey(), SpellInfoText.strip(path.getName())),
                    next == null ? null : plugin.rankDisplay(next.getKey(), SpellInfoText.strip(next.getName())), byCasts, byWand,
                    com.winkreyes.excelwandbars.core.ProgressText.candidates(book));
            // Spell info 2026-10-26: say how the next wand rank is reached (config text, reloadable).
            // Rank-up by casting (2026-10-26): SP progress, how, and any XP bonus on now.
            RankUpHook ru = plugin.rankUp();
            if (byWand && ru != null && ru.enabled && ru.isLadderWand(wand)) {
                org.bukkit.entity.Player owner = null;
                try { owner = wand.getMage() == null ? null : wand.getMage().getPlayer(); } catch (Throwable ignored) { }
                List<String> lines = ru.header(owner, wand);
                if (!lines.isEmpty()) { h.addAll(lines); return h; }
            }
            if (byWand && next != null) {
                String nn = plugin.rankDisplay(next.getKey(), SpellInfoText.strip(next.getName()));
                String how = plugin.rankUpHow(next.getKey(), nn);
                if (how != null && !how.isBlank()) h.add("How to reach " + nn + " rank: " + how.trim());
            }
            return h;
        } catch (Throwable t) {
            if (plugin.debug()) plugin.getLogger().info("rank header: " + t);
            return new ArrayList<>();
        }
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
    void openSpellBook(Player p, String only) { openSpellBook(p, only, false); }

    /**
     * 0.2.2: fromWand = opened in place of Magic's chest spell list (Bedrock, Classic).
     * Returns true when the player got a spell book (form, or chat on the rig's simulated Bedrock).
     * The only change it can make is "Use this spell" (sets the held wand's active spell).
     */
    boolean openSpellBook(Player p, String only, boolean fromWand) {
        List<SpellFacts> book = book(p);
        if (book == null) {
            if (fromWand) return false;
            p.sendMessage(Component.text("No Magic wand found. Keep your wand in your inventory (take it out of your Utility Pouch if it's in there), then try /spellbook again.", NamedTextColor.GRAY));
            return true;
        }
        if (book.isEmpty()) {
            if (fromWand) return false;
            p.sendMessage(Component.text("This wand has no spells yet.", NamedTextColor.GRAY));
            return true;
        }
        SpellFacts pick = null;
        if (only != null) {
            String q = only.toLowerCase(Locale.ROOT);
            for (SpellFacts f : book) if (f.key.equalsIgnoreCase(q) || f.name.equalsIgnoreCase(only)) { pick = f; break; }
        }
        // 0.2.2: plugin.isBedrock also counts the admin test list (simbedrock); a simulated
        // player has no Geyser connection, so the form fails and the chat book below shows.
        boolean bedrock = plugin.isBedrock(p);
        Wand bw = bookWand(p, api.getMage(p));
        List<String> header = bw == null ? new ArrayList<>() : rankHeader(bw, book);
        if (bedrock) {
            if (pick != null) { sendDetailForm(p, pick); return true; } // chat fallback inside
            if (sendListForm(p, book, header)) return true;
            // form failed: fall through to chat (a real Bedrock player from the wand gets Magic's chest instead)
            if (fromWand && !plugin.simulatedBedrock(p)) return false;
        }
        if (pick != null) {
            for (String line : SpellInfoText.lines(pick)) p.sendMessage(Component.text(line, NamedTextColor.YELLOW));
            return true;
        }
        p.sendMessage(Component.text(com.winkreyes.excelwandbars.core.ControlsText.bookTitle(bedrock), NamedTextColor.GOLD));
        for (String h : header) p.sendMessage(Component.text(" " + h, NamedTextColor.GREEN));
        boolean bars = plugin.barsStyle(p);
        for (SpellFacts f : book) {
            String label = (bars && f.bar > 0 ? "Bar " + f.bar + ": " : "") + SpellInfoText.button(f);
            Component hover = Component.text(String.join("\n", SpellInfoText.lines(f)));
            p.sendMessage(Component.text(" " + label, NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(hover))
                    .clickEvent(ClickEvent.runCommand("/spellinfo " + f.key)));
        }
        if (plugin.styleHook() != null && plugin.styleHook().enabled()) {
            p.sendMessage(Component.text(com.winkreyes.excelwandbars.core.ControlsText.controlsButton(styleLabel(p), bedrock), NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/wandstyle")));
        }
        SpellTipHook st = plugin.spellTip();
        if (st != null) st.onBookOpened(p, false);
        return true;
    }

    /** 0.2.3: Classic, Sneak bars or Cycle. */
    private String styleLabel(Player p) {
        StyleHook s = plugin.styleHook();
        if (s == null || !s.enabled()) return plugin.barsStyle(p) ? "Sneak bars" : "Classic";
        return s.styleOf(p).label();
    }

    private boolean sendListForm(Player p, List<SpellFacts> book, List<String> header) {
        List<String> buttons = new ArrayList<>();
        boolean bars = plugin.barsStyle(p);
        for (SpellFacts f : book) buttons.add((bars && f.bar > 0 ? "Bar " + f.bar + ": " : "") + SpellInfoText.button(f) + (f.active ? " (in use)" : ""));
        StringBuilder content = new StringBuilder();
        for (String h : header) content.append(h).append("\n");
        if (!header.isEmpty()) content.append("\n");
        SpellTipHook st = plugin.spellTip();
        boolean tipButton = st != null && st.pendingInBook(p);
        if (tipButton) {
            content.append("Tip: ").append(String.join(" ", com.winkreyes.excelwandbars.core.SpellTip.body(true))).append("\n\n");
        }
        content.append(bars ? (pagesWord(p) ? "Tap a spell to read what it does, its mana cost, cooldown and rank. Reading never casts.\nMore than one page? Crouch with the bar up for the next page."
                        : "Tap a spell to read what it does, its cost and its cooldown.\nSneak once with the bar open to flip to the next bar.")
                : "Tap a spell to read it, then Use this spell to put it on your wand.\nTap to cast. Press and hold picks the next spell."
                  + (plugin.styleHook() != null && plugin.styleHook().styleOf(p) == com.winkreyes.excelwandbars.core.WandStyle.CYCLE
                     ? "\nCrouch and press and hold goes back one spell." : ""));
        boolean styleButton = plugin.styleHook() != null && plugin.styleHook().enabled();
        if (styleButton) buttons.add("Wand controls: " + styleLabel(p) + " (change)");
        if (tipButton) buttons.add(com.winkreyes.excelwandbars.core.SpellTip.OK_BUTTON);
        final int styleId = styleButton ? book.size() : -2;
        final int tipId = tipButton ? book.size() + (styleButton ? 1 : 0) : -2;
        boolean sent = sendForm(p, "Spell Book", content.toString(), buttons, id -> {
            if (id == styleId) {
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && plugin.styleHook() != null) plugin.styleHook().openChooser(p); });
                return;
            }
            if (id == tipId) {
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && plugin.spellTip() != null) plugin.spellTip().ack(p, true); });
                return;
            }
            if (id < 0 || id >= book.size()) return;
            SpellFacts f = book.get(id);
            Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) sendDetailForm(p, f); });
        });
        if (sent && st != null) st.onBookOpened(p, true);
        return sent;
    }

    /** True when the player holds the wand the book reads (so "Use this spell" can change it). */
    private boolean holdsBookWand(Player p, SpellFacts f) {
        Mage mage = api.getMage(p);
        Wand held = mage == null ? null : mage.getActiveWand();
        if (held == null) return false;
        Collection<String> sp = held.getSpells();
        if (sp == null) return false;
        for (String k : sp) if (k != null && base(k).equalsIgnoreCase(base(f.key))) return true;
        return false;
    }

    private boolean sendDetailForm(Player p, SpellFacts f) {
        List<String> lines = SpellInfoText.lines(f);
        String content = String.join("\n\n", lines.subList(1, lines.size()));
        List<String> buttons = new ArrayList<>();
        // 0.2.5: no "Use this spell" in Tap bar (a tap on the icon casts; the active spell is not used there).
        boolean tapBar = plugin.styleHook() != null && plugin.styleHook().enabled() && plugin.styleHook().styleOf(p) == com.winkreyes.excelwandbars.core.WandStyle.TAPBAR;
        boolean canUse = holdsBookWand(p, f) && !f.active && !tapBar;
        if (f.active && holdsBookWand(p, f) && !tapBar) content = "You are using this spell now.\n\n" + content;
        if (canUse) buttons.add(USE_BUTTON);
        buttons.add("Back to Spell Book");
        buttons.add("Close");
        final int useId = canUse ? 0 : -2, backId = canUse ? 1 : 0;
        boolean sent = sendForm(p, f.name + " (" + SpellInfoText.rankText(f) + ")", content, buttons, id -> {
            if (id == useId) Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) useSpell(p, f.key); });
            else if (id == backId) Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) openSpellBook(p, null); });
        });
        if (!sent) for (String line : lines) p.sendMessage(Component.text(line, NamedTextColor.YELLOW));
        return sent;
    }

    // ------------------------------------------------------------------ 0.2.4 Tap bar: hold = read the spell

    /** The spell key on a Magic spell icon (null for anything else). */
    String spellKeyOf(ItemStack item) { return keyOf(item); }

    /** The same facts the Spell Book and /spellinfo show (live: cast count, next rank), for one spell on the held wand. */
    SpellFacts factsFor(Player p, String key) {
        if (key == null) return null;
        List<SpellFacts> book = book(p);
        if (book == null) return null;
        for (SpellFacts f : book) if (base(f.key).equalsIgnoreCase(base(key))) return f;
        return null;
    }

    /**
     * Tap bar press and hold: the exact Spell Book detail page for this spell (form on Bedrock,
     * the same lines in chat on Java or when the form cannot be sent). Reads only; never casts.
     * No spell known: the Spell Book list. Returns "detail key", "book" or "none".
     */
    String showSpellInfo(Player p, String key) {
        SpellFacts f = factsFor(p, key);
        if (f == null) { openSpellBook(p, null); return "book"; }
        if (plugin.isBedrock(p)) { sendDetailForm(p, f); return "detail " + base(f.key); }
        for (String line : SpellInfoText.lines(f)) p.sendMessage(Component.text(line, NamedTextColor.YELLOW));
        return "detail " + base(f.key);
    }

    static final String USE_BUTTON = "Use this spell";
    private long uses = 0;
    long uses() { return uses; }

    /**
     * "Use this spell": make it the held wand's active spell (what the next cast uses).
     * Magic's own Wand.setActiveSpell (api) saves it on the wand item and renames the
     * wand. Only the held wand, only a spell that wand already has. Returns a status word.
     */
    String useSpell(Player p, String key) {
        Mage mage = api.getMage(p);
        Wand held = mage == null ? null : mage.getActiveWand();
        if (held == null) { p.sendMessage(Component.text("Hold your wand, then pick the spell again.", NamedTextColor.GRAY)); return "no-wand"; }
        String want = null;
        Collection<String> sp = held.getSpells();
        if (sp != null) for (String k : sp) if (k != null && base(k).equalsIgnoreCase(base(key))) { want = base(k); break; }
        if (want == null) { p.sendMessage(Component.text("Your wand does not know that spell.", NamedTextColor.GRAY)); return "not-on-wand"; }
        held.setActiveSpell(want);
        uses++;
        Spell s = held.getSpell(want, mage);
        String name = s != null ? SpellInfoText.strip(s.getName()) : want;
        // Sent 2 ticks later: Magic shows its own active-spell name on the action bar right after
        // setActiveSpell, which would otherwise replace this line at once.
        final String shown = name;
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) p.sendActionBar(Component.text("Now using: " + shown, NamedTextColor.AQUA)); }, 2L);
        if (plugin.debug()) plugin.getLogger().info(p.getName() + " use spell " + want + " -> active " + held.getActiveSpellKey());
        return "ok " + held.getActiveSpellKey();
    }

    // ------------------------------------------------------------------ forms (with a rig test record)

    /** Last form per player, memory only: admin /wandbars lastform and /wandbars formtap (rig tests). */
    static final class LastForm { String title, content; List<String> buttons; java.util.function.IntConsumer cb; }
    private final Map<java.util.UUID, LastForm> lastForms = new java.util.HashMap<>();
    LastForm lastForm(Player p) { return lastForms.get(p.getUniqueId()); }

    boolean sendForm(Player p, String title, String content, List<String> buttons, java.util.function.IntConsumer cb) {
        LastForm lf = new LastForm();
        lf.title = title; lf.content = content; lf.buttons = new ArrayList<>(buttons); lf.cb = cb;
        lastForms.put(p.getUniqueId(), lf);
        return forms.simple(p, title, content, buttons, cb);
    }

    void forgetForms(Player p) { lastForms.remove(p.getUniqueId()); }

    MagicAPI api() { return api; }
    String baseKey(String k) { return base(k); }

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
        List<SpellFacts> bk = book(p);
        for (String h : rankHeader(wand, bk)) out.add("  rank: " + h);
        for (SpellFacts f : bk) out.add("  [" + f.key + "] " + SpellInfoText.button(f) + " :: " + SpellInfoText.summary(f)
                + (f.toggle ? " | toggle" : "") + (f.stopsManaRegen ? " | no regen" : "")
                + (f.nextRankCasts > 0 ? " | next rank " + f.castCount + "/" + f.nextRankCasts + " casts " + f.nextRankPath + (f.pathOk ? " ok" : f.rankByWand ? " by-wand-rank" : " locked") : "")
                + (f.active ? " | ACTIVE" : "")
                + (f.description.isEmpty() ? " | NO DESCRIPTION" : " | " + f.description));
        return out;
    }
}
