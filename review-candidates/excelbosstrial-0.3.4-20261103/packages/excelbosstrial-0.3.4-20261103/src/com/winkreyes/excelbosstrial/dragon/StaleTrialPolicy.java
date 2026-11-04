/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

/**
 * 0.2.11 (B-4 — Mike's decision, Oct 30, 2026: "WIPE the saved fight
 * on update — start clean"): whether persisted dragon-trial state may
 * be restored at boot. Pure logic; DragonChapter.loadState owns the
 * file work (archive, closure log, fresh stamped save).
 *
 * The rule: state recorded under a DIFFERENT plugin build than the
 * one booting is not restored when it holds an OPEN battle — the
 * battle is closed out instead and the old state file archived. An
 * absent stamp counts as a different build, so the first boot of a
 * stamping build over pre-stamp state wipes. The Oct 30 dragon kill
 * is why: a stale Oct 27 PHASE1 trial restored under a newer build
 * fought under rules its saved state never anticipated.
 *
 * Scope discipline: only the open battle closes. Completed-battle
 * counters are kept, DONE/NONE worlds load exactly as before, and
 * everything outside the chapter state — seal credits, reward
 * claims, signatures paid, monument counts — is never touched here.
 */
public final class StaleTrialPolicy {

    /** What boot should do with the loaded state file. */
    public enum Action {
        /** Restore the saved state exactly as before this rule existed. */
        RESTORE,
        /** Close the open battle(s) out and archive the saved state. */
        CLOSE_STALE
    }

    private StaleTrialPolicy() {
    }

    /** An open battle is a live phase with a dragon on record. */
    public static boolean isOpen(BattlePhase phase, String dragonUuid) {
        return dragonUuid != null && phase != null
                && phase != BattlePhase.NONE && phase != BattlePhase.DONE;
    }

    /**
     * Decide the fate of one loaded state file.
     *
     * @param savedBuild    the build stamp stored with the state, or
     *                      null when the state predates stamping
     * @param currentBuild  the build booting now
     * @param anyOpenBattle true when any world in the state holds an
     *                      open battle (see {@link #isOpen})
     */
    public static Action onLoad(String savedBuild, String currentBuild, boolean anyOpenBattle) {
        if (!anyOpenBattle) {
            return Action.RESTORE;
        }
        if (savedBuild != null && savedBuild.equals(currentBuild)) {
            return Action.RESTORE;
        }
        return Action.CLOSE_STALE;
    }

    /** Archive file name for a wiped state file (name only, no directory). */
    public static String archiveName(String savedBuild) {
        String tag = savedBuild == null || savedBuild.isBlank()
                ? "unstamped"
                : savedBuild.replaceAll("[^A-Za-z0-9._-]", "_");
        return "state.yml.archived-" + tag;
    }
}
