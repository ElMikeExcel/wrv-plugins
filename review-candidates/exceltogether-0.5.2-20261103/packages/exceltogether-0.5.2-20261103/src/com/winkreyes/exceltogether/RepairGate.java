package com.winkreyes.exceltogether;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelTogether 0.5.2 - the confirmation gate in front of the party-repair
 * actions (ExcelPartyRepair / ExcelRepairItem). Mike's call, Nov 1, 2026:
 * Repair Equipment must not be usable by accident - "add a confirmation
 * screen pop up for this spell to combat using it when not wanted."
 *
 * Why the gate lives here and not in Magic: Magic 11.2.5 has NO spell-cast
 * confirmation facility (verified in the jar - every "confirm" in it is a
 * selector/shop GUI option or a recall-marker flag, and BaseSpell charges
 * the spell's cooldown the moment a cast runs, whatever the actions do).
 * So the spell's own Magic cooldown drops to a 1-second anti-bounce and
 * the real 4-hour cooldown is THIS gate's lockout, enforced per caster:
 *
 *   cast 1 (ARM)     - nothing repairs. The caster is told what a confirm
 *                      would do, with the live damaged-item count, and
 *                      that confirming starts the lockout. No lockout is
 *                      started, ever, by an arming cast.
 *   cast 2 (CONFIRM) - within the confirm window: the lockout starts NOW
 *                      (persisted, so it survives restarts and relogs) and
 *                      the 0.5.1 repair runs exactly as proven - including
 *                      when the count was 0, per Mike's settled design.
 *   cast while locked - refused, with the remaining time. An expired arm
 *                      simply re-arms: a lapsed confirmation costs nothing.
 *
 * Pure time-and-state logic (no Bukkit), so the whole state machine is
 * unit-testable headless; the persistence hand-off is snapshot/restore of
 * the confirmed-cast map (RepairCooldownStore owns the file). Arms are
 * deliberately NOT persisted: a restart between arm and confirm just
 * means the next cast arms again - nothing repaired, nothing burned.
 * Excel - Wink Reyes Valley.
 */
public final class RepairGate {

    public enum Decision { ARM, CONFIRM, LOCKED }

    /** The answer to one cast attempt; remainingMs is set for LOCKED. */
    public static final class Verdict {
        public final Decision decision;
        public final long remainingMs;
        Verdict(Decision decision, long remainingMs) {
            this.decision = decision;
            this.remainingMs = remainingMs;
        }
    }

    /** The three player-facing strings, config-owned (see config.yml). */
    public static final class Texts {
        public static final String DEFAULT_PROMPT =
                "Repair Equipment: cast again within {seconds} seconds to fully repair {items} damaged item(s) "
                + "across {members} party member(s) - armor + main hand, FULL repair. "
                + "Confirming starts your {cooldown} cooldown.";
        public static final String DEFAULT_PROMPT_NONE =
                "Repair Equipment: no damaged gear found across {members} party member(s). "
                + "Cast again within {seconds} seconds to cast it anyway - it still starts your {cooldown} cooldown.";
        public static final String DEFAULT_REFUSED =
                "Repair Equipment is on cooldown: {remaining} remaining.";

        public final String prompt;
        public final String promptNone;
        public final String refused;

        public Texts(String prompt, String promptNone, String refused) {
            this.prompt = prompt == null || prompt.isBlank() ? DEFAULT_PROMPT : prompt;
            this.promptNone = promptNone == null || promptNone.isBlank() ? DEFAULT_PROMPT_NONE : promptNone;
            this.refused = refused == null || refused.isBlank() ? DEFAULT_REFUSED : refused;
        }

        public static Texts defaults() { return new Texts(null, null, null); }
    }

    private final long confirmWindowMs;
    private final long cooldownMs;
    private final Texts texts;
    private final Map<UUID, Long> armedUntil = new HashMap<>();
    private final Map<UUID, Long> lastConfirmed = new HashMap<>();

    public RepairGate(long confirmWindowMs, long cooldownMs, Texts texts) {
        this.confirmWindowMs = Math.max(1L, confirmWindowMs);
        this.cooldownMs = Math.max(0L, cooldownMs);
        this.texts = texts == null ? Texts.defaults() : texts;
    }

    public long confirmWindowMs() { return confirmWindowMs; }
    public long cooldownMs() { return cooldownMs; }
    public Texts texts() { return texts; }

    /**
     * What one cast attempt means right now. The lockout is judged first:
     * nothing (including a fresh arm) outruns a live lockout.
     */
    public synchronized Verdict verdict(UUID caster, long now) {
        Long last = lastConfirmed.get(caster);
        if (last != null) {
            long remaining = cooldownMs - (now - last);
            if (remaining > 0) return new Verdict(Decision.LOCKED, remaining);
            lastConfirmed.remove(caster);
        }
        Long until = armedUntil.get(caster);
        if (until != null && now < until) return new Verdict(Decision.CONFIRM, 0);
        return new Verdict(Decision.ARM, 0);
    }

    /** Open (or refresh) the confirmation window for a caster. */
    public synchronized void arm(UUID caster, long now) {
        armedUntil.put(caster, now + confirmWindowMs);
    }

    /** Consume the arm and start the lockout. */
    public synchronized void confirm(UUID caster, long now) {
        armedUntil.remove(caster);
        lastConfirmed.put(caster, now);
    }

    /** Milliseconds of lockout left for a caster (0 when not locked). */
    public synchronized long remainingMs(UUID caster, long now) {
        Long last = lastConfirmed.get(caster);
        if (last == null) return 0;
        return Math.max(0, cooldownMs - (now - last));
    }

    /** Confirmed-cast timestamps for persistence; expired entries pruned. */
    public synchronized Map<UUID, Long> snapshotConfirmed(long now) {
        lastConfirmed.values().removeIf(t -> now - t >= cooldownMs);
        return new HashMap<>(lastConfirmed);
    }

    /** Restore confirmed-cast timestamps loaded from disk. */
    public synchronized void restoreConfirmed(Map<UUID, Long> loaded) {
        lastConfirmed.clear();
        if (loaded != null) lastConfirmed.putAll(loaded);
    }

    // ------------------------------------------------------------ messages

    /** The arming prompt: the live count, or the honest zero-count text. */
    public String promptText(int items, int membersWithDamage, int membersChecked) {
        if (items <= 0) return fill(texts.promptNone, 0, membersChecked, 0);
        return fill(texts.prompt, items, membersWithDamage, 0);
    }

    /** The lockout refusal, with the remaining time. */
    public String refusedText(long remainingMs) {
        return fill(texts.refused, 0, 0, remainingMs);
    }

    private String fill(String template, int items, int members, long remainingMs) {
        return template
                .replace("{items}", Integer.toString(items))
                .replace("{members}", Integer.toString(members))
                .replace("{seconds}", Long.toString(Math.max(1, (confirmWindowMs + 999) / 1000)))
                .replace("{cooldown}", formatDuration(cooldownMs))
                .replace("{remaining}", formatDuration(remainingMs));
    }

    /** "4h 0m" / "3h 12m" / "10m" / "59s" / "0s" - minutes round up. */
    public static String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        if (ms < 60_000L) return ((ms + 999) / 1000) + "s";
        long totalMinutes = (ms + 59_999L) / 60_000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + "m";
    }
}
