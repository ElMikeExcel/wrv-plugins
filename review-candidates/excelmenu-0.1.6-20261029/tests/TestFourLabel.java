// Excel signature - WRV maintained source; existing authorship notices preserved.
import com.winkreyes.excelmenu.core.BedrockDetail;
import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.MenuValidator;
import com.winkreyes.excelmenu.core.Selector;

import java.util.List;

/**
 * The 0.1.2 Bedrock four button (Follow-up B of the
 * ExcelCosmetics 0.1.1 pricing packet): a modal item's
 * four-label renders a third action button that dispatches the
 * item's shift-left click lines — the Bedrock mirror of the
 * Java shift-left click, closing the 4-week-rental parity gap.
 * Covers: the loader reading four-label, the BedrockDetail
 * button/index mapping the renderer shares, the resolved
 * dispatch lines, the validator's label⇔lines pairing in both
 * directions, and the no-four-label item composing EXACTLY as
 * it did in 0.1.1 (no phantom button).
 */
public final class TestFourLabel {

    private static final String MODAL_HEAD = """
            format: 1
            title: 'Test Menu'
            size: 9
            items:
              thing:
                material: PINK_DYE
                slot: 0
                name: 'Thing'
                clicks:
                  left:
                  - '[console] say left'
                  right:
                  - '[console] say right'
            """;

    private static final String SHIFT_LINES = """
                  shift-left:
                  - '[console] say four'
            """;

    private static String bedrockBlock(String fourLabel) {
        return """
                bedrock:
                  mode: modal
                  left-label: 'Do left'
                  right-label: 'Do right'
            """ + (fourLabel == null ? "" : "      four-label: '" + fourLabel + "'\n");
    }

    private static MenuDef load(String yaml) {
        DefinitionLoader.Result r = MenuFixtures.loadString("four_test", yaml);
        if (!r.ok()) throw new IllegalStateException("fixture failed to load: " + r.errors());
        return r.def();
    }

    private static List<String> validate(MenuDef def) {
        return MenuValidator.validate(List.of(def), MenuFixtures.materialNames());
    }

    private static boolean hasError(List<String> errors, String needle) {
        for (String e : errors) if (e.contains(needle)) return true;
        return false;
    }

    public static void run() {
        List<MenuDef> defs = MenuFixtures.loadShipped();
        MenuDef colors = MenuFixtures.byId(defs, "spawnshop_cosmetics_colors");
        MenuDef.ItemDef pink = colors.item("pink");

        // --- parse: the loader reads four-label off the bedrock block ---
        Check.eq("Rent 4 weeks - $700", pink.bedrock().fourLabel(),
                "pink four-label parsed from the shipped colors menu");

        // --- the shared mapping: [left, right, four, Back] ---
        Check.eq(List.of("Rent 1 week - $200", "Own forever - $15,000", "Rent 4 weeks - $700"),
                BedrockDetail.actionLabels(pink.bedrock()),
                "pink action labels: left, right, four in order");
        Check.eq(List.of("Rent 1 week - $200", "Own forever - $15,000",
                        "Rent 4 weeks - $700", "Back"),
                BedrockDetail.buttonLabels(pink.bedrock()),
                "pink detail buttons: three actions + Back last");
        Check.eq(List.of("left", "right", "shift-left"),
                BedrockDetail.actionKinds(pink.bedrock()),
                "pink action kinds: the four button is shift-left");
        Check.eq("left", BedrockDetail.clickKind(pink.bedrock(), 0), "index 0 -> left");
        Check.eq("right", BedrockDetail.clickKind(pink.bedrock(), 1), "index 1 -> right");
        Check.eq("shift-left", BedrockDetail.clickKind(pink.bedrock(), 2),
                "index 2 (the four button) -> shift-left");
        Check.eq(null, BedrockDetail.clickKind(pink.bedrock(), 3),
                "index 3 is Back, not a click kind");
        Check.eq(null, BedrockDetail.clickKind(pink.bedrock(), -1),
                "closed form (negative index) maps to nothing");
        Check.ok(BedrockDetail.isBack(pink.bedrock(), 3), "index 3 is Back for a four item");
        Check.ok(!BedrockDetail.isBack(pink.bedrock(), 2), "index 2 is NOT Back for a four item");

        // --- dispatch: the four button resolves to the real buy line ---
        Check.eq(List.of("[console] excelcosmetics buy %player_name% color pink 4"),
                Selector.clickLines(pink, BedrockDetail.clickKind(pink.bedrock(), 2)),
                "pink four button dispatches the 4-week console buy, byte-exact");

        // --- fleet sweep: all 34 modal items carry the pair ---
        int modal = 0, four = 0;
        for (MenuDef def : defs) {
            for (MenuDef.ItemDef item : def.items) {
                if (item.bedrock() == null) {
                    List<String> sl = item.clicks().get("shift-left");
                    Check.ok(sl == null || sl.isEmpty(),
                            def.id + ":" + item.id()
                                    + " no bedrock block and no orphan shift-left lines");
                    continue;
                }
                modal++;
                List<String> sl = item.clicks().get("shift-left");
                boolean hasLines = sl != null && !sl.isEmpty();
                boolean hasLabel = item.bedrock().fourLabel() != null
                        && !item.bedrock().fourLabel().isBlank();
                Check.ok(hasLines && hasLabel,
                        def.id + ":" + item.id() + " shift-left lines paired with a four-label");
                Check.eq(List.of("left", "right", "shift-left"),
                        BedrockDetail.actionKinds(item.bedrock()),
                        def.id + ":" + item.id() + " composes [left, right, shift-left]");
                if (hasLabel) four++;
            }
        }
        Check.eq(34, modal, "34 modal items across the shipped set (chat colors v2: 21 colors)");
        Check.eq(34, four, "all 34 modal items carry a four-label");

        // --- no four-label: the 0.1.1 shape, exactly ---
        MenuDef plain = load(MODAL_HEAD + bedrockBlock(null));
        MenuDef.ItemDef plainItem = plain.item("thing");
        Check.eq(null, plainItem.bedrock().fourLabel(),
                "a modal block without four-label parses to null");
        Check.eq(List.of("Do left", "Do right", "Back"),
                BedrockDetail.buttonLabels(plainItem.bedrock()),
                "no four-label: buttons are [left, right, Back] as in 0.1.1");
        Check.eq(List.of("left", "right"), BedrockDetail.actionKinds(plainItem.bedrock()),
                "no four-label: kinds are [left, right] as in 0.1.1");
        Check.ok(BedrockDetail.isBack(plainItem.bedrock(), 2),
                "no four-label: index 2 is Back as in 0.1.1");
        Check.eq(null, BedrockDetail.clickKind(plainItem.bedrock(), 2),
                "no four-label: index 2 dispatches nothing");
        Check.eq(List.of(), validate(plain),
                "modal with neither four-label nor shift-left lines validates clean");

        // --- validator pairing, both directions are load errors ---
        MenuDef both = load(MODAL_HEAD + SHIFT_LINES + bedrockBlock("Do four"));
        Check.eq(List.of(), validate(both),
                "modal with shift-left lines AND a four-label validates clean");
        Check.eq("Do four", both.item("thing").bedrock().fourLabel(),
                "synthetic four-label parsed");

        MenuDef linesOnly = load(MODAL_HEAD + SHIFT_LINES + bedrockBlock(null));
        Check.ok(hasError(validate(linesOnly), "no four-label"),
                "shift-left lines without a four-label: refused (parity gap made loud)");

        MenuDef labelOnly = load(MODAL_HEAD + bedrockBlock("Do four"));
        Check.ok(hasError(validate(labelOnly), "no shift-left click lines"),
                "four-label without shift-left lines: refused (dead button made loud)");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
