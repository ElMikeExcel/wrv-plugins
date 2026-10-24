import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.ChatFormatter;
import com.winkreyes.excelcosmetics.core.ColorMath;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;

/** The painted product: exact segment-level expectations. */
public final class TestFormat {

    private static final long T0 = 1_700_000_000_000L;
    private static final long WEEK = 7L * 24 * 3600 * 1000;

    public static void run() {
        Catalog catalog = TestCatalog.build();
        System.out.println("[format]");

        // Plain player: untouched
        PlayerState plain = new PlayerState();
        ChatFormatter.Plan p0 = ChatFormatter.format(catalog, plain.appearance(catalog, T0), "Steve", "hello", false);
        Check.eq(1, p0.name().size(), "plain name is one segment");
        Check.eq(null, p0.name().get(0).rgb(), "plain name has no color");
        Check.eq("hello", p0.message().get(0).text(), "plain message passes through");

        // Color only: name + message share the color
        PlayerState colored = new PlayerState();
        colored.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        ChatFormatter.Plan p1 = ChatFormatter.format(catalog, colored.appearance(catalog, T0), "Alex", "hi all", false);
        Check.eq(0xFF55FF, p1.name().get(0).rgb(), "pink paints the name");
        Check.eq(0xFF55FF, p1.message().get(0).rgb(), "pink paints the message");

        // Tokens: free basic set for everyone, even plain players
        ChatFormatter.Plan p2 = ChatFormatter.format(catalog, PlayerState.Appearance.PLAIN, "Steve", "love you <3 :star:", false);
        Check.eq("love you ♥ ★", p2.message().get(0).text(), "basic tokens become glyphs for everyone");
        ChatFormatter.Plan p3 = ChatFormatter.format(catalog, PlayerState.Appearance.PLAIN, "Steve", ":unknown: stays", false);
        Check.eq(":unknown: stays", p3.message().get(0).text(), "unknown tokens pass through untouched");

        // Gradient name: per-character, endpoints exact, message solid
        PlayerState grad = new PlayerState();
        grad.applyGrant(Kind.COLOR, "sky", T0, WEEK, false);
        grad.applyGrant(Kind.EFFECT, "gradient-name", T0, WEEK, false);
        ChatFormatter.Plan p4 = ChatFormatter.format(catalog, grad.appearance(catalog, T0), "Ab", "msg", false);
        Check.eq(2, p4.name().size(), "gradient splits the name per character");
        Check.eq(0xFF85C0, p4.name().get(0).rgb(), "gradient starts at the from color");
        Check.eq(0x55C8FF, p4.name().get(1).rgb(), "gradient ends at the to color");
        Check.eq(0x55FFFF, p4.message().get(0).rgb(), "message keeps the solid equipped color under a name effect");

        // Single-character gradient name does not blow up
        ChatFormatter.Plan p5 = ChatFormatter.format(catalog, grad.appearance(catalog, T0), "A", "msg", false);
        Check.eq(1, p5.name().size(), "one-char name renders one segment");
        Check.eq(0xFF85C0, p5.name().get(0).rgb(), "one-char gradient = the from color");

        // Rainbow: every character colored, colors differ across the name
        PlayerState rain = new PlayerState();
        rain.applyGrant(Kind.EFFECT, "rainbow-name", T0, WEEK, false);
        ChatFormatter.Plan p6 = ChatFormatter.format(catalog, rain.appearance(catalog, T0), "Rainbow", "msg", false);
        Check.eq(7, p6.name().size(), "rainbow splits per character");
        Check.ok(!p6.name().get(0).rgb().equals(p6.name().get(3).rgb()), "rainbow colors vary across the name");
        Check.eq(null, p6.message().get(0).rgb(), "rainbow never touches the message body");

        // Bold / italic ride on the name only
        PlayerState bold = new PlayerState();
        bold.applyGrant(Kind.EFFECT, "bold-name", T0, WEEK, false);
        ChatFormatter.Plan p7 = ChatFormatter.format(catalog, bold.appearance(catalog, T0), "Tank", "msg", false);
        Check.ok(p7.name().get(0).bold(), "bold-name bolds the name");
        Check.ok(!p7.message().get(0).bold(), "bold-name leaves the message unbolded");

        // Sparkle: obfuscated flanks around the name
        PlayerState spark = new PlayerState();
        spark.applyGrant(Kind.EFFECT, "sparkle-accents", T0, WEEK, false);
        ChatFormatter.Plan p8 = ChatFormatter.format(catalog, spark.appearance(catalog, T0), "Rylie", "msg", false);
        Check.eq(3, p8.name().size(), "sparkle = flank + name + flank");
        Check.ok(p8.name().get(0).obfuscated() && p8.name().get(2).obfuscated(), "both flanks obfuscated");
        Check.eq("Rylie", p8.name().get(1).text(), "name itself stays readable between the sparkles");
        Check.eq(ChatFormatter.SPARKLE_DEFAULT_RGB, p8.name().get(0).rgb(), "flanks use the default gold without a color");

        // ColorMath guards
        Check.eq(0xFF85C0, ColorMath.parseHex("#FF85C0"), "hex parse with #");
        Check.eq(0xFF85C0, ColorMath.parseHex("ff85c0"), "hex parse lowercase bare");
        Check.eq(-1, ColorMath.parseHex("#12345"), "short hex rejected");
        Check.eq(-1, ColorMath.parseHex("nothex"), "non-hex rejected");
        int[] grad3 = ColorMath.gradient(0x000000, 0xFFFFFF, 3);
        Check.eq(0x000000, grad3[0], "gradient[0] = from");
        Check.eq(0x808080, grad3[1], "gradient midpoint rounds to 0x80");
        Check.eq(0xFFFFFF, grad3[2], "gradient[last] = to");
        Check.eq(1, ColorMath.gradient(0x112233, 0xFFFFFF, 1).length, "1-step gradient is just the from color");
        Check.ok(ColorMath.luminance(0x000000) < 0.01, "black luminance ~0");
        Check.ok(ColorMath.luminance(0xFFFFFF) > 0.99, "white luminance ~1");
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
