# ExcelRewardGuard 1.7 — SOURCE record

Excel — Wink Reyes Valley. November 10, 2026.
Commission: the console-admin coverage audit
(`~/workspace/console-admin-audit-20261109/AUDIT.md` §4) under
Mike's Nov 3, 2026 console-admin rule, as re-scoped by the
stopped packet in `BUILD-RECORD.md` (the audit's "1.2" label
was stale; live is 1.6, so this is 1.7).

## Provenance

- Project tree: `~/workspace/rewardguard-17-build/ExcelRewardGuard-1.7/`
  (plugin.yml version 1.7, main
  com.winkreyes.rewardguard.ExcelRewardGuard, api-version '1.21').
  This is the delta source tree for records; the deposited base
  tree below was NOT edited.
- Base: the proven 1.6 tree
  `~/workspace/excelrewardguard/work/ExcelRewardGuard-1.6/`
  (deposited by the base-proof run; `BASE-PROOF-1.6.md`:
  unmodified rebuild = all 25 classes byte-identical to the
  live jar, 500 assertions). The 1.7 tree is a copy of it,
  verified byte-identical (`diff -r`) before any edit.
- Live gate fingerprint: the live jar is
  `ExcelRewardGuard-identity-OFFLINE.jar` (Shared-XP bundle
  install, Oct 30, 2026) — 54,649 B — SHA-256
  `dbf08161e773a25d871bc5365c01eda5ee1df88a46eb31ef58ca71d48b423093`,
  plugin.yml version 1.6.
- Built by the tree's `build.sh` (JDK 25 at
  `~/workspace/.jdk25`, `--release 25`, Paper 26.2 runtime
  libraries + the exact provider jars) with ONE mandatory
  override from the base proof: `MCMMO_JAR` pointed at the WRV
  mcMMO fork jar
  `~/workspace/moon-packets-audit/inspect/sharedxp/Excel-Shared-XP-Identity-release/native/mcMMO-2.3.003-WRV-XP-2-ExcelShare-1-a.jar`
  (SHA-256 `4276bd3e07bdaf60c4edbb5461c1c4a61c3252e24f15f324faaa58d7300dff5c`).
  The stock mcmmo-edit jar cannot compile the 1.6 delta (no
  raw-XP accessors on the Pre event).

## What 1.7 changes vs 1.6 (the whole delta)

CONSOLE ADMIN. 1.6 (like every version before it) had NO
command at all — an operator could not inspect the guard
without reading config + logs. In short:

- NEW `RewardGuardStatus`: pure, Bukkit-free formatting of a
  snapshot of the guard's real state + the permission gate.
  The reply shows: Jobs / mcMMO farm rates with their percents,
  boss band rate + verdict-feed connected/inactive, the
  fall-softening threshold + fall records currently tracked,
  the three counters the plugin actually increments (Jobs
  interventions, mcMMO interventions, elite verdicts since
  enable), the seal-step state (enabled, seals of 5, source
  live/unavailable, per-seal steps, pinned par / powers
  observed), audit-log + debug flags, suppression window, and
  the most recent farmed origin. The plugin keeps NO per-kill
  marked counter, so none is shown — nothing was invented.
- `ExcelRewardGuard`: an `onCommand` for `/rewardguard` —
  `status` is the only form; the console always passes, an
  in-game sender needs `excelrewardguard.admin`; bare or
  malformed invocations print the usage line. The handler only
  snapshots existing fields and sends lines: no reward path,
  state, or file is touched.
- `plugin.yml`: version 1.6 → 1.7; the `rewardguard` command
  registration; the `excelrewardguard.admin` permission
  (default op). The author line was set to the shipped 1.6
  jar's line (`author: Moon (provenance candidate) and Argus
  (enforcement completion)`) replacing the tree's
  `authors: [Iris, Mike]`, so that against the LIVE jar the
  only plugin.yml differences are the version + registration
  (verified by diff — see BUILD-RECORD-1.7.md).
- `build.sh`: the jar step now writes
  `ExcelRewardGuard-1.7.jar` stamped 2026-11-10 (the carried
  script still named the 1.5 jar with a 2026-10-23 stamp), and
  the new test suite runs in the build.
- Tests: new `RewardGuardStatusTests` (39 assertions).
  README refreshed to 1.7 (it was the stale 1.4 README).
- NOTHING else: `config.yml` ships byte-identical to the live
  1.6 jar's; every other main/test source is byte-identical
  to the 1.6 tree.

## Artifacts

- ExcelRewardGuard-1.7.jar — 60,694 bytes
  SHA-256 d433749336609985d5c6b1fba8302fa476944517b9ac2e3f177f10b845babf89
  Deterministic double-build byte-identical.
- ExcelRewardGuard-1.7-source.zip — 84,954 bytes
  SHA-256 f5d8629ea5eae205d8243ac3bce2dc0f648182bf27ef69ae10119282d200f7f4
  (the whole ExcelRewardGuard-1.7/ tree, 33 files; timestamps
  pinned 2026-11-10.)

## Verification summary

Desk 539/539 (500 carried 1.6 assertions unchanged + 39 new
status assertions). Control proof: of the live 1.6 jar's 25
classes, 24 are byte-identical in the 1.7 jar; the main class
differs (the command) and 2 classes are new (RewardGuardStatus
+ its Snapshot record) — 27 classes total. Rig on a minimal
Paper 26.2-123 server with ONLY the 1.7 jar (rig-1.7/ in the
build folder): clean v1.7 enable, zero SEVERE, clean stop,
8/8 behavior checks typed on the real console stdin — the
exact 6-line status reply, usage lines for bare / bogus /
extra-arg forms. NOT installed — built + staged only.
