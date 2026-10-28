# ExcelBossTrial 0.2.8: reviewed seal recovery

Sanitized, unmerged draft source record for the paired GuildGate 0.3.4 / BossTrial 0.2.8 recovery core. This record does not authorize deployment or an administrative grant.

## Scope
Reviewed server-wide unlock for all five seals with only the selected BossTrial cooldown cleared. Raid is cooldown-N/A. Already-broken boss seals can preserve their global unlock while repairing the selected stale cooldown. Elder pending respawns block recovery. No rewards, chests, XP, currency, difficulty or anti-cage changes.

## Build and tests
Set JAVA_HOME to JDK 25 and BUILD_CP to the available Paper 26.2 API/runtime library JARs. GuildGate also needs compile-only PlaceholderAPI 2.11.6. BossTrial needs the exact baseline GuildGate 0.3.3 JAR for SealCreditEvent API. Then run `bash build.sh` and `bash run-tests.sh`. No dependencies or compiled binaries are committed. These portable scripts replace historical machine-specific build paths. JDK/dependency changes can alter byte-level output; compare the published candidate hash rather than assuming any rebuild matches.

Production Java source and bundled default resources are byte-identical to the reviewed candidate. Test-only personal-name fixtures are replaced with TestPlayer placeholders. No live configuration/state, player inventory/logs, credentials, private links or machine paths are included. Bundled config.yml is the build's default resource, NOT the live configuration and must not overwrite it. Existing public website URLs and product instructions remain unchanged.

## Administrative commands
Actual local server console is authorized. Players need both `excelguildgate.admin` and `excelguildgate.recovery`; command blocks, remote console and generic senders are denied.

`/guildgate recovery preview <seal> <case-id> <reason...>`

`/guildgate recovery confirm <token>`

Supported seal IDs: raid, wither, elder_guardian, ender_dragon, warden. Confirm only a reviewed win, using the original actor's short-lived token. Preview displays global action, reason and Boss snapshot. A case ID is immutable and completed receipts are terminal. There is no automatic grant or retry on startup.

## Install and rollback review
Deploy the pair only under explicit authorization, after fresh version/hash/player-state checks and verified private backups of both old JARs, configs and states. Do not replace live configs with these defaults. Preserve unrelated plugins and access state, verify startup and read-only status afterward. Installation alone must not execute recovery commands.

Before rollback inspect whether any recovery occurred. Preserve post-operation state/receipts; old versions can erase new receipts and restoring old state can undo grants or resurrect cooldowns. Do not blindly restore pre-recovery state. See ROLLOUT-LIMITS.md for stale intent, atomic-storage and external-writer limitations.

## Verification
See FINAL-VERIFICATION.md for separate unit/proxy/filesystem and isolated Paper 26.2 integration results. Exact live Paper 26.3/full-stack and real-player acceptance are separate. Publication timing does not establish deployment status.

Publication-copy verification: portable scripts were executed against the stated local dependencies; both reproduced the exact candidate JAR SHA-256. All discovered main-method test harnesses completed successfully after fixture-name sanitization. This adds no live-server or player-test claim.
