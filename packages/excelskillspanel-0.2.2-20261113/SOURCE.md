# ExcelSkillsPanel 0.2.2 — SOURCE record

Excel — Wink Reyes Valley. Authors: Iris / Mike. November 13, 2026.
Commission of record: Menu Programme Items 12 + 13. Mike's report,
~12:38 PM CST Nov 13, 2026 — his live screenshots showed the 0.2.1
panel reading "Rank: Beginner / Next: Student / Mana 60 / regen 5 /
Total casts: 694" while his Renewal wand in hand read "Rank:
Apprentice, Credited casts 1,215, Next: Master — 785 to go".
Screenshots: `~/workspace/user/files/13990_12_9j9z.png` (the panel) +
`~/workspace/user/files/13991_13_bt17.png` (the wand). Diagnosis:
DIAGNOSIS.md in this package and in the build folder.

## Provenance

- Project tree: `~/workspace/skillspanel-022-build-20261113/ExcelSkillsPanel/`
  (plugin.yml version 0.2.2, main
  com.winkreyes.excelskillspanel.ExcelSkillsPanel, api-version '1.21').
- Base: ExcelSkillsPanel 0.2.1 exactly as installed live (jar
  57,652 B, SHA-256
  ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c,
  installed 03:54 CST Nov 13, 2026). The base gate rebuilt 0.2.1
  from its source byte-identical to the live jar before this build
  (BUILD-RECORD.md §3); base-gate JUnit 33/33.
- Built by `local-build.sh` (the house deterministic recipe:
  JUnit first, timestamp-pinned jar built twice and compared,
  fingerprint gate), against the same compile surface as 0.2.1.
  One compile fix was needed first (F0, BUILD-RECORD.md §3):
  `PanelTest` calls `MagicApiReads.wrvDisplayName(...)` /
  `wrvNextKey(...)`, which were package-private — 13 javac errors
  on the first rebuild attempt. Fix: both methods made `public`
  (pure functions; the tests pin their contract). No behaviour
  change.
- Ownership: all code is ours (Iris / Mike lineage, Excel
  signature). Magic, mcMMO, ExcelMagicLevel, ExcelMagicGate,
  ExcelGuildGate, PlaceholderAPI are softdepends (integration
  points only, never shaded); no new dependency of any kind.
  The Bedrock form pair (PlatformDetect + BedrockForms) follows
  the house reflection pattern.

## What 0.2.2 changes vs 0.2.1 (the whole delta)

- **Item 12:** no "Class: Mage" line on any surface — the field is
  gone from the DTO end-to-end (`MagicData`, `MagicApiReads`,
  provider), not merely nulled.
- **Item 13:** rank / next rank / mana / regen come from the SAME
  resolved CARRIED wand (held → cached-active-if-carried →
  offhand → inventory scan, highest path wins → bound wands →
  class path last, only when the player owns no wand). Next rank
  from the WRV order (Apprentice → Master, never Magic's
  "Wizard" — Magic's own chain name for the master path is
  "Wizard", but the WRV ladder and MagicGate's wand lore say
  Master). Mana displays as the effective pool (live current /
  effective max, regen effective); the Boosts line still carries
  the percentages. Casts come from MagicGate's ledger, labelled
  "Credited casts"; the panel NEVER falls back to
  ExcelMagicLevel's `totalCasts` again (if MagicGate cannot
  answer, the line is omitted).
- Source diff vs 0.2.1 (BUILD-RECORD.md §3): `BuildInfo`
  (version), `MagicLines` (no Class line; credited-casts label;
  effective-mana helpers), `MagicApiReads` (carried-wand
  resolution; WRV rank order; F0 visibility), `MagicGateReads`
  (new — MagicGate ledger + lore fallback), `MagicHeaderProvider`
  (effective-pool composition; credited casts),
  `MagicLevelStoreReads` (totalCasts removed), `PanelTest`
  (0.2.2 cases).

Unchanged from 0.2.1, by design and by test: Magic Level / XP
bar / total XP, Boosts line, seals, mcMMO rows, perk ladders,
Bedrock `%` handling, gold bars. No config of its own (the
plugin ships no config.yml and reads no plugin data files of
its own); it only READS ExcelMagicLevel's live `config.yml`
reward table for the regen/cost fractions. Pure jar swap:
outgoing `ExcelSkillsPanel-0.2.1.jar` → incoming
`ExcelSkillsPanel-0.2.2.jar`; plugin count unchanged (84 by the
house count — 87 boot entries minus the 3 offline/diagnostic
plugins).

## Artifacts

- ExcelSkillsPanel-0.2.2.jar — 62,142 bytes
  SHA-256 374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e
  MD5 9a9a920a68aed5e2fca0546d0be2a359
  Build EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9 (fingerprint
  gate PASS). Deterministic double-build byte-identical; the
  repro jar carries the identical hash. NOT included in this
  records package (house rule for this package); the jar of
  record lives at
  `~/workspace/skillspanel-022-build-20261113/ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar`
  and is live on the server (see Install record below).
- No sources zip was shipped by this build (unlike the
  ExcelSummons 0.1.7/0.1.8 builds). The src/ and tests/ trees
  in this package were copied from the build tree and verified
  byte-identical at staging (PACKAGE-NOTES.md); they are the
  source of record for this version.

## Verification summary

PanelTest JUnit **38/38 pass** (4 containers, 0 failed) in the
project build (TEST-RESULTS.md). Desk rig on Paper 26.3 +
Magic 11.2.5 (rig/ in the build folder), two phases: phase A
(no Geyser/Floodgate) **71/0 PASS**, phase B (+ live-line
Geyser-Spigot 2.12.1-b1292 + Floodgate) **71/0 PASS**
(RIG-RESULTS.md). The prior lane's 13:19 rig run stood at
64 pass / 7 fail; this lane resolved all 7 to 71/0 — the
"Wizard" expectation was already fixed in source and only
needed a rebuild from current source, the mana expectation
was corrected to the effective pool (154/154 at the rig's
level-136 fixture; at Mike's level 26 the effective pool IS
150/150, exactly the wand lore), and the stale-reference trap
in the driver was corrected (BUILD-RECORD.md §2). A real
player's `/skillpanel` proof remains Mike's post-install live
proof, stated plainly in the packet's Proof section.

## Install record

INSTALLED Nov 13, 2026 — wrv-install-failsafe window scheduled
15:03 CST (file-and-fire; supervisor run 309 filing), PC lane,
no fallback. Boot at 3:48:09 PM CST: Done (22.360s), 87 plugins,
guild LOCKED 4 of 5, SEVERE 0. Window backup:
"ExcelSkillsPanel-0.2.2 pre-install1113" (7.5 GB); cap-at-6
prune: "pre-ExcelMenu-3.0.0-20261113" (oldest manual; total
stays 6). Predecessor ExcelSkillsPanel-0.2.1.jar held in place
as ExcelSkillsPanel-0.2.1.jar.bak-20261113-pre022 (57,652 B /
ce430d1f…). Full record: INSTALLED.txt + OUTCOME.txt in this
package. Proof owed: Mike's live `/skillpanel` check while
holding food (wand NOT in hand), Java + Bedrock — Rank
Apprentice, Next rank Master, Mana 150 / 150, regen 25,
Credited casts 1,215, no Class line. No live two-wand check is
owed by this packet (rig guard only; Mike disputes carrying
two wands and the registry question is a separate supervisor
check).
