package excelemfixes.mcmmo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Per-hit provenance for the per-damage veto.
 *
 * <p>mcMMO's XP gain events carry the earner, the skill and the
 * amount — never the entity that was hit. So the module cannot
 * ask mcMMO "did this gain come from an EliteMobs entity?"; it
 * keeps its own receipts instead. Every player (or pet) hit on
 * an EliteMobs-tracked entity plants a token here; when a PVE
 * gain event for the same player and skill arrives within the
 * token's time-to-live, the token is consumed and the gain is
 * vetoed. This is the same plant-and-poll shape the house
 * already runs for BossTrial band verdicts (a per-hit FIFO with
 * a short TTL, polled at gain time).
 *
 * <p>Party shares need their own handling: mcMMO delivers each
 * member's share through that member's own gain event BEFORE
 * the earner's own event (the earner is appended last), and a
 * share carries the SHARED_PVE reason. A token therefore is not
 * consumed by share vetoes; it carries a share budget (the most
 * shares one hit can plausibly mint — bounded by party size)
 * that share vetoes draw down, and the token itself is consumed
 * by the earner's own gain or expires by TTL.
 *
 * <p>Pure logic (no Bukkit): ticks are always passed in.
 */
public final class HitTokens {

    /** One planted hit receipt. */
    public static final class Token {
        private final UUID earner;
        private final String skill;
        private final long plantedTick;
        private final UUID target;
        private int shareBudget;

        Token(UUID earner, String skill, long plantedTick, UUID target,
              int shareBudget) {
            this.earner = earner;
            this.skill = skill;
            this.plantedTick = plantedTick;
            this.target = target;
            this.shareBudget = shareBudget;
        }

        public UUID earner() {
            return earner;
        }

        public String skill() {
            return skill;
        }

        public long plantedTick() {
            return plantedTick;
        }

        public UUID target() {
            return target;
        }

        public int shareBudget() {
            return shareBudget;
        }
    }

    private final Deque<Token> queue = new ArrayDeque<>();

    private void prune(long nowTick, long ttlTicks) {
        queue.removeIf(t -> nowTick - t.plantedTick > ttlTicks);
    }

    /** Plants a token for one landed hit on an EliteMobs entity. */
    public synchronized void plant(UUID earner, String skill, long tick,
                                   UUID target, int shareBudget) {
        if (earner == null || skill == null) return;
        queue.addLast(new Token(earner, skill, tick, target,
                Math.max(0, shareBudget)));
    }

    /**
     * Consumes the oldest live token for (earner, skill) — the
     * earner's own PVE gain matched a planted EliteMobs hit.
     * Returns true when a token matched (the gain is vetoed).
     */
    public synchronized boolean consumeForGain(UUID earner, String skill,
                                               long nowTick, long ttlTicks) {
        prune(nowTick, ttlTicks);
        Token match = null;
        for (Token t : queue) {
            if (t.earner.equals(earner) && t.skill.equals(skill)) {
                match = t;
                break;
            }
        }
        if (match == null) return false;
        queue.remove(match);
        return true;
    }

    /**
     * Finds a live token whose earner could have sourced a party
     * share in {@code skill} and still has share budget; the
     * caller checks the party relationship itself (it needs the
     * mcMMO API this pure class must not touch) and then calls
     * {@link #spendShareBudget(Token)} to draw the budget down.
     * Returns the candidate tokens oldest-first.
     */
    public synchronized List<Token> shareCandidates(String skill,
                                                    long nowTick,
                                                    long ttlTicks) {
        prune(nowTick, ttlTicks);
        List<Token> out = new ArrayList<>();
        for (Token t : queue) {
            if (t.skill.equals(skill) && t.shareBudget > 0) out.add(t);
        }
        return out;
    }

    /** Draws one share veto from a token's budget. */
    public synchronized void spendShareBudget(Token token) {
        if (token != null && token.shareBudget > 0) token.shareBudget--;
    }

    /** Live token count (tests and debug). */
    public synchronized int size() {
        return queue.size();
    }

    /**
     * Live tokens planted for one (earner, skill) — the
     * watchdog's flag context: how many EliteMobs-hit receipts
     * were outstanding when a suspicious gain rate was seen.
     */
    public synchronized int liveCountFor(UUID earner, String skill,
                                         long nowTick, long ttlTicks) {
        prune(nowTick, ttlTicks);
        int count = 0;
        for (Token t : queue) {
            if (t.earner.equals(earner) && t.skill.equals(skill)) {
                count++;
            }
        }
        return count;
    }
}
