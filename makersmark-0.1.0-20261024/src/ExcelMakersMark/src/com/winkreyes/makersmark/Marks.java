package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.Gear;
import com.winkreyes.makersmark.core.LoreModel;
import com.winkreyes.makersmark.core.Trophies;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Item tag (PersistentDataContainer) read/write and the gray lore lines rebuilt from it. */
final class Marks {
    /** Everything this plugin stores on one item. */
    static final class Data {
        String by, name, date, id;
        int mob, pvp, hits;
        Map<String, Integer> slain = new LinkedHashMap<>();
        boolean marked() { return by != null && name != null; }
        boolean hasStats() { return mob > 0 || pvp > 0 || hits > 0 || !slain.isEmpty(); }
        boolean empty() { return !marked() && !hasStats() && id == null; }
    }

    final NamespacedKey kBy, kName, kDate, kId, kMob, kPvp, kHits, kSlain, kOwned, kWeapon, kOwner;
    private final ExcelMakersMark plugin;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    Marks(ExcelMakersMark plugin) {
        this.plugin = plugin;
        Plugin p = plugin;
        kBy = new NamespacedKey(p, "crafter_uuid");
        kName = new NamespacedKey(p, "crafter_name");
        kDate = new NamespacedKey(p, "crafted_on");
        kId = new NamespacedKey(p, "item_id");
        kMob = new NamespacedKey(p, "kills_mob");
        kPvp = new NamespacedKey(p, "kills_player");
        kHits = new NamespacedKey(p, "hits_blocked");
        kSlain = new NamespacedKey(p, "slain");
        kOwned = new NamespacedKey(p, "lore_lines");
        kWeapon = new NamespacedKey(p, "weapon_id");   // on projectiles
        kOwner = new NamespacedKey(p, "shooter");      // on projectiles
    }

    Data read(ItemMeta m) {
        Data d = new Data();
        if (m == null) return d;
        PersistentDataContainer c = m.getPersistentDataContainer();
        d.by = c.get(kBy, PersistentDataType.STRING);
        d.name = c.get(kName, PersistentDataType.STRING);
        d.date = c.get(kDate, PersistentDataType.STRING);
        d.id = c.get(kId, PersistentDataType.STRING);
        d.mob = c.getOrDefault(kMob, PersistentDataType.INTEGER, 0);
        d.pvp = c.getOrDefault(kPvp, PersistentDataType.INTEGER, 0);
        d.hits = c.getOrDefault(kHits, PersistentDataType.INTEGER, 0);
        d.slain = Trophies.parse(c.get(kSlain, PersistentDataType.STRING));
        return d;
    }

    Data read(ItemStack it) { return it == null || !it.hasItemMeta() ? new Data() : read(it.getItemMeta()); }

    void write(ItemMeta m, Data d) {
        PersistentDataContainer c = m.getPersistentDataContainer();
        str(c, kBy, d.by); str(c, kName, d.name); str(c, kDate, d.date); str(c, kId, d.id);
        num(c, kMob, d.mob); num(c, kPvp, d.pvp); num(c, kHits, d.hits);
        str(c, kSlain, d.slain.isEmpty() ? null : Trophies.serialize(d.slain));
    }

    private static void str(PersistentDataContainer c, NamespacedKey k, String v) {
        if (v == null || v.isEmpty()) c.remove(k); else c.set(k, PersistentDataType.STRING, v);
    }
    private static void num(PersistentDataContainer c, NamespacedKey k, int v) {
        if (v <= 0) c.remove(k); else c.set(k, PersistentDataType.INTEGER, v);
    }

    /**
     * Rebuilds our gray lines from the tag. Any existing line that looks like ours (or that we wrote last time)
     * is removed first, so a hand-written "Crafted by" line can never survive a rebuild.
     */
    void render(ItemMeta m, String material, Data d, boolean showStats) {
        PersistentDataContainer c = m.getPersistentDataContainer();
        Set<String> owned = new HashSet<>();
        String prev = c.get(kOwned, PersistentDataType.STRING);
        if (prev != null && !prev.isEmpty()) owned.addAll(Arrays.asList(prev.split("\n")));
        List<Component> lore = m.hasLore() && m.lore() != null ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.removeIf(line -> { String p = PLAIN.serialize(line); return owned.contains(p) || LoreModel.looksOurs(p); });
        List<String> ours = LoreModel.lines(d.marked() ? d.name : null, Gear.weapon(material), Gear.armor(material),
                d.mob, d.pvp, d.hits, Gear.weapon(material) ? d.slain : Map.of(), showStats && plugin.statsEnabled(),
                plugin::trophyLabel, plugin.wrapAt());
        for (String s : ours) lore.add(Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        m.lore(lore.isEmpty() ? null : lore);
        str(c, kOwned, ours.isEmpty() ? null : String.join("\n", ours));
    }

    /** Write the data and redraw in one meta update. */
    void apply(ItemStack it, Data d, boolean showStats) {
        it.editMeta(m -> { write(m, d); render(m, it.getType().name(), d, showStats); });
    }

    /** Merge for a grid repair or anvil/grindstone combine: first marked wins, counters keep the higher value. */
    static Data merge(Data a, Data b) {
        Data out = new Data();
        Data src = a.marked() ? a : b;
        out.by = src.by; out.name = src.name; out.date = src.date;
        out.id = a.id != null ? a.id : b.id;
        out.mob = Math.max(a.mob, b.mob);
        out.pvp = Math.max(a.pvp, b.pvp);
        out.hits = Math.max(a.hits, b.hits);
        out.slain = Trophies.max(a.slain, b.slain);
        return out;
    }
}
