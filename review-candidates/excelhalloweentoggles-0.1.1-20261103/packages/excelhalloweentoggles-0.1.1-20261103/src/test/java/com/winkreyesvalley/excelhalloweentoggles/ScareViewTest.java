package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.Material;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * ExcelHalloweenToggles — the 0.1.1 tappable-surface view model.
 *
 * Pins the render contract both platforms share: state → label/icon
 * mapping (identical to the ExcelMenu halloween_scares definition),
 * form payload shape (7 scare buttons + Close, ids = registry order),
 * GUI slot mapping, and flip round-trips through the real ToggleStore
 * — rebuild the payload from the store after a flip and the new state
 * is what the player sees (the re-open / re-render guarantee).
 */
public class ScareViewTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private FeatureRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = FeatureRegistry.fromConfig(ToggleStoreTest.loadShippedConfig());
    }

    private Function<String, Boolean> allOn() {
        return key -> true;
    }

    @Test
    public void formPayloadIsSevenStateButtonsPlusClose() {
        String[] buttons = ScareView.formButtons(registry, allOn());
        assertEquals(8, buttons.length);
        assertEquals("Close", buttons[7]);
        for (int i = 0; i < 7; i++) {
            FeatureDef f = registry.all().get(i);
            assertEquals(f.displayName() + ": ✔ ON", buttons[i]);
        }
    }

    @Test
    public void formButtonsReflectMixedStates() {
        String[] buttons = ScareView.formButtons(registry, key -> !"darkness".equals(key));
        assertEquals("Darkness: ✘ OFF", buttons[3]);
        assertEquals("Slenderman: ✔ ON", buttons[2]);
        assertEquals("Herobrine: ✔ ON", buttons[6]);
    }

    @Test
    public void buttonIdsResolveInRegistryOrderAndCloseIsNotAFeature() {
        for (int i = 0; i < 7; i++) {
            assertEquals(registry.all().get(i).key(), ScareView.buttonKey(registry, i));
        }
        assertNull("the Close button must flip nothing", ScareView.buttonKey(registry, 7));
        assertNull("a dismissed form reports -1", ScareView.buttonKey(registry, -1));
        assertNull(ScareView.buttonKey(registry, 99));
    }

    @Test
    public void iconsMatchTheExcelMenuDefinition() {
        Map<String, Material> expected = Map.of(
                "login_pumpkin", Material.CARVED_PUMPKIN,
                "pumpkin_curse", Material.JACK_O_LANTERN,
                "slenderman", Material.ENDER_EYE,
                "darkness", Material.INK_SAC,
                "lightning", Material.LIGHTNING_ROD,
                "fire", Material.CAMPFIRE,
                "herobrine", Material.SKELETON_SKULL);
        for (FeatureDef f : registry.all()) {
            assertEquals("icon for " + f.key(), expected.get(f.key()), ScareView.icon(f));
        }
    }

    @Test
    public void guiModelRendersStateAtAGlance() {
        ScareView.GuiModel model = ScareView.gui(registry, key -> !"fire".equals(key));
        assertEquals(7, model.items().size());
        for (int i = 0; i < 7; i++) {
            ScareView.GuiItem item = model.items().get(i);
            FeatureDef f = registry.all().get(i);
            assertEquals(10 + i, item.slot());
            assertEquals(f.key(), item.key());
            assertEquals(f.key(), model.keyAt(item.slot()));
        }
        ScareView.GuiItem fire = model.items().get(5);
        assertTrue(fire.name().contains("✘ OFF"));
        assertTrue("OFF items render gray", fire.name().startsWith("§7"));
        assertEquals("§7Click to turn it ON", fire.lore().get(fire.lore().size() - 1));
        ScareView.GuiItem slender = model.items().get(2);
        assertTrue(slender.name().contains("✔ ON"));
        assertTrue("ON items render green", slender.name().startsWith("§a"));
        assertEquals("§7Click to turn it OFF", slender.lore().get(slender.lore().size() - 1));
        assertNull("Close slot is not a feature", model.keyAt(ScareView.CLOSE_SLOT));
        assertNull(model.keyAt(0));
    }

    @Test
    public void flipRoundTripsThroughTheStoreIntoBothPayloads() {
        ToggleStore store = new ToggleStore(new File(tmp.getRoot(), "players.yml"),
                registry, Logger.getLogger("test"));
        UUID p = UUID.randomUUID();
        Function<String, Boolean> state = key -> store.isEnabled(p, key);

        String[] before = ScareView.formButtons(registry, state);
        assertEquals("Slenderman: ✔ ON", before[2]);

        // The form tap path: id → key → the store flip the command performs.
        String key = ScareView.buttonKey(registry, 2);
        assertEquals("slenderman", key);
        assertFalse(store.toggle(p, key));

        String[] reopened = ScareView.formButtons(registry, state);
        assertEquals("the re-opened form must show the flip", "Slenderman: ✘ OFF", reopened[2]);
        ScareView.GuiModel gui = ScareView.gui(registry, state);
        assertTrue("the GUI reads the same store", gui.items().get(2).name().contains("✘ OFF"));

        assertTrue(store.toggle(p, key));
        assertEquals("Slenderman: ✔ ON", ScareView.formButtons(registry, state)[2]);
    }

    @Test
    public void herobrineKeepsItsTwoLineMenuDescription() {
        ScareView.GuiModel model = ScareView.gui(registry, allOn());
        ScareView.GuiItem herobrine = model.items().get(6);
        assertEquals(3, herobrine.lore().size()); // two description lines + flip line
        assertTrue(herobrine.lore().get(0).contains("He watches from the dark"));
    }
}
