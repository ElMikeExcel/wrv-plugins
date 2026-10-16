# ExcelNoPhantoms 1.1.0 — rename of NoPhantoms 1.0.0

> **INSTALL RECORD — this draft pull request is the record.**
>
> Installed live October 16, 2026, ~8:46 AM CDT via auto-watch
> (empty-server gates 8:41 AM and 8:43 AM CDT). The old jar was kept
> as `.bak-20261016`. Repo `wrv-plugins`, branch
> `record/excel-renames-part-2-2026-10-16` (WRV-060, Excel Renames
> Part 2), this record under `ExcelNoPhantoms/`. Draft per this
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
| Automated tests | **Yes — 7 passing** (phantom-cancel suite, + migration suite) |
| Test server (scratch boot) | **Passed** — clean enable; seeded legacy folder migrated byte-for-byte |
| Installed live | **Yes** — October 16, 2026, ~8:46 AM CDT, via auto-watch (gates 8:41 + 8:43 AM CDT); old jar kept as `.bak-20261016` |
| Java check | **Not done** — no player-facing UI; phantom suppression in live play is still open |
| Bedrock check | **Not applicable** — server-side spawn rule only |

## 1. Artifact

- **Plugin:** ExcelNoPhantoms (was NoPhantoms)
- **Version:** 1.1.0 (was 1.0.0)
- **Jar:** `ExcelNoPhantoms-1.1.0.jar`
- **SHA-256:** `2c8f3a7688df3f11f295b29d220de74d4c08f37b0ba4d6a103f5d53128529609`
- **Main class:** `com.winkreyes.excelnophantoms.ExcelNoPhantoms`
  (was `com.winkreyes.nophantoms.NoPhantoms`)
- **Commands / permissions / placeholders / config:** none (unchanged)
- **depend / softdepend:** none; api-version `"1.13"` preserved from
  the live jar.
- **Data folder:** `plugins/ExcelNoPhantoms/` (was
  `plugins/NoPhantoms/`; first-boot auto-migration, §3). No live data
  folder exists for this plugin today, and the plugin writes nothing
  to it — the folder exists only because Bukkit assigns one.
- **Rollback artifact:** `NoPhantoms-1.0.0.jar.bak-20261016` kept on
  the server at install (see the project INSTALL-PACKET).

## 2. What changed vs 1.0.0

The identity, and nothing else:

- Package and class renamed (above); jar and data folder renamed.
- `plugin.yml`: name/version/main updated; description and
  api-version carried over exactly.
- The enable log line carries the new name.
- Added `LegacyDataMigration` + a null-guarded first step in
  `onEnable` (§3).
- **Zero behavior changes.** The spawn handler — cancel every
  `CreatureSpawnEvent` whose entity type is PHANTOM — is
  instruction-for-instruction identical (§4).

Source of record: local tree `~/workspace/nophantoms/`, **proven** to
be the live 1.0.0 build before renaming (repo Java identical;
compiled under the original identity, normalized `javap -c` diff vs
the live jar = 0 lines; local `plugin.yml` matches the live jar).

## 3. Data-folder migration

On first enable, if `plugins/NoPhantoms/` exists and
`plugins/ExcelNoPhantoms/` contains no files yet, the legacy tree is
copied recursively, byte-for-byte (`COPY_ATTRIBUTES`), and the legacy
folder is **left in place**. Logs
`Migrated legacy data folder plugins/NoPhantoms -> plugins/ExcelNoPhantoms (N files)`.
Missing/empty legacy folder = no-op; a new folder that already has
files is never overwritten; failures are logged, never fatal. (In
practice there is nothing to migrate — see §1.)

## 4. Change-scope proof

- Normalized `javap -p -c` diff vs the live jar: the main class
  differs only by the added migration wiring (44 diff lines, all
  additions); the `onCreatureSpawn` handler body is identical. Full
  report: project `build/scope-diff/NoPhantoms.txt`.
- The only new class in the jar is `LegacyDataMigration`.
- `plugin.yml` read back from the built jar: name `ExcelNoPhantoms`,
  version `1.1.0`, main as above.

## 5. Build & verification (Moon's playbook)

- **Toolchain:** JDK 25, `javac --release 21`, Oct 15 classpath.
- **Tests:** 7 passing — the renamed repo suite (phantom spawns
  cancelled, other entity types untouched, cancelled events ignored)
  + 5 migration tests.
- **Rebuild reproduction:** two consecutive builds byte-identical
  (deterministic packager; see the project RENAME-REPORT §5).
- **Scratch boot:** Paper 26.2 build 123 with Vault,
  EssentialsX(+Spawn), Jobs, CMILib, custom mcMMO, PlaceholderAPI.
  Seeded legacy folder → migration log line + byte-identical copies +
  legacy intact; enable line
  `ExcelNoPhantoms enabled: phantoms will not spawn.`;
  `version ExcelNoPhantoms` → `ExcelNoPhantoms version 1.1.0`; no
  SEVERE; no error naming the plugin.
- **Build note (owned):** the first insertion of the migration call
  into this class landed outside the class braces during the build
  and was caught by the compiler before any jar existed; the file was
  restructured and re-verified. Details in the project RENAME-REPORT
  §7.

## 6. Rollback

Panel Shut Down → delete `ExcelNoPhantoms-1.1.0.jar` → rename
`NoPhantoms-1.0.0.jar.bak-20261016` back → Start. Full procedure:
project INSTALL-PACKET.

## 7. What this release does not do

- It does not change which spawns are blocked (phantoms only), add a
  toggle, or add any config.
- It does not touch insomnia mechanics or any other plugin.

## 8. Pre-publish gates

1. **Install.** Done October 16, 2026, ~8:46 AM CDT via auto-watch
   (gates 8:41 + 8:43 AM CDT). Old jar kept as
   `NoPhantoms-1.0.0.jar.bak-20261016`. This draft pull request is
   the record.
2. Phantom suppression after install is confirmed passively in
   normal play (no phantoms at night). **Still open.** No separate
   tap test exists for this plugin.
3. **Website name updates** for this rename are **still open**.
4. Publishing mechanics follow Mike's standing function rules
   (GitHub writes pre-authorized; wrv-plugins PRs stay open drafts;
   website update merged + live-verified byte-for-byte; shepherd
   connector writes while Mike is awake — approval cards expire).

*End of draft.*
