/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.Gear;
import com.winkreyes.makersmark.core.KillCredit;
import com.winkreyes.makersmark.core.MilestoneColors;
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
        String foundBy, foundName, foundDate;
        int mob, pvp, hits;
        long mobCredit = -1L;
        long creditUnits() { return mobCredit < 0 ? KillCredit.baseline(mob) : mobCredit; }
        void mobKill(boolean reduced) { mobCredit = KillCredit.add(creditUnits(), reduced); if (mob < Integer.MAX_VALUE) mob++; }
        Map<String, Integer> slain = new LinkedHashMap<>();
        Set<String> ownedLines = new HashSet<>(); // transient evidence carried across repair result rebuilds
        boolean marked() { return by != null && name != null; }
        boolean found() { return foundBy != null && foundName != null; }
        boolean hasProvenance() { return by != null || name != null || date != null || foundBy != null || foundName != null || foundDate != null; }
        boolean hasStats() { return mobCredit >= 0 || mob > 0 || pvp > 0 || hits > 0 || !slain.isEmpty(); }
        boolean empty() { return !hasProvenance() && !hasStats() && id == null; }
    }

    final NamespacedKey kBy, kName, kDate, kId, kMob, kPvp, kHits, kSlain, kOwned, kWeapon, kOwner, kFoundBy, kFoundName, kFoundDate, kMobCredit;
    private final ExcelMakersMark plugin;
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    Marks(ExcelMakersMark plugin) {
        this.plugin = plugin;
        Plugin p = plugin;
        kBy = new NamespacedKey(p, "crafter_uuid");
        kName = new NamespacedKey(p, "crafter_name");
        kDate = new NamespacedKey(p, "crafted_on");
        kFoundBy = new NamespacedKey(p, "finder_uuid");
        kFoundName = new NamespacedKey(p, "finder_name");
        kFoundDate = new NamespacedKey(p, "found_on");
        kId = new NamespacedKey(p, "item_id");
        kMob = new NamespacedKey(p, "kills_mob");
        kMobCredit = new NamespacedKey(p, "mob_credit_fifths_v1");
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
        d.foundBy = c.get(kFoundBy, PersistentDataType.STRING);
        d.foundName = c.get(kFoundName, PersistentDataType.STRING);
        d.foundDate = c.get(kFoundDate, PersistentDataType.STRING);
        d.id = c.get(kId, PersistentDataType.STRING);
        d.mob = c.getOrDefault(kMob, PersistentDataType.INTEGER, 0);
        Long credit = c.get(kMobCredit, PersistentDataType.LONG);
        d.mobCredit = credit == null ? -1L : Math.max(0L, credit);
        d.pvp = c.getOrDefault(kPvp, PersistentDataType.INTEGER, 0);
        d.hits = c.getOrDefault(kHits, PersistentDataType.INTEGER, 0);
        d.slain = Trophies.parse(c.get(kSlain, PersistentDataType.STRING));
        String owned = c.get(kOwned, PersistentDataType.STRING);
        if (owned != null && !owned.isEmpty()) d.ownedLines.addAll(Arrays.asList(owned.split("\n")));
        return d;
    }

    Data read(ItemStack it) { return it == null || !it.hasItemMeta() ? new Data() : read(it.getItemMeta()); }

    void write(ItemMeta m, Data d) {
        PersistentDataContainer c = m.getPersistentDataContainer();
        str(c, kBy, d.by); str(c, kName, d.name); str(c, kDate, d.date); str(c, kId, d.id);
        str(c, kFoundBy, d.foundBy); str(c, kFoundName, d.foundName); str(c, kFoundDate, d.foundDate);
        if (d.mobCredit < 0) c.remove(kMobCredit); else c.set(kMobCredit, PersistentDataType.LONG, d.mobCredit);
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
        Set<String> owned = new HashSet<>(d.ownedLines);
        String prev = c.get(kOwned, PersistentDataType.STRING);
        if (prev != null && !prev.isEmpty()) owned.addAll(Arrays.asList(prev.split("\n")));
        List<Component> lore = m.hasLore() && m.lore() != null ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.removeIf(line -> { String p = PLAIN.serialize(line); return owned.contains(p) || LoreModel.looksOurs(p); });
        List<String> ours = LoreModel.lines(d.marked() ? d.name : null, Gear.weapon(material), Gear.blockStats(material),
                d.mob, d.pvp, d.hits, Gear.weapon(material) ? d.slain : Map.of(), showStats && plugin.statsEnabled(),
                plugin::trophyLabel, plugin.wrapAt());
        // Finder is a separate first-tracked-acquisition fact, never a fabricated crafter.
        // Existing unowned lore is not removed by a new broad "Found by" matcher.
        if (!d.marked() && d.found()) ours.add(0, "Found by " + d.foundName);
        if(showStats && plugin.historyRecorder()!=null && d.id!=null) {
            try{var key=new NamespacedKey(plugin,"history_tooltip_v2");var versionKey=new NamespacedKey(plugin,"history_tooltip_revision_v2");long version=c.getOrDefault(new NamespacedKey(plugin,"history_revision_v2"),PersistentDataType.LONG,0L);
                java.util.List<String> lines=plugin.historyRecorder().lines(java.util.UUID.fromString(d.id),version);
                if(lines==null){String saved=c.get(key,PersistentDataType.STRING);long savedVersion=c.getOrDefault(versionKey,PersistentDataType.LONG,0L);lines=saved!=null&&savedVersion==version?java.util.Arrays.asList(saved.split("\\n")):java.util.List.of("History pending display refresh; /mark history page");}
                else {str(c,key,lines.isEmpty()?null:String.join("\n",lines));c.set(versionKey,PersistentDataType.LONG,version);}ours.addAll(lines);
            }catch(IllegalArgumentException ignored){}
        }
        for (String s : ours) lore.add(Component.text(s, MilestoneColors.forCreditLine(s, d.creditUnits(), d.pvp, d.hits)).decoration(TextDecoration.ITALIC, false));
        m.lore(lore.isEmpty() ? null : lore);
        str(c, kOwned, ours.isEmpty() ? null : String.join("\n", ours));
    }

    /** Write the data and redraw in one meta update. */
    void apply(ItemStack it, Data d, boolean showStats) {
        it.editMeta(m -> { write(m, d); render(m, it.getType().name(), d, showStats); });
    }

    /** Preview is not another acquired legacy item; credit comes only from actual inputs. */
    static Data mergeResult(Data inputs, Data result) {
        Data out = merge(inputs, result); out.mobCredit = inputs.creditUnits(); return out;
    }

    /** Merge for a grid repair or anvil/grindstone combine: first marked wins, counters keep the higher value. */
    static Data merge(Data a, Data b) {
        Data out = new Data();
        Data src = a.hasProvenance() ? a : b;
        out.by = src.by; out.name = src.name; out.date = src.date;
        out.foundBy = src.foundBy; out.foundName = src.foundName; out.foundDate = src.foundDate;
        out.id = a.id != null ? a.id : b.id;
        out.mob = Math.max(a.mob, b.mob);
        out.mobCredit = a.mobCredit < 0 && b.mobCredit < 0 ? -1L : Math.max(a.creditUnits(), b.creditUnits());
        out.pvp = Math.max(a.pvp, b.pvp);
        out.hits = Math.max(a.hits, b.hits);
        out.slain = Trophies.max(a.slain, b.slain);
        out.ownedLines.addAll(a.ownedLines); out.ownedLines.addAll(b.ownedLines);
        return out;
    }
}
