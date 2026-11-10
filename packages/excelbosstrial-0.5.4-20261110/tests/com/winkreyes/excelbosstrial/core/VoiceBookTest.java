/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Per-encounter voice state (0.5.2): the Warden boom gate
 * (rate-limited to the taunt cooldown, the two clips
 * alternating), the once-per-encounter open / last-door
 * guards, the missing-clip log-once set, and the prune shape
 * the encounter map imposes.
 */
public final class VoiceBookTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        VoiceSettings warden = VoiceSettings.defaults(BossKind.WARDEN);
        VoiceBook book = new VoiceBook();
        UUID boss = UUID.randomUUID();

        // --- Boom: first speaks, barrage swallowed, clips alternate.
        check(book.onBoom(boss, 0L, 25_000L, warden)
                        .equals(Optional.of("wrv:bosstrial.warden.boom")),
                "boom: the first boom of a fight always speaks (boom)");
        check(book.onBoom(boss, 1_000L, 25_000L, warden).isEmpty(),
                "boom: a second boom inside the taunt cooldown is silent");
        check(book.onBoom(boss, 24_999L, 25_000L, warden).isEmpty(),
                "boom: still silent one ms before the cooldown lapses");
        check(book.onBoom(boss, 25_000L, 25_000L, warden)
                        .equals(Optional.of("wrv:bosstrial.warden.boom_alt")),
                "boom: the next spoken boom alternates to boom_alt");
        check(book.onBoom(boss, 50_000L, 25_000L, warden)
                        .equals(Optional.of("wrv:bosstrial.warden.boom")),
                "boom: the third spoken boom alternates back to boom");
        check(book.boomsSpoken(boss) == 3, "boom: the spoken count tracks the alternation");
        check(book.boomsSpoken(UUID.randomUUID()) == 0, "boom: an unknown boss has spoken none");

        // --- Boom: one-clip and no-clip chapters.
        VoiceSettings boomOnly = VoiceSettings.sanitized(null, null, null, null, null,
                null, null, null, null, null, "", null, null, warden);
        VoiceBook single = new VoiceBook();
        UUID b2 = UUID.randomUUID();
        check(single.onBoom(b2, 0L, 0L, boomOnly)
                        .equals(Optional.of("wrv:bosstrial.warden.boom")),
                "boom: with boom-alt silenced, boom speaks");
        check(single.onBoom(b2, 1L, 0L, boomOnly)
                        .equals(Optional.of("wrv:bosstrial.warden.boom")),
                "boom: with boom-alt silenced, boom repeats (no phantom alt)");
        VoiceSettings silent = VoiceSettings.sanitized(null, null, null, null, null,
                null, null, null, null, "", "", null, null, warden);
        check(single.onBoom(b2, 2L, 0L, silent).isEmpty(),
                "boom: a chapter with no boom clips never speaks");
        check(single.onBoom(b2, 3L, 0L, null).isEmpty(),
                "boom: null settings are silence, not an exception");

        // --- Once guards: open and last-door.
        VoiceBook guards = new VoiceBook();
        UUID elder = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        check(guards.markOpened(elder), "guard: the open line fires the first time");
        check(!guards.markOpened(elder), "guard: the open line never fires twice per encounter");
        check(guards.markOpened(other), "guard: another boss's open is its own");
        check(guards.markLastDoor(elder), "guard: the last-door line fires the first time");
        check(!guards.markLastDoor(elder), "guard: the last-door line never fires twice");

        // --- Missing-clip log-once.
        check(guards.shouldLogMissing("wither:taunt:3"),
                "missing: a gap logs the first time it is hit");
        check(!guards.shouldLogMissing("wither:taunt:3"),
                "missing: the same gap never logs twice");
        check(guards.shouldLogMissing("wither:taunt:4"),
                "missing: a different slot is a different gap");

        // --- Prune shape: close resets the fight, retainOnly
        // keeps the live set, the log-once set is per-boot.
        guards.close(elder);
        check(guards.markOpened(elder), "prune: close forgets the boss - a new fight opens fresh");
        VoiceBook keep = new VoiceBook();
        UUID live = UUID.randomUUID();
        UUID gone = UUID.randomUUID();
        keep.markOpened(live);
        keep.markOpened(gone);
        keep.onBoom(gone, 0L, 0L, warden);
        keep.shouldLogMissing("elder:relay");
        keep.retainOnly(Set.of(live));
        check(!keep.markOpened(live), "prune: retainOnly keeps the live boss's guards");
        check(keep.markOpened(gone), "prune: retainOnly forgets the pruned boss");
        check(!keep.shouldLogMissing("elder:relay"),
                "prune: the missing-clip log set survives the sweep (per-boot, not per-fight)");
        check(keep.boomsSpoken(gone) == 0, "prune: a pruned boss's boom state is gone");

        System.out.println("VoiceBookTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
