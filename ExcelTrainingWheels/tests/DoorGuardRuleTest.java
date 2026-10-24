import com.winkreyes.exceltrainingwheels.core.DoorGuardRule;

public class DoorGuardRuleTest {
    public static void run() {
        System.out.println("DoorGuardRuleTest");
        Check.ok(DoorGuardRule.shouldCancel(true, true), "mob hunting a protected player: break cancelled");
        Check.ok(!DoorGuardRule.shouldCancel(true, false), "mob hunting an unprotected player: break proceeds (veteran drama)");
        Check.ok(!DoorGuardRule.shouldCancel(false, false), "mob hunting a villager / no player target: break proceeds");
        Check.ok(!DoorGuardRule.shouldCancel(false, true), "non-player target never cancels even if flagged protected");
    }
}
