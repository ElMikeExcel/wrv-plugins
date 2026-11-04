# ExcelEliteLadder 0.1.2: the ladder tops out at Seal 4, compressed (patch, STAGED, not live)

Built Sun Nov 1, 2026, on the box. Deterministic build: two clean builds give the same jar sha
(7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb, 31,403 B).
Live is 0.1.1 (f7ec4c1ff86487e80a992ce1a982151ec8900ceab482b8ca46c94b42bdf71521, live Oct 26, 2026,
~12:07 AM CDT; state at build time: applied step S3, live MobCombatSettings.yml = S3-norm,
sha 983fa757d5c4d075d3f69444739081f395ad559de9bde1c209346d520554f5c4).

## Why
Mike re-decided the EliteMobs overworld ladder on Nov 1, 2026, 2:30 PM CST: it TOPS OUT AT
SEAL 4, COMPRESSED. Seal 4 carries the full end-state values (spawn 0.11, dealt x2.35,
taken x0.40, level cap 150); there is no Seal 5 ladder step, because once Seal 5 breaks the
players transfer to the EliteMobs guild world and an overworld step landing at 5 of 5 would
never be felt. This supersedes the Oct 21, 2026, 1:22 PM S5-parity final (S5 0.11/2.35/0.40/150
as a separate step) and the Oct 25, 2026 Seal 5 picks (spawn 0.12 in 0.1.1). The ladder table
is built into the jar on purpose (not editable in config), so the change needs a new jar.
URGENT-BEFORE-WITHER: left on 0.1.1, the Wither break would auto-apply the OLD S4 row
(0.10/120/0.45/2.20) and the compressed peak would never land.

## What changed
- `LadderTable`: S4 row 0.10/120/0.45/2.20 -> 0.11/150/0.40/2.35 (the compressed peak);
  S5 row 0.12/120/0.45/2.20 -> 0.11/150/0.40/2.35, IDENTICAL to S4, so a Seal 5 trigger is a
  safe no-op and the plugin's row structure stays intact. S1-S3 rows, the dungeon
  (normalized) values 0.5/2.0 on every step, and every other class are byte-identical to 0.1.1
  (jar entry diff vs the live 0.1.1 jar: only LadderTable.class, plugin.yml, config.yml differ).
- `plugin.yml` version 0.1.2; `build.sh` output name 0.1.2, jar stamp 2026-11-01.
- `config.yml` (the default the jar creates on a fresh install): header now says 0.1.2 and
  prints the new table. Comment only. An existing plugins/ExcelEliteLadder/config.yml is never
  overwritten, so on the live server its comment keeps saying 0.1.0/0.11; that is harmless
  (the ladder is read from the jar, not the config).
- `tests/fixtures/S4-norm-MobCombatSettings.yml` and `S5-norm-MobCombatSettings.yml`: both now
  the compressed-peak file (byte-identical to each other by design, sha a8af53d70cf5146131fe4201bdf50b5bf461af6a7ea291a740c69544b43c0b3a).
- Tests: the 0.1.1 block is replaced by a 0.1.2 block (peak values pinned; S5 = S4 on all six
  values; a 0.1.1-era S4 file and a 0.1.1-era S5 file are both off ladder, and the S4 refusal
  names exactly the four peak lines) plus FileApplier proofs (S3 -> S4 peak applies and lands
  the expected EM-form sha; S5 from the peak is a no-op leaving the file byte-identical).
  Plain-JVM suite: 77 passed, 0 failed.

## The ladder (built into the jar)
| step | spawn | cap | toElite (L109) | toPlayer (L112) | dungeon toElite/toPlayer (L91/L94) |
|---|---|---|---|---|---|
| S1 | 0.07 | 120 | 0.55 | 1.75 | 0.5 / 2.0 |
| S2 | 0.08 | 120 | 0.50 | 1.90 | 0.5 / 2.0 |
| S3 | 0.09 | 120 | 0.50 | 2.05 | 0.5 / 2.0 |
| S4 | **0.11** | **150** | **0.40** | **2.35** | 0.5 / 2.0 |
| S5 | 0.11 (= S4, no-op) | 150 | 0.40 | 2.35 | 0.5 / 2.0 |

## Shas
- Base proof: the unmodified 0.1.1 source tree (GitHub wrv-plugins PR #70 branch
  feature/exceleliteladder-0.1.0 @ d508910) rebuilds BYTE-IDENTICAL to the shipped jar
  (f7ec4c1f...) on Temurin 25.0.5+8, and its suite passes 63/63 before any change.
- S4 after apply (EliteMobs number form, on the live file): 4708f3c3d924a61d2038ad17b338f10d5d101dcb9e1a07d73cf3d7b58f6c418d
  (proven on the rig against the pulled live S3 file; see RIG-TEST-RESULTS-0.1.2.md).
  S1 to S3 EM-form shas are unchanged (57ad4ab2, 9b4c3f95, 983fa757).

## Upgrade notes
- State carries over: plugins/ExcelEliteLadder/state.yml (applied step, last seals, latch),
  backups/ and apply-log.txt are read as before. Nothing is re-applied on upgrade.
- Install 0.1.2 BEFORE the Wither (Seal 4) can break. If 0.1.1 has already applied its S4
  (0.10/120/0.45/2.20) before 0.1.2 lands, 0.1.2 shows the live file as OFF LADDER and changes
  nothing (no sideways edits by design); the fix is then the supervised key-edit in
  INSTALL-0.1.2.md.
- Pairing: ExcelTrainingWheels' seal compensation follows the dealt column (S4/S5 -> 2.35);
  its reconciled packet is staged separately (~/workspace/mob-scaling/trainingwheels-compressed-20261101/)
  and is NOT part of this jar.

## Tests
- Plain-JVM suite: 77 passed, 0 failed (`./run-tests.sh`).
- Box rig (Paper 26.3-235, EliteMobs 10.9.12, ExcelGuildGate 0.4.0): live-state boot,
  Wither-break simulation (S4 peak auto-applies), Warden-break simulation (S5 no-op).
  See RIG-TEST-RESULTS-0.1.2.md.
