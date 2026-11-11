# MANIFEST ENTRY DRAFT — ExcelAbilityBridge 0.1.1 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelAbilityBridge 0.1.1 (console admin) — packet: ~/workspace/abilitybridge-011-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/abilitybridge-011-build/INSTALL-PACKET.md`
  (build record: `~/workspace/abilitybridge-011-build/BUILD-RECORD.md`;
  commission: `~/workspace/console-admin-audit-20261109/AUDIT.md`
  §4).
- **Artifact:** `~/workspace/abilitybridge-011-build/ExcelAbilityBridge-0.1.1.jar`
  — **30,606 B** — SHA-256
  `62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55`
  (deterministic double-build byte-identical; no in-jar
  build-ID fingerprint exists in this lineage — version + hash
  identify it).
- **Live-jar gate (what must be live before the swap):**
  `ExcelAbilityBridge-0.1.0.jar` — 28,048 B — SHA-256
  `51f7dd303ac33115135b4415e09c8de153a93d1437650cb4cb0587c8d80740c9`
  (held aside at swap as `ExcelAbilityBridge-0.1.0.jar.bak-20261110`,
  date adjusted to the install day). An unmatched live jar STOPS
  the packet — lineage question, not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelAbilityBridge only. No
  config edit (config.yml byte-identical, no new keys; do not
  overwrite the live config), no data migration, no other plugin
  change, no permission grants required (console works at
  once; `excelabilitybridge.admin` is declared by this jar,
  default op). NOT a text live-edit — fully gated.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** one fresh backup for the window + cap-at-6 (name
  prunees; never the Daily newest or the two newest manuals);
  emptiness `list` = 0 twice; fresh hash of the staged 0.1.1 jar
  at the provenance gate.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelAbilityBridge v0.1.1`;
  - one of the 0.1.0 healthy enable shapes (bridge ready, or
    standing-by followed by the "bridge online" transition);
    a stand-down line = the EliteMobs seam broke, STOP;
  - no SEVERE attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `abilities status` prints the four status
  lines (enabled + version; EliteMobs link UP with its version
  once ACS is online — STANDING BY is the honest reading
  before the transition; Geyser/floodgate transport; the
  config/permissions line) — those lines in the log are the
  live proof; bare `abilities` prints the 0.1.0 console line
  verbatim.
- **What it adds:** ONE global status on the existing command —
  `/abilities status` (EliteMobs/Geyser link state), routed
  BEFORE the player-only gate so the console can run it, gated
  by the NEW `excelabilitybridge.admin` (default op; checked
  inside the status branch only — deliberately no
  command-level permission, so every Bedrock player keeps
  their own /abilities menu). Commissioned from the Nov 9,
  2026 console-admin audit (AUDIT.md §4 — ExcelAbilityBridge
  0.1.0 verdict FAIL, packet sized TINY: no per-player op,
  because per-player ability state belongs to EliteMobs)
  under Mike's Nov 3, 2026 console-admin rule. Player-facing
  behaviour: none — the form/fire/gateway paths are
  byte-for-byte 0.1.0 at jar level.
- **Proofs:** base proof — the unmodified 0.1.0 tree matches
  the live jar class-for-class byte-identically (one recorded
  plugin.yml author-line resource diff, resolved in the
  shipped jar's favour in the 0.1.1 tree); desk 174 distinct
  checks (150 carried 0.1.0 checks unchanged + 24 new
  StatusCommand checks; 198 raw executions across probe
  modes); deterministic double-build byte-identical; rig
  (Paper 26.2-123 + EliteMobs 10.9.8 + Geyser + floodgate)
  clean boot, SEVERE 0, **7/7 behavior checks typed on the
  real console stdin** (`rig-0.1.1/RIG-RESULTS.md`),
  including the UP status after the ACS transition and a
  first run proving the STANDING BY shape verbatim. Stated
  limitations: the non-admin denial is harness-pinned (no
  player sender in the rig); the DOWN status shape is
  unreachable on a normal boot by the plugin's own
  stand-down design.
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips check,
  menus check — itemized in the install packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelabilitybridge-0.1.1-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
