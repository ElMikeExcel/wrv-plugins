package com.winkreyes.excelguildgate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who has been offered the narrator pack on Java, and what happened -
 * the offer discipline behind ExcelGuildGate 0.3.2's Java narrator
 * delivery.
 *
 * The shape follows ExcelQuartermaster 0.2.1's live-proven delivery
 * (fixed pack id, addResourcePack, persisted per-player state), but
 * the DISCIPLINE is Mike's rule of Oct 19, 2026, which supersedes the
 * Quartermaster's 30-day / max-3 prompt-once numbers for this plugin:
 * players tap-spam prompts without reading them, so everyone gets
 * FIVE chances, the prompt itself shows the count ("offer N of 5")
 * and says plainly that after 5 offers - or once they download it -
 * it will not ask again. Then it keeps that promise:
 *
 * - Platform gate first: only a CONFIRMED Java player is ever
 *   offered the pack. Bedrock players already get the narrator
 *   through Geyser's forced .mcpack, and an unknown platform is
 *   treated as "do not send" - a Java pack pushed at a Bedrock
 *   client whose platform check merely glitched would be worse
 *   than not sending.
 * - Offers happen on join, at most one per join. A player with no
 *   record gets offer 1; each send is counted as it happens
 *   (applySend), so the counter the player sees is the count the
 *   state machine enforces, and the count survives restarts in
 *   narrator-pack-state.txt - there is no path to an offer 6.
 * - LOADED (the player accepted and the pack applied): never
 *   offered again, ever. They have it; asking again is spam.
 * - Any other outcome - declined, failed download, or no outcome
 *   recorded (left mid-download) - simply spends the chance: while
 *   total offers stay under the cap the next join brings the next
 *   offer. There is no waiting period and no reset cycle.
 * - The 5th offer without an accept is the last: at the cap, every
 *   later decision is SKIP, forever.
 *
 * Attempts count SENDS (applySend), never status events, so the cap
 * cannot be dodged or tripped by client chatter.
 *
 * No Bukkit dependencies: the whole state machine is unit-testable
 * on a plain JVM. The Bukkit wiring calls {@link #decideOffer} on
 * join, hands the pack to the player when it says SEND (with the
 * prompt text built by NarratorPack.buildPrompt for the offer
 * number {@link #nextOfferNumber} returns), and reports the outcome
 * back through {@link #applySend} / {@link #applyStatus}. Records
 * are persisted by NarratorPackStateStore.
 */
public final class NarratorPackPrompt {

    /** The last known outcome for one player. */
    public enum Outcome {
        SENT, LOADED, DECLINED, FAILED
    }

    /**
     * @param outcome          last known outcome
     * @param attempts         total pack offers made so far (never negative)
     * @param lastChangeMillis when the outcome last changed (or the
     *                         last offer, for SENT)
     */
    public record Record(Outcome outcome, int attempts, long lastChangeMillis) {}

    /** What the join wiring should do for one player. */
    public enum Action {
        SEND, SKIP
    }

    /** The platform a joining player is confirmed to be on. */
    public enum Platform {
        JAVA, BEDROCK, UNKNOWN
    }

    private final Map<UUID, Record> players = new LinkedHashMap<>();

    /** A player's record, or null if they have never been offered the pack. */
    public synchronized Record record(UUID playerId) {
        return playerId == null ? null : players.get(playerId);
    }

    /** How many players carry a pack record. */
    public synchronized int trackedCount() {
        return players.size();
    }

    /** Snapshot of every record. */
    public synchronized Map<UUID, Record> snapshot() {
        return new LinkedHashMap<>(players);
    }

    /** Restore persisted records. Null keys/values are skipped;
     *  attempts and timestamps clamp at 0. */
    public synchronized void restore(Map<UUID, Record> records) {
        players.clear();
        if (records != null) {
            for (Map.Entry<UUID, Record> e : records.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && e.getValue().outcome() != null) {
                    players.put(e.getKey(), new Record(e.getValue().outcome(),
                            Math.max(0, e.getValue().attempts()),
                            Math.max(0L, e.getValue().lastChangeMillis())));
                }
            }
        }
    }

    /** The outcome of one {@link #reset} call. */
    public enum ResetResult {
        /** The record was cleared; the next join offers 1 of maxOffers. */
        RESET,
        /** The player already has the pack; the record was left untouched. */
        ALREADY_HAS_PACK,
        /** The player had no record; nothing happened. */
        NO_RECORD
    }

    /** Counts from one {@link #resetAll} call. */
    public record ResetSummary(int resetCount, int keptAcceptedCount) {}

    /**
     * The manual escape hatch (Mike, Oct 19, 2026): clear one
     * player's offer record so their offers start again at 1 - for
     * the player who swears they never saw the prompt. A player who
     * already ACCEPTED the pack is not a reset case: their record is
     * evidence they have it, so it is left untouched and reported
     * as ALREADY_HAS_PACK. Clearing removes the record entirely,
     * which is exactly the never-offered state.
     */
    public synchronized ResetResult reset(UUID playerId) {
        if (playerId == null) {
            return ResetResult.NO_RECORD;
        }
        Record rec = players.get(playerId);
        if (rec == null) {
            return ResetResult.NO_RECORD;
        }
        if (rec.outcome() == Outcome.LOADED) {
            return ResetResult.ALREADY_HAS_PACK;
        }
        players.remove(playerId);
        return ResetResult.RESET;
    }

    /**
     * Reset every non-accepted player at once. Accepted players are
     * kept and counted, everyone else's record is cleared.
     */
    public synchronized ResetSummary resetAll() {
        int reset = 0;
        int kept = 0;
        java.util.Iterator<Map.Entry<UUID, Record>> it = players.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Record> e = it.next();
            if (e.getValue().outcome() == Outcome.LOADED) {
                kept++;
            } else {
                it.remove();
                reset++;
            }
        }
        return new ResetSummary(reset, kept);
    }

    /** Record that the pack was just offered to this player (one
     *  chance spent, whatever happens next). */
    public synchronized Record applySend(UUID playerId, long nowMillis) {
        if (playerId == null) {
            return null;
        }
        Record prev = players.get(playerId);
        Record next = new Record(Outcome.SENT,
                (prev == null ? 0 : prev.attempts()) + 1, nowMillis);
        players.put(playerId, next);
        return next;
    }

    /**
     * Fold one client status into the record. Returns true when the
     * record changed (so the caller persists). IGNORED statuses and
     * null players never change anything.
     */
    public synchronized boolean applyStatus(UUID playerId, NarratorPack.Status status,
                                            long nowMillis) {
        if (playerId == null || status == null || status == NarratorPack.Status.IGNORED) {
            return false;
        }
        Outcome outcome = switch (status) {
            case LOADED -> Outcome.LOADED;
            case DECLINED -> Outcome.DECLINED;
            case FAILED -> Outcome.FAILED;
            case IGNORED -> null; // unreachable; guarded above
        };
        if (outcome == null) {
            return false;
        }
        Record prev = players.get(playerId);
        Record next = new Record(outcome, prev == null ? 0 : prev.attempts(), nowMillis);
        if (next.equals(prev)) {
            return false;
        }
        players.put(playerId, next);
        return true;
    }

    /**
     * The number of the NEXT offer to this player (1 for a player
     * with no record). This is the N the prompt shows ("offer N of
     * 5"); the wiring asks only when {@link #decideOffer} said SEND,
     * so N never exceeds the cap.
     */
    public static int nextOfferNumber(Record record) {
        return (record == null ? 0 : Math.max(0, record.attempts())) + 1;
    }

    /**
     * The full join decision: the platform gate, then the discipline.
     * Pure: same inputs, same answer, no state read or written.
     * Anything but a confirmed Java player is a SKIP, whatever their
     * record says - Bedrock is served by Geyser, and unknown is never
     * guessed at.
     */
    public static Action decideOffer(Platform platform, Record record, int maxOffers) {
        if (platform != Platform.JAVA) {
            return Action.SKIP;
        }
        return decide(record, maxOffers);
    }

    /**
     * The discipline for one confirmed-Java player. Pure: same
     * inputs, same answer, no state read or written. See the class
     * docs for the rules. {@code maxOffers} below 1 is treated as 1.
     */
    public static Action decide(Record record, int maxOffers) {
        if (record == null) {
            return Action.SEND;
        }
        if (record.outcome() == Outcome.LOADED) {
            return Action.SKIP; // accepted: never offered again, ever
        }
        int max = Math.max(1, maxOffers);
        return record.attempts() < max ? Action.SEND : Action.SKIP;
    }
}
