# ExcelSealAlerts 0.1.2 sanitized source record

Runtime-only rejection diagnostics remain OFF at startup; no packaged config key enables them. Existing HTTP400 delivery failure is unresolved. This record is not a webhook repair or delivery success claim. Packaged webhook URL/key are explicit placeholders and players.labels is empty.

Fresh verification: complete production compilation and 28/28 unit tests passed, including runtime diagnostic lifecycle/privacy tests and unchanged synthetic parser parity fixtures. Fresh public-source rebuild SHA256: `62f40480de63d63d6cc11ad2299cc85cc4dea9e0dab272dc38277f33eaa93e3b`. No live endpoint was contacted; HTTP tests use loopback fixtures.

The expected parity fixture and input were retained byte-for-byte, not silently regenerated. Original gen_expected.py was excluded because it imports absent external sealwatch.py and contains a private path. Its upstream generator source/licensing is unavailable in this packet; fixture regeneration is BLOCKED until that exact input is supplied/reviewed. Existing fixture-consuming tests passed. This gap does not block compiling/testing the included plugin but prevents claiming fully reproducible fixture generation.

## Build and publication boundaries
LOCAL REVIEW ONLY; not published, no PR created, no remote writes or live-server actions. Keep historical version records and PR100 separate. Source-only public approval is still required. A full source rebuild is possible with the supplied portable recipes and separately acquired exact dependencies in DEPENDENCIES.sha256; no dependency binaries are included. Set JAVA_HOME (tested JDK25), PAPER_LIBS_DIR (tested Paper26.2 build123), JUNIT_JAR (1.11.4), BUILD_DIR (new empty external absolute directory), then run bash build-test.sh and optionally bash package.sh. Scripts never install anything.

Public rebuilt outputs are not asserted byte-identical to installed artifacts: source signature comments, packaging timestamp/entry decisions, compiler/dependency environment and historical surgical binary reuse are distinct provenance. Test outputs are fresh local runs of this sanitized tree, not a live-server/client acceptance test. Baseline/private archives are excluded. No original file was changed.

## Sanitization and inputs
Only production Java, test Java, original packaged default resources and MIT license were selected. Inherited operational READMEs/build scripts were excluded because they contained private paths or stale candidate status. Portable replacement recipes are new packaging additions. Same-line Excel maintenance comments added to Java keep original line counts; no Java behavior or fixtures changed. SOURCE-PROVENANCE.json and SOURCE-CHANGES.patch record exact primary-source changes. Synthetic test names/coordinates/UUIDs are fixtures, not player data or production-world identifiers. No secrets, live configuration/state, logs of players, binaries, screenshots, or private operator paths included.

## Licensing
MIT license was present in both input trees and preserved verbatim; no new license invented. Existing Wink Reyes Valley author/copyright retained. Integrations do not confer dependency ownership. Dependency licenses are not bundled/relicensed. No Magic dependency is used by this plugin or its build/test recipe.
