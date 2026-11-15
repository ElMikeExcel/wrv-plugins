/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * One chapter's boss-taunt settings (ExcelBossTrial 0.4.0
 * rebuild; Mike's direct request, Nov 8, 2026): when the
 * Wither EATS - kills a non-player mob and regains health
 * from the kill, the vanilla kill-heal PM-1's heal-aware
 * ledger already observes - it speaks a line in chat to the
 * encounter's participants, styled as the boss talking.
 *
 * Truth rule: a line fires ONLY on a real eat. The adapter
 * calls the book solely from the boss regain handler when
 * the regain reason is WITHER (the game's own fused
 * kill+heal signal: the Wither regains 5 HP when it kills
 * an entity, and Bukkit reports exactly that heal with
 * reason WITHER) - never on a timer, never decoratively.
 *
 * Config-first, read adapter-side like the milestone and
 * sprinkle leaf settings: {@code <chapter>.taunts} (the
 * line list) and {@code <chapter>.taunt-cooldown-seconds}.
 * An ABSENT list keeps the chapter default; a PRESENT but
 * empty list silences the chapter (the milestone-line
 * precedent). Lines ship configured ONLY for the Wither;
 * every other chapter's default is an empty list, so the
 * mechanism stays dark there until a config says otherwise.
 */
public record TauntSettings(List<String> lines, int cooldownSeconds) {

    /** The settled gap between spoken lines (seconds): a
     *  fight may see a hundred eats; it hears at most one
     *  line per cooldown, plus the first eat of the fight,
     *  which always speaks. */
    public static final int DEFAULT_COOLDOWN_SECONDS = 25;

    /** The shipped Wither lines (Mike's slate, Nov 8, 2026):
     *  scary, family-safe. */
    public static final List<String> DEFAULT_WITHER_LINES = List.of(
            "MORE.",
            "Your little offerings only make me stronger.",
            "I hunger. Send me more.",
            "Every soul you send me is mine.",
            "Feed me, champions.",
            "Their strength is mine now.",
            "You brought me a feast.",
            "I will devour this world, one gift at a time.",
            "More meat for the storm.",
            "Yes… feed the storm.");

    /** Armed = at least one line to say. */
    public boolean enabled() {
        return !lines.isEmpty();
    }

    /**
     * Sanitized construction: a null list keeps the fallback's
     * lines (the key was absent); a present list is trimmed
     * with blanks dropped (a present-but-all-blank list is an
     * honored silence). The cooldown clamps to [0, 3600].
     */
    public static TauntSettings sanitized(List<String> lines, int cooldownSeconds,
                                          TauntSettings fallback) {
        List<String> out = new java.util.ArrayList<>();
        if (lines == null) {
            out.addAll(fallback.lines());
        } else {
            for (String s : lines) {
                if (s != null && !s.isBlank()) {
                    out.add(s.trim());
                }
            }
        }
        int cd = Math.max(0, Math.min(3600, cooldownSeconds));
        return new TauntSettings(List.copyOf(out), cd);
    }

    /** The settled defaults per chapter: the Wither speaks;
     *  every other chapter ships silent. */
    public static TauntSettings defaults(BossKind kind) {
        return switch (kind) {
            case WITHER -> new TauntSettings(DEFAULT_WITHER_LINES,
                    DEFAULT_COOLDOWN_SECONDS);
            case WARDEN, ENDER_DRAGON, ELDER_GUARDIAN ->
                    new TauntSettings(List.of(), DEFAULT_COOLDOWN_SECONDS);
        };
    }
}
