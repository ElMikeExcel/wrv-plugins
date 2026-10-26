package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.api.wand.WandTemplate;
import com.winkreyes.excelwandbars.core.BarLoadout;
import com.winkreyes.excelwandbars.core.LoadoutText;
import com.winkreyes.excelwandbars.core.SpellbookText;
import com.winkreyes.excelwandbars.core.PageLayout;
import com.winkreyes.excelwandbars.core.SpellInfoText;
import com.winkreyes.excelwandbars.core.WandStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 0.2.9: each player's own spell bar (loadout). Saved per player in loadouts.yml
 * (server data folder only). StyleHook asks it for the bar layout; the Spell Book and
 * /spellbar change it. Config bar-loadout.enabled: false = the 0.2.8 "every spell on the
 * bar in pages" behaviour.
 */
final class LoadoutHook implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final File file;
    private final Map<UUID, BarLoadout> map = new HashMap<>();
    boolean enabled = true;
    int size = BarLoadout.DEFAULT_SLOTS;
    boolean newSpellLines = true;
    int maxLines = 3;
    private boolean saveQueued = false;
    private long saves = 0, seeded = 0, placed = 0, toBook = 0, changes = 0;
    private String lastSync = "-";

    LoadoutHook(ExcelWandBars plugin, MagicAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.file = new File(plugin.getDataFolder(), "loadouts.yml");
    }

    void loadSettings(ConfigurationSection c) {
        enabled = c == null || c.getBoolean("enabled", true);
        size = BarLoadout.clampSize(c == null ? BarLoadout.DEFAULT_SLOTS : c.getInt("slots", BarLoadout.DEFAULT_SLOTS));
        newSpellLines = c == null || c.getBoolean("new-spell-lines", true);
        maxLines = Math.max(1, Math.min(10, c == null ? 3 : c.getInt("max-new-spell-lines", 3)));
        boolean any = false;
        for (BarLoadout l : map.values()) if (l.size() != size) { l.resize(size); any = true; }
        if (any) saveSoon();
    }

    String summary() {
        return enabled ? "ON (" + size + " slots, new spell lines " + (newSpellLines ? "ON, max " + maxLines : "off") + ", " + map.size() + " player(s) saved)"
                : "off (bar-loadout.enabled: false, every spell on the bar in pages as in 0.2.8)";
    }

    String counters() {
        return "seeded " + seeded + " placed " + placed + " to-book " + toBook + " changes " + changes + " saves " + saves + " last " + lastSync;
    }

    // ------------------------------------------------------------------ storage

    void load() {
        map.clear();
        try {
            if (!file.isFile()) return;
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection s = y.getConfigurationSection("players");
            if (s == null) return;
            for (String k : s.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(k);
                    map.put(id, BarLoadout.restore(s.getStringList(k + ".slots"), s.getStringList(k + ".seen"), size));
                } catch (IllegalArgumentException bad) { plugin.getLogger().warning("loadouts.yml: skipped " + k); }
            }
        } catch (Throwable t) { plugin.getLogger().warning("loadouts.yml: " + t); }
    }

    /** Write to loadouts.yml.tmp, then move over loadouts.yml (never a half-written file). */
    void save() {
        saveQueued = false;
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.set("version", 1);
            for (Map.Entry<UUID, BarLoadout> e : map.entrySet()) {
                y.set("players." + e.getKey() + ".slots", e.getValue().storedSlots());
                y.set("players." + e.getKey() + ".seen", new ArrayList<>(e.getValue().seen()));
            }
            if (!plugin.getDataFolder().isDirectory()) plugin.getDataFolder().mkdirs();
            File tmp = new File(plugin.getDataFolder(), "loadouts.yml.tmp");
            y.save(tmp);
            try { Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException no) { Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            saves++;
        } catch (Throwable t) { plugin.getLogger().warning("loadouts.yml: " + t); }
    }

    void saveSoon() {
        if (saveQueued) return;
        saveQueued = true;
        try { Bukkit.getScheduler().runTaskLater(plugin, () -> { if (saveQueued) save(); }, 20L); }
        catch (Throwable t) { save(); } // plugin disabling
    }

    BarLoadout get(UUID id) { return map.get(id); }

    void forget(UUID id) { if (map.remove(id) != null) saveSoon(); }

    // ------------------------------------------------------------------ wand reads

    static Map<String, Integer> current(com.elmakers.mine.bukkit.wand.Wand impl) {
        Map<String, Integer> cur = new HashMap<>();
        for (Map.Entry<String, Integer> e : impl.getSpellInventory().entrySet()) cur.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        return cur;
    }

    static Collection<String> known(Wand w) {
        List<String> out = new ArrayList<>();
        if (w instanceof com.elmakers.mine.bukkit.wand.Wand impl) { for (String k : impl.getBaseSpells()) if (k != null) out.add(k.toLowerCase(Locale.ROOT)); return out; }
        Collection<String> sp = w.getSpells();
        if (sp != null) for (String k : sp) { String n = BarLoadout.norm(k); if (n != null && !out.contains(n)) out.add(n); }
        return out;
    }

    /** The wand's spells in the order the player sees them now (bar slots, then the rest). */
    static List<String> currentOrder(Wand w) {
        if (w instanceof com.elmakers.mine.bukkit.wand.Wand impl) return PageLayout.order(current(impl), known(w));
        return new ArrayList<>(known(w));
    }

    /** The wand template's own spell list (then its parents'), the curated order for new spells. */
    static List<String> templateOrder(Wand w) {
        List<String> out = new ArrayList<>();
        try {
            WandTemplate t = w.getTemplate();
            for (int d = 0; t != null && d < 32; d++, t = t.getParent()) {
                ConfigurationSection c = t.getConfiguration();
                if (c != null) for (String s : c.getStringList("spells")) { String k = BarLoadout.norm(s); if (k != null && !out.contains(k)) out.add(k); }
            }
        } catch (Throwable ignored) { }
        return out;
    }

    static List<String> placementOrder(Wand w) {
        Map<String, Integer> cur = w instanceof com.elmakers.mine.bukkit.wand.Wand impl ? current(impl) : new HashMap<>();
        return BarLoadout.placementOrder(templateOrder(w), cur, known(w));
    }

    String spellName(String key) {
        try {
            SpellTemplate t = api.getController().getSpellTemplate(key);
            if (t != null && t.getName() != null) return SpellInfoText.strip(t.getName());
        } catch (Throwable ignored) { }
        return key;
    }

    Map<String, String> names(Collection<String> keys) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String k : keys) if (k != null) m.put(k, spellName(k));
        return m;
    }

    // ------------------------------------------------------------------ the bar

    /**
     * Seed (first time) or sync (new spells) this player's loadout against the wand they hold.
     * announce: send the "New spell" lines (inventory styles only).
     */
    BarLoadout.Sync observe(Player p, Wand w, boolean announce) {
        UUID id = p.getUniqueId();
        BarLoadout l = map.get(id);
        if (l == null) {
            l = BarLoadout.seed(currentOrder(w), size);
            map.put(id, l);
            seeded++;
            lastSync = p.getName() + " seeded " + l.filled() + "/" + l.size();
            saveSoon();
            return new BarLoadout.Sync();
        }
        BarLoadout.Sync r = l.sync(placementOrder(w));
        if (!r.changed()) return r;
        placed += r.placed.size();
        toBook += r.toBook.size();
        lastSync = p.getName() + " placed " + r.placed + " book " + r.toBook;
        saveSoon();
        plugin.getLogger().info("Spell bar for " + p.getName() + ": " + r.count() + " new spell(s), " + r.placed.size() + " on the bar, " + r.toBook.size() + " in the Spellbook.");
        if (announce && newSpellLines) {
            List<String> on = new ArrayList<>(), book = new ArrayList<>();
            for (String k : r.placed.keySet()) on.add(spellName(k));
            for (String k : r.toBook) book.add(spellName(k));
            for (String line : LoadoutText.announce(on, book, maxLines)) {
                NamedTextColor c = line.startsWith("Change") ? NamedTextColor.GRAY : NamedTextColor.GREEN;
                Component m = Component.text(line, c);
                if (!plugin.isBedrock(p) && line.startsWith("Change")) m = m.clickEvent(ClickEvent.runCommand("/spellbar"));
                p.sendMessage(m);
            }
        }
        return r;
    }

    /** The layout for this wand; never stores anything (an unseen player is previewed as seeded). */
    BarLoadout.Layout layout(Player p, Wand w, int perPage) {
        BarLoadout l = map.get(p.getUniqueId());
        if (l == null) l = BarLoadout.seed(currentOrder(w), size);
        return l.layout(currentOrder(w), perPage);
    }

    // ------------------------------------------------------------------ menus (/spellbar, Spell Book buttons)

    private static final class Item { final String label, args; Item(String l, String a) { label = l; args = a; } }

    private void menu(Player p, String title, String content, List<Item> items) {
        boolean bedrock = plugin.isBedrock(p);
        MagicHook mh = plugin.magicHook();
        if (bedrock && mh != null) {
            List<String> labels = new ArrayList<>();
            for (Item i : items) labels.add(LoadoutText.fit(i.label));
            boolean sent = mh.sendForm(p, LoadoutText.fit(title), content, labels, id -> {
                if (id < 0 || id >= items.size()) return;
                String a = items.get(id).args;
                if (a == null) return;
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) handle(p, a.isEmpty() ? new String[0] : a.split(" ")); });
            });
            if (sent) return;
        }
        p.sendMessage(Component.text(title, NamedTextColor.GOLD));
        for (String line : content.split("\n")) p.sendMessage(Component.text(" " + line, NamedTextColor.GRAY));
        for (Item i : items) {
            if (i.args == null) continue; // Close
            p.sendMessage(Component.text(" [" + i.label + "]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand(i.args.startsWith("@book") ? "/spellbook" + i.args.substring(5) : "/spellbar " + i.args)));
        }
    }

    /** The wand the menus read: the held rank wand, else the Spell Book's wand. */
    private Wand menuWand(Player p) {
        Mage mage = api.getMage(p);
        MagicHook mh = plugin.magicHook();
        return mh == null ? (mage == null ? null : mage.getActiveWand()) : mh.bookWand(p, mage);
    }

    private int perPage(Player p) {
        StyleHook s = plugin.styleHook();
        return s == null ? 7 : s.perPageFor(s.styleOf(p));
    }

    /** null = fine; else the line to show. */
    private String blocked(Player p) {
        StyleHook s = plugin.styleHook();
        if (!enabled || s == null || !s.enabled() || !s.pagesOn()) return LoadoutText.OFF;
        if (!s.styleOf(p).inventory()) return LoadoutText.NOT_BAR_STYLE;
        return null;
    }

    /** Is the bar loadout on for this player's style (Spell Book buttons)? */
    boolean usable(Player p) { return blocked(p) == null; }

    private BarLoadout ensure(Player p, Wand w) {
        BarLoadout l = map.get(p.getUniqueId());
        if (l == null) { observe(p, w, false); l = map.get(p.getUniqueId()); }
        else observe(p, w, true);
        return l;
    }

    void openBar(Player p) {
        String why = blocked(p);
        if (why != null) { p.sendMessage(Component.text(why, NamedTextColor.GRAY)); return; }
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return; }
        BarLoadout l = ensure(p, w);
        Collection<String> known = known(w);
        Map<String, String> nm = names(known);
        int inBook = 0;
        for (String k : known) if (l.indexOf(k) < 0) inBook++;
        List<Item> items = new ArrayList<>();
        int used = 0;
        for (int i = 0; i < l.size(); i++) {
            String k = l.get(i);
            boolean ok = k != null && known.contains(k);
            if (ok) used++;
            items.add(new Item(LoadoutText.slotButton(i, ok ? nm.get(k) : null), "slot " + (i + 1)));
        }
        items.add(new Item(LoadoutText.BACK_BOOK, "@book"));
        items.add(new Item(LoadoutText.CLOSE, null));
        menu(p, LoadoutText.barTitleCount(used, l.size()), LoadoutText.barContent(plugin.isBedrock(p), perPage(p), l.size(), inBook), items);
    }

    void openSlot(Player p, int slot) {
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return; }
        BarLoadout l = ensure(p, w);
        if (slot < 0 || slot >= l.size()) { openBar(p); return; }
        Collection<String> known = known(w);
        String k = l.get(slot);
        String name = k != null && known.contains(k) ? spellName(k) : null;
        List<Item> items = new ArrayList<>();
        items.add(new Item(LoadoutText.PICK_BUTTON, "pick " + (slot + 1)));
        if (name != null) {
            items.add(new Item(LoadoutText.moveButton(name), "put " + k));
            items.add(new Item(LoadoutText.offButton(name), "remove " + (slot + 1)));
        }
        items.add(new Item(LoadoutText.BACK_BAR, ""));
        items.add(new Item(LoadoutText.CLOSE, null));
        menu(p, LoadoutText.slotTitle(slot, name), name == null ? "This slot is empty." : name + " is in this slot.", items);
    }

    void openPickSpell(Player p, int slot) {
        MagicHook mh = plugin.magicHook();
        if (plugin.spellBookGroups() && mh != null) { openPickGroups(p, slot); return; }
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return; }
        BarLoadout l = ensure(p, w);
        if (slot < 0 || slot >= l.size()) { openBar(p); return; }
        List<Item> items = new ArrayList<>();
        for (String k : currentOrder(w)) {
            if (k.equals(l.get(slot))) continue;
            items.add(new Item(LoadoutText.pickSpellButton(spellName(k), l.indexOf(k)), "put " + k + " " + (slot + 1)));
        }
        items.add(new Item(LoadoutText.BACK_BAR, ""));
        items.add(new Item(LoadoutText.CLOSE, null));
        menu(p, LoadoutText.slotTitle(slot, l.get(slot) == null ? null : spellName(l.get(slot))), LoadoutText.pickSpellContent(slot), items);
    }

    // ------------------------------------------------------------------ 0.3.0 picker by Spell Book group

    /** The wand's spells (all but the one already in this slot) as Spell Book entries with bar marks. */
    private List<SpellbookText.Entry> pickEntries(Wand w, BarLoadout l, int slot) {
        List<String> keys = new ArrayList<>();
        for (String k : currentOrder(w)) if (!k.equals(l.get(slot))) keys.add(k);
        List<SpellbookText.Entry> out = plugin.magicHook().plainEntries(keys, this::spellName);
        for (SpellbookText.Entry e : out) e.barSlot = l.indexOf(e.key);
        return out;
    }

    /** Pick a spell for a slot, step 1: the groups (spells the wand knows only), then All A to Z. */
    void openPickGroups(Player p, int slot) {
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return; }
        BarLoadout l = ensure(p, w);
        if (slot < 0 || slot >= l.size()) { openBar(p); return; }
        List<SpellbookText.Entry> es = pickEntries(w, l, slot);
        List<Item> items = new ArrayList<>();
        for (SpellbookText.GroupCount c : SpellbookText.counts(plugin.spellbookGroups(), es, false))
            items.add(new Item(SpellbookText.pickGroupButton(c), "pick " + (slot + 1) + " " + c.key));
        items.add(new Item(SpellbookText.ALL_BUTTON, "pick " + (slot + 1) + " " + SpellbookText.ALL));
        items.add(new Item(LoadoutText.BACK_BAR, ""));
        items.add(new Item(LoadoutText.CLOSE, null));
        menu(p, LoadoutText.slotTitle(slot, l.get(slot) == null ? null : spellName(l.get(slot))), SpellbookText.pickGroupContent(slot), items);
    }

    /** Step 2: one group's spells, paged (Bedrock 12, Java 10), with the 0.2.9 bar marks. */
    void openPickList(Player p, int slot, String typed, int page) {
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return; }
        BarLoadout l = ensure(p, w);
        if (slot < 0 || slot >= l.size()) { openBar(p); return; }
        com.winkreyes.excelwandbars.core.SpellbookGroups g = plugin.spellbookGroups();
        boolean all = SpellbookText.ALL.equalsIgnoreCase(typed);
        String gk = all ? SpellbookText.ALL : g.find(typed);
        if (gk == null) { openPickGroups(p, slot); return; }
        List<SpellbookText.Entry> list = SpellbookText.list(g, pickEntries(w, l, slot), gk, false);
        int per = plugin.isBedrock(p) ? SpellbookText.BEDROCK_PER_PAGE : SpellbookText.JAVA_PER_PAGE;
        int pg = SpellbookText.clampPage(page, list.size(), per);
        int pages = SpellbookText.pages(list.size(), per);
        List<Item> items = new ArrayList<>();
        for (SpellbookText.Entry e : SpellbookText.page(list, pg, per))
            items.add(new Item(LoadoutText.pickSpellButton(e.name, e.barSlot), "put " + e.key + " " + (slot + 1)));
        String base = "pick " + (slot + 1) + " " + gk + " ";
        if (pg + 1 < pages) items.add(new Item(SpellbookText.NEXT, base + (pg + 2)));
        if (pg > 0) items.add(new Item(SpellbookText.PREV, base + pg));
        items.add(new Item(SpellbookText.GROUPS, "pick " + (slot + 1)));
        items.add(new Item(LoadoutText.BACK_BAR, ""));
        items.add(new Item(LoadoutText.CLOSE, null));
        String name = all ? SpellbookText.allName() : g.nameOf(gk);
        menu(p, SpellbookText.pickListTitle(slot, name, pg, pages), LoadoutText.pickSpellContent(slot), items);
    }

    void openPickSlot(Player p, String key) {
        String why = blocked(p);
        if (why != null) { p.sendMessage(Component.text(why, NamedTextColor.GRAY)); return; }
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return; }
        String k = BarLoadout.norm(key);
        Collection<String> known = known(w);
        if (k == null || !known.contains(k)) { p.sendMessage(Component.text(LoadoutText.NOT_KNOWN, NamedTextColor.GRAY)); return; }
        BarLoadout l = ensure(p, w);
        String name = spellName(k);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            String o = l.get(i);
            items.add(new Item(LoadoutText.slotButton(i, o != null && known.contains(o) ? spellName(o) : null), "put " + k + " " + (i + 1)));
        }
        items.add(new Item(LoadoutText.BACK_BAR, ""));
        items.add(new Item(LoadoutText.CLOSE, null));
        menu(p, LoadoutText.pickTitle(name), LoadoutText.pickSlotContent(name), items);
    }

    /** 0.3.0: the bar's spell keys by slot (null = empty slot), or null when there is no own bar here. Never stores. */
    List<String> barKeys(Player p) {
        if (!usable(p)) return null;
        BarLoadout l = map.get(p.getUniqueId());
        if (l == null) {
            Wand w = menuWand(p);
            if (w == null) return null;
            l = BarLoadout.seed(currentOrder(w), size);
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) out.add(l.get(i));
        return out;
    }

    /** Detail page buttons: on the bar now? (-1 = no; -2 = loadout not usable here). */
    int slotOf(Player p, String key) {
        if (!usable(p)) return -2;
        BarLoadout l = map.get(p.getUniqueId());
        if (l == null) {
            Wand w = menuWand(p);
            if (w == null) return -2;
            l = BarLoadout.seed(currentOrder(w), size);
        }
        return l.indexOf(key);
    }

    private void done(Player p, BarLoadout.Change c, boolean reopen) {
        if (c.ok()) {
            changes++;
            saveSoon();
            plugin.getLogger().info("Spell bar for " + p.getName() + ": " + c);
            StyleHook s = plugin.styleHook();
            if (s != null) Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) s.applyNow(p, "spell bar change"); });
        }
        List<String> keys = new ArrayList<>();
        if (c.spell != null) keys.add(c.spell);
        if (c.other != null) keys.add(c.other);
        p.sendMessage(Component.text(LoadoutText.changed(c, names(keys)), c.ok() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        if (reopen) openBar(p);
    }

    private static int num(String s) { try { return Integer.parseInt(s.trim()) - 1; } catch (Exception e) { return -1; } }

    /** /spellbar [slot n | pick n | put spell [n] | remove n | swap a b]. Returns a status word (rig tests). */
    String handle(Player p, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("@book")) { if (plugin.magicHook() != null) plugin.magicHook().openSpellBook(p, null); return "book"; }
        String why = blocked(p);
        if (why != null) { p.sendMessage(Component.text(why, NamedTextColor.GRAY)); return "blocked"; }
        Wand w = menuWand(p);
        if (w == null) { p.sendMessage(Component.text(LoadoutText.NO_WAND, NamedTextColor.GRAY)); return "no-wand"; }
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        BarLoadout l = ensure(p, w);
        Collection<String> known = known(w);
        switch (sub) {
            case "": openBar(p); return "bar";
            case "slot": if (a.length > 1) { openSlot(p, num(a[1])); return "slot"; } break;
            case "pick":
                // 0.3.0: pick <slot> <group|all> [page] = one group's spells for that slot.
                if (a.length > 2 && plugin.spellBookGroups() && plugin.magicHook() != null) { openPickList(p, num(a[1]), a[2], a.length > 3 ? Math.max(0, num(a[3])) : 0); return "pick-group"; }
                if (a.length > 1) { openPickSpell(p, num(a[1])); return "pick"; } break;
            case "put":
                if (a.length == 2) { openPickSlot(p, a[1]); return "pick-slot"; }
                if (a.length > 2) {
                    String k = BarLoadout.norm(a[1]);
                    if (k == null || !known.contains(k)) { p.sendMessage(Component.text(LoadoutText.NOT_KNOWN, NamedTextColor.GRAY)); return "not-known"; }
                    BarLoadout.Change c = l.put(k, num(a[2]));
                    done(p, c, true);
                    return c.toString();
                }
                break;
            case "remove": if (a.length > 1) { BarLoadout.Change c = l.remove(num(a[1])); done(p, c, true); return c.toString(); } break;
            case "swap": if (a.length > 2) { BarLoadout.Change c = l.swap(num(a[1]), num(a[2])); done(p, c, true); return c.toString(); } break;
            default: break;
        }
        openBar(p);
        return "bar";
    }

    /** Admin/rig view. */
    List<String> describe(Player p) {
        List<String> out = new ArrayList<>();
        BarLoadout l = map.get(p.getUniqueId());
        out.add("loadout " + p.getName() + ": " + (l == null ? "not seeded yet" : l.filled() + "/" + l.size() + " slots " + l.storedSlots() + " seen " + l.seen().size()));
        Wand w = menuWand(p);
        if (w != null) {
            BarLoadout.Layout lay = layout(p, w, perPage(p));
            out.add("  wand " + w.getTemplateKey() + " knows " + known(w).size() + " | layout pages " + lay.pages + " on bar " + lay.onBar + " in book " + lay.inBook + " " + lay.slots);
        }
        out.add("  " + counters());
        return out;
    }

    /** Admin: reseed from the held wand (rig aid). */
    String reseed(Player p) {
        Wand w = menuWand(p);
        if (w == null) return "no wand";
        map.remove(p.getUniqueId());
        observe(p, w, false);
        return "seeded " + map.get(p.getUniqueId()).storedSlots();
    }

    static boolean barStyle(WandStyle s) { return s != null && s.inventory(); }
}
