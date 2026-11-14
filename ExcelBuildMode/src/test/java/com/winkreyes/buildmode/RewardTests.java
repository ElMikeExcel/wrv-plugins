package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import com.winkreyes.buildmode.paper.ExcelBuildMode;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Deterministic core/failure tests and concurrent linearization probes. No fake provider attestation is a release approval. */
public final class RewardTests {
    private static int assertions;
    private static final UUID ACTOR = new UUID(1, 1), RECIPIENT = new UUID(1, 2);
    private static final byte[] SURVIVAL = {1, 2, 3, 4};
    private static final Path ROOT = Path.of("target/reward-test-work");
    @FunctionalInterface interface Checked { void run() throws Exception; }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(ROOT);
        registration(); immutableOrigins(); recipientReservations(); leases(); invalidHandles(); shutdownAndEpoch(); capacityBounds();
        durableAdmission(); recoveryFailures(); callbackLockBoundary(); publicationBoundary(); races(); bridgeShape();
        String result = "PASS " + assertions + " assertions (reward ledger, coordinator failures, reflection contract, 1000 concurrent admission races; no provider/runtime coverage claim)\n";
        System.out.print(result); Files.writeString(Path.of("target/reward-test-result.txt"), result);
    }
    private static RewardLedger ready() {
        RewardLedger ledger = new RewardLedger(Set.of("Jobs"));
        check(ledger.registerProvider("Jobs", "test-1.0", coverage("Jobs")), "Jobs manifest accepted");
        return ledger;
    }
    private static String[] coverage(String provider) { return RewardLedger.COVERAGE.get(provider).toArray(String[]::new); }
    private static UUID begin(RewardLedger r, UUID actor) { return r.begin(actor, "Jobs", "action.test", null, true); }
    private static void registration() throws Exception {
        RewardLedger r = new RewardLedger(RewardLedger.COVERAGE.keySet());
        check(r.readinessProblem() != null, "loaded unpatched providers block admission");
        AtomicInteger prepares = new AtomicInteger();
        reject(() -> r.admit(ACTOR, prepares::incrementAndGet), "missing coverage rejects");
        check(prepares.get() == 0 && !r.isGuarded(ACTOR), "missing coverage performs no snapshot or suppression");
        check(!r.registerProvider(null, "1", new String[0]), "null provider rejected");
        check(!r.registerProvider("unknown", "1", new String[0]), "unknown provider rejected");
        check(!r.registerProvider("jobs", "1", coverage("Jobs")), "provider identity is case sensitive");
        for (String name : RewardLedger.COVERAGE.keySet()) {
            check(!r.registerProvider(name, null, coverage(name)), "null revision rejected");
            check(!r.registerProvider(name, " \n", coverage(name)), "blank revision rejected");
            check(!r.registerProvider(name, "1", null), "null coverage rejected");
            check(!r.registerProvider(name, "1", new String[0]), "empty coverage rejected");
            var missing = new ArrayList<>(RewardLedger.COVERAGE.get(name)); missing.removeFirst();
            check(!r.registerProvider(name, "1", missing.toArray(String[]::new)), "partial coverage rejected");
            var extra = new ArrayList<>(RewardLedger.COVERAGE.get(name)); extra.add("extra");
            check(!r.registerProvider(name, "1", extra.toArray(String[]::new)), "superset coverage rejected");
            extra.set(extra.size() - 1, extra.getFirst());
            check(!r.registerProvider(name, "1", extra.toArray(String[]::new)), "duplicate coverage rejected");
            extra.set(extra.size() - 1, null);
            check(!r.registerProvider(name, "1", extra.toArray(String[]::new)), "null member rejected");
            check(r.registerProvider(name, "1", coverage(name)), "exact coverage accepted");
            String[] reverse = coverage(name); Collections.reverse(Arrays.asList(reverse));
            check(r.registerProvider(name, "1", reverse), "same manifest reordered is idempotent");
        }
        check(r.readinessProblem() == null, "all exact provider attestations ready");
        check(!r.registerProvider("Jobs", "different", coverage("Jobs")), "provider replacement refused");
        check(r.readinessProblem().contains("Jobs"), "provider replacement poisons new entry");
        check(!r.registerProvider("Jobs", "1", coverage("Jobs")), "replacement cannot be undone by reregistering");
        RewardLedger absent = new RewardLedger(Set.of()); absent.admit(ACTOR, () -> {});
        check(absent.isGuarded(ACTOR), "no installed providers needs no registration");
        absent.providerLoaded("mcMMO"); check(absent.readinessProblem() != null, "hot loaded provider blocks new entry");
        absent.providerLoaded(null); absent.providerUnavailable(null);
    }
    private static void immutableOrigins() throws Exception {
        RewardLedger r = ready(); UUID normal = begin(r, ACTOR);
        check(r.isAllowed(normal) && r.generation(normal) == 0, "normal origin allowed at generation zero");
        check(r.pending(ACTOR) == 1, "Survival origin reserves pending work");
        reject(() -> r.admit(ACTOR, () -> {}), "earned Survival work must drain");
        check(!r.isGuarded(ACTOR) && r.isAllowed(normal), "blocked entry does not suppress existing Survival origin");
        r.complete(normal); r.admit(ACTOR, () -> {});
        UUID denied = begin(r, ACTOR);
        check(!r.isAllowed(denied) && r.generation(denied) == 1, "Build origin has immutable denied generation");
        check(r.pending(ACTOR) == 0, "denied root does not block later recovery");
        r.guardRecovery(ACTOR); check(r.generation(begin(r, ACTOR)) == 1, "recovery/landing guard does not create another session");
        r.releaseGuard(ACTOR);
        check(!r.isAllowed(denied), "Build origin remains denied after exit");
        UUID survivor = begin(r, ACTOR); check(r.isAllowed(survivor), "fresh normal origin resumes after release"); r.complete(survivor);
        r.admit(ACTOR, () -> {}); UUID next = begin(r, ACTOR);
        check(!r.isAllowed(next) && r.generation(next) == 2 && r.generation(denied) == 1, "sessions never rewrite earlier origin generation");
        r.releaseGuard(ACTOR);
        UUID veto = r.begin(ACTOR, "Jobs", "farm", RECIPIENT, false);
        check(veto != null && !r.isAllowed(veto), "independent farm veto creates explicit denied root");
        UUID child = r.fork(veto, RECIPIENT); check(child != null && !r.isAllowed(child), "farm veto cannot be overridden by sharing");
        r.complete(denied); r.complete(next); r.complete(veto); r.complete(child);
    }
    private static void recipientReservations() throws Exception {
        RewardLedger r = ready(); UUID parent = begin(r, ACTOR);
        check(r.canReceive(parent, RECIPIENT) && r.pending(RECIPIENT) == 0, "eligibility preview reserves nothing");
        r.admit(RECIPIENT, () -> {}); UUID denied = r.fork(parent, RECIPIENT);
        check(denied != null && !r.isAllowed(denied), "recipient guard imposes a denied child after a stale preview");
        r.releaseGuard(RECIPIENT); check(!r.isAllowed(denied), "denied recipient child immutable after exit");
        UUID accepted = r.fork(parent, RECIPIENT); check(r.isAllowed(accepted) && r.pending(RECIPIENT) == 1, "eligible child reserves recipient atomically");
        reject(() -> r.admit(RECIPIENT, () -> {}), "accepted child blocks recipient entry until award finishes");
        r.complete(parent); check(r.isAllowed(accepted), "child outlives parent");
        check(r.fork(parent, RECIPIENT) == null, "closed parent cannot be recaptured");
        r.complete(accepted); r.admit(RECIPIENT, () -> {});
        check(r.isGuarded(RECIPIENT), "recipient admits once sharing work drains"); r.complete(denied);
        check(r.openTokens() == 0 && r.pending(ACTOR) == 0 && r.pending(RECIPIENT) == 0, "recipient accounting fully drains");
        r.releaseGuard(RECIPIENT); r.admit(ACTOR, () -> {}); UUID build = begin(r, ACTOR);
        UUID normalChild = r.fork(build, RECIPIENT);
        check(!r.isAllowed(normalChild) && !r.canReceive(build, RECIPIENT), "normal recipient cannot rescue denied original actor");
        r.complete(normalChild); r.complete(build);
    }
    private static void leases() throws Exception {
        RewardLedger r = ready(); UUID root = begin(r, ACTOR);
        check(r.retain(root) && r.retain(root), "multiple deferred owners can retain before publishing");
        check(r.pending(ACTOR) == 1, "references retain one pending reservation");
        r.complete(root); r.complete(root);
        check(r.isAllowed(root) && r.pending(ACTOR) == 1, "partial releases keep reward alive");
        reject(() -> r.admit(ACTOR, () -> {}), "last deferred owner still blocks entry");
        r.complete(root); check(!r.isAllowed(root) && r.pending(ACTOR) == 0, "last owner closes reservation");
        r.complete(root); check(r.pending(ACTOR) == 0, "repeat release AFTER closure is no-op");
        check(!r.retain(root), "closed handle cannot retain");
    }
    private static void invalidHandles() {
        RewardLedger r = ready();
        for (UUID id : Arrays.asList(null, UUID.randomUUID())) {
            check(!r.isAllowed(id), "unknown/null allowed false"); check(r.generation(id) == -1, "unknown/null generation sentinel");
            check(!r.canReceive(id, RECIPIENT), "unknown/null receive false"); check(r.fork(id, RECIPIENT) == null, "unknown/null fork null");
            check(!r.retain(id), "unknown/null retain false"); r.complete(id);
        }
        check(r.begin(null, "Jobs", "test", null, true) == null, "null actor fails closed");
        check(r.begin(ACTOR, null, "test", null, true) == null, "null provider fails closed");
        check(r.begin(ACTOR, "Jobs", null, null, true) == null, "null source fails closed");
        check(r.begin(ACTOR, "Jobs", "  ", null, true) == null, "blank source fails closed");
        check(r.begin(ACTOR, "mcMMO", "test", null, true) == null, "unregistered provider fails closed");
        UUID root = begin(r, ACTOR); check(!r.canReceive(root, null) && r.fork(root, null) == null, "null recipient fails closed"); r.complete(root);
    }
    private static void shutdownAndEpoch() throws Exception {
        RewardLedger r = ready(), next = ready(); UUID root = begin(r, ACTOR);
        check(!r.epoch().equals(next.epoch()), "replacement has a new epoch");
        check(!next.isAllowed(root) && next.generation(root) == -1, "old handle never translates to new helper");
        r.providerUnavailable("Jobs"); check(r.readinessProblem() != null, "provider shutdown blocks new admission");
        check(begin(r, ACTOR) == null, "provider shutdown stops new origins");
        check(r.isAllowed(root), "already classified Survival work retains eligibility");
        r.closeOrigins(); check(begin(r, ACTOR) == null, "helper shutdown stops origins");
        check(!r.registerProvider("Jobs", "test-1.0", coverage("Jobs")), "stopping helper refuses registration");
        reject(() -> r.admit(RECIPIENT, () -> {}), "stopping helper refuses admission");
        check(r.retain(root), "existing owner can finish a known callback chain during stop");
        r.complete(root); r.complete(root); check(r.pending(ACTOR) == 0, "stopping ledger still drains existing leases");
    }
    private static void capacityBounds() throws Exception {
        RewardLedger r = new RewardLedger(Set.of("Jobs"), 2, 2);
        check(r.registerProvider("Jobs", "test", coverage("Jobs")), "bounded fixture registered");
        UUID first = begin(r, ACTOR), second = begin(r, RECIPIENT);
        try { r.fork(first, RECIPIENT); throw new AssertionError("token capacity must throw"); }
        catch (RewardLedger.CapacityExceeded expected) { check(true, "token capacity failure explicit"); }
        check(r.openTokens() == 2 && r.isAllowed(first) && r.isAllowed(second), "saturation never evicts earned work");
        reject(() -> r.admit(new UUID(2, 3), () -> {}), "saturation sticky admission refusal");
        r.complete(first); r.complete(second);
        check(r.openTokens() == 0 && r.pending(ACTOR) == 0 && r.pending(RECIPIENT) == 0, "existing work can fully drain after saturation");
        reject(() -> r.admit(ACTOR, () -> {}), "draining does not silently clear saturation fault");
        UUID subsequent = begin(r, ACTOR); check(r.isAllowed(subsequent), "normal earning may continue after capacity drains while Build remains blocked"); r.complete(subsequent);
        RewardLedger sessions = new RewardLedger(Set.of(), 2, 1);
        sessions.admit(ACTOR, () -> {}); sessions.releaseGuard(ACTOR);
        AtomicInteger snapshots = new AtomicInteger();
        reject(() -> sessions.admit(RECIPIENT, snapshots::incrementAndGet), "identity cap refuses before snapshot");
        check(snapshots.get() == 0 && !sessions.isGuarded(RECIPIENT), "identity capacity does not suppress ordinary Survival");
        sessions.guardRecovery(RECIPIENT); check(sessions.isGuarded(RECIPIENT), "durable recovery guard is never dropped even over capacity");
        RewardLedger cold = new RewardLedger(Set.of("Jobs"));
        check(!cold.isActorGuarded(ACTOR), "unguarded cold incomplete provider can preserve ordinary Survival");
        cold.guardRecovery(ACTOR); check(cold.isActorGuarded(ACTOR) && !cold.isActorGuarded(RECIPIENT), "cold state query distinguishes startup recovery");
        check(cold.isActorGuarded(null), "null actor query fails closed"); cold.closeOrigins(); check(cold.isActorGuarded(RECIPIENT), "stopped helper never claims actor is safe");
    }
    private static final class Port implements SessionCoordinator.PlayerPort {
        RewardLedger ledger; String fail = ""; int captures, enters, restores, saves; boolean build; byte[] current = SURVIVAL.clone();
        Port(RewardLedger ledger) { this.ledger = ledger; }
        public byte[] capture() throws Exception { captures++; if (fail.equals("capture")) throw new IOException("capture"); check(!ledger.isGuarded(ACTOR), "capture happens before guard publication"); return current.clone(); }
        public void enterBuild() throws Exception { enters++; check(ledger.isGuarded(ACTOR), "guard precedes destructive entry"); current = new byte[]{9}; build = true; if (fail.equals("enter")) throw new IOException("cancelled Creative entry"); }
        public void restore(byte[] bytes) throws Exception { restores++; check(ledger.isGuarded(ACTOR), "guard remains during inventory restoration"); if (fail.equals("restore")) throw new IOException("restore"); current = bytes.clone(); build = false; }
        public void savePlayerData() throws Exception { saves++; check(ledger.isGuarded(ACTOR), "guard remains during vanilla save attempt"); if (fail.equals("save")) throw new IOException("save"); }
    }
    private static void durableAdmission() throws Exception {
        for (String point : List.of("capture", "primary", "backup", "verified", "state_temp", "committed", "enter", "save")) {
            RewardLedger r = ready(); Path dir = Files.createTempDirectory(ROOT, "admit-" + point);
            RecoveryJournal journal = new RecoveryJournal(dir, stage -> { if (stage.equals(point)) throw new IOException(point); });
            Port port = new Port(r); port.fail = point; SessionCoordinator core = new SessionCoordinator(journal, r);
            failure(() -> core.enter(ACTOR, port), "entry fault surfaced " + point);
            boolean committed = Set.of("committed", "enter", "save").contains(point);
            check(r.isGuarded(ACTOR) == committed, "only committed recovery is guarded " + point);
            check(journal.active(ACTOR).isPresent() == committed, "journal commit boundary preserved " + point);
            check(port.enters == (Set.of("enter", "save").contains(point) ? 1 : 0), "no early destructive entry " + point);
            UUID reward = begin(r, ACTOR); check(r.isAllowed(reward) != committed, "failed precommit entry does not suppress Survival " + point); r.complete(reward);
            if (committed) {
                port.fail = ""; new SessionCoordinator(new RecoveryJournal(dir), r).recover(ACTOR, port);
                check(!r.isGuarded(ACTOR) && Arrays.equals(port.current, SURVIVAL), "failed entry safely restores " + point);
            }
        }
        RewardLedger r = ready(); RecoveryJournal j = new RecoveryJournal(Files.createTempDirectory(ROOT, "pending")); Port port = new Port(r);
        UUID earned = begin(r, ACTOR); reject(() -> new SessionCoordinator(j, r).enter(ACTOR, port), "pending gate before capture");
        check(port.captures == 0 && port.enters == 0 && !r.isGuarded(ACTOR), "pending refusal has no inventory or mode effect"); r.complete(earned);
    }
    private static void recoveryFailures() throws Exception {
        for (String point : List.of("restore", "save", "before_complete", "state_temp", "completed")) {
            RewardLedger r = ready(); Path dir = Files.createTempDirectory(ROOT, "restore-" + point);
            RecoveryJournal journal = new RecoveryJournal(dir); Port port = new Port(r); new SessionCoordinator(journal, r).enter(ACTOR, port);
            port.fail = point; RecoveryJournal faulty = new RecoveryJournal(dir, stage -> { if (stage.equals(point)) throw new IOException(point); });
            failure(() -> new SessionCoordinator(faulty, r).recover(ACTOR, port), "recovery fault surfaced " + point);
            check(r.isGuarded(ACTOR), "failure retains guard " + point); UUID delayed = begin(r, ACTOR);
            check(!r.isAllowed(delayed), "failed/landing/recovery reward denied " + point);
            port.fail = "";
            if (!point.equals("completed")) {
                new SessionCoordinator(new RecoveryJournal(dir), r).recover(ACTOR, port);
                check(!r.isGuarded(ACTOR), "successful recovery releases only after journal completion " + point);
                check(!r.isAllowed(delayed), "old failure-origin stays denied after recovery " + point);
            } else {
                failure(() -> new SessionCoordinator(new RecoveryJournal(dir), r).recover(ACTOR, port), "ambiguous postcommit failure is not silently unguarded");
                check(!new RecoveryJournal(dir).active(ACTOR).isPresent(), "postcommit record is tombstoned but failed current lifetime stays guarded");
            }
            r.complete(delayed);
        }
        RewardLedger r = ready(); RecoveryJournal j = new RecoveryJournal(Files.createTempDirectory(ROOT, "restart")); j.begin(ACTOR, SURVIVAL);
        r.guardRecovery(ACTOR); UUID beforeJoin = begin(r, ACTOR); check(!r.isAllowed(beforeJoin), "startup record guards before provider origins");
        new SessionCoordinator(j, r).recover(ACTOR, new Port(r)); check(!r.isGuarded(ACTOR) && !r.isAllowed(beforeJoin), "rejoin cannot rescue startup-denied origin"); r.complete(beforeJoin);
    }
    private static void callbackLockBoundary() throws Exception {
        RewardLedger r = ready(); RecoveryJournal journal = new RecoveryJournal(Files.createTempDirectory(ROOT, "callback"));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            SessionCoordinator.PlayerPort port = new SessionCoordinator.PlayerPort() {
                public byte[] capture() { return SURVIVAL; }
                private void callback() throws Exception {
                    UUID id = pool.submit(() -> begin(r, ACTOR)).get(3, TimeUnit.SECONDS);
                    check(id != null && !r.isAllowed(id), "async provider callback sees guard without deadlocking"); r.complete(id);
                }
                public void enterBuild() throws Exception { callback(); }
                public void restore(byte[] snapshot) throws Exception { callback(); }
                public void savePlayerData() throws Exception { callback(); }
            };
            SessionCoordinator c = new SessionCoordinator(journal, r); c.enter(ACTOR, port); c.recover(ACTOR, port);
            check(!r.isGuarded(ACTOR), "no lock held over event-bearing callbacks or save");
        } finally { pool.shutdownNow(); }
    }
    private static void publicationBoundary() throws Exception {
        for (boolean failCapture : List.of(false, true)) {
            RewardLedger r = ready(); CountDownLatch capture = new CountDownLatch(1), allowCapture = new CountDownLatch(1), producer = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<?> enter = pool.submit(() -> { try { r.admit(ACTOR, () -> { capture.countDown(); allowCapture.await(); if (failCapture) throw new IOException("capture"); }); } catch (Exception e) { if (!failCapture) throw new CompletionException(e); } });
                check(capture.await(3, TimeUnit.SECONDS), "admission reaches durable preparation");
                Future<UUID> origin = pool.submit(() -> { producer.countDown(); return begin(r, ACTOR); });
                check(producer.await(3, TimeUnit.SECONDS), "concurrent producer starts");
                check(!origin.isDone(), "producer cannot bypass snapshot/admission critical section");
                allowCapture.countDown(); enter.get(3, TimeUnit.SECONDS); UUID result = origin.get(3, TimeUnit.SECONDS);
                check(r.isAllowed(result) == failCapture, "origin linearizes after completed or failed preparation");
                check(r.isGuarded(ACTOR) != failCapture, "snapshot failure never publishes suppression"); r.complete(result);
            } finally { allowCapture.countDown(); pool.shutdownNow(); }
        }
    }
    private static void races() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 1000; i++) {
                RewardLedger r = ready(); boolean sharing = (i & 1) != 0; UUID actor = sharing ? RECIPIENT : ACTOR;
                UUID parent = sharing ? begin(r, ACTOR) : null; CyclicBarrier start = new CyclicBarrier(2);
                Future<UUID> origin = pool.submit(() -> { start.await(); return sharing ? r.fork(parent, actor) : begin(r, actor); });
                Future<Boolean> admission = pool.submit(() -> { start.await(); try { r.admit(actor, () -> {}); return true; } catch (RewardLedger.AdmissionRejected e) { return false; } });
                UUID id = origin.get(3, TimeUnit.SECONDS); boolean admitted = admission.get(3, TimeUnit.SECONDS);
                check(r.isAllowed(id) != admitted, "linearization forbids both allowed origin and Build admission");
                check(r.isGuarded(actor) == admitted, "race guard matches admission result");
                check(r.pending(actor) == (admitted ? 0 : 1), "race pending matches reservation");
                r.complete(id); if (parent != null) r.complete(parent);
                check(r.openTokens() == 0 && r.pending(actor) == 0, "race leases drain without underflow");
            }
        } finally { pool.shutdownNow(); }
    }
    private static void bridgeShape() throws Exception {
        Class<?> c = ExcelBuildMode.class;
        check(c.getMethod("rewardIsActorGuarded", UUID.class).getReturnType() == boolean.class, "cold state query signature");
        check(c.getMethod("rewardApiVersion").getReturnType() == int.class, "API version signature");
        check(c.getMethod("rewardApiEpoch").getReturnType() == UUID.class, "API epoch signature");
        check(c.getMethod("rewardRegisterProvider", String.class, String.class, String[].class).getReturnType() == boolean.class, "register signature");
        check(c.getMethod("rewardBegin", UUID.class, String.class, String.class, UUID.class, boolean.class).getReturnType() == UUID.class, "begin signature");
        for (String method : List.of("rewardIsAllowed", "rewardRetain")) check(c.getMethod(method, UUID.class).getReturnType() == boolean.class, "boolean token signature");
        check(c.getMethod("rewardGeneration", UUID.class).getReturnType() == long.class, "generation signature");
        check(c.getMethod("rewardCanReceive", UUID.class, UUID.class).getReturnType() == boolean.class, "recipient preview signature");
        check(c.getMethod("rewardFork", UUID.class, UUID.class).getReturnType() == UUID.class, "fork signature");
        check(c.getMethod("rewardComplete", UUID.class).getReturnType() == void.class, "completion signature");
    }
    private static void check(boolean condition, String text) { if (!condition) throw new AssertionError(text); assertions++; }
    private static void failure(Checked work, String text) throws Exception { try { work.run(); } catch (IOException expected) { assertions++; return; } throw new AssertionError(text); }
    private static void reject(Checked work, String text) throws Exception { try { work.run(); } catch (RewardLedger.AdmissionRejected expected) { assertions++; return; } throw new AssertionError(text); }
}
