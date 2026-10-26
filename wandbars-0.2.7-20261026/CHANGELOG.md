# ExcelWandBars 0.2.7 (staged 2026-10-26, not installed)

## Wand moves to the last hotbar slot (new: WandSlotPlan, WandSlotHook)
- Styles `tapbar` and `original` (config `wand-slot.styles`): the wand swaps into slot 9 (`wand-slot.slot`).
  The slot-9 item swaps into the wand's old slot in the same tick. Never dropped, never deleted, no copies.
- Two triggers: picking the wand (held-slot change onto a wand: swap, then select slot 9, so the bar opens as usual
  and nothing is cast), and a sweep every 20 ticks for wands already sitting in the hotbar.
- Does nothing while: the wand bar is up (the hotbar then holds spell icons and Magic owns the stored items),
  any other inventory view is open, the cursor holds an item, within 3000 ms of an inventory click, join or respawn,
  dead, spectator or creative, the player is holding slot 9 (waits until they look away), the wand is in the main
  inventory, or the wand is already in slot 9.
- If the wand is the held item when it moves, Magic's wand is deactivated first (synchronous) and re-checked after
  (`setHeldItemSlot` does not fire `PlayerItemHeldEvent`, so `mage.checkWand()` is called by hand).
- Admin: `/wandbars wandslot <player>` (runs the check once now and prints the result and counters).
- Reloadable: `wand-slot.enabled`, `slot`, `styles`, `quiet-after-click-ms`, `sweep-ticks`. Missing keys use the defaults.

## Tap bar: tap the wand to get your items back (TapBarHook, new GeyserTapHook)
- Bedrock: Geyser drops a "select the slot you already hold" packet before Bukkit sees it. 0.2.7 wraps Geyser's
  MobEquipmentPacket translator (looked up per packet, so wrapping at runtime works). A tap on the held wand slot
  calls `onWandTap`: bar up = close it and give items back (exact, Magic's own close); bar down = open it
  (`tap-bar.tap-wand: toggle`; `close` only closes; `off` disables).
- Geyser fills its translator table after plugins enable, so the hook retries once a second for up to 60 sec.
- Guard: taps within 700 ms (`tap-bar.tap-wand-guard-ms`) of a server-side slot change are ignored, so Geyser
  re-sends never close the bar by themselves.
- Java sends nothing for a same-slot click; Java keeps Q, right click and the empty slot.
- Admin: `/wandbars wandtap <player>` (same code path, for testing).
- Fails safe: any Geyser API change logs one WARN and the rest of the plugin runs normally.

## Menu wording (ControlsText, StyleHook, TipGate)
- Bedrock button: `Tap bar (tap wand: items back)` (30 chars) and `Tap: cast. Hold: read. Crouch: page` (35 chars).
- Java menu heading: `Tap bar (Q: items back)`.
- Tip: "Tap bar (best on phone): tap a spell to cast, press and hold to read, crouch for more, tap the wand for your items."

## Spell info (folded from spellinfo-rankup-20261026, same code as its jar)
- The spell info screen is built from the server's own Magic spells.yml: hearts healed, damage, effects and levels,
  duration, targets, radius, cast time, what stops it, cost, the real cooldown, range, and what the next rank changes.
- Optional reloadable `spell-info.rank-up.<rank|default>` text; default "{rank} rank is not open yet. Watch What's New."
- Admin: `/wandbars spelldump <player>` and `/wandbars spelldump template <spell|level>`.
- See SPELLINFO-BEFORE-AFTER.md.

## Unchanged
Stance lock never blocks scrolling. Tap bar still puts all unlocked spells on the bar (7 per page) and crouch pages.
No config or styles file is written. No em dashes in player text.
