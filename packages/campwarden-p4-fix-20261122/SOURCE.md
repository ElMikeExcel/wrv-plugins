# SOURCE - Camp Warden P4 door-seal content fix (D188)

CONTENT-FILE FIX, not a plugin: one EliteMobs boss file swapped
on the live server -
plugins/EliteMobs/custombosses/primis/primis_bloodtemple_climb_miniboss_1_p4.yml.
Fix: P4 onRemoveBlockStates extended 15 -> 30 entries (the 15
Door B open entries copied verbatim from P1 onSpawnBlockStates,
appended after the existing 15 Door A restore entries), so on
Camp Warden death in P4 BOTH arena doors restore. No other line
changes (diff-verified). Known limit (accepted, D188): no door
reopens while the boss is alive.
Build folder of record: ~/workspace/camp-warden-fix-20261122/
Files in this package: the fixed yml (of record), the pre-fix
live pull (primis_bloodtemple_climb_miniboss_1_p4.live-pre-fix.yml),
INSTALL-PACKET.md, INSTALLED.txt, RECEIPTS.md,
MANIFEST-ENTRY-DRAFT.md.
Fixed file: primis_bloodtemple_climb_miniboss_1_p4.yml -
3,485 B / SHA-256 831f86d3566bc8ed3968acc256bc32e7afa2240d9609210a711953e67be020b6 / MD5 3af2962cc709cb91d0ad4b77ca8988ee.
Installed live hash verified EXACT against this file
(RUN-RECORD: 831f86d3 / 3,485 B; onRemove census on live = 30).
Pre-fix live file: 2,815 B / SHA-256
7661989fe8c3870838d54bd43b72609b2aa2ef15c88e70c7dafc20ca60cc1f53
(kept live as .bak at install).
Install: INSTALLED Nov 22, 2026, 02:21-02:27 CT by the D189 hand window hand-20261122-0221-installchain (parent seat; chain of 4, one backup snapshot-20261122-022350, emptiness 0 twice, boot Done 26.944s, SEVERE 0) - see INSTALLED.txt and the run record ~/workspace/wrv-install-queue/runs/20261122-0221-hand/RUN-RECORD.md.
Status: Installed. Proof owed: next natural Camp Warden kill
restores BOTH doors (or bot proof). Upstream MagmaGuy report
only after that proof (D188; patch-ours-first).

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-22): name search for the P4 fix
artifacts - results in RECORDS-RECORD.md.
- primis_bloodtemple_climb_miniboss_1_p4.yml (fixed) -> "Source"
  folder (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): ID/size/MD5 in
  RECORDS-RECORD.md (verified EXACT, download-back MD5+SHA-256).
- CampWarden-P4-fix-INSTALL-PACKET.md -> "Source" folder:
  ID/size/MD5 in RECORDS-RECORD.md.
- CampWarden-P4-fix-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 recorded in RECORDS-RECORD.md.
