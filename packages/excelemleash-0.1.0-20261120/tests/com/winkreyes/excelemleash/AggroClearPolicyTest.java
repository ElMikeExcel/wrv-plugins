import com.winkreyes.excelemleash.AggroClearPolicy;

/** Leg A targeting decisions: the full truth table of the scope guard. */
public final class AggroClearPolicyTest {

    public static void run() {
        System.out.println("AggroClearPolicyTest");

        // The only CLEAR row: leg on, entity resolves, and it is a regional boss.
        Check.eq(AggroClearPolicy.Decision.CLEAR,
                AggroClearPolicy.decide(true, true, true), "enabled + resolved + regional -> CLEAR");

        // Scope guard: a non-regional elite (dungeon boss / natural elite) is never touched.
        Check.eq(AggroClearPolicy.Decision.SKIP_NOT_REGIONAL,
                AggroClearPolicy.decide(true, true, false), "enabled + resolved + not regional -> SKIP_NOT_REGIONAL");

        // An entity the tracker does not know is never touched.
        Check.eq(AggroClearPolicy.Decision.SKIP_NO_ELITE,
                AggroClearPolicy.decide(true, false, false), "enabled + unresolved -> SKIP_NO_ELITE");
        Check.eq(AggroClearPolicy.Decision.SKIP_NO_ELITE,
                AggroClearPolicy.decide(true, false, true), "unresolved wins over regional flag");

        // The leg toggle wins over everything.
        Check.eq(AggroClearPolicy.Decision.SKIP_LEG_DISABLED,
                AggroClearPolicy.decide(false, true, true), "disabled + resolved + regional -> SKIP_LEG_DISABLED");
        Check.eq(AggroClearPolicy.Decision.SKIP_LEG_DISABLED,
                AggroClearPolicy.decide(false, false, false), "disabled wins over unresolved");
    }
}
