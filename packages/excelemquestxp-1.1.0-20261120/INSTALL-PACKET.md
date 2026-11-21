# INSTALL-PACKET - ExcelEMQuestXP 1.1.0 (player-chosen quest XP target, D159; rename D160)

Lint: ~9.1 KB / max line 90 / non-ASCII minimal (targets <= ~12 KB, lines < ~300 chars)

## PENDING - ExcelEMQuestXP 1.1.0 (Nov 20, 2026)

- **Packet:** `~/workspace/questxp-110-build-20261120/INSTALL-PACKET.md`
- **What this is:** ExcelQuestXP 1.0.1 -> **ExcelEMQuestXP 1.1.0**. Two
  changes in one swap: (1) D159 — players choose where their quest XP goes
  (default HIGHEST = exactly today's behavior, or a pinned weapon skill),
  via `/questxp target`, `/questxp menu`, and a popup (Java chest GUI /
  Bedrock form); (2) D160 — the plugin is RENAMED ExcelEMQuestXP (the
  `/questxp` command does NOT change). The rename forces a data-folder
  migration, specified below — it is part of this swap, not a follow-up.
- **Live-premise receipts (G1):** fresh pull Fri Nov 20, 2026, ~10:43 AM CT
  via the Homebase route (rclone copyto from
  `shockbyte:1. Michael's Server/plugins/` on mike-pc, scp back, local
  sha256sum): live jar `plugins/ExcelQuestXP-1.0.1.jar` = 31,728 B /
  SHA-256 `28e5dbf122423947034c356b0616a71e259bf1be3aef2044e090f6f223ae37da`
  — byte-identical to the 1.0.1 build of record
  (`~/workspace/questxp-101-20261119/ExcelQuestXP-1.0.1.jar`). Live
  EliteMobs jar `plugins/EliteMobs-10.9.10.jar` (internal version 10.9.14)
  = 14,563,211 B / SHA-256
  `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`
  (same-day receipt in the WeekendXP 1.3.0 packet; the bridge's EM pin is
  unchanged in 1.1.0).
- **Candidate:** `~/workspace/questxp-110-build-20261120/ExcelEMQuestXP-1.1.0.jar`
  - 50,603 B / SHA-256 `cab1574471c50ace591bc12603afbd42272c06aec8c4cc78a3b660ae112b670e`;
    deterministic double-build byte-identical.
  - Tests 267/267 (Run A: QuestXp 42 + Backpay 35 + Target 34; Run B:
    EmApi 45 + QuestXp 42 + Backpay 35 + Target 34). Rig: phase A 21/0
    (migration pays-0 proof + HIGHEST + PINNED grants), phase B 12/0
    (restart persistence). Full detail: BUILD-RECORD.md.
- **Live-jar gate (STOP on mismatch):** live file
  `plugins/ExcelQuestXP-1.0.1.jar` = 31,728 B / `28e5dbf1...7da` — sourced
  from the live-premise receipt above, never from lineage. ALSO STOP if
  live `plugins/ExcelQuestXP/backpay.yml` does not hash to
  `24274c55...` (the Nov 20 restaged snapshot) — the migration copies
  whatever is live; if the restage is not what is live, the migration
  premise is wrong.
- **Staged artifacts:** `~/workspace/questxp-110-build-20261120/`
  (candidate jar + source zip + BUILD-RECORD.md + this packet + rig logs).

## The swap (Runner lane; ONE window, in this order)

1. Backup first + gates per the standard Runner packet discipline
   (marker, backup cap-at-6, emptiness list = 0/10 twice).
2. **Migrate the data folder BEFORE the boot:** copy the ENTIRE live
   `plugins/ExcelQuestXP/` folder to `plugins/ExcelEMQuestXP/`
   (recursive, every file: backpay.yml, backpay-paid.yml, grants.log,
   quest-xp.yml, and anything else present). Byte-verify the copy:
   per-file size + SHA-256 of at least backpay.yml, backpay-paid.yml and
   grants.log must match the source folder exactly; STOP on any mismatch.
   Do NOT move or delete the source folder — `plugins/ExcelQuestXP/`
   stays in place as the migration source/backup for this window
   (removal is a later cleanup, NOT part of this install).
3. **Jar swap in the same window:** `plugins/ExcelQuestXP-1.0.1.jar` OUT
   (kept as the rollback copy), `plugins/ExcelEMQuestXP-1.1.0.jar` IN.
   The two jars must never be present together at a boot — the old
   plugin and the new plugin share the `/questxp` command and the grant
   listener; running both would double-grant.
4. Restart (jar + new-plugin load; a reload is NOT sufficient for a
   plugin rename).

## Scope

- What 1.1.0 adds: per-player quest-XP target in
  `plugins/ExcelEMQuestXP/targets.yml` (created on first change; absent =
  everyone on HIGHEST, today's exact behavior). Grant path unchanged
  except target resolution at grant time; Armor stays floor(xp/3); the
  shared SkillGrant path still serves BOTH the live listener and
  `/questxp backpay` verbatim. New player surfaces: `/questxp target`,
  `/questxp target highest`, `/questxp target <skill>`, `/questxp menu`,
  bare `/questxp` (players) opens the popup. Admin surfaces
  (`/questxp reload`, `/questxp backpay <player>`) unchanged, still
  `excelquestxp.admin` (op).
- What it does NOT change: quest files, the XP table (112 quests,
  130,355 total XP), backpay amounts or guards, economy, player data.
  No backpay is paid by the install itself.
- Rollback: remove `ExcelEMQuestXP-1.1.0.jar`, restore
  `ExcelQuestXP-1.0.1.jar`, restart. The old plugin re-reads
  `plugins/ExcelQuestXP/` (untouched by the migration) — any targets
  chosen under 1.1.0 are simply ignored by 1.0.1, and backpay state is
  exactly as before the window PROVIDED no backpay was paid under
  1.1.0; if any was, the NEW folder's backpay-paid.yml is the current
  ledger — copy it back over `plugins/ExcelQuestXP/backpay-paid.yml`
  before booting 1.0.1, so a paid quest can never be re-paid.

## Boot verify

- Enable lines (grep the server log): `ExcelEMQuestXP 1.1.0 ready` naming
  build EXC-EMQUESTXP-1.1.0-WRV-20261120-01 and pinned EliteMobs 10.9.14;
  `Quest XP table loaded: 112 quests, 130355 total XP, 109 name fallbacks.`
- `Backpay table loaded: 2 players.` and `Backpay paid ledger loaded:
  N quest(s) already paid.` — N MUST equal the paid-entry count of the
  pre-swap live `backpay-paid.yml` copied in step 2. Expected at packet
  writing: **46** (Marvin 13 + Jester 33, both paid Nov 20 under 1.0.1).
  A different N with a matching step-2 count is fine (more backpay may
  have been paid in between); an N that disagrees with the copied file
  is a STOP.
- `XP targets loaded: 0 player(s) pinned.` on this first boot (no
  targets.yml exists yet — it is created in the NEW folder the first
  time a player sets a target). The ready line's `XP targets pinned: 0`
  agrees.
- SEVERE 0 attributable to ExcelEMQuestXP; guild state unchanged.
  (Two WARN lines `Could not save quest-xp.yml / backpay.yml ... already
  exists` are EXPECTED — the migrated files are already in place.)
- `plugins/ExcelEMQuestXP/` exists on the live server with the migrated
  files; `plugins/ExcelQuestXP/` still present alongside it.

## Proof owed after install

- **Paid-ledger migration proof (rename-critical):** from the console,
  `/questxp backpay GumGumJetPistol` then `/questxp backpay Jester_Turtle`
  — each must report all quests already paid and grant **0 XP** (Marvin:
  13 already paid; Jester: 33 already paid). Any actual payment here
  means the migrated ledger was not honored — STOP, do not pay anyone
  else, escalate. (This is a verification, not a payment run: both
  players were fully paid Nov 20 under 1.0.1.)
- **D159 feature proof:** a player (Jester or Marvin, D154 pool) runs
  `/questxp target <a skill that is NOT their highest>`, completes any
  paying quest, and the grant message + grants.log line show the XP in
  the PINNED skill with the `target=pinned` tag; then `/questxp target
  highest` restores auto. A Bedrock player opens `/questxp menu` and
  gets the form (not a chest GUI); a Java player gets the chest GUI.
- Player-lane proofs ride the D122 post-release rule; the ledger proof
  above is install-window work, not player-lane.

## Same-pass surfaces (owed at ship, supervisor lane)

- PLAYER-FACING: new player choice + commands + popup. Website pair
  (page + ai-knowledge) gains the /questxp target + menu explanation;
  What's New on BOTH surfaces (website section + in-game ExcelWhatsNew
  changelog, verified from the live file); tips check; menus check
  (ExcelMenu/ExcelBedrockMenu — add the target popup to the quest/XP
  menu path if a fitting entry exists, else note none). Guide leg
  suspended under the Nov 16 Guide hibernation; website stays canon.
  Rename note: surfaces that name the plugin say ExcelEMQuestXP 1.1.0;
  the command stays /questxp everywhere.

## MANIFEST entry (pointer stub — for the supervisor to file)

- Heading: `## PENDING -- ExcelEMQuestXP 1.1.0 (2026-11-20)`
- Packet: `~/workspace/questxp-110-build-20261120/INSTALL-PACKET.md`
- Candidate: `ExcelEMQuestXP-1.1.0.jar` — 50,603 B / SHA-256
  `cab1574471c50ace591bc12603afbd42272c06aec8c4cc78a3b660ae112b670e`;
  tests 267/267; rig phase A 21/0 + phase B 12/0.
- Live-jar gate: `plugins/ExcelQuestXP-1.0.1.jar` = 31,728 B /
  `28e5dbf122423947034c356b0616a71e259bf1be3aef2044e090f6f223ae37da`;
  plus live `plugins/ExcelQuestXP/backpay.yml` sha256 `24274c55...`.
- Scope: jar swap 1.0.1 -> 1.1.0 WITH data-folder migration
  (copy plugins/ExcelQuestXP/ -> plugins/ExcelEMQuestXP/, byte-verified;
  old folder left in place); old jar out in the same swap; restart.
- Proof owed: console backpay dry verification pays 0 for both players
  (ledger migration proof); player pins a non-highest skill and a quest
  grant lands there; Bedrock menu = form, Java menu = chest GUI.
