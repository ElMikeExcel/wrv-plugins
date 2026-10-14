# Independent gameplay review: reconstructed ExcelMobVeil candidate

Reviewed 2026-10-14 UTC. This is a **new, reconstructed candidate** after loss of the earlier workspace. All counts and hashes below come from the new clean build. Earlier candidate results are not evidence for this artifact.

## Current result

No remaining release-blocking gameplay source finding was identified in this fresh source/mocked-event review. Native Paper use, real AI, elapsed-time runtime behavior, and client presentation remain separate acceptance work and are not claimed as passed here.

Fresh offline clean build completed at **2026-10-14 01:49:43 UTC**:

- **180 tests; 0 failures, 0 errors, 0 skipped**
- 71 reconstructed gameplay tests: `test/com/winkreyes/excelmobveil/VeilGameplayTest.java`
- 13 reconstructed timing tests: `test/com/winkreyes/excelmobveil/VeilTimingTest.java`
- 96 current purchase/menu/journal/resource tests: 49 + 33 + 11 + 3
- Evidence: `build-reconstructed-gameplay.log` and `target/surefire-reports/`
- Artifact: `target/ExcelMobVeil-0.1.0.jar`
- SHA-256: `66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc`

Production Java timestamps precede this compilation. The packaged `plugin.yml` and `config.yml` were compared byte-for-byte with the current source resources and matched. The build used the existing JDK/Maven cache with offline mode enabled. This reviewer wrote test files and this report only, not production Java. No download of software, installation, live-server modification, publication, or EULA acceptance was performed by this reviewer.

## Required behavior verified by the new tests and source review

### Stored potion, additive timer, and cancellation

- The default configuration is $2,000 per potion. Purchase creates a stored, tagged, drinkable WATER-base potion; merely holding the tagged item does not activate the veil.
- Each successful drink adds **480,000 milliseconds** to remaining time. Two fresh sequential drinks yield 960,000 ms. An active extension retains the original start epoch. An expired timer starts one fresh duration.
- Eight sequential drinks and 10,000 pure deadline extensions are tested. There is no arbitrary gameplay cap; signed-long deadline representability is the bound.
- Deadline addition uses exact arithmetic and is checked before native consumption. Negative clocks and unrepresentable additions fail closed. Tick conversion safely clamps its representation at `Integer.MAX_VALUE`, while the absolute deadline remains authoritative. Large-effect chunk refresh and nonoverflowing status rounding are tested.
- A duplicate pending consume is rejected. Later sequential drinks are accepted. Final cancellation, a changed item, logout, death, and a pending extension invalidated by attacking cannot grant unintended extra time.
- Main-hand and offhand use the same activation/cancellation rules. The mock fixture explicitly models native use as the held potion becoming a glass bottle; actual native behavior still needs the separate runtime harness.

### Visibility, farming, targeting, and lifecycle

- Veil invisibility has a recorded signature: amplifier 0, non-ambient, particles off, icon on. Ordinary pulses do not resurrect a missing effect.
- Armor is not read or changed for target suppression. Standard target events and special Enderman, Slime, and Warden hooks are tested. Existing targets, paths, anger memory, Warden anger, and Wither heads are cleared at API-call level.
- Entity melee attempts, bow/projectile use, and supported player-attributed projectile/cloud/TNT/fang damage end **all accumulated time**. Incoming damage and player health are not cancelled, reduced, or rewritten.
- Block breaking/mining, planting/placing, harvesting, interaction/tilling, and air swings do not dispatch a broad listener that mistakes farming for an entity attack. Passive Thorns and utility pearls, experience bottles, and fishing hooks retain the veil.
- Death, milk, exact expiry, disable/re-enable restoration, offline elapsed time, and world changes are covered. Reconnect/restart/world transitions do not extend the deadline.
- An external effect replacement/removal retires the record synchronously, preventing a same-tick attack from stripping the foreign replacement. Unrelated effects are preserved. Failed restoration cannot leave active mob-ignore under a missing effect. Invalid typed or incomplete PDC records fail closed and are cleaned up.

### Observed failures and conservative rollback

- A thrown preflight `saveData()` failure cancels before native consumption.
- An observed post-consumption application/save failure restores the prior timer/effect where possible. If restoring the previous effect also fails, the record is cleared immediately.
- A consumed bottle is restored only when the captured hand snapshot and current glass-bottle slot provide evidence for a safe restoration. Main-hand/offhand isolation is tested. Uncertain slot changes receive no blind replacement and produce manual-reconciliation feedback.
- Creative/no-decrement and uncertain-slot rollback paths still request persistence of the reverted timer. An overflow occurring after native use follows the proven-slot restoration path without adding time.

## Limitations and release caveats

1. **Not crash-atomic.** Paper's void `saveData()` API can swallow disk errors. A successful return or rollback request does not prove durability. Native inventory decrement, effect changes, and playerdata writes are not one transactional commit. The tests verify decisions and in-memory state, not survival of a disk failure or process crash. Keep purchase/audit evidence and reconcile uncertainty manually.
2. **Effect ownership is inferred.** Bukkit exposes no durable per-effect owner ID. The plugin relies on the record plus effect signature. An indistinguishable foreign effect applied without normal events cannot be reliably distinguished.
3. **Other plugins can veto changes.** A later MONITOR cancellation can conservatively end mob-ignore while ordinary invisibility remains. A plugin cancelling removal can prevent immediate visual reveal even after the veil record ends. Conflicting effect or inventory plugins require compatibility testing with the installed set.
4. **No damage immunity.** Contact, environmental, untargeted, and already-launched attacks remain possible. Custom EliteMobs scripts, dragon behavior, boss scripts, and area attacks are outside the targeting guarantee. Armor and held items remain visible to other players.
5. **Tick-bound processing.** Time is measured against an absolute wall-clock deadline, including offline time. Visible cleanup and AI maintenance run on server ticks; a stalled server cannot apply a rendered change until processing resumes.
6. **Mocked coverage is bounded.** These checks prove listener decisions and API calls using mocked Bukkit state. They do not prove native inventory decrement, every mob's AI, client rendering, or third-party compatibility. The runtime and real-time-expiry acceptance evidence must identify this exact JAR hash before release.
7. **Durability gate still applies.** The parent's final bundle must be saved, materialized into a different directory, and hash-verified before installation. This review does not replace that receipt or authorize deployment from the only local copy.
