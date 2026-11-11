# BUILD RECORD — ExcelRewardGuard 1.7 (console admin)

Built Nov 10, 2026 (desk build lane for WRV board supervisor
run 168, continuation). Commission: the console-admin coverage
audit (`~/workspace/console-admin-audit-20261109/AUDIT.md` §4)
under Mike's Nov 3, 2026 console-admin rule. This record is
the continuation of the STOPPED packet in `BUILD-RECORD.md`
in this folder: that run established live = 1.6 (not the
audit's stale 1.2 target) and stopped for lack of a 1.6 source
tree; the base-proof run then recovered, deposited, and proved
the 1.6 tree (`BASE-PROOF-1.6.md`, VERDICT PASS). **Not
installed.** The install packet is `INSTALL-PACKET.md` in this
folder. Per the supervisor's instruction this lane filed
NOTHING into the MANIFEST (the supervisor files panel-clear),
and touched no panel, GitHub, Drive, email, or MEMORY.md.
Records staged at
`~/workspace/records-catchup-20261103/packages/excelrewardguard-1.7-20261110/`
(local staging only; the GitHub/Drive records legs are the
parent's — see `PUSH-NOTES.md`).

## Base proof (carried, not re-run)

- `BASE-PROOF-1.6.md`: the deposited tree
  `~/workspace/excelrewardguard/work/ExcelRewardGuard-1.6/`
  rebuilds unmodified (with the fork provider jar) to all 25
  class files byte-identical to the live 1.6 jar, 500/500
  assertions; the only jar-level diff was the documented
  plugin.yml author line. That tree was not edited by this
  build and remains clean (no target/).
- The 1.7 delta was applied to a copy in
  `~/workspace/rewardguard-17-build/ExcelRewardGuard-1.7/`
  (copy verified `diff -r` identical to the 1.6 tree before
  any edit). This folder's tree is the delta source tree for
  records. Both candidate builds ran in further scratch copies
  (`scratch-delta-1/`, `scratch-delta-2/` in this folder,
  each with `build-output.log`).

## Mandatory conditions from the base proof — all carried

1. **Fork provider jar.** Both builds ran with
   `MCMMO_JAR=~/workspace/moon-packets-audit/inspect/sharedxp/Excel-Shared-XP-Identity-release/native/mcMMO-2.3.003-WRV-XP-2-ExcelShare-1-a.jar`
   (SHA-256 re-verified this run:
   `4276bd3e07bdaf60c4edbb5461c1c4a61c3252e24f15f324faaa58d7300dff5c`).
   The stock mcmmo-edit jar cannot compile the 1.6 delta.
2. **Jar step fixed.** The carried build.sh wrote
   `ExcelRewardGuard-1.5.jar` with a 2026-10-23 stamp; the 1.7
   build.sh writes `ExcelRewardGuard-1.7.jar` stamped
   2026-11-10T06:00:00Z.
3. **plugin.yml author line.** Set to the shipped 1.6 jar's
   line — `author: Moon (provenance candidate) and Argus
   (enforcement completion)` — replacing the tree's
   `authors: [Iris, Mike]`. Explicit consequence, verified by
   diffing the built jar's plugin.yml against the LIVE jar's:
   the only plugin.yml changes in 1.7 are version 1.6 → 1.7
   plus the command/permission registration. No author diff
   remains.

## Candidate

- `ExcelRewardGuard-1.7.jar` — **60,694 B** — SHA-256
  `d433749336609985d5c6b1fba8302fa476944517b9ac2e3f177f10b845babf89`
- Deterministic double-build byte-identical (two scratch
  builds, same hash; `cmp` clean).
- `ExcelRewardGuard-1.7-source.zip` — 84,954 B — SHA-256
  `f5d8629ea5eae205d8243ac3bce2dc0f648182bf27ef69ae10119282d200f7f4`
  (the whole ExcelRewardGuard-1.7/ tree, 33 files; timestamps
  pinned 2026-11-10).

## What shipped (the whole change, nothing else)

Player-facing behaviour change: **none.** Every reward path
(Jobs scaling, mcMMO scaling, boss band, seal step, fall
softening, provenance) is behaviour-unchanged. Source diff
vs the 1.6 tree — exactly these:

- `RewardGuardStatus.java` (new) — Bukkit-free formatter +
  gate: snapshot record of the guard's real fields/counters,
  `canUse(console, hasAdmin)` (console always; in-game needs
  the node), `isStatusArgs` (exactly `status`), usage/denial
  lines, and the 6-line reply. Read-only by construction: it
  formats values the caller already read.
- `ExcelRewardGuard.java` — imports for Command/CommandSender/
  ConsoleCommandSender, a 1.7 javadoc paragraph, an
  `onCommand` (`status` dispatch; denial and usage shapes;
  sends the formatted lines) and a private `statusSnapshot()`
  reading only existing state: jobsFarmRate, mcMmoFarmRate,
  bossBandRate, bossTrialFeed.connected(),
  farmFallDamagePercent, fallTracker.size(), the
  jobsInterventions / mcMmoInterventions / eliteVerdicts
  counters, the SealStepState (enabled / seals / steps /
  pinned par / known powers) + sealSourceLive, auditLog,
  debugLogging, suppressionWindowTicks, recentFarmedOrigin().
- `plugin.yml` — version 1.7; author line (above); the
  `rewardguard` command (permission excelrewardguard.admin);
  the `excelrewardguard.admin` permission, default op.
- `RewardGuardStatusTests.java` (new, 39 assertions) + one
  registration line in `build.sh`; `build.sh` jar name/stamp.
- `README.md` — refreshed from the stale 1.4 text to 1.7
  (records only; not in the jar).
- **No change:** config.yml (byte-identical — verified in the
  jar against the live jar's), SharedXpProvenance, McMmoVeto,
  JobsVeto, and every other 1.6 source and carried test file.

### State-model honesty (nothing invented)

The audit's sizing asked for "config % / marked-kill counters".
The config percents are real (farm rates + boss band rate +
fall threshold, printed with their config keys). A marked-kill
counter does NOT exist in this plugin: kills are classified
per event and never tallied. What the plugin actually counts
— and what status shows, labelled "Interventions since
enable" — is the three enforcement counters:
jobsInterventions, mcMmoInterventions, eliteVerdicts. No
counter was added, because adding one would change the
enforcement classes this packet must not touch; if a true
marked-kill tally is wanted, that is its own packet.

## Tests

- **Desk: 539/539** via `build.sh` in both scratch builds —
  the carried 1.6 suite passes unchanged (**500 assertions**,
  11 suites: Provenance 85, FallDamageTracker 81, SealStep 83,
  FarmRate 58, RewardScale 51, LiveApiSignature 44 against the
  fork mcMMO jar, JobsSingleScaling 27, XpSuppression 22,
  BandScale 21, AuditLog 16, BandFeed 12) plus the new
  **RewardGuardStatusTests: 39 assertions** (gate truth table,
  args shape, exact reply text for the live-shaped snapshot,
  seal wording for off / un-read / live / source-lost shapes,
  feed-inactive + null-origin fallbacks, and the plugin.yml
  gate: version 1.7, author line, command, node default op).
- **Control proof** (`class-compare-1.7/` in this folder):
  against the live 1.6 jar — 24 of 25 classes byte-identical;
  `ExcelRewardGuard.class` differs (the command handler);
  2 classes added (`RewardGuardStatus`, `RewardGuardStatus$Snapshot`);
  27 classes total. config.yml and META-INF/MANIFEST.MF
  byte-identical; plugin.yml differs only as stated above.
- **Rig** (minimal Paper 26.2-123, only the 1.7 jar, commands
  typed on the real console stdin, `rig-1.7/`): clean v1.7
  enable with all 1.6 enable lines in shape, **SEVERE 0**,
  clean stop, **8/8 behavior checks** — the exact 6-line
  status reply, and usage lines for bare / bogus / extra-arg
  invocations. Full table + limitations in
  `rig-1.7/RIG-RESULTS.md`; log: `rig-1.7/boot.log`.

## Surfaces checked (expected no-change, confirmed)

- Guide knowledge/intents: operator-only command — no player
  teaching owed; nothing added.
- Website pair, What's New BOTH surfaces, broadcaster tips,
  in-game menus (ExcelMenu / ExcelBedrockMenu): no
  player-facing claim about RewardGuard admin exists to
  correct; no changes made. The install packet names the one
  operator note owed at install (What's New operator line).

## Live proof owed after install (not part of the build)

One console `rewardguard status` on the live server, read
back in the log (6 lines with the live rates/counters, boss
band feed connected, seal source live). No player-visible
check is owed — no player surface changes.
