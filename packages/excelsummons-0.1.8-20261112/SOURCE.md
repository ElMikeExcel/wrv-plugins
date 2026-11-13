# ExcelSummons 0.1.8 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 12, 2026.
Commission of record: Mike, Nov 12, 2026, 6:51–6:52 PM CST — his
live screenshot showed the 0.1.7 Bedrock form rendering with dead
buttons, and he asked for out counts on the page. Commission
file: `~/workspace/summons-menu-017-build/COMMISSION-018.md`.
Design: DESIGN-0.1.8.md in this package and in the sources zip.

## Provenance

- Project tree: `~/workspace/summons-menu-017-build/build-0.1.8/ExcelSummons/`
  (plugin.yml version 0.1.8, main
  com.winkreyes.excelsummons.ExcelSummons, api-version '1.21').
- Base: ExcelSummons 0.1.7 exactly as installed live (jar
  90,538 B, SHA-256
  2fc1a226f11a25d4d54e5b11ce0f663a9bec1500f3ae79ff0f4cd60ed4475d56).
  Sources differ from 0.1.7 in exactly four files:
  BedrockForms.java (rewritten), MenuModel.java (counts),
  SummonsMenus.java (info-book lore only), BuildInfo.java +
  plugin.yml (version). config.yml is byte-identical to 0.1.7
  (SHA-256 659d9e2c643189c9f65e243c1eb8517c7f5d4775a7cad5ed9cb2502662f3c3a9)
  — no config change, no new setting.
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.1.7.
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic is a hard depend (integration point only,
  never shaded); no new dependency of any kind. The Bedrock
  form pair (PlatformDetect + BedrockForms) follows the house
  reflection pattern; 0.1.8's BedrockForms mirrors the
  live-proven ExcelWandBars implementation shape.

## What 0.1.8 changes vs 0.1.7 (the whole delta)

Design summary: DESIGN-0.1.8.md in this package and in the
sources zip. In short: (1) Bedrock buttons FIXED —
BedrockForms now mirrors the live-proven ExcelWandBars
implementation: Geyser class loader ONLY (the Floodgate-first
attempt against the OLD Cumulus is removed), NEW Cumulus
(`org.geysermc.cumulus.form.SimpleForm` via
`GeyserApi.connectionByUuid`), response handling via
`FormBuilder.resultHandler(BiConsumer)` unwrapping
`ValidFormResponseResult.response()` →
`SimpleFormResponse.clickedButtonId()`, send via the
connection's assignable 1-arg sendForm hunt. Handler attach
failure fails the build loudly; unparseable responses /
missing connections / failed sends log at WARNING — the
0.1.7 silent-swallow failure mode is gone. The send API
(`sendSimpleForm(...) → boolean`, false → chat fallback) is
unchanged, as are all three SummonsMenus form flows.
WandBars' FormContrast label normalization was NOT imported
(Summons labels are plain already). (2) OUT-NOW COUNTS —
MenuModel derives `OutCounts` from the page's CAST entries
via `SummonRecord.baseKey`: mount → mounts out; wolf/cat/
familiar → cast pets out; all other cast keys → combat
summons out; total = cast list size. Adopted pets are never
counted. `Page.outNowLine()` renders
`Out now: M mount(s) · P cast pet(s) · C combat summon(s)
(T total)` (all three groups always print) or
`Nothing out right now.`; it sits in the Bedrock main body
after the registered line and leads the Java info book's
lore (name still "Mana held: H% of your pool").

Unchanged from 0.1.7, by design and by test: adopted pets
hold 0% mana (Mike's Nov 12 ruling), caps, percent table,
dismiss scopes, the confirm-gated per-pet adopted dismiss
as the only adopted-dismiss door, `/summons list` output,
persistence, config (byte-identical). NO ExcelMenu entry
change — the Summons compass door is 0.1.7's and is already
live (registry stays 94); this package therefore carries no
menu-entry file (see STAGING-REPORT.md).

## Artifacts

- ExcelSummons-0.1.8.jar — 92,387 bytes
  SHA-256 835419aaeff434b61e6be2f5cda1340233299e11262feebf96206cdd00bb8841
  MD5 e65d3751e7034964eb8af2804a562042
  Build EXC-SUMMONS-0.1.8-WRV-20261112-01-f8d6ba (fingerprint
  gate PASS). Deterministic double-build byte-identical, AND
  a clean-room rebuild from the shipped sources zip
  reproduced this hash exactly (three independent builds,
  94/94 tests in each). NOT included in this records package
  (house rule for this package); the jar of record lives at
  `~/workspace/summons-menu-017-build/build-0.1.8/ExcelSummons-0.1.8.jar`
  and is live on the server (see Install record below).
- ExcelSummons-0.1.8-sources.zip — 117,441 bytes
  SHA-256 6c6e2943bdb35f2fd7ce17adc0eb5bb50d1f7ad4f65437442beaa77eb9029974
  MD5 73682864fff37e52991472d04fed4f37
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.8.md + DESIGN-0.1.7.md + DESIGN-0.1.6.md +
  DESIGN-0.1.5.md + DESIGN-0.1.4.md + DESIGN-0.1.3.md;
  timestamps pinned 2026-11-12.) Note the build ships this
  artifact as "-sources.zip" (0.1.7 shipped "-source.zip");
  the package carries it under its exact build name. The src/
  and tests/ trees in this package were copied from the build
  tree and verified byte-identical to this shipped zip at
  staging (STAGING-REPORT.md).

## Verification summary

SummonsTest 94/94 (89 carried from 0.1.7 — 2 version re-pins,
named in TEST-RESULTS.md — + 5 new 0.1.8 tests: counts
derivation incl. tiered folding and adopted exclusion, zero
cases, the line's format and placement, the BedrockForms
handler shape pin (new Cumulus in; old Cumulus /
validResultHandler / Floodgate out), and the behavioral
handler test against the real Geyser jar's Cumulus classes —
every button of all three form shapes, both dispatch paths,
closed → −1). Desk rig on Paper 26.3 + Magic 11.2.5
(rig-0.1.8 in the build folder), two phases: phase A (no
Geyser/Floodgate) 24/24 PASS, SEVERE 0 — all 22 of 0.1.7's
checks carried + the 2 counts checks; phase B (+ Geyser-Spigot
2.12.1-b1292 + Floodgate b141, the live mix) 30/30 PASS,
SEVERE 0 — synthetic form responses driven through the
attached handler for every button of all three forms on both
dispatch paths (RIG-RESULTS.md). A real Bedrock client's tap
remains Mike's post-install live proof, stated plainly in
the packet's Acceptance.

## Install record

INSTALLED Nov 12, 2026 — wrv-install-failsafe window
19:16–19:43 CST (manual fire), PC lane, no fallback. Boot at
19:32 CST: Done (21.596s), 83 plugins, guild LOCKED 4 of 5,
SEVERE 0, registry holds 272 summons. Window backup:
"Summons 0.1.8 pre-install 1112"; cap-at-6 prune:
"WandBars 0.3.16 pre-install 1112" (oldest manual; total
stays 6). Predecessor ExcelSummons-0.1.7.jar held in place as
ExcelSummons-0.1.7.jar.bak-20261112. Full record:
INSTALLED.txt + OUTCOME.txt in this package. Proof owed:
Mike's live Bedrock tap test (Adopted pets sub-page opens,
confirm form, Keep/Back, out-counts line on the main page).
