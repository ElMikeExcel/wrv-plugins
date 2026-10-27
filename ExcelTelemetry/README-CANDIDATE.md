# ExcelTelemetry 0.1.1 offline candidate
NOT INSTALLED. No live settings, gameplay, rewards or saved logs changed.

## Baseline proof
Original 0.1.0 backup JAR SHA256 11f51c7abcd19788a0fdc1badd8b284dbceb5ac65f11e64c044f6aaba0a234b3. Source rebuilt with matching class/resource bytes. JDK25.0.4 Created-By differs from historical25.0.4.1; restoring historical manifest yields exact whole-JAR match. Source ZIP SHA25620d669b979d6ebeb47318f5314b2d924f3a4d066ccb05983916c4e66e8cfcea6.

## Scope
- Active BossTrial preferred, legacy DragonTrial fallback, explicit none status.
- Read provider's HP configuration rather than independent stale defaults; disabled chapter is unknown.
- Optional opening phase reads BossTrial dragon/state.yml with matching dragon UUID; legacy root state.yml fallback. Reject stale/missing/finished phase and disabled chapter.
- Paper26.2 UNLOAD recognition preserves encounter until damage resumes or existing idle timeout. Unload marker does not refresh activity.
- Add idle_timeout and observer_stopped markers; unchanged event schema/outcomes otherwise.
- NO old log rewrite or historical phase reconstruction.

## Validation
Full compile with exact local Paper26.2 runtime dependencies and recovered mcMMO XP2 (SHA2565f6279995c7b30240d2a7e07cb916b6e28b82537826f9ef0f2702b080cf20978).9 suites/110 checks PASS. Independent review and independent29-check compatibility run passed. No actual Bukkit dispatch/live fight test.

## Build
Set JAVA_HOME to JDK25 and TELEMETRY_CP to colon-separated Paper26.2 runtime library JARs plus exact XP2 dependency. Run bash run-tests.sh then bash build.sh OUTPUT.jar. Does not use historical manifest for new version. Build timestamp fixed2026-10-27T00:00:00Z. Exact output depends on compiler/manifest version.

## Limits and acceptance
This remains an observer, not BossTrial's authoritative ledger. Existing idle/restart cutoff remains abandoned/failed_attempt, now marked explicitly; reports must not treat every row as a defeat. Canceled phase-flip hit is not observed; damage totals are not total health depletion. State read at encounter opening remains synchronous as before. Retention limited by existing configured idle interval, not indefinitely persisted across restarts.
Before deployment: fresh livebaseline/config check, verified backups, authorization, standard update checklist; no deprecated DragonTrial reinstall. Acceptance should use naturally occurring encounters only when appropriate, verify provider status and existing event streams first, then phase labels and unload/resume behavior. No fight need be created for deployment.

