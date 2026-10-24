# ExcelAttunement 0.1.4 install (STAGED, NOT LIVE)

Status: built + tested on the box Oct 24, 2026 ~12:43 AM CDT. Mike has NOT approved going live.
Our plugin (not Magic, not EliteMobs; EliteMobs untouched). Replaces the staged 0.1.3 at
magic-heal STEPS.md step 4b. Same empty-server restart as the heal kit, BEFORE `magic load`.

## Why
Mike, Oct 24 ~12:27 AM CDT: in caster stance he switched to a sword, did not notice it did
0 damage, and nearly died. Ask: "Is there no way to just lock the weapon away?"

## What changed (vs 0.1.3)
- Weapon lock (caster stance): a sword, axe, mace, trident, bow or crossbow cannot be moved
  into the main hand. Blocked: hotbar switch (scroll / number key / tap; the event is cancelled
  and the server resends the old slot), F offhand swap, inventory click into the held slot
  (place, number-key swap, shift-click that could land in an empty hand, drag), and picking a
  weapon up when it would land in the empty held slot (it stays on the ground until the stance
  ends; pickups into any other slot are normal).
- Wand lock (fighter stance): a Magic wand (detected with the Magic API isWand) cannot be moved
  into the main hand the same ways.
- Magic's own items (wands, action-bar spell icons) never count as weapons, so the Magic bar
  and quick cast keep working in caster stance.
- Backstops unchanged: a weapon already in hand still does 0 in caster stance; a wand already
  in hand is still blocked at PreCastEvent in fighter stance.
- Feedback: action bar with a live countdown, refreshed every second while a stance is active:
  "Caster stance: weapons locked 24s" / "Fighter stance: magic locked 18s", then "Stance clear"
  once at the end. On a blocked attempt or a 0-damage hit: the line turns bright, a short dull
  drum sound (max every 0.4 s), and a small title ("Weapons locked" / "Magic locked") on the
  first blocked attempt of each stance. Pickup tries are quiet (action bar, max once a second).
- Chat lines on stance start / end are unchanged.
- Design unchanged (Big Brain WRV-088): stance rules, 30 s window, 10 s combat context,
  out-of-combat support casts never attune, spell table 105 spells (same table as 0.1.3).

## Config (new keys, all default ON; the live config.yml does not need editing)
weapon-lock: true, wand-lock: true, actionbar: true, actionbar-live: true, sounds: true.
Missing keys read as true. actionbar-live: false brings back the quieter 0.1.2 style (line only
on stance start and blocked attempts). Note: Mike called the per-second resend distracting in
0.1.1 (it was removed then); 0.1.4 brings it back on purpose with this knob as the off switch.

## Artifact
- ExcelAttunement-0.1.4.jar  35263 bytes
  SHA-256 626d565c798f0c7187d2ef21dd794a59d4bc4f3290d6d54bdbbcf4a0a20d3fdd (two consecutive builds byte-identical)
- ExcelAttunement-0.1.4-source.zip (full source tree)
- rollback/ExcelAttunement-0.1.3.jar (a650d630..., the staged 0.1.3) and
  rollback/ExcelAttunement-0.1.2.jar (box copy of 0.1.2; prefer the server's own .bak)
- Check: `cd /workspace/batch-next/magic-heal/excelattunement-0.1.4 && sha256sum -c SHA256SUMS`

## Tests (details in TEST-OUTPUT.txt and RIG-TEST.txt)
- Plain-JVM suites 379 passed, 0 failed (309 from 0.1.3 + 70 new lock/feedback/text pins).
- Display + lock pins 0 failed (rewritten for 0.1.4).
- Box rig, Paper 26.3-214 + Magic 11.2.5 + heal kit (combined): 26 of 26 PASS, 0 SEVERE.
- Not tested on a Bedrock client (no Bedrock client can log into the box rig). The hotbar
  block relies on the server resending the held slot, which the rig proved (Java gets
  held_item_slot back). Geyser turns that into a Bedrock hotbar update, but players on PC and
  iPad (Bedrock) should confirm in game.

## Install steps (server, after Mike's go)
Jar swap = server RESTART. Do it in the empty-server restart with the heal kit, restart rule.
1. Panel File Manager: copy plugins/ExcelAttunement-0.1.2.jar to
   plugins/ExcelAttunement-0.1.2.jar.bak-20261024 (never overwrite an existing backup; add -2).
2. Upload ExcelAttunement-0.1.4.jar into plugins/. Rename the old 0.1.2 jar away (the .bak
   copy is enough) so only ONE ExcelAttunement jar is in plugins/. Do NOT also install 0.1.3.
3. Leave plugins/ExcelAttunement/config.yml as is. Make sure there is NO
   plugins/ExcelAttunement/spell-classes.txt.
4. Restart. Console checks:
   "ExcelAttunement enabled: ON, window 30s, combat context 10s, spell table 105 spells."
   "Stance lock: weapon-lock ON, wand-lock ON, actionbar ON (live ON), sounds ON."
5. Continue the heal kit steps (`magic load` etc.). `attunement status` shows ON plus the locks.

## Rollback
- Locks only (no restart): not possible live; set weapon-lock/wand-lock false in config.yml
  and restart, or swap the jar.
- To 0.1.3 (keeps heal-kit spell classes, no lock, old quiet action bar): stop, remove the 0.1.4
  jar, upload rollback/ExcelAttunement-0.1.3.jar, start.
- To 0.1.2 (the pre-heal-kit live jar): stop, remove the 0.1.4 jar, restore
  plugins/ExcelAttunement-0.1.2.jar.bak-20261024 to ExcelAttunement-0.1.2.jar, start.
  Heal-kit spells then fall back to COMBAT (fail-closed), same as before 0.1.3.
