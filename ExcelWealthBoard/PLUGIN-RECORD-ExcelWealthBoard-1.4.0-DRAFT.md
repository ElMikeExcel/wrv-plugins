# ExcelWealthBoard 1.4.0 — rename of WealthBoard 1.3.0-mobile.7

> **INSTALL RECORD — this draft pull request is the record.**
>
> Installed live October 16, 2026, ~8:46 AM CDT via auto-watch
> (empty-server gates 8:41 AM and 8:43 AM CDT). The old jar was kept
> as `.bak-20261016`. Repo `wrv-plugins`, branch
> `record/excel-renames-part-2-2026-10-16` (WRV-060, Excel Renames
> Part 2), this record under `ExcelWealthBoard/`. Draft per this
> repo's live-version-record pattern; stays unmerged.
>
> Prepared earlier the same day as a local draft while the jar was
> still staged. The install has since happened. Website name updates
> and the in-game checks named in §8 are still open.

## Status at a glance

Statuses are kept separate, per the server rule. Do not collapse them.

| Status | State |
|---|---|
| Built | **Yes** — October 16, 2026, local build |
| Automated tests | **Yes — 16 passing** (repo behavior contract incl. the mobile.7 popup frame timings, + migration suite) |
| Test server (scratch boot) | **Passed** — clean enable with Vault + Jobs + Essentials present; seeded legacy folder migrated byte-for-byte (project verification logs, Boots A/C) |
| Installed live | **Yes** — October 16, 2026, ~8:46 AM CDT, via auto-watch (gates 8:41 + 8:43 AM CDT); old jar kept as `.bak-20261016` |
| Java check | **Not done** — a real player join (popup render + Jobs auto-enroll) is still open |
| Bedrock check | **Not done** — same join check covers both client types; still open |

## 1. Artifact

- **Plugin:** ExcelWealthBoard (was WealthBoard)
- **Version:** 1.4.0 (was 1.3.0-mobile.7; the `-mobile.N` suffix chain
  is dropped with this rename — it was a build-channel tag, and nothing
  reads it)
- **Jar:** `ExcelWealthBoard-1.4.0.jar`
- **SHA-256:** `f989a82431e0a5fb509bd8cff9f15b9452abbd4dfae332cdf816e5b6517cfee2`
- **Main class:** `com.winkreyes.excelwealthboard.ExcelWealthBoard`
  (was `com.winkreyes.wealthboard.WealthBoard`)
- **Commands / permissions / placeholders:** none (unchanged)
- **depend:** `[Vault]`; **softdepend:** `[Jobs]` (both unchanged —
  third-party plugins keep their names)
- **Data folder:** `plugins/ExcelWealthBoard/` (was
  `plugins/WealthBoard/`; first-boot auto-migration, §3). No live data
  folder exists for this plugin today; its leaderboard state is
  in-memory and rebuilt from Vault/Jobs on enable.
- **Rollback artifact:** `WealthBoard-1.3.0-mobile.7.jar.bak-20261016`
  kept on the server at install (see the project INSTALL-PACKET).

## 2. What changed vs 1.3.0-mobile.7

The identity, and nothing else:

- Package and class renamed (above); jar and data folder renamed.
- `plugin.yml`: name/version/main updated; description, depend,
  softdepend, api-version (`"1.13"`) carried over exactly.
- Added `LegacyDataMigration` + a null-guarded first step in
  `onEnable` (§3).
- **Zero behavior changes.** The popup/welcome layout is Mike-approved
  current behavior and is byte-proven unchanged (§4): the mobile.7
  frame contract — intermediate frames fadeOut 0 / stay 70, last frame
  fadeOut 15 / stay 55 — is exactly what ships.

Source of record: wrv-plugins PR #12 branch
`fix/wealthboard-popup-cycle-2026-10-13`, **proven** to be the live
mobile.7 build before renaming (compiled under the original identity;
normalized `javap -c` diff vs the live jar = 0 lines). The local
`~/workspace/wealthboard/` tree is stale mobile.6 and was not used.

## 3. Data-folder migration

On first enable, if `plugins/WealthBoard/` exists and
`plugins/ExcelWealthBoard/` contains no files yet, the legacy tree is
copied recursively, byte-for-byte (`COPY_ATTRIBUTES`), and the legacy
folder is **left in place**. Logs
`Migrated legacy data folder plugins/WealthBoard -> plugins/ExcelWealthBoard (N files)`.
Missing/empty legacy folder = no-op; a new folder that already has
files is never overwritten; failures are logged, never fatal.

## 4. Change-scope proof

- Normalized `javap -p -c` diff vs the live jar: the inner `Entry`
  class is **0 lines** different; the main class differs only by the
  added migration call, the `migrateLegacyDataFolder()` method, and
  its log lambda (47 diff lines, all additions of the migration
  wiring). Full report: project `build/scope-diff/WealthBoard.txt`.
- The only new class in the jar is `LegacyDataMigration`.
- `plugin.yml` read back from the built jar: name `ExcelWealthBoard`,
  version `1.4.0`, main as above.

## 5. Build & verification (Moon's playbook)

- **Toolchain:** JDK 25, `javac --release 21`, Oct 15 classpath +
  VaultAPI + the backup's `Jobs.jar`.
- **Tests:** 16 passing — the renamed repo suite (popup frame sequence
  and timings, welcome/Jobs-enroll behavior) + 5 migration tests
  (byte-for-byte nested copy with legacy untouched; skip when the new
  folder has files; missing-legacy no-op; empty-legacy no-op;
  empty-new-folder migrates).
- **Rebuild reproduction:** two consecutive builds byte-identical
  (deterministic packager; see the project RENAME-REPORT §5).
- **Scratch boot:** Paper 26.2 build 123 with Vault, EssentialsX(+Spawn),
  Jobs, CMILib, custom mcMMO, PlaceholderAPI. Seeded legacy folder →
  migration log line + byte-identical copies + legacy intact; enable
  line `ExcelWealthBoard enabled (join popup mode, no sidebar).`;
  `version ExcelWealthBoard` → `ExcelWealthBoard version 1.4.0`; no
  SEVERE; no error naming the plugin.

## 6. Rollback

Panel Shut Down → delete `ExcelWealthBoard-1.4.0.jar` → rename
`WealthBoard-1.3.0-mobile.7.jar.bak-20261016` back → Start. Any
`plugins/ExcelWealthBoard/` folder is harmless to the old jar; the
legacy folder was never moved. Full procedure: project INSTALL-PACKET.

## 7. What this release does not do

- It does not change any popup frame, timing, text, or the Jobs
  auto-enroll behavior.
- It does not rename or touch Vault, Jobs, or any third-party plugin.
- It does not migrate any player data (there is none persisted; state
  is rebuilt on enable) — the folder migration exists for completeness
  and future-proofing.

## 8. Pre-publish gates

1. **Install.** Done October 16, 2026, ~8:46 AM CDT via auto-watch
   (gates 8:41 + 8:43 AM CDT). Old jar kept as
   `WealthBoard-1.3.0-mobile.7.jar.bak-20261016`. This draft pull
   request is the record.
2. **In-game join check** (Java or Bedrock): popup renders as before;
   Jobs auto-enroll fires for a not-yet-enrolled player. **Still
   open** after the live install.
3. **Website name updates** for this rename are **still open**.
4. Publishing mechanics follow Mike's standing function rules
   (GitHub writes pre-authorized; wrv-plugins PRs stay open drafts;
   website update merged + live-verified byte-for-byte; shepherd
   connector writes while Mike is awake — approval cards expire).

*End of draft.*
