# FINGERPRINTS — ExcelAutoSort 0.2.3

Per the house standard: passive provenance only — no
phone-home, no telemetry, no sabotage.

## Build identity (Layer B)

- Build ID: `EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07`
  (SKU AUTOSORT · version 0.2.3 · channel WRV · 2026-11-14 ·
  seq 01; tag = first 6 hex of SHA-256 of the ID stem).
- Homes: (1) `excel-build.txt` at the jar root — verified in
  this prep: the candidate jar's `excel-build.txt` reads
  exactly `EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07`;
  (2) the `BuildInfo` constant, assembled from string
  fragments in
  `src/com/winkreyes/excelautosort/BuildInfo.java`
  (`"EXC-AUTOSORT-" + "0.2.3" + "-WRV-20261114-" + "01" +
  "-" + "c41a07"`).
- The local build's fingerprint gate verifies homes (1) and
  (2) in the shipped jar on every build; it passed for this
  artifact (BUILD-RECORD.md / RECEIPTS.md). The live awake
  line at install named this exact build ID (OUTCOME.txt).
- Prior builds: `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`
  (0.2.1 record, PR #174); the 0.2.2 (BB-04) candidate jar
  is 160,197 B / `8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28`
  (no record PR — flagged separately, not part of this
  package).

## Semantic markers (Layer C — persist version to version)

All markers carried from the 0.2.1 record persist unchanged
except where the 0.2.3 increment itself is the change.
0.2.3 adds:

- **The in-dialog filter toggle:** Java selector slot 3
  LEVER button and the Bedrock main form's FIRST button,
  worded by the four new config keys only
  (`messages.filter-toggle-on`, `messages.filter-toggle-off`,
  `messages.form-filter-toggle-on`,
  `messages.form-filter-toggle-off`) — the install's config
  merge was exactly those 4 keys and nothing else.
- **Toggle memory semantics:** OFF never erases the chest's
  last active mode; ON re-arms in that mode (WHITELIST if it
  never filtered) through the existing confirm gate
  (`core/FilterToggleCore.java`).
- **Menu placement:** the `autosort` ExcelMenu entry lives
  in group `chest_tools`, order 25 / order-bedrock 245
  (`menu/autosort.yml` in this package, 2,381 B /
  `d579593873757466c34797fd2c85fe6cca36b9bfa209682bed7a9e324c24f8c8`).
