# ExcelTelemetry 0.1.1 install record

Status: source record only. Deployment evidence is maintained separately. Real-encounter acceptance remains pending. This PR remains draft and unmerged.

- Candidate JAR: 56,598 bytes; SHA-256 `dd6472dcb9a3a7efd60ab4ace33e54c785ed59400039747961c930dd7170fee7`.
- Previous 0.1.0 rollback JAR: SHA-256 `11f51c7abcd19788a0fdc1badd8b284dbceb5ac65f11e64c044f6aaba0a234b3`.
- Source, tests, build instructions and private rollback backups verified separately. No player data or live configs included here.
- 9 suites /110 checks pass; independent compatibility review and29-check rerun pass. No live encounter dispatch validation yet.

Scope: observation-only phase-provider and unload compatibility. Preserve current config, all old JSONL, rewards, gameplay and stats. Do not reinstall retired DragonTrial. Empty-server restart only after backups/staged artifact verification.

Rollback: safely stop, restore previous exact JAR, preserve all logs/configs and start/check. Never rewrite collected events.

Acceptance: startup version0.1.1/provider=ExcelBossTrial, healthy event writer and integrations. Naturally occurring encounters later verify phase and unload/resume. Idle/shutdown cutoffs retain abandoned/failed_attempt semantics with explanatory markers; these are observation limits, not proof of player defeat. No player-facing tips/Guide changes required.
