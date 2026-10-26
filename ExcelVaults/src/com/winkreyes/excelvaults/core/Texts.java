package com.winkreyes.excelvaults.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 0.1.1: platform-correct player text. Pure logic so it can be unit tested.
 * Bedrock players read "tap / sneak + tap / type /vaults" instead of "right-click" and "click a [Reopen] button"
 * (chat buttons cannot be clicked on Bedrock). A server owner can override any Bedrock line with
 * messages.&lt;key&gt;-bedrock in config.yml; without it the code default below is used, so the live config.yml
 * needs no edit.
 */
public final class Texts {
    private Texts() {}

    /** Keys that have a Bedrock version. */
    public static final List<String> PLATFORM_KEYS = List.of("hint-pay", "list-footer", "help");

    /** Bedrock defaults (single lines). */
    public static final Map<String, String> BEDROCK = Map.of(
            "hint-pay", "&7Sneak + tap it, or type &e/vaults&7, to reopen it now for &e${price}&7.",
            "list-footer", "&7Tap a vault below to reopen it now. Or sneak + tap a locked vault.");

    /** Bedrock default for the multi-line help. */
    public static final List<String> BEDROCK_HELP = List.of(
            "&6&lTrial vaults",
            "&f- Each vault reopens for you after a while: &e{normal-hours}h&f, ominous &e{ominous-hours}h&f.",
            "&f- Tap a used vault to see how long is left.",
            "&f- Sneak + tap it (or type &e/vaults&f) to pay and reopen it now. On a controller: sneak, then press LT.",
            "&f- You still need a key to open it.");

    /** Shown under the hint when a Bedrock tap opens the reopen window (bedrock-tap-opens-form). */
    public static final String BEDROCK_FORM_NOTE = "&7A window opened: tap &aReopen&7 to pay, or &cCancel&7. Nothing is charged until you tap Reopen.";

    /**
     * Java lines whose 0.1.0 default said "Tap" for a chat button. If the config still holds the exact
     * 0.1.0 default text (never customised), the new Java default is used instead.
     */
    public static final Map<String, String[]> JAVA_UPGRADES = Map.of(
            "list-footer", new String[]{"&7Tap a [Reopen] button, or sneak + right-click a locked vault.",
                                        "&7Click a [Reopen] button, or sneak + right-click a locked vault."});

    /** One line for this platform. cfg looks up messages.&lt;key&gt; and returns null when unset. */
    public static String pick(String key, boolean bedrock, Function<String, String> cfg) {
        if (bedrock && BEDROCK.containsKey(key)) {
            String o = cfg.apply(key + "-bedrock");
            return o != null ? o : BEDROCK.get(key);
        }
        String v = cfg.apply(key);
        String[] up = JAVA_UPGRADES.get(key);
        if (up != null && (v == null || v.equals(up[0]))) return up[1];
        return v != null ? v : key;
    }

    /** Multi-line help for this platform. cfgList returns an empty list when unset. */
    public static List<String> pickLines(String key, boolean bedrock, Function<String, List<String>> cfgList) {
        if (bedrock && key.equals("help")) {
            List<String> o = cfgList.apply("help-bedrock");
            return o != null && !o.isEmpty() ? o : BEDROCK_HELP;
        }
        List<String> v = cfgList.apply(key);
        return v == null ? List.of() : v;
    }

    /** Per player+vault cooldown for the Bedrock tap window, so tapping the same vault does not reopen it every tap. */
    public static final class TapGate {
        private final Map<String, Long> last = new HashMap<>();
        public boolean allow(String who, long now, long cooldownMs) {
            Long t = last.get(who);
            if (t != null && now - t < cooldownMs) return false;
            last.put(who, now);
            return true;
        }
        public void forgetPlayer(String uuidPrefix) { last.keySet().removeIf(k -> k.startsWith(uuidPrefix)); }
        public int size() { return last.size(); }
    }
}
