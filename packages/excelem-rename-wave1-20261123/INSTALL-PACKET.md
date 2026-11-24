# INSTALL PACKET - ExcelEM rename wave 1 (4 renames + SealAlerts companion) - Nov 23, 2026

Lint: 11611 B / max line 139 / non-ASCII 0 (counted before this line
was filled; the filled line keeps the same counts within a few bytes).
Status: FILED as a MANIFEST pointer stub (Nov 23, 2026).

Authority: D163 rename audit, COMPLETE Nov 20, 2026
(~/workspace/excelem-rename-audit-20261120/RENAME-PLAN.md), Wave 1 =
cheap standalones, one shared window; plus D211 (Nov 23, 2026): the
SealAlerts companion build rides this window so SealLog can rename in
wave 1. Together and RaidBuff are NOT in this wave (Mike declined
their renames Nov 22, 2026).

## ATOMICITY REQUIREMENT (read first)

Change 4 (ExcelSealLog -> ExcelEMSealLog rename) and change 5
(ExcelSealAlerts 0.1.2 -> 0.1.3 jar swap) are ONE atomic unit: they
land in the SAME stop/start cycle, both or neither. SealAlerts taps
SealLog's logger by plugin name; either half alone leaves the taps
empty SILENTLY (proven both directions on the rig, SEVERE 0, no
error - receipts D211, boots C2 and D2). A window that cannot land
both halves lands neither: swap all five back together.

## Scope

Five jar changes in one shared window. Changes 1-4 are name-only
renames (QuestXP precedent): plugin.yml name line + jar filename;
every other jar entry byte-identical to the live jar; commands,
permission-node strings and config keys unchanged; each data folder
copied old -> new and byte-verified; old jar removed in the same
swap; old data folder left in place as the window backup. Change 5
is a same-name jar swap (ExcelSealAlerts keeps its name): the only
functional change is its by-name reference to SealLog, moved to the
new name (D211); its data folder is NOT touched.

| # | Live jar | Candidate jar | Result |
|---|---|---|---|
| 1 | ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar | ExcelEMAbilityBedrockBridge-0.2.0.jar | plugin ExcelEMAbilityBedrockBridge |
| 2 | ExcelPartyDrops-0.1.0.jar | ExcelEMPartyDrops-0.1.0.jar | plugin ExcelEMPartyDrops |
| 3 | ExcelWitherSkulls-0.1.0.jar | ExcelEMWitherSkulls-0.1.0.jar | plugin ExcelEMWitherSkulls |
| 4 | ExcelSealLog-1.0.0.jar | ExcelEMSealLog-1.0.0.jar | plugin ExcelEMSealLog (stateless, no folder) |
| 5 | ExcelSealAlerts-0.1.2-runtime-diagnostic-OFFLINE.jar | ExcelSealAlerts-0.1.3.jar | plugin ExcelSealAlerts v0.1.3, taps the new name |

## Live-premise receipts (G1)

Receipts files: ~/workspace/rename-wave1-staging-20261123/packets/
RECEIPTS.md, SHA-256 abae3d3abeb8c75c33ae61898a89e08dc92a7c0ac6c44397d09c34f62b49a814
(changes 1-4: census, pulls, scans, rig results, surfaces greps) and
RECEIPTS-D211.md, SHA-256 0ff32b1fb89f65d1fc559e6ed4ed6cd865b47284c3fa65f07857ebb89104467f
(change 5 + fresh re-verification of change 4). Fresh Homebase SSH
pulls Nov 23, 2026 (18:07 CT wave-1 census; 18:24 CT D211 census,
PC-side hashes cross-verified against pulled bytes). Claims carried:

- All five live jar identities (gate values below) - census + pulled
  bytes agree; changes 1-4 also match the audit's Nov 20 census and
  change 5 matches the staging census (no live drift anywhere).
- Data folders: changes 1-3 hold exactly one config.yml each (hashes
  in RECEIPTS.md); ExcelSealLog folder ABSENT live (stateless);
  ExcelSealAlerts folder present (6 files, config.yml 2,233 B /
  434ca631... carries the deployed webhook) and stays untouched.
- No other live plugin declares a depend/softdepend on changes 1-3
  (audit plugin-yml sweep); the one runtime coupling on change 4 is
  change 5 itself (D211), and no class in changes 1-4 holds its own
  name as a lookup string (bytecode scans in the receipts).

## Candidates

Builds in ~/workspace/rename-wave1-staging-20261123/builds/
(SHA256SUMS.txt). Changes 1-4: jar-level renames of the hash-verified
live jars - the plugin.yml name line is the only changed entry; a
build script asserted per-entry byte-identity for all other entries
and a one-line diff for plugin.yml. Change 5: compiled from the
on-hand source (~/workspace/sealalerts-fix/source-0.1.2, the live
build's provenance snapshot) with the SealLog name reference moved
at all three functional sites (TAPPED list, plugin.yml softdepend,
SealParser dispatch key - see RECEIPTS-D211 for why the third site
is part of the same one logical change) + version bump to 0.1.3;
deterministic build (two clean builds byte-identical, build.sh in
sealalerts-build/); parser dispatch harness 4/4 PASS.

- ExcelEMAbilityBedrockBridge-0.2.0.jar - 31,990 B /
  9c4158212c7443f49be5371784aab229855381cc1f8ba46359a7637b7a32a4c1
- ExcelEMPartyDrops-0.1.0.jar - 14,543 B /
  12f3352341b42a14a19977e0ee43a97eee13b1e79062c8e0e0345e33ed0f8df8
- ExcelEMWitherSkulls-0.1.0.jar - 14,862 B /
  9361dd79f9b0dd639d2b0256bfb866f57d3160ebe1e850e718d01a80e6abf8ed
- ExcelEMSealLog-1.0.0.jar - 8,344 B /
  efa5719c33993c4704bdfe20af47c8d6b5133010cfcb071cffb65b89741c2c92
- ExcelSealAlerts-0.1.3.jar - 43,153 B /
  26c2bb34b89b7f7baf48cde5c98b7bc260eb8da70249967aaa9f4346fc985159

Rig: local Paper 26.3 build 256 (the live build) + Temurin JRE 25 +
live EliteMobs jar, staged in ~/workspace/rename-wave1-staging-20261123/rig/.
Changes 1-3: boots A (no EM) + B (EM), SEVERE 0, statuses green,
migrated configs byte-identical (RECEIPTS.md). Pair proofs for
changes 4+5 (RECEIPTS-D211): boot D1 (SealAlerts 0.1.3 + ExcelEMSealLog)
READY taps=[ExcelEMSealLog], SEVERE 0; mismatch controls C2 (old
SealAlerts + renamed SealLog) and D2 (new SealAlerts + old SealLog)
both taps=[], silent - the atomicity evidence above.

## Live-jar gates (STOP on mismatch, re-pull fresh at window time)

- plugins/ExcelEliteMobsAbilityBedrockBridge-0.2.0.jar = 32,339 B /
  c28f95a1a893075bc046f622c59ca598c3f45422d1691fb9b073aef9ede74f8f
- plugins/ExcelPartyDrops-0.1.0.jar = 14,731 B /
  afae735b991d683cf45f4aa30187f51aaf53bf2b7b114761d4279979beba6196
- plugins/ExcelWitherSkulls-0.1.0.jar = 15,034 B /
  6797ce0dce6afcc451d486d1907b9d47171f5d83f1abce3063057bc243b56d9a
- plugins/ExcelSealLog-1.0.0.jar = 8,420 B /
  e75de2643cf0e5f1e34902574f2bc3732144356ae6609dae1c15b231b61eb0be
- plugins/ExcelSealAlerts-0.1.2-runtime-diagnostic-OFFLINE.jar =
  43,426 B / b3f1bb5470d3fcf79653b05b822b33b70e21d028f0d6a46750da853bf5cf8345
- Absence gates: plugins/ExcelSealLog/ and plugins/ExcelEMSealLog/
  folders both ABSENT; no ExcelEM*.jar from this wave present yet.
- Untouched gate: plugins/ExcelSealAlerts/config.yml stays 2,233 B /
  434ca6319fc1b7a9d08ceaea7eaefb871ee1efbc12429bc599c15f14e6952875
  before and after the window (folder never written by the swap).

## Window steps (one shared window, failsafe or Runner lane)

1. Marker discipline per D67 v2; BACKUP FIRST (one snapshot, cap-at-6,
   name prunees; never the Daily newest or the two newest manuals);
   emptiness list = 0/10 twice.
2. Stop the server. Verify all five live-jar gates + absence gates
   by fresh hash.
3. Changes 1-3, per plugin: copy plugins/<Old>/ to plugins/<New>/,
   verify byte-identity by SHA-256 of every file (one config.yml
   each; expected hashes in RECEIPTS.md). Swap jars: remove the old
   jar, place the candidate jar, verify candidate hash after
   placement.
4. Changes 4+5, as one atomic unit in this same stop/start cycle:
   remove ExcelSealLog-1.0.0.jar, place ExcelEMSealLog-1.0.0.jar;
   remove ExcelSealAlerts-0.1.2-runtime-diagnostic-OFFLINE.jar,
   place ExcelSealAlerts-0.1.3.jar; verify both candidate hashes
   after placement. Do NOT create any SealLog data folder. Do NOT
   touch plugins/ExcelSealAlerts/ (the folder).
5. Start. Boot verify (below). On any failure: restore the snapshot /
   swap all five back together (old jars + old folders untouched).

## Boot verify (expected lines, proven on the rig)

- "[ExcelEMAbilityBedrockBridge] Enabling ExcelEMAbilityBedrockBridge v0.2.0"
- "[ExcelEMPartyDrops] Enabling ExcelEMPartyDrops v0.1.0"
- "[ExcelEMWitherSkulls] Enabling ExcelEMWitherSkulls v0.1.0"
- "[SealLog] Enabling ExcelEMSealLog v1.0.0" + "[SealLog] READY version=1.0.0"
- "[SealAlerts] Enabling ExcelSealAlerts v0.1.3"
- "[SealAlerts] READY version=0.1.3 taps=[ExcelEMSealLog, ExcelGuildGate, ExcelBossTrial]"
  (all three tapped plugins exist live; set equality is the check,
  order may vary with enable order)
- Console "plugins" lists the four new names + ExcelSealAlerts;
  old names absent.
- "partydrops status" -> "ON, radius 50.0, step 0.5, multiplier cap 3.0,
  participant cap 5, EliteMobs hooked" (values from the migrated config).
- "witherskulls status" -> "top-up ON, target 25.0%, EliteMobs seam BOUND".
- "abilities status" -> "ExcelEliteMobsAbilityBedrockBridge status:
  enabled (v0.2.0)" + EliteMobs link line (ACS ready on live).
- "sealalerts status" -> "Watching: [ExcelEMSealLog, ExcelGuildGate,
  ExcelBossTrial]" and the version line reads 0.1.3.
- SEVERE 0 attributable; guild state untouched beyond the seal-watch
  feed itself. Marker: build folder INSTALLED.txt written by the window.

## Same-pass surfaces (ride this install)

- Website pair: plugin-list name swap in index.html + ai-knowledge.json
  (live mention counts in RECEIPTS.md: AbilityBridge 2/3, PartyDrops
  2/4, WitherSkulls 1/1, SealLog 1/1) + Pages deploy check (G3).
  SealAlerts keeps its name - no website change for change 5.
- What's New BOTH surfaces: one combined wave-1 rename entry (website +
  in-game changelog.yml, new id; the 2026-11-14 historical entry is not
  edited).
- GitHub record: wrv-plugins draft record PR for the four rename builds
  + the SealAlerts 0.1.3 companion build (G2 duplicate check first;
  stays an open draft).
- Menus: verified NONE for all four renamed plugins (live ExcelMenu +
  ExcelBedrockMenu grep, Nov 23). Tips / broadcaster: verified NONE.
- Guide: hibernated - no action, noted per the audit.

## Proof owed after install

- Console read-backs at the window (above), including sealalerts status.
- First SealAlerts server_started / heartbeat event after the window:
  plugins_ok = true with all three names watched - the live pair proof
  for changes 4+5 (the rig proves the pair shape; live proves the full
  three-name set). If plugins_ok reads false with a SealLog name in
  not_watching, treat as a failed atomic landing and roll back per
  step 5.
- /abilities status from console at the window; the Bedrock ability-form
  cast rides the existing Bedrock proof lane on next Bedrock play (no
  dedicated session). No player proof gates this packet.

## D211 resolution (why wave 1 files as 5 changes, not 3)

Staging held SealLog out of this packet: live ExcelSealAlerts taps
SealLog's logger by plugin NAME at runtime (TAPPED list + getPlugin
lookups, re-tapped on PluginEnableEvent), and the rig control pair
proved a renamed SealLog leaves taps=[] silently - a cosmetic rename
would have traded away the seal-watch feed invisibly. D211 chose the
companion build: ExcelSealAlerts 0.1.3 moves its by-name SealLog
reference (TAPPED entry, plugin.yml softdepend, and the SealParser
dispatch key - the third site is part of the same reference: the tap
hands the parser the plugin's Bukkit name, so a TAPPED-only fix
would attach the tap and still drop every SealLog fight line at the
parser; evidence + harness proof in RECEIPTS-D211). Wave 3 note for
the D163 plan: the same TAPPED list still names ExcelGuildGate and
ExcelBossTrial - when those rename, SealAlerts' TAPPED + parser keys
for them must move in that window the same way.

## Filing note

Filed into ~/workspace/wrv-install-queue/MANIFEST.md as a PENDING
pointer stub on Nov 23, 2026, citing this packet + RECEIPTS.md +
RECEIPTS-D211.md. Packet lint is counted at filing and recorded in
D211-FILING-RECORD.md (the header line above is filled from the same
count).
