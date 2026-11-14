# PENDING - EM wipe + ExcelSealFreeze 0.1.0 (P-SEALFREEZE-1)
# CLEAN REBUILD Nov 14, 2026 (supervisor run 331 worker).
# Rebuilt from scratch from primary sources; see REBUILD-REPORT.md.
# Lint: 12078 B / max line 79 / non-ASCII 0

- Packet: ~/workspace/sealfreeze-filing-20261114/INSTALL-PACKET.md
- Item: P-SEALFREEZE-1. Authority: Mike, Nov 14, 2026,
  ~1:06-1:08 AM CST - wipe ALL EM progression to zero for
  the expansion era, coins too; NO gains stick until the
  fifth seal breaks (5/5); play never blocked.
- Marker on install:
  ~/workspace/sealfreeze-filing-20261114/INSTALLED.txt

## Candidate

- ExcelSealFreeze-0.1.0.jar - 20456 B - SHA-256
  d3610948d7a3d1bcdd150d1a61c9e27743d7dfdb7cb86cad23a434383c7ee925
  Path: ~/workspace/sealfreeze-20261114/ExcelSealFreeze-0.1.0.jar
  Build: EXC-SEALFREEZE-0.1.0-WRV-20261114-01. Tests: 25/25
  PASS (TEST-RESULTS.txt 1338 B /
  1fc760ca722fc27ebe162de8d4cc24393021a41904c1eb86ec8984cb8eb4148f).
  Deterministic: two build.sh runs byte-identical
  (BUILD-RECORD.md 4326 B /
  0ef53b7bb2bbc57f105def139cea926cb12493cbafb9dfecadee1e2781f67d5b;
  SHA256SUMS jar line matches the candidate hash).
- Plus the packet wipe UPDATE (section Steps, step 5) run on
  a working copy of the live DB. No other artifact.

## Receipts (cited by path + hash, never inlined)

- ~/workspace/sealfreeze-filing-20261114/RECEIPTS.md - 8356 B
  SHA-256 63863e73b80031438088021b36c46c65c26972ba82126211e473c3a69b4105b2
- ~/workspace/sealfreeze-filing-20261114/G1-CHECKLIST.md -
  5652 B - SHA-256
  90a250d8e5977f21c11072494597a9d28fd1e0062a147fa84b80cc8d3bd722e9
- Build inputs (hashes in RECEIPTS.md, verified for the
  rebuild report): WIPE-PACKET.md 9729 B, FREEZE-SCOPING.md
  7853 B, RIG-ACCEPTANCE-PLAN.md 5444 B (build folder);
  PACKET-DRAFT.md 8710 B, FREEZE-DESIGN.md 8196 B,
  RECEIPTS.md 4747 B (prep folder sealfreeze-prep-20261114).
- Rig: ~/workspace/expansion-rig-20261114/RIG-REPORT.md
  Part C, run 329 W7 - PASS for the gated window mechanics
  (wipe rehearsal exact, arm, clamp, unlock lift, fail-safes).

## Live-premise receipts (G1)

PC-route pulls 2026-11-14 02:44-02:45 CST, run 330; probe
PASS, last success 02:43:14 CST; no fallback. Copies in
g1-pull-20261114/. Slots 1-3 + 6 FILLED; 4-5 window-only.

- Slot 1 - live plugins/EliteMobs/data/player_data.db:
  pulled copy 28672 B /
  b9d251a0e61e5ecc7d1bea552f1fa86ffbae1b4941826c56df9f5c8074b4da4f;
  3 tables; PlayerData 31 cols / 11 rows; ClassProgress
  0 rows; ClassProfile 11 rows; integrity ok; verification
  set NULL x11; SkillBonusSelections '{}' x11; SUM Deaths
  18 / GamblingDebt 0.0 / Cents 0; no -wal / -shm at pull.
  Tools: /usr/bin/sqlite3 + python3 sqlite3 3.45.1.
- Slot 2 - live ExcelGuildGate/state.yml: 1015 B /
  79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0;
  unlocked: false; 4 of 5 broken (warden false). If
  unlocked already true at the window: STOP, escalate.
- Slot 3 - SealFreeze absence: 0 hits in listing_plugins
  (16709 B / 75c741afa4cc79c4712d876827a49a2190ae442b9c3b58942ad31b88a16484a7);
  jar + data folder ABSENT (lsf exit 3). Live jars:
  EliteMobs-10.9.10.jar, ExcelGuildGate-0.3.4.jar.
- Slot 6 - D50 rider carrier: live
  plugins/EliteMobs/EconomySettings.yml 9545 B /
  dbec1c1e80557898eb8c70186be62fec0dcd536da9b58daf4c67dc0420ffba93;
  line 13 useVault: true; line 15 enableCurrencyShower: true
  (the filing-day value the seal-5 lift restores).
  plugins/EliteMobs/config.yml 8419 B /
  8e4d72bfaaa12f9f4d4a04866317eee4077382cccc28fb1b11a8344ac319ff26;
  0 hits for either key - EconomySettings.yml is the carrier.
- Slots 4-5 NOT filled at filing, by design: slot 4 is the
  window emptiness pair + marker fields (server OFFLINE at
  pull time); slot 5 artifacts are created BY the window.
  The window re-verifies slots 1-3 + WAL before any write.

## Live-jar gate (STOP on mismatch)

- No live ExcelSealFreeze jar exists (slot 3). Placed bytes
  must hash to the candidate hash above. Any schema, row
  count, seal state, or jar-census mismatch against the
  STOP conditions in G1-CHECKLIST.md = STOP, no write,
  escalate to the supervisor.

## Scope

One gated unit, one empty-server full-stop window: DB
backup-first; 18-column wipe on a working copy; post-wipe
SELECT verification; jar placement + boot + /sealfreeze
arm; the D50 rider edit.
- WIPE to 0 (18 cols, all rows, no WHERE): CurrencyV2,
  CurrencyCents, Score, Kills, HighestLevelKilled,
  QuestsCompleted, DungeonsCompleted, SkillXP_ARMOR,
  SkillXP_SWORDS, SkillXP_AXES, SkillXP_BOWS,
  SkillXP_CROSSBOWS, SkillXP_TRIDENTS, SkillXP_HOES,
  SkillXP_MACES, SkillXP_SPEARS, SkillXP_STAVES,
  SkillXP_WANDS. (Quests/Dungeons are 0 x11; set anyway so
  the post-state is proven. Enforcer clamp set = 16: the
  18 minus those two, which do not accrue - D49.)
- PRESERVE, never written (8 cols): PlayerUUID, DisplayName,
  Deaths, GamblingDebt, GamblingDebtCents,
  BackTeleportLocation, UseBookMenus,
  DismissEMStatusScreenMessage.
- VERIFY, never written (5 cols): QuestStatus,
  PlayerQuestCooldowns, DungeonBossLockouts, QuestLockouts
  (NULL x11), SkillBonusSelections ('{}' x11). Other value
  = STOP. ClassProgress must be 0 rows (else STOP);
  ClassProfile 11 rows preserved, dump-diffed. No deletes.
- RIDER (D50, run 329): the sole EM config exception - set
  enableCurrencyShower: false for the freeze duration,
  restored to the filing-day value (true, slot 6) at the
  seal-5 lift. No other EM key touched; useVault unchanged.
- NOT changed: GuildGate state, any other jar, any other
  plugin data, world data, Vault/Essentials balances.

## Mechanism (D49, run 329, Nov 14 ~1:40 AM CST)

- ADOPTED: the built ExcelSealFreeze 0.1.0 enforcer is the
  freeze mechanism. While armed it clamps any progression
  column exceeding the arm-time baseline back TO baseline
  (never below; death-driven negative Score survives), on an
  async cycle (default 60 s) plus join/quit passes, in one
  transaction with rollback; it never touches Deaths /
  gambling / quest columns, cancels no events, does no
  main-thread DB work, and auto-disarms the first cycle
  GuildGate reads unlocked: true (5/5). Arm refused if
  already unlocked or DB unreachable; armed-without-baseline
  disarms on boot; DB busy/missing = log + skip the cycle.
- Tier 1 post-lift reconciliation (same UPDATE + V1-V6 at
  the first gated window after lift) is BACKSTOP ONLY (D49).
- Rig Part C: wipe rehearsal exact; arm baseline all-zero;
  injected gains clamped; unlock lifted within one cycle;
  E-2 / E-3 PASS. Player-proof class: C-3 Vault pickup leg
  (closed live by the D50 rider) + C-1/C-2/C-4 client forms.

## Gates

- WINDOW-IN-PROGRESS.txt marker created at window open;
  re-checked immediately before Shut Down and again before
  Start; a foreign marker under 3 hours old, or an in-flight
  failsafe run, = coordination abort (entry stays PENDING).
- Emptiness: RCON list = 0 twice before Shut Down (slot 4).
  The wipe write happens ONLY with the server fully stopped
  (EM holds player data in memory while running).
- BACKUP FIRST: one fresh Shockbyte backup at window open,
  cap-at-6 (prune oldest manuals by name; never the Daily
  newest or the two newest manuals). PLUS step 2's DB
  backup. No reload-only exemption (jar + data change).

## Steps (window procedure)

0. Re-pull slots 1-3 + 6 fresh; re-dump schema; confirm
   sqlite tool; fill slot 4. Any mismatch = STOP.
1. Open the window backup (Gates); record name + prunees
   in slot 5.
2. Shut Down (full stop). Confirm stopped + shutdown lines
   in latest.log; re-check the WAL line here (any -wal / -shm
   beside the stopped DB is copied with the backup set).
   Copy player_data.db to player_data.db.prewipe-<timestamp>
   on the server; pull one copy off-server; SHA-256 both -
   equal required. Record name + both hashes in slot 5.
3. Pre-wipe snapshot (script output): per-player table +
   sums on the stopped DB; verification-set check +
   ClassProgress 0-row guard. Any failure = STOP.
4. Working copy: copy player_data.db to a wipework copy.
   Run the wipe on the WORKING COPY only, one transaction;
   commit only after the in-transaction V2 check returns 0:
   UPDATE PlayerData SET CurrencyV2=0, CurrencyCents=0,
   Score=0, Kills=0, HighestLevelKilled=0, QuestsCompleted=0,
   DungeonsCompleted=0, SkillXP_ARMOR=0, SkillXP_SWORDS=0,
   SkillXP_AXES=0, SkillXP_BOWS=0, SkillXP_CROSSBOWS=0,
   SkillXP_TRIDENTS=0, SkillXP_HOES=0, SkillXP_MACES=0,
   SkillXP_SPEARS=0, SkillXP_STAVES=0, SkillXP_WANDS=0;
   Expected: 11 rows updated (the step-3 count governs; a
   mismatch = STOP). Run V1-V6 on the working copy; all
   must pass BEFORE the swap.
5. Swap: live DB to a pre-swap safety name, verified
   wipework file to player_data.db; no stale pre-wipe
   -wal / -shm stays paired. Record post-swap SHA-256 in
   slot 5. Re-run V1-V6 on the now-live path (fresh pull).
6. Place ExcelSealFreeze-0.1.0.jar in plugins/; SHA-256 the
   placed bytes - must equal the candidate hash.
7. RIDER (D50): while still stopped, back up
   EconomySettings.yml in place (.bak first), then set
   enableCurrencyShower: false; grep-verify false and
   useVault still true; no other key changed.
8. Start. Boot verify (next section). Then an op runs
   /sealfreeze arm (its baseline snapshot IS the wiped
   zeros), then /sealfreeze status.
9. Write the marker file (Marker section), delete
   WINDOW-IN-PROGRESS.txt, close the window record with
   slot 5 complete.

## Verification by script output

- V1 rows: SELECT COUNT(*) FROM PlayerData; -> 11
- V2 wiped-zero: SELECT COUNT(*) FROM PlayerData WHERE any
  of the 18 wipe columns <> 0; -> 0
- V3 preserved sums: SELECT SUM(Deaths), SUM(GamblingDebt),
  SUM(GamblingDebtCents) FROM PlayerData; -> identical to
  the step-3 snapshot (filing base: 18, 0.0, 0).
- V4 preserved rows: dump the 8 preserve columns +
  SkillBonusSelections ordered by PlayerUUID, pre vs post;
  diff -> no differences (Deaths + GamblingDebt preserved).
- V5 other tables: PlayerClassProgress count -> 0;
  PlayerClassProfile full dump pre vs post -> no diff.
- V6 integrity: PRAGMA integrity_check; -> ok
- V7 boot: latest.log Done line present; EliteMobs enable
  lines with no database ERROR; SEVERE 0 attributable;
  ExcelGuildGate shows the same LOCKED 4 of 5 as slot 2.
  ExcelExpansions level store seeds from the V1/V2 zeros.
- V8 arm: /sealfreeze status -> armed: true, baseline
  players = 11, seal unlocked: false. Placed jar SHA-256
  = candidate hash.
- V8R rider: post-Start grep of live EconomySettings.yml
  -> enableCurrencyShower: false; the .bak + the filing-day
  value (true) are in the window record.
- V9 RIDER LIFT (at seal 5/5, same lift window as the
  freeze lift): restore enableCurrencyShower to the
  filing-day value from the rider .bak; grep-verify true;
  Tier 1 backstop re-check V1-V6 runs in that window (D49).

## Rollback

- Before the swap: discard the working copy (live DB never
  written); remove any placed jar; restore EconomySettings
  from its rider .bak if step 7 ran; Start; boot verify.
- After the swap: Shut Down; restore the step-2 prewipe
  backup to player_data.db (+ sidecars if backed up);
  SHA-256 must equal the recorded backup hash; remove the
  placed jar + data folder; restore the rider .bak; Start;
  step-3 sums must match the pre-wipe snapshot exactly.
  Reversible until the 5/5 lift; after lift, rollback needs
  Mike's word (it would erase post-lift gains).

## Same-pass surfaces

None owed: no player-facing change. Guide, website, What's
New (both), tips, menus checked - no change; expansion
framing ships with ExcelExpansions.

## Proof owed after install

No player proof for the wipe: correctness is the V1-V8R
script output + the post-wipe SELECT table in the window
record. Rig Part C PASS stands for clamp mechanics; the
client kill / join-quit / shop forms stay player-proof
class, and the C-3 Vault leg is closed live by the rider.

## Marker on install

~/workspace/sealfreeze-filing-20261114/INSTALLED.txt
Written by the installing window only, after V1-V8R pass.
