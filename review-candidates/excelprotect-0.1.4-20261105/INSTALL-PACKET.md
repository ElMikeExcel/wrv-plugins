# INSTALL PACKET — ExcelProtect 0.1.4 "Where" (location timelines + playtime/where + all-stream inspect)

**What:** swap the live ExcelProtect 0.1.3 jar for 0.1.4. 0.1.4 adds the
`locations` stream (position samples + typed anchors), the `where` and
`playtime` console queries, and extends `inspect player` to all
streams in per-stream sections. Everything 0.1.3 does is carried
unchanged. Config untouched (the new keys default ON/60s when absent,
which is the desired state).

## Gate — read before touching anything

- **BACKUP FIRST + cap-at-6:** take a fresh Shockbyte backup before the
  swap. Before creating it, prune the OLDEST manual backups until the
  post-creation total is ≤ 6; never delete the Daily Backup's newest
  copy or the two newest manual restore points; name prunees in the
  report.
- **VERIFIED-EMPTY:** install only with the server verified empty
  (list = 0 twice) per the standing gate (weekend countdown protocol
  supersedes on Fri/Sat if Mike directs).
- **PANEL DISCIPLINE:** one panel actor; create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window
  start, delete at finish.
- **QUEUE ORDER:** 0.1.3 installs AHEAD of this entry. If 0.1.3 has not
  landed yet, this window does not run — never skip a version.

## LIVE GATE — jar identity, or STOP

The live `plugins/ExcelProtect-0.1.3.jar` MUST be exactly:
- size **165,366 B**, SHA-256
  `f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`

Any mismatch = STOP, do not swap, report. If the live jar is still
0.1.2 (138,039 B / `26b7f5ba…`), STOP the same way — 0.1.3 goes first.
(Panel shows size; the SHA is verified against the build folder copy
of record, `~/workspace/protect-dealt-build/ExcelProtect-0.1.3.jar`.)

Candidate jar: `ExcelProtect-0.1.4.jar` — **184,594 B**, SHA-256
`2b471e7b45f9dd029f79570eee3bb24d99da3f830ca17c097c3e942dda9d3b4c`
(deterministic double-build, byte-identical; repro jar alongside).

## Steps

1. Backup first per the gate above.
2. Rename the live jar aside:
   `plugins/ExcelProtect-0.1.3.jar` → `plugins/ExcelProtect-0.1.3.jar.held-<date>-pre014`
3. Place `ExcelProtect-0.1.4.jar` in `plugins/`. Verify size 184,594 B.
4. **Config: DO NOT touch.** `plugins/ExcelProtect/config.yml` stays
   as live; the new keys (`log.locations`,
   `locations.sample-seconds`) default ON/60s when absent.
5. Restart via panel Shut Down + Start (no console stop).
6. **Boot-verify in the log:**
   - `[ExcelProtect] damage incidents: ON gap=2000ms near-death<=30% elitemobs=hooked` (carried)
   - `[ExcelProtect] damage dealt: ON gap=2000ms elitemobs=hooked` (carried)
   - `[ExcelProtect] healing incidents: ON gap=2000ms` (carried)
   - `[ExcelProtect] locations: ON sample=60s anchors=join,quit,teleport,world,respawn,gamemode` (new)
   - `[ExcelProtect] READY version=0.1.4` with gp/vault/mcmmo as live
   - `[ExcelProtect] combat: Magic CastEvent feed hooked` (carried)
   - GuildGate seals line unchanged (3 of 5, LOCKED), plugin count 79,
     SEVERE 0 attributable to the swap.
7. Console smoke: `excelprotect status` → version=0.1.4,
   dropped-total=0; `excelprotect playtime <anyone>` → a real total
   or the clean "no recorded sessions" line (the players stream has
   arcs going back to 0.1.0, so real totals are expected);
   `excelprotect where <anyone>` → the clean "no location records"
   line (fresh stream) until someone has played a minute.
8. Exactly ONE active ExcelProtect jar afterwards
   (`ExcelProtect-0.1.4.jar`); the held 0.1.3 jar stays inert beside it.
9. Write `~/workspace/protect-where-build/INSTALLED.txt`, move the
   MANIFEST entry to INSTALLED with the boot facts, append the
   daily log.

## Rollback

Stop the server, remove `ExcelProtect-0.1.4.jar`, rename
`ExcelProtect-0.1.3.jar.held-<date>-pre014` back to
`ExcelProtect-0.1.3.jar`, start. The new stream's files
(`locations-*.jsonl` in `plugins/ExcelProtect/`) are inert to 0.1.3 —
it never reads them; leave them in place.

## Post-install proofs owed (in play)

1. **A real where-route:** after any player's normal session (join,
   some travel, a /places or /tpa hop, quit), console
   `excelprotect where <player>` renders the route in order — start,
   the teleport anchor with its cause, the quit — matching where
   they actually went.
2. **Playtime sanity:** `excelprotect playtime <player>` for someone
   who played that day reads within a minute of their real session
   length; the same name with a past day count (`playtime <player> 7`)
   totals the week.
3. **Inspect breadth:** `excelprotect inspect player <player>` on an
   active player shows sections from at least five streams (players,
   locations, combat/incidents, plus world/economy as applicable),
   each capped with its "…and N more" tail where the stream is deep.
4. **Bedrock parity:** a Bedrock player's samples and anchors record
   identically (platform field reads bedrock) — the .Name player's
   next session answers `where` the same way.
