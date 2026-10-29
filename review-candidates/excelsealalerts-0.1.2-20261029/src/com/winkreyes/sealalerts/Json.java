/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;

import java.util.*;

/** Minimal JSON encoder (Map / List / String / Number / Boolean / null). Insertion order kept. */
final class Json {
    private Json() {}
    static String of(Object o) { StringBuilder b = new StringBuilder(); w(b, o); return b.toString(); }
    @SuppressWarnings("unchecked")
    private static void w(StringBuilder b, Object o) {
        if (o == null) b.append("null");
        else if (o instanceof String s) str(b, s);
        else if (o instanceof Boolean || o instanceof Integer || o instanceof Long) b.append(o);
        else if (o instanceof Number n) { double d = n.doubleValue(); if (Double.isFinite(d)) b.append(d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d)); else b.append("null"); }
        else if (o instanceof Map<?, ?> m) {
            b.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) { if (!first) b.append(", "); first = false; str(b, String.valueOf(e.getKey())); b.append(": "); w(b, e.getValue()); }
            b.append('}');
        } else if (o instanceof Collection<?> c) {
            b.append('['); boolean first = true;
            for (Object x : c) { if (!first) b.append(", "); first = false; w(b, x); }
            b.append(']');
        } else str(b, o.toString());
    }
    private static void str(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        b.append('"');
    }
}
