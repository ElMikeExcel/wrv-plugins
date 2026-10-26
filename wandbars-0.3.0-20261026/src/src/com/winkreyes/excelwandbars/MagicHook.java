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
        lastView.remove(event.getPlayer().getUniqueId());
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
            int on = n;
            // 0.2.9: own spell bar: say how many are on it and where the rest are.
            LoadoutHook lo = plugin.styleHook() == null ? null : plugin.styleHook().loadouts();
            if (lo != null) on = lo.layout(player, wand, plugin.styleHook().perPageFor(plugin.styleHook().styleOf(player))).onBar;
            player.sendActionBar(Component.text(com.winkreyes.excelwandbars.core.LoadoutText.onePage(on, n), NamedTextColor.AQUA));
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
        // 0.3.0: organized Spell Book (spell-book.groups: false = the 0.2.9 flat list below).
        if (pick == null && plugin.spellBookGroups()) {
            if (only != null) {
                // A locked spell typed by key: its "unlocks at" page.
                Ctx lc = ctx(p, true);
                if (lc != null) for (com.winkreyes.excelwandbars.core.SpellbookText.Entry e : lc.entries)
                    if (e.locked && (e.key.equalsIgnoreCase(only) || e.name.equalsIgnoreCase(only))) { openLocked(p, e.key); return true; }
            }
            Ctx c = ctx(p, true);
            if (c != null) return openHome(p, c, fromWand);
        }
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
            barChatButtons(p, pick);
            View v = viewFor(p, pick.key);
            if (v != null) backChat(p, v.group, v.page);
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
        if (loadoutUsable(p)) {
            p.sendMessage(Component.text("[" + com.winkreyes.excelwandbars.core.LoadoutText.BOOK_BUTTON + "]", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/spellbar")));
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
        // 0.2.9: own spell bar button first after the spells.
        boolean barButton = loadoutUsable(p);
        if (barButton) buttons.add(com.winkreyes.excelwandbars.core.LoadoutText.BOOK_BUTTON);
        if (styleButton) buttons.add("Wand controls: " + styleLabel(p) + " (change)");
        if (tipButton) buttons.add(com.winkreyes.excelwandbars.core.SpellTip.OK_BUTTON);
        final int barId = barButton ? book.size() : -2;
        final int styleId = styleButton ? book.size() + (barButton ? 1 : 0) : -2;
        final int tipId = tipButton ? book.size() + (barButton ? 1 : 0) + (styleButton ? 1 : 0) : -2;
        boolean sent = sendForm(p, "Spell Book", content.toString(), buttons, id -> {
            if (id == barId) {
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && plugin.loadouts() != null) plugin.loadouts().openBar(p); });
                return;
            }
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
        List<Runnable> acts = new ArrayList<>();
        if (canUse) { buttons.add(USE_BUTTON); acts.add(() -> useSpell(p, f.key)); }
        // 0.2.9: own spell bar buttons (hotbar styles only).
        int onBar = barSlot(p, f.key);
        if (onBar >= -1) {
            final String k = base(f.key);
            if (onBar >= 0) content = "On your bar: slot " + (onBar + 1) + ".\n\n" + content;
            else content = "Not on your bar. It is in your Spellbook.\n\n" + content;
            buttons.add(onBar >= 0 ? com.winkreyes.excelwandbars.core.LoadoutText.MOVE_BUTTON : com.winkreyes.excelwandbars.core.LoadoutText.PUT_BUTTON);
            acts.add(() -> { if (plugin.loadouts() != null) plugin.loadouts().openPickSlot(p, k); });
            if (onBar >= 0) {
                final int slot = onBar;
                buttons.add(com.winkreyes.excelwandbars.core.LoadoutText.OFF_BUTTON);
                acts.add(() -> { if (plugin.loadouts() != null) plugin.loadouts().handle(p, new String[]{"remove", String.valueOf(slot + 1)}); });
            }
        }
        // 0.3.0: Back goes to the group page the spell was opened from.
        View v = viewFor(p, f.key);
        if (v != null) {
            String gname = com.winkreyes.excelwandbars.core.SpellbookText.ALL.equals(v.group) ? com.winkreyes.excelwandbars.core.SpellbookText.allName() : plugin.spellbookGroups().nameOf(v.group);
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.backTo(gname)); acts.add(() -> openGroup(p, v.group, v.page));
        } else { buttons.add("Back to Spell Book"); acts.add(() -> openSpellBook(p, null)); }
        buttons.add("Close"); acts.add(null);
        boolean sent = sendForm(p, f.name + " (" + SpellInfoText.rankText(f) + ")", content, buttons, id -> {
            if (id < 0 || id >= acts.size() || acts.get(id) == null) return;
            Runnable r = acts.get(id);
            Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) r.run(); });
        });
        if (!sent) { for (String line : lines) p.sendMessage(Component.text(line, NamedTextColor.YELLOW)); barChatButtons(p, f); }
        return sent;
    }

    // ------------------------------------------------------------------ 0.3.0 organized Spell Book

    /** Last group page a player looked at (memory only), so a spell's Back goes to it. */
    static final class View { final String group; final int page; View(String g, int p) { group = g; page = p; } }
    private final Map<java.util.UUID, View> lastView = new java.util.HashMap<>();
    void forgetView(Player p) { lastView.remove(p.getUniqueId()); }
    /** The remembered group page, only when this spell is on it (A to Z holds every spell). */
    private View viewFor(Player p, String key) {
        if (!plugin.spellBookGroups()) return null;
        View v = lastView.get(p.getUniqueId());
        if (v == null) return null;
        if (com.winkreyes.excelwandbars.core.SpellbookText.ALL.equals(v.group)) return v;
        return plugin.spellbookGroups().groupOf(key).equals(v.group) ? v : null;
    }

    private List<String> ladderOrder() {
        RankUpHook ru = plugin.rankUp();
        if (ru != null) return ru.ladderOrder();
        return new ArrayList<>(com.winkreyes.excelwandbars.core.RankLadder.wrvDefault().order());
    }

    /** Spell keys on one wand template (and the templates it inherits), as Magic has them loaded now. */
    List<String> templateSpells(String templateKey) {
        List<String> out = new ArrayList<>();
        try {
            com.elmakers.mine.bukkit.api.wand.WandTemplate t = api.getController().getWandTemplate(templateKey);
            for (int d = 0; t != null && d < 16; d++) {
                ConfigurationSection c = t.getConfiguration();
                if (c != null) for (String s : c.getStringList("spells")) {
                    String k = com.winkreyes.excelwandbars.core.BarLoadout.norm(s);
                    if (k != null && !k.isEmpty() && !out.contains(k)) out.add(k);
                }
                t = t.getParent();
            }
        } catch (Throwable ignored) { }
        return out;
    }

    /** Every spell on the rank wands now (the seals broken so far), lowest rank first. */
    List<String> ladderSpells() {
        List<String> out = new ArrayList<>();
        for (String r : ladderOrder()) for (String k : templateSpells(r)) if (!out.contains(k)) out.add(k);
        return out;
    }

    private String bandName(List<String> order, int i) {
        if (i < 0 || i >= order.size()) return "";
        String key = order.get(i);
        RankUpHook ru = plugin.rankUp();
        if (ru != null) return ru.rankName(key);
        return plugin.rankDisplay(key, Character.toUpperCase(key.charAt(0)) + key.substring(1));
    }

    private String spellTemplateName(String key) {
        try {
            SpellTemplate t = api.getController().getSpellTemplate(key);
            if (t != null && t.getName() != null) { String n = SpellInfoText.strip(t.getName()).trim(); if (!n.isEmpty()) return n; }
        } catch (Throwable ignored) { }
        return key.isEmpty() ? key : Character.toUpperCase(key.charAt(0)) + key.substring(1).replace('_', ' ');
    }

    private String spellTemplateDescription(String key) {
        try {
            SpellTemplate t = api.getController().getSpellTemplate(key);
            if (t != null && t.getDescription() != null) return SpellInfoText.strip(t.getDescription()).trim();
        } catch (Throwable ignored) { }
        return "";
    }

    /** The book as entries: the wand's spells, then (ladder wands, show-locked on) the higher ranks' spells it lacks. */
    private List<com.winkreyes.excelwandbars.core.SpellbookText.Entry> entries(Player p, Wand bw, List<SpellFacts> book, Map<String, SpellFacts> facts) {
        List<com.winkreyes.excelwandbars.core.SpellbookText.Entry> out = new ArrayList<>();
        List<String> order = ladderOrder();
        Map<String, Integer> band = new LinkedHashMap<>();
        Map<Integer, List<String>> byRank = new LinkedHashMap<>();
        for (int i = 0; i < order.size(); i++) {
            List<String> ks = templateSpells(order.get(i));
            byRank.put(i, ks);
            for (String k : ks) band.putIfAbsent(k, i);
        }
        LoadoutHook lo = plugin.loadouts();
        List<String> bar = lo == null ? null : lo.barKeys(p);
        boolean bars = plugin.barsStyle(p);
        java.util.Set<String> known = new java.util.HashSet<>();
        for (SpellFacts f : book) {
            String k = com.winkreyes.excelwandbars.core.BarLoadout.norm(f.key);
            known.add(k);
            facts.put(k, f);
            int b = band.getOrDefault(k, com.winkreyes.excelwandbars.core.SpellbookText.NO_BAND);
            com.winkreyes.excelwandbars.core.SpellbookText.Entry e = new com.winkreyes.excelwandbars.core.SpellbookText.Entry(k, f.name, b, bandName(order, b), false)
                    .rank(SpellInfoText.rankText(f));
            if (bar != null) e.barSlot = bar.indexOf(k);
            else if (bars && f.bar > 0) e.page = f.bar;
            e.active = f.active;
            out.add(e);
        }
        if (plugin.showLocked() && bw != null) {
            int cur = order.indexOf(RankUpHook.rankOf(bw));
            if (cur >= 0) for (int i = cur + 1; i < order.size(); i++) for (String k : byRank.get(i)) {
                if (known.contains(k)) continue;
                known.add(k);
                out.add(new com.winkreyes.excelwandbars.core.SpellbookText.Entry(k, spellTemplateName(k), band.getOrDefault(k, i), bandName(order, band.getOrDefault(k, i)), true));
            }
        }
        return out;
    }

    /** 0.3.0 bar picker: plain entries (known spells) with rank bands, for grouping and sorting. */
    List<com.winkreyes.excelwandbars.core.SpellbookText.Entry> plainEntries(Collection<String> keys, java.util.function.Function<String, String> name) {
        List<String> order = ladderOrder();
        Map<String, Integer> band = new LinkedHashMap<>();
        for (int i = 0; i < order.size(); i++) for (String k : templateSpells(order.get(i))) band.putIfAbsent(k, i);
        List<com.winkreyes.excelwandbars.core.SpellbookText.Entry> out = new ArrayList<>();
        for (String k : keys) {
            int b = band.getOrDefault(k, com.winkreyes.excelwandbars.core.SpellbookText.NO_BAND);
            out.add(new com.winkreyes.excelwandbars.core.SpellbookText.Entry(k, name.apply(k), b, bandName(order, b), false));
        }
        return out;
    }

    /** Book + wand for the grouped views; null after telling the player why there is none. */
    private final class Ctx {
        Wand wand; List<SpellFacts> book; Map<String, SpellFacts> facts = new java.util.HashMap<>();
        List<com.winkreyes.excelwandbars.core.SpellbookText.Entry> entries;
    }
    private Ctx ctx(Player p, boolean quiet) {
        List<SpellFacts> book = book(p);
        if (book == null) {
            if (!quiet) p.sendMessage(Component.text("No Magic wand found. Keep your wand in your inventory (take it out of your Utility Pouch if it's in there), then try /spellbook again.", NamedTextColor.GRAY));
            return null;
        }
        if (book.isEmpty()) { if (!quiet) p.sendMessage(Component.text("This wand has no spells yet.", NamedTextColor.GRAY)); return null; }
        Ctx c = new Ctx();
        c.book = book;
        c.wand = bookWand(p, api.getMage(p));
        c.entries = entries(p, c.wand, book, c.facts);
        return c;
    }

    private void later(Player p, Runnable r) { Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) r.run(); }); }

    /** Home: wand line, bar line, a button per group, All A to Z, Put spells on my bar. */
    private boolean openHome(Player p, Ctx c, boolean fromWand) {
        com.winkreyes.excelwandbars.core.SpellbookGroups g = plugin.spellbookGroups();
        List<com.winkreyes.excelwandbars.core.SpellbookText.GroupCount> gc = com.winkreyes.excelwandbars.core.SpellbookText.counts(g, c.entries, plugin.showLocked());
        int known = com.winkreyes.excelwandbars.core.SpellbookText.known(c.entries);
        int locked = plugin.showLocked() ? com.winkreyes.excelwandbars.core.SpellbookText.locked(c.entries) : 0;
        int onBar = com.winkreyes.excelwandbars.core.SpellbookText.onBar(c.entries);
        boolean barButton = loadoutUsable(p);
        String wandName = c.wand == null ? "" : SpellInfoText.strip(c.wand.getName());
        String wandLine = com.winkreyes.excelwandbars.core.SpellbookText.wandLine(wandName, known);
        String barLine = barButton ? com.winkreyes.excelwandbars.core.SpellbookText.barLine(onBar, known) : null;
        String lockedLine = com.winkreyes.excelwandbars.core.SpellbookText.lockedLine(locked);
        List<String> header = c.wand == null ? new ArrayList<>() : rankHeader(c.wand, c.book);
        boolean bedrock = plugin.isBedrock(p);
        boolean styleButton = plugin.styleHook() != null && plugin.styleHook().enabled();
        SpellTipHook st = plugin.spellTip();
        lastView.remove(p.getUniqueId());
        if (bedrock) {
            StringBuilder content = new StringBuilder(wandLine).append("\n");
            if (barLine != null) content.append(barLine).append("\n");
            if (lockedLine != null) content.append(lockedLine).append("\n");
            for (String h : header) content.append(h).append("\n");
            boolean tipButton = st != null && st.pendingInBook(p);
            if (tipButton) content.append("\nTip: ").append(String.join(" ", com.winkreyes.excelwandbars.core.SpellTip.body(true))).append("\n");
            content.append("\n").append(com.winkreyes.excelwandbars.core.SpellbookText.HOME_HINT_BEDROCK);
            List<String> buttons = new ArrayList<>();
            List<Runnable> acts = new ArrayList<>();
            for (com.winkreyes.excelwandbars.core.SpellbookText.GroupCount x : gc) {
                buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.groupButton(x));
                final String gk = x.key;
                acts.add(() -> openGroup(p, gk, 0));
            }
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.ALL_BUTTON);
            acts.add(() -> openGroup(p, com.winkreyes.excelwandbars.core.SpellbookText.ALL, 0));
            if (barButton) { buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.BAR_BUTTON); acts.add(() -> { if (plugin.loadouts() != null) plugin.loadouts().openBar(p); }); }
            if (styleButton) { buttons.add("Wand controls: " + styleLabel(p) + " (change)"); acts.add(() -> { if (plugin.styleHook() != null) plugin.styleHook().openChooser(p); }); }
            if (tipButton) { buttons.add(com.winkreyes.excelwandbars.core.SpellTip.OK_BUTTON); acts.add(() -> { if (plugin.spellTip() != null) plugin.spellTip().ack(p, true); }); }
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.CLOSE); acts.add(null);
            boolean sent = sendForm(p, com.winkreyes.excelwandbars.core.SpellbookText.HOME_TITLE, content.toString(), buttons, id -> {
                if (id < 0 || id >= acts.size() || acts.get(id) == null) return;
                later(p, acts.get(id));
            });
            if (sent) { if (st != null) st.onBookOpened(p, true); return true; }
            if (fromWand && !plugin.simulatedBedrock(p)) return false;
        }
        p.sendMessage(Component.text(com.winkreyes.excelwandbars.core.SpellbookText.HOME_TITLE + ": " + wandLine, NamedTextColor.GOLD));
        if (barLine != null) p.sendMessage(Component.text(" " + barLine, NamedTextColor.GRAY));
        if (lockedLine != null) p.sendMessage(Component.text(" " + lockedLine, NamedTextColor.GRAY));
        for (String h : header) p.sendMessage(Component.text(" " + h, NamedTextColor.GREEN));
        p.sendMessage(Component.text(bedrock ? com.winkreyes.excelwandbars.core.SpellbookText.HOME_HINT_BEDROCK : com.winkreyes.excelwandbars.core.SpellbookText.HOME_HINT_JAVA, NamedTextColor.GRAY));
        Component line = Component.empty();
        int n = 0;
        for (com.winkreyes.excelwandbars.core.SpellbookText.GroupCount x : gc) {
            if (n > 0 && n % 3 == 0) { p.sendMessage(line); line = Component.empty(); }
            if (n % 3 != 0) line = line.append(Component.text(" "));
            line = line.append(Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.groupJava(x) + "]", NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(Component.text(com.winkreyes.excelwandbars.core.SpellbookText.groupHover(x))))
                    .clickEvent(ClickEvent.runCommand("/spellbook group " + x.key)));
            n++;
        }
        if (n > 0) p.sendMessage(line);
        Component tail = Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.ALL_JAVA + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/spellbook all"));
        if (barButton) tail = tail.append(Component.text(" ")).append(Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.BAR_BUTTON + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/spellbar")));
        p.sendMessage(tail);
        if (styleButton) {
            p.sendMessage(Component.text(com.winkreyes.excelwandbars.core.ControlsText.controlsButton(styleLabel(p), bedrock), NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/wandstyle")));
        }
        if (st != null) st.onBookOpened(p, false);
        return true;
    }

    /** One group (or ALL = A to Z), one page. Java: chat 10 per page. Bedrock: form 12 per page. */
    boolean openGroup(Player p, String typed, int page) {
        com.winkreyes.excelwandbars.core.SpellbookGroups g = plugin.spellbookGroups();
        boolean all = com.winkreyes.excelwandbars.core.SpellbookText.ALL.equalsIgnoreCase(typed);
        String gk = all ? com.winkreyes.excelwandbars.core.SpellbookText.ALL : g.find(typed);
        if (gk == null) {
            p.sendMessage(Component.text("No group called " + typed + ". Open /spellbook to see the groups.", NamedTextColor.GRAY));
            return true;
        }
        Ctx c = ctx(p, false);
        if (c == null) return true;
        List<com.winkreyes.excelwandbars.core.SpellbookText.Entry> list = com.winkreyes.excelwandbars.core.SpellbookText.list(g, c.entries, gk, plugin.showLocked());
        String name = all ? com.winkreyes.excelwandbars.core.SpellbookText.allName() : g.nameOf(gk);
        boolean bedrock = plugin.isBedrock(p);
        boolean anyLocked = false;
        for (com.winkreyes.excelwandbars.core.SpellbookText.Entry e : list) if (e.locked) { anyLocked = true; break; }
        if (bedrock) {
            int per = com.winkreyes.excelwandbars.core.SpellbookText.BEDROCK_PER_PAGE;
            int pg = com.winkreyes.excelwandbars.core.SpellbookText.clampPage(page, list.size(), per);
            int pages = com.winkreyes.excelwandbars.core.SpellbookText.pages(list.size(), per);
            lastView.put(p.getUniqueId(), new View(gk, pg));
            List<String> buttons = new ArrayList<>();
            List<Runnable> acts = new ArrayList<>();
            for (com.winkreyes.excelwandbars.core.SpellbookText.Entry e : com.winkreyes.excelwandbars.core.SpellbookText.page(list, pg, per)) {
                buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.bedrockButton(e));
                if (e.locked) acts.add(() -> sendLocked(p, e, gk, pg));
                else { SpellFacts f = c.facts.get(e.key); acts.add(() -> { if (f != null) sendDetailForm(p, f); }); }
            }
            if (pg + 1 < pages) { buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.NEXT); acts.add(() -> openGroup(p, gk, pg + 1)); }
            if (pg > 0) { buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.PREV); acts.add(() -> openGroup(p, gk, pg - 1)); }
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.GROUPS); acts.add(() -> openSpellBook(p, null));
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.CLOSE); acts.add(null);
            boolean sent = sendForm(p, com.winkreyes.excelwandbars.core.SpellbookText.groupTitle(name, list.size(), pg, pages),
                    com.winkreyes.excelwandbars.core.SpellbookText.groupContent(true, anyLocked), buttons, id -> {
                        if (id < 0 || id >= acts.size() || acts.get(id) == null) return;
                        later(p, acts.get(id));
                    });
            if (sent) return true;
        }
        int per = com.winkreyes.excelwandbars.core.SpellbookText.JAVA_PER_PAGE;
        int pg = com.winkreyes.excelwandbars.core.SpellbookText.clampPage(page, list.size(), per);
        int pages = com.winkreyes.excelwandbars.core.SpellbookText.pages(list.size(), per);
        lastView.put(p.getUniqueId(), new View(gk, pg));
        p.sendMessage(Component.text(com.winkreyes.excelwandbars.core.SpellbookText.groupHeading(name, list.size(), pg, pages), NamedTextColor.GOLD));
        for (String l : com.winkreyes.excelwandbars.core.SpellbookText.groupContent(bedrock, anyLocked).split("\n")) p.sendMessage(Component.text(l, NamedTextColor.GRAY));
        for (com.winkreyes.excelwandbars.core.SpellbookText.Entry e : com.winkreyes.excelwandbars.core.SpellbookText.page(list, pg, per)) {
            if (e.locked) {
                p.sendMessage(Component.text(" " + com.winkreyes.excelwandbars.core.SpellbookText.javaLine(e), NamedTextColor.DARK_GRAY)
                        .hoverEvent(HoverEvent.showText(Component.text(String.join("\n", com.winkreyes.excelwandbars.core.SpellbookText.lockedLines(e, spellTemplateDescription(e.key))))))
                        .clickEvent(ClickEvent.runCommand("/spellbook locked " + e.key)));
            } else {
                SpellFacts f = c.facts.get(e.key);
                Component t = Component.text(" " + com.winkreyes.excelwandbars.core.SpellbookText.javaLine(e), NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.runCommand("/spellinfo " + e.key));
                if (f != null) t = t.hoverEvent(HoverEvent.showText(Component.text(String.join("\n", SpellInfoText.lines(f)))));
                p.sendMessage(t);
            }
        }
        String cmd = all ? "/spellbook all " : "/spellbook group " + gk + " ";
        Component nav = Component.empty();
        if (pg > 0) nav = nav.append(Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.JAVA_PREV + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand(cmd + pg))).append(Component.text(" "));
        nav = nav.append(Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.JAVA_GROUPS + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/spellbook")));
        if (pg + 1 < pages) nav = nav.append(Component.text(" ")).append(Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.JAVA_NEXT + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand(cmd + (pg + 2))));
        p.sendMessage(nav);
        return true;
    }

    /** /spellbook locked <spell>: what a higher rank brings (read only). */
    boolean openLocked(Player p, String key) {
        Ctx c = ctx(p, false);
        if (c == null) return true;
        String k = com.winkreyes.excelwandbars.core.BarLoadout.norm(key);
        for (com.winkreyes.excelwandbars.core.SpellbookText.Entry e : c.entries) {
            if (!e.key.equals(k)) continue;
            if (!e.locked) { openSpellBook(p, e.key); return true; }
            View v = lastView.get(p.getUniqueId());
            String gk = v != null ? v.group : plugin.spellbookGroups().groupOf(e.key);
            sendLocked(p, e, gk, v != null ? v.page : 0);
            return true;
        }
        p.sendMessage(Component.text("That spell is not in your Spell Book.", NamedTextColor.GRAY));
        return true;
    }

    private void sendLocked(Player p, com.winkreyes.excelwandbars.core.SpellbookText.Entry e, String gk, int pg) {
        List<String> lines = com.winkreyes.excelwandbars.core.SpellbookText.lockedLines(e, spellTemplateDescription(e.key));
        String gname = com.winkreyes.excelwandbars.core.SpellbookText.ALL.equals(gk) ? com.winkreyes.excelwandbars.core.SpellbookText.allName() : plugin.spellbookGroups().nameOf(gk);
        if (plugin.isBedrock(p)) {
            List<String> buttons = new ArrayList<>();
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.backTo(gname));
            buttons.add(com.winkreyes.excelwandbars.core.SpellbookText.CLOSE);
            boolean sent = sendForm(p, com.winkreyes.excelwandbars.core.SpellbookText.lockedTitle(e), String.join("\n\n", lines.subList(1, lines.size())), buttons, id -> {
                if (id == 0) later(p, () -> openGroup(p, gk, pg));
            });
            if (sent) return;
        }
        p.sendMessage(Component.text(lines.get(0) + " (locked)", NamedTextColor.GRAY));
        for (String l : lines.subList(1, lines.size())) p.sendMessage(Component.text(" " + l, NamedTextColor.GRAY));
        backChat(p, gk, pg);
    }

    /** Java: [Back to Healing] under a spell opened from a group page. */
    private void backChat(Player p, String gk, int pg) {
        if (gk == null) return;
        boolean all = com.winkreyes.excelwandbars.core.SpellbookText.ALL.equals(gk);
        String gname = all ? com.winkreyes.excelwandbars.core.SpellbookText.allName() : plugin.spellbookGroups().nameOf(gk);
        String cmd = all ? "/spellbook all " + (pg + 1) : "/spellbook group " + gk + " " + (pg + 1);
        p.sendMessage(Component.text("[" + com.winkreyes.excelwandbars.core.SpellbookText.backTo(gname) + "]", NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand(cmd)));
    }

    /** Console (admin /wandbars groups <player>): this player's group counts. */
    List<String> describeGroups(Player p) {
        List<String> out = new ArrayList<>();
        Ctx c = ctx(p, true);
        if (c == null) { out.add(p.getName() + ": no wand with spells"); return out; }
        com.winkreyes.excelwandbars.core.SpellbookGroups g = plugin.spellbookGroups();
        out.add(p.getName() + ": wand " + (c.wand == null ? "?" : SpellInfoText.strip(c.wand.getName())) + " rank " + (c.wand == null ? "?" : RankUpHook.rankOf(c.wand))
                + ", known " + com.winkreyes.excelwandbars.core.SpellbookText.known(c.entries) + ", locked " + com.winkreyes.excelwandbars.core.SpellbookText.locked(c.entries)
                + ", on bar " + com.winkreyes.excelwandbars.core.SpellbookText.onBar(c.entries));
        for (com.winkreyes.excelwandbars.core.SpellbookText.GroupCount x : com.winkreyes.excelwandbars.core.SpellbookText.counts(g, c.entries, plugin.showLocked())) {
            StringBuilder sb = new StringBuilder("  " + x.name + " " + x.known + (x.locked > 0 ? " +" + x.locked + " locked" : "") + ":");
            for (com.winkreyes.excelwandbars.core.SpellbookText.Entry e : com.winkreyes.excelwandbars.core.SpellbookText.list(g, c.entries, x.key, plugin.showLocked()))
                sb.append(" ").append(e.key).append(e.locked ? "(L:" + e.bandName + ")" : "").append(e.barSlot >= 0 ? "#" + (e.barSlot + 1) : "");
            out.add(sb.toString());
        }
        return out;
    }

    // ------------------------------------------------------------------ 0.2.9 own spell bar

    private boolean loadoutUsable(Player p) {
        LoadoutHook lo = plugin.loadouts();
        return lo != null && lo.usable(p);
    }

    /** Slot of this spell on the player's own bar, -1 = in the Spellbook, -2 = no own bar here. */
    private int barSlot(Player p, String key) {
        LoadoutHook lo = plugin.loadouts();
        return lo == null ? -2 : lo.slotOf(p, base(key));
    }

    /** Java chat: clickable bar buttons under a spell's details. */
    private void barChatButtons(Player p, SpellFacts f) {
        int onBar = barSlot(p, f.key);
        if (onBar < -1) return;
        String k = base(f.key);
        p.sendMessage(Component.text(onBar >= 0 ? "On your bar: slot " + (onBar + 1) + "." : "Not on your bar. It is in your Spellbook.", NamedTextColor.GRAY));
        Component line = Component.text("[" + (onBar >= 0 ? com.winkreyes.excelwandbars.core.LoadoutText.MOVE_BUTTON : com.winkreyes.excelwandbars.core.LoadoutText.PUT_BUTTON) + "]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/spellbar put " + k));
        if (onBar >= 0) line = line.append(Component.text(" ")).append(Component.text("[" + com.winkreyes.excelwandbars.core.LoadoutText.OFF_BUTTON + "]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/spellbar remove " + (onBar + 1))));
        p.sendMessage(line);
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
