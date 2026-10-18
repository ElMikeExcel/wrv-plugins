# Magic dormant install — runbook (WRV-070)

**Decision:** Mike, Sat Oct 17, 2026, 7:17 PM CDT — "Skip the trial,
stage Magic dormant." The Phase 0 player trial is OFF by his call. This
supersedes the ⛔ GATE at the top of the ExcelMagicGate runbook
(`~/workspace/excelmagicgate-0.1.0/INSTALL-RUNBOOK.md`), which remains
the source of truth for the install *shape* and all seal-night
behavior; this runbook is the live-execution wrapper around it.

**What "dormant" means:** Magic and the ExcelMagicGate bridge are
installed and running, but the world sits at **Stage 0**: the curated
30-spell Phase 1 universe exists on disk, the stage-0 grant list is
intentionally empty, and the stage-0 `wands.yml` carries only hidden
spell-less base templates. **No player can cast anything or obtain a
working wand.** When Seal 1 falls, the bridge applies Stage 1 on its
own (backup → config swap → `magic reload` → grants → announcement),
and so on through Stage 5. The family effectively becomes the test as
each stage opens.

**Still owed, does NOT block this install:** the first-wand
acquisition design pick (how a player earns their first real wand when
Seal 1 falls). It must be settled before the first seal falls, not
before staging.

## Contents of this packet (`staging/`)

| File | SHA-256 |
|---|---|
| `Magic-11.2.5.jar` (7,641,369 B, NathanWolf) | `79e49fe32fdc4e25375c0099d2bd10665b9792bfd9b35fab8d64a498ebfc3532` |
| `ExcelMagicGate-0.1.0.jar` (149,848 B, ours) | `2bc272218d11d8230d54fbf909bd2587e294d6052ba300206e92fd95d237b0ef` |
| `stage0-seed/` — the six pre-seed .yml files, extracted from the staged bridge jar | hashes in `staging/SHA256SUMS.txt` |

Verify both jar hashes before uploading (`sha256sum -c SHA256SUMS.txt`
in `staging/`). The Magic jar is the verified WRV-070 deposit copy
(official CurseForge CDN artifact, byte-identical to the Drive
"WRV-070 Magic" deposit). Nothing else gets installed: LuckPerms and
ExcelGuildGate are already live and are not touched.

## Preconditions

1. **Watch chain (this install is LAST in the restart queue):** the
   optimization packet has landed, the GuildGate update has landed,
   and the ExcelBedrockMenu 1.5.8 install watch has finished — trigger
   is `~/workspace/excelbedrockmenu-1.5.8/INSTALLED.txt` existing, OR
   that watch's stand-down (Sun Oct 18, 2026, 1:00 PM CDT) having
   passed, whichever comes first.
2. **Server empty, twice-confirmed:** panel console `list` reads
   0 of 10, wait ~2 minutes, `list` again still 0 of 10. One empty
   reading is never enough. If Kailey (.coolgirlkail) is online, hold
   and tell Mike (standing rule).
3. This runbook's own stand-down: **1:00 AM CDT, Oct 20, 2026.** If it
   hasn't landed by then, the watch stands down and reports instead
   of installing.
4. Rehearsal: this exact runbook was performed start-to-finish on the
   local Paper 26.2 scratch stack Oct 17 (see `rehearsal/EVIDENCE.md`)
   and every probe below matched.

## Rollback story (read before installing)

This install **modifies no existing live file**. It adds exactly:

- `plugins/Magic-11.2.5.jar`
- `plugins/ExcelMagicGate-0.1.0.jar`
- `plugins/Magic/` (new folder: six seeded files + what Magic
  generates on first boot)
- `plugins/ExcelMagicGate/` (new folder: bridge config, `state.yml`,
  extracted stage packets)

GuildGate's `state.yml` and all LuckPerms data stay untouched until a
seal actually falls — at Stage 0 the bridge writes only its own
`state.yml`, and the stage-0 grant list runs zero commands.

**Rollback = deletion.** Panel Shut Down → delete the two jars and the
two folders → Start. The server returns to exactly today's state; no
restore-from-backup is needed because nothing was overwritten. (If a
seal has fallen and a stage has applied by the time anyone wants out,
rollback is the bridge runbook's manual procedure instead — restoring
packet files from `plugins/ExcelMagicGate/backups/<timestamp>/` and
unsetting grants. At Stage 0, plain deletion is always the whole
story.)

## Install steps (panel only)

Panel: Shockbyte panel (server link withheld)

1. Confirm the preconditions above (chain trigger + twice-empty).
2. Upload `staging/Magic-11.2.5.jar` and
   `staging/ExcelMagicGate-0.1.0.jar` into `plugins/`.
3. **Pre-seed the dormant config** (bridge runbook Step 1 — do not
   skip; without it Magic's first boot generates its 557-spell
   defaults instead of the packet):
   - Create the folder `plugins/Magic/`.
   - Upload the six files from `staging/stage0-seed/` into it:
     `config.yml`, `spells.yml`, `paths.yml`, `wands.yml`,
     `classes.yml`, `messages.yml`.
   - (The seed already carries the packet's Geyser fix,
     `url_icons_enabled: false`, and all four `load_default_*`
     switches set to false.)
4. **Restart via the panel Shut Down button ONLY** — never console
   `stop` (it auto-restarts and is not a clean restart). Wait for the
   server to fully stop, then Start it.
   *(Wording note: the bridge runbook's Step 2 says "Panel Restart";
   the house rule — Shut Down, confirm stopped, then Start —
   supersedes it. Same effect, cleaner state.)*
5. Run the probe list below from the panel console. If any probe
   fails, roll back per the story above and report; do not improvise
   a fix on live.

## Post-install probes (expected results — all rehearsal-verified)

Boot log, in order (spells/paths lines may land a few seconds after
the wand line):

```
[ExcelMagicGate] ExcelMagicGate enabled: applied stage 0 of 5, watching ExcelGuildGate seals every 60s
[Magic] Loaded 6 wands
[Magic] Loaded 30 spells
[Magic] Loaded 5 progression paths
```

- **No SEVERE canary about `stormshackle`.** If it appears, the
  pre-seed did not land and Magic booted its defaults — roll back and
  report. (Zero SEVERE from Magic/ExcelMagicGate in the rehearsal.)
- Console probes:

```
magicgate status
```
→ `Seals fallen: 0 of 5`, `Applied Magic stage: 0 of 5`,
`Stage 1 The Healer - locked (needs 1 seals)` (stages 2–5 likewise
locked). If the campaign has legitimately advanced before this
install lands, the seal counts will show it — the bridge applies up
to the highest eligible stage on its first poll, which is correct
behavior, and the report must say so.

```
guildgate status
```
→ unchanged: `Guild: LOCKED | Progress: 0 of 5` (or the campaign's
real state — the point is this install did not move it).

```
lp group default permission check magic.cast.heal
lp group default permission check magic.cast.cure
```
→ both `Result: undefined` ("default does not have … set"). This is
the dormancy proof: the spells exist on disk, nobody holds a grant.

```
list
version
```
→ `list` answers, server healthy, players can join normally.

## Close-out (install watch does this)

1. Write `~/workspace/magic-dormant-install/INSTALLED.txt` with the
   timestamp and the probe results.
2. Append a line to the day's `~/memory/YYYY-MM-DD.md` log.
3. Report to the main chat: installed time, probe outcomes, player
   count after.
4. Publishing follows in the next records pass (wrv-plugins record +
   website Magic entries at the installed-dormant status) — the
   install is not "complete" until that lands, per the standing
   completion gate.

## After install

Seal nights are automatic — see the bridge runbook's Step 4 for the
per-stage operator confirmations (heal true at Stage 1, blink at 2,
chainlightning at 3, walls for the builder group at 4, platform at 5).
ExcelGuildGate's join catch-up and the narrator (GuildGate updates)
tell returning players what opened; nothing about this install
changes what players see until the first seal falls.
