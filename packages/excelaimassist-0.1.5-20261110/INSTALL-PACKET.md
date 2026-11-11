# INSTALL PACKET — ExcelAimAssist 0.1.5 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands for support
ops on a player's state. 0.1.4 was a FAIL: /aimassist was
self-only and player-gated, so an operator at the console could
neither inspect nor fix another player's toggle. 0.1.5 closes
it with the audit's SMALL packet: one admin subcommand family
on the existing command. (The audit recommended the number
0.1.4; the live menu-toggle build already holds it, so the
packet is 0.1.5 per the audit folder's supplement.) Build
record: `BUILD-RECORD.md` in this folder.

## Why

Verified from the 0.1.4 source (audit + this build): the
single /aimassist handler returns at a `sender instanceof
Player` gate before any argument is read, and plugin.yml
declares no admin node — from the console every form, even a
status read, is refused with the per-player line. A player
whose toggle is wrong (set by accident, flipped by a menu
double-tap, needed OFF for a test) could only be fixed by that
player in game. 0.1.5 adds `/aimassist admin status <player>`
and `/aimassist admin set <player> on|off` on the SAME
ToggleStore the self toggle, the menus and %aimassist_status%
read — status spells out explicit-vs-default, set writes
through the store's own set() and persists immediately —
gated by the new `excelaimassist.admin` (default op; console
explicitly allowed), routed before the player gate. No aim
behaviour, no config, no menu or placeholder change.

## The artifact

- ExcelAimAssist-0.1.5.jar — 22,745 bytes
  SHA-256 9e5d926b74f0ddaba9b0d9adaaacf9cd41ace56a15a97dbe44a7a28b1fbc7a50
  Staged at ~/workspace/aimassist-015-build/ExcelAimAssist-0.1.5.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This lineage has no
  in-jar build-ID fingerprint; version + hash identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelAimAssist-0.1.4.jar — 19,401 bytes — SHA-256
  7d040d98fc063418b3635c676e8a6f3db5adf093a4e99dec28a8f1c1ac1e6a44.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 0.1.4: new AdminCommands core class, the admin
  dispatch + handler in ExcelAimAssist, plugin.yml (version,
  usage, the new admin node). config.yml ships byte-identical
  content (no new keys) — do NOT overwrite the live config. No
  data migration: toggles.txt format is untouched and read
  exactly as before. No permission grants required (console
  works at once; the admin node defaults to op). NOTE: the
  Server Menu trio and the Bedrock form are NOT part of this
  swap — they read the same, unchanged contracts.

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelAimAssist-0.1.4.jar →
   ExcelAimAssist-0.1.4.jar.bak-20261110 (adjust the date to
   the install day; keep alongside — only one ExcelAimAssist
   jar loadable at a time).
3. Upload ExcelAimAssist-0.1.5.jar as
   plugins/ExcelAimAssist-0.1.5.jar. Panel has no checksum
   field: verify by exact size, 22,745 bytes, in the file
   listing.
4. NO config edit, NO data change, NO other plugin change
   (in particular NOT ExcelBedrockMenu or server_menu.yml —
   they belong to the 0.1.4 trio and stay as they are).
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelAimAssist v0.1.5" present.
- "[PlaceholderAPI] Successfully registered internal
  expansion: aimassist [0.1.5]" and "[ExcelAimAssist] Hooked
  into PlaceholderAPI (%aimassist_status%)." — the menu /
  placeholder surface survived the bump.
- The enable line keeps its 0.1.4 shape: "ExcelAimAssist
  enabled: ON, cone 15.0 deg, range 40.0 blocks, aim height
  0.6, drop compensation ON, target lead ON (max 5.0 blocks,
  min target speed 0.5 blocks/sec), N saved player toggle(s)"
  with the LIVE saved-toggle count.
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `aimassist admin status <a real family member BY NAME>` →
   the one-line status (state + explicit/default basis). This
   is the name-resolution live proof the rig could not give
   (it addressed UUIDs); the line reading back in the log is
   the packet's post-install proof.
2. `aimassist admin status <that member's UUID>` → the same
   state (the offline/UUID form).
3. `aimassist admin set <member> <the state they already
   have>` → the set line with was-state equal to the new state
   (a truthful no-op write; do NOT flip a real player's toggle
   for a smoke — a genuine need is the only reason to change
   one, and the player can flip it back in game either way).
4. `aimassist admin status NobodyHereXYZ` → "No online player
   named 'NobodyHereXYZ', and it is not a UUID. Nothing was
   looked up."
5. `aimassist admin` → both usage lines; `aimassist` →
   the per-player line (the self gate is intact from console).
6. Optional, only if a non-op player is online and willing:
   that player runs `aimassist admin status <anyone>` → the
   denial naming excelaimassist.admin. (Harness-pinned; skip
   rather than stage it.)

## Player proof

One, owed after install (the rig has no real client):
1. A player (Mike or any family member) flips their own assist
   once in game — `/aimassist toggle` or the Server Menu
   button — and /aimassist reports the new state; console
   `aimassist admin status <that player>` agrees with it.
That single check closes the self path end-to-end at 0.1.5;
no other player-visible behaviour changes.

## Rollback

Stop; delete ExcelAimAssist-0.1.5.jar; rename
ExcelAimAssist-0.1.4.jar.bak-20261110 →
ExcelAimAssist-0.1.4.jar; start; verify "Enabling
ExcelAimAssist v0.1.4", the aimassist expansion registered at
[0.1.4], and normal toggle/menu behaviour. Data files are
compatible in both directions — 0.1.5 wrote no new state of
any kind (an admin set is an ordinary toggles.txt entry, which
0.1.4 reads natively).

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only commands — no player
teaching owed; confirm no player-facing AimAssist-admin claim
exists), What's New BOTH surfaces (website section AND the
in-game ExcelWhatsNew changelog — an operator-note line that
console status/set for a player's aim toggle now exists is
the only owed text; no player-facing change), rotation tips
checked (no tip change — player surface unchanged; state the
check in the install report), in-game menus checked
(ExcelMenu + ExcelBedrockMenu — no menu change; the Aim Assist
button keeps working on the unchanged contracts; state the
check in the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelaimassist-0.1.5-20261110/,
and a one-line note closing the audit's ExcelAimAssist FAIL
row (console-admin audit §2/§4) once the live console-status
proof above passes.
