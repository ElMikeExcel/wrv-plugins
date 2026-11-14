# PULLS REPORT - P-SPELLBOOK-1 filing-day G1 pulls (board supervisor run 330, worker W5)

Date: 2026-11-14 (America/Chicago). Scope: PC-route pulls only.
No live changes, no installs, no MANIFEST.md edit. The live server was
OFFLINE for the nightly maintenance window; all pulls are rclone SFTP
file reads, valid regardless of server process state.

## Probe outcome

- PC route probe: PASS on attempt 1 of 3 (retry pattern not needed
  beyond attempt 1). Command: ssh -F ~/.ssh/config mike-pc "hostname"
  -> DESKTOP-IL05DQR at 2026-11-14 02:42:26 CST.
- Last successful probe BEFORE this run: 01:44 CST Nov 14 (run 329 W1,
  per board-manager-state.md). This run's probe + 16 successful rclone
  copyto operations (02:42:51-02:43:48 CST) are the new last-success
  evidence: 2026-11-14 02:43:48 CST.
- Fallback: NOT used. No FALLBACK (panel) receipt was needed or taken.
  No panel touched.

## Method

- Pull script: g1-pulls/pull-g1-bm330-spellbook.ps1 (kept in this
  folder's g1-pulls/ subfolder), run on the PC; remote
  shockbyte:1. Michael's Server; rclone copyto per file into PC temp
  staging C:\Users\MikeExcel\muse-temp\g1-bm330-spellbook\, then scp
  down to ~/workspace/spellbook-fix-20261114/g1-pulls/.
- Hashes: SHA-256 computed locally on the pulled bytes; cross-checked
  against the PC-side Get-FileHash output (HASHES.txt in g1-pulls/) -
  all 16 files identical both sides.
- PC temp staging + the pull script copy on the PC were deleted after
  copy-down (Test-Path returned False). Pulled bytes retained locally
  in g1-pulls/ (14 packet files + ExcelMagicGate config + latest.log
  + HASHES.txt + pull script).
- Re-base check additionally re-run as a script over the pulled bytes
  vs the RECEIPTS.md section B bases: output "RE-BASE: 14/14 EXACT".

## Slots filled: 15/15

RECEIPTS.md section C is fully filled (no blank fields remain).
RECEIPTS.md after fill: 13,061 B / SHA-256
43b60a09018ad430b19881896d35a2b91156226f7401fcecb974353fcb889101
(supersedes the desk-assembly receipt 9,801 B / f15c2aeb... cited in
INSTALL-PACKET.md; the packet's receipts citation needs that hash
refreshed at filing.)

| Slot | Live path | Bytes | SHA-256 (first 8) | Pull time CST | Re-base verdict |
|---|---|---|---|---|---|
| PULL-01 | plugins/ExcelWandBars/spellbook.yml | 2,547 | d3bc78d4 | 02:42:51 | EXACT |
| PULL-02 | plugins/Magic/spells.yml | 188,164 | 68006831 | 02:42:56 | EXACT |
| PULL-03 | plugins/ExcelMagicGate/stages/stage2/spells.yml | 89,176 | 5312fdd1 | 02:42:59 | EXACT |
| PULL-04 | plugins/ExcelMagicGate/stages/stage3/spells.yml | 145,625 | 0bb76856 | 02:43:03 | EXACT |
| PULL-05 | plugins/ExcelMagicGate/stages/stage4/spells.yml | 188,164 | 68006831 | 02:43:07 | EXACT |
| PULL-06 | plugins/ExcelMagicGate/stages/stage5/spells.yml | 188,164 | ad10c4eb | 02:43:11 | EXACT |
| PULL-07 | plugins/ExcelMagicGate/stages/stage3/paths.yml | 2,629 | 1fb6d5ad | 02:43:15 | EXACT |
| PULL-08 | plugins/ExcelMagicGate/stages/stage3/wands.yml | 5,857 | e3bfc5c1 | 02:43:19 | EXACT |
| PULL-09 | plugins/ExcelMagicGate/stages/stage4/paths.yml | 2,983 | 78c88792 | 02:43:23 | EXACT |
| PULL-10 | plugins/ExcelMagicGate/stages/stage4/wands.yml | 6,648 | 744cb385 | 02:43:26 | EXACT |
| PULL-11 | plugins/ExcelMagicGate/stages/stage5/paths.yml | 2,983 | 78c88792 | 02:43:30 | EXACT |
| PULL-12 | plugins/ExcelMagicGate/stages/stage5/wands.yml | 6,648 | 744cb385 | 02:43:34 | EXACT |
| PULL-13 | plugins/Magic/paths.yml | 2,983 | 78c88792 | 02:43:37 | EXACT |
| PULL-14 | plugins/Magic/wands.yml | 6,648 | 744cb385 | 02:43:41 | EXACT |
| PULL-15 | active guild-stage state | - | - | 02:43:48 (log pull) | stage 4 - see below |

Full hashes are recorded per-slot in RECEIPTS.md section C.

## Re-base verdict per base file

All 14 section B bases: EXACT match (size + SHA-256) against the fresh
same-day pulls. Drift count: 0. No file needs re-basing; verify.py does
not need a re-run on new bases (its staged-side "RESULT: ALL CHECKS
PASS" stands against unchanged bases).

## PULL-15 verdict

Active stage = 4. Evidence (all pulled this run, rclone):
- logs/latest.log (258,617 B / a95e13f4...) line 666:
  "[ExcelMagicGate] ExcelMagicGate enabled: applied stage 4 of 5,
  watching ExcelGuildGate seals every 60s; build
  EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b; stage ceiling master"
- Same log line 340: "ExcelGuildGate enabled: 4 of 5 seals broken,
  guild LOCKED" (consistent: 4 seals -> stage 4 applied).
- Hash discriminator: live Magic/spells.yml (68006831...) equals the
  stage4 template pull and differs from the stage5 template pull
  (ad10c4eb...); live Magic paths/wands equal the stage4 pulls.
- Supporting pull: plugins/ExcelMagicGate/config.yml (2,926 B /
  6c46a707...) carries no stage field - applied stage is runtime
  state, proven by the log line.
The packet's assumption (stage4-generation live pair) holds. The stage
has NOT flipped.

## Filing readiness

FILING-READY on the G1 gate: 15/15 slots filled, 14/14 bases EXACT,
stage confirmed 4. Nothing in this leg blocks filing the
MANIFEST-ENTRY-DRAFT stub. One clerical note for the filing pass:
INSTALL-PACKET.md cites RECEIPTS.md as 9,801 B / f15c2aeb... (the
pre-fill desk state); after this fill RECEIPTS.md is 13,061 B /
43b60a09... - refresh that citation when filing.
