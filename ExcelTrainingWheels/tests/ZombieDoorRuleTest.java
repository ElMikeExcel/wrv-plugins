import com.winkreyes.exceltrainingwheels.core.ZombieDoorRule;
import java.util.List;
import java.util.Set;

public class ZombieDoorRuleTest {
    public static void run() {
        System.out.println("ZombieDoorRuleTest");
        Set<String> d = ZombieDoorRule.parseTypes(null);
        Check.eq(Set.of("ZOMBIE", "HUSK", "ZOMBIE_VILLAGER"), d, "missing list -> default zombie/husk/zombie villager");
        Check.eq(d, ZombieDoorRule.parseTypes(List.of()), "empty list -> defaults");
        Check.ok(!d.contains("DROWNED"), "drowned never in defaults");
        Check.ok(!ZombieDoorRule.parseTypes(List.of("drowned", "zombie")).contains("DROWNED"), "drowned refused even if configured");
        Check.ok(ZombieDoorRule.parseTypes(List.of(" zombie ")).contains("ZOMBIE"), "type names trimmed + upper-cased");
        Check.ok(ZombieDoorRule.parseTypes(List.of("zombified_piglin")).contains("ZOMBIFIED_PIGLIN"), "opt-in extra type honored");
        Check.ok(ZombieDoorRule.shouldGrant(true, d, "ZOMBIE", false, false), "plain zombie without the roll: granted");
        Check.ok(ZombieDoorRule.shouldGrant(true, d, "HUSK", false, false), "husk: granted");
        Check.ok(ZombieDoorRule.shouldGrant(true, d, "ZOMBIE_VILLAGER", false, false), "zombie villager: granted");
        Check.ok(!ZombieDoorRule.shouldGrant(true, d, "DROWNED", false, false), "drowned: never");
        Check.ok(!ZombieDoorRule.shouldGrant(true, d, "ZOMBIFIED_PIGLIN", false, false), "zombified piglin: not by default");
        Check.ok(!ZombieDoorRule.shouldGrant(true, d, "ZOMBIE", true, false), "EliteMobs elite: left alone");
        Check.ok(!ZombieDoorRule.shouldGrant(true, d, "ZOMBIE", false, true), "already can: no-op");
        Check.ok(!ZombieDoorRule.shouldGrant(false, d, "ZOMBIE", false, false), "feature off: vanilla roll stands");
        Check.ok(!ZombieDoorRule.shouldGrant(true, d, null, false, false), "null type: no");
        Check.ok(!ZombieDoorRule.shouldGrant(true, null, "ZOMBIE", false, false), "null type set: no");
    }
}
