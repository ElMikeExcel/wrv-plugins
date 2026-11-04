# INSTALL PACKET #3 — Spell fix pass (Familiar cooldown root cause + audit leftovers)

Applies the fixes convicted/named by the full spell audit
`~/workspace/spell-audit/FULL-AUDIT-20261102.md` (desk census + log
phase). Config-only pass: four staged spells.yml files, five
destinations (the s3 file serves BOTH the live file and the stage-3
template — the same layout as packets #1 and #2). No jar changes, no
other config changes.

**This packet chains on packet #2** (installed Nov 2, 2026, hand
window 1:39–2:22 AM CST; its outputs are the current live files and
the base of record). Packet #3's staged files were built FROM packet
#2's outputs — the packet #2 folder was never edited; copies were
taken and only the deltas below applied.

## What changes

1. **Familiar — remove the `entity_parameters.brain` block (s3/s4/s5;
   absent in s2).** Root cause, proven by today's live log (audit, LOG
   PHASE §2): Magic 11.2.5's platform layer throws
   `java.lang.IllegalAccessError` — `MobUtilsBase.removeGoals` →
   protected field `net.minecraft.world.entity.Mob.goalSelector` — in
   `EntityData.applyBrain`, aborting the cast *after* the animal
   spawns but *before* the completion path that registers the cooldown
   and arms the undo. Mike's four casts today errored identically and
   repeated at 8–10 s against a configured 300 s cooldown; the flood
   animals likewise never auto-removed despite `undo: 20000`. The
   entry's `entity_parameters` contained ONLY the `brain` block
   (goals `follow_owner` priority 2, `look_at_player` priority 3), so
   the removal takes the whole 7-line block including the now-empty
   parent key (a bare `entity_parameters:` would parse as a null
   section). **Nothing else in the entry changes** — mana 50,
   cooldown 300000, the 8-type spawn table, range 32, `undo: 20000`,
   `loot: false` are byte-identical (diff + parsed assertions below).
   Functional loss: none. `applyBrain` never completes on this build,
   so familiars already do not follow their owner today; with the
   block gone the cast completes, so the cooldown and the 20-second
   undo engage for the first time.
2. **Boomerang — cooldown 0 → 1000 (s3/s4/s5, wherever present; not
   in s2).** The standing spell floor: no cast cooldown under 1000 ms.
   Mana 30 unchanged.
3. **workbench|4 — cooldown 0 → 2500, mana 0 → 5 (ALL FOUR stage
   files).** The defect was the ZEROING, not the descent: the
   workbench ladder descends by rank — base 20000 / 50, |2 =
   10000 / 30, |3 = 5000 / 10 — each rank cheaper and faster than
   the one below it. |4 continues the ladder (cooldown halving
   10000 → 5000 → 2500; mana 30 → 10 → 5). Restoring the BASE
   values at |4 (this packet's first build) would have made the
   top rank MORE restrictive than |2 and |3, inverting the ladder;
   that build was corrected before install — the staged files in
   this folder carry 2500 / 5. (The key exists — and was zeroed —
   in s2 as well as s3/s4/s5; the audit headline discussed the live
   stages, but the raw files carry it in every stage, so every
   stage is fixed.)
4. **Stage-s5 only — mend and flashheal `cooldown_mage` 941 → 1000.**
   Normalizes the lone cross-stage value drift in the corpus (every
   other stage carries 1000).

**Not changed:** everything else. The five unpaced novelty casts the
audit named (magictorch, rolldice, skating, wearskates, tamepet) were
not in this packet's brief and remain exactly as they were — the
harness still flags them (check 9), on purpose and on record.

## Line-level diff vs packet #2 (complete — this is the entire delta)

### stage-s2 — 2 lines (workbench|4 only)

```diff
@@ -1562,9 +1562,9 @@
   upgrade_required_casts: 100
   upgrade_required_path: master
   parameters:
-    cooldown: 0
+    cooldown: 2500
   costs:
-    mana: 0
+    mana: 5
 glide:
```

### stage-s3 — 10 lines (workbench|4, familiar, boomerang)

```diff
@@ -1737,9 +1737,9 @@
   upgrade_required_casts: 100
   upgrade_required_path: master
   parameters:
-    cooldown: 0
+    cooldown: 2500
   costs:
-    mana: 0
+    mana: 5
 glide:
@@ -4455,13 +4455,6 @@
       horse: 5
       snowman: 5
       iron_golem: 2
-    entity_parameters:
-      brain:
-        goals:
-        - type: follow_owner
-          priority: 2
-        - type: look_at_player
-          priority: 3
     target_type: LivingEntity
     allow_max_range: false
     cooldown: 300000
@@ -5153,7 +5146,7 @@
     hitbox_size: 0.2
     damage: 1
     entity_damage: 2
-    cooldown: 0
+    cooldown: 1000
     right_arm_item: diamond_sword
     location_offset: 0,-1,0
     teleport: true
```

### stage-s4 — 10 lines (identical hunks to s3, at s4 line numbers)

```diff
@@ -1737,9 +1737,9 @@   (workbench|4: cooldown 0 -> 2500, mana 0 -> 5)
@@ -4455,13 +4455,6 @@   (familiar: same 7-line entity_parameters/brain block removed)
@@ -5709,7 +5702,7 @@   (boomerang: cooldown 0 -> 1000)
```

### stage-s5 — 12 lines (workbench|4, familiar, boomerang, mend, flashheal)

```diff
@@ -1825,9 +1825,9 @@   (workbench|4: cooldown 0 -> 2500, mana 0 -> 5)
@@ -4543,13 +4543,6 @@   (familiar: same 7-line entity_parameters/brain block removed)
@@ -5414,7 +5407,7 @@   (boomerang: cooldown 0 -> 1000)
@@ -7108,7 +7101,7 @@
     only_friendly: true
     bypass_confusion: true
     amount: 1.5
-    cooldown_mage: 941
+    cooldown_mage: 1000
     hitbox_size: 0.1
   costs:
     mana: 4
@@ -7192,7 +7185,7 @@
     only_friendly: true
     bypass_confusion: true
     amount: 5
-    cooldown_mage: 941
+    cooldown_mage: 1000
     hitbox_size: 0.1
   costs:
     mana: 15
```

Diff method: `diff -u` of each packet #3 stage file against the
packet #2 output for that stage, run at build time. The s3 hunks are
reproduced verbatim above; s4/s5 hunks are byte-identical in content
to s3's except line numbers and the two s5-only heal hunks (also
verbatim). The familiar hunk shows the brain block removal and
nothing else — the lines around it (`entity_types` weights above;
`target_type`, `allow_max_range`, `cooldown: 300000` below) are
unchanged context.

## Staged files

| staged file | sha256 | bytes |
|---|---|---|
| `install-20261102-packet3/stage-s2/spells.yml` | `0420eb36ea06100aab920543869efc4c3c915694e80690886f7932a4c19f5c78` | 86,719 |
| `install-20261102-packet3/stage-s3/spells.yml` | `a7fcf51e342d62db74340b19e4ee4e92dc37e365ed79ecfdfdbdd48ebe0ad772` | 139,962 |
| `install-20261102-packet3/stage-s4/spells.yml` | `9e4024a8113faf1812c3908daf02a2ee411df388aa1178db2ff95a237e699969` | 178,203 |
| `install-20261102-packet3/stage-s5/spells.yml` | `e124568524103de8723f190a19efa1d9bea112dfe6a6e3456913bb594da7de6d` | 178,203 |

Unlike the packet #1→#2 chain, packet #3's s2 file is NOT
byte-identical to its base: workbench|4 is zeroed in s2 too, so s2
takes the same 2-line fix.

## YAML validity + parsed end-state (all PASS)

All four files parse cleanly with a strict YAML loader (PyYAML
safe_load): 106 / 168 / 209 / 209 top-level keys, matching packet #2's
key counts exactly. Parsed assertions, run per file:

- familiar (s3/s4/s5): parsed entry has **no `entity_parameters` key
  at all**; `cooldown == 300000`; `costs.mana == 50`; `undo == 20000`;
  spawn table == {chicken 30, cow 100, pig 100, mushroom_cow 15,
  bat 15, horse 5, snowman 5, iron_golem 2}; range 32 /
  target_type LivingEntity / loot false unchanged.
- boomerang (s3/s4/s5): `cooldown == 1000`, `costs.mana == 30`
  unchanged.
- workbench (all four): the full ladder parses as base 20000 / 50,
  |2 = 10000 / 30, |3 = 5000 / 10, **|4 = 2500 / 5** — descending
  at every rank, no zeroed tier.
- s5: mend and flashheal `cooldown_mage == 1000`.
- Raw-text belt: `grep -c 'brain:|follow_owner|look_at_player'` = 0
  in all four files; `grep -c 941` = 0 in s5.
- s2: familiar and boomerang still absent — stage gating untouched.

## Harness re-run (checks 1–11, unmodified harness)

Full output: `install-20261102-packet3/harness-rerun-packet3.txt`.
Baseline = the same harness freshly run over the packet #2 outputs;
its totals reproduce audit §6 exactly (120 / 224 / 272 / 272, same
severity splits, same check-9 counts 6 / 10 / 12 / 12, check 11 = 1
per file, check 8 = 0 everywhere).

| File | Keys | Enabled | Findings P2 | Findings P3 | CRIT | HIGH | MED | LOW |
|---|---|---|---|---|---|---|---|---|
| stage-s2 | 106 | 105 | 120 | **120** | 0 | 6 | 39 → 39 | 75 → 75 |
| stage-s3 | 168 | 167 | 224 | **224** | 0 | 7 | 81 → 80 | 136 → 137 |
| stage-s4 | 209 | 208 | 272 | **272** | 0 | 8 | 90 → 89 | 174 → 175 |
| stage-s5 | 209 | 208 | 272 | **272** | 0 | 8 | 90 → 89 | 174 → 175 |

Every findings-row change between the two runs is attributable to the
packet's deltas — the row-level diff contains nothing else:

- **workbench|4 (all four files):** the check-9 zero-cost MEDIUM row
  ("mana 0, effective cooldown None, no cost of any kind") is GONE —
  check 9 still counts 5 / 9 / 11 / 11 rows per file and workbench|4
  is not among them in any file — and its check-4 LOW row ("mana 0,
  NO cooldown key in config") is gone with it. In their place: a
  check-1 LOW sustainability row at the ladder values (mana 5,
  cooldown 2500 → drain 2.0/s ≤ regen 5/s at pool 60; informational)
  and one check-4 MEDIUM *family* row — "cooldown shrinks 8x across
  tiers (20000 ms → 2500 ms)". That family row is the descending
  ladder's own check-4 signature, the same flag the corpus already
  carries for blink's ladder; it appears precisely because every
  rank now has a real, descending cooldown. It is not a defect and
  not a CRIT/HIGH — the descent is the design — but it is recorded
  here rather than smoothed over: it is why the amended totals sit
  +1 MEDIUM above this packet's first (ladder-inverting) build.
- **boomerang (s3/s4/s5):** the check-4 MEDIUM row ("mana 30, NO
  cooldown key in config") is GONE. In its place one check-1 LOW row:
  at the richest pool only (200 mana, 35/s regen) a 30-mana / 1000 ms
  cycle drains 30.0/s ≤ 35/s, so the 1 s floor is the pace there; at
  pools 60 / 100 / 150 the same cycle out-drains regen and mana
  throttles it. This is the direct, expected arithmetic of the
  standing 1000 ms floor — recorded, not a regression.
- **Familiar (s3/s4/s5): NO row changes.** No harness check reads
  `entity_parameters` — the defect was engine behavior, never a config
  number — so the harness sees the same entry before and after. Its
  two rows (check-2 summon-caps MEDIUM "no concurrent cap in config",
  check-1 LOW) are identical in both runs, and the checks 8–11 detail
  still reads: familiar — mana 50, cooldown 300000 ms, undo 20000 →
  ~0.20 spawns/min.
- **Check 9 (zero-cost keys):** rows drop by exactly 1 per file —
  6 → 5 (s2), 10 → 9 (s3), 12 → 11 (s4), 12 → 11 (s5) — the
  workbench|4 removal. The remaining rows are the five novelty casts,
  unchanged by design.
- **Check 11 (summon pacing):** unchanged — exactly 1 row per file
  (piggyback) in both runs; familiar / cat / wolf pass at their
  configured pacing (0.20 / 0.20 / 1.00 per min).
- **Check 8 (bowpull):** 0 rows in every file, both runs — packet
  #2's arrow fixes still hold.
- **s5 heal normalization:** produces no harness delta (no check
  scores `cooldown_mage` drift — audit §6 already showed s5 totals
  identical to s4 despite the 941). The fix is proven by the parsed
  assertion above, not by the harness.

Net: totals equal the packet #2 baseline (120 / 224 / 272 / 272)
with the composition improved — no new CRITICAL or HIGH row anywhere
(CRIT 0; HIGH 6 / 7 / 8 / 8, identical to baseline). The two genuine
defect rows are gone (workbench|4's zero-cost MEDIUM, boomerang's
no-cooldown MEDIUM); what replaced them is informational: two
check-1 LOW rows and the workbench family check-4 MEDIUM described
above (MEDIUM −1, LOW +1 net in s3/s4/s5; s2 unchanged in count).
Checks 9 and 11 are unchanged-or-better in every file, as required.

## Install shape (for the queue — standard gates, nothing special)

These are **MagicGate stage TEMPLATE files plus the live spells
file**: five destinations, the same five as packets #1 and #2.

| destination (live path) | staged file | current file MUST hash (packet #2 output) |
|---|---|---|
| `plugins/Magic/spells.yml` (LIVE) | stage-s3 | `c49829004d786580107825b0959f1374c9938ca81157243fd560608cd0cf47e6` |
| `plugins/ExcelMagicGate/stages/stage3/spells.yml` | stage-s3 | `c49829004d786580107825b0959f1374c9938ca81157243fd560608cd0cf47e6` |
| `plugins/ExcelMagicGate/stages/stage2/spells.yml` | stage-s2 | `0ec3def9ef38331d032dd39dc393609f1dac9442860f3402459b56dc791c4653` |
| `plugins/ExcelMagicGate/stages/stage4/spells.yml` | stage-s4 | `936af53b2c23ee726eceeced71e8ec4f89c38328c79592f9d826e9d08f698c77` |
| `plugins/ExcelMagicGate/stages/stage5/spells.yml` | stage-s5 | `bda9559f7c7ac991c8a887290f958af6f8dd00b3fd8d8df387e4c12b3cf13663` |

Steps (through the standing gates — verified-empty at any hour, or
the weekend protocol with players on; do not leave destinations split
across windows):

1. For each destination: verify the gate hash above (any mismatch =
   STOP for that destination — the live file is no longer packet #2's
   output), back up the current file in place as
   `spells.yml.bak-20261102-prefix3`, place the staged file, verify
   the placed hash against the staged table.
2. **Restart the server** (panel Shut Down → Start; full restart, not
   `/magic reload`, so boot-verify runs against the lines of record).
3. Boot-verify from the fresh log:
   - Done line present; TPS 20.0.
   - Magic loads with **"Loaded 167 spells"** and no spell-load
     errors; SEVERE 0 attributable (the lone pre-existing Essentials
     unsupported-version ERROR is not attributable).
   - Guild LOCKED with seal count UNCHANGED (3 of 5).
   - Together's awake line still shows the 0.5.2 repair registration +
     confirm gate (proves the repair spells survived the swap
     in-file).
4. Write the marker `install-20261102-packet3/INSTALLED.txt` with the
   window record and flip this entry in the MANIFEST to INSTALLED.

## Rollback

Restore each destination's `spells.yml.bak-20261102-prefix3` and
restart. All values return to the **packet-#2 state** (packets #1 and
#2 stay in place; only this packet's four deltas revert — including
re-arming the familiar brain crash). Nothing else was changed, so
nothing else needs rolling back.

## Player proofs owed (the acceptance that counts)

1. **Familiar (the headline proof):** one wand cast — the animal
   spawns, **no ERROR line in the log** (no IllegalAccessError), an
   immediate recast is refused by the cooldown, and the spawn
   **disappears ~20 seconds later** (the undo engaging for the first
   time). Note: a cooldown-reduction wand may show the remaining
   cooldown slightly under 300 s — same display behavior as wolf's
   observed ~51 s against its 60 s config.
2. **Cat (audit §3 prediction):** one cast — clean, a tamed cat, and
   a blocked recast for 300 s. Cat never carried brain data, so it
   should already behave; this settles the prediction on record.
3. **Boomerang:** casts pace at ~1 per second — no machine-gun
   casting at any pool.
