import com.winkreyes.excelmenu.core.Text;

/** Color translation: what players see must not shift in migration. */
public final class TestText {

    public static void run() {
        Check.eq("§aHi", Text.colorize("&aHi"), "ampersand code becomes section code");
        Check.eq("§eBalance $%vault_eco_balance_2dp%",
                Text.colorize("&eBalance $%vault_eco_balance_2dp%"),
                "shop title survives colorize (placeholder untouched)");
        Check.eq("Sets & Effects", Text.colorize("Sets & Effects"),
                "a bare & in text stays literal");
        Check.eq("§x§f§f§8§5§c§0", Text.colorize("&#FF85C0"), "hex color expands to §x form");
        Check.eq("§d§lPink", Text.colorize("&d&lPink"), "combined codes translate");
        Check.eq("Pink", Text.strip("&d&lPink"), "strip removes codes");
        Check.eq("X", Text.strip("&#FF85C0X"), "strip removes hex");
        Check.eq("", Text.colorize(null), "null colorizes to empty");
        Check.eq("", Text.strip(null), "null strips to empty");
        // 0.1.5 Bedrock downsample: same pick as Geyser chat
        Check.eq("§c", Text.colorizeBedrock("&#FF85C0"), "old Pink hex lands on Bedrock red (the bug)");
        Check.eq("§d§lPink", Text.colorizeBedrock("&#FC87C6&lPink"), "retuned Pink lands on Bedrock pink");
        Check.eq("§u", Text.colorizeBedrock("&#B48AFC"), "retuned Lavender lands on amethyst");
        Check.eq("§d§lX", Text.colorizeBedrock("&d&lX"), "legacy codes pass through untouched");
        Check.eq("§9", Text.colorizeBedrock("&#B18CFF"), "old Lavender lands on blue");
        // 0.1.5 chat colors v2: every shop hex is an exact Bedrock code
        Check.eq("§d", Text.colorizeBedrock("&#FF55FF"), "Pink #FF55FF lands on &d");
        Check.eq("§u", Text.colorizeBedrock("&#9A5CC6"), "Amethyst #9A5CC6 lands on &u");
        Check.eq("§5", Text.colorizeBedrock("&#AA00AA"), "Grape #AA00AA lands on &5");
        Check.eq("§9", Text.colorizeBedrock("&#5555FF"), "Blue #5555FF lands on &9");
        Check.eq("§b", Text.colorizeBedrock("&#55FFFF"), "Aqua #55FFFF lands on &b");
        Check.eq("§s", Text.colorizeBedrock("&#2CBAA8"), "Diamond #2CBAA8 lands on &s");
        Check.eq("§3", Text.colorizeBedrock("&#00AAAA"), "Teal #00AAAA lands on &3");
        Check.eq("§a", Text.colorizeBedrock("&#55FF55"), "Lime #55FF55 lands on &a");
        Check.eq("§q", Text.colorizeBedrock("&#11A036"), "Emerald #11A036 lands on &q");
        Check.eq("§2", Text.colorizeBedrock("&#00AA00"), "Forest Green #00AA00 lands on &2");
        Check.eq("§e", Text.colorizeBedrock("&#FFFF55"), "Yellow #FFFF55 lands on &e");
        Check.eq("§g", Text.colorizeBedrock("&#DDD605"), "Minecoin Gold #DDD605 lands on &g");
        Check.eq("§p", Text.colorizeBedrock("&#DEB12D"), "Gold #DEB12D lands on &p");
        Check.eq("§6", Text.colorizeBedrock("&#FFAA00"), "Orange #FFAA00 lands on &6");
        Check.eq("§v", Text.colorizeBedrock("&#EB7214"), "Resin #EB7214 lands on &v");
        Check.eq("§n", Text.colorizeBedrock("&#B4684D"), "Copper #B4684D lands on &n");
        Check.eq("§c", Text.colorizeBedrock("&#FF5555"), "Red #FF5555 lands on &c");
        Check.eq("§h", Text.colorizeBedrock("&#E3D4D1"), "Quartz #E3D4D1 lands on &h");
        Check.eq("§i", Text.colorizeBedrock("&#CECACA"), "Iron #CECACA lands on &i");
        Check.eq("§7", Text.colorizeBedrock("&#AAAAAA"), "Gray #AAAAAA lands on &7");
        Check.eq("§f", Text.colorizeBedrock("&#FFFFFF"), "Frost White #FFFFFF lands on &f");
        // 0.1.5 {sample} lore lines: Java shows them as authored, Bedrock keeps the real color
        Check.eq("§x§f§f§5§5§f§fAlex: hello!", Text.colorize("{sample}&#FF55FFAlex: hello!"), "sample marker removed for Java");
        Check.eq("Alex: hello!", Text.strip("{sample}&#FF55FFAlex: hello!"), "sample marker removed by strip");
        Check.eq("§l§fName\n§fAlex: hi\n§0Own it", Text.bedrockText("&l&#FFFFFFName",
                java.util.List.of("{sample}&#FFFFFFAlex: hi", "&fOwn it")),
                "sample line keeps white on Bedrock while normal pale lore still darkens");
        Check.eq("§0Alex: hi", Text.bedrockText("", java.util.List.of("&#FFFF55Alex: hi")).trim(),
                "without the marker a pale hex line still darkens (unchanged behavior)");
        Check.eq("{x}", Text.colorize("{x}"), "other braces untouched");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
