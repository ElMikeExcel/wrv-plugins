package com.winkreyes.buildmode.core;

import java.io.IOException;
import java.util.*;

/** Serial/main-thread operations. A thrown error always leaves recovery available if entry was committed. */
public final class SessionCoordinator {
    public interface PlayerPort {
        byte[] capture() throws Exception;
        void enterBuild() throws Exception;
        void restore(byte[] snapshot) throws Exception;
        void savePlayerData() throws Exception;
    }
    /** Production ports must acknowledge persisted marker writes, not merely invoke a void save API. */
    public interface MarkerPort extends PlayerPort {
        void commitBuildMarker(RecoveryJournal.Session session) throws Exception;
        void validateRecoveryMarker(RecoveryJournal.Session session) throws Exception;
        void commitRestoredMarker(RecoveryJournal.Session session) throws Exception;
        void clearRecoveryMarker(RecoveryJournal.Session session) throws Exception;
    }
    private final RecoveryJournal journal;
    private final RewardLedger rewards;
    private final ActiveSessionIndex index;
    public SessionCoordinator(RecoveryJournal journal) { this(journal, null); }
    public SessionCoordinator(RecoveryJournal journal, RewardLedger rewards) { this(journal, rewards, null); }
    public SessionCoordinator(RecoveryJournal journal, RewardLedger rewards, ActiveSessionIndex index) {
        this.journal = journal; this.rewards = rewards; this.index = index;
    }
    public void enter(UUID player, PlayerPort port) throws Exception {
        if (index != null && !(port instanceof MarkerPort)) throw new IOException("Verified persistent marker support is required");
        if (journal.active(player).isPresent()) throw new IOException("Recovery already pending");
        final RecoveryJournal.Session[] prepared = new RecoveryJournal.Session[1];
        RewardLedger.DurablePreparation preparation = () -> {
            prepared[0] = journal.begin(player, port.capture());
            if (index != null) index.begin(prepared[0]);
        };
        if (rewards == null) preparation.run();
        else rewards.admit(player, () -> {
            try { preparation.run(); }
            catch (Exception failure) {
                // A write can report failure AFTER its atomic commit. Publish recovery before releasing the lock.
                try { if (journal.active(player).isPresent()) rewards.guardRecovery(player); }
                catch (Exception unreadable) { rewards.guardRecovery(player); failure.addSuppressed(unreadable); }
                throw failure;
            }
        });
        // Failure from this point intentionally retains the immutable journal; caller quarantines/retries.
        // Marker/save/event-bearing callbacks are outside the admission lock.
        if (index != null) ((MarkerPort) port).commitBuildMarker(prepared[0]);
        port.enterBuild();
        port.savePlayerData();
    }
    public boolean recover(UUID player, PlayerPort port) throws Exception {
        var session = journal.active(player);
        if (session.isEmpty()) {
            if (index != null && index.read(player).filter(ActiveSessionIndex.Entry::active).isPresent())
                throw new IOException("Active index has no active journal snapshot; administrator review is required");
            if (rewards != null && rewards.isGuarded(player))
                throw new IOException("Guarded recovery has no active journal record; administrator review is required");
            return false;
        }
        if (index != null) {
            if (!(port instanceof MarkerPort)) throw new IOException("Verified persistent marker support is required");
            index.validate(session.get());
            ((MarkerPort) port).validateRecoveryMarker(session.get());
        }
        if (rewards != null) rewards.guardRecovery(player);
        port.restore(session.get().snapshot());
        if (index != null) ((MarkerPort) port).commitRestoredMarker(session.get());
        else port.savePlayerData();
        journal.complete(session.get());
        if (index != null) {
            index.complete(session.get());
            ((MarkerPort) port).clearRecoveryMarker(session.get());
        }
        if (rewards != null) rewards.releaseGuard(player);
        return true;
    }
}
