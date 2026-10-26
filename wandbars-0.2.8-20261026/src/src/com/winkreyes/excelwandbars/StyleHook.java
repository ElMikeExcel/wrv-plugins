package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.event.LoadEvent;
import com.elmakers.mine.bukkit.api.event.WandActivatedEvent;
import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.api.wand.WandTemplate;
import com.winkreyes.excelwandbars.core.ApplyGuard;
import com.winkreyes.excelwandbars.core.StylePrefs;
import com.winkreyes.excelwandbars.core.StyleProfile;
import com.winkreyes.excelwandbars.core.WandStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.2.0 wand style opt-in. Every player gets CLASSIC wand controls unless
 * they pick Spell bars with /wandstyle. The choice is saved per player in
 * plugins/ExcelWandBars/styles.yml and put back on the wand they hold:
 * whenever Magic activates a wand (join, hold, a new or re-issued wand,
 * respawn) and after every Magic load (ExcelMagicGate seal stage applies
 * dispatch "magic reload").
 *
 * Applying uses the same sequence as Magic's own "/wandp <player> configure"
 * (close the bar, save, configure the item, deactivate, re-check), which was
 * tested working on the box for the per-wand chest override (STEPS step 8).
 * Only rank wands (template inherits base_wand, not an excluded template)
 * that the player may use are touched. Only keys whose EFFECTIVE value
 * differs are written, so on a chest-mode base_wand a classic player's wand
 * is left exactly as Magic made it.
 */
final class StyleHook implements Listener {
    enum Result { APPLIED, ALREADY, NO_WAND, NOT_RANK_WAND, NOT_OWNER, THROTTLED, DISABLED, FAILED }

    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final StylePrefs prefs = new StylePrefs();
    private final StyleProfile profile = new StyleProfile();
    private final ApplyGuard guard = new ApplyGuard(4, 10_000);
    private final BedrockForms forms;
    private final File file;
    private boolean enabled = true;
    private boolean barsBedrockOnly = true;
    /** 0.2.5 parity: Sneak bars for Java too (new key, so an older config.yml with bars-bedrock-only: true still opens it). */
    private boolean sneakBarsJava = true;
    private String inheritKey = "base_wand";
    private final Set<String> excluded = new HashSet<>();
    private long applied = 0;

    StyleHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.forms = new BedrockForms(plugin.getLogger());
        this.file = new File(plugin.getDataFolder(), "styles.yml");
    }

    // ------------------------------------------------------------------ settings + storage

    void loadSettings(ConfigurationSection c) {
        enabled = c == null || c.getBoolean("enabled", true);
        barsBedrockOnly = c == null || c.getBoolean("bars-bedrock-only", true);
        sneakBarsJava = c == null || c.getBoolean("sneak-bars-java", true);
        WandStyle d = c == null ? null : WandStyle.parse(c.getString("default", "classic"));
        prefs.setDefault(d == null ? WandStyle.CLASSIC : d);
        inheritKey = c == null ? "base_wand" : c.getString("rank-wand-inherits", "base_wand");
        // 0.2.5 pages (new keys; an older config.yml without them gets these defaults).
        pagesEnabled = c == null || c.getBoolean("pages.enabled", true);
        perPageTap = com.winkreyes.excelwandbars.core.PageLayout.clampPerPage(c == null ? 7 : c.getInt("pages.tap-bar-per-page", 7));
        perPageBars = com.winkreyes.excelwandbars.core.PageLayout.clampPerPage(c == null ? 8 : c.getInt("pages.sneak-bars-per-page", 8));
        excluded.clear();
        // Missing section or key (e.g. a 0.1.0 config.yml kept on upgrade) = the builder wand stays excluded.
        List<String> ex = c != null && c.isSet("exclude-templates") ? c.getStringList("exclude-templates") : List.of("masons");
        for (String s : ex) excluded.add(s.toLowerCase(Locale.ROOT));
        if (c != null) {
            for (WandStyle st : WandStyle.values()) {
                ConfigurationSection s = c.getConfigurationSection("controls." + st.key());
                if (s == null) continue;
                for (String k : s.getKeys(false)) {
                    if (!profile.set(st, k, s.getString(k))) plugin.getLogger().warning("wand-style.controls." + st.key() + "." + k + " is not a control key, ignored");
                }
            }
        }
    }

    void loadPrefs() {
        Map<String, String> raw = new LinkedHashMap<>();
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection s = y.getConfigurationSection("players");
            if (s != null) for (String k : s.getKeys(false)) raw.put(k, s.getString(k));
        }
        int bad = prefs.load(raw);
        if (bad > 0) plugin.getLogger().warning("styles.yml: " + bad + " unreadable line(s) skipped");
    }

    private void savePrefs() {
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(List.of(
                "ExcelWandBars wand style choices (written by the plugin; /wandstyle).",
                "Only players who used /wandstyle are listed; everyone else gets the default.",
                "Default: " + prefs.defaultStyle().key()));
        for (Map.Entry<String, String> e : prefs.save().entrySet()) y.set("players." + e.getKey(), e.getValue());
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "styles.yml.tmp");
            y.save(tmp);
            if (!tmp.renameTo(file)) { java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save styles.yml: " + ex.getMessage());
        }
    }

    void persist() { savePrefs(); }

    /** Short console view of the held wand's controls (rig test + admins). */
    String describe(Player p) {
        Mage mage = api.getMage(p);
        Wand w = mage == null ? null : mage.getActiveWand();
        if (w == null) return "none";
        Map<String, String> e = effective(w);
        return w.getTemplateKey() + " rank=" + isRankWand(w) + " mode=" + e.get("mode") + " right_click=" + e.get("right_click")
                + " right_click_sneak=" + e.get("right_click_sneak") + " drop=" + e.get("drop") + " quick_cast=" + e.get("quick_cast")
                + " matches=" + profile.changes(e, styleOf(p)).isEmpty()
                + " saved=" + savedStyle(p).key() + " bedrock=" + plugin.isBedrock(p)
                + " open=" + w.isInventoryOpen() + " active=" + w.getActiveSpellKey()
                + " left_click=" + e.get("left_click") + " | " + describeLayout(p);
    }

    boolean enabled() { return enabled; }
    StylePrefs prefs() { return prefs; }
    long applied() { return applied; }
    boolean barsBedrockOnly() { return barsBedrockOnly && !sneakBarsJava; }
    /** What the player picked (or the default), before the Bedrock-only rule. */
    WandStyle savedStyle(Player p) { return prefs.get(p.getUniqueId()); }
    /** May this player use Sneak bars? (0.2.1: Bedrock only.) */
    boolean barsAllowed(Player p) { return !barsBedrockOnly() || plugin.isBedrock(p); }
    /**
     * The style that is put on the wand. 0.2.1: Sneak bars is Bedrock only,
     * so a Java player always gets Classic, even with "bars" saved (their
     * saved choice is kept and comes back if they join from Bedrock).
     */
    WandStyle styleOf(Player p) {
        WandStyle s = prefs.get(p.getUniqueId());
        return s == WandStyle.BARS && !barsAllowed(p) ? WandStyle.CLASSIC : s;
    }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWandActivated(WandActivatedEvent event) {
        Mage mage = event.getMage();
        if (mage == null || !mage.isPlayer()) return;
        Player p = mage.getPlayer();
        if (p == null) return;
        // Next tick: never re-configure a wand inside Magic's own activation call.
        Bukkit.getScheduler().runTask(plugin, () -> applySafe(p, "activate"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMagicLoad(LoadEvent event) {
        // ExcelMagicGate's seal apply and "magic load" both end here; Magic
        // re-reads templates, so check every online player's held wand after it settles.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) applySafe(p, "magic load");
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        // Magic skips its activation event while the mage is still loading, so check after join too.
        Bukkit.getScheduler().runTaskLater(plugin, () -> applySafe(p, "join"), 30L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> applySafe(p, "join"), 100L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> applySafe(p, "respawn"), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { guard.forget(event.getPlayer().getUniqueId()); }

    // ------------------------------------------------------------------ apply

    private void applySafe(Player p, String why) {
        if (p == null || !p.isOnline()) return;
        try {
            Result r = apply(p);
            if (plugin.debug() || r == Result.APPLIED || r == Result.THROTTLED || r == Result.FAILED) {
                plugin.getLogger().info("Wand style " + styleOf(p).key() + " for " + p.getName() + " (" + why + "): " + r);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Wand style error for " + p.getName() + " (" + why + "): " + t);
        }
    }

    Map<String, String> effective(Wand w) {
        Map<String, String> eff = new LinkedHashMap<>();
        for (String k : StyleProfile.KEYS) {
            Object v = w.getProperty(k);
            eff.put(k, v == null ? null : String.valueOf(v));
        }
        return eff;
    }

    boolean isRankWand(Wand w) {
        String key = w.getTemplateKey();
        if (key == null) return false;
        if (excluded.contains(key.toLowerCase(Locale.ROOT))) return false;
        if (inheritKey == null || inheritKey.isEmpty()) return true;
        WandTemplate t = w.getTemplate();
        for (int depth = 0; t != null && depth < 32; depth++, t = t.getParent()) {
            if (inheritKey.equalsIgnoreCase(t.getKey())) return true;
        }
        return false;
    }

    /** Put the player's chosen style on the wand they hold. Main thread only. */
    Result apply(Player p) {
        if (!enabled) return Result.DISABLED;
        Mage mage = api.getMage(p);
        Wand w = mage == null ? null : mage.getActiveWand();
        if (w == null) return Result.NO_WAND;
        if (!isRankWand(w)) return Result.NOT_RANK_WAND;
        if (!w.canUse(p)) return Result.NOT_OWNER;
        WandStyle style = styleOf(p);
        Map<String, String> need = profile.changes(effective(w), style);
        Layout lay = layoutFor(w, style);
        if (need.isEmpty() && (lay == null || lay.ok)) return Result.ALREADY;
        if (!guard.allow(p.getUniqueId(), System.currentTimeMillis())) return Result.THROTTLED;
        // Same order as Magic's WandCommandExecutor.onWandConfigure.
        w.closeInventory();
        w.saveState();
        ConfigurationSection node = new MemoryConfiguration();
        for (Map.Entry<String, String> e : need.entrySet()) node.set(e.getKey(), e.getValue());
        if (lay != null && !lay.ok) {
            // 0.2.5: every unlocked spell on the hotbar pages (packed, same order), enough pages for them.
            node.set("hotbar_count", lay.pages);
            node.createSection("spell_inventory", new LinkedHashMap<String, Object>(lay.packed));
            layouts++;
        }
        w.configure(node);
        w.deactivate();
        mage.checkWand();
        applied++;
        Wand now = mage.getActiveWand();
        if (now != null && !profile.changes(effective(now), style).isEmpty()) return Result.FAILED;
        Layout after = now == null ? null : layoutFor(now, style);
        if (after != null && !after.ok) return Result.FAILED;
        return Result.APPLIED;
    }

    // ------------------------------------------------------------------ 0.2.5 pages

    private int perPageTap = 7, perPageBars = 8;
    String pagesSummary() {
        return pagesEnabled ? "all spells on the bar in pages ON (" + perPageTap + " per page Tap bar / original, " + perPageBars + " Sneak bars)" : "pages OFF";
    }
    private boolean pagesEnabled = true;
    private long layouts = 0;
    long layouts() { return layouts; }

    /** What the wand's hotbar pages should be for this style (null = chest style or paging off). */
    static final class Layout {
        Map<String, Integer> packed; int pages; int curPages; boolean ok; int spells;
    }

    Layout layoutFor(Wand w, WandStyle style) {
        if (!pagesEnabled || style == null || !style.inventory()) return null;
        if (!(w instanceof com.elmakers.mine.bukkit.wand.Wand impl)) return null;
        Map<String, Integer> cur = new java.util.HashMap<>();
        for (Map.Entry<String, Integer> e : impl.getSpellInventory().entrySet()) cur.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        int per = style == WandStyle.BARS ? perPageBars : perPageTap;
        Layout l = new Layout();
        l.packed = com.winkreyes.excelwandbars.core.PageLayout.pack(cur, impl.getBaseSpells(), per);
        l.spells = l.packed.size();
        l.pages = com.winkreyes.excelwandbars.core.PageLayout.pages(l.spells, per);
        l.curPages = impl.getHotbarCount();
        l.ok = l.curPages == l.pages && com.winkreyes.excelwandbars.core.PageLayout.same(cur, l.packed);
        return l;
    }

    String describeLayout(Player p) {
        Mage mage = api.getMage(p);
        Wand w = mage == null ? null : mage.getActiveWand();
        if (w == null) return "no wand";
        Layout l = layoutFor(w, styleOf(p));
        if (l == null) return "no pages (" + styleOf(p).key() + ")";
        return "spells=" + l.spells + " pages=" + l.curPages + "/" + l.pages + " ok=" + l.ok + " packed=" + l.packed;
    }

    /** Remove the plugin's control keys from the held wand so its template rules again (admin rollback aid). */
    int clearOverrides(Player p) {
        Mage mage = api.getMage(p);
        Wand w = mage == null ? null : mage.getActiveWand();
        if (w == null || !isRankWand(w)) return -1;
        w.closeInventory();
        w.saveState();
        int n = 0;
        for (String k : StyleProfile.KEYS) if (w.removeProperty(k)) n++;
        w.deactivate();
        mage.checkWand();
        return n;
    }

    // ------------------------------------------------------------------ player side

    static List<String> howTo(WandStyle s) { return com.winkreyes.excelwandbars.core.ControlsText.howTo(s, false); }

    /** 0.2.2: Bedrock players read tap / press and hold, never "click". */
    List<String> howTo(WandStyle s, Player p) { return com.winkreyes.excelwandbars.core.ControlsText.howTo(s, plugin.isBedrock(p)); }

    /** Set and apply. Returns the message lines for the player. */
    List<String> choose(Player p, WandStyle s) {
        List<String> out = new ArrayList<>();
        if (s == WandStyle.BARS && !barsAllowed(p)) {
            out.add("Sneak bars are for Bedrock players (phone, tablet, console).");
            out.add("On Java your wand uses Classic or Cycle: right click picks the next spell, left click casts.");
            return out;
        }
        if (plugin.tips() != null) plugin.tips().ack(p); // picking a style = the wand tip is understood
        boolean changed = prefs.set(p.getUniqueId(), s);
        if (changed || prefs.hasChoice(p.getUniqueId())) savePrefs();
        Result r = apply(p);
        out.add("Wand controls: " + s.label() + (changed ? "" : " (already set)") + ".");
        out.addAll(howTo(s, p));
        if (s == WandStyle.BARS) out.add("Like Classic better? Switch back any time.");
        if (s == WandStyle.CYCLE) out.add("Want the spell list back? Pick Classic. Your spells: /spellbook.");
        if (s == WandStyle.TAPBAR) out.add(plugin.isBedrock(p) ? "Best on a phone or tablet. Your items are kept safe while the spell bar is up." : "Your items are kept safe while the spell bar is up.");
        if (s == WandStyle.ORIGINAL) out.add("This is how the wand first worked. Your items are kept safe while the spell bar is up.");
        if (r == Result.NO_WAND || r == Result.NOT_RANK_WAND) out.add("Your wand switches the next time you hold it.");
        else if (r == Result.THROTTLED) out.add("Hold your wand again in a few seconds to finish switching.");
        else if (r == Result.FAILED) out.add("Your wand did not switch. Tell Mike.");
        out.add(barsAllowed(p) ? "Change any time: /wandstyle or Wand controls in the compass menu." : "See this again: /wandstyle");
        return out;
    }

    /** /wandstyle with no argument: Bedrock form, or clickable chat for Java (Classic only). */
    void openChooser(Player p) {
        WandStyle cur = styleOf(p);
        if (!barsAllowed(p)) {
            // Java: every style but Sneak bars (Bedrock only). One line each.
            p.sendMessage(Component.text("Wand controls (now: " + com.winkreyes.excelwandbars.core.ControlsText.shortName(cur, false) + "). Click one:", NamedTextColor.GOLD));
            for (WandStyle s : com.winkreyes.excelwandbars.core.ControlsText.chooserOrder(false)) {
                p.sendMessage(Component.text(" [" + com.winkreyes.excelwandbars.core.ControlsText.buttonName(s, false) + "]" + (s == cur ? " (now)" : ""), NamedTextColor.AQUA)
                        .hoverEvent(HoverEvent.showText(Component.text(String.join("\n", howTo(s)))))
                        .clickEvent(ClickEvent.runCommand("/wandstyle " + s.typed()))
                        .append(Component.text(" " + com.winkreyes.excelwandbars.core.ControlsText.oneLine(s, false), NamedTextColor.YELLOW)));
            }
            p.sendMessage(Component.text(" Sneak bars are for Bedrock players.", NamedTextColor.GRAY));
            return;
        }
        // 0.2.4: five buttons, name on line 1 and a one-line description on line 2.
        final boolean bedrock = plugin.isBedrock(p);
        final WandStyle[] order = com.winkreyes.excelwandbars.core.ControlsText.chooserOrder(true).toArray(new WandStyle[0]);
        List<String> buttons = new ArrayList<>();
        for (WandStyle s : order) buttons.add(com.winkreyes.excelwandbars.core.ControlsText.button(s, cur, bedrock));
        String content = com.winkreyes.excelwandbars.core.ControlsText.chooserContent(cur, bedrock, true);
        java.util.function.IntConsumer cb = id -> {
            if (id < 0 || id >= order.length) return;
            WandStyle pick = order[id];
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!p.isOnline()) return;
                for (String line : choose(p, pick)) p.sendMessage(Component.text(line, NamedTextColor.YELLOW));
            });
        };
        lastChooser.put(p.getUniqueId(), new java.util.AbstractMap.SimpleEntry<>(buttons, content));
        lastChooserCb.put(p.getUniqueId(), cb);
        boolean sent = BedrockForms.isBedrock(p) && forms.simple(p, "Wand controls", content, buttons, cb);
        if (sent) return;
        // No form (Geyser missing, or an admin test player): clickable chat.
        p.sendMessage(Component.text(com.winkreyes.excelwandbars.core.ControlsText.chooserHeading(cur.label(), plugin.isBedrock(p)), NamedTextColor.GOLD));
        for (WandStyle s : order) {
            p.sendMessage(Component.text(" [" + com.winkreyes.excelwandbars.core.ControlsText.buttonName(s, bedrock) + "]" + (s == cur ? " (now)" : ""), NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(Component.text(String.join("\n", howTo(s, p)))))
                    .clickEvent(ClickEvent.runCommand("/wandstyle " + s.typed()))
                    .append(Component.text(" " + com.winkreyes.excelwandbars.core.ControlsText.oneLine(s, bedrock), NamedTextColor.YELLOW)));
        }
    }

    /** 0.2.3 rig aid: the last Wand controls chooser built for a player (buttons, text). */
    private final Map<UUID, Map.Entry<List<String>, String>> lastChooser = new java.util.HashMap<>();
    Map.Entry<List<String>, String> lastChooser(Player p) { return lastChooser.get(p.getUniqueId()); }
    private final Map<UUID, java.util.function.IntConsumer> lastChooserCb = new java.util.HashMap<>();
    /** Rig aid: run the chooser's button handler as if the player tapped button n (same code as the form). */
    boolean chooserTap(Player p, int n) { java.util.function.IntConsumer cb = lastChooserCb.get(p.getUniqueId()); if (cb == null) return false; cb.accept(n); return true; }
    void forgetChooser(Player p) { lastChooser.remove(p.getUniqueId()); lastChooserCb.remove(p.getUniqueId()); }
}
