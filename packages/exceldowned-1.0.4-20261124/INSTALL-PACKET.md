# PENDING - ExcelDowned 1.0.4 (wand-owner guard: unmark on quit/join)

Filed Nov 23, 2026 (D212, decision D214). Parent seat installs; this lane did NOT install.

## What this fixes

The ExcelDowned x Magic wand-deletion defect (evidence:
~/workspace/wand-restore-20261123/RECORD.md; fix record:
~/workspace/downed-wand-fix-20261123/RECORD.md). While a player is
downed, ExcelDowned sets their display/list name to the marked form
"✝ DOWNED <name>". Magic derives a display-name-bound wand's owner
string from the display name (boundDisplayName defaults to true in
Magic 11.2.5), so the marked name gets absorbed into the wand's owner
("✝ DOWNED MikeExcel" in Magic's own save, Nov 23, 2026). At the next
login the wand fails Magic's owner validation and is deleted silently
- no drop, no lostwands registration, no log line. The bleed-out death
and its normal loot drop are documented design and are NOT changed.

1.0.4 is the prevention half of a two-plugin fix (the repair half is
ExcelMagicGate 0.2.1, filed alongside): ExcelDowned never lets the
mark be the name another plugin can read at a save/validation seam.

## Changes (jar swap + one config insertion)

- On player quit (EventPriority.LOWEST, ahead of other plugins' quit
  handlers): restore the player's real display/list name. The downed
  session itself is untouched - it persists across logout exactly as
  designed; the saved original names are kept, so a later revive or
  bleed-out finalize restores through the existing path.
- On player join (EventPriority.LOWEST): clear any stale mark before
  normal join processing, so a session restored from disk can never
  present the marked name at login. Live downed sessions re-mark
  through the existing join handling, unchanged.
- Mark composition now lives in one pure helper (DownedMark); the
  composed mark is byte-identical to 1.0.3's.
- Config: ONE new section inserted into the live config (below).
  Toggle `wand-owner-guard.unmark-on-quit` (default true) disables
  both restores if the guard ever needs to come off.
- No change to downed, bleed-out, death, drop, revive, or spell
  behavior. No data-file format change.

## Candidate

- ~/workspace/downed-104-wandfix-20261123/ExcelDowned-1.0.4.jar -
  141,173 B / SHA-256
  c4c90d62eaadf59df48c2ee32cbd4f230f36d5a4f58e97a03771189ccf29d029
- Build: EXC-DOWNED-1.0.4-WRV-20261123-01-wandguard
- Tests: 84/84 PASS (81 carried from the 1.0.3 suite + 3 new
  mark/guard tests pinning the mark to the incident string).
  Double-build byte-identical; fingerprint gate PASS.
- Source tree: ~/workspace/downed-104-wandfix-20261123/work/build-1.0.4/
  (DESIGN-1.0.4.md carries the change write-up).
- Mirror proof: see "Proof on record" below (filled at filing).

## Live-premise / live-jar gate (STOP on mismatch)

- 1.0.4's premise is **ExcelDowned 1.0.3 INSTALLED**. Live
  plugins/ExcelDowned-1.0.3.jar must equal 139,573 B /
  3559187979ba18efa0a265b439cb6664081527b1d32c4fa23364841f122983ca
  (re-pull fresh at the gate; never cite this line as the gate).
- If live runs anything else, STOP and re-base.

## Config insertion (pure insertion into live plugins/ExcelDowned/config.yml)

Insert this block (top level, e.g. right after the bleed-out section):

```yaml
# Wand-owner guard (1.0.4): while you are downed, your display name
# carries the downed mark. Some items (Magic wands) record their
# owner from that display name, and a wand stamped with the marked
# name fails its owner check at your next login and is deleted.
# With this on, ExcelDowned restores your real display name the
# moment you log out (and clears any stale mark when you log in),
# before other plugins save or check anything - so the mark can
# never be written into an item. Your downed session is NOT
# affected: it still persists and still bleeds out on schedule.
wand-owner-guard:
  unmark-on-quit: true
```

If the live config already carries the section (it should not),
skip the insertion.

## Swap steps (standard empty-server window, backup first)

1. Verify server empty (list = 0 twice); verify live jar hash =
   35591879... (STOP if not).
2. Snapshot/backup per standing window procedure.
3. Rename live `plugins/ExcelDowned-1.0.3.jar` to
   `plugins/ExcelDowned-1.0.3.jar.bak` (rollback copy, kept in place).
4. Copy the candidate in as `plugins/ExcelDowned-1.0.4.jar`.
5. Insert the config block above into
   `plugins/ExcelDowned/config.yml` (back up the config first).
6. Start server.

## Rollback

Remove `ExcelDowned-1.0.4.jar`, rename
`ExcelDowned-1.0.3.jar.bak` back to `ExcelDowned-1.0.3.jar`, restore
the backed-up config.yml, restart. The guard writes no data files;
downed.yml and records are untouched by the fix.

## Boot-verify lines to check

- Plugin list shows `ExcelDowned (1.0.4)`; the enable/ready line
  carries build `EXC-DOWNED-1.0.4-WRV-20261123-01-wandguard`.
- SEVERE count = 0.
- No player-facing behavior change - no What's New / website /
  Guide surface owed.

## Proof on record

- Unit suite 84/84; deterministic double-build byte-identical.
- Mirror repro (Homebase mirror, throwaway offline player
  WandProbe, fresh mage + beginner wand; full account in
  ~/workspace/downed-wand-fix-20261123/RECORD.md): the DEFECT
  trigger reproduced - downed by husks, logout while downed
  (health floored at 1.0), bleed-out waited out offline, relog:
  wand GONE (Leg A). Controls: plain logout/relog keeps the wand
  (Leg D); cast + plain relog keeps it (Leg E). The mirror could
  NOT validate the fix end-to-end: the server's Magic is the
  WRV-patched build (byte-identical to live, b78f03b8...) whose
  wand-load gate (wrv.magic.level.RawWandLoadBridge) throws
  "raw item baseline not durably confirmed" for a command-minted
  wand on a fresh mage, and a throwaway wand cannot be brought
  into the registered state real player wands are in - so on the
  mirror the loss runs through that gate, a path real wands never
  take, and neither half can be exercised against it (the
  MagicGate repair's wand construction is itself gate-blocked;
  Downed's name restores are no-ops against a gate throw). The
  live defect path - contaminated owner in Magic's save, deleted
  by owner validation at login before the bleed-out death - is
  evidenced in the wand-restore record and is the path this fix
  closes: quit/join name restores at EventPriority.LOWEST run
  before every other plugin's quit/join handling and before
  Magic's next-tick wand checks, so the marked name is never the
  name Magic saves or validates against. Unit tests pin the
  mark composition to the incident string byte-for-byte.
