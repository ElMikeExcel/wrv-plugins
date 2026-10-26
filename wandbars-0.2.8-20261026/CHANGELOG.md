# ExcelWandBars 0.2.8 (staged Oct 26, 2026, not installed)

0.2.7 (`499af4e3`) plus rank-up by casting. Jar sha256 `bc9fd7f6642602a5515f66f9a5e3098008d5a8515073129affd260735ba23542`.
Restart needed (jar). Config and text changes after that are `/wandbars reload`.

## Why
The four Magic rank wands (Beginner, Student, Apprentice, Master) came from Magic 11.2.5's shipped defaults with no
path and `earn_multiplier: 0`, so casting never paid spell points and nobody could ever rank up.

## What's new
- **SP for casting.** A successful cast with a rank wand pays that spell's `earns` (from Magic's spells.yml) into
  Magic's `sp` currency. Full pay once per 60 sec per player and spell. Heal, Mend, Flash Heal and Greater Heal pay only
  when they really heal someone.
- **Daily pace.** 800 SP a day at full rate, then 25% up to 1,600. Resets each day.
- **Rank-up in place.** At 800 / 1,600 / 1,800 SP the held, bound wand becomes the next rank (Student, Apprentice,
  Master) through Magic's own template change: same item, still bound, every spell kept (any lost are re-added), new
  spells learned, the SP cost spent. No seal cap: seals only decide which spells exist (`max-rank-by-stage: {}`).
- **Celebration.** Title `Student rank!`, a sound, a personal chat line, and a server-wide chat cheer
  `<name> reached Student rank!` using the display name/nickname (colour codes and the nickname `~` dropped). Same on
  Java and Bedrock. `announce.broadcast` turns the cheer off.
- **Server XP bonuses count.** Weekend x1.2 (read from mcMMO's xprate, set by ExcelWeekendXP on Fri-Sun; Jobs' weekend
  boost is the same days and not counted twice). Party x1.25 when an ExcelTogether party member is within 50 blocks.
  Together capped at x1.5. Both read by reflection; if a plugin is missing the bonus is simply x1.0.
- **Spell Book and hotbar.** The Spell Book header shows the rank, `340/1,600 SP to Apprentice`, how to earn, and any
  bonus on now. Each gain shows above the hotbar: `+2 SP (x1.25 group) | Student 342/1,600`.
- **No overlap with the party line.** If ExcelTogether showed its party line in the last 2.5 sec
  (`party-bar-wait-ms`), the SP line waits for it (at most 3 tries, then skips).
- **Names.** The 4th rank shows as "Master" in player text (`rank-names:`); Magic's template key is not renamed.
  The wand item's own name still comes from Magic's messages ("Wizard Wand").
- **Admin.** `/wandbars rankup <player> [status|check|addsp <n>]`; `/wandbars` status shows rank-up state.

## Config
New sections `rank-up-casting:` (enabled true, sp-to-reach 800/1600/1800, daily, throttle, heal guard, bonus,
announce, text) and `rank-names:`. A missing section means OFF. Off switch: `enabled: false` + `/wandbars reload`.

## Unchanged
Everything in 0.2.7 (wand to slot 9, tap the wand, spell info, all bar styles). No Magic file is changed. No config or
styles file is written by the plugin. No em dashes or seal mentions in player text.
