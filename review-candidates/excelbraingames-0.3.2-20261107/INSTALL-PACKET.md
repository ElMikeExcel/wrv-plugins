# INSTALL PACKET — ExcelBrainGames 0.3.2 (Word Search Bedrock geometry)

**Build:** EXC-BRAINGAMES-0.3.2-WRV-20261107-01-b87f7f
**Jar:** `ExcelBrainGames-0.3.2.jar` — 130,850 bytes, SHA-256 `8ef0465010bc3046efe18a7e2ada658bd730d44d6e74a76dc83a2a60de124511`
(repro jar not produced separately: build.sh's double assembly +
two full script runs are byte-identical — see BUILD-RECORD.md).
**Class of install:** GATED jar swap + config merge. NOT a text live-edit:
the jar changes, and the softdepend binding happens at boot, so this
goes through the normal empty-server window with BACKUP-FIRST.
**Why:** Mike's Nov 7, 2026 Bedrock Word Search proof against live
0.3.1: letter tiles render, but the 9-wide board scrambles in the
phone's ~5-wide Geyser wrap — 3 of his 5 kid-tier words were
unfindable by eye. Investigation:
`~/workspace/brain-games/WORDSEARCH-LAYOUT-INVESTIGATION.md`.
Mike's ruling: same lists/tiers both platforms; Bedrock horizontal
runs cap at 5 letters, longer words vertical/diagonal; Java unchanged.

## What changes for players

- Bedrock players starting a Word Search round are dealt a 5-wide
  stream board (Kid 5x8, Standard 5x10, Challenge 5x10) shaped for
  the phone: the wrap IS the board, every word traceable by eye in
  its placed direction; words longer than 5 letters run up/down or
  diagonal. Same word lists, word counts, payouts, par.
- The tier picker names the board they'll be dealt ("Kid 5x8" etc.).
- Java players: nothing changes. Nobody's in-progress round changes
  shape: a round renders in the geometry it was dealt in, on every
  client, forever (the geometry is saved with the round).
- No pack changes: the 0.3.1 tile packs are byte-identical in this
  build; Bedrock players need no new pack download, no
  server.properties change, no Geyser config change.

## Pre-window gate (census — STOP on mismatch)

- Live jar: `plugins/ExcelBrainGames-0.3.1.jar` = 125,732 B,
  SHA-256 b51d4c14105292fc3e118205e1b8b6449905fa40adc1e2e8a72ab99dc13d118f.
- Server verified empty per the standing gate; no other install in
  the same window.

## Window steps

1. BACKUP-FIRST per house rule, cap-at-6 (prune the OLDEST manual
   backups first if needed; never the Daily Backup's newest copy or
   the two newest manual restore points; name prunees in the report).
   Backup name: "BrainGames pre-0.3.2 20261107" (date-stamped to the
   install day).
2. Swap the jar: `ExcelBrainGames-0.3.1.jar` →
   rename aside to `ExcelBrainGames-0.3.1.jar.bak-20261107-pre032`;
   place `ExcelBrainGames-0.3.2.jar`; panel-side checksum of the
   placed file MUST equal the SHA-256 above.
3. **Config MERGE — do NOT wholesale-replace `config.yml`.** The live
   config carries admin-set `kids:` entries a fresh bundled config
   would lose. Back up the live file to
   `config.yml.bak-20261107-pre032`, then add ONLY the
   `games.wordsearch.bedrock-tiers` section from the bundled 0.3.2
   config (kid 5x8 / standard 5x10 / challenge 5x10 — exact text in
   the build folder's config.yml), and READ BACK the merged section
   before Start. Nothing else in the live config changes.
4. Start the server (restart, not reload — the Floodgate softdepend
   binding happens at boot).

## Boot verification (all required)

- `Done (` line present; SEVERE 0 attributable to this install.
- Enable line reads `ExcelBrainGames 0.3.2 enabled:` and ends
  `platform-detection=floodgate, build=EXC-BRAINGAMES-0.3.2-WRV-20261107-01-b87f7f`
  — floodgate IS present and enabled on the live server, so the
  detection half MUST read `floodgate`. If it reads `unavailable`,
  STOP: Bedrock players would silently get Java boards (safe, but
  the fix would not be live) — report before proceeding.
- Also in the boot log, BEFORE the enable line:
  `ExcelBrainGames: floodgate detected — Bedrock players get the 5-wide Word Search board.`
- Plugin count unchanged from the pre-window census (76);
  guildgate LOCKED 3 of 5 unchanged (BossTrial/GuildGate untouched —
  chapter acceptance does NOT re-open).

## Player proof owed after install (parent tracks)

1. **Mike on Bedrock (phone):** start a Word Search round (any tier):
   the board reads as one 5-wide stream; every word on the Words list
   is findable BY EYE in the stream in its direction; long words run
   vertical/diagonal; two-tap circling works as before. The
   round-start console line should read
   `geometry=bedrock, board=5x8` (kid) / `5x10` (standard/challenge).
2. **Cross-platform resume:** a round started on Bedrock, opened from
   a Java client, still renders in its 5-wide stream layout (its
   dealt geometry), and a Java-started round still renders 9-wide
   everywhere.
3. Java regression spot-check: any Java player opens Word Search —
   board identical to 0.3.1.

## Rollback

Stop server; remove `ExcelBrainGames-0.3.2.jar`; restore
`ExcelBrainGames-0.3.1.jar.bak-20261107-pre032` to its original name;
restore `config.yml.bak-20261107-pre032` (the merged section is inert
under 0.3.1, so restoring the config is belt-and-braces, but do it —
the pair rolls back together); Start; verify the 0.3.1 enable line.
Save safety: 0.3.2 saves carry one extra key (`geometry`) that 0.3.1
ignores — no save is corrupted by rollback. One honest caveat: a
round DEALT on Bedrock under 0.3.2 (5-wide board stored in the save)
renders under 0.3.1 on the Java slot map — scrambled on a phone,
playable on Java — until that round is finished or discarded. All
other rounds (including every pre-0.3.2 round) are unaffected.

## Same-pass surfaces owed at install (parent/installer)

- What's New BOTH surfaces from the single registry
  (`~/workspace/whatsnew-pipeline/registry/whatsnew.yml`): website
  section + in-game `ExcelWhatsNew/changelog.yml`.
- Website pair: index.html + ai-knowledge.json (regen via
  `~/workspace/site-ai/extract.py`, ship the full regen).
- Guide chunk: Word Search on Bedrock (5-wide board, long words
  vertical/diagonal) in knowledge/intents as needed; broadcaster
  tips check.
- Records: the 0.3.2 package is staged at
  `~/workspace/records-catchup-20261103/packages/excelbraingames-0.3.2-20261107/`
  with PUSH-NOTES — draft PR + Drive legs are the parent/board-manager's.
