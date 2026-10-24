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
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
