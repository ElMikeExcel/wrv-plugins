# INSTALL PACKET — ExcelProtect 0.1.3 (damage dealt + healing incidents)

**What:** swap the live ExcelProtect 0.1.2 jar for 0.1.3. 0.1.3 adds two
coalesced incident streams — `dealt` (player → mob damage, elite victims,
kill attribution) and `healing` (self vs player-to-player) — plus the
`dealt` / `healing` console query forms. Everything 0.1.2 does is carried
unchanged. Config untouched (new toggles default ON when absent, which is
the desired state).

## Gate — read before touching anything

- **BACKUP FIRST + cap-at-6:** take a fresh Shockbyte backup before the
  swap. Before creating it, prune the OLDEST manual backups until the
  post-creation total is ≤ 6; never delete the Daily Backup's newest copy
  or the two newest manual restore points; name prunees in the report.
- **VERIFIED-EMPTY:** install only with the server verified empty
  (list = 0 twice) per the standing gate (weekend countdown protocol
  supersedes on Fri/Sat if Mike directs).
- **PANEL DISCIPLINE:** one panel actor; create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window start,
  delete at finish.

## LIVE GATE — jar identity, or STOP

The live `plugins/ExcelProtect-0.1.2.jar` MUST be exactly:
- size **138,039 B**, SHA-256
  `26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`

Any mismatch = STOP, do not swap, report. (Panel shows size; the SHA is
verified against the build folder copy of record,
`~/workspace/protect-damage-build/ExcelProtect-0.1.2.jar`.)

Candidate jar: `ExcelProtect-0.1.3.jar` — **165,366 B**, SHA-256
`f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`
(deterministic double-build, byte-identical; repro jar alongside).

## Steps

1. Backup first per the gate above.
2. Rename the live jar aside:
   `plugins/ExcelProtect-0.1.2.jar` → `plugins/ExcelProtect-0.1.2.jar.held-<date>-pre013`
3. Place `ExcelProtect-0.1.3.jar` in `plugins/`. Verify size 165,366 B.
4. **Config: DO NOT touch.** `plugins/ExcelProtect/config.yml` stays as
   live; the new keys (`log.combat.damage-dealt`,
   `log.combat.healing-incidents`) default ON when absent.
5. Restart via panel Shut Down + Start (no console stop).
6. **Boot-verify in the log:**
   - `[ExcelProtect] damage incidents: ON gap=2000ms near-death<=30% elitemobs=hooked` (carried)
   - `[ExcelProtect] damage dealt: ON gap=2000ms elitemobs=hooked` (new)
   - `[ExcelProtect] healing incidents: ON gap=2000ms` (new)
   - `[ExcelProtect] READY version=0.1.3` with gp/vault/mcmmo as live
   - `[ExcelProtect] combat: Magic CastEvent feed hooked` (carried)
   - GuildGate seals line unchanged (3 of 5, LOCKED), plugin count 79,
     SEVERE 0 attributable to the swap.
7. Console smoke: `excelprotect status` → version=0.1.3, dropped-total=0;
   `excelprotect dealt <anyone>` and `excelprotect healing <anyone>` →
   the clean "no … recorded" line (fresh streams) or real records.
8. Exactly ONE active ExcelProtect jar afterwards
   (`ExcelProtect-0.1.3.jar`); the held 0.1.2 jar stays inert beside it.
9. Write `~/workspace/protect-dealt-build/INSTALLED.txt`, move the MANIFEST
   entry to INSTALLED with the boot facts, append the daily log.

## Rollback

Stop the server, remove `ExcelProtect-0.1.3.jar`, rename
`ExcelProtect-0.1.2.jar.held-<date>-pre013` back to
`ExcelProtect-0.1.2.jar`, start. The new streams' files
(`dealt-*.jsonl`, `healing-*.jsonl` in `plugins/ExcelProtect/`) are inert
to 0.1.2 — it never reads them; leave them in place.

## Post-install proofs owed (in play)

1. **Volunteer engagement:** a player beats on a normal mob for a few
   seconds, pauses, and console `excelprotect dealt <player>` shows ONE
   merged incident with the health arc.
2. **Elite contribution:** after a real elite kill (any natural elite),
   `excelprotect dealt <player>` shows the elite incident with
   `ELITE Lv<n>` and the party's per-player totals answer the
   contribution question.
3. **Healing:** a player heals themself (potion/food) and later receives
   a Magic spell heal from another player; `excelprotect healing
   <player>` shows the self line and the received SPELL line with the
   healer named.
4. **PvP boundary:** a friendly PvP hit appears in `damage` (victim side,
   0.1.2) and does NOT appear in the attacker's `dealt` — no double count.
