package com.winkreyes.exceltrainingdummy.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One dummy's test: a VIRTUAL health pool. The dummy entity never
 * takes real damage (the listener zeroes it after reading the final
 * damage), so vanilla death — and with it XP, loot, mcMMO/Jobs/economy
 * and boss-credit paths — never fires: clean by construction
 * (DESIGN §7). This class owns the numbers: pool, checkpoints at
 * each 25% of the pool (GW2 style, DESIGN §3), per-attacker ledgers,
 * the tester's hazard damage taken / healing done, and the finish
 * state. Endless mode (DESIGN §2): the pool is a lap — reaching it
 * finishes the lap, the caller prints the report and resets.
 *
 * Time is injected (millis) so the whole class is unit-testable.
 */
public final class TestSession {

    /** Checkpoint fractions of the pool CONSUMED, ascending. */
    public static final double[] CHECKPOINT_FRACTIONS = {0.25, 0.50, 0.75};

    private final Tier tier;
    private final double pool;
    private final boolean endless;
    private final boolean executeStart;
    private boolean checkpointsEnabled;

    private double consumedAtStart;   // execute preset pre-consumes 75%
    private double totalDamage;       // damage dealt during this test (all attackers)
    private long startMillis = -1;
    private long finishMillis = -1;
    private int checkpointsFired;
    private boolean finished;
    private boolean resetting;        // endless: report printed, waiting to reset
    private int lap = 1;

    private final Map<UUID, DamageLedger> ledgers = new LinkedHashMap<>();
    private double hazardDamageTaken; // by the tester (owner), hazardous mode
    private double healingDone;       // by the tester (owner) during the test

    public TestSession(Tier tier, double pool, boolean endless,
                       boolean executeStart, boolean checkpointsEnabled) {
        this.tier = tier;
        this.pool = pool;
        this.endless = endless;
        this.executeStart = executeStart;
        this.checkpointsEnabled = checkpointsEnabled;
        this.consumedAtStart = executeStart ? pool * 0.75 : 0;
        // Checkpoints at or below the execute start are already past.
        this.checkpointsFired = checkpointsAlreadyPassed();
    }

    private int checkpointsAlreadyPassed() {
        int n = 0;
        double frac = pool <= 0 ? 1 : consumedAtStart / pool;
        for (double f : CHECKPOINT_FRACTIONS) if (frac >= f) n++;
        return n;
    }

    // ---- damage intake -------------------------------------------------

    /**
     * Records one hit. Returns the checkpoint fraction crossed by this
     * hit (0.25 / 0.50 / 0.75) or 0 when none crossed — the caller
     * prints the running-DPS checkpoint line. Returns -1 when this hit
     * finished the test (pool consumed). A hit is capped at the
     * remaining pool: overkill never inflates a report.
     */
    public double recordDamage(UUID attacker, DamageLedger.Category category,
                               double amount, long nowMillis) {
        if (finished || resetting || amount <= 0) return 0;
        if (startMillis < 0) startMillis = nowMillis;
        double applied = Math.min(amount, remaining());
        if (applied <= 0) return 0;
        ledgers.computeIfAbsent(attacker, k -> new DamageLedger()).add(category, applied);
        totalDamage += applied;
        if (remaining() <= 0) {
            finished = !endless;         // endless: lap finished, caller resets us
            if (endless) resetting = true;
            finishMillis = nowMillis;
            return -1;
        }
        if (checkpointsEnabled) {
            double frac = consumedFraction();
            while (checkpointsFired < CHECKPOINT_FRACTIONS.length
                    && frac >= CHECKPOINT_FRACTIONS[checkpointsFired]) {
                double crossed = CHECKPOINT_FRACTIONS[checkpointsFired];
                checkpointsFired++;
                return crossed;
            }
        }
        return 0;
    }

    /** Endless reset: a fresh lap with clean ledgers and checkpoints. */
    public void resetLap(long nowMillis) {
        lap++;
        totalDamage = 0;
        ledgers.clear();
        hazardDamageTaken = 0;
        healingDone = 0;
        startMillis = -1;
        finishMillis = -1;
        finished = false;
        resetting = false;
        consumedAtStart = executeStart ? pool * 0.75 : 0;
        checkpointsFired = checkpointsAlreadyPassed();
    }

    /** Live toggle from the dummy settings menu (counter state is kept). */
    public void setCheckpointsEnabled(boolean enabled) { this.checkpointsEnabled = enabled; }
    public boolean checkpointsEnabled() { return checkpointsEnabled; }

    /** Execute reset from the dummy settings menu: restart at 25% HP. */
    public void restartAtExecute(long nowMillis) {
        totalDamage = 0;
        ledgers.clear();
        hazardDamageTaken = 0;
        healingDone = 0;
        startMillis = -1;
        finishMillis = -1;
        finished = false;
        resetting = false;
        consumedAtStart = pool * 0.75;
        checkpointsFired = checkpointsAlreadyPassed();
    }

    // ---- tester (owner) hazard / healing -------------------------------

    public void recordHazardDamage(double amount) {
        if (amount > 0) hazardDamageTaken += amount;
    }

    public void recordHealing(double amount) {
        if (amount > 0) healingDone += amount;
    }

    // ---- reads -----------------------------------------------------------

    public Tier tier() { return tier; }
    public double pool() { return pool; }
    public boolean endless() { return endless; }
    public boolean executeStart() { return executeStart; }
    public boolean finished() { return finished; }
    public boolean resetting() { return resetting; }
    public int lap() { return lap; }

    public double consumed() { return consumedAtStart + totalDamage; }
    public double remaining() { return Math.max(0, pool - consumed()); }
    public double consumedFraction() { return pool <= 0 ? 1 : consumed() / pool; }
    public double totalDamage() { return totalDamage; }
    public double hazardDamageTaken() { return hazardDamageTaken; }
    public double healingDone() { return healingDone; }

    public DamageLedger ledger(UUID attacker) { return ledgers.get(attacker); }
    public Map<UUID, DamageLedger> ledgers() { return ledgers; }

    public long startMillis() { return startMillis; }

    /** Elapsed test seconds: first hit to finish (or to `now` while live). */
    public double elapsedSeconds(long nowMillis) {
        if (startMillis < 0) return 0;
        long end = finishMillis >= 0 ? finishMillis : nowMillis;
        return Math.max(0, (end - startMillis) / 1000.0);
    }

    /** Running or final average DPS for one attacker. */
    public double dps(UUID attacker, long nowMillis) {
        DamageLedger l = ledgers.get(attacker);
        double secs = elapsedSeconds(nowMillis);
        if (l == null || secs <= 0) return 0;
        return l.total() / secs;
    }

    public double hps(long nowMillis) {
        double secs = elapsedSeconds(nowMillis);
        return secs <= 0 ? 0 : healingDone / secs;
    }
}
