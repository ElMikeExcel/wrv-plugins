# Wand-Eating Seam — Hardening Build (D214 family) — Nov 24, 2026

**Lane:** HARDENING BUILD (parent-dispatched). The Renewal (Wand) has been lost
TWICE to this defect family and restored twice by playerdata surgery
(~/workspace/wand-restore-20261123/RECORD.md, ~/workspace/wand-restore-20261124/RECORD.md).
This lane closes the seam: mechanics from primary sources first, design second,
reproduce-then-fix on the mirror as the gate, then build + file. NO install —
the failsafe takes filed packets in the next window.

**Status:** mechanics established (below) → design chosen → repro/build pending.

## 1. The seam, in code terms (primary sources)

Sources read: the LIVE Magic jar pulled fresh from the PC this lane
(`evidence/Magic-11.2.5-wrvnpe1-live.jar`, 7,662,464 B, SHA-256 `b78f03b8…`),
decompiled with CFR 0.152; ExcelDowned 1.0.4 source
(`~/workspace/downed-104-wandfix-20261123/work/build-1.0.4/`); ExcelMagicGate
0.3.4 source (`~/workspace/magicgate-rebase-20261124/build-0.3.4/`).

### 1a. Live Magic is a WRV fork of Magic 11.2.5 — and the fork does not touch this seam
- Live jar `Magic-11.2.5-wrvnpe1.jar` ≠ stock `Magic-11.2.5.jar` (different hash).
  Decompiled diff of the three seam classes: `PlayerController` and `Mage` are
  IDENTICAL to stock; `Wand` differs only in the fleet's learned-spells/lore/
  evidence bridges (`LearnedHooks`, `WandLoreBridge`, `RawWandLoadBridge`) —
  no diff hunk touches `takeOwnership`, the `activate` bound-owner check, or
  `canUse`. The stock-11.2.5 analysis below therefore holds on live.

### 1b. The absorption seam (code-proven): a display-name-bound wand takes its owner from the DISPLAY name
- `Wand.takeOwnership(Player)` (Wand.java:1110–1128):
  `ownerName = boundDisplayName ? player.getDisplayName() : player.getName()`;
  `owner = ChatColor.stripColor(ownerName)`; writes `owner`/`owner_id`
  properties onto the item and saves state.
- `Wand.activate(Mage, boolean)` (Wand.java:~5525–5533): for a bound wand,
  `mageName = ChatColor.stripColor(mage.getPlayer().getDisplayName())`
  (ALWAYS the display name here); if `ownerId == mage.getId()` and
  `owner != mageName` → treated as a rename → `takeOwnership(player)`.
- MikeExcel's wand IS display-name-bound (empirically proven Nov 23: its stored
  owner became "✝ DOWNED MikeExcel" while he was downed). So **any activation
  of the wand while the display name carries ExcelDowned's mark re-stamps the
  wand's owner to the marked name** — in the carried item's NBT and in the
  mage record alike.
- ExcelDowned's mark: `DownedMark.markedDisplay(name)` =
  `§c✝ DOWNED §r<name>` (DownedMark.java:34), applied by
  `ExcelDowned.applyDownedState` via `setDisplayName`/`setPlayerListName`
  (ExcelDowned.java:415–416).

### 1c. Magic's own owner validation is UUID-only (code-proven) — there is no name-based rejection in Magic
- `Wand.canUse(Player)` (Wand.java:5809–5825) compares `ownerId` against the
  mage identifier / player UUID only. The owner NAME is never validated.
- Full read of the join/quit/death-adjacent Magic code (PlayerController,
  Mage.load/onLoad/onPlayerQuit, Wand.activate/deactivate/checkId,
  InventoryController, ItemController) found **no code path that removes a
  carried wand over its owner string**. The exact deleting hand therefore
  remains an inference (both restore records label it the same way); the
  mirror repro (§3) localizes it empirically by staged on-disk reads —
  playerdata + mage file after the hard kill (logout side) vs after the
  rejoin (login side) vs after the bleed-out death (death side).

### 1d. Join/quit ordering on the live stack (code-proven)
PlayerJoinEvent handler priorities:
- ExcelDowned `onJoinUnmark` — **LOWEST** → `unmarkForHandoff` (restores the
  saved real name, but ONLY if the in-memory `savedNames` map still holds it;
  no-op after a server restart).
- ExcelMagicGate `onJoinRepairWandOwner` — **LOWEST** → `WandOwnerRepair`
  (heals a CARRIED wand whose owner is exactly "✝ DOWNED <own name>").
- ExcelDowned `onJoin` — **NORMAL** → expired session: `finalizeDeath`;
  live session: `applyDownedState` — **which RE-MARKS the display name**.
- Magic `PlayerController.onPlayerJoin` — **HIGHEST** → `getMage(player)`
  (mage load) → `Mage.onLoad` → `checkWand()` (+ a finish-load task 1 tick
  later). Any held-wand activation in this window runs the §1b rename check
  against the CURRENT display name.
- MagicGate's delayed recheck (+40 ticks) re-runs the repair and the floor
  reconcile — after Magic's checks; it can observe a loss, never prevent one.

**The open window (1.0.4's residual hole):** on a rejoin into a still-live
downed session, Downed re-marks the name at NORMAL — BEFORE Magic's HIGHEST
join processing and before everything that follows (finish-load checkWand,
held-item changes, the bleed-out death itself). The marked name is the
player's identity for the entire post-join world, exactly as in open play.
1.0.4's unmark covers only the quit instant and the pre-NORMAL join instant.

PlayerQuitEvent: Magic's quit handler is ALSO **LOWEST**
(PlayerController.java:961–962 → mage save via `controller.playerQuit`) —
the same priority as Downed's `onQuitUnmark`. Their relative order is
listener-registration (plugin-enable) order, not a guarantee. Nov 24
evidence (mage blob owner stayed clean across the 02:44 hard kill) is
consistent with Downed's unmark currently winning that race on this server;
nothing pins it.

### 1e. The resurrection path exists in Magic's API (code-proven)
- `Mage.restoreWand()` (Mage.java:3425–3458, public API): for every bound
  wand in the mage's `boundWands` map whose TEMPLATE is not found among the
  player's inventory + ender chest items, it gives `wand.duplicate().getItem()`
  (amount 1) back via `giveItem`. Duplication-safe by construction: bound
  wands only, template-presence check first, exactly one item per missing
  template. `boundWands` is populated at mage load from the mage record
  (`Mage.load`, Mage.java:1266–1280) and kept current by `addBound` whenever
  a bound wand is set active/offhand (template must be restorable).
- MikeExcel's wand is in that map: MagicGate's reconcile already detects the
  corpse state from it — "Rank reconcile DEFERRED … bound wand is not a live
  carried item" (MagicFloorHook, DEFERRED_NO_LIVE_WAND) — today logged at
  INFO, once per player+outcome, and acted on by nobody.
- The player-facing `/wand restore` command is NOT this method's only door
  (it was permission-hidden and lostwands-fed in both incidents); the API
  method is directly callable by ExcelMagicGate reflectively, its house
  pattern.

## 2. Design (chosen by the evidence above)

Layered, one change per owning plugin, each layer independently useful:

**ExcelDowned 1.0.5 — prevention: the mark is presentation, applied LAST.**
1. Join re-mark moves from NORMAL to **MONITOR**: `onJoin` keeps session
   expiry/finalize and the non-name downed state (health, slowness, aggro)
   at NORMAL; the display/list-name mark for a live session is applied by a
   new MONITOR-priority join handler. During the whole LOWEST→HIGHEST join
   dispatch the player's name is clean (unmarked at LOWEST), so no plugin's
   login processing — Magic's included — can read the mark as identity.
   Visible behavior unchanged: the mark lands in the same event dispatch,
   before the join is observable by any player.
2. Unmark fallbacks when `savedNames` has no original (server restart, any
   future path that loses the map): at LOWEST join AND LOWEST quit, if the
   player's current display/list name carries Downed's OWN mark prefix
   (DownedMark stripped form), reset both to the raw profile name. This
   makes the guard self-healing instead of map-dependent.

**ExcelMagicGate 0.3.5 — cure + resurrection net + alarm.**
3. Bound-but-not-carried resurrection: in MagicFloorHook's existing
   DEFERRED_NO_LIVE_WAND handling — the branch that already proves
   "mage record binds a wand, no live item carried, NOT parked in the
   Utility Pouch" (the pouch branch is intentionally-parked and must never
   be duplicated) — call Magic's `Mage.restoreWand()` reflectively, once
   per player per login session (latch cleared on quit), behind config
   toggle `wand-owner-repair.restore-missing-bound.enabled` (default on).
   This converts ANY silent deletion in this family — whatever the deleting
   hand turns out to be — into a self-healing event at the next reconcile
   leg (join delayed recheck / watch sweep / activation).
4. Alarm (task leg 4): that same detection now logs at **WARNING** with a
   greppable line naming the silent-deletion signature (bound wand not
   carried, not in pouch, restore attempted + outcome), replacing the
   routine INFO DEFERRED line for the NO_LIVE_WAND outcome only. The
   POUCH deferral stays INFO (it is a designed parking state).

Why not the alternatives: (a) alone (join-order repair races) is what
0.3.4 already does — it cannot resurrect and it races the finish-load
task; (b) alone (Downed mark lifetime) leaves every non-Downed consumer
of a marked name exposed during open play and does nothing for a wand
already eaten; (c) alone leaves the contamination source live. The pair
above removes the join-window exposure at the source AND makes any
residual loss self-healing + loud.

## 3. Repro plan (mirror; the gate — no repro, no filing)

Mirror: `C:\Users\MikeExcel\wrv-testserver\server` (Paper 26.3-245),
bot via ViaProxy bind 25569 → mirror 127.0.0.1:25566, bot-cont.js units.
Substrate planted from live (mirror-only, disposable): current live
playerdata for MikeExcel (wand at Slot 1, restored 04:09), current live
Magic mage record (bound wand, clean owner), live Downed 1.0.4 +
MagicGate 0.3.4 jars, live Magic fork jar; mirror Downed config
bleed-out shortened to 90 s (mirror ONLY — the expiry mechanics are
window-length-independent; disclosed here so nobody mistakes it for a
live change).

Script (unpatched stack first):
1. Bot joins; INV dump — wand present.
2. RCON-summoned husk downs the bot (lethal-hit intercept); INV dump
   while downed — wand present; downed record read.
3. Bot node process HARD-KILLED (Stop-Process) — no clean quit.
4. On-disk reads: playerdata .dat + mage record + downed.yml
   (logout-side evidence).
5. Wait past the 90 s bleed-out; bot rejoins; INV dumps at join, +5 s,
   and after the bleed-out death/respawn; on-disk reads again
   (login/death-side evidence). Mirror latest.log captured throughout.
Verdict: wand absent at any post-rejoin stage = REPRODUCED (with the
stage that lost it named). Then the identical script on the patched
build: wand must survive with owner/mana/spells intact.
Also run the within-window variant (rejoin before expiry — the live
session / re-mark branch) on the patched build.

## 4. Design caveat SETTLED (Nov 24, 2026, ~04:52 CT)

The one open caveat from §2: a bound wand deliberately parked in a WORLD
CHEST presents identically to a lost one (restoreWand checks inventory +
ender chest only). Decision: **ship the net anyway**, for these recorded
reasons: (1) restoreWand is Magic's OWN recovery semantics — it is the
backend of Magic's /wand restore command (verified in
WandCommandExecutor.onWandRestore), so the engine's own design accepts
exactly this tradeoff; the command failed our incidents only because it
was permission-hidden/lostwands-fed for players, not because its
semantics are wrong. (2) Asymmetry: a duplicate is visible and
operator-removable; a silently deleted wand had NO in-game recovery at
all and cost two surgical restores. (3) Every re-issue is loud — WARNING
log + in-chat notice to the player — so a chest-parking surprise
surfaces immediately, never silently. (4) The net is config-gated
(wand-owner-repair.restore-missing-bound.enabled, default true) and the
caveat is written into the shipped config comments and the install
packet.

## 5. Builds (Nov 24, 2026, ~04:50–05:05 CT)

- **ExcelDowned 1.0.5** — work/build-1.0.5/. Changes per §2 items 1–2:
  applyDownedState split (mark optional; join path passes markName=false
  when the guard is on), new DownedListener.onJoinMarkLast at MONITOR →
  ExcelDowned.applyJoinMark, unmarkForHandoff fallback via new
  DownedMark.isMarkedName. Suite: **86/86 pass** (84 prior + 2 new
  isMarkedName tests; version pins rolled forward). Double-build
  byte-identical; fingerprint gate PASS
  (EXC-DOWNED-1.0.5-WRV-20261124-02-marklast).
  Jar SHA-256 `50bd6b890ce5089a0bf79eca64c03d80678c4a6a1ba0f5c2458e0378d952d174`.
- **ExcelMagicGate 0.3.5** — work/build-0.3.5-mg/. Changes per §2
  items 3–4: new pure WandLossNet (signature + attempt gating, token
  WAND-LOSS-SIGNATURE); MagicFloorHook DEFERRED branch split — pouch
  stays INFO, NO_LIVE_WAND-with-record logs WARNING +
  maybeRestoreMissingBoundWand (reflective Mage.restoreWand, once per
  login session, dead-player skip without consuming the attempt);
  ExcelMagicGate.onQuitForgetWandLossNet clears the latch; config
  toggle added with the reason + chest caveat. Suite: **325/325 pass**
  with fixtures (302 without), incl. 10 new WandLossNet checks.
  Double-build byte-identical.
  Jar SHA-256 `1e26c13ea6995bc490524763efceb3980a24357f0379df6844844d08909dfdf1`.

## 6. MECHANISM CORRECTION (Nov 24, 2026, mirror-proven) — supersedes the §1/§2 framing

The §1 mechanism (marked name absorbed into the wand owner at rejoin,
then Magic deletes the wand) is REAL but is NOT the killing step. The
mirror experiments in §7 disproved it as the killer and isolated the
real one:

- Magic's death processing PARKS keep-wands out of the inventory:
  EntityController's death handling calls
  `mage.addToRespawnInventory(index, item)` for every item with NBT
  `keep` (and for any wand while `keepWandsOnDeath`), nulls the slot,
  and removes it from the drops. The parked map IS persisted in the
  mage save (save ~line 1611, load ~line 1354 of the decompiled Mage).
- The wand comes back ONLY via `Mage.restoreRespawnInventories()`,
  called from `Mage.onRespawn()` and from the finish-load path ONLY
  `if (!player.isDead())` (Mage.java ~line 1220).
- A bleed-out death finalized DURING the join dispatch (an expired
  persisted downed session, finalized ~1 s after login) breaks that
  cycle: the parked wand never lands back — not dropped, not carried,
  not in the death record. Both live incidents were exactly this
  shape (deaths ~1–2 s after the return join). Incident 2 had a CLEAN
  mage record, which the mark theory could not explain; the
  death-at-join theory explains both.
- The name-mark contamination remains an aggravator (incident 1's mage
  save absorbed "✝ DOWNED MikeExcel" as the wand owner; §7 NET session
  captured it live again: a wand given while downed was stamped with
  the marked owner). Mark hygiene stays in the build; the death
  deferral is the headline fix.

## 7. Mirror evidence (Nov 24, 2026; mirror = wrv-testserver, Paper 26.3-245)

Substrate: live playerdata + live mage record transplanted to the
bot's offline UUID (1d200ff8-56d4-3c74-8502-d6cd33a86548); mirror
Downed bleed-out effectively 300 s in all runs. Bot logs UTC,
server logs PC-CST (UTC-6). Evidence under evidence/.

| Run | Stack | Scenario | Result |
|-----|-------|----------|--------|
| B | Downed 1.0.4 + MagicGate 0.3.4 (unpatched) | downed, node hard-killed, rejoin AFTER session expiry | Wand GONE from the first inventory dump; DeathRecord "unknown cause (0 dropped, 1 carried)" — compass only. REPRODUCED. (evidence/repro-unpatched/) |
| B2 | Downed 1.0.5 mark-last (-02) + MagicGate 0.3.5 | same expired rejoin | Wand GONE identically (death at join 11:04:31Z). Mark hygiene alone does NOT fix it. (evidence/repro-patched/) |
| E1 | unpatched | plain join, clean quit, rejoin, no downed/death | Wand survives both joins. Control clean. |
| E2 | patched | within-window rejoin into a live downed session | Wand survives; bot re-entered downed, wand at slot 37 throughout. |
| E3 | patched | online bleed-out death + normal respawn | Death fired online 11:17:51Z; wand BACK after respawn (slot 37). Magic's respawn cycle works for normal deaths. |
| B3 | Downed 1.0.5 death-deferral (-03) + MagicGate 0.3.5 | same expired rejoin as B/B2 | PASSED: "✝ You bled out while you were gone" at join 11:33:08Z but NO death; wand present at join dump; death fired ~5 s later (11:33:13Z, the 100-tick deferral); wand present in +3 s, +10 s, post-respawn and final dumps (slot 37, "Renewal (Wand)"). (evidence/repro-patched-v2/) |
| NET | patched, Marvin-substrate transplant | carried bound wand deleted via namespaced clear | Net could NOT be exercised on the mirror — see §8. Deferred death with a full 25-item inventory verified en route (DeathRecord "0 dropped, 25 carried", wand restored at respawn). |

## 8. The 0.3.5 net leg — mirror verdict: BLOCKED BY SUBSTRATE (honest limitation)

The net (WARNING escalation + restoreWand re-issue) could not be
exercised end-to-end on the mirror, for a named reason with log
evidence (evidence/server-log-net2.txt): the mirror's Magic fork
REJECTS transplanted bound-wand records at load — "Failed to load
bound wand for MikeExcel" fired for Mike's transplanted record AND
for all four of Marvin's (mirror-native record, transplanted only in
UUID). `boundWands` therefore loads EMPTY on every transplant, the
loss signature can never be true on this substrate, and MagicGate
falls back to the non-signature INFO DEFERRED line. A freshly-given
wand (mgive) does not enter boundWands either (verified: record
stayed 3,537 B, no `wands:` section). On LIVE the record demonstrably
persists through losses (both incidents; 0.3.4 logged the DEFERRED
detection state at the real Nov 24 loss, 02:54:37) — the net's
trigger state is real there; only the mirror cannot reproduce the
record side. The net leg therefore ships on: (a) 10 dedicated
WandLossNet unit checks inside the 325/325 suite; (b) live-detection
evidence from 0.3.4; (c) restoreWand verified as the backend of
Magic's own /wand restore (decompiled WandCommandExecutor).
The PREVENTION leg (1.0.5 death deferral) — the fix for the proven
killer — is mirror-proven end-to-end (run B3 above).

## 9. FINAL builds (supersede §5's Downed numbers; §5's MagicGate numbers stand)

- **ExcelDowned 1.0.5 FINAL** — build EXC-DOWNED-1.0.5-WRV-20261124-03-deathdefer.
  Jar SHA-256 `233d20b8fd6c099f0ee22fa9c3974aca837c1ca37fa0889354e8ece0dbf17f09`,
  143,021 B. Suite **87/87** (86 in §5 + joinDeathDelayShipsAt100Ticks
  pin). Double-build byte-identical; fingerprint gate PASS.
  Changes vs §5: (1) DEATH DEFERRAL (headline) — DownedListener.onJoin
  expired branch calls plugin.deferExpiredFinalize(p) instead of
  immediate finalizeDeath; new ExcelDowned.deferExpiredFinalize +
  deferredFinalize set + isFinalizeDeferred; stateTick skips deferred
  players; the session is NOT consumed until the death fires
  (relogging delays, never dodges; the task fires at
  +joinDeathDelayTicks, only if the player is online and the session
  is still expired). New config key
  `join-expired-death-delay-ticks: 100` (0 = legacy immediate death).
  Void-death finalize in the damage handler untouched. (2) §5's
  mark-last + unmark fallback, unchanged. DESIGN-1.0.5.md rewritten
  to lead with the deferral and the corrected mechanism; plugin.yml +
  config.yml carry the reason (two losses, mirror proof).
  The §5 -02-marklast build (SHA 50bd6b89…) is SUPERSEDED — do not file.
- **ExcelMagicGate 0.3.5** — as §5: jar SHA-256
  `1e26c13ea6995bc490524763efceb3980a24357f0379df6844844d08909dfdf1`,
  255,175 B, 325/325 with fixtures (302 without). Net limitation: §8.

## 10. Mirror restoration (Nov 24, 2026)

Mirror stopped via RCON and restored to its as-found state from
C:\Users\MikeExcel\muse-tools\wh1-backup\ (verified after restore):
ExcelDowned-0.1.4.jar back as the only Downed jar; MagicGate jar back
to 243,885 B as-found content; both configs restored; downed.yml
restored (2,232 B); playerdata + mage records restored for BOTH UUIDs
(7401: 2,013 B / 706 B); rig jars un-held (DungeonRig.jar,
TowersRig-0.1.0.jar). Mirror left STOPPED. Known residue: 2–3
lane-summoned husks persist in the mirror world save near the arena
(-1369.5, 70, -348.5) — the mirror is a disposable test substrate and
prior lanes left comparable residue; noted here so it is not a
surprise.

## Log
- 04:13 CT Nov 24 — lane opened; both restore records read in full.
- 04:15–04:35 CT — sources read: Downed 1.0.4 (listener + mark + unmark),
  MagicGate 0.3.4 (repair + matcher + floor-hook DEFERRED), live Magic jar
  pulled + decompiled (stock diff: fork does not touch the seam),
  join/quit priority map established, `Mage.restoreWand()` found.
- Design chosen (§2). Repro next.
- ~04:52 CT — chest-parking caveat settled (§4). Builds green (§5).
  Mirror repro session next: unpatched fail first, then patched pass +
  within-window variant.
- ~05:48–05:54 CT — run B (unpatched): loss REPRODUCED on the expired
  rejoin; on-disk playerdata after the hard kill still held the wand
  with a CLEAN owner and zero "DOWNED" strings in the mage record —
  the mark theory's killing step put in doubt.
- ~06:04 CT — run B2 (mark-last build): loss reproduced identically.
  Mark hygiene alone is not the fix. Decompiled Magic's death/respawn
  cycle (EntityController park + Mage.restoreRespawnInventories gated
  on !isDead) — mechanism corrected (§6): the killer is the
  death-at-join.
- ~06:17–06:33 CT — controls E1 (clean), E2 (within-window rejoin,
  wand survives), E3 (online death + respawn, wand returns).
- ~06:33 CT — run B3 (revised 1.0.5 with death deferral): PASSED —
  death deferred ~5 s past join, wand survives every dump.
- ~06:34–06:42 CT — NET attempts: mgive wand never entered
  boundWands; Marvin-substrate transplant's bound wands all rejected
  at load by the fork. Net leg verdict recorded (§8).
- ~06:43 CT — mirror stopped + restored to as-found state (§10).
- Filed per D212: INSTALL-PACKET.md + RECEIPTS.md in this folder for
  both plugins; pointer-stub PENDING entries appended to
  ~/workspace/wrv-install-queue/MANIFEST.md. NOT installed (failsafe
  takes it next window).
