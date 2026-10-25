import com.winkreyes.exceltrainingwheels.core.CombatDivisor;
import com.winkreyes.exceltrainingwheels.core.CombatDivisor.Path;

/** 0.1.6: the shield divides by the multiplier EliteMobs actually used for the hit. */
public class CombatDivisorTest {
    static void run() {
        System.out.println("CombatDivisor:");
        // Overworld natural elite at S1 live 1.75, table row 1.75.
        Check.close(1.75, CombatDivisor.divisor(Path.SCALED, true, 1.75, 1.0, 1.0, 1.75), "scaled: live 1.75");
        // Live ladder wins over a stale table (the Oct 25 case: table 1.75, live 1.60).
        Check.close(1.60, CombatDivisor.divisor(Path.SCALED, true, 1.60, 1.0, 1.0, 1.75), "scaled: live 1.60 beats stale table 1.75");
        Check.close(0.25, CombatDivisor.factor(0.25, 1.60) * 1.60, "scaled: protected takes exactly 0.25 of unscaled at live 1.60");
        // Dungeon / instanced boss: normalized multiplier, read live (S5 file: 2.0).
        Check.close(2.0, CombatDivisor.divisor(Path.NORMALIZED, true, 2.2, 2.0, 1.0, 2.2), "normalized: live 2.0, not the ladder 2.2");
        Check.close(0.25, CombatDivisor.factor(0.25, 2.0) * 2.0, "normalized: 0.25 of unscaled at 2.0");
        Check.close(1.0, CombatDivisor.divisor(Path.NORMALIZED, true, 1.75, 1.0, 1.0, 1.75), "normalized at live 1.0 (today): flat base, no over-protection");
        Check.close(0.25, CombatDivisor.factor(0.25, CombatDivisor.divisor(Path.NORMALIZED, true, 1.75, 1.0, 1.0, 1.75)), "normalized today: factor 0.25 (0.1.5 gave 0.1429)");
        // Other custom bosses: damageToPlayerMultiplier.
        Check.close(1.0, CombatDivisor.divisor(Path.OTHER, true, 2.2, 2.0, 1.0, 2.2), "other custom boss: live 1.0");
        // Fallbacks = the 0.1.5 table divisor (never weaker).
        Check.close(2.2, CombatDivisor.divisor(Path.UNKNOWN, true, 2.2, 2.0, 1.0, 2.2), "unknown path -> table");
        Check.close(2.2, CombatDivisor.divisor(Path.NORMALIZED, true, 2.2, null, 1.0, 2.2), "unreadable live -> table");
        Check.close(2.2, CombatDivisor.divisor(Path.NORMALIZED, true, 2.2, Double.NaN, 1.0, 2.2), "NaN live -> table");
        Check.close(2.2, CombatDivisor.divisor(Path.SCALED, false, 1.6, 2.0, 1.0, 2.2), "source=table -> table");
        Check.close(2.2, CombatDivisor.divisor(null, true, 1.6, 2.0, 1.0, 2.2), "null path -> table");
        // Never below 1.0.
        Check.close(1.0, CombatDivisor.divisor(Path.OTHER, true, 1.0, 1.0, 0.5, 1.6), "live 0.5 -> divisor floor 1.0");
        Check.close(1.0, CombatDivisor.divisor(Path.SCALED, false, null, null, null, 0.2), "table below 1.0 -> 1.0");
        Check.close(0.25, CombatDivisor.factor(0.25, 0.5), "factor never above base");
        Check.close(1.0, CombatDivisor.factor(1.0, 1.0), "graduated base 1.0 stays 1.0");
        Check.ok(CombatDivisorTest.throwsIAE(() -> CombatDivisor.factor(1.5, 2.0)), "factor rejects base > 1");
        // Table sync check.
        Check.ok(CombatDivisor.tableMatches(1.75, 1.75), "table matches live 1.75");
        Check.ok(!CombatDivisor.tableMatches(1.75, 1.6), "table 1.75 vs live 1.60 = mismatch");
        Check.ok(CombatDivisor.validLive(2.0) && !CombatDivisor.validLive(0.0) && !CombatDivisor.validLive(50.0), "live bounds");
    }

    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; }
        catch (IllegalArgumentException e) { return true; }
    }
}
