# Repository repair report — 2026-10-12

## Scope

Repair source loss, case-colliding project layout, unusable local-cache build assumptions, and concrete defects found during recovery. Changes are candidates for a draft pull request. No live server, deployment, restart, economy settings, player data, upstream mcMMO patch content, or production Git branch was changed by this repair.

## What changed

### Source and history

- Restored the deleted NoPhantoms, RestartCountdown and WealthBoard implementations, descriptors and documentation into PascalCase folders.
- Restored original readable BedrockMenu, Leaderboard and WeekendXP Java sources from verified Git history. Kept their archived recovery material intact.
- Preserved newer MenuJoin 1.2.0 recovery mechanics and PDC keys, ShopVillager, Spawn Shop menus and the two mcMMO Together bonus patches.
- Merged the actual BedrockMenu 1.4.2 Spawn Shop change (`shop`) into readable source. Corrected documentation that incorrectly described the command as `dm open spawnshop`.
- Restored the root README with current source versions, exact build target and working canonical links; no capitalization-colliding folders were reintroduced.

### Behavior repairs

| Component | Repair |
| --- | --- |
| MenuJoin 1.2.1 | Full-inventory compass delivery no longer drops leftovers into the world, preventing repeated `/compass` duplication. Messages distinguish existing compass, successful delivery and full inventory. Delayed join opening rechecks the current opt-out toggle. PlaceholderAPI reports the real plugin version. |
| BedrockMenu 1.4.3 | Uses EssentialsSpawn's actual `getSpawn(String)` API. Fallback selects a loaded overworld instead of the player's current dimension; an optional explicit world fails closed if missing/wrong dimension. Preserves village coordinates, checks standing-height bounds and reports unsuccessful teleports truthfully. |
| RestartCountdown 1.1.1 | Adds the intended reason-bearing on-screen titles alongside chat, including the minute warning and 10-to-1 sequence. Cancellation now covers the final 40-tick shutdown grace period; duplicate countdowns are blocked and disable cancels pending tasks. The recovered two-minute warning duration is retained. |
| WealthBoard 1.3.1 | Skips delayed enrollment after logout or when Jobs is disabled, isolates the optional Jobs linkage, preserves signed/localized Vault currency formatting, rejects non-finite balances and orders ties predictably. |
| WeekendXP 1.0.1 | Applies weekday reset on initial check instead of assuming the prior runtime rate is correct. Failed dispatches are retried instead of marked applied. Declares mcMMO load dependency and uses its namespaced command. Keeps Chicago time, 1.2× weekend policy and the original 30-minute check interval. |
| ShopVillager 1.0.1 | Tagged existing villagers receive the documented invulnerability; logout and disable clear per-player click cooldown bookkeeping. Preserves interaction debounce and vanilla trade-window prevention. |
| Spawn Shop generator | Rejects nonpositive, non-finite and overflowing price multipliers before overwriting output, makes import safe and supports an explicit output path. Default output remains byte-for-byte identical; catalog, base prices and purchase commands are unchanged. |

NoPhantoms 1.0.0 and Leaderboard 1.0.0 behavior was retained and tested.

### Build and checks

- Added one Maven reactor for all eight plugin modules, with explicit API/test/build-plugin versions and full transitive compile dependencies.
- Replaced machine-specific shell classpaths with Maven-backed module wrappers. A checksum-verified Maven 3.9.9 bootstrap avoids requiring pre-existing local dependencies.
- Targets Java 21 with `--release 21` and a timestamp-pinned Paper 1.21.4 API. Descriptors state minimum API 1.21; no live-server version is inferred.
- Configured Mockito as an explicit test JVM agent so tests do not rely on unsupported self-attachment.
- Added behavior tests, repository/JAR audits, case-collision checks and original recovery-blob integrity checks.
- Added read-only GitHub Actions verification and candidate-artifact upload. CI does not deploy.
- Kept BedrockMenu's historical 1.4.2 ASM rebuild separate from the maintained source build, with pinned ASM retrieval and an explicit local-JAR override. Its rebuilt payload was checked for four Java 21 classes, the original descriptor and all eight menu mappings.

## Verification

The top-level build and a second clean Maven build both passed. The Maven wrapper was exercised from an empty wrapper cache; normal dependency resolution was used. Two clean builds in the same environment produced byte-identical JARs for all eight modules; their SHA-256 values are in [verification/artifacts.sha256](verification/artifacts.sha256).

- All eight source modules compile against the managed APIs; the clean reactor ran **65 JUnit tests** with zero failures/errors/skips after fixture fixes.
- **5 Python repository/artifact tests**: main-class and descriptor/version agreement, Java 21 bytecode, config resources, no bundled test/API classes, no case collisions and original recovery assets unchanged.
- Spawn Shop: **9 Python tests**, including byte-identical default generation and invalid-input no-overwrite checks.
- Historical BedrockMenu: the unmodified recovered 1.4.1 source compiles with JDK 21. All four normalized disassemblies match the archived classes' compared instructions, signatures, constants and bootstrap arguments. See [SOURCE_RECOVERY.md](BedrockMenu/SOURCE_RECOVERY.md) for exact methods and limits.
- Both mcMMO patches apply cleanly, in order, to README-pinned upstream commit `058279c148a65bd708dc85067d3858f6adde1173`. The patched upstream source/tests compile and **19 focused PartyTogetherBonus tests** pass with zero failures/errors/skips. This is separate from the eight-plugin build; the full upstream test suite was not run.
- Shell syntax, Python syntax and Git whitespace checks pass.

Jobs v5.2.6.2 publishes a POM containing invalid local system-path entries for two optional dependencies. Maven reports those upstream metadata warnings; this build excludes those transitives and uses the provided Jobs API JAR successfully. No credentials, fake API stubs or copied server libraries were needed.

## Limits and deliberately deferred work

- No live Paper or Java/Bedrock client session was run. Unit/mocked tests are not runtime integration proof. Installed JARs were not compared with the repaired artifacts.
- Retained TeleportMenu behavior can show its optional prompt after a handled `/tpa` command even if Essentials rejected the request. An Essentials pending-request API rewrite and large-server pagination were not added during source recovery.
- Spawn fallback keeps the recovered surface-height policy; staging must check actual terrain and protection-plugin behavior.
- RestartCountdown performs shutdown only. Host/supervisor restart behavior requires separate validation.
- Bukkit command dispatch success does not prove mcMMO's effective rate changed. Weekend transitions can lag by up to 30 minutes, as before. Initial reset repairs stale same-process/reload state; it is not evidence that rates persist across a full server restart.
- Existing enchanted-book give syntax and installed Jobs/Geyser/Essentials compatibility need the actual server version and staging checks. Shop prices and Minecraft item syntax were deliberately not guessed or redesigned.

Use [TESTING.md](TESTING.md) before approving deployment. The repository is prepared for the next development ideas; production rollout remains a separate reviewed step.
