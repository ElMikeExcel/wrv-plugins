package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * ExcelHalloweenToggles — the /scares surface view model (0.1.1).
 *
 * Pure render logic for the two tappable surfaces Mike required on
 * Nov 1, 2026: the Bedrock GeyserForms list and the Java chest GUI.
 * Both surfaces are built from THIS one model — button labels, GUI
 * slots, icons, names and lore — so the two platforms can never drift
 * apart in wording or state display (house build style: one
 * implementation, thin per-platform renderers). No Bukkit player or
 * inventory state lives here; a surface is a pure function of the
 * registry plus a state lookup, which is what the unit tests pin.
 *
 * Icon and wording choices mirror the ExcelMenu 2.0 halloween_scares
 * definition (packet-files/halloween_scares.yml) exactly: same
 * materials, same state marks (✔ ON green / ✘ OFF gray), same
 * one-line descriptions, so /scares feels like the menu it replaces.
 */
public final class ScareView {

    /** Chest GUI geometry: 27 slots, scares in the middle row from
     *  slot 10 (the ExcelMenu definition's layout), Close at 22. */
    public static final int GUI_SIZE = 27;
    public static final int FIRST_SLOT = 10;
    public static final int CLOSE_SLOT = 22;

    /** Label of the form's final button — tapping it (id == feature
     *  count) ends the form instead of flipping anything. */
    public static final String CLOSE_LABEL = "Close";

    private ScareView() {}

    public static String stateWord(boolean on) {
        return on ? "ON" : "OFF";
    }

    /** One Bedrock form button: "Slenderman: ✔ ON". */
    public static String formButton(FeatureDef feature, boolean on) {
        return feature.displayName() + ": " + (on ? "✔ ON" : "✘ OFF");
    }

    /** The full Bedrock form payload: one button per scare in registry
     *  order, then Close. Button ids are indexes into this array. */
    public static String[] formButtons(FeatureRegistry registry, Function<String, Boolean> state) {
        List<FeatureDef> all = registry.all();
        String[] out = new String[all.size() + 1];
        for (int i = 0; i < all.size(); i++) {
            FeatureDef f = all.get(i);
            out[i] = formButton(f, Boolean.TRUE.equals(state.apply(f.key())));
        }
        out[all.size()] = CLOSE_LABEL;
        return out;
    }

    /** Feature key behind a form button id, or null when the id is
     *  Close (== feature count), a dismissal (-1), or out of range. */
    public static String buttonKey(FeatureRegistry registry, int buttonId) {
        List<FeatureDef> all = registry.all();
        if (buttonId < 0 || buttonId >= all.size()) {
            return null;
        }
        return all.get(buttonId).key();
    }

    /** The icon the ExcelMenu definition uses for this scare. */
    public static Material icon(FeatureDef feature) {
        return switch (feature.key()) {
            case "login_pumpkin" -> Material.CARVED_PUMPKIN;
            case "pumpkin_curse" -> Material.JACK_O_LANTERN;
            case "slenderman" -> Material.ENDER_EYE;
            case "darkness" -> Material.INK_SAC;
            case "lightning" -> Material.LIGHTNING_ROD;
            case "fire" -> Material.CAMPFIRE;
            case "herobrine" -> Material.SKELETON_SKULL;
            default -> Material.PUMPKIN;
        };
    }

    /** The one-line description the ExcelMenu definition shows. */
    public static List<String> loreLines(FeatureDef feature) {
        return switch (feature.key()) {
            case "login_pumpkin" -> List.of("§7A pumpkin lands on your head when you join");
            case "pumpkin_curse" -> List.of("§7A pumpkin lands on your head every so often");
            case "slenderman" -> List.of("§7A tall stranger flashes in front of you");
            case "darkness" -> List.of("§7Your screen goes dark for a moment");
            case "lightning" -> List.of("§7Lightning strikes near you — looks only, no damage");
            case "fire" -> List.of("§7You catch fire for show — no damage");
            case "herobrine" -> List.of("§7He watches from the dark at night,",
                    "§7a little closer every time");
            default -> List.of();
        };
    }

    /** Full lore for one GUI item: description + the flip instruction
     *  for the CURRENT state (the ExcelMenu ON/OFF variant wording). */
    public static List<String> guiLore(FeatureDef feature, boolean on) {
        List<String> out = new ArrayList<>(loreLines(feature));
        out.add(on ? "§7Click to turn it OFF" : "§7Click to turn it ON");
        return out;
    }

    /** Item display name: green "Name: ✔ ON" / gray "Name: ✘ OFF" —
     *  identical wording to the ExcelMenu state variants. */
    public static String guiName(FeatureDef feature, boolean on) {
        return (on ? "§a§l" : "§7§l") + feature.displayName() + ": " + (on ? "✔ ON" : "✘ OFF");
    }

    /** One rendered GUI item. */
    public record GuiItem(int slot, String key, Material material, String name, List<String> lore) {}

    /** The chest-GUI model: items in registry order at slots 10..,
     *  plus the slot → key map the click handler resolves against. */
    public static final class GuiModel {
        private final List<GuiItem> items;
        private final Map<Integer, String> keyBySlot;

        private GuiModel(List<GuiItem> items, Map<Integer, String> keyBySlot) {
            this.items = items;
            this.keyBySlot = keyBySlot;
        }

        public List<GuiItem> items() { return items; }

        /** Feature key at a GUI slot, or null (Close / filler / the
         *  player's own inventory slots are never resolved here). */
        public String keyAt(int slot) { return keyBySlot.get(slot); }
    }

    public static GuiModel gui(FeatureRegistry registry, Function<String, Boolean> state) {
        List<GuiItem> items = new ArrayList<>();
        Map<Integer, String> bySlot = new LinkedHashMap<>();
        List<FeatureDef> all = registry.all();
        for (int i = 0; i < all.size(); i++) {
            FeatureDef f = all.get(i);
            boolean on = Boolean.TRUE.equals(state.apply(f.key()));
            int slot = FIRST_SLOT + i;
            items.add(new GuiItem(slot, f.key(), icon(f), guiName(f, on), guiLore(f, on)));
            bySlot.put(slot, f.key());
        }
        return new GuiModel(List.copyOf(items), Map.copyOf(bySlot));
    }
}
