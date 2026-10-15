# ExcelBuildMode 1.0 candidate — NOT DEPLOYED

Version 1.0 is the first custom two-part release label. The Bukkit plugin name remains `ExcelBuildMode`; no 0.1.1 artifact is produced. This is an offline candidate, not deployment approval.

## Implemented

- Added one synchronized, pure-Java reward-origin/admission ledger and frozen reflective bridge v1 using JDK-only signatures.
- Added exact manifest/revision registration for loaded Jobs, mcMMO and EliteMobs providers. An absent provider requires no handshake; a loaded unpatched or partially patched provider blocks new Build entry while ordinary Survival remains available.
- Captured immutable actor/farm eligibility, root generation and helper epoch. Denied origins remain denied after recovery and later sessions within the same epoch. Unknown handles fail closed; provider queues cannot translate handles to a replacement helper.
- Added reference-counted pending Survival work. Build admission refuses until prior allowed work drains. Recipient forks atomically reserve eligible recipients before split calculation; a preview does not reserve anything.
- Published durable recovery guards before provider handshakes at startup. Kept the ledger monitor released before game-mode changes, event-bearing callbacks, permission attachments and player-data save calls.
- Preserved the preexisting journal/inventory safety ordering. Failed preparation before journal commit leaves Survival rewards eligible; an ambiguous post-commit failure retains the recovery guard. Restoration releases it only after inventory restore, player-data save return and journal completion.
- Added a state-only guarded-actor query for cold incomplete providers, preserving ordinary Survival without claiming complete coverage. Added explicit token/session identity capacity failures, without evicting legitimate in-flight work.
- Added cold-start load order, provider-disable/hot-load refusal, and safe recovery of active builders after provider loss. Hot reload is unsupported.

## Intentionally unchanged

- No economy or skill rollback, broad PDC copying, paid-effect rollback, OP grants, user permission grants or global provider configuration changes.
- No generic reward replay or settlement engine. Each provider owns earning semantics, negative costs, native cancellation, deferred work and payout exactly-once behavior.
- Shared-world Creative blocks remain real blocks and can later be harvested. This helper is trusted-user assistance, not a provenance or anti-cheat boundary.

## Verification provenance

The inventory-only baseline JAR is retained under `baseline-0.1.0/` with SHA-256 `561a616a605c1b085d59e0ea4cb63f44d99c0470624e51c1a01d1b468da26a8a`. Its historical 5,709 core assertions and 150 exact-Paper assertions are separate from new candidate results. See `VERIFICATION-1.0.md` and the current output manifest for the final candidate evidence.
