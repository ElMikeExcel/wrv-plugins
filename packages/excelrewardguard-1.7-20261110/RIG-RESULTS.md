# RIG RESULTS — ExcelRewardGuard 1.7

Minimal Paper 26.2-123 rig (the runtime-server skeleton at
`~/workspace/excelbuildmode-pilot/runtime-server/`, incl. its
cached `mojang_26.2.jar` — the sandbox cannot download it),
ONLY the 1.7 jar in plugins/, offline mode, commands typed on
the real console stdin from `cmdfile.txt`. Run in /tmp/rg17rig;
`boot.log` + `cmdfile.txt` in this folder are the evidence.
No softdepend plugins were present (Jobs / mcMMO / EliteMobs /
GuildGate / BossTrial all absent) — the supported fail-safe
shape, exactly as 1.6 behaves without them.

| # | Check | Result |
|---|---|---|
| 1 | Plugin list shows ExcelRewardGuard (1.7); "Loading server plugin ExcelRewardGuard v1.7" | PASS |
| 2 | "Enabling ExcelRewardGuard v1.7" + all 1.6 enable lines keep their shape (provenance, farm rates 33%/33%, seal step, fall 50%, audit OFF, boss band INACTIVE fail-safe, Jobs/mcMMO/EliteMobs not-found, seal source UNAVAILABLE, vanilla XP line) | PASS |
| 3 | SEVERE count in the whole boot log | 0 |
| 4 | `rewardguard status` (console) → the exact 6 status lines: farm rates 33%/33%, boss band 25% feed inactive, fall threshold 50% / 0 tracked, interventions Jobs 0 / mcMMO 0 / elite 0, seal step ON with seals not yet read, audit OFF / debug OFF / window 2 ticks / origin window | PASS |
| 5 | Bare `rewardguard` → "Usage: /rewardguard status", mutates nothing | PASS |
| 6 | `rewardguard bogus` → usage line | PASS |
| 7 | `rewardguard status extra` → usage line (exactly-one-arg shape) | PASS |
| 8 | Clean `stop`: "Disabling ExcelRewardGuard v1.7", worlds saved, exit 0 | PASS |

8/8. The only ERROR lines in the log are Paper's own sandbox
network failures (version-info / yggdrasil fetch over blocked
SSL) — present in every local rig on this VM, not attributable
to the plugin; SEVERE is the packet's gate and it is 0.

Limitations, stated plainly: the in-game denial for a non-op
sender is Bukkit's plugin.yml gate (the `excelrewardguard.admin`
node, default op, also declared as the command's permission) —
pinned by RewardGuardStatusTests' gate checks, not observed
with a joined player, because the rig joins none and the
command is operator-facing only. With the live softdepends
present, the status reply's feed/seal fields read their live
values through the same snapshot code the rig exercised in
their absent shape.
