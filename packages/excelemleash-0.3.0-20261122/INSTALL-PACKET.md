# INSTALL PACKET — ExcelEMLeash 0.3.0 (D183 MMO leash, 96 blocks)

Lint: 4861 B / max line 69 / non-ASCII 42
Filed: Nov 21, 2026. Decision: D183 (Mike, Nov 21, 2026, 7:16 PM CT,
tapped "96 blocks from spawn"); supersedes D182. Backlog:
P-LEASH-BOSS-AGGRO-181. Player-fix push rule applies: install at the
first verified-empty moment.

## Live-premise receipts (G1)

All receipts in `RECEIPTS.md` in this folder (pulled fresh Nov 21,
2026, 21:31 CT, Homebase scp route):

- Live jar: `plugins/ExcelEMLeash-0.2.0.jar` = 7,356 B / SHA-256
  `7f72e67259552f1a5d188ad0fcfc1fe7ae95e8a675c8b274f1c7ba96be6fe838`.
- Live config: `plugins/ExcelEMLeash/config.yml` = 988 B / SHA-256
  `1b44b7df6626a2bb198d991e75ad392d3f9fdc942ccf610912b0b0b967f5a84a`
  (0.2.0 shape: `leash-removal.enabled: true`, `debug: true`).

## Candidate

- `~/workspace/leash-mmo-96-20261121/ExcelEMLeash-0.3.0.jar` —
  13,655 B / SHA-256
  `2b04e90f651767047279ec1a53181916bac0e43cd6371f919c6675986356033e`;
  deterministic build (two builds byte-identical).
- Tests: harness 31/31 PASS; rig suite 10/10 PASS, SEVERE 0
  (preserved log: `rig-runtime-final.log` in the build folder).
  Rig cases: registry/suppression (REG/R1/R2/R3), (a) pull past 96
  -> deaggro + walk home + full heal on arrival, (b) mid-return hit
  -> instant re-aggro on the damager + no heal, (c) camp fight at
  ~40 blocks never deaggros (Marvin regression), (d) death and
  world-leave end aggro.

## Live-jar gate (STOP on mismatch)

Live `plugins/ExcelEMLeash-0.2.0.jar` must hash to `7f72e672…`
(7,356 B) at swap time, per the receipt above. Any mismatch = stop,
do not swap, report.

## Staged artifacts

Folder `~/workspace/leash-mmo-96-20261121/` (`SHA256SUMS` inside):

- `ExcelEMLeash-0.3.0.jar` — 13,655 B / `2b04e90f…` (above).
- `config.yml` — 1,819 B / SHA-256
  `65d0f4b2863f8b4ff211c6d51073823184c0a332d4dc8e2e0ab9af108fcb2df5`
  (new 0.3.0 shape: `leash.enabled: true`, `leash.range: 96`,
  `debug: true`).

## Scope

- Swap `plugins/ExcelEMLeash-0.2.0.jar` -> `ExcelEMLeash-0.3.0.jar`;
  the 0.2.0 jar is kept as `.bak` in the plugins folder (reversal).
- Place the staged `config.yml` over
  `plugins/ExcelEMLeash/config.yml`; the live 0.2.0 config is kept
  as `.bak` beside it first.
- Nothing else changes: no boss files (the plugin never writes
  them), no other plugin, no data folders, no player data.
- Behavior change (D183): EM's native leash stays suppressed
  (in-memory radius 0, as 0.2.0); the plugin now runs the return
  itself — a combat elite pulled more than 96 blocks from its spawn
  deaggros, walks home, and heals to full on arrival; player damage
  mid-return re-aggroes it on the damager immediately and the heal
  never runs. Inside 96 blocks nothing changes at all.

## Gates

Standard gated install: window marker discipline (v2) with
re-checks before Shut Down and before Start; BACKUP FIRST +
cap-at-6; emptiness `list` = 0 twice before firing. RESTART pickup
(jar + config swap; no reload path).

## Boot verify

- Boot log carries the ready line verbatim: "ExcelEMLeash 0.3.0
  ready — MMO leash on: deaggro + return + heal past 96 blocks from
  spawn (D183)."
- Boot log carries a "LEASH suppression applied" summary line.
- Post-boot jar hash = `2b04e90f…`; SEVERE 0 attributable.
- Marker: `~/workspace/leash-mmo-96-20261121/INSTALLED.txt`.

## Installing lane — required extras

1. **Timing:** append this window's row to
   `~/workspace/wrv-install-queue/INSTALL-TIMING-LOG.md`
   (fire->window open, backup, downtime stop->Done, boot,
   fire->verified close), per Mike's Nov 21, 2026 rule.
2. **What's New (same pass, BOTH surfaces — website section AND
   in-game ExcelWhatsNew changelog):** the entry must describe the
   **96-block leash** behavior: an elite pulled more than 96 blocks
   from where it spawned gives up the chase, heads home, and heals
   when it gets there — unless you hit it on its way home, which
   starts the fight again with no heal. The 0.2.0 "reset removed"
   wording was never published and is superseded — do not publish
   it. Guide knowledge/intents + broadcaster tips + in-game menus:
   checked in the same pass (no leash text expected there; state
   the check result in the run record).
3. **Player comms:** Marvin is owed a short update email after
   install (parent seat sends): he was told Nov 21 ~18:29 the reset
   is "gone entirely" (D182); D183 refines it to the 96-block leash
   the same evening.

## Proof owed after install

First natural in-world chase past 96 blocks from a boss's spawn (or
a bot proof) showing: deaggro at >96, walk home, heal on arrival —
and, when it occurs naturally, a mid-return hit re-aggroing with no
heal. That proof closes P-LEASH-BOSS-AGGRO-181. Mirror/rig proof is
on file (above) per the ship-it rule; the live proof is the
acceptance tail, not an install gate.
