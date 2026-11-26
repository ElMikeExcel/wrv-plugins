# BUILD RECORD — ExcelEMFixes 0.23.0, wither-skulls module (Wave 2 fold of ExcelEMWitherSkulls 0.1.0)

Built Nov 25, 2026 (evening, CT) by the Wave 2 worker lane, working
inside `~/workspace/excelemfixes-witherskulls-20261125/` on top of
the canonical 0.22.0 tree
(`~/workspace/excelemfixes-questxp-fold-20261125/`, jar 596,100 B /
49eae192… — installed live Nov 25, 8:51 PM CT, failsafe run
20261125-2042). Ruling: **D245** (consolidation programme Wave 2).
Creation reason carried in README + config comments, verbatim in
substance from the audit/programme: Jester's Oct 21, 2026 feedback
"elite rewards aren't worth it" — EM neither suppresses nor boosts
vanilla drops for natural elites, leaving elite wither skeletons
at vanilla 2.5%; WRV runs ~25%. Not installed by this lane; no
panel contact; no messages sent.

## Artifact

- `packet-0.23.0/ExcelEMFixes-0.23.0.jar` — 609,952 B — sha256
  `48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b`
- Deterministic: repeated builds byte-identical.
- Version bumped in `ExcelEMFixes.VERSION`, `plugin.yml`,
  `build.sh`. ModuleRegistry registers `WitherSkullsModule` last —
  25 modules total.

## What was ported (0.1.0 source → `excelemfixes.witherskulls`, from source, never memory)

- Verbatim ports (package line only): `SkullMath`, `SkullSettings`,
  `TopUpDecider` (the pure probability math, settings clamp, and
  verdict table).
- `EmGateway` — the reflective EliteMobs tagger seam, unchanged
  except the log prefix (fails safe to "not elite", never throws).
- `SkullListener` — behavior verbatim (EntityDeathEvent HIGHEST,
  player-kill attribution via getKiller(), Looting from main hand,
  at most one skull appended); settings now read from a supplier
  fed by the module.
- Standalone main + command → `WitherSkullsModule` (FixModule +
  CommandExecutor + TabCompleter): `/witherskulls` (alias
  `/wskulls`) status|reload, permission node
  `excelwitherskulls.admin` declared in plugin.yml (kept from
  0.1.0 so existing grants carry over) — command name/shape
  FROZEN per programme gate 8. Reload re-reads the pack config
  and swaps the settings snapshot.
- Config: `modules.wither-skulls` section (enabled /
  target-chance-percent / debug-log) with the reason comments;
  shipped defaults 25.0 / false.

## Tests

- Full unit suite: **1,972 passed / 0 failed** on the clean run
  (0.22.0 baseline 1,908 + 64 new: the standalone's SkullMath /
  SkullSettings / TopUpDecider suites ported verbatim + 5
  plugin.yml/config guard checks incl. "no hard depend line").
- Known flake: SkillNudgeMathTest tie check failed intermittently
  in 2 of 4 runs — pre-existing, documented in the 0.22.0 record,
  in a module this fold does not touch; pristine 0.22.0 tree
  passes 1,908/0 in the same session.

## Rig verdicts (Paper 26.2 + EM 10.9.10; standalone's proven SkullRigDriver + MONITOR census; logs in `rig2/`)

- **Phase A** (target 100.0, debug on): ready line "Wither Skulls
  ready: top-up ON, target skull chance 100.0% (realized 97.56%
  with no Looting), EliteMobs seam BOUND"; 25 modules; SEVERE 0.
  - 100% leg: ELITE 40 kills → **40 skulls** (38 module ADDED +
    2 vanilla).
  - PLAIN 20 → 1 skull (vanilla only); ELITE no-player-kill 10 → 0.
  - 0% leg (config → target 0.0, `/witherskulls reload` →
    "reloaded - top-up ON, target 0.0%"): ELITE 40 → 2 skulls,
    **0 module adds** (vanilla-only; module inert at 0%).
  - TAGGER-MISMATCH 0 / MULTI-SKULL 0 / CONVERT-FAILED 0.
- **Phase B** (modules.wither-skulls.enabled=false, target 100):
  ready line lists 24 modules (wither-skulls absent, command not
  registered); ELITE 40 → 0 skulls, 0 adds; SEVERE 0. Toggle OFF
  = exact vanilla rate.
- Rig honesty note: the first Phase A attempt booted with an
  invalid plugin.yml (build lane appended a description sentence
  at EOF) and ExcelEMFixes did not load; fixed (sentence moved
  onto the description line, YAML re-validated), jar rebuilt,
  Phase A re-run clean from scratch. Spoiled boot used for
  nothing.

## Live-premise receipts (G1, fresh Homebase SSH pull Nov 25, 2026 ~8:59 PM CT)

Full detail in `packet-0.23.0/RECEIPTS.md`
(sha256 `02191aa2fc25ce076137229e238f81be0956b344288c4ede246bf4b77c1a7d03`):

- Live ExcelEMFixes 0.22.0 jar: 596,100 B / `49eae192…`
- Live ExcelEMFixes config.yml: 84,251 B / `e8b67009…`
- Live ExcelEMWitherSkulls-0.1.0.jar: 14,862 B / `9361dd79…`
  (differs from the Oct 21 source-record jar — live is the gate)
- Live ExcelWitherSkulls config.yml: 1,507 B / `a0c5a868…`;
  values enabled true / target-chance-percent **25.0** /
  debug-log false — **diffed against the staged module config:
  identical** (no transcription change; the audit's named failure
  shape cleared).
- Deploy config: live config + only the wither-skulls section
  (+62 / −0 lines), 87,117 B / `daa7b8bd…`.

## D246 gates

- Toggleable + config-driven: yes (`modules.wither-skulls.enabled`,
  `target-chance-percent`, `debug-log`); module-off rig-proven.
- Shipped defaults vs WRV values: target 25.0 / 25.0, debug
  false / false, enabled true / true — WRV runs the shipped
  defaults unchanged (stated in README + config comments).
- Hardcoding sweep: `grep -rn "Wink|WRV|winkreyes|Jester|Marvin|duckdns|webhook"`
  over the module package → ONE hit, the player name inside the
  creation-REASON comment that gate 6 requires (same pattern as
  quest-xp). No WRV literal in code logic or shipped defaults.
  No WRV-specific data exists for this module → no local override
  needed.
- Gate 7 (self-contained): plugin.yml gains NO hard depend
  (guard-tested); EliteMobs reached only via the pack's existing
  softdepend + reflective seam; absent EM → seam reads "not
  elite", top-up inert, pack boots clean. No other cross-system
  leg exists. Absent-plugin behaviour stated in the packet.

## Packet + filing (D212 — finish line is FILED)

- Packet folder: `packet-0.23.0/` — INSTALL-PACKET.md
  (**Lint: 5106 B / max line 95 / non-ASCII 19**), RECEIPTS.md,
  SURFACES.md, SHA256SUMS (verified OK ×2), jar, deploy-config.yml.
- Packet directs the window to remove the standalone
  ExcelEMWitherSkulls jar in the SAME window, only after module
  proof, kept in the backup set as rollback.
- MANIFEST PENDING pointer stub filed under
  `~/workspace/wrv-install-queue/MANIFEST.md` ("## PENDING —
  ExcelEMFixes 0.23.0 (wither-skulls module …) (2026-11-25)")
  and read back.

## Surfaces staging (SURFACES.md in the packet folder)

- In-game What's New entry text: STAGED.
- Website What's New + plugin row 0.22.0→0.23.0: STAGED TEXT.
- Records draft PR: OWED — close-out leg after install (not
  pushed by this lane, per brief).
- Guide: hibernated — verify-no-change noted (no behavior,
  command, or rate change).
- Tips + ExcelMenu/ExcelBedrockMenu: checked, no change owed.

## Finish-and-fire drops (D228)

Filed 2 drops to `~/hooks/state/followon-drops/`:
1. 0.23.0 surfaces publication after install (What's New ×2 +
   website row, per SURFACES.md).
2. 0.23.0 records draft PR (wrv-plugins, close-out leg).
Drop files: `drop-1795664653344.json` (surfaces publication) and
`drop-1795664654351.json` (records draft PR), both in
`~/hooks/state/followon-drops/`.
