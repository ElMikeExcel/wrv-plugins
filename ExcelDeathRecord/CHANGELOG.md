# ExcelDeathRecord changelog

## 0.3.0 (2026-10-24): Lost & Found

### New: Lost & Found (`/lostfound`, alias `/recover`)
- Players bring back their OWN items from the Burn Ledger: items lost to lava,
  fire, the void, or a despawn the ledger records. That includes items a
  player dropped by dying that then burned, fell into the void or despawned.
  The death ledger itself is not offered (ops' `/deathrecord restore` covers it).
- Price: $16,000 per recovery in server money (Vault economy). Config
  `lost-and-found.price`.
- First recovery free when the player has less than $10,000 at the moment they
  confirm (`free-first-recovery`, `free-threshold`). Once per player, and only
  if it is their very first recovery (`free-only-if-no-earlier-recovery`).
- Only the owner: the player the item belonged to when it was lost. Items
  picked up by someone else and then lost belong to the person who lost them.
- 7-day window (`window-days`), one recovery per 24 hours (`cooldown-hours`).
- Java: chest menu (list, then "Bring it back?" with Yes / Cancel).
  Bedrock (Geyser/Floodgate): a list form and a confirm modal. Bedrock words
  say "tap", never "click".
- The list shows how each item was lost, where (Overworld, Nether, End, or the
  world's name) and when (CDT).
- Owner gets a chat line when a valuable is lost: "Your {item} {how}. Type
  /lostfound within {days} to get it back." (`tell-owner`, `tell-owner-text`).
  At most one line per player per 20 seconds, so a burst of losses is one line.

### Safety rules in the confirm step (all on the main thread, under one lock)
- The offer is used up at the first click: a double click or double tap gives
  one item and charges once.
- The record is re-read from disk at confirm time. If an op restored it, the
  7 days ran out, or the cooldown started while the screen was open, it is
  refused with no charge.
- The price is decided again at confirm time (the free rule looks at the
  balance then). If it changed since the screen opened, the player is shown
  the new price and must confirm again.
- Needs a free inventory slot first. A full inventory is refused before any
  money moves.
- Order: charge, write the restore mark (by SELF), save the player's state,
  give the item, write the CSV rows. If anything fails after the charge, the
  money is refunded and the mark undone, and the console says so.
- Recovered entries are marked "restored by SELF" in the Burn Ledger, so an
  op `/deathrecord restore burned` of the same entry is refused without `force`
  (no duplicates either way).
- Quitting or a server restart with the confirm screen open gives nothing and
  charges nothing; the item stays listed.

### Burn Ledger changes
- Owner stamp: when a player drops or throws an item, or drops it by dying,
  the item entity gets the owner's id in its data (`exceldeathrecord:owner`), so ownership survives
  restarts and chunk unloads.
- Hopper and player pickups are not losses: an item a hopper or another player
  picks up is never logged (including the same-tick case where a burning item
  is pulled into a hopper). A partly picked-up stack keeps being watched.
- Despawn logging now also covers valuable unenchanted gear by type, set by
  `despawn-log-types` (default: diamond_*, netherite_*, elytra,
  totem_of_undying, trident, mace, heavy_core, *shulker_box,
  enchanted_golden_apple, nether_star, beacon). Enchanted or named items are
  logged as before.
- Void losses are caught in every world, including custom ones (EliteMobs
  dungeon and guild worlds) and the End.

### First-use tip
- Shown the first times a player opens Lost & Found, every time until they say
  OK (no cap).
- If ExcelTips 0.1.0 is installed, the tip is handed to ExcelTips
  (`register("lostfound")`, shown through ExcelTips' chat [OK] on Java and its
  calm-moment OK pop-up on Bedrock). Lost & Found then shows no tip of its own,
  so it never doubles. An OK in either place counts in both
  (ExcelTipsAckEvent, `/tips ok`, a plain "ok" reply, `/lostfound ok`).
- Without ExcelTips: its own tip screen (Java) or modal (Bedrock) with
  "OK, got it!" / "Remind me later". `/lostfound ok` also hides it.
- ExcelTips is a soft dependency, used by reflection only. Adding or removing
  it needs no change in ExcelDeathRecord.

### Ops
- `/deathrecord lostfound <player>`: recoveries, free used, cooldown, tip OK,
  balance, next price, open entries.
- `/deathrecord lostfound <player> reset cooldown|free|tip|all`.
  With ExcelTips installed, a tip reset also needs `/tips reset <player> lostfound`.
- `/deathrecord burned <player>` and `/deathrecord restore burned ...` work as
  in 0.2.0 and show the SELF mark.

### Files
- `plugins/ExcelDeathRecord/lostfound/players/<uuid>.yml`: per-player state
  (free used, recoveries, last recovery time, tip OK).
- `plugins/ExcelDeathRecord/lostfound.csv`: one row per recovery (free or paid,
  amount, balance, result), plus a row for any failure after money moved.
- `restores.csv` gets a row for every recovery (by SELF).
- Config upgrade: an existing 0.2.0 `config.yml` gets the new
  `lost-and-found` and `despawn-log-types` blocks appended once. The old file
  is kept as `config.yml.bak-pre-0.3.0` (never overwritten).

### Compatibility
- Reads all 0.1.0 and 0.2.0 records. Burn Ledger files gain an
  `epoch-millis` line (older files without it still work).
- Rolling back to 0.2.0 is safe: 0.2.0 reads 0.3.0 files, sees the SELF marks
  and refuses an op restore of a recovered entry without `force`.
- softdepend: Geyser-Spigot, floodgate, Essentials, Vault, ExcelTips.

## 0.2.0 (2026-10-23)
- `/deathrecord restore` (typed and tap menu), all-or-nothing restores,
  restore marks, `restores.csv`. See README.

## 0.1.0
- Death ledger and Burn Ledger.
