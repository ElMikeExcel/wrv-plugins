import com.winkreyes.excelwitherskulls.core.SkullSettings;

/**
 * Config snapshot behavior: defaults, overrides, and the
 * clamp-never-throw rule (a typo degrades, never wedges — the
 * reload-safety requirement).
 */
public final class SkullSettingsTest {

    public static void run() {
        System.out.println("SkullSettingsTest");

        SkullSettings def = SkullSettings.defaults();
        Check.ok(def.enabled(), "defaults: enabled");
        Check.close(25.0, def.targetChancePercent(), "defaults: target 25.0");
        Check.ok(!def.debugLog(), "defaults: debug off");

        SkullSettings absent = SkullSettings.of(null, null, null);
        Check.eq(def, absent, "all keys absent = defaults");

        SkullSettings over = SkullSettings.of(false, 40.0, true);
        Check.ok(!over.enabled(), "override: disabled honored");
        Check.close(40.0, over.targetChancePercent(), "override: target 40 honored");
        Check.ok(over.debugLog(), "override: debug on honored");

        Check.close(100.0, SkullSettings.of(null, 250.0, null).targetChancePercent(),
                "target 250 clamps to 100");
        Check.close(0.0, SkullSettings.of(null, -5.0, null).targetChancePercent(),
                "target -5 clamps to 0");
        Check.close(25.0, SkullSettings.of(null, Double.NaN, null).targetChancePercent(),
                "NaN target falls back to default 25");
        Check.close(25.0, SkullSettings.of(null, Double.POSITIVE_INFINITY, null).targetChancePercent(),
                "infinite target falls back to default 25");
        Check.close(0.5, SkullSettings.of(null, 0.5, null).targetChancePercent(),
                "small fractional target kept");
    }
}
