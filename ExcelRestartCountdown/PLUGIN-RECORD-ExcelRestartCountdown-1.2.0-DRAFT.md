# ExcelRestartCountdown 1.2.0 — rename of RestartCountdown 1.1.0

> **INSTALL RECORD — this draft pull request is the record.**
>
> Installed live October 16, 2026, ~8:46 AM CDT via auto-watch
> (empty-server gates 8:41 AM and 8:43 AM CDT). The old jar was kept
> as `.bak-20261016`. Repo `wrv-plugins`, branch
> `record/excel-renames-part-2-2026-10-16` (WRV-060, Excel Renames
> Part 2), this record under `ExcelRestartCountdown/`. Draft per
> this repo's live-version-record pattern; stays unmerged.
>
> Prepared earlier the same day as a local draft while the jar was
> still staged. The install has since happened. Website name updates
> are still open. Built from live RestartCountdown 1.1.0. The PR #1
> RestartCountdown 1.1.1 line is ABANDONED / UNUSED.

## Status at a glance

Statuses are kept separate, per the server rule. Do not collapse them.

| Status | State |
|---|---|
| Built | **Yes** — October 16, 2026, local build |
| Automated tests | **Yes — 10 passing** (new behavior suite for the live 1.1.0 line incl. legacy-permission and alias coverage, + migration suite) |
| Test server (scratch boot) | **Passed** — clean enable; **both** command names answered from the console; seeded legacy folder migrated byte-for-byte |
| Installed live | **Yes** — October 16, 2026, ~8:46 AM CDT, via auto-watch (gates 8:41 + 8:43 AM CDT); old jar kept as `.bak-20261016` |
| Java check | **Not applicable as a client feature** — admin console/op command; a real countdown start is deliberately never part of a probe (it ends in a shutdown) |
| Bedrock check | **Not applicable** |

## 1. Artifact

- **Plugin:** ExcelRestartCountdown (was RestartCountdown)
- **Version:** 1.2.0 (was 1.1.0)
- **Jar:** `ExcelRestartCountdown-1.2.0.jar`
- **SHA-256:** `50dfa7b89b6bad50ce368bda198314d6b3d429522c77c8f811e95124f04530cd`
- **Main class:** `com.winkreyes.excelrestartcountdown.ExcelRestartCountdown`
  (was `com.winkreyes.restartcountdown.RestartCountdown`)
- **Command:** `excelrestartwarn` (canonical), with **`restartwarn`
  kept as a registered alias** — see §2.
- **Permissions:** `excelrestartcountdown.warn` (canonical, default
  op) **and** `restartcountdown.warn` (legacy, default op) — the
  handler accepts either; see §2.
- **Placeholders / config:** none (unchanged); api-version `"1.13"`
  preserved from the live jar.
- **Data folder:** `plugins/ExcelRestartCountdown/` (was
  `plugins/RestartCountdown/`; first-boot auto-migration, §3). No live
  data folder exists for this plugin today.
- **Rollback artifact:** `RestartCountdown-1.1.0.jar.bak-20261016`
  kept on the server at install (see the project INSTALL-PACKET).

## 2. What changed vs 1.1.0

The identity, with compatibility kept everywhere an old name is used:

- Package and class renamed (above); jar and data folder renamed.
- **Command:** canonical name is now `excelrestartwarn`; `restartwarn`
  is registered as its alias and keeps working for console habit,
  notes, and runbooks. The handler checks the canonical name (Bukkit
  resolves the alias to the same command). In-chat help/usage strings
  still show `/restartwarn` — the alias works, and the strings were
  left untouched to keep the rename scope minimal.
- **Permission:** canonical node renamed to
  `excelrestartcountdown.warn`. The legacy node
  `restartcountdown.warn` is also declared in `plugin.yml` (default
  op) and the handler's check accepts **either** node, so any grant
  made under the old name keeps working. (Audit found no stored
  grants for either node — LuckPerms holds no config-file grants for
  them, `ops.json` is empty, admin runs through the console; the dual
  accept is insurance, and it is unit-tested both ways.)
- `plugin.yml`: name/version/main updated; both permission nodes
  declared, defaults unchanged (op).
- Added `LegacyDataMigration` + a null-guarded first step in
  `onEnable` (§3).
- **Zero behavior changes** beyond the identity: the 2-minute
  countdown, its broadcasts, the cancel semantics, the
  refuse-a-second-countdown behavior, and the shutdown call are
  unchanged and byte-proven (§4).

Source of record: local tree `~/workspace/restartcountdown/`,
**proven** to be the live 1.1.0 build before renaming (compiled under
the original identity; normalized `javap -c` diff vs the live jar =
0 lines).

> **Flag — the PR #1 source is NOT this plugin's line.** The
> wrv-plugins PR #1 "canonical" RestartCountdown source is a
> different, **uninstalled 1.1.1 build** (api-version 1.21, title
> announcements, onDisable cancellation, separate cancellable
> shutdown task). This rename was deliberately built from live 1.1.0
> so no unshipped behavior sneaks in under cover of a rename. The
> PR #1 RestartCountdownTest targets 1.1.1 and does not apply here; a
> new behavior suite was written for the 1.1.0 line instead (§5).
> **DECIDED Oct 16, 2026:** Mike marked the divergent 1.1.1 line
> ABANDONED / UNUSED — never installed, do not build or ship it.
> The live line was 1.1.0, renamed here to ExcelRestartCountdown
> 1.2.0 (WRV-060). Marking comment on PR #1:
> https://github.com/ElMikeExcel/wrv-plugins/pull/1#issuecomment-6216545722

## 3. Data-folder migration

On first enable, if `plugins/RestartCountdown/` exists and
`plugins/ExcelRestartCountdown/` contains no files yet, the legacy
tree is copied recursively, byte-for-byte (`COPY_ATTRIBUTES`), and
the legacy folder is **left in place**. Logs
`Migrated legacy data folder plugins/RestartCountdown -> plugins/ExcelRestartCountdown (N files)`.
Missing/empty legacy folder = no-op; a new folder that already has
files is never overwritten; failures are logged, never fatal.

## 4. Change-scope proof

- Normalized `javap -p -c` diff vs the live jar: 59 diff lines, all
  accounted for — the migration wiring, the command-name check moving
  to `excelrestartwarn`, and the permission check gaining the new node
  ahead of the legacy one. Everything else instruction-identical.
  Full report: project `build/scope-diff/RestartCountdown.txt`.
- The only new class in the jar is `LegacyDataMigration`.
- `plugin.yml` read back from the built jar: name
  `ExcelRestartCountdown`, version `1.2.0`, main as above, command
  `excelrestartwarn` with alias `restartwarn`, both permission nodes
  declared with default op.

## 5. Build & verification (Moon's playbook)

- **Toolchain:** JDK 25, `javac --release 21`, Oct 15 classpath.
- **Tests:** 10 passing — a new suite written for the live 1.1.0
  line: permission refusal with neither node; **legacy node alone
  works**; new node works; unknown command ignored;
  start/refuse-second/cancel flow — + 5 migration tests.
- **Rebuild reproduction:** two consecutive builds byte-identical
  (deterministic packager; see the project RENAME-REPORT §5).
- **Scratch boot:** Paper 26.2 build 123 with Vault,
  EssentialsX(+Spawn), Jobs, CMILib, custom mcMMO, PlaceholderAPI.
  Console probes: `excelrestartwarn cancel` → `No countdown is running.`
  and `restartwarn cancel` → `No countdown is running.` (alias proven
  live, not just declared). Seeded legacy folder → migration log line
  + byte-identical copies + legacy intact; `version ExcelRestartCountdown`
  → `ExcelRestartCountdown version 1.2.0`; no SEVERE. A real countdown
  was never started in any probe, on purpose.

## 6. Rollback

Panel Shut Down → delete `ExcelRestartCountdown-1.2.0.jar` → rename
`RestartCountdown-1.1.0.jar.bak-20261016` back → Start → confirm
`restartwarn cancel` answers `No countdown is running.` Full
procedure: project INSTALL-PACKET. Rollback also restores the old
command/permission names as the only names (the new jar is gone), and
any grants under either node remain valid for the old jar's node.

## 7. What this release does not do

- It does not adopt the uninstalled PR #1 1.1.1 feature set (no title
  announcements, no onDisable cancellation changes) — that line was
  marked ABANDONED / UNUSED on Oct 16, 2026; see the §2 flag.
- It does not change the countdown length, messages, or the shutdown
  behavior.
- It does not add the command to any player-facing menu or catalog
  (it stays admin-only, as before).

## 8. Pre-publish gates

1. **Install.** Done October 16, 2026, ~8:46 AM CDT via auto-watch
   (gates 8:41 + 8:43 AM CDT). Old jar kept as
   `RestartCountdown-1.1.0.jar.bak-20261016`. This draft pull request
   is the record. Built from live RestartCountdown 1.1.0. The PR #1
   RestartCountdown 1.1.1 line is ABANDONED / UNUSED.
2. A real countdown start is deliberately not an in-game check (it
   ends in a shutdown). Alias `restartwarn` was probed at install
   (`restartwarn cancel` → `No countdown is running.`).
3. **Website name updates** for this rename are **still open**.
4. Publishing mechanics follow Mike's standing function rules
   (GitHub writes pre-authorized; wrv-plugins PRs stay open drafts;
   website update merged + live-verified byte-for-byte; shepherd
   connector writes while Mike is awake — approval cards expire).

*End of draft.*
