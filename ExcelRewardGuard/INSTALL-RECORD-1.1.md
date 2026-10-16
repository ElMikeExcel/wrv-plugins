# ExcelRewardGuard 1.1 — install record

> **INSTALL RECORD — published to `wrv-plugins` as a draft PR,
> October 16, 2026** (this repo's live-version-record pattern; draft
> PRs here stay unmerged). Published under the owner's explicit
> GitHub authorization for the Oct 16, 2026 overnight queue.
> The in-game acceptance checks in §8 are still open and are
> stated as open wherever they apply.

## Status at a glance

| Status | State |
|---|---|
| Built | **Yes** — October 15, 2026 |
| Automated tests | **187 assertions, all passing**, on every build |
| Test server (scratch boot) | **Passed** — standalone and with providers (Jobs + mcMMO + EliteMobs present) |
| Installed live | **Yes** — October 15, 2026, ~11:14 PM CDT, fresh install at farm-reward-percent **55** (§7) |
| Java check | **NOT DONE YET** — in-game acceptance pending (§8) |
| Bedrock check | **NOT DONE YET** — in-game acceptance pending (§8) |

## 1. What it does

ExcelRewardGuard scales **plugin** rewards for farmed kills —
kills whose spawn origin is a mob spawner, a spawn egg, or a
nether portal:

- **Jobs** money, points, and XP are scaled to the configured
  `farm-reward-percent`.
- **mcMMO** XP gains for the killer — and for party members
  sharing the gain (SHARED_PVE goes through the same per-player
  gain path) — are scaled inside a short suppression window
  planted at the farmed death. The scaling sits at LOWEST priority
  on the gain event, so weekend/together multipliers multiply the
  already-reduced amount: the farm check runs before multipliers.
- **Vanilla XP is never touched.** Item drops are never touched.
- **EliteMobs rewards are logged, never scaled** — a proven
  limitation, not an oversight: `javap` on the installed
  EliteMobs 10.9.8 jar shows `EliteMobDeathEvent` is not
  cancellable, so no plugin can veto EliteMobs rewards through
  its API. The guard records its verdict for EliteMobs kills and
  leaves the rewards alone (EliteMobs has its own farming cap;
  see the farm-integrity record in the WRV-056 work).

## 2. The percent — design history in one paragraph

1.0 zeroed farmed rewards entirely. On Oct 15 the owner changed
the design: players still have to find or build the farm and land
the kills, so farmed kills earn a **reduced share** instead. 1.1
implements the reduction as the configurable `farm-reward-percent`
(shipped default **65**; the zero design remains reachable by
setting 0). The **live config runs 55** — the owner's final call
at install. Config parsing is defensive and pinned by tests:
missing key → shipped default; out-of-range → clamped; fractional
or unparseable → defaulted with a startup warning naming the bad
value and the percent actually in use.

## 3. Artifact

- **Jar:** `ExcelRewardGuard-1.1.jar`
- **SHA-256:** `9ded87378eb907555a12bbd0120981896b11871e8cfa47e4c2d6a4a04a3c71b2`
- **Source snapshot:** `snapshots/ExcelRewardGuard-1.1-source-20261015.zip`
  (36 files: source, tests, README, build.sh, test results, both
  boot logs), SHA-256
  `eb9eaf1fbfcdc064a29539308aae3bdc1987e82a187ab820727c09f18e77ce4a`
  (local archive of record; this PR carries the same source tree).
- **Fresh install:** no prior version, no data migration, no
  rollback jar — rollback is removal (§9).

## 4. What changed vs 1.0

- Zeroing replaced by percent scaling for Jobs (money/points/XP)
  and mcMMO XP, including party shares.
- The suppression window mechanics are unchanged; the mcMMO
  handler now scales `rawXpGained` by the percent instead of
  zero + cancel (percent 0 reproduces the 1.0 behavior exactly;
  percent 100 is an exact pass-through with no rounding).
- Scaled doubles round half-up to 2 decimal places; a 0.01 reward
  at any percent above 0 never silently becomes 0.

## 5. Tests

**187 assertions, all passing, on every build** — provenance
classification, suppression-window behavior (including party
scaling inside the window at the configured percent), the
scaling math and its rounding contract, config parsing, and
live-API signature checks against the installed Jobs/mcMMO
builds. Test sources and recorded results are in this PR under
`ExcelRewardGuard/src/test/`.

## 6. Boot tests (local Paper 26.2-123, JDK 25)

Standalone and with-providers boots both enable cleanly; the
startup lines report the configured percent and which scalings
are ACTIVE. With the shipped default config the line reads 65%;
the live server runs the owner's 55% (§7).

## 7. Install — live execution

- **When:** October 15, 2026, ~11:14 PM CDT, on an empty server
  (gate checked repeatedly, per the standing install rule).
- **Startup verification (from the install record):** the log
  showed `Farm reward percent: 55%`, Jobs scaling **ACTIVE**,
  mcMMO scaling **ACTIVE**, EliteMobs verdict **logging only**,
  and **zero errors** naming ExcelRewardGuard. The installed
  jar's SHA-256 matches §3.

## 8. Still to check in game — NOT DONE YET

The owner's acceptance checks. (The build report's copy of this
list says 65% because it was written against the shipped
default; the live percent is **55**.)

- Spawner-origin kill pays **55%** Jobs money/XP and **55%**
  mcMMO XP, with **full** vanilla XP.
- Spawn-egg kill: same.
- Nether-portal piglin kill: same.
- A natural kill nearby still pays **100%**.
- A party member near a farm kill gains a **55%** share — not 0%,
  not 100%.

Evidence limit, stated plainly: the per-kill scaling log lines
are debug-gated in the source, and the live config has
`debug-logging: false`, so **no per-kill payout counts exist in
the server logs** and none can be reconstructed. Log evidence
gathered Oct 16 (`ACCEPTANCE-EVIDENCE-2026-10-16.md`, local)
confirms the install-time verification and nothing more — these
checks are in-game only, or need one planned farm session with
debug logging on (the owner's call).

## 9. Rollback

Remove `plugins/ExcelRewardGuard-1.1.jar` and its config folder
at a restart. Nothing else changes: the guard only ever scaled
Jobs/mcMMO payouts at kill time, so removal restores full
payouts immediately and leaves no data behind.

## 10. Follow-up already in flight

ExcelRewardGuard **1.2** (built Oct 16, 2026) adds fall-softened
detection for sky farms — a natural-origin kill whose cumulative
fall damage reaches the configured threshold counts as farmed —
closing the one origin class 1.1 cannot see. It gets its own
record when it is live.
