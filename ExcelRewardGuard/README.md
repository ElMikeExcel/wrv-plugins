# ExcelRewardGuard 1.2

Farm-reward guard for the Wink Reyes Valley family server (Paper 26.2, Java 25).

**Status: Built, automated tests passed, local test-server passed. NOT installed live.** Installation needs Mike's approval and an empty-server restart window, like every WRV plugin. Install packet: `~/workspace/excelrewardguard/install-prep-1.2/`.

Authorship: the provenance candidate was written by Moon (Oct 14, 2026 handoff package, marked DO NOT INSTALL because it classified mobs but blocked nothing). The enforcement completion, tests and this README are by Argus (Oct 15, 2026), finishing that candidate to the design Mike agreed on Oct 13. Version 1.1 (Oct 15, 2026) changes the policy from "farmed kills earn nothing" to "farmed kills earn a reduced share", per Mike's decision the same day. Version 1.2 (Oct 16, 2026) adds the sky-farm signature below, per Mike's decision that night.

## What it does

Mobs born from a **spawner block**, a **spawn egg**, or through a **Nether portal** are marked in their own persistent data at birth. The mark is sticky: it survives chunk save/load and transformations/splits that Bukkit surfaces through EntityTransformEvent. When a marked mob is killed, its Jobs and mcMMO rewards are scaled to **`farm-reward-percent`** in `config.yml` — **65 percent by default** (Mike's live value is 55):

- **Jobs pays the reduced share**: money, points and Jobs XP are each scaled on Jobs' own payment/XP events, which carry the killed entity, so the reduction is exact per kill. Nothing is cancelled at any percent above 0.
- **mcMMO pays the reduced share of XP**: neither the killer's combat XP nor party share escapes; the reduction lands before the weekend (1.20x) and together (1.25x) multipliers can grow the amount, so a reduced gain stays reduced after multipliers.
- **Vanilla XP is never touched.** Marked mobs still drop their normal XP orbs and loot. The guard cancels no damage or death event (except at percent 0, below), removes no entity, and edits no balance or skill directly.

The two endpoints of `farm-reward-percent` are deliberate: **0** reproduces ExcelRewardGuard 1.0 exactly (Jobs payment cancelled, Jobs and mcMMO XP zeroed/blocked), and **100** turns the reduction off entirely (full rewards, an exact pass-through with no rounding). The percent is a whole number 0–100; anything else in the config is clamped or defaulted to 65 with a warning in the startup log.

## The 1.2 addition: fall-softened kills (sky farms)

Birth marks cannot see a sky farm: its mobs spawn naturally in the dark at height, fall, and are finished with one hit, so origin-based classification calls them NATURAL and pays 100%. Since 1.2 the guard also tracks each mob's **cumulative FALL damage** (the final damage of every fall event, with the mob's max health captured at each fall). A kill whose target's cumulative fall damage reaches **`farm-fall-damage-percent`** of its max health (default **50**) counts as farmed and earns the same reduced share, on both the Jobs and mcMMO paths, logged under a distinct **FALL_SOFTENED** label. A kill matching both a birth mark and the fall signature is scaled once; the origin label wins.

Rules, exactly as implemented and pinned by tests:

- **Cumulative, never forgiven.** Healing or regenerating does not reduce the total. A mob that fell for 60% of its health bar, healed to full, and is then killed still counts as softened; a second fall after healing stacks on top (totals can exceed 100%).
- **Latest max health.** The ratio uses the max health observed at the mob's most recent fall; the comparison is `>=`, so exactly at the threshold counts.
- **Bounded memory.** Only mobs that actually took fall damage get an entry. Entries are dropped when the mob dies (after death processing completes), is removed/despawns, or its chunk unloads, and the whole map clears on plugin disable. Players are not tracked.
- **Accepted false positive.** A mob that falls down a ravine entirely on its own and is finished by a player at or above the threshold counts as farmed. No signal separates a built farm's fall from a natural ravine's; the policy errs toward the farm side.

Everything else stays deliberately unclassified by the fall path: mobs that never fell (or fell less than the threshold) keep their normal rewards. EliteMobs reward handling is unchanged from 1.1 (verdict logging only — see the limitation below).

## Dependencies (all optional, disclosed per Mike's rule)

| Plugin | What it provides | Without it |
|---|---|---|
| Jobs 5.2.6.3 | `JobsPrePaymentEvent` and `JobsExpGainEvent`, whose money/points/XP amounts are scaled for farmed kills | Jobs enforcement inactive; marking, fall tracking and the API still work |
| mcMMO 2.3.003-WRV | `McMMOPlayerXpGainEvent` / `McMMOPlayerPreXpGainEvent`, scaled (or, at percent 0, zeroed/cancelled) inside a farmed-kill window | mcMMO enforcement inactive; marking, fall tracking and the API still work |
| EliteMobs 10.9.8 | `EliteMobDeathEvent`, used for verdict logging only | No elite logging; nothing else changes |
| Vault / an economy plugin | Not used by the guard itself. Jobs needs one to pay out at all | n/a |

Every API signature above was verified with javap against the exact jars the server runs, and `LiveApiSignatureTests` re-checks them on every build. 1.2 uses no new third-party API: the fall path is plain Bukkit (`EntityDamageEvent`, `EntityDeathEvent`, `EntityRemoveEvent`, `EntitiesUnloadEvent`, the MAX_HEALTH attribute).

**EliteMobs limitation, stated plainly:** EliteMobs 10.9.8 exposes no cancellable reward event (its death event is informational), and it pays coin showers through its own internal handling. The guard therefore **cannot reduce EliteMobs rewards**; it logs the deaths of marked elites so the gap is visible. 1.2 deliberately does not extend even the logging to fall-softened elites — EliteMobs interaction stays exactly what 1.1 shipped.

## How the mcMMO reduction works (and its one tradeoff)

mcMMO's XP events carry the player, skill and amount, but not the killed entity, so the guard cannot ask "which mob was this XP for?". Instead, when a farmed mob dies (birth-marked or fall-softened), the guard plants a short window (default: the death tick + 2 ticks, 40-block radius). mcMMO grants kill XP synchronously while processing that same death, so PVE and SHARED_PVE gains landing inside the window, in the same world, for the killer or anyone inside the radius, are scaled at LOWEST event priority. The scaling happens exactly once, on the XP gain event: the installed mcMMO build's own source shows the pre-gain amount feeding into the gain event, so scaling both events would double-count the cut. The tradeoff: if a second player lands an unrelated legitimate kill in the same 2-tick window within 40 blocks of a farm death, that gain is scaled too. On a small family server this is vanishingly rare, and the policy errs toward the farm side on purpose; every reduction is counted, and `debug-logging: true` prints each one with its before/after amounts.

Jobs needs no such window: its events name the entity, so a Jobs reduction can only ever hit the exact farmed kill. One Jobs caveat: if Jobs is configured to batch payments and no longer carries the killed entity when it pays, that payment cannot be reduced; the reduction covers every payment Jobs still attributes to the entity, which is how 5.2.6.3 fires its pre-payment event.

## Provenance notes

- Marking starts when the plugin is installed. Mobs already alive in the world at install time carry no mark and are unclassified by origin (the WRV mcMMO build's own persistent mob flags cover part of this gap for mcMMO's internal math, but the guard only speaks for its own marks). Fall tracking likewise starts at install: a mob that fell before 1.2 was installed has no fall history.
- The mark uses only this plugin's own namespaced keys (`excelrewardguard:origin_v1`, `denied_v1`, `reason_v1`). Foreign plugins' tags are never read or mutated. Fall records live only in memory (never persisted), keyed by entity UUID.
- Fail-closed: if provenance storage ever throws, classification reports unavailable, which the API contract defines as DENY, and the failure is logged once at SEVERE. Rewards fail safe (reduced/blocked), never silently open.

## Consumer API (unchanged from the candidate)

`ExcelRewardGuard.farmApiVersion()` returns `1`. `farmDecision(LivingEntity)` returns `[decision, reason, "1"]` where decision is `DENY` or `UNKNOWN` — this remains the **origin** verdict, exactly as in 1.0/1.1. 1.2 adds enforcement-side accessors (`isFarmed`, `farmedReason`, `isFallSoftened`, `fallDamagePercentOf`, `farmFallDamagePercent`) used by the guard's own Jobs/mcMMO paths; the published API contract is unchanged.

## Configuration

`config.yml`: `farm-reward-percent` (65; the share of Jobs/mcMMO rewards a farmed kill still earns — see above), `farm-fall-damage-percent` (50; cumulative fall damage, as a percent of max health, at which a kill counts as farmed — see the sky-farm section), `jobs-veto`, `mcmmo-veto`, `elitemobs-verdict-logging` (all default true; each is an off switch for its own path — the names keep "veto" from 1.0, they now gate the reduction), `party-share-radius-blocks` (40), `suppression-window-ticks` (2), `debug-logging` (false). Both percents parse defensively (clamped to 0–100, invalid values defaulted with a startup warning) and are re-read on enable, as in 1.1.

## Tests

Five suites run on every build (`bash build.sh`, compiling against the exact Paper 26.2-123 API and the live Jobs/mcMMO/EliteMobs jars) — **268 assertions**:

- `ProvenanceTests`: 85 assertions (unchanged from 1.1) — denied/unknown origin policy, denial stickiness across copies and re-marking, corrupt-marker fail-closed, API shape.
- `XpSuppressionTests`: 22 assertions (unchanged) — killer match at bow range, party radius boundary, world separation, non-kill reasons never suppressed, window expiry and purge, multi-marker and pre-death timestamps.
- `RewardScaleTests`: 51 assertions (unchanged) — the scaling math, half-up cent rounding, the 0 and 100 endpoints, config parsing, window+party scaling.
- `FallDamageTrackerTests`: 81 assertions (new in 1.2) — the sky-farm signature, the >= threshold boundary, multi-fall and fractional accumulation, the healed-mob cumulative rule, the latest-max-health rule, threshold 0/100 endpoints, no-entry guards, per-entity independence, death/despawn/chunk-unload/disable cleanup, spawner+fall overlap composition (farmed once, origin label wins), and threshold config parsing.
- `LiveApiSignatureTests`: 29 assertions (unchanged) — every third-party method/cancellation property the enforcement code relies on, checked against the installed jars.

Boot evidence (local Paper 26.2-123, Oct 16): with CMILib + Jobs + Vault + mcMMO + EliteMobs present and a fresh config, the guard generates `config.yml` with `farm-fall-damage-percent: 50`, reports fall-softened detection ON at 50%/65%, and reaches Done with zero SEVERE; a second boot with edited values (55% / threshold 40) reports exactly those values. Rebuilding reproduces the identical jar hash. Logs: `target/boot-with-providers.log`, `target/boot-edited-config.log`.

## Still unverified (honest list)

- Live-server behavior: no install has happened. The real sky-farm test (a fall-dropped mob at Kailey's farm paying the reduced share while a full-health kill nearby pays 100%, vanilla XP whole either way) is an in-game acceptance check after install, not done yet. No mob was simulated in the boot tests — classification is unit-verified, wiring is boot-verified.
- Jobs deferred/batch payment attribution on the live Jobs config (see the caveat above).
- EliteMobs reduction: impossible through its 10.9.8 API; config-side review is a separate track (unchanged by 1.2).
- Performance/save volume on the 3 GB live server is unmeasured; the guard writes at most three small PDC keys per special-origin spawn and holds at most one small in-memory record per mob that has taken fall damage.
