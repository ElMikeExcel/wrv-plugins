# INSTALL PACKET — ExcelEMFixes 0.1.0 (Nov 22, 2026)

One jar replaces three standalones and adds four new modules. This is a
swap install: read the sequence before touching anything.

## Artifacts (all in ~/workspace/excelemfixes-20261122/)

- `ExcelEMFixes-0.1.0.jar` — 169,041 B — SHA-256
  `777d2c6a2985e098e75c66c622016a8a620dd7bd313d670a97eaca148ceffca9`
  (deterministic build: three consecutive builds, identical hash)
- `deploy/config.yml` — 11,800 B — SHA-256
  `9badff9c66d8cf04fba2f98e37fb3c2dbe4157f1e0f59f155e5aa6890a99ca07`
  → lands as `plugins/ExcelEMFixes/config.yml` (WRV's deployed config:
  all modules on; arcane-bolt scale 1.5; food-heal debug on + worlds
  listed; leash debug on; progress-tracker kill-records-folder
  "ExcelProtect" — see README "What WRV runs differently")
- `deploy/quest-destinations.yml` — 9,229 B — SHA-256
  `3d812df69057d1063600752edd6d22a74847857dda50ed8f96cbe513220e27ab`
  → `plugins/ExcelEMFixes/quest-destinations.yml` (the 11-row table)
- `deploy/drops-index.yml` — 1,020,641 B — SHA-256
  `a70ca2c6ef56ef5cddc00b01c7f0dbf9901ffdc355c28d72af86b554c65d884d`
  → `plugins/ExcelEMFixes/drops-index.yml`
- `deploy/tracker-catalog.yml` — 52,591 B — SHA-256
  `2075ab085a901fca0cdf0b0d5e1052699f2d37e4af20d254e3c0916123cf101e`
  → `plugins/ExcelEMFixes/tracker-catalog.yml`
- `deploy/seal-restore.yml` — 4,712 B — SHA-256
  `ab624874c7e155e065710f98ad36a41a2e1ad1ac7c651b6fd476ca15640eb012`
  → `plugins/ExcelEMFixes/seal-restore.yml` (the Camp Warden row,
  30 blocks, generated from EM's own content files)

## Swap sequence (single window, verified-empty gate, backup-first)

1. Snapshot + the usual pre-window backup.
2. Place `ExcelEMFixes-0.1.0.jar` in `plugins/`; create
   `plugins/ExcelEMFixes/` with the five deploy files above.
3. Rename the absorbed standalones in place (kept as rollback):
   - `ExcelEMCompanion.jar` (0.1.0) → `ExcelEMCompanion.jar.bak-excelemfixes`
   - `ExcelEMLeash-0.3.0.jar` → `ExcelEMLeash-0.3.0.jar.bak-excelemfixes`
   - `ExcelEMFoodHeal-0.2.0.jar` → `ExcelEMFoodHeal-0.2.0.jar.bak-excelemfixes`
   (Confirm exact live filenames from the plugins listing at the window;
   the versions are the Nov 22 installs.)
4. **Camp Warden content revert (same window, after the jar is in):**
   revert `plugins/EliteMobs/custombosses/primis/primis_bloodtemple_climb_miniboss_1_p4.yml`
   to stock by restoring the `.bak` kept from the Nov 22 D188 install.
   Verify the reverted file hashes to
   `7661989fe8c3870838d54bd43b72609b2aa2ef15c88e70c7dafc20ca60cc1f53`
   (the pre-fix evidence copy,
   ~/workspace/camp-warden-verify-20261122/evidence/). The seal-restore
   module is then the single fix of record for both doorways. The
   primis_sleep power patch is NOT touched (it stays a content fix by
   design — see BUILD-RECORD).
5. Start the server; verify from the log: all seven modules enabled
   (quest-destinations, arcane-bolt, drop-guide, progress-tracker,
   food-heal, leash, seal-restore), coverage scan uncovered=[], drop
   guide entry points resolved > 0, seal-restore watching 1 boss /
   30 blocks, no SEVERE.
6. Write the marker (below) and the install-timing ledger line.

## Marker

`~/workspace/excelemfixes-20261122/INSTALLED.txt` — written by the
installing lane with window id, timestamp, and post-install log checks.

## Same-pass surfaces (both What's New surfaces, same window)

Website (wrv-site-ai) + in-game `plugins/ExcelWhatsNew/changelog.yml`
(edit the pulled live file, never regenerate from the stale registry).
Entry (house style — plain words, no em dashes):

- id: `2026-11-22-quest-directions-glow`
- date: Nov 22 / posted: 2026-11-22
- title: `NEW: Quest directions and glow guides`
- changes:
  - `/questwhere shows where to go for quests whose tracker shows no destination, like Severance payback and The trial of ice.`
  - `Tap Guide me there and the entrance glows so you can find it.`
  - `/drops and /progress now have a Glow the way in button that lights up where a boss is found.`
  - `In /progress, quests like those also have a guide button in Quests Remaining.`
  - `One plugin box (ExcelEMFixes) now holds the drop guide, progress tracker, food healing and boss leash fixes. Same commands, nothing to relearn.`

Menu surface (same window): the EliteMobs compass submenu
(`elitemobs_menu`, live since Nov 22, 2026 — see
`~/workspace/em-menu-20261122/PACKET.md`) gains its Find My Quest door
in this same pass, because /questwhere only exists once this pack
installs: place `entries/el_questwhere.yml` (id el_questwhere, menu
elitemobs_menu, group elite, order 80, order-bedrock 70, icon
RECOVERY_COMPASS, label '&a&lFind My Quest', lore '&7Where to go for a
quest whose tracker', '&7shows no destination', action '[close]' +
'[player] questwhere') in `plugins/ExcelMenu/entries/` and run
`excelmenu reload`; verify the registry diff shows +1 added
(el_questwhere), 0 failed.

## Proofs owed after install (named)

- **Jester tracker on live /progress**: 63 completions / 61 distinct of
  111 / main chain 26 of 31 / 20 dungeon runs / 1,622 kills (the mirror
  reproduced these exactly through the folded module).
- **P-JESTER-OOC-HEAL-1** (food-heal): first natural `FOODHEAL bonus
  start` line; then flip food-heal debug off.
- **P-LEASH-BOSS-AGGRO-181** (leash): first natural chase past 96
  blocks — return + arrival heal in the log; then flip leash debug off.
- **P-ARCANE-BOLT-LIVE-1**: Jester session with arcane debug on — bolt
  an elite (expect buff=1.000), Spell Echo, bolt again (expect
  buff≈1.14).
- **Camp Warden next natural kill**: BOTH doorways restored, now by the
  seal-restore module (log line "Seal Restore: boss
  primis_bloodtemple_climb_miniboss_1_p4.yml died — restored N of 30").
- **Glow entity-mode live case**: guide quest 28 (Legacy of the Flame)
  — the Granus Teletiamus teleporter NPC itself glows.
- **Companion Bedrock form pass**: /drops + /progress on Bedrock,
  including the expertise legend and the new glow buttons.

## Rollback

Remove `ExcelEMFixes-0.1.0.jar` + `plugins/ExcelEMFixes/`, rename the
three `.bak-excelemfixes` jars back, re-apply the D188 candidate p4
(~/workspace/camp-warden-verify-20261122/candidate/) if the seal module
must come out, restart. Player data is untouched by this install
(everything reads EM/ExcelProtect data; nothing migrates).
