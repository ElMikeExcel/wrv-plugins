import excelemfixes.leash.ReturnPolicy;

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

        // --- D251 arrival heal: the teleport-heal cycle fix ---
        // Touched + tuning on + in scope: NO heal on arrival.
        Check.eq(ReturnPolicy.ArrivalHeal.NONE, ReturnPolicy.arrivalHeal(true, true, true),
                "D251: combat-touched return does not heal");
        // Untouched returns keep the classic D183 full heal.
        Check.eq(ReturnPolicy.ArrivalHeal.FULL, ReturnPolicy.arrivalHeal(true, false, true),
                "D251: untouched return still heals to full");
        // The knob off restores classic behavior even when touched.
        Check.eq(ReturnPolicy.ArrivalHeal.FULL, ReturnPolicy.arrivalHeal(false, true, true),
                "D251: tuning off = classic heal-to-full");
        // Out-of-scope bosses keep the classic heal even when touched.
        Check.eq(ReturnPolicy.ArrivalHeal.FULL, ReturnPolicy.arrivalHeal(true, true, false),
                "D251: out-of-scope boss keeps classic heal");

        // --- D251 HP outcomes: touched return preserves HP exactly ---
        Check.eq(137.5, ReturnPolicy.healthAfterArrival(ReturnPolicy.ArrivalHeal.NONE, 137.5, 400.0),
                "D251: touched return HP unchanged at arrival");
        Check.eq(1.0, ReturnPolicy.healthAfterArrival(ReturnPolicy.ArrivalHeal.NONE, 1.0, 400.0),
                "D251: touched return at 1 HP stays at 1 HP (no courtesy heal)");
        Check.eq(400.0, ReturnPolicy.healthAfterArrival(ReturnPolicy.ArrivalHeal.FULL, 137.5, 400.0),
                "D251: untouched return restores max HP");
        // Post-return recovery for a touched elite is the standing
        // out-of-combat regen (D184) and nothing else: the module's
        // own heal contribution at arrival is exactly zero, so any
        // later HP gain can only come from that regen — the arrival
        // instant itself never jumps.
        Check.eq(0.0, ReturnPolicy.healthAfterArrival(ReturnPolicy.ArrivalHeal.NONE, 200.0, 400.0) - 200.0,
                "D251: module heal at arrival is zero; recovery is regen-only (D184)");

        // --- D251 scope resolution ---
        Check.ok(ReturnPolicy.scopeApplies(java.util.List.of(), "primis_ice_village_manifested_nightmare.yml"),
                "D251 scope: empty list covers every boss");
        Check.ok(ReturnPolicy.scopeApplies(null, "anything.yml"),
                "D251 scope: null list covers every boss");
        Check.ok(ReturnPolicy.scopeApplies(java.util.List.of("a.yml", "b.yml"), "b.yml"),
                "D251 scope: listed boss is in scope");
        Check.ok(!ReturnPolicy.scopeApplies(java.util.List.of("a.yml"), "b.yml"),
                "D251 scope: unlisted boss is out of scope");
        Check.ok(!ReturnPolicy.scopeApplies(java.util.List.of("a.yml"), null),
                "D251 scope: unidentified boss fails open to classic heal when a list is set");

        // --- v30 combat-end reset-heal veto ---
        // The reset heal itself: tuning on + FULL heal + touched +
        // EM out of combat + in scope -> VETO.
        Check.ok(ReturnPolicy.shouldVetoCombatEndHeal(true, true, true, false, true),
                "v30: combat-end reset heal for a touched elite is vetoed");
        // Untouched elites keep the stock combat-end heal.
        Check.ok(!ReturnPolicy.shouldVetoCombatEndHeal(true, true, false, false, true),
                "v30: untouched elite keeps the stock combat-end heal");
        // In-combat full heals (phase resets, powers) never vetoed.
        Check.ok(!ReturnPolicy.shouldVetoCombatEndHeal(true, true, true, true, true),
                "v30: in-combat full heal is never vetoed");
        // Amount-based heals are a different event shape: never vetoed.
        Check.ok(!ReturnPolicy.shouldVetoCombatEndHeal(true, false, true, false, true),
                "v30: amount-based heal is never vetoed");
        // The knob off restores stock behavior even when touched.
        Check.ok(!ReturnPolicy.shouldVetoCombatEndHeal(false, true, true, false, true),
                "v30: tuning off = stock combat-end heal");
        // Out-of-scope bosses keep the stock heal even when touched.
        Check.ok(!ReturnPolicy.shouldVetoCombatEndHeal(true, true, true, false, false),
                "v30: out-of-scope boss keeps the stock combat-end heal");
    }
}
