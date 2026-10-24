import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.DamageRule;
import com.winkreyes.excelattunement.core.ProjectileTracker;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.Stance;
import com.winkreyes.excelattunement.core.WeaponKind;
import com.winkreyes.excelattunement.core.Weapons;

import java.util.Optional;
import java.util.UUID;

/**
 * Mike's pin (Oct 19, 2026): the RANGED martial cases, explicitly.
 * These tests simulate the exact glue flow — launch recorded in the
 * tracker, damage resolved at landing — for bow, crossbow and thrown
 * trident, plus the caster-zeroing of projectiles and the missed shot.
 */
public class RangedMartialTest {
    static final long T0 = 5_000_000L;
    static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

    /** Mirror of the glue: a tracked projectile lands for rawDamage. Returns the final damage. */
    static double landProjectile(ProjectileTracker tracker, AttunementState shooterState,
                                 UUID projectileId, double rawDamage, long now) {
        // Paper fires ProjectileHitEvent BEFORE the damage event of the
        // same impact (rig-proven), so the glue marks the hit first and
        // the tracker still resolves inside its grace window.
        tracker.markHit(projectileId, now);
        Optional<ProjectileTracker.Shot> shot = tracker.resolve(projectileId, now);
        if (shot.isEmpty()) return rawDamage; // untracked: not a martial projectile hit
        double dmg = rawDamage;
        if (DamageRule.shouldZero(shooterState.activeStance(now), shot.get().kind())) {
            dmg = 0.0;
        }
        if (DamageRule.isMartialTrigger(shot.get().kind(), false, dmg)) {
            shooterState.onMartialHit(now);
        }
        return dmg;
    }

    static UUID launch(ProjectileTracker tracker, UUID shooter, String entity, String hand, long now) {
        UUID id = UUID.randomUUID();
        tracker.record(id, shooter, Weapons.kindOfProjectile(entity, hand), now);
        return id;
    }

    static void run() {
        System.out.println("== RangedMartialTest ==");
        projectileKinds();
        bowSetsAndRefreshes();
        crossbowSets();
        tridentThrowSets();
        casterZeroesProjectiles();
        judgedAtLanding();
        missedShotSetsNothing();
        otherPeoplesArrows();
        zeroDamageHitNeverTriggers();
    }

    static void projectileKinds() {
        Check.eq(WeaponKind.BOW, Weapons.kindOfProjectile("ARROW", "BOW"), "arrow from a bow is BOW");
        Check.eq(WeaponKind.CROSSBOW, Weapons.kindOfProjectile("ARROW", "CROSSBOW"), "bolt from a crossbow is CROSSBOW");
        Check.eq(WeaponKind.BOW, Weapons.kindOfProjectile("SPECTRAL_ARROW", "BOW"), "spectral arrow is BOW");
        Check.eq(WeaponKind.TRIDENT, Weapons.kindOfProjectile("TRIDENT", "TRIDENT"), "thrown trident is TRIDENT");
        Check.eq(WeaponKind.NONE, Weapons.kindOfProjectile("SNOWBALL", "BOW"), "snowball is never martial");
        Check.eq(WeaponKind.NONE, Weapons.kindOfProjectile("EGG", null), "egg is never martial");
        Check.eq(WeaponKind.NONE, Weapons.kindOfProjectile("ENDER_PEARL", "BOW"), "ender pearl is never martial");
        Check.eq(WeaponKind.NONE, Weapons.kindOfProjectile(null, "BOW"), "null projectile is never martial");
        ProjectileTracker t = new ProjectileTracker();
        UUID id = UUID.randomUUID();
        t.record(id, ALICE, WeaponKind.NONE, T0);
        Check.ok(t.resolve(id, T0).isEmpty(), "tracker refuses to record a non-martial shot");
    }

    static void bowSetsAndRefreshes() {
        ProjectileTracker t = new ProjectileTracker();
        AttunementState s = new AttunementState(30, 10);
        UUID a1 = launch(t, ALICE, "ARROW", "BOW", T0);
        Check.eq(5.0, landProjectile(t, s, a1, 5.0, T0 + 500), "bow arrow lands for full damage");
        Check.eq(Stance.MARTIAL, s.activeStance(T0 + 500), "(1) bow arrow hit SETS martial stance");
        UUID a2 = launch(t, ALICE, "ARROW", "BOW", T0 + 20_000);
        landProjectile(t, s, a2, 5.0, T0 + 20_500);
        Check.eq(Stance.MARTIAL, s.activeStance(T0 + 50_499), "second arrow refreshes the window");
        Check.eq(Stance.NONE, s.activeStance(T0 + 50_500), "refreshed window still expires");
    }

    static void crossbowSets() {
        ProjectileTracker t = new ProjectileTracker();
        AttunementState s = new AttunementState(30, 10);
        UUID bolt = launch(t, ALICE, "ARROW", "CROSSBOW", T0);
        landProjectile(t, s, bolt, 7.0, T0 + 300);
        Check.eq(Stance.MARTIAL, s.activeStance(T0 + 300), "(2) crossbow bolt hit SETS martial stance");
    }

    static void tridentThrowSets() {
        ProjectileTracker t = new ProjectileTracker();
        AttunementState s = new AttunementState(30, 10);
        UUID tri = launch(t, ALICE, "TRIDENT", "TRIDENT", T0);
        landProjectile(t, s, tri, 8.0, T0 + 400);
        Check.eq(Stance.MARTIAL, s.activeStance(T0 + 400), "(3) thrown trident hit SETS martial stance");
    }

    static void casterZeroesProjectiles() {
        ProjectileTracker t = new ProjectileTracker();
        AttunementState s = new AttunementState(30, 10);
        s.onCast(SpellClass.COMBAT, T0); // caster stance from T0
        UUID a = launch(t, ALICE, "ARROW", "BOW", T0 + 1_000);
        Check.eq(0.0, landProjectile(t, s, a, 6.0, T0 + 1_500),
                "(4) arrow damage is ZEROED while caster stance is active");
        Check.eq(Stance.CASTER, s.activeStance(T0 + 1_500), "zeroed arrow does not flip caster to martial");
        Check.eq(Stance.NONE, s.activeStance(T0 + 30_000), "zeroed arrow did not refresh anything");
        UUID bolt = launch(t, ALICE, "ARROW", "CROSSBOW", T0 + 2_000);
        s.onCast(SpellClass.COMBAT, T0 + 2_000); // refresh caster so it is active at landing
        Check.eq(0.0, landProjectile(t, s, bolt, 6.0, T0 + 2_500),
                "(4) crossbow bolt damage is ZEROED while caster stance is active");
        UUID tri = launch(t, ALICE, "TRIDENT", "TRIDENT", T0 + 3_000);
        Check.eq(0.0, landProjectile(t, s, tri, 8.0, T0 + 3_500),
                "(4) thrown trident damage is ZEROED while caster stance is active");
    }

    static void judgedAtLanding() {
        // Arrow loosed BEFORE the cast, lands AFTER: still zeroed.
        ProjectileTracker t = new ProjectileTracker();
        AttunementState s = new AttunementState(30, 10);
        UUID a = launch(t, ALICE, "ARROW", "BOW", T0);
        s.onCast(SpellClass.COMBAT, T0 + 200);
        Check.eq(0.0, landProjectile(t, s, a, 6.0, T0 + 600),
                "in-flight arrow is judged at landing (zeroed under new caster stance)");
        // And the mirror: a shot landing while martial refreshes martial.
        ProjectileTracker t2 = new ProjectileTracker();
        AttunementState s2 = new AttunementState(30, 10);
        s2.onMartialHit(T0);
        UUID a2 = launch(t2, ALICE, "ARROW", "BOW", T0 + 25_000);
        landProjectile(t2, s2, a2, 5.0, T0 + 25_500);
        Check.eq(Stance.MARTIAL, s2.activeStance(T0 + 55_499), "arrow landing while martial refreshes martial");
    }

    static void missedShotSetsNothing() {
        ProjectileTracker t = new ProjectileTracker();
        AttunementState s = new AttunementState(30, 10);
        UUID a = launch(t, ALICE, "ARROW", "BOW", T0);
        t.markHit(a, T0 + 100); // hit a block: the miss path — no damage event ever follows
        Check.eq(Stance.NONE, s.activeStance(T0 + 200), "(5) a missed shot sets NOTHING");
        Check.ok(t.resolve(a, T0 + 150).isPresent(),
                "a hit record stays resolvable inside the grace window (hit event precedes damage)");
        Check.ok(t.resolve(a, T0 + 100 + ProjectileTracker.HIT_GRACE_MS + 1).isEmpty(),
                "after the grace window the spent record no longer resolves");
        Check.eq(1, t.sweep(T0 + 100 + ProjectileTracker.HIT_GRACE_MS + 1),
                "sweep retires the spent record after grace");
        // Age sweep also retires a record, so a stale id can never be borrowed later.
        UUID b = launch(t, ALICE, "ARROW", "BOW", T0);
        Check.eq(1, t.sweep(T0 + ProjectileTracker.MAX_AGE_MS + 1), "age sweep drops the stale record");
        Check.eq(5.0, landProjectile(t, s, b, 5.0, T0 + ProjectileTracker.MAX_AGE_MS + 2),
                "a swept (untracked) projectile id cannot trigger anything");
        Check.eq(Stance.NONE, s.activeStance(T0 + ProjectileTracker.MAX_AGE_MS + 2), "still unstanced");
        // Shooter cleanup (quit) drops their in-flight records.
        UUID c = launch(t, ALICE, "TRIDENT", "TRIDENT", T0);
        t.forgetShooter(ALICE);
        Check.ok(t.resolve(c, T0).isEmpty(), "quit cleanup drops the shooter's in-flight shots");
    }

    static void otherPeoplesArrows() {
        ProjectileTracker t = new ProjectileTracker();
        AttunementState alice = new AttunementState(30, 10);
        AttunementState bob = new AttunementState(30, 10);
        UUID bobsArrow = launch(t, BOB, "ARROW", "BOW", T0);
        Optional<ProjectileTracker.Shot> shot = t.resolve(bobsArrow, T0);
        Check.ok(shot.isPresent() && shot.get().shooter().equals(BOB), "tracker attributes the shot to its shooter");
        landProjectile(t, bob, bobsArrow, 5.0, T0 + 400);
        Check.eq(Stance.MARTIAL, bob.activeStance(T0 + 400), "the shooter's state attunes");
        Check.eq(Stance.NONE, alice.activeStance(T0 + 400), "the bystander's state does not");
    }

    static void zeroDamageHitNeverTriggers() {
        // A hit fully absorbed elsewhere (final damage 0) is not a landed hit.
        AttunementState s = new AttunementState(30, 10);
        Check.ok(!DamageRule.isMartialTrigger(WeaponKind.BOW, false, 0.0), "0-damage arrow is no trigger");
        Check.ok(!DamageRule.isMartialTrigger(WeaponKind.MELEE, true, 5.0), "cancelled hit is no trigger");
        Check.ok(DamageRule.isMartialTrigger(WeaponKind.TRIDENT, false, 0.5), "real landed hit is a trigger");
        Check.eq(Stance.NONE, s.activeStance(T0), "no trigger fired, no stance");
    }
}
