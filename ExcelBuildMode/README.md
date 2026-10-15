# ExcelBuildMode 1.0 candidate — NOT DEPLOYED

## 1.1 pilot candidate (October 14, 2026) — NOT DEPLOYED

This tree now also builds **ExcelBuildMode 1.1**, the construction-only pilot
from the approved supported-actions specification. Pilot profile
(`pilot-construction-only: true`): entry does not wait on reward-provider
registration; earning while building is suppressed by a session `jobs.use`
denial and early input gates. The 1.0 strict profile remains available with
the flag off. All 1.0 suites still pass unchanged, plus 53 new pilot
assertions. Jar SHA-256
`cf1b2a1b92895ca16a3432edd02120ea88ba91f60a7fc7673dc644a86250ec06`
(clean-rebuild reproduced). Full detail: [docs/PILOT-1.1.md](docs/PILOT-1.1.md).
Still not installed; no live changes of any kind.

OFFLINE CANDIDATE. Not installed, not approved for deployment, and not production-ready. No live player, server, permission, balance, job, or file changes were made. The plugin name remains ExcelBuildMode and the custom version is 1.0. The previous inventory-only baseline is retained separately. See [the 1.0 changelog](docs/CHANGELOG-1.0.md) and [new verification evidence](docs/VERIFICATION-1.0.md).

## Intended experience

A trusted builder uses `/buildmode` to open two buttons: **Build Mode** and **Play Normally**, with a status item between them. `/buildmode on`, `off`, and `status` also work. There is no target-player argument. Entry permission `excelbuildmode.use` defaults to false, including for OP through this permission declaration. No OP or group changes occur in code. This project does not grant anyone permission.

- Begin from Survival, on solid ground, out of a vehicle, with an empty cursor and crafting grid.
- Entry saves Survival storage, armor, offhand, ender chest, held slot, all three XP values, Survival mode, flight flags and movement speeds. The Creative inventory starts empty.
- Build anywhere in the existing world. No hub coordinates, region enclosure, world changes or teleport are needed.
- Land, then choose Play Normally. The exact saved state returns. There is no persistent Creative-inventory save.
- Quit on safe ground also restores. Interrupted sessions recover on join/startup. Forced mode changes trigger recovery rather than replacing the saved Survival snapshot.
- A death clears Creative drops/kept items/XP and retains recovery until respawn. Normal damage and attacking are blocked while a session is pending.

### Airborne recovery

Voluntary exit refuses while airborne. Forced exit, reconnect, quit or shutdown while airborne enters a temporary **Survival landing hold**, at the current location. Build contents are cleared; the saved Survival contents remain in the journal. WorldEdit permissions are removed; flight is temporarily available so the player can descend to solid ground. Building, transfers and non-recovery commands are locked. Once grounded, the original inventory/XP/flight state is restored and the recovery record is completed. There is no teleport.

If the plugin is disabled while a hold or error remains, it kicks that player and retains the journal. While the plugin is absent/disabled, it cannot enforce recovery or strip a flight flag saved during a hold. Do not remove/disable the plugin or recovery folder while sessions are active. A full server stop/restart with this plugin retained is the intended operational path. There is no in-game administrator override or destructive reset command.

## Scope and limits

This is trusted-player assistance, not a permission sandbox, anti-cheat system, economy firewall or item-provenance system.

**Creative-built blocks remain ordinary world blocks. They can later be harvested by the builder or anyone else.** WorldEdit also changes the same shared world. Griefing, valuable blocks, fluids, explosions, redstone machinery and similar world effects are not comprehensively contained.

Defaults stop common accidental item drops/pickups, container/GUI transfers, entity interactions, shop signs and commands. In Build Mode the command guard permits `/buildmode`, common private chat/reply/help/list commands (configurable), and optionally the fixed WorldEdit subset. The main server compass/menu is blocked by the default container/command guards because that GUI includes economy actions; `/buildmode` directly opens the two-button mode menu. This prototype does not add a button to the existing server menu or compass. It blocks arbitrary `/shop`, `/pay`, `/eco`, `/gamemode`, namespaced variants, etc. It does not intercept every plugin-internal command dispatch, console command, custom NPC/shop callback, entity projectile, or third-party API. Turning guard settings off intentionally reduces those protections; landing/error recovery remains locked.

There is **no balance snapshot/restore, no Jobs snapshot/restore, no mcMMO snapshot/restore, no broad PDC copy, and no paid feature/effect rollback**. The new reward bridge coordinates exact provider integrations for Jobs, mcMMO and EliteMobs. Every loaded provider must register its full audited manifest before new Build entry; unpatched loaded providers block entry. The bridge alone does not instrument a provider. Unrelated reward plugins and external direct balance/XP writes remain outside this contract. MobVeil's own PDC and timer are not modified by this plugin.

## Optional WorldEdit

`temporary-worldedit: false` by default. It has no WorldEdit dependency or bundled WorldEdit JAR. Enabling it requires a separately approved installation and compatibility test. The fixed allowlist covers selection, set/replace/walls/faces, copy/paste/rotate/flip and self-only undo/redo (both `/undo`/`/redo` and `//undo`/`//redo`, including WorldEdit namespaced variants). History uses only `worldedit.history.undo.self` and `worldedit.history.redo.self`; no `.other` grant. It grants no wildcard, OP, group, bypass, reload, schematic file, scripting, limit-override or other-player permission.

The attachment exists only during Build Mode and is explicitly removed for recovery, quit, death and shutdown. It checks every allowed permission after attaching and refuses/recoveries if LuckPerms does not apply it. It also refuses entry when any allowlisted WorldEdit node already exists outside Build Mode, rather than pretending to scope pre-existing permissions. It checks those nodes again after attachment removal. This cannot prove absence of every dangerous inherited permission: baseline permissions and WorldEdit command semantics still need review. No WorldEdit session/clipboard/undo-state cleanup has been implemented or tested; that is a deployment gate when optional WorldEdit is enabled. No wildcard or baseline deny is installed.

## Durable recovery design

The 1.0 source requires a private, redundant ever-enrolled builder cohort in addition to permission and current eligibility. Defaults permit nobody. It retains two checksummed immutable inventory snapshots, a separate session index and an acknowledged native playerdata marker before Creative mutation. Restoration acknowledges the native restored marker before completing records, then acknowledges marker removal before releasing the guard. An unreadable journal leaves recovery listeners active; verified ordinary Survival players remain available while affected or unknown identities are held individually.

Entry-only budgets bound future snapshot growth (128 MiB retained journal, 2,048 sessions and 512 MiB filesystem reserve by default). Recovery is never refused solely because entry budgets are exhausted; snapshots are never automatically deleted. Shockbyte account quota still needs a separate deployment check.

Read [the scoped recovery policy](docs/RECOVERY-POLICY-1.0.md) and [native playerdata proof](docs/PLAYERDATA-PROOF.md) for ordering, fallback assumptions and limitations. These separate stores do not form one atomic transaction. Mixed-date backup restores, external writers, hardware loss and absent/disabled helper enforcement remain operational risks. No loss-proof or duplication-proof claim is made.

## Offline build and test

The exact previously cached Paper API is `paper-api-26.2.build.123-stable.jar`; compilation uses the existing Java 25 JDK. No dependency download, Maven execution, EULA acceptance or runtime start is performed by the build.

```sh
./build.sh
```

The default read-only dependency root is `../leaderboard-runtime-prep`. Override `RUNTIME` and `JAVA_HOME` for another local copy. All generated test data and output stay in this project under `target/`.

Tests cover the core journal/state coordinator, fixed command/permission policy, reward ledger, admission races, immutable origin/farm denial, recipient reservations, failure boundaries and public bridge signatures. These are not a substitute for actual Paper events, real playerdata serialization, Java/Bedrock clients, or plugin coexistence. See [docs/VERIFICATION.md](docs/VERIFICATION.md) for exact coverage and remaining gates.

## Reward integration

The [v1 API contract](docs/REWARD-API-V1.md) defines provider coverage, ownership, startup and concurrency rules. Build entry refuses while previously earned Survival work remains pending, without clearing inventory or suppressing ordinary rewards. Build/landing/recovery/failed origins remain denied after exit. A provider must reserve recipient forks before calculating a party divisor or bonus. No timeout substitutes for a fully audited pending-work boundary.

Cold start only: stop the whole server and retain the recovery folder when changing these artifacts. Do not hot reload the helper/providers or remove the helper while recovery is pending. Provider registration attests complete instrumentation; exact provider artifact tests remain a deployment gate.

## Existing-plugin alternative

Essentials already provides Creative/Survival/flight commands, but those commands alone do not implement these separate inventory snapshots. A mature inventory plugin such as Multiverse-Inventories can supply game-mode inventories, but adds its required Multiverse-Core dependency and configuration/integration work. This prototype avoids that dependency stack at the cost of owning and proving custom recovery code. The parent review should compare both before choosing. No migration or installation is included.
