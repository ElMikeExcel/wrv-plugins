package com.winkreyes.excelwhatsnew.core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses the entries list of changelog.yml (newest first) into Entry objects.
 * Takes the raw YAML maps so it can be unit tested without Bukkit.
 * Bad entries are skipped with a warning; bad lines are kept but warned about.
 */
public final class Changelog {
    public final List<Entry> entries;
    public final List<String> warnings;

    private Changelog(List<Entry> e, List<String> w) { entries = List.copyOf(e); warnings = List.copyOf(w); }

    static String str(Object o) { return o == null ? null : String.valueOf(o).trim(); }

    public static Changelog parse(List<?> raw) {
        List<Entry> out = new ArrayList<>();
        List<String> warn = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        if (raw == null) { warn.add("changelog.yml has no 'entries:' list"); return new Changelog(out, warn); }
        int n = 0;
        LocalDate prev = null;
        for (Object o : raw) {
            n++;
            if (!(o instanceof Map<?, ?> m)) { warn.add("entry " + n + ": not a map, skipped"); continue; }
            Object io = m.get("id");
            if (io instanceof java.util.Date d) io = d.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate().toString(); // id: 2026-11-01 unquoted
            String id = str(io), date = str(m.get("date")), title = str(m.get("title"));
            if (id == null || id.isEmpty()) { warn.add("entry " + n + ": no id, skipped"); continue; }
            id = id.toLowerCase(Locale.ROOT);
            if (!ids.add(id)) { warn.add("entry " + n + " (" + id + "): id used twice, skipped"); continue; }
            if (title == null || title.isEmpty()) { warn.add(id + ": no title, skipped"); continue; }
            if (date == null || date.isEmpty()) { warn.add(id + ": no date label"); date = ""; }
            LocalDate posted = null;
            Object po = m.get("posted");
            String p = str(po);
            if (po instanceof java.util.Date d) posted = d.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate(); // unquoted YAML date
            else if (po instanceof LocalDate ld) posted = ld;
            else if (p != null) {
                try { posted = LocalDate.parse(p); } catch (DateTimeParseException ex) { warn.add(id + ": posted '" + p + "' is not YYYY-MM-DD"); }
            }
            if (posted != null && prev != null && posted.isAfter(prev)) warn.add(id + ": newer than the entry above it (keep the newest at the top)");
            if (posted != null) prev = posted;
            List<Entry.Cmd> nc = cmds(m.get("new-commands"), id, "new-commands", warn);
            List<Entry.Cmd> bc = cmds(m.get("better-commands"), id, "better-commands", warn);
            List<String> ch = new ArrayList<>();
            Object c = m.get("changes");
            if (c instanceof List<?> l) for (Object x : l) { String s = str(x); if (s != null && !s.isEmpty()) ch.add(s); }
            else if (c != null) warn.add(id + ": changes is not a list");
            if (nc.isEmpty() && bc.isEmpty() && ch.isEmpty()) warn.add(id + ": has no commands and no changes");
            Entry e = new Entry(id, date, posted, title, nc, bc, ch);
            for (String line : allText(e)) {
                if (line.contains("\u2014")) warn.add(id + ": em dash in \"" + cut(line) + "\" (house style: no em dashes)");
                if (line.length() > 160) warn.add(id + ": long line \"" + cut(line) + "\" (" + line.length() + " chars, keep it short)");
            }
            out.add(e);
        }
        return new Changelog(out, warn);
    }

    private static List<Entry.Cmd> cmds(Object o, String id, String key, List<String> warn) {
        List<Entry.Cmd> out = new ArrayList<>();
        if (o == null) return out;
        if (!(o instanceof List<?> l)) { warn.add(id + ": " + key + " is not a list"); return out; }
        for (Object x : l) {
            if (!(x instanceof Map<?, ?> m)) { warn.add(id + ": " + key + " line is not {cmd, what}"); continue; }
            String cmd = str(m.get("cmd")), what = str(m.get("what"));
            if (cmd == null || what == null || cmd.isEmpty() || what.isEmpty()) { warn.add(id + ": " + key + " line needs cmd and what"); continue; }
            if (!cmd.startsWith("/")) warn.add(id + ": command '" + cmd + "' should start with /");
            out.add(new Entry.Cmd(cmd, what, emptyNull(str(m.get("bedrock"))), emptyNull(str(m.get("java")))));
        }
        return out;
    }

    private static String emptyNull(String s) { return s == null || s.isEmpty() ? null : s; }
    private static String cut(String s) { return s.length() > 40 ? s.substring(0, 40) + "..." : s; }

    /** Every player-facing text of an entry (for checks). */
    public static List<String> allText(Entry e) {
        List<String> t = new ArrayList<>();
        t.add(e.title); t.add(e.date);
        for (Entry.Cmd c : e.newCommands) { t.add(c.cmd); t.add(c.what); if (c.bedrock != null) t.add(c.bedrock); if (c.java != null) t.add(c.java); }
        for (Entry.Cmd c : e.betterCommands) { t.add(c.cmd); t.add(c.what); if (c.bedrock != null) t.add(c.bedrock); if (c.java != null) t.add(c.java); }
        t.addAll(e.changes);
        return t;
    }

    public Entry byId(String id) {
        if (id == null) return null;
        String k = id.toLowerCase(Locale.ROOT);
        for (Entry e : entries) if (e.id.equals(k)) return e;
        return null;
    }

    public int indexOf(String id) {
        Entry e = byId(id);
        return e == null ? -1 : entries.indexOf(e);
    }
}
