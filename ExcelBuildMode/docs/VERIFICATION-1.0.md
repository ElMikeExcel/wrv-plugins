# ExcelBuildMode 1.0 verification — NOT DEPLOYED

Current helper build at 2026-10-14 06:35 UTC, SHA-256:
`d45b847568deb7a682086414352ad96eacbd11c0e8cc02c91ed61aa9555445a7`.
Compiled against exact cached Paper 26.2 build 123 using Java 25. Custom version is quoted `1.0`.

## Completed current evidence

- Core journal/coordinator/policy: 5,709 assertions.
- Reward ledger/admission/concurrency: 5,283 assertions, including 1,000 admission races. Counts are assertions, not end-to-end plugin scenarios.
- Scoped cohort, storage budget, independent index, review holds and crash ordering: 84 assertions.
- Native gzip/NBT parsing bounds, UUID/mode/marker identity, file force and detectable replacement: 145 assertions.
- The first normal-mode run returned 95 assertions but exposed an old duplicate helper JAR during the next fault test. Its artifact attribution is INVALID. These 95 checks are not evidence for the current recovery build; corrected loaded-class checks and a rerun are required.

Build log: target-build-recovery-0635.log. Native logs and detailed assertions are saved separately. The probe uses genuine CraftPlayer/native serialization and a synthetic network connection; it is not a real player client.

Synthetic provider fixtures register Jobs/mcMMO/EliteMobs coverage ONLY to exercise helper admission. They contain no actual provider reward paths, so helper adapter checks do not certify those plugins.

## Pending targeted cases

The compiled native probe adds disabled native saving before entry, during restored-marker acknowledgement, and final marker removal; a real native filesystem-save failure swallowed by Paper; missing journal/cohort with ordinary Survival availability; and restart retaining UNKNOWN review classification. Execution results will be appended only after terminal output. These probe additions do not change the helper artifact above.

Clean JVM restart from persisted Creative, reward-provider startup ordering, menus and paid-effect coexistence must be rerun against this exact helper. Historical old-helper runtime evidence is not credited to 1.0. Exact live MenuJoin 1.2.0 and LuckPerms/WorldEdit attachments remain separate.

## Open release gates

1. Actual Jobs, mcMMO and EliteMobs integrations must finish producer attribution, delayed/shared awards and lifecycle tests before complete coverage registration. Entry stays blocked with any loaded unpatched/partial provider. Registration alone is an attestation, not proof.
2. Finish native silent-save, fallback and restart cases. Coherent backup/restore and power-loss assumptions still require an operational plan; no cross-file atomicity is promised.
3. Installed menu/compass behavior, real Java/Bedrock interaction and optional WorldEdit edit completion remain separate gates. WorldEdit is off and absent by default.
4. Forced actual death can consume DeathProtection/MobVeil entitlements under native policy. The helper never resurrects paid PDC or rewinds expiry timers. Prior coexistence fixtures have documented artifact limitations.
5. Hot reload/helper replacement and persisted vendor queues without a durable origin protocol remain unsupported. Unknown/old-epoch handles cannot become allowed.
6. Production backup/account quota and precise access/install approval remain required.

An earlier new-candidate launch had no compiled probe and later suffered a shared JVM stall; it produced no runtime assertions. Current scripts retain stdin, disable perf-data, restrict resources and own a bounded stop/termination path. The initial 95-assertion attempt also cannot be attributed to this artifact because of the duplicate helper.

## Corrected final-artifact native results, 06:54 UTC

After quarantining the duplicate helper JAR, each run's preflight requires exactly one ExcelBuildMode descriptor and matching original JAR SHA. The runtime probe additionally compares the loaded helper class bytes with that JAR and requires the scoped review-hold field. Exact class SHA: 6f1d72bb0bc656ddfbf85f86f9c6230117becc67b5f10c988d66026087525379.

- Normal: 97 assertions
- Native disabled-save entry/restoration/clear and real swallowed filesystem-save failure: 73
- Missing journal/cohort scoped joins: 13
- Independent UNKNOWN/review-hold restart: 14
- Active Creative session seed: 5
- Separate JVM reward-aware restore: 15

All six owned children exited 0. The native filesystem fault log contains Paper's failed-player-save warning. The missing-storage tests preserved unrelated verified Survival availability, paid-state PDC and timer values while retaining the affected identity's review hold. The restart provider checks use synthetic handshake fixtures, not actual reward plugin implementation. Detailed preflight hashes, logs, assertions and invalid earlier diagnostics are in verification/current-recovery-evidence/. These results supersede only the listed pending cases above; full-stack, real-client and complete provider gates remain open.

## Added release blocker: capacity exhaustion
Current provider exception handling can make token exhaustion sticky and deny new ordinary Survival rewards after capacity returns. No full pilot approval should be requested until the negotiated capacity-outcome/diagnostic policy is tested and refused awards are surfaced or safely retained. The hard limits and smallest proposed contract increment are documented in proposals/CAPACITY-RELEASE-GATE.md. Existing snapshot/storage tests do not resolve this separate reward-resource issue.
