# INSTALL PACKET — ExcelPartyDrops 0.1.0 (NEW plugin)

**What:** Fresh plugin install. No predecessor exists. No config edits
anywhere else, no permissions to grant, no other plugin changes.
**Artifact:** `~/workspace/excelpartydrops/ExcelPartyDrops-0.1.0.jar`
— 14,731 B, MD5 `590d97fe78e7b03d74bcfcd5fb8659b8`,
SHA-256 `afae735b991d683cf45f4aa30187f51aaf53bf2b7b114761d4279979beba6196`.
Also deposited on Drive (Build files) — file ID in BUILD.md / manifest entry.

## Precondition — STOP check

- If ANY `plugins/ExcelPartyDrops*.jar` is already present on the server,
  STOP and report. There is no predecessor; a jar already there means
  something is off.

## Steps

1. Upload `ExcelPartyDrops-0.1.0.jar` into `plugins/` (Shockbyte File
   Manager; upload menu left open, element ref without '@').
2. Verify the uploaded file's size reads **14,731 B** in the File Manager.
3. Panel **Shut Down**, then **Start** (a restart applies it; console
   `stop` auto-restarts on Shockbyte — use the panel buttons).
4. Post-boot verify — ALL must hold:
   - Enable line present:
     `ExcelPartyDrops enabled: ON, radius 50.0 blocks, step 0.5, multiplier cap 3.0, participant cap 5, EliteMobs hooked`
     (the tail must read "EliteMobs hooked" — if it reads "EliteMobs NOT
     found - inert", EliteMobs failed to load; report it.)
   - **Zero SEVERE** in the boot log.
   - `plugins/ExcelPartyDrops/config.yml` exists with shipped defaults:
     enabled true, party-radius 50.0, step-per-extra 0.5, multiplier-cap
     3.0, participant-cap 5, respect-farming-cap **false**,
     hint-cooldown-seconds 600, debug **false**.
   - Console `partydrops status` prints the ON line with
     "EliteMobs hooked".
   - Console `guildgate status` still reads **LOCKED 0 of 5**.
5. Write the marker: `~/workspace/excelpartydrops/INSTALLED.txt`
   (date/time + verified jar SHA-256).

## Behavior at runtime (what the executor may see)

- One console audit line per multiplied kill with a non-empty drop list:
  `multiplied elite drops: type=… level=… participants=… multiplier=… items A -> B`
- Players in range of a multiplied kill may see one quiet action-bar line
  ("Hunting together pays off — the group earned extra drops.") — first
  time, then at most once per 10 minutes per player. By design (§7).
- Solo kills, spawner/egg/raid/command-origin elites, and anti-exploit
  flagged elites pay exactly vanilla. Nothing else changes: EM gear,
  coin showers, dropped XP, Jobs, mcMMO all untouched.

## Rollback

Delete `plugins/ExcelPartyDrops-0.1.0.jar` and the
`plugins/ExcelPartyDrops/` data folder, then restart (panel Shut Down →
Start). Fully reversible; the plugin keeps no state outside its own
folder and writes nothing anywhere else.

## Owed at install (same pass — MAIN AGENT's job, publish nothing early)

Website + Guide player-facing note, in player words (draft):

> **Excel Party Drops:** Hunting together pays off. When a tough elite
> monster falls, everyone in the fight nearby shares a fuller pile of
> drops — the more of the family in range when it falls (within about
> 50 blocks), the more drops, up to three times as many. Solo hunts pay
> exactly as before.

Then Guide restamp + retrieval verification, per the standing knowledge
gate. Player proof owed after install (one test at a time): one real
group hunt (2+ players) on an elite — audit line in console + extra
drops on the ground + the one-time hint seen by a participant.
