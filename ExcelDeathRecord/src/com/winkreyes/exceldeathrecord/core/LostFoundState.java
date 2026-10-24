package com.winkreyes.exceldeathrecord.core;

import java.util.Locale;

/**
 * 0.3.0: one player's Lost &amp; Found state, one small file per player
 * under lostfound/players/&lt;uuid&gt;.yml:
 *
 *   free-used: false          the one free recovery has been used
 *   free-used-epoch: 0
 *   recoveries: 0             recoveries done so far (paid + free)
 *   last-recovery-epoch: 0    start of the 24 h cooldown
 *   tip-ok: false             tapped OK on the first-use tip
 *
 * Unknown keys are ignored and missing keys keep their defaults, so
 * later versions can add keys without breaking this reader.
 */
public final class LostFoundState {
    public boolean freeUsed = false;
    public long freeUsedEpoch = 0;
    public int recoveries = 0;
    public long lastRecoveryEpoch = 0;
    public boolean tipOk = false;

    public LostFoundState copy() {
        LostFoundState s = new LostFoundState();
        s.freeUsed = freeUsed; s.freeUsedEpoch = freeUsedEpoch; s.recoveries = recoveries;
        s.lastRecoveryEpoch = lastRecoveryEpoch; s.tipOk = tipOk;
        return s;
    }

    public static String write(LostFoundState s) {
        return "# ExcelDeathRecord Lost & Found player state (0.3.0)\n"
                + "free-used: " + s.freeUsed + "\n"
                + "free-used-epoch: " + s.freeUsedEpoch + "\n"
                + "recoveries: " + s.recoveries + "\n"
                + "last-recovery-epoch: " + s.lastRecoveryEpoch + "\n"
                + "tip-ok: " + s.tipOk + "\n";
    }

    public static LostFoundState read(String text) {
        LostFoundState s = new LostFoundState();
        if (text == null) return s;
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int c = t.indexOf(':');
            if (c < 0) continue;
            String k = t.substring(0, c).trim().toLowerCase(Locale.ROOT);
            String v = t.substring(c + 1).trim();
            try {
                switch (k) {
                    case "free-used" -> s.freeUsed = Boolean.parseBoolean(v);
                    case "free-used-epoch" -> s.freeUsedEpoch = Long.parseLong(v);
                    case "recoveries" -> s.recoveries = Integer.parseInt(v);
                    case "last-recovery-epoch" -> s.lastRecoveryEpoch = Long.parseLong(v);
                    case "tip-ok" -> s.tipOk = Boolean.parseBoolean(v);
                    default -> { }
                }
            } catch (NumberFormatException ignored) { /* keep default */ }
        }
        return s;
    }
}
