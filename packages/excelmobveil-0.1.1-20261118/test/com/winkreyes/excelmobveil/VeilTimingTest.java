package com.winkreyes.excelmobveil;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Reconstructed source; results must come from a fresh run, not a prior candidate. */
class VeilTimingTest {
    @Test void eachBottleAddsEightRealTimeMinutes() {
        assertEquals(480_000L, VeilTiming.DURATION_MILLIS);
        assertEquals(480_000L, VeilTiming.remaining(1_000L,481_000L,1_000L));
        assertEquals(123L, VeilTiming.remaining(1_000L,481_000L,480_877L));
    }
    @Test void exactDeadlineAndLaterAreInactive() {
        assertEquals(0,VeilTiming.remaining(1_000L,481_000L,481_000L));
        assertEquals(0,VeilTiming.remaining(1_000L,481_000L,Long.MAX_VALUE));
    }
    @Test void missingOrMalformedRecordsFailClosed() {
        assertEquals(0,VeilTiming.remaining(null,481_000L,1_000));
        assertEquals(0,VeilTiming.remaining(1_000L,null,1_000));
        assertEquals(0,VeilTiming.remaining(-1L,100L,50));
        assertEquals(0,VeilTiming.remaining(100L,100L,100));
        assertEquals(0,VeilTiming.remaining(100L,99L,100));
    }
    @Test void backwardsClockFailsClosedRatherThanExtendingTime() {
        assertEquals(0,VeilTiming.remaining(1_000L,481_000L,999));
    }
    @Test void offlineAndRestartDoNotResetDeadline() {
        assertEquals(300_000L,VeilTiming.remaining(10_000L,490_000L,190_000L));
        assertEquals(60_000L,VeilTiming.remaining(10_000L,490_000L,430_000L));
        assertEquals(0,VeilTiming.remaining(10_000L,490_000L,500_000L));
    }
    @Test void largeValidTimestampsDoNotOverflowRemainingTime() {
        long start=Long.MAX_VALUE-480_000L;
        assertEquals(480_000L,VeilTiming.remaining(start,Long.MAX_VALUE,start));
        assertEquals(Long.MAX_VALUE-1,VeilTiming.remaining(0L,Long.MAX_VALUE,1));
        assertEquals(0,VeilTiming.remaining(Long.MAX_VALUE,Long.MIN_VALUE,Long.MAX_VALUE));
    }
    @Test void ticksRoundUpAndRepresentStackedDuration() {
        assertEquals(1,VeilTiming.ticks(0)); assertEquals(1,VeilTiming.ticks(1));
        assertEquals(1,VeilTiming.ticks(50)); assertEquals(2,VeilTiming.ticks(51));
        assertEquals(9_600,VeilTiming.ticks(480_000)); assertEquals(19_200,VeilTiming.ticks(960_000));
    }
    @Test void accumulatedDeadlineHasNoArbitraryEightMinuteCap() {
        assertEquals(960_000L,VeilTiming.remaining(1_000L,961_000L,1_000L));
        assertEquals(480_001L,VeilTiming.remaining(0L,480_001L,0L));
        assertEquals(4_800_000_000L,VeilTiming.remaining(0L,4_800_000_000L,0L));
    }
    @Test void freshAndSequentialExtensionsAddExactlyEightMinutes() {
        assertEquals(481_000L,VeilTiming.extension(null,1_000));
        assertEquals(961_000L,VeilTiming.extension(481_000L,1_000));
        assertEquals(961_000L,VeilTiming.extension(481_000L,121_000));
        long expiry=1_000;
        for(int i=0;i<10_000;i++) expiry=VeilTiming.extension(expiry,1_000);
        assertEquals(4_800_001_000L,expiry);
    }
    @Test void expiredDeadlineStartsFreshAtCurrentTime() {
        assertEquals(980_000L,VeilTiming.extension(481_000L,500_000));
        assertEquals(980_000L,VeilTiming.extension(null,500_000));
    }
    @Test void exactAdditionAllowsMaximumDeadlineAndRejectsOverflow() {
        assertEquals(Long.MAX_VALUE,VeilTiming.extension(Long.MAX_VALUE-480_000L,0));
        assertThrows(ArithmeticException.class,()->VeilTiming.extension(Long.MAX_VALUE-479_999L,0));
        assertThrows(ArithmeticException.class,()->VeilTiming.extension(Long.MAX_VALUE,1_000));
        assertThrows(ArithmeticException.class,()->VeilTiming.extension(null,Long.MAX_VALUE));
    }
    @Test void tickRepresentationClampsWithoutOverflowingDeadline() {
        long boundary=(long)Integer.MAX_VALUE*50;
        assertEquals(Integer.MAX_VALUE,VeilTiming.ticks(boundary));
        assertEquals(Integer.MAX_VALUE,VeilTiming.ticks(boundary+1));
        assertEquals(Integer.MAX_VALUE,VeilTiming.ticks(Long.MAX_VALUE));
        assertEquals(1,VeilTiming.ticks(Long.MIN_VALUE));
    }
    @Test void negativeClockCannotStartOrExtendDuration() {
        assertThrows(IllegalArgumentException.class,()->VeilTiming.extension(null,-1));
        assertThrows(IllegalArgumentException.class,()->VeilTiming.extension(481_000L,-1));
    }
}
