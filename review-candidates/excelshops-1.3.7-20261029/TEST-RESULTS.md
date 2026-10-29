# Fresh sanitized derivative verification — 2026-10-29 UTC

PASS full production-source and test compilation, JDK 25 with --release 21 and supplied dependencies.
PASS 433 JUnit tests, 65 containers, zero failed/skipped/aborted.
PASS 66 GuardChecks coordinator checks including real subprocess crash/lock scenarios.
PASS 31 LifecycleCrashChecks paired-journal/coordinator subprocess checks.
PASS StatsPipeProbe: blocked analytics read preserves exclusive ownership and truthful timeout; releasing synthetic FIFO lets stats and queued task complete after close. Observed close duration about 5.03 seconds.

These are fresh tests against this packet including changed synthetic buyer/UUID fixtures and Excel signature comments. Expected fail-closed WARNING/SEVERE messages occurred in deliberate test scenarios; this is not an error-free-runtime claim. Compiler emitted deprecation/unchecked warnings; JVM emitted Byte Buddy/Unsafe warnings. The inherited dependency cache includes duplicate Byte Buddy versions and this run loaded 1.15.11; dependency inventory records available candidates rather than claiming only the documented nominal version loaded.

No Minecraft/Paper server, production environment or real client ran. No live 19-menu requalification, production economy durability, power-loss/disk-full acceptance or installed JAR equivalence inferred. No replacement JAR was packaged or deployed. Full build recipe requires separately supplied dependency binaries, including guarded ChestShop API; not a self-contained dependency-complete build. Tests for omitted historical packaging scripts were not run. Only compile/test output outside the public packet was produced.
