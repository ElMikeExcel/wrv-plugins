/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import wrv.rewards.TeamRewardLedger;

/**
 * 0.2.11: the grantclaim STORE contract, at the ledger level with
 * opaque payloads (the real ItemStack encoding is Bukkit-bound and
 * rig-proven end-to-end). The sequence is exactly the one
 * TeamClaims.grantClaim performs — openEncounter, qualify, win —
 * so what is pinned here is what the console command stores:
 * a grant to an OFFLINE player persists across a restart (a fresh
 * ledger over the same file), delivers exactly once, and two
 * grants stack as two separate claim entries.
 */
public final class GrantClaimStoreTest {

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

    /** The store sequence TeamClaims.grantClaim performs. */
    private static String storeGrant(TeamRewardLedger ledger, UUID owner, String run,
                                     List<TeamRewardLedger.RewardUnit> units) throws Exception {
        ledger.openEncounter(run);
        ledger.qualify(run, owner);
        ledger.win(run, "console-grant-v1", Map.of(owner, units));
        return run;
    }

    public static void main(String[] args) throws Exception {
        Path file = Files.createTempDirectory("grant-claim-store-").resolve("team-rewards.dat");
        UUID offline = UUID.randomUUID();
        List<TeamRewardLedger.RewardUnit> units = List.of(
                new TeamRewardLedger.RewardUnit("grant:" + offline + ":0", "paper-item-v1:TEST"));

        // --- Grant while the player has never been seen (offline).
        try (TeamRewardLedger ledger = new TeamRewardLedger(file)) {
            storeGrant(ledger, offline, "console-grant:test-1", units);
            var claims = ledger.claims(offline);
            check(claims.size() == 1 && claims.get(0).state() == TeamRewardLedger.State.OWED
                            && claims.get(0).units().equals(units),
                    "the grant is stored as one OWED claim with its units");
            check(claims.get(0).id().equals("console-grant:test-1/" + offline),
                    "the claim id is the grant run + owner, like an earned claim");
        }

        // --- Restart: a fresh ledger over the same file still holds it.
        try (TeamRewardLedger ledger = new TeamRewardLedger(file)) {
            var claims = ledger.claims(offline);
            check(claims.size() == 1 && claims.get(0).state() == TeamRewardLedger.State.OWED,
                    "the offline grant persists across a restart");

            // --- Delivers exactly once: the second begin is refused.
            var begun = ledger.beginDelivery(offline, claims.get(0).id());
            ledger.finishDelivery(offline, claims.get(0).id(), begun.attempt(),
                    TeamRewardLedger.Outcome.DELIVERED);
            check(ledger.claims(offline).get(0).state() == TeamRewardLedger.State.DELIVERED,
                    "the claim delivers");
            boolean secondDeliveryRefused = false;
            try {
                ledger.beginDelivery(offline, claims.get(0).id());
            } catch (IllegalStateException refused) {
                secondDeliveryRefused = true;
            }
            check(secondDeliveryRefused, "a delivered grant cannot be delivered twice");

            // --- A second grant is a second entry, not a mutation.
            storeGrant(ledger, offline, "console-grant:test-2", units);
            var all = ledger.claims(offline);
            check(all.size() == 2, "two grants stack as separate claim entries");
            check(all.stream().filter(c -> c.state() == TeamRewardLedger.State.OWED).count() == 1
                            && all.stream().filter(c -> c.state() == TeamRewardLedger.State.DELIVERED).count() == 1,
                    "the second grant is OWED; the delivered one is untouched");
        }

        // --- Read-back through a third open: the store round-trips clean.
        try (TeamRewardLedger ledger = new TeamRewardLedger(file)) {
            check(ledger.claims(offline).size() == 2,
                    "the store round-trips both grants after another restart");
            check(ledger.roster("console-grant:test-1").equals(java.util.Set.of(offline)),
                    "the grant roster is exactly the target");
        }

        System.out.println("GrantClaimStoreTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
