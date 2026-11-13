# ExcelSkillsPanel 0.2.2 — RIG RESULTS

Desk rig: `rig/` in the build folder (`run-rig.sh` reproduces
both phases; driver `rig/driver-src/panelrig/PanelRig.java`).
Real Paper 26.3 + Magic 11.2.5, fixture = Mike's real mage
data (pull-bm228, id/name rewritten to rig player RigCaster
`33333333-4444-5555-6666-777777777777`), seeded
ExcelMagicLevel record (total-xp 5000, total-casts 57 DECOY),
seeded MagicGate ledger row (credited_total 1,215).

**Phase A (no Geyser/Floodgate): SUMMARY pass=71 fail=0.**
**Phase B (+ live-line Geyser-Spigot 2.12.1-b1292 + Floodgate):
SUMMARY pass=71 fail=0.**
Full tallies: `rig/phase-a-results.txt` (16,717 B /
SHA-256 775c96b7f18fa5ce45daca3f4936ff9647251f6b80c2f9ae85dd3d4e7ce16df5)
and `rig/phase-b-results.txt` (16,716 B /
SHA-256 f4816a4394f957c1c83a27cc80625e1287f554775ca6387e117020403aa2d056)
in the build folder; driver `rig/PanelRig.jar` (20,399 B /
SHA-256 52fa620c77e16cf0d6dfd55910135881c79de5a8625be1145ad835678887fe44).
Boot logs `rig/boot-a.log` (77,779 B) / `rig/boot-b.log`
(83,616 B) likewise stay in the build folder — the raw rig
tree (544 MB of server worlds) is build evidence, not records
material. This file is the records summary.

## The 7-failure resolution (prior run 64/7 → this run 71/0)

Full account in BUILD-RECORD.md §2; in short:

- "Next rank: Wizard" — the WRV-order code fix was already in
  source; the 13:19 rig ran the stale 13:11 jar built before it
  landed. Rebuilding from current source resolved it; the rig
  expectation (next rank must stay Master) was correct throughout.
- "Mana: 154 / 150" — the rig EXPECTATION was wrong, not the
  panel: Magic itself holds the player's mana at 154, above the
  150 frame (frame 150 × (1 + 3.2095%) = 154.8 → 154, exactly
  `mage.getEffectiveManaMax()`). The panel now shows the
  effective pool, 154 / 154 at the rig's level-136 fixture. At
  Mike's Magic Level 26 (boost 0.5943%) the same formula gives
  (int)(150 × 1.005943) = 150 — "150 / 150", exactly the wand
  lore.
- Stale-reference trap — the first rebuild + re-run still failed
  2/71 because the corrected driver read `mage.getMana()` /
  `getEffectiveManaMax()` as the live-mana reference, and with no
  wand active those fall back to the CLASS frame (not-held mage
  mana 51.0, effMax 51). The reference is now a fresh
  `controller.getIfWand(...)` wrapper on the slot-0 Renewal wand
  item, read in the same tick; the `getEffectiveManaMax()`
  cross-check applies only while a wand is active.

## Phase A — key proven lines (phase B identical)

- HELD: `PANEL chain: rankName=Apprentice rankKey=apprentice
  next=Master mana=154.0 manaMax=150 manaRegen=25`; header
  `[Rank: Apprentice, Next rank: Master, Mana: 154 / 154, Mana
  regen: 25, Boosts: mana pool +3.21%, mana regen +3.21%, cast
  cost -1.28%, Credited casts: 1,215, Guild seals: 0 of 5, …
  2% — 1 / 48 XP]`.
- NOT-HELD (food held, Renewal wand in inventory — the live
  failure mode): same readings; `active wand while food held:
  null` (Magic clears the cache — the 0.2.1 panel fell back to
  Beginner here); header identical to held.
- TWO-WAND (beginner + Renewal carried, Renewal held): same
  readings; `active wand with two wands carried: student`.
- No Class line on any surface (header, detail, Bedrock body,
  chat); no "Total casts" label anywhere; "Credited casts:
  1,215" from the MagicGate ledger (the 57 decoy never
  appears); casts lore fallback parses 1,215.
- Bedrock: body carries the corrected header; gold-filled bars
  (§6); `bedrock-send-honest-false` (form send for a Java fake
  player answers false, chat fallback applies). Phase B
  additionally boots with the live Geyser/Floodgate jars
  present.
- Java: command opens the chest, skill click opens detail, Back
  returns, Magic header click opens the Magic detail; gold bars
  in main + detail.
- mcMMO rows verified against direct ExperienceAPI reads
  (Mining 25, 120/1520 XP, power 28); all 19 rows; perk ladders
  + descriptions verified live (Item 10 sweep intact).
