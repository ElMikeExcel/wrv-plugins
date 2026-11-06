# INSTALL PACKET — ExcelProtect 0.1.5 "Clarity" (delivery outcomes + item chains + chain-of-custody query + inspect pairing/LIVE read + player timelines)

**What:** swap the live ExcelProtect 0.1.4 jar for 0.1.5. 0.1.5 stamps
delivery outcomes on acquisitions (`item_delivery` records), links
trades/drops/pickups/chests into item chains, adds the
`/excelprotect item` chain-of-custody query and
`/excelprotect timelines rebuild`, pairs drops with resolutions in
`inspect player` (UNRECOVERED flags), reads an online player's
inventory LIVE in inspect, and projects every player-named record
into per-player timeline files (`players/<player>-<day>.jsonl`).
Everything 0.1.4 does is carried unchanged; all changes are additive
— no existing stream format changes. **Config:** the live config may
stay as-is (the new keys default ON/1800/5 when absent, which is the
desired state); placing the shipped 0.1.5 `config.yml` is equally
safe if the live file is first hash-checked against the 0.1.4 shipped
config — when in doubt, do not touch the config.

## Gate — read before touching anything

- **BACKUP FIRST + cap-at-6:** take a fresh Shockbyte backup before
  the swap. Before creating it, prune the OLDEST manual backups until
  the post-creation total is ≤ 6; never delete the Daily Backup's
  newest copy or the two newest manual restore points; name prunees
  in the report.
- **VERIFIED-EMPTY:** install only with the server verified empty
  (list = 0 twice) per the standing gate (weekend countdown protocol
  supersedes on Fri/Sat if Mike directs).
- **PANEL DISCIPLINE:** one panel actor; create
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window
  start, delete at finish; re-check the marker immediately before
  Shut Down and again before Start.
- **QUEUE ORDER:** 0.1.4 is LIVE (installed Nov 5, 2026). This entry
  swaps 0.1.4 → 0.1.5 directly. Never skip a version.

## LIVE GATE — jar identity, or STOP

The live `plugins/ExcelProtect-0.1.4.jar` MUST be exactly:
- size **184,594 B**, SHA-256
  `2b471e7b45f9dd029f79570eee3bb24d99da3f830ca17c097c3e942dda9d3b4c`

Any mismatch = STOP, do not swap, report. (Panel shows size; the SHA
is verified against the build folder copy of record,
`~/workspace/protect-where-build/ExcelProtect-0.1.4.jar`.)

Candidate jar: `ExcelProtect-0.1.5.jar` — **225,181 B**, SHA-256
`c9b5dc1f58cd67de7db60e67e82b8e1cd8cb83b6ed0207ae9597bca0fa3ed5cb`
(deterministic double-build, byte-identical; repro jar alongside;
fingerprint gate PASS — the jar carries `excel-build.txt`).

## Steps

1. Backup first per the gate above.
2. Rename the live jar aside:
   `plugins/ExcelProtect-0.1.4.jar` → `plugins/ExcelProtect-0.1.4.jar.held-<date>-pre015`
3. Place `ExcelProtect-0.1.5.jar` in `plugins/`. Verify size 225,181 B.
4. **Config: leave the live file alone** (defaults carry the new
   keys). Do NOT overwrite a live config that has drifted from the
   shipped one.
5. Restart via panel Shut Down + Start (no console stop).
6. **Boot-verify in the log:**
   - `[ExcelProtect] damage incidents: ON gap=2000ms near-death<=30% elitemobs=hooked` (carried)
   - `[ExcelProtect] damage dealt: ON gap=2000ms elitemobs=hooked` (carried)
   - `[ExcelProtect] healing incidents: ON gap=2000ms` (carried)
   - `[ExcelProtect] locations: ON sample=60s anchors=join,quit,teleport,world,respawn,gamemode` (carried)
   - `[ExcelProtect] clarity: ON delivery=next-tick chains=expiry 1800s ground-radius=5` (new)
   - `[ExcelProtect] player timelines: ON (projection of the streams; streams stay canonical)` (new)
   - `[ExcelProtect] READY version=0.1.5` with gp/vault/mcmmo as live
   - `[ExcelProtect] combat: Magic CastEvent feed hooked` (carried)
   - GuildGate seals line unchanged (3 of 5, LOCKED), plugin count 79,
     SEVERE 0 attributable to the swap.
7. Console smoke: `excelprotect status` → version=0.1.5,
   dropped-total=0, and a `timelines:` health line with failures=0;
   `excelprotect item <anyone> diamond` → a story or the clean
   "no records for … in the last 24h" line (pre-0.1.5 records carry no
   chains — plain event lines are correct for them);
   `excelprotect timelines rebuild <a past day>` → a copies count
   line, no errors.
8. Exactly ONE active ExcelProtect jar afterwards
   (`ExcelProtect-0.1.5.jar`); the held 0.1.4 jar stays inert beside it.
9. Write `~/workspace/protect-clarity-build/INSTALLED.txt`, move the
   MANIFEST entry to INSTALLED with the boot facts, append the
   daily log.

## Rollback

Stop the server, remove `ExcelProtect-0.1.5.jar`, rename
`ExcelProtect-0.1.4.jar.held-<date>-pre015` back to
`ExcelProtect-0.1.4.jar`, start. 0.1.5's additions are inert to
0.1.4: `item_delivery` lines ride the economy day files 0.1.4 already
reads past as an unknown type, `file_header` lines likewise, and the
`players/` timeline folder is never read by 0.1.4 — leave everything
in place.

## Post-install proofs owed (in play)

1. **A real trade, both landings:** a player buys from a villager
   with space (trade record + `delivered inventory`) and once with a
   full inventory near a villager (the spilled item reads
   `delivered ground`); console `excelprotect item <player> <item>`
   tells both stories in order.
2. **A real ground pickup chain:** the spilled item from proof 1 is
   picked up minutes later by a real (client) player; the pickup
   record carries the trade's chain id, and the item query closes
   the story with "chain in inventory".
3. **Inspect in play:** `excelprotect inspect player <online player>`
   shows the [LIVE] section matching what they're actually carrying,
   and the [items] section pairs a recent drop or flags it
   UNRECOVERED; the same command on an offline player shows the
   snapshot as-of line instead.
4. **Timeline spot-check:** after a session, pick any record from a
   stream file and find its copy (with role + stream) in that
   player's `players/<player>-<day>.jsonl`; run
   `excelprotect timelines rebuild <today>` on a quiet moment and
   confirm the day's files regenerate with the same content.
5. **Bedrock parity:** a Bedrock player's (.Name) trades and timeline
   file (sanitized key) record identically.
