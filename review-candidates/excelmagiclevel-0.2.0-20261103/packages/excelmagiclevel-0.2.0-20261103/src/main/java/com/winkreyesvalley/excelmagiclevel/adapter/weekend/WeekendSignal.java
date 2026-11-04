package com.winkreyesvalley.excelmagiclevel.adapter.weekend;

import java.time.Instant;

/**
 * ExcelMagicLevel — "is the weekend XP window active right now", behind an
 * adapter (SPEC §2: the same signal that drives mcMMO's weekend rate comes
 * from ExcelWeekendXP's rhythm, not from mcMMO itself).
 */
public interface WeekendSignal {
    boolean isActive(Instant now);
}
