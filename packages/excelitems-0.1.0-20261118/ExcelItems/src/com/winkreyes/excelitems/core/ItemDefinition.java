package com.winkreyes.excelitems.core;

import java.util.List;
import java.util.regex.Pattern;

/**
 * One custom item, defined once as data (CUSTOM ITEM REGISTRY design
 * law, Mike, Nov 17, 2026): a permanent ID number + a stable key +
 * everything needed to mint the item. The item's identity is carried
 * in its own PersistentDataContainer at mint time (see ItemFactory in
 * the Bukkit layer): the primary mark is excelitems:id = this ID, and
 * during the migration era the factory also stamps the item's legacy
 * mark (if any) so the owning plugin keeps recognizing freshly minted
 * items exactly as before.
 *
 * <p>Pure data, Bukkit-free: the material is the Bukkit Material name
 * as a string and the potion colour is an RGB int, so the plain-JVM
 * suites can pin every field. Display name and lore carry legacy '&'
 * colour codes; the factory translates them at mint time.
 *
 * <p>Kind: ITEM = a physical ItemStack is handed to the player.
 * ACTIVATION = buying applies an effect immediately and no physical
 * item ever exists (today: death protection basic, whose shop icon is
 * a CHEST display only). For an ACTIVATION definition, create()
 * yields the display/representative stack only; delivery stays with
 * the owning plugin.
 *
 * <p>handlerId names the behavior registered against this ID by its
 * owning plugin (e.g. "excelmobveil:veil-drink"); it is a routing
 * label, not code — behavior code lives in the owning plugin, never
 * in ExcelItems (the registry stays small, stable and boring).
 */
public record ItemDefinition(
        int id,
        String key,
        String displayName,
        String material,
        Integer potionColorRgb,
        String basePotionType,
        boolean glint,
        List<String> lore,
        Kind kind,
        String legacyNamespace,
        String legacyKey,
        String legacyLoreKey,
        int loreVersion,
        String handlerId) {

    public enum Kind { ITEM, ACTIVATION }

    private static final Pattern KEY_SHAPE = Pattern.compile("[a-z0-9_]+");
    private static final Pattern MATERIAL_SHAPE = Pattern.compile("[A-Z0-9_]+");

    public ItemDefinition {
        if (id <= 0) throw new IllegalArgumentException("registry id must be positive: " + id);
        if (key == null || !KEY_SHAPE.matcher(key).matches())
            throw new IllegalArgumentException("registry key must match [a-z0-9_]+: " + key);
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("display name required for " + key);
        if (material == null || !MATERIAL_SHAPE.matcher(material).matches())
            throw new IllegalArgumentException("material must be a Bukkit Material name: " + material);
        if (potionColorRgb != null && (potionColorRgb < 0 || potionColorRgb > 0xFFFFFF))
            throw new IllegalArgumentException("potion colour out of RGB range: " + potionColorRgb);
        lore = lore == null ? List.of() : List.copyOf(lore);
        if (kind == null) throw new IllegalArgumentException("kind required for " + key);
        boolean hasLegacy = legacyNamespace != null && legacyKey != null;
        boolean noLegacy = legacyNamespace == null && legacyKey == null;
        if (!hasLegacy && !noLegacy)
            throw new IllegalArgumentException("legacy mark needs both namespace and key: " + key);
        if (legacyLoreKey != null && !hasLegacy)
            throw new IllegalArgumentException("legacy lore key without a legacy mark: " + key);
        if (loreVersion < 0) throw new IllegalArgumentException("lore version must be >= 0: " + key);
    }

    /** True when this item carries a pre-registry identity mark. */
    public boolean hasLegacyMark() {
        return legacyNamespace != null;
    }
}
