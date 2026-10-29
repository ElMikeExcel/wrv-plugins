// Excel signature - WRV maintained source; existing authorship notices preserved.
import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.JavaConfirm;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Selector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 0.1.4 Java purchase confirm: which chest clicks get a Confirm/Cancel screen. */
public final class TestJavaConfirm {
    private static MenuDef load(String id, String yaml) {
        DefinitionLoader.Result r = MenuFixtures.loadString(id, yaml);
        if (!r.ok()) throw new IllegalStateException(r.errors().toString());
        return r.def();
    }
    public static void run() {
        MenuDef modal = load("m", """
                format: 1
                title: 'T'
                size: 9
                items:
                  pink:
                    material: PINK_DYE
                    slot: 0
                    clicks:
                      left:
                      - '[console] excelcosmetics buy %player_name% color pink'
                      shift-left:
                      - '[console] excelcosmetics buy %player_name% color pink 4'
                      right:
                      - '[console] excelcosmetics buy %player_name% color pink own'
                    bedrock:
                      mode: modal
                      left-label: Rent 1 week - $200
                      right-label: Own forever - $15,000
                      four-label: Rent 4 weeks - $700
                  bare_buy:
                    material: PINK_DYE
                    slot: 1
                    clicks:
                      left:
                      - '[console] excelcosmetics buy %player_name% emotes command-pack'
                  browse:
                    material: BREAD
                    slot: 2
                    clicks:
                      left:
                      - '[player] excelshop browse bread'
                  veil:
                    material: POTION
                    slot: 3
                    clicks:
                      left:
                      - '[console] mobveil buy %player_name%'
                  nav:
                    material: ARROW
                    slot: 4
                    clicks:
                      left:
                      - '[openguimenu] spawnshop'
                  display:
                    material: GOLD_NUGGET
                    slot: 5
                """);
        MenuDef.ItemDef pink = modal.item("pink");
        Check.ok(JavaConfirm.required(pink, Selector.clickLines(pink, "left")), "modal left needs confirm");
        Check.ok(JavaConfirm.required(pink, Selector.clickLines(pink, "right")), "modal right (own forever) needs confirm");
        Check.ok(JavaConfirm.required(pink, Selector.clickLines(pink, "shift-left")), "modal shift-left needs confirm");
        Check.eq("Confirm: Own forever - $15,000", JavaConfirm.confirmLabel(pink, "right"), "right label");
        Check.eq("Confirm: Rent 1 week - $200", JavaConfirm.confirmLabel(pink, "left"), "left label");
        Check.eq("Confirm: Rent 4 weeks - $700", JavaConfirm.confirmLabel(pink, "shift-left"), "four label");
        Check.eq("Confirm", JavaConfirm.confirmLabel(pink, "middle"), "unknown kind falls back");
        MenuDef.ItemDef bare = modal.item("bare_buy");
        Check.ok(JavaConfirm.required(bare, Selector.clickLines(bare, "left")), "non-modal cosmetics buy still confirms");
        Check.eq("Confirm", JavaConfirm.confirmLabel(bare, "left"), "non-modal label");
        Check.ok(!JavaConfirm.required(modal.item("browse"), Selector.clickLines(modal.item("browse"), "left")), "excelshop browse: own review, no extra screen");
        Check.ok(!JavaConfirm.required(modal.item("veil"), Selector.clickLines(modal.item("veil"), "left")), "mobveil buy: own review, no extra screen");
        Check.ok(!JavaConfirm.required(modal.item("nav"), Selector.clickLines(modal.item("nav"), "left")), "navigation: no confirm");
        Check.ok(!JavaConfirm.required(modal.item("display"), Selector.clickLines(modal.item("display"), "left")), "display piece: no confirm");
        Check.ok(!JavaConfirm.required(pink, null), "no lines: no confirm");
        // Every shipped menu: every excelcosmetics buy click is covered, nothing else changes.
        try (var files = Files.list(Path.of("menus"))) {
            int buys = 0, gated = 0, other = 0;
            for (Path f : files.filter(p -> p.toString().endsWith(".yml")).sorted().toList()) {
                String id = f.getFileName().toString().replace(".yml", "");
                DefinitionLoader.Result r = MenuFixtures.loadString(id, Files.readString(f));
                if (!r.ok()) continue;
                for (MenuDef.ItemDef it : r.def().items) {
                    for (String k : List.of("left", "right", "shift-left", "shift-right", "middle", "any")) {
                        List<String> lines = Selector.clickLines(it, k);
                        if (lines == null) continue;
                        boolean buy = lines.stream().anyMatch(l -> l.contains("excelcosmetics buy"));
                        boolean req = JavaConfirm.required(it, lines);
                        if (buy) { buys++; if (req) gated++; }
                        else if (req) other++;
                    }
                }
            }
            Check.ok(buys > 0, "shipped menus contain cosmetics buys (" + buys + ")");
            Check.eq(buys, gated, "every shipped cosmetics buy click is gated");
            Check.eq(0, other, "no non-buy shipped click is gated");
        } catch (java.io.IOException e) { Check.ok(false, "read menus: " + e); }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
