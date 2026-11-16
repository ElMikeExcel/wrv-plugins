/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Damage-feedback settings (0.3.6): the settled defaults, the
 * per-chapter override independence, and the sanitize rules - a
 * junk value must degrade to the settled behavior, never to a
 * silent off and never to a crash.
 */
public final class FeedbackSettingsTest {

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
        // --- Settled defaults (Mike, Nov 2, 2026, option (e)).
        FeedbackSettings d = FeedbackSettings.defaults();
        check(d.poolNumbers(), "default: pool numbers ON");
        check(FeedbackSettings.MODE_ACTIONBAR.equals(d.damageTotalMode()),
                "default: damage totals on the action bar");
        check(FeedbackSettings.DEFAULT_TITLE_FORMAT.equals(d.titleFormat()),
                "default: settled title format");
        check(FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT.equals(d.damageTotalFormat()),
                "default: settled damage-total format");
        check(d.viewerRange() == 64, "default: viewer range 64");
        check(d.damageTotalMinIntervalMs() == 500, "default: action-bar interval 500 ms");
        check(d.titleTickMs() == 1000, "default: title sweep cadence 1000 ms");
        check(!d.dark() && d.poolNumbersOn() && d.actionBarOn(),
                "default: fully live, not dark");

        // --- Per-chapter resolution: no overrides = the globals,
        // for every chapter.
        for (BossKind kind : BossKind.values()) {
            check(d.withOverrides(null, null).equals(d),
                    "chapter " + kind + ": no overrides inherits the globals whole");
        }

        // --- Overrides are independent: each replaces alone.
        FeedbackSettings poolOff = d.withOverrides(false, null);
        check(!poolOff.poolNumbers() && poolOff.actionBarOn(),
                "override pool-numbers off alone: totals still on");
        FeedbackSettings totalsOff = d.withOverrides(null, "off");
        check(totalsOff.poolNumbers() && !totalsOff.actionBarOn() && totalsOff.dark() == false,
                "override damage-total off alone: pool numbers still on");
        check(d.withOverrides(false, "off").dark(),
                "both overridden off: the chapter is dark");
        check(d.withOverrides(true, "actionbar").equals(d),
                "overrides restating the globals change nothing");
        check(FeedbackSettings.MODE_ACTIONBAR.equals(
                        d.withOverrides(null, "banana").damageTotalMode()),
                "a chapter's junk mode reads as the settled default, not a silent off");

        // --- Sanitize: junk degrades to settled behavior.
        check(FeedbackSettings.MODE_ACTIONBAR.equals(FeedbackSettings.sanitized(
                        true, "banana", "x", "x", 1, 0, 1).damageTotalMode()),
                "sanitize: damage-total 'banana' -> actionbar");
        check(FeedbackSettings.MODE_OFF.equals(FeedbackSettings.sanitized(
                        true, "OFF", "x", "x", 1, 0, 1).damageTotalMode()),
                "sanitize: damage-total 'OFF' -> off (case-insensitive)");
        check(FeedbackSettings.MODE_ACTIONBAR.equals(FeedbackSettings.sanitized(
                        true, null, "x", "x", 1, 0, 1).damageTotalMode()),
                "sanitize: damage-total null -> actionbar");
        check(FeedbackSettings.sanitized(true, "off", "x", "x", -5, 0, 1).viewerRange() == 64,
                "sanitize: viewer range -5 -> 64");
        check(FeedbackSettings.sanitized(true, "off", "x", "x", 0, 0, 1).viewerRange() == 64,
                "sanitize: viewer range 0 -> 64");
        check(FeedbackSettings.sanitized(true, "off", "x", "x", 99999, 0, 1).viewerRange()
                        == FeedbackSettings.MAX_VIEWER_RANGE,
                "sanitize: viewer range is capped");
        check(FeedbackSettings.sanitized(true, "off", "x", "x", 1, -1, 1)
                        .damageTotalMinIntervalMs() == 500,
                "sanitize: interval -1 -> 500");
        check(FeedbackSettings.sanitized(true, "off", "x", "x", 1, 0, 1)
                        .damageTotalMinIntervalMs() == 0,
                "sanitize: interval 0 is a real choice (no throttle)");
        check(FeedbackSettings.sanitized(true, "off", "x", "x", 1, 0, 0).titleTickMs() == 1000,
                "sanitize: title tick 0 -> 1000");
        check(FeedbackSettings.DEFAULT_TITLE_FORMAT.equals(FeedbackSettings.sanitized(
                        true, "off", "  ", "x", 1, 0, 1).titleFormat()),
                "sanitize: blank title format -> settled default");
        check(FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT.equals(FeedbackSettings.sanitized(
                        true, "off", "x", null, 1, 0, 1).damageTotalFormat()),
                "sanitize: null damage-total format -> settled default");

        // --- The pool-numbers switch from a raw config value:
        // junk reads as the settled ON, never a silent off.
        check(!FeedbackSettings.poolNumbersValue(Boolean.FALSE),
                "pool-numbers: Boolean false reads false");
        check(FeedbackSettings.poolNumbersValue(Boolean.TRUE),
                "pool-numbers: Boolean true reads true");
        check(!FeedbackSettings.poolNumbersValue("false"),
                "pool-numbers: 'false' reads false");
        check(FeedbackSettings.poolNumbersValue("true"),
                "pool-numbers: 'true' reads true");
        check(FeedbackSettings.poolNumbersValue("junk"),
                "pool-numbers: junk string reads as the default ON");
        check(FeedbackSettings.poolNumbersValue(null),
                "pool-numbers: a missing key reads as the default ON");
        check(FeedbackSettings.poolNumbersValue(42),
                "pool-numbers: a non-boolean reads as the default ON");

        // --- The damage-total mode from a raw config value
        // (rig finding F4): in YAML an unquoted `off` - the
        // documented way to write this setting - parses as the
        // boolean false, and a typed string read would hand
        // back null and silently resurrect the action bar.
        check(FeedbackSettings.MODE_OFF.equals(
                        FeedbackSettings.damageTotalModeValue(Boolean.FALSE)),
                "mode value: Boolean false (an unquoted YAML off) reads off");
        check(FeedbackSettings.MODE_ACTIONBAR.equals(
                        FeedbackSettings.damageTotalModeValue(Boolean.TRUE)),
                "mode value: Boolean true reads as the settled actionbar");
        check(FeedbackSettings.MODE_OFF.equals(
                        FeedbackSettings.damageTotalModeValue("off")),
                "mode value: the string 'off' passes through");
        check("banana".equals(FeedbackSettings.damageTotalModeValue("banana")),
                "mode value: a junk string passes through for sanitize to judge");
        check(FeedbackSettings.damageTotalModeValue(42) == null,
                "mode value: a non-string non-boolean reads as absent");
        check(FeedbackSettings.damageTotalModeValue(null) == null,
                "mode value: a missing key reads as absent");
        check(!FeedbackSettings.sanitized(true,
                        FeedbackSettings.damageTotalModeValue(Boolean.FALSE),
                        "x", "x", 1, 0, 1).actionBarOn(),
                "mode value: the full unquoted-off path lands dark on totals");

        System.out.println("FeedbackSettingsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
