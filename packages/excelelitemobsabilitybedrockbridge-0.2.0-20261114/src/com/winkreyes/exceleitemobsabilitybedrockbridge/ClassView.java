package com.winkreyes.exceleitemobsabilitybedrockbridge;

import java.util.List;

/**
 * A player's EliteMobs class state as read live from the module, in the
 * bridge's own types. Nothing here is stored or mirrored by the bridge;
 * a fresh view is built on every /abilities invocation.
 *
 * @param hasClass      whether EliteMobs reports an active class
 * @param className     active form display name (e.g. "Paladin"), or null
 * @param resourceName  class resource display name (e.g. "Resolve"), or null
 * @param resourceAmount current resource amount, or null when unreported
 * @param resourceMax   resource pool maximum, or null when unreported
 * @param slots         exactly three SlotViews, ordered MOBILITY,
 *                      SIGNATURE, UTILITY
 */
public record ClassView(boolean hasClass, String className, String resourceName,
                        Double resourceAmount, Double resourceMax,
                        List<SlotView> slots) {

    public ClassView {
        slots = slots == null ? List.of() : List.copyOf(slots);
    }

    public static ClassView noClass() {
        return new ClassView(false, null, null, null, null,
                List.of(SlotView.absent(BridgeSlot.MOBILITY),
                        SlotView.absent(BridgeSlot.SIGNATURE),
                        SlotView.absent(BridgeSlot.UTILITY)));
    }

    public SlotView slot(BridgeSlot slot) {
        for (SlotView view : slots) {
            if (view.slot() == slot) {
                return view;
            }
        }
        return SlotView.absent(slot);
    }

    public boolean anyPresent() {
        for (SlotView view : slots) {
            if (view.present()) {
                return true;
            }
        }
        return false;
    }
}
