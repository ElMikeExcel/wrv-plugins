# RECEIPTS — ExcelEMFixes 0.22.0 (quest-xp fold, D244)

## Artifact

- `ExcelEMFixes-0.22.0.jar` — 596,100 B — sha256
  `49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04`
- Determinism: built twice back-to-back; both builds produced the
  identical sha256 above.
- Jar contents verified: all 11 `excelemfixes/questxp/` classes plus
  the bundled `quest-xp.yml` (9,437 B) and `backpay.yml` (2,817 B)
  resources at the jar root.
- Bundled data files are byte-identical to the standalone 1.1.0
  sources: quest-xp.yml sha256
  `821e7103c839c3c1bcb790f19555d347af625d53e88bcebb0c3098eddfec2faa`,
  backpay.yml sha256
  `b7e544b3411160b92a73c94f593f915073ee15f274c92c3140e46b8ba844e3e3`.

## Deploy config

- `deploy-config.yml` — 84,251 B — sha256
  `e8b670092ca423e0fd0e5ae7d555ea55729de89dfb3efe592b04de96bbe17086`
- Pure delta vs the live 0.21.0 config (81,482 B, sha256
  `949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f`):
  diff = 57 added lines, 0 removed — exactly the
  `modules.quest-xp` section (enabled: true + the REASON comment
  block). Both files parse; 24 module sections; quest-xp enabled.

## Live-premise anchors (verified from the in-folder 0.21.0 copies)

- 0.21.0 jar: 552,041 B, sha256
  `4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a` ✓
- 0.21.0 deploy config: 81,482 B, sha256
  `949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f` ✓

## Unit suite

- Full suite on the 0.22.0 tree: **1,908 passed / 0 failed**
  (0.21.0 baseline 1,843 + 65 new quest-xp checks in
  `QuestXpModuleTest`: bundled table 112 quests / 130,355 XP, all
  lookup forms + configured zeros, Armor floor(xp/3), highest/pinned
  resolution, session claim ledger, backpay table + counter guard,
  paid-ledger save/load exactly-once + corrupt-ledger throws,
  target policy incl. Armor refusal, target store round-trip +
  non-weapon pin skip, popup view model shape/order).
- The documented SkillNudgeMathTest tie flake did not appear.

## Rig (Paper 26.3 + EliteMobs 10.9.10 jar / internal 10.9.14 — the
live versions; NMS fake players, real CustomQuest objects, real
QuestCompleteEvents, all reads from EM's own PlayerData / player_data.db)

The packet's own steps were performed on the rig server first:
standalone jar removed, 0.22.0 jar in, the five state files copied
`plugins/ExcelEMQuestXP/` → `plugins/ExcelEMFixes/quest-xp/` with
per-file sha256 MATCH on all five, deploy config in place.

- **Phase A — 33 PASS / 0 FAIL.** Module enabled (24 modules);
  boot ready line: table 112 / 130,355, backpay already paid: 3
  (the rig fixture ledger); console backpay for both already-paid
  players paid 0 (NO DOUBLE PAY — totals and BACKPAY audit lines
  unchanged); fresh player: main_quest_1 paid exactly 20 to highest
  (Swords) + 6 Armor, audit tag `target=highest`; pin via
  `/questxp target wands` as a non-op player persisted; invalid
  skill refused, pin unchanged; main_quest_2 paid 20 into the pin
  (Wands) + 6 Armor, tag `target=pinned:WANDS`; owed-backpay player
  pinned first, then paid exactly 80 into Wands + 26 Armor with a
  BACKPAY audit line, ledger write-through verified on disk, and a
  re-run paid 0 (exactly once); `/questxp menu` + bare `/questxp`
  dispatched true (informational leg).
- **Phase B — 12 PASS / 0 FAIL** (restart). Totals and the Wands pin
  survived the restart; main_quest_3 (480) still routed to the pin
  (+160 Armor); `/questxp target highest` cleared the pin; the
  welcome quest (50) resumed HIGHEST routing (+16 Armor).
- **Phase C — 6 PASS / 0 FAIL** (toggle). With
  `modules.quest-xp.enabled: false`: module absent, `/questxp` does
  not exist (dispatch false), a real fired completion event granted
  nothing, and the other 23 modules enabled normally.
- **Boot SEVERE: 0** in all three phases. (Offline-rig noise only:
  Paper's version fetcher ERRORs — no network — present in every
  offline rig boot, unrelated to the pack.)

Rig honesty notes: phase A's first attempt ran 28/5 — all 5 failures
traced to a fixture accident (a stray phase-B boot, caused by the
reference server's leftover phase marker, granted the fresh player a
quest before phase A began); the fixture was reset (EM rows for the
two fresh players deleted, migration copy redone) and the clean
re-run passed 33/0. Phase B's first re-run stalled after data-load
with no driver output (that boot also carried an EliteMobs init
race); the instrumented re-run passed 12/0. Phase C's first two
attempts used quests the fresh player had already completed or was
chain-gated from (EM returns no quest object — the event never
fired); the final run fired a real completion (Adventurer's Guild
training quest) and passed 6/0.
