# ExcelEliteLadder 0.2.0 — BUILD RECORD

- **Built:** Nov 5, 2026 (America/Chicago), by Iris (build lane).
  Commissioned by Mike Nov 5, 2026: 2.0 decision sheet item 3 approved
  ("Yes to all 2.0") — the ladder table moves jar → config with strict
  validation, malformed-config refusal, and last-good fallback.
- **Tree:** `~/workspace/eliteladder-config-build/` — the live 0.1.2 build
  tree copied verbatim before any edit (0.1.2 jar of record: 31,403 B,
  SHA-256 `7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb`,
  installed live Nov 1, 2026). Contents: src/ (9 classes — NEW
  `core/LadderConfig` + `core/LadderConfigException`; `core/LadderTable`
  statics → instance), tests/ (the carried 0.1.2 suite adapted to the
  instance API + NEW `LadderConfigSuite`), build.sh, run-tests.sh,
  plugin.yml, config.yml, NEW ladder.yml, base-proof/ (unmodified-0.1.2
  rebuild proof), rig/ (scratch Paper 26.3-235 rig; evidence logs in
  rig/evidence/).
- **Candidate:** `ExcelEliteLadder-0.2.0.jar`, sha256
  `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`,
  40,475 B. build.sh assembles the jar twice and `cmp`s them —
  byte-identical.
- **Tests: 231 checks, 0 failures** — the carried 0.1.2 suite, the
  migration equality suite (shipped ladder.yml == the 0.1.2 hardcoded
  table, exact), a 25-case malformed battery (each refused + defect named
  + last-good kept), the reload-flip suite, and a three-boot Paper
  26.3-235 rig (fresh-install generation, in-session reload flip +
  refused reload, a real S4 apply driven by the flipped table,
  broken-config boot on the built-in default). SEVERE 0 on every clean
  boot. Detail: TEST-REPORT.md + TEST-RESULTS.txt.
- **Deviation of record (toolchain, predates the refactor):** the
  unmodified 0.1.2 sources rebuilt under this VM's toolchain come out
  31,499 B / `336a57d2…` vs the jar of record 31,403 B / `7ea61276…` —
  entry lists identical, plugin.yml + config.yml byte-identical, every
  .class differs (the original build machine's exact Paper API jar /
  javac patch does not exist in this VM). 0.2.0's acceptance does not
  depend on it: the migration proof is value-level and the rig runs the
  actual 0.2.0 jar on the actual Paper 26.3-235. See TEST-REPORT.md.
- **Install:** filed PENDING in `~/workspace/wrv-install-queue/MANIFEST.md`
  Nov 5, 2026 (INSTALL-PACKET.md, gated jar swap); INSTALLED LIVE Nov 5,
  2026, 6:04–6:20 AM CST by wrv-install-failsafe — see INSTALLED.txt.
  Player proofs owed: none (no player-facing change).
