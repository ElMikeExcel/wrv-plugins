package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.2.1 onboarding tip about the two wand styles. Pure logic (no Bukkit or
 * Magic types) so it is unit-tested on the plain JVM.
 *
 * A player sees the tip on each of their first {@code maxUses} wand uses
 * (a cast or right click with a Magic wand), until they acknowledge it
 * ([OK] in Java chat, OK in the Bedrock pop-up, /wandstyle ok, or picking a
 * style). After an acknowledgement, or after maxUses, it never shows again.
 * Clicks closer together than {@code useGapMs} count as one use, so one
 * burst of casting is one tip, not ten.
 * The Bedrock pop-up is sent at most once per session and never within
 * {@code combatQuietMs} of the player hitting or being hit.
 * Stored per player: uses (0..maxUses) and ack. Session data is not stored.
 */
public final class TipGate {
    public enum Show { SHOW, ACKED, DONE, GAP, DISABLED }

    public boolean enabled = true;
    public int maxUses = 15;
    public long useGapMs = 2000;
    public long combatQuietMs = 10_000;

    private final Map<UUID, Integer> uses = new HashMap<>();
    private final Set<UUID> acked = new HashSet<>();
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private final Set<UUID> formThisSession = new HashSet<>();
    private boolean dirty = false;

    public int uses(UUID id) { Integer n = uses.get(id); return n == null ? 0 : n; }
    public boolean acked(UUID id) { return acked.contains(id); }

    /** A wand use happened. SHOW means: show the tip now (the use is counted). */
    public Show onUse(UUID id, long nowMs) {
        if (!enabled) return Show.DISABLED;
        if (acked.contains(id)) return Show.ACKED;
        int n = uses(id);
        if (n >= maxUses) return Show.DONE;
        Long last = lastUse.get(id);
        if (last != null && nowMs - last >= 0 && nowMs - last < useGapMs) return Show.GAP;
        lastUse.put(id, nowMs);
        uses.put(id, n + 1);
        dirty = true;
        return Show.SHOW;
    }

    /** May the Bedrock OK pop-up go out now? If yes it is marked sent for this session. */
    public boolean takeForm(UUID id, long nowMs, long lastCombatMs) {
        if (formThisSession.contains(id)) return false;
        if (lastCombatMs > 0 && nowMs - lastCombatMs >= 0 && nowMs - lastCombatMs < combatQuietMs) return false;
        formThisSession.add(id);
        return true;
    }

    public boolean formSentThisSession(UUID id) { return formThisSession.contains(id); }

    /** Acknowledge. Returns true when this is new. */
    public boolean ack(UUID id) {
        if (id == null || !acked.add(id)) return false;
        dirty = true;
        return true;
    }

    /** Admin: start over for one player (count 0, not acknowledged). */
    public void reset(UUID id) {
        boolean ch = uses.remove(id) != null | acked.remove(id);
        lastUse.remove(id);
        formThisSession.remove(id);
        if (ch) dirty = true;
    }

    /** Player left: drop session-only data (pop-up allowed again next session). */
    public void endSession(UUID id) {
        lastUse.remove(id);
        formThisSession.remove(id);
    }

    public void put(UUID id, int n, boolean ack) {
        if (id == null) return;
        int c = Math.max(0, n);
        if (c > 0) uses.put(id, c); else uses.remove(id);
        if (ack) acked.add(id); else acked.remove(id);
    }

    public void clearAll() { uses.clear(); acked.clear(); lastUse.clear(); formThisSession.clear(); dirty = false; }

    public Set<UUID> known() { Set<UUID> s = new HashSet<>(uses.keySet()); s.addAll(acked); return s; }

    public boolean dirty() { return dirty; }
    public void saved() { dirty = false; }

    // ------------------------------------------------------------------ text

    public static final String TITLE = "Wand tip";
    public static final String L_INTRO = "Wand tip: you can pick how your wand works!";
    public static final String L_CLASSIC = "Classic: right-click to pick the next spell, left-click to cast.";
    /** 0.2.2: Bedrock players read tap and hold, not right-click (phones, tablets, controllers). */
    public static final String L_CLASSIC_BEDROCK = "Classic: press and hold (controller LT) picks the next spell, tap (RT) casts. On a phone, try Tap bar.";
    /** 0.2.3 Cycle line (Java and Bedrock wording). */
    public static final String L_CYCLE = "Cycle: right-click for the next spell, sneak + right-click or Q to go back, left-click to cast. No spell list.";
    public static final String L_CYCLE_BEDROCK = "Cycle: press and hold for the next spell, crouch + hold to go back, tap to cast (controller: LT, RT).";
    /** 0.2.4 hotbar-icon styles. */
    public static final String L_TAPBAR = "Tap bar: all your spells fill the hotbar. A key casts at once, left click reads, sneak for more, Q puts items back.";
    public static final String L_TAPBAR_BEDROCK = "Tap bar (best on phone): tap a spell to cast, press and hold to read, crouch for more, tap the wand for your items.";
    public static final String L_ORIGINAL = "Tap to pick (original): hotbar key picks a spell, left-click casts, Q puts your items back.";
    public static final String L_ORIGINAL_BEDROCK = "Tap to pick (original): tap your wand, tap a spell icon to pick it, press and hold to cast.";
    public static final String L_SNEAK = "Sneak bars: press and hold to open your spell bar, tap a spell to cast, crouch to flip to the next bar.";
    /** 0.2.5 parity: Sneak bars for Java too, Java wording. */
    public static final String L_SNEAK_JAVA = "Sneak bars: right-click opens your spell bar, a hotbar key casts, sneak flips to the next bar.";
    public static final String L_PICK_BEDROCK = "Pick one with /wandstyle or the Wand controls button in the compass menu.";
    public static final String L_PICK_JAVA = "Type /wandstyle to see your controls. Your spells: Spellbook button in the compass menu.";
    public static final String L_OK_BEDROCK = "Tap OK on the pop-up, or type /wandstyle ok, to hide this tip.";
    public static final String L_OK_JAVA = "Click [OK] to hide this tip.";
    public static final String OK_BUTTON = "OK, got it!";
    public static final String DONE_ACK = "Got it! The wand tip won't show again. /wandstyle any time.";

    /** Chat lines for one tip. 0.2.5: everyone gets the Sneak bars line (Java wording on Java). */
    public static List<String> lines(boolean bedrock) {
        List<String> out = new ArrayList<>();
        out.add(L_INTRO);
        out.add(bedrock ? L_CLASSIC_BEDROCK : L_CLASSIC);
        out.add(bedrock ? L_CYCLE_BEDROCK : L_CYCLE);
        out.add(bedrock ? L_SNEAK : L_SNEAK_JAVA);
        out.add(bedrock ? L_ORIGINAL_BEDROCK : L_ORIGINAL);
        out.add(bedrock ? L_TAPBAR_BEDROCK : L_TAPBAR);
        out.add(bedrock ? L_PICK_BEDROCK : L_PICK_JAVA);
        out.add(bedrock ? L_OK_BEDROCK : L_OK_JAVA);
        return out;
    }

    /** Text inside the Bedrock pop-up (no "tap OK" line, the button says it). */
    public static String formContent() {
        return L_TAPBAR_BEDROCK + "\n\n" + L_CLASSIC_BEDROCK + "\n\n" + L_CYCLE_BEDROCK + "\n\n" + L_SNEAK
                + "\n\n" + L_ORIGINAL_BEDROCK + "\n\n" + L_PICK_BEDROCK;
    }
}
