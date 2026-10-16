# ExcelRewardGuard 1.1

Farm-reward guard for the Wink Reyes Valley family server (Paper 26.2, Java 25).

**Status: Built, automated tests passed, local test-server passed. NOT installed live.** Installation needs Mike's approval and an empty-server restart window, like every WRV plugin.

Authorship: the provenance candidate was written by Moon (Oct 14, 2026 handoff package, marked DO NOT INSTALL because it classified mobs but blocked nothing). The enforcement completion, tests and this README are by Argus (Oct 15, 2026), finishing that candidate to the design Mike agreed on Oct 13. Version 1.1 (Oct 15, 2026) changes the policy from "farmed kills earn nothing" to "farmed kills earn a reduced share", per Mike's decision the same day: players still have to find or build the farm and land the kills themselves, so the reward is cut, not erased.

## What it does

Mobs born from a **spawner block**, a **spawn egg**, or through a **Nether portal** are marked in their own persistent data at birth. The mark is sticky: it survives chunk save/load and transformations/splits that Bukkit surfaces through EntityTransformEvent. When a marked mob is killed, its Jobs and mcMMO rewards are scaled to **`farm-reward-percent`** in `config.yml` — **65 percent by default**:

- **Jobs pays 65%**: money, points and Jobs XP are each scaled on Jobs' own payment/XP events, which carry the killed entity, so the reduction is exact per kill. Nothing is cancelled at any percent above 0.
- **mcMMO pays 65% XP**: neither the killer's combat XP nor party share escapes; the reduction lands before the weekend (1.20x) and together (1.25x) multipliers can grow the amount, so a reduced gain stays reduced after multipliers.
- **Vanilla XP is never touched.** Marked mobs still drop their normal XP orbs and loot. The guard cancels no damage or death event (except at percent 0, below), removes no entity, and edits no balance or skill directly.

The two endpoints of `farm-reward-percent` are deliberate: **0** reproduces ExcelRewardGuard 1.0 exactly (Jobs payment cancelled, Jobs and mcMMO XP zeroed/blocked), and **100** turns the reduction off entirely (full rewards, an exact pass-through with no rounding). The percent is a whole number 0–100; anything else in the config is clamped or defaulted to 65 with a warning in the startup log.

Everything else is deliberately unclassified: natural mobs, bred mobs, trial-spawner mobs and raid mobs keep their normal rewards. This plugin does not try to detect "grinder behavior" from location or damage patterns; provenance at birth is the whole policy.

## Dependencies (all optional, disclosed per Mike's rule)

| Plugin | What it provides | Without it |
|---|---|---|
| Jobs 5.2.6.3 | `JobsPrePaymentEvent` and `JobsExpGainEvent`, whose money/points/XP amounts are scaled for marked kills | Jobs enforcement inactive; marking and the API still work |
| mcMMO 2.3.003-WRV | `McMMOPlayerXpGainEvent` / `McMMOPlayerPreXpGainEvent`, scaled (or, at percent 0, zeroed/cancelled) inside a marked-kill window | mcMMO enforcement inactive; marking and the API still work |
| EliteMobs 10.9.8 | `EliteMobDeathEvent`, used for verdict logging only | No elite logging; nothing else changes |
| Vault / an economy plugin | Not used by the guard itself. Jobs needs one to pay out at all | n/a |

Every API signature above was verified with javap against the exact jars the server runs, and `LiveApiSignatureTests` re-checks them on every build. For 1.1 that includes the scaling surface: on Jobs 5.2.6.3, `JobsPrePaymentEvent` exposes `get/setAmount` (money), `get/setPoints` (points) and `get/setExp` (XP) — each setter writes the event's amounts map under its `CurrencyType` (checked in the bytecode) — and `JobsExpGainEvent` exposes `get/setExp`. Every component the 1.0 veto covered has a setter, so **no component falls back to cancellation** at percent above 0.

**EliteMobs limitation, stated plainly:** EliteMobs 10.9.8 exposes no cancellable reward event (its death event is informational), and it pays coin showers through its own internal handling. The guard therefore **cannot reduce EliteMobs rewards**; it logs the deaths of marked elites so the gap is visible. EliteMobs' own anti-exploit caps remain its farm control, and its spawner-related settings should be reviewed on the live config before this plugin is installed.

## How the mcMMO reduction works (and its one tradeoff)

mcMMO's XP events carry the player, skill and amount, but not the killed entity, so the guard cannot ask "which mob was this XP for?". Instead, when a marked mob dies, the guard plants a short window (default: the death tick + 2 ticks, 40-block radius). mcMMO grants kill XP synchronously while processing that same death, so PVE and SHARED_PVE gains landing inside the window, in the same world, for the killer or anyone inside the radius, are scaled at LOWEST event priority. The scaling happens exactly once, on the XP gain event: the installed mcMMO build's own source shows the pre-gain amount feeding into the gain event, so scaling both events would double-count the cut. The tradeoff: if a second player lands an unrelated legitimate kill in the same 2-tick window within 40 blocks of a farm death, that gain is scaled too. On a small family server this is vanishingly rare, and the policy errs toward the farm side on purpose; every reduction is counted, and `debug-logging: true` prints each one with its before/after amounts.

Jobs needs no such window: its events name the entity, so a Jobs reduction can only ever hit the exact marked kill. One Jobs caveat: if Jobs is configured to batch payments and no longer carries the killed entity when it pays, that payment cannot be reduced; the reduction covers every payment Jobs still attributes to the entity, which is how 5.2.6.3 fires its pre-payment event.

## Provenance notes

- Marking starts when the plugin is installed. Mobs already alive in the world at install time carry no mark and are unclassified (the WRV mcMMO build's own persistent mob flags cover part of this gap for mcMMO's internal math, but the guard only speaks for its own marks).
- The mark uses only this plugin's own namespaced keys (`excelrewardguard:origin_v1`, `denied_v1`, `reason_v1`). Foreign plugins' tags are never read or mutated.
- Fail-closed: if provenance storage ever throws, classification reports unavailable, which the API contract defines as DENY, and the failure is logged once at SEVERE. Rewards fail safe (reduced/blocked), never silently open.

## Consumer API (unchanged from the candidate)

`ExcelRewardGuard.farmApiVersion()` returns `1`. `farmDecision(LivingEntity)` returns `[decision, reason, "1"]` where decision is `DENY` or `UNKNOWN`. No ALLOW exists. DENY means the kill's rewards are farm rewards (the guard itself scales them to `farm-reward-percent`; a consumer making its own reward decision should apply the same policy before bonuses, party sharing, quests and payouts); UNKNOWN means "not proven farmed", never "proven clean".

## Configuration

`config.yml`: `farm-reward-percent` (65; the share of Jobs/mcMMO rewards a marked kill still earns — see above), `jobs-veto`, `mcmmo-veto`, `elitemobs-verdict-logging` (all default true; each is an off switch for its own path — the names keep "veto" from 1.0, they now gate the reduction), `party-share-radius-blocks` (40), `suppression-window-ticks` (2), `debug-logging` (false).

## Tests

Four suites run on every build (`bash build.sh`, compiling against the exact Paper 26.2-123 API and the live Jobs/mcMMO/EliteMobs jars):

- `ProvenanceTests`: 85 assertions — denied/unknown origin policy, denial stickiness across copies and re-marking, corrupt-marker fail-closed, API shape.
- `XpSuppressionTests`: 22 assertions — killer match at bow range, party radius boundary, world separation, non-kill reasons never suppressed, window expiry and purge, multi-marker and pre-death timestamps.
- `RewardScaleTests`: 51 assertions — the 65% scaling math for money-style doubles and XP floats, half-up cent rounding (including sub-cent results), the 0 and 100 endpoints, config parsing (missing/invalid/out-of-range/fractional/wrong-type), and the window+party scenario where a party-share gain inside the window is scaled to 65%, not zeroed.
- `LiveApiSignatureTests`: 29 assertions — every third-party method/cancellation property the enforcement code relies on, checked against the installed jars, including the pinned absence of a cancellable EliteMobs reward event and the full get/set surface the scaling relies on.

Boot evidence (local Paper 26.2-123, Oct 15): with CMILib + Jobs + Vault + mcMMO + EliteMobs present, the guard enables with both scaling paths ACTIVE at 65% and the server reaches Done with no guard errors; with no providers present it enables standalone with each path reported inactive and no class-loading failures. Rebuilding reproduces the identical jar hash. Logs: `target/boot-with-providers.log`, `target/boot-standalone.log`.

## Still unverified (honest list)

- Live-server behavior: no install has happened. In-game farm test (spawner/egg/portal kills paying 65% while natural kills pay normally) is part of the install checks, not done yet.
- Jobs deferred/batch payment attribution on the live Jobs config (see the caveat above).
- EliteMobs reduction: impossible through its 10.9.8 API; config-side review pending (see limitation).
- Performance/save volume on the 3 GB live server is unmeasured; the guard writes at most three small PDC keys per special-origin spawn.
