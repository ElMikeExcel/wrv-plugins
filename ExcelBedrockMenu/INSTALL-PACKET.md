# ExcelBedrockMenu 1.5.8 — install packet (NOT installed yet)

Mike's direction, Sat Oct 17, 2026: menu teleport buttons get a
confirmation step (players were teleporting by accident; made a
standing standard for both menus the same day). This packet is the
Bedrock half. The Java (DeluxeMenus) half is a separate config packet.

## Artifact

- Jar: `ExcelBedrockMenu-1.5.8.jar` (this folder), 22,486 bytes
- SHA-256: `52766c5916b50dda597ad3782f414ad3950c15656018699ec177e7e0d3db255c`
- Replaces live: ExcelBedrockMenu 1.5.7
  (SHA-256 `0787c1616d576795b1b50ef7be42bd0f5de0658450b7c859ec1f93f4e56fb7c7`)
- Source: `src/` in this folder (build: `src/build-158.sh`;
  verification: `VERIFICATION-1.5.8.md`)
- Statuses: Built YES / automated tests 38/38 PASS / scratch boot PASS /
  installed live NO / Bedrock check PENDING (needs a real device tap)

## What changes for players

Tapping **Spawn** (and **Adventurers Guild** while the guild is
unlocked, and the /spawn, /home, /ag entries in Server Commands) now
opens a "Teleport to …?" form with **Yes, teleport** / **Cancel**
instead of teleporting instantly. Cancel, or closing the form, leaves
the player where they stand. Everything else in the menu is identical
to 1.5.7 (proven at the byte level for all untouched classes).

## Preconditions (standing install rule)

- Server empty (0/10, gate checked twice). If Kailey is online, tell
  Mike and HOLD.
- The staged server-optimization watch (view/simulation distance +
  ViaVersion packet-limiter, stands down 1:00 AM Oct 18) also wants a
  restart — install in the SAME window if it has not already applied,
  so players eat one restart, not two.

## Install steps

1. Panel → Files: download the live `plugins/ExcelBedrockMenu-1.5.7.jar`
   and hash it locally; it must equal
   `0787c1616d576795b1b50ef7be42bd0f5de0658450b7c859ec1f93f4e56fb7c7`
   before anything is replaced. If it does not match, STOP and report.
2. Rename the live jar in place to
   `ExcelBedrockMenu-1.5.7.jar.bak-20261017` (kept on the server as the
   rollback).
3. Upload `ExcelBedrockMenu-1.5.8.jar` to `plugins/`. Confirm exactly
   ONE active ExcelBedrockMenu jar exists (the .bak is not loaded by
   Paper, but verify the file list anyway).
4. Restart via the panel **Shut Down** button only. NEVER console
   `stop` (it auto-restarts mid-state).
5. Boot verification (console + log):
   - `version ExcelBedrockMenu` → `ExcelBedrockMenu version 1.5.8`
   - Log shows `Enabling ExcelBedrockMenu v1.5.8` and the enabled line,
     no SEVERE naming the plugin.
   - `list` → 0/10 (or whoever legitimately rejoined).
6. Drop the source zip + jar into Drive "Minecraft WRV" (Source /
   Build files) at install time, per the standing file-home rule.

## Post-install Bedrock tap check (Mike or a Bedrock player)

1. /menu → tap **Spawn**: the "Teleport to spawn?" form appears and
   the player does NOT move yet.
2. **Cancel**: form closes, player still standing in the same spot.
3. Reopen, tap Spawn, close the form without choosing: same, no move.
4. Reopen, tap Spawn → **Yes, teleport**: player lands at spawn,
   exactly once (console spawn or village fallback, same as 1.5.7).
5. Server Commands → Getting Around → /spawn button: confirm appears;
   Cancel leaves the player in place.
6. (After the first guild seal falls) Adventurers Guild button shows
   "Teleport to the Adventurers Guild?" before travelling. While the
   guild is still sealed, tapping it should open the seals quest page
   with NO confirm — that is correct behavior, not a bug.

## Rollback

1. Panel Shut Down.
2. Delete `plugins/ExcelBedrockMenu-1.5.8.jar`.
3. Rename `ExcelBedrockMenu-1.5.7.jar.bak-20261017` back to
   `ExcelBedrockMenu-1.5.7.jar`.
4. Start the server; `version ExcelBedrockMenu` must read 1.5.7.
No data-folder or config changes are involved in either direction
(config.yml is byte-identical between the two jars).

## Publishing (at install time, NOT done in this build pass)

- wrv-plugins: new OPEN DRAFT PR, branch
  `record/excelbedrockmenu-1.5.8-2026-10-17`: full source (this
  folder's `src/`), `CHANGELOG-1.5.8.md` (ships in this folder),
  install record (installed timestamp, gate readings, jar SHA-256,
  rollback location), this packet's verification summary.
- Website: ExcelBedrockMenu entry updated to 1.5.8 + a changelog
  bullet: teleport buttons (Spawn, Adventurers Guild, and the
  /spawn /home /ag command entries) now ask "Teleport to …?" first;
  Cancel or closing the prompt does nothing. Credit line unchanged
  (ExcelBedrockMenu is ours).
- Drive deposits: jar + source zip, size-verified (step 6 above).
- Records: VERSIONS-AND-ROLLBACK.md row (1.5.7 → SUPERSEDED by 1.5.8),
  Route Sheet note, Big Brain same turn.
