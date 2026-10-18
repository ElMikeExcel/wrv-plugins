# VERIFICATION — Java "Skills & Perks" menu packet

Date: Oct 17, 2026. Method: Moon's playbook (baseline provenance, checksums,
scratch boot on the exact live plugin jar, cold re-check of the failure that
mattered).

## Baseline provenance

- `current/server_menu.yml` and `current/commands_menu.yml` are byte copies
  of the teleport-confirm packet's `deploy/` files
  (`~/workspace/java-menu-spawn-confirm/deploy/`), whose own provenance is
  recorded in that packet (live files reconstructed from the wrv-plugins
  PR #33 record + the Oct 16 Party item). This packet therefore stacks on the
  confirm packet; see the dependency note in INSTALL-PACKET.md.
- `current/config.yml` is the confirm packet's scratch DeluxeMenus
  config.yml: the live-backup config (Oct 15) plus the confirm packet's four
  `gui_menus:` registrations — 13 menus, matching its VERIFICATION.md
  ("13 registered in DeluxeMenus/config.yml").
- DeluxeMenus jar: the live 1.14.1 build from the Oct 15 backup,
  SHA-256 `ec10a131…` (same jar the confirm packet scratch-verified with).
  Paper: 26.2 build 123, jar SHA-256
  `7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08`
  (byte-identical to the confirm packet's recorded jar). Java 25
  (`~/workspace/.jdk25`, OpenJDK 25.0.4.1) — matches live.

## The catch this packet exists because of

First scratch boot loaded **13 menus, not 14**, with `skills_perks.yml`
present in gui_menus/ and NO error anywhere in the log. Bisection: a
byte-copy of a working menu (zz_test_copy.yml) also failed to load. Root
cause found in DeluxeMenus config.yml: 1.14 loads ONLY menus registered in
the `gui_menus:` map in config.yml; unregistered files are silently ignored.
Fix: the packet now includes the two-line registry addition
(diffs/config.yml.diff), exactly like the confirm packet's step 4.

## Scratch result (final, boot-skills7.log in scratch/)

- Boot clean on Paper 26.2-123 + DeluxeMenus 1.14.1 + PlaceholderAPI 2.12.3.
- `[DeluxeMenus] 14 GUI menus loaded!` (13 baseline + skills_perks).
- `dm reload` → "DeluxeMenus successfully reloaded!"
- `dm list` pages show `- skills_perks.yml - skills_perks` among 14 menus.
- Zero DeluxeMenus load errors; the only ERROR lines in the log are the
  sandbox-proxy SSL artifacts (Mojang/PlaceholderAPI update fetches), an
  environment effect also present in the confirm packet's boots.
- The one remaining unknown is the same one the confirm packet carries: only
  a real Java click proves the open flow in game. That check is listed in
  INSTALL-PACKET.md step 7 and stays a separate status.

## Checksums

See SHA256SUMS.txt (current/ and deploy/ for all four files).
commands_menu.yml is byte-identical between current/ and deploy/
(SHA-256 `bdfb5052…`).
