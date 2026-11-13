# INSTALL PACKET — ExcelMenu 3.0.1 (Bedrock Server Menu section colours)

**Status: READY TO FILE — desk build complete, not filed. The board
supervisor files the MANIFEST entry and the failsafe owns the install.**
Build record: `~/workspace/menu-colors-20261113/BUILD-RECORD.md`.

**Item:** ExcelMenu 3.0.0 → 3.0.1 (Menu Programme / backlog Item 11:
Bedrock Server Menu section colours — Mike's direct Nov 13, 2026,
~12:27 PM CST, palette approved ~12:38 PM CST).

**Candidate jar:** `~/workspace/menu-colors-20261113/packet/ExcelMenu-3.0.1.jar`
— 330,214 B / SHA-256
`5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939`
(double-build byte-identical; suite OK 169 tests).
**Live filename at swap:** `plugins/ExcelMenu-2.0.0.jar` (carries 3.0.0
content) — the 3.0.1 jar is placed under the SAME live filename; the
outgoing jar is preserved in place as
`ExcelMenu-2.0.0.jar.bak-20261113-pre301`.

## Premise (supervisor re-verifies with fresh G1 pulls at filing; STOP on mismatch)

- Live jar is 3.0.0: 329,425 B /
  `306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51`
  (landed Nov 13, 2026, 08:03 CST; exactly one live ExcelMenu jar).
- Live `plugins/ExcelMenu/config.yml` is the 3.0.0-placed file:
  2,564 B /
  `910c2f834215ded57a4f24576f06eba6f8f5fb8017227f63106fba2d5f31c3be`.
  The hashes above are desk-side records from the 3.0.0 install;
  the installing window re-pulls fresh and STOPs on any mismatch.
- No layout or entries file changes in this packet: the live
  GROUPED `layouts/server_menu.yml` (7,182 B / `9af8ddef…`,
  pins `[spawn, tpmenu]`) and the 93-entry census are untouched.

## Scope — ONE window, ONE backup, ONE restart

1. Jar swap (above) under the same live filename.
2. Place `packet/filing-data/config.yml` — 3,215 B /
   `c0467b5f04a2c77424eab776e02b3076caa4d2f20be62eff3aa949692050eef7`
   (the live file with ONLY the `group-colors` palette changed to
   Travel `&a` / Me `&d` / Shops `&6` / Help `&c` / Settings `&e` /
   Chest Organization `&8`, plus the new `pin-color: "&1"` key)
   → `plugins/ExcelMenu/config.yml`. In-place .bak of the outgoing
   file first (`config.yml.bak-20261113-pre301`).

The staged config is byte-identical to the rig-proven seed-301
config (hash above re-verified supervisor-side at filing). Any
extra hunk in the diff = stop.

## Install steps / gates

Marker discipline + re-checks; BACKUP FIRST (one backup for the
window) + cap-at-6 (name prunees; never the Daily's newest or the
two newest manuals); emptiness `list` 0/10 twice; fresh-pull live
gates STOP on mismatch; place jar + config hash-verified pull-back;
ONE restart. Filing timing: avoid the Sat Nov 14 Warden-attempt
evening (~8:30 PM, flexible 9–10 PM); weekend protocol governs a
weekend landing (empty → push; players on → countdown, push,
bring back).

**Boot verify:** "ExcelMenu 3.0.1 enabled" line with **93 entries /
2 registry menus / 13 definition menus**; bridge PRESENT; SEVERE 0;
TPS 20.0 after settle; placed files pull-back EXACT; guild LOCKED
4 of 5 unchanged. Console smoke: `excelmenu list` answers.

**Rollback:** restore the .bak jar + .bak config, restart, verify
the 3.0.0 enable line and 93/2/13.

## Same-pass surfaces (supervisor legs, sequenced after landing)

Guide (menu colours note if the Guide documents the picker);
website pair; What's New BOTH surfaces; tips checked; menus = this
packet. Records per the standing rules: G2 duplicate check + claim
line, Drive deposit, draft PR in wrv-plugins (stays OPEN DRAFT);
website PR merged + G3 deploy check.

## Player proofs owed after install (Mike)

Bedrock: open the Server Menu — Spawn pin one tap (still blue),
Teleport pin one tap (still blue); picker shows Travel green, Me
light purple, Shops gold, Help red, Settings yellow, Chest
Organization dark gray; open Travel and Me forms and confirm the
entry buttons carry the section colour and Back returns to the
picker. Settings yellow + Chest Organization dark gray are the
build's picks — Mike vetoes by reply if he wants different colours.
