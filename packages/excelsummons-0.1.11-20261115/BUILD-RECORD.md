# P-SUMMONS-TUNING-1 — BUILD RESULT (board supervisor run 364, worker W2)

**Verdict: BUILT. ExcelSummons 0.1.11 candidate ready for supervisor
filing (packet draft only — NOT filed to MANIFEST, NOT installed,
live server untouched, BB-11 packet/tree untouched).**

Reporting place: `~/workspace/board-r364/summons-build/`
(tree `ExcelSummons/`, packet draft `packet/`, rig log `rig/`,
base gate `base-gate/`, sums `SHA256SUMS`.)

## Base identity + ordering vs BB-11 (explicit)

- **Live lineage:** live is ExcelSummons 0.1.9 seq-02, candidate
  102,260 B / `ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46`,
  tree of record `~/workspace/summons-cap-build-20261114/ExcelSummons/`.
- **BB-11** already built + FILED (MANIFEST PENDING) ExcelSummons
  **0.1.10** on that tree: candidate 102,597 B /
  `97e442165b755fb7fac325e712f634c2607490bf7f0de0dd83b28510036ff7d0`,
  build `EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110`, not yet installed
  (no `INSTALLED.txt` in its folder at build time).
- **This build's base = the BB-11 0.1.10 build tree**
  (`~/workspace/back-button-wave3-r356/BB-11/build-0.1.10/ExcelSummons/`),
  COPIED into this folder — the BB-11 tree/packet were not mutated
  (post-build re-hash: BB-11 jar still `97e44216…`, its packet still
  `a2ebaf52…`). Basing on live 0.1.9 instead would either collide with
  the filed 0.1.10 version or regress the back-button fixes when
  installed after it; basing on the BB-11 tree is the only
  non-regressing reading, so there was no ambiguity to stop on.
- **Base gate PASS:** the copied tree rebuilt UNMODIFIED in
  `base-gate/` produced `97e44216…` byte-identical to the filed BB-11
  candidate, 109/109 tests, before any edit.
- **Ordering: installs AFTER BB-11.** The packet draft's live-jar gate
  is the 0.1.10 jar (`97e44216…`) with a STOP if BB-11 has not landed.
  Version is **0.1.11**, build `EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11`.

## What was implemented (design draft + D64)

1. **Raised hold table (D64)** in `config.yml`
   (`summon-reserve-percent`): archer 20 / bruiser 25 / guardian 35 /
   revenant 30 / ghast 35 / wolf 15 / familiar 10 / cat 10 / mount 15;
   ceiling 75, floor 20, adopted 0 unchanged. Test fixtures + all
   derived assertions re-pinned to the new math (incl. the D64
   coherence pins: elite trio raw 100 / applied 70, 5 wolves = exactly
   75, Mike's 2-Archer+Bruiser field = 65).
2. **18 modifier templates** staged as one replacement file
   (`packet/modifiers-replacement-0.1.11.yml`, 3,516 B /
   `62c6975e…`): all 18 `wrv_summon_reserve_*` keys, boosts
   = −(new% × count), description text updated; YAML-parsed, 18/18 keys
   verified. Packet = jar + config + modifiers in ONE gated unit.
3. **Nameplate owner format** (`Nameplate`): renders
   `Archer (MikeExcel) — 20/20` (em dash, per design); owner resolved
   online-first then offline, Floodgate names as Bukkit reports them;
   unresolvable owner renders the old plate (never a UUID). Strip is
   exact-match on the current owner's segment + health + countdown —
   re-render idempotence and parenthesised pet names are unit-tested.
   Final-15s countdown segment (`· 12s`) renders and strips.
   `ExcelSummons.menuPageFor` strips with the same owner-aware rule so
   menu base names stay the pet's name alone.
4. **Despawn legibility** (new pure `DespawnNotice`): reason enum
   EXPIRED / DISMISSED / DIED / UNLOADED (the §4.1 set, reusable by
   P-PROTECT-WATCH-1); fade warnings T-30 (durations ≥ 150 s only) +
   T-15 (all timed), once per threshold per summon, evaluated on the
   existing sweep tick; **one notify funnel** (`finalizeRemoval` in
   `SummonsListener`) now routes sweep expiry (EXPIRED), chunk-load
   overdue + grace-retired records (UNLOADED), death (DIED), and
   dismissals (DISMISSED). Scope dismissals aggregate to ONE summary
   line per action (`Dismissed: Archer, Archer, Guardian. (75% mana
   released.)`); single removals send the per-summon line with the
   record's APPLIED hold (adopted/clamp-skipped = no mana clause).
   Mount recast toggle sends the standard DISMISSED line — the old
   "Your mount drifts home." is retired per draft §4.3 (veto point 3).
   Caster logout/death and server shutdown stay silent by design
   (§4.1); `onDisable` was left as-is for that reason.

## Structured evidence

- **Candidate jar:** `packet/ExcelSummons-0.1.11.jar` (also in tree) —
  **107,064 B / SHA-256 `3d9b22a0ed3878912832760bd9df9e1f9979be7339fd7b49f805610643f7661a`**;
  fingerprint gate PASS (excel-build.txt + BuildInfo fragments).
- **Sources zip:** `packet/ExcelSummons-0.1.11-sources.zip` —
  146,198 B / `c2233881141f4722b33a00260659b07a16ffb6e336563c9e971f121bc988fadb` (40 files).
- **Candidate config:** `packet/config.yml` — 6,187 B /
  `696005bae6201ab93a8485394021a87cf304d151eda0af0d711944a078106df8` (= tree config).
- **Base identity + hash:** BB-11 0.1.10 tree (above); base-gate rebuild
  `97e44216…` EXACT, 109/109.
- **Tests:** **116 found / 116 successful / 0 failed** (109 carried,
  re-pinned to D64 + version 0.1.11; 7 new: owner plate format,
  idempotence/parenthesised names, countdown strip, notice strings
  ± mana clause, summary aggregation, warning thresholds per duration
  row, D64 worked fields).
- **Determinism:** build-script double-build byte-identical AND a
  second full build run reproduced `3d9b22a0…` exactly.
- **Rig verdict: PASS / SEVERE 0.** Local scratch rig (Paper 26.2-123 +
  Magic 11.2.5, candidate hash-verified in place, candidate config
  staged): `Enabling ExcelSummons v0.1.11`, enable line carries build
  `EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11`, `Done (89.525s)`,
  SEVERE count 0, clean disable. Log: `rig/boot-0111.log` (28,485 B /
  `6fb9e3e6…`). Scratch rig deleted after the boot, per BB-11 recipe.
  (Player-flow proofs — two-player plates, Archer stopwatch, per-reason
  triggers — remain the packet's post-install proof owed, as drafted;
  they need real clients and were not run by this desk build.)
- **Packet draft:** `packet/INSTALL-PACKET.md` — 4,811 B /
  `d29a5ca5…`; **lint: 4811 bytes / max line 66 / non-ASCII 0.**
  `packet/RECEIPTS.md` skeleton — 1,698 B / `66f735b0…`;
  lint: 1698 / 67 / 0. **G1 slots blank by design** (live-jar gate is
  pre-filled with the BB-11 candidate hash; same-day live pulls are
  the supervisor's filing-day step). NOT filed to MANIFEST.

## Notes / residuals for the supervisor

- Same-pass surfaces are named as owed in the packet draft (Guide +
  What's New both surfaces + website pair; old-percent text sweep) —
  not staged by this build worker.
- `dismissOneAdopted` (menu per-pet confirm) now also sends the funnel's
  DISMISSED line; the menu's own pre-existing confirmation message to
  the actor still fires — one extra similar line in that one flow,
  flagged rather than silently redesigning the menu text.
- The pre-existing mount-toggle `despawn(rec, false)` KEEP-decision
  semantics were preserved exactly; only its success wording source
  changed (funnel line instead of the flavor line).
