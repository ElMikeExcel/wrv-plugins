package com.winkreyes.exceleitemobsabilitybedrockbridge;

import java.util.Locale;

/** Small pure formatting/label helpers shared by the gateway and form builder. */
public final class Labels {

    private Labels() {}

    /**
     * Formats a resource amount or cost the way a player expects:
     * whole numbers without a decimal point ("40", "100"), anything
     * else with at most one decimal place ("65.5").
     */
    public static String number(Double value) {
        if (value == null) {
            return "?";
        }
        double d = value;
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "?";
        }
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return Long.toString(Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }

    /**
     * Turns an EliteMobs ability id (e.g. "paladin.signature",
     * "divine_steed") into a readable label ("Paladin Signature",
     * "Divine Steed") for use when the module reports an id but no
     * display name.
     */
    public static String prettifyId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String tail = id;
        int dot = tail.lastIndexOf('.');
        if (dot >= 0 && dot < tail.length() - 1) {
            tail = tail.substring(dot + 1);
            // ids like "paladin.signature" end in the slot name, which is
            // not the ability's name; fall back to the whole id prettified.
            if (tail.equalsIgnoreCase("mobility") || tail.equalsIgnoreCase("signature")
                    || tail.equalsIgnoreCase("utility")) {
                tail = id;
            }
        }
        StringBuilder out = new StringBuilder();
        for (String word : tail.split("[._\\s]+")) {
            String w = word.trim();
            if (w.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                out.append(w.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.length() == 0 ? null : out.toString();
    }

    /**
     * Resolves the display label for a slot: the module's live ability
     * name wins; a blank name falls back to the prettified ability id;
     * both blank means the slot has no label (treated as not present
     * when there is also no snapshot - the gateway decides presence).
     */
    public static String resolveAbilityLabel(String liveName, String abilityId) {
        if (liveName != null && !liveName.isBlank()) {
            return liveName.trim();
        }
        return prettifyId(abilityId);
    }
}
