package com.winkreyes.excelwandbars.core;

/**
 * 0.3.7: how a spell's place on the bar is named (Mike, Nov 1, 2026). Every
 * hotbar row is a BAR and every position in it is a SLOT, and a position is
 * always written with both: "Bar 1 · Slot 1". The old labels printed the
 * flattened loadout index as a bar number ("bar 17" for the spell at loadout
 * index 16, which is really bar 3, slot 3 on a 7-wide bar) — the same defect
 * in the Spell Book, the picker and the result lines.
 *
 * The input is the loadout's flat slot index and the bar width (perPage) of
 * the player's style; both outputs are 1-based, matching how players count.
 * The format string is a config key (tap-bar.position-format), held here so
 * every surface formats identically; the settled default is
 * {@link #DEFAULT_TEMPLATE}. Pure arithmetic plus that one holder, so the
 * mapping is unit-pinned.
 */
public final class BarPosition {
    private BarPosition() {}

    public static final String DEFAULT_TEMPLATE = "Bar {bar} · Slot {slot}";

    private static volatile String template = DEFAULT_TEMPLATE;

    /** Config load (and tests): a null/blank value restores the settled default. */
    public static void setTemplate(String t) {
        template = t == null || t.isBlank() ? DEFAULT_TEMPLATE : t.trim();
    }

    public static String template() { return template; }

    /** 1-based bar (row) of a flat loadout slot. */
    public static int barOf(int flatSlot, int perPage) {
        int pp = Math.max(1, perPage);
        return Math.max(0, flatSlot) / pp + 1;
    }

    /** 1-based slot (position within its bar) of a flat loadout slot. */
    public static int slotOf(int flatSlot, int perPage) {
        int pp = Math.max(1, perPage);
        return Math.max(0, flatSlot) % pp + 1;
    }

    /** Format with the configured template. */
    public static String format(int flatSlot, int perPage) {
        return format(template, flatSlot, perPage);
    }

    /** Format with an explicit template (tests, one-off callers). */
    public static String format(String template, int flatSlot, int perPage) {
        String t = template == null || template.isBlank() ? DEFAULT_TEMPLATE : template;
        return t.replace("{bar}", Integer.toString(barOf(flatSlot, perPage)))
                .replace("{slot}", Integer.toString(slotOf(flatSlot, perPage)));
    }
}
