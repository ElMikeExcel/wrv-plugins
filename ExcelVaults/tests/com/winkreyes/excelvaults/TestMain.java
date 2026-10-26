package com.winkreyes.excelvaults;

import com.winkreyes.excelvaults.core.Pricing;
import com.winkreyes.excelvaults.core.Rules;
import com.winkreyes.excelvaults.core.Rules.Action;
import com.winkreyes.excelvaults.core.Store;
import com.winkreyes.excelvaults.core.TimeFmt;
import com.winkreyes.excelvaults.core.VaultKey;
import java.io.InputStream;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;

public final class TestMain {
    static int pass, fail;
    static void check(boolean c, String w) { if (c) pass++; else { fail++; System.out.println("FAIL " + w); } }
    static final long H = 3_600_000L, M = 60_000L;

    public static void main(String[] a) throws Exception {
        // ---- VaultKey
        VaultKey k = new VaultKey("world", 10, -20, 30);
        check(k.toString().equals("world;10;-20;30"), "key toString");
        check(k.equals(VaultKey.parse("world;10;-20;30")), "key parse round trip");
        check(VaultKey.parse("world;1;2") == null, "key parse: too few parts");
        check(VaultKey.parse("world;a;2;3") == null, "key parse: not a number");
        check(VaultKey.parse(null) == null, "key parse: null");
        check(VaultKey.parse(";1;2;3") == null, "key parse: empty world");
        check(k.pretty().equals("world 10 -20 30"), "key pretty");
        boolean threw = false; try { new VaultKey("a;b", 1, 2, 3); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "key rejects ; in world name");

        // ---- TimeFmt
        check(TimeFmt.hm(24 * H).equals("24h 0m"), "hm 24h");
        check(TimeFmt.hm(72 * H).equals("72h 0m"), "hm 72h");
        check(TimeFmt.hm(5 * H + 3 * M).equals("5h 3m"), "hm 5h 3m");
        check(TimeFmt.hm(5 * H + 2 * M + 1).equals("5h 3m"), "hm rounds minutes up");
        check(TimeFmt.hm(30_000).equals("1m"), "hm under a minute says 1m");
        check(TimeFmt.hm(59 * M).equals("59m"), "hm under an hour has no h");
        check(TimeFmt.hm(0).equals("0m") && TimeFmt.hm(-5).equals("0m"), "hm zero and negative");

        // ---- Pricing (approved defaults: normal $750 / min $50, ominous $3,000 / min $200, step $5, scaled)
        check(Pricing.price(24 * H, 24 * H, 750, 50, true, 5) == 750, "normal just locked = full $750");
        check(Pricing.price(12 * H, 24 * H, 750, 50, true, 5) == 375, "normal half left = $375");
        check(Pricing.price(6 * H, 24 * H, 750, 50, true, 5) == 190, "normal quarter left = 187.5 -> nearest $5 = $190");
        check(Pricing.price(17 * H, 24 * H, 750, 50, true, 5) == 530, "normal 17h left = 531.25 -> $530 (nearest, not up)");
        check(Pricing.price(1 * H, 24 * H, 750, 50, true, 5) == 50, "normal 1h left = 31.25 -> floored at min $50");
        check(Pricing.price(1, 24 * H, 750, 50, true, 5) == 50, "normal almost ready = min $50, never near-free");
        check(Pricing.price(0, 24 * H, 750, 50, true, 5) == 0, "nothing left = ready, no purchase");
        check(Pricing.price(72 * H, 72 * H, 3000, 200, true, 5) == 3000, "ominous just locked = $3,000");
        check(Pricing.price(36 * H, 72 * H, 3000, 200, true, 5) == 1500, "ominous half left = $1,500");
        check(Pricing.price(4 * H, 72 * H, 3000, 200, true, 5) == 200, "ominous 4h left = 166.67 -> floored at min $200");
        check(Pricing.price(10 * H, 72 * H, 3000, 200, true, 5) == 415, "ominous 10h left = 416.67 -> $415");
        check(Pricing.price(10 * H, 24 * H, 750, 50, false, 5) == 750, "flat mode = full");
        check(Pricing.price(48 * H, 24 * H, 750, 50, true, 5) == 750, "more left than cooldown (config lowered) capped at full");
        check(Pricing.price(5 * H, 24 * H, 750, 900, true, 5) == 750, "min above full clamps to full");
        check(Math.abs(Pricing.price(7 * H, 24 * H, 750, 50, true, 0) - 218.75) < 1e-6, "step 0 = no rounding");
        check(Pricing.price(5 * H, 0, 750, 50, true, 5) == 750, "zero cooldown = full");
        check(Pricing.price(13 * H, 24 * H, 750, 50, true, 10) == 410, "step $10: 406.25 -> $410");
        boolean mono = true; double prev = 1e9;
        for (long l = 24 * H; l > 0; l -= 7 * M) { double q = Pricing.price(l, 24 * H, 750, 50, true, 5); if (q > prev || q % 5 != 0 || q < 50) mono = false; prev = q; }
        check(mono, "price only falls as time passes, always a multiple of $5, never under $50");

        // ---- Rules.decide
        long now = 1_000_000_000_000L, cd = 24 * H, ocd = 72 * H;
        check(Rules.decide(false, null, false, true, false, now, cd).action() == Action.VANILLA, "not rewarded = vanilla");
        check(!Rules.decide(false, null, false, true, false, now, cd).startTimer(), "not rewarded = no timer");
        check(Rules.decide(false, now - 5 * H, false, true, true, now, cd).action() == Action.VANILLA, "not rewarded even with record = vanilla");
        Rules.Decision d = Rules.decide(true, null, false, true, false, now, cd);
        check(d.action() == Action.HINT && d.startTimer(), "locked before install: start timer + hint (no mass unlock)");
        d = Rules.decide(true, null, false, true, true, now, cd);
        check(d.action() == Action.PROMPT && d.startTimer(), "locked before install + sneak: start timer + prompt");
        check(Rules.decide(true, now - 5 * H, false, true, false, now, cd).action() == Action.HINT, "locked 5h ago: hint");
        check(Rules.decide(true, now - 5 * H, false, true, true, now, cd).action() == Action.PROMPT, "locked + sneak: prompt");
        check(Rules.decide(true, now - 24 * H, false, true, false, now, cd).action() == Action.REALLOW, "exactly 24h: reallow");
        check(Rules.decide(true, now - 24 * H + 1, false, true, false, now, cd).action() == Action.HINT, "1 ms before 24h: still locked");
        check(Rules.decide(true, now - 30 * H, false, true, true, now, cd).action() == Action.REALLOW, "ready + sneak: reallow (no charge)");
        check(Rules.decide(true, 0L, false, true, false, now, cd).action() == Action.REALLOW, "pending (paid/reset) = reallow");
        d = Rules.decide(true, null, true, false, false, now, ocd);
        check(d.action() == Action.GATED && d.startTimer(), "ominous, gate closed, no record: gated (timer recorded silently)");
        check(Rules.decide(true, now - 100 * H, true, false, true, now, ocd).action() == Action.GATED, "ominous gate closed even when time is up: gated");
        check(Rules.decide(true, 0L, true, false, false, now, ocd).action() == Action.GATED, "ominous pending but gate closed: gated");
        check(Rules.decide(true, now - 50 * H, true, true, false, now, ocd).action() == Action.HINT, "ominous gate open, 50h of 72h: hint");
        check(Rules.decide(true, now - 72 * H, true, true, false, now, ocd).action() == Action.REALLOW, "ominous gate open, 72h: reallow");
        check(Rules.decide(false, null, true, false, false, now, ocd).action() == Action.VANILLA, "ominous not used yet = vanilla");
        check(Rules.left(now - 5 * H, now, cd) == 19 * H, "left = 19h");
        check(Rules.left(0, now, cd) == 0, "left pending = 0");
        // gate
        check(!Rules.gateOpen(null, 2), "gate: unknown seals = closed");
        check(!Rules.gateOpen(1, 2), "gate: 1 seal < 2 closed");
        check(Rules.gateOpen(2, 2), "gate: 2 seals open");
        check(Rules.gateOpen(5, 2), "gate: 5 seals open");
        check(Rules.gateOpen(null, 0), "gate: min 0 always open");

        // ---- Store
        Store s = new Store();
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001"), p2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        VaultKey k2 = new VaultKey("world", 1, 2, 3);
        check(s.get(k, p1) == null && !s.dirty(), "empty store");
        s.put(k, p1, 111L, false); s.put(k, p2, 222L, false); s.put(k2, p1, 333L, true);
        check(s.dirty() && s.get(k, p1) == 111L && s.get(k2, p1) == 333L && s.isOminous(k2) && !s.isOminous(k), "put/get/ominous");
        check(s.vaultCount() == 2 && s.recordCount() == 3, "counts");
        check(s.forPlayer(p1).size() == 2 && s.forPlayer(p2).size() == 1, "forPlayer");
        check(s.remove(k, p2) && !s.remove(k, p2), "remove once");
        check(s.age(p1, 100L) == 2 && s.get(k, p1) == 11L && s.get(k2, p1) == 233L, "age moves times back");
        check(s.age(p1, 10_000L) == 2 && s.get(k, p1) == 1L, "age never reaches 0 (0 means pending)");
        check(s.resetPlayer(p1) == 2 && s.get(k, p1) == 0L && s.get(k2, p1) == 0L, "reset marks pending");
        s.addPaid(p1, "2026-10-25", true); s.addPaid(p2, "2026-10-25", true);
        check(s.clearPaid(p1) >= 1 && s.paidToday(p1, "2026-10-25", true) == 0 && s.paidToday(p2, "2026-10-25", true) == 1, "clearPaid clears only that player's paid count");
        check(s.paidToday(p1, "2026-10-25", true) == 0, "paid starts 0");
        s.addPaid(p1, "2026-10-25", true); s.addPaid(p1, "2026-10-25", true); s.addPaid(p1, "2026-10-25", false);
        check(s.paidToday(p1, "2026-10-25", true) == 2 && s.paidToday(p1, "2026-10-25", false) == 1, "paid counts per type");
        s.addPaid(p1, "2026-10-26", false);
        check(s.paidToday(p1, "2026-10-25", true) == 0 && s.paidToday(p1, "2026-10-26", false) == 1, "new day resets and drops old day");
        s.removeVault(k2);
        check(s.get(k2, p1) == null && s.vaultCount() == 1, "removeVault");

        // round trip through YAML text (the real save format)
        s.put(k2, p2, 1_761_400_000_000L, true);
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, Object> e : s.toMap().entrySet()) y.set(e.getKey(), e.getValue());
        String text = y.saveToString();
        YamlConfiguration y2 = new YamlConfiguration(); y2.loadFromString(text);
        Store s2 = new Store();
        int skipped = s2.fromMap(plain(y2.getValues(false)));
        check(skipped == 0, "round trip: nothing skipped");
        check(Long.valueOf(0L).equals(s2.get(k, p1)) && Long.valueOf(1_761_400_000_000L).equals(s2.get(k2, p2)) && s2.isOminous(k2), "round trip: values and big epoch ms survive");
        check(s2.paidToday(p1, "2026-10-26", false) == 1, "round trip: paid counts survive");
        check(!s2.dirty(), "loaded store is clean");
        // world name with a dot survives when the path separator is changed (as the plugin does)
        Store sd = new Store(); VaultKey kd = new VaultKey("my.world", 4, 5, 6); sd.put(kd, p1, 99L, false);
        YamlConfiguration yd = new YamlConfiguration(); yd.options().pathSeparator('\u0001');
        for (Map.Entry<String, Object> e : sd.toMap().entrySet()) yd.set(e.getKey(), e.getValue());
        YamlConfiguration yd2 = new YamlConfiguration(); yd2.options().pathSeparator('\u0001'); yd2.loadFromString(yd.saveToString());
        Store sd2 = new Store(); sd2.fromMap(plain(yd2.getValues(false)));
        check(Long.valueOf(99L).equals(sd2.get(kd, p1)), "world name with a dot round trips");
        // lenient load
        YamlConfiguration bad = new YamlConfiguration();
        bad.loadFromString("vaults:\n  'nope': {ominous: false, players: {x: 1}}\n  'world;1;1;1':\n    ominous: true\n    players:\n      not-a-uuid: 5\n      00000000-0000-0000-0000-000000000003: 7\n");
        Store s3 = new Store();
        check(s3.fromMap(plain(bad.getValues(false))) == 2 && s3.recordCount() == 1, "lenient load skips a bad key and a bad uuid");
        check(new Store().fromMap(null) == 0, "null map ok");

        // ---- helpers in the plugin class
        check(ExcelVaults.money(1500).equals("1,500") && ExcelVaults.money(1234.5).equals("1,234.50"), "money format");
        check(ExcelVaults.plain("&6Hi &lthere").equals("Hi there"), "plain strips codes");
        check(ExcelVaults.fill("{a}-{b}-{a}", Map.of("a", "1", "b", "2")).equals("1-2-1"), "fill placeholders");
        check(ExcelVaults.csv("=cmd").equals("'=cmd") && ExcelVaults.csv("a,b").equals("\"a,b\""), "csv guards formulas and commas");

        // ---- shipped config.yml and plugin.yml parse, and carry the approved defaults
        YamlConfiguration cfg = load("config.yml"), py = load("plugin.yml");
        check(cfg.getInt("normal.cooldown-hours") == 24 && cfg.getInt("ominous.cooldown-hours") == 72, "config: 24h / 72h");
        check(cfg.getInt("ominous.min-seals") == 2, "config: ominous waits for seal 2");
        check(cfg.getDouble("normal.reopen-now.full-price") == 750 && cfg.getDouble("normal.reopen-now.min-price") == 50 && cfg.getBoolean("normal.reopen-now.scale-with-time-left"), "config: normal $750 scaled, min $50");
        check(cfg.getDouble("ominous.reopen-now.full-price") == 3000 && cfg.getDouble("ominous.reopen-now.min-price") == 200 && cfg.getBoolean("ominous.reopen-now.scale-with-time-left"), "config: ominous $3,000 scaled, min $200");
        check(cfg.getDouble("price-step") == 5, "config: rounded to nearest $5");
        check(cfg.getInt("ominous.reopen-now.daily-limit") == 2 && cfg.getInt("normal.reopen-now.daily-limit") == 0, "config: daily limits");
        boolean noDash = true;
        for (String key : cfg.getKeys(true)) { Object v = cfg.get(key); if (v != null && v.toString().contains("\u2014")) noDash = false; }
        check(noDash, "config: no em dashes in player text");
        check("ExcelVaults".equals(py.getString("name")) && "0.1.1".equals(py.getString("version")), "plugin.yml name/version");
        check("op".equals(py.getString("permissions.excelvaults.admin.default")) && py.getBoolean("permissions.excelvaults.use.default"), "plugin.yml perms");
        check(py.getStringList("softdepend").containsAll(List.of("Vault", "floodgate", "ExcelGuildGate", "ExcelAuditLog")), "plugin.yml softdepends");

        // ---- 0.1.1 platform text (Bedrock cannot click chat buttons or right-click)
        {
            Map<String, String> none = new HashMap<>();
            java.util.function.Function<String, String> live010 = key -> Map.of(
                    "hint-pay", "&7Sneak + right-click it to reopen it now for &e${price}&7.",
                    "list-footer", "&7Tap a [Reopen] button, or sneak + right-click a locked vault.").get(key);
            String bp = com.winkreyes.excelvaults.core.Texts.pick("hint-pay", true, live010);
            check(bp.contains("Sneak + tap") && bp.contains("/vaults") && !bp.toLowerCase().contains("right-click") && !bp.toLowerCase().contains("click"), "Bedrock hint-pay: sneak + tap or /vaults, no clicking words (live 0.1.0 config, no -bedrock key)");
            check(com.winkreyes.excelvaults.core.Texts.pick("hint-pay", false, live010).contains("Sneak + right-click"), "Java hint-pay unchanged: Sneak + right-click");
            String bf = com.winkreyes.excelvaults.core.Texts.pick("list-footer", true, live010);
            check(bf.startsWith("&7Tap a vault") && !bf.contains("[Reopen]") && !bf.toLowerCase().contains("right-click"), "Bedrock list footer: tap a vault below (form buttons), no [Reopen] chat button");
            check(com.winkreyes.excelvaults.core.Texts.pick("list-footer", false, live010).equals("&7Click a [Reopen] button, or sneak + right-click a locked vault."), "Java list footer: untouched 0.1.0 default 'Tap a [Reopen]' upgrades to 'Click a [Reopen]'");
            check(com.winkreyes.excelvaults.core.Texts.pick("list-footer", false, key -> key.equals("list-footer") ? "&7Custom footer" : null).equals("&7Custom footer"), "Java list footer: a customised line is kept");
            check(com.winkreyes.excelvaults.core.Texts.pick("hint-pay", true, key -> key.equals("hint-pay-bedrock") ? "&7Owner text" : null).equals("&7Owner text"), "Bedrock override messages.hint-pay-bedrock wins");
            check(com.winkreyes.excelvaults.core.Texts.pick("hint", true, key -> key.equals("hint") ? "&6Opens in {left}" : null).equals("&6Opens in {left}"), "neutral keys are the same on both platforms");
            check(com.winkreyes.excelvaults.core.Texts.pick("nope", false, key -> null).equals("nope"), "missing key falls back to the key name (as 0.1.0)");
            List<String> bh = com.winkreyes.excelvaults.core.Texts.pickLines("help", true, key -> List.of());
            check(bh.size() == 5 && String.join(" ", bh).contains("Sneak + tap") && String.join(" ", bh).contains("LT") && !String.join(" ", bh).toLowerCase().contains("click"), "Bedrock /vaults help: tap, sneak + tap, controller LT, no click words");
            List<String> jh = com.winkreyes.excelvaults.core.Texts.pickLines("help", false, key -> key.equals("help") ? List.of("&fJava help") : List.of());
            check(jh.equals(List.of("&fJava help")), "Java help comes from config as before");
            boolean asciiB = true;
            for (String t : com.winkreyes.excelvaults.core.Texts.BEDROCK.values()) if (!t.chars().allMatch(ch -> ch < 128)) asciiB = false;
            for (String t : com.winkreyes.excelvaults.core.Texts.BEDROCK_HELP) if (!t.chars().allMatch(ch -> ch < 128)) asciiB = false;
            if (!com.winkreyes.excelvaults.core.Texts.BEDROCK_FORM_NOTE.chars().allMatch(ch -> ch < 128)) asciiB = false;
            check(asciiB, "Bedrock text is plain ASCII (no em dashes)");
            com.winkreyes.excelvaults.core.Texts.TapGate g = new com.winkreyes.excelvaults.core.Texts.TapGate();
            check(g.allow("u1|v1", 1000, 30_000), "tap window: first tap opens it");
            check(!g.allow("u1|v1", 20_000, 30_000), "tap window: same vault 19s later does not reopen it");
            check(g.allow("u1|v2", 20_000, 30_000), "tap window: a different vault opens");
            check(g.allow("u2|v1", 20_000, 30_000), "tap window: another player is not blocked");
            check(g.allow("u1|v1", 31_000, 30_000), "tap window: after 30s it opens again");
            g.forgetPlayer("u1|"); check(g.size() == 1, "tap window: quit forgets only that player");
            check(cfg.getBoolean("bedrock-tap-opens-form") && cfg.getInt("bedrock-tap-form-cooldown-seconds") == 30, "config: bedrock-tap-opens-form true, 30s");
            check(cfg.getString("messages.hint-pay-bedrock").equals(com.winkreyes.excelvaults.core.Texts.BEDROCK.get("hint-pay"))
                    && cfg.getString("messages.list-footer-bedrock").equals(com.winkreyes.excelvaults.core.Texts.BEDROCK.get("list-footer"))
                    && cfg.getString("messages.bedrock-form-note").equals(com.winkreyes.excelvaults.core.Texts.BEDROCK_FORM_NOTE)
                    && cfg.getStringList("messages.help-bedrock").equals(com.winkreyes.excelvaults.core.Texts.BEDROCK_HELP), "shipped config Bedrock lines = the built-in defaults (live config needs no edit)");
            check(cfg.getString("messages.list-footer").startsWith("&7Click a [Reopen]") && cfg.getString("messages.hint-pay").contains("Sneak + right-click"), "shipped config Java lines say click / right-click");
            check(py.getString("version").equals("0.1.1"), "plugin.yml 0.1.1");
        }
        System.out.println("ExcelVaults unit tests: " + pass + " pass, " + fail + " fail");
        if (fail > 0) System.exit(1);
    }

    static YamlConfiguration load(String n) throws Exception {
        try (InputStream in = TestMain.class.getClassLoader().getResourceAsStream(n)) {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            return y;
        }
    }
    static Map<String, Object> plain(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            out.put(e.getKey(), v instanceof org.bukkit.configuration.ConfigurationSection cs ? plain(cs.getValues(false)) : v);
        }
        return out;
    }
}
