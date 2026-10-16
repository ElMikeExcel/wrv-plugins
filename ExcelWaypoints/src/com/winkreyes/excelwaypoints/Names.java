package com.winkreyes.excelwaypoints;

/** Place-name cleanup. Strips color codes, control and odd characters. */
public final class Names {
    private Names() {}
    public static final int MAX = 24;

    /** Returns the cleaned name, or "" if nothing usable is left. Never throws. */
    public static String clean(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("\\s", " ").replaceAll("(?i)[\u00a7&][0-9a-fk-or]", "");   // color and format codes
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i); i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp) && cp < 0x250 /* Latin only, safe on every client font */
                    || cp == ' ' || cp == '\'' || cp == '-' || cp == '_' || cp == '.' || cp == ',' || cp == '!' || cp == '(' || cp == ')') {
                b.appendCodePoint(cp);
            }
        }
        String out = b.toString().replaceAll("\\s+", " ").trim();
        if (out.codePointCount(0, out.length()) > MAX) {
            out = out.substring(0, out.offsetByCodePoints(0, MAX)).trim();
        }
        return out;
    }

    /** Display form of a Floodgate name: drops the leading "." prefix. */
    public static String display(String realName) {
        if (realName == null) return "Someone";
        String n = realName.startsWith(".") ? realName.substring(1) : realName;
        return n.isEmpty() ? "Someone" : n;
    }
}
