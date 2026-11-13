# ExcelSkillsPanel 0.1.0 — BUILD RECORD

**Product:** ExcelSkillsPanel 0.1.0 — the unified Skills panel
(menu programme item 2): a character header (Power Level + the
FULL Magic block) and per-skill rows with levels and XP
progress bars, MMO-style, in a Java chest and a Bedrock
Cumulus form over one pure PanelModel (the ExcelSummons
pattern).
**Commission:** Mike, Nov 12, 2026 (menu programme): DESIGN.md
§7 (panel design, provider seam) + DECISIONS.md (the panel is
its own standalone plugin; Q2 = FULL Magic header — verify
path/rank FIRST) + the run-273 verification
(MAGIC-HEADER-VERIFICATION-bm273.md), which superseded DESIGN
§7.2's two blockers.
**Build:** EXC-SKILLSPANEL-0.1.0-WRV-20261112-01-c3d9a1
**Built:** 2026-11-12 (America/Chicago)
**Command:** `/skillpanel` — collision-checked against the
Magic, mcMMO (WRV fork), ExcelMagicLevel, ExcelGuildGate jars
and the ExcelMenu/menu-programme sources: no existing command
registers it. (A live-plugin-list re-check is a G1 gate in
INSTALL-PACKET.md.)

## Deliverables (this folder)

- `ExcelSkillsPanel/ExcelSkillsPanel-0.1.0.jar` — **45,497 B**
  — SHA-256 `4f9f819d75a4e9fba098cbc1ade42e2d01265d678948993f918083125d23cb19`
- `ExcelSkillsPanel/ExcelSkillsPanel-0.1.0-sources.zip` —
  37,028 B — SHA-256 `ff3dd1cbbcfb32a04464225aa96edde8046f531e4e281c3f202215e422980ae2`
  (src/ + tests/ + plugin.yml + local-build.sh, timestamps
  pinned 2026-11-12)
- `TEST-RESULTS.md` — 18/18 harness tests.
- `RIG-RESULTS.md` — two-phase desk rig, 30/30 PASS both
  phases, SEVERE 0, incl. the rank-name call-chain proof.
- `INSTALL-PACKET.md` — the gated install packet (NOT filed;
  the board supervisor verifies and files).
- `records-package/` — the staged records package (jar +
  sources + this record + SOURCE.md + INSTALL-PACKET.md +
  SHA256SUMS).

## Verification

- **Deterministic double-build: byte-identical** (both jars
  4f9f819d…19 in one build run).
- **Clean-room rebuild: byte-identical** — the shipped sources
  zip extracted to a fresh directory and built with its own
  local-build.sh reproduces 4f9f819d…19 exactly (tests re-run
  green inside it).
- **Fingerprint gate: PASS** — excel-build.txt +
  BuildInfo fragments carry EXC-SKILLSPANEL-0.1.0-WRV-20261112-01-c3d9a1
  in the shipped jar.
- **Rig:** see RIG-RESULTS.md. Headline: the rank-name proof
  settled on the active wand's path (the class chain reports
  the base path); the composed header carries "Rank:
  Apprentice" from Mike's own mage fixture, matching his
  wand's lore.

## Design decisions taken inside the commission

- **Child skills (rig-found):** Salvage and Smelting are
  mcMMO child skills with levels but no XP; they render
  level-only with an explanatory detail view instead of a
  fake bar (DESIGN counted 19 skills; the bar design did not
  distinguish child skills).
- **Casts/boosts without the optional ExcelMagicLevel
  placeholder build:** total casts reads ExcelMagicLevel's
  live store reflectively (persisted players.yml fallback);
  regen/cost boost % derive from ExcelMagicLevel's own live
  config fractions via its RewardTable formula. Both omit
  silently-never — failures log and the line drops.
- **Party-share line omitted** from the Magic detail view:
  ExcelMagicLevel keeps that string in a local variable at
  enable; no verified surface exposes it (the /magiclevel
  chat output still shows it, unchanged).
- **Magic "of max" omitted:** the PAPI surface does not expose
  the curve's max level, so the header shows the level and
  total XP without an invented cap figure (the bar renders
  MAX at the cap, which the surface does report).
- **Power Level cap line:** shown only when getPowerLevelCap
  returns a positive value (the fork returns Integer.MAX_VALUE
  when uncapped in practice — the rig printed it; on live the
  line will read the same way mcMMO reports it. Noted as a
  cosmetic watch item for Mike's live pass, not a data error:
  the value is mcMMO's own).

## Honest limits

- No real Bedrock client on a desk rig: form send to a real
  Bedrock connection + both presentations' in-game look are
  Mike's live-pass proofs.
- The plugin is read-only: it writes no data files, so there
  is nothing to migrate and rollback is jar removal.
- NOT installed, NOT filed in the MANIFEST (per commission —
  the board supervisor verifies this return and files).
