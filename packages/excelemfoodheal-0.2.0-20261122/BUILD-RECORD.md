# ExcelEMFoodHeal 0.2.0 — BUILD RECORD (out-of-combat base heal raised to 2%/s in EliteMobs worlds)

**Task:** build the D184 delta on ExcelEMFoodHeal 0.1.0 (D162): a
config-gated base out-of-combat regen bonus of +1%/s in EliteMobs
worlds, so EliteMobs' hardcoded ACS trickle (1%/s) plus ours totals
2%/s. Origin: Jester's suggestion (backlog P-JESTER-OOC-HEAL-1);
Mike ruled 2%/s on Nov 21, 2026, 8:19 PM CST (alternatives: 3%/s,
keep 1%).
**Date:** 2026-11-21. **Builder:** build worker (parent-seat dispatch).
**Status:** COMPLETE — built, unit-tested, rig-proven, filed in
the MANIFEST; not installed (the failsafe installs at the first
verified-empty moment).

## Design (D184 — config-driven, defaults ship the ruled behavior)

| Constant | Config key | Default |
|---|---|---|
| Bonus on/off | `ooc-bonus-enabled` | true |
| Bonus rate | `ooc-bonus-percent-per-second` | 1.0 (% of max health / s) |

Semantics:
- The bonus ticks once per second for every player in an EliteMobs
  world (same gate as the food budgets: EliteMobs registry OR the
  config world list) who is out of combat under the plugin's existing
  combat tracking (20 s window, damage dealt or taken — the same
  condition the food HoT already uses, mirroring ACS's own rule).
- No food required. The food HoT is unchanged and stacks on top
  (a steak meal out of combat delivers its 0.5%/s budget AND the
  1%/s bonus while both run).
- In combat: no bonus ticks. Overworld/nether/end: no bonus
  (vanilla regen untouched). Never heals above max health (the
  existing per-tick clamp to missing health). Self-only, as before.
- Total base out-of-combat heal in EM worlds after this change:
  EM ACS 1%/s (hardcoded in EM 10.9.14, research
  `~/workspace/em-healing-research-20261120/RESEARCH.md`) + this
  bonus 1%/s = 2%/s, both gated on the same 20 s out-of-combat rule.
- Implementation: a pure `BonusPolicy.tickPercent(enabled, rate,
  inEmWorld, inCombat)` decision (unit-tested truth table) called
  from the plugin's existing 1 s tick; delivery reuses the existing
  clamped `deliver()`. A debug transition line (`FOODHEAL bonus
  start ...`) logs when bonus delivery begins for a player, so the
  post-install live proof has a log witness (debug stays as the
  live config has it — live value is `debug: true`, see receipts).

## G1 live premise (pulled this run, Nov 21, 2026, 8:22 PM CST)

- Live jar `plugins/ExcelEMFoodHeal-0.1.0.jar` = 15,564 B /
  SHA-256 `8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05`
  — PC-side Get-FileHash matches the scp pull; byte-identical to
  the 0.1.0 build artifact. Copies in `receipts/`.
- Live config `plugins/ExcelEMFoodHeal/config.yml` = 1,301 B /
  SHA-256 `cd9428299cf69150046825c08b2b216727080edbe3aa093a308e377c18466079`,
  PC mtime 2026-11-20 18:19:54 (install-generated, never edited
  since): byte-identical to the 0.1.0 bundled default, `debug: true`.
  Full receipts: `RECEIPTS.md` in this folder.

## Phases

- [x] P0 premise: live jar + live config pulled fresh via Homebase
      SSH/scp and hashed (above); 0.1.0 source + build scripts
      carried into this folder; EM API pin unchanged (the plugin
      compiles against the same pinned EliteMobs jar as 0.1.0).
- [x] P1 source: 0.2.0 delta — new pure `BonusPolicy` decision;
      `ExcelEMFoodHeal` reads `ooc-bonus-enabled` /
      `ooc-bonus-percent-per-second`, and its existing 1 s tick
      calls `tickBonus` after the meal loop (bonus for every
      online player in a gated world who is out of combat on the
      engine's existing combat marks; delivery via the existing
      clamped `deliver()`; a `FOODHEAL bonus start` debug line on
      each delivery-stretch transition). Bundled config gains the
      two keys with the D184 comment block; plugin.yml 0.2.0.
      HealEngine / FoodTable / WorldGate are byte-identical to
      0.1.0 — the food layer is untouched.
- [x] P2 unit tests: **145/145 pass** (137 carried forward from
      0.1.0 + 8 new BonusPolicyTest checks: full truth table,
      rate pass-through, zero/negative rate).
- [x] P3 deterministic double-build: both builds 16,948 B,
      SHA-256 `787198b6859eb91bc3a3e452c03cd3b1cb650c035bb678e5129509e6dd971680`,
      `cmp` byte-identical.
- [x] P4 rig proof (Paper 26.2-123 + real EliteMobs 10.9.14;
      evidence `rig/runtime.log`, driver in `rig/driver/`).
      Boot: EliteMobs enabling precedes ExcelEMFoodHeal v0.2.0,
      the new enable line logs the bonus (+1.00%/s), SEVERE 0.
      **16/16 RIGRESULT PASS:**
      API (registry predicate callable; rig worlds unregistered,
      gate via config list as documented);
      S3 overworld steak no-op — food AND bonus blocked (exactly
      3 FOODHEAL meal lines for RigFoodie all run, all in
      em_primis; the overworld meal produced none);
      S1-instant 51.0 exact;
      S1-rate (proof d) delta exactly 7.5 HP over 5 s = 5 ticks x
      1.5 (HoT 0.5 + bonus 1.0 stacked; food-only would read ~2.5,
      bonus-only ~5.0);
      S1 budget-in-full at meal+20 s: 79.6 (expect ~79), FOODHEAL
      complete line logged;
      S2-pause: health frozen at 45.5 across the combat window
      (both layers paused; FOODHEAL pause line remaining=7.10%,
      the exact 0.1.0 arithmetic);
      S2-resume at the 20 s mark (resume line, remainder intact),
      budget completes after resume;
      S4 clamp: 100.0 / 100.0, never above max;
      S5 replace-not-stack with the bonus running: 60.5 at
      bread+12 s (expect ~60.5; a stacked food total ~68);
      B1 (proof a) bonus alone, no food ever: 30.0 -> 40.0 over
      exactly 10 s = +1%/s of the 100 HP pool, with a
      `FOODHEAL bonus start player=RigBonus rate=1.00%/s` line
      and zero meal lines for RigBonus — with EM ACS's own
      hardcoded 1%/s (same 20 s gate; ACS does not run in the
      rig's unregistered flat world, the documented limitation,
      so the EM half is the research-verified constant) the
      live total is 2%/s;
      B2-delay (proof e): health still 30.0 at hit+10 s — no
      bonus ticks inside the 20 s window;
      B2-resume: 33.0 after the window;
      B2-stop (proof b): second hit freezes health at 33.0
      across +5 s of combat;
      B4 (proof c): overworld player at 40.0 after 10 s — no
      bonus outside EM worlds.
- [x] P5 deliverables: jar (16,948 B / SHA-256 in P3), source
      zip (18,980 B / SHA-256
      `a8cb4420a85a092ee6d8b74605efb23e71b14f6e31fe77b4322c7e8535b77ef5`),
      SHA256SUMS (verified `sha256sum -c` clean), INSTALL-PACKET.md,
      RECEIPTS.md; MANIFEST stub filed; D184 decision-log entry;
      backlog P-JESTER-OOC-HEAL-1 + daily-log updates.

## Log

- 2026-11-21 — record created (write-first). G1 pull done (above).
  Design fixed before code, per the spec.
- 2026-11-21 — P1/P2/P3 complete (145/145 tests; double-build
  byte-identical). Rig assembled from the 0.1.0 skeleton (heavy
  trees symlinked, configs/world copied; the candidate jar and a
  fresh-generated config in the rig). First foreground rig launch
  was reaped by the exec wrapper at ~25 s (SIGTERM, mid-boot, no
  server error); relaunched detached — clean run, exit 0,
  done-marker reached, 16/16 PASS, SEVERE 0. BUILD COMPLETE.
