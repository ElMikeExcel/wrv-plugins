import com.winkreyes.exceltrainingwheels.core.DoorHoldRule;

public class DoorHoldRuleTest {
    public static void run() {
        System.out.println("DoorHoldRuleTest");
        Check.ok(DoorHoldRule.shouldCancel(true, true), "mob hunting a protected player: break cancelled");
        Check.ok(!DoorHoldRule.shouldCancel(true, false), "mob hunting an unprotected player: break proceeds (veteran drama)");
        Check.ok(!DoorHoldRule.shouldCancel(false, false), "mob hunting a villager / no player target: break proceeds");
        Check.ok(!DoorHoldRule.shouldCancel(false, true), "non-player target never cancels even if flagged protected");
    }
}
