# ExcelRewardGuard 1.2 — install record

> **INSTALL RECORD — published to `wrv-plugins` as a draft PR,
> October 16, 2026** (this repo's live-version-record pattern; draft
> PRs here stay unmerged). Published under the owner's standing
> GitHub authorization (standing function rules, October 16, 2026).
>
> **Installed live October 16, 2026, 1:57 AM CDT** by its
> auto-watch (§7). **What is still open, stated up front:** the
> in-game proof (§8) — a real session at Kailey's sky farm paying
> the reduced rate — has **not** been done yet. It is stated as
> pending in §8 and in the website changelog text in §10, and
> must stay stated that way until it happens.

## Status at a glance

| Status | State |
|---|---|
| Built | **Yes** — October 16, 2026 |
| Automated tests | **268 assertions, all passing**, on both builds |
| Test server (scratch boot) | **Passed** — fresh config and edited config, with providers (Jobs + mcMMO + EliteMobs present) |
| Installed live | **Yes** — October 16, 2026, 1:57 AM CDT, via auto-watch, sequenced after the GuildGate campaign install (§7) |
| Java check | **NOT DONE YET** — in-game proof pending (§8) |
| Bedrock check | **NOT DONE YET** — the sky-farm proof session is a Bedrock-side check and is pending (§8) |

## 1. What it does

1.2 keeps everything 1.1 does (origin-based scaling for spawner /
spawn-egg / nether-portal kills — see the 1.1 record, PR #26) and
adds a second classification: **fall-softened kills**.

- The guard tracks **cumulative FALL damage per living entity**,
  with the entity's max health captured at each fall.
- On a kill, if `cumulativeFallDamage / maxHealth` reaches the
  configured `farm-fall-damage-percent` (new key, default **50**),
  the kill counts as **FARMED** and the same `farm-reward-percent`
  scaling as the origin rules applies — Jobs money/points/XP and
  mcMMO XP at the live **55%**. Vanilla XP is never touched; item
  drops are never touched.
- A kill matching both an origin rule and the fall rule is scaled
  **exactly once**; the origin verdict keeps the log label.
- Fall classifications log with a distinct **FALL_SOFTENED** label
  (Jobs lines; the mcMMO suppression-window plant line), so the
  server log shows *why* a payout was scaled. As in 1.1, these
  per-kill lines are debug-gated in the source.
- **The healed-mob rule (deliberate, pinned by tests):** the fall
  total is cumulative and never forgiven. A mob that fell for 60% of
  its health bar, healed to full, and is then killed still counts
  as softened. How the kill was set up is the signature, not the
  mob's current health.
- Memory stays bounded: only entities with fall damage > 0 get
  entries; entries are dropped at the end of death processing, on
  entity removal/despawn, and on chunk unload; the map is cleared
  on disable. Players are not tracked.
- **EliteMobs stance is unchanged**: verdict logging only.
  `EliteMobDeathEvent` in 10.9.8 is not cancellable (proven
  Oct 15), and 1.2 does not extend the logging to fall-softened
  elites. `EliteVerdictLog` is byte-identical source to 1.1.

## 2. Why this exists — the sky farm

1.1 classifies by birth origin. Kailey's farm is a **sky farm**:
mobs spawn naturally in the dark at height, fall, and are finished
with one hit — so its kills read NATURAL and paid 100% on Jobs +
mcMMO. On Oct 16, 2026 (~1:20 AM) the owner ordered that nerfed
too — "Not nerf the Minecraft xp but everything else." 1.2 is that
order: the plugin side of a fall-softened kill is reduced, vanilla
XP stays exempt. The EliteMobs side needed no change (EliteMobs'
own farming cap already governs it, and the owner decided the same
night to leave that cap at 20 rewarded kills / 30 minutes).

## 3. Artifact

- **Jar:** `ExcelRewardGuard-1.2.jar`
- **SHA-256:** `15672a7b5e0e4d6662b032c7a8bef3e2bb6a8c72173534a1bc93258f47073473`
  — a second full build reproduced the identical hash.
- **Source snapshot:** `snapshots/ExcelRewardGuard-1.2-source-20261016.zip`,
  SHA-256 `988e2143b1893c464db0f4c79d7ede15ca163d89a80cdceb61bb617e896995b5`
  (checksums in `snapshots/SHA256SUMS-1.2.txt`; local archive of
  record; the PR carries the same source tree).
- **Previous version:** 1.1, kept on the server as
  `ExcelRewardGuard-1.1.jar.bak-20261016` (rollback, §9).

## 4. What changed vs 1.1

- **Added:** `FallDamageTracker.java` + `FallDamageTrackerTests.java`
  (the fall bookkeeping and its 81 assertions).
- **Modified:** `ExcelRewardGuard.java` (composes one classification
  — origin DENY or `fall_softened`, never both — and clears the
  tracker on disable), `JobsVeto.java` (FALL_SOFTENED log lines for
  fall kills; origin-kill lines byte-identical to 1.1),
  `RewardScale.java` (a `parsePercent` overload with a configurable
  default; the 1.1 entry point delegates, behavior identical),
  `config.yml`, `plugin.yml`, `build.sh`, `README.md`.
- **Byte-identical to 1.1:** `FarmVerdict.java`,
  `ProvenanceStore.java`, `XpSuppression.java`, `McMmoVeto.java`,
  `EliteVerdictLog.java`, and all four pre-existing test suites.
- **Config diff (verified by YAML parse):** exactly one added key,
  `farm-fall-damage-percent: 50`. Zero changed values —
  `farm-reward-percent` stays **55**, the live value.
- The mcMMO path needed no code change of its own: the suppression
  window now plants for any farmed death (origin or fall), and its
  plant debug line distinguishes the fall case.

## 5. Tests

**268 assertions, all passing, run on both builds:**

| Suite | Count | State |
|---|---|---|
| ProvenanceTests | 85 | Unchanged from 1.1 |
| XpSuppressionTests | 22 | Unchanged from 1.1 |
| RewardScaleTests | 51 | Unchanged from 1.1 |
| LiveApiSignatureTests | 29 | Unchanged from 1.1 |
| **FallDamageTrackerTests** | **81** | **NEW** |

The new suite pins: a full-health/no-fall kill is unaffected; 60%
fall is softened; exactly-at-threshold (50%) counts; just-below
(49.5%) does not; multi-fall and fractional accumulation; the
healed-mob cumulative rule (60% fall → heal → still softened; a
second fall stacks to 120%, softened even at threshold 100); the
latest-max-health rule; threshold 0 and 100 endpoints; no-entry
guards (zero/negative/NaN damage, bad max health, null id);
per-entity independence; death cleanup for non-player kills;
despawn/chunk-unload bulk cleanup; clear-on-disable; the
spawner-origin + fall overlap classified once with the origin
label; and threshold config parsing (with the 1.1 parse default
still 65).

## 6. Boot tests (local Paper 26.2-123, JDK 25, with providers)

Two scratch boots beside CMILib 1.6.0.1, Jobs 5.2.6.3, Vault 2.0.0,
mcMMO 2.3.003, EliteMobs 10.9.8:

- **Fresh config:** enables clean; generated config contains
  `farm-fall-damage-percent: 50` and `farm-reward-percent: 65`
  (the shipped default); startup lines report provenance ON, 65%,
  fall-softened detection ON at 50%, Jobs + mcMMO scaling ACTIVE;
  **zero SEVERE**; clean disable.
- **Edited config** (`farm-reward-percent: 55`,
  `farm-fall-damage-percent: 40`): startup lines report exactly
  55% and 40 — config pickup on enable verified. Zero SEVERE;
  clean disable.

**Simulated vs unit-only, stated plainly:** no client exists in the
sandbox, so no mob was actually dropped and killed during the boot
tests. Classification, thresholds, cleanup and overlap are
unit-verified (81 assertions); the Bukkit wiring is boot-verified
for clean enable/disable beside the real providers. The real
sky-farm kill test is the owner's in-game check (§8).

## 7. Install — live execution

- **When:** October 16, 2026, **1:57 AM CDT**, by the install
  auto-watch, sequenced after the GuildGate campaign install
  (its marker was present; the watch held until it existed).
- **Gate:** panel `list` twice, both **0/10** (1:49:48 AM and
  1:51:41 AM CDT). No players online.
- **Sequence:** panel **Shut Down** (never console `stop`); the
  live 1.1 jar was renamed to
  `ExcelRewardGuard-1.1.jar.bak-20261016`; the 1.2 jar was
  uploaded (26,183 bytes, matching the packet; the panel shows no
  hash, so the SHA was verified locally pre-install, not in-panel).
- **Config:** the live `config.yml` was backed up to
  `config.yml.bak-20261016` first; `farm-reward-percent: 55` was
  verified before editing; exactly one key was added,
  `farm-fall-damage-percent: 50`; a re-read verified both keys.
- **Startup verification (from the install marker):**
  `Enabling ExcelRewardGuard v1.2`; `Farm reward percent: 55%`;
  `Fall-softened kill detection ON: a mob whose cumulative fall
  damage reaches 50% of its max health counts as farmed`;
  Jobs scaling **ACTIVE** at 55%; mcMMO scaling **ACTIVE** at 55%;
  server `Done` at 1:57:29 AM CDT; post-start `list` 0/10; **no
  SEVERE** naming ExcelRewardGuard (baseline noise only).
  Per-kill FALL_SOFTENED lines are absent from the log, as
  expected: they are debug-gated and the live config has
  `debug-logging: false`.

## 8. Still to check in game — NOT DONE YET

The owner's proof session, ideally at Kailey's sky farm:

- A fall-dropped mob finished by a player pays **55%** Jobs
  money/XP and **55%** mcMMO XP, and drops **full vanilla XP**.
- A full-health cave kill nearby still pays **100%**.
- A spawner kill still pays **55%** with its origin (not
  FALL_SOFTENED) label — scaled once, never twice.
- A party member sharing a fall-softened kill's mcMMO gain gets
  the **55%** share.

Evidence limit, stated plainly (same as 1.1): the per-kill scaling
lines are debug-gated and live `debug-logging` is `false`, so the
log cannot prove these payouts after the fact. Either the session
is judged by what the players see in game, or `debug-logging` is
flipped on for one planned session (the owner's call, still open).

**Accepted false positive, part of the design:** a mob that falls
down a ravine entirely on its own and is finished by a player at
or above the threshold counts as farmed. No signal separates a
built farm's fall from a natural ravine's; knockback-off-a-cliff
kills and previously-fallen-then-healed mobs count too. Vanilla XP
and loot are never affected.

## 9. Rollback

1. Panel Shut Down.
2. Delete `ExcelRewardGuard-1.2.jar`; rename
   `ExcelRewardGuard-1.1.jar.bak-20261016` back to
   `ExcelRewardGuard-1.1.jar`.
3. Config: the added key is inert under 1.1 (unknown keys are
   ignored); deleting the `farm-fall-damage-percent` line (or
   restoring `config.yml.bak-20261016`) restores the file exactly.
4. Panel Start; verify `Enabling ExcelRewardGuard v1.1` and the
   55% line.

No player data, PDC marks, or balances are affected in either
direction — the fall tracker is in-memory only and dies with the
plugin.

## 10. Website delta (prepared, not applied)

**Finding first: the website does not list ExcelRewardGuard at
all.** Checked October 16, 2026 against the live page's source
(the published `index.html`, byte-verified that night): zero
mentions of RewardGuard — there is no plugin-table row and no
version number to bump, and 1.1 never got a website entry either.
So there is **no version delta to apply for 1.2**. If the owner
wants the guard on the public page, the proportionate step is one
changelog entry in the existing Oct 16, 2026 block. Prepared text
(exact, ready to paste as a new `<li>` in that block):

> `<li><b>ExcelRewardGuard 1.2</b> is live: farmed kills now also cover sky farms. A mob that took at least half its health from falling and is then finished by a player counts as farmed, so its Jobs money and mcMMO XP pay the same reduced 55% rate as spawner-farm kills. Vanilla XP and item drops are never reduced. <b>Still to check in game:</b> a real session at a sky farm paying the reduced rate — that proof session has not happened yet.</li>`

That is the whole website prep for 1.2: no table edit exists to
make. If a full plugin-table row for ExcelRewardGuard is ever
wanted, it is a separate, larger addition (it would also need to
cover 1.1's origin rules to be honest) and is not prepped here.

---

## Sources consulted for this record

Local files only; no GitHub, website, or server access was used.

- `~/workspace/excelrewardguard/BUILD-REPORT-1.2.md` — the build
  of record (change scope, tests, boots, limitations).
- `~/workspace/excelrewardguard/install-prep-1.2/INSTALL-PACKET.md`
  — artifact SHA, install/rollback steps, honest unverified list.
- `~/workspace/excelrewardguard/install-prep-1.2/INSTALLED-1.2.txt`
  — the live install: times, gate checks, config handling,
  startup lines.
- `~/workspace/excelrewardguard/install-prep/INSTALL-RECORD-1.1.md`
  — the 1.1 record (published as wrv-plugins draft PR #26); this
  draft mirrors its structure.
- `~/workspace/excelrewardguard/snapshots/SHA256SUMS-1.2.txt` —
  jar + source-snapshot checksums.
- The published website `index.html` (via
  `~/workspace/overnight-2026-10-16/website-prep/index-campaign.html`,
  verified byte-identical to the live page the night it was
  published) — the §10 finding that RewardGuard is not listed.

*End of record.*
