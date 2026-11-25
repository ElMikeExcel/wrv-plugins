# RECEIPTS — ExcelEMFixes 0.18.0 packet

## Live-premise receipts (G1 — fresh pull, same day)

Same pull as the 0.17.0 packet (Nov 25, 2026, 11:30 AM CT,
via ssh-pc.sh scp from Homebase):

- Live jar: D:/muse-servers/wrv/plugins/ExcelEMFixes-0.16.0.jar
  -> ~/workspace/guidance-builds-recovery-20261125/receipts/live-ExcelEMFixes-0.16.0.jar
  506,738 B / SHA-256 ae6b53a6a76c3a208797242c9474dc14951868f2213c6a2432e889af4d734d54
- Live config: D:/muse-servers/wrv/plugins/ExcelEMFixes/config.yml
  -> ~/workspace/guidance-builds-recovery-20261125/receipts/live-config.yml
  72,329 B / SHA-256 ded17ff994b031ab524ce81a5730580b9f6fd82796e69f2bc8559bc84752fcb3

This packet's own gate is the 0.17.0 post-install state
(0.17.0 jar 510,196 B / 599a978c...; 0.17.0 deploy config
73,626 B / 3980a614...) — see packet-0.17.0/INSTALL-PACKET.md.

## Build provenance

- Tree: ~/workspace/guidance-builds-recovery-20261125/build-root
  (the verified 0.17.0 tree + the signposting diff:
  QuestChainCore.unlockedBy + markerPerms +
  formatUnlockLine, EmQuestChainData.questFile bridge,
  QuestChainClarityModule QuestCompleteEvent listener,
  config + README, version bumps, +14 core checks).
- Suites: 1,167 passed / 0 failed.
- Deterministic double-build: identical, 511,858 B /
  dda8d7a3ffca9ae78befe257a201fabca9b40a71b3eef38336abbfeef5f001c6.

## Rig proof (reference rig, full Primis content, Nov 25, 2026)

- Boot: "ExcelEMFixes 0.18.0 ready", 22 modules, SEVERE 0;
  "Quest Chain Clarity census — 112 quests indexed".
- ON case: a bot carrying The Groves Call completed it
  through EliteMobs' own completion flow (dialog objective
  completed by clicking King Nemian; /em quest complete).
  EliteMobs' completion + reward lines arrived, then the
  module's lines (bot chat log, Nov 25 17:29:54 UTC):
  "New quest unlocked: 'Population control!' — King
  Nemian gives it." and "New quest unlocked:
  'Acrobatics' — Posuere Hominem at the course end gives
  it." Server debug: "ChainBot completed
  primis_pontis_mille_the_groves_call.yml -> unlocked
  [Population control!, Acrobatics]".
- OFF case (A/B control): signposting-enabled: false +
  emfixes reload, then the same bot completed Feeding
  the Sick through the same flow — completion + rewards
  arrived and NO unlock line was sent (the unlock-line
  count in the bot's full chat log stayed at the ON
  case's 2).
