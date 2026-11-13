# RIG RESULTS — ExcelWandBars 0.3.17 (Shape A) on the real-Magic mirror

Nov 12, 2026, ~5:36–6:05 PM CST. **This rig is the evidence of record
for the layout gates.** The 0.3.16 desk sim is disqualified for this
defect class (DIAGNOSIS-ROUND3: it never ran a real Wand object).

## Rig

- PC test-server mirror `C:\Users\MikeExcel\wrv-testserver\server\`
  on DESKTOP-IL05DQR (Paper 26.3 build 245, same as live), full live
  plugin set minus ExcelBuildMode (held out: its join proof fails on
  the mirror's playerdata-identity check — a mirror storage issue,
  unrelated to WandBars; jar parked in `plugins-disabled-rig\` during
  the rig and restored after). Mirror MagicGate upgraded to the live
  0.3.2 jar so the mirror matches live.
- Real Magic plugin + real `Wand` objects throughout. The bot
  **WandRigBot** (offline UUID c899ff87-eade-326b-b7fd-d96fa07296ac)
  is a raw minecraft-protocol client (26.1 schema shimmed to protocol
  777) driving join / held-slot churn / swing / relog / quit.
- Fixture: Mike's actual saved mage data
  (`fixtures/mike-mage-live-20261112.dat` from the 0.3.16 build),
  transformed to the bot's identity. The student record's wand:
  template `student`, path `apprentice`, **80 spells**, 21-slot
  loadout in Mike's live order. Its item still carries the fossil
  counters (hotbar_count 3 / hotbar_inventory_count 2) — inert under
  path ownership, and 0.3.17 neither reads nor writes them.
- MagicGate earned seeded per phase to match the path under test
  (Apprentice 1210 — Mike's exact live standing; Master 2500;
  Beginner 50). Without the seed, MagicGate's reconcile re-derives
  the path from earned casts (seen in STEP 1 run A: demotion to
  Beginner, effective 1 — itself a confirmation that the PATH, not
  the wand item, sets the page count).
- WandRig (rig-only plugin) places fixture wands and probes the real
  Wand: effectiveHotbarCount (path-resolved), raw props, path, spell
  count, built hotbars. It also installs a **rig-local
  RawWandLoadBridge owner** (confirm-all, same scope shape WandBars
  registers): the mirror's ExcelMakersMark cannot start its history
  archive (mirror storage degradation — the parent lane's owed
  diagnosis), which otherwise leaves every scoped wand load throwing
  `managed raw load owner unavailable`. The bridge owner gates only
  the practice-provenance ledger, which the layout code never
  consults; the Wand/path/layout machinery exercised is 100% the
  shipped code path. Disclosed here so the rig's limits are explicit.

## STEP 1 — REPRODUCE (shipped 0.3.16 on the rig) — ✅ failure reproduced

Apprentice-seeded bot, pristine student fixture wand, live sequence
(hold-activate → deactivate/checkWand churn → open attempts):

- Probe (stable across churn): `template=student
  effectiveHotbarCount=2 prop_hotbar_count=2 prop_hic=2
  prop_path=apprentice spells=80 builtHotbars=2`.
- Verdicts: `FAILED [branch=layout layout: loadout on-bar=21 book=59
  pages=2/3 ok=false … hic=2 firstDiff=cure at 75, want 18
  template=student]` — after RETRYING cycles whose packed book
  region **re-offset between attempts** (firstDiff moved:
  repairequipment@75 → wrv_spark@75; book indices rotated
  eat_food 24→82, gills 28→82): the layout is never a fixed point,
  exactly the round-2/round-3 signature on real Magic.
- `Tap bar tryOpen(hold)` and `tryOpen(admin)`: refusing,
  `layoutOk=false pages=2/3`.
- Full excerpts: `rig-assets/step1-log-excerpts.txt`.

**The diagnosis held on real Magic; the fix was credited to proceed.**

## STEP 2 — FIX + REPROVE (0.3.17, same rig, same sequence) — ✅

- Boot: `Enabling ExcelWandBars v0.3.17` / build
  `EXC-WANDBARS-0.3.17-WRV-20261112-01-9d0b77`.
- Fresh pristine student wand: style verdict
  `loadout on-bar=14 book=66 pages=2/2 ok=true` with the Shape A map
  (mend=0 … throwmagictorch=14 on-bar; **repairequipment=16,
  wrv_spark=17, cure=18, shell=19, repairitem=20, cloak=21, earth=22**
  parked; remaining 59 in wand order from 23), `hic=2` (fossil,
  informational). A wand already carrying 0.3.16's churned state
  verified `apply ALREADY` against the same target.
- **Tap bar opened:** `tapbar … open -> opened wand open=true`
  (admin path), probe after: `invOpen=true`. On the second cycle
  (relog + slot churn + swing) the bar was already open from the
  bot's own gestures — log: `Tap bar tryOpen(admin) for WandRigBot
  -> already open layoutOk=true pages=2/2`.
- **Fixed point:** post-relog verdict identical map, `ok=true` —
  a second consecutive apply/verify cycle produces zero diffs.
- **Accounting:** the saved mage data after both cycles
  (`rig-assets/bot-mage-after-0317.dat`): student wand's
  spell_inventory = **80 entries, key set identical to the fixture**
  (nothing lost, nothing added); on-bar picks at 0–6/8–14; the seven
  page-3 picks at exactly 16–22.
- Note: tryOpen's separate inventory gate (`other inventory open
  (CHEST)`) fired transiently during churn while other mirror
  plugins presented menus to the bot; WandRig's `closeinv` (the ESC
  a real client sends) cleared it. That gate is pre-existing
  behavior, not part of this defect — with the layout gate green it
  no longer masks the refusal reason.

## STEP 3 — REGRESSIONS — ✅ all pass

- **Master path** (earned 2500, path=master): probe
  `effectiveHotbarCount=3 … spells=80 builtHotbars=3`; verdict
  `on-bar=21 book=59 pages=3/3 ok=true` (book from 24); **opens**.
- **Beginner path** (earned 50, path=beginner): probe
  `effectiveHotbarCount=1 … builtHotbars=1`; verdict
  `on-bar=7 book=73 pages=1/1 ok=true` (book from 8); **opens**.
- **Sneak bars** (student wand, perPage 8): `on-bar=16 book=64
  pages=2/2 ok=true`.
- **ORIGINAL style** (student wand): `on-bar=14 book=66 pages=2/2
  ok=true`; tap bar re-picked after: `pages=2/2 ok=true`.
- Carried unit suites for all styles: 23 suites / 669 checks green
  (TEST-RESULTS.txt).

## Rig incidents (recorded honestly; none touch the verdicts)

1. The first 0.3.17 candidate (`db07038a…`) was rejected by the
   mirror: invalid plugin.yml (a ": " in the new description
   sentence broke the plain scalar). Rebuilt as `949e9cd4…` with the
   YAML parse-verified in the jar. The rig caught a defect the unit
   suites could not — the reason the rig gate exists.
2. Mid-rig, RCON calls began timing out and the mirror was
   hard-restarted twice before the cause was found: the commands
   were going through the LIVE server's RCON client defaults
   (host = the Shockbyte IP) instead of the mirror's own client
   (`mrcon.py`, which reads the mirror's server.properties). No
   packet ever reached the live server (every connect timed out at
   TCP; the live RCON port was never dialed). The mirror's RCON was
   never wedged. Lesson recorded for the packet: rig work uses
   mrcon.py only.
3. The mirror was left restored and OFF (see the build folder's
   cleanup record in BUILD-NOTES' companion packet + the parent
   report): original jars, server.properties, earned.yml and
   MakersMark history restored from `muse-temp\wandrig\backup\`;
   rig plugin/fixtures/data removed; MuseWandRig* tasks deleted.
