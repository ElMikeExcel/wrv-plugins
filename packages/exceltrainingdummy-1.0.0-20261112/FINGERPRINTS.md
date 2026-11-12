# FINGERPRINTS — ExcelTrainingDummy 1.0.0

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-TDUMMY-1.0.0-WRV-20261112-01-178829`
  (SKU TDUMMY · version 1.0.0 · channel WRV · 2026-11-12 · seq 01 ·
  tag 178829 = first 6 hex of SHA-256 of "EXC-TDUMMY-1.0.0-training-dummy").
  First build of this SKU; no previous builds.
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant, assembled from string fragments in
  `src/com/winkreyes/exceltrainingdummy/BuildInfo.java`; (3) read
  back at runtime by `/dummy version` and the enable line. The
  standard's third home (a generated data-file header) does not
  apply — this plugin writes no data files; sessions are in-memory
  by design.
- The build's fingerprint gate verifies homes (1) and (2) in the
  shipped jar on every build; it passed for this artifact.
- Artifact identity: `ExcelTrainingDummy-1.0.0.jar` — 49,376 B —
  SHA-256 b46250ed7a408a447749b8f62e10c432c235c9050a1e4dcff06a8138c516c098,
  reproduced by the in-tree double-build (byte-identical).

## Semantic markers (Layer C — categories + where to look)

1. **Default config values with a signature shape:** the tier pool
   triplet 1000 / 5000 / 20000 paired with checkpoint quartiles at
   25/50/75% consumed and hazard pulse defaults 2 / 4 / 8 every
   40 ticks (config.yml + core/Tier.java + core/Hazard.java).
2. **Default config phrasing:** the config.yml comment voice
   ("The dummy's health pool is virtual…", "Putting dummies away.").
3. **Schema dialect:** the entity stamp keys — PDC `training` +
   `owner` under this plugin's namespace and the scoreboard tag
   `excelTrainingDummy` — are how a leaked install's world data
   names this SKU.
4. **Message-string voice:** the report block's exact phrasing
   ("Test complete —", "Your damage by source:", "Checkpoint 25% —
   your DPS so far:") in core/ReportText.java.
5. **Algorithmic quirk:** overkill is capped at the remaining pool
   before it reaches a ledger, and a multi-checkpoint hit reports
   the oldest pending checkpoint first (core/TestSession.java).
6. **Innocuous idiosyncrasy:** the permission nodes are spelled
   with the full product name (`exceltrainingdummy.use` /
   `.admin`) where sibling SKUs abbreviate.

## Layer D

Channel WRV (our own server's build; never distributed). No other
channel variants minted for 1.0.0.
