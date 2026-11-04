# ExcelGuildGate 0.4.0 — SOURCE.md

## Artifact
- File: `ExcelGuildGate-0.4.0.jar` (in `../jars/`)
- SHA-256: `aa983fc40cbae6476b35113bc0611818b355400e36dc9da67196efd4b6f69f6e`
- Size: 92,823 bytes
- Rebuild reproduction: **PASS** — a clean copy of the staged tree,
  rebuilt with the tree's own `build.sh`, produced a byte-identical
  jar (verified twice, before and after the calibration edit below).

## Base
`~/workspace/excelguildgate/SOURCE-0.3.4/` — the shipped 0.3.4 source
record (its `ARTIFACT-SHA256.txt` records the 0.3.4 jar as
`b65dc579e5f1126a9865b3a2450744bd41fd0f3a9ec198cb6904081fafc6268d`).
The pristine 0.3.4 suite was re-run in this environment before any
change and passed in full, so the 0.4.0 suite results are comparable
against a green baseline. The 0.4.0 tree is that base plus the raid
companion change set.

## Toolchain
- JDK 25.0.4.1 (`~/workspace/.jdk25`, `JAVA_HOME`), `javac --release 21`.
- `BUILD_CP`: Paper 26.2 API/runtime jars + PlaceholderAPI 2.11.6
  (the shared dependency classpath; GuildGate needs no BossTrial
  classes — the dependency runs the other way).
- Build: `bash build.sh`; tests: `bash run-tests.sh`; jar entry date
  pinned 2026-10-27.

## What 0.4.0 changes (vs 0.3.4)
Per `~/workspace/seal-tuning/RAID-COMPANION-SPEC.md`:
- **RaidRepeatJudge + RaidRepeatBook**: the 0.3.4 `isBroken` early
  return in `onRaidFinish` no longer ends the story. A winning raid
  against an already-broken raid seal is judged
  FIRST_BREAK_PATH / REPEAT_CREDIT / DENY_REPEAT_WINDOW /
  REPEAT_NO_WINNER / REPEAT_OMEN_LOW against the repeat omen floor
  (max(5, configured)). The repeat window and clear counts persist
  in a new additive `raidRepeat` state section **before**
  RaidRepeatEvent fires; the repeat path never calls the seal-credit
  function (BossTrial's SealCreditHook veto log proves the no-op).
- **Events**: `api/RaidBreakEvent` (first break) and
  `api/RaidRepeatEvent` (repeat credit) carry seal id, winner UUIDs
  and names, omen level (the repeat event also carries the clear
  counts snapshot and the new window deadline). GuildGate never
  pays rewards; a BossTrial-side adapter pays via the claims path.
- **Raid pressure**: sessions tracked from RaidTriggerEvent at
  omen ≥ floor; per-raider health scaling at wave spawn
  (`health-multiplier × (1 + wave-ramp × waveIndex)`), flat attack
  scaling, extra waves behind a reflective call (default 0), and a
  2.0 HP / 20 s MAGIC attrition pulse while the raid is hot.
- **Calibration**: `raid.pressure.health-multiplier` default is
  **3.5**, solved 2026-11-01 from the vanilla Hard Bad-Omen-V wave
  tables (~2,562 aggregate HP; ramp-weighted ~3,698; ≈ 16.9 min at
  the reference group's 17 HP/s with 0.75 uptime). Full working:
  `../RAID-CALIBRATION.md`. An earlier build of this tree shipped
  3.0 in staging (`2d54c080…`); it was superseded by the calibrated
  build above before anything left the workspace.
- With `raid.repeat.enabled: false` and `raid.pressure.enabled:
  false`, behavior is the 0.3.4 behavior; the new state section is
  additive, so a 0.3.4 rollback reads a 0.4.0 state file safely.

## Live-config diff still owed at install time
No live GuildGate config was pulled during this build, so the
install packet (a) adds the tree's `raid:` section and the four
repeat message keys if the live config lacks them, and (b) requires
a live-config read at the install window before any merge. All
`raid.*` keys are read with the settled defaults in code, so a
pre-0.4.0 live config runs the full behavior even unmerged — the
merge is for visibility/retuning, not for function.
