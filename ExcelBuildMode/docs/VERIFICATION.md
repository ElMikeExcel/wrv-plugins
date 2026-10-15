# Offline verification and release gates

## Verified here

- Compiled the complete adapter and core against the exact cached Paper 26.2 build123 API and runtime library set using Java25.
- Standalone core tests: 5,709 assertions in the final core build. Re-run `build.sh` and inspect `target/core-test-result.txt` for the final count.
- Exhaustive enter/recover combinations of length8: 256 traces, reopening the journal at every step, testing repeated entry and no-op recovery without overwriting survival state.
- Round-trip, completed tombstone, immutable retained copies, defensive array copies, different sessions, stale completion rejection and size limits.
- Simulated process restart from an active session and completed session.
- Injected I/O failures after primary/backup write, verification, temporary-state write, state commit, before completion and after completion; no destructive call before durable entry returns.
- Capture/enter/restore/save port failures, retry restoration, and save-before-complete ordering.
- Primary-copy corruption fallback, both-copy corruption failure, corrupt active state and truncated-file rejection. Corrupt sessions cannot be replaced with a new snapshot.
- Default command denial and fixed optional WorldEdit allowlist; no wildcard/admin/bypass/schematic permission node in the attachment list.

These tests use synthetic bytes and a fake player port. They DO NOT execute Paper's real item serialization, game physics, event dispatch, connection handling or any client. Test counts reflect assertions, not 5,709 separate gameplay scenarios.

## Adapter/code review checks

- Checks mode after setGameMode, so a cancelled Creative transition cannot be reported successful; entry failure triggers recovery.
- Menu callbacks require the exact clicked inventory to remain open and re-check state on the main thread. Repeated entry is refused; repeated completed exit is a no-op.
- Cursor and crafting matrix must be empty before opening the menu/entering; inventory is closed before capture.
- Entire snapshot decoding happens before any recovery mutation; serialized storage/armor/offhand/ender sizes and numeric fields are validated.
- Permission defaults false; self-only command; no OP, LuckPerms group edit, player-target command, broad PDC copy, balance API or Jobs API.
- Permission loss during Build Mode triggers recovery within the periodic check.
- Optional WorldEdit attachment is checked effective after application and removed/checked on recovery. Pre-existing allowed nodes refuse temporary-scoped entry.
- Known harmful footing is excluded from ground checks, but collision/physics edge cases still require real-player tests.

## Exact WorldEdit history source check

WorldEdit7.4.5 [HistoryCommands](https://raw.githubusercontent.com/EngineHub/WorldEdit/7.4.5/worldedit-core/src/main/java/com/sk89q/worldedit/command/HistoryCommands.java) defines ordinary undo/redo plus slash-prefixed aliases, accepts the `.self` permissions and separately checks `.other` for another player's history. [PermissionCondition](https://raw.githubusercontent.com/EngineHub/WorldEdit/7.4.5/worldedit-core/src/main/java/com/sk89q/worldedit/command/util/PermissionCondition.java) uses any-match for declared permission alternatives. The attachment now grants only the self-scoped history nodes; policy tests cover ordinary, double-slash and WorldEdit namespaced spellings, including denial when temporary WorldEdit is off. This source review is not a running WorldEdit integration test.

## Mandatory actual-Paper tests before live use

1. Real inventory round-trip with all 36 storage slots, four armor slots, offhand, 27 ender slots, cursor/grid refusals, held slot and XP level/total/progress. Include named/enchantmented custom/PDC items, written books, map IDs, nested shulker contents and bundles. Verify byte payload data/components survive correctly.
2. Cancelled entry/restore mode events; double clicks and command spam; permission removal; attachment apply failure (LuckPerms setting) and proper removal; rejoin/restart after interrupted entry/restoration.
3. Quit/shutdown/reload/reconnect in normal Build Mode and airborne landing hold, grounded restore, disable behavior, wrong-footing edge cases, full disk/unwritable storage, corrupt payload and state records. Verify actual playerdata and exact crash boundary; don't infer fsync from saveData returning.
4. Death/respawn, death cancellation by another listener, `/kill`/void/plugin-caused death, no Creative item/XP drops, and one exact snapshot restoration. If the player is actually dead, recovery waits until alive/respawn.
5. Real Java and Bedrock clients through the installed Geyser/Floodgate path: inventory menu, Creative inventory selection/drag/drop, container transfers, double click, disconnect timing, safe landing, cursor and offhand.
6. Economy/Jobs/mcMMO: audit Creative payment/progression configuration. This helper intentionally does not roll these systems back. Shop/NPC/menu routes must be tested on the actual installed plugins.
7. WorldEdit7.4.5 (if separately approved/installed): correct permission nodes and commands, async/in-progress edit completion, undo/redo/clipboard lifecycle across exit/reentry/disconnect, inherited rights, limits and side effects. WorldEdit/FAWE are not installed in this prototype.

## Coexistence provenance and risks

MenuJoin source inspected at `wrv-menu-install-docs/MenuJoin` reports version1.2.1, while live startup reportedly identifies1.2.0; it is not established as byte-identical to the live plugin. Its known behavior inserts a compass 60ticks after join and 1tick after respawn, suppressing duplicates when one is present. ExcelBuildMode softdepends on MenuJoin and schedules its own respawn restore after the known insertion, so the original snapshot wins. This ordering and insertion need actual installed-artifact tests; reflection fixtures using that source must be labeled candidate/synthetic. If the original snapshot had no compass, a later normal MenuJoin insertion is still normal server behavior, not a snapshot duplication.

DeathProtection0.1.3 source/JAR inspected in the prior reconstruction is not byte-identical to the installed live plugin. It consumes active one-use protection on an uncancelled actual death at MONITOR. **A forced Build Mode death can therefore consume that paid protection even when inventory recovery succeeds.** This prototype does not copy/restore that PDC entitlement, change effects, or prevent the other plugin consuming it. Normal damage is cancelled; that is not proof all actual deaths are impossible. A separately approved compatibility hook or accepted explicit behavior is required. Tests with the reconstructed JAR must be labeled synthetic/candidate.

MobVeil's PDC and duration are not touched; its timer should continue naturally. That needs a coexistence test, not a claim from copying the entitlement. Do not snapshot/restore broad PDC or effects to make a fixture pass.

## Status

This is a tested core plus compiling adapter, with runtime/integration gates still open unless separate verified evidence is appended. No production deployment, permission change, WorldEdit install, end-to-end client test or absolute recovery guarantee is implied.
