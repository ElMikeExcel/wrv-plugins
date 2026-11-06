# INSTALL PACKET — ExcelBrainGames 0.3.1 "Letter Tiles"

**Classification: GATED install.** Jar swap + Geyser mapping/pack file
placement + Geyser config flag + server.properties lines. This is NOT a
text live-edit — the empty-server gate and BACKUP-FIRST apply in full.

**What it does (player-facing):** Word Search and Sudoku grids and Quick
Math kid answer cards show their letters/digits ON the tiles (Java server
resource pack + Geyser Bedrock pack). Sudoku also prints its board as a
bordered square in chat (at open + a "Show board in chat" button), Word
Scramble kid choices print once per word in chat, and Quick Math kid
questions print once per question in chat (the question itself lived in
the prompt paper's name — the add-on build closed that; see proof 9).
Presentation only — no logic, payout, config, or save changes; 0.3.0
player data carries over untouched.

## Candidate artifacts (verify before touching the panel)

| File | Size | SHA-256 |
|---|---|---|
| `build-0.3.1/ExcelBrainGames-0.3.1.jar` | 125,732 B | `b51d4c14105292fc3e118205e1b8b6449905fa40adc1e2e8a72ab99dc13d118f` (add-on build; supersedes the first build's 125,602 B / `45b178a7…`) |
| `build-0.3.1/packs/dist/braingames-tiles-java.zip` | 189,943 B | `05942eed…` SHA-1 (for server.properties): `d110eadec60cf07e7c6f0d5c27623de4a7ee729d` |
| `build-0.3.1/packs/dist/braingames-tiles-bedrock.mcpack` | 85,596 B | `486bcad6e1452ffa8e6e83e267112d6edad693c2de68f4e1057c3bc9c2221286` |
| `build-0.3.1/packs/dist/geyser/braingames-tiles.json` | 46,768 B | (hash in build folder SHA256SUMS) |

## Gates — STOP conditions

1. **Live jar gate:** the ONE active BrainGames jar in `plugins/` must be
   exactly 118,307 B / SHA-256 `acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63`
   (0.3.0). Anything else → STOP, report.
2. **Emptiness gate:** standard verified-empty protocol (weekend protocol
   applies Fri/Sat: countdown + push anyway). Panel marker discipline:
   create `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window
   start if hand-run; check for an in-flight failsafe run first.
3. **Hosting gate (Java pack):** `braingames-tiles-java.zip` must be
   reachable at a stable public HTTPS URL (website repo / GitHub raw on
   `ElMikeExcel/winkreyesvalley` are the candidates). **Do the Bedrock +
   jar steps only in the same window the URL goes live** — a
   `resource-pack` URL that 404s prompts every Java player with a failed
   download. The sha1 is fixed: `d110eadec60cf07e7c6f0d5c27623de4a7ee729d`.

## Steps

1. **BACKUP-FIRST (cap-at-6):** if 6 backups exist, prune the OLDEST manual
   backups until the post-creation total is ≤ 6 (never the Daily Backup's
   newest copy, never the two newest manual restore points; name prunees
   in the install report). Create the fresh backup:
   `BrainGames pre-0.3.1 20261105` (date-stamped at window time).
2. **Jar swap:** rename the live 0.3.0 jar aside in place:
   `ExcelBrainGames-0.3.0.jar` → `ExcelBrainGames-0.3.0.jar.bak-<date>-pre031`
   (match the existing `.bak-20261105-pre030` convention). Place
   `ExcelBrainGames-0.3.1.jar`. No config files change.
3. **Geyser mappings:** place `braingames-tiles.json` at
   `plugins/Geyser-Spigot/custom_mappings/braingames-tiles.json`
   (verify the actual Geyser folder name in the panel — `Geyser-Spigot`
   expected for the live 2.12.1-b1280 plugin build).
4. **Geyser pack:** place `braingames-tiles-bedrock.mcpack` in the Geyser
   plugin's `packs` folder (`plugins/Geyser-Spigot/packs/`).
5. **Geyser config:** in `plugins/Geyser-Spigot/config.yml`, set
   `enable-custom-content: true` (check first — if already true, no change;
   note it in the report either way).
6. **server.properties:** set exactly:
   ```
   resource-pack=<the hosted HTTPS URL of braingames-tiles-java.zip>
   resource-pack-sha1=d110eadec60cf07e7c6f0d5c27623de4a7ee729d
   ```
   `require-resource-pack` must be **false or absent** (fallback law: a
   client without the pack plays exactly 0.3.0). Do not set
   `resource-pack-id` or a prompt override.
7. **Restart** the server (full restart — Geyser loads mappings/packs and
   server.properties only at boot).

## Boot verify (all required)

- Boot `Done` line present; **SEVERE 0** attributable.
- Enable line: `ExcelBrainGames 0.3.1 enabled: games=[word-search,
  memory-match, sudoku, quick-math, word-scramble, riddle],
  kid-tiers=ENABLED, daily-cap=$1,500.00, … economy=EssentialsX Economy`.
- Plugin count **79** (unchanged); guild **LOCKED 3 of 5** line present.
- Geyser startup shows the custom pack/mappings loading without errors
  (watch the Geyser section of the log; a mappings parse error = rollback).

## Player proofs owed after install (name them in the report)

1. **Mike (Bedrock, pack accepted):** Word Search — letters visible ON the
   tiles without tapping; tapping still selects/circles as before.
2. **Mike (Bedrock):** Sudoku — given/placed/palette digits visible on
   tiles; the chat square prints at open and matches the board; the
   "Show board in chat" button re-prints after moves.
3. **Mike (Bedrock):** Quick Math kid card — the three emeralds show their
   numbers before any tap.
4. **Mike (Bedrock):** Word Scramble kid card — the choices print ONCE in
   chat, numbered in card order; a wrong tap does not re-print.
5. **Mike (Bedrock):** join once and DECLINE the Geyser pack — report what
   stamped cells look like (vanilla item vs placeholder). This is the one
   genuinely open rendering question.
6. **Jester (Java, pack accepted):** Word Search + Sudoku tiles visible;
   Sudoku chat square lines up with the board.
7. **Any Java client WITHOUT the pack:** games play by item names exactly
   as 0.3.0 (fallback law proof).
8. **Rylie (kid tiers):** Sudoku picture round — palette pictures unchanged;
   given/wrong tiles show palette numbers; chat legend reads right.
9. **Mike (Bedrock):** Quick Math kid round — each QUESTION reads in chat
   ("Quick Math — question N of 10: …") without tapping the prompt paper;
   it prints once when the question arrives, does not repeat while the
   same question is up, and the next question prints as it comes.

## Same-pass surfaces owed at install (installer/parent executes)

- What's New: registry entry (both surfaces — website section + in-game
  changelog) describing letter tiles + the Sudoku chat square as a
  self-contained how-to.
- Website canon: Brain Games section updated (index.html + ai-knowledge.json
  regen — both artifacts).
- Guide knowledge: a chunk covering tiles, the chat square + Show-board
  button, and the Scramble chat print.
- Route Sheet: the 0.3.1 row updated to installed with date + proofs owed.

## Rollback

1. Remove the two `resource-pack` lines from server.properties.
2. Delete `plugins/Geyser-Spigot/custom_mappings/braingames-tiles.json` and
   `plugins/Geyser-Spigot/packs/braingames-tiles-bedrock.mcpack`.
   (`enable-custom-content` may stay true — harmless with no mappings — or
   be restored; note which.)
3. Remove `ExcelBrainGames-0.3.1.jar`; restore the held
   `ExcelBrainGames-0.3.0.jar.bak-<date>-pre031` to its active name.
4. Restart; verify the 0.3.0 enable line and SEVERE 0.
Saves/player data are untouched by both directions (presentation only;
0.3.1 writes no new persisted state).

## On completion

Write `INSTALLED.txt` in `~/workspace/brain-games/build-0.3.1/` with the
window date/time, backup name, prunees (if any), the hosted pack URL used,
and the boot-verify lines — matching the 0.3.0 marker convention.
