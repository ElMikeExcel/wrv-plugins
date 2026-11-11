# RIG RESULTS — ExcelAimAssist 0.1.5 (console admin)

Rig: local Paper 26.3-231 server (the unpacked rig tree from
the 0.1.4 AimAssist rig — paper.jar + its
versions/libraries/cache, copied to /tmp; paperclip runs
offline from the seeded cache) with ExcelAimAssist-0.1.5.jar
(SHA-256 verified 9e5d926b…a50 in the rig plugins folder before
boot) and the live PlaceholderAPI 2.12.3 jar — the softdepend
the live server carries, so the expansion registration path is
exercised too. Offline mode, fresh world, port 25599, no other
plugins. Full log: `boot.log` in this folder; the exact
console commands: `cmdfile.txt`.

The toggle state was seeded before boot (`toggles-seed.txt`):
UUID 11111111… explicit OFF, UUID 22222222… explicit ON, and
UUID 33333333… with no entry at all (the default-ON case).
Every command was typed on the server's stdin — the REAL
console sender (`CONSOLE`), exactly the caller this packet
exists for. The exercise ran in two phases so the read-only
claim is proven on disk, not just in the log: phase 1 was
status reads only, and toggles.txt was snapshotted before any
set was sent (`toggles-after-status.txt`).

## Boot

- "Enabling ExcelAimAssist v0.1.5" present; the enable line
  keeps its 0.1.4 shape and reports "2 saved player toggle(s)"
  (the seed loaded).
- "[PlaceholderAPI] Successfully registered internal
  expansion: aimassist [0.1.5]" and "Hooked into
  PlaceholderAPI (%aimassist_status%)." — the 0.1.4 menu /
  placeholder surface is intact at the new version.
- Done (35.897s). SEVERE: 0 in the whole boot + exercise +
  shutdown. The only ERROR lines are PlaceholderAPI's and
  Paper's own internet fetch failures (the sandbox has no Java
  internet — the same class the 0.1.4 rig report named), none
  attributable to ExcelAimAssist; no WARN/ERROR line names
  ExcelAimAssist at all.
- Clean stop: "Disabling ExcelAimAssist v0.1.5", exit code 0.

## Behavior exercise — 19/19 PASS (verbatim lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Boot reaches Done with v0.1.5 enabled | PASS |
| 2 | SEVERE count for the whole run | PASS — 0 |
| 3 | Seeded toggles loaded (2 saved toggles on the enable line) | PASS |
| 4 | `aimassist admin status <uuid>` explicit OFF → "Aim assist status for 11111111… (11111111…): OFF (explicit choice)." | PASS |
| 5 | `aimassist admin status <uuid>` explicit ON → "…: ON (explicit choice)." | PASS |
| 6 | `aimassist admin status <uuid>` no entry → "…: ON (default - no explicit choice stored)." | PASS |
| 7 | `aimassist admin status NobodyHereXYZ` → "No online player named 'NobodyHereXYZ', and it is not a UUID. Nothing was looked up." | PASS |
| 8 | Read-only on disk: after the status phase, toggles.txt (`toggles-after-status.txt`) is byte-identical to the seed — the reads created and changed nothing | PASS |
| 9 | `aimassist admin set <uuid> on` on the OFF target → "… set to ON (was OFF)." plus the plugin log line "Aim assist admin set by CONSOLE: …" | PASS |
| 10 | `aimassist admin status <same uuid>` after the set → "ON (explicit choice)." | PASS |
| 11 | `aimassist admin set <uuid> off` on the no-entry target → "… set to OFF (was ON)." (the default reads as the was-state; an explicit entry is created) | PASS |
| 12 | `aimassist admin set NobodyHereXYZ on` → "… Nothing was changed." | PASS |
| 13 | `aimassist admin set <uuid> maybe` → "Usage: /aimassist admin set <player> on\|off" (malformed value; nothing written) | PASS |
| 14 | Bare `aimassist admin` → both usage lines, mutates nothing | PASS |
| 15 | `aimassist admin bogus x` → both usage lines, mutates nothing | PASS |
| 16 | Bare `aimassist` from console → the 0.1.4 line verbatim: "aimassist is a per-player toggle - run it in game as /aimassist on\|off\|toggle." (self path unchanged) | PASS |
| 17 | `aimassist on` from console → the same 0.1.4 line (self forms still player-gated; the admin routing did not loosen them) | PASS |
| 18 | `papi list` → "A total of 1 placeholder hook(s) are active: aimassist"; `plugins` lists ExcelAimAssist (0.1.5) + PlaceholderAPI (2.12.3) | PASS |
| 19 | Final toggles.txt (`toggles-after.txt`) holds exactly the two set outcomes (11111111…=on, 22222222…=on untouched, 33333333…=off) and the server stops clean | PASS |

## Limitations, stated plainly

- Targets were addressed by UUID, because the rig has no
  joined player to resolve by name. The name path is the same
  `getPlayerExact` call the shape shares with the live-proven
  Quartermaster packreset resolution, and it is exercised live
  by the install packet's console smoke (status by real player
  name). An offline player known only by name cannot be
  resolved at all — the plugin keeps no name→UUID record and
  0.1.5 does not invent one; that limit is stated in the build
  record and the install packet, not papered over.
- No real player joined, so an in-game self-toggle flip by a
  real client was not observed in this rig. What the rig does
  prove about the self path: its console gate line is
  byte-for-byte the 0.1.4 line (checks 16–17), the self-service
  code below the new admin branch is unchanged source, and the
  ToggleStore toggle/set logic it runs on passes its carried
  suite unchanged (190/190 baseline checks inside the 234/234).
  The real-client flip is an owed player proof in the install
  packet, as it was for 0.1.4.
- The non-op in-game denial (a non-console sender without
  excelaimassist.admin) cannot be produced by a console-only
  rig; it is pinned by the AdminCommandsTest gate checks (the
  exact `allowed(...)` decision the handler runs) and owed as
  an optional live check in the install packet.
