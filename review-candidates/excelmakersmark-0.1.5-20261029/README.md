# ExcelMakersMark 0.1.5 sanitized source record

Current lookup-prefilter code, retaining the 0.1.4 preference failed-rename fix. Original plugin identity and descriptor preserved. Historical CHANGELOG is preserved as a packaged resource, not claimed current release notes.

Fresh verification: complete production compilation succeeded (one existing deprecated API warning), 40/40 core tests, 44 exact-method baseline/candidate outputs and ambiguity effects identical, and 10 actual Prefs failure/retry checks. Prefs.java before header is byte-identical to the reviewed 0.1.4 minimal fix source; only preference temporary rename failure behavior is preserved, not re-opened. The latter two suites use synthetic stand-ins; no real player/client acceptance claimed. Run regression/run-lookup.py with JAVA_HOME and BUILD_DIR; run bash regression/run-prefs.sh with JAVA_HOME, PAPER_LIBS_DIR, BUILD_DIR.

For build-test.sh also set PAPI_JAR (2.11.6) and MAGIC_JAR (custom reviewed API dependency hash recorded). Fresh public-source rebuild SHA256: `5df72e4df3f89117b982f35cb9f6e01c912df4a9ab33f1a9f704b1cd7c13003f`. Installed candidate historical hash: `5cb6a71771f1bde24eafa5d7ac8ce7a4f046de4da79445012ed6db3ef1104319`. Different artifacts by design; no installation authorized.

## Build and publication boundaries
LOCAL REVIEW ONLY; not published, no PR created, no remote writes or live-server actions. Keep historical version records and PR100 separate. Source-only public approval is still required. A full source rebuild is possible with the supplied portable recipes and separately acquired exact dependencies in DEPENDENCIES.sha256; no dependency binaries are included. Set JAVA_HOME (tested JDK25), PAPER_LIBS_DIR (tested Paper26.2 build123), JUNIT_JAR (1.11.4), BUILD_DIR (new empty external absolute directory), then run bash build-test.sh and optionally bash package.sh. Scripts never install anything.

Public rebuilt outputs are not asserted byte-identical to installed artifacts: source signature comments, packaging timestamp/entry decisions, compiler/dependency environment and historical surgical binary reuse are distinct provenance. Test outputs are fresh local runs of this sanitized tree, not a live-server/client acceptance test. Baseline/private archives are excluded. No original file was changed.

## Sanitization and inputs
Only production Java, test Java, original packaged default resources and MIT license were selected. Inherited operational READMEs/build scripts were excluded because they contained private paths or stale candidate status. Portable replacement recipes are new packaging additions. Same-line Excel maintenance comments added to Java keep original line counts; no Java behavior or fixtures changed. SOURCE-PROVENANCE.json and SOURCE-CHANGES.patch record exact primary-source changes. Synthetic test names/coordinates/UUIDs are fixtures, not player data or production-world identifiers. No secrets, live configuration/state, logs of players, binaries, screenshots, or private operator paths included.

## Licensing
MIT license was present in both input trees and preserved verbatim; no new license invented. Existing Wink Reyes Valley author/copyright retained. Integrations do not confer dependency ownership. Magic upstream creator NathanWolf is credited in ATTRIBUTION.md. Dependency licenses are not bundled/relicensed. Public availability of a matching custom Magic dependency is a reproduction prerequisite, not a claim made here.
