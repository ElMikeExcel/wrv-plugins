package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import com.winkreyes.buildmode.paper.BuildPolicy;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Standalone, dependency-free assertions: run using build.sh; all data stays under target/test-work. */
public final class CoreTests {
    private static int assertions;
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final byte[] SURVIVAL = "synthetic-survival-inventory-xp-flight".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static Path root;
    private static Path fixture(String label) throws IOException { return Files.createTempDirectory(root, label + "-"); }
    public static void main(String[] args) throws Exception {
        root = Path.of("target/test-work"); Files.createDirectories(root);
        roundTripAndRetain(); duplicateAndStale(); crashRecovery(); corruption(); faultMatrix(); portFailures(); interleavings(); policy();
        System.out.println("PASS " + assertions + " assertions (journal/coordinator/policy; no Bukkit client simulation)");
        Files.writeString(Path.of("target/core-test-result.txt"), "PASS " + assertions + " assertions\n");
    }
    private static void roundTripAndRetain() throws Exception {
        Path dir = fixture("roundtrip"); RecoveryJournal journal = new RecoveryJournal(dir);
        FakePort port = new FakePort(); SessionCoordinator core = new SessionCoordinator(journal);
        check(journal.active(PLAYER).isEmpty(), "new player is not pending");
        core.enter(PLAYER, port); check(port.build, "enters build"); check(port.saves == 1, "saves after entry");
        var session = journal.active(PLAYER).orElseThrow(); check(Arrays.equals(session.snapshot(), SURVIVAL), "exact snapshot");
        byte[] mutatedCopy = session.snapshot(); mutatedCopy[0] ^= 5;
        check(Arrays.equals(session.snapshot(), SURVIVAL), "session defensive accessor");
        check(core.recover(PLAYER, port), "restored"); check(!port.build, "not build after restore");
        check(Arrays.equals(port.current, SURVIVAL), "survival restored"); check(port.saves == 2, "saved after restoration");
        check(journal.active(PLAYER).isEmpty(), "no pending restore after commit");
        check(!core.recover(PLAYER, port), "no double restore"); check(port.restores == 1, "restore called once");
        Path archive = dir.resolve(PLAYER.toString()).resolve(session.id().toString());
        check(Files.exists(archive.resolve("survival.bin")) && Files.exists(archive.resolve("survival.backup.bin")), "immutable snapshots retained after exit");
        check(journal.players().equals(Set.of(PLAYER)), "player index covers finished record");
    }
    private static void duplicateAndStale() throws Exception {
        RecoveryJournal journal = new RecoveryJournal(fixture("stale"));
        var first = journal.begin(PLAYER, SURVIVAL);
        fails(() -> journal.begin(PLAYER, "overwrite".getBytes()), "duplicate active session refused");
        check(Arrays.equals(journal.active(PLAYER).orElseThrow().snapshot(), SURVIVAL), "duplicate cannot replace original");
        journal.complete(first); var second = journal.begin(PLAYER, "next survival".getBytes());
        fails(() -> journal.complete(first), "stale completion refused");
        check(journal.active(PLAYER).orElseThrow().id().equals(second.id()), "new session survives stale completion");
        byte[] input = SURVIVAL.clone(); RecoveryJournal.Session copy = new RecoveryJournal.Session(PLAYER, UUID.randomUUID(), input); input[0] ^= 5;
        check(Arrays.equals(copy.snapshot(), SURVIVAL), "session defensive constructor");
        fails(() -> journal.begin(UUID.randomUUID(), new byte[0]), "empty snapshot refused");
        fails(() -> journal.begin(UUID.randomUUID(), new byte[RecoveryJournal.MAX_BYTES + 1]), "oversize snapshot refused");
    }
    private static void crashRecovery() throws Exception {
        Path dir = fixture("restart"); RecoveryJournal first = new RecoveryJournal(dir); first.begin(PLAYER, SURVIVAL);
        RecoveryJournal second = new RecoveryJournal(dir); check(second.active(PLAYER).isPresent(), "crash after durable entry recovered");
        FakePort port = new FakePort(); port.current = new byte[]{9}; port.build = true;
        new SessionCoordinator(second).recover(PLAYER, port); check(Arrays.equals(port.current, SURVIVAL), "restart restores original rather than creative data");
        check(new RecoveryJournal(dir).active(PLAYER).isEmpty(), "completed restart stays complete");
    }
    private static void corruption() throws Exception {
        Path dir = fixture("corruption"); RecoveryJournal journal = new RecoveryJournal(dir); var s = journal.begin(PLAYER, SURVIVAL);
        Path archive = dir.resolve(PLAYER.toString()).resolve(s.id().toString());
        byte[] original = Files.readAllBytes(archive.resolve("survival.bin"));
        byte[] corrupt = original.clone(); corrupt[corrupt.length - 1] ^= 1; Files.write(archive.resolve("survival.bin"), corrupt);
        check(Arrays.equals(journal.active(PLAYER).orElseThrow().snapshot(), SURVIVAL), "primary corruption recovers backup");
        Files.write(archive.resolve("survival.backup.bin"), corrupt);
        fails(() -> journal.active(PLAYER), "both corrupt copies fail closed");
        FakePort port = new FakePort(); fails(() -> new SessionCoordinator(journal).recover(PLAYER, port), "corrupt recovery refused");
        check(port.restores == 0 && port.saves == 0, "corruption performs no mutation");
        fails(() -> journal.begin(PLAYER, new byte[]{1}), "corruption cannot be replaced with fresh snapshot");
        Files.write(archive.resolve("survival.bin"), original); Files.write(archive.resolve("survival.backup.bin"), original);
        byte[] state = Files.readAllBytes(dir.resolve(PLAYER.toString()).resolve("state.bin"));
        state[state.length - 1] ^= 1; Files.write(dir.resolve(PLAYER.toString()).resolve("state.bin"), state);
        fails(() -> journal.active(PLAYER), "state corruption fails closed");
        fails(() -> journal.begin(PLAYER, SURVIVAL), "corrupt state cannot be overwritten");
        for (int size : new int[]{0, 4, 39, 40, 41}) {
            Path broken = fixture("truncated"); RecoveryJournal j = new RecoveryJournal(broken); var row = j.begin(PLAYER, SURVIVAL);
            Path a = broken.resolve(PLAYER.toString()).resolve(row.id().toString());
            Files.write(a.resolve("survival.bin"), new byte[size]); Files.write(a.resolve("survival.backup.bin"), new byte[size]);
            fails(() -> j.active(PLAYER), "invalid sizes fail closed " + size);
        }
    }
    private static void faultMatrix() throws Exception {
        for (String point : List.of("primary", "backup", "verified", "state_temp", "committed")) {
            Path dir = fixture("fault-" + point);
            RecoveryJournal journal = new RecoveryJournal(dir, stage -> { if (stage.equals(point)) throw new IOException("injected " + point); });
            FakePort port = new FakePort(); fails(() -> new SessionCoordinator(journal).enter(PLAYER, port), "entry failure surfaced at " + point);
            check(port.enters == 0 && port.saves == 0, "no destructive mutation before durable success " + point);
            var pending = new RecoveryJournal(dir).active(PLAYER);
            check(pending.isPresent() == point.equals("committed"), "restart has correct pending record " + point);
            if (pending.isPresent()) check(Arrays.equals(pending.get().snapshot(), SURVIVAL), "commit error retains snapshot");
        }
        for (String point : List.of("state_temp", "before_complete", "completed")) {
            Path dir = fixture("restore-fault-" + point); RecoveryJournal first = new RecoveryJournal(dir); first.begin(PLAYER, SURVIVAL);
            RecoveryJournal faulty = new RecoveryJournal(dir, stage -> { if (stage.equals(point)) throw new IOException("injected " + point); });
            FakePort port = new FakePort(); fails(() -> new SessionCoordinator(faulty).recover(PLAYER, port), "completion failure surfaced " + point);
            check(port.restores == 1 && port.saves == 1, "complete only after restore and save " + point);
            check(new RecoveryJournal(dir).active(PLAYER).isPresent() != point.equals("completed"), "pending retained unless completion committed " + point);
        }
    }
    private static void portFailures() throws Exception {
        for (String point : List.of("capture", "enter", "save", "restore")) {
            Path dir = fixture("port-" + point); RecoveryJournal journal = new RecoveryJournal(dir); FakePort port = new FakePort();
            SessionCoordinator core = new SessionCoordinator(journal);
            if (point.equals("restore")) core.enter(PLAYER, port);
            port.fail = point;
            fails(() -> { if (point.equals("restore")) core.recover(PLAYER, port); else core.enter(PLAYER, port); }, "port failure " + point);
            check(journal.active(PLAYER).isPresent() != point.equals("capture"), "snapshot remains after mutation failure " + point);
            if (!point.equals("capture")) {
                check(Arrays.equals(journal.active(PLAYER).orElseThrow().snapshot(), SURVIVAL), "no mutated snapshot " + point);
                port.fail = ""; core.recover(PLAYER, port); check(Arrays.equals(port.current, SURVIVAL), "retry repairs " + point);
            }
        }
        RecoveryJournal journal = new RecoveryJournal(fixture("restore-save")); FakePort port = new FakePort(); SessionCoordinator core = new SessionCoordinator(journal);
        core.enter(PLAYER, port); port.fail = "save";
        fails(() -> core.recover(PLAYER, port), "save failure after restore");
        check(journal.active(PLAYER).isPresent(), "save failure cannot complete journal");
        port.fail = ""; core.recover(PLAYER, port); check(port.restores == 2, "repeated exact restoration supported");
    }
    private static void interleavings() throws Exception {
        // Exhaust every enter/recover trace of length 8 (256 traces), recreating storage to model reloads.
        for (int pattern = 0; pattern < 256; pattern++) {
            Path dir = fixture("trace"); FakePort p = new FakePort(); boolean active = false;
            for (int i = 0; i < 8; i++) {
                RecoveryJournal journal = new RecoveryJournal(dir); SessionCoordinator core = new SessionCoordinator(journal);
                boolean enter = ((pattern >> i) & 1) == 1;
                if (enter && active) { fails(() -> core.enter(PLAYER, p), "repeat enter cannot resnapshot"); }
                else if (enter) { core.enter(PLAYER, p); active = true; }
                else { check(core.recover(PLAYER, p) == active, "recover status tracks durable state"); active = false; }
                check(journal.active(PLAYER).isPresent() == active, "trace journal state");
                if (active) check(Arrays.equals(journal.active(PLAYER).orElseThrow().snapshot(), SURVIVAL), "trace keeps survival immutable");
                else check(Arrays.equals(p.current, SURVIVAL), "trace recovers survival exactly");
            }
        }
    }
    private static void policy() {
        for (String command : List.of("/buildmode", "/buildmode off", "/EXCELBUILDMODE:buildmode status"))
            check(BuildPolicy.allowsCommand(command, false), "self command permitted");
        for (String command : List.of("/op Marvin", "/lp user Marvin permission set * true", "/gamemode creative Mike", "/minecraft:gamemode creative", "/eco give Marvin 10", "/shop", "/essentials:shop", "/pay Mike 10", "/worldedit:/schem load test", "//schem save test", "/we", "/worldedit reload", "//script test", "//limit -1", "//setbiome desert", "//regen", "//set stone"))
            check(!BuildPolicy.allowsCommand(command, false), "default command guard denies " + command);
        for (String command : List.of("//set stone", "//pos1", "//copy", "/worldedit:/paste", "//undo"))
            check(BuildPolicy.allowsCommand(command, true), "allowlisted WE root " + command);
        for (String command : List.of("/op Marvin", "//schem load foo", "/we reload", "//limit -1", "//regen", "/worldedit:reload", "//setbiome forest"))
            check(!BuildPolicy.allowsCommand(command, true), "WE enable does not allow admin roots");
        for (String history : List.of("/undo", "/redo", "//undo", "//redo", "/worldedit:undo", "/worldedit:redo", "/worldedit:/undo", "/worldedit:/redo")) {
            check(BuildPolicy.allowsCommand(history, true), "native and alias history command allowed " + history);
            check(!BuildPolicy.allowsCommand(history, false), "history command disabled without WorldEdit " + history);
        }
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.containsAll(Set.of("worldedit.history.undo.self", "worldedit.history.redo.self")), "self-only history nodes granted");
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.history.undo") && !BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.history.redo"), "broader history nodes omitted");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().noneMatch(node -> node.endsWith(".other")), "other-player permissions omitted");
        for (String chat : List.of("/msg Mike hello", "/essentials:reply hello", "/minecraft:tell Mike hello", "/bukkit:help", "/list")) {
            check(BuildPolicy.allowsCommand(chat, false, true), "chat/help option allows " + chat);
            check(!BuildPolicy.allowsCommand(chat, false, false), "chat/help option disabled " + chat);
        }
        check(!BuildPolicy.allowsCommand("/untrustedplugin:msg hello", false), "unknown namespaced command does not inherit chat allowance");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().noneMatch(s -> s.contains("*") || s.contains("bypass") || s.contains("admin") || s.contains("reload") || s.contains("schematic")), "attachment has no broad/dangerous node");
    }
    private static final class FakePort implements SessionCoordinator.PlayerPort {
        byte[] current = SURVIVAL.clone(); boolean build; int enters, restores, saves; String fail = "";
        private void maybe(String stage) throws IOException { if (fail.equals(stage)) throw new IOException("port failure " + stage); }
        public byte[] capture() throws Exception { maybe("capture"); return current.clone(); }
        public void enterBuild() throws Exception { enters++; current = new byte[]{0}; build = true; maybe("enter"); }
        public void restore(byte[] data) throws Exception { restores++; current = data.clone(); build = false; maybe("restore"); }
        public void savePlayerData() throws Exception { saves++; maybe("save"); }
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void fails(Checked operation, String label) throws Exception {
        try { operation.run(); throw new AssertionError("Expected failure: " + label); }
        catch (IOException expected) { assertions++; }
    }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); assertions++; }
}
