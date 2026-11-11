/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Per-encounter boss-voice state (ExcelBossTrial 0.5.2), the
 * TauntBook shape: one small state record per open boss,
 * pruned with the encounter map. The book owns the voice
 * rules that are about TIME and ONCE-ness; the ADAPTER owns
 * the truth rule (it calls in only when the seam's real event
 * happened - a sonic boom landed, an encounter opened, a
 * verdict closed), so nothing in here can make a boss speak
 * decoratively.
 *
 * What lives here:
 *  - the Warden boom gate: sonic booms can land in a barrage,
 *    so the boom line is rate-limited to the chapter's taunt
 *    cooldown (DESIGN section 4.4), and consecutive SPOKEN
 *    booms alternate the two shipped clips (boom, then
 *    boom_alt) so a long fight hears both lines;
 *  - the once-per-encounter guards for the open line and the
 *    elder last-door line (an encounter opens once; the last
 *    door is named once);
 *  - the log-once set for missing clips (a line with no clip
 *    is chat-only and logged ONCE per chapter+slot, not once
 *    per fight forever).
 */
public final class VoiceBook {

    /** One boss's boom state for one encounter. */
    private static final class BoomState {
        long lastBoomMs = Long.MIN_VALUE;
        int spoken;
    }

    private final Map<UUID, BoomState> booms = new HashMap<>();
    private final Set<UUID> opened = new HashSet<>();
    private final Set<UUID> lastDoored = new HashSet<>();
    private final Set<String> missingLogged = new HashSet<>();

    /**
     * Record one real sonic boom for a boss and ask whether
     * it speaks, and with which clip. Empty when the cooldown
     * swallows this boom or the chapter carries no boom clips.
     * The first boom of a fight always speaks (the taunt
     * rule's shape); after that, at most one boom line per
     * cooldown. Spoken booms alternate boom / boom_alt; when
     * only one of the pair is configured it repeats.
     */
    public synchronized Optional<String> onBoom(UUID bossUuid, long nowMs,
                                                long cooldownMs,
                                                VoiceSettings settings) {
        if (settings == null
                || (settings.boom().isEmpty() && settings.boomAlt().isEmpty())) {
            return Optional.empty();
        }
        BoomState st = booms.computeIfAbsent(bossUuid, k -> new BoomState());
        if (st.spoken > 0 && nowMs - st.lastBoomMs < Math.max(0L, cooldownMs)) {
            return Optional.empty();
        }
        String slug = st.spoken % 2 == 0 ? settings.boom() : settings.boomAlt();
        if (slug.isEmpty()) {
            slug = settings.boom().isEmpty() ? settings.boomAlt() : settings.boom();
        }
        Optional<String> event = BossVoice.eventId(BossKind.WARDEN, slug);
        if (event.isEmpty()) {
            return Optional.empty();
        }
        st.spoken++;
        st.lastBoomMs = nowMs;
        return event;
    }

    /** How many boom lines this boss has spoken this fight. */
    public synchronized int boomsSpoken(UUID bossUuid) {
        BoomState st = booms.get(bossUuid);
        return st == null ? 0 : st.spoken;
    }

    /** Mark an encounter's open line as fired: true only the
     *  first time for this boss (the open speaks once). */
    public synchronized boolean markOpened(UUID bossUuid) {
        return opened.add(bossUuid);
    }

    /** Mark the elder last-door line as fired for this boss:
     *  true only the first time, whichever seam reaches it
     *  first (the relay at progress 2, or the last elder's
     *  own encounter opening at progress 2). */
    public synchronized boolean markLastDoor(UUID bossUuid) {
        return lastDoored.add(bossUuid);
    }

    /** True only the first time this missing-clip key is
     *  asked about (the host logs the miss then, and stays
     *  quiet on every later fight with the same gap). */
    public synchronized boolean shouldLogMissing(String key) {
        return missingLogged.add(key);
    }

    /** Forget one boss (its encounter closed). */
    public synchronized void close(UUID bossUuid) {
        booms.remove(bossUuid);
        opened.remove(bossUuid);
        lastDoored.remove(bossUuid);
    }

    /** Forget every boss not in the live set (the encounter
     *  map's prune sweep - state never outlives its fight).
     *  The missing-clip log set is per-boot, not per-fight,
     *  and deliberately survives. */
    public synchronized void retainOnly(Set<UUID> liveBosses) {
        booms.keySet().retainAll(liveBosses);
        opened.retainAll(liveBosses);
        lastDoored.retainAll(liveBosses);
    }

    public synchronized boolean isEmpty() {
        return booms.isEmpty() && opened.isEmpty() && lastDoored.isEmpty();
    }
}
