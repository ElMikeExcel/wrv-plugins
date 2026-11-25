/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * One chapter's milestone-beat settings (ExcelBossTrial 0.3.2):
 * the per-chapter switches the adjustability law requires, read
 * adapter-side from the chapter's {@code milestones:} config
 * block (the fight-tracking precedent - no core verdict rule
 * consumes them, so BossTrialConfig's canonical record stays
 * untouched).
 *
 * Switch semantics, each independent:
 * <ul>
 *   <li>{@code enabled: false} - the whole feature is off for
 *       the chapter: no lines, no surges.</li>
 *   <li>an empty text for a threshold - that threshold's line is
 *       off; the beat still counts (and still surges).</li>
 *   <li>{@code surge-multiplier: 0} - no surge; the lines still
 *       fire. At the default 1.0 the surge is exactly one extra
 *       pulse at the chapter's existing pulse-damage (under the
 *       ramp, like any pulse).</li>
 * </ul>
 * Both text and surge off for every threshold = the feature is
 * off in effect, whatever {@code enabled} says.
 */
public record MilestoneSettings(boolean enabled, double surgeMultiplier,
                                String text75, String text50, String text25) {

    /** Sanitized construction: a NaN multiplier reads as the
     *  default 1.0, a negative one clamps to 0 (surge off), and
     *  null texts read as empty (line off). */
    public static MilestoneSettings sanitized(boolean enabled, double surgeMultiplier,
                                              String text75, String text50, String text25) {
        double mult = Double.isNaN(surgeMultiplier) || Double.isInfinite(surgeMultiplier)
                ? 1.0 : Math.max(0.0, surgeMultiplier);
        return new MilestoneSettings(enabled, mult,
                text75 == null ? "" : text75,
                text50 == null ? "" : text50,
                text25 == null ? "" : text25);
    }

    /** The line for a threshold bit (0 = 75%, 1 = 50%, 2 = 25%). */
    public String textFor(int bitIndex) {
        return switch (bitIndex) {
            case 0 -> text75;
            case 1 -> text50;
            case 2 -> text25;
            default -> "";
        };
    }

    /** True when at least one threshold has a line to say. */
    public boolean anyText() {
        return !text75.isEmpty() || !text50.isEmpty() || !text25.isEmpty();
    }

    /**
     * The settled default wording per chapter ({name} is the
     * boss's display name). The lines speak in SPENT fractions -
     * at 75% remaining a quarter of the fight is spent - and the
     * warden's read as progress, because the warden has no
     * vanilla health bar: these lines are its progress display
     * (SEAL-CHALLENGE-AUDIT section 3).
     */
    public static MilestoneSettings defaults(BossKind kind) {
        return switch (kind) {
            case WITHER -> sanitized(true, 1.0,
                    "&5{name} &7reels - a quarter of the fight is spent.",
                    "&5{name} &7staggers - half the fight is spent.",
                    "&5{name} &7falters - three quarters spent. The end is in sight.");
            case ELDER_GUARDIAN -> sanitized(true, 1.0,
                    "&3{name} &7falters - a quarter of the fight is spent.",
                    "&3{name} &7staggers - half the fight is spent.",
                    "&3{name} &7reels - three quarters spent. The tide is turning.");
            case WARDEN -> sanitized(true, 1.0,
                    "&8The Warden &7falters - a quarter of the fight is spent.",
                    "&8The Warden &7staggers - half the fight is spent.",
                    "&8The Warden &7reels - three quarters spent. The deep is losing its grip.");
            case ENDER_DRAGON -> sanitized(true, 1.0,
                    "&5{name} &7falters - a quarter spent.",
                    "&5{name} &7staggers - half spent.",
                    "&5{name} &7reels - three quarters spent.");
        };
    }
}
