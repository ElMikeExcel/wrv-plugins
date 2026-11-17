# RECEIPTS — BB-08 ExcelLeaderboard 1.3.2 (Back-button pass, run 355)

All timestamps America/Chicago (CST), Sat Nov 14, 2026.

## G1 live-premise receipt (Homebase route, no fallback)

- Probes: `ssh -F ~/.ssh/config mike-pc "hostname"` x3 over
  17:48-17:49 CST -> DESKTOP-IL05DQR, exit 0, PASS 3/3. No fallback
  used. (Relay had flapped earlier in the day per run 354; it was
  stable for this whole run.)
- Pull: rclone on the PC from remote `shockbyte:1. Michael's Server`,
  PC staging `muse-temp\g1-r355-bb08b`, then scp down to `pull-g1/`.
  Pull completed 17:50 CST. PC staging + pull scripts deleted after
  the pull (verified Test-Path False / False).
- Census (pull-g1/lsf-files.txt, rclone lsf --files-only of the live
  plugins dir, include ExcelLeaderboard*.jar): exactly ONE active
  jar: `ExcelLeaderboard-1.3.1.jar`. No 1.3.0 jar live.
- Live path: `plugins/ExcelLeaderboard-1.3.1.jar`
- Pulled copy: `pull-g1/ExcelLeaderboard-1.3.1-live.jar`
- Size: 57,897 B
- SHA-256: 3b2c1f1c11faf9e1bdadd77fc25e5f9c905f3eadb2a472d01da6919e978060ee
- In-jar plugin.yml version: 1.3.1 (read from the pulled jar).

## Delta vs the audit (RE-BASE — reported per the brief)

- Audited version (AUDIT.md, FIXBATCH-SPLIT-R348.md BB-08): 1.3
  (local tree ExcelLeaderboard-1.3, jar 1.3.0, 54,070 B /
  8cf1343e3712191092428326d4a9329fc54f6af0eb31bad18bda3068f388b8ce).
- Live is 1.3.1, which shipped before Nov 3, 2026 (Nov 3 source
  census already flagged "Leaderboard 1.3.1" as panel-only). No
  local 1.3.1 source tree exists anywhere in the workspace.
- Class-level delta, live 1.3.1 vs local 1.3.0 build: plugin.yml
  version only; NEW core/VanillaStatsCache (+ Totals, Slot);
  CHANGED ExcelLeaderboard, ExcelLeaderboard$Snapshot,
  SoftAdapters; config.yml identical. ALL presentation classes
  (BoardMenu, BoardPresentation, BedrockBoardForms, ChatBoardMenu,
  Category) byte-identical to the audited 1.3.0 build.
- Row re-check against live 1.3.1 bytecode (javap): the three BB-08
  rows still hold exactly — main grid, main form, and main chat
  panel carry no Server Menu return; board views still carry
  "All boards" Back. No row changed, none already fixed, none gone.
- 1.3.1's functional delta (from bytecode): an offline vanilla
  stats cache — refresh recomputes a player's six vanilla totals
  only when the stats file stamp (mtime * 31 + size) changes;
  online players / missing files / unknown stats folder are never
  cached (log line: "offline stats are read uncached (1.3.0
  behavior)"); SoftAdapters probe catches widened with LinkageError.

## Base gate (reconstructed 1.3.1 rebuild)

- Reconstructed tree: `base-1.3.1/` in this folder (1.3.0 tree +
  VanillaStatsCache.java written from the live bytecode, the
  ExcelLeaderboard cache wiring, the SoftAdapters catch widening,
  plugin.yml 1.3.1). SHA256SUMS in the tree: 27/27 OK.
- Rebuilt jar: `base-1.3.1/target/ExcelLeaderboard-1.3.1.jar`,
  SHA-256 32a21e96c0886fe60940caa624a8d486986bcb2360d51a1bca33eace84fbb54e.
- Entry comparison vs the live jar (33 entries): 28 byte-identical
  (incl. SoftAdapters, Snapshot, every presentation class, plugin.yml,
  config.yml). The 5 differing entries: MANIFEST.MF (Created-By JDK
  patch string only: live 25.0.5, local JDK 25.0.4.1) and 4 classes
  (ExcelLeaderboard, VanillaStatsCache, VanillaStatsCache$Totals,
  VanillaStatsCache$Slot) whose javap -c instruction streams are
  IDENTICAL to live (normalized diff = 0 lines for all four) —
  they differ only in LineNumberTable entries, i.e. the original
  author's source line layout, which is not recoverable from the
  jar and has no runtime effect.
- Base suite at reconstruction: 188 assertions / 0 failures (the
  exact 1.3.0 suite counts, incl. LiveApiSignatureTests 15).
- Verdict: base gate PASSES on bytecode identity. The 1.3.2 build
  stands on this reconstructed 1.3.1 tree, so no live behaviour
  (stats cache included) is lost.

## Candidate (1.3.2)

- Artifact: `build-1.3.2/target/ExcelLeaderboard-1.3.2.jar`
- Size: 59,635 B
- SHA-256: 1d7e6d320219def16abe8731184aff7cf7426d52c1b8779d87ff6e1a2da01d01
- Determinism: two full build.sh runs, jar byte-identical.
- Build-folder sums: `build-1.3.2/SHA256SUMS` (30 entries, all OK).
- Delta vs reconstructed 1.3.1 (entry comparison): changed =
  BoardPresentation (+ its ChatLine/Kind inner classes, line-table
  shifts), BoardMenu, BedrockBoardForms (+ ResponseGate, line-table
  shift), plugin.yml (version); new = paper/ServerMenu.class.
  ChatBoardMenu and everything else byte-identical.
- Board-view safety proof (javap -c, normalized): openBoard in
  BoardMenu = 0 diff; openBoard in BedrockBoardForms = 0 diff;
  boardChatLines = 0 diff; boardContent = 0 diff;
  ChatBoardMenu.openBoard = 0 diff. Board views are untouched.

## Tests (1.3.2, both build runs)

247 assertions / 0 failures: BoardMathTests 25, LedgerTests 10,
VaultModeTests 6, LiveApiSignatureTests 15, LayoutTests 45 (42
carried + 3 BB-08), ChatMenuTests 36 (31 + 5), FormContentTests 31
(28 + 3), ClaimBlocksMathTests 18, ClaimsPresentationTests 13,
VanillaStatsCacheTests 19 (new; covers the reconstructed cache),
BackNavTests 29 (new; pins the three rows + the untouched board
views at the pure-builder level).

## Rig (local, this folder's rig/)

Scratch copy of the 1.3 scratch rig (Paper 26.2-123 +
GriefPrevention 16.18.7 with real GP data), 1.3.0 jar swapped for
the 1.3.2 candidate; server copy deleted after the boot, log kept.

- Log: `rig/boot-132.log`, SHA-256
  1a36249335419f70de86befa92c524f5ae1cdd7f265634d64a0bd55d9e960b12
- Lines: "Enabling ExcelLeaderboard v1.3.2"; "Done (29.295s)";
  "ExcelLeaderboard: Land Claimed category available
  (GriefPrevention)." at first refresh; the 1.3.1 cache line
  "stats folder not found; offline stats are read uncached
  (1.3.0 behavior)." (this rig's world has no stats folder —
  correct miss behaviour). SEVERE count: 0 (grep -c).
- Real-client tap walks cannot run in this desk lane (no Minecraft
  client); they are the packet's player proof, not desk evidence.
  The PC test-server mirror was not booted: tonight is the Warden
  session and the mirror shares the Homebase PC; the local rig
  covers the boot proof and the builder-level walk is pinned by
  BackNavTests.
