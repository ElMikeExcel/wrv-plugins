# BUILD-RECORD — ExcelWeekendXP 1.3.0 (EliteMobs skill-XP leg, decision D155)

- Worker: build worker, parent-seat dispatch, Fri Nov 20, 2026, started ~9:08 AM CST
  (VM clock 15:08 UTC). One worker, whole unit.
- Scope (Mike's direct Nov 20, 9:03 AM CST + decision D155 in
  `~/workspace/goals/family-minecraft-server-build/hidden_files/decision-log.md`):
  the Fri-Sun 1.2x weekend bonus must also cover EliteMobs skill XP. 1.2.1 has
  exactly two legs — mcMMO via console `mcmmo:xprate 1.2 true` / `reset`, and
  vanilla green-bar XP via `VanillaXpMultiplier` (a `PlayerExpChangeEvent`
  listener). 1.3.0 adds a third leg on EliteMobs' own API event
  `com.magmaguy.elitemobs.api.EliteSkillXpGainEvent`: on weekend days, combat
  EM skill-XP gains scale to `round(xp * 1.2)`, never reduced; weekdays the
  event is left untouched.
- By construction (D155): programmatic grants (`PlayerData.addSkillXP` —
  ExcelQuestXP payouts, quest backpay) do NOT fire `EliteSkillXpGainEvent`, so
  exact-amount payouts stay exact. No grant-path code is added. No world
  filter: the event only fires on elite kills (overworld timed-event elites
  count too, per D121).
- Claim (written first, before any code): 1.3.0 = 1.2.1 source + one new class
  `EmSkillXpMultiplier` (listener mirroring `VanillaXpMultiplier` semantics) +
  `WeekendSchedule.apply` extended to drive all three legs with the existing
  APPLIED/FAILED/UNCHANGED all-or-nothing discipline + conditional listener
  registration in `onEnable` (only when EliteMobs is present and enabled) +
  `softdepend: [EliteMobs]` in plugin.yml + version 1.3.0. The mcMMO and
  vanilla legs' behavior stays identical. Everything below was measured after
  this claim was written.
- Source base: `~/workspace/weekendxp-121-build-20261119/src` (copied into
  this folder's `src/`; the 1.2.1 tree is not modified).
- NOT filed to the MANIFEST (supervisor files after verifying this record).
  NOT installed. No live placement, no panel, no MANIFEST access by this worker.

## Plan

0. LIVE PREMISE (before code): fresh-pull the live EliteMobs jar and the live
   ExcelWeekendXP jar via the Homebase route (ssh mike-pc, rclone remote
   `shockbyte:`, paths under `1. Michael's Server/plugins/`). Record
   name/size/sha256 for both. Verify (a) live WeekendXP jar hash == the 1.2.1
   build hash `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`
   (15,148 B); (b) `EliteSkillXpGainEvent` + `Reason.COMBAT` exist in the LIVE
   EliteMobs jar. Either check fails -> STOP, record the failure, report.
1. Implement per the claim; compile against the LIVE EliteMobs jar (Step-0
   pull) so the API surface compiled against is the live one.
2. Tests: extend the suite in its own style — EM-leg unit tests + three-leg
   WeekendSchedule transition tests + plugin wiring tests (EM present/absent).
   All old tests must pass unmodified.
3. Deterministic double-build; the two jars must be byte-identical.
4. Rig boot following the 1.2.1 rig pattern. The 1.2.1 rig carries an
   `EliteMobs` data folder but NO EliteMobs jar; this build's rig adds the
   Step-0 live EliteMobs jar so the registration path can be proven from the
   boot log. If EM will not enable on the rig, prove the no-EM softdepend
   path instead and say plainly which path was proven. NOTE: today is Friday
   (a weekend day, Chicago), so a rig boot exercises the 1.2x engagement path
   for all legs — unlike the 1.2.1 rig, which booted on a Thursday.
5. Deliverables: this record, `ExcelWeekendXP-1.3.0.jar`,
   `ExcelWeekendXP-1.3.0-source.zip`, `SHA256SUMS`, `INSTALL-PACKET.md`
   (PACKET-TEMPLATE shape, lint numbers in header), `RECEIPTS.md`.

## Step 0 — live premise (fresh pull via Homebase) — PASS

- Pull: Fri Nov 20, 2026, 15:08 UTC (9:08 AM CST), Homebase route
  (ssh mike-pc -> rclone copyto from `shockbyte:1. Michael's Server/plugins/`
  -> scp to `live-pull/` in this folder).
- Live `plugins/ExcelWeekendXP-1.2.1.jar` = 15,148 B / SHA-256
  `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`.
  CHECK (a) PASS: byte-identical to the 1.2.1 build-of-record hash.
- Live `plugins/EliteMobs-10.9.10.jar` (file name lags; its plugin.yml says
  version 10.9.14) = 14,563,211 B / SHA-256
  `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`.
  CHECK (b) PASS: `javap` on the pulled jar shows
  `com.magmaguy.elitemobs.api.EliteSkillXpGainEvent` — `public final class`,
  extends `org.bukkit.event.Event`, implements `Cancellable`; methods
  `getPlayer()`, `getSkillType()`, `getEliteEntity()`, `getReason()`,
  `getOriginalXp()`, `getXp()`, `setXp(long)`, `isCancelled()`,
  `setCancelled(boolean)`; nested enum `EliteSkillXpGainEvent$Reason` has
  exactly one value, `COMBAT`.
- Cross-check: both pulled jars are byte-identical (same SHA-256) to this
  morning's independent audit pull
  (`~/workspace/plugin-audit-2026-11-20-rerun/pull/20261120-062811/plugins/`).
- The pulled live EliteMobs jar is the compile classpath for the EM leg
  (copied to `libs/EliteMobs-10.9.10.jar` in this folder) and the rig jar.
- Premise holds; building proceeds.

## Implementation (1.2.1 -> 1.3.0)

- ADDED `EmSkillXpMultiplier.java`: `Listener` holding a volatile double
  multiplier (default 1.0), mirroring `VanillaXpMultiplier` semantics exactly.
  `applyMultiplier(double)` validates finite and >= 1.0 (throws
  `IllegalArgumentException` otherwise) and returns true. Handler
  `onEliteSkillXpGain(EliteSkillXpGainEvent)` at `EventPriority.HIGHEST` with
  `ignoreCancelled = true`: returns untouched when multiplier <= 1.0 or
  xp <= 0; otherwise `scaled = Math.round(xp * multiplier)` (long) and
  `setXp(scaled)` only when `scaled > xp` — never reduces.
- `WeekendSchedule.java`: new 4-arg `apply(instant, dispatch, applyVanilla,
  applyEmSkill)` drives all three legs in order (mcMMO command -> vanilla ->
  EM) under the existing all-or-nothing discipline: any leg returning false
  -> FAILED with the cached state invalidated (`lastAppliedWeekend = null`
  before the legs run), so the whole transition retries at the next check.
  The pre-1.3.0 3-arg `apply` is kept as a delegating overload (EM leg =
  no-op true), so the 1.2.1 tests compile and pass unmodified and the
  two-leg behavior is provably unchanged.
- `ExcelWeekendXP.java`: constructs `EmSkillXpMultiplier` and registers it
  ONLY when `getPlugin("EliteMobs")` is present and `isEnabled()`; otherwise
  logs one info line ("EliteMobs not present or not enabled: running with
  the mcMMO and vanilla XP legs only.") and passes a no-op EM predicate to
  the schedule. On APPLIED with the EM leg active, the verified line gains
  "Verified EliteMobs skill XP event multiplier 1.2x." (or 1.0x). The
  mcMMO/vanilla wording is otherwise byte-identical to 1.2.1.
- `plugin.yml`: version 1.3.0; description now names all three legs; added
  `softdepend: [EliteMobs]`; `depend: [mcMMO]` unchanged.
- NOT changed: `VanillaXpMultiplier.java`, `LegacyDataMigration.java`, the
  `mcmmo:xprate` command strings, the 30-minute check cadence, any config
  (the plugin has none). No grant-path code anywhere (grep-verified below).
- API finding from the test run (real event objects, live jar): the
  `EliteSkillXpGainEvent` constructor itself rejects negative XP —
  `IllegalArgumentException: Skill XP must be non-negative`, thrown by
  `setXp` from the constructor — so a negative gain can never reach the
  listener; the handler's `xp <= 0` guard only ever covers the zero case.
  `setXp` does not rewrite `getOriginalXp()` (asserted in tests).

## Candidate build (1.3.0) — deterministic double-build, byte-identical

- Build 1: 18,941 B / SHA-256 `a9f161355907033c50d20246ed47cd8360641631f7033bd21dcbb84e9de11b96`
- Build 2: 18,941 B / SHA-256 `a9f161355907033c50d20246ed47cd8360641631f7033bd21dcbb84e9de11b96`
- Byte-identical: YES (`cmp` clean). Script: `build-and-test.sh` in this
  folder (1.2.1 toolchain: JDK 25, `--release 21`, deterministic
  `make-jar.py`; classpath adds the Step-0 live EliteMobs jar).
- Jar contents: `META-INF/MANIFEST.MF`, `EmSkillXpMultiplier.class`,
  `ExcelWeekendXP.class`, `LegacyDataMigration.class`,
  `VanillaXpMultiplier.class`, `WeekendSchedule.class`,
  `WeekendSchedule$Result.class`, `plugin.yml` (verified via `unzip -l`).
- Source zip: `ExcelWeekendXP-1.3.0-source.zip`, 16,945 B / SHA-256
  `6f1737d27bebfc1cd75dee673f9a50aac46436526389da7b7d8fbe08557c2c7a`
  (fixed zip timestamps 2026-11-20 09:15 CST; layout mirrors the 1.2.1 zip:
  `src-1.3.0/` with `plugin.yml`, `src/`, `tests/`). See `SHA256SUMS`.

## Tests — 38/38 pass on BOTH builds (8 containers, 0 failed)

Old suite: 25 tests (the 1.2.1 count), all passing UNMODIFIED —
`VanillaXpMultiplierTest` 5, `LegacyDataMigrationTest` 5, plus the original
11 `WeekendScheduleTest` and 4 `ExcelWeekendXPTest` tests (the plugin tests'
fixture leaves `getPlugin("EliteMobs")` unstubbed = EM absent, so they also
prove the absent path registers exactly one listener and keeps every 1.2.1
log assertion). New suite: 13 tests.

| Class | New tests | What they prove |
|---|---|---|
| `EmSkillXpMultiplierTest` | 5 | Weekend 1.2x scales real `EliteSkillXpGainEvent`s: 10->12, 5->6, 3->4 (round), 100->120; `getOriginalXp()` untouched by scaling; 1->1 never reduced; 0 stays 0; negative XP rejected by the EM constructor itself; weekday 1.0x leaves 1/3/7/10/100/1000 exactly untouched and 1.2->1.0 returns to no-op; guard rejects 0.0/0.99/-1.0/NaN/±Inf and leaves the live rate at 1.2; exactly one `@EventHandler`, taking `EliteSkillXpGainEvent`, priority HIGHEST, `ignoreCancelled = true` |
| `WeekendScheduleTest` (three-leg) | 6 | All three legs transition together in order (command -> vanilla -> EM) across Thu->Fri->Mon; failed command -> EM never invoked; failed vanilla -> EM never invoked; failed EM -> FAILED, retry re-runs ALL legs (command dispatched twice) and only then caches (then UNCHANGED); throwing EM leaves all legs retryable; a partial three-leg transition cannot reuse an old successful state |
| `ExcelWeekendXPTest` (wiring) | 2 | EM present + enabled -> two listeners registered, the EM listener's multiplier matches the dispatched command leg, "EliteMobs detected" + "Verified EliteMobs skill XP event multiplier" logged; EM present but disabled -> exactly one listener, absent-path info line, vanilla verified line still logged |

No-double-count / no-grant-path evidence: the EM leg listens only for
`EliteSkillXpGainEvent`; `grep -rn "addSkillXP\|PlayerData" src/` returns
exactly 2 hits, both explanatory comments (the `EmSkillXpMultiplier`
javadoc and its test) stating that grants never fire the event — no code
calls any grant API.

## Rig proof — BOTH paths booted clean on a real server (Fri Nov 20, 2026)

- Rig: copy of the 1.2.1 rig (Paper 26.2-123, Java 25) at `rig/` in this
  folder; plugins: rig originals + live patched mcMMO
  `mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` (3,721,966 B / SHA-256
  `404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501`,
  hash-verified in place) + candidate `ExcelWeekendXP-1.3.0.jar`
  (hash-verified in place, `a9f16135...`). Today is Friday — a weekend day
  in Chicago — so both boots exercised the 1.2x engagement path. Both boots
  were stopped cleanly via console `stop`, JVM exit code 0.

### Boot 1 — EliteMobs present (registration path) — PROVEN

- Added the Step-0 live EliteMobs jar to `rig/plugins/` (hash-verified in
  place, `4a6bc4a7...`). Boot Nov 20, 15:14 UTC. Log preserved as
  `rig/logs/boot1-with-em.log`, SHA-256
  `50095af0691373f38dd48ec7eb73b282a8ff3be8c47449a3c95c090158a138f8`.
- Boot lines (verbatim, `rig/logs/boot1-with-em.log`):
  - `:143` `[EliteMobs] Enabling EliteMobs v10.9.14`
  - `:155` `[mcMMO] Enabling mcMMO v2.3.003-SNAPSHOT`
  - `:162` `[ExcelWeekendXP] Enabling ExcelWeekendXP v1.3.0`
  - `:163` `[ExcelWeekendXP] EliteMobs detected: EliteMobs skill XP weekend multiplier leg enabled.`
  - `:166` `[ExcelWeekendXP] Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x. Verified EliteMobs skill XP event multiplier 1.2x.`
  - `:181` `Done (41.809s)!`
- SEVERE: 0. ERROR: 7 lines, NONE naming ExcelWeekendXP, EliteMobs, or
  mcMMO — the same pre-existing rig noise as the 1.2.1 boot (PaperVersion
  Fetcher x3, ExcelBuildMode storage x3, Yggdrasil key fetch x1; sandbox
  offline). Zero ExcelWeekendXP WARN/ERROR/SEVERE lines; zero
  `schedule incomplete` / `bridge` hits.

### Boot 2 — EliteMobs absent (softdepend path) — PROVEN

- Same rig with the EliteMobs jar removed. Boot Nov 20, 15:16 UTC. Log:
  `rig/logs/boot2-no-em.log` (also the rig's current `logs/latest.log`),
  SHA-256 `683b0818429c62235c019cba6b4e520e4dd3ec1a7b2f66ae37d4e130ec6481fb`.
- Boot lines (verbatim, `rig/logs/boot2-no-em.log`):
  - `:143` `[ExcelWeekendXP] Enabling ExcelWeekendXP v1.3.0`
  - `:144` `[ExcelWeekendXP] EliteMobs not present or not enabled: running with the mcMMO and vanilla XP legs only.`
  - `:147` `[ExcelWeekendXP] Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x.`
  - `:162` `Done (40.241s)!`
- SEVERE: 0; zero ExcelWeekendXP WARN/ERROR/SEVERE lines. Exactly one info
  line for the absent EM leg — no crash, no error spam — and the two
  existing legs run exactly as in 1.2.1. The rig was restored to the
  boot-1 configuration afterwards (EliteMobs jar back in `rig/plugins/`).

### What the rig did NOT prove (honest limits)

- No player was on either rig boot, so no live `EliteSkillXpGainEvent` was
  fired on a server; in-game scaling is proven at unit level against real
  event objects from the live jar (38/38 tests) and is owed as player proof
  after install (see INSTALL-PACKET.md).
- The 30-minute re-check did not elapse during either ~1-minute boot.
- The rig runs Paper 26.2; live runs Paper 26.3 — same API level for this
  plugin (api-version 1.21). The weekday 1.0x path was exercised by the
  1.2.1 rig boot (Thursday) and is covered here by the schedule/listener
  unit tests.

## Files in this folder

- `BUILD-RECORD.md` (this file)
- `ExcelWeekendXP-1.3.0.jar` — candidate, 18,941 B / `a9f16135...`
- `ExcelWeekendXP-1.3.0-source.zip` — 16,945 B / `6f1737d2...`
- `SHA256SUMS` — jar + source zip hashes (`sha256sum -c` passes)
- `INSTALL-PACKET.md` — file-ready packet (supervisor files; this worker
  did not file), lint 5570 B / max line 182 / non-ASCII 0 (self-consistent)
- `RECEIPTS.md` — Step-0 live-premise receipts + candidate/rig evidence
- `build-and-test.sh` — adapted 1.3.0 build script (adds the live EliteMobs
  jar to the compile classpath)
- `build/` — build intermediates (classes, test-classes, sources lists)
- `src/` — 1.3.0 source tree (main + tests + plugin.yml), copied from the
  1.2.1 tree, which was not modified
- `libs/EliteMobs-10.9.10.jar` — Step-0 live pull copy (compile classpath)
- `live-pull/` — the Step-0 pulled live jars (WeekendXP 1.2.1 + EliteMobs)
- `rig/` — rig copy used for both boot proofs (Paper 26.2-123 + live
  patched mcMMO + live EliteMobs + candidate jar; `rig/logs/boot1-with-em.log`
  and `rig/logs/boot2-no-em.log` are the boot evidence)

Status: BUILD COMPLETE — candidate + packet file-ready, NOT filed, NOT
installed (supervisor chain files after verifying this record).

---

## RENAME - ExcelWeekendXP 1.3.0 -> ExcelEMWeekendXP 1.3.0 (D163/D171)

- Rename worker, parent-seat dispatch, Fri Nov 20, 2026, after Mike approved
  the rename (~12:50 PM CT; D163 borderline ruling + D171). Scope per the
  D163 audit plan item 14 and the shipped ExcelEMQuestXP 1.1.0 pattern:
  plugin.yml name + jar filename + packet re-stamp, BEFORE install, riding
  the pending install. This section supersedes the deliverable identity in
  the sections above (candidate jar name/hash, source zip, packet); the
  feature-build evidence above stands unchanged.
- **Old name, as found (jar-authoritative):** `ExcelWeekendXP` - read from
  the candidate jar's plugin.yml AND the live 1.2.1 jar's plugin.yml (both
  agree: `name: ExcelWeekendXP`, `main: com.winkreyes.excelweekendxp.ExcelWeekendXP`).
  The "WeekendXP" shorthand used in some lane prose is the pre-Excel 1.0.0
  name (`~/workspace/weekendxp/plugin.yml`: `name: WeekendXP`), not the
  current name.
- **Changed:**
  - `src/plugin.yml` line 1: `name: ExcelWeekendXP` -> `name: ExcelEMWeekendXP`
    (the ONLY source change; version stays 1.3.0; description carries no
    plugin name and is unchanged).
  - Jar filename: `ExcelWeekendXP-1.3.0.jar` -> `ExcelEMWeekendXP-1.3.0.jar`
    (`build-and-test.sh` output name updated to match).
  - `INSTALL-PACKET.md` re-stamped for the renamed plugin (see below).
- **Deliberately NOT changed (QuestXP pattern):** main class and package
  (`com.winkreyes.excelweekendxp.ExcelWeekendXP` - ExcelEMQuestXP likewise
  kept `com.winkreyes.excelquestxp.ExcelQuestXP`); all Java source (zero
  edits); all tests (zero edits). The plugin defines no commands (no
  `/weekendxp` exists in plugin.yml), so no command name is affected. It
  has no config file, so no config header exists to rename. The Bukkit
  logger prefix derives from the plugin.yml name and renames automatically
  (rig-proven below). The one hard-coded log message still reading
  "ExcelWeekendXP scheduler enabled: ..." is message text, not identity,
  and is left as-is per the same precedent.
- **Tests:** full suite re-run on BOTH rename builds: 38/38 pass, 8
  containers, 0 failed (25 pre-existing + 13 D155 tests, all unmodified).
- **Deterministic double-build, byte-identical:** Build 1 and Build 2 both
  18,943 B / SHA-256
  `9bc65521d16079921cd1239b18f3fb056c1058e617bb658bc4f47e6374413f70`
  (`cmp` clean). Entry-by-entry diff against the pre-rename candidate
  (`a9f16135...`): the ONLY differing jar entry is `plugin.yml`, and within
  it only line 1; every class file is byte-identical to the rig-proven
  1.3.0 build, so behavior is identical by construction.
- **Rig proof (boot 3, renamed jar):** same rig as boots 1-2, EliteMobs
  present, renamed jar hash-verified in place (`9bc65521...`). Boot Fri
  Nov 20, 2026, 18:55 UTC; stopped cleanly (JVM exit after graceful
  shutdown). Log: `rig/logs/boot3-renamed-with-em.log`, SHA-256
  `f0f806e51640c4a4e3c8a99887ae1259ab154394324b6779717bc45bb52964d3`.
  Verbatim lines:
  - plugin list: `ExcelEMWeekendXP (1.3.0)` among the enabled plugins
  - `[ExcelEMWeekendXP] Enabling ExcelEMWeekendXP v1.3.0`
  - `[ExcelEMWeekendXP] EliteMobs detected: EliteMobs skill XP weekend multiplier leg enabled.`
  - `[ExcelEMWeekendXP] Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x. Verified EliteMobs skill XP event multiplier 1.2x.`
  - `[ExcelEMWeekendXP] ExcelWeekendXP scheduler enabled: America/Chicago, every 30 minutes.`
  - `Done (37.507s)!`; SEVERE 0; `schedule incomplete` 0.
  Also observed: after the full boot + enable cycle, NO data folder exists
  for the plugin in `rig/plugins/` (no `ExcelEMWeekendXP/`, no
  `ExcelWeekendXP/`) - the plugin creates and writes none.
  Limits (unchanged from the feature build): no player was on the rig, so
  no live `EliteSkillXpGainEvent` fired on a server; in-game scaling stays
  proven at unit level and owed as player proof after install (packet).
- **Data-folder determination (for the packet's migration spec):** from
  the 1.2.1 lineage - the plugin has no config (no config.yml in any jar,
  1.0.0-1.3.0), no player data, and no persisted state: `WeekendSchedule`
  state is in-memory only, re-derived from the America/Chicago clock at
  enable and every 30 minutes. The ONLY possible content of live
  `plugins/ExcelWeekendXP/` is files its built-in `LegacyDataMigration`
  copied in from the pre-Excel `plugins/WeekendXP/` folder on a first
  enable (it copies only while the target folder holds no files, and never
  deletes the source). No fleet record enumerates the live folder's
  contents (the Nov 20 audit pull covered jars only; the 1.2.0 install
  record states "No data folders touched"), so the packet specifies the
  determination AT install time: fresh-list + hash the folder as the
  migration receipt, copy byte-verified to `plugins/ExcelEMWeekendXP/` if
  it holds files, skip with the outcome recorded if absent/empty, old
  folder untouched either way, and read state back after boot via the
  verified-rate boot line plus a post-boot re-hash of the copied folder.
- **Deliverables after rename:**
  - `ExcelEMWeekendXP-1.3.0.jar` - 18,943 B / `9bc65521...` (above)
  - `ExcelEMWeekendXP-1.3.0-source.zip` - 16,967 B / SHA-256
    `39e27419e6d9e00bf04bcdec157d3240af707906840641f75019b9693b8cd494`
    (same `src-1.3.0/` layout and fixed timestamps as the pre-rename zip;
    its plugin.yml carries the new name)
  - `SHA256SUMS` - rewritten for the two renamed artifacts; `sha256sum -c`
    passes.
  - `INSTALL-PACKET.md` - revised (11,380 B / max line 184 / non-ASCII 0):
    renamed candidate + hashes, jar swap with the 1.2.1 jar kept as
    `.bak-20261120`, the data-folder migration steps above, boot-verify
    lines under the `[ExcelEMWeekendXP]` prefix (from boot 3), rollback
    (restore the .bak jar; both data folders left in place), same-pass
    surfaces updated to also rename the plugin in canon.
  - Pre-rename artifacts preserved under `superseded-pre-rename/`
    (`ExcelWeekendXP-1.3.0.jar` `a9f16135...`, `ExcelWeekendXP-1.3.0-source.zip`
    `6f1737d2...`) so the build-folder root names exactly one candidate.
- Status: RENAME COMPLETE - renamed candidate + revised packet file-ready,
  NOT filed, NOT installed. The supervisor re-points the existing MANIFEST
  PENDING entry to the revised packet
  (`~/workspace/weekendxp-130-build-20261120/INSTALL-PACKET.md`) and
  candidate hash `9bc65521...`.
