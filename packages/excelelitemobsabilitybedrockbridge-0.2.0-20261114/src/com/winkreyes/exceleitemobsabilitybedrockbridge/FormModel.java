package com.winkreyes.exceleitemobsabilitybedrockbridge;

import java.util.List;

/**
 * A fully composed abilities form, ready to hand to the Cumulus
 * transport: title, body text, button labels, and - parallel to the
 * buttons - which slot each button fires (null = the Close button,
 * which fires nothing).
 */
public record FormModel(String title, String content,
                        List<String> buttons, List<BridgeSlot> buttonSlots) {

    public FormModel {
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
        // buttonSlots deliberately allows nulls (null = the Close button),
        // so it cannot use List.copyOf.
        buttonSlots = buttonSlots == null ? List.of()
                : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(buttonSlots));
    }

    /** The slot a clicked button fires, or null for Close / no button. */
    public BridgeSlot slotForButton(int index) {
        if (index < 0 || index >= buttonSlots.size()) {
            return null;
        }
        return buttonSlots.get(index);
    }
}
