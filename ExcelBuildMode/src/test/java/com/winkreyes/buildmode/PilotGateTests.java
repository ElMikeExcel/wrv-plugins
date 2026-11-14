package com.winkreyes.buildmode;

import com.sk89q.worldedit.LocalSession;
import com.winkreyes.buildmode.core.*;
import com.winkreyes.buildmode.paper.BuildPolicy;
import com.winkreyes.buildmode.paper.WorldEditLimitBridge;
import java.nio.file.*;
import java.util.*;
import org.bukkit.Material;

/** ExcelBuildMode 1.1 pilot: standalone assertions for the construction-only profile. Run via build.sh. */
public final class PilotGateTests {
    private static int assertions;
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000043");
    private static final byte[] SURVIVAL = "pilot-synthetic-survival".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static Path root;

    public static void main(String[] args) throws Exception {
        root = Path.of("target/test-work"); Files.createDirectories(root);
        worldEditSetIsFixed(); sessionPermissionShape(); harvestMatrix(); commandSpotChecks();
        schematicRootIdentity(); bigPastePolicy(); bigPasteSessionSeam();
        pilotAdmissionNeedsNoProviders(); strictAdmissionStillGated();
        System.out.println("PASS " + assertions + " assertions (pilot gates; no Bukkit client simulation)");
        Files.writeString(Path.of("target/pilot-test-result.txt"), "PASS " + assertions + " assertions\n");
    }

    private static void worldEditSetIsFixed() {
        // 1.3: this fixed set is the VERIFICATION PROBE list — representative nodes
        // sampled to prove a WorldEdit grant is effective (on entry/restore) or fully
        // gone (on exit). The session grant itself is the worldedit.* wildcard below.
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.size() == 41, "WorldEdit probe set has exactly 41 nodes (16 pilot + 25 added for brushes in 1.2)");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.brush.sphere"), "sphere brush is a probe node (1.2)");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.brush.options.size"), "brush size option is a probe node (1.2)");
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.brush.butcher"), "probe set stays curated (butcher is not sampled)");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().noneMatch(n -> n.contains("*")), "no wildcard nodes inside the probe set itself");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.clipboard.paste"), "clipboard paste is a probe node");
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.history.undo.other"), "probe set has no other-player history node");
    }

    private static void sessionPermissionShape() {
        Map<String, Boolean> off = BuildPolicy.sessionPermissions(false);
        check(off.size() == 1, "without WorldEdit the session attachment has one node");
        check(Boolean.FALSE.equals(off.get("jobs.use")), "jobs.use is denied in session");
        check(!off.containsKey("worldedit.*"), "no WorldEdit grant when WorldEdit is off (the landing/fallback shape)");
        Map<String, Boolean> on = BuildPolicy.sessionPermissions(true);
        // 1.3 (Mike, Oct 20, 2026): the full WorldEdit ability set, held only while
        // Build Mode is active — the worldedit.* wildcard PLUS every probe node
        // granted explicitly (Bukkit wildcard expansion skips unregistered nodes
        // such as worldedit.brush.smooth; the 1.3 runtime probe caught that).
        check(on.size() == 43, "with WorldEdit the session attachment has 43 nodes (jobs.use denial + worldedit.* + 41 explicit)");
        check(Boolean.FALSE.equals(on.get("jobs.use")), "jobs.use stays denied with WorldEdit on");
        check(Boolean.TRUE.equals(on.get("worldedit.*")), "the full WorldEdit set is granted via worldedit.*");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().allMatch(n -> Boolean.TRUE.equals(on.get(n))), "every probe node is also granted explicitly");
        check(on.keySet().stream().filter(n -> n.contains("*")).allMatch("worldedit.*"::equals), "worldedit.* is the only wildcard session node");
        check(!on.containsKey("excelbuildmode.use"), "entry permission is never self-granted by the attachment");
    }

    private static void harvestMatrix() {
        // Harvest blocks are denied with anything (or nothing) in hand.
        for (Material block : List.of(Material.COMPOSTER, Material.SWEET_BERRY_BUSH, Material.BEEHIVE,
                Material.BEE_NEST, Material.CAMPFIRE, Material.SOUL_CAMPFIRE)) {
            check(BuildPolicy.blocksHarvestInteraction(block, Material.AIR), block + " denied bare-handed");
            check(BuildPolicy.blocksHarvestInteraction(block, Material.DIAMOND_PICKAXE), block + " denied with a tool");
        }
        // Stripping: an axe on a strippable block is denied.
        for (Material block : List.of(Material.OAK_LOG, Material.SPRUCE_LOG, Material.CRIMSON_STEM,
                Material.WARPED_STEM, Material.OAK_WOOD, Material.CRIMSON_HYPHAE, Material.BAMBOO_BLOCK,
                Material.STRIPPED_OAK_LOG)) {
            check(BuildPolicy.blocksHarvestInteraction(block, Material.IRON_AXE), block + " denied with an axe");
        }
        // Construction interactions stay allowed.
        check(!BuildPolicy.blocksHarvestInteraction(Material.OAK_LOG, Material.AIR), "placing against a log bare-handed is construction");
        check(!BuildPolicy.blocksHarvestInteraction(Material.OAK_LOG, Material.OAK_PLANKS), "placing a block against a log is construction");
        check(!BuildPolicy.blocksHarvestInteraction(Material.STONE, Material.IRON_AXE), "an axe near stone is not stripping");
        check(!BuildPolicy.blocksHarvestInteraction(Material.DIRT, Material.WOODEN_SHOVEL), "a shovel on dirt is untouched");
        check(!BuildPolicy.blocksHarvestInteraction(Material.CHEST, Material.IRON_AXE), "chests are the container guard's job, not the harvest gate");
        check(!BuildPolicy.blocksHarvestInteraction(null, Material.IRON_AXE), "no clicked block, no denial");
    }

    private static void commandSpotChecks() {
        check(!BuildPolicy.allowsCommand("/jobs", true, true), "jobs command denied while building");
        check(!BuildPolicy.allowsCommand("/gamemode creative", true, true), "gamemode denied while building");
        check(!BuildPolicy.allowsCommand("/em party", true, true), "EliteMobs command denied while building");
        check(BuildPolicy.allowsCommand("/buildmode off", true, true), "the toggle itself is always allowed");
        check(BuildPolicy.allowsCommand("//set 64 stone", true, true), "WorldEdit set allowed with session WorldEdit");
        check(!BuildPolicy.allowsCommand("//set 64 stone", false, true), "WorldEdit set denied without session WorldEdit");
        check(BuildPolicy.allowsCommand("//copy", true, true) && BuildPolicy.allowsCommand("//paste", true, true), "clipboard allowed with session WorldEdit");
        check(BuildPolicy.allowsCommand("/msg Marvin done", true, true), "chat stays allowed");
        // 1.3.1 hotfix: the schematic family, fed as the literal strings a
        // player types. The village paste workflow needs load + save from
        // inside Build Mode; 1.3 cancelled both (Marvin, live, Oct 20 2026).
        check(BuildPolicy.allowsCommand("//schem load medieval-village-130red", true, true), "schem load allowed with session WorldEdit (the live-blocked command)");
        check(BuildPolicy.allowsCommand("//schem save wrv-old-village-2026-10-20", true, true), "schem save allowed with session WorldEdit");
        check(BuildPolicy.allowsCommand("//schem list", true, true), "schem list allowed with session WorldEdit (read-only helper)");
        check(BuildPolicy.allowsCommand("//schem formats", true, true), "schem formats allowed with session WorldEdit (read-only helper)");
        check(!BuildPolicy.allowsCommand("//schem load medieval-village-130red", false, true), "schem load denied without session WorldEdit");
        check(!BuildPolicy.allowsCommand("//schem save wrv-old-village-2026-10-20", false, true), "schem save denied without session WorldEdit");
        check(!BuildPolicy.allowsCommand("//schem delete x", true, true), "schem delete denied while building (file-destructive)");
        check(!BuildPolicy.allowsCommand("//schem rename x y", true, true), "schem rename denied while building (file-destructive)");
        check(!BuildPolicy.allowsCommand("//schem", true, true), "bare schem denied (no approved subcommand)");
        check(!BuildPolicy.allowsCommand("//schem bogus x", true, true), "unknown schem subcommand denied while building");
        check(BuildPolicy.allowsCommand("//schematic load x", true, true), "schematic alias load allowed with session WorldEdit");
        check(!BuildPolicy.allowsCommand("//schematic delete x", true, true), "schematic alias delete denied while building");
        check(BuildPolicy.allowsCommand("/worldedit:schem load x", true, true), "namespaced schem load allowed with session WorldEdit");
        check(BuildPolicy.allowsCommand("/worldedit:/schem save x", true, true), "namespaced-slash schem save allowed with session WorldEdit");
        check(!BuildPolicy.allowsCommand("/worldedit:schem delete x", true, true), "namespaced schem delete denied while building");
        check(BuildPolicy.allowsCommand("//SCHEM LOAD x", true, true), "schem matching is case-insensitive like the other roots");
        check(BuildPolicy.allowsCommand("//schem load x", true, false), "schem load does not depend on the chat/help toggle");
    }

    /**
     * 1.3.2, from Mike's live report (Oct 21, 2026): on the live server
     * //schem does not dispatch at all (WorldEdit's schem alias is broken
     * locally) while //schematic works — so the packet's player steps use
     * //schematic. BuildPolicy itself must decide IDENTICALLY for both
     * roots: it normalizes the root (namespace stripped, leading slashes
     * stripped) and gates "schem" and "schematic" on the same subcommand
     * set. This sweep pins every root form x subcommand x WorldEdit
     * shape: identical decisions across roots, and the expected decision
     * (load/save/list/formats allowed with session WorldEdit only;
     * delete/rename/bare/unknown always denied).
     */
    private static void schematicRootIdentity() {
        List<String> roots = List.of("//schem", "//schematic", "/schem", "/schematic",
            "/worldedit:schem", "/worldedit:/schem", "/worldedit:schematic", "//SCHEMATIC");
        // subcommand tail -> allowed with session WorldEdit?
        Map<String, Boolean> subs = new LinkedHashMap<>();
        subs.put(" load medieval-village-130red", true);
        subs.put(" save wrv-old-village-2026-10-20", true);
        subs.put(" list", true);
        subs.put(" formats", true);
        subs.put(" delete x", false);
        subs.put(" rename x y", false);
        subs.put("", false);
        subs.put(" bogus x", false);
        for (boolean worldEdit : new boolean[] {true, false}) {
            for (var sub : subs.entrySet()) {
                boolean expected = worldEdit && sub.getValue();
                Boolean first = null;
                for (String root : roots) {
                    boolean decision = BuildPolicy.allowsCommand(root + sub.getKey(), worldEdit, true);
                    check(decision == expected, "root " + root + " sub '" + sub.getKey().trim() + "' worldEdit=" + worldEdit + " decides " + expected);
                    if (first == null) first = decision;
                    else check(decision == first, "root " + root + " decides identically to the other roots for '" + sub.getKey().trim() + "'");
                }
            }
        }
    }

    private static void bigPastePolicy() {
        // The node is ours, never a WorldEdit node: it must not appear in
        // the session attachment or the WorldEdit probe set, so holding it
        // permanently trips neither the entry probe nor the exit probe.
        check(BigPastePolicy.PERMISSION.equals("excelbuildmode.bigpaste"), "big paste node is excelbuildmode.bigpaste");
        check(!BigPastePolicy.PERMISSION.startsWith("worldedit"), "big paste node is not a WorldEdit node");
        check(!BuildPolicy.sessionPermissions(true).containsKey(BigPastePolicy.PERMISSION), "big paste node is never attached by a session");
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains(BigPastePolicy.PERMISSION), "big paste node is not in the WorldEdit probe set");
        check(BigPastePolicy.DEFAULT_LIMIT == 1_000_000, "big paste default cap is 1000000 (finite, ~2.3x the village copy bounding box)");
        // (a) holder + session WorldEdit grant in effect -> raised to the configured value.
        check(BigPastePolicy.limitToApply(true, true, 1_000_000).orElseThrow() == 1_000_000, "holder entry raises to the configured cap");
        check(BigPastePolicy.limitToApply(true, true, 250_000).orElseThrow() == 250_000, "holder raise honors a configured cap verbatim");
        check(BigPastePolicy.limitToApply(true, true, -1).orElseThrow() == -1, "a deliberate -1 (unlimited) configuration is honored for holders");
        // (b) non-holder -> untouched (the WorldEdit config default, live 5000, stands).
        check(BigPastePolicy.limitToApply(true, false, 1_000_000).isEmpty(), "non-holder session limit is untouched");
        // No WorldEdit grant in effect (landing hold / WorldEdit off) -> untouched even for holders.
        check(BigPastePolicy.limitToApply(false, true, 1_000_000).isEmpty(), "no raise without the session WorldEdit grant in effect");
        check(BigPastePolicy.limitToApply(false, false, 1_000_000).isEmpty(), "no raise for a non-holder without the grant");
        // Sanitizing: typos fall back to the default instead of disabling or unbounding.
        check(BigPastePolicy.sanitizeLimit(0) == BigPastePolicy.DEFAULT_LIMIT, "a 0 config falls back to the default");
        check(BigPastePolicy.sanitizeLimit(-2) == BigPastePolicy.DEFAULT_LIMIT, "a below--1 config falls back to the default");
        check(BigPastePolicy.sanitizeLimit(5000) == 5000, "an ordinary positive cap passes sanitizing verbatim");
        // (e) //limit stays denied by policy for everyone, holders included —
        // the raise is the plugin's act, never a player command.
        check(!BuildPolicy.allowsCommand("//limit -1", true, true), "//limit -1 denied while building");
        check(!BuildPolicy.allowsCommand("//limit 1000000", true, true), "//limit with a number denied while building");
        check(!BuildPolicy.allowsCommand("/worldedit:/limit -1", true, true), "namespaced //limit denied while building");
    }

    /**
     * API-level proof against the REAL WorldEdit 7.4.5 classes (the build's
     * test classpath carries the exact server jar): the bridge's apply step
     * moves a real LocalSession's block-change limit to the raised value,
     * per-session — a second session is untouched. WorldEdit's own
     * LocalSession.createEditSession reads getBlockChangeLimit() into the
     * edit's maxBlocks (verified against the 7.4.5 bytecode), so this is
     * the value the session's //copy and //paste enforce.
     */
    private static void bigPasteSessionSeam() {
        LocalSession session = new LocalSession();
        LocalSession bystander = new LocalSession();
        int bystanderBaseline = bystander.getBlockChangeLimit();
        WorldEditLimitBridge.applyTo(session, 5000);
        check(session.getBlockChangeLimit() == 5000, "bridge can set a real LocalSession to the live default shape");
        // (a) at the API seam: the holder's session goes to the configured cap.
        WorldEditLimitBridge.applyTo(session, BigPastePolicy.limitToApply(true, true, BigPastePolicy.DEFAULT_LIMIT).orElseThrow());
        check(session.getBlockChangeLimit() == 1_000_000, "holder session limit raised to the big-paste cap on a real LocalSession");
        check(session.getBlockChangeLimit() > 433_998, "raised cap clears the village copy bounding box (433998)");
        check(session.getBlockChangeLimit() > 70_326, "raised cap clears the village paste block count (70326)");
        // (b) at the API seam: a session the policy leaves alone keeps its limit.
        check(bystander.getBlockChangeLimit() == bystanderBaseline, "raising one session never touches another session");
        WorldEditLimitBridge.applyTo(bystander, 5000);
        check(bystander.getBlockChangeLimit() == 5000 && session.getBlockChangeLimit() == 1_000_000,
            "limits are per-session: raising one session never touches another");
    }

    private static void pilotAdmissionNeedsNoProviders() throws Exception {
        Path dir = Files.createTempDirectory(root, "pilot-");
        RecoveryJournal journal = new RecoveryJournal(dir);
        SessionCoordinator pilot = new SessionCoordinator(journal); // no RewardLedger at all
        StubPort port = new StubPort();
        pilot.enter(PLAYER, port);
        check(port.build, "pilot entry enters build with zero reward providers");
        check(journal.active(PLAYER).isPresent(), "pilot entry still writes the recovery snapshot first");
        check(pilot.recover(PLAYER, port), "pilot recovery restores");
        check(!port.build && Arrays.equals(port.current, SURVIVAL), "pilot recovery returns the exact Survival bytes");
        check(journal.active(PLAYER).isEmpty(), "no pending recovery after pilot exit");
    }

    private static void strictAdmissionStillGated() throws Exception {
        Path dir = Files.createTempDirectory(root, "strict-");
        RecoveryJournal journal = new RecoveryJournal(dir);
        String provider = RewardLedger.COVERAGE.keySet().iterator().next();
        RewardLedger ledger = new RewardLedger(Set.of(provider)); // loaded in config, never registered
        SessionCoordinator strict = new SessionCoordinator(journal, ledger, null);
        StubPort port = new StubPort();
        fails(() -> strict.enter(PLAYER, port), "strict profile refuses entry while provider registration is incomplete");
        check(!port.build, "strict refusal happens before any build state");
        check(journal.active(PLAYER).isEmpty(), "strict refusal writes no snapshot");
    }

    private static final class StubPort implements SessionCoordinator.PlayerPort {
        boolean build; byte[] current = new byte[0]; int saves;
        public byte[] capture() { return SURVIVAL; }
        public void enterBuild() { build = true; }
        public void restore(byte[] snapshot) { build = false; current = snapshot; }
        public void savePlayerData() { saves++; }
    }

    private interface Throwing { void run() throws Exception; }
    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + label);
    }
    private static void fails(Throwing action, String label) {
        assertions++;
        try { action.run(); } catch (Exception expected) { return; }
        throw new AssertionError("EXPECTED FAILURE: " + label);
    }
}
