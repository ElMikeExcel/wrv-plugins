import com.winkreyes.exceldeathrecord.core.BurnCause;

public class BurnCauseTest {
    public static void run() {
        System.out.println("BurnCauseTest");
        Check.eq("LAVA", BurnCause.fromDamageCause("LAVA"), "LAVA damage maps to LAVA");
        Check.eq("FIRE", BurnCause.fromDamageCause("FIRE"), "FIRE damage maps to FIRE");
        Check.eq("FIRE", BurnCause.fromDamageCause("FIRE_TICK"), "FIRE_TICK damage maps to FIRE");
        Check.eq("VOID", BurnCause.fromDamageCause("VOID"), "VOID damage maps to VOID");
        Check.eq(null, BurnCause.fromDamageCause("ENTITY_ATTACK"), "ENTITY_ATTACK is not a burn cause");
        Check.eq(null, BurnCause.fromDamageCause("CONTACT"), "CONTACT (cactus) is not tracked in v1");
        Check.eq(null, BurnCause.fromDamageCause("ENTITY_EXPLOSION"), "explosions are not a burn cause");
        Check.eq(null, BurnCause.fromDamageCause("FALL"), "FALL is not a burn cause");
        Check.eq(null, BurnCause.fromDamageCause(null), "null damage cause maps to null");
        Check.eq(null, BurnCause.fromDamageCause(""), "empty damage cause maps to null");
    }
}
