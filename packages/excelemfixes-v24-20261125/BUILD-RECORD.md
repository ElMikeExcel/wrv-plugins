# BUILD RECORD — ExcelEMFixes v24, party-drops module (Wave 3 fold of ExcelEMPartyDrops 0.1.0)

Built Nov 25, 2026 (evening, CT) by the Wave 3 worker lane,
working inside `~/workspace/excelemfixes-partydrops-20261125/`
on top of the canonical 0.23.0 tree
(`~/workspace/excelemfixes-witherskulls-20261125/`, jar
609,952 B / 48d0be09…). Built as 0.24.0; renamed to **v24**
mid-build per **D247** (Mike, Nov 25, 2026, 9:53 PM CT —
fleet-wide simple v-numbers; a concurrent lane applied the
rename to this folder: source VERSION + plugin.yml + build.sh
→ v24, jar rebuilt deterministically; this record reconciles
both names). Ruling: **D245** (consolidation programme
Wave 3). Creation reason carried in README + config comments,
verbatim in substance from the programme: Mike-approved spec,
Oct 20, 2026 — party play should make natural elites drop
more (1x–3x by party presence); solo stays exactly vanilla.
Not installed by this lane; packet STAGED, not filed (G1 —
0.23.0 still installing); no panel contact; no messages sent.

## Artifact

- `packet-v24/ExcelEMFixes-v24.jar` — 626,284 B — sha256
  `372c41031c8336a78d1113a1e9778c0fbcadd3afbb15c1840e16c56b0e042df9`
- Pre-rename jar (rig-proven): 626,286 B — sha256
  `9f2dc14335eef065c2e03fd064fde9e801d81cf2b999ef3472ed0dc3342566d3`.
  Jar-entry diff vs the candidate: exactly TWO entries differ
  — plugin.yml (version line) and ExcelEMFixes.class (VERSION
  constant). All module code byte-identical; rig verdicts
  carry verbatim.
- Deterministic: repeated builds byte-identical (both names).
- Version bumped in `ExcelEMFixes.VERSION`, `plugin.yml`,
  `build.sh`. ModuleRegistry registers `PartyDropsModule`
  last — 26 modules total.

## What was ported (0.1.0 source → `excelemfixes.partydrops`, from source, never memory)

- Verbatim ports (package line only): `MultiplierMath`,
  `PartyRange`, `ParticipantCounter`, `OriginGate`, `HintGate`
  (the pure multiplier math, the range test, the participant
  filter, the origin gate, the hint cadence).
- **The range math is a COPY, documented as a copy:**
  `PartyRange.within()` re-implements ExcelTogether's
  `PartyBonus.within()` semantics (same world && squared
  distance <= radius², inclusive boundary) exactly — copied,
  not called; NO Together dependency exists (guard-tested);
  Together folds in wave 13 and the copy stays a copy.
- `PartyDropsListener` — behavior verbatim from 0.1.0's
  EmDeathListener (EliteMobDeathEvent at MONITOR, origin
  gate, participant snapshot, per-stack probabilistic
  scaling of the vanilla drop list only, one audit line per
  multiplied kill, cooldown-gated quiet action-bar hint).
  It is the only class referencing EliteMobs API types and is
  loaded/registered only when EliteMobs is present+enabled.
- `PartyDropsSettings` — new immutable snapshot record in the
  SkullSettings house style; clamps replicate 0.1.0's
  loadSettings() ranges exactly.
- Standalone main + command → `PartyDropsModule` (FixModule +
  CommandExecutor + TabCompleter): `/partydrops` status|reload,
  permission node `excelpartydrops.admin` declared in
  plugin.yml (kept from 0.1.0 so existing grants carry over)
  — command name/shape FROZEN per programme gate 8. Reload
  re-reads the pack config and swaps the settings snapshot
  (and rebuilds the hint gate, as 0.1.0 did).
- Config: `modules.party-drops` section (enabled /
  party-radius / step-per-extra / multiplier-cap /
  participant-cap / respect-farming-cap /
  hint-cooldown-seconds / debug) with the reason comments;
  shipped defaults = spec values (50.0 / 0.5 / 3.0 / 5 /
  false / 600 / false).

## Tests

- Full unit suite: **2,075 passed / 0 failed** on the clean
  run (0.23.0 baseline 1,972 + 103 new: the standalone's five
  suites ported verbatim + 9 plugin.yml/config guard checks
  incl. "no hard depend line" and "no Together dependency").
- One additive harness change: the pack's shared test `Check`
  gained the standalone's `near()` tolerance helper; no
  existing check altered.
- Known flake: SkillNudgeMathTest tie check failed in 1 of
  this build's runs — pre-existing, documented in the
  0.22.0/0.23.0 records, in a module this fold does not touch.

## Rig verdicts (Paper 26.2 + EM 10.9.10; PdModuleRig driver in `rig-driver/`, logs in `rig/`)

Driver pattern: the Nov 25 standalone proof's LOWEST/MONITOR
drop-sum bracketing + NATURAL-spawn conversion tracking, with
NMS fake players replacing the mineflayer bots (PdRigA killer
at the pad; PdRigB bystander 10 blocks away, zero damage).
EM spawn percentage 1.0 in the RIG copy of
MobCombatSettings.yml only. Every kill in every leg verified
natural=true, spawn reason NATURAL, antiExploit=false,
nonEliteKills=0.

- **Phase A** (module on): ready line "Party Drops ready: ON,
  radius 50.0 blocks, step 0.5, multiplier cap 3.0,
  participant cap 5, EliteMobs hooked"; 26 modules;
  `/partydrops status` correct; SEVERE 0.
  - SOLO leg (PdRigA alone): 30 kills → drops **32 → 32**
    (exactly vanilla), multipliedKills 0, 0 audit lines.
  - PARTY leg (PdRigB present): 30 kills → drops **22 → 34**
    at multiplier 1.5 (exact expectation 33; +1 inside the
    per-stack Bernoulli noise — exact in expectation, the
    design the standalone proof measured at 45 → 66 vs 67.5);
    multipliedKills 12; **16 audit lines, every one
    "participants=2 multiplier=1.5"** — the MONITOR-timing
    mutation lands in the drop list before Paper scatters
    it (the MONITOR census probe reads the multiplied list).
- **Phase B** (modules.party-drops.enabled=false, party
  setup): 25 modules, party-drops absent, no ready line;
  30 kills → drops **35 → 35** (exactly vanilla),
  multipliedKills 0, 0 audit lines; SEVERE 0. Toggle OFF =
  exact vanilla solo/party behavior restored.
- Standalone proof reused, not re-proven: parent-seat proof,
  Nov 25, 2026 (`~/workspace/partydrops-proof-20261125/RESULTS.md`)
  — live jar vs live EM 10.9.14, 120 bot kills: SOLO 1.000,
  PARTY 1.467 aggregate at 1.5, FAR 1.000. PASS.

## Live-premise receipts (G1, fresh Homebase SSH pull Nov 25, 2026 ~9:51 PM CT)

Full detail in `packet-v24/RECEIPTS.md`
(sha256 `aaa31cab73707970ee98cc60c46158cd94d48365fb9773979d48a0675e373776`):

- Live ExcelEMPartyDrops 0.1.0 jar: 14,543 B / `12f33523…`
  (byte-identical to the Nov 25 proof jar).
- Live ExcelEMPartyDrops config.yml: 2,117 B / `7f64d4b4…`
  (legacy ExcelPartyDrops folder copy byte-identical);
  values enabled true / radius 50.0 / step 0.5 / cap 3.0 /
  participant cap 5 / respect-farming-cap false / hint 600 /
  debug false — **diffed against the staged module config:
  identical** (shipped defaults = spec values = WRV values).
- Live ExcelEMFixes config.yml at pull: 87,117 B /
  `daa7b8bd…` (the 0.23.0 deploy config; the 0.23.0 window
  was mid-flight — live jar still 0.22.0 at pull). Deploy
  config for v24: live pull + only the party-drops section
  (+105 / −0 lines), 91,671 B / `5a4a9468…`.

## D246 gates

- Toggleable + config-driven: yes (all knobs above);
  module-off rig-proven (Phase B).
- Shipped defaults vs WRV values: identical on every knob
  (table in RECEIPTS.md) — WRV runs the shipped defaults
  unchanged (stated in README + config comments).
- Hardcoding sweep: `grep -rn "Wink|WRV|winkreyes|Jester|Marvin|duckdns|webhook"`
  over the module package → ONE hit, the letters "WRV" in
  the D246 documentation comment gate 6 requires (same
  pattern as wither-skulls). No WRV literal in code logic
  or shipped defaults. No WRV-specific data exists for this
  module → no local override needed.
- Gate 7 (self-contained): plugin.yml gains NO hard depend
  (guard-tested); softdepend list unchanged. Cross-system
  legs: EliteMobs death event only, via the pack's existing
  softdepend with the listener loaded only when EM is
  present (absent → module logs inert, pack boots clean).
  ExcelTogether: no leg at all — the range math is a
  documented copy. No other cross-system leg exists.

## Packet + filing (STAGED — filing is a gated follow-on)

- Packet folder: `packet-v24/` — INSTALL-PACKET.md
  (**Lint: 6017 B / max line 88 / non-ASCII 19**),
  RECEIPTS.md, SURFACES.md, SHA256SUMS (verified OK ×5),
  jar, deploy-config.yml.
- Packet directs the window to remove the standalone
  ExcelEMPartyDrops jar in the SAME window, only after
  module proof, kept in the backup set as rollback.
- **NOT filed into MANIFEST and no FIRE-REQUESTED.txt**,
  per brief: ExcelEMFixes 0.23.0 was still PENDING/installing
  at build time (failsafe fired 9:46 PM CT Nov 25), so a v24
  live-premise claim would be false (G1). Filing is
  follow-on #1 below, gated on the 0.23.0 install closing;
  the packet's gate section requires a fresh re-pull at
  filing.

## Surfaces staging (SURFACES.md in the packet folder)

- In-game What's New entry text: STAGED.
- Website What's New + plugin row 0.23.0→v24: STAGED TEXT.
- Records draft PR: OWED — close-out leg after install.
- Guide: hibernated — verify-no-change noted (no behavior,
  command, or rate change).
- Tips + ExcelMenu/ExcelBedrockMenu: checked, no change owed.

## Finish-and-fire drops (D228)

Filed 3 drops to `~/hooks/state/followon-drops/`:
1. File the staged v24 packet into MANIFEST + FIRE once the
   0.23.0 install closes (with the fresh G1 re-pull).
2. Same-pass surfaces publication after the v24 install
   (What's New ×2 + website row, per SURFACES.md).
3. wrv-plugins records draft PR at v24 install close-out.
