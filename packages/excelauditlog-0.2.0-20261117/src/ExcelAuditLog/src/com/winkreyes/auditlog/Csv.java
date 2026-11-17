package com.winkreyes.auditlog;

import java.util.ArrayList;
import java.util.List;

/** Minimal RFC4180-style CSV helpers. */
public final class Csv {
    private Csv() {}

    public static String esc(String s) {
        if (s == null) return "";
        s = s.replace('\r', ' ').replace('\n', ' ');
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) return '"' + s.replace("\"", "\"\"") + '"';
        return s;
    }

    public static String row(String... f) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < f.length; i++) { if (i > 0) b.append(','); b.append(esc(f[i])); }
        return b.toString();
    }

    public static List<String> parse(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (q) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else q = false;
                } else cur.append(c);
            } else if (c == '"') q = true;
            else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
}
