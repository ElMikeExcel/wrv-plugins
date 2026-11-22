import com.winkreyes.excelemfoodheal.HealEngine;
import java.util.UUID;

/**
 * Engine behavior on an injected clock: delivery rate, combat
 * pause/resume (budget waits, never drains), replace-not-stack,
 * expiry (including mid-pause), the full-health wait, and the
 * instant-only floor meal.
 */
public final class HealEngineTest {

    private static final HealEngine.Settings SETTINGS =
            new HealEngine.Settings(1.0, 0.5, 20_000L, 90_000L);
    private static final UUID P = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private static HealEngine engine() {
        return new HealEngine(SETTINGS);
    }

    public static void run() {
        System.out.println("HealEngineTest");
        deliveryRateAndCompletion();
        combatPauseAndResume();
        combatMarkRefreshesWindow();
        dealtDamageAlsoPauses();
        replaceNeverStacks();
        expiryAtDeadline();
        expiryBeatsPause();
        fullHealthWaitDoesNotDrain();
        floorMealIsInstantOnly();
        forgetClearsEverything();
        noMealTicksWait();
    }

    private static void deliveryRateAndCompletion() {
        HealEngine e = engine();
        double instant = e.startMeal(P, "COOKED_BEEF", 9.6, 0L);
        Check.close(1.0, instant, "steak meal returns the 1% instant");
        Check.ok(!e.reportDelivered(P, 1.0), "budget not finished after instant");
        Check.close(8.6, e.remainingPercent(P), "remaining is total minus instant (8.6)");
        long t = 1000L;
        for (int i = 0; i < 17; i++, t += 1000L) {
            HealEngine.TickDecision d = e.tick(P, t);
            Check.ok(d.kind() == HealEngine.TickKind.DELIVER, "tick " + i + " delivers");
            Check.close(0.5, d.percentToDeliver(), "tick " + i + " delivers the 0.5%/s rate");
            Check.ok(!d.resumed(), "tick " + i + " is not a resume");
            e.reportDelivered(P, d.percentToDeliver());
        }
        Check.close(0.1, e.remainingPercent(P), "after 17 ticks + instant, 0.1% remains");
        HealEngine.TickDecision last = e.tick(P, t);
        Check.close(0.1, last.percentToDeliver(), "final tick delivers only the remainder (no overshoot)");
        Check.ok(e.reportDelivered(P, last.percentToDeliver()), "budget finishes on the final tick");
        Check.ok(!e.hasMeal(P), "meal removed after completion");
    }

    private static void combatPauseAndResume() {
        HealEngine e = engine();
        e.startMeal(P, "COOKED_BEEF", 9.6, 0L);
        e.reportDelivered(P, 1.0);
        e.tick(P, 1000L);
        e.reportDelivered(P, 0.5); // remaining 8.1
        e.markCombat(P, 2000L);    // hit taken at t=2s
        HealEngine.TickDecision paused = e.tick(P, 3000L);
        Check.ok(paused.kind() == HealEngine.TickKind.PAUSED, "first in-combat tick reports PAUSED");
        Check.close(8.1, e.remainingPercent(P), "pause does not drain the budget");
        for (long t = 4000L; t <= 21000L; t += 1000L) {
            HealEngine.TickDecision d = e.tick(P, t);
            Check.ok(d.kind() == HealEngine.TickKind.WAIT, "in-combat tick at " + t + " waits");
        }
        Check.close(8.1, e.remainingPercent(P), "budget fully intact after the whole combat window");
        HealEngine.TickDecision resumed = e.tick(P, 22000L); // exactly 20 s after the hit
        Check.ok(resumed.kind() == HealEngine.TickKind.DELIVER, "ticks resume once the 20 s window passes");
        Check.ok(resumed.resumed(), "resume tick is flagged as a resume");
        Check.close(0.5, resumed.percentToDeliver(), "resume tick delivers the normal rate");
    }

    private static void combatMarkRefreshesWindow() {
        HealEngine e = engine();
        e.startMeal(P, "BREAD", 4.5, 0L);
        e.reportDelivered(P, 1.0);
        e.markCombat(P, 5000L);
        e.markCombat(P, 20000L); // fresh hit late in the window restarts it
        Check.ok(e.inCombat(P, 39999L), "refreshed window still in combat at 39.999 s");
        Check.ok(!e.inCombat(P, 40000L), "refreshed window ends 20 s after the LAST hit");
    }

    private static void dealtDamageAlsoPauses() {
        // The engine has one markCombat entry point; the plugin calls it
        // for damage dealt as well as taken. Verify the mark alone pauses.
        HealEngine e = engine();
        e.startMeal(P, "BREAD", 4.5, 0L);
        e.reportDelivered(P, 1.0);
        e.markCombat(P, 1000L);
        Check.ok(e.tick(P, 2000L).kind() == HealEngine.TickKind.PAUSED,
                "a combat mark (dealt or taken) pauses delivery");
    }

    private static void replaceNeverStacks() {
        HealEngine e = engine();
        e.startMeal(P, "COOKED_BEEF", 9.6, 0L);
        e.reportDelivered(P, 1.0);
        e.tick(P, 1000L);
        e.reportDelivered(P, 0.5); // steak remaining 8.1
        double instant = e.startMeal(P, "BREAD", 4.5, 2000L); // re-eat replaces
        Check.close(1.0, instant, "replacement meal returns its own instant");
        Check.eq("BREAD", e.mealFood(P), "active meal is the replacement food");
        Check.close(4.5, e.remainingPercent(P), "remaining is the NEW total, not old + new (no stacking)");
        e.reportDelivered(P, 1.0);
        Check.close(3.5, e.remainingPercent(P), "replacement drains against its own total");
        // Expiry clock restarted with the new meal: old deadline (t=90s) must not kill it.
        HealEngine.TickDecision d = e.tick(P, 90500L);
        Check.ok(d.kind() != HealEngine.TickKind.EXPIRED, "replacement meal survives the original deadline");
    }

    private static void expiryAtDeadline() {
        HealEngine e = engine();
        e.startMeal(P, "COOKED_BEEF", 9.6, 0L);
        e.reportDelivered(P, 1.0);
        Check.ok(e.tick(P, 89999L).kind() != HealEngine.TickKind.EXPIRED, "no expiry before 90 s");
        HealEngine.TickDecision expired = e.tick(P, 90000L);
        Check.ok(expired.kind() == HealEngine.TickKind.EXPIRED, "budget expires at 90 s");
        Check.close(8.6, expired.remainingPercent(), "expiry reports the undelivered remainder");
        Check.ok(!e.hasMeal(P), "expired meal is removed");
    }

    private static void expiryBeatsPause() {
        HealEngine e = engine();
        e.startMeal(P, "COOKED_BEEF", 9.6, 0L);
        e.reportDelivered(P, 1.0);
        e.markCombat(P, 89000L); // in combat across the deadline
        Check.ok(e.tick(P, 90000L).kind() == HealEngine.TickKind.EXPIRED,
                "expiry fires even while paused in combat");
    }

    private static void fullHealthWaitDoesNotDrain() {
        HealEngine e = engine();
        e.startMeal(P, "COOKED_BEEF", 9.6, 0L);
        // Player at full health: the Bukkit layer can deliver 0.
        Check.ok(!e.reportDelivered(P, 0.0), "zero delivery does not finish the budget");
        Check.close(9.6, e.remainingPercent(P), "zero delivery does not drain the budget (instant waits too)");
        HealEngine.TickDecision d = e.tick(P, 1000L);
        Check.ok(d.kind() == HealEngine.TickKind.DELIVER, "ticks still offer delivery while waiting at full health");
        Check.ok(!e.reportDelivered(P, 0.0), "repeated zero delivery still does not drain");
        Check.close(9.6, e.remainingPercent(P), "budget intact until health is actually missing");
        e.reportDelivered(P, 1.0);
        Check.close(8.6, e.remainingPercent(P), "once damage lands, delivered % drains normally");
    }

    private static void floorMealIsInstantOnly() {
        HealEngine e = engine();
        double instant = e.startMeal(P, "COOKIE", 1.0, 0L);
        Check.close(1.0, instant, "floor meal's whole budget is the instant");
        Check.ok(e.reportDelivered(P, 1.0), "floor meal finishes on its instant delivery");
        Check.ok(!e.hasMeal(P), "no HoT state lingers for a floor meal");
    }

    private static void forgetClearsEverything() {
        HealEngine e = engine();
        e.startMeal(P, "BREAD", 4.5, 0L);
        e.markCombat(P, 0L);
        e.forget(P);
        Check.ok(!e.hasMeal(P), "forget drops the meal");
        Check.ok(!e.inCombat(P, 1000L), "forget drops the combat mark");
    }

    private static void noMealTicksWait() {
        HealEngine e = engine();
        Check.ok(e.tick(P, 12345L).kind() == HealEngine.TickKind.WAIT, "tick with no meal waits quietly");
        Check.ok(e.reportDelivered(P, 1.0), "report with no meal is a no-op finish");
    }
}
