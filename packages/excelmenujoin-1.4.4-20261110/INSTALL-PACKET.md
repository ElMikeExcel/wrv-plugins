# INSTALL PACKET — ExcelMenuJoin 1.4.4 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands for support
ops on a player's state. Live 1.4.3 was a FAIL (the audit read
1.4.1; the FAIL was re-verified against the 1.4.3 source during
the build): /excelmenujoin and /compass are both self-only and
player-gated, so an operator at the console can neither inspect
nor fix another player's join-menu state, nor hand them a
replacement compass. 1.4.4 closes it with the audit's SMALL
packet: one admin subcommand family on the existing command.
(The audit's recommended number was 1.4.2; that version — and
1.4.3 — already shipped, so the packet is 1.4.4. See
BUILD-RECORD.md's re-base correction.) Build record:
`BUILD-RECORD.md` in this folder.

## Why

Verified from the 1.4.3 source (the shipped packet tree): both
command handlers return at a `sender instanceof Player` gate
before any argument is read, and plugin.yml declares no admin
node — from the console every form is refused with "Only
players can use this command." A player whose join auto-open
is wrong, or who lost their menu compass while an operator is
at the console, could only be fixed by that player in game.
1.4.4 adds `/excelmenujoin admin status <player>`,
`/excelmenujoin admin toggle <player>` and
`/excelmenujoin admin compass <player>` — status and toggle
act on the player's real auto-open flag (the PDC entry the
self toggle, the join handler and %menujoin_status% all read,
flipped through the same setDisabled write), compass gives
through the plugin's single give path so the 1.4.0 Utility
Pouch no-duplicate rule applies — gated by the new
`excelmenujoin.admin` (default op; console explicitly
allowed), routed before the player gate. No join/compass/tap
behaviour (the 1.4.2 air-tap and 1.4.3 preprocess/session
fixes are carried untouched), no config (the plugin has
none), no menu or placeholder change.

## The artifact

- ExcelMenuJoin-1.4.4.jar — 14,080 bytes
  SHA-256 4df80550fa4ecf653534c32c5eacd597ad4f24fd0fd8d1af0956cf71ee7c5068
  Staged at ~/workspace/menujoin-142-build/ExcelMenuJoin-1.4.4.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This lineage has no
  in-jar build-ID fingerprint; version + hash identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelMenuJoin-1.4.3.jar — 11,678 bytes — SHA-256
  178febcce9da81127e25dce07b99a985db63a91dbbfde5d7a434283794f9bfd7.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 1.4.3: new AdminCommands class, the admin
  dispatch + handler in ExcelMenuJoin, plugin.yml (version,
  usage, the new admin node). There is NO config file in this
  plugin — nothing to overwrite or preserve. No data
  migration: the toggle state lives in player PDC and is read
  exactly as before (both namespaces, same dual-write). No
  permission grants required (console works at once; the
  admin node defaults to op). NOTE: ExcelMenu and
  server_menu.yml are NOT part of this swap — they read the
  same, unchanged %menujoin_status% contract.

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelMenuJoin-1.4.3.jar →
   ExcelMenuJoin-1.4.3.jar.bak-20261110 (adjust the date to
   the install day; keep alongside — only one ExcelMenuJoin
   jar loadable at a time).
3. Upload ExcelMenuJoin-1.4.4.jar as
   plugins/ExcelMenuJoin-1.4.4.jar. Panel has no checksum
   field: verify by exact size, 14,080 bytes, in the file
   listing.
4. NO config edit (none exists), NO data change, NO other
   plugin change (in particular NOT ExcelMenu or
   server_menu.yml).
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelMenuJoin v1.4.4" present.
- "[ExcelMenuJoin] Hooked into PlaceholderAPI
  (%menujoin_status%)." and "[ExcelMenuJoin] ExcelMenuJoin
  enabled." — the menu/placeholder surface survived the bump
  (the 1.4.3 install record's expansion line shape:
  menujoin [1.4.4]).
- NO UnknownDependencyException and no ExcelMenuJoin load
  error anywhere in the boot (the 1.4.0 failure mode stays
  gone — the no-depend guard is harness-pinned).
- ExcelMenu self-check: all menus loaded, 0 failed (the count
  is whatever the then-current menu set holds — do not pin a
  number from an older set).
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

All three admin forms need their target ONLINE (the state
lives on the player — there is no offline form), so run this
smoke while a family member is in game, or fold it into the
player proof below.

1. `excelmenujoin admin status <a real family member BY NAME>`
   → the one-line status (join auto-open ON/OFF; menu compass
   held / not held). This line reading back in the log is the
   packet's post-install proof.
2. `excelmenujoin admin status NobodyHereXYZ` → "No online
   player named 'NobodyHereXYZ'. … Nothing was looked up."
3. `excelmenujoin admin` → all three usage lines; mutates
   nothing.
4. `excelmenujoin` → "Only players can use this command."
   (the self gate is intact from console).
5. Do NOT smoke a real toggle or compass give on a family
   member just to test: toggle flips their actual join
   behaviour and compass gives an actual item. A genuine need
   is the only reason to run one — or run the toggle TWICE on
   a willing member (flip and flip back, ending in their
   original state, both lines in the log) ONLY with their
   agreement in the moment.

## Player proof

Two, owed after install:
1. The compass proof carried since 1.4.2/1.4.3, first in
   line: a player joins — compass present, server menu
   auto-opens; tap the compass IN THE AIR → Server Menu
   opens (the 1.4.2 fix, still player-unproven per the
   canon); click it against a block → Server Menu opens.
2. While that player is online, the console smoke above runs
   against their name (status read back in the log), closing
   the admin surface live. If they have a compass parked in
   the Utility Pouch, an `admin compass` returning the
   not-given line additionally proves the pouch rule live —
   only if the situation arises naturally; do not stage it.
No other player-visible behaviour changes.

## Rollback

Stop; delete ExcelMenuJoin-1.4.4.jar; rename
ExcelMenuJoin-1.4.3.jar.bak-20261110 →
ExcelMenuJoin-1.4.3.jar; start; verify "Enabling
ExcelMenuJoin v1.4.3", the PlaceholderAPI hook line, and
normal compass behaviour. Player data is compatible in both
directions — 1.4.4 wrote no new state of any kind (an admin
toggle is an ordinary PDC flag write, which 1.4.3 reads
natively; an admin compass give is an ordinary menu compass).

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only commands — no player
teaching owed; confirm no player-facing MenuJoin-admin claim
exists), What's New BOTH surfaces (website section AND the
in-game ExcelWhatsNew changelog — an operator-note line that
console status/toggle/compass for a player's join-menu state
now exists is the only owed text; no player-facing change),
rotation tips checked (no tip change — player surface
unchanged; state the check in the install report), in-game
menus checked (ExcelMenu + the Bedrock form path — no menu
change; the join-toggle element keeps working on the
unchanged %menujoin_status% contract; state the check in the
install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelmenujoin-1.4.4-20261110/,
and a one-line note closing the audit's ExcelMenuJoin FAIL
row (console-admin audit §2/§4) once the live console-status
proof above passes — noting the audit's version label (1.4.1)
was stale and the FAIL was closed against live 1.4.3 → 1.4.4.
