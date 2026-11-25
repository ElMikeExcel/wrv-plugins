# PENDING - ExcelDowned 1.0.5 + ExcelMagicGate 0.3.5 (wand-loss hardening pair)

Filed Nov 24, 2026 (D212). Parent seat installs; this lane did NOT install.
Lint: 7817 B / max line 90 / non-ASCII 0

## What this fixes

The Renewal (Wand) was lost TWICE (Nov 23 and Nov 24, 2026) and
restored twice by surgical playerdata edits. Records:
~/workspace/wand-restore-20261123/RECORD.md and
~/workspace/wand-restore-20261124/RECORD.md. This lane
(~/workspace/wand-hardening-20261124/RECORD.md) established the exact
mechanism from primary sources and mirror experiments:

- Magic's death processing PARKS keep-wands out of the inventory
  (mage.addToRespawnInventory) and returns them only via
  restoreRespawnInventories(), which the finish-load path calls only
  when the player is NOT dead.
- A bleed-out death finalized DURING the join dispatch (an expired
  persisted downed session, finalized ~1 s after login) breaks that
  cycle: the parked wand never lands back - not dropped, not carried,
  not in the death record.
- The downed name mark being absorbed into the wand owner string is
  real (incident 1) but is an aggravator, not the killing step:
  incident 2's mage record was clean, and on the mirror the
  mark-hygiene build alone still lost the wand (run B2), while the
  death-deferral build passed the identical scenario (run B3).

## Changes

### ExcelDowned 1.0.5 (jar swap + config merge)

1. DEATH DEFERRAL (headline): when a player rejoins with an EXPIRED
   persisted downed session, the bleed-out death is deferred by
   config `join-expired-death-delay-ticks` (default 100 ticks = 5 s)
   instead of firing inside the join dispatch. The player still gets
   the "bled out while you were gone" message at join; the session is
   NOT consumed until the death fires, so relogging delays but never
   dodges it. The deferred task fires only if the player is online
   and the session is still expired. This moves the death out of the
   join-processing window so Magic's respawn-inventory cycle runs
   normally. Void-death finalize is untouched.
2. Mark hygiene (aggravator fix, carried from the design): on the
   persisted-session rejoin the downed name mark is applied LAST
   (MONITOR priority, after every other plugin's join handling) and
   only when the wand-owner guard is enabled; the quit-unmark gains a
   marked-name fallback so a rejoin never presents a marked name to
   other plugins' identity checks. Gated on the existing toggle
   `wand-owner-guard.unmark-on-quit` (live value: true).
3. Config merge (do NOT replace the live config): add the single key
   `join-expired-death-delay-ticks: 100` under the existing settings
   (placement per the shipped config.yml in the source zip) and the
   updated wand-owner-guard comment block. All live values stay as
   they are (bleed-out 300, guard true).

### ExcelMagicGate 0.3.5 (jar swap, live filename ExcelMagicGate-0.2.0.jar + config merge)

1. LOUD NET: the bound-but-not-carried detection (a mage record holds
   a bound wand but no live wand is carried - the corpse state of
   this defect family, which 0.3.4 logged as a routine INFO DEFERRED
   line at the real Nov 24 loss) now logs a greppable WARNING with
   the token WAND-LOSS-SIGNATURE and, once per login session, calls
   Magic's own Mage.restoreWand() (the backend of /wand restore) to
   re-issue the missing bound wand, logging recovery at WARNING and
   notifying the player in chat. Dead players are skipped without
   consuming the attempt. Toggle:
   `wand-owner-repair.restore-missing-bound.enabled` (default true).
   CAVEAT (also in the shipped config comments): a bound wand
   deliberately parked in a WORLD CHEST presents identically to a
   lost one (restoreWand checks inventory + ender chest only), so
   the net may re-issue it - a visible, operator-removable duplicate,
   accepted over a silent unrecoverable deletion (RECORD.md s.4).
2. Everything else is 0.3.4 behavior unchanged (rank system, repair,
   credit hooks, seal watch).
3. Config merge (do NOT replace the live config): add the key
   `wand-owner-repair.restore-missing-bound.enabled: true` under the
   existing wand-owner-repair section.

## Candidates

- ~/workspace/wand-hardening-20261124/ExcelDowned-1.0.5.jar
  - 143,021 B - SHA-256
  233d20b8fd6c099f0ee22fa9c3974aca837c1ca37fa0889354e8ece0dbf17f09
  - Build: EXC-DOWNED-1.0.5-WRV-20261124-03-deathdefer
  - Tests: 87/87 PASS. Double-build byte-identical.
  - Source: ~/workspace/wand-hardening-20261124/work/build-1.0.5/ ;
    source zip ~/workspace/wand-hardening-20261124/
    ExcelDowned-1.0.5-source.zip - SHA-256
    ddcbfdc96d0fc537f33c9f0947c925d9d6ab21c512572d491c66091a9b29a034
- ~/workspace/wand-hardening-20261124/ExcelMagicGate-0.3.5.jar
  - 255,175 B - SHA-256
  1e26c13ea6995bc490524763efceb3980a24357f0379df6844844d08909dfdf1
  - Build: EXC-MAGICGATE-0.3.5-WRV-20261124-02-wandnet
  - Tests: 325/325 PASS with fixtures (302 without). Double-build
    byte-identical.
  - Source: ~/workspace/wand-hardening-20261124/work/build-0.3.5-mg/ ;
    source zip ~/workspace/wand-hardening-20261124/
    ExcelMagicGate-0.3.5-source.zip - SHA-256
    b0ad8627da07b3df58505fbcb5e188cc751d3376fd268c2bf020a89da1205c3b

## Live gates (verify before swapping; abort on mismatch)

- ExcelDowned-1.0.4.jar live: 141,173 B, SHA-256 starts C4C90D62
  (full hash in RECEIPTS.md).
- ExcelMagicGate-0.2.0.jar live (internal 0.3.4): 252,773 B, SHA-256
  starts 624F6F00 (full hash in RECEIPTS.md).
- Gates read plugin.yml / hashes, never filenames. MagicGate ships
  under the live filename ExcelMagicGate-0.2.0.jar - never rename.

## Mirror proof (the gate - summary; full table in RECORD.md s.7)

Mirror: wrv-testserver (Paper 26.3-245), live playerdata + mage
record transplanted to the bot's offline UUID.
- Unpatched (1.0.4 + 0.3.4): expired-session rejoin after a hard kill
  - wand GONE from the first inventory dump. REPRODUCED.
- Mark-hygiene-only build (-02): identical loss. Not the fix.
- Final pair (1.0.5 -03 + 0.3.5): identical scenario - death deferred
  ~5 s past join, wand present in every dump incl. post-respawn.
  PASSED. Controls: clean rejoin (E1), within-window rejoin (E2),
  online death + respawn (E3) all clean.
- LIMITATION (stated plainly): the 0.3.5 net leg (WARNING +
  restoreWand re-issue) could NOT be exercised end-to-end on the
  mirror - the mirror's Magic fork rejects transplanted bound-wand
  records at load ("Failed to load bound wand"), so the net's
  trigger state cannot exist there (RECORD.md s.8). The net ships on
  its unit tests (10 dedicated WandLossNet checks in the 325 suite),
  on 0.3.4 having logged the exact detection state at the real live
  loss, and on restoreWand being Magic's own /wand restore backend
  (decompiled). The prevention leg above is the fix for the proven
  killer and IS mirror-proven.

## Install + verify + rollback

- Standard window discipline: verified-empty gate, snapshot first,
  whitelist per protocol. Swap both jars, apply both config merges,
  boot, confirm: Downed ready line build
  EXC-DOWNED-1.0.5-WRV-20261124-03-deathdefer; MagicGate ready line
  build EXC-MAGICGATE-0.3.5-WRV-20261124-02-wandnet; SEVERE 0.
- Post-install watch (parent): the first real expired-session rejoin
  or any WAND-LOSS-SIGNATURE line in the live log is the live proof
  point for the net leg; grep for WAND-LOSS-SIGNATURE in the nightly
  audit until it has fired once or 7 days pass quietly.
- Rollback: restore the pre-install jars + configs from the window
  backup (Downed back to 1.0.4, MagicGate back to 0.3.4 content).
  No data-format changes in either plugin; downed.yml and earned.yml
  are untouched by the swap.
- Markers: ~/workspace/wand-hardening-20261124/INSTALLED-DOWNED.txt
  and ~/workspace/wand-hardening-20261124/INSTALLED-MAGICGATE.txt.
