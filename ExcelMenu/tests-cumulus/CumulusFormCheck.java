import com.winkreyes.excelmenu.core.BedrockDetail;
import com.winkreyes.excelmenu.core.DefinitionLoader;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Text;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.form.SimpleForm;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Construction proof for the Bedrock item detail form, using the
 * REAL Cumulus classes from the rig's Geyser-Spigot.jar: build
 * detail forms exactly as BedrockMenus does (title, darkened
 * content, buttons composed by core.BedrockDetail) and read the
 * built forms back. No server, no connection — this proves what
 * the send path hands Geyser; the live send is a player proof.
 *
 * 0.1.2: a cosmetics item with a four-label builds FOUR buttons
 * ([Rent 1 week, Own forever, Rent 4 weeks, Back]); an item
 * without one builds the 0.1.1 form — three buttons, unchanged.
 *
 * Run: java -cp <core-classes>:<Geyser-Spigot.jar> CumulusFormCheck
 * (working directory = the ExcelMenu project dir, for menus/.)
 */
public final class CumulusFormCheck {

    private static int failures = 0;

    private static void eq(Object expected, Object actual, String name) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        System.out.println((same ? "  PASS " : "  FAIL ") + name
                + (same ? "" : "  expected=" + expected + " actual=" + actual));
        if (!same) failures++;
    }

    private static void ok(boolean cond, String name) {
        System.out.println((cond ? "  PASS " : "  FAIL ") + name);
        if (!cond) failures++;
    }

    /** Exactly BedrockMenus.sendItemDetail's composition. */
    private static SimpleForm buildDetail(String title, MenuDef.ItemDef item) {
        List<String> lore = new ArrayList<>(item.lore());
        String content = Text.bedrockText(
                item.name() == null ? item.id() : item.name(), lore);
        List<String> buttons = new ArrayList<>();
        for (String label : BedrockDetail.buttonLabels(item.bedrock())) {
            buttons.add(BedrockDetail.BACK_LABEL.equals(label)
                    ? label : Text.colorize(label));
        }
        SimpleForm.Builder b = SimpleForm.builder().title(title).content(content);
        for (String label : buttons) b.button(label);
        return b.build();
    }

    public static void main(String[] args) {
        DefinitionLoader.Result r = DefinitionLoader.load(
                Path.of("menus", "spawnshop_cosmetics_colors.yml"));
        if (!r.ok()) throw new IllegalStateException("colors def failed: " + r.errors());
        MenuDef def = r.def();
        String title = Text.colorize(def.title);

        // --- the 0.1.2 four button: Pink and Coral detail forms ---
        for (String itemId : new String[]{"pink", "coral"}) {
            MenuDef.ItemDef item = def.item(itemId);
            SimpleForm form = buildDetail(title, item);

            List<ButtonComponent> buttons = form.buttons();
            eq(4, buttons.size(), itemId + ": detail form has 4 buttons");
            eq("Rent 1 week - $200", buttons.get(0).text(), itemId + ": button 1 is Rent 1 week");
            eq("Own forever - $15,000", buttons.get(1).text(), itemId + ": button 2 is Own forever");
            eq("Rent 4 weeks - $700", buttons.get(2).text(), itemId + ": button 3 is Rent 4 weeks");
            eq("Back", buttons.get(3).text(), itemId + ": button 4 is Back");
            String content = Text.bedrockText(item.name(), item.lore());
            eq(content, form.content(), itemId + ": form content is the composed text");
            String firstLine = form.content().split("\n", -1)[0];
            eq(Text.colorize(item.name()), firstLine,
                    itemId + ": content title line keeps authored color");
            ok(!form.content().toLowerCase().substring(firstLine.length())
                            .matches("(?s).*§[7feab].*"),
                    itemId + ": no pale code anywhere below the title line");
            System.out.println("  FORM[" + itemId + "] title=" + form.title());
            for (String line : form.content().split("\n", -1)) {
                System.out.println("  FORM[" + itemId + "] content| " + line);
            }
        }

        // --- no four-label: the 0.1.1 form, byte-for-byte the same shape ---
        String yaml = """
                format: 1
                title: 'Plain Menu'
                size: 9
                items:
                  thing:
                    material: PINK_DYE
                    slot: 0
                    name: '&d&lThing'
                    lore:
                    - '&7A plain thing.'
                    clicks:
                      left:
                      - '[console] say left'
                      right:
                      - '[console] say right'
                    bedrock:
                      mode: modal
                      left-label: 'Do left - $5'
                      right-label: 'Do right - $9'
                """;
        DefinitionLoader.Result pr = DefinitionLoader.load("plain",
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        if (!pr.ok()) throw new IllegalStateException("plain def failed: " + pr.errors());
        MenuDef.ItemDef plain = pr.def().item("thing");
        SimpleForm plainForm = buildDetail("Plain Menu", plain);
        List<ButtonComponent> pb = plainForm.buttons();
        eq(3, pb.size(), "plain: detail form has 3 buttons (0.1.1 shape)");
        eq("Do left - $5", pb.get(0).text(), "plain: button 1 is the left label");
        eq("Do right - $9", pb.get(1).text(), "plain: button 2 is the right label");
        eq("Back", pb.get(2).text(), "plain: button 3 is Back — no phantom four button");

        System.out.println(failures == 0 ? "CUMULUS CONSTRUCTION: ALL PASS"
                : "CUMULUS CONSTRUCTION: " + failures + " FAILURES");
        if (failures > 0) System.exit(1);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
