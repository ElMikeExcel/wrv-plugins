package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** ExcelMagicLevel — window bookkeeping semantics (SPEC §4.2/§4.3, §7.1). */
public class AntiAbuseWindowsTest {
    private final UUID player = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @Test
    public void firstCastHasNoElapsed() {
        AntiAbuseWindows w = new AntiAbuseWindows();
        assertNull(w.spellElapsed(player, "heal", 1_000_000L));
        assertFalse(w.isTargetRepeat(player, target, 1_000_000L, 30_000L));
    }

    @Test
    public void recordPayStartsTheClock() {
        AntiAbuseWindows w = new AntiAbuseWindows();
        w.recordPay(player, "heal", target, 1_000_000L);
        assertEquals(Long.valueOf(5_000L), w.spellElapsed(player, "heal", 1_005_000L));
        assertNull(w.spellElapsed(player, "cure", 1_005_000L));       // per-spell
        assertNull(w.spellElapsed(other, "heal", 1_005_000L));        // per-player
    }

    @Test
    public void targetRepeatInsideWindowOnly() {
        AntiAbuseWindows w = new AntiAbuseWindows();
        w.recordPay(player, "heal", target, 1_000_000L);
        assertTrue(w.isTargetRepeat(player, target, 1_029_999L, 30_000L));
        assertFalse(w.isTargetRepeat(player, target, 1_030_000L, 30_000L)); // window over
        assertFalse(w.isTargetRepeat(player, UUID.randomUUID(), 1_001_000L, 30_000L)); // different target
        assertFalse(w.isTargetRepeat(player, null, 1_001_000L, 30_000L));
    }

    @Test
    public void spellWithoutTargetStillRecordsSpellWindow() {
        AntiAbuseWindows w = new AntiAbuseWindows();
        w.recordPay(player, "recall", null, 500L);
        assertEquals(Long.valueOf(100L), w.spellElapsed(player, "recall", 600L));
        assertFalse(w.isTargetRepeat(player, target, 600L, 30_000L));
    }

    @Test
    public void forgetClearsOnlyThatPlayer() {
        AntiAbuseWindows w = new AntiAbuseWindows();
        w.recordPay(player, "heal", target, 1_000L);
        w.recordPay(other, "heal", target, 1_000L);
        w.forget(player);
        assertNull(w.spellElapsed(player, "heal", 2_000L));
        assertEquals(Long.valueOf(1_000L), w.spellElapsed(other, "heal", 2_000L));
    }
}
