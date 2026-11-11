package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import com.winkreyes.buildmode.paper.BuildPolicy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * ExcelBuildMode 1.3.3: standalone assertions for the console admin
 * surface (core/AdminPolicy) plus an exercised restore: a stuck Build
 * Mode session is entered through the real SessionCoordinator and then
 * force-restored through the SAME coordinator recover() the paper
 * class's admin off calls — the audit's condition for this packet is
 * that the restore path is exercised, not just wired. Run via build.sh.
 * The Bukkit-side dispatch itself (console sender, getPlayerExact) is
 * covered by the 1.3.3 rig boot + console exercise, not here.
 */
public final class AdminTests {
    private static int assertions;
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000001333");
    private static final byte[] SURVIVAL = "admin-synthetic-survival".getBytes(StandardCharsets.UTF_8);
    private static final byte[] BUILD = {9, 9, 9};

    public static void main(String[] args) throws Exception {
        gateMatrix(); routing(); playerPathUnchanged(); targetClassification();
        offDecisions(); pluginYmlPins(); stuckSessionRestore(); reviewHoldRefusal();
        System.out.println("PASS " + assertions + " assertions (console admin policy + exercised restore; Bukkit dispatch covered by the rig)");
        Files.writeString(Path.of("target/admin-test-result.txt"), "PASS " + assertions + " assertions\n");
    }

    private static void gateMatrix() {
        check(AdminPolicy.mayUseAdmin(true, false), "console is explicitly allowed for the admin branch");
        check(AdminPolicy.mayUseAdmin(true, true), "console with the node allowed");
        check(AdminPolicy.mayUseAdmin(false, true), "an operator holding excelbuildmode.admin is allowed");
        check(!AdminPolicy.mayUseAdmin(false, false), "a player without the admin node is refused");
        check(AdminPolicy.PERMISSION.equals("excelbuildmode.admin"), "admin node is excelbuildmode.admin");
        check(!AdminPolicy.PERMISSION.equals(BigPastePolicy.PERMISSION), "admin node is distinct from the bigpaste node");
    }

    private static void routing() {
        // Console sender: both forms route, target carried verbatim.
        var status = AdminPolicy.route(new String[]{"admin", "status", "Marvin"}, true, false);
        check(status.route() == AdminPolicy.Route.ADMIN_STATUS && "Marvin".equals(status.target()),
            "console: admin status <player> routes with its target");
        var off = AdminPolicy.route(new String[]{"admin", "off", "Marvin"}, true, false);
        check(off.route() == AdminPolicy.Route.ADMIN_OFF && "Marvin".equals(off.target()),
            "console: admin off <player> routes with its target");
        check(AdminPolicy.route(new String[]{"ADMIN", "OFF", "Marvin"}, true, false).route() == AdminPolicy.Route.ADMIN_OFF,
            "admin routing is case-insensitive like the player paths");
        // Operator player with the node routes identically.
        check(AdminPolicy.route(new String[]{"admin", "status", "Marvin"}, false, true).route() == AdminPolicy.Route.ADMIN_STATUS,
            "operator with the node: admin status routes");
        // Permission denial: a non-admin player is refused even for a well-formed call,
        // and the denial precedes any target handling.
        check(AdminPolicy.route(new String[]{"admin", "status", "Marvin"}, false, false).route() == AdminPolicy.Route.ADMIN_DENIED,
            "non-admin player: admin status is denied");
        check(AdminPolicy.route(new String[]{"admin", "off", "Marvin"}, false, false).route() == AdminPolicy.Route.ADMIN_DENIED,
            "non-admin player: admin off is denied");
        check(AdminPolicy.route(new String[]{"admin"}, false, false).route() == AdminPolicy.Route.ADMIN_DENIED,
            "non-admin player: even bare 'admin' is denied, not a usage leak");
        // Usage errors for senders who DO hold the gate: missing target, unknown
        // subcommand, extra arguments, blank target.
        check(AdminPolicy.route(new String[]{"admin"}, true, false).route() == AdminPolicy.Route.ADMIN_USAGE,
            "bare 'admin' is a usage error");
        check(AdminPolicy.route(new String[]{"admin", "status"}, true, false).route() == AdminPolicy.Route.ADMIN_USAGE,
            "admin status without a player is a usage error");
        check(AdminPolicy.route(new String[]{"admin", "off"}, true, false).route() == AdminPolicy.Route.ADMIN_USAGE,
            "admin off without a player is a usage error");
        check(AdminPolicy.route(new String[]{"admin", "bogus", "Marvin"}, true, false).route() == AdminPolicy.Route.ADMIN_USAGE,
            "unknown admin subcommand is a usage error");
        check(AdminPolicy.route(new String[]{"admin", "off", "Marvin", "extra"}, true, false).route() == AdminPolicy.Route.ADMIN_USAGE,
            "extra arguments are a usage error, never a partial act");
        check(AdminPolicy.route(new String[]{"admin", "off", " "}, true, false).route() == AdminPolicy.Route.ADMIN_USAGE,
            "a blank target is a usage error");
    }

    private static void playerPathUnchanged() {
        // Every pre-1.3.3 invocation shape stays on the player path for every
        // sender kind — including the console, whose player-path rejection in
        // the paper class is unchanged by 1.3.3.
        String[][] shapes = {{}, {"on"}, {"off"}, {"status"}, {"bogus"}, {"on", "extra"}, {"administrator"}, {"adminx", "off", "Marvin"}};
        for (String[] shape : shapes) {
            check(AdminPolicy.route(shape, true, false).route() == AdminPolicy.Route.PLAYER_PATH,
                "console keeps the player path for " + Arrays.toString(shape));
            check(AdminPolicy.route(shape, false, false).route() == AdminPolicy.Route.PLAYER_PATH,
                "plain player keeps the player path for " + Arrays.toString(shape));
            check(AdminPolicy.route(shape, false, true).route() == AdminPolicy.Route.PLAYER_PATH,
                "operator keeps the player path for " + Arrays.toString(shape));
        }
        check(AdminPolicy.route(null, true, false).route() == AdminPolicy.Route.PLAYER_PATH,
            "a null arg array is the player path (defensive)");
    }

    private static void targetClassification() {
        // Unknown-player handling at the decision level: a name that is neither
        // online nor backed by a durable record is UNKNOWN (the paper class
        // reports it and changes nothing); a durable-only record is its own
        // class so status/off can describe the join-recovery instead of
        // pretending a live restore ran.
        check(AdminPolicy.targetKind(true, false) == AdminPolicy.TargetKind.ONLINE, "online target classifies ONLINE");
        check(AdminPolicy.targetKind(true, true) == AdminPolicy.TargetKind.ONLINE, "online wins over a durable record");
        check(AdminPolicy.targetKind(false, true) == AdminPolicy.TargetKind.DURABLE_ONLY, "offline with a record classifies DURABLE_ONLY");
        check(AdminPolicy.targetKind(false, false) == AdminPolicy.TargetKind.UNKNOWN, "offline with no record classifies UNKNOWN");
    }

    private static void offDecisions() {
        check(AdminPolicy.decideOff(true, false, true) == AdminPolicy.OffDecision.PROCEED,
            "guarded + no hold + storage ready: the restore proceeds");
        check(AdminPolicy.decideOff(false, false, true) == AdminPolicy.OffDecision.NOT_GUARDED,
            "not guarded: a clean no-op decision");
        check(AdminPolicy.decideOff(false, true, false) == AdminPolicy.OffDecision.NOT_GUARDED,
            "not guarded beats hold and storage facts (nothing to restore at all)");
        check(AdminPolicy.decideOff(true, true, true) == AdminPolicy.OffDecision.REVIEW_HOLD,
            "a review hold refuses the forced restore");
        check(AdminPolicy.decideOff(true, true, false) == AdminPolicy.OffDecision.REVIEW_HOLD,
            "a review hold is reported before a storage problem (the fact the operator must act on)");
        check(AdminPolicy.decideOff(true, false, false) == AdminPolicy.OffDecision.STORAGE_UNAVAILABLE,
            "unready recovery storage blocks the attempt");
    }

    private static void pluginYmlPins() throws Exception {
        String yml;
        try (InputStream in = AdminTests.class.getResourceAsStream("/plugin.yml")) {
            check(in != null, "plugin.yml is on the test classpath (build copies resources)");
            yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        check(yml.contains("version: '1.3.4'"), "plugin.yml declares version 1.3.4");
        check(yml.contains("excelbuildmode.admin:"), "plugin.yml registers the admin node");
        // The admin node's block must declare default op — NOT default false
        // like the use nodes (the audit's explicit condition).
        int adminAt = yml.indexOf("excelbuildmode.admin:");
        String adminBlock = yml.substring(adminAt, Math.min(yml.length(), adminAt + 400));
        check(adminBlock.contains("default: op"), "excelbuildmode.admin defaults to op");
        check(!adminBlock.contains("default: false"), "excelbuildmode.admin is NOT default false");
        int useAt = yml.indexOf("excelbuildmode.use:");
        check(yml.substring(useAt, useAt + 200).contains("default: false"), "excelbuildmode.use stays default false (unchanged)");
        int pasteAt = yml.indexOf("excelbuildmode.bigpaste:");
        check(yml.substring(pasteAt, pasteAt + 400).contains("default: false"), "excelbuildmode.bigpaste stays default false (unchanged)");
        check(yml.contains("admin status <player>") && yml.contains("admin off <player>"),
            "the usage line names both admin forms");
        check(!BuildPolicy.sessionPermissions(true).containsKey(AdminPolicy.PERMISSION)
            && !BuildPolicy.sessionPermissions(false).containsKey(AdminPolicy.PERMISSION),
            "the admin node is never granted by a session attachment");
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains(AdminPolicy.PERMISSION),
            "the admin node is not in the WorldEdit probe set");
    }

    /**
     * THE exercised restore (the audit's condition): a session is entered
     * through the real coordinator until it is STUCK (journal active,
     * build state in place of the Survival bytes), the admin decision is
     * taken, and the restore runs through SessionCoordinator.recover —
     * the exact call ExcelBuildMode.recover() delegates to and the admin
     * off path invokes. The Survival bytes must come back byte-for-byte
     * and every durable record must complete.
     */
    private static void stuckSessionRestore() throws Exception {
        Path base = Files.createTempDirectory("builder-admin-restore-test");
        RecoveryJournal journal = new RecoveryJournal(base.resolve("journal"));
        ActiveSessionIndex index = new ActiveSessionIndex(base.resolve("index"));
        RewardLedger rewards = new RewardLedger(Set.of());
        Port port = new Port(journal, index, rewards);
        SessionCoordinator coordinator = new SessionCoordinator(journal, rewards, index);
        coordinator.enter(PLAYER, port);
        check(Arrays.equals(port.items, BUILD), "entry replaced the live state with build state (the stuck shape)");
        check(journal.active(PLAYER).isPresent() && index.read(PLAYER).orElseThrow().active(),
            "the stuck session's journal + index are active");
        check(port.marker != null && port.marker.startsWith("v1:active:"), "the stuck session's marker is active");
        // The admin flow: guarded, no hold, storage ready -> PROCEED -> recover().
        check(AdminPolicy.decideOff(true, false, true) == AdminPolicy.OffDecision.PROCEED,
            "admin decision for the stuck session is PROCEED");
        check(coordinator.recover(PLAYER, port), "the existing recovery path runs for the admin off");
        check(Arrays.equals(port.items, SURVIVAL), "the Survival bytes are restored byte-for-byte");
        check(journal.active(PLAYER).isEmpty(), "the journal session is completed by the restore");
        check(!index.read(PLAYER).orElseThrow().active(), "the session index is tombstoned by the restore");
        check(port.marker == null, "the recovery marker is cleared by the restore");
        check(!rewards.isGuarded(PLAYER), "the reward guard is released by the restore");
        check(port.events.indexOf("restore") >= 0
            && port.events.indexOf("restore") < port.events.indexOf("marker-restored")
            && port.events.indexOf("marker-restored") < port.events.indexOf("marker-clear"),
            "restore ordering is the standard one: restore, restored-marker, clear");
        // A second admin off is the NOT_GUARDED no-op: decided before any call,
        // and the coordinator confirms there is nothing left to recover.
        check(AdminPolicy.decideOff(false, false, true) == AdminPolicy.OffDecision.NOT_GUARDED,
            "after the restore, admin off decides NOT_GUARDED");
        check(!coordinator.recover(PLAYER, port), "a redundant recover reports false and changes nothing");
        check(Arrays.equals(port.items, SURVIVAL), "the restored state survives the redundant call untouched");
    }

    private static void reviewHoldRefusal() throws Exception {
        Path base = Files.createTempDirectory("builder-admin-hold-test");
        RecoveryJournal journal = new RecoveryJournal(base.resolve("journal"));
        ActiveSessionIndex index = new ActiveSessionIndex(base.resolve("index"));
        RewardLedger rewards = new RewardLedger(Set.of());
        Port port = new Port(journal, index, rewards);
        SessionCoordinator coordinator = new SessionCoordinator(journal, rewards, index);
        coordinator.enter(PLAYER, port);
        // The decision refuses; per the paper flow NO recover() call follows,
        // so the session must remain exactly as stuck as it was.
        check(AdminPolicy.decideOff(true, true, true) == AdminPolicy.OffDecision.REVIEW_HOLD,
            "admin decision for a held session is REVIEW_HOLD");
        check(journal.active(PLAYER).isPresent() && Arrays.equals(port.items, BUILD),
            "the refused admin off leaves the held session untouched");
        // Storage-unavailable likewise attempts nothing.
        check(AdminPolicy.decideOff(true, false, false) == AdminPolicy.OffDecision.STORAGE_UNAVAILABLE,
            "admin decision without ready storage is STORAGE_UNAVAILABLE");
        check(journal.active(PLAYER).isPresent(), "the unattempted admin off leaves the session untouched");
    }

    /** MarkerPort fake in the RecoverySafetyTests.Port shape. */
    private static final class Port implements SessionCoordinator.MarkerPort {
        final RecoveryJournal journal; final ActiveSessionIndex index; final RewardLedger rewards;
        final List<String> events = new ArrayList<>();
        byte[] items = SURVIVAL.clone(); String marker;
        Port(RecoveryJournal journal, ActiveSessionIndex index, RewardLedger rewards) {
            this.journal = journal; this.index = index; this.rewards = rewards;
        }
        public byte[] capture() { return items.clone(); }
        public void commitBuildMarker(RecoveryJournal.Session session) { marker = "v1:active:" + session.id(); events.add("marker-active"); }
        public void enterBuild() { items = BUILD.clone(); events.add("creative"); }
        public void savePlayerData() { events.add("save"); }
        public void validateRecoveryMarker(RecoveryJournal.Session session) throws Exception {
            if (marker != null && !marker.equals("v1:active:" + session.id()) && !marker.equals("v1:restored:" + session.id()))
                throw new IOException("Wrong marker session");
        }
        public void restore(byte[] snapshot) { items = snapshot.clone(); events.add("restore"); }
        public void commitRestoredMarker(RecoveryJournal.Session session) { marker = "v1:restored:" + session.id(); events.add("marker-restored"); }
        public void clearRecoveryMarker(RecoveryJournal.Session session) { marker = null; events.add("marker-clear"); }
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + label);
    }
}
