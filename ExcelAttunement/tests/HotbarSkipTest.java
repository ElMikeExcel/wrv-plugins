import com.winkreyes.excelattunement.core.HotbarSkip;
import java.util.Set;
import java.util.function.IntPredicate;

/** 0.1.7: every hotbar slot stays reachable by scrolling, whatever is locked. */
public class HotbarSkipTest {
    static IntPredicate lockedAt(Integer... s) { Set<Integer> set = Set.of(s); return set::contains; }

    /** Scroll one way from `start` up to 30 steps with the hop rule; return the set of slots visited. */
    static Set<Integer> scrollAll(int start, int dir, IntPredicate locked) {
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        int cur = start; seen.add(cur);
        for (int i = 0; i < 30; i++) {
            int next = Math.floorMod(cur + dir, 9);
            if (locked.test(next)) { int t = HotbarSkip.target(cur, next, locked); if (t == HotbarSkip.REFUSE) break; next = t; }
            Check.ok(!locked.test(next), "never lands on a locked slot (" + cur + "->" + next + ")");
            cur = next; seen.add(cur);
        }
        return seen;
    }

    public static void run() {
        System.out.println("== HotbarSkipTest ==");
        Check.eq(1, HotbarSkip.direction(3, 4), "3->4 is a scroll right");
        Check.eq(-1, HotbarSkip.direction(4, 3), "4->3 is a scroll left");
        Check.eq(1, HotbarSkip.direction(8, 0), "8->0 wraps right");
        Check.eq(-1, HotbarSkip.direction(0, 8), "0->8 wraps left");
        Check.eq(0, HotbarSkip.direction(0, 4), "0->4 is a jump (number key / tap)");
        Check.eq(0, HotbarSkip.direction(2, 2), "same slot is no move");
        Check.eq(0, HotbarSkip.direction(-1, 2), "bad slot is no move");
        // The reported layout: wand in slot 5 (index 4), potions in 1 and 9 (index 0 and 8).
        IntPredicate wand = lockedAt(4);
        Check.eq(5, HotbarSkip.target(3, 4, wand), "scroll right onto the wand hops to the next slot");
        Check.eq(3, HotbarSkip.target(5, 4, wand), "scroll left onto the wand hops to the previous slot");
        Check.eq(HotbarSkip.REFUSE, HotbarSkip.target(0, 4, wand), "number key straight onto the wand is still refused");
        for (int start : new int[]{0, 3, 5, 8}) for (int dir : new int[]{1, -1}) {
            Set<Integer> seen = scrollAll(start, dir, wand);
            Check.eq(8, seen.size(), "from slot " + (start + 1) + " scrolling " + (dir > 0 ? "right" : "left") + " reaches all 8 other slots");
            Check.ok(seen.contains(0) && seen.contains(8), "potions in slot 1 and 9 reachable from slot " + (start + 1));
        }
        // Two wands side by side, and wands at the wrap edge.
        IntPredicate two = lockedAt(4, 5);
        Check.eq(6, HotbarSkip.target(3, 4, two), "two locked slots in a row: hop both");
        Check.eq(3, HotbarSkip.target(6, 5, two), "two locked slots, leftward: hop both");
        IntPredicate edge = lockedAt(8, 0);
        Check.eq(1, HotbarSkip.target(7, 8, edge), "locked 9 and 1: scroll right from 8 lands on 2 (wraps)");
        Check.eq(7, HotbarSkip.target(1, 0, edge), "locked 1 and 9: scroll left from 2 lands on 8 (wraps)");
        Check.eq(7, scrollAll(3, 1, edge).size(), "edge locks: all 7 unlocked slots reachable");
        // Everything else locked: nowhere to go, refuse (player stays put).
        IntPredicate allButOne = s -> s != 2;
        Check.eq(HotbarSkip.REFUSE, HotbarSkip.target(2, 3, allButOne), "all other slots locked: stay put");
        Check.eq(HotbarSkip.REFUSE, HotbarSkip.target(2, 1, allButOne), "all other slots locked, leftward: stay put");
        // Never returns the old slot or a locked one.
        for (int o = 0; o < 9; o++) for (int n = 0; n < 9; n++) {
            final int oo = o;
            IntPredicate lk = s -> s == Math.floorMod(oo + 1, 9) || s == Math.floorMod(oo - 1, 9);
            int t = HotbarSkip.target(o, n, lk);
            if (t != HotbarSkip.REFUSE) { Check.ok(t != o && !lk.test(t), "target " + o + "->" + n + " is a fresh unlocked slot"); }
        }
    }
}
