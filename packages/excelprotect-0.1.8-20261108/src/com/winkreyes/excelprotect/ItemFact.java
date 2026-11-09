package com.winkreyes.excelprotect;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * ExcelProtect — one item fact, two layers (SPEC §1): the FINGERPRINT (id, count, damage,
 * enchantments, custom name, trim) is the search layer present on every item fact; the
 * PAYLOAD (gzip + Base64 of Bukkit serializeAsBytes — ExcelDeathRecord's model) is the
 * restore layer, present on restorable records. Payloads restore byte-exact stacks.
 * Excel — Wink Reyes Valley
 */
public record ItemFact(String id, int count, int damage, String name,
                       Map<String, Integer> enchants, String trimMaterial, String trimPattern,
                       String payload, String rid) {

    /** Provenance record-ID key (Amendment 3.4), set by the main class at enable. */
    public static org.bukkit.NamespacedKey RID_KEY;
    public static void init(org.bukkit.plugin.Plugin plugin) {
        RID_KEY = new org.bukkit.NamespacedKey(plugin, "rid");
    }

    public static ItemFact of(ItemStack s, boolean withPayload) {
        if (s == null || s.getType() == Material.AIR || s.getAmount() <= 0) return null;
        int dmg = 0; String name = null;
        TreeMap<String, Integer> ench = new TreeMap<>();
        String tm = null, tp = null;
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            if (m instanceof Damageable d && d.hasDamage()) dmg = d.getDamage();
            if (m.hasDisplayName()) name = m.getDisplayName().replaceAll("§.", "");
            for (Map.Entry<Enchantment, Integer> e : m.getEnchants().entrySet())
                ench.put(e.getKey().getKey().toString(), e.getValue());
            if (m instanceof EnchantmentStorageMeta es)
                for (Map.Entry<Enchantment, Integer> e : es.getStoredEnchants().entrySet())
                    ench.put(e.getKey().getKey().toString(), e.getValue());
            if (m instanceof ArmorMeta am && am.hasTrim() && am.getTrim() != null) {
                tm = am.getTrim().getMaterial().getKey().toString();
                tp = am.getTrim().getPattern().getKey().toString();
            }
        }
        String payload = null;
        if (withPayload) {
            try { payload = encode(s.serializeAsBytes()); } catch (Throwable ignored) { payload = null; }
        }
        String rid = null;
        if (m != null && RID_KEY != null) {
            try { rid = m.getPersistentDataContainer().get(RID_KEY,
                    org.bukkit.persistence.PersistentDataType.STRING); } catch (Throwable ignored) { }
        }
        return new ItemFact(s.getType().getKey().toString(), s.getAmount(), dmg, name,
                ench.isEmpty() ? null : ench, tm, tp, payload, rid);
    }

    /** Durability left at a recorded damage value: never negative, 0 when the item is spent/broken. */
    public static int durabilityRemaining(int damage, int maxDurability) {
        if (maxDurability <= 0) return 0;
        return Math.max(0, maxDurability - Math.max(0, damage));
    }

    /** Restore the byte-exact stack, or null when this fact carries no payload. */
    public ItemStack restore() {
        if (payload == null) return null;
        try { return ItemStack.deserializeBytes(decode(payload)); } catch (Throwable t) { return null; }
    }

    static String encode(byte[] raw) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bos)) { gz.write(raw); }
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) { return Base64.getEncoder().encodeToString(raw); }
    }

    static byte[] decode(String b64) throws IOException {
        byte[] raw = Base64.getDecoder().decode(b64);
        if (raw.length > 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b) {
            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                return gz.readAllBytes();
            }
        }
        return raw;
    }

    /** Short search fingerprint: id#count plus name/enchant markers. */
    public String fingerprint() {
        return id + "#" + count + (name != null ? " \"" + name + "\"" : "")
                + (enchants != null ? " " + enchants.keySet() : "");
    }

    public Json.Obj toJson() {
        Json.Obj o = new Json.Obj().put("id", id).put("count", count);
        if (damage > 0) o.put("damage", damage);
        if (name != null) o.put("name", name);
        if (enchants != null) {
            Json.Obj e = new Json.Obj();
            enchants.forEach(e::put);
            o.put("enchants", e);
        }
        if (trimMaterial != null) o.put("trim", new Json.Obj().put("material", trimMaterial).put("pattern", trimPattern));
        if (payload != null) o.put("bytes", payload);
        if (rid != null) o.put("rid", rid);
        return o;
    }

    /** Rebuild a fact from an export/ledger JSON object (restore path reads `bytes`). */
    public static List<ItemFact> listFromJson(String jsonArray) {
        // Ledger lines are parsed by the restore/L&F layer only for records it wrote; the
        // payload string is extracted verbatim so restores never round-trip through a lossy model.
        List<ItemFact> out = new ArrayList<>();
        for (ItemFact f : slotListFromJson(jsonArray)) if (f != null) out.add(f);
        return out;
    }

    /**
     * Slot-preserving variant of listFromJson (0.1.6): entries with no item
     * id (the `{empty:true}` placeholders snapshot writers emit for empty
     * slots) come back as null AT THEIR INDEX, so a slot-exact restore can
     * reproduce the recorded layout — empties included — instead of
     * compacting everything to slot 0.
     */
    public static List<ItemFact> slotListFromJson(String jsonArray) {
        List<ItemFact> out = new ArrayList<>();
        if (jsonArray == null) return out;
        for (String obj : splitObjects(jsonArray)) {
            String id = Json.strField(obj, "id");
            if (id == null) { out.add(null); continue; }
            int count = (int) Json.numField(obj, "count", 1);
            String bytes = Json.strField(obj, "bytes");
            String name = Json.strField(obj, "name");
            out.add(new ItemFact(id, count, (int) Json.numField(obj, "damage", 0), name, null, null, null, bytes,
                    Json.strField(obj, "rid")));
        }
        return out;
    }

    private static List<String> splitObjects(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = -1; boolean str = false, esc = false;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (str) { if (esc) esc = false; else if (c == '\\') esc = true; else if (c == '"') str = false; continue; }
            if (c == '"') str = true;
            else if (c == '{') { if (depth++ == 0) start = i; }
            else if (c == '}') { if (--depth == 0 && start >= 0) { out.add(arr.substring(start, i + 1)); start = -1; } }
        }
        return out;
    }
}
