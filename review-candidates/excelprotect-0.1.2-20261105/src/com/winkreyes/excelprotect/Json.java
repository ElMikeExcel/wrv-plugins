package com.winkreyes.excelprotect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Excel — minimal JSON rendering for the black-box event lines (0.3.0). No dependencies;
 * insertion-ordered objects, compact output, one event per line.
 */
public final class Json {
    private Json() {}

    public static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                default -> {
                    if (c < 0x20) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.toString();
    }

    /** Integral doubles render as integers (4 not 4.0); others get up to 2 decimals, trimmed. */
    public static String num(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return Long.toString((long) d);
        String s = String.format(Locale.ROOT, "%.2f", d);
        while (s.contains(".") && s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static String render(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + esc(s) + "\"";
        if (v instanceof Double d) return num(d);
        if (v instanceof Float f) return num(f.doubleValue());
        if (v instanceof Number n) return n.toString();
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof Obj o) return o.toString();
        if (v instanceof Arr a) return render(a.items);
        if (v instanceof Map<?, ?> m) {
            StringBuilder b = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) b.append(',');
                b.append('"').append(esc(String.valueOf(e.getKey()))).append("\":").append(render(e.getValue()));
                first = false;
            }
            return b.append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder b = new StringBuilder("[");
            boolean first = true;
            for (Object x : l) { if (!first) b.append(','); b.append(render(x)); first = false; }
            return b.append(']').toString();
        }
        return "\"" + esc(String.valueOf(v)) + "\"";
    }

    public static final class Obj {
        private final LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        public Obj put(String k, Object v) { if (v != null) m.put(k, v); return this; }
        public Obj putAlways(String k, Object v) { m.put(k, v); return this; }
        public Obj putRaw(String k, Object v) { m.put(k, v); return this; }
        public void forEach(java.util.function.BiConsumer<String, Object> f) { m.forEach(f); }
        public boolean has(String k) { return m.containsKey(k); }
        public boolean hasAny() { return !m.isEmpty(); }
        public Object get(String k) { return m.get(k); }
        public String render() { return Json.render(m); }
        @Override public String toString() { return Json.render(m); }
    }

    /** Ordered JSON array builder (renders via Json.render). */
    public static final class Arr {
        final java.util.List<Object> items = new java.util.ArrayList<>();
        public Arr add(Object v) { items.add(v); return this; }
        public int size() { return items.size(); }
        public String render() { return Json.render(items); }
        @Override public String toString() { return render(); }
    }

    // ---- raw-line field extraction (read path: export / rollback / lost-and-found) ----

    /** String field value from a rendered JSON object line, or null. Handles \" and \\ escapes. */
    public static String strField(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int colon = json.indexOf(':', i + key.length() + 2);
        if (colon < 0) return null;
        int p = colon + 1;
        while (p < json.length() && json.charAt(p) == ' ') p++;
        if (p >= json.length() || json.charAt(p) != '"') return null;
        StringBuilder sb = new StringBuilder();
        for (int j = p + 1; j < json.length(); j++) {
            char ch = json.charAt(j);
            if (ch == '\\' && j + 1 < json.length()) { sb.append(json.charAt(++j)); continue; }
            if (ch == '"') return sb.toString();
            sb.append(ch);
        }
        return null;
    }

    public static double numField(String json, String key, double def) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return def;
        int colon = json.indexOf(':', i + key.length() + 2);
        if (colon < 0) return def;
        int p = colon + 1;
        while (p < json.length() && json.charAt(p) == ' ') p++;
        int q = p;
        while (q < json.length() && "-+0123456789.eE".indexOf(json.charAt(q)) >= 0) q++;
        if (q == p) return def;
        try { return Double.parseDouble(json.substring(p, q)); } catch (Exception e) { return def; }
    }

    /** All string values of a rendered JSON array (e.g. sign lines), in order. */
    public static java.util.List<String> strings(String arrayRaw) {
        java.util.List<String> out = new java.util.ArrayList<>();
        boolean str = false, esc = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayRaw.length(); i++) {
            char c = arrayRaw.charAt(i);
            if (str) {
                if (esc) { sb.append(c); esc = false; }
                else if (c == '\\') esc = true;
                else if (c == '"') { out.add(sb.toString()); sb.setLength(0); str = false; }
                else sb.append(c);
            } else if (c == '"') str = true;
        }
        return out;
    }

    /** The raw {...} or [...] substring for a key, or null (brace-matched, string-aware). */
    public static String rawField(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int colon = json.indexOf(':', i + key.length() + 2);
        if (colon < 0) return null;
        int p = colon + 1;
        while (p < json.length() && json.charAt(p) == ' ') p++;
        if (p >= json.length()) return null;
        char open = json.charAt(p);
        if (open != '{' && open != '[') return null;
        char close = open == '{' ? '}' : ']';
        int depth = 0; boolean str = false, esc = false;
        for (int j = p; j < json.length(); j++) {
            char ch = json.charAt(j);
            if (str) { if (esc) esc = false; else if (ch == '\\') esc = true; else if (ch == '"') str = false; continue; }
            if (ch == '"') str = true;
            else if (ch == open) depth++;
            else if (ch == close) { if (--depth == 0) return json.substring(p, j + 1); }
        }
        return null;
    }
}
