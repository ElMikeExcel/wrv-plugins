# INSTALL PACKET — ExcelQuartermaster 0.3.1 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands for support
ops on a player's state. 0.3.0 was a FAIL: `/qm packreset` (the
fix op) was console-runnable, but the console had no way to
inspect a player's pack state at all. 0.3.1 closes it with the
audit's TINY packet: one read-only subcommand. Build record:
`BUILD-RECORD.md` in this folder.

## Why

Verified from the 0.3.0 source (audit + this build): the single
`/qm` handler knows exactly one subcommand, `packreset`; there
is no status form, so an operator deciding whether a reset is
even needed had to guess at a player's offers-used count and
pack outcome. 0.3.1 adds `/qm status <player>` — offers used and
pack state from the player's real PackPrompt record (the same
record packreset acts on), read-only, gated by the existing
`excelquartermaster.admin` (default op) that the console
already satisfies for packreset. No new permission node, no
config key, no data change. Player-facing behaviour is
unchanged.

## The artifact

- ExcelQuartermaster-0.3.1.jar — 79,785 bytes
  SHA-256 a11aa0fcab31544ffeb92c6ab31207412e797bb8d1147eb73d34e4375d0fa2f9
  Staged at ~/workspace/quartermaster-031-build/ExcelQuartermaster-0.3.1.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This lineage has no
  in-jar build-ID fingerprint; version + hash identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelQuartermaster-0.3.0.jar — 77,066 bytes — SHA-256
  70096fc47b61f09131aa42c1f3a66fbee3c248085d3d39f476338cb01b15765a.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 0.3.0: new PackStatus formatter, the status
  dispatch + handler in ExcelQuartermaster, plugin.yml
  (version, usage, permission description). config.yml ships
  byte-identical content (no new keys) — do NOT overwrite the
  live config. No data migration: `player-tales.txt`,
  `java-pack-state.txt`, and `issued-wands.txt` are untouched
  in format and read exactly as before. No permission grants
  required (console works at once; the admin node already
  exists and defaults to op).

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelQuartermaster-0.3.0.jar →
   ExcelQuartermaster-0.3.0.jar.bak-20261110 (adjust the date to
   the install day; keep alongside — only one
   ExcelQuartermaster jar loadable at a time).
3. Upload ExcelQuartermaster-0.3.1.jar as
   plugins/ExcelQuartermaster-0.3.1.jar. Panel has no checksum
   field: verify by exact size, 79,785 bytes, in the file
   listing.
4. NO config edit, NO data change, NO other plugin change.
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelQuartermaster v0.3.1" present.
- The four enable lines keep their 0.3.0 shape (tales/anchor,
  Bedrock presentation, Java parity with the live count of
  players with saved pack state, wand issue with the live
  issued count / seal count / Magic presence).
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `qm status <a real family member, or their UUID>` → the
   one-line status: offers used N of 5, remaining, pack state,
   last change. (Live proof: this line reading back in the log
   is the packet's post-install proof.)
2. `qm status 99999999-9999-9999-9999-999999999999` → the
   no-record line (0 of 5; nothing is created by the read).
3. `qm status NobodyHereXYZ` → "No online player named
   'NobodyHereXYZ', and it is not a UUID. Nothing was looked up."
4. `qm` → both usage lines (packreset + status); mutates nothing.
5. Do NOT smoke a real packreset outside a genuine need —
   packreset is a mutating op and is already live-proven.

## Player proof

None owed — no player surface changes. The first real console
status lookup for a family member is the live exercise; the
line in the server log is its record.

## Rollback

Stop; delete ExcelQuartermaster-0.3.1.jar; rename
ExcelQuartermaster-0.3.0.jar.bak-20261110 →
ExcelQuartermaster-0.3.0.jar; start; verify "Enabling
ExcelQuartermaster v0.3.0" and normal QM behaviour (tales,
pack offers, wand issue). Data files are compatible in both
directions — 0.3.1 wrote no new state of any kind.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only command — no player
teaching owed; confirm no player-facing QM-admin claim exists),
What's New BOTH surfaces (website section AND the in-game
ExcelWhatsNew changelog — an operator-note line that console
status for QM pack state now exists is the only owed text; no
player-facing change), rotation tips checked (no tip change —
player surface unchanged; state the check in the install
report), in-game menus checked (ExcelMenu + ExcelBedrockMenu —
no menu change; state the check in the install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelquartermaster-0.3.1-20261110/,
and a one-line note closing the audit's ExcelQuartermaster FAIL
row (console-admin audit §2/§4) once the live console-status
proof above passes.
