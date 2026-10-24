# ExcelTrash 0.2.0 test record (Oct 24, 2026, CDT)

Unit: 13/13 JUnit (v2 store, one-shot take, second take empty, session = one batch, new screen replaces,
after-undo new batch, 0.1.0 file migration, 0.1.0 reading v2 sees expired, downtime shift, purge, Layout).
Jar rebuilt twice byte-identical (sha256 a7c00ba7...abeb1e).

Rig: isolated copy `/workspace/trash-rig` (Paper 26.3-214, port 25951, same plugin set as sbtn-rig minus
Geyser/floodgate, plus a test-only probe plugin `RigTrashProbe` that makes a bot open/break a block through the
normal Bukkit events). Java bots (mineflayer 1.21.11 via ViaVersion). ship1025-rig, tips-rig, sbtn-rig and the
other running rigs were not touched. Every full run starts from 0.1.0 and upgrades to 0.2.0.

| Run | jar | main | stop | rollback |
|---|---|---|---|---|
| run2 | final | 59/59 | 3/3 | 2/2 (vacuous: part A hit a login cooldown) |
| run3 | final | 59/59 | 3/3 | 3/3 |
| extra | final | close-action delete 3/3 | | rollback 3/3 |
| run1 | pre-rename (holder class Session) | 58/59 (1 = test measuring race, fixed in test) | 3/3 | |

Covered (each a PASS line in test-logs/): 0.1.0 pending batch upgraded and returned by /trash undo; second undo
nothing; OK delete + Recover button; OK, close, /trash undo, second undo (/bin undo) nothing; two OKs in one screen
come back together; only the last batch comes back; close/Cancel without OK gives back; undo with full inventory
drops 128 at feet; chest mode: tap moves the real stack, OK, /trash undo returns it INTO the chest, second undo
nothing; Recover button inside chest screen; put back from cursor/red side; 60-click spam then close conserves
every item; close mid-move conserves; other player open DENIED, break DENIED, /trash chest refused; hopper frozen
then works after close; TNT next to it, chest survives; chest open by someone else, Trash refused; chest replaced
under you, screen closes, red-side items to player, nothing copied; double chest 2 pages, delete from page 2,
undo back in; shulker with 10 diamonds inside comes back intact; no shulker into a shulker box; GriefPrevention
claim chest refused and untouched; compass server_menu slot 26 opens the new Trash; chest button switches mode;
death with items on red side drops them normally; ExcelAuditLog `/audit` shows trash + undo rows; server stop
with items on the red side: chest items back in chest, own items back in inventory, nothing deleted; rollback to
0.1.0 with a pending 0.2.0 batch: nothing returned (no dupe).

Not verified: real Bedrock touch / controller through Geyser (no Bedrock client on the box); double-click collect
(mineflayer can't send it; code cancels COLLECT_TO_CURSOR); hard crash (kill -9) with items on the red side;
ExcelShops shop chest interaction (code backs off if another screen opens; not run); live menu file (rig used
the Oct 23 server_menu.fixed.yml copy).
