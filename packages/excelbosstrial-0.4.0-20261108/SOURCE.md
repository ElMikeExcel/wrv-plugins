# ExcelBossTrial 0.4.0 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** ExcelBossTrial is the server's governed boss-fight plugin
  (Wither / Warden / Dragon / Elder chapters: pool-space damage governing,
  verdicts, sprinkle adds, seal loot via GuildGate). Design of record:
  `~/workspace/bosstrial-fix-20261108/DESIGN.md`; backlog ㉚ in
  `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`.
  (The in-tree SOURCE.md this replaces was the stale 0.3.5 note carried in
  the copied 0.3.7 base tree; this file is the 0.4.0 source of record for
  the package.)
- **0.4.0 delta (backlog ㉚, the boss-fight fix packet):** built on the
  proven-live 0.3.7 source tree (baseline suite reproduced first:
  27,142 checks / 0 failures). PM-8 fight-record persistence (every
  ending + 60 s mid-fight snapshots); PM-1 heal-aware reconciliation
  (net-damage pool math, HealMath); PM-2 VerdictJudge + pool-exhaustion
  verdict via a governed lethal blow; PM-9 unattributed accounting
  (GovernorStash closes the ×21 amplification structurally); PM-4/PM-5
  bar truth (named number spaces, net bar, FALLING state); PM-6 sprinkle
  rework (SprinkleCadence judge — milestone waves guaranteed, DEFER
  unspent); PM-10 signature-aware grantclaim; PM-12
  closeEncounterForSeal API + maintenance grant watch; PM-11
  BossRewards auto-present (silent skips, one waiting line, Bedrock
  Cumulus fallback policy). Suite: 27,225 checks / 0 failures.
- **REBUILD of record (Nov 8, 2026, evening):** the shipped candidate is
  the clamp-raise rebuild — sprinkle per-fight sanitizer clamp raised
  48 → 140 (Mike's ~5:57 PM sprinkle-volume ruling), Wither taunt lines
  added, loud clamp WARNING on boot when a config value is clamped.
  The original candidate (393,906 B / 0638c158…) is SUPERSEDED, never
  install it. Rig proof: `~/workspace/bosstrial-fix-20261108/build/rig-final/`
  (config 999 → clamped to 140 warning line; cap-140 boot line; live
  taunt on a real eat; PM-3 proven).
- **Source:** this folder (`src/com/winkreyes/excelbosstrial/` + the
  `src/wrv/` rewards tree), built by `package.sh` / `build-test.sh`
  (deterministic double-build byte-identical for the shipped jar).
- **Candidate jar:** `ExcelBossTrial-0.4.0.jar`, 401,269 B,
  sha256 `043c15588ed844fa94ba3e93149101bca45e8e1366d8e71e433fbe436c1bafc3`
  (at `~/workspace/bosstrial-fix-20261108/build/ExcelBossTrial-0.4.0.jar`).
- **Status:** INSTALLED Nov 8, 2026, 10:18–11:04 PM CST as one half of
  the BossTrial 0.4.0 + ExcelProtect 0.1.8 pair (parent hand window;
  backup "Boss pair pre-install 1108"; config merge verified on disk —
  sprinkle 120 s / 16 alive / 140 per fight, taunts ARMED, no clamp
  warning; boot 10:59 PM Done 19.893 s, 78 plugins, SEVERE 0; MANIFEST
  INSTALLED entry + marker
  `~/workspace/bosstrial-fix-20261108/build/INSTALLED.txt`).
  Follow-on 0.4.1 (fight-stats surface) installed separately Nov 8,
  11:26 PM. Post-install player proofs (chapter re-acceptance fights,
  Item 35 V-1) still owed — player-proof class.
- **Package staged:** Nov 8, 2026, board-manager run 78, from
  `~/workspace/bosstrial-fix-20261108/build/bosstrial/` (full source tree
  + BUILD-RECORD.md + PACKET-DRAFT.md + this SOURCE.md; jar itself NOT
  in the package per house rule — it rides the Drive deposit).
  Publication (GitHub draft record PR + Drive deposit) is the next leg,
  together with the ExcelProtect 0.1.8 package staged in run 77.
