# ExcelWitherSkulls 0.1.0 — BUILD RECORD

Built Oct 21, 2026 (evening, CDT) from the commission: the skull
piece of the trimmed rewards package Mike approved Oct 21
(Jester's feedback; audit `~/workspace/elitemobs-feedback-2026-10-21/FEEDBACK-REVIEW.md`
Part 2). Spec: `~/workspace/excelwitherskulls/SPEC.md`.
STAGED ONLY — not installed on the live server.

## Artifact
- `ExcelWitherSkulls-0.1.0.jar` — 15,034 bytes
  SHA-256 `6797ce0dce6afcc451d486d1907b9d47171f5d83f1abce3063057bc243b56d9a`
  MD5 `5a32e0acdd24c30886df02c34af70086`
- `ExcelWitherSkulls-0.1.0-source.zip` — SHA-256
  `3059de776e594b018e20623d2fc9dc6df6748aa9e6cc6be12cbe09c8ba2a2270`,
  MD5 `d1aefb2786877b9803b2715751c7da7a`
- Rebuild reproduction: two consecutive project builds
  byte-identical; a clean-room rebuild from the source zip
  byte-identical; and both identical to the jar the rig tested
  (the rig's own copy hashes the same). Three-way match.
- SUPERSEDED INTERMEDIATE, recorded honestly: the first
  double-build during the classpath-fix window produced a
  different hash (d02ee0d7…, 13,643 B, two runs agreeing). That
  build overlapped the build-script classpath migration (javac 25
  dies completing org.bukkit.entity.Entity without the JetBrains
  annotations + full house classpath — a compiler bug, bisected
  to the Entity.class reflection literal) and was never
  rig-tested, never deposited, and is not the shipped artifact.
  The shipped artifact's chain (source zip → build → rig →
  deposit) is verified end to end at 6797ce0d….
- Drive deposit (Minecraft WRV folders, MD5-verified against
  local after upload): jar → jars folder; source.zip +
  SOURCE.md → sources folder. Drive MD5s match local exactly.

## Unit tests (plain JVM, house Check harness)
65 checks, 0 failures (also re-run green on the clean-room tree):
- SkullMathTest (24): vanilla table 2.5/3.5/4.5/5.5 by Looting;
  difference formula at Looting 0–3 (22.5/21.5/20.5/19.5);
  target ≤ vanilla → 0; clamps (negative, >100, NaN); realized
  totals 24.4375 (Looting 0) and 23.9275 (Looting III) at the
  25 target; roll boundary (roll == chance misses).
- TopUpDeciderTest (22): every spec edge as a row — skull
  already present (no double even on roll 0), non-player kill,
  elite non-wither, non-elite wither, target below vanilla
  effective (3.0 vs Looting III), disabled-beats-everything,
  verdict-order pins, target-100 boundary exactness.
- SkullSettingsTest (12): defaults, overrides, clamps,
  NaN/infinity fallback.
- PluginYmlGuardTest (7): EliteMobs softdepend only, no hard
  depend line, identity fields.

## Rig proof (scratch rig, Paper 26.2 build 123 + EliteMobs 10.9.10, live-mirror EM config)
Driver: purpose-built fake-player rig (`scratch/driver/`), mobs
converted through EM's own public `EliteEntity` constructor
(level 3, SpawnReason.NATURAL), killed by player-attributed
damage; an independent MONITOR census in the driver reads the
FINAL drops and cross-checks PersistentTagger against
spawn-time ground truth on every kill.

- Boot: **SEVERE 0**; plugin enable line: "top-up ON, target
  25.0% (realized 24.44%), EliteMobs seam BOUND"; zero
  ExcelWitherSkulls WARN/ERROR lines in the whole run.
- Batch 1 — ELITE ×60 player kills @ target 25: driver census
  **60 kills, 18 skulls (30.0%)** vs realized expectation
  24.44% → 14.7 ± 3.3 (within ~1σ). Plugin verdicts close the
  arithmetic exactly: 16 ADDED + 42 ROLL_MISS + 2
  SKULL_ALREADY_PRESENT = 60; census skulls = 16 + 2 vanilla.
- Batch 2 — PLAIN ×60 player kills: census **60 kills, 1 skull
  (1.7%)** ≈ vanilla 2.5%; the plugin logged no verdicts and
  added nothing. Non-elites are untouched, proven in-stack.
- Batch 3 — ELITE ×30, NO player attribution (setHealth(0)):
  census **30 kills, 0 skulls**; the plugin logged exactly 30
  NOT_PLAYER_KILL verdicts — the player-kill gate held on every
  one.
- Live reload mid-run: config edited to target 100,
  `/witherskulls reload` answered "reloaded - top-up ON, target
  100.0%" and took effect immediately — reload safety proven in
  the stack, no restart.
- Batch 4 — ELITE ×60 player kills @ target 100: census **60
  kills, 60 skulls (100%)**; verdicts 60 ADDED / 0 MISS —
  the near-deterministic separation proof (realized expectation
  97.56%).
- Integrity counters across all 210 tracked kills:
  TAGGER-MISMATCH 0 (the seam's answer agreed with spawn-time
  truth every time), MULTI-SKULL 0 (never more than one skull
  in any final drop list), CONVERT-FAILED 0.

Rig runs 1–2 (driver defects, fixed before run 3; the plugin
was not changed): run 1 stalled when the first conversion raced
EliteMobs' async post-boot content load (NPE inside EM's
randomizePowers) and a knocked-off elite fell out of honest
reach — EM anti-exploit cancels far damage, so the kill loop
spun on one mob at 4.24 HP on the ground. Driver fixes: 45 s
post-boot settle in the run script, conversion retry, mob
pinned beside the player, stuck-mob removal without death.
Run 2 was aborted for speed (level-10 elites + EM damage
normalization ≈ 2.4 kills/min); run 3 used level 3 and
50-damage hits (~6 kills/min).

Known cosmetic wart, documented not hidden: the debug line's
elite level prints -1 in run 3 (it printed correctly in run 1).
The level readout resolves through EM's live EliteEntity
registry, which EM clears during its own death handling before
HIGHEST priority; the DECISION read (PersistentTagger) is
unaffected — proven by the 210-kill mismatch count of 0. The
level is debug-text only. Left as-is in 0.1.0 so the shipped
jar stays byte-identical to the rig-tested jar; a fix would be
a 0.1.1 with its own rig pass.

## Install (when Mike calls it)
Empty-server window, can ride a future combined window: drop
the jar in plugins/, boot, confirm the seam BOUND line. No
config file needs to pre-exist (defaults are the approved
values). Rollback = remove the jar + restart; the plugin keeps
no data files. Post-install player proofs owed (one at a time):
an elite wither kill each on Java and Bedrock during natural
play, or the first fortress session's drop rate — Mike's call.
