package com.winkreyes.excelwhatsnew.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Text for one entry. Java gets '&'-coded chat lines (the plugin turns them into
 * components and adds the clickable buttons). Bedrock gets one form body with
 * section-sign colors, which Bedrock forms show natively.
 */
public final class Render {
    private Render() { }

    public static String formTitle(Entry e) {
        return e.date == null || e.date.isEmpty() ? "What's New" : "What's New (" + e.date + ")";
    }

    /** Java chat lines between the top and bottom rules (no buttons). */
    public static List<String> java(Entry e) {
        List<String> l = new ArrayList<>();
        l.add("&e&lWhat's New" + (e.date.isEmpty() ? "" : " &7(" + e.date + ")"));
        l.add("&f" + e.title);
        if (!e.newCommands.isEmpty()) {
            l.add("&a&lNew commands");
            for (Entry.Cmd c : e.newCommands) {
                l.add(" &e" + c.cmd + " &7- &f" + c.what);
                if (c.java != null) l.add("   &7" + c.java);
            }
        }
        if (!e.betterCommands.isEmpty()) {
            l.add("&b&lCommands that got better");
            for (Entry.Cmd c : e.betterCommands) {
                l.add(" &e" + c.cmd + " &7- &f" + c.what);
                if (c.java != null) l.add("   &7" + c.java);
            }
        }
        if (!e.changes.isEmpty()) {
            l.add("&d&lOther changes");
            for (String s : e.changes) l.add(" &7- &f" + s);
        }
        return l;
    }

    /** Bedrock form body. */
    public static String bedrock(Entry e, String footer) {
        StringBuilder b = new StringBuilder();
        b.append("\u00a7l").append(e.title).append("\u00a7r");
        if (!e.newCommands.isEmpty()) {
            b.append("\n\n\u00a7a\u00a7lNew commands\u00a7r");
            for (Entry.Cmd c : e.newCommands) cmd(b, c);
        }
        if (!e.betterCommands.isEmpty()) {
            b.append("\n\n\u00a73\u00a7lCommands that got better\u00a7r");
            for (Entry.Cmd c : e.betterCommands) cmd(b, c);
        }
        if (!e.changes.isEmpty()) {
            b.append("\n\n\u00a75\u00a7lOther changes\u00a7r");
            for (String s : e.changes) b.append("\n- ").append(s);
        }
        if (footer != null && !footer.isEmpty()) b.append("\n\n\u00a77").append(footer).append("\u00a7r");
        return b.toString();
    }

    private static void cmd(StringBuilder b, Entry.Cmd c) {
        b.append("\n\u00a7e").append(c.cmd).append("\u00a7r - ").append(c.what);
        if (c.bedrock != null) b.append("\n   \u00a77No typing: \u00a7r").append(c.bedrock);
    }
}
