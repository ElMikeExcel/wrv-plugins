package com.winkreyes.exceltips.core;

import java.util.ArrayList;
import java.util.List;

/** One feature's tip, as read from tips.yml. Plain data. */
public final class TipDef {
    public String id;
    public String title = "Tip";
    public boolean enabled = true;
    public int maxShows = -1;          // -1 = use the default
    public long useGapMs = -1;         // -1 = use the default
    public boolean form = true;        // Bedrock OK pop-up allowed for this tip
    public String permission = "";     // optional: only players with this permission see it
    public String needsPlugin = "";    // "Name" or "Name>=1.2.0": tip is on only while that plugin is enabled
    public String unlessPlugin = "";   // "Name>=1.2.0": tip is off while that plugin (version) is enabled
    public String owner = "";          // plugin that registered this tip through the API ("" = tips.yml)
    public List<String> lines = new ArrayList<>();
    public List<String> java = new ArrayList<>();
    public List<String> bedrock = new ArrayList<>();
    public List<Trigger> triggers = new ArrayList<>();

    public List<String> linesFor(boolean isBedrock) {
        List<String> v = isBedrock ? bedrock : java;
        return v.isEmpty() ? lines : v;
    }

    /** Problems a reviewer should see (empty = fine). */
    public List<String> problems() {
        List<String> p = new ArrayList<>();
        if (lines.isEmpty() && (java.isEmpty() || bedrock.isEmpty())) p.add(id + ": needs text, or both java and bedrock text");
        for (String s : all()) if (Words.hasDash(s)) p.add(id + ": has a long dash: " + s);
        if (Words.hasDash(title)) p.add(id + ": title has a long dash");
        return p;
    }

    public List<String> all() { List<String> a = new ArrayList<>(lines); a.addAll(java); a.addAll(bedrock); return a; }

    public static final class Trigger {
        public String type = "";                       // command, item-use, block-use, bed-enter, entity-use, inventory-open, region, world-enter, join, respawn, event, api
        public List<String> values = new ArrayList<>(); // commands / materials / worlds / title text, by type
        public List<String> pdcKeys = new ArrayList<>();
        public List<String> names = new ArrayList<>();   // entity-use: name contains (any)
        public List<String> notNames = new ArrayList<>(); // entity-use: name must not contain
        public List<String> metadata = new ArrayList<>(); // entity-use: entity has metadata key (any)
        public List<String> holders = new ArrayList<>();  // inventory-open: holder class simple name contains (any)
        public String world = "";
        public int[] min, max;                            // region box (inclusive block coords)
        public String eventClass = "";
        public String playerPath = "getPlayer";
        public long delayTicks = -1;                      // -1 = default for the type
    }
}
