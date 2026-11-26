# RECEIPTS — ExcelEMFixes 0.21.0 packet

## Live-premise gate source (G1)

This build lane ran no live pull (build scope: no
panel, no live install). The gate values in
INSTALL-PACKET.md are the 0.20.0 packet's
post-install state, recorded same-day Nov 25, 2026:

- Installed record: ~/workspace/excelemfixes-caster-
  xp-toggle-20261125/packet-0.20.0/INSTALLED.txt
  (installed Nov 25, 2026 ~17:57 CT, wrv-install-
  failsafe window wrv-install-failsafe-20261125-1752,
  Install 1 of 1).
- Candidate placed live: ExcelEMFixes-0.20.0.jar
  543,319 B / SHA-256
  a4999341b2238d2f26a827a411e0e2d19cdba24ee29daa64e21cc5b685697cd2
- Deploy config placed live: 80,048 B / SHA-256
  40f3ed445c43ec3dff32bbc51677fe1144d7ea580d8f39075242df53adf8839f

The installer MUST re-pull the live jar + config
fresh at install time and STOP on any mismatch with
the gate above; these recorded values never satisfy
the gate by themselves.

## Build provenance

- Tree: ~/workspace/excelemfixes-class-xp-sharing-
  20261125/build-root (copy of the 0.20.0 build tree
  placed by the parent seat under Mike's Nov 25,
  2026 approval; the casterxp package generalized,
  one embedded pair table added).
- Candidate: ExcelEMFixes-0.21.0.jar — 552,041 B /
  SHA-256 4ecf2709ee41f08b2517184621c2b349ec2dbb
  21aaca5834d3b36e407fd5700a (hash string wrapped;
  see SHA256SUMS for the unwrapped value). Two
  consecutive builds byte-identical (deterministic),
  re-verified after the final source change.
- Deploy config: 81,482 B / SHA-256
  949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f
  Built from the live 0.20.0 config (reference/
  deploy-config-0.20.0.yml). Diff vs that config:
  62 lines added, 36 removed, ALL inside the
  caster-xp-sharing section — comments rewritten
  (reason updated: D242 + audit universality +
  Marvin's Paladin case + D243) and the two
  player-facing message strings; every functional
  key/value unchanged (diff-verified at filing).
- Suites: baseline 0.20.0 tree 1,259 passed / 0
  failed. Final tree: TOTAL 1,843 passed, 0 failed
  on clean runs (+584 new/changed checks: all-76-
  form pair resolution vs the audit catalog, the
  Marvin-shaped Paladin catch-up case, per-tree
  independence, 0.20.0 -> 0.21.0 migration, legacy
  mirror in both pair orders). ONE intermittent
  failure can appear on either tree:
  "Jester-shaped: armor named first on the tie"
  (SkillNudgeMathTest). Root cause, verified by
  probe in the 0.20.0 lane: the check iterates a
  Map.of whose iteration order is salted per JVM
  launch, and ties go to the first-iterated skill.
  Pre-existing, unrelated to this module, and
  carried in prior packets' notes.

## Rig proof (local test rig only, Nov 25–26, 2026)

Rig: the lane's rig/ (Paper 26.1.2-74, EliteMobs
10.9.14 — the live internal version), bots staged
via /em skill set + /em class test set; all XP
reads are EliteMobs' own player_data.db. Test boss:
custombosses/classxp_test_boss.yml (zombie, level
12, no AI, zero damage). NOTE: EM pays a native
~+61 ARMOR on every elite kill regardless of weapon
(measured in STOCK with sword, axe, and paired
weapons); the assertions below account for it.
Full narrative: the lane's BUILD-RECORD.md.

- BOOT: "ExcelEMFixes 0.21.0 ready", all 23 modules
  incl. "Class XP Sharing ready — per-player
  foundation-skill sharing toggle for every
  EliteMobs class form (default mode STOCK, catch-up
  on, N stored player record(s))", SEVERE 0;
  pre-placed 0.20.0-format state rows load clean.
- PALADIN (ARMOR+SWORDS), staged 17,210 / 51,711
  then played: STOCK sword kill +185 SWORDS / +61
  ARMOR (native only; repeated at the end of the
  leg, same values). Catch-up on /classxp shared:
  ARMOR 17,394 -> 52,083 = SWORDS total exactly
  (grant 34,689; per-tree audit record; player
  message correct); re-toggle fires nothing.
  SHARED kill: +123 / +123, combined 246 = native
  combined award (XP-neutral). CARRY kill: +246 /
  +246, gap constant at 0.
- BERSERKER (AXES+MACES), staged AXES 37,023 (Lv 12)
  / MACES 849 (Lv 5): catch-up on /classxp carry:
  MACES 849 -> 37,023 exactly (grant 36,174;
  berserker tree record). CARRY axe kill: +185 /
  +185, gap 0. SHARED: +93 / +92 = 185 (XP-neutral).
  STOCK: AXES +185, MACES +0 exactly.
- SPELLCASTER OWED (pre-placed 0.20.0 row: mode
  STOCK, catch-up-done false; staged WANDS 198,972
  / STAVES 37,023): /classxp shared fires the owed
  catch-up once: STAVES -> 198,972 (grant 161,949;
  spellcaster record + legacy mirror written).
  Re-toggle to CARRY: no refire, totals unchanged;
  the re-saved legacy mirror carries wands-before
  198,972 / staves-before 37,023.
- SPELLCASTER FIRED / MIGRATION (pre-placed 0.20.0
  row: mode SHARED, catch-up-done true, amount
  162,150; staged fresh WANDS 198,972 / STAVES
  37,023): /classxp carry sets the mode, fires
  NOTHING — STAVES stays 37,023, zero catch-up
  messages; the saved row preserves the original
  162,150 record in the per-tree map and the legacy
  fields.
- MENU: /skillmenu -> Swords page (non-caster)
  carries the "Class XP Sharing" row -> module view
  (3 mode buttons, current mode marked [ON]) ->
  clicking "Shared pool" sets SHARED (chat
  confirmation + state file). Bedrock parity rides
  the same Views/MenuRenderer surface as all
  modules since 0.15.0.

VERDICT: PASS on every leg.
