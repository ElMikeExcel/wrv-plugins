# INSTALL PACKET — ExcelChunks 0.1.1 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands. 0.1.0 was a
FAIL: `/chunks` was self-only, so the console could not even
inspect the plugin's state. 0.1.1 closes it with the audit's
TINY packet: one global status on the existing command. Build
record: `BUILD-RECORD.md` in this folder.

## Why

Verified from the 0.1.0 source (audit + this build): the single
`/chunks` handler returns the in-game refusal to any non-player
sender and knows no other form; there is no admin node at all.
0.1.1 adds `/chunks status` — the plugin's enabled/config
state (version, the resync constants, the permission nodes) —
routed before the player-only gate, gated by the new
`excelchunks.admin` (default op) that the console satisfies.
The plugin holds no persistent per-player state (the only
per-player state is the in-memory 10-second cooldown), so a
global status is the whole passing surface. No config file, no
data change. Player-facing behaviour is unchanged.

## The artifact

- ExcelChunks-0.1.1.jar — 8,784 bytes
  SHA-256 8d9b47e3dfaa453322915fd216ddcf4820f15c3c33e6418f76a7cc7c19b33877
  Staged at ~/workspace/chunks-011-build/ExcelChunks-0.1.1.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This lineage has no
  in-jar build-ID fingerprint; version + hash identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelChunks-0.1.0.jar — 7,150 bytes — SHA-256
  cf4f08c835ba4ec1314b0b12460367e98d6f857dd4bd37ad16e6faaee6ce2948.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 0.1.0: at jar level only ExcelChunks.class
  changed and core/StatusCommand.class was added; plugin.yml
  (version, usage/description, the new admin permission) is
  the only resource change. There is NO config to merge (the
  plugin has none) and NO data migration. No permission
  grants required (console works at once; the admin node is
  new in this jar's plugin.yml and defaults to op).

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelChunks-0.1.0.jar →
   ExcelChunks-0.1.0.jar.bak-20261110 (adjust the date to
   the install day; keep alongside — only one ExcelChunks
   jar loadable at a time).
3. Upload ExcelChunks-0.1.1.jar as
   plugins/ExcelChunks-0.1.1.jar. Panel has no checksum
   field: verify by exact size, 8,784 bytes, in the file
   listing.
4. NO config edit, NO data change, NO other plugin change.
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelChunks v0.1.1" present.
- The enable line keeps its 0.1.0 shape:
  `ExcelChunks enabled: /chunks view resync ON (item radius 96 blocks, cooldown 10s)`
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `chunks status` → the three status lines:
   "ExcelChunks status: enabled (v0.1.1)"; the "View resync:
   ON - …" constants line; the "Config: no config file - …"
   line. (Live proof: these lines reading back in the log
   are the packet's post-install proof.)
2. `chunks` → the 0.1.0 refusal line verbatim:
   "ExcelChunks: /chunks only works in game - it refreshes
   the view of the player who runs it." (Self path unchanged
   for console.)

## Player proof

None owed — no player surface changes. Opportunistic only: a
non-op family member running `/chunks status` should get the
permission denial, and their own `/chunks` should keep working
exactly as before. The 0.1.0 player proof still owed
separately (Kailey's first live /chunks on a genuinely
desynced pile, then Rylie's) is NOT discharged by this packet.

## Rollback

Stop; delete ExcelChunks-0.1.1.jar; rename
ExcelChunks-0.1.0.jar.bak-20261110 → ExcelChunks-0.1.0.jar;
start; verify "Enabling ExcelChunks v0.1.0" and the 0.1.0
enable line. There is no data in either direction — nothing
to migrate back.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only command — no player
teaching owed; confirm no player-facing Chunks-admin claim
exists), What's New BOTH surfaces (website section AND the
in-game ExcelWhatsNew changelog — an operator-note line that
console status for ExcelChunks now exists is the only owed
text; no player-facing change), rotation tips checked (no
tip change — player surface unchanged; state the check in
the install report), in-game menus checked (ExcelMenu +
ExcelBedrockMenu — no menu change; state the check in the
install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelchunks-0.1.1-20261110/,
and a one-line note closing the audit's ExcelChunks FAIL row
(console-admin audit §2/§4) once the live console-status
proof above passes.
