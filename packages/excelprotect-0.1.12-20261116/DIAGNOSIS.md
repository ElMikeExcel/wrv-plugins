# P-PROTECT-EM-1 — DIAGNOSIS: why ExcelProtect 0.1.11's EM block-attempt recorder is blind live

**Worker:** board supervisor run 432, desk diagnosis only (no installs, no live-server changes, no server contact this run).
**Date:** Mon Nov 16, 2026.
**Item:** ExcelProtect 0.1.11 (Installed Nov 15, 2026) — its `block_attempt` recorder produced ZERO records in live EliteMobs (EM) worlds during the Nov 16 live proof, while overworld controls recorded perfectly.
**Headline finding (one line):** 0.1.11 listens at the END of the break/place event chain (`BlockBreakEvent` / `BlockPlaceEvent`, MONITOR). EliteMobs 10.9.14 stops break and place attempts at the START of the chain — it cancels `BlockDamageEvent` at LOWEST and sets `BlockCanBuildEvent.setBuildable(false)` at LOWEST — so in a protected EM world the `BlockBreakEvent` / `BlockPlaceEvent` Protect listens for **are never fired at all**. There is nothing for a MONITOR listener to observe, cancelled or not. This is proven from the actual live-version EM jar's bytecode (Section 2), not inferred.

---

## 1. Protect side — source location and exact listener registrations

### 1.1 Source tree (the tree that built the installed 0.1.11)

Root: `~/workspace/protect-0111-build-20261115/build-tree/`

| File | Size (B) | SHA-256 |
|---|---:|---|
| `ExcelProtect-0.1.11.jar` (candidate = installed jar) | 303,587 | `ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f` |
| `src/com/winkreyes/excelprotect/BlockAttemptWatch.java` | 4,182 | `022171cde139b600d128aad58a52d1f38db6aa7496f4caa0a2dba3a41d248a68` |
| `src/com/winkreyes/excelprotect/BlockAttempts.java` (pure coalescer) | 5,761 | `a737d69807579725a808e502a0e5dfb7024a074c0c11477dcce9efd017eb79fa` |
| `src/com/winkreyes/excelprotect/Blocks.java` (block_change family) | 8,902 | `c218f4fdc5f2f6bd22497fe0eb05e9280836ad322387db863f6d11fde174d6c7` |
| `src/com/winkreyes/excelprotect/Settings.java` | 11,747 | `1b45c780d5f882584f7ea7a3367cd07ee14907edc4e098dece7c55c7f34c244c` |
| `src/com/winkreyes/excelprotect/ExcelProtect.java` (main / registration) | 77,294 | `b90aa42bc553adfb4637feb7781f92c6ed9d339e4c213e3962130ca2dd3d2360` |
| `excelprotect-0.1.11-source.zip` (at `~/workspace/protect-0111-build-20261115/`) | — | `ef13e67fcdf7aa6418aad56b1b301d3a42ef2d316b2acc38dc8d1d137b6db262` |

Installed-jar identity is independently recorded in `~/workspace/protect-0111-build-20261115/SHA256SUMS` and `INSTALLED.txt` (jar `ba10bbcd…`, boot `READY version=0.1.11`, installed 2026-11-15 18:55 CST).

### 1.2 The attempt listeners (the 0.1.11 headline feature)

File: `BlockAttemptWatch.java`. Registration in `ExcelProtect.java` lines 172–176: the watch is constructed and registered **only if** `cfg.attemptsEnabled()` at `onEnable`, and a 20-tick sweep timer drives its coalescer flush.

> `BlockAttemptWatch.java:44` — `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)`
> `BlockAttemptWatch.java:45` — `public void onBreak(BlockBreakEvent e)` — body: returns unless `e.isCancelled()`, then `feed(player, block, "break")`.
>
> `BlockAttemptWatch.java:53` — `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)`
> `BlockAttemptWatch.java:54` — `public void onPlace(BlockPlaceEvent e)` — body: returns unless `e.isCancelled()`, then `feed(player, block, "place")` (block = `e.getBlock()`, the attempted placed type).

`feed()` (same file, lines 64–73) drops anything outside EM scope (`Settings.attemptInScope(world)`: world name starts with `log.attempts.world-prefix`, default `"em_"`, or is in `log.attempts.worlds`; `Settings.java:97–102`) and feeds the per-player coalescer (`BlockAttempts`: open records keyed `Map<UUID, Map<key, Record>>`, key = world|pos|action|block, gap default 2,000 ms from `log.attempts.coalesce-ms`). Closed records render as `type=block_attempt, cancelled=true, count, first/last_ts_ms` with **no before/after fields** (never rollback input) and are emitted by the Ledger to the separate `attempts` stream; per-player timelines project from that stream. This part of the design is sound and is retained unchanged by the fix.

Note the correction to the older shorthand: the **attempt** watch is MONITOR + `ignoreCancelled=false` (it *does* see cancelled break/place events — the rig in Section 3 proved that). MONITOR + `ignoreCancelled=true` describes the older `Blocks.java` block_change handlers:

> `Blocks.java:80` — `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)` / `onPlace(BlockPlaceEvent)`
> `Blocks.java:88` — `@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)` / `onBreak(BlockBreakEvent)`
> (all 14 handlers in `Blocks.java` carry the same annotation.)

### 1.3 What Protect does NOT listen to (verified by grep over the whole 0.1.11 tree)

- No `BlockDamageEvent` handler anywhere in the tree.
- No `BlockCanBuildEvent` handler anywhere in the tree.
- The only `PlayerInteractEvent` handler is the inspect click in `ExcelProtect.java:283` (`onInspectClick`); the only interact-entity handler is in `Entities.java:199`. Neither feeds attempts.

So Protect has **zero** presence at the two points where EM actually stops attempts (Section 2).

### 1.4 Config in force live (from the Nov 16 proof, `~/workspace/proofs-20261116/RESULTS.md`)

`log.attempts.enabled: true`; boot line verbatim: `attempts: ON stream=attempts scope=prefix 'em_' + config list coalesce=2000ms`. No `attempts-<day>.jsonl` file existed for Nov 15 or Nov 16 before the proof session — zero attempt records since the Nov 15 18:56 install. The recorder was on, in scope, and correctly registered; it simply never received an event.

---

## 2. EliteMobs side — how EM 10.9.14 actually intercepts break/place

Evidence artifact (the exact live version, 10.9.14 — the jar staged for the Nov 14 EM update, whose SHA-256 matches the Modrinth-published hash in `~/workspace/em10914-update-packet-20261114/RECEIPTS.md`):

| Artifact | Size (B) | SHA-256 |
|---|---:|---|
| `~/workspace/em10914-update-packet-20261114/window-1114/staged-update-EliteMobs.jar` (EliteMobs 10.9.14) | 14,563,211 | `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd` |
| Class `com/magmaguy/elitemobs/magmacore/instance/InstanceProtector.class` (inside that jar) | 21,028 | `910efbf233b76aca14478c52695d9392fb85a0665be64d225d8edbe397a43afc` |
| Class `com/magmaguy/elitemobs/dungeons/EliteMobsWorld.class` (inside that jar) | 6,026 | `9a90c2b10861a410e54d842ec51a52e8f471bfb8fab916ef93b6dc383b2d8341` |

Full `javap -v -p` dump of `InstanceProtector` is preserved beside this file as `InstanceProtector.javap.txt` (3,035 lines; SHA-256 in SHA256SUMS). Method line numbers below are the class's own `LineNumberTable` (source `InstanceProtector.java`).

### 2.1 Which worlds are protected, and by what registration

`EliteMobsWorld` calls `InstanceProtector.addProtectedWorld(world, rules)` for each content-package world whose package sets `protect` (the `isProtect` field → `WorldProtectionRules`); instanced dungeons register the same way (the only classes in the jar referencing `addProtectedWorld` are `EliteMobsWorld`, `InstanceProtector` itself, and `DungeonInstance$WorldRetirement`). `InstanceProtector` keeps `protectedWorlds: Map<UUID, WorldProtectionRules>` and a per-player bypass set (`shouldBypass(player)` — the `/em` protection-bypass command). Every handler below first checks `isProtectedWorld(block.getWorld())` and `!shouldBypass(player)`, then stops the action.

### 2.2 The interception table (all in `InstanceProtector`, all `priority = LOWEST, ignoreCancelled = true`)

| # | Method (source lines) | Event | Action in bytecode |
|---|---|---|---|
| E1 | `preventPlayerBlockDamage` (117–120) | **`BlockDamageEvent`** | `event.setCancelled(true)` |
| E2 | `preventPlayerBlockBreak` (124–127) | `BlockBreakEvent` | `event.setCancelled(true)` |
| E3 | `preventPlayerBlockPlace` (137–140) | **`BlockCanBuildEvent`** | `event.setBuildable(false)` (player-null-safe; bypass-checked) |
| E4 | `preventBlockPlace` (193–196) | `BlockPlaceEvent` | `event.setCancelled(true)` |

Plus same-pattern LOWEST cancellers for burn, explosions, fade, fertilize, fire, buckets, and `PlayerInteractEvent` handlers for doors/containers/dragon egg — same class, same shape (see javap dump). A whole-jar scan confirms `InstanceProtector` is the **only** EM class handling `BlockCanBuildEvent`, and one of only two handling `BlockDamageEvent` (the other, `MatchListener.onBannerHit`, is a default-priority, non-cancelling minigame-banner hook — not world protection; dump preserved as `MatchListener.javap.txt`).

### 2.3 Why E1 and E3 make E2/E4 (and Protect) unreachable in real play

- **Break:** a survival dig starts with `BlockDamageEvent` (dig start / progress). EM cancels it at LOWEST (E1). A cancelled damage event aborts the dig — the block never completes breaking, so the server **never constructs or fires `BlockBreakEvent`**. E2 exists only as a backstop for paths that bypass the damage stage. The Nov 16 live proof matches exactly: a 5-second held attack on a guild stone wall (plus 900 ms / 1,200 ms leaf-wall attempts) left the block unchanged and produced no event Protect could see.
- **Place:** a placement attempt first asks `BlockCanBuildEvent` whether the target cell is buildable for that player/material. EM sets buildable=false at LOWEST (E3). A non-buildable placement is refused by the server **before `BlockPlaceEvent` is constructed or fired**; E4 is again only a backstop. The Nov 16 proof matches: right-click place against a guild stone face — dirt stayed in hand, block never appeared, no event.
- `BlockCanBuildEvent` in the server's Paper API (26.3, verified with `javap` against `~/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar`) is **not** `Cancellable`; it carries `isBuildable()/setBuildable(boolean)`, `getPlayer()`, `getBlock()` (target cell), and **`getMaterial()` / `getBlockData()` — the attempted placed type**. `BlockDamageEvent` carries `getPlayer()`, `getBlock()`, `getItemInHand()`, `getInstaBreak()`, `isCancelled()`.

### 2.4 What is NOT unknown anymore, and the one residual unknown

Determined locally, from the live-version jar: event classes, priorities, `ignoreCancelled`, exact stop actions, protected-world registration path, bypass path. The earlier "earlier interception — damage/interact stage — or EM's own protection path" hypothesis (RESULTS.md root-cause read) is now resolved to the damage stage for break and the can-build stage for place.

Residual unknown (count semantics only, does not affect the fix design): how many times `BlockDamageEvent` re-fires during one continuous held attack after each cancellation (client re-dig behaviour). This affects only raw feed rate into the coalescer, which merges same player+position+action+block feeds within 2,000 ms by design. The mirror Tier-2 test in Section 5 measures it; no other experiment is needed to determine the interception mechanism.

---

## 3. Why the mirror rig passed and live failed (the false proof)

`~/workspace/board-r404/protect-rig2/RIG.md` (Q4, 7/7 PASS) and its harness `src/rig/ProtectRig.java`:

- The harness registered a synthetic canceller at **HIGH** on `BlockBreakEvent` / `BlockPlaceEvent` and then **called those very events directly** through `PluginManager.callEvent(...)` with a fake player.
- That models "a plugin cancels BlockBreak/BlockPlace" — which Protect handles correctly (MONITOR, `ignoreCancelled=false`, verified: cancelled state observed, exactly one coalesced record, no block_change, toggle OFF silent).
- It does **not** model EM: EM's real first move is on *different events* (`BlockDamageEvent`, `BlockCanBuildEvent`) that the harness never fired, and in real play those early stops mean the harness's events never occur. The rig proved the recorder's logic; it never exercised the recorder's *input path* against EM's real path. Both statements in the Nov 15 check (`~/workspace/protect-em-check-20261115/REPORT.md`, Q4) — "EM cancels block break/place, Protect's handlers only record uncancelled events" — were true of `Blocks.java` but were silently extended to the new attempt watch, whose actual blind spot is one stage earlier.

Live-proof corroboration (RESULTS.md, Need #73): guild BREAK and PLACE attempts in `em_adventurers_guild` (SURVIVAL, attempts ON) → no attempts file created, nothing in the player timeline; overworld controls in `world` with the same client/technique → block_change records exactly as expected (attempts correctly silent out of scope). Primis + instance legs were deliberately not run live; the toggle leg was not run live (no reload command exists — see 4.4).

---

## 4. Root cause

**Root cause:** ExcelProtect 0.1.11's attempt recorder subscribes to the terminal events of the break/place flows (`BlockBreakEvent`, `BlockPlaceEvent` at MONITOR). EliteMobs 10.9.14's world protection (`InstanceProtector`, LOWEST) terminates both flows at their initial events — cancelling `BlockDamageEvent` for breaks and denying `BlockCanBuildEvent` for places — so in every protected EM world the terminal events are never fired, and the recorder is structurally blind there regardless of its priority or `ignoreCancelled` setting. The mirror rig's synthetic HIGH canceller on the terminal events masked this by firing events that real EM-protected play never produces.

Contributing factor: none on the config/scope side — scope (`em_` prefix + list), enablement, registration, coalescer, stream and timeline projection are all verified working (rig Q4 + live overworld controls + live boot line). The defect is exactly one layer: **wrong observation points**.

---

## 5. Fix design — proposed ExcelProtect 0.1.12

### 5.1 Patch (all inside `BlockAttemptWatch.java`; coalescer, Settings, Ledger, record shape otherwise untouched)

Add two handlers that mirror EM's two real interception points, keeping the two existing handlers as backstops:

| Handler | Event | Annotation | Record condition | Feed values |
|---|---|---|---|---|
| NEW `onDamage` | `BlockDamageEvent` | `MONITOR, ignoreCancelled = false` | `attemptsEnabled() && e.isCancelled()` + EM-scope world | player = `e.getPlayer()`; block/pos = `e.getBlock()` (the block being dug); action = `"break"`; block type = `e.getBlock().getType()` |
| NEW `onCanBuild` | `BlockCanBuildEvent` | `MONITOR, ignoreCancelled = false` (annotation value is inert — the event is not Cancellable — but keeps house style explicit) | `attemptsEnabled() && !e.isBuildable() && e.getPlayer() != null` + EM-scope world | player = `e.getPlayer()`; pos = `e.getBlock()` (target cell); action = `"place"`; **block type = `e.getMaterial()`** (the attempted type — `e.getBlock().getType()` at the target cell is still air/replaceable and must NOT be used) |
| KEEP `onBreak` | `BlockBreakEvent` | `MONITOR, ignoreCancelled = false` (unchanged, line 44) | `e.isCancelled()` (unchanged) | unchanged |
| KEEP `onPlace` | `BlockPlaceEvent` | `MONITOR, ignoreCancelled = false` (unchanged, line 53) | `e.isCancelled()` (unchanged) | unchanged |

Implementation notes for the build worker:

1. Both new handlers follow the existing house pattern exactly: whole body in `try { … } catch (Throwable ignored) {}`, read-only (never call `setCancelled`/`setBuildable`, never touch world state), no IO on the event path — they call the existing private `feed(player, block, action)` where possible. `onCanBuild` needs a small overload/variant of `feed` that accepts an explicit block-type string, because the attempted material comes from the event, not the target block.
2. **No double-count invariant** (state it in the class javadoc and test it in the mirror): one physical attempt produces at most one feed. If `BlockDamageEvent` is cancelled, `BlockBreakEvent` never fires; if `BlockCanBuildEvent` denies, `BlockPlaceEvent` never fires. The kept backstop handlers can only fire for attempts the early events did *not* stop (e.g. a canceller that acts only on the terminal event — the old rig's pattern — or EM bypass toggles changing mid-flow), which are disjoint from the new handlers' cases.
3. **Per-player law (unchanged, verified in code):** feeds go into `BlockAttempts`, whose open map is keyed by player UUID first; records render `player` + `player_uuid`; the Ledger writes the `attempts` stream and the per-player timeline projection reads it — one player's whole attempt story stays in a single pull, exactly as 0.1.11.
4. **Recommended (optional, one field):** add `"via"` to the record render (`"damage" | "can_build" | "break" | "place"`) so mirror/live proofs can show which observation point fired and a future EM change is diagnosable from records alone. This is additive JSON; if the supervisor prefers a byte-stable record shape for 0.1.12, drop `via` — the fix does not depend on it.
5. Version bump: **0.1.12**. Touch points: `plugin.yml` version + description tail, `local-build.sh` fingerprint string (`EXC-PROTECT-0.1.12-…`) and jar name, class javadoc headers in the two touched files. No EM compile dependency is introduced — both new events are Bukkit/Paper core, and `plugin.yml` already lists EliteMobs as a softdepend only.
6. New unit tests in `ProtectTest.java` alongside the existing 0.1.11 attempt tests (which start at line 2341): the coalescer is untouched, so existing tests stand; add render/feed tests only if `via` is adopted. The Bukkit glue itself is covered by the mirror re-proof (Section 6), as with 0.1.11.

### 5.2 Config / toggle implications

- **No new config keys.** The new handlers read the same `log.attempts.*` block (`enabled`, `coalesce-ms`, `world-prefix`, `worlds`) through the same `Settings` methods and the same `attemptInScope` check. The candidate `config.yml` does not need to change; the live config (attempts section present since 0.1.11) is already sufficient.
- **The attempts-toggle flip still rides a restart window.** There is no `/excelprotect reload`; `Settings` is a boot-time snapshot and `BlockAttemptWatch` is only constructed/registered when `attemptsEnabled()` is true at `onEnable` (`ExcelProtect.java:172–176`). Flipping `log.attempts.enabled` therefore requires a config edit + server restart, and the fix install itself is a jar swap — both belong in the same gated install/restart window. (This matches the Nov 16 proof note: the live toggle check was pre-deferred to the fix's restart window.)

---

## 6. Mirror re-proof plan (real EM path — synthetic canceller retired for this feature)

Venue: Homebase test-server mirror (`C:\Users\MikeExcel\wrv-testserver\server\`), mirror only, full backup/restore discipline as in rig2. The defining rule: **EliteMobs itself is the canceller.** No harness plugin may cancel break/place/damage/can-build events.

### 6.0 Setup

1. Place the real EliteMobs jar on the mirror: EliteMobs 10.9.14, 14,563,211 B, SHA-256 `4a6bc4a7…` (artifact in Section 2; verify hash after placement). Place the candidate ExcelProtect 0.1.12 jar + its config (attempts ON) the same way rig2 placed 0.1.11 (hash-verified, pre-rig jars/config backed up, restored byte-exact after).
2. Stage at least one **real EM content-package world with `protect: true`** on the mirror (the mirror previously carried no EM world folders — this staging is the step rig2 skipped): the Adventurers Guild package (world `em_adventurers_guild`) and the Primis package (world `em_primis`), using the same package files as live. Boot and verify from the log that EM loads both worlds and registers them protected (EM boot lines + a non-bypass player being stopped, observed in Tier 2).
3. A probe/listener harness is allowed **only** as a passive observer: a MONITOR, `ignoreCancelled=false` logger that records every `BlockDamageEvent`, `BlockCanBuildEvent`, `BlockBreakEvent`, `BlockPlaceEvent` it sees (class, cancelled/buildable state, player, pos) and cancels nothing. This produces the event-traffic evidence rig2 never had.

### 6.1 Tier 1 — event-path proof with a real client (the decisive tier)

Drive a real client on the mirror (Bedrock `.MikeExcel` via the Homebase PC agent, exactly as the Nov 16 live proof was driven; Java client equivalent acceptable). Player in SURVIVAL, not protection-bypassing. In `em_adventurers_guild`, at a named stone wall position (reuse the Nov 16 alcove coordinates ~(214, 87, 230) if the staged guild matches live):

| Case | Action | Expected structured evidence |
|---|---|---|
| M-1 BREAK | Hold attack ≥5 s on the wall block | Block unchanged; observer log shows `BlockDamageEvent` cancelled=true and **zero** `BlockBreakEvent` for the position; attempts stream gains exactly ONE coalesced `block_attempt` (action=break, that pos, count ≥1) after the 2 s coalesce window; ZERO block_change for the pos |
| M-2 PLACE | Right-click a held block (dirt) against a stone face from a legal cell | Placement refused (item stays in hand); observer shows `BlockCanBuildEvent` buildable=false and **zero** `BlockPlaceEvent`; exactly ONE `block_attempt` (action=place, block = the held material, target cell pos); ZERO block_change |
| M-3 CONTROL-IN-SCOPE | In the same EM world, an action EM allows (e.g. break/place where the package permits, or any uncancelled terminal event) | No attempt record for it; normal block_change if it changes a block |
| M-4 CONTROL-OUT-OF-SCOPE | Break + place in `world` (overworld), incl. one deliberately cancelled terminal event via any non-EM means if handy | Attempts stream: ZERO records with `"world":"world"` (scope gate intact) |

M-1/M-2 also settle the residual unknown from 2.4: the observer log counts raw `BlockDamageEvent` firings during the 5 s hold; the record's `count` must equal the number of *feeds* the coalescer merged (observer cancelled-damage count for that pos, modulo the window split) — report both numbers.

### 6.2 Tier 2 — coverage legs (same boot)

5. **Primis:** travel to `em_primis`, repeat M-1 + M-2 at a named position → one break + one place attempt record each, world field `em_primis`.
6. **Instance:** enter a real instanced dungeon from Primis (e.g. Wolf Den, as in the Nov 15 session) so EM creates an `em_id_*` world; repeat M-1 + M-2 inside → attempt records with the instance world name; exit, let EM delete the world, then confirm the records persist in the day file (retention behaviour already proven for other streams; attempts must match).
7. **Per-player separation:** a second player (or the fake-player rig identity, if it can be put through the real EM path) attempts at the same position → separate records per player UUID, no merging across players.
8. **Non-interference:** every attempted block is byte-identical before/after; EM protection outcome unchanged (nothing breaks/places that EM stopped before the fix).

### 6.3 Tier 3 — toggle flip (second boot, same window)

9. Edit mirror config `log.attempts.enabled: false`, restart (Boot B). Boot line must read `attempts: OFF (config log.attempts.enabled=false)`. Repeat M-1 + M-2 in the guild: EM still stops both (observer confirms), attempts file **byte-identical** across Boot B, zero records at the test positions. Restore mirror (jars, configs, worlds, rig artifacts) byte-exact and verify, per rig2 cleanup table.

**Pass bar:** M-1, M-2, Primis, instance, per-player and toggle legs all produce the expected records AND the observer log proves the terminal events never fired — i.e. the proof demonstrates the fix records *at EM's real interception points*, not merely that records appear.

### 6.4 Live re-proof (after the gated install of 0.1.12, in its restart window)

Repeat the Nov 16 protocol verbatim (RESULTS.md, Need #73): guild break + place (alcove ~(214, 87, 230)), Primis break + place, one `em_id_*` instance break + place, overworld controls at the house position, then read back the `attempts-<day>.jsonl` stream + the player's timeline; flip `log.attempts.enabled` off in the same window's second restart only if the supervisor schedules it (otherwise the mirror Tier-3 result stands and the live toggle remains owed — do not improvise a second live restart). Success = live attempt records exist for all three EM world classes, controls stay silent, and 0.1.12 is then Proven (not merely Installed).

---

## 7. Build readiness

**Verdict: buildable from the local source tree as-is.** The 0.1.11 tree that produced the installed jar is complete and self-contained; the fix is a two-handler addition to one file plus version/fingerprint bumps.

A build worker needs exactly:

1. **Source:** copy `~/workspace/protect-0111-build-20261115/build-tree/` to a new 0.1.12 build folder (src = 56 Java files incl. `api/`, tests = `tests/com/winkreyes/excelprotect/ProtectTest.java`, 152,983 B, SHA-256 `a899db77b0d6c327e46c2c1424657abc9ef8dde2a42e05b2a2ab2891f0edde1f`, already containing the 0.1.11 attempt test block at line 2341+). Apply the Section 5.1 patch to `BlockAttemptWatch.java` (and `BlockAttempts.java` only if `via` is adopted).
2. **Build script:** `local-build.sh` in the tree (deterministic: JUnit first, timestamp-pinned jar, fingerprint gate). Update only: plugin version strings, the `EXC-PROTECT-…` fingerprint line, and the output jar name.
3. **Toolchain (all verified present locally this run):** JDK 25 at `~/workspace/.jdk25` (`javac` present); Paper API `~/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar`; JUnit console `~/workspace/excelshops/build-libs/junit-platform-console-standalone-1.11.4.jar`; compile libs in-tree (`libs/GriefPrevention-16.18.7.jar`, `Vault-2.0.0.jar`, `mcMMO.jar`) plus the WorldEdit jars the script probes under `~/workspace/.m2/…` (softdepend, fail-soft if absent).
4. **No new dependencies:** no EliteMobs jar is needed to compile or run the fix (Bukkit/Paper events only); EM is needed only on the mirror for re-proof (Section 6.0, artifact + hash given there).
5. **Build gates (house standard, as for 0.1.11):** YAML parse of plugin.yml/config.yml, full JUnit suite green, double-build byte-identical jars, fingerprint gate PASS, SHA256SUMS + source zip produced, candidate hash recorded before any filing.
6. **Filing after build:** pointer-stub MANIFEST packet per PACKET-TEMPLATE with same-day G1 live-premise receipts (live jar is 0.1.11 `ba10bbcd…` — re-pull fresh at filing time; a baseline hash does not satisfy the gate), rig evidence from Section 6 attached, install + toggle flip scheduled into one restart window. Surfaces (Guide/What's New both/website/menus/tips check) ride the install pass per same-pass rules — none are part of this diagnosis.

Nothing is blocked: no missing source, no missing dependency, no open design question. The one judgment call (`via` field, 5.1.4) is reversible and does not gate the build.

---

## Evidence index (all hashes in SHA256SUMS beside this file)

- `~/workspace/proofs-20261116/RESULTS.md` — live proof, Need #73 (attempts blind live; controls pass) + Need #76.
- `~/workspace/protect-em-check-20261115/REPORT.md` — Nov 15 coverage investigation (Q1–Q5; blocks NOT COVERING; fix design note Option A).
- `~/workspace/board-r404/protect-rig2/RIG.md` + `src/rig/ProtectRig.java` — the synthetic-canceller rig (Q4 7/7) and why it does not model EM.
- `InstanceProtector.javap.txt`, `MatchListener.javap.txt` — in this folder; `javap -v -p` dumps generated this run from the EM 10.9.14 jar above.
- Protect 0.1.11 build tree + `SHA256SUMS` / `INSTALLED.txt` / `OUTCOME.txt` at `~/workspace/protect-0111-build-20261115/`.

VERDICT: DIAGNOSIS COMPLETE
