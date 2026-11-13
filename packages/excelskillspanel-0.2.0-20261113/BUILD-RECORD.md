# ExcelSkillsPanel 0.2.0 — BUILD RECORD

**Product:** ExcelSkillsPanel 0.2.0 — the in-panel perk ladder
build (menu programme Item 8): every skill's detail view now
renders its real mcMMO perk ladder in-panel — one line per
perk (name + unlock requirement + earned/rank state at the
player's level) — replacing the "Full perk ladder: Skill
Trees page on the website" pointer for every skill.
**Commission:** board supervisor run 278 continuation
dispatch, Nov 13, 2026 (~12:54 AM CST), on the data lane's
verified spec of record
(`~/workspace/skillspanel-020-design-20261113/LADDER-DATA.md`)
and decision-log **D32**.
**Base:** ExcelSkillsPanel 0.1.1
(`~/workspace/skillspanel-011-build-20261113/`, untouched by
this build; jar 45,739 B / `084b110d…c2`). 0.1.1 is filed
PENDING and not yet installed at build time — 0.2.0 builds ON
0.1.1, and its install sequences after 0.1.1's (see
INSTALL-PACKET.md).
**Build:** EXC-SKILLSPANEL-0.2.0-WRV-20261113-01-e2f020
**Built:** 2026-11-13 (America/Chicago)
**Command:** `/skillpanel` (unchanged).

## Base reproduction gate (FIRST STEP — PASS)

Before any editing, the 0.1.1 tree was copied to a scratch
directory, rebuilt with its own `local-build.sh`, and the
result compared to the shipped 0.1.1 jar:

- Rebuilt jar: **45,739 B, SHA-256
  `084b110ddcc044f32f837e355f54d54ee274a80899d0375cad85bb1535e643c2`
  — byte-identical to the shipped 0.1.1 jar**, 21/21 harness
  tests green inside the reproduction, double-build identical,
  fingerprint gate PASS. The 0.2.0 build proceeded on that
  verified base. (The 0.1.1 folder itself was not modified.)

## G1 premises (receipted by the supervisor, relied on here)

`~/workspace/g1-pulls-bm278/RECEIPTS-mcmmo-ladder.md` (Nov 13,
2026, ~12:47–12:52 AM CST, PC route): live mcMMO jar
`4276bd3e…f5c` EXACT vs the LADDER-DATA expectation;
`General.RetroMode.Enabled: true` live; live
`plugins/mcMMO/skillranks.yml` (21,566 B / `99ca270a…`) parsed
VALUE-EQUAL to the jar-embedded `fd7cc0f1…` data the spec is
built on. The embedded table below was generated FROM that
G1-receipted live file.

## The embedded table — generation + three-way cross-check

`core/PerkLadder.java`'s table was generated from the
G1-receipted live `skillranks.yml` (RetroMode ladders), with
the display names and per-skill order taken from LADDER-DATA
§3. Before the Java was written, the generator cross-checked:

1. **Live skillranks (G1 pull)** — every ranked perk's Retro
   ladder read from the receipted live file;
2. **LADDER-DATA §3 parsed back out of the spec document** —
   every row's name, order, and full rank ladder compared:
   **zero mismatches**;
3. **Fork source `SubSkillType`** — every perk's ladder length
   equals its constant's numRanks, and every no-rank perk's
   constant really declares no rank count: **zero problems**.

Result: **82 perks across all 19 skills** (the spec's 83
display rows minus Flux Mining, omitted per D32).

## Data source at runtime (the choice, and why)

**Primary: the live mcMMO data, through the fork's own
surfaces.** On each panel assembly, `McMMoProvider` re-reads,
per skill, every perk's live rank count
(`SubSkillType.getNumRanks()`) and live unlock levels
(`RankConfig.getInstance().getSubSkillUnlockLevel(subSkill,
rank)` — the fork's own config surface over the live
`skillranks.yml`, in the server's **current** mode; both
methods verified present with identical signatures in the
compile-API jar and in the G1-receipted live jar `4276bd3e…`).

**The embedded verified table is the verification baseline
(and the source of display names/order), not a blind
fallback.** `PerkLadder.matchesLive` compares the live reads
against the embedded table per skill:

- **Exact match → the ladder renders**, with unlocks/ranks
  computed from the (now-proven-equal) values at the player's
  level by the pure RankUtils.getRank rule.
- **Any mismatch** — a drifted live skillranks.yml, a
  RetroMode→Standard mode change, a fork update that moves a
  threshold, a missing constant, or any failed read — **fails
  soft to the old pointer line for that skill alone**, with a
  WARNING in the server log naming the skill. An unverified
  ladder is never rendered.

Why this shape: LADDER-DATA §8 prescribes computing earned
state from data the panel already reads plus the static
verified table, with **no new mcMMO API surface** — and §7/G1
exist precisely because the live folder's file, not the jar's,
is what mcMMO runs. Rendering the embedded table unchecked
would silently go stale under exactly that drift; reading
live values unchecked would render whatever a drifted file
says as if it were the verified ladder. The verify-then-render
gate keeps both failure modes out: what renders is always
live-confirmed AND spec-verified. Graceful Roll (no
SubSkillType of its own) is verified through its parent perk
Roll, whose constant is checked like every other.

## D32 compliance

1. **All 19 skills render ladders**, including the child
   skills Salvage + Smelting; the child explainer lines stay
   ABOVE the ladder (rig-proven: "Perks:" sits below both
   explainer lines in the Salvage/Smelting readbacks). The
   pointer line is REPLACED for every skill — no skill keeps
   it on a verified ladder (harness + rig
   `panel-ladder-all-19-verified-live`).
2. **Flux Mining is OMITTED entirely** — it appears nowhere
   in the table, the code, or any rendered line, at any level
   (harness `fluxMiningIsAbsentEntirely`; rig Smelting
   readback + the all-19 check assert no "Flux" text).
3. **Each ladder row = perk name + unlock requirement +
   earned/rank state** at the player's level: "✓ Dodge —
   Lv 1 · Rank 1 of 1", "✗ Archery Limit Break — Lv 100 ·
   Rank 0 of 10", "✓ Roll — always available" for the
   numRanks = 0 rule. NO full perk descriptions in-panel.
   Earned state follows LADDER-DATA's rule exactly (highest
   rank whose Retro unlock ≤ the skill's level; the DERIVED
   level for child skills — the same `ExperienceAPI.getLevel`
   value the provider already reads; numRanks = 0 → always
   earned via the −1 sentinel).
4. **Both surfaces, same facts, same order:** Java detail
   (the detail item's lore = `detailLines()`) and the Bedrock
   form (content = the same lines joined) consume the one
   `detailLines()` source — no new UI machinery. The 0.1.1
   rendering rules hold: Bedrock form bodies pass through
   `PanelModel.bedrockSafe` (U+FF05) at the send boundary;
   Java/chat keep ASCII "%". Ladder lines contain no "%" by
   construction.

## Deliverables (this folder)

- `ExcelSkillsPanel/ExcelSkillsPanel-0.2.0.jar` — **53,816 B**
  — SHA-256 `f513eaf5992aea09c08763d1c653193b3d012f2a2f3b6824dccaa929be22bc45`
- `ExcelSkillsPanel/ExcelSkillsPanel-0.2.0-sources.zip` —
  49,799 B — SHA-256 `9f4395fdcb6f559815ef88ae46f6237fe804bb230ccde3108e4d206fddfb7a06`
  (src/ + tests/ + plugin.yml + local-build.sh, timestamps
  pinned 2026-11-13)
- `TEST-RESULTS.md` — 29/29 harness tests (21 carried from
  0.1.1, all passing unchanged, + 8 new ladder tests).
- `RIG-RESULTS.md` — desk rig against 0.2.0: Phase A 39/39
  PASS, Phase B 39/39 PASS, SEVERE 0 both, with composed
  detail readbacks for Acrobatics + Salvage + Smelting.
- `INSTALL-PACKET.md` — DRAFT gated jar-swap packet ON TOP of
  0.1.1 (NOT filed; G1 marked supervisor-to-confirm at
  filing; 0.2.0 installs only after 0.1.1 is live).
- `records-package/` — the staged records package (jar +
  sources + this record + SOURCE.md + INSTALL-PACKET.md +
  SHA256SUMS), mirroring the 0.1.1 package.

## Verification

- **Deterministic double-build: byte-identical** (both jars
  `f513eaf5…45` in one build run).
- **Clean-room rebuild: byte-identical** — the shipped
  sources zip extracted to a fresh directory and built with
  its own local-build.sh reproduces `f513eaf5…45` exactly
  (29/29 tests green inside it).
- **Fingerprint gate: PASS** — excel-build.txt + BuildInfo
  fragments carry EXC-SKILLSPANEL-0.2.0-WRV-20261113-01-e2f020
  in the shipped jar.
- **Rig:** see RIG-RESULTS.md.

## Diff vs 0.1.1 source

Confined to `core/PerkLadder.java` (new), `core/SkillMath.java`
(detail-tail row builders; the no-tail builders keep the
pointer), `provider/mcmmo/McMMoProvider.java` (the per-skill
live-verification ladder tail), `tests/…/PanelTest.java`
(+8 ladder tests), the rig driver/script (candidate 0.2.0 +
5 ladder checks + readbacks), and version knobs (plugin.yml,
BuildInfo, local-build.sh). Everything else is 0.1.1 verbatim.

## Honest limits

- No real Bedrock client on a desk rig: the ✓/✗/· glyphs'
  on-phone rendering in the Bedrock detail form is Mike's
  live-glance acceptance (the same class as 0.1.1's U+FF05
  percent sign). The Java chest renders the identical lines
  through Adventure components.
- The drift fail-soft is harness-proven against the pure
  gate; the rig exercises the matching-live path end-to-end
  (its mcMMO generates the fork-default skillranks, the same
  values G1 receipted live). No rig run edited a live file.
- Desk build only: NOT installed, NOT filed in the MANIFEST
  (per commission — the board supervisor verifies this
  return, re-bases the packet's G1 gates with a fresh live
  pull at filing time, and files; the failsafe installs).
