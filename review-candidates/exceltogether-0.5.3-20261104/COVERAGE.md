# COVERAGE — console admin paths across the other Excel plugins (desk note, Nov 3, 2026)

Context: Mike's standing rule (Nov 3, 2026, 11:37 PM CST) — every plugin we build ships
console-runnable admin commands for the support operations a helper might need on a
player's state. ExcelTogether 0.5.3 is the first build under the rule. This note is the
evidence pass over the other plugins' CURRENT source trees (one line each), feeding a
later coverage audit. Nothing here was built or changed.

- **ExcelWandBars 0.3.10 — HAS a console path.** `/wandbars` is not player-gated:
  `reload`, `groups [player]`, `loadout <player> [show|reseed|do …]`, `style <player>`
  all run for a console sender (the player-only messages in /spellbar and /wandstyle
  themselves point admins there). Evidence: build-0.3.10/work/src ExcelWandBars.java
  onCommand (the `wandbars` branch has no instanceof check; loadout admin branch at
  "0.2.9 admin/rig"). A stuck player's bar can be inspected/reseeded from the console.
- **ExcelBossTrial 0.3.7 — HAS.** `/bosstrial [status|feedback|reload|reset [all]|
  grantclaim <player> …]` is sender-agnostic behind the `excelbosstrial.admin`
  permission split (console passes); no instanceof Player gate in onCommand.
  Evidence: seal-tuning/build-0.3.7 tree, ExcelBossTrial.java onCommand + plugin.yml.
- **ExcelGuildGate 0.4.0 — HAS.** `/guildgate <status|recovery|grant|revoke|reset|
  open|catchup|narrate|packreset>` behind `excelguildgate.admin`, no player-only gate
  on the admin branch (only the player-facing `/seals` is players-only, which is a
  view, not a state operation). Evidence: seal-tuning/build-0.3.0/guildgate tree,
  ExcelGuildGate.java onCommand/guildGate + plugin.yml.
- **ExcelMagicGate — HAS.** `/magicgate <status|apply <stage>>` behind
  `excelmagicgate.admin`, no sender-type check. Evidence:
  excelmagicgate-0.1.0/src, ExcelMagicGate.java onCommand.
- **ExcelProtect 0.1.0 — MOSTLY HAS.** `/excelprotect export|inspect|rollback|
  restore-stand|deaths|import|status` routes for any sender and the state-changing
  ops (rollback, restore-stand, import) are console-runnable; the exception is
  `inspect`, which answers "players only" (it toggles a click-to-inspect mode on the
  sender — there is no console form for putting ANOTHER player into inspect mode).
  `/lostfound` is the player's own recovery command. Evidence:
  excelprotect/src, ExcelProtect.java protect().
- **ExcelMenu 2.0 — HAS (registry-level).** `/excelmenu list|entries|reload` behind
  `excelmenu.admin`: the denial only fires for a PLAYER without the permission
  (`!hasPermission && sender instanceof Player`), so the console runs it. The menu
  itself is player-only by nature; per-player menu state isn't an admin op there.
  Evidence: menu-2.0/src, ExcelMenuCommands.java.
- **ExcelGuide — HAS (partial, by design).** `/guide status|reload` and
  `/guide off|on <player>` (a player's Guide toggle — a player-state op) are
  console-runnable; the handler explicitly answers "Console: /guide off <player>".
  `/ask` answers "The Valley Guide answers players in game." — a player feature, not
  a state operation. Evidence: excelguide/src, ExcelGuide.java onGuide/onAsk.
- **ExcelTrainingWheels 0.1.3 — HAS.** `/trainingwheels list|add|remove|setfactor|
  reload` behind `exceltrainingwheels.admin` is the plugin's only command and has no
  sender-type check — Rylie's protection factor can be inspected/changed from the
  console. Evidence: exceltrainingwheels/src-0.1.3, ExcelTrainingWheels.java
  onCommand + plugin.yml.
- **ExcelEliteLadder 0.1.2 — HAS.** `/eliteladder status|check|retry` is
  sender-agnostic (no instanceof gate in onCommand). It administers the ladder, not
  per-player state — there is no per-player state to operate on. Evidence:
  excel-eliteladder-0.1.2/base-0.1.1 (+ build-0.1.2), ExcelEliteLadder.java
  onCommand + plugin.yml.
- **ExcelWhatsNew — console reload PROVEN IN OPERATION; source not in workspace.**
  No source tree for the plugin exists under ~/workspace to inspect (only its
  changelog data files in the surfaces-pass folders). Operationally, the install
  failsafe runs `whatsnew reload` FROM THE CONSOLE in every text live-edit entry
  (see MANIFEST records, e.g. "ExcelWhatsNew reloaded: 33 entries…"), so its main
  operation is console-runnable. Whether it holds per-player state (seen markers)
  with no admin path could not be verified from source — flag for the audit.
- **ExcelAutoSortBridge 0.1.0 — N/A.** No commands at all (plugin.yml declares
  none): it is a PlaceholderAPI expansion publishing %autosort_status%. It holds no
  player state of its own (the on/off state lives in upstream AutoSort, not ours).
  Evidence: menu-toggle-fix-20261103/bridge/plugin.yml.

Summary for the audit: of the ten, seven have a full console path for their main
state operations (WandBars, BossTrial, GuildGate, MagicGate, Menu, TrainingWheels,
EliteLadder), Guide has one for its player-state toggle, Protect has one gap
(inspect mode is sender-bound), WhatsNew is unverifiable from source, and
AutoSortBridge has nothing to operate. ExcelTogether was the fleet's real gap —
now closed by 0.5.3.
