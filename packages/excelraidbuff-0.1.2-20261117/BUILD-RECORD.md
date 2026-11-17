# ExcelRaidBuff 0.1.2 — Build Record ("Raid Expiry")

Built Oct 23, 2026 (Iris/Argus), on Mike's order the same
morning (7:28 AM CDT): *"the raid is still going; set a limit
of after 6 hours it dies down and they have to start over
again."* A raid on the live server had run on and on; 0.1.2
caps every raid's lifetime at 6 hours by default, after which
the raid dies down and must be started over. NOT installed by
this build — the install rides the queue + failsafe window
(see `INSTALL-PACKET-0.1.2.md`).

## What shipped

One feature, three parts:

1. **`core/ExpiryTracker.java` (new, Bukkit-free)** — per-raid
   lifetime clocks. First sight starts a clock (repeated
   sightings return false and never move it — there is NO
   progress input on purpose); `isExpired` fires at age ≥ cap;
   snapshot/restore is the restart bridge (a restore never
   overwrites a clock already running).
2. **Adapter wiring (`ExcelRaidBuff`)** — the clock starts at
   the raid's trigger event, with the janitor sweep's
   first-sight rule as backstop for raids already in progress;
   the sweep prunes clocks of raids that ended without an
   event and expires the rest. Expiry = clear reveal glow →
   retire the raid state → remove members → drop the clock →
   one announce line → one console line.
3. **Persistence (`expiry-starts.yml`)** — the clocks live in
   the plugin's data folder, saved on every change and on
   disable, restored on enable. This is the load-bearing piece:
   the server restarts daily at 5 AM, and a clock that reset
   on restart would make a 6-hour cap meaningless.

Config gains an `expiry:` block (enabled / hours / line) —
see Design. Status gains a third line (`Raid expiry: ON, cap
6h, raids tracked N[, oldest age …]`); the awake line gains
`; expiry ON cap 6h` (or `OFF`).

## Design

- **The cap is a hard lifetime limit.** Unlike the reveal's
  quiet clock, fighting does not reset it; the tracker takes
  no progress input at all, so nothing can. (Adapter-level
  corollary proven by the unit pins: a raid seen a hundred
  times mid-fight still dies at its first-sight + cap.)
- **Wall-clock, downtime included.** The stored value is the
  start epoch; age is `now − start`. Server downtime counts —
  stated in the config comments and the packet, so nobody is
  surprised that a raid aged through the 2–5 AM window.
- **Death, but not a kill.** Members are removed with
  `Entity.remove()`, never damaged: no drops, no XP orbs, no
  EliteMobs coins (coins ride death events), no GuildGate
  credit path, no Hero of the Village. The raid is neither
  VICTORY nor LOSS — `RaidFinishEvent` never fires for it.
- **The raid state retires through the game's own stop** —
  `((CraftRaid) raid).getHandle().stop()`, the exact mechanism
  the 0.1.1 rig's `rbforceend` proved (boot k: hero got Hero
  of the Village for wave 1's kills, never victory, boss bar
  gone). The Bukkit API has no raid stop, and the plugin
  compiles against the Paper API only, so the adapter reaches
  it reflectively (the EliteMobs seam discipline). Order
  matters: stop BEFORE removing members, so vanilla cannot
  answer an emptied wave by spawning the next one. If the
  stop ever fails, members are still removed, a warning logs,
  and the expiry line carries a `[WARNING: …]` suffix — the
  failure is loud, never silent. (It never failed in the rig.)
- **Announce:** the expiry line goes to players in the raid's
  world within 128 blocks of its center — the reveal's radius
  convention — via the reveal's sender, refactored into a
  shared `sendNearbyLine`. Always sent (there is no separate
  announce toggle; the reveal's toggle governs the reveal).
- **The log line** (for the nightly combat analysis):
  `RaidBuff expiry: raid <key> died down at the <cap> cap
  (age <age>) - N member(s) removed, no loot, line sent to M
  nearby player(s)`.
- **First-sight is the honest bound.** A raid already in
  progress when the plugin enables — or sitting in unloaded
  chunks (the sweep reads the world's raid list, which
  includes those) — starts its clock at first sight, not at
  its true start. Documented in the config comments, the
  SOURCE.md and the packet. Consequence observed in the rig:
  two pre-existing world raids (leftovers of earlier boots)
  expired at their first-sight caps with 0 members in loaded
  chunks — the feature working as documented, and incidentally
  the correct live behavior for a raid stuck in an unloaded
  chunk: it dies down at its cap instead of living forever.
- **Reload semantics mirror the reveal:** turning the feature
  (or the plugin) off in a reload retires every clock,
  persisted file included; turning it back on starts clocks
  fresh at first sight. A graceful disable persists the
  clocks (that's the restart bridge); a disabling *config*
  clears them.

## Delta discipline

- **Base proof first:** the untouched 0.1.1 tree (snapshot:
  `build-0.1.2/base-0.1.1-tree-verified/`) rebuilt
  byte-identical to the shipped 0.1.1 jar before any 0.1.2
  edit — SHA-256 `ab330c27…`, 36,597 B (see
  `build-0.1.2/BASE-VERIFICATION.txt`).
- **Change scope, by class-byte diff vs the 0.1.1 jar:**
  `ExcelRaidBuff` + its strip runnable, `RaidBuffConfig`,
  NEW `ExpiryTracker`, `config.yml`, `plugin.yml`. **Every
  other class is byte-identical — `ExcelRaidBuffAPI`
  included — so the TrainingWheels compensation seam and
  every damage path are untouched** (the 0.1.1 invariant,
  kept). GuildGate: no interaction at all.
- **Bundled config = live bytes + the new block only:** the
  tree config's first 5,029 bytes are byte-identical to the
  installed live config (tuned level-bonus columns III 5 /
  IV 10 / V 15 + the reveal block); its first 4,180 bytes are
  the elite-tuning record file itself; the `expiry:` block is
  appended after (tree config now 6,138 B).
- **plugin.yml trap avoided:** the 0.1.1 jar died on a `": "`
  in its description; the 0.1.2 description was checked to
  contain no `": "` sequence, and every rig boot parsed it
  clean.

## Tests — 316/316 green

Carried from 0.1.1 (275, all green): ScalingTable 61,
RaidBuffConfig 69→83, BuffMath 18, MembershipGate 13,
AnnounceRule 13, EliteComposition 35, RylieCompensation 42,
RevealTracker 24. New:

- **ExpiryTrackerTest (27):** first sight starts the clock
  exactly once; no fire before the cap, fire exactly AT the
  cap; a hundred mid-life sightings never move the start (the
  no-progress-reset pin); a raid ended before the cap can
  never expire after the fact; snapshot → restore into a
  fresh tracker keeps the start AND the deadline (the restart
  pin); restore never overwrites a live clock; the snapshot
  is a defensive copy; two raids expire independently;
  retainOnly pruning reports exactly the vanished; reset;
  oldest-start.
- **RaidBuffConfigTest expiry pins (+14):** defaults on /
  6.0 h / cap 21,600,000 ms / default line; a no-block config
  keeps the defaults; overrides off / 2.5 h → 9,000,000 ms /
  custom line; hours 0 floored to 0.01 (36,000 ms — rig-sized
  values stay legal); hours 10,000 capped at 168; garbage
  `"six"` → 6.0; blank line → default.
- Expiry-disabled = no-op is pinned at the config layer here
  and proven end-to-end in rig boot p below. "Members
  removed, no drops, line once, glow cleared" is adapter
  behavior — proven in rig boot m.

## Builds

- A/B in-tree builds byte-identical; clean-room rebuild from
  the deposited source zip byte-identical (jar + full test
  suite re-run green in the clean room).
- Final jar: `ExcelRaidBuff-0.1.2.jar`, 42,438 B, SHA-256
  `009284dfcc91acbdcb237562d45fb79666f1c5c12cca15bc11507814d3e3149d`,
  MD5 `bbc6e7e71ee04131034f7d05c35283ad`.
- Source zip: 56,552 B, SHA-256
  `ca8b8333f0e03361523d7d830cc19f35a17d7e212fa40a9741ce7dd324214c36`,
  MD5 `8cc7a36f109cfaf53a3a64b731f1d3f2` (37 entries = the
  0.1.1 zip's set + `ExpiryTracker.java` +
  `ExpiryTrackerTest.java`).

## Rig proof (scratch rig, Paper 26.2-123, EM 10.9.10,
## GuildGate 0.3.3, BossTrial 0.2.0; logs in scratch/rig/logs/)

Rig config = the bundled 0.1.2 config with rig-only overrides
(`reveal.quiet-seconds: 30`, `expiry.hours` per boot). The
driver gained one scratch-only probe (`rbhero`: the rig
hero's Hero-of-the-Village state + per-world ongoing-raid
counts). Zero SEVERE in every boot; no ExcelRaidBuff error
lines (the only ERROR lines are the sandbox proxy's SSL noise
against Mojang/version endpoints, present in every rig boot).

- **Boot m — expiry fires (cap 0.02 h = 72 s).** Awake line:
  `… reveal ON quiet 30s; expiry ON cap 0.02h)`. Before any
  staging, the sweep's first-sight rule started clocks on two
  pre-existing raids in the world raid list (leftovers from
  earlier boots, chunks unloaded) and both died down at
  12:41:05, age exactly 1m 12s, 0 members loaded, line to 0
  players — the documented first-sight behavior. The staged
  raid at (400,400): reveal fired at +30 s quiet (2 members
  glowing, line to 1), then at 12:52:47 **expiry: age exactly
  1m 12s, 2 members removed, line sent to 1 nearby player**.
  After: census 0 raiders anywhere; glow census members 0 /
  allGlowing 0 (glow cleared with them, no strays); drops
  census CROSSBOW 0 / EMERALD 0 / IRON_AXE 0; `rbhero`:
  HERO_OF_THE_VILLAGE absent, 0 ongoing raids; status `Raid
  expiry: ON, cap 0.02h, raids tracked 0`.
- **Boot n — control win (cap 0.5 h).** The rig hero won the
  (400,400) raid honestly via `raidfight` (7 waves) in ~3
  minutes. **HERO_OF_THE_VILLAGE = PRESENT** (victory paid
  normally), zero `RaidBuff expiry` lines in the whole boot,
  tracker back to 0 after the win (the finish dropped the
  clock). Untouched, as ordered.
- **Boot o — restart persistence (cap 0.04 h = 144 s).** Boot
  o1: raid triggered 13:01:01; `expiry-starts.yml` on disk
  held `starts: { '126328778': 1792760461715 }` while the raid
  was live; graceful console `stop` at 13:01:14 (server down
  ~80 s of the raid's life). Boot o2: **`RaidBuff expiry:
  restored 1 raid clock(s) from disk`** on enable; expiry
  fired at **13:03:26 — the original start + 144 s (age
  printed 2m 24s exactly), downtime counted**, 7 members
  removed, line to 1 nearby player. A reset clock would have
  fired at ~13:04:47; it did not.
- **Boot p — disabled no-op (cap 0.02 h, `enabled: false`).**
  Awake line `expiry OFF`; the staged raid was still ONGOING
  at ~110 s old — well past the would-be 72 s cap — with zero
  "died down" lines and status `Raid expiry: OFF, cap 0.02h,
  raids tracked 0`.

Two rig notes for the next builder: (1) a fake player's potion
effects do not tick unless the driver's `playertick` scaffold
is on — without it, Bad Omen never converts and no raid ever
starts (this cost boot m its first staging attempt at the old
(1400,1400) site; `raidprep`'s clean (400,400) site plus
`playertick` is the reliable path). (2) The (1400,1400) site
is contaminated — leftover raids and a pillager patrol from
earlier boots were still shooting natural-village villagers
in boot m; prefer (400,400).

## Deposits (Drive, folder `Minecraft WRV`
## `1BASUtSwJOFl7j608gfTfyxfbloUg8J5-`, MD5 read-back verified)

- Jar → `Build files` (`1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI`):
  `ExcelRaidBuff-0.1.2.jar` = `1cU_JH2oESmzHAKRu3dryHG7ls45Q-CqS`
  (MD5 read-back `bbc6e7e7…` = local).
- Source zip → `Source` (`1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz`):
  `ExcelRaidBuff-0.1.2-source.zip` =
  `1kN91lQfA0E3dYugdbAiVBC_iLXtXX5r1` (MD5 read-back
  `8cc7a36f…` = local).
- SOURCE.md → `Source`: `ExcelRaidBuff-0.1.2-SOURCE.md` =
  `1wQAkNJAsynM2TguTcqCRmhl2fTpzDYB1` (MD5 read-back
  `448b559a…` = local).
- Install packet → `Source`: `INSTALL-PACKET-0.1.2.md` =
  `1bx0K59q2DzPn5YUuFdj8l8KWk3h5B6hO` (MD5 read-back
  `71054e2e…` = local).

## Player proof owed (after install)

One: a real raid crossing 6 hours dies down — the line is
seen, the raiders leave, no Hero of the Village. Event-driven
(a raid has to actually reach 6 hours); nobody is asked to
stage one. The shipped default itself (6 h) is only
rig-proven at scaled-down caps (72 s / 144 s) plus unit pins
at the exact 6 h cap value — stated plainly rather than
dressed up.
