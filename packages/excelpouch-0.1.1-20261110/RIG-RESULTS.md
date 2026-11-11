# RIG RESULTS — ExcelPouch 0.1.1 (console admin)

Rig: copy of the 0.1.0 pouch rig server (`~/workspace/excelpouch/
scratch/rig-server/` — Paper 26.2 + Magic 11.2.5 + DeluxeMenus +
PlaceholderAPI + ExcelMenuJoin 1.4.0) with the jar swapped to
ExcelPouch-0.1.1.jar (SHA-256 verified b4cdfa20…f9c4 in the rig
plugins folder before boot) and the driver swapped to the new
PouchAdminRig (driver-src in this folder). Offline mode, rig
world, fresh ExcelPouch data dir. Full final log: `boot.log` in
this folder. Server reached Done, was exercised from the REAL
console sender (`Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
…)` for every admin act), and stopped cleanly ("Disabling
ExcelPouch v0.1.1").

## Boot

- "Enabling ExcelPouch v0.1.1" present; plugin enabled with the
  0.1.0 boot line shape (2 pockets, both allowlist kinds).
- SEVERE: 0 in the whole boot + exercise + shutdown.

## Behavior exercise — 44/44 PASS, 0 FAIL (POUCHADMIN>> tally in boot.log)

| Brief §6.2 item | Checks | Result |
|---|---|---|
| (a) status: online / offline-seeded / never-joined / fresh UUID | a-offline-status-creates-nothing, a-offline-store-not-retained, a-seeded-offline-pocket-intact | PASS — data dir byte-identical after all four reads; reads retain no store (no autosave minting); the seeded offline store kept its compass |
| (b) global status + reload after slots edit | b-reload-applies-slots, b-reload-deletes-nothing, b-reload-back-to-2 | PASS — slots 2→3 applied by console reload, all 3 pockets (incl. the hidden compass) survive, restored to 2 |
| (c) return | c-return-by-kind, c-return-flips-holdsKind, c-return-full-inventory-stays-parked, c-return-after-space, c-return-repeat-is-noop | PASS — hidden compass returned by kind; full inventory leaves the item parked exactly as-was ("Inventory full — item left parked in pocket 2."); repeat is the already-empty no-op |
| (d) clear ± confirm | d-clear-without-confirm-changes-nothing, d-clear-with-confirm, d-rescue-byte-equal, d-clear-flips-holdsKind | PASS — no mutation and NO rescue file without `confirm`; with confirm the pocket empties and the rescue file byte-equals the removed stored entry |
| (e) duplicate wands (§4.6 worked example) | e-kind-many-refused, e-preview-changes-nothing, e-clear-duplicate-by-pocket, e-rescue-byte-equal, e-holdsKind-still-true | PASS — `clear AdmA magic_wand confirm` refused with the pocket list; preview for pocket:1 changed nothing; `clear pocket:1 confirm` removed exactly that copy with a byte-equal rescue; the surviving wand still answers holdsKind |
| (f) GUI-open race | f-pouch-open, f-return-under-open-gui, f-no-resurrection-after-close, f-clear-under-open-gui, f-clear-no-resurrection | PASS — return and clear both land while the target's pouch GUI is open; after the target closes the GUI the item does NOT reappear in the store |
| (g) broken / corrupt stores | g1-corrupt-file-never-overwritten, g2-broken-store-refuses-mutation | PASS — see limitations for how each half was fixtured |
| (h) sealed pocket | h-return-refuses-sealed, h-clear-sealed-succeeds, h-sealed-rescue-byte-equal | PASS — status reports the pocket UNREADABLE (log-verbatim), return refuses, clear+confirm succeeds and the rescue copy holds the sealed bytes verbatim |
| (i) non-op denial | i-nonop-admin-denied | PASS — a non-op fake player's clear AND return attempts changed nothing (proven by state; a fake player's chat is not echoed to the log — same stated limitation as the Downed rig) |
| (j) ExcelMenuJoin regression | j-parked-compass-blocks-gives | PASS — after admin status/return/clear cycles, a parked compass still blocks both the respawn give and `/compass` |
| (k) D10 negative tests | k1-unwritable-rescue-refuses, k2-post-close-change-aborts, k2-no-new-rescue | PASS — rescue path blocked by a regular file: clear refused, wand pocket intact; pocket swapped (wand→compass) inside the GUI during the close-sync: clear aborted, the synced item was NOT deleted, no rescue written |
| offline target (return/clear) | offline-status-readonly, offline-return-refuses, offline-clear-preview-noop, offline-clear-works, offline-clear-holdsKind-flips | PASS — offline status byte-identical on disk; return refuses with the design's one-liner (log-verbatim); clear preview is a no-op; clear+confirm empties the pocket with a byte-equal rescue and flips holdsKind |

Console message shapes verified verbatim in boot.log (the log's
encoding renders "—" as "?"): the global status trio ("ExcelPouch
0.1.1 — slots: 2 (visible), allowlist: [menu_compass, magic_wand],
wand key: 'wand'" / Stores line / "PouchQuery service: registered
(holdsKind live for ExcelMenuJoin)"), per-pocket lines in store
order incl. empty and "[HIDDEN by config shrink]" pockets and the
duplicate-wand pair, the BROKEN line ×3 (status, return, clear),
the UNREADABLE status line, "Inventory full — …", the MANY
refusal naming "pocket 0, pocket 1", both "Would clear …" preview
lines, the no-file line for a fresh UUID, the offline-return
refusal, and the §4.0 audit lines ("Pouch admin return: actor=
CONSOLE target=… selector=… kind=… bytes=…" ×3, "Pouch admin
clear: …" ×5).

## Iteration history (kept honest)

- Run 1 (38/44, log kept as `boot-run1.log`): the (f) GUI-race
  acts aborted with "changed while … GUI was closing" and the
  offline-by-name segment could not resolve the removed fake
  player. Two distinct causes, both fixed and re-proven:
  (1) byte-strict post-close revalidation is unworkable for wands
  — the GUI round-trip re-serializes the item and Magic refreshes
  volatile wand fields (mana_timestamp; the 0.1.0 rig's own
  finding), so the revalidation now compares the §4.1 item
  identity (kind / material / amount / display name / wand owner)
  — see the BUILD-RECORD deviation note; the k2 different-item
  swap still aborts. (2) NMS fake players placed via
  placeNewPlayer do not land in the server usercache, so the
  offline segment now targets by UUID string (design §4.0 path 3);
  the by-name rejection lines from run 1 are in boot-run1.log.
- Run 3 (43/44): the only failure was a DRIVER bug —
  rescueContentFor returned the earliest pocket-0 rescue (from
  (f)) instead of the latest (from (h)); verified on disk that
  the (h) rescue byte-matched the sealed entry. Driver fixed to
  read the latest matching rescue; final run is 44/44 against
  the shipped jar (the audit-log kind=null→"unknown" polish was
  also rebuilt in before this run, so the rigged jar IS the
  shipped jar, hash-verified in the plugins folder).

## Rig limitations, stated plainly

- (g) broken-store fixture, two halves. A syntax-corrupt YAML
  does NOT trip PouchStore's `broken` flag on Paper:
  `YamlConfiguration.loadConfiguration` swallows syntax errors
  and returns a blank config (desk-verified against the Paper
  API jar before the rig; pre-existing 0.1.0 load behavior,
  unchanged by 0.1.1). So (g1) seeds a corrupt file and proves
  the operator-safety property that matters — status + a clear
  attempt leave the file byte-identical, mint no `.corrupt`, and
  write no rescue (the non-retaining read + no-save evict is
  what protects it). The BROKEN branch itself (status wording,
  return/clear refusal) is proven in (g2) against a store whose
  `broken` flag the driver set by reflection — the same state
  `load(...)` produces when a read does fail.
- The non-op denial is proven by STATE (targets untouched across
  both attempts): fake-player chat is not echoed to the server
  log, so the denial message text is read in the handler source,
  not observed in boot.log.
- No Bedrock form round-trip: 0.1.1 adds no player surface at
  all, so there is nothing Bedrock-facing to prove.
