package com.winkreyes.excelmenu.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bedrock item-detail button mapping (0.1.2): the single
 * source the renderer, the unit tests, and the Cumulus
 * construction check all share, so the button a Bedrock player
 * sees and the click it dispatches can never drift apart.
 *
 * A modal item's detail form shows its action buttons in this
 * order, with Back always last:
 *
 *   [0] left label       -> the item's "left" click lines
 *   [1] right label      -> the item's "right" click lines
 *   [2] four label       -> the item's "shift-left" click lines
 *       (only when the item defines a four-label; the validator
 *        pairs the label with non-empty shift-left lines)
 *   [n] "Back"           -> re-opens the parent menu
 *
 * An item without a four-label composes exactly as it did in
 * 0.1.1 — [left, right, Back] — no phantom button. Labels are
 * returned raw (authored text); the renderer colorizes them.
 */
public final class BedrockDetail {

    /** The trailing button on every detail form. */
    public static final String BACK_LABEL = "Back";

    private BedrockDetail() {}

    /** The action-button labels in render order (Back excluded). */
    public static List<String> actionLabels(MenuDef.BedrockDef bedrock) {
        List<String> labels = new ArrayList<>();
        labels.add(bedrock.leftLabel());
        labels.add(bedrock.rightLabel());
        if (hasFour(bedrock)) labels.add(bedrock.fourLabel());
        return labels;
    }

    /** The click kind each action button dispatches, parallel to
     *  {@link #actionLabels}: index i of the form runs
     *  actionKinds().get(i); index actionKinds().size() is Back. */
    public static List<String> actionKinds(MenuDef.BedrockDef bedrock) {
        List<String> kinds = new ArrayList<>();
        kinds.add("left");
        kinds.add("right");
        if (hasFour(bedrock)) kinds.add("shift-left");
        return kinds;
    }

    /** Every button on the detail form, Back last. */
    public static List<String> buttonLabels(MenuDef.BedrockDef bedrock) {
        List<String> buttons = actionLabels(bedrock);
        buttons.add(BACK_LABEL);
        return buttons;
    }

    /** True when form index idx is the Back button. Out-of-range
     *  and closed-form indexes (negative) are not Back. */
    public static boolean isBack(MenuDef.BedrockDef bedrock, int index) {
        return index == actionKinds(bedrock).size();
    }

    /** The click kind for a form index, or null when the index
     *  is Back, negative, or beyond the composed button list. */
    public static String clickKind(MenuDef.BedrockDef bedrock, int index) {
        List<String> kinds = actionKinds(bedrock);
        return index >= 0 && index < kinds.size() ? kinds.get(index) : null;
    }

    private static boolean hasFour(MenuDef.BedrockDef bedrock) {
        return bedrock.fourLabel() != null && !bedrock.fourLabel().isBlank();
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
