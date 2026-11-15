package com.winkreyes.excelwhatsnew.core;

import java.time.LocalDate;
import java.util.List;

/** One news entry from changelog.yml. Plain data, no Bukkit. */
public final class Entry {
    /** One command line: the command, what it does, and optional no-typing hints. */
    public static final class Cmd {
        public final String cmd, what, bedrock, java;
        public Cmd(String cmd, String what, String bedrock, String java) {
            this.cmd = cmd; this.what = what; this.bedrock = bedrock; this.java = java;
        }
    }

    public final String id;        // unique, never reused (players.yml stores it)
    public final String date;      // short label for titles, e.g. "Oct 24"
    public final LocalDate posted; // for the join age limit; may be null
    public final String title;
    public final List<Cmd> newCommands;
    public final List<Cmd> betterCommands;
    public final List<String> changes;

    public Entry(String id, String date, LocalDate posted, String title, List<Cmd> newCommands, List<Cmd> betterCommands, List<String> changes) {
        this.id = id; this.date = date; this.posted = posted; this.title = title;
        this.newCommands = List.copyOf(newCommands); this.betterCommands = List.copyOf(betterCommands); this.changes = List.copyOf(changes);
    }
}
