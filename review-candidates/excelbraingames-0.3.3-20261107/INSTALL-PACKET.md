# INSTALL PACKET — ExcelBrainGames 0.3.3 (Sudoku visual box view for Bedrock)

**Build:** EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61
**Jar:** `ExcelBrainGames-0.3.3.jar` — 135,705 bytes, SHA-256 `64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1`
(repro jar not produced separately: build.sh's double assembly +
two full script runs are byte-identical — see BUILD-RECORD.md).
**Class of install:** GATED jar swap ONLY. NOT a text live-edit: the
jar changes, so this goes through the normal empty-server window
with BACKUP-FIRST. **There is NO config merge in this packet** —
0.3.3's `config.yml` is byte-identical to 0.3.2's (SHA-256
68eb3db30f1288279e8427a3372247a4a24a05dc8964748b35385ec42e4723de on
both trees); do not touch the live config at all.
**Why:** Mike's Nov 5–7, 2026 verdict on the 0.3.1 chat text grid
("helpful but still very confusing, borderline unplayable" — on a
phone the 9×9 board cannot render as a square in Geyser's ~5-wide
wrap, so taps meant counting) and his design approval in his own
words, Nov 7, 2026, 7:44 PM CST: Bedrock Sudoku becomes a box
picker + one box at a time as a true visual square, a context line
(full row + full column) on a cell tap, and a 3×3 digit-tile
picker. Spec: `SPEC-DELTA.md` in this folder.

## What changes for players

- Bedrock players opening Sudoku land on a box picker: one entry
  per box, finished boxes marked (glinting lime — "Finished — every
  square right!"), full-but-wrong boxes yellow, the rest showing
  their filled count. Opening a box shows its squares as digit
  tiles arranged as a true square on the phone — no counting.
- Tapping a square prints its full row and full column in chat
  (the context line — Sudoku constraints run on rows and columns
  too), and for an empty/filled square opens a 3×3 digit-tile
  picker with an erase tile. Same place/erase/check rules as Java,
  including the mistake cap and payouts — one shared code path.
- "Show board in chat" is one tap away from the picker and the box
  view: the full-board text grid, unchanged.
- Java players: nothing changes — the full 9×9 board exactly as
  today. Nobody's in-progress round changes: the board is identical
  on both platforms; only the presentation differs, and it follows
  the VIEWER, so a round opened from the other platform shows the
  same entries in that platform's presentation.
- No pack changes, no save changes (0.3.3 adds no save key), no
  config changes, no server.properties or Geyser changes.

## Pre-window gate (census — STOP on mismatch)

- **Live jar: `plugins/ExcelBrainGames-0.3.2.jar` = 130,850 B,
  SHA-256 8ef0465010bc3046efe18a7e2ada658bd730d44d6e74a76dc83a2a60de124511.**
  0.3.3 installs ONLY over a confirmed-live 0.3.2. (0.3.2 is
  recorded INSTALLED in the MANIFEST, boot-verified Nov 7, 2026 by
  the parent's read-only panel verification; if the live jar is
  anything else, STOP and report.)
- Server verified empty per the standing gate; no other install in
  the same window.

## Window steps

1. BACKUP-FIRST per house rule, cap-at-6 (prune the OLDEST manual
   backups first if needed; never the Daily Backup's newest copy or
   the two newest manual restore points; name prunees in the
   report). Backup name: "BrainGames pre-0.3.3 20261107"
   (date-stamped to the install day).
2. Swap the jar: `ExcelBrainGames-0.3.2.jar` → rename aside to
   `ExcelBrainGames-0.3.2.jar.bak-20261107-pre033` (use the actual
   install date in the suffix if it differs); place
   `ExcelBrainGames-0.3.3.jar`; panel-side checksum of the placed
   file MUST equal the SHA-256 above. Exactly one active
   ExcelBrainGames jar when done.
3. **Config: NO STEP.** Do not merge, replace, or edit
   `plugins/ExcelBrainGames/config.yml` — it is already exactly the
   config this jar ships.
4. Start the server (restart, not reload).

## Boot verification (all required)

- `Done (` line present; SEVERE 0 attributable to this install.
- Enable line reads `ExcelBrainGames 0.3.3 enabled:` and ends
  `platform-detection=floodgate, build=EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61`
  — floodgate IS present and enabled on the live server, so the
  detection half MUST read `floodgate`. If it reads `unavailable`,
  STOP: Bedrock players would silently get the Java full-board
  presentation (safe, but the fix would not be live) — report
  before proceeding.
- Also in the boot log, BEFORE the enable line:
  `ExcelBrainGames: floodgate detected — Bedrock players get the 5-wide Word Search board and the Sudoku box view.`
- Plugin count unchanged from the pre-window census (76);
  guildgate LOCKED 3 of 5 unchanged (BossTrial/GuildGate untouched —
  chapter acceptance does NOT re-open).

## Player proof owed after install (parent tracks)

1. **Mike on Bedrock (phone):** open Sudoku (any tier): you land on
   the box picker; open a box — it reads as a true square, every
   square tappable with no counting; tap a square — the chat
   context line shows that square's full row and full column; enter
   digits from the 3×3 picker, erase one, use Check (wrong squares
   highlight as on Java); finish a box — the picker marks it
   finished; "Show board in chat" prints the full grid.
2. **Cross-platform resume:** a Sudoku round started on Bedrock,
   opened from a Java client, shows the identical board as the full
   9×9 with the same entries; a Java-started round opened on the
   phone shows the same entries in the box view.
3. Java regression spot-check: any Java player opens Sudoku — board
   identical to 0.3.2.

## Rollback

Stop server; remove `ExcelBrainGames-0.3.3.jar`; restore
`ExcelBrainGames-0.3.2.jar.bak-20261107-pre033` to its original
name; Start; verify the 0.3.2 enable line. Config was never touched
by this install, so there is nothing to restore on the config side
— the rollback pair is the jar alone, trivially consistent. Save
safety: 0.3.3 adds no save key and changes no save semantics, so
every save — including rounds played under 0.3.3 — reads under
0.3.2 exactly as written. No caveats.

## Same-pass surfaces owed at install (parent/installer)

- What's New BOTH surfaces from the single registry
  (`~/workspace/whatsnew-pipeline/registry/whatsnew.yml`): website
  section + in-game `ExcelWhatsNew/changelog.yml`.
- Website pair: index.html + ai-knowledge.json (regen via
  `~/workspace/site-ai/extract.py`, ship the full regen).
- Guide chunk: Sudoku on Bedrock (box picker, one box as a square,
  context line, digit picker) in knowledge/intents as needed;
  broadcaster tips check.
- Records: the 0.3.3 package is staged at
  `~/workspace/records-catchup-20261103/packages/excelbraingames-0.3.3-20261107/`
  with PUSH-NOTES — draft PR + Drive legs are the parent/board-manager's.
