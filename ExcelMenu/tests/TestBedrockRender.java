import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.MenuValidator;
import com.winkreyes.excelmenu.core.Selector;

import java.util.List;
import java.util.Map;

/**
 * 0.1.3 — bedrock-render: chest. The Safe Blocks shop category is
 * the one menu Bedrock players get as a chest GUI (real block
 * icons through Geyser) instead of a text form; every other menu
 * keeps the form default. Covers: the flag on the shipped set
 * (exactly one chest menu), the root wiring and slot order, the
 * shelf's items/prices/dispatch (identical to the ExcelShops
 * catalog offers), loader parsing (default, case, bad value is a
 * validator error, misspelled key is a load error), and render
 * parity (the visible set both platforms consume).
 */
public final class TestBedrockRender {

    private static final String FIXTURE = """
            format: 1
            title: '&6Fixture'
            size: 9
            type: dispenser
            %s
            items:
              stone:
                material: STONE
                slot: 0
            """;

    public static void run() {
        List<MenuDef> defs = MenuFixtures.loadShipped();

        // Exactly one shipped menu opts out of the form.
        int chestCount = 0;
        for (MenuDef d : defs) if (d.bedrockChest()) chestCount++;
        Check.eq(1, chestCount, "exactly one shipped menu renders chest for Bedrock");
        for (MenuDef d : defs) {
            Check.eq(d.id.equals("spawnshop_safe_blocks"), d.bedrockChest(),
                    d.id + " bedrockChest=" + d.bedrockChest());
        }

        // Root wiring: the new category, and the re-slot that keeps
        // the Bedrock form order content -> info -> Back last.
        MenuDef shop = MenuFixtures.byId(defs, "spawnshop");
        MenuDef.ItemDef cat = shop.item("category_safe_blocks");
        Check.eq("LIGHT", cat.material(), "safe blocks category icon is the light block itself");
        Check.eq(5, cat.slot(), "safe blocks category at root slot 5");
        Check.eq(List.of("[openguimenu] spawnshop_safe_blocks"), cat.clicks().get("left"),
                "safe blocks category left click opens the shelf");
        Check.eq(List.of("[openguimenu] spawnshop_safe_blocks"), cat.clicks().get("right"),
                "safe blocks category right click opens the shelf");
        Check.eq(6, shop.item("category_cosmetics").slot(), "cosmetics re-slotted to 6");
        Check.eq(7, shop.item("balance").slot(), "balance re-slotted to 7");
        Check.eq(8, shop.item("back").slot(), "back re-slotted to 8 (last)");

        // The shelf itself: same offers as the ExcelShops catalog.
        MenuDef shelf = MenuFixtures.byId(defs, "spawnshop_safe_blocks");
        Check.eq("chest", shelf.bedrockRender, "shelf declares bedrock-render: chest");
        Check.eq("dispenser", shelf.type, "shelf is a dispenser menu");
        Check.eq(9, shelf.size, "shelf size 9");
        Check.eq(4, shelf.items.size(), "shelf has 4 items (2 products + balance + back)");

        MenuDef.ItemDef light = shelf.item("light_block");
        Check.eq("LIGHT", light.material(), "light block icon is LIGHT");
        Check.eq(0, light.slot(), "light block at slot 0");
        Check.eq(4, light.amount(), "light block shows the 4-block bundle");
        Check.eq(List.of("[player] excelshop browse light_block"), light.clicks().get("left"),
                "light block left click dispatches the catalog browse");
        Check.eq(List.of("[player] excelshop browse light_block"), light.clicks().get("right"),
                "light block right click dispatches the catalog browse");
        Check.ok(light.lore().toString().contains("$300"), "light block lore names the $300 bundle price");

        MenuDef.ItemDef slab = shelf.item("petrified_slab");
        Check.eq("PETRIFIED_OAK_SLAB", slab.material(), "slab icon is PETRIFIED_OAK_SLAB");
        Check.eq(1, slab.slot(), "slab at slot 1");
        Check.eq(64, slab.amount(), "slab shows the 64-slab bundle");
        Check.eq(List.of("[player] excelshop browse petrified_slab"), slab.clicks().get("left"),
                "slab left click dispatches the catalog browse");
        Check.eq(List.of("[player] excelshop browse petrified_slab"), slab.clicks().get("right"),
                "slab right click dispatches the catalog browse");
        Check.ok(slab.lore().toString().contains("$240"), "slab lore names the $240 bundle price");

        Check.ok(shelf.item("balance").clicks().isEmpty(), "balance is display-only");
        Check.eq(List.of("[openguimenu] spawnshop"), shelf.item("back").clicks().get("left"),
                "shelf back returns to the shop categories");

        // Parity: one visible set feeds both renderers.
        Map<Integer, MenuDef.ItemDef> vis = Selector.visibleItems(shelf, new MenuFixtures.FakeContext());
        Check.eq(4, vis.size(), "shelf renders all 4 slots for any viewer");
        Check.eq("light_block", vis.get(0).id(), "slot 0 is the light block in the shared visible set");
        Check.eq("petrified_slab", vis.get(1).id(), "slot 1 is the slab in the shared visible set");

        // Loader: default, explicit form, chest, case-insensitivity.
        DefinitionLoader.Result r = MenuFixtures.loadString("fx_default", FIXTURE.formatted(""));
        Check.ok(r.ok(), "fixture without bedrock-render loads");
        Check.eq("form", r.def().bedrockRender, "absent key defaults to form");
        Check.ok(!r.def().bedrockChest(), "default is not chest");

        r = MenuFixtures.loadString("fx_form", FIXTURE.formatted("bedrock-render: form"));
        Check.ok(r.ok() && !r.def().bedrockChest(), "explicit form stays form");

        r = MenuFixtures.loadString("fx_chest", FIXTURE.formatted("bedrock-render: chest"));
        Check.ok(r.ok(), "bedrock-render: chest loads");
        Check.ok(r.def().bedrockChest(), "chest flag reaches the definition");

        r = MenuFixtures.loadString("fx_case", FIXTURE.formatted("bedrock-render: CHEST"));
        Check.ok(r.ok() && r.def().bedrockChest(), "value is case-insensitive");

        // A bad value is a loud validator error, never a fallback.
        r = MenuFixtures.loadString("fx_bogus", FIXTURE.formatted("bedrock-render: modal"));
        Check.ok(r.ok(), "bogus value still parses (validator owns the verdict)");
        List<String> errors = MenuValidator.validate(List.of(r.def()), MenuFixtures.materialNames());
        Check.eq(1, errors.size(), "bogus bedrock-render yields exactly one validation error");
        Check.ok(errors.get(0).contains("bedrock-render"), "the error names bedrock-render: " + errors);

        // A misspelled key is a load error (strict key set).
        r = MenuFixtures.loadString("fx_typo", FIXTURE.formatted("bedrock-renderr: chest"));
        Check.ok(!r.ok(), "misspelled bedrock-renderr is refused at load");
        Check.ok(r.errors().toString().contains("unknown menu key"),
                "the refusal names the unknown key: " + r.errors());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
