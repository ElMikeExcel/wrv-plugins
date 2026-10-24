package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses the words after "/deathrecord restore":
 *
 *   restore <player>                          open the tap menu at step 1
 *   restore <player> <n|id>                   open the tap menu at that death
 *   restore <player> <n|id> <items> [to <target>] [xp] [force]
 *   restore burned <n|id> to <target> [force]
 *
 * <n> = 1 is the newest death; <id> is a record id such as
 * 2026-10-23_221412 (stable even after newer deaths). <items>
 * is a Selection ("all", "3", "2,3,7", "1-5"). Flags may come in
 * any order at the end. The player name may contain spaces
 * (Bedrock); it is everything before the record number.
 */
public final class RestoreArgs {

    public enum Mode { MENU, DEATH, BURNED, ERROR }

    public static final Pattern RECORD_ID = Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{6}(-\\d+)?");

    public Mode mode = Mode.ERROR;
    public String player = "";
    public int n = -1;            // 1-based record number, or -1 when recordId is used / absent
    public String recordId = "";  // file name without .yml
    public String items = "";
    public String target = "";    // "" = the dead player
    public boolean xp = false;
    public boolean force = false;
    public String error = "";

    private RestoreArgs() {}

    public boolean hasRecord() { return n > 0 || !recordId.isEmpty(); }

    public static RestoreArgs parse(String[] words) {
        RestoreArgs a = new RestoreArgs();
        List<String> w = new ArrayList<>(Arrays.asList(words));
        w.removeIf(s -> s == null || s.isBlank());
        // trailing flags, any order
        boolean changed = true;
        while (changed && !w.isEmpty()) {
            changed = false;
            String last = w.get(w.size() - 1).toLowerCase(Locale.ROOT);
            if (last.equals("force")) { a.force = true; w.remove(w.size() - 1); changed = true; }
            else if (last.equals("xp")) { a.xp = true; w.remove(w.size() - 1); changed = true; }
            else if (w.size() >= 2 && w.get(w.size() - 2).equalsIgnoreCase("to")) {
                if (!a.target.isEmpty()) break;
                a.target = w.get(w.size() - 1);
                w.remove(w.size() - 1); w.remove(w.size() - 1);
                changed = true;
            }
        }
        if (w.isEmpty()) { a.error = "Missing player."; return a; }
        if (w.get(w.size() - 1).equalsIgnoreCase("to")) { a.error = "Say who gets it after 'to', e.g. to Sam."; return a; }

        if (w.get(0).equalsIgnoreCase("burned")) {
            if (w.size() != 2 || !a.setRecord(w.get(1))) {
                a.error = "Use: /deathrecord restore burned <n> to <player>";
                return a;
            }
            if (a.target.isEmpty()) { a.error = "Say who gets it: /deathrecord restore burned " + w.get(1) + " to <player>"; return a; }
            a.mode = Mode.BURNED;
            return a;
        }

        // <player...> [<n|id> [<items>]]
        if (w.size() >= 3 && isRecordToken(w.get(w.size() - 2))) {
            a.setRecord(w.get(w.size() - 2));
            a.items = w.get(w.size() - 1);
            if (!a.items.matches("(?i)all|[0-9,\\- ]+")) {
                a.error = "'" + a.items + "' is not an item list (all, 3, 2,3,7 or 1-5). To give to someone else, add: to " + a.items;
                return a;
            }
            a.player = String.join(" ", w.subList(0, w.size() - 2));
            a.mode = Mode.DEATH;
            return a;
        }
        if (w.size() >= 2 && isRecordToken(w.get(w.size() - 1))) {
            a.setRecord(w.get(w.size() - 1));
            a.player = String.join(" ", w.subList(0, w.size() - 1));
            a.mode = Mode.MENU;
            return a;
        }
        a.player = String.join(" ", w);
        a.mode = Mode.MENU;
        return a;
    }

    static boolean isRecordToken(String s) {
        if (s.endsWith(".yml")) s = s.substring(0, s.length() - 4);
        if (RECORD_ID.matcher(s).matches()) return true;
        try { return Integer.parseInt(s) > 0; } catch (NumberFormatException e) { return false; }
    }

    private boolean setRecord(String s) {
        String t = s.endsWith(".yml") ? s.substring(0, s.length() - 4) : s;
        if (RECORD_ID.matcher(t).matches()) { recordId = t; return true; }
        try {
            int v = Integer.parseInt(t);
            if (v > 0) { n = v; return true; }
        } catch (NumberFormatException ignored) { }
        return false;
    }
}
