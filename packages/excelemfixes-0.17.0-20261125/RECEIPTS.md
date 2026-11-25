# RECEIPTS — ExcelEMFixes 0.17.0 packet

## Live-premise receipts (G1 — fresh pull, same day)

Pulled Nov 25, 2026, 11:30 AM CT (17:30 UTC) from Homebase
via ~/workspace/server-gui-20261121/work/ssh-pc.sh scp:

- Live jar: PC path D:/muse-servers/wrv/plugins/ExcelEMFixes-0.16.0.jar
  -> ~/workspace/guidance-builds-recovery-20261125/receipts/live-ExcelEMFixes-0.16.0.jar
  506,738 B / SHA-256 ae6b53a6a76c3a208797242c9474dc14951868f2213c6a2432e889af4d734d54
- Live config: PC path D:/muse-servers/wrv/plugins/ExcelEMFixes/config.yml
  -> ~/workspace/guidance-builds-recovery-20261125/receipts/live-config.yml
  72,329 B / SHA-256 ded17ff994b031ab524ce81a5730580b9f6fd82796e69f2bc8559bc84752fcb3

Note: an older chain record cited a 69,861 B live config;
the fresh pull decides — live is the 72,329 B file, which
matches the 0.16.0 packet's deploy-config hash exactly.

## Base gate

The pristine 0.16.0 source tree (carried at
~/workspace/guidance-builds-recovery-20261125/build-root-base0160)
rebuilt via its own build.sh reproduces the live jar
byte-for-byte: 506,738 B / ae6b53a6... (verified Nov 25,
2026 by the recovery lane before any edit).

## Build provenance

- Tree: ~/workspace/guidance-builds-recovery-20261125/build-root
  (base 0.16.0 + the R1 diff: 10 files — ExcelEMFixes.java,
  plugin.yml, config.yml, README.md, build.sh, questglow
  GlowState / QuestGlowCore / EmQuestGlowData /
  QuestGlowModule / QuestGlowTest).
- Suites: 1,153 passed / 0 failed.
- Deterministic double-build: identical, 510,196 B /
  599a978c0b2fd083554a030e36fbd75fc6d10bee1633cd955b7bb0ceed51d6e7.

## Rig proof (reference rig, full Primis content, Nov 25, 2026)

- Boot: "ExcelEMFixes 0.17.0 ready", 22 modules, SEVERE 0;
  census "73 quest-giver NPCs and 118 glow-capable NPCs"
  (0.16.0's universe was 76 — dialog targets joined).
- Baseline: a bot carrying nothing sees no glow on King
  Nemian's entity.
- The Jester case, staged with a real carried quest
  (GlowProbe PlayerData.addQuest of
  primis_pontis_mille_the_groves_call.yml; server log:
  "carries primis_pontis_mille_the_groves_call.yml
  objectives: [DialogObjective completed=false
  npc=primis_groves_nemus_leader.yml]"): the carrier's
  packet log shows Nemian's entity with the glow flag set
  and team qgobjective joined; server debug repeats
  "GlowBot <- primis_groves_nemus_leader.yml = OBJECTIVE".
- Non-carrier control: a second bot beside the first
  received no glow metadata for Nemian's entity.
- Toggle control: objective-enabled: false + emfixes
  reload -> the carrier's glow flag cleared
  (META flags 0x0) while the quest stayed carried.
