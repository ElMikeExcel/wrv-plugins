package com.winkreyes.buildmode.core;

import java.util.*;

/**
 * The relog decision (ExcelBuildMode 1.3), as a pure function so the
 * whole matrix is unit-testable without a server.
 *
 * Design call (Mike, Oct 20, 2026, Option 1): a player who logs off in
 * Build Mode is restored INTO Build Mode on relog — gamemode, flight
 * and the Jobs-suppression session attachment are re-applied as one
 * state, airborne or not. The grounding/teleport variant was rejected:
 * it only patched the airborne symptom and cost the builder his
 * working position on every relog.
 *
 * Hard invariant: a relog NEVER yields Survival + flight + Jobs pay
 * (or Survival + WorldEdit) together. RESTORE_BUILD is reachable only
 * with a build-return record that names the currently active journal
 * session — the same session whose survival snapshot the journal
 * still holds. Every other guarded state falls back to the standard
 * survival recovery (which itself lands an airborne player under a
 * Jobs-denied hold before restoring), and a guarded player whose
 * journal session has vanished entirely fails closed for an
 * administrator, exactly as before.
 */
public final class BuildReturnPolicy {
    private BuildReturnPolicy() { }

    public enum Decision { RESTORE_BUILD, RECOVER_SURVIVAL, FAIL_CLOSED }

    /**
     * @param record          the build-return record read at join
     * @param activeSession   the recovery journal's active session id for this player, if any
     */
    public static Decision decide(BuildReturnStore.Record record, Optional<UUID> activeSession) {
        if (record instanceof BuildReturnStore.Record.Present present) {
            if (activeSession.isPresent() && activeSession.get().equals(present.session())) return Decision.RESTORE_BUILD;
            // A record pointing at a session the journal no longer holds
            // is stale or corrupt: never restore a mode from it.
            return activeSession.isPresent() ? Decision.RECOVER_SURVIVAL : Decision.FAIL_CLOSED;
        }
        // Absent or corrupt record: the fallback is the full survival
        // recovery — when a session exists to recover from. With no
        // session at all, the guarded state itself is unverifiable.
        return activeSession.isPresent() ? Decision.RECOVER_SURVIVAL : Decision.FAIL_CLOSED;
    }
}
