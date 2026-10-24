import com.winkreyes.excelattunement.core.AttunementState;
import com.winkreyes.excelattunement.core.LockFeedback;
import com.winkreyes.excelattunement.core.LockRule;
import com.winkreyes.excelattunement.core.LockRule.Block;
import com.winkreyes.excelattunement.core.SpellClass;
import com.winkreyes.excelattunement.core.Stance;
import com.winkreyes.excelattunement.core.StanceText;

/** 0.1.4: weapon lock / wand lock decisions, feedback throttle, text. */
public class LockTest {
    static final long T0 = 5_000_000L;

    static void run() {
        System.out.println("== LockTest ==");
        weaponLock();
        wandLock();
        knobs();
        epochs();
        feedback();
        text();
    }

    static void weaponLock() {
        for (String m : new String[]{"IRON_SWORD", "NETHERITE_SWORD", "DIAMOND_AXE", "MACE", "TRIDENT", "BOW", "CROSSBOW"}) {
            Check.eq(Block.WEAPON, LockRule.check(Stance.CASTER, m, false, false, true, true), "caster locks " + m);
            Check.eq(Block.NONE, LockRule.check(Stance.MARTIAL, m, false, false, true, true), "fighter may hold " + m);
            Check.eq(Block.NONE, LockRule.check(Stance.NONE, m, false, false, true, true), "no stance may hold " + m);
        }
        for (String m : new String[]{"SHIELD", "IRON_PICKAXE", "STONE_SHOVEL", "GOLDEN_HOE", "STICK", "AIR", "BREAD", "TORCH"}) {
            Check.eq(Block.NONE, LockRule.check(Stance.CASTER, m, false, false, true, true), "caster may hold " + m);
        }
        Check.eq(Block.NONE, LockRule.check(Stance.CASTER, null, false, false, true, true), "caster empty hand ok");
        Check.eq(Block.NONE, LockRule.check(Stance.CASTER, "GOLDEN_AXE", true, true, true, true), "a Magic wand shaped like an axe is not a weapon");
        Check.eq(Block.NONE, LockRule.check(Stance.CASTER, "IRON_SWORD", true, false, true, true), "a Magic spell icon shaped like a sword is not a weapon");
    }

    static void wandLock() {
        Check.eq(Block.WAND, LockRule.check(Stance.MARTIAL, "STICK", true, true, true, true), "fighter locks a wand");
        Check.eq(Block.NONE, LockRule.check(Stance.MARTIAL, "STICK", false, false, true, true), "fighter may hold a plain stick");
        Check.eq(Block.NONE, LockRule.check(Stance.MARTIAL, "GLISTERING_MELON_SLICE", true, false, true, true), "fighter: spell icon is not a wand (PreCast backstop)");
        Check.eq(Block.NONE, LockRule.check(Stance.CASTER, "STICK", true, true, true, true), "caster may hold a wand");
        Check.eq(Block.NONE, LockRule.check(Stance.NONE, "STICK", true, true, true, true), "no stance may hold a wand");
    }

    static void knobs() {
        Check.eq(Block.NONE, LockRule.check(Stance.CASTER, "IRON_SWORD", false, false, false, true), "weapon-lock false = no weapon lock");
        Check.eq(Block.NONE, LockRule.check(Stance.MARTIAL, "STICK", true, true, true, false), "wand-lock false = no wand lock");
        Check.eq(Block.WAND, LockRule.check(Stance.MARTIAL, "STICK", true, true, false, true), "wand-lock independent of weapon-lock");
        Check.eq(Block.WEAPON, LockRule.check(Stance.CASTER, "IRON_SWORD", false, false, true, false), "weapon-lock independent of wand-lock");
    }

    static void epochs() {
        AttunementState s = new AttunementState(30, 10);
        Check.eq(0L, s.epoch(), "epoch starts 0");
        s.onCast(SpellClass.COMBAT, T0);
        Check.eq(1L, s.epoch(), "caster SET bumps epoch");
        s.onCast(SpellClass.COMBAT, T0 + 5_000);
        Check.eq(1L, s.epoch(), "caster REFRESH keeps epoch");
        s.onHostileDamageTaken(T0 + 6_000);
        Check.eq(1L, s.epoch(), "taken-damage refresh keeps epoch");
        Check.eq(Stance.CASTER, s.activeStance(T0 + 35_999), "window still 30s from last refresh");
        Check.eq(Stance.NONE, s.activeStance(T0 + 36_000), "lock clears when the window ends");
        Check.ok(s.pollExpired(T0 + 36_000), "expiry polled once");
        s.onMartialHit(T0 + 40_000);
        Check.eq(2L, s.epoch(), "martial SET bumps epoch");
        Check.eq(Stance.MARTIAL, s.activeStance(T0 + 40_000), "martial after caster lapsed");
        // Unchanged design pins (WRV-088): 30s window, 10s combat context.
        AttunementState t = new AttunementState(30, 10);
        t.onDamageDealt(T0);
        Check.eq(AttunementState.CastResult.SET, t.onCast(SpellClass.SUPPORT, T0 + 10_000), "support at 10s still in combat");
        AttunementState u = new AttunementState(30, 10);
        u.onDamageDealt(T0);
        Check.eq(AttunementState.CastResult.NOT_QUALIFYING, u.onCast(SpellClass.SUPPORT, T0 + 10_001), "support after 10s never attunes");
        AttunementState v = new AttunementState(30, 10);
        Check.eq(AttunementState.CastResult.NOT_QUALIFYING, v.onCast(SpellClass.SUPPORT, T0), "out-of-combat support never attunes");
        Check.eq(Stance.NONE, v.activeStance(T0), "out-of-combat support leaves no lock");
    }

    static void feedback() {
        LockFeedback f = new LockFeedback();
        Check.ok(f.takeTitle(1L), "first blocked attempt in a stance shows the title");
        Check.ok(!f.takeTitle(1L), "second blocked attempt in the same stance: no title");
        Check.ok(f.takeTitle(2L), "new stance shows the title again");
        Check.ok(f.takeQuiet(T0), "first quiet (pickup) feedback shows");
        Check.ok(!f.takeQuiet(T0 + 999), "quiet feedback throttled inside 1s");
        Check.ok(f.takeQuiet(T0 + 1000), "quiet feedback again after 1s");
        Check.ok(f.takeSound(T0), "first sound plays");
        Check.ok(!f.takeSound(T0 + 399), "sound throttled inside 400ms");
        Check.ok(f.takeSound(T0 + 400), "sound plays again after 400ms");
    }

    static void text() {
        Check.eq("Caster stance: weapons locked 24s", StanceText.actionBar(Stance.CASTER, 24), "caster action bar text");
        Check.eq("Fighter stance: magic locked 18s", StanceText.actionBar(Stance.MARTIAL, 18), "fighter action bar text");
        Check.eq("Stance clear", StanceText.actionBar(Stance.NONE, 0), "clear text");
        Check.ok(StanceText.actionBar(Stance.CASTER, 30).length() <= 34, "caster line fits a phone (<=34 chars)");
        Check.ok(StanceText.actionBar(Stance.MARTIAL, 30).length() <= 34, "fighter line fits a phone (<=34 chars)");
        Check.eq("Weapons locked", StanceText.title(Block.WEAPON), "weapon title");
        Check.eq("Magic locked", StanceText.title(Block.WAND), "wand title");
    }
}
