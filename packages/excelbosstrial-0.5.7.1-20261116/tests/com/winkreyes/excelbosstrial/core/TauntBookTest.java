/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Boss taunts (0.4.0 rebuild, Mike Nov 8, 2026): the speaking
 * gate. The book is only ever called on a real eat (the
 * adapter owns that truth rule); here the rate limit, the
 * first-eat-always rule, and the no-immediate-repeat picker
 * are pinned.
 */
public final class TauntBookTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        List<String> lines = List.of("MORE.", "Feed me.", "The storm hungers.");
        UUID boss = UUID.randomUUID();
        long cd = 25_000L;

        // --- trigger gating: no lines, no voice (the adapter
        // likewise never calls the book without an eat at all)
        TauntBook dark = new TauntBook();
        check(dark.onEat(boss, 1_000L, List.of(), cd, () -> 0).isEmpty(),
                "an empty line list never speaks");
        check(dark.onEat(boss, 2_000L, null, cd, () -> 0).isEmpty(),
                "a null line list never speaks");

        // --- the first eat of the fight always speaks
        TauntBook book = new TauntBook();
        Optional<String> first = book.onEat(boss, 10_000L, lines, cd, () -> 0);
        check(first.isPresent() && first.get().equals("MORE."),
                "the first eat speaks (picker draw 0)");

        // --- a burst of eats inside the cooldown = one line
        check(book.onEat(boss, 10_001L, lines, cd, () -> 1).isEmpty(),
                "an eat 1 ms later is swallowed");
        check(book.onEat(boss, 20_000L, lines, cd, () -> 1).isEmpty(),
                "an eat mid-cooldown is swallowed");
        check(book.onEat(boss, 34_999L, lines, cd, () -> 1).isEmpty(),
                "an eat 1 ms before the cooldown ends is swallowed");
        check(book.eats(boss) == 4,
                "swallowed eats are still counted (1 spoken + 3 swallowed)");

        // --- after the cooldown the next eat speaks, and the
        // picker cannot repeat the line just spoken
        Optional<String> second = book.onEat(boss, 35_000L, lines, cd, () -> 0);
        check(second.isPresent() && second.get().equals("Feed me."),
                "at the cooldown boundary an eat speaks; a colliding draw "
                        + "advances past the previous line");
        Optional<String> third = book.onEat(boss, 60_000L, lines, cd, () -> 1);
        check(third.isPresent() && third.get().equals("The storm hungers."),
                "the no-repeat rule holds on the next collision too");

        // --- a one-line list may repeat (nothing else to say)
        TauntBook solo = new TauntBook();
        check(solo.onEat(boss, 0L, List.of("MORE."), cd, () -> 0)
                        .orElse("").equals("MORE."),
                "a one-line list speaks its line");
        check(solo.onEat(boss, cd, List.of("MORE."), cd, () -> 0)
                        .orElse("").equals("MORE."),
                "a one-line list repeats after the cooldown (no alternative)");

        // --- an out-of-range draw wraps instead of crashing
        TauntBook wrap = new TauntBook();
        check(wrap.onEat(boss, 0L, lines, cd, () -> -7).isPresent(),
                "a negative picker draw floor-mods into range");

        // --- close forgets the fight: the next eat is a FIRST
        // eat again and speaks at once
        book.close(boss);
        check(book.eats(boss) == 0, "close forgets the eat count");
        check(book.onEat(boss, 60_001L, lines, cd, () -> 2).isPresent(),
                "after close, the next eat speaks immediately (a new fight)");

        // --- the prune sweep keeps only live bosses
        UUID other = UUID.randomUUID();
        book.onEat(other, 0L, lines, cd, () -> 0);
        book.retainOnly(Set.of(other));
        check(book.eats(boss) == 0 && book.eats(other) == 1,
                "retainOnly prunes closed encounters and keeps live ones");

        System.out.println("TauntBookTest: " + checks + " checks, "
                + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
