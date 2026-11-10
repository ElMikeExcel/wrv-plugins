package com.winkreyes.excelautosort.core;

/**
 * A filter chest's mode (SPEC §5.1). OFF keeps the record and its list
 * but deletes nothing — the player's pause button. WHITELIST keeps only
 * listed materials; BLACKLIST removes only listed materials.
 */
public enum Mode {
    OFF("off"),
    WHITELIST("whitelist"),
    BLACKLIST("blacklist");

    private final String id;

    Mode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Mode fromId(String s) {
        if (s == null) return OFF;
        for (Mode m : values()) {
            if (m.id.equalsIgnoreCase(s) || m.name().equalsIgnoreCase(s)) return m;
        }
        return OFF;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
