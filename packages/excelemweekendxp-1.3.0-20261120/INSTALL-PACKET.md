# INSTALL-PACKET - ExcelEMWeekendXP 1.3.0 (D155 EM skill-XP leg + D163/D171 rename)

Lint: 11380 B / max line 184 / non-ASCII 0 (targets <= ~12 KB, lines < ~300 chars, minimal non-ASCII)

## PENDING - ExcelEMWeekendXP 1.3.0 (Nov 20, 2026) - REVISED packet, supersedes the ExcelWeekendXP 1.3.0 packet

- **Packet:** `~/workspace/weekendxp-130-build-20261120/INSTALL-PACKET.md` (this file,
  revised for the rename; the MANIFEST PENDING entry filed against the old packet
  must be re-pointed to this packet and the candidate hash below - supervisor action;
  this worker did not touch the MANIFEST).
- **Rename (D163/D171, Mike approved Nov 20, 2026, ~12:50 PM CT):** the built
  ExcelWeekendXP 1.3.0 was renamed to ExcelEMWeekendXP BEFORE install, following the
  ExcelEMQuestXP 1.1.0 pattern exactly: plugin.yml `name` + jar filename only.
  Main class and package are unchanged (`com.winkreyes.excelweekendxp.ExcelWeekendXP`);
  the plugin defines no commands, so no command name is affected; it has no config
  file. The Bukkit logger prefix derives from the plugin.yml name, so all boot lines
  carry `[ExcelEMWeekendXP]` after this install (rig-proven, see Candidate). The one
  hard-coded log message that still reads "ExcelWeekendXP scheduler enabled" is
  message text, not identity - left unchanged per the QuestXP precedent (its class
  and code identity were not renamed either).
- **Live-premise receipts (G1):** fresh pull Fri Nov 20, 2026, 15:08 UTC via
  the Homebase route (rclone from `shockbyte:1. Michael's Server/plugins/`):
  live jar `plugins/ExcelWeekendXP-1.2.1.jar` = 15,148 B / SHA-256
  `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`
  (byte-identical to the 1.2.1 build of record), and live jar
  `plugins/EliteMobs-10.9.10.jar` (internal version 10.9.14) = 14,563,211 B /
  SHA-256 `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`,
  with `EliteSkillXpGainEvent` + `Reason.COMBAT` verified on the pulled jar
  via `javap`. Full receipts:
  `~/workspace/weekendxp-130-build-20261120/RECEIPTS.md`.
- **Candidate:** `~/workspace/weekendxp-130-build-20261120/ExcelEMWeekendXP-1.3.0.jar`
  - 18,943 B / SHA-256 `9bc65521d16079921cd1239b18f3fb056c1058e617bb658bc4f47e6374413f70`;
  deterministic double-build byte-identical (both builds); tests 38/38 on BOTH
  builds (25 pre-existing unmodified + 13 D155 tests; the rename changed no Java
  source and no test). Entry-by-entry diff against the pre-rename candidate jar
  (`a9f16135...`, kept in the build folder under `superseded-pre-rename/`): the
  ONLY differing entry is `plugin.yml`, and within it only line 1
  (`name: ExcelWeekendXP` -> `name: ExcelEMWeekendXP`); every class file is
  byte-identical. Behavior is therefore identical to the rig-proven 1.3.0 build.
  - Rename rig proof (boot 3, Fri Nov 20, 2026, same rig as boots 1-2, EliteMobs
  present, renamed jar hash-verified in place): plugin list shows
  `ExcelEMWeekendXP (1.3.0)`; `[ExcelEMWeekendXP] Enabling ExcelEMWeekendXP v1.3.0`;
  `[ExcelEMWeekendXP] EliteMobs detected: EliteMobs skill XP weekend multiplier leg enabled.`;
  `[ExcelEMWeekendXP] Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x. Verified EliteMobs skill XP event multiplier 1.2x.`;
  `Done (37.507s)`; SEVERE 0; `schedule incomplete` 0; clean stop. Log preserved
  as `rig/logs/boot3-renamed-with-em.log`, SHA-256
  `f0f806e51640c4a4e3c8a99887ae1259ab154394324b6779717bc45bb52964d3`.
  Build record (rename section at the end):
  `~/workspace/weekendxp-130-build-20261120/BUILD-RECORD.md`.
- **Live-jar gate (STOP on mismatch):** live file
  `plugins/ExcelWeekendXP-1.2.1.jar` = 15,148 B / `b4725c55...` - sourced
  from the live-premise receipt above, never from lineage.
- **Staged artifacts:** `~/workspace/weekendxp-130-build-20261120/`
  (SHA256SUMS in the build folder) - candidate jar + source zip + build
  record + receipts + this packet.
- **Scope:** (a) 1.3.0 feature scope, unchanged from the superseded packet: adds
  a third weekend-XP leg - EliteMobs skill XP from combat gains scales 1.2x
  Fri-Sun (America/Chicago) via EliteMobs' own `EliteSkillXpGainEvent`
  (HIGHEST, ignoreCancelled; round to nearest long; never reduces; weekday =
  event untouched). Programmatic grants (ExcelEMQuestXP payouts, quest backpay
  via `PlayerData.addSkillXP`) do NOT fire that event and stay exact - no
  grant-path code exists. The mcMMO `mcmmo:xprate` leg and the vanilla
  `PlayerExpChangeEvent` leg are behavior-identical to 1.2.1. If EliteMobs is
  absent the plugin runs the two existing legs (rig-proven in the original
  build). (b) Rename scope: plugin identity only, per the Rename bullet above.
  ExcelEMQuestXP is NOT touched.
- **Jar swap:** with the server stopped: rename the live jar
  `plugins/ExcelWeekendXP-1.2.1.jar` to
  `plugins/ExcelWeekendXP-1.2.1.jar.bak-20261120` (kept, never deleted in this
  window); place `plugins/ExcelEMWeekendXP-1.3.0.jar`; hash-verify it in place
  against the Candidate line (STOP on mismatch).
- **Data-folder migration (QuestXP pattern - copy, byte-verify, read back):**
  plugin facts first: ExcelWeekendXP has NO config and NO player data, and it
  persists NO state - the applied rate is re-derived from the America/Chicago
  clock at every enable and every 30 minutes, and lives only in memory. Its
  data folder can hold at most files its built-in LegacyDataMigration copied
  in from the pre-Excel `plugins/WeekendXP/` folder on some first enable.
  Rig evidence: a full boot + enable cycle creates NO data folder at all.
  Steps, with the server stopped, alongside the jar swap:
  1. Fresh-list live `plugins/ExcelWeekendXP/`: record every file (relative
     path, size, SHA-256) in the install record as the migration receipt.
  2. If the folder exists and holds files: copy it wholesale to
     `plugins/ExcelEMWeekendXP/` (COPY, never move - the old folder stays
     untouched as part of the rollback record), then hash-verify every copied
     file against the receipt: file count and per-file SHA-256 must match
     exactly (STOP on mismatch).
  3. If the folder is absent or holds no files: record that as the receipt
     outcome and skip - there is nothing to migrate.
  4. Leave `plugins/ExcelWeekendXP/` and any `plugins/WeekendXP/` folder
     untouched. Note: under the new name, the plugin's built-in legacy
     migration still looks for `plugins/WeekendXP/` at enable, but it copies
     only when the new folder holds no files at all - after a step-2 copy it
     stays inert; if it ever fires, the boot log shows
     `Migrated legacy data folder plugins/WeekendXP -> plugins/ExcelEMWeekendXP (N files)`,
     which is designed behavior, not an anomaly.
  5. State read-back after the install boot (the state-loaded proof): the
     plugin's only live state is the applied rate - read it back from the
     boot log's verified-rate line under the `[ExcelEMWeekendXP]` prefix
     (see Boot verify). If step 2 copied files, additionally re-hash
     `plugins/ExcelEMWeekendXP/` after the boot and confirm it is still
     byte-identical to the receipt (the plugin never writes to its folder;
     any difference is an anomaly - report it, do not silently accept).
  Do NOT delete `plugins/ExcelWeekendXP/` or the `.bak` jar in this window;
  cleanup is a later records step, only after the post-install proofs pass.
- **Gates:** marker discipline with re-checks before Shut Down and before
  Start; BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest or
  the two newest manuals); emptiness `list` = 0/10 twice.
- **Boot verify:** `[EliteMobs] Enabling EliteMobs v10.9.14` precedes
  `[ExcelEMWeekendXP] Enabling ExcelEMWeekendXP v1.3.0`;
  `[ExcelEMWeekendXP] EliteMobs detected: EliteMobs skill XP weekend multiplier leg enabled.`;
  on Fri-Sun `[ExcelEMWeekendXP] Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x. Verified EliteMobs skill XP event multiplier 1.2x.`
  (on a weekday the same line with reset / 1.0x values);
  `[ExcelEMWeekendXP] ExcelWeekendXP scheduler enabled: America/Chicago, every 30 minutes.`;
  the boot plugin list shows `ExcelEMWeekendXP (1.3.0)` and NO `ExcelWeekendXP`
  plugin; `Done` line; SEVERE 0 attributable to ExcelEMWeekendXP; no
  `schedule incomplete` WARN. Marker:
  `~/workspace/weekendxp-130-build-20261120/INSTALLED.txt` (written at install).
- **Rollback:** stop the server; remove `plugins/ExcelEMWeekendXP-1.3.0.jar`;
  rename `plugins/ExcelWeekendXP-1.2.1.jar.bak-20261120` back to
  `plugins/ExcelWeekendXP-1.2.1.jar` (15,148 B / `b4725c55...`, hash-verify);
  leave BOTH data folders in place (`plugins/ExcelEMWeekendXP/` is inert under
  the old name - 1.2.1 reads only `plugins/ExcelWeekendXP/`, which this install
  never modified); restart and verify the 1.2.1 boot lines under the
  `[ExcelWeekendXP]` prefix.
- **Same-pass surfaces:** PLAYER-FACING - the weekend bonus now covers
  EliteMobs skill XP, canon currently teaches only "mcMMO skill XP and
  green-bar XP" (D155), and canon names the plugin ExcelWeekendXP. Owed in
  the install pass (supervisor lane): website pair (page + ai-knowledge)
  updated to name EliteMobs skill XP in the weekend bonus and the plugin as
  ExcelEMWeekendXP; What's New on BOTH surfaces (website section + in-game
  ExcelWhatsNew changelog); broadcaster tips check (update any tip that
  scopes the bonus to mcMMO/vanilla only); in-game menus check (no menu or
  command changes in 1.3.0 - confirm none needed). Guide knowledge/intents
  leg is suspended under the Nov 16 Guide hibernation; the website stays
  canon. Record the change in the wrv-plugins record PR + Drive deposits at
  close-out.
- **Proof owed after install:** (1) weekend leg - on a Fri-Sun day, a player
  kills an elite mob and the EliteMobs skill-XP gain pays 1.2x (note skill
  XP before/after against the unscaled gain for the same kill type);
  (2) weekday leg - on a Mon-Thu day, the same gain is exactly 1.0x
  (untouched); (3) no `schedule incomplete` WARN across one boot + one
  30-minute cycle. Testers: Jester or Marvin (D154 testing pool; Mike does
  not test); Java + Bedrock both. A proof is only discharged by the
  tester's own report.

## Verification by script output

- `sha256sum ~/workspace/weekendxp-130-build-20261120/ExcelEMWeekendXP-1.3.0.jar`
  -> `9bc65521...` (must match the Candidate line above).
- Live gate re-pull at install: `sha256sum` of live
  `plugins/ExcelWeekendXP-1.2.1.jar` -> `b4725c55...` (STOP on mismatch).
- `unzip -p <candidate jar> plugin.yml` -> `name: ExcelEMWeekendXP`,
  `version: 1.3.0`, `main: com.winkreyes.excelweekendxp.ExcelWeekendXP`,
  `softdepend: [EliteMobs]`.
- Migration receipt check (only when the live folder held files):
  `find plugins/ExcelEMWeekendXP -type f | sort` count equals the receipt
  count, and per-file `sha256sum` matches the receipt line for line.
- `grep -c "Enabling ExcelEMWeekendXP v1.3.0" <live latest.log>` -> 1 after
  the install boot; `grep -c "Enabling ExcelWeekendXP v1.3.0" <live latest.log>`
  -> 0.
- `grep -c "EliteMobs detected: EliteMobs skill XP weekend multiplier leg enabled" <live latest.log>`
  -> >= 1 after the install boot.
- `grep -c SEVERE <live latest.log>` -> 0 attributable to ExcelEMWeekendXP.
- `grep -ci "schedule incomplete" <live latest.log>` -> 0.
