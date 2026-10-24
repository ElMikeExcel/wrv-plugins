import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.AttunementState.CastResult;
import com.winkreyes.excelattunement.core.AttunementState.HitResult;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.Stance;

/** State machine rules: entries, refresh, expiry, no-flip, combat context. */
public class StanceStateTest {
    static final long T0 = 1_000_000L;

    static AttunementState fresh() {
        return new AttunementState(30, 10);
    }

    static void run() {
        System.out.println("== StanceStateTest ==");
        martialLifecycle();
        casterLifecycle();
        supportContext();
        utilityNever();
        takenRefreshesDealtDoesNot();
        noFlipEitherWay();
        expiryPollAndClear();
        isolation();
    }

    static void martialLifecycle() {
        AttunementState s = fresh();
        Check.eq(Stance.NONE, s.activeStance(T0), "starts unstanced");
        Check.eq(HitResult.SET, s.onMartialHit(T0), "first landed hit SETS martial");
        Check.eq(Stance.MARTIAL, s.activeStance(T0), "martial active");
        Check.eq(30L, s.remainingSeconds(T0), "full 30s at set");
        Check.eq(HitResult.REFRESHED, s.onMartialHit(T0 + 20_000), "second hit refreshes");
        Check.eq(Stance.MARTIAL, s.activeStance(T0 + 49_999), "still martial 1ms before refreshed expiry");
        Check.eq(Stance.NONE, s.activeStance(T0 + 50_000), "expires exactly at refreshed window edge");
        Check.eq(0L, s.remainingSeconds(T0 + 50_000), "no time left after expiry");
    }

    static void casterLifecycle() {
        AttunementState s = fresh();
        Check.eq(CastResult.SET, s.onCast(SpellClass.COMBAT, T0), "combat cast SETS caster with no prior context");
        Check.eq(Stance.CASTER, s.activeStance(T0), "caster active");
        Check.eq(CastResult.REFRESHED, s.onCast(SpellClass.COMBAT, T0 + 25_000), "second combat cast refreshes");
        Check.eq(Stance.CASTER, s.activeStance(T0 + 54_999), "caster holds to refreshed edge");
        Check.eq(Stance.NONE, s.activeStance(T0 + 55_000), "caster expires at edge");
    }

    static void supportContext() {
        // Out of combat: the accepted pre-fight opener — never attunes.
        AttunementState s = fresh();
        Check.eq(CastResult.NOT_QUALIFYING, s.onCast(SpellClass.SUPPORT, T0),
                "support cast out of combat does NOT set caster");
        Check.eq(Stance.NONE, s.activeStance(T0), "still unstanced after opener heal");

        // In combat by dealing damage.
        AttunementState d = fresh();
        d.onDamageDealt(T0);
        Check.eq(CastResult.SET, d.onCast(SpellClass.SUPPORT, T0 + 5_000),
                "support cast after dealing damage sets caster");
        // In combat by taking hostile damage.
        AttunementState t = fresh();
        t.onHostileDamageTaken(T0);
        Check.eq(CastResult.SET, t.onCast(SpellClass.SUPPORT, T0 + 9_999),
                "support cast after taking damage sets caster");
        // Context edge: exactly 10s later still counts; 10s+1ms does not.
        AttunementState e = fresh();
        e.onDamageDealt(T0);
        Check.ok(e.inCombat(T0 + 10_000), "context holds at exactly 10s");
        Check.ok(!e.inCombat(T0 + 10_001), "context gone at 10s + 1ms");
        Check.eq(CastResult.NOT_QUALIFYING, e.onCast(SpellClass.SUPPORT, T0 + 10_001),
                "support cast after context lapses does not set caster");
    }

    static void utilityNever() {
        AttunementState s = fresh();
        Check.eq(CastResult.NOT_QUALIFYING, s.onCast(SpellClass.UTILITY, T0), "utility out of combat: nothing");
        s.onDamageDealt(T0);
        s.onHostileDamageTaken(T0);
        Check.eq(CastResult.NOT_QUALIFYING, s.onCast(SpellClass.UTILITY, T0 + 100),
                "utility in combat: still nothing");
        Check.eq(Stance.NONE, s.activeStance(T0 + 100), "utility never attunes");
    }

    static void takenRefreshesDealtDoesNot() {
        AttunementState s = fresh();
        s.onMartialHit(T0); // expires T0+30s
        s.onDamageDealt(T0 + 20_000);
        Check.eq(Stance.NONE, s.activeStance(T0 + 30_000),
                "dealing damage does NOT refresh the stance window");
        AttunementState t = fresh();
        t.onMartialHit(T0);
        t.onHostileDamageTaken(T0 + 20_000);
        Check.eq(Stance.MARTIAL, t.activeStance(T0 + 49_999),
                "taking hostile damage refreshes the active stance");
        Check.eq(Stance.NONE, t.activeStance(T0 + 50_000), "refreshed stance still expires");
        // Same for caster.
        AttunementState c = fresh();
        c.onCast(SpellClass.COMBAT, T0);
        c.onHostileDamageTaken(T0 + 20_000);
        Check.eq(Stance.CASTER, c.activeStance(T0 + 49_999), "hostile damage refreshes caster too");
    }

    static void noFlipEitherWay() {
        // A caster who swings: zeroed hits never flip them (state-level guard).
        AttunementState s = fresh();
        s.onCast(SpellClass.COMBAT, T0);
        Check.eq(HitResult.IGNORED_CASTER_ACTIVE, s.onMartialHit(T0 + 1_000),
                "martial hit while caster is IGNORED (no flip)");
        Check.eq(Stance.CASTER, s.activeStance(T0 + 1_000), "still caster after the swing");
        Check.eq(Stance.CASTER, s.activeStance(T0 + 29_999), "caster expiry unmoved by the swing");
        Check.eq(Stance.NONE, s.activeStance(T0 + 30_000), "caster expires on its original clock");
        // A martial player cannot cast (Bukkit blocks it); if a cast event
        // somehow arrives anyway, the state refuses to flip too.
        AttunementState m = fresh();
        m.onMartialHit(T0);
        Check.eq(CastResult.IGNORED_MARTIAL_ACTIVE, m.onCast(SpellClass.COMBAT, T0 + 1_000),
                "cast while martial is IGNORED (no flip)");
        Check.eq(Stance.MARTIAL, m.activeStance(T0 + 1_000), "still martial");
    }

    static void expiryPollAndClear() {
        AttunementState s = fresh();
        s.onMartialHit(T0);
        Check.ok(!s.pollExpired(T0 + 29_999), "no expiry before the window ends");
        Check.ok(s.pollExpired(T0 + 30_000), "expiry reported at the window edge");
        Check.ok(!s.pollExpired(T0 + 30_001), "expiry reported exactly once");
        Check.eq(HitResult.SET, s.onMartialHit(T0 + 31_000), "can attune again after expiry");
        s.clear();
        Check.eq(Stance.NONE, s.activeStance(T0 + 31_000), "clear drops the stance");
        Check.ok(!s.inCombat(T0 + 31_000), "clear drops the combat context");
        Check.eq(CastResult.NOT_QUALIFYING, s.onCast(SpellClass.SUPPORT, T0 + 31_000),
                "support right after clear is out-of-combat again");
        Check.eq(15L, remainingCeilCheck(), "remaining seconds round up");
    }

    static long remainingCeilCheck() {
        AttunementState s = fresh();
        s.onMartialHit(T0);
        return s.remainingSeconds(T0 + 15_500); // 14.5s left -> shows 15
    }

    static void isolation() {
        AttunementState a = fresh();
        AttunementState b = fresh();
        a.onMartialHit(T0);
        b.onCast(SpellClass.COMBAT, T0);
        Check.eq(Stance.MARTIAL, a.activeStance(T0), "player A martial");
        Check.eq(Stance.CASTER, b.activeStance(T0), "player B caster");
        a.clear();
        Check.eq(Stance.CASTER, b.activeStance(T0), "clearing A leaves B untouched");
    }
}
