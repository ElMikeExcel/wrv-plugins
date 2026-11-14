# RIG / ACCEPTANCE PLAN — wipe rehearsal + freeze proof (P-SEALFREEZE-1)

Runs on the PC test-server mirror (`C:\Users\MikeExcel\wrv-testserver\`)
**after the P-FSYNC-1 fixes land** (the mirror must boot BuildMode /
BossTrial / Shops / MakersMark without the directory-fsync degradations, or
no boot on it proves anything). Desk status: the clamp policy, seal-flag
parser, baseline round-trip and the full clamp engine are ALREADY proven
against the real pulled database (25/25 harness checks, including a
simulated-gain clamp on a throwaway copy). The rig proves the integrated
behavior — real EM writing to its real database under a running server.

## Fixture setup
1. Mirror server stopped. Copy the G4 pull database
   (`~/workspace/g4-pull-bm326/em_player_data.db`, sha256 `b9d251a0…`) into
   the mirror as `plugins/EliteMobs/data/player_data.db` (fixture copy —
   the pull itself is never written).
2. Mirror GuildGate state: leave the mirror's real `state.yml` (4 of 5,
   `unlocked: false`); keep a flipped copy ready (`unlocked: true`, warden
   `broken: true`) for step D.
3. Drop `ExcelSealFreeze-0.1.0.jar` into the mirror's `plugins/`.

## Steps

**A. Wipe rehearsal (executes WIPE-PACKET.md end-to-end on the fixture)**
- A-1. Run the packet's step-0 pre-checks on the fixture (11 rows, class
  progress 0).
- A-2. Backup step: fixture `.prewipe` copy created, hashes recorded.
- A-3. Run the wipe UPDATE on a working copy, run the packet's §5
  verification SELECTs — expect: (a) 0 offending rows, (b) 11 rows,
  (c) Deaths identical to the pre-wipe dump, (d) readback all zeros.
- A-4. Swap in, boot the mirror: EM enables clean (no database ERROR),
  GuildGate still reports 4 of 5.
- ✅ Pass = every §5 expectation holds AND the boot is clean. This rehearsal
  is the wipe packet's execution proof; the live run then follows the same
  paper in the gated window.

**B. Arm**
- B-1. `/sealfreeze arm` from console → baseline snapshot written;
  `/sealfreeze status` shows armed, baseline players = 11, seal locked.
- B-2. Inspect `plugins/ExcelSealFreeze/baseline.txt`: all values zero
  (post-wipe snapshot).

**C. Freeze holds under real play (the core acceptance)**
- C-1. Join a fixture player; spawn/kill a leveled elite through EM's own
  systems (custom boss or natural elite at a known level ≥ 5). Wait one
  clamp cycle (or `/sealfreeze clampnow`). Then read the fixture DB:
  **HighestLevelKilled, Score, Kills and every SkillXP_* for that player
  are still 0**, while Deaths (if they died) DID move — play and history
  both alive, only accrual held.
- C-2. Repeat with a second kill after a quit/rejoin (join/quit clamp path).
- C-3. **Currency + the Vault question:** give the fixture player an EM
  coin pickup during the freeze. Record (i) EM `CurrencyV2`/`CurrencyCents`
  after one cycle (expect clamped to 0) and (ii) the player's
  Vault/Essentials balance before/after the pickup. This step SETTLES the
  FREEZE-SCOPING Vault caveat: if the balance rose, the currency class
  leaks through Vault and the supervisor decides on the
  `enableCurrencyShower: false` rider for the live freeze window; if it
  did not, the DB clamp alone is complete.
- C-4. Shop check (play-not-blocked): the fixture player can still open
  the EM shop / Adventurers Guild surfaces during the freeze.

**D. Unlock restores gains**
- D-1. Stop the mirror (or use a live file swap + one cycle — the enforcer
  re-reads the file every cycle, no restart needed): replace GuildGate
  `state.yml` with the flipped fixture (`unlocked: true`).
- D-2. Within one clamp cycle the console logs the auto-lift line and
  `/sealfreeze status` shows disarmed.
- D-3. Kill another elite: HLK/Score/Kills/XP now ACCRUE and stay (two
  consecutive reads, one cycle apart, values stable and > 0).

**E. Fail-safe drills**
- E-1. Rename the fixture EM database while armed (fresh mirror boot
  without step B — re-arm first on a restored fixture): clamp cycles log
  "skipped" warnings; the server runs normally; no stack traces beyond the
  one-line warning; restore the file → cycles resume clamping.
- E-2. Delete `baseline.txt` while armed and restart: the plugin DISARMS
  with the severe log line rather than clamping blind.
- E-3. Corrupt the seal `state.yml` (truncate mid-file) while armed:
  freeze stays ON (reads locked), server unaffected.

## Acceptance mapping (backlog P-SEALFREEZE-1)
| Backlog acceptance | Proven by |
|---|---|
| Post-wipe verification table (all rows zeroed per scope) | Step A-3 (+ the live window's own §5 run) |
| A test elite kill during the freeze moves NO progression field | Steps C-1 / C-2 |
| Currency frozen (and the Vault question answered, not assumed) | Step C-3 |
| Play itself never blocked | Steps C-1 (deaths accrue), C-4 |
| Flipping the seal-5 flag restores gains automatically | Steps D-1 → D-3 |
| Fail-safe: DB trouble can never corrupt or lock the server | Steps E-1 → E-3 (+ the desk harness's rollback-tested engine) |

## Live-window follow-through (supervisor's, after rig pass)
Wipe window per WIPE-PACKET.md → `/sealfreeze arm` in the same window →
status readback in the OUTCOME file → the freeze rides until the Nov 14
Warden kill flips GuildGate to 5/5, at which point the lift log line is the
expected, celebrated no-op: progression simply starts accruing for the
expansion era, and ExcelExpansions' level store begins reading real values.
