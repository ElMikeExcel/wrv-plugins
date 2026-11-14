package com.winkreyes.exceleitemobsabilitybedrockbridge;

/**
 * One ability slot as read live from EliteMobs, in the bridge's own
 * types. A slot is "present" only when EliteMobs resolved a resource
 * snapshot for it (i.e. the player's class actually has an ability
 * there); a non-present slot never becomes a form button.
 *
 * @param slot        which of the three slots
 * @param present     whether the player's class has an ability in it
 * @param abilityName live ability name from EliteMobs (may be null when
 *                    not present); already display-resolved by the gateway
 * @param cost        EliteMobs' current cost for the ability, or null
 *                    when not present / not reported
 * @param affordable  whether the player's current resource covers the
 *                    cost, as reported by EliteMobs (informational
 *                    only - EliteMobs re-checks at fire time)
 * @param resourceName display name of the class resource the cost is
 *                    paid in (e.g. "Resolve"), or null when unknown
 */
public record SlotView(BridgeSlot slot, boolean present, String abilityName,
                       Double cost, boolean affordable, String resourceName) {

    public static SlotView absent(BridgeSlot slot) {
        return new SlotView(slot, false, null, null, false, null);
    }
}
