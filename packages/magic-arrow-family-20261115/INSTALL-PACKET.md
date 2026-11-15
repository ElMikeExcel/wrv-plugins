# INSTALL PACKET (DRAFT — supervisor files; install is parent-gated)

## PENDING — Magic spells.yml arrow family velocity fix (R8, Nov 14, 2026)
- Lint: 5625 B / max line 84 / non-ASCII 22
- Packet: ~/workspace/board-r360/r8-arrow-balance/build/INSTALL-PACKET.md
- Receipts: ~/workspace/board-r360/r8-arrow-balance/build/RECEIPTS.md
  (G1 pulls 2026-11-14 21:25–21:27 CST via Homebase, probes 3/3 PASS;
  hashes in RECEIPTS.md, cited there by path + hash per P2/P3.)

## What this fixes
Magic's ProjectileAction writes `arrow_damage` onto a real Bukkit
TippedArrow via setDamage — that is vanilla BASE damage, and vanilla
multiplies it by impact velocity (ceil(velocity x base) + crit bonus).
The arrow ladder fires at speed 3–5, so the physical arrow lands for
~3–5x its configured value: arrow|6 measured 41–43 HP PROJECTILE
incidents (Protect, Nov 9) and Jester's Nov 10 test read 42 direct /
16 indirect per cast — ~58 total vs the config's balanced 23.
Diagnosis of record: ~/workspace/spell-audit/
ARROW-DOUBLING-INVESTIGATION-20261109.md. Commissioned Nov 9, 2026
(Mike: "Yeah get it done") under sword > bow > magic.

## The change (config-only, no jar)
In each of the 5 destination spells.yml files, ONLY `arrow_damage:`
lines inside the real-arrow blocks change:
new = old / speed, rounded to 2 dp. Speeds, payloads (entity_damage /
player_damage), mana, cooldowns, spread, and every other spell are
byte-identical (diff-proved: 9/6/9/9/9 lines per file).
- Ladder (live/s3/s4/s5): 4->1.33, 6->1.71, 6->1.5, 6->1.33, 7->1.4,
  7->1.4 (arrow, |2..|6). Stage2 uses its own values: 5->1.67, 6->1.71,
  6->1.5, 7->1.56, 7->1.4, 7->1.4.
- Item-cost family: arrow_ice 8->2.67, arrow_shock 8->2.67,
  arrow_regular 9->3.0 (all @ speed 3).
- arrowrain NOT changed: speed 1 makes arrow_damage already true
  (5 = 5); the build asserts identity.
- Not exposed, not touched: arrow_light / missile family
  (CustomProjectile + flat Damage action — exact already).

## Post-fix model (script-verified, build/evidence/model-test.txt)
Physical arrow at point blank = ceil(speed x new) reproduces the
config's intended arrow part within 1 HP everywhere (ice/shock land
9 vs intent 8 from the ceil on 8.01 — noted, accepted).
arrow|6 per-cast = 7 physical + 16 payload = 23 (the shipped config's
own total), 20.0 DPS at its 1150 ms cooldown = Power V bow parity,
the Nov 8 audit's top-tier magic target. Pre-fix reality was ~58/cast
(44–60 DPS), the strongest single-target damage in the game.
Flight-distance drag lowers the physical part with range, exactly
like a real bow (bow parity by construction).

## Candidate
- Staged files: ~/workspace/board-r360/r8-arrow-balance/build/staged/
  (live / stage2 / stage3 / stage4 / stage5 spells.yml;
  SHA256SUMS in build/). Sizes + hashes in RECEIPTS.md.
- Tests: spell logic-test harness (Mike's Nov 2 gate) on staged live
  = findings IDENTICAL to base live (278; CRITICAL 0 / HIGH 6 /
  MEDIUM 83 / LOW 189) — mana/cooldown untouched, so sustainability
  at every mana tier is the config's existing profile by construction
  (arrow drain 26.7/s at base tier, sustainable only at pool 200 —
  pre-existing LOW, unchanged). Model test PASS (39 spell-file rows).
- Rig: local Paper rig + Magic 11.2.5 with staged live file —
  "Loaded 591 spells" (rig book count), Done (39.282s), SEVERE 0,
  zero arrow/spells warnings (build/evidence/rig-boot.log).

## Live-jar gate (STOP on mismatch)
Config-only packet; no jar. File gates instead: at install, fresh-pull
all 5 destinations; SHA-256 must equal the G1 receipts in RECEIPTS.md
(live 75877195…, s2 9be7eda6…, s3 682ab22f…, s4 75877195…,
s5 66ebb58c…). Any mismatch = STOP and re-base (a stage flip or
another spells packet may have moved the base — e.g. if the stage-5
flip lands first tonight, live must gate against the stage5 receipt).

## Gates / install discipline
Gated spells swap, full discipline: WINDOW-IN-PROGRESS marker with
re-checks before Shut Down and before Start; BACKUP FIRST + cap-at-6
(name prunees; never the Daily newest or two newest manuals);
emptiness list = 0/10 twice. In-place backups
spells.yml.bak-<date>-prearrowvel at all 5 destinations.
Destinations: live plugins/Magic/spells.yml + ExcelMagicGate
stage 2/3/4/5 template spells.yml, ONE boot (restart, per the
wave-2 / L2 damage-delta precedent). Rollback = restore the 5
.bak files + restart.

## Boot verify (by script output)
- grep boot log: "Loaded 167 spells" (live enabled count) present;
  SEVERE count attributable = 0; zero warnings naming arrow/spells.
- Post-boot fresh pulls of all 5 destinations hash EXACT to the
  staged candidate hashes in RECEIPTS.md / build/SHA256SUMS.
- guildgate LOCKED count unchanged from pre-window state.
Marker on install: ~/workspace/board-r360/r8-arrow-balance/build/
INSTALLED.txt

## Same-pass surfaces (owed at install close-out, parent lane)
Guide knowledge + website spell numbers for the arrow family (any
"arrow hits ~40+" claims corrected to config values), What's New
BOTH surfaces (website section + in-game ExcelWhatsNew changelog),
broadcaster tips check, in-game menus check (menus key off spell
keys; expected no change — verify).

## Proof owed after install
Jester recast set (the commission's named proof): arrow|6 on a
full-health unarmored target at fixed distance, 10+ casts —
expect ~23 per direct hit (7 physical + 16 payload as separate
incidents), no 40+ PROJECTILE incidents; ice / shock / regular
arrows per-hit ~9 / ~9+pulses / 9. Discharged only by Jester's
own report / Protect stream read.
