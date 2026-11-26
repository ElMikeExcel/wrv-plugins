# RECEIPTS — ExcelEMFixes 0.23.0 (wither-skulls fold, Wave 2 / D245)

## Live-premise receipts (G1 — fresh Homebase SSH pull, Nov 25, 2026, ~8:59 PM CT / 2026-11-26T02:59Z)

Route: `ssh -F ~/.ssh/config mike-pc` + `scp` of the config files;
hashes cross-checked against PowerShell Get-FileHash on the PC.

- Live `plugins/ExcelEMFixes-0.22.0.jar` — 596,100 B — SHA-256
  `49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04`
  (matches the 0.22.0 build of record; 0.22.0 installed live Nov 25,
  8:51 PM CT, failsafe run 20261125-2042).
- Live `plugins/ExcelEMFixes/config.yml` — 84,251 B — SHA-256
  `e8b670092ca423e0fd0e5ae7d555ea55729de89dfb3efe592b04de96bbe17086`
  (pulled copy: `live-pull/live-ExcelEMFixes-config.yml` in the
  build folder).
- Live `plugins/ExcelEMWitherSkulls-0.1.0.jar` — 14,862 B — SHA-256
  `9361dd79f9b0dd639d2b0256bfb866f57d3160ebe1e850e718d01a80e6abf8ed`
  (the standalone being retired; note it differs from the Oct 21
  source-record jar 15,034 B / 6797ce0d… — the live jar is the gate
  value, per G1).
- Live `plugins/ExcelWitherSkulls/config.yml` — 1,507 B — SHA-256
  `a0c5a868fe05d38555fd8b1467b053193fcf82d525f15b1c1ec3a7c81a10da46`
  (pulled copy: `live-pull/live-ExcelWitherSkulls-config.yml`).
  Values read from the pulled bytes: `enabled: true`,
  `target-chance-percent: 25.0`, `debug-log: false` — IDENTICAL to
  the module's shipped defaults; the staged deploy config therefore
  carries the live value 25.0 with no transcription change (the
  audit's named failure shape, diffed and cleared).

## Candidate

- `ExcelEMFixes-0.23.0.jar` — 609,952 B — SHA-256
  `48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b`;
  deterministic (repeated builds byte-identical).
- `deploy-config.yml` — 87,117 B — SHA-256
  `daa7b8bde577447fc417523cfe416f8b7d65074acef3cf40081e0046600cd102`;
  diff vs the live config pull: +62 lines / −0 lines (only the
  `modules.wither-skulls` section added).

## Tests

- Full unit suite: 1,972 passed / 0 failed on the clean run
  (0.22.0 baseline 1,908 + 64 new wither-skulls checks: the
  standalone's SkullMath / SkullSettings / TopUpDecider suites
  ported verbatim + 5 plugin.yml/config guard checks).
- Known flake: SkillNudgeMathTest "armor named first on the tie"
  failed intermittently in 2 of 4 runs (pre-existing, documented in
  the 0.22.0 build record; HashMap-order tie in a module this fold
  does not touch; the pristine 0.22.0 tree passes 1,908/0).

## Rig (Paper 26.2 + EliteMobs 10.9.10, standalone's proven SkullRigDriver; logs in `rig2/`)

- Phase A boot: "Wither Skulls ready: top-up ON, target skull
  chance 100.0% (realized 97.56% with no Looting), EliteMobs seam
  BOUND"; 25 modules enabled; SEVERE 0.
- 100% leg: ELITE 40 kills → 40 skulls (38 module "ADDED 1 skull"
  debug lines + 2 vanilla); PLAIN 20 → 1 skull (vanilla only);
  ELITE no-player-kill 10 → 0 skulls.
- 0% leg (same boot; config edited to target 0.0, `/witherskulls
  reload` → "reloaded - top-up ON, target 0.0%", status confirms
  seam BOUND): ELITE 40 kills → 2 skulls, 0 module adds (vanilla
  2.5% expectation ~1; module provably inert).
- Phase B boot (modules.wither-skulls.enabled=false, target 100):
  ready line lists 24 modules, wither-skulls absent,
  `/witherskulls` not registered; ELITE 40 kills → 0 skulls,
  0 module adds; SEVERE 0.
- TAGGER-MISMATCH 0, MULTI-SKULL 0, CONVERT-FAILED 0 across both
  phases.
- Rig honesty note: the first Phase A attempt booted with an
  invalid plugin.yml (a description sentence appended at EOF by
  the build lane) and ExcelEMFixes did not load; the yml was
  fixed (sentence moved onto the description line, YAML
  re-validated), the jar rebuilt (hash above), and Phase A was
  re-run clean from scratch. No rig result from the spoiled boot
  is used.

## D246 / gate-7 evidence

- Shipped defaults vs WRV values: target-chance-percent shipped
  25.0 / WRV live 25.0 (identical); debug-log shipped false / WRV
  false; module enabled shipped true / WRV true. No WRV-specific
  data exists for this module (no names, paths, webhooks, content
  IDs) — nothing to confine to a local override.
- Hardcoding sweep (script: `grep -rn "Wink|WRV|winkreyes|Jester|Marvin|duckdns|webhook"
  src/src/excelemfixes/witherskulls/`): ONE hit — the player name
  in the creation-REASON comment in WitherSkullsModule.java,
  which programme gate 6 REQUIRES the module to carry (the same
  pattern as quest-xp's reason comment). No WRV literal in code
  logic or in any shipped default value.
- Dependency: plugin.yml `depend` — none added (guard test pins
  "no line starts with depend:"); EliteMobs remains in the pack's
  existing softdepend list, reached reflectively via EmGateway.
  Cross-system legs: EliteMobs elite tagger only (soft seam;
  absent → reads "not elite", top-up inert, pack boots clean —
  the seam's fail-safe direction, unit-pinned by the standalone's
  guard tests carried over). No other cross-system leg exists.
