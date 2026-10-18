# ExcelMagicGate 0.1.0 — install runbook (WRV-070)

## ⛔ GATE — READ FIRST

**Do not install until the WRV-070 Phase 0 player trial passes.**
The trial is the packets' own gate (Phase 1 packet INSTALL-RUNBOOK:
in-game two-client verdict — targeted heal lands, Bedrock icons
readable, wand flow works). ExcelMagicGate automates the *release* of
Magic; it does not change what Magic is. If the trial verdict is not a
pass, this plugin does not ship either.

Also required before install, per the standard WRV install rule: the
server must be **empty** (nobody online), and the install must stay
reversible (this runbook keeps backups at every step).

What installs, in the dormant shape:

- `Magic-11.2.5.jar` (SHA256 `79e49fe3…`, see SOURCE.md) — skipped if
  already installed by the Phase 1 packet runbook.
- `LuckPerms` — already live on WRV (no change).
- `ExcelGuildGate 0.1.0` — already live on WRV (no change).
- `ExcelMagicGate-0.1.0.jar` into `plugins/`.
- `plugins/Magic/` pre-seeded with the **stage 0** file set (below) so
  Magic's first boot loads the WRV packet shape, **not** its 557-spell
  defaults.

## Step 1 — pre-seed the dormant stage 0 config

The stage files ship inside the bridge jar. Extract them before the
first boot with the bridge present:

```
cd plugins
mkdir -p Magic ExcelMagicGate-tmp
cd ExcelMagicGate-tmp
jar xf ../ExcelMagicGate-0.1.0.jar stages/stage0
cp stages/stage0/config.yml stages/stage0/spells.yml stages/stage0/paths.yml \
   stages/stage0/wands.yml stages/stage0/classes.yml stages/stage0/messages.yml \
   ../Magic/
cd ..
rm -rf ExcelMagicGate-tmp
```

(Equivalently, copy the six files from
`~/workspace/excelmagicgate-0.1.0/src/ExcelMagicGate/stages/stage0/`.)

Stage 0 = the Phase 1 packet files with two differences: `wands.yml`
carries only the hidden spell-less base templates (the four rank wands
are removed), and there are no grants. A world in this state has Magic
loaded with the curated 30-spell universe on disk and **no way for any
player to obtain a working wand or a cast grant**.

If the Phase 1 packet runbook was already followed (Magic installed
with the full Phase 1 files), stage 0 differs only in `wands.yml`:
swap in the stage-0 `wands.yml` above. The bridge's own first apply
(stage 1, when Seal 1 falls) restores the packet `wands.yml` anyway;
the seed matters because until then the rank wands must not exist.

## Step 2 — install the jar, restart from the panel

1. Verify nobody is online.
2. Upload `ExcelMagicGate-0.1.0.jar` to `plugins/`.
3. Panel **Restart** (a full restart — this is a new plugin, not a
   reload candidate).

## Step 3 — verify the dormant state (console)

- Startup log shows `[ExcelMagicGate] Enabling ExcelMagicGate v0.1.0`
  and `applied stage 0 of 5, watching ExcelGuildGate seals`.
- **No SEVERE canary line** about `stormshackle`. (If it appears, the
  pre-seed in Step 1 did not land — Magic booted its defaults. Fix the
  seed and restart before any seal falls; the bridge would also repair
  the set automatically at the stage 1 apply, but do not rely on it.)
- Magic's boot lines: `Loaded 30 spells`, `Loaded 6 wands`,
  `Loaded 5 progression paths`.
- Console checks:

```
magicgate status
lp group default permission check magic.cast.heal
guildgate status
```

Expect: seals 0 of 5 (or whatever the campaign is at), applied stage
0, stage 1 `locked (needs 1 seals)`, heal check **undefined/not set**.

## Step 4 — seal night (automatic; manual fallback)

When a seal falls in game, the bridge's watch (60 s poll, plus a check
on every join) notices within about a minute and applies the stage on
its own: backup → swap → `magic reload` → grants → announce. Watch the
console for `Magic stage N applied successfully` and the broadcast.

Operator confirmations after any auto-apply (the bridge checks command
acceptance, not LuckPerms' per-node replies):

```
lp group default permission check magic.cast.heal
spells
```

- Stage 1: heal **true**; `spells` reports 30 total.
- Stage 2: also `magic.cast.blink` true (68 spells). Stage 3:
  `magic.cast.chainlightning` true (127). Stage 4: `magic.cast.walls`
  true for the `builder` group (142). Stage 5: `magic.cast.platform`
  true (186).
- Magic's reload completion shows as fresh `Loaded N spells` /
  `Loaded N wands` lines after the apply.

Manual fallback (bridge disabled, or seal night run by hand):
`/magicgate apply <stage>` in game or `magicgate apply <stage>` from
the panel console runs the exact same path. It refuses stages below
the applied one.

If an apply FAILS: the console shows SEVERE with the failed step,
`appliedStage` does not move, and the watch retries every poll. Fix
the named cause (missing plugin, missing stage file) and let it retry,
or run `/magicgate apply` by hand.

## Revoke / rollback

- `/guildgate revoke <seal>` after a stage applied: the bridge warns
  (console + online admins) and **changes nothing** — grants and
  configs stay. Rolling Magic back is a deliberate manual job: restore
  the six files from `plugins/ExcelMagicGate/backups/<timestamp>/`
  into `plugins/Magic/`, restart, and unset grants per the packet
  runbook for the lower stage if truly required. Do not edit the
  bridge's `state.yml` by hand except as part of that manual rollback
  (set `appliedStage` to the stage you restored).
- Uninstall: remove `ExcelMagicGate-0.1.0.jar`, restart. Magic stays at
  whatever stage was last applied (its config files are plain packet
  files by then — the packets' own runbooks describe managing them).

## Config notes

`plugins/ExcelMagicGate/config.yml`: `enabled` master switch (watch
inert when false; status and manual apply still work),
`poll-seconds` (default 60), `announce.sound` / volume / pitch, and
optional per-stage message overrides under `messages.stages.<n>`
(built-in roadmap defaults otherwise). Stage packet files live in
`plugins/ExcelMagicGate/stages/` after first enable (extracted from
the jar); edit them only to ship a corrected packet, then re-apply
that stage with `/magicgate apply <same stage>` (allowed as a repair).
