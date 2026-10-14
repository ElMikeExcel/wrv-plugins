# Source publication record

This new standalone module is proposed in a separate draft PR against the verified current main branch. No existing module or pull-request branch is modified, and no JAR/server binary, private full configuration or live player data is committed.

## Verified release identity

- Durable release ZIP: ExcelMobVeil-0.1.0-VERIFIED-REBUILD.zip
- ZIP SHA-256: `0c943e2e38be65934522326ea2755e697241caa8490b71540bb6b19af000df74`
- Production JAR SHA-256: `66e7e50821b439f51a9c1882ce9e60d0011d97065b7b4ed26b407e81d35080fc`
- All 69 payload checksums were verified again after downloading the durable release.
- Fresh evidence: 180 unit/event tests; 150 native Paper assertions across ten modes, including the actual eight-minute single-potion soak. See VERIFICATION.md and TEST-RESULTS.json for scope and limits.

## Publication-only changes

Production Java, resources, Maven build, unit/event tests and runtime Java harness are byte-identical to the verified release. The bundled module files are kept at ExcelMobVeil; bundle evidence/runtime harness folders are nested within that module. The original source hash manifest is retained as verified-bundle-source-manifest.json.

Machine-local paths in logs and fixture provenance are replaced by a descriptive placeholder. Runtime shell/Python wrappers use explicit environment inputs instead of those paths, with their existing loopback and accepted-EULA checks retained. A private storage identity paragraph is omitted from the independent review; its findings and artifact hash are unchanged. Runtime documentation points to the actual published evidence locations. These changes do not alter the production build or reinterpret original evidence. An independent offline build of the public copy passed all 180 tests and reproduced the exact production JAR hash above. Wrapper syntax is checked; the original runtime results are not presented as a new execution of the portable wrappers.

PUBLICATION-MANIFEST.json records the public file hashes and each original release path/hash, identifying every exact or modified copy. It excludes itself to avoid a circular hash.

## Current status

This source PR is draft and must remain unmerged. Source publication, live installation, website deployment and real-client/live-purchase acceptance are separate outcomes. The installer confirmed this exact artifact and the new shop entry at 02:18 UTC on October 14. See INSTALLATION-0.1.0.md for the public-safe record and remaining unverified checks. The subsequent README/player-guide status changes and installed entry snippet are documentation-only additions; the production source/resources/tests remain unchanged.
