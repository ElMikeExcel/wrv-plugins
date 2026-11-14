package com.winkreyes.exceleitemobsabilitybedrockbridge;

/**
 * The bridge's own view of the three EliteMobs class ability slots.
 * Names deliberately match EliteMobs' AbilitySlot enum constants so the
 * gateway mapping is a name-for-name translation (pinned by a test that
 * runs against the real EliteMobs jar).
 */
public enum BridgeSlot {
    MOBILITY("Mobility"),
    SIGNATURE("Signature"),
    UTILITY("Utility");

    private final String displayName;

    BridgeSlot(String displayName) {
        this.displayName = displayName;
    }

    /** Player-facing slot label, e.g. "Signature". */
    public String displayName() {
        return displayName;
    }
}
