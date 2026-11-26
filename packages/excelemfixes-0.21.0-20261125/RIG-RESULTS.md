# excelemfixes-0.21.0-20261125 — RIG RESULTS

Rig proof (local rig, Nov 25–26, 2026). Full detail: BUILD-RECORD.md in
this package. VERDICT: PASS on every leg.

- Rig: Paper 26.1.2-74 + EliteMobs 10.9.14 (the live internal version)
  + the built 0.21.0 jar (552,041 B / 4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a,
  deterministic — two consecutive builds byte-identical); mineflayer
  bots; all XP reads from EM's own player_data.db after its flush.
- PALADIN (pair ARMOR+SWORDS), staged ARMOR Lv 10 / SWORDS Lv 13: STOCK
  sword kill +185 SWORDS / +61 ARMOR (native only, twice). /classxp
  shared fired the catch-up: ARMOR 17,394 -> 52,083 = SWORDS total
  exactly (grant 34,689; audit written); re-toggle fired nothing.
  SHARED kill +123/+123 (XP-neutral). CARRY kill +246/+246, gap 0. PASS.
- BERSERKER (pair AXES+MACES), staged AXES Lv 12 (37,023) / MACES Lv 5
  (849): /classxp carry fired the catch-up: MACES 849 -> 37,023 exactly
  (grant 36,174; berserker tree record). CARRY axe kill +185/+185.
  SHARED +93/+92 = 185 native (XP-neutral). STOCK +185/+0 exactly. PASS.
- SPELLCASTER REGRESSION — owed (pre-placed 0.20.0-format row, mode
  STOCK, catch-up-done false; WANDS 198,972 / STAVES 37,023): /classxp
  shared fired the owed catch-up exactly once: STAVES -> 198,972
  (grant 161,949; spellcaster record + legacy mirror written).
  Re-toggle to CARRY: no refire. PASS.
- SPELLCASTER REGRESSION — fired/migration (pre-placed 0.20.0-format
  row, mode SHARED, catch-up-done true, amount 162,150): /classxp carry
  set the mode and fired NOTHING — STAVES stayed 37,023; the saved row
  preserves the original record in both the per-tree map and the legacy
  fields. PASS — a 0.20.0-fired catch-up does not refire under 0.21.0.
- MENU: /skillmenu -> Swords page (a non-caster page) carries the
  "Class XP Sharing" row -> module view opens (three mode buttons,
  current mode marked) -> clicking "Shared pool" set the mode. PASS.
  Bedrock parity rides the same Views/MenuRenderer surface as every
  module since 0.15.0.
- BOOT: "ExcelEMFixes 0.21.0 ready" with all 23 modules incl. Class XP
  Sharing; SEVERE 0; pre-placed state rows loaded.

Unit suites: 1,843 passed / 0 failed on clean runs (0.20.0 baseline
1,259; +584 new/changed checks, incl. pair resolution for all 76
catalog forms and the Marvin-shaped Paladin case). One intermittent
single failure can appear — the documented pre-existing skill-nudge
tie-break flake, reproduced on the pristine 0.20.0 tree as well.

Live acceptance (install window wrv-install-failsafe-20261125-1914):
boot Done (33.650s) 19:19:16 CT, "ExcelEMFixes 0.21.0 ready" with 23
modules, emfixes reload ZERO reload-failed, SEVERE 0, guild OPEN 5 of
5, TPS 19.8. Confirmatory live proof owed: Marvin's first non-STOCK
toggle + Paladin catch-up verification in EM player data.
