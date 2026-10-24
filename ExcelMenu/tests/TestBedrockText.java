import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Text;

import java.util.List;

/**
 * The 0.1.1 Bedrock readability fix (Mike's in-game report: the
 * lore under a Cosmetics color name was pale-on-light-grey,
 * unreadable until the button was highlighted). Content lines are
 * darkened to black on the Bedrock door; item titles keep their
 * authored colors; the exact strings composed here are the exact
 * strings BedrockMenus hands to Cumulus.
 */
public final class TestBedrockText {

    private static String dark(String raw) {
        return Text.darkenPale(Text.colorize(raw));
    }

    private static boolean hasPale(String s) {
        String low = s.toLowerCase();
        for (String code : new String[]{"§7", "§f", "§e", "§a", "§b"}) {
            if (low.contains(code)) return true;
        }
        return false;
    }

    public static void run() {
        // --- darkenPale: the pale palette -> black ---
        Check.eq("§0Warm coral red.", Text.darkenPale("§7Warm coral red."),
                "light grey (§7) description becomes black");
        Check.eq("§0$40", Text.darkenPale("§f$40"), "white (§f) becomes black");
        Check.eq("§0Bundle price: §0$40", dark("&eBundle price: &f$40"),
                "mixed line: yellow lead and white amount both become black");
        Check.eq("§0Left click: rent 1 week - $200", dark("&aLeft click: rent 1 week - $200"),
                "light green (§a) action line becomes black");
        Check.eq("§0Right click: own forever - $2,000", dark("&eRight click: own forever - $2,000"),
                "yellow (§e) action line becomes black");
        Check.eq("§0Aqua", Text.darkenPale("§bAqua"), "aqua (§b) becomes black");
        Check.eq("§0White", Text.darkenPale("§FWhite"), "uppercase §F becomes black");
        Check.eq("§0§lHeader", Text.darkenPale("§7§lHeader"),
                "bold survives while its grey becomes black");
        Check.eq("§0Hi§r there", Text.darkenPale("§7Hi§r there"), "reset code untouched");
        Check.eq("", Text.darkenPale(null), "null darkens to empty");

        // --- darkenPale: saturated tones and hex discipline ---
        Check.eq("§8Command group", Text.darkenPale("§8Command group"),
                "dark grey (§8) headers pass through");
        Check.eq("§c§lCoral", Text.darkenPale("§c§lCoral"), "red (§c) passes through");
        Check.eq("§6Gold", Text.darkenPale("§6Gold"), "gold (§6) passes through");
        Check.eq("§0", Text.darkenPale(Text.colorize("&#FFD9EC")),
                "pale hex (bright pink-white) becomes black as a whole");
        Check.eq(Text.colorize("&#1F3B99"), Text.darkenPale(Text.colorize("&#1F3B99")),
                "dark hex blue survives byte-for-byte (digits not legacy codes)");
        Check.eq(Text.colorize("&#FF55FF"), Text.darkenPale(Text.colorize("&#FF55FF")),
                "saturated hex pink survives (brightness below the pale bar)");

        // --- bedrockText on the REAL shipped definitions ---
        List<MenuDef> defs = MenuFixtures.loadShipped();
        MenuDef colors = MenuFixtures.byId(defs, "spawnshop_cosmetics_colors");

        MenuDef.ItemDef pink = colors.item("pink");
        String[] pinkLines = Text.bedrockText(pink.name(), pink.lore()).split("\n", -1);
        Check.eq("§d§lPink", pinkLines[0], "Pink title keeps its authored color");
        Check.eq("§0Your name & messages in pink.", pinkLines[1],
                "Pink description line is black");
        Check.eq("§0Left click: rent 1 week - $200", pinkLines[2], "Pink rent line is black");
        Check.eq("§0Shift-left click: rent 4 weeks - $700", pinkLines[3],
                "Pink 4-week line is black");
        Check.eq("§0Right click: own forever - $15,000", pinkLines[4], "Pink own line is black");
        Check.eq("§0Rentals end quietly; renewals stack.", pinkLines[5],
                "Pink rental note is black");

        MenuDef.ItemDef coral = colors.item("coral");
        String[] coralLines = Text.bedrockText(coral.name(), coral.lore()).split("\n", -1);
        Check.eq("§c§lCoral", coralLines[0], "Coral title keeps its authored color");
        Check.eq("§0Warm coral red.", coralLines[1],
                "Coral description (Mike's screenshot) is black");

        // --- fleet sweep: no pale code survives in ANY menu's content lines ---
        for (MenuDef def : defs) {
            int violations = 0;
            for (MenuDef.ItemDef item : def.items) {
                String[] lines = Text.bedrockText(
                        item.name() == null ? item.id() : item.name(), item.lore())
                        .split("\n", -1);
                for (int i = 1; i < lines.length; i++) {
                    if (hasPale(lines[i])) violations++;
                }
            }
            Check.eq(0, violations, "no pale content lines in " + def.id);
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
