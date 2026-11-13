# ExcelSummons 0.1.7 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 12, 2026.
Commission of record: Mike, Nov 12, 2026, ~5:55 PM CST ("Yes, add
a Summons page to the compass menu"), with both same-evening
refinements — the adopted sub-page split (~6:01 PM CST) and the
confirm-gated per-pet adopted dismiss (~6:05 PM CST). Commission
file: `~/workspace/summons-menu-017-build/COMMISSION.md`. This
release supersedes the Nov 12 "adopted pets are not dismissable"
answer given to Jester: the confirm-gated menu flow below is now
the one adopted-dismiss door.

## Provenance

- Project tree: `~/workspace/summons-menu-017-build/build-0.1.7/ExcelSummons/`
  (plugin.yml version 0.1.7, main
  com.winkreyes.excelsummons.ExcelSummons, api-version '1.21').
- Base: the LIVE 0.1.6 tree `~/workspace/summons-016-build/ExcelSummons/`,
  copied (never modified) into this build. Its jar re-verified
  before copying: 66,209 B, SHA-256
  08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1
  — the exact live 0.1.6 candidate.
- The 0.1.6 policy core is byte-identical in 0.1.7:
  ReservePolicy, CapPolicy, AdminPolicy, LifetimePolicy,
  TargetRules, Nameplate, SummonRecord, SummonRegistry all hash
  equal to the 0.1.6 tree (verified at build close-out).
  config.yml is byte-identical to 0.1.6 (SHA-256 659d9e2c…c3a9)
  — no config change, no new setting.
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.1.6
  (house lib set + Paper API 26.3 + Magic 11.2.5 API).
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic is a hard depend (integration point only,
  never shaded); no new dependency of any kind. The Bedrock
  form pair (PlatformDetect + BedrockForms) is carried from
  ExcelTrainingDummy (house reflection pattern; package + log
  prefix only).

## What 0.1.7 changes vs 0.1.6 (the whole delta)

Design summary: DESIGN-0.1.7.md in this tree and in the source
zip. In short: (1) NEW `MenuModel` (pure) — the page split
(cast main page / adopted sub-page), list-identical rendering,
applied-hold rendering, pagination (45/page), the
dismiss-button/scope table, the result text (the command's
own), the adopted confirm texts; (2) NEW
`AdoptedDismissPolicy` (pure) — the adopted-dismiss gate
(record exists + owner match + ADOPTED origin) and the INFO
audit line format; (3) NEW `SummonsMenus` +
`SummonsMenuListener` (Bukkit mechanics, DummyMenus/
DummyListener pattern) — Java GUIs (main / adopted / confirm
views), click routing, dismiss reporting; (4) NEW
`PlatformDetect` + `BedrockForms` — Bedrock main form, adopted
form (body = every adopted line; buttons = per-pet dismiss +
Back), confirm form; a form failure falls back to the chat
list; (5) `ExcelSummons` registers the menu listener and adds
the `menu` subcommand (players only) + its name in the two
usage lines; (6) `SummonsListener` gains ONE new method,
`dismissOneAdopted` — the narrowly-scoped single-record path
(owner + entityId, AdoptedDismissPolicy-gated, despawn through
the existing funnel, INFO audit line); no existing method
changed. plugin.yml: version 0.1.7; usage + description name
the menu form. Nothing else moves: 0.1.6 mana/reserve behavior,
cast rates, caps, command dismiss scopes, messaging, and
config (byte-identical) are 0.1.6's. The command scopes still
never select an adopted pet — the confirm-gated menu flow is
the only adopted-dismiss door, and there is no
dismiss-all-adopted control anywhere.

Packet companion: the staged compass door
`menu-entry/summons.yml` in this package (ExcelMenu registry
93 → 94 at install; 1,862 B, SHA-256
2d9577ce2c184f2bea0f1e2bc670990ee76a4e0061b3be4c5f3603a6a30710cb).
No Magic config, no messages addition, no other plugin's
files.

## Artifacts

- ExcelSummons-0.1.7.jar — 90,538 bytes
  SHA-256 2fc1a226f11a25d4d54e5b11ce0f663a9bec1500f3ae79ff0f4cd60ed4475d56
  MD5 f320f621a9b0cc5cd1e5b5833a9ff729
  Build EXC-SUMMONS-0.1.7-WRV-20261112-01-7e4a17 (fingerprint
  gate PASS). Deterministic double-build byte-identical, AND a
  clean-room rebuild from the shipped source zip reproduced
  this hash exactly (three independent builds, 89/89 tests in
  each). NOT included in this records package (house rule for
  this package); the jar of record lives at
  `~/workspace/summons-menu-017-build/build-0.1.7/ExcelSummons-0.1.7.jar`
  and is live on the server (see Install record below).
- ExcelSummons-0.1.7-source.zip — 108,590 bytes
  SHA-256 091b63ab00860cb59b1507b8aff602767d990ad2f37a648874750f88069b59c1
  MD5 cb8ff6b0270fe49006ad841e856afd90
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.7.md + DESIGN-0.1.6.md + DESIGN-0.1.5.md +
  DESIGN-0.1.4.md + DESIGN-0.1.3.md; timestamps pinned
  2026-11-12.) The src/ and tests/ trees in this package were
  extracted from this shipped zip and verified byte-identical
  to the build tree at staging.

## Verification summary

SummonsTest 89/89 (78 carried — 2 version re-pins, named in
TEST-RESULTS.md — + 11 new 0.1.7 tests on the pure menu layer:
cast-only main page with list-identical lines, adopted
sub-page indicators, the 0.1.6 clamp rendered at the menu
layer, Jester's all-adopted fixture at held 0%, pagination
45/page on both pages, complete Bedrock bodies, dismiss-scope
routing equal to the commands, the confirm text naming the
pet and warning permanent removal, the narrow audited
adopted-dismiss gate, command scopes still never selecting
adopted, and the empty-registry render). Desk rig on Paper
26.3-235 + Magic 11.2.5 (rig-0.1.7 in the build folder):
22/22 behavior checks (RIGOBS), SEVERE 0, clean boot +
shutdown — command-path GUI open, GUI/list agreement, adopted
split + indicators, the confirm flow (unconfirmed removes
nothing; confirmed removes exactly one; audit line in the
server log), GUI-vs-command A/B on all three dismiss scopes,
pagination 45+5, Bedrock send path exercised with the client
proof explicitly OWED (RIG-RESULTS.md).

## Install record

INSTALLED Nov 12, 2026 — parent hand window (Mike-directed
push, 18:18–18:19 CST directive; window 18:20–18:37 CST),
chained with ExcelWandBars 0.3.17, PC lane (no fallback).
Boot at 18:32 CST: Done (21.245s), 83 plugins, guild LOCKED
4 of 5, SEVERE 0, registry holds 272 summons;
`/excelmenu reload` → 94 entries, 0 failed. Window backup:
"WB 0.3.17 + Summons 0.1.7 pre 1112"; cap-at-6 prune:
"WandBars 0.3.15 pre-install 1112" (oldest manual; total
stays 6). Predecessor ExcelSummons-0.1.6.jar held in place as
ExcelSummons-0.1.6.jar.bak-20261112. Full record: INSTALLED.txt
+ OUTCOME.txt in this package. Proofs owed: Mike's Bedrock
compass proof (opens the compass Summons page on Bedrock) and
Jester using the page instead of `/summons list`.
