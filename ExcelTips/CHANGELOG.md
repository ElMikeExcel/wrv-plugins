# ExcelTips changelog

## 0.1.0 (Oct 24, 2026) - first release

New plugin for Wink Reyes Valley (Paper 26.3, Geyser + Floodgate). Short chat
tips the first few times a player uses a server feature, until they say OK.

### Tips
- 22 tips in `tips.yml` (some only switch on when their plugin is running):
  compass menu, jobs, money, spawn shop, villager shops, /tpa, beds, /spawn,
  Trash (ExcelTrash 0.1.0 wording), Trash bin (ExcelTrash 0.2.0 wording),
  chest sorting (AutoSort), maker's mark (ExcelMakersMark hand-over), seals,
  Adventurers Guild gate, /guide, land claims, party, pouch, places,
  leaderboard, skills, coming back after dying.
- No wand tips at all. ExcelWandBars 0.2.1 ships its own wand tip and the
  0.2.2 Spell Book tip registers itself (as `spellscreen`), so players never
  see two wand tips. The unit tests fail if anyone adds a `wandstyle`,
  `magic-wand`, `spellscreen` or `lostfound` entry to `tips.yml`.
- Plugins that ship their own tip (Lost & Found in ExcelDeathRecord 0.3.0,
  the 0.2.2 Spell Book, ExcelMakersMark) get a note in `tips.yml` and the
  API hook, never a second copy of the tip.
- Seal and guild-gate tips only say a seal fight exists and where to look
  (/seals). No mechanics, enemies, phases or counters. The rule is written at
  the top of `tips.yml` and checked by the unit tests.
- Each tip shows up to 3 times (per tip `max-shows`; 0 = until OK). Uses
  that are blocked (combat, spacing) do not count.

### Saying OK
- Java: clickable **[OK]** under the tip.
- Bedrock: an "OK, got it!" / "Remind me later" pop-up (Floodgate simple
  form). It waits for a calm moment: never in combat or within 10 s of it,
  never within 45 s of joining, never while a menu or chest is open, at most
  one pop-up per 5 minutes. Players with a blocked pop-up still get the chat tip.
- Everyone: `/tips ok [tip]`, or typing "ok" / "got it" in chat soon after a tip.

### Triggers
- Config-driven: command, join, respawn, item-use, block-use (incl. `#tags`),
  bed-enter, inventory-open (by title or holder class), entity-interact (by
  name or metadata), region (enter an area), event (any plugin's event class,
  with a player path), api.
- Event triggers for plugins that load after ExcelTips are hooked when the
  server finishes loading and whenever a plugin enables.
- Command triggers listen at MONITOR priority, so a command another plugin
  answers (for example the guild gate page) still counts.

### API (for ExcelTrash, ExcelMakersMark, ExcelWandBars)
- `ExcelTipsAPI.available()`, `hasFeature(name)`, `tip(player, name)`,
  `acknowledge(player, name)`, `isAcknowledged(uuid, name)`, `isFinished(uuid, name)`.
- `ExcelTipsAPI.register(plugin, name, title, javaLines, bedrockLines, maxShows)`
  and a short `register(plugin, name, title, lines)`; `unregister(plugin, name)`.
  A `tips.yml` entry with the same name always wins. Registrations are dropped
  when the owning plugin disables.
- `ExcelTipsAckEvent` (player id, tip name, source chat/popup/api) fires the
  first time a player says OK to a tip.
- `needs-plugin: Name[>=version]` and `unless-plugin: Name>=version` in
  `tips.yml` switch tips on or off by which plugins are running.

### Commands
- Players: `/tips` (list), `/tips ok [tip]`, `/tips show <tip>`. Alias `/tip`.
- Admin (`exceltips.admin`, op): `/tips reset <player> [tip]`,
  `/tips status <player>`, `/tips trigger <player> <tip>`, `/tips check`,
  `/tips reload`.

### Storage
- `plugins/ExcelTips/players.yml`: per player, per tip: shows and OK. Saved
  every 30 s and on shutdown.

### Changes while finishing the build (Oct 24, 2:15 PM CDT, before release)
- Removed the `wandstyle` and `magic-wand` tips (ExcelWandBars 0.2.1 already
  has its own wand tip, so they would have doubled up). `Magic` dropped from
  `softdepend` because nothing listens to Magic events any more.
- The Trash bin tip (`trash-bin`) now uses the exact words from ExcelTrash
  0.2.0's TIPS-NOTE.md: "Put things you do not want on the red side, then
  click/tap OK to throw them away." and "Oops? Click/Tap Recover last trash,
  or type /trash undo, within 5 minutes." Added the Trash screen
  (`TrashHolder`) open as a third trigger. One tip per open, never two.
- New unit checks: Bedrock lines never say "click", shared lines say neither
  "click" nor "tap", any tip that mentions seals goes through the seal word
  check, and no duplicate entries for plugins that ship their own tip.
  422 unit checks pass.
- API javadoc example uses a neutral `myfeature` name.

### Fixes during testing (before release)
- The "coming back after dying" tip was blocked as combat after a death; combat
  quiet now resets on respawn.
- Removed `/job` and `/adventurersguild` aliases (not on the server).
- The beds tip also triggers on right-clicking any bed (daytime clicks too).
