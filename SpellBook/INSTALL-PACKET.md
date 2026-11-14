# INSTALL PACKET - P-SPELLBOOK-1 spellbook regroup fix (F1-F4, config only)

Lint: 6119 B / max line 85 / non-ASCII 0
Status: DESK-ASSEMBLED 2026-11-14 (run 329). NOT FILED - the filing-day
G1 pulls in RECEIPTS.md section C are blank and must be filled from fresh
same-day pulls before this packet enters the MANIFEST.
Scope source: ~/workspace/spellbook-audit-20261114/AUDIT.md section 10,
fix list F1-F4. F5 is the separate owed item P-SPELLBOOK-F5 (WandBars
locked-view code gap) and is NOT in this packet.

- **Packet:** ~/workspace/spellbook-fix-20261114/INSTALL-PACKET.md
- **Receipts:** ~/workspace/spellbook-fix-20261114/RECEIPTS.md
  13,061 B / SHA-256 43b60a09018ad430b19881896d35a2b91156226f7401fcecb974353fcb889101
  (section A staging evidence, section B candidate hash table for all 14
  destinations, section C filing-day G1 pulls FILLED Nov 14, 2026, run 330:
  15/15 slots, 14/14 bases EXACT, stage 4 confirmed not flipped.)
- **Staging record:** ~/workspace/spellbook-fix-20261114/STAGING-RECORD.md
  15,193 B / SHA-256 39ffae507a5069ce03ee3255758ebd8459efdf9caf92981cfe5402ce6962ee03

## Live-premise receipts (G1)

No live pull exists yet for this packet - by design. The 14 named
filing-day pulls (PULL-01 to PULL-14) plus the active guild-stage state
(PULL-15) are listed in RECEIPTS.md section C with size / SHA-256 / pull
timestamp / transport fields BLANK. They are filled at filing from fresh
same-day pulls (rclone via the PC route; FALLBACK (panel) receipts are
valid if the PC route fails its probes). Values must NEVER be copied
from the audit census: the PC route was down during the audit and its
census rows are marked not-re-verified. Gate rule: a pulled hash that
differs from the RECEIPTS.md section B base hash for that destination =
STOP, re-base that staged file from the pull, re-run verify.py, and only
file when the output again ends "RESULT: ALL CHECKS PASS".

## Candidate

- Staged folder: ~/workspace/spellbook-fix-20261114/staged/ - 14 files
  (1 spellbook + 5 spell books + 8 paths/wands), per-file sizes and
  SHA-256 in RECEIPTS.md section B; unified diffs in diffs/.
- Integrity: SHA256SUMS (30 entries) re-run at assembly: 30/30 OK.
- Verification: verify.py output VERIFY-OUTPUT.txt ends
  "RESULT: ALL CHECKS PASS". Control run over the base files reproduces
  the audit census and Mike's Nov 14 screenshots exactly (Apprentice 80
  known / Summon 4 / Other 3). Post-fix counts by the same counter:
  Master 114 known / Summon 11 / Other 0; Apprentice 81 known
  (+grouprenewal) / Summon 7 / Other 0.
- No jar is built or changed by this packet; no build id applies.

## Live-file gate (STOP on mismatch)

There is no live jar in this packet. The gate is the 14 live config
files: each filing-day pull (RECEIPTS.md section C) must equal the base
size + SHA-256 in RECEIPTS.md section B before its staged file may be
placed. Any mismatch stops that file's placement pending re-base.

## Scope

- F1 regroup (staged/wandbars/spellbook.yml): Summon adds archer,
  bruiser, guardian, revenant, ghast, mount, tamepet (4 -> 11); Damage
  drops flare + counterspell, gains earth + anvil (stays 32); Building
  drops earth (9 -> 8); Utility drops anvil, gains flare + counterspell
  (22 -> 23). Post-fix listings total 114.
- F2 grant path (config route, no jar change): grouprenewal added at
  Apprentice and resurrection at Master in paths.yml + wands.yml for
  live Magic and the stage3/4/5 MagicGate templates (stage3 receives
  grouprenewal only - resurrection is absent from the stage3 book);
  both listed under Healing in spellbook.yml (10 -> 12). Existing wands
  pick both up via the standing WandInventorySync reconcile.
- F3 rename: tamepet display name becomes "Pet Heal" (name: line only,
  key untouched) in all five spell books; with F1 it shelves in Summon
  beside Tame.
- F4 display names: explicit name: lines for archer, bruiser, guardian,
  revenant, ghast, mount in every spell book where each exists
  (26 F3+F4 name lines total across the five books, additions only).
- Does NOT change: any plugin jar; the MagicGate supplemental-grants
  map; any player data, wand, or earned file; any spell's mechanics,
  costs, or tiers; the F5 locked-view gap (separate item P-SPELLBOOK-F5).

## Gates

Gated install (config swap, NOT a text live-edit): marker discipline
with marker re-checks immediately before Shut Down and before Start;
BACKUP FIRST with cap-at-6 (prune oldest manual backups, name prunees,
never delete the Daily Backup's newest copy or the two newest manual
restore points); emptiness gate list = 0/10 twice before the swap.

## Boot verify (by script output)

- Pull back all 14 placed files and run sha256sum against the candidate
  hashes in RECEIPTS.md section B: expected 14/14 match.
- Boot log: the ExcelWandBars warning "tamepet in no spellbook group" is
  ABSENT (it fired on every boot before this fix); no new SEVERE lines
  attributable to ExcelWandBars, ExcelMagicGate, or Magic.
- Guild state unchanged: guild LOCKED n of 5 reads the same n as the
  pre-install boot.
- Marker: ~/workspace/spellbook-fix-20261114/INSTALLED.txt

## Same-pass surfaces (owed at install, named here)

- Guide knowledge: spellbook groups + the two new grants + Pet Heal
  rename - OWED at install.
- What's New, website section - OWED at install.
- What's New, in-game ExcelWhatsNew changelog - OWED at install.
- Broadcaster rotation tips check - OWED at install.
- In-game menus check (spellbook is the menu surface; no menu config
  changes) - OWED at install.
- Player-facing what-changed explanation (Nov 13 rule): Summon family
  complete at 11; Group Renewal unlocks at Apprentice; Resurrection at
  Master; Tame Pet is now Pet Heal - ships with the surfaces above.

## Proof owed after install

Acceptance: Mike's in-game glance at a Summon group showing the full
family - Summon 11 / Other 0 post-fix per the verify output - on a
Master wand, spot-checked on Java + Bedrock. Supporting glance on an
Apprentice wand: Group Renewal in Healing, Pet Heal in Summon, Other
empty. Proof is discharged only by Mike's own report or screenshot.

## Marker on install

~/workspace/spellbook-fix-20261114/INSTALLED.txt
