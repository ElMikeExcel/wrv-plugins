# ExcelSkillsPanel 0.2.2 — DIAGNOSIS (backlog Items 12 + 13)

**Date:** Nov 13, 2026 (America/Chicago). **Panel live:** 0.2.1
(57,652 B / `ce430d1f…`, installed 03:54 CST, landing verified).
**Report:** Mike, ~12:38 PM CST, screenshots
`~/workspace/user/files/13990_12_9j9z.png` (the panel) +
`~/workspace/user/files/13991_13_bt17.png` (the wand).

## 1. What the player saw

| Panel (13990, Bedrock form "Skills") | Wand in hand (13991, Renewal wand) |
|---|---|
| Magic Level 26 — 579 XP total | Rank: **Apprentice** (ceiling: Master) |
| Rank: **Beginner** | Credited casts: **1,215** |
| Class: Mage | Next: Master — **785 to go** |
| Next rank: Student | Milestones: Student 100 · Apprentice 400 · Master 2,000 |
| Mana: **60 / 60** | |
| Mana regen: **5** | |
| Boosts: mana pool +0.59%, mana regen +0.59%, cast cost -0.24% | |
| Total casts: **694** | |
| Guild seals: 4 of 5 | |

Every Magic field except level/XP/boosts/seals is wrong: rank, next
rank, mana pool, regen, and the cast count all disagree with the
wand the player is holding. This is a real panel data bug (Mike's
words: "See something is definitely wrong. That's the wrong data
look at my wand."). It is NOT a "wrong wand in hand" explanation.

## 2. Second-wand existence check (the corrected brief's first question)

**Finding: a second wand EXISTS in Magic's bound-wand registry, but
that does NOT prove Mike carries two wands, and the bug does not
need a second wand to occur.**

Evidence — Mike's live mage data, pulled Nov 12, 2026 (run 228,
`~/workspace/magic-break-diagnosis-20261112/pull-bm228/mike-mage.dat`,
24,554 B / `8478a1ab…`), section `wands:`:

- Key `beginner` — "Heal (Beginner Wand)", template `beginner`,
  17 spells, lore "Mana: 60 / Mana Regeneration: 5/s", stored mana
  5.553926, `mana_timestamp` 1793505959980.
- Key `student` — "Renewal (Student Wand)", template `student`,
  `path: "apprentice"`, `level_spells_to_path: "student"`,
  `mana: 150.0`, `mana_max: 150.0`, `mana_regeneration: 25.0`,
  lore "Rank: Apprentice (ceiling: Master) / Credited casts: 1,212 /
  Next: Master — 788 to go", `mana_timestamp` 1794467129438.

The beginner wand's last mana timestamp is **11.1 days older** than
the Renewal wand's — it has not been the active wand since ~Nov 1.
The two lore cast counts (1,212 on Nov 12 → 1,215 today) track the
same growing counter, so the Renewal wand is the live one.

What this does and does not prove:

- PROVEN: Magic's mage store binds TWO wand records to Mike.
  (One-wand-per-player is design law, reaffirmed by Mike today
  ~12:45 PM CST — so the beginner record is itself a defect to
  reconcile elsewhere; it is NOT this build's job to delete it.)
- NOT PROVEN: that a second physical wand item sits in Mike's
  inventory, pouch, or ender chest. The mage `.dat` is Magic's
  registry, not the player inventory — the player inventory lives
  in `world/playerdata/<uuid>.dat`, which is in NO workspace pull.
  Mike disputes carrying two wands ("I don't remember carrying
  more than one wand"). Whether the beginner item is carried,
  pouched, ender-chested, or a stale registry entry is an open
  check for the supervisor (fresh playerdata pull), not a settled
  fact, and not a premise of this fix.

## 3. Root cause — the exact code path (proven from Magic 11.2.5 bytecode)

The live 0.2.1 panel reads rank/mana through exactly one wand
source, `MagicApiReads.read()` (`provider/wrv/MagicApiReads.java`):

```
1. Mage.getActiveWand() -> Wand.getPath()          // rank + next rank
2. (null) -> Mage.getActiveClass().getPath()        // FALLBACK: class base path
3. (null) -> Mage.getActiveProperties().getPath()
   Mage.getMana() / getEffectiveManaMax() /
   getEffectiveManaRegeneration()                  // mana + regen
```

Disassembly of the real `Magic-11.2.5.jar`
(`~/workspace/magic-scratch/downloads/Magic-11.2.5.jar`,
`com.elmakers.mine.bukkit.magic.Mage`, read with a desk
class-file parser — no JDK disassembler in this VM) proves:

- **`Mage.getActiveWand()` returns a cached field** (`activeWand`).
  It does NOT read the player's hand. The hand is re-read only by
  `checkMainhandWand()`, which `Mage.tick()` runs once per tick for
  online players: main-hand item → `Wand.isWand(item)` → if it is
  not a wand, the old wand is deactivated and `setActiveWand(null)`
  clears the field (`Mage.deactivateWand` calls
  `setActiveWand(null)` when the deactivated wand is the active
  one — verified in bytecode).
- **`Mage.getActiveProperties()` falls through to the class** when
  no wand is active: active wand → active class → mage properties
  (verified in bytecode). `getEffectiveManaMax()` /
  `getEffectiveManaRegeneration()` delegate to it. So with no wand
  active, mana reads the CLASS frame.
- **The mage class's base path is `beginner`, and its frame is
  60 mana / 5 regen** (mage store: `active_class: mage`,
  `classes.mage.mana: 60.0`; beginner wand lore: "Mana: 60 /
  Mana Regeneration: 5/s"). The class fallback therefore yields
  EXACTLY the observed panel values: Rank Beginner, Next Student,
  Mana 60/60, regen 5.

So the panel's failure mode is: **whenever the wand is not the
mage's active wand at the instant `/skillpanel` runs, the panel
reports the class default as if it were the player's rank.**
Two live triggers are both consistent with the screenshot and are
observationally equivalent from it alone (both yield 60/5):

- (i) **No wand in hand at open time.** Mike's hotbar (13991)
  carries food in slots 1–7 and the wand in slot 9; the panel was
  opened while he held something else. The held item in 13990 is
  a dark item with a red zigzag — not identifiable as the wand
  from pixels (Bedrock custom-model rendering is ambiguous), so
  this trigger is consistent but not pixel-proven.
- (ii) **The beginner bound wand active.** The registry holds a
  beginner wand; if it was the last-activated wand, the cached
  field points at it. Its 11-day-old timestamp makes this the
  less likely trigger, but it cannot be excluded from desk
  evidence alone.

Either way the panel is at fault, not the player: a character
panel must show the player's rank whenever the player OWNS the
wand, not only in the tick window when it happens to be the
active one. Players open `/skillpanel` from the menu while
holding food, tools, or blocks — the rank line must survive
that. **The 0.2.1 rig never exercised this case:** its driver
equipped the fixture wand into the hotbar and let ticks pass
before every panel read, so `getActiveWand()` was always
populated and the class fallback never fired — which is why the
rig passed 46/46 while live failed. The rig gap is part of the
bug.

## 4. The casts bug — a second, independent wrong source

`Total casts: 694` comes from `MagicLevelStoreReads.totalCasts()`
— ExcelMagicLevel's `PlayerRecord.totalCasts`. That counter and
MagicGate's credited counter are DIFFERENT counters by design
(MagicGate `EarnedStore` header: "Magic's counter keeps counting
uncredited casts too, so the two diverge permanently and by
design"; the credited total was placement-seeded from the mage's
full history, ExcelMagicLevel's from its own window — hence
credited 1,215 > "total" 694, which is expected, not a
contradiction). The panel labelled the ExcelMagicLevel number
"Total casts" while the wand — and the rank system — runs on
MagicGate's credited total (1,215). The 0.2.1 code even knew this
(`MagicLines` javadoc: "NEVER 'credited': its equivalence to the
rank-driving credited-cast count is unverified") and shipped the
wrong number under a true-but-misleading label. Fix: read
MagicGate's ledger and label it "Credited casts".

## 5. The fix (what 0.2.2 implements)

**Wand resolution — carried items, not the momentary active wand**
(new order in `MagicApiReads`, replacing the single-source read):

1. **Held main hand** — `MageController.getIfWand(player's
   main-hand item)`. The held item is ground truth at open time.
   (`getIfWand` only — NEVER `getWand(item)`: `getWand` wand-ifies
   ANY item into a phantom wand. MagicGate's own code carries the
   same warning, rig-caught in 0.2.0.)
2. **Cached active wand**, but only if its live item is still
   carried in the player's inventory (a same-tick window where
   Magic has not yet cleared it).
3. **Offhand** — `getIfWand(offhand item)`, else
   `mage.getOffhandWand()`.
4. **Inventory scan** — every carried item through `getIfWand`;
   if several real wands are carried, take the one whose path
   ranks HIGHEST (master > apprentice > student > beginner;
   unknown keys rank below beginner). This is the step that fixes
   Mike's case with no wand in hand: the Renewal wand is in his
   inventory, so the panel reads Apprentice. It also guards the
   two-wand case deterministically — never first-found, never the
   lowest. (MagicGate's own discovery takes the FIRST carried
   bound wand; that order-dependent pick is exactly what this
   rule replaces for the panel.)
5. **Class path, last** — only when the player owns no wand
   anywhere. A wand-less mage's rank IS the class base; that
   fallback is honest there and only there.

The panel does NOT call `mage.checkWand()` to refresh Magic's
cache: that mutates Magic state (activates/deactivates wands,
fires `WandActivatedEvent`) and this panel is read-only. The
carried-item resolution is pure.

**Rank / next rank / mana / regen all come from the SAME resolved
wand object:** `wand.getPath()` (+ `getNextPath()`),
`wand.getMana()`, `wand.getManaMax()`, `wand.getManaRegeneration()`
(all verified present in the Magic 11.2.5 `Wand` API:
`getMana()F`, `getManaMax()I`, `getManaRegeneration()I`,
`getPath()` → `WandUpgradePath`). The mage-effective reads are
kept ONLY as the no-wand fallback (step 5's class standing).
Mana therefore shows the wand's own frame (Apprentice: 150 /
150, regen 25 — the numbers the wand itself displays), with the
ExcelMagicLevel boosts kept on their own Boosts line, not folded
in — no double-counting, no stale-active-wand contamination.

**Casts from MagicGate's ledger** (new `MagicGateReads`):
1. live in-memory store, reflectively: plugin `ExcelMagicGate` →
   public `earnedStore()` → public `creditedTotal(String uuid)`
   (same reflective family as `GuildSealsReads`);
2. failing that, `plugins/ExcelMagicGate/earned.yml` →
   `players.<uuid>.credited_total` (the `EarnedStore` codec,
   verified against the 0.3.2 placement file);
3. failing that, the resolved wand's own lore line
   "Credited casts: N" (the number MagicGate itself writes on
   the wand the player sees).
The line is relabelled **"Credited casts: N"**. If MagicGate
cannot answer at all, the line is omitted — the panel NEVER
falls back to ExcelMagicLevel's `totalCasts` again (that counter
is a different number; showing it reintroduces this bug).
`MagicLevelStoreReads.totalCasts` is removed; its
`perLevelBoosts` (regen/cost fractions) stays.

**Item 12 — remove the "Class: Mage" line** (Mike, ~12:28 PM CST:
"Maybe remove the class mage line since everyone is technically
a mage"): the `className` field is removed end-to-end
(`MagicData` record, `MagicApiReads.MagicApiData`,
`MagicHeaderProvider`) and `headerLines()` no longer emits it.
Both surfaces consume the same `headerLines()` source, so Java
chest lore, Bedrock form body, Magic detail view, and chat
fallback all lose the line together. No other emitter exists
(grep: the only "Class: " emission is `MagicLines` line 100).

## 6. What the rig must prove (0.2.2)

Primary fixture — Mike's stated state, single Apprentice wand
(the Nov 12 mage data + a seeded MagicGate ledger row,
credited_total 1,215 for the rig UUID; the ExcelMagicLevel
total-casts seed stays 57 as a decoy proving the panel no longer
reads it):

- Wand HELD: Rank Apprentice / Next Master / Mana 150 / 150 /
  regen 25 / Credited casts 1,215 / no Class line.
- **Wand NOT in hand** (food held, Renewal wand in inventory —
  the live failure mode): the SAME readings. This is the check
  0.2.1 never ran.
- Two-wand guard (secondary hypothesis — exercised because the
  registry DOES list two bound wands): beginner + Renewal both
  carried, Renewal held → Apprentice, never Beginner.
- No-wand control: fresh mage, no wands → class path honestly
  (Beginner / 60 / 5), casts line omitted (no ledger row).

## 7. Numbers used — provenance

- 1,215 / 785 / Apprentice / Master / 150 / 25: Mike's Renewal
  wand lore, screenshot 13991, Nov 13, 2026 (~12:38 PM CST).
- 1,212 / 788 / 150.0 / 25.0 / path `apprentice`: the same wand
  in the Nov 12 run-228 mage pull (one day earlier; +3 casts
  since — consistent growth, not a second wand).
- 694: ExcelMagicLevel's `totalCasts` as the live panel read it
  (screenshot 13990); a different counter, retired by this fix.
- 60 / 5: the beginner frame (mage class stored mana 60.0;
  beginner wand lore "Mana: 60 / Mana Regeneration: 5/s").
- 579 XP / Magic Level 26 / +0.59% / +0.59% / -0.24% / seals 4
  of 5: the live panel's own correct fields (screenshot 13990),
  unchanged by this fix.
- Nothing above is invented: every number comes from the live
  data, the wand lore, or the Magic 11.2.5 API verified desk-side.
