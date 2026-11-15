# FINGERPRINTS — ExcelAutoSort 0.2.2

Per the house standard: passive provenance only — no
phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2`
  (SKU AUTOSORT · version 0.2.2 · channel WRV · 2026-11-14 ·
  seq 01; tag = first 6 hex of SHA-256 of the ID stem).
- Homes: (1) `excel-build.txt` at the jar root — verified in
  this prep: the candidate jar's `excel-build.txt` reads
  exactly `EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2`;
  (2) the `BuildInfo` constant, assembled from string
  fragments in
  `src/com/winkreyes/excelautosort/BuildInfo.java`
  (`"EXC-AUTOSORT-" + "0.2.2" + "-WRV-20261114-" + "01" +
  "-" + "8fdbf2"`).
- The local build's fingerprint gate verifies homes (1) and
  (2) in the shipped jar on every build; it passed for this
  artifact (BUILD-RECORD.md / RECEIPTS.md). The live awake
  line at install named this exact build ID (OUTCOME.txt).
- Prior builds: `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`
  (0.2.1 record, PR #174). Successor:
  `EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07` (0.2.3 record,
  PR #230), built on this 0.2.2 tree.

## Semantic markers (Layer C — persist version to version)

All markers carried from the 0.2.1 record persist unchanged
except where the 0.2.2 increment itself is the change.
0.2.2 adds (BB-04, 12 back-button audit rows):

- **Back landings:** the filter recovery box (Java slot 53;
  Bedrock Back button before Done), the chest-filter
  Selector (Java slot 49; Bedrock main form), and Max
  Organization Unlock (Java slot 45; Bedrock form) carry
  Back buttons landing on the ExcelMenu Server Menu via
  `performCommand("menu")`, with the recovery box returning
  to the Selector / main form when opened from it.
- **Relabels (handlers unchanged):** the shared
  `confirm-not-yet` config value reads "&eBack" (Selector +
  MaxOrg purchase confirms, Java + Bedrock modals); the
  MaxOrg Network button reads "Back" via the new
  `back-button` config key and still opens the Unlock
  screen.
- **Bedrock main form:** gains the Recovery box button the
  Java Selector already had (button indices after index 1
  shift: 2 recovery, 3 Back, 4 Done) — the entry path the
  Bedrock recovery Back depends on.
- **Install config merge was exactly 3 lines:**
  `confirm-not-yet` -> "&eBack"; added `back-button`:
  "&eBack"; added the `menu-open-failed` line (placed
  config 7,363 B / dea66e61…, OUTCOME.txt).
