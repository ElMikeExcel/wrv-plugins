# Changelog: Spellbook button, wand controls choice, wand tip, Auto-sort button

Batch staged Sat Oct 24, 2026 (CDT). Built and tested on the box test server only.
NOT installed, no restart. Magic (elmakers) and EliteMobs (MagmaGuy) files are not changed.

## ExcelWandBars 0.2.0 -> 0.2.1 (jar sha256 d25cf1e8...ecd26)

### Wand controls: a per-player choice (/wandstyle)
* Two styles, and the names players see are "Classic" and "Sneak bars". The old label
  "Spell bars" is now "Sneak bars". The saved value in styles.yml is still `bars`, so
  choices saved by 0.2.0 carry over with no migration.
* **Classic is the default for everyone.** It is the same Magic (elmakers) chest-mode
  set that is live today (WRV-150 wands.yml): right click = next spell, sneak + right
  click = spell list, Q = back one, left click casts. Controller players are unchanged.
* **Sneak bars is Bedrock only.** It works like this: right click opens the spell bar,
  you tap a spell to cast it, and a sneak press while the bar is open flips to the next
  bar (1, 2, 3, then back to 1).
  - Press only (the release does nothing), with a 250 ms debounce.
  - It does nothing while the bar is closed, so sneak-walking is safe.
  - It does nothing in a vehicle, swimming, gliding or flying.
  - It needs a wand with 2 or more bars (student/apprentice 2, master 3; the beginner
    wand has 1 bar, so there is nothing to flip).
* **Java players always get Classic.**
  - `/wandstyle sneakbars` on Java is refused with a friendly message and nothing is saved.
  - If a Java player already has a saved `bars` choice (from live 0.2.0, or the same
    account on Bedrock), Classic is put on the wand while they play on Java. The saved
    choice is kept, so it comes back when they play on Bedrock again.
* New config key `wand-style.bars-bedrock-only` (default true).
* `/wandstyle` with no argument:
  - Bedrock: a two-button form, "Classic (now)" / "Sneak bars". The chat fallback is a
    clickable [Classic] / [Sneak bars].
  - Java: a short explanation of the Classic controls plus a clickable [OK, Classic].
* New argument spellings: `/wandstyle sneakbars` (also `sneak`, `sneak-bars`, `sneak_bars`,
  `sneakbar`, `bars`), `/wandstyle classic`, `/wandstyle switch` and `/wandstyle ok`.
* Switching back to Classic puts back the exact Classic controls. The rig check compared
  the controls with a player who never switched.

### Wand tip (new, first 15 wand uses)
* Shows a short chat tip on each wand use, meaning a cast or right click with a Magic
  (elmakers) wand.
  - Repeat uses within 2 seconds don't count again.
  - It stops for good once the player says OK, or after 15 uses even with no OK.
* How players say OK:
  - Java: a clickable green [OK] in chat.
  - Bedrock: a pop-up form with an "OK, got it!" button. It shows at most once per
    session, and never within 10 seconds of taking or dealing damage. Closing the form
    does not count as OK.
  - Anyone can type `/wandstyle ok`.
  - Picking either style also counts as OK.
* Stored per player in plugins/ExcelWandBars/tips.yml (uses + ack). The file is written
  safely with a temp file and a rename.
* New config section `wand-tip` with these keys:
  - `enabled` (default true)
  - `max-uses` (default 15)
  - `use-gap-ms` (default 2000)
  - `bedrock-popup-combat-quiet-ms` (default 10000)
* Exact text:
  - Java:
    - "Wand tip: you can pick how your wand works!"
    - "Classic: right-click to pick the next spell, left-click to cast."
    - "Type /wandstyle to see your controls. Your spells: Spellbook button in the compass menu."
    - "Click [OK] to hide this tip."
  - Bedrock:
    - "Wand tip: you can pick how your wand works!"
    - "Classic: right-click to pick the next spell, left-click to cast."
    - "Sneak bars (Bedrock): right-click to open your spell bar, tap a spell to cast, sneak to flip to the next bar."
    - "Pick one with /wandstyle or the Wand controls button in the compass menu."
    - "Tap OK on the pop-up, or type /wandstyle ok, to hide this tip."
  - Bedrock pop-up: the title is "Wand tip". The body is the Classic, Sneak bars and
    Pick lines. The button is "OK, got it!".
  - After OK: "Got it! The wand tip won't show again. /wandstyle any time."

### Spellbook (/spellinfo, aliases /spellbook, /wandspells)
* The Spellbook now works while you hold the compass or anything else. It finds the
  Magic wand in your inventory, then your offhand, and prefers a wand that has spells.
  Before, you had to hold the wand.
* The no-wand message is now: "No Magic wand found. Keep your wand in your inventory (take
  it out of your Utility Pouch if it's in there), then try /spellbook again."

### Admin and ops
* `/wandbars tip <player> [reset]` shows the tip count and OK state, or resets it.
* `/wandbars simbedrock <player> [on|off]` is a test aid for ops only and is kept in
  memory only. It treats a Java player as Bedrock and is cleared when that player quits.
* `/wandbars style` now shows the saved choice and whether the player is on Bedrock.
* `/wandbars reload` also reloads the tip settings, and tips are saved on shutdown.
* Boot log lines: "... sneak bars Bedrock only" and "Wand tip ON (first 15 wand uses until OK, N player(s) tracked)".
* Unit tests: 150 passed, 0 failed (new TipTest). Reproducible jar, built twice with the same hash.

## ExcelBedrockMenu 1.5.17 -> 1.5.18 (jar sha256 ee3d00f0...09382)
The Bedrock compass menu now has 17 buttons. There are three new ones, and every other
button keeps its order:
* **Spellbook** sits right after Utility Pouch and runs /spellinfo.
* **Wand controls: Classic or Sneak bars** runs /wandstyle, which opens the two-button
  form with no typing.
* **Auto-sort my chests: on/off** runs /autosort, a toggle. AutoSort's own chat line
  says "Sorting enabled/disabled".
* If the owning plugin is missing, each button gives a short message instead of an
  "unknown command" error.
* Menu tests: 141 checks, ALL PASS. Reproducible jar.

## ExcelMenu server_menu.yml (Java compass menu): 2e86584f... -> 1cdc8c29...
* New item at slot 20: **Spellbook** (writable book), which runs /spellinfo.
* New item at slot 21: **Auto-sort my chests: on/off** (hopper), which runs /autosort.
* There is no wand controls item on Java, by design, because Java is always Classic.
* No other item was changed. It applies with an ExcelMenu reload.

## AutoSort 0.1.17+26.3 by nothub (new, from Modrinth)
* Jar sha256 448a5fff...6439. The sha1 and sha512 match the Modrinth listing.
* Off for everyone by default. Do not pre-fill plugins/autosort/active.txt.
* `/autosort` (alias `/sort`) turns it on or off for yourself. When it is on, your
  chests, barrels, shulker boxes and ender chest are sorted when you open and close them.
* It does not touch plugin menus such as the compass menus, shops, leaderboard, Utility
  Pouch, trash, TP menu or the Magic spell list.
* It skips a container that someone else is already looking at.
* It needs a restart.

## Not in this release
* The ExcelTips hand-over (TIPS-INTEGRATION.md, written by the ExcelTips job) is not
  built in. 0.2.1 always runs its own wand tip. If ExcelTips also ships a `wandstyle`
  tip, turn one of them off (`wand-tip.enabled: false` here) so players don't see two
  tips. The bridge can come in 0.2.2.
* The device-OS (touch only) filter for Sneak bars is not used.
