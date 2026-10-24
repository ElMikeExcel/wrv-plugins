# Check 2: line-by-line review of ExcelWandBars 0.1.0 -> 0.2.0

Reviewed: full `diff -ruN` of src/ExcelWandBars against the 0.1.0 source
(commit 61dfb79): build.sh, config.yml, plugin.yml, README.md,
ExcelWandBars.java, MagicHook.java, StyleHook.java (new), core/ApplyGuard,
FlipGate, StylePrefs, StyleProfile, WandStyle (new or changed), tests.

## Findings and what was done
| # | where | finding | action |
|---|---|---|---|
| 1 | StyleHook.loadSettings | with a kept 0.1.0 config.yml (no `wand-style:` section) the exclude list was empty, so a builder wand inheriting base_wand would have been re-configured | FIXED: missing key = `[masons]`; rig run with the 0.1.0 config file |
| 2 | StylePrefs / styles.yml header | comment said only non-default picks are stored; code stores every explicit pick | FIXED comment + header (behaviour kept: an explicit Classic stays Classic if the default ever changes) |
| 3 | ApplyGuard comment | said one warning per streak; code logs each THROTTLED | FIXED comment |
| 4 | MagicHook spell book | "Bar N:" labels shown to classic players (meaningless in chest mode) | FIXED: labels only for bars players, Java and Bedrock |
| 5 | StyleProfile | bars set lacked the overlay's live_hotbar_cooldown / live_hotbar_mana | FIXED: bars-only keys, classic never touches them (unit test) |
| 6 | design, Magic 11.2.5 | chest-mode cycleSpells NPE on empty spell slots: classic on the overlay layout breaks Q / right click past the first gap | NOT a plugin bug, cannot be fixed from the API. INSTALL-RECORD part B (gap-free noab wands set, Magic + stages 1-5) is required when the overlay is live |
| 7 | bars on the noab layout | beginner wand has 1 bar (6 spells, no hotbar_count), so nothing to flip; student has 2 | accepted, documented; bars is opt-in |

## Checked and OK
* Threading: Geyser form callbacks only schedule onto the main thread
  (openChooser, spell book style button). apply() only runs on the main thread
  (event + scheduler + command paths).
* Re-entry: apply() does closeInventory, saveState, configure, deactivate,
  checkWand (Magic's own /wandp configure order). The activation it causes
  is handled next tick and returns ALREADY. ApplyGuard (4 per 10 s per
  player) stops any loop; ALREADY never consumes the guard.
* Only keys whose effective value differs are written; on the noab (chest)
  base a classic player's wand gets nothing written (rig: no APPLIED line).
* canUse() guard: never re-configures someone else's bound wand.
* Stage applies: ExcelMagicGate copies stageN/* over plugins/Magic/ and runs
  `magic reload`; LoadEvent -> all online players re-applied 40 ticks later.
  styles.yml is in plugins/ExcelWandBars/, which ExcelMagicGate never copies.
* Join: Magic suppresses WandActivatedEvent while the mage loads, so join
  re-checks at 30 and 100 ticks, respawn at 20; stale Player objects are
  skipped (isOnline).
* styles.yml write: tmp file then rename (fallback move REPLACE_EXISTING);
  bad lines are skipped and counted, never fatal.
* FlipGate: CLASSIC_STYLE checked after NO_WAND, before WRONG_MODE;
  State.barsStyle defaults true so 0.1.0 tests keep their meaning; when the
  style switch is off or missing barsStyle() returns true (0.1.0 behaviour).
* Commands: /wandstyle is players only, permission default true;
  admin /wandbars style is op. Unknown argument shows usage.
* No em dashes, no names, gamertags, UUIDs, IPs or tokens in source, config,
  docs or messages ("Tell Mike" is the only name, the server owner).
* No spells.yml, no EliteMobs (MagmaGuy), no ExcelMenuJoin change.
* Jar build is deterministic (two builds, same sha).

Result: PASS after fixes 1-5 (jar rebuilt, unit tests and rig rerun on the
final jar). Item 6 is handled in the install procedure, not in code.
