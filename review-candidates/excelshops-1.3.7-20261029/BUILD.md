# Offline build / test instructions

Use reviewed JDK 25 with `--release 21`. `bash build-and-test.sh` requires explicit environment variables:

- `JAVA_HOME`: reviewed JDK installation.
- `CP_MAIN`: complete existing main compile/runtime dependency classpath.
- `CP_TEST_EXTRA`: existing JUnit console, Mockito, Byte Buddy/agent, Objenesis and runtime logging dependencies.
- `MOCKITO_AGENT`: existing Mockito core JAR.
- `OUTPUT_DIR`: new nonexistent writable output directory outside this source packet. Existing directories are refused; nothing is deleted.

The script compiles main/test sources, copies packaged defaults into the isolated output, and runs JUnit plus all three subprocess suites. It downloads nothing, starts no Minecraft server and makes no JAR/deployment. Synthetic journal tests create only temporary fixture data.

Required dependency families: Paper API; VaultAPI 1.7; separate ChestShop 3.13-pre-1-build474 confirmation.1 API build; Adventure 4.20.0 and examination; option 1.0.0; Gson 2.11.0; Guava 33.3.1 and failureaccess; SnakeYAML 2.2; JOML 1.10.8; json-simple 1.1.1; fastutil 8.5.19; Brigadier 1.3.10; Bungee chat API; jspecify; slf4j; Log4j; JUnit console 1.11.4; Mockito 5.15.2; Byte Buddy/agent; Objenesis 3.3. DEPENDENCY-INVENTORY.json records exact existing classpath candidate files/hashes used for this check, not an assertion every JAR was loaded. The historical cache includes duplicate Byte Buddy versions; this is documented, not silently rationalized.

The original source archive did not contain all external runtime libraries or the separate guarded ChestShop binary. It was not standalone reproducible. This sanitized source remains dependency-incomplete by design. No upstream binaries or inherited operator-specific build paths are included. The old Maven POM referred to an absent parent and stale version; it and obsolete host-specific build/package scripts are omitted rather than presented as usable builds. Historical Python packaging tests belong to that omitted packager and are not claimed run here.

Do not install compiled output or infer installed binary equality from these tests. A future release needs separately reviewed packaging, dependency provenance, exact artifact comparison and explicit deployment authorization.

The third suite, StatsPipeProbe, requires POSIX mkfifo and exercises a synthetic blocked read with ownership retention. No server starts. Output is classes/tests only, not a deployment JAR.
