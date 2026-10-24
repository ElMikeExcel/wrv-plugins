# Movement spells sealed near a live Ender Dragon (arena lock): ExcelAttunement 0.1.6 (STAGED ONLY)

Staged Sat Oct 24, 2026, about 10:20 AM CDT, after Mike changed the design at 9:27 AM CDT. The rule is no longer "the whole End until the dragon seal falls". It is now "only near the dragon while she is alive".
**Not installed and not pushed. Nothing live was touched.** EliteMobs (MagmaGuy) and ExcelBossTrial were not touched. BossTrial source was only read.

Version: stays at **0.1.6**. No earlier 0.1.6 jar was ever installed or pushed. The seal-lock builds (`72b37bc2…` Glide-only, `135f…` Glide+Fling) and an intermediate arena build (`b884126c…`) were only staged and are **void**. The old seal-lock deliverable is in `superseded-seal-lock-135f/`, for reference only. The only valid 0.1.6 jar is the one in SHA256SUMS.txt:
`441d3f707283c28f293dbce5802e94236c2b856efbe24a8e9d36c6c053e3cc2b`

## What it does
- **Zone:** a vertical cylinder (all heights, void included) of **radius 200 around 0,0** in every End-environment world (`world_the_end` and any custom End world).
- **Active:** while that world has a live Ender Dragon. Outside the zone, in other worlds, or when no dragon is alive, every spell works normally.
- **Locked by default** (Magic (elmakers) spells, any level):

| Spell | Message (chat + action bar) | Extra |
|---|---|---|
| glide | "Glide is sealed near the dragon while she lives" | A running Glide is **stopped** when the player is inside the active zone. That covers flying in, and a dragon re-summoned while the player is already inside. Checked every 5 ticks. |
| fling (all levels, incl. the `leap` alias = `fling\|4`) | "Fling is sealed near the dragon while she lives" | |
| hulkleap | "Hulkleap is sealed near the dragon while she lives" | |
| lift | "Lift is sealed near the dragon while she lives" | Also blocked when it **targets a player inside the active zone**, even if the caster is outside. |
| blink | "Blink is sealed near the dragon while she lives" | |

- **Open by default:** backflip, recall, gather, dash, evade. Each one is a commented one-line add in the config, e.g. `backflip: {}`. That gives the default message "<Spell> is sealed near the dragon while she lives". The rig proved this with backflip.
- **Blocked casts spend no mana and start no cooldown.** They are cancelled at PreCastEvent (LOW). In Magic 11.2.5, costs and the cooldown are applied later, in `finalizeCast`. The lift-target check runs at PreCast too: it resolves the target through Magic's own targeting before the cast. The rig measured both (mana unchanged, the next cast works right away).
- **Messages:** at most one line per player **per spell** every 3 s.
- **Fail-safe:** if the dragon state can't be read, the zone counts as active.
- **Kept from before:**
  - Bypass permission `excelattunement.seallock.bypass` (default false).
  - `enabled` toggle.
  - If the `arena-locks` section is missing, the same default rules and zone are used.
- Elytra flight is not affected. The ward from 0.1.5 (Magic off in the EliteMobs worlds) is unchanged.

### Why radius 200
- The vanilla main island reaches about 100 blocks from 0,0.
- The obsidian pillars stand in a ring of radius 42.
- The arrival platform is at **100,49,0**, so it is **inside**. The rig cast there and was blocked.
- Vanilla adds players to the dragon fight (boss bar) within **192 blocks of 0,128,0**, so 200 covers everyone the game itself counts as in the fight, plus a void margin.
- The outer islands start 1000+ blocks out, so they are never affected.
- The end-gateway ring (r≈96) is inside the zone. Gateways only appear after a kill, when the zone is open. During a re-fight, though, a player can't Glide or Blink to them.
- The radius is configurable (`zone.radius`). A bad value (negative or NaN) falls back to 200.

### How "dragon alive" is decided (per world)
The zone is active if **any** of these is true:
1. a live (not dead) EnderDragon entity is in the world, including one in her death flight or one placed with /summon;
2. the battle's linked dragon is alive;
3. a respawn ritual is running (respawn phase ≠ NONE), which covers the crystal gap before the new dragon spawns;
4. the dragon boss bar is visible;
5. the world's fight has never been won (`!hasBeenPreviouslyKilled`).

It turns off only when all of them are false, i.e. after the death animation finishes.

### ExcelBossTrial findings (read-only)
- When phase-1 damage would be lethal, BossTrial 0.2.6 **cancels it and heals the same entity** to the phase-2 bar. She is invulnerable during the flip. No new dragon is spawned and there is no gap where she is missing. The state log on the rig shows ALIVE without a break from the first hit, through the flip and the death animation, until NONE.
- A BossTrial re-summon goes through the vanilla respawn ritual. Signal 3 keeps the zone locked through it, and the rig showed no unlock during the ritual.
- BossTrial ends its trial, with a WARN "Trial battle … ended during PHASE1", if every player goes beyond its ~192-block range, and re-registers when they return. That is BossTrial's own behaviour. The lock doesn't depend on it.

### Seal fallback: dropped (recommendation)
The arena lock now follows the dragon directly. A seal fallback would lock the whole End again before the first kill, which is exactly what Mike moved away from. It would also bring back the ExcelGuildGate dependency. Signal 5 already covers a never-fought End, and the fail-safe covers an unreadable state. **0.1.6 no longer needs ExcelGuildGate.** softdepend is now `[Magic, EliteMobs]`.

## Files
| File | Purpose |
|---|---|
| ExcelAttunement-0.1.6.jar | the plugin (sha in SHA256SUMS.txt) |
| config-merge-arena-locks.yml | the `arena-locks` section, to append to the live config |
| config-0.1.6-default.yml | full default config, for reference only. **Do not copy it over the live config.** |
| ExcelAttunement-0.1.5-to-0.1.6.patch | full source diff from the 0.1.5 baseline (`a7e2610`). Applying it to that tree gives exactly the source zip. |
| source/ExcelAttunement-0.1.6-src.zip | full source, tests, build.sh |
| evidence/ | unit tests (656/656, both from the repo and rebuilt from the zip), rig logs for the 4 modes plus an earlier full main run, rig boot logs, test-only harness (`t_arena.js`, `lib.js`, `run-all-modes.sh`) and helper plugin source (`RigWorlds-test-only.java`). IPs in the logs and in lib.js are replaced with `<ip>` / `RIG_HOST`, which also blanked some dotted version numbers. Logs contain only rig bot names. |

**Reproducible:** two builds, plus a rebuild from the source zip in a clean dir, all give `441d3f70…`. The rig ran this exact jar.

## Preconditions
1. Live ExcelAttunement should be **0.1.5** (`plugins/ExcelAttunement-0.1.5.jar`, sha `69d4afc53a0b7687e65a699266833d41961cf44ec199cf40e57a5c2d1c57d6b8`, from SHIP-20261024-noab). If that batch has **not** shipped yet, ship 0.1.6 **in place of** 0.1.5 there. It contains all of 0.1.5. Apply that batch's config step plus step B3 below.
2. ExcelGuildGate is **not** needed by this version.
3. Window: **2–5 AM CDT, 0 players online**, server stopped for the jar swap.

## Steps
A. Stop the server.

B. Back up first. Copy, don't move. **Never overwrite an existing .bak**: if the name is taken, use `-2`, `-3`, and so on.
   1. Copy `plugins/ExcelAttunement/config.yml` to `plugins/ExcelAttunement/config.yml.bak-<YYYYMMDD>-arena`.
   2. Rename `plugins/ExcelAttunement-0.1.5.jar` to `plugins/ExcelAttunement-0.1.5.jar.bak-<YYYYMMDD>-arena`. Afterwards there must be exactly one ExcelAttunement jar.
   3. **Merge, don't overwrite.** Append `config-merge-arena-locks.yml` to the end of the live `plugins/ExcelAttunement/config.yml` without changing any other key.
      - If a `seal-locks:` section is somehow present from a test, remove it. 0.1.6 ignores it.
      - If this step is missed, the code uses the same default rules and zone. The rig proved this.

C. Upload `ExcelAttunement-0.1.6.jar` to `plugins/` and check its sha256 against SHA256SUMS.txt.

D. Start the server and check:
   - `[ExcelAttunement] Enabling ExcelAttunement v0.1.6`
   - `Guild ward (Magic off in the EliteMobs worlds): ON.` (unchanged)
   - `Arena locks: ON (sealed near a live dragon: glide (stopped on entry), fling, hulkleap, lift (+ targets in zone), blink; zone radius 200 around 0,0 in THE_END; unknown dragon state = locked).`
     A `[default rules]` tag means the section wasn't merged. The rules still work, but redo step B3.
   - 0 SEVERE lines for ExcelAttunement.
   - `/attunement status` ends with `Arena locks: ON, … world_the_end: dragon NONE (zone open)` if the live dragon is dead, or `dragon ALIVE (zone locked)` if she is alive or has never been fought.

## Rollback
1. Stop the server.
2. Rename `ExcelAttunement-0.1.6.jar` to `ExcelAttunement-0.1.6.jar.off`.
3. Rename `ExcelAttunement-0.1.5.jar.bak-<date>-arena` back to `ExcelAttunement-0.1.5.jar`.
4. Optionally copy `config.yml.bak-<date>-arena` back. 0.1.5 ignores `arena-locks`.
5. Start the server.

Neither version changes world, dragon or seal state.

## Checks (two). Both passed.
**Check 1: tests plus rig** (final jar `441d3f70…`):
- **Unit tests: 656/656 PASS.** The same result when rebuilt from the source zip.
- Compiles against paper-api 26.3.build.214 with `-Xlint:all`. The only warnings are 2 pre-existing `getMage` deprecations.
- **Rig setup:**
  - Paper 26.3-214, Magic 11.2.5, ExcelBossTrial 0.2.6, LuckPerms, Via. No EliteMobs.
  - Offline mineflayer bots.
  - A **fresh vanilla `world_the_end` with a real Ender Dragon** fight, plus a custom End world `rig_end`.
  - Console casts set to pay costs (`cast_console_cost_free: false`, rig only), and bots on the student path, so mana is measured.
  - Each mode starts from a fresh End.
- **Main: 59/59 PASS** (`evidence/arena-final-main.log`):
  - At the platform 100,49,0 with the dragon alive, Glide is blocked, with the exact message in chat and as the last action-bar line.
  - Message limits: 3 s rate limit, a separate limit per spell, and the line shows again after 3 s.
  - fling, fling|2, `leap` (fling|4), hulkleap, lift and blink are all blocked in the zone. backflip, dash, evade, gather and recall stay open.
  - Heal is unaffected.
  - Outside the zone (r=300), with the dragon alive, Lift works and pays mana. Blocked lift/glide cost **0 mana**, and Glide works right after a blocked try (no cooldown).
  - **Lift from outside (205,0) onto a player inside (195,0) is blocked.** The target gets no levitation and no mana is spent. The control case, both players outside, works.
  - In the overworld at 0,0, with the End dragon alive, glide, blink, hulkleap and fling all work.
  - A Glide cast outside the zone, then flown into it, is stopped.
  - The bypass permission works.
  - Real fight:
    - The BossTrial phase flip happens on the same single dragon, and the lock holds through the flip, in phase 2 and during the death animation.
    - After the kill, glide, fling, hulkleap, blink, and Lift into the zone all work at the platform.
  - A `/summon`ed extra dragon locks the zone, and removing it opens it again.
  - **Respawn:**
    - A Glide running inside the open zone is stopped when the ritual starts.
    - The zone stays locked through the whole crystal gap, and every cast during the ritual is blocked.
    - The re-summoned dragon keeps it locked.
  - The custom End world `rig_end` is locked at 50,0 on its first fight and open at 400,0.
  - State log: no break in ALIVE from the first hit to NONE, never NONE after the ritual starts, never UNKNOWN.
- **`enabled: false`: 3/3 PASS.** Glide and blink work in the zone with the dragon alive, and the status reads OFF.
- **No `arena-locks` section: 6/6 PASS.** The default rules and zone apply, with the `[default rules]` tag. glide, blink and lift are blocked with their messages, backflip is open, and overworld Glide works.
- **One-line add (`backflip: {}`): 2/2 PASS.** backflip is blocked with "Backflip is sealed near the dragon while she lives". dash, still commented out, stays open.
- 0 SEVERE lines in all 4 boots.

**Check 2: separate line-by-line review.** Covered `ArenaLock`, `ArenaLocks`, the `MagicBridge` changes (PreCast LOW handler, target probe, in-flight backstop, canTarget, running-spell stop, re-registration on Magic PreLoad), the main-class diff, plugin.yml, config.yml, build/test diffs, and the merge file against the code defaults. It found 4 small things, all fixed before the final build:
- a leftover `ExcelGuildGate` softdepend, now removed;
- a test expecting it, now updated;
- two inaccurate comments.

It also confirmed:
- Magic fires StartCastEvent after `onCast`, so the in-flight target backstop covers Magic's own target selection.
- The merge file's ids, spells, messages and flags match `ArenaLocks` defaults.
- The commented one-line adds are indented correctly.
- The handler skips already-cancelled casts and non-player mages.

No open issues.

## Not verified
- The live server (no live access; staging only), real Java clients with physics, and Bedrock (Geyser). It's the same PreCastEvent path, so the expected behaviour is identical.
- Re-registration after `/magic reload` was not exercised on the rig (only reviewed in code).
- **Backstop path:**
  - If a Lift target only becomes "in the zone" between PreCast and Magic's targeting, the in-flight canTarget backstop still stops the effect, but mana may be spent in that rare case.
  - This path is only reviewed in code. The rig's lift-target case was caught at PreCast, with no mana spent.
- The UNKNOWN (unreadable dragon state) path is only unit-tested. The rig never produced UNKNOWN.
- Glide-stop timing used rig bots, whose glides end in about 2 s. With real clients the stop arrives within `check-ticks` (5 ticks), or immediately on zone entry.
- Elytra is not blocked (by design). During a re-fight, players can't Glide or Blink to the gateways (r≈96, inside the zone).
- The live `world_the_end` dragon state (dead or alive) was not checked. If she has never been killed on live, the zone is locked until the first kill, which is intended.
