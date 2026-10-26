# TEST RESULTS: ExcelWandBars 0.2.7 (2026-10-26, box only, nothing live)

Jar: `files/plugins/ExcelWandBars-0.2.7.jar` sha256 `499af4e3339e5d353f67636764284658ccd5791549c4e5dcb54c9dab0459d7fb`
(built twice from the same source, same sha; contains only `com/winkreyes/**`, `config.yml`, `plugin.yml`).
Build: JDK 25.0.5, `src/build.sh` (Geyser-Spigot on the compile classpath only, not shaded).

## Unit tests: 1050 passed, 0 failed (`evidence/unit-tests-027.log`)
| Suite | Pass | Notes |
|---|---|---|
| WandSlotTest (new) | 76 | slot math, every block reason, pick vs sweep, waits while holding slot 9, 2000 random hotbars (each item exactly once, wand in 9, slot 9 item in the old slot), tap vs re-send guard, menu lines <= 38 chars, no long dashes |
| SpellInfoTest (folded) | 511 | golden checks against a copy of live spells.yml (c505e510) |
| TapBarTest | 174 | |
| ProgressTest | 56 | |
| CycleTest | 52 | |
| StyleTest | 48 | |
| TipTest | 42 | |
| TextTest | 37 | |
| ControlsTest | 27 | |
| FlipGateTest | 25 | |
| CumulusFormTest | 2 | |

## Java rig: 23 pass, 0 fail (`evidence/rig-slot-027.log`, `evidence/t_slot.js`)
Private rig `/workspace/wb027-rig` (Paper 26.3-214, Magic 11.2.5, Geyser and Floodgate, live Magic config copies),
mineflayer bot. Every move check counts every stack and checks nothing is on the ground.
- Classic: wand does not move.
- Tap bar sweep: wand to 9, slot 9 item to the old slot, exact counts, nothing dropped.
- Holding slot 9: the sweep waits. Picking the wand: moves to 9, selects 9, Tap bar opens, nothing cast.
- Tap the wand (`/wandbars wandtap`, same code as the Geyser path): bar up = items back exact; again = opens;
  a re-sent slot right after a server change is ignored (guard).
- Bar up: no move. Inventory click: waits 3 sec, then moves. Cursor item: no move until put back.
- Relog and death/respawn (keep inventory): moved after the quiet time, exact.
- Wand held when switching to Tap bar: moves with the hand, still held. Tap to pick (original) moves.
- Wand in the main inventory: left alone. Creative: no move; back to survival: moves.

## Spell info on the rig
`/wandbars spelldump template mend|2` matches `evidence/rig-spelldump-20261026.txt` ("Heals 0.75 heart").

## Mana boss bar B1 on the rig (`evidence/rig-bossbar-B1.log`, `rig-bossbar-B1b.log`)
- `$mana` resolves to a raw decimal ("12.164999961853027"); `$mana_max` resolves to the lore text "Mana: 100". Not usable.
- `@mana` resolves to a whole number counting up with regen (12, 13, 14 ...). Staged title is `Mana @mana`.
- The rig `wands.yml` was put back after the test (sha 289539cd, same as before).

## Not verified
- **Bedrock real tap on the held wand slot.** The Geyser hook installs on the rig ("Geyser hook installed."),
  and the same `onWandTap` code is covered by `/wandbars wandtap` and the unit tests. But the test Bedrock bot
  (bedrock-protocol) could not send a MobEquipment packet Geyser accepts (IllegalPacketException on the item
  stack id, both forms tried) and kept dropping with encryption checksum errors. So a real phone tap is untested.
  Mike's phone check in INSTALL.md step 8 covers it. If the phone does not send the packet, nothing breaks:
  the empty slot and press-and-hold still give items back.
- A real player's screen on the live wrvnpe1 Magic build.
