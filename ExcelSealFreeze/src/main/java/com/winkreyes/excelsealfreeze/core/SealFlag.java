package com.winkreyes.excelsealfreeze.core;

/**
 * Reads the seal-5 unlock flag from ExcelGuildGate's persisted state:
 * plugins/ExcelGuildGate/state.yml carries a TOP-LEVEL "unlocked: true"
 * once every enabled seal is broken (GuildGate writes it on every state
 * save; derivation in GuildGate's SealState: unlocked exactly when every
 * enabled seal is broken; the Warden is the fifth).
 *
 * Deliberately conservative parsing (raw text, no YAML dependency): only a
 * column-0 "unlocked:" line counts, so a nested lookalike key can never
 * lift the freeze early. Missing file, unreadable text, "unlocked: false"
 * or anything unparseable all read as STILL LOCKED - the freeze is the
 * safe default for the flag, exactly as designed.
 */
public final class SealFlag {

    private SealFlag() {
    }

    public static boolean isUnlocked(String stateYmlText) {
        if (stateYmlText == null) return false;
        for (String line : stateYmlText.split("\n")) {
            if (line.startsWith("unlocked:")) {
                String value = line.substring("unlocked:".length()).trim();
                return value.equalsIgnoreCase("true");
            }
        }
        return false;
    }

    /** Display-only: how many seals state.yml currently records as broken. */
    public static int brokenCount(String stateYmlText) {
        if (stateYmlText == null) return 0;
        int n = 0;
        for (String line : stateYmlText.split("\n")) {
            if (line.trim().equals("broken: true")) n++;
        }
        return n;
    }
}
