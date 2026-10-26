# ExcelAttunement 0.1.7: the stance lock never walls off a hotbar slot

Built Mon Oct 26, 2026, about 1:20 AM CDT. Staged only, not installed.
Base: the LIVE 0.1.6 jar (sha256 441d3f70..., installed Oct 24, 11:37 AM CDT). The 0.1.6 source rebuilds 441d3f70 byte for byte on the box, and 0.1.7 is that tree plus this one fix. It has no other changes: no Mend, Tap bar or hold-to-read work, and no config change.

## The bug (live since 0.1.4)
- In Fighter stance (you hit something with a sword, axe, bow and so on) a Magic wand may not come into your main hand. In Caster stance a weapon may not.
- 0.1.4 to 0.1.6 did this by refusing the hotbar switch onto the locked item.
- Scrolling moves one slot at a time, so the scroll bounced off the locked slot. Everything on the far side was out of reach until the stance ran out (30 s, and taking hits keeps it going).
- Reported Oct 26: a Java player in default wand mode had his wand in the middle of the bar and could not scroll past it to his healing potions. He died.
- Same trap: Caster stance with a sword in the middle of the bar, and Bedrock controller players (the LB/RB bumpers also step one slot).

## The fix
- A one-slot move (mouse wheel, controller bumper) onto a locked item now hops over it to the next unlocked slot in the same direction. It wraps 9 to 1 like the normal hotbar.
  - Example: wand in slot 5, scrolling right from 4 goes 4, 6, 7, 8, 9, 1, 2, 3, 4, 6...
  - Two locked items side by side are both hopped.
- A direct jump (number key, Bedrock touch tap) onto the locked item is still refused. You stay where you were, and every other slot is one key or tap away.
- The locked item itself still never lands in your hand, so the stance rule is unchanged. The backstops are unchanged too: casts are blocked in Fighter stance and weapon damage is 0 in Caster stance.
- Feedback: a hop shows only the stance action bar ("Fighter stance: magic locked 18s"), with no title and no thunk sound, so scrolling past is not treated as a mistake. A refused number key or tap keeps the 0.1.6 feedback: action bar, a throttled thunk, and the title once per stance.
- The hop is applied 1 tick later, after the server has answered the refused switch. If the player already moved on (fast scrolling), or the slot changed in that tick, nothing is forced.
- `/attunement status` now also shows "scroll hops N" (a counter since boot).
- Debug log (debug: true): `HOP hotbar 4 -> 6 over a locked item for <player>`.

## Files changed
- `core/HotbarSkip.java` (new, pure logic, unit-tested): `direction(old,new)` (+1 / -1 / 0 for a jump, with wrap) and `target(old,new,locked)`, which returns the first unlocked slot past the locked one, or REFUSE.
- `ExcelAttunement.java`: `onHeld` uses HotbarSkip; status line counter; class doc history line.
- `plugin.yml`: version 0.1.7. `build.sh`: version, jar date 2026-10-26, and `OUT` can be overridden.
- Tests: `HotbarSkipTest` (new, 321 checks; total 977 = 656 from 0.1.6 + 321) and the WardTest version pin moved to 0.1.7.
- config.yml: NOT changed (byte-identical to 0.1.6). The live config is kept as-is.

## Not changed / known
- The offhand swap (F), inventory click/drag and pickup locks are unchanged. They never block the hotbar: they only stop the locked item moving into the held slot.
- If you press the number key for the slot right next to you and that slot is locked, it counts as a scroll and hops one further. This is harmless, and the action bar explains it.
- If every other slot holds a locked item, there is nowhere to go and the switch is refused, as before.
- Real Bedrock device: not tested. The rig has no Geyser. Geyser sends taps and bumpers to the server as the same hotbar packet the Java bot sent.
- ExcelWandBars Tap bar (0.2.4) is not touched. In Fighter stance the wand cannot be selected, so Tap bar cannot open then. A Tap bar that was already open when the stance started will be checked in the separate WandBars 0.2.5 work.
