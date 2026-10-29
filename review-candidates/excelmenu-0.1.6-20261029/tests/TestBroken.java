// Excel signature - WRV maintained source; existing authorship notices preserved.
import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.MenuValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The anti-regression suite: deliberately broken definitions must
 * fail LOUDLY with named errors — above all a navigation target
 * that does not exist, the exact defect that silently killed the
 * Java cosmetics store under DeluxeMenus on 2026-10-20.
 */
public final class TestBroken {

    private static final String VALID = """
            format: 1
            title: 'Test Menu'
            size: 9
            items:
              only:
                material: STONE
                slot: 0
                name: 'Rock'
            """;

    private static List<String> validate(String id, String yaml) {
        DefinitionLoader.Result r = MenuFixtures.loadString(id, yaml);
        if (!r.ok()) return r.errors();
        return MenuValidator.validate(List.of(r.def()), MenuFixtures.materialNames());
    }

    private static boolean hasError(List<String> errors, String needle) {
        for (String e : errors) if (e.contains(needle)) return true;
        return false;
    }

    public static void run() {
        Check.eq(List.of(), validate("ok_menu", VALID), "control: the valid fixture validates clean");

        // 1. THE defect: a menu pointing at a menu that does not exist.
        List<String> errs = validate("broken_nav", """
                format: 1
                title: 'Broken Nav'
                size: 9
                items:
                  go:
                    material: COMPASS
                    slot: 0
                    clicks:
                      left:
                        - '[openguimenu] menu_that_does_not_exist'
                """);
        Check.ok(hasError(errs, "is not a loaded menu"),
                "missing [openguimenu] target is a load error, not a dead click");

        // 2. Cross-check: a shipped menu linking to the broken one
        //    does not rescue it; the error names the linking menu.
        DefinitionLoader.Result broken = MenuFixtures.loadString("broken_nav2", """
                format: 1
                title: 'Broken Nav 2'
                size: 9
                items:
                  go:
                    material: COMPASS
                    slot: 0
                    clicks:
                      left:
                        - '[openguimenu] also_missing'
                """);
        List<MenuDef> set = new ArrayList<>(MenuFixtures.loadShipped());
        set.add(broken.def());
        List<String> setErrs = MenuValidator.validate(set, MenuFixtures.materialNames());
        Check.ok(hasError(setErrs, "broken_nav2") && hasError(setErrs, "also_missing"),
                "validator names the broken menu in the full set");
        Check.eq(1, setErrs.size(), "only the broken menu errors in the full set");

        // 3. Unknown action tag.
        errs = validate("bad_tag", VALID.replace("name: 'Rock'",
                "clicks:\n      left:\n        - '[teleport] spawn'"));
        Check.ok(hasError(errs, "unknown action tag"), "unknown action tag rejected");

        // 4. Unknown requirement type.
        errs = validate("bad_req", """
                format: 1
                title: 'Bad Req'
                size: 9
                items:
                  gated:
                    material: STONE
                    slot: 0
                    view-requirement:
                      requirements:
                        - id: x
                          type: has-rank
                          rank: gold
                """);
        Check.ok(hasError(errs, "unknown requirement type"), "unknown requirement type rejected");

        // 5. Unknown material.
        errs = validate("bad_mat", VALID.replace("STONE", "STONE2"));
        Check.ok(hasError(errs, "unknown material"), "unknown material rejected");

        // 6. Slot outside the menu.
        errs = validate("bad_slot", VALID.replace("slot: 0", "slot: 9"));
        Check.ok(hasError(errs, "outside menu size"), "slot outside menu size rejected");

        // 7 + 8. Slot collisions.
        String twoItems = """
                format: 1
                title: 'Collision'
                size: 9
                items:
                  a:
                    material: STONE
                    slot: 0
                  b:
                    material: DIRT
                    slot: 0
                """;
        errs = validate("collision", twoItems);
        Check.ok(hasError(errs, "no priority"), "shared slot without priorities rejected");
        errs = validate("collision2", twoItems.replace("material: DIRT",
                "material: DIRT\n    priority: 1").replace("material: STONE",
                "material: STONE\n    priority: 1"));
        Check.ok(hasError(errs, "already used"), "shared slot with duplicate priority rejected");

        // 9. Duplicate YAML key -> loader refuses the file.
        DefinitionLoader.Result dup = MenuFixtures.loadString("dup",
                "format: 1\nformat: 1\ntitle: 'Dup'\nsize: 9\nitems: {}\n");
        Check.ok(!dup.ok(), "duplicate YAML key refused by the loader");

        // 10. Unknown item key -> loader refuses.
        DefinitionLoader.Result unk = MenuFixtures.loadString("unk", """
                format: 1
                title: 'Unknown Key'
                size: 9
                items:
                  a:
                    material: STONE
                    slot: 0
                    enchantments:
                      - 'SILK_TOUCH;1'
                """);
        Check.ok(!unk.ok() && hasError(unk.errors(), "unknown item key"),
                "unknown item key refused by the loader (no silent drops)");

        // 11. Wrong format version.
        DefinitionLoader.Result fmt = MenuFixtures.loadString("fmt",
                VALID.replace("format: 1", "format: 2"));
        Check.ok(!fmt.ok() && hasError(fmt.errors(), "'format' must be 1"),
                "format other than 1 refused");

        // 12. update: true without an interval.
        errs = validate("no_interval", """
                format: 1
                title: 'No Interval'
                size: 9
                items:
                  bal:
                    material: GOLD_NUGGET
                    slot: 0
                    update: true
                """);
        Check.ok(hasError(errs, "update-interval"), "update item without interval rejected");

        // 13. Reserved command claims.
        errs = validate("reserved", VALID.replace("size: 9", "size: 9\ncommand: dm"));
        Check.ok(hasError(errs, "reserved"), "a menu may not claim /dm");

        // 14. Dispenser with a chest size.
        errs = validate("bad_disp", VALID.replace("size: 9", "size: 27\ntype: dispenser"));
        Check.ok(hasError(errs, "dispenser size must be 9"), "dispenser size enforced");

        // 15. has-permission without its option.
        errs = validate("req_missing_opt", """
                format: 1
                title: 'Missing Opt'
                size: 9
                open-requirement:
                  requirements:
                    - id: p
                      type: has-permission
                items:
                  a:
                    material: STONE
                    slot: 0
                """);
        Check.ok(hasError(errs, "missing option 'permission'"),
                "requirement missing its option rejected");

        // 16. Bedrock modal without labels.
        errs = validate("bad_modal", """
                format: 1
                title: 'Bad Modal'
                size: 9
                items:
                  a:
                    material: STONE
                    slot: 0
                    bedrock:
                      mode: modal
                      left-label: 'Go'
                """);
        Check.ok(hasError(errs, "bedrock modal needs both"),
                "bedrock modal without both labels rejected");

        Set<String> mats = MenuFixtures.materialNames();
        Check.ok(mats.contains("PINK_DYE") && mats.size() > 1000,
                "material fixture is the real Bukkit enum (" + mats.size() + " names)");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
