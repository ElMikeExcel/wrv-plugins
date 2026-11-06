import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.ColorMath;
import com.winkreyes.excelcosmetics.core.Kind;

import java.util.LinkedHashMap;
import java.util.Map;

/** The shipped catalog fixture: the SAME palette/prices the
 *  production config.yml carries, rebuilt here so the test suite
 *  guards the real product (palette readability included). If
 *  config.yml changes, this fixture must change with it — the
 *  rig boot cross-checks the live config against these ids. */
public final class TestCatalog {

    public static Catalog build() {
        Map<String, Catalog.ColorItem> colors = new LinkedHashMap<>();
        colors.put("pink", new Catalog.ColorItem("pink", "Pink", 0xFF55FF));
        colors.put("amethyst", new Catalog.ColorItem("amethyst", "Amethyst", 0x9A5CC6));
        colors.put("grape", new Catalog.ColorItem("grape", "Grape", 0xAA00AA));
        colors.put("blue", new Catalog.ColorItem("blue", "Blue", 0x5555FF));
        colors.put("aqua", new Catalog.ColorItem("aqua", "Aqua", 0x55FFFF));
        colors.put("diamond", new Catalog.ColorItem("diamond", "Diamond", 0x2CBAA8));
        colors.put("teal", new Catalog.ColorItem("teal", "Teal", 0x00AAAA));
        colors.put("lime", new Catalog.ColorItem("lime", "Lime", 0x55FF55));
        colors.put("emerald", new Catalog.ColorItem("emerald", "Emerald", 0x11A036));
        colors.put("forest", new Catalog.ColorItem("forest", "Forest Green", 0x00AA00));
        colors.put("yellow", new Catalog.ColorItem("yellow", "Yellow", 0xFFFF55));
        colors.put("minecoin", new Catalog.ColorItem("minecoin", "Minecoin Gold", 0xDDD605));
        colors.put("gold", new Catalog.ColorItem("gold", "Gold", 0xDEB12D));
        colors.put("orange", new Catalog.ColorItem("orange", "Orange", 0xFFAA00));
        colors.put("resin", new Catalog.ColorItem("resin", "Resin", 0xEB7214));
        colors.put("copper", new Catalog.ColorItem("copper", "Copper", 0xB4684D));
        colors.put("red", new Catalog.ColorItem("red", "Red", 0xFF5555));
        colors.put("quartz", new Catalog.ColorItem("quartz", "Quartz", 0xE3D4D1));
        colors.put("iron", new Catalog.ColorItem("iron", "Iron", 0xCECACA));
        colors.put("gray", new Catalog.ColorItem("gray", "Gray", 0xAAAAAA));
        colors.put("frost", new Catalog.ColorItem("frost", "Frost White", 0xFFFFFF));
        // Retired 0.1.1 ids: not sold, still worn by owners
        colors.put("bubblegum", new Catalog.ColorItem("bubblegum", "Bubblegum Pink", 0xFF55FF, false));
        colors.put("magenta", new Catalog.ColorItem("magenta", "Magenta", 0xFF55FF, false));
        colors.put("lavender", new Catalog.ColorItem("lavender", "Lavender", 0x9A5CC6, false));
        colors.put("sky", new Catalog.ColorItem("sky", "Sky Blue", 0x55FFFF, false));
        colors.put("mint", new Catalog.ColorItem("mint", "Mint", 0x55FF55, false));
        colors.put("peach", new Catalog.ColorItem("peach", "Peach", 0xB4684D, false));
        colors.put("coral", new Catalog.ColorItem("coral", "Coral", 0xFF5555, false));

        Map<String, Catalog.EffectItem> effects = new LinkedHashMap<>();
        effects.put("rainbow-name", new Catalog.EffectItem("rainbow-name", "Rainbow Name",
                Catalog.EffectKind.RAINBOW, -1, -1));
        effects.put("gradient-name", new Catalog.EffectItem("gradient-name", "Gradient Name",
                Catalog.EffectKind.GRADIENT, 0xFF85C0, 0x55C8FF));
        effects.put("bold-name", new Catalog.EffectItem("bold-name", "Bold Name",
                Catalog.EffectKind.BOLD, -1, -1));
        effects.put("italic-name", new Catalog.EffectItem("italic-name", "Italic Name",
                Catalog.EffectKind.ITALIC, -1, -1));
        effects.put("sparkle-accents", new Catalog.EffectItem("sparkle-accents", "Sparkle Accents",
                Catalog.EffectKind.SPARKLE, -1, -1));
        effects.put("ember-gradient", new Catalog.EffectItem("ember-gradient", "Ember Gradient",
                Catalog.EffectKind.GRADIENT, 0xFF3C3C, 0xFFC94D));
        effects.put("abyss-gradient", new Catalog.EffectItem("abyss-gradient", "Abyss Gradient",
                Catalog.EffectKind.GRADIENT, 0x9B5CFF, 0x3FE8D8));

        Map<String, Catalog.SetItem> sets = new LinkedHashMap<>();
        sets.put("tank", new Catalog.SetItem("tank", "Tank Set", 0xCECACA, "bold-name",
                "Iron-bright chat with a bold name — the front line."));
        sets.put("healer", new Catalog.SetItem("healer", "Healer Set", 0xFF5555, "sparkle-accents",
                "Rose-red chat with sparkle accents — the heart of the party."));
        sets.put("warlock", new Catalog.SetItem("warlock", "Warlock Set", 0x9A5CC6, "abyss-gradient",
                "Amethyst chat with an abyss-gradient name — dark pacts, bright letters."));
        sets.put("firebender", new Catalog.SetItem("firebender", "Firebender Set", 0xEB7214, "ember-gradient",
                "Flame-orange chat with an ember-gradient name."));
        sets.put("storm", new Catalog.SetItem("storm", "Lightning/Storm Set", 0x55FFFF, "italic-name",
                "Electric-cyan chat with a slanted name — fast as weather."));

        Catalog.Prices prices = new Catalog.Prices(200, 600, 400, 300, 3.5,
                15000, 30000, 25000, 20000, 7);

        Map<String, String> basic = new LinkedHashMap<>();
        basic.put(":heart:", "♥");
        basic.put("<3", "♥");
        basic.put(":star:", "★");
        basic.put(":note:", "♪");
        basic.put(":sparkle:", "✦");
        basic.put(":dot:", "●");
        basic.put(":diamond:", "◆");

        return Catalog.build(colors, effects, sets, prices, basic, Map.of(), false);
    }

    /** The fixture must match the shipped config.yml (hex + for-sale). */
    static void crossCheckConfig(Catalog c) {
        java.nio.file.Path cfg = java.nio.file.Path.of("config.yml");
        if (!java.nio.file.Files.exists(cfg)) { System.out.println("  (config.yml not found; cross-check skipped)"); return; }
        try {
            java.util.regex.Pattern line = java.util.regex.Pattern.compile(
                    "^  ([a-z0-9-]+):\\s*\\{ name: \"([^\"]+)\",\\s*hex: \"#([0-9A-Fa-f]{6})\"(, for-sale: false)? \\}");
            boolean inColors = false; int seen = 0;
            for (String l : java.nio.file.Files.readAllLines(cfg)) {
                if (l.startsWith("colors:")) { inColors = true; continue; }
                if (inColors && !l.isEmpty() && !l.startsWith(" ")) break;
                if (!inColors) continue;
                java.util.regex.Matcher m = line.matcher(l);
                if (!m.find()) continue;
                seen++;
                Catalog.ColorItem item = c.colors().get(m.group(1));
                Check.ok(item != null && item.rgb() == Integer.parseInt(m.group(3), 16)
                                && item.name().equals(m.group(2)) && item.forSale() == (m.group(4) == null),
                        "config.yml color " + m.group(1) + " matches the fixture");
            }
            Check.eq(c.colors().size(), seen, "config.yml lists every fixture color");
        } catch (java.io.IOException e) {
            Check.ok(false, "config.yml readable: " + e);
        }
    }

    public static void run() {
        System.out.println("[catalog]");
        Catalog c = build();
        Check.eq(28, c.colors().size(), "catalog carries 21 shop colors + 7 retired ids");
        long onSale = c.colors().values().stream().filter(Catalog.ColorItem::forSale).count();
        Check.eq(21L, onSale, "21 colors on sale");
        Check.ok(c.colors().containsKey("pink"), "pink is in the first color list (priority)");
        Check.ok(c.forSale(Kind.COLOR, "pink"), "pink is on sale");
        Check.ok(c.exists(Kind.COLOR, "bubblegum") && !c.forSale(Kind.COLOR, "bubblegum"),
                "bubblegum kept for owners but retired from sale");
        Check.eq(0xFF55FF, c.colors().get("bubblegum").rgb(), "bubblegum wears the same pink as Pink (Mike's call)");
        for (Catalog.ColorItem item : c.colors().values()) {
            Check.ok(ColorMath.luminance(item.rgb()) >= ColorMath.MIN_PALETTE_LUMINANCE,
                    "palette color " + item.id() + " passes the readability floor");
        }
        // 0.1.2: Java/Bedrock accuracy. Every color and set hex is the EXACT
        // RGB of a Bedrock code, and no two on-sale colors share a code.
        java.util.Set<Character> used = new java.util.HashSet<>();
        for (Catalog.ColorItem item : c.colors().values()) {
            Character code = ColorMath.exactBedrockCode(item.rgb());
            Check.ok(code != null, "color " + item.id() + " is an exact Bedrock code");
            if (item.forSale()) Check.ok(code != null && used.add(code), "on-sale color " + item.id() + " has its own Bedrock code");
        }
        for (Catalog.SetItem set : c.sets().values()) {
            Check.ok(ColorMath.exactBedrockCode(set.rgb()) != null, "set " + set.id() + " is an exact Bedrock code");
        }
        for (char dark : new char[]{'0', '1', '4', '8', 'j', 'm', 't'}) {
            Check.ok(ColorMath.luminance(ColorMath.BEDROCK_CODES.get(dark)) < ColorMath.MIN_PALETTE_LUMINANCE,
                    "dropped code &" + dark + " is below the floor");
        }
        crossCheckConfig(c);
        Check.eq(5, c.sets().size(), "five archetype sets ship");
        for (String id : new String[]{"tank", "healer", "warlock", "firebender", "storm"}) {
            Check.ok(c.sets().containsKey(id), "set ships: " + id);
        }
        Check.close(200, c.prices().price(Kind.COLOR, 1, false), "color rent = 200/week");
        Check.close(400, c.prices().price(Kind.COLOR, 2, false), "color rent 2 weeks = 400");
        Check.close(600, c.prices().price(Kind.SET, 1, false), "set rent = 600/week");
        Check.close(400, c.prices().price(Kind.EFFECT, 1, false), "effect rent = 400/week");
        Check.close(300, c.prices().price(Kind.EMOTES, 1, false), "emote pack rent = 300/week");
        // The full per-kind price table: 1 week / 4 weeks at 3.5x / flat own
        Check.close(700, c.prices().price(Kind.COLOR, 4, false), "color 4 weeks = 3.5x = 700");
        Check.close(15000, c.prices().price(Kind.COLOR, 1, true), "own a color = flat 15000");
        Check.close(1050, c.prices().price(Kind.EMOTES, 4, false), "emote pack 4 weeks = 3.5x = 1050");
        Check.close(20000, c.prices().price(Kind.EMOTES, 1, true), "own the emote pack = flat 20000");
        Check.close(1400, c.prices().price(Kind.EFFECT, 4, false), "effect 4 weeks = 3.5x = 1400");
        Check.close(25000, c.prices().price(Kind.EFFECT, 1, true), "own an effect = flat 25000");
        Check.close(2100, c.prices().price(Kind.SET, 4, false), "set 4 weeks = 3.5x = 2100");
        Check.close(30000, c.prices().price(Kind.SET, 1, true), "own a set = flat 30000");
        Check.close(1000, c.prices().price(Kind.COLOR, 5, false), "5 weeks stays linear = 1000 (tier is exactly 4)");
        Check.close(15000, c.prices().ownPrice(Kind.COLOR), "ownPrice color = 15000");
        Check.eq(7L * 24 * 3600 * 1000, c.prices().rentalMillis(1), "one week = 7 days in millis");
        Check.eq(28L * 24 * 3600 * 1000, c.prices().rentalMillis(4), "four weeks = 28 days in millis");
        Check.ok(c.exists(Kind.EMOTES, "command-pack"), "emote pack sku exists");
        Check.ok(!c.exists(Kind.COLOR, "ultraviolet"), "unknown color does not exist");
        Check.ok(!c.premiumTokensEnabled(), "premium glyph tokens ship disabled pending two-client verification");

        boolean threw = false;
        try {
            Map<String, Catalog.SetItem> bad = new LinkedHashMap<>();
            bad.put("broken", new Catalog.SetItem("broken", "Broken", 0xFF0000, "no-such-effect", ""));
            Catalog.build(c.colors(), c.effects(), bad, c.prices(), Map.of(), Map.of(), false);
        } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "set referencing an unknown effect is rejected at build");

        threw = false;
        try {
            Catalog.build(c.colors(), c.effects(), c.sets(),
                    new Catalog.Prices(0, 600, 400, 300, 3.5,
                            15000, 30000, 25000, 20000, 7), Map.of(), Map.of(), false);
        } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "zero price is rejected at build");

        threw = false;
        try {
            Catalog.build(c.colors(), c.effects(), c.sets(),
                    new Catalog.Prices(200, 600, 400, 300, 3.5,
                            0, 30000, 25000, 20000, 7), Map.of(), Map.of(), false);
        } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "zero own price is rejected at build");

        threw = false;
        try {
            Catalog.build(c.colors(), c.effects(), c.sets(),
                    new Catalog.Prices(200, 600, 400, 300, 0,
                            15000, 30000, 25000, 20000, 7), Map.of(), Map.of(), false);
        } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "zero four-week multiplier is rejected at build");
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
