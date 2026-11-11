# INSTALL PACKET — ExcelTreeCleanup 0.1.1 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands for support
ops on a player's state. 0.1.0 was a FAIL: `/treecleanup on|off`
was self-only and player-gated, so the console could neither
inspect nor fix another player's toggle. 0.1.1 closes it with
the audit's SMALL packet: one admin subcommand family on the
existing command. Build record: `BUILD-RECORD.md` in this
folder.

## Why

Verified from the 0.1.0 source (audit + this build): the single
`/treecleanup` handler returns the player-only guidance to any
non-player sender and knows only the self forms (query, on,
off); there is no admin node at all. 0.1.1 adds
`/treecleanup admin status <player>` and
`/treecleanup admin set <player> on|off` — inspection and fix
of the player's real ToggleStore state (effective state plus
explicit-vs-default), gated by the new `exceltreecleanup.admin`
(default op) that the console satisfies, routed before the
player-only gate. No config key, no data change. Player-facing
behaviour is unchanged; the self toggle stays ungated (the new
node is checked inside the admin branch only, deliberately not
as a command-level permission).

## The artifact

- ExcelTreeCleanup-0.1.1.jar — 24,389 bytes
  SHA-256 66e5f4a431fa920fc9f7a9eb582d045f0486b9bc2356555e0d8b422778dc21a7
  Staged at ~/workspace/treecleanup-011-build/ExcelTreeCleanup-0.1.1.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This lineage has no
  in-jar build-ID fingerprint; version + hash identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelTreeCleanup-0.1.0.jar — 20,515 bytes — SHA-256
  0507acefca25ec83858d25ab9dfbd17c47f230fd708939a86b357080bb883423.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 0.1.0: new core/AdminCommand, the admin
  dispatch + handlers in ExcelTreeCleanup, plugin.yml
  (version, usage/description, the new admin permission).
  config.yml ships byte-identical content (no new keys) — do
  NOT overwrite the live config. No data migration:
  `toggles.txt` is untouched in format and read exactly as
  before. No permission grants required (console works at
  once; the admin node is new in this jar's plugin.yml and
  defaults to op).

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelTreeCleanup-0.1.0.jar →
   ExcelTreeCleanup-0.1.0.jar.bak-20261110 (adjust the date to
   the install day; keep alongside — only one
   ExcelTreeCleanup jar loadable at a time).
3. Upload ExcelTreeCleanup-0.1.1.jar as
   plugins/ExcelTreeCleanup-0.1.1.jar. Panel has no checksum
   field: verify by exact size, 24,389 bytes, in the file
   listing.
4. NO config edit, NO data change, NO other plugin change.
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelTreeCleanup v0.1.1" present.
- The enable line keeps its 0.1.0 shape:
  `ExcelTreeCleanup enabled: ON, remnant max 3 logs, flood cap 512, <live count> saved player toggle(s), GriefPrevention claim checks ON`
  (the claim-checks tail must read ON — GriefPrevention is
  live. If it reads OFF, STOP and report: the claim seam
  failed to link.)
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `treecleanup admin status <a real family member, or their UUID>` →
   the one-line status: ON/OFF plus explicit-vs-default.
   (Live proof: this line reading back in the log is the
   packet's post-install proof.)
2. `treecleanup admin status 99999999-9999-9999-9999-999999999999` →
   "ON (default - no explicit choice stored)." and no record is
   created by the read.
3. `treecleanup admin status NobodyHereXYZ` → "No online player
   named 'NobodyHereXYZ', and it is not a UUID. Nothing was
   looked up."
4. `treecleanup admin` → the combined admin usage line; mutates
   nothing.
5. Do NOT smoke a real admin set outside a genuine need — set
   is a mutating op (rig-proven); if a genuine fix is needed,
   the set's audit line in the log is its record.

## Player proof

None owed — no player surface changes. Opportunistic only: a
non-op family member running `/treecleanup admin status <anyone>`
should get the permission denial, and their own
`/treecleanup on|off` should keep working exactly as before.
The 0.1.0 player proof still owed separately (one real chop
leaving a treetop, Java and Bedrock) is NOT discharged by this
packet.

## Rollback

Stop; delete ExcelTreeCleanup-0.1.1.jar; rename
ExcelTreeCleanup-0.1.0.jar.bak-20261110 →
ExcelTreeCleanup-0.1.0.jar; start; verify "Enabling
ExcelTreeCleanup v0.1.0" and the 0.1.0 enable line. Data files
are compatible in both directions — toggles.txt format is
unchanged, and an explicit choice stored by a 0.1.1 admin set
is an ordinary toggle row that 0.1.0 reads natively.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only commands — no player
teaching owed; confirm no player-facing TreeCleanup-admin
claim exists), What's New BOTH surfaces (website section AND
the in-game ExcelWhatsNew changelog — an operator-note line
that console status/set for TreeCleanup toggles now exists is
the only owed text; no player-facing change), rotation tips
checked (no tip change — player surface unchanged; state the
check in the install report), in-game menus checked (ExcelMenu
+ ExcelBedrockMenu — no menu change; state the check in the
install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/exceltreecleanup-0.1.1-20261110/,
and a one-line note closing the audit's ExcelTreeCleanup FAIL
row (console-admin audit §2/§4) once the live console-status
proof above passes.
