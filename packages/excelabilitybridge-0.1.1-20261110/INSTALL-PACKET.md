# INSTALL PACKET — ExcelAbilityBridge 0.1.1 (console admin)

An Excel plugin by Iris / Mike for Wink Reyes Valley. Prepared
Nov 10, 2026. Status: BUILT + STAGED ONLY — do not install from
this packet without a proper install window under the standing
gates (the parent/failsafe installs; this desk packet installs
nothing). Commissioned from the console-admin coverage audit
(Nov 9, 2026) under Mike's Nov 3, 2026 console-admin rule: every
Excel plugin ships console-runnable admin commands.
0.1.0 was a FAIL: `/abilities` was player-only, so the console
could not inspect the bridge's link state. 0.1.1 closes it with
the audit's TINY packet: one global status on the existing
command. Build record: `BUILD-RECORD.md` in this folder.

## Why

Verified from the 0.1.0 source (audit + this build): the single
`/abilities` handler returns "The ability bridge is for players
in game." to any non-player sender; there is no admin node at
all. 0.1.1 adds `/abilities status` — the bridge's link state
(EliteMobs present/version + whether its Advanced Combat
System answers through the bridge's gateway; Geyser-Spigot +
floodgate presence; the debug flag) — routed before the
player-only gate, gated by the new `excelabilitybridge.admin`
(default op) that the console satisfies. Per-player ability
state belongs to EliteMobs, not the bridge, so a global status
is the whole passing surface. No config key, no data change.
Player-facing behaviour is unchanged; /abilities stays open
to every player (the new node is checked inside the status
branch only, deliberately not as a command-level permission).

## The artifact

- ExcelAbilityBridge-0.1.1.jar — 30,606 bytes
  SHA-256 62076c1541673fd0d1ffb22be46be401d2475c297ce9f71423c645e7dc323e55
  Staged at ~/workspace/abilitybridge-011-build/ExcelAbilityBridge-0.1.1.jar
  (build folder root; build record + SOURCE record alongside).
  Deterministic double-build byte-identical. This
  lineage has no in-jar build-ID fingerprint; version + hash
  identify it.
- **Live-jar gate (what must be live before the swap):**
  ExcelAbilityBridge-0.1.0.jar — 28,048 bytes — SHA-256
  51f7dd303ac33115135b4415e09c8de153a93d1437650cb4cb0587c8d80740c9.
  If the live jar does not hash to this, STOP — an unmatched
  live jar is a lineage question (source recovery first), not
  a swap.
- Delta vs live 0.1.0: at jar level only AbilitiesCommand.class
  and ExcelAbilityBridge.class changed and StatusCommand.class
  was added; config.yml ships byte-identical content (no new
  keys) — do NOT overwrite the live config. plugin.yml is the
  only other change (version, usage/description, the new admin
  permission; its author line matches the live jar's already).
  No data migration. No permission grants required (console
  works at once; the admin node is new in this jar's plugin.yml
  and defaults to op).

## The swap (one jar; standard gates)

Preconditions: `list` = 0 twice per the standing gate; marker
discipline (~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt
at start, deleted at finish, foreign-marker re-checks before
Shut Down and before Start); ONE fresh backup for the window
with cap-at-6 pruning (name prunees; never the Daily Backup's
newest copy or the two newest manual restore points).

1. Stop the server fully (panel Shut Down — console `stop`
   auto-restarts on this host).
2. Rename the live jar: plugins/ExcelAbilityBridge-0.1.0.jar →
   ExcelAbilityBridge-0.1.0.jar.bak-20261110 (adjust the date
   to the install day; keep alongside — only one
   ExcelAbilityBridge jar loadable at a time).
3. Upload ExcelAbilityBridge-0.1.1.jar as
   plugins/ExcelAbilityBridge-0.1.1.jar. Panel has no checksum
   field: verify by exact size, 30,606 bytes, in the file
   listing.
4. NO config edit, NO data change, NO other plugin change.
5. Start the server; run the boot-verify + console smoke below.

## Boot-verify (all must hold; any failure → rollback)

- "Enabling ExcelAbilityBridge v0.1.1" present.
- The enable takes one of the 0.1.0 healthy shapes: "Bedrock
  ability bridge ready: /abilities …" OR the standing-by line
  followed later by "EliteMobs class system (ACS) is up -
  Bedrock ability bridge online: /abilities". A stand-down
  line ("not found or not enabled" / "could not be reached")
  means the EliteMobs seam broke — STOP and report.
- Zero SEVERE attributable to the swap; plugin count unchanged
  from pre-swap; guildgate LOCKED count unchanged.

## Console smoke (from the Shockbyte console, immediately post-boot)

1. `abilities status` → the four status lines. If it runs
   before the "bridge online" line, the EliteMobs link
   honestly reads STANDING BY; run it again after the online
   line and it must read "EliteMobs link: UP - EliteMobs
   10.9.8 enabled, Advanced Combat System READY." (Live
   proof: these lines reading back in the log are the
   packet's post-install proof.)
2. `abilities` → "The ability bridge is for players in
   game." (Player path unchanged for console.)

## Player proof

None owed — no player surface changes. Opportunistic only: a
non-op family member running `/abilities status` should get
the permission denial, and a Bedrock player's own
`/abilities` form should keep working exactly as before.

## Rollback

Stop; delete ExcelAbilityBridge-0.1.1.jar; rename
ExcelAbilityBridge-0.1.0.jar.bak-20261110 →
ExcelAbilityBridge-0.1.0.jar; start; verify "Enabling
ExcelAbilityBridge v0.1.0" and one of the 0.1.0 healthy
enable shapes. There is no data in either direction —
nothing to migrate back.

## Same-pass surfaces owed at install

Per the same-pass rule, closing this install also verifies:
Guide knowledge + intents (operator-only command — no player
teaching owed; confirm no player-facing AbilityBridge-admin
claim exists), What's New BOTH surfaces (website section AND
the in-game ExcelWhatsNew changelog — an operator-note line
that console status for AbilityBridge now exists is the only
owed text; no player-facing change), rotation tips checked
(no tip change — player surface unchanged; state the check
in the install report), in-game menus checked (ExcelMenu +
ExcelBedrockMenu — no menu change; state the check in the
install report).

## Records owed at install

INSTALLED marker + daily log, Route Sheet row, Big Brain
(local + shared + Drive), records package already staged at
~/workspace/records-catchup-20261103/packages/excelabilitybridge-0.1.1-20261110/,
and a one-line note closing the audit's ExcelAbilityBridge
FAIL row (console-admin audit §2/§4) once the live
console-status proof above passes.
