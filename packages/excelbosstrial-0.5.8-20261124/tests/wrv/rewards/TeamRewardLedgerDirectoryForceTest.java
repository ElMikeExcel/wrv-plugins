/* Excel WRV maintenance; existing authorship retained. */ package wrv.rewards;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TeamRewardLedger directory-force contract (0.5.6 fsync
 * follow-up rebuild; P-FSYNC-1, Nov 14, 2026). Same idiom
 * and same fix as AtomicStateFile: the post-move directory
 * force in persist() is best-effort ONLY for the platform
 * refusal (Windows NIO throws AccessDeniedException opening
 * a directory channel); the mandatory temp-file fsync
 * carries the write. Any other directory-force failure
 * still fails the save and marks the ledger failed. The
 * Windows refusal cannot occur naturally on this Linux
 * build host, so it is injected through the package-private
 * DirectoryForce seam of the production persist path.
 */
public final class TeamRewardLedgerDirectoryForceTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("team-reward-ledger-dirforce-test");
        UUID player = UUID.randomUUID();
        try {
            // 1. Ordinary saves through the public API: the
            // real directory force runs on this platform.
            Path file1 = dir.resolve("one/ledger.bin");
            try (TeamRewardLedger ledger = new TeamRewardLedger(file1)) {
                ledger.openEncounter("enc-1");
                ledger.qualify("enc-1", player);
            }
            try (TeamRewardLedger reopened = new TeamRewardLedger(file1)) {
                check(reopened.outcome("enc-1") == TeamRewardLedger.EncounterOutcome.ACTIVE,
                        "encounter survives close and reopen with the real directory force");
                check(reopened.roster("enc-1").contains(player),
                        "roster survives close and reopen with the real directory force");
            }

            // 2. The Windows failure mode, injected: the
            // directory force throws AccessDeniedException.
            Path file2 = dir.resolve("two/ledger.bin");
            int[] forceCalls = {0};
            try (TeamRewardLedger ledger = new TeamRewardLedger(file2)) {
                ledger.directoryForce = d -> {
                    forceCalls[0]++;
                    throw new AccessDeniedException(d.toString());
                };
                ledger.openEncounter("enc-2");
                ledger.qualify("enc-2", player);
                check(forceCalls[0] > 0, "directory force is actually attempted before it is excused");
                check(ledger.outcome("enc-2") == TeamRewardLedger.EncounterOutcome.ACTIVE
                        && ledger.roster("enc-2").contains(player),
                        "saves succeed and stay readable under the platform refusal");
                ledger.win("enc-2", "plan-1",
                        Map.of(player, List.of(new TeamRewardLedger.RewardUnit("unit-1", "payload"))));
                List<TeamRewardLedger.Claim> claims = ledger.claims(player);
                check(claims.size() == 1 && claims.get(0).state() == TeamRewardLedger.State.OWED,
                        "win persists a claim under the platform refusal");
            }
            try (TeamRewardLedger reopened = new TeamRewardLedger(file2)) {
                check(reopened.outcome("enc-2") == TeamRewardLedger.EncounterOutcome.WON,
                        "win outcome is durable on disk after the refusal");
                List<TeamRewardLedger.Claim> claims = reopened.claims(player);
                check(claims.size() == 1 && claims.get(0).state() == TeamRewardLedger.State.OWED,
                        "claim is durable on disk after the refusal");
            }

            // 3. Control: any OTHER directory-force failure
            // still fails the save (Linux semantics unchanged —
            // the ledger's persistence-uncertain self-protection
            // keeps its trigger for genuine durability failures).
            Path file3 = dir.resolve("three/ledger.bin");
            try (TeamRewardLedger ledger = new TeamRewardLedger(file3)) {
                ledger.directoryForce = d -> {
                    throw new IOException("injected genuine directory-force failure");
                };
                boolean propagated = false;
                try {
                    ledger.openEncounter("enc-3");
                } catch (IOException e) {
                    propagated = e.getMessage() != null
                            && e.getMessage().contains("injected genuine directory-force failure");
                }
                check(propagated, "a non-platform directory-force failure still propagates as IOException");
                boolean locked = false;
                try {
                    ledger.outcome("enc-3");
                } catch (IllegalStateException e) {
                    locked = true;
                }
                check(locked, "ledger refuses further work after a genuine directory-force failure");
            }
        } finally {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }

        System.out.println("TeamRewardLedgerDirectoryForceTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
