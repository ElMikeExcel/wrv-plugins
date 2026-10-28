# Offline recovery core verification, October 27 2026

## Result
The paired offline candidates compile, pass their tests and pass real isolated Paper 26.2 console command/restart integration. This is not approval or evidence for a live deployment and not a real-player fight test.

- ExcelGuildGate 0.3.4: 507 checks across 10 suites: 442 retained, 30 coordinator, 35 actual-adapter/proxy/temp-filesystem checks.
- ExcelBossTrial 0.2.8: 1,075 checks across 31 suites: 1,023 retained and 52 recovery API/proxy/temp-filesystem checks.
- Independent current-code and narrow console-authorization review found no established critical mutation flaw. This is bounded review, not proof against every fault.
- Earlier 46 Java + 28 Python state/delivery model checks are separate and not counted as production integration.

## Real isolated runtime
`run-integration.py`, `integration-console.log`, `integration-results.json` contain the harness, raw log and nine observations. The fixture was a fresh local loopback-only Paper 26.2 server with only the two candidates. No live or copied player state was used.

1. Console preview/confirm completed a synthetic reviewed raid seal.
2. Wither recovery cleared its seeded deadline while retaining the unrelated Warden deadline.
3. Elder, final dragon and Warden reviewed seals each completed through their scoped APIs.
4. Readback showed global 5/5 and guild OPEN.
5. Clean stop/restart loaded the durable receipts.
6. Reusing a case with a changed reason was rejected as CONFLICT.
7. Same case and original reason after restart returned COMPLETED_HISTORY, without replay.
8. Existing synthetic Wither signature-paid flag was retained; recovery did not deliver rewards.
9. Both controlled server instances stopped cleanly. An earlier initial console session had lost writable stdin and was terminated before the controlled rerun. Its world lock was not removed or bypassed.

Real integration exposed a console permission bug that proxy checks missed. The final code explicitly authorizes actual ConsoleCommandSender only; players need both dedicated recovery and admin permissions, and command blocks/remote-console/generic senders remain denied. The final integration used the rebuilt corrected JARs.

Environment-only DNS failures occurred for Mojang public keys and Paper update checks. These were not plugin-load errors; do not describe the whole log as error-free. Exact live Paper 26.3, full plugin stack, actual platform storage behavior and player acceptance remain untested.

## Final JAR hashes
- ExcelGuildGate 0.3.4: `b65dc579e5f1126a9865b3a2450744bd41fd0f3a9ec198cb6904081fafc6268d`
- ExcelBossTrial 0.2.8: `ff72e4f7cc3b20aa8f6fab0be00435afcdf1ccf4333648aee5e3d65738469f05`

## Limits and delivery
See ROLLOUT-LIMITS.md and PACKAGING.md. In particular: stale pre-Boss-apply intents after restart need review; elder pending respawns block recovery; uncertain state writes lock further writes and demand prompt intervention; rollback must preserve post-operation receipts. Disk fingerprint checks do not make concurrent external editors transactional.

No rewards, chests, item/XP/currency recovery, difficulty changes or anti-cage changes were implemented. All artifacts remain local. No live grant, reset, installation, upload or PR was performed.
