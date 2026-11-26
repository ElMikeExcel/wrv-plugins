package excelemfixes.bedrockabilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure composition of the abilities form from a live ClassView.
 *
 * Rules (each pinned by the unit suite):
 * - No class: the form says so plainly and offers only a Close button -
 *   never dead ability buttons.
 * - With a class: the body names the class, shows the live resource
 *   snapshot, and states every slot's position (ability + cost,
 *   "not enough yet", or "nothing learned yet"). Buttons exist only
 *   for slots that actually hold an ability; an unaffordable ability
 *   still gets its button (the resource refills; EliteMobs re-checks
 *   at fire time and answers in its own words).
 * - Button order follows slot order MOBILITY, SIGNATURE, UTILITY, with
 *   Close last; buttonSlots stays parallel, null on Close.
 */
public final class FormModelBuilder {

    public static final String TITLE = "Class Abilities";
    public static final String CLOSE_LABEL = "Close";

    private FormModelBuilder() {}

    public static FormModel build(ClassView view) {
        if (view == null || !view.hasClass()) {
            return new FormModel(TITLE,
                    "You don't have a class yet.\n"
                            + "Pick one with /em class - your class abilities will show up here.",
                    List.of(CLOSE_LABEL),
                    closeOnly());
        }

        StringBuilder body = new StringBuilder();
        String className = view.className() == null || view.className().isBlank()
                ? "Your class" : view.className().trim();
        body.append(className);
        if (view.resourceName() != null && view.resourceAmount() != null) {
            body.append(" - ").append(view.resourceName()).append(' ')
                    .append(Labels.number(view.resourceAmount()));
            if (view.resourceMax() != null) {
                body.append(" / ").append(Labels.number(view.resourceMax()));
            }
        }
        body.append('\n');

        List<String> buttons = new ArrayList<>();
        List<BridgeSlot> buttonSlots = new ArrayList<>();
        for (BridgeSlot slot : BridgeSlot.values()) {
            SlotView sv = view.slot(slot);
            body.append('\n').append(slotLine(sv));
            if (sv.present()) {
                buttons.add(buttonLabel(sv));
                buttonSlots.add(slot);
            }
        }
        body.append("\n\nTap an ability to use it. EliteMobs checks the cost when you tap.");

        buttons.add(CLOSE_LABEL);
        buttonSlots.add(null);
        return new FormModel(TITLE, body.toString(), buttons, buttonSlots);
    }

    private static List<BridgeSlot> closeOnly() {
        List<BridgeSlot> slots = new ArrayList<>();
        slots.add(null);
        return slots;
    }

    /** One body line describing a slot's state, e.g. "Signature: Provoke - 40 Resolve". */
    static String slotLine(SlotView sv) {
        String slotName = sv.slot().displayName();
        if (!sv.present()) {
            return slotName + ": nothing learned yet";
        }
        StringBuilder line = new StringBuilder(slotName).append(": ");
        line.append(sv.abilityName() == null ? "Ability" : sv.abilityName());
        if (sv.cost() != null) {
            line.append(" - ").append(Labels.number(sv.cost()));
            if (sv.resourceName() != null) {
                line.append(' ').append(sv.resourceName());
            }
            if (!sv.affordable()) {
                line.append(" (not enough yet)");
            }
        }
        return line.toString();
    }

    /** Button label for a live slot, e.g. "Provoke (Signature)". */
    static String buttonLabel(SlotView sv) {
        String name = sv.abilityName() == null ? "Ability" : sv.abilityName();
        return name + " (" + sv.slot().displayName() + ")";
    }
}
