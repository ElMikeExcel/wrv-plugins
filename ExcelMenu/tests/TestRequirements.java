import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.RequirementEngine;

import java.util.List;
import java.util.Map;

/** Requirement evaluation against a fake viewer. */
public final class TestRequirements {

    private static MenuDef.Requirement req(String type, Map<String, Object> options) {
        return new MenuDef.Requirement("t", type, options, List.of());
    }

    public static void run() {
        List<MenuDef> defs = MenuFixtures.loadShipped();

        // The real guild gate from server_menu.
        MenuDef server = MenuFixtures.byId(defs, "server_menu");
        MenuDef.ItemDef guildLocked = server.item("guild_locked");
        MenuFixtures.FakeContext ctx = new MenuFixtures.FakeContext();
        ctx.placeholders.put("%guildgate_unlocked%", "false");
        Check.ok(RequirementEngine.allPass(guildLocked.viewRequirements(), ctx),
                "string-equals: locked item passes when placeholder is 'false'");
        ctx.placeholders.put("%guildgate_unlocked%", "true");
        Check.ok(!RequirementEngine.allPass(guildLocked.viewRequirements(), ctx),
                "string-equals: locked item fails when placeholder flips to 'true'");

        // The real basics open gate.
        MenuDef basics = MenuFixtures.byId(defs, "basics_menu");
        MenuFixtures.FakeContext noPerm = new MenuFixtures.FakeContext();
        Check.ok(!RequirementEngine.allPass(basics.openRequirements, noPerm),
                "has-permission: basics stays shut without deluxemenus.admin");
        noPerm.permissions.add("deluxemenus.admin");
        Check.ok(RequirementEngine.allPass(basics.openRequirements, noPerm),
                "has-permission: basics opens with the node");

        MenuFixtures.FakeContext rich = new MenuFixtures.FakeContext();
        rich.balance = 250.0;
        Check.ok(RequirementEngine.passes(req("has-money", Map.of("amount", 200)), rich),
                "has-money: 250 covers a 200 gate");
        Check.ok(!RequirementEngine.passes(req("has-money", Map.of("amount", 300)), rich),
                "has-money: 250 fails a 300 gate");

        Check.ok(RequirementEngine.passes(
                req("string-contains", Map.of("input", "hello world", "output", "world")),
                rich), "string-contains matches");
        Check.ok(RequirementEngine.passes(
                req("string-equals-ignorecase", Map.of("input", "ON", "output", "on")),
                rich), "string-equals-ignorecase matches");
        Check.ok(RequirementEngine.passes(
                req("regex-matches", Map.of("input", "abc123", "regex", "\\d+")),
                rich), "regex-matches matches digits");
        Check.ok(RequirementEngine.passes(
                req("gt", Map.of("input", "7", "output", "5")), rich), "gt: 7 > 5");
        Check.ok(!RequirementEngine.passes(
                req("gt", Map.of("input", "5", "output", "5")), rich), "gt: 5 is not > 5");
        Check.ok(RequirementEngine.passes(
                req("eq", Map.of("input", "5", "output", "5")), rich), "eq: 5 == 5");
        Check.ok(RequirementEngine.passes(
                req("lte", Map.of("input", "5", "output", "5")), rich), "lte: 5 <= 5");
        Check.ok(!RequirementEngine.passes(
                req("gt", Map.of("input", "not-a-number", "output", "5")), rich),
                "unparseable number counts as not met, never throws");
        Check.ok(RequirementEngine.allPass(List.of(), rich), "no requirements always passes");
        Check.ok(RequirementEngine.allPass(null, rich), "null requirements always passes");
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
