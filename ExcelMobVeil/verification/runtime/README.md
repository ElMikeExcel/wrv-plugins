# Freshly reconstructed MobVeil runtime harness

This is a new reconstruction after the earlier shared workspace disappeared. No prior assertion list or result is evidence for this candidate. Results must come from new executions against the exact newly built JAR.

The fixture copies the surviving approved leaderboard-runtime-prep server, its cached Paper 26.2 build 123, and its accepted eula.txt byte-for-byte. It uses the surviving cached Java 25. It binds only 127.0.0.1:25591, retains online mode and whitelist, uses native synthetic ServerPlayer/CraftPlayer objects with no-op packet sinks, and a fake Vault ledger. No downloads, software installation, new EULA acceptance, live players, real economy, or production state.

Run build_fixture.sh, put exactly one new ExcelMobVeil candidate JAR into server/plugins, and run run_fixture.py with modes stacking, edges, normal, purchase, lifecycle, restart, no-economy, review-races, soak. Restart must immediately follow lifecycle. The coexist mode additionally requires the verified approved DeathProtection baseline JAR; it is not a pass or supported compatibility claim unless that actual run completes.

Native runtime behavior includes actual item-use completion, inventories, main/offhand bottle handling, potion effects, registered event ordering, real scheduler, native death/reset, native saveData/loadData across JVMs, mob targets, and Warden anger. Review tests use real Bukkit command dispatch and InventoryClick/Drag events.

Farming, world transitions, and selected attack/target hooks are event-path tests. They are not a real client farming/teleport session. Synthetic players are temporarily listed online for the watchdog; they do not perform network login. No real Java/Bedrock rendering, Geyser, production economy, custom boss/plugin interactions, or power-loss durability guarantee. Observed thrown save failures are injected deliberately; silent disk errors remain a limitation.

The final single-potion soak really waits over 480 seconds. Shortened deadline tests do not substitute for that soak. A STOP_REQUESTED file triggers orderly console shutdown through the runner, but a stopped test is not a pass. Every run records its own candidate SHA and loopback-port release. Never distribute FixtureVault or MobVeilProbe as production plugins.

Final coexist execution used a reconstructed reviewed DeathProtection source baseline, not verified live-server JAR bytes. See ../../VERIFICATION.md and the final report for its exact source commit and new hash.

## Public checkout paths

Supply JAVA_HOME, VAULT_API_JAR and ANNOTATIONS_JAR for the exact verified cached dependencies before running build_fixture.sh. Supply BASELINE_EULA as the path to the already accepted, approved baseline eula.txt before run_fixture.py; the runner requires a byte-for-byte match and does not accept an agreement. Prepare only an authorized disposable fixture under server, with Paper 26.2 build 123, loopback binding and the whitelist retained. These environment inputs replace publication-redacted machine-local paths. The original recorded runs predate this path-only adjustment; no new runtime result is claimed for the portable wrapper. Per-run JSON, assertion lists and sanitized logs are under ../../evidence/runtime/.
