# INSTALL PACKET — ExcelPouch 0.1.1 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands for support
ops on a player's state. 0.1.0 was a FAIL: `/pouch` replied
"Only players can use the Utility Pouch." to every non-player
sender — the console could not even inspect a pouch. 0.1.1
closes it. Brief: `~/workspace/pouch-consoleadmin-20261110/
BUILD-BRIEF.md`; build record: `BUILD-RECORD.md` in this folder.

## Why

Verified from the 0.1.0 source (audit + design): the single
command handler rejects every non-player sender before any
argument handling, there is no `excelpouch.admin` node, no
reload path, and no accessor for a broken store's state. The
state at risk is small but irreplaceable — parked menu compasses
and a player's own bound Magic wand, whose learned spells and
rank live inside the parked bytes with no second copy. 0.1.1
adds `/pouch admin status [<player>]`, `/pouch admin return
<player> <selector>`, `/pouch admin clear <player> <selector>
confirm` (the D10 safety stack: confirm gate → close-GUI-first
with post-sync revalidation → verified verbatim rescue copy →
delete via setPocket → audit line), and `/pouch admin reload`,
gated by the new `excelpouch.admin` (default op) with an
explicit console bypass. Player-facing behaviour is unchanged.

## The artifact

- ExcelPouch-0.1.1.jar — 45,109 bytes
  SHA-256 b4cdfa2057733a06fe39607bd018c571d8bb0a9cfbce06d753cc0e6cfff3f9c4
  Staged at ~/workspace/pouch-011-build/ExcelPouch-0.1.1.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This lineage has no
  in-jar build-ID fingerprint; version + hash identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelPouch-0.1.0.jar — 28,574 bytes — SHA-256
  ace560bfd315f3dd9b3ebc54562fb45a5ca82a5bae1750ec521293122a69d211.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 0.1.0: admin routing + handlers in ExcelPouch,
  new core/AdminSelector, additive PouchStore accessors
  (isBroken / snapshot / rawEntry), plugin.yml (version, usage,
  the new permission). config.yml ships byte-identical content
  (no new keys) — do NOT overwrite the live config. No data
  migration: existing `plugins/ExcelPouch/data/*.yml` files are
  format-compatible in both directions. `data/rescue/` is
  created by the plugin on the first confirmed clear, not by
  this packet. No permission grants required (console works at
  once; `excelpouch.admin` defaults to op).

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelPouch-0.1.0.jar →
   ExcelPouch-0.1.0.jar.bak-20261110 (keep alongside; only one
   ExcelPouch jar loadable at a time).
3. Upload ExcelPouch-0.1.1.jar as plugins/ExcelPouch-0.1.1.jar.
   Panel has no checksum field: verify by exact size, 45,109
   bytes, in the file listing.
4. NO config edit, NO data change, NO other plugin change
   (ExcelMenuJoin needs no bump — the PouchQuery contract is
   untouched).
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelPouch v0.1.1" present.
- Enable line names 2 pocket(s) and the allowlist
  [menu_compass, magic_wand] with wand key 'wand' (or the live
  config's values if they have legitimately changed).
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `pouch admin status` → the three global lines: version/
   slots/allowlist/wand key; loaded stores vs data files
   (quarantined count); "PouchQuery service: registered
   (holdsKind live for ExcelMenuJoin)".
2. `pouch admin status NobodyHereXYZ` → "No player named
   'NobodyHereXYZ' has played here, and it is not a UUID —
   nothing was created."
3. `pouch admin` → the admin help block (mutates nothing).
4. `pouch` → "Only players can use the Utility Pouch." (the
   player path still rejects the console — the allowance is
   the admin branch only, by design).
5. Live proof (brief §6.3): `pouch admin status <a real family
   member>` → the per-pocket listing reads back in the log.
   Read-only; do NOT smoke a real return/clear outside a
   genuine rescue need.

## Player proof

None owed — no player surface changes (brief §6.3). The first
real rescue (a console status/return/clear for a family member)
is the live exercise; the audit line in the log is its record.

## Rollback

Stop; delete ExcelPouch-0.1.1.jar; rename
ExcelPouch-0.1.0.jar.bak-20261110 → ExcelPouch-0.1.0.jar; start;
verify "Enabling ExcelPouch v0.1.0" and normal pouch behaviour.
Data files are compatible in both directions; any
`data/rescue/*.b64` files written by 0.1.1 are inert text under
0.1.0 and are left in place.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only commands — no player
teaching owed; confirm no player-facing pouch-admin claim
exists), What's New BOTH surfaces (website section AND the
in-game ExcelWhatsNew changelog — an operator-note line that
console rescue for the pouch now exists is the only owed text;
no player-facing change), rotation tips checked (no tip change
— player surface unchanged; state the check in the install
report), in-game menus checked (ExcelMenu + ExcelBedrockMenu —
no menu change; state the check in the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain (local
+ shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelpouch-0.1.1-20261110/,
and a one-line note closing the audit's ExcelPouch FAIL row
(console-admin audit §2/§4) once the §6.3 live console-status
proof passes.
