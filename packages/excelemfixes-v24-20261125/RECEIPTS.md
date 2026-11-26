# RECEIPTS — ExcelEMFixes v24 (party-drops fold, Wave 3 / D245) — renamed from 0.24.0 per D247 (Nov 25, 2026)

## Live-premise receipts (G1 — fresh Homebase SSH pull, Nov 25, 2026, ~9:51 PM CT / 2026-11-26T03:51Z)

Route: `ssh -F ~/.ssh/config mike-pc` + `scp` of the files;
directory listings via PowerShell Get-ChildItem on the PC.
Pulled copies in `~/workspace/excelemfixes-partydrops-20261125/live-pull/`.

- Live `plugins/ExcelEMPartyDrops-0.1.0.jar` — 14,543 B — SHA-256
  `12f3352341b42a14a19977e0ee43a97eee13b1e79062c8e0e0345e33ed0f8df8`
  (the standalone being retired; byte-identical to the jar the
  Nov 25, 2026 parent-seat rig proof proved over 120 bot kills).
- Live `plugins/ExcelEMPartyDrops/config.yml` — 2,117 B — SHA-256
  `7f64d4b4ac8d16594f310bbe8c054c492229808e442d47eae20e30863b30dc31`
  (pulled copy: `live-pull/live-ExcelEMPartyDrops-config.yml`).
  The legacy `plugins/ExcelPartyDrops/config.yml` is byte-identical
  (same size + hash). Values read from the pulled bytes:
  `enabled: true`, `party-radius: 50.0`, `step-per-extra: 0.5`,
  `multiplier-cap: 3.0`, `participant-cap: 5`,
  `respect-farming-cap: false`, `hint-cooldown-seconds: 600`,
  `debug: false` — IDENTICAL to the module's shipped defaults
  (which are the spec values); the staged deploy config therefore
  carries the live values with no transcription change (the
  audit's named failure shape, diffed and cleared).
- Live `plugins/ExcelEMFixes/config.yml` at pull time — 87,117 B —
  SHA-256 `daa7b8bde577447fc417523cfe416f8b7d65074acef3cf40081e0046600cd102`
  (pulled copy: `live-pull/live-ExcelEMFixes-config.yml`) — this is
  the 0.23.0 deploy config (the wither-skulls section is present):
  the 0.23.0 window was mid-flight at pull time (failsafe fired
  9:46 PM CT; the plugins listing at 9:50 PM CT still showed
  `ExcelEMFixes-0.22.0.jar`, 596,100 B, as the only non-backup
  ExcelEMFixes jar). CONSEQUENCE (G1): this packet's live-premise
  gate for the ExcelEMFixes jar is NOT stated from this pull —
  it must be re-pulled fresh at filing time, after the 0.23.0
  install closes, and will read `ExcelEMFixes-0.23.0.jar`
  609,952 B / `48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b`
  if that install landed as built. That is why this packet is
  staged, not filed.

## Candidate

- `ExcelEMFixes-v24.jar` — 626,284 B — SHA-256
  `372c41031c8336a78d1113a1e9778c0fbcadd3afbb15c1840e16c56b0e042df9`;
  deterministic (repeated builds byte-identical). Renamed per D247:
  rebuilt from the same source with only the version strings changed
  (plugin.yml + VERSION constant 0.24.0 → v24); the pre-rename jar was
  626,286 B / SHA-256 9f2dc14335eef065c2e03fd064fde9e801d81cf2b999ef3472ed0dc3342566d3.
- `deploy-config.yml` — 91,671 B — SHA-256
  `5a4a9468808a540409f1fee66ced8aab78c36acebeae5d25abd009ad59b63a01`;
  built from the live config pull above plus ONLY the
  `modules.party-drops` section; diff vs the live pull:
  +105 lines / −0 lines (pure addition, verified by diff).

## Tests

- Full unit suite: 2,075 passed / 0 failed on the clean run
  (0.23.0 baseline 1,972 + 103 new party-drops checks: the
  standalone's MultiplierMath / PartyRange / ParticipantCounter /
  OriginGate / HintGate suites ported verbatim + 9 plugin.yml/
  config guard checks incl. "no hard depend line" and "no
  Together dependency"). One additive harness change: the pack's
  shared test `Check` gained the standalone's `near()` tolerance
  helper (no existing check altered).
- Known flake (pre-existing, documented in the 0.22.0/0.23.0
  records): SkillNudgeMathTest tie check may fail intermittently;
  it failed in 1 of this build's runs, in a module this fold
  does not touch; the clean run above is the recorded result.

## Rig (Paper 26.2 + EliteMobs 10.9.10; PdModuleRig driver in
`rig-driver/` — the Nov 25 standalone proof's LOWEST/MONITOR
drop-sum bracketing + NATURAL-spawn conversion tracking, with
NMS fake players in place of the mineflayer bots: PdRigA the
killer at the pad, PdRigB a bystander 10 blocks away dealing
zero damage; EM spawn percentage set to 1.0 in the RIG copy of
MobCombatSettings.yml only; logs in `rig/`)

- Phase A boot: "Party Drops ready: ON, radius 50.0 blocks,
  step 0.5, multiplier cap 3.0, participant cap 5, EliteMobs
  hooked"; 26 modules enabled (party-drops among them);
  `/partydrops status` → "ExcelPartyDrops: ON, radius 50.0
  blocks, step 0.5, multiplier cap 3.0, participant cap 5,
  EliteMobs hooked"; SEVERE 0.
- SOLO leg (PdRigA alone): 30 natural elite kills, every kill
  verified natural=true / reason=NATURAL / antiExploit=false,
  nonEliteKills=0 — drops 32 → 32, ratio exactly 1.000,
  multipliedKills=0, 0 audit lines. Solo is exactly vanilla.
- PARTY leg (PdRigB present 10 blocks away, zero damage):
  30 kills, same gate verification, nonEliteKills=0 — drops
  22 → 34 at multiplier 1.5 (exact-1.5 expectation 33; +1 is
  inside the per-stack Bernoulli noise, the same
  exact-in-expectation behavior the standalone proof measured:
  45 → 66 vs 67.5 expected); multipliedKills=12; 16 audit
  lines, EVERY one "participants=2 multiplier=1.5" — the
  MONITOR-timing mutation lands in the drop list before Paper
  scatters it (the MONITOR census probe reads the multiplied
  list). Sample: "multiplied elite drops: type=ZOMBIE level=5
  participants=2 multiplier=1.5 items 2 -> 3".
- Phase B (modules.party-drops.enabled=false, party setup —
  PdRigA + PdRigB both present): boot lists 25 modules,
  party-drops absent, no Party Drops ready line; 30 kills,
  same gate verification, nonEliteKills=0 — drops 35 → 35,
  ratio exactly 1.000, multipliedKills=0, 0 audit lines;
  SEVERE 0. Toggle OFF = exact vanilla, solo and party alike.
- Rename note (D247): the rig ran on the pre-rename jar
  (0.24.0, 626,286 B / 9f2dc143…). A jar-entry diff against
  the v24 candidate shows exactly TWO differing entries —
  plugin.yml (the version line only) and
  excelemfixes/ExcelEMFixes.class (the VERSION constant) —
  every party-drops class and all other content byte-identical,
  so the rig verdicts above carry to the candidate verbatim.

## Standalone proof reused (not re-proven)

- Parent-seat rig proof, Nov 25, 2026
  (`~/workspace/partydrops-proof-20261125/RESULTS.md`): the
  live standalone jar (hash above) against live EM 10.9.14,
  120 real mineflayer bot kills — SOLO 49 → 49 (1.000), PARTY
  45 → 66 (1.467 aggregate at 1.5), FAR (150 blocks) 48 → 48
  (1.000), every kill natural/NATURAL/not-antiExploit. PASS.

## D246 / gate-7 evidence

- Shipped defaults vs WRV values: enabled true / true;
  party-radius 50.0 / 50.0; step-per-extra 0.5 / 0.5;
  multiplier-cap 3.0 / 3.0; participant-cap 5 / 5;
  respect-farming-cap false / false; hint-cooldown-seconds
  600 / 600; debug false / false — WRV runs the shipped
  defaults unchanged (stated in README + config comments).
  No WRV-specific data exists for this module (no names,
  paths, webhooks, content IDs) — nothing to confine to a
  local override.
- Hardcoding sweep (script: `grep -rn "Wink|WRV|winkreyes|Jester|Marvin|duckdns|webhook"
  src/src/excelemfixes/partydrops/`): ONE hit — the letters
  "WRV" inside the D246 documentation comment in
  PartyDropsModule.java that gate 6 requires (the same pattern
  as wither-skulls' reason comment). No WRV literal in code
  logic or in any shipped default value.
- Dependency: plugin.yml `depend` — none added (guard-tested);
  softdepend list unchanged. Cross-system legs: EliteMobs
  death event only (the pack's existing softdepend; the EM
  listener class is loaded/registered only when EliteMobs is
  present and enabled — absent, the module logs "EliteMobs NOT
  found - inert", no drops are multiplied, and the pack boots
  clean). ExcelTogether: NO leg at all — PartyRange is a
  documented COPY of PartyBonus.within() semantics, not a call
  (guard-tested: no Together entry in plugin.yml). No other
  cross-system leg exists.

## Filing refresh (G1 re-pull at filing, Nov 25, 2026, 10:15 PM CT / 2026-11-26T04:15Z)

Route: `ssh -F ~/.ssh/config mike-pc` + `scp` (same route as the
9:51 PM receipts above). Gate discharged: ExcelEMFixes 0.23.0
installed by failsafe run 20261125-2146, verified closed by the
supervisor (run 962 close-out, Nov 25, 2026, 10:12 PM CT).
Pulled copies in `~/workspace/excelemfixes-partydrops-20261125/live-pull/`
(filing-refresh suffixes).

- Live `plugins/ExcelEMFixes-0.23.0.jar` — 609,952 B — SHA-256
  `48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b`
  — EXACT match to the 0.23.0 packet jar (the packet gate).
- Live `plugins/ExcelEMFixes/config.yml` — 87,117 B — SHA-256
  `daa7b8bde577447fc417523cfe416f8b7d65074acef3cf40081e0046600cd102`
  — byte-identical to the staging pull, so the staged
  `deploy-config.yml` (91,671 B / `5a4a9468...`, +105 / -0 lines)
  stands as built; NO regeneration needed.
- Live `plugins/ExcelEMPartyDrops-0.1.0.jar` — 14,543 B — SHA-256
  `12f3352341b42a14a19977e0ee43a97eee13b1e79062c8e0e0345e33ed0f8df8`
  — unchanged from the 9:51 PM receipt.
- Live `plugins/ExcelEMPartyDrops/config.yml` — 2,117 B — SHA-256
  `7f64d4b4ac8d16594f310bbe8c054c492229808e442d47eae20e30863b30dc31`
  — unchanged from the 9:51 PM receipt.

Verdict: every live premise CONFIRMED fresh; the packet gate
below is now stated from this pull. Packet FILED into the
MANIFEST as a PENDING pointer stub (see MANIFEST).
