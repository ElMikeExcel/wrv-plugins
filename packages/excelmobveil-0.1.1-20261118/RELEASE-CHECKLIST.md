# Required durability gate

Local workspace files are disposable. Do not call a build ready or start installation from the only local copy.

1. During implementation, run release_checkpoint.py snapshot OUTPUT.zip --label "in-progress source; not verified". Save this ZIP to ChatGPT Library immediately. Record the returned canonical Library file ID and version. This step does not imply a passing build.
2. Run a fresh clean build, unit tests, independent review, isolated native Paper matrix and the real-time expiry test. Record the exact candidate JAR SHA-256 on every runtime result. Never inherit results or checksums from a lost or different build.
3. Package final source, JAR, tests, review, test reports, integration instructions and SHA256SUMS. Save the final bundle to Library before waiting or ending the task. Preserve the earlier checkpoint history.
4. Materialize the saved final Library version into a different directory. Run release_checkpoint.py verify-download DOWNLOADED.zip EXPECTED_SHA --library-id RETURNED_ID --receipt receipt.json. Extract the downloaded JAR and verify its hash against the final manifest and runtime results.
5. Save the receipt/checksums durably and pass the canonical Library ID, exact version and verified hashes to the publishing/installing task. Install only those verified downloaded bytes. A local path or earlier chat statement is not a durability receipt.
6. If upload, readback or a hash check fails, stop publication/installation and report the blocker. Rebuilding creates a new candidate requiring new verification; it cannot inherit the missing artifact's claims.

The script packages and verifies bytes. Library saves and materialization must use the supported current Library tools; no credentials or storage URLs are written into this repository.
