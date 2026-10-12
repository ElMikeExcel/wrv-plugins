# Excel XP: Jobs source repair

**Source-only review candidate. Not installed; full Jobs build and award-path tests remain blocked.**

Version `5.2.6.3-Excel-XP-1` preserves the `Jobs` identity and existing data/configuration. Apply the patch to exact upstream Jobs tag `v5.2.6.3`, commit `6fe3c272c3c970d4a94fbf7615953803eb722503`. The compatible mcMMO candidate is `2.3.003-WRV-XP-3`; WeekendXP remains unchanged at `1.0.2`.

Read the [paired behavior, limitations and staging checklist](../mcMMO-TogetherBonus/JOBS-XP-CANDIDATE.md). This folder contains corresponding source changes and test instructions, not a deployment script or downloadable server plugin.

## Reconstruct source

From this repository's root:

```sh
git clone https://github.com/Zrips/Jobs.git Jobs-ExcelXP/target/upstream
git -C Jobs-ExcelXP/target/upstream checkout --detach 6fe3c272c3c970d4a94fbf7615953803eb722503
git -C Jobs-ExcelXP/target/upstream apply --check "$PWD"/Jobs-ExcelXP/patches/*.patch
git -C Jobs-ExcelXP/target/upstream apply "$PWD"/Jobs-ExcelXP/patches/*.patch
```

The patch adds `EXCEL-XP-PATCH.md` to the reconstructed source with a precise explanation of the upstream cached-award bug, repaired XP-only calculation, APIs and preserved currency behavior. Both upstream license texts are retained here; preserve complete corresponding source when distributing any future binary.

## Standalone bridge check

Use a full JDK 21 and run:

```sh
./mvnw -f Jobs-ExcelXP/verification/pom.xml clean test
```

This deliberately compiles only the real optional bridge and its tests against the repository's pinned Paper API. It does not need a Jobs/CMILib binary, compile `Jobs.action`/`Jobs.perform`, produce an installable plugin, or prove the actual job-level pipeline. Use `-o` if the ordinary Maven/test dependencies are already available.

## Full Jobs build and tests

The reconstructed upstream build uses its own original dependencies and repositories:

```sh
cd Jobs-ExcelXP/target/upstream
mvn clean test package
```

Use JDK 21 for the added JUnit/Mockito checks. Optional upstream integration dependencies must be available. The real award-path tests are in `src/test/java/com/gamingmesh/jobs/ExcelXpAwardTest.java`; verify the exact file name in the reconstructed checkout if it changes.

The current environment could not complete the full build: optional dependency repositories returned HTTP 403 and the official exact Jobs/CMILib binary fetch was blocked. An older cached Jobs artifact was rejected rather than substituted. No full Jobs test pass or release artifact is claimed. The separate bridge-only pass is not sufficient to authorize installation.

## Deployment precautions

- Keep current `Jobs` configuration and player data; this source patch never enables points or restores prior display settings.
- Money and points do not receive the new XP multiplier. Existing currency-limit accounting remains unchanged and needs review before enabling limits.
- Both matched candidates, full automated checks, isolated runtime tests, real Java/Bedrock acceptance and explicit installation approval are required.
- Do not hot reload or place duplicate Jobs/mcMMO JARs on the server.
