# FINGERPRINTS — ExcelAutoSort 0.1.0

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-AUTOSORT-0.1.0-WRV-20261106-04-f96f93`
  (SKU AUTOSORT · version 0.1.0 · channel WRV · 2026-11-06 · seq 02).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, assembled from string fragments in
  `src/com/winkreyes/excelautosort/BuildInfo.java`; (3) the header
  stamped into every data file the plugin writes
  (`plugins/ExcelAutoSort/data/*.yml` carry the build line).
- The local build's fingerprint gate verifies homes (1) and (2) in
  the shipped jar on every build; it passed for this artifact.
- Seq note: `-01-5e7a3c` was an intermediate pre-release build of
  0.1.0 from earlier the same night, superseded before any install
  when Mike's in-flight rulings (targeted access, compass entry, the
  GP configure-rights rule) were folded in; `-02-a59fab` and
  `-03-59c5eb` were in turn superseded the same night as the
  acceptance rig caught and then pinned down the double-chest
  keying defect (see BUILD-RECORD). Only `-04-f96f93` was filed
  for install; the registry ledger carries the -04 row.

## Semantic markers (Layer C — persist version to version)

1. **C2, config phrasing:** `config.yml`'s voice — the header
   ("Every player-facing sentence the plugin speaks lives in this
   file"), the hopper comment's family-weekend reasoning, the
   recovery comment ("the family plays heaviest on weekends").
2. **C3, data header / schema dialect:** data files open with the
   ExcelAutoSort build header; chest records use the
   `world:x:y:z` canonical key dialect and the field dialect
   `owner-name` / `suspend-reason`.
3. **C4, message voice:** "Recovery box" naming throughout
   (`gui-recovery-title`, claim lines), the confirm screen's
   "Check before it starts" / "Not yet" wording.
4. **C5, algorithmic constants:** buffer cap **162** (three double
   chests), the **hourly** buffer sweep cadence, selector page size
   **28**, targeted-command reach **6**.
5. **C6, idiosyncrasy:** the `/autosort` help listing is in WORKFLOW
   order (chest, recover, status, admin) — a deliberate house habit,
   documented in the command class; the compass-tap settings entry
   (§7.1) is a house-pattern interaction unique to this plugin's
   settings flow.
