# ExcelBossTrial 0.4.1 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** ExcelBossTrial is the server's governed boss-fight plugin
  (Wither / Warden / Dragon / Elder chapters: pool-space damage governing,
  verdicts, sprinkle adds, seal loot via GuildGate). Design of record:
  `~/workspace/bosstrial-fix-20261108/DESIGN.md` §㉚.9; backlog ㉚.9 in
  `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`.
  (The in-tree SOURCE.md this replaces was the stale 0.3.5 note carried in
  the copied base tree; this file is the 0.4.1 source of record for
  the package.)
- **0.4.1 delta (backlog ㉚.9, the post-fight stats surface):** built on
  the 0.4.0 tree (baseline suite reproduced first: 27,225 checks /
  0 failures). Post-fight pop-up at every encounter close (each
  still-online participant is shown their own line from the PM-8 fight
  record; Java chat, Bedrock Floodgate/Cumulus form, fail-soft to chat;
  automatic surfaces obey the `/bosstrial feedback off` opt-out);
  `/bosstrial stats` (public, like status/feedback) re-shows the
  caller's most recent CLOSED fight record, read back with the new
  `FightTracker.parseRecord`; the PM-9 split in both renderings
  (players / other / boss-healed / net of pool); PM-4 space naming
  (every number is pool damage — the numbers the boss bar showed);
  the healing column is IN this build because the ㉚.10 controlled-heal
  proof passed in the 0.4.0 code phase. Delta files: NEW
  `core/FightStatsFormat.java`; EDIT `core/FightTracker.java`
  (parseRecord only), `core/CommandAccess.java` (isStatsCall),
  `ExcelBossTrial.java`, `config.yml` (`fight-tracking.stats-popup:
  true`, `fight-tracking.stats-command: true`), `plugin.yml` (version
  0.4.1). Suite: 27,328 checks / 0 failures (+103 over the 0.4.0
  baseline; post-rebase harness-summary tally 2,109 checks /
  75 harnesses / 0 failures — tally methods differ per lane, see
  BUILD-RECORD.md).
- **REBASE of record (Nov 8, 2026, evening):** this build was first
  made on the staged 0.4.0 candidate `0638c158…`, then REBASED onto
  the REBUILT 0.4.0 — sprinkle clamp raise 48 → 140 + Wither taunts +
  loud sanitize — `043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3`
  (401,269 B) the same evening. The original 0.4.1 candidate
  (404,260 B / `8eb5a004…`) is VOID, never install it. Rebase detail:
  `BUILD-RECORD.md`, REBASE section.
- **Source:** this folder (`src/com/winkreyes/excelbosstrial/` + the
  `src/wrv/` rewards tree), built by `package.sh` / `build-test.sh`
  (deterministic double-build byte-identical for the shipped jar).
- **Candidate jar:** `ExcelBossTrial-0.4.1.jar`, 411,565 B,
  sha256 `f7376dc93f86e866d7e1fa5553d5e66a09e41fdd0ca383682f48d32900da20f1`
  (at `~/workspace/bosstrial-fix-20261108/build-stats/ExcelBossTrial-0.4.1.jar`).
- **Status:** INSTALLED Nov 8, 2026, parent hand window
  11:10–11:26 PM CST (gate: live 0.4.0 jar 401,269 B / `043c1558…`
  matched; backup "BossTrial 0.4.1 pre-install 1108" created after
  pruning "Composed spells pre-install 1108", total 6; config merge
  added `fight-tracking.stats-popup: true` + `stats-command: true`;
  boot 11:22 PM Done 20.632 s, 78 plugins, SEVERE 0; MANIFEST
  INSTALLED entry + marker `INSTALLED.txt` in this package).
  Post-install player proofs (stats pop-up on a live close;
  `/bosstrial stats` readout) still owed — player-proof class.
- **Package staged:** from
  `~/workspace/bosstrial-fix-20261108/build-stats/bosstrial/` (full
  source tree) + `BUILD-RECORD.md` + `PACKET-DRAFT.md` +
  `INSTALLED.txt` from `~/workspace/bosstrial-fix-20261108/build-stats/`;
  jar itself NOT in the package per house rule — it rides the Drive
  deposit. Publication (GitHub draft record PR + Drive deposit) is
  the next leg, together with the ExcelBossTrial 0.4.0 package staged
  alongside it.
