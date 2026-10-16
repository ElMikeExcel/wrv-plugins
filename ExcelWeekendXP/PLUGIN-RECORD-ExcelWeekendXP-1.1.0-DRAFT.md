# ExcelWeekendXP 1.1.0 — rename of WeekendXP 1.0.2

> **INSTALL RECORD — this draft pull request is the record.**
>
> Installed live October 16, 2026, ~8:46 AM CDT via auto-watch
> (empty-server gates 8:41 AM and 8:43 AM CDT). The old jar was kept
> as `.bak-20261016`. Repo `wrv-plugins`, branch
> `record/excel-renames-part-2-2026-10-16` (WRV-060, Excel Renames
> Part 2), this record under `ExcelWeekendXP/`. Draft per this
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
| Automated tests | **Yes — 30 passing** (schedule, bridge, main-plugin suites, + migration suite) |
| Test server (scratch boot) | **Passed** — clean enable against the real custom mcMMO build; bridge verified in the boot log; seeded legacy folder migrated byte-for-byte |
| Installed live | **Yes** — October 16, 2026, ~8:46 AM CDT, via auto-watch (gates 8:41 + 8:43 AM CDT); old jar kept as `.bak-20261016` |
| Java check | **Not done** — no player-facing UI; the first weekend-boundary log line after install is the functional check and is still open |
| Bedrock check | **Not applicable** — server-side XP rates only |

## 1. Artifact

- **Plugin:** ExcelWeekendXP (was WeekendXP)
- **Version:** 1.1.0 (was 1.0.2)
- **Jar:** `ExcelWeekendXP-1.1.0.jar`
- **SHA-256:** `e9150198dd3260ad5ae829067f84131cc2dabc135e495e6c5d7e41f228417fa1`
- **Main class:** `com.winkreyes.excelweekendxp.ExcelWeekendXP`
  (was `com.winkreyes.weekendxp.WeekendXP`); `WeekendSchedule` and
  `VanillaXpBridge` keep their class names inside the new package.
- **Commands / permissions / placeholders:** none (unchanged)
- **depend:** `[mcMMO]` (unchanged — mcMMO is third-party and keeps
  its name); api-version `"1.21"` preserved from the live jar.
- **Data folder:** `plugins/ExcelWeekendXP/` (was
  `plugins/WeekendXP/`; first-boot auto-migration, §3). No live data
  folder exists for this plugin today.
- **Rollback artifact:** `WeekendXP-1.0.2.jar.bak-20261016` kept on
  the server at install (see the project INSTALL-PACKET).

## 2. What changed vs 1.0.2

The identity, and nothing else:

- Package renamed; main class renamed; jar and data folder renamed.
- `plugin.yml`: name/version/main updated; description, depend,
  api-version carried over exactly.
- One identity string in `VanillaXpBridge`: the bridge-missing
  warning now reads "ExcelWeekendXP 1.1.0 requires the matching
  custom mcMMO vanilla XP bridge; check the installed mcMMO build…"
  (was "WeekendXP 1.0.2 requires…"). Verified at the constant-pool
  level; the only string change in that class.
- Added `LegacyDataMigration` + a null-guarded first step in
  `onEnable` (§3).
- **Zero behavior changes.** Schedule (Sat–Sun America/Chicago), the
  1.2x rates, the 30-minute re-check, the dispatched
  `mcmmo:xprate 1.2 true` / `mcmmo:xprate reset` console commands, and
  the reflection bridge into the custom mcMMO build are all unchanged
  and byte-proven (§4). A descriptive comment in mcMMO's own default
  config mentions "WeekendXP" — prose, not a lookup; left alone.

Source of record: wrv-plugins PR #2 branch
`feature/together-weekend-xp-2026-10-12`, **proven** to be the live
1.0.2 build before renaming (compiled under the original identity;
normalized `javap -c` diff vs the live jar = 0 lines, all three
classes). The local `~/workspace/weekendxp/` tree is stale 1.0.0 (a
different, mcMMO-only implementation) and was not used.

## 3. Data-folder migration

On first enable, if `plugins/WeekendXP/` exists and
`plugins/ExcelWeekendXP/` contains no files yet, the legacy tree is
copied recursively, byte-for-byte (`COPY_ATTRIBUTES`), and the legacy
folder is **left in place**. Logs
`Migrated legacy data folder plugins/WeekendXP -> plugins/ExcelWeekendXP (N files)`.
Missing/empty legacy folder = no-op; a new folder that already has
files is never overwritten; failures are logged, never fatal.

## 4. Change-scope proof

- Normalized `javap -p -c` diff vs the live jar: `WeekendSchedule`,
  `WeekendSchedule$Result`, and `VanillaXpBridge` are **0 lines**
  different in bytecode; the main class differs only by the added
  migration wiring (47 diff lines, all additions). Full report:
  project `build/scope-diff/WeekendXP.txt`.
- The only new class in the jar is `LegacyDataMigration`.
- `plugin.yml` read back from the built jar: name `ExcelWeekendXP`,
  version `1.1.0`, main as above.

## 5. Build & verification (Moon's playbook)

- **Toolchain:** JDK 25, `javac --release 21`, Oct 15 classpath.
- **Tests:** 30 passing — the renamed repo suites (weekend/weekday
  schedule boundaries, bridge reflection behavior against fake mcMMO
  shapes, main-plugin enable/dispatch) + 5 migration tests.
- **Rebuild reproduction:** two consecutive builds byte-identical
  (deterministic packager; see the project RENAME-REPORT §5).
- **Scratch boot:** Paper 26.2 build 123 with the **real custom
  mcMMO build** (2.3.003-WRV-XP-2) plus Vault, EssentialsX(+Spawn),
  Jobs, CMILib, PlaceholderAPI. Friday boot →
  `Weekday detected: dispatched mcMMO XP-rate reset command; verified vanilla XP event multiplier 1.0x.`
  — the bridge confirms support on the actual mcMMO and the namespaced
  dispatch path works end-to-end. Seeded legacy folder → migration log
  line + byte-identical copies + legacy intact; `version ExcelWeekendXP`
  → `ExcelWeekendXP version 1.1.0`; no SEVERE.

## 6. Rollback

Panel Shut Down → delete `ExcelWeekendXP-1.1.0.jar` → rename
`WeekendXP-1.0.2.jar.bak-20261016` back → Start. **Note:** the old
jar's first boot re-applies the correct rate for the current day, so
a rollback self-heals the XP state within one scheduler cycle. Full
procedure: project INSTALL-PACKET.

## 7. What this release does not do

- It does not change the weekend window, the 1.2x rates, or which XP
  (mcMMO + vanilla) is boosted.
- It does not rename or touch mcMMO, and does not change the
  `mcmmo:xprate` commands it dispatches.
- It does not add a config file or any player-facing surface.

## 8. Pre-publish gates

1. **Install.** Done October 16, 2026, ~8:46 AM CDT via auto-watch
   (gates 8:41 + 8:43 AM CDT). Old jar kept as
   `WeekendXP-1.0.2.jar.bak-20261016`. This draft pull request is
   the record.
2. **First weekend-boundary check after install:** the Sat 00:00
   America/Chicago scheduler line applying 1.2x (and the Mon 00:00
   reset) is the functional proof. **Still open.**
3. **Website name updates** for this rename are **still open**.
4. Publishing mechanics follow Mike's standing function rules
   (GitHub writes pre-authorized; wrv-plugins PRs stay open drafts;
   website update merged + live-verified byte-for-byte; shepherd
   connector writes while Mike is awake — approval cards expire).

*End of draft.*
