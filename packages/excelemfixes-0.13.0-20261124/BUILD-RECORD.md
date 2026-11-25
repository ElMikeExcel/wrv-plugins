# BUILD RECORD — ExcelEMFixes 0.13.0 (skill-bonus Claim route + nudge reword)

Lane: ~/workspace/em-menu-replacement-20261124/
Tree of record: copied from the 0.12.0 tree
(~/workspace/mcmmo-acrobatics-check-20261124/build-root/) —
that lane's build is the live jar (premise verified below).
Filed under the standing ExcelEMFixes module workflow
(Mike, Nov 24, 2026): the fix is a change inside the existing
clarity-menus module plus the skill-bonus-nudge text; the
reason (Jester's report) is carried in the config comments
and the pack README, per the rule.

## The defect this fixes

Jester, Nov 24, 2026, 2:38 PM CT (Discord DM): "elitemobs
normal menu still has this bug where it does not show skill
passives" — he claimed his Armor bonuses only through
/elitemobs alt. Verified against the live EliteMobs jar
(file-named 10.9.10, internal 10.9.14, SHA-256 4a6bc4a7…):
the normal /em menu is a dialog whose Skills page is built
as a text component (levels + XP only — its builder never
opens a menu); the clickable claim screens exist only in
the chest-style menu tree (menus/skill_bonus_menu.yml).
Our own surfaces pointed at the broken page: /emguide's
skills route ran bare `em`, its step-by-step text described
clicking a skill on that page, and the skill-bonus-nudge's
message + click-command (/elitemobs) named the same page.

## Changes (complete list)

1. clarity-menus — new route kind. A route in
   clarity-menus.yml now carries exactly one of `command`
   (run as the player, unchanged) or `em-menu` (open one of
   EliteMobs' own screens directly). ClarityText validates:
   exactly one kind present; em-menu must be a known target
   (only "skill-bonus"). New bridge class EmMenuBridge calls
   EliteMobs' public static SkillBonusMenu.openWeaponSelectMenu
   — the exact opener EliteMobs' own chest-style status menu
   uses for its Skills item (javap-verified in the live jar).
   The module opens it next tick after closing our menu,
   checks EliteMobs is present/enabled first, and on any
   failure sends the manual /elitemobs alt path instead of
   leaving a dead button. EliteMobs stays a compile-time-only
   dependency; the bridge class loads only when clicked.
2. clarity-menus.yml — the `skills-screen` route (bare `em`)
   is replaced by `claim-bonuses` (em-menu: skill-bonus).
   The Skills page's route button is now "Claim skill
   bonuses" (opens the claim screens directly); the
   step-by-step detail is reworded to match, and now also
   states plainly that the normal /em Skills page only lists
   levels and teaches the /elitemobs alt path for players
   going through EliteMobs' own menus.
3. skill-bonus-nudge text (config) — click-command
   "/elitemobs" -> "/emguide skills"; message line 2 leads
   with the guide's Claim button and names the
   /elitemobs alt path. Reason comments added beside both,
   per the module workflow rule.
4. Rider fix (named for the next build by the 0.12.0 lane):
   the main class VERSION literal was stale at "0.11.0" —
   now "0.13.0", so the boot ready line prints the true
   version (spec v2.1 A11 cross-check material).
5. plugin.yml version 0.12.0 -> 0.13.0. Module count
   unchanged (19). README clarity-menus section gains the
   0.13.0 reason paragraph.

## Build facts

- ExcelEMFixes-0.13.0.jar — 461,532 bytes
  SHA-256 eeec91c1ea0496401544e477a2f170d46df53e80ca5c3772366c900298d65fd5
  Deterministic double-build byte-identical (build.sh in the
  lane's build-root/).
- Suites: 1,059 checks, 0 failed (two consecutive clean
  runs; new checks cover the claim route's shape and the
  loader's three new rejection cases). One intermittent
  failure in the pre-existing skill-nudge tie-break check
  was observed once in an earlier run — the documented
  pre-existing flake in untouched code; it did not recur.
- Rig boot smoke (clone of the arcane lane's Paper 26.1.2
  rig with the live EliteMobs jar, clarity-menus enabled in
  the rig config): "Clarity Menus ready — 5 pages,
  10 routes.", "ExcelEMFixes 0.13.0 ready — modules:
  [arcane-bolt, clarity-menus]", boot Done, SEVERE 0.
- NOT proven pre-install: the end-to-end Claim click
  (a player inside the menus). The opener itself is
  EliteMobs' own verified public static; the click-through
  is proof-owed (see INSTALL-PACKET.md).

## Premises verified by this lane (Nov 24, 2026, read-only)

- Live plugins/ExcelEMFixes-0.12.0.jar: 459,581 bytes,
  SHA-256 e71620292afe95b58c90650c2d65d85f1a5da8163ecb279d6de861619be4a9a2
  (Get-FileHash on the PC, this lane).
- Live plugins/ExcelEMFixes/config.yml: 64,032 bytes,
  SHA-256 0c694bfa3d0bf9165f451c797637cbfe87767882de621f6fd55139971ba25cff.
- Live plugins/ExcelEMFixes/ folder contains NO
  clarity-menus.yml — the bundled text governs (folder
  listing read this lane).
