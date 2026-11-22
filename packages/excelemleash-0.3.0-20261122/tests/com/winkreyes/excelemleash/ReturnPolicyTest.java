import com.winkreyes.excelemleash.ReturnPolicy;

/**
 * Plain-JVM tests for ReturnPolicy — the D183 state-machine
 * decisions (no Bukkit / EliteMobs on the classpath).
 */
public class ReturnPolicyTest {

    public static void run() {
        // --- Trigger: only in combat, only strictly beyond range ---
        Check.ok(!ReturnPolicy.shouldTriggerReturn(false, true, 40.0, 96.0),
                "no trigger inside range (camp fight)");
        Check.ok(!ReturnPolicy.shouldTriggerReturn(false, true, 96.0, 96.0),
                "no trigger exactly at range");
        Check.ok(ReturnPolicy.shouldTriggerReturn(false, true, 96.01, 96.0),
                "trigger just past range");
        Check.ok(ReturnPolicy.shouldTriggerReturn(false, true, 250.0, 96.0),
                "trigger far past range");
        Check.ok(!ReturnPolicy.shouldTriggerReturn(false, false, 250.0, 96.0),
                "no trigger when not in combat");
        Check.ok(!ReturnPolicy.shouldTriggerReturn(true, true, 250.0, 96.0),
                "no re-trigger while already returning");
        Check.ok(ReturnPolicy.shouldTriggerReturn(false, true, 49.0, 48.0)
                        && !ReturnPolicy.shouldTriggerReturn(false, true, 49.0, 96.0),
                "range is the caller's value (48 vs 96)");

        // --- Arrival ---
        Check.ok(ReturnPolicy.hasArrived(0.0), "arrived at spawn point");
        Check.ok(ReturnPolicy.hasArrived(ReturnPolicy.ARRIVE_RADIUS), "arrived inside arrive radius");
        Check.ok(!ReturnPolicy.hasArrived(ReturnPolicy.ARRIVE_RADIUS + 0.01),
                "not arrived just outside arrive radius");
        Check.ok(!ReturnPolicy.hasArrived(50.0), "not arrived mid-walk");

        // --- Teleport fallback ---
        Check.ok(!ReturnPolicy.shouldTeleportFallback(1_000L, 1_000L),
                "no fallback while progressing");
        Check.ok(ReturnPolicy.shouldTeleportFallback(6_000L, ReturnPolicy.STUCK_PROGRESS_MILLIS + 1),
                "fallback when stuck past the progress window");
        Check.ok(!ReturnPolicy.shouldTeleportFallback(5_000L, ReturnPolicy.STUCK_PROGRESS_MILLIS),
                "no fallback at exactly the stuck window");
        Check.ok(ReturnPolicy.shouldTeleportFallback(ReturnPolicy.MAX_RETURN_MILLIS + 1, 0L),
                "fallback when the return overruns in total");

        // --- Damage: the free-fight fix ---
        Check.eq(ReturnPolicy.DamageOutcome.REAGGRO, ReturnPolicy.onPlayerDamage(true),
                "damage mid-return re-aggroes");
        Check.eq(ReturnPolicy.DamageOutcome.NONE, ReturnPolicy.onPlayerDamage(false),
                "damage in a normal fight is ordinary combat");
    }
}
