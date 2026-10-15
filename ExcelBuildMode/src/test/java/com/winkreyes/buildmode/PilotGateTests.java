package com.winkreyes.buildmode;

import com.winkreyes.buildmode.core.*;
import com.winkreyes.buildmode.paper.BuildPolicy;
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
        pilotAdmissionNeedsNoProviders(); strictAdmissionStillGated();
        System.out.println("PASS " + assertions + " assertions (pilot gates; no Bukkit client simulation)");
        Files.writeString(Path.of("target/pilot-test-result.txt"), "PASS " + assertions + " assertions\n");
    }

    private static void worldEditSetIsFixed() {
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.size() == 16, "WorldEdit set has exactly 16 nodes");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().noneMatch(n -> n.contains("*")), "no wildcard WorldEdit nodes");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.clipboard.paste"), "clipboard paste is in the approved set");
        check(!BuildPolicy.WORLD_EDIT_PERMISSIONS.contains("worldedit.history.undo.other"), "no other-player history access");
    }

    private static void sessionPermissionShape() {
        Map<String, Boolean> off = BuildPolicy.sessionPermissions(false);
        check(off.size() == 1, "without WorldEdit the session attachment has one node");
        check(Boolean.FALSE.equals(off.get("jobs.use")), "jobs.use is denied in session");
        Map<String, Boolean> on = BuildPolicy.sessionPermissions(true);
        check(on.size() == 17, "with WorldEdit the session attachment has 17 nodes");
        check(Boolean.FALSE.equals(on.get("jobs.use")), "jobs.use stays denied with WorldEdit on");
        check(BuildPolicy.WORLD_EDIT_PERMISSIONS.stream().allMatch(n -> Boolean.TRUE.equals(on.get(n))), "every WorldEdit node granted true");
        check(on.keySet().stream().noneMatch(n -> n.contains("*")), "no wildcard session nodes");
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
