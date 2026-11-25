import excelemfixes.shared.TextUtil;

/** Bedrock text rendering: legacy codes strip cleanly to plain text. */
public final class TextUtilTest {

    public static void run() {
        System.out.println("TextUtilTest");
        Check.eq("Sewage Keys", TextUtil.plain("&2Sewage Keys"),
                "color code stripped");
        Check.eq("[lvl 011] Primis - The Sewers",
                TextUtil.plain("&2[lvl 011] &aPrimis - The Sewers"),
                "multi-code name stripped");
        Check.eq("Gray -> White -> Mythic",
                TextUtil.plain("&8Gray &7-> &fWhite &7-> &bMythic"),
                "ladder line stripped");
        Check.eq("", TextUtil.plain(null), "null is empty");
        Check.eq("Plain", TextUtil.plain("Plain"), "plain text untouched");

        // Nameplate cleaning (ExcelProtect matching): color codes
        // and the leading level bracket come off, the name remains.
        Check.eq("The Shredder",
                TextUtil.cleanNameplate("§4『§c11§4』§f §5The Shredder"),
                "EM nameplate cleans to the boss name");
        Check.eq("Noxious Ninja",
                TextUtil.cleanNameplate("§2[§a10§2]§f §2Noxious Ninja"),
                "bracket nameplate cleans to the boss name");
        Check.eq("The Shredder",
                TextUtil.cleanNameplate("&5The Shredder"),
                "config name cleans to the boss name");
        Check.eq("Spore Spreader",
                TextUtil.cleanNameplate("§2[§a9§2]§f §4Spore Spreader"),
                "second nameplate form cleans");
        Check.eq("", TextUtil.cleanNameplate(null),
                "null nameplate is empty");
    }
}
