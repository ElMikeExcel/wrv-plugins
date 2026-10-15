# Scoped recovery and storage policy for the first 1.0 release

Status: offline candidate source and tests in progress. No deployment or access grant.

## Scope and eligibility

The first installation is intended for one explicitly approved Java builder. Actual identity appears only in the private installation manifest and the installed private configuration, never source defaults. A permission, including OP or wildcard permissions, is insufficient by itself: the actor must also be in the configured eligible UUID list and two verified recovery-cohort replicas. Defaults allow nobody.

The recovery cohort is an ever-enrolled set, separate from current eligibility. Removing entry eligibility does not remove recovery protection. Future cohort migration must retain former builders until their journal, independent index, saved player marker and inventory recovery are verified. The two cohort files must match; one damaged copy or two differing copies refuses new entry, and the union of readable identities remains available for protective recovery. No automatic cohort migration occurs.

## Independent recovery signals

1. The journal retains two checksummed immutable inventory snapshots and an atomic active/completed state.
2. A separate active-session index outside the snapshot directory records actor and session identity before player mutation.
3. The player's native saved data contains only our own PDC string, `excelbuildmode:recovery`. The value is `v1:active:<session UUID>` during entry/build and `v1:restored:<session UUID>` after restoration. A separate `v1:review:<UUID>` marker plus an immutable per-player review-hold record preserves UNKNOWN classification across Paper quit/save rewrites. Review holds require explicit verified administrative resolution; they are never automatically cleared. Other PDC, money, paid-effect state and timers are never copied or rolled back.

Native playerdata location is obtained from exact Paper's actual playerIo.getPlayerDir(), not guessed from the player's dimension or a legacy directory name. In Paper build 123, saveData delegates to a native save routine that can return early or catch/log failures. Therefore invoking saveData is not an acknowledgement. The candidate reads the exact UUID's compressed record, verifies UUID/mode/marker, forces that same file and its directory, and checks for detectable replacement/change. Missing, corrupt, oversized, wrong-UUID, unsupported or changing data is UNKNOWN, never proof that a player is unmarked.

## Ordered entry

On the server thread: validate private eligibility and permission, grounded Survival state and empty cursor/grid; atomically drain earned Survival rewards and prepare the immutable journal plus independent active index; publish the authoritative reward guard; release the ledger lock; set and save the active native marker; read back and force the exact marker while still in Survival; only then clear the Survival inventory and enter Creative. An `after Creative, before journal` window is prohibited by construction. Any error after journal commit retains recovery and guard state. Player saves, gamemode events and permissions are never invoked under the ledger lock.

## Ordered restoration

Require matching journal/index/session evidence and an acceptable native marker. Do not guess on a mismatched or malformed marker or unknown native record. Restore the saved inventory and Survival state, save and acknowledge the restored marker, then complete the journal and index, then remove only our marker and acknowledge the resulting native record. Release the reward guard last. Failure during marker clearing after completed journal/index is held for review; it is not permission to replay an old inventory or silently clear a current guard.

The stores are still separate transactions. The ordering and acknowledgements close specific silent-save and detection gaps; they do not promise atomicity across all files, storage hardware failures, unrelated plugin writes, or a mixed-date backup restore.

## Journal/cohort failure and joining

The plugin stays loaded in recovery-only mode rather than silently removing its listeners. It refuses all new Build entry while recovery setup is unverified. It does not impose a server-wide join lock or shut the server down.

- A known cohort member whose recovery cannot be checked is held for administrator review.
- A player with an independent native recovery marker is held or follows verified active recovery, even if cohort replicas are lost.
- An ordinary player with verified, matching-UUID, unmarked Survival playerdata can continue while cohort/journal files are unavailable.
- Missing/corrupt playerdata or unmarked non-Survival state is UNKNOWN. That player is held rather than guessed safe. During catastrophic identity-storage failure this can also delay a genuinely new player with no saved record, or an unrelated Creative player. It does not automatically deny established, verified ordinary Survival players.

Healthy cohort data proves that identities outside the ever-enrolled cohort were never eligible for this helper; their normal play is unaffected. This proof assumes reviewed cohort migrations, no alternate helper version granting access outside the cohort, and a coherent backup/restore process. If all relevant copies have been deleted or restored inconsistently, the plugin cannot reconstruct destroyed inventory or historical identity evidence.

## Bounded retention without deletion

Proposed first-release defaults:

- Maximum retained journal data: 128 MiB
- Maximum retained snapshot sessions: 2,048
- Minimum filesystem free-space reserve after estimated next write: 512 MiB

Before a new session, account for both snapshots and metadata headroom. Refuse before any new journal or inventory mutation when a budget is exceeded. Scan size is bounded; links and unexpected file types are refused. Recovery reads and completion bypass these entry-only budgets. No active or completed snapshot is automatically deleted.

This bounds future growth, rather than pruning history. Reaching a limit requires reviewed export/archive and verified backups before any separate cleanup approval. FileStore free space is not the Shockbyte account-quota API; account quota and backup capacity remain separate installation checks. Other plugins can consume disk after admission, so the reserve cannot guarantee later writes succeed. Write failures retain recovery evidence and stop the affected session.

## Tests required before release

Pure tests cover missing/disagreeing cohorts, eligibility shrink versus historical recovery, storage refusal without mutation, recovery over budget, corrupt indexes, ordering, marker-write failure before Creative, post-Creative save failure, restoration acknowledgement failure, and ambiguous final marker clearing. Native playerdata tests cover gzip/NBT bounds, UUID, marker types, truncated/corrupt/duplicate input, unrelated PDC preservation, file replacement and exact marker acknowledgement.

An exact final-artifact Paper run must still verify actual save disable/failure, join/restart ordering, marker/index/session mismatch, inventory and paid-effect coexistence, real playerdata path, forced death and landing recovery. Existing inventory-only results and earlier ledger results cannot be attributed to this later source revision without a rerun. Complete Jobs/mcMMO/EliteMobs provider registration remains a separate release gate.

## Primary source

Paper commit 5001879, CraftPlayer.saveData delegates to native playerIo.save:
https://github.com/PaperMC/Paper/blob/5001879/paper-server/src/main/java/org/bukkit/craftbukkit/entity/CraftPlayer.java
The cached build-123 native bytecode and fixture evidence are recorded in verification/playerdata/.
