package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.2.2 first-use tip for the Spell Book screen and the progress line.
 * Administrator's rule: it repeats until the player says OK (maxShows 0 = no cap).
 * Shows at most once per showGapMs so it never floods chat. Pure logic.
 * OK paths: [OK] in Java chat, the OK button inside the Bedrock Spell Book,
 * or /spellbook ok. Stored per player (shows, ack) in spellscreen-tips.yml.
 */
public final class SpellTip {
    public enum Show { SHOW, ACKED, DONE, GAP, DISABLED }

    public boolean enabled = true;
    public int maxShows = 0;          // 0 = until OK
    public long showGapMs = 60_000;

    private final Map<UUID, Integer> shows = new HashMap<>();
    private final Set<UUID> acked = new HashSet<>();
    private final Map<UUID, Long> lastShow = new HashMap<>();
    private boolean dirty = false;

    public int shows(UUID id) { Integer n = shows.get(id); return n == null ? 0 : n; }
    public boolean acked(UUID id) { return acked.contains(id); }
    /** Still worth showing (enabled, not OK'd, under the cap). */
    public boolean pending(UUID id) { return enabled && !acked.contains(id) && (maxShows <= 0 || shows(id) < maxShows); }

    public Show onTrigger(UUID id, long nowMs) {
        if (!enabled) return Show.DISABLED;
        if (acked.contains(id)) return Show.ACKED;
        int n = shows(id);
        if (maxShows > 0 && n >= maxShows) return Show.DONE;
        Long last = lastShow.get(id);
        if (last != null && nowMs - last >= 0 && nowMs - last < showGapMs) return Show.GAP;
        lastShow.put(id, nowMs);
        shows.put(id, n + 1);
        dirty = true;
        return Show.SHOW;
    }

    public boolean ack(UUID id) {
        if (id == null || !acked.add(id)) return false;
        dirty = true;
        return true;
    }

    public void reset(UUID id) {
        boolean ch = shows.remove(id) != null | acked.remove(id);
        lastShow.remove(id);
        if (ch) dirty = true;
    }

    public void endSession(UUID id) { lastShow.remove(id); }

    public void put(UUID id, int n, boolean ack) {
        if (id == null) return;
        if (n > 0) shows.put(id, n); else shows.remove(id);
        if (ack) acked.add(id); else acked.remove(id);
    }

    public void clearAll() { shows.clear(); acked.clear(); lastShow.clear(); dirty = false; }
    public Set<UUID> known() { Set<UUID> s = new HashSet<>(shows.keySet()); s.addAll(acked); return s; }
    public boolean dirty() { return dirty; }
    public void saved() { dirty = false; }

    // ------------------------------------------------------------------ text (no long dashes)

    public static final String FEATURE = "spellscreen";
    public static final String TITLE = "Spell Book tip";
    public static final String B_OPEN = "Spell Book: crouch and press and hold with your wand (controller: crouch and press LT) to open it.";
    public static final String B_USE = "Tap a spell to read it, then tap Use this spell to put it on your wand.";
    public static final String J_OPEN = "Spell Book: type /spellbook to see your rank and what each spell's next rank needs.";
    public static final String PROGRESS = "Each spell shows its rank and what its next rank needs. If casting counts, a line above your hotbar shows it.";
    public static final String B_OK = "Tap OK in your Spell Book, or type /spellbook ok, to hide this tip.";
    public static final String J_OK = "Click [OK] to hide this tip.";
    public static final String OK_BUTTON = "OK, got it! (hide the tip)";
    public static final String DONE_ACK = "Got it! The Spell Book tip won't show again.";

    /** Chat lines (the last line is the OK line). */
    public static List<String> lines(boolean bedrock) {
        List<String> out = new ArrayList<>();
        if (bedrock) { out.add(B_OPEN); out.add(B_USE); }
        else out.add(J_OPEN);
        out.add(PROGRESS);
        out.add(bedrock ? B_OK : J_OK);
        return out;
    }

    /** Lines without the OK line, for ExcelTips (it adds its own OK line) and the Spell Book header. */
    public static List<String> body(boolean bedrock) {
        List<String> l = lines(bedrock);
        return new ArrayList<>(l.subList(0, l.size() - 1));
    }
}

