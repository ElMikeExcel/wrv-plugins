# INSTALL PACKET — Java "Skills & Perks" menu entry (DeluxeMenus, config-only)

Status: **BUILT + SCRATCH-VERIFIED, NOT APPLIED.** Do not apply before its
dependency below. Built Oct 17, 2026 (Skills & Perks visibility pass).

## What it does

Adds a **Skills & Perks** button to the Java Server Menu and a new
`skills_perks` info menu behind it: an in-game summary of the perk ladders
(mcMMO live, Magic sealed) that points players at the website **Skill Trees**
page (live Oct 17, 2026, website PR #40, `#skill-trees`) and at the check
commands (`/mcstats`, `/<skillname>`, `/mctop`). Nothing in the new menu
teleports, so the menu confirm standard does not apply to it. The Server Menu
button itself only opens a menu, so it needs no confirm either.

## DEPENDENCY — read first

**Apply ONLY AFTER the teleport-confirm packet**
(`~/workspace/java-menu-spawn-confirm/`) **has been applied.** This packet's
`current/` baseline is that packet's `deploy/` state (server_menu.yml with
the four teleport_confirm menus registered in config.yml, 13 menus total).
As of this packet's build (Oct 17, ~6:35 PM CDT) the confirm packet had NOT
applied yet (no APPLIED.txt). If the confirm packet's apply was aborted or
its baseline changed, STOP and re-baseline: hash the live files and compare
against `current/` + SHA256SUMS.txt before touching anything.

## Files

| File | Change |
|---|---|
| `plugins/DeluxeMenus/gui_menus/server_menu.yml` | +1 item: `skills`, ENCHANTED_BOOK, **slot 13** (free in the baseline), opens `skills_perks`. Nothing else changes (see diffs/server_menu.diff, 9 added lines). |
| `plugins/DeluxeMenus/gui_menus/skills_perks.yml` | NEW menu (size 27, 6 items): mcMMO summary, Check Your Skills (runs `/mcstats`), Magic (Sealed) summary, Skill Trees Website (clickable address, same [json]+[message] pattern as the existing Server Website item, URL carries the `#skill-trees` anchor), Back, Close. |
| `plugins/DeluxeMenus/config.yml` | +2 lines in the `gui_menus:` registry: `skills_perks: file: skills_perks.yml` (after commands_menu). **Required** — DeluxeMenus 1.14 only loads registered menus; an unregistered file in gui_menus/ is silently ignored (proven in scratch, see VERIFICATION.md). |
| `plugins/DeluxeMenus/gui_menus/commands_menu.yml` | UNCHANGED (byte-identical, carried for completeness). |

## Apply (when the server is empty or by panel edit + reload; NO restart)

DeluxeMenus applies with `dm reload` (same as the confirm packet; Oct 16
guild-button precedent). Gate: server empty OR Mike's go, per standing rules.

1. Back up the three live files as `*.bak-20261017-skilltrees` in place.
2. FIRST verify live `server_menu.yml` and `config.yml` hash-match this
   packet's `current/` copies. Mismatch = abort untouched and report.
3. Write `deploy/skills_perks.yml` to `plugins/DeluxeMenus/gui_menus/`.
4. Replace `gui_menus/server_menu.yml` with `deploy/server_menu.yml`.
5. Add the two registry lines to `plugins/DeluxeMenus/config.yml`
   (diffs/config.yml.diff shows the exact spot).
6. Console: `dm reload`. Expect "DeluxeMenus successfully reloaded!" and
   `dm list` showing **14 menus** including `skills_perks`.
7. Java click check (needs a player): Server Menu → Skills & Perks (slot 13)
   opens the info menu; Check Your Skills runs /mcstats; the website item
   posts the clickable Skill Trees address; Back returns to the Server Menu.
   Until a player clicks it, status is "applied, Java click check pending,"
   per the standing separate-statuses rule.

## Rollback

Restore the three `.bak-20261017-skilltrees` files, delete
`gui_menus/skills_perks.yml`, run `dm reload`. No restart, no data touched.

## Publishing (at apply time, per the completion gate)

- wrv-plugins: add the three deploy files + this packet under the Java menus
  record (open DRAFT PR, as usual).
- Website: the Skill Trees page already shipped (PR #40). Add one changelog
  line when this applies ("In game: the Java Server Menu now has a Skills &
  Perks button…") — draft copy is in MENU-SPEC.md §4.
