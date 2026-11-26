# INSTALL PACKET — ExcelEMFixes 0.22.0 (quest-xp module; ExcelEMQuestXP fold, D244)

Build: EXC-EMFIXES-0.22.0-WRV-20261125 (Excel — Wink Reyes Valley).
Ruling: D244 (Mike, Nov 25, 2026) — ExcelEMQuestXP folds into
ExcelEMFixes as the toggleable `quest-xp` module; the standalone jar
retires in the same window, only after the module proofs pass (they
have — see RECEIPTS.md: unit suite 1,908/0, rig phases A 33/0, B 12/0,
C 6/0, boot SEVERE 0 in every phase).

## What changes

- ExcelEMFixes 0.21.0 → 0.22.0: adds ONE module, `quest-xp`
  (24 modules total). Player-facing behavior is IDENTICAL to the live
  ExcelEMQuestXP 1.1.0: the `/questxp` command name, syntax, messages,
  popup content/order, grant amounts (quest table 112 quests /
  130,355 XP, Armor at 1/3), target pins, and exactly-once backpay.
- The standalone `ExcelEMQuestXP-1.1.0.jar` is REMOVED in this window.
- All QuestXP state moves from `plugins/ExcelEMQuestXP/` to
  `plugins/ExcelEMFixes/quest-xp/` (same five filenames:
  quest-xp.yml, backpay.yml, backpay-paid.yml, targets.yml,
  grants.log), byte-verified, BEFORE the first 0.22.0 boot.

## Live-premise gate (verify BEFORE any step; abort on mismatch)

- `plugins/ExcelEMFixes-0.21.0.jar` — 552,041 B —
  sha256 `4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a`
- Live ExcelEMFixes `config.yml` — 81,482 B —
  sha256 `949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f`
- `plugins/ExcelEMQuestXP-1.1.0.jar` present (the standalone being retired).
- `plugins/ExcelEMQuestXP/` present with all five state files;
  `backpay-paid.yml` holds 46 paid quests across 2 players.
- Server verified empty (or the weekend push protocol, if Mike directs).

## Sequence (in order; do not reorder)

1. **Backup.** Snapshot/backup per the standing window procedure,
   PLUS file copies of: the live ExcelEMFixes jar + config.yml, the
   ExcelEMQuestXP jar, and the ENTIRE `plugins/ExcelEMQuestXP/`
   folder. Keep the backup until the window closes clean.
2. **Data migration (server stopped or before jar swap; files only).**
   Create `plugins/ExcelEMFixes/quest-xp/` and copy the five state
   files into it. Verify EACH file's sha256 equals its source in
   `plugins/ExcelEMQuestXP/` (all five must MATCH; abort on any
   mismatch). Leave `plugins/ExcelEMQuestXP/` in place (it becomes
   inert history once its jar is gone; it is also the rollback source).
3. **Jar swap.** Remove `ExcelEMQuestXP-1.1.0.jar` from `plugins/`.
   Replace the ExcelEMFixes jar with `ExcelEMFixes-0.22.0.jar`
   (596,100 B, sha256
   `49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04`).
   Replace ExcelEMFixes `config.yml` with this packet's
   `deploy-config.yml` (84,251 B, sha256
   `e8b670092ca423e0fd0e5ae7d555ea55729de89dfb3efe592b04de96bbe17086` —
   the live 0.21.0 config plus ONLY the `modules.quest-xp` section;
   pure addition, verified by diff).
   The standalone jar must be out BEFORE the boot: both jars listen
   to the same quest-completion event and own `/questxp` — never let
   both be present at a boot (double-grant risk).
4. **Boot verify.** Boot the server. Required evidence:
   - Boot SEVERE count 0.
   - ExcelEMFixes reports 24 modules enabled, `quest-xp` among them.
   - The quest-xp ready line reads: quests in table: 112; total XP
     across table: 130355; **backpay already paid: 46** (the migrated
     ledger's count — this is the ledger read-back proof); XP targets
     pinned: the live pin count from the migrated targets.yml.
   - `plugins/ExcelEMFixes/quest-xp/backpay-paid.yml` sha256 AFTER
     the boot still equals the pre-boot migrated copy (boot is
     read-only for the ledger).
5. **Ledger proof (already-paid spot check, optional but preferred
   when a paid player is online).** The boot count in step 4 is the
   proof of record. Do NOT run `/questxp backpay` on a real player
   as a "test" — it pays anything genuinely owed by design.
6. **Close-out.** Record the window (hashes, boot lines, SEVERE
   count) in the install record; run the surfaces pass in
   SURFACES.md; the retired standalone's data folder stays in place
   for one full backup cycle, then may be archived.

## Rollback

1. Stop the server. Remove `ExcelEMFixes-0.22.0.jar`; restore the
   backed-up 0.21.0 jar + live config.yml.
2. Restore `ExcelEMQuestXP-1.1.0.jar` into `plugins/`.
3. **Exactly-once guard:** if ANY backpay ran under the module
   (check `plugins/ExcelEMFixes/quest-xp/grants.log` for BACKPAY
   lines dated after the install), copy the module folder's
   `backpay-paid.yml` OVER `plugins/ExcelEMQuestXP/backpay-paid.yml`
   before booting — the standalone must see every quest the module
   paid, or it could pay one twice. Same for `targets.yml` if pins
   changed under the module (copy it over too).
4. Boot; verify ExcelEMQuestXP 1.1.0's ready line reports the same
   paid count the module last reported.
5. The `plugins/ExcelEMFixes/quest-xp/` folder may stay in place —
   0.21.0 ignores it.
