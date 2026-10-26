package com.winkreyes.trash;

/**
 * Two-tap OK (pure, unit-tested). The first tap arms a 5 second confirm for the current item count;
 * a second tap inside the window with the same count deletes. If the count changed in between, the
 * second tap re-arms with the new count instead of deleting (so the number on the button is always
 * what gets deleted). An empty trash area never arms.
 */
public final class Confirm {
    public static final long WINDOW_MS = 5000L;
    public enum Result { EMPTY, ARMED, DELETE }
    private long until; private int count; private int token;

    public Result press(long now, int items) {
        if (items <= 0) { reset(); return Result.EMPTY; }
        if (until > now && count == items) { reset(); return Result.DELETE; }
        until = now + WINDOW_MS; count = items; token++;
        return Result.ARMED;
    }
    public boolean armed(long now) { return until > now; }
    public int count() { return count; }
    /** Changes on every arm and reset, so a delayed "go back to normal" task can tell it is stale. */
    public int token() { return token; }
    public void reset() { until = 0; count = 0; token++; }
}
