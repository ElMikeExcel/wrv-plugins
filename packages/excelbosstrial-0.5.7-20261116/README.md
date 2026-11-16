# ExcelBossTrial 0.2.10 — local source-publication candidate

**Local-only candidate; separate public-source approval is required.** No license supplied in the recovered source; none added here. This is the user’s custom plugin in the existing PR #98 source lineage. Preserve descriptor author **Argus** and original attribution. Existing public visibility does not itself grant recipients an open-source license. A new license grant would be a separate owner decision; its absence alone is not treated as a blocker to preparing this custom-source record. Third-party dependency binaries/source are not redistributed.

Complete production Java source and packaged defaults are preserved from the marker-fix candidate; existing tests plus team-reward tests, world-resolution tests and the real-Bukkit marker proxy regression are included. No JARs or dependencies are distributed. Marker protection inspects physical double-chest sides directly rather than recursively reopening combined inventories. Gameplay choices are unchanged. Marker world allowlists stay empty; these are build defaults, never replacement live configuration.

## Build and verification

Run `bash build-test.sh` with JDK 25, target `--release 21`, and `DEPENDENCY_CLASSPATH` containing the dependencies listed in DEPENDENCIES.sha256. The script compiles all production source rather than preserving baseline binary classes. It then compiles and runs every main-method harness. It writes ephemeral local build output only; no installation action is performed. Supply trusted dependency artifacts yourself; hashes identify the tested inputs, not permission to redistribute them.

This is a source record, not a byte-identical installed-JAR claim. Excel signature comments were prefixed on the same Java source line, and design-note comments in default configuration were generalized; the complete original-to-candidate changes are recorded in SANITIZATION.diff. No tested synthetic fixture was replaced. Compiler/debug metadata and different packaging can change output bytes. Existing installed/recovery artifacts remain private and untouched.

Prior isolated Paper startup and actual server installation are historical evidence and were not rerun for this public-copy preparation. Real-player reward acceptance remains separate. Preserve all reward ledgers and consumed signature scopes during any separately authorized rollback; never replay payouts automatically.
