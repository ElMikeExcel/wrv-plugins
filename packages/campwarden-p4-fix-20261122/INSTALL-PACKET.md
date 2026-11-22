# PENDING — Camp Warden P4 door-seal content fix (2026-11-22)

- Lint: 5415 B / max line 76 / non-ASCII 3
- Packet: ~/workspace/camp-warden-fix-20261122/INSTALL-PACKET.md
- Receipts: ~/workspace/camp-warden-fix-20261122/RECEIPTS.md
  (G1 live-premise receipt lives there, cited by the gate below)

## Live-premise receipts (G1)

- Live P4 file exists with 2,815 B / 7661989f... — receipt:
  RECEIPTS.md in this folder (fresh Homebase pull
  2026-11-22 01:02 AM CT; pulled copy in live-pull/).

## Candidate

- primis_bloodtemple_climb_miniboss_1_p4.yml in this folder —
  3,485 B / SHA-256
  831f86d3566bc8ed3968acc256bc32e7afa2240d9609210a711953e67be020b6
- Build: content-only edit of the live P4 file. P4
  onRemoveBlockStates extended 15 -> 30 entries: the 15 Door B
  open entries (offset z -14.5) copied verbatim from P1
  onSpawnBlockStates, appended after the existing 15 Door A
  restore entries. No other line changes (diff-verified).
- Tests: YAML parse-validated in the verify unit; entry census
  by script: candidate onRemove = 30 (15 Door A + 15 Door B),
  onSpawn = 0; live onRemove = 15, onSpawn = 0.
- Basis: ~/workspace/camp-warden-verify-20261122/VERIFY-RECORD.md
  sections 4/6 (bytecode-proven DEATH removal path applies the
  current phase's onRemoveBlockStates; P4 is the terminal
  phase). Decision D188: ship this content fix; boss-alive
  knock-out half accepted (option a); options (b)/(c) deferred.

## Live-file gate (STOP on mismatch)

- Live file plugins/EliteMobs/custombosses/primis/
  primis_bloodtemple_climb_miniboss_1_p4.yml must equal
  2,815 B / 7661989fe8c3870838d54bd43b72609b2aa2ef15c88e70c7dafc20ca60cc1f53
  at install time (fresh re-pull at the gate; the RECEIPTS.md
  pull does not substitute for the gate re-pull).
- Candidate staged bytes must equal 3,485 B / 831f86d3...
  (full hash above) at install time.

## Scope

- Changes: ONE config file swapped on the live server:
  plugins/EliteMobs/custombosses/primis/
  primis_bloodtemple_climb_miniboss_1_p4.yml (live bytes ->
  candidate bytes). Effect: on Camp Warden death in P4, both
  arena doors restore (Door A as today, Door B newly added).
- Does NOT change: P1/P2/P3 phase files; seal behavior while
  the boss is alive (both doors still seal during the fight);
  any jar, plugin, data, player, or world file; loot.
- Known limit (accepted, D188): no door reopens while the boss
  is alive; a knocked-out player waits for the kill or uses
  /spawn and returns after the fight. Despawn/reset restores
  nothing (unchanged from today).

## Gates (fully gated install)

- Marker discipline: check WINDOW-IN-PROGRESS.txt before the
  window; write the installing lane's own v2 marker at window
  start; re-check the marker immediately before Shut Down and
  again before Start; a foreign LIVE / LIVE-LONG-STEP /
  SUSPECT marker = coordination abort.
- BACKUP FIRST + cap-at-6: full server backup before the swap;
  prune oldest manual backups past the cap, naming prunees;
  never delete the Daily Backup's newest copy or the two
  newest manual restore points.
- Emptiness: RCON `list` = 0 players, twice, before Shut Down.
- In-place net: the replaced live file is kept beside the
  target as primis_bloodtemple_climb_miniboss_1_p4.yml.bak
  (pre-swap live bytes, 2,815 B / 7661989f...).

## Install steps

1. Pass all gates above.
2. Back up the live P4 file to the .bak name above; verify the
   .bak hashes to 7661989f... by script output.
3. Copy the candidate over the live P4 path.
4. Verify the live path now hashes to 831f86d3... (3,485 B)
   by script output (Get-FileHash / sha256sum).
5. Start the server; run boot verify below.

## Boot verify (by script output)

- Server reaches Done; EliteMobs enables with no attributable
  SEVERE (grep the boot log: SEVERE count attributable = 0).
- Live P4 file still 3,485 B / 831f86d3... after boot.
- YAML census on the live file by script: onRemove = 30.
- Guild state unchanged (LOCKED count as before the window).
- Marker: ~/workspace/camp-warden-fix-20261122/INSTALLED.txt
  written by the installing lane with window, backup name,
  prunees, gate results, and the hashes above.

## Same-pass surfaces

- What's New BOTH surfaces are OWED in the install pass
  (website section + in-game ExcelWhatsNew changelog.yml),
  entry text drafted here, player wording:
  "Camp Warden arena doors now both reopen when the boss dies."
- Guide knowledge/intents: checked in the install pass; no
  Guide text teaches the old one-door behavior (verify record
  found none); if any is found, update it in the same pass.
- Broadcaster tips: checked in the install pass; none expected.
- In-game menus: checked in the install pass; none affected.
- Website pair beyond What's New: none (no mechanics page
  describes the door restore list); named here as checked.

## Proof owed after install

- Next natural Camp Warden kill restores BOTH doors (Door A at
  x=687 and Door B at z=1115 both back to their open state),
  reported by the killing player(s); OR a bot proof driving
  the same kill path. Proof is only discharged by that report
  or the bot run's block-probe output, filed against this
  packet. Until then the item stays proof-owed, not proven.

## Rollback

- Restore the .bak over the live P4 path under the same gates
  (backup-first, emptiness twice, marker discipline), restart,
  re-run boot verify with the gate hash back at 7661989f....
