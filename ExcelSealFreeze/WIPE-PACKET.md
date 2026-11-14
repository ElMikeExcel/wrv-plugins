# WIPE PACKET — EliteMobs progression reset (P-SEALFREEZE-1, half 1)

**Status: PREPARED, NOT EXECUTED.** This packet is paper. It is written for the
supervisor's gated empty-server window to execute verbatim. No live database
was touched in its preparation (desk evidence: the G4 pull copy still hashes
to `b9d251a0…`, unchanged).

**Authority:** Mike's directive, Nov 14, 2026, ~1:06–1:08 AM CST — wipe ALL
EliteMobs player progression to zero for the expansion era, "everything EM,
coins too." Recorded in MEMORY.md + backlog P-SEALFREEZE-1.

**Target:** `plugins/EliteMobs/data/player_data.db` on the live server
(Shockbyte root nests the server under `1. Michael's Server/`), table
`PlayerData`, SQLite (`Database.yml: useMySQL: false`).

---

## 1. Scope — the wipe set (all 11 rows, no WHERE clause)

Zero exactly these 17 columns:

`HighestLevelKilled`, `Score`, `Kills`, `CurrencyV2`, `CurrencyCents`,
`SkillXP_ARMOR`, `SkillXP_SWORDS`, `SkillXP_AXES`, `SkillXP_BOWS`,
`SkillXP_CROSSBOWS`, `SkillXP_TRIDENTS`, `SkillXP_HOES`, `SkillXP_MACES`,
`SkillXP_SPEARS`, `SkillXP_STAVES`, `SkillXP_WANDS`

**NEVER touched (history / identity, not progression):** `Deaths`,
`GamblingDebt`, `GamblingDebtCents`, `QuestStatus`, `QuestLockouts`,
`DungeonBossLockouts`, `PlayerQuestCooldowns`, `BackTeleportLocation`,
`UseBookMenus`, `DismissEMStatusScreenMessage`, `SkillBonusSelections`,
`DisplayName`, `PlayerUUID`. No rows are deleted; no other table is written.
`PlayerClassProgress` held **0 rows** at the G4 pull — the executor re-counts
it in step 0 and STOPS for escalation if it is non-zero (out of packet scope).
`PlayerClassProfile.SelectedFormId` is class identity, not accrual — untouched.

## 2. Pre-wipe state of record (G4 live pull, Nov 14 ~00:26 CST, sha256 `b9d251a0…`)

| Player | HLK | Score | Kills | Deaths* | CurV2 | Cents | Notable SkillXP |
|---|---|---|---|---|---|---|---|
| GumGumJetPistol (Marvin) | 23 | 858 | 75 | 3 | 7.0 | 700 | ARMOR 571, SWORDS 631, BOWS 935, TRIDENTS 32 |
| Jester_Turtle (Jesús) | 22 | 3674 | 220 | 1 | 0.0 | 0 | ARMOR 3550, SWORDS 1701, BOWS 610, MACES 25, TRIDENTS 10, AXES 3 |
| .coolgirlkail (Kailey) | 16 | −988 | 10 | 11 | 0.0 | 0 | SWORDS 7 |
| .MikeExcel (Mike, Bedrock) | 12 | 86 | 9 | 0 | 0.0 | 0 | BOWS 4, TRIDENTS 1 |
| FADEOUTMinecraft | 2 | 4 | 1 | 0 | 0.0 | 0 | SWORDS 1 |
| oklajoma | 2 | 4 | 1 | 0 | 0.0 | 0 | AXES 1 |
| .RylieDSunflower | 0 | 0 | 0 | 0 | 0.0 | 0 | — |
| .KWSunflower | 0 | 0 | 0 | 0 | 0.0 | 0 | — |
| MikeExcel (Java) | 0 | 0 | 0 | 0 | 0.0 | 0 | — |
| .FoamyDragon9514 | 0 | 0 | 0 | 0 | 0.0 | 0 | — |
| Racimbio | 0 | −300 | 0 | 3 | 0.0 | 0 | — |

\* Deaths shown for the untouched-column diff check in step 4; Deaths are NOT wiped.
QuestsCompleted / DungeonsCompleted: 0 on all 11 rows (nothing to lose).
**The executor re-pulls and re-verifies this table at window time (step 0)** —
values may have drifted since the pull; drift is fine, the wipe zeroes
whatever is there, but the row count and the Deaths column are the diff base.

## 3. Procedure

### Step 0 — Window gates + fresh pull (server still running)
- Normal window discipline: `WINDOW-IN-PROGRESS.txt` marker set, failsafe
  coordination checked, emptiness verified (`list` = 0, twice).
- Fresh-pull `plugins/EliteMobs/data/player_data.db` via the PC rclone lane.
  Run the §5 pre-checks against the pull: row count (expect 11),
  `PlayerClassProgress` count (expect 0 — else STOP), and record the pull's
  SHA-256. This pull is evidence only; the write happens on the server copy.

### Step 1 — Quiesce: FULL server stop (this is the quiesce step; nothing weaker is accepted)
EliteMobs/MagmaCore keeps `player_data.db` open with live connection(s) and
flushes loaded player objects on save cycles and on plugin disable. Editing
the file under a running server — or across a mere plugin reload — races
those flushes and can silently resurrect wiped values. Therefore:
- Panel **Shut Down** (the window's one backup per the standing rule is taken
  at window open, before this, per normal discipline).
- Confirm the process is fully down: panel status stopped + `latest.log`
  shows the shutdown completing (EliteMobs disable lines present, no new
  writes for ~30 s).

### Step 2 — Backup (BEFORE any write; two copies, hash-matched)
- On the server (via the PC SFTP/rclone lane or panel File Manager): copy
  `plugins/EliteMobs/data/player_data.db` →
  `plugins/EliteMobs/data/player_data.db.prewipe-20261114`.
  If `player_data.db-wal` / `player_data.db-shm` exist beside it after the
  clean stop, copy them to matching `.prewipe-20261114` names too and keep
  them with the backup set (do not delete them; a clean stop should have
  checkpointed the WAL into the main file, making them absent or empty).
- Pull the `.prewipe-20261114` copy off-server to this lane folder:
  `~/workspace/sealfreeze-20261114/backup/player_data.db.prewipe-20261114`.
- Record SHA-256 of BOTH copies in the window's OUTCOME file; they must be
  identical to each other. (They will differ from the step-0 pull if the
  server ran between pull and stop — that is expected; the backup of record
  is the stopped-server copy.)

### Step 3 — The wipe (edit a WORKING COPY, verify, then swap)
Work on a copy so the in-place original survives until the wiped file is
proven good:
1. Copy `player_data.db` → `player_data.db.wipework` (server side).
2. Run against `player_data.db.wipework` (sqlite3 CLI or Python `sqlite3`
   on the PC against the pulled working copy, then push it back — either
   route, but the file that lands in step 3.4 must be the verified one):
   ```sql
   BEGIN IMMEDIATE;
   UPDATE PlayerData SET
     HighestLevelKilled = 0,
     Score = 0,
     Kills = 0,
     CurrencyV2 = 0.0,
     CurrencyCents = 0,
     SkillXP_ARMOR = 0, SkillXP_SWORDS = 0, SkillXP_AXES = 0, SkillXP_BOWS = 0,
     SkillXP_CROSSBOWS = 0, SkillXP_TRIDENTS = 0, SkillXP_HOES = 0,
     SkillXP_MACES = 0, SkillXP_SPEARS = 0, SkillXP_STAVES = 0,
     SkillXP_WANDS = 0;
   COMMIT;
   ```
   Expected: `11` rows updated (or whatever step 0 counted — the counts must
   agree; a mismatch = STOP).
3. Run the §5 verification SELECTs against the working copy. All must pass
   BEFORE the swap.
4. Swap: rename `player_data.db` → `player_data.db.pre-swap-20261114`
   (a second, redundant safety copy), then rename `player_data.db.wipework`
   → `player_data.db`. Ensure no stale `player_data.db-wal`/`-shm` from the
   pre-wipe file remains paired with the new file — if the step-2 WAL files
   existed, move them alongside their `.prewipe-20261114` names so the new
   database starts with no sidecars.

### Step 4 — Post-swap verification (file level, server still down)
Re-run the §5 SELECTs against the now-live `player_data.db` path (a fresh
pull). Identical expectations. Record the wiped file's SHA-256.

### Step 5 — Boot + freeze arming (same window)
- Panel **Start**. Boot checks on `latest.log`: EliteMobs enables with no
  database ERROR/SEVERE; ExcelGuildGate reports "4 of 5 seals broken"
  (unchanged — the wipe does not touch GuildGate).
- Install/arm the freeze in the same window, in this order: the
  ExcelSealFreeze jar is filed/installed per its own packet (supervisor
  owns); after boot, an op runs `/sealfreeze arm` — the baseline snapshot
  it takes IS the wiped zeros — then `/sealfreeze status` must show
  `armed: true`, baseline players = row count, `seal unlocked: false`.
- First live sanity: with the server empty this is a state check only; the
  behavioral proof is the rig plan (RIG-ACCEPTANCE-PLAN.md) and, if Mike is
  in game later, one elite kill + `/sealfreeze clampnow` + status.

### Rollback (any failure after the swap)
Stop the server → restore `player_data.db.prewipe-20261114` (step-2 backup)
to `player_data.db` (with its WAL sidecars if any were backed up) → Start →
verify boot + the §5 pre-check values are back. The wipe is fully reversible
until the freeze era ends; keep the backup at least until the fifth seal
breaks.

## 4. What this packet deliberately does NOT do
- No GuildGate / BossTrial state changes (the seals stand at 4 of 5).
- No Vault/Essentials money changes — EM's currency columns only. Whether
  EM coin pickups also credit the Vault balance under `useVault: true` is a
  rig question (RIG-ACCEPTANCE-PLAN step C-3), not a wipe question.
- No deletion of the held ResurrectionChest folder's data, no other plugin's
  files.

## 5. SQL of record

Pre-checks (against any copy):
```sql
SELECT COUNT(*) FROM PlayerData;                 -- expect 11
SELECT COUNT(*) FROM PlayerClassProgress;        -- expect 0, else STOP
SELECT PlayerUUID, Deaths FROM PlayerData ORDER BY PlayerUUID;  -- diff base
```

Post-wipe verification (ALL must hold):
```sql
-- (a) every progression column is zero on every row: expect 0 rows returned
SELECT PlayerUUID FROM PlayerData
WHERE HighestLevelKilled <> 0 OR Score <> 0 OR Kills <> 0
   OR CurrencyV2 <> 0.0 OR CurrencyCents <> 0
   OR SkillXP_ARMOR <> 0 OR SkillXP_SWORDS <> 0 OR SkillXP_AXES <> 0
   OR SkillXP_BOWS <> 0 OR SkillXP_CROSSBOWS <> 0 OR SkillXP_TRIDENTS <> 0
   OR SkillXP_HOES <> 0 OR SkillXP_MACES <> 0 OR SkillXP_SPEARS <> 0
   OR SkillXP_STAVES <> 0 OR SkillXP_WANDS <> 0;
-- (b) row count unchanged: expect the step-0 count (11)
SELECT COUNT(*) FROM PlayerData;
-- (c) history untouched: Deaths per UUID identical to the step-0 diff base
SELECT PlayerUUID, Deaths FROM PlayerData ORDER BY PlayerUUID;
-- (d) full readback table for the window record
SELECT DisplayName, HighestLevelKilled, Score, Kills, Deaths, CurrencyV2,
       CurrencyCents FROM PlayerData ORDER BY HighestLevelKilled DESC;
```
