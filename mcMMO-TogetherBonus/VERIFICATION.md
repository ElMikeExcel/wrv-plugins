# XP feature verification and staging plan

This feature is a **candidate for review and staging**, not a claim of live activation. No server files, player data, permissions, startup settings, or restart state were changed while implementing it.

## Current indicator-only candidate

mcMMO **2.3.003-WRV-XP-2** appends patch 0004 to the unchanged first three patches, and pairs with unchanged WeekendXP **1.0.2**. It is not deployed. The installed XP-1 pair remains documented in [the deployment record](DEPLOYMENT-2026-10-12.md).

Local and remote verification for this update is recorded in `verification/xp-indicator-fix.json`. The original results below are historical evidence for XP-1, not a substitute for testing XP-2. No production server access, upload, restart or player-data changes are part of this indicator fix.

### Isolated runtime verification

The unchanged XP-2 CI artifact passed **38 assertions** on exact **Paper 26.2 build 123 / Temurin 25.0.4**, alongside unchanged WeekendXP 1.0.2. Native Paper bossbars, scheduler, XP profiles/events, actual 60-tick expiry, repeated gains, zero/cancelled awards, preference controls and cleanup were exercised. Together/weekend skill composition and vanilla fractional payout passed. The process stopped cleanly and its loopback-only port closed.

Players are synthetic and outgoing packets suppressed. Actual Java/Bedrock rendering, Geyser/Floodgate, gameplay orb collisions/Mending, reconnect persistence and the production plugin stack remain untested. The fixture neutralized upstream EarlyGameBoost only in its disposable config to isolate true zero awards. See [reproducible harness and evidence](verification/runtime-harness/README.md).

### Player acceptance checklist for XP-2

1. Join and stand still, change worlds and move in/out of Together range: no indicator until XP is earned. `/xpbonus status` stays available in chat.
2. Earn positive Mining or another mcMMO skill XP, then stop: one indicator appears and disappears after about 3 seconds at normal tick rate.
3. Pick up positive gameplay XP orbs, including a source without a simultaneous skill award (for example furnace extraction): same temporary behavior. A fully Mended/zero award produces no bar.
4. Keep earning XP: the one bar stays briefly after the most recent gain. Stop earning: it disappears. Check ×1.00, ×1.25 Together, ×1.20 weekend, and ×1.50 combined without changing XP amounts.
5. Hide, earn, reconnect and earn: still hidden. Show/toggle on: wait for the next gain; no persistent bar. Status alone must not create one.
6. Confirm cancelled/zero skill awards, rejected sources, Creative/Spectator and unloaded profiles do not cause false gain flashes; an independently valid skill award may still notify when its orb is excluded.
7. Quit or change worlds with the bar active; no stale bar remains. Normal server restart clears pending notifications and keeps visibility preferences. No hot reload.
8. Repeat on Java and Geyser/Bedrock with skill progress bars and other bossbars active. Client rendering and the exact live Paper 26.2 build 123 / Java 25 plugin stack require authorized runtime acceptance.

## Previous XP-1 deliverables

Install the paired custom mcMMO `2.3.003-WRV-XP-1` and WeekendXP `1.0.2` builds only after approval and staging. Existing repaired plugins outside this pair are a separate rollout. Keep the original two Together patches unchanged; apply the new third patch after them to the pinned upstream base.

The associated delivery report records final build commands, test totals, source identities and candidate SHA-256 hashes. Do not substitute the upstream Maven `-bin.zip` without checking its contents; the reviewed deployment candidate is the final shaded `target/mcMMO.jar`.

## Previous XP-1 verified local results (2026-10-12)

- mcMMO clean `verify`: **2,263 tests**, zero failures/errors/skips in the normal upstream profile. Default `skip`, `stress` and `docker` tags remain excluded.
- Included **308** Together/all-skill/precision cases, **45** green-bar cases, and **27** focused status/indicator/command/registration/WorldGuard cases.
- WRV clean eight-module `verify`: **85 tests**, zero failures/errors/skips; repository audit **5/5**; shop generator **9/9**.
- All three patches applied cleanly to a fresh copy of the pinned source; resulting source tree exactly matched the tested tree. A clean package rebuild produced the identical shaded mcMMO JAR. Two WeekendXP clean builds also produced identical JARs.
- Candidate bytecode: mcMMO own classes Java 17 (61); all shaded dependencies no newer than 17. WeekendXP Java 21 (65). Both descriptors and bridge methods verified; no bundled Bukkit/test API classes.
- Independent code review's indicator accuracy issues (unsupported green, Spectator and WorldGuard PVE semantics) were corrected and tested.

The first complete test run exposed one old command-alias test fixture that did not initialize the new XP-handler dependency. Its fixture now injects the dependency explicitly; the subsequent clean full run passed. No production assertion failed in that initial run.

## Local checks

- All non-Docker/non-stress upstream tests, including new XP and status regressions
- Full eight-module WRV build plus repository and Spawn Shop tests
- Clean patch application on a fresh copy of the pinned source
- JAR descriptor/version, Java bytecode, expected class/API/resource presence and exclusion of tests/server APIs
- Whitespace and YAML checks
- Repeat build hash comparison when feasible; snapshot Maven dependencies are not a permanent byte-for-byte lock across environments

GitHub workflow changes add a separate pinned-upstream mcMMO patch/build job. That job has not run remotely until the feature branch is published; local success is not remote CI success.

## Authorized staging checklist

Use a disposable staging copy with the actual intended Paper, Java, Geyser/Floodgate, WorldGuard (if present), LuckPerms, mcMMO and WeekendXP versions. Use test players/data, never production player balances.

1. **Baseline and load:** Record exact `/version`, `/version mcMMO` and `/version WeekendXP`, compare installed JAR hashes, start once with exactly one JAR per plugin, and inspect startup for API/linkage/config warnings. Confirm `/xpbonus` is registered. Verify `Party.Together_Bonus.Enabled: true`, multiplier 1.25, range 32, minimum one, and intended permission on test players.
2. **Weekday solo:** With controlled staging clock/schedule, collect a known 100-point gameplay-orb total and earn known skill XP. Both should have only their baseline/event-independent rates. Indicator shows bonus ×1.00 when base rates are neutral.
3. **Weekday Together:** Same-party member in same world at 10 blocks; expect ×1.25 in each eligible channel. Four separate 1-point pickups yield five points. Verify stored skill fraction (e.g. 10 base -> 12.5), not just chat or a rounded skill command.
4. **Weekend solo:** Current Chicago weekend state yields ×1.20 for both channels. Five 1-point pickups yield six total points. Restart staging on a weekend and again on a weekday to verify both initial applications; exercise transition and failure/retry without touching production time.
5. **Weekend Together:** Expect ×1.50, including two 1-point pickups -> three points. Skill global rate 1.2 and Together1.25 compose once. Verify party sharing with at least two participants, including different recipient proximity/permission states, and unchanged party-level Together accounting.
6. **Eligibility edges:** No party, different party, other world, 31.999/32/32.001 distance, offline teammate, no perk, insufficient member count, Creative/Spectator, unloaded profile, disabled parties/Together, blacklisted world, and region flags. Known death/custom/unknown orb pickups should not boost. Normal commands/direct API and direct advancement rewards remain outside the orb hook.
7. **Source and Mending matrix:** Test mining, mobs, fishing, furnace extraction, breeding, trading, grindstone, ordinary bottles and mcMMO-created reward orbs. With damaged Mending equipment, check only leftover green-bar XP gets the factor; a fully consumed orb earns no green-bar bonus. Check existing mcMMO fishing/smelting vanilla modifiers still coexist normally.
8. **Provenance limitation:** Test separate and mixed-source death/gameplay orbs. Record the upstream merge-label limitation rather than calling it an exploit-proof guarantee. Do not add an XP-bank/bottling plugin without integration review. Ensure the server's current plugin set has no disguised-withdrawal loop.
9. **Fraction persistence:** Earn a .25/.5/.75 pending fraction, disconnect/rejoin, fully restart staging, change worlds/party and test death/respawn both with and without keepInventory. Verify normal entity persistent-data behavior and document any host/plugin deviations before rollout.
10. **Indicator, Java and Bedrock:** At base, Weekend, Together and combined states, verify legibility and refresh within roughly one second. Enter/leave proximity and worlds, toggle show/hide repeatedly, reconnect and restart, and verify there is only one dedicated indicator. Use `/xpbonus status` for per-skill overrides and unavailable/blocked states. Check skill progress bars, boss fights/EliteMobs bars, action-bar notices and mobile screen space; do not claim client compatibility from a unit test alone.
11. **Cleanup and rollback:** Disconnect all test players and disable/shut down staging; no stale indicator or repeating task should remain. Stop the process before swapping back both previous plugin JARs. Preserve configs/player data backups; do not use unsupported plugin hot reload as a deployment method.

## Production rollout, only after explicit approval

- Verify the actual installed versions/config and that no newer edits supersede this candidate.
- Back up both old JARs, relevant mcMMO/WeekendXP configs and player data.
- Upload the two reviewed candidate JARs, remove duplicate old plugin files, preserve the approved Together flag, and perform one approved normal restart.
- Verify startup, both versions/hashes, live rates, `/xpbonus`, ordinary gameplay and Java/Bedrock display.
- Roll back both JARs together if the bridge/capability checks or gameplay fail. A startup failure is not proof that a retry/reload is safe.
