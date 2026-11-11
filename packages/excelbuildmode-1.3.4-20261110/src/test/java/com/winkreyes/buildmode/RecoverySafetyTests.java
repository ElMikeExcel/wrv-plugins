package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Pure persistence/admission policy tests. Native marker serialization is tested separately. */
public final class RecoverySafetyTests {
    private static final UUID PLAYER = new UUID(701, 1), OTHER = new UUID(701, 2);
    private static final byte[] ORIGINAL = {1, 2, 3, 4, 5};
    private static int assertions;
    public static void main(String[] args) throws Exception {
        cohort(); storage(); index(); reviewHolds(); ordering(); crashBoundaries();
        System.out.println("PASS " + assertions + " recovery/storage assertions");
    }
    private static void cohort() throws Exception {
        Path root = Files.createTempDirectory("builder-cohort-test");
        Path a = root.resolve("a"), b = root.resolve("b");
        check(!CohortStore.inspect(a, b).verified(), "missing cohort refuses entry");
        Files.write(a, CohortStore.encode(Set.of(PLAYER)));
        var one = CohortStore.inspect(a, b);
        check(!one.verified() && one.known().equals(Set.of(PLAYER)), "single surviving replica preserves builder identity but refuses entry");
        Files.write(b, Files.readAllBytes(a));
        check(CohortStore.inspect(a, b).verified(), "matching independently stored cohort replicas accepted");
        Files.write(b, CohortStore.encode(Set.of(OTHER)));
        var inconsistent = CohortStore.inspect(a, b);
        check(!inconsistent.verified() && inconsistent.known().equals(Set.of(PLAYER, OTHER)), "disagreement retains union for recovery and refuses entry");
        Files.write(b, new byte[]{0});
        check(CohortStore.inspect(a, b).known().contains(PLAYER), "corrupt replica does not erase known identity");
        var policy = new RecoveryCohort(Set.of(PLAYER, OTHER), Set.of(PLAYER));
        check(policy.mayEnter(PLAYER) && !policy.mayEnter(OTHER), "entry eligibility can shrink without dropping historical cohort");
        check(policy.mustLockWhenJournalUnknown(OTHER), "former builder retains recovery protection");
        check(!policy.mustLockWhenJournalUnknown(new UUID(701, 3)), "unrelated ordinary player is outside cohort lock");
        try { new RecoveryCohort(Set.of(PLAYER), Set.of(OTHER)); throw new AssertionError("Unknown builder accepted"); }
        catch (IllegalArgumentException good) { assertions++; }
    }
    private static void storage() throws Exception {
        Path root = Files.createTempDirectory("builder-storage-test");
        StorageAdmission low = new StorageAdmission(1_000_000, 100_000, 2, path -> 100_001);
        fails(() -> low.beforeEntry(root, ORIGINAL.length), "reserve accounts for the pending write rather than current free bytes only");
        try (var files = Files.list(root)) { check(files.findAny().isEmpty(), "storage refusal writes no snapshot or metadata"); }
        StorageAdmission budget = new StorageAdmission(1_000_000, 100_000, 1, path -> Long.MAX_VALUE);
        RecoveryJournal journal = new RecoveryJournal(root, stage -> {}, budget);
        var session = journal.begin(PLAYER, ORIGINAL);
        check(budget.inspect(root).archives() == 1, "snapshot archive counted");
        journal.complete(session);
        long bytes = budget.inspect(root).bytes();
        fails(() -> journal.begin(PLAYER, ORIGINAL), "retained-session budget refuses next entry");
        check(budget.inspect(root).bytes() == bytes, "refusal does not prune immutable snapshots");
        check(Files.exists(root.resolve(PLAYER.toString()).resolve(session.id().toString()).resolve("survival.bin")), "completed snapshot remains recoverable");
        Path root2 = Files.createTempDirectory("builder-recovery-budget-test");
        RecoveryJournal first = new RecoveryJournal(root2); var active = first.begin(PLAYER, ORIGINAL);
        RecoveryJournal full = new RecoveryJournal(root2, stage -> {}, new StorageAdmission(1, Long.MAX_VALUE, 1, path -> 0));
        check(Arrays.equals(full.active(PLAYER).orElseThrow().snapshot(), ORIGINAL), "recovery reads ignore exhausted entry budget");
        full.complete(active); check(full.active(PLAYER).isEmpty(), "completion ignores exhausted entry budget");
        Path root3 = Files.createTempDirectory("builder-byte-budget-test");
        fails(() -> new StorageAdmission(100, 0, 100, path -> Long.MAX_VALUE).beforeEntry(root3, ORIGINAL.length), "logical byte budget counts write margin");
    }
    private static void index() throws Exception {
        Path root = Files.createTempDirectory("builder-index-test"); ActiveSessionIndex index = new ActiveSessionIndex(root);
        var session = new RecoveryJournal.Session(PLAYER, UUID.randomUUID(), ORIGINAL);
        check(index.read(PLAYER).isEmpty(), "new index genuinely absent");
        index.begin(session); check(index.read(PLAYER).orElseThrow().active(), "index activates before Creative mutation");
        check(index.players().equals(Set.of(PLAYER)), "independent enumeration preserves active actor");
        fails(() -> index.validate(new RecoveryJournal.Session(PLAYER, UUID.randomUUID(), ORIGINAL)), "mismatched session cannot restore or clear");
        index.complete(session); check(!index.read(PLAYER).orElseThrow().active(), "completion stores tombstone");
        Files.write(root.resolve(PLAYER + ".idx"), new byte[]{1});
        fails(() -> index.read(PLAYER), "corrupt index is unknown, not empty");
    }
    private static void ordering() throws Exception {
        Path base = Files.createTempDirectory("builder-order-test");
        RecoveryJournal journal = new RecoveryJournal(base.resolve("journal")); ActiveSessionIndex index = new ActiveSessionIndex(base.resolve("index"));
        RewardLedger rewards = new RewardLedger(Set.of()); Port port = new Port(journal, index, rewards);
        SessionCoordinator coordinator = new SessionCoordinator(journal, rewards, index);
        coordinator.enter(PLAYER, port);
        check(port.events.equals(List.of("capture", "marker-active", "creative", "save")), "snapshot/index/marker precede Creative and final save");
        check(port.marker.startsWith("v1:active:"), "active marker stays present during Build");
        coordinator.recover(PLAYER, port);
        check(port.events.subList(4, 7).equals(List.of("restore", "marker-restored", "marker-clear")), "restore acknowledgement precedes journal/index completion then clear");
        check(!rewards.isGuarded(PLAYER) && port.marker == null && Arrays.equals(port.items, ORIGINAL), "successful completion releases only after marker clear");
        rewards.blockAdmission("storage unknown");
        check(!rewards.isActorGuarded(OTHER), "operational entry refusal never suppresses unrelated Survival");
        fails(() -> coordinator.enter(PLAYER, port), "operational failure prevents new entry");
    }
    private static void reviewHolds() throws Exception {
        Path root = Files.createTempDirectory("builder-review-hold-test");
        ReviewHoldStore holds = new ReviewHoldStore(root);
        check(holds.players().isEmpty(), "fresh provisioned review store has no holds");
        holds.hold(PLAYER); check(holds.players().equals(Set.of(PLAYER)), "unknown actor gets only its own durable hold");
        byte[] first = Files.readAllBytes(root.resolve(PLAYER + ".hold"));
        holds.hold(PLAYER); check(Arrays.equals(first, Files.readAllBytes(root.resolve(PLAYER + ".hold"))), "duplicate hold preserves original record");
        check(new ReviewHoldStore(root).players().contains(PLAYER), "review hold survives a new store instance/native rewrite");
        check(!new ReviewHoldStore(root).players().contains(OTHER), "unrelated ordinary player never inherits the hold");
        Files.write(root.resolve(PLAYER + ".hold"), new byte[0]);
        check(new ReviewHoldStore(root).players().contains(PLAYER), "corrupt hold content remains a hold rather than false absence");
        fails(() -> new ReviewHoldStore(root.resolve("missing")), "missing hold storage is not silently created as empty history");
    }
    private static void crashBoundaries() throws Exception {
        for (String failure : List.of("marker-active", "creative", "save", "marker-restored", "marker-clear")) {
            Path base = Files.createTempDirectory("builder-crash-test");
            RecoveryJournal journal = new RecoveryJournal(base.resolve("journal")); ActiveSessionIndex index = new ActiveSessionIndex(base.resolve("index"));
            RewardLedger rewards = new RewardLedger(Set.of()); Port port = new Port(journal, index, rewards);
            SessionCoordinator coordinator = new SessionCoordinator(journal, rewards, index);
            if (failure.startsWith("marker-r") || failure.equals("marker-clear")) {
                coordinator.enter(PLAYER, port); port.failure = failure;
                fails(() -> coordinator.recover(PLAYER, port), "injected restore boundary " + failure);
            } else { port.failure = failure; fails(() -> coordinator.enter(PLAYER, port), "injected entry boundary " + failure); }
            check(rewards.isGuarded(PLAYER), "failure retains authoritative guard at " + failure);
            check(port.marker != null, "failure retains independent player marker at " + failure);
            if (failure.equals("marker-active")) check(port.events.stream().noneMatch("creative"::equals), "marker failure prevents any Creative mutation");
            if (failure.equals("marker-clear")) {
                check(journal.active(PLAYER).isEmpty() && !index.read(PLAYER).orElseThrow().active(), "ambiguous clear is not replayed after completed restore");
                fails(() -> coordinator.recover(PLAYER, port), "no automatic replay/unsuppression after ambiguous marker clear");
            } else check(journal.active(PLAYER).isPresent() && index.read(PLAYER).orElseThrow().active(), "recovery evidence survives " + failure);
        }
        Path base = Files.createTempDirectory("builder-missing-journal-test");
        RecoveryJournal journal = new RecoveryJournal(base.resolve("journal")); ActiveSessionIndex index = new ActiveSessionIndex(base.resolve("index"));
        index.begin(new RecoveryJournal.Session(PLAYER, UUID.randomUUID(), ORIGINAL));
        fails(() -> new SessionCoordinator(journal, new RewardLedger(Set.of()), index).recover(PLAYER, new Port(journal, index, new RewardLedger(Set.of()))), "active index never treats missing journal as completed recovery");
    }
    private static final class Port implements SessionCoordinator.MarkerPort {
        final RecoveryJournal journal; final ActiveSessionIndex index; final RewardLedger rewards; final List<String> events = new ArrayList<>();
        byte[] items = ORIGINAL.clone(); String marker, failure;
        Port(RecoveryJournal journal, ActiveSessionIndex index, RewardLedger rewards) { this.journal = journal; this.index = index; this.rewards = rewards; }
        void event(String e) throws IOException { events.add(e); if (e.equals(failure)) throw new IOException("Injected " + e); }
        public byte[] capture() throws Exception { event("capture"); return items.clone(); }
        public void commitBuildMarker(RecoveryJournal.Session session) throws Exception {
            check(journal.active(PLAYER).isPresent() && index.read(PLAYER).orElseThrow().active(), "journal and independent index durable before marker");
            check(rewards.isGuarded(PLAYER), "reward guard published before marker save callback"); marker = "v1:active:" + session.id(); event("marker-active");
        }
        public void enterBuild() throws Exception { check(marker != null, "Creative never precedes marker"); items = new byte[]{9}; event("creative"); }
        public void savePlayerData() throws Exception { event("save"); }
        public void validateRecoveryMarker(RecoveryJournal.Session session) throws Exception {
            if (marker != null && !marker.equals("v1:active:" + session.id()) && !marker.equals("v1:restored:" + session.id())) throw new IOException("Wrong marker session");
        }
        public void restore(byte[] snapshot) throws Exception { items = snapshot.clone(); event("restore"); }
        public void commitRestoredMarker(RecoveryJournal.Session session) throws Exception { check(Arrays.equals(items, ORIGINAL), "items restored before restored marker save"); marker = "v1:restored:" + session.id(); event("marker-restored"); }
        public void clearRecoveryMarker(RecoveryJournal.Session session) throws Exception {
            check(journal.active(PLAYER).isEmpty() && !index.read(PLAYER).orElseThrow().active(), "journal/index completion before marker clear");
            check(rewards.isGuarded(PLAYER), "guard remains while clearing marker"); event("marker-clear"); marker = null;
        }
    }
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static void fails(Checked work, String label) throws Exception { try { work.run(); } catch (IOException expected) { assertions++; return; } throw new AssertionError(label); }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); assertions++; }
}
