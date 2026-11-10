# BUILD RECORD — ExcelBossTrial 0.5.4 (Elder sprinkle melee fix)

Built Nov 10, 2026 (desk lane, board supervisor dispatch).
Commission: Mike, Nov 9, 2026, ~10:55 PM CST — the Elder
Guardian fight's sprinkle wave mobs must (1) take much more
knockback when hit and (2) spawn staggered over ~2 seconds
instead of all at once; NO damage-number changes; elder
first, mechanism spec'd as per-boss config so other bosses
can opt in later.

## Why this is a build, not a config edit (route finding)

The 0.5.3 sprinkle system has NO knockback or stagger knob:
`SprinkleSettings` carries enabled/chance/count/levels/
max-alive/max-per-fight/leash/types/cadence/waves only, and
`SprinkleService.spawnWave` converts and spawns every add
synchronously in one tick, never touching an attribute.
Nothing in the live config can express either half of the
commission, so the fix is a minimal BossTrial build (0.5.4)
adding exactly the two per-chapter keys.

## Candidate jar (FINAL)

- `~/workspace/elder-sprinkle-melee-fix-20261110/build-054/ExcelBossTrial-0.5.4.jar`
  — **443,874 B / sha256
  aa9db99da0ce22c5bc0af15b01906cbbce5f97e89432af111cb979cb75b169c2**
- Build ID: **EXC-BOSSTRIAL-0.5.4-WRV-20261110-01-5371d5**
  (fresh tag; 0.5.3's 35bd2b, 0.5.2's 13447a and 0.5.1's
  a3aa16 never reused).

## Base proof

- Tree copied tree-identical from the shipped 0.5.3 build
  tree (`~/workspace/warden-retune-20261109/build-053/bosstrial/`).
- Pristine `package.sh` rebuild of the untouched copy is
  byte-identical to the shipped 0.5.3 jar: 440,242 B /
  sha256 09c5eb0e4e0860855c6a28e07d703a6627a778bbca5c6a9dacf431c7a0cf35e8
  (`work/base-pristine.jar`, log `work/base-build.log`),
  suite 2,435 checks / 0 failures / 85 harnesses (the
  0.5.3 lane's tally method) before any 0.5.4 edit.

## The change (complete list — tree diff vs the 0.5.3 tree)

Modified: `config.yml`, `plugin.yml`, `excel-build.txt`,
`src/.../ExcelBossTrial.java`, `src/.../core/BuildInfo.java`,
`src/.../core/SprinkleSettings.java`,
`src/.../sprinkle/SprinkleService.java`,
`tests/.../core/PluginYmlParseTest.java`,
`tests/.../core/SprinkleSettingsTest.java`.
New: `src/.../core/SprinkleMelee.java`,
`tests/.../core/SprinkleMeleeTest.java`.
Nothing else differs.

**K1 — knockback (new per-chapter key
`<chapter>.sprinkle.knockback-resistance`):**
- `core/SprinkleMelee.java` (new, pure logic): the set/unset
  rule (NaN = unset — the chapter keeps the EliteMobs
  conversion's own attribute, the pre-0.5.4 behavior), the
  configured clamp [-1.0, 1.0], and the applied rule: a
  negative ask applies at the vanilla attribute floor 0.0
  (full vanilla knockback — the most the game gives), a
  positive ask applies as written.
- `SprinkleSettings` gains `knockbackResistance` (double),
  sanitized loud: NaN survives as UNSET with NO adjustment
  line (unset is a state, not an error); an infinite value
  fails to UNSET with an adjustment line; a finite value
  clamps loud into [-1.0, 1.0].
- `SprinkleService.spawnOne` (extracted from `spawnWave`):
  after a PROVEN EliteMobs conversion, a chapter with the
  key set has `appliedKnockback(...)` forced onto the add's
  `KNOCKBACK_RESISTANCE` attribute base value; the read-back
  rides the per-add spawn log line
  (`, knockbackResistance 0.0 (chapter key -1.0)`).
- Host `loadSettings` reads the key per chapter: an ABSENT
  key keeps the chapter default; a present key is the ask.

**K2 — stagger (new per-chapter key
`<chapter>.sprinkle.stagger-ticks`):**
- `SprinkleMelee.delayTicks(index, count, staggerTicks)`:
  adds spread evenly across the window — first add
  immediate, last exactly at the window's end
  (delay = round(stagger × i / (count−1))); a 0 window or a
  one-add wave is the instant ring, unchanged.
- `SprinkleSettings` gains `staggerTicks` (int), clamped
  loud into [0, 200].
- `SprinkleService.tryFireWave`: with stagger > 0 and a
  wave of 2+, the wave is committed at schedule time (owed
  cleared, cadence stamped, rotation advanced, announced at
  the planned count, wave listener told) and each add lands
  on its delay via the scheduler. Pending (committed, not
  yet spawned) adds COUNT against the alive cap in the
  cadence judge, so no second wave can fire into promised
  room. A scheduled spawn whose encounter has closed never
  lands (no orphans; `closeEncounter` also drops the
  pending count). A failed conversion inside a stagger
  skips that one add — the instant ring's fail-soft.
  The instant path (stagger 0 / one-add waves) is the
  0.5.3 code path, bookkeeping extracted verbatim into
  `waveFired(...)`.

**Defaults + bundled config:** the ELDER_GUARDIAN chapter
alone ships the fix in its code defaults AND its bundled
config block (`knockback-resistance: -1.0`,
`stagger-ticks: 40`), so a live config predating the keys
still runs the fix and the install merge makes it explicit.
Wither / warden / dragon defaults are UNSET / 0 — their
fights are byte-for-byte the 0.5.3 behavior until a config
opts them in. Boot line: a chapter with either knob set
gains the suffix ` [melee: kb -1.0 stagger 40t]`.

**No-damage-change proof:** the functional src diff's only
damage-adjacent added line is the pre-existing
`damageMultiplier` READ riding the relocated spawn log
line; no damage, pulse, pressure, pool, cap, reflect, or
reward value is added, removed, or altered anywhere in
the tree diff (verified by scan of the full diff), and
`FeedbackNoDamageChangeTest` stays green in the suite.

**Version surfaces:** `plugin.yml` version 0.5.4 + the
0.5.4 description sentence (no colon-space; parse-pinned);
`core/BuildInfo.java` fragments rotated (PART_B
`0.5.4-WRV-`, PART_C `20261110-01-5371d5`, VERSION 0.5.4);
`excel-build.txt` carries the new build ID.

**Tests:** NEW `SprinkleMeleeTest` (23 checks: unset/set,
sanitize clamp, applied floor, delay tables for wave sizes
2/4/5/6 at 40t, instant-ring retirements).
`SprinkleSettingsTest` 29 → 38 (+9: elder defaults carry
the fix, other chapters unset, in-range load, kb clamps,
infinite→unset loud, stagger clamps loud, unset records
no adjustment; all 12 pre-existing `sanitized(...)` call
sites extended with the two new arguments at UNSET/0).
`PluginYmlParseTest` 44 → 48 (version re-pin, +1
description pin, +3 config pins: elder keys ship, other
chapters ship neither key).

## Tests

- Full suite via `package.sh` (both builds): **0 failures**.
  Tally by the 0.5.2/0.5.3 lane's method (`tally.py`):
  **2,471 checks / 86 harnesses**, vs the base proof's
  2,435 / 85 with the same tool — delta +36 checks /
  +1 harness = SprinkleMeleeTest 23 + SprinkleSettingsTest
  +9 + PluginYmlParseTest +4, exactly the new pins.
- SprinkleMeleeTest: **23 checks, 0 failures** (both builds).
- SprinkleSettingsTest: **38 checks, 0 failures**.
- PluginYmlParseTest: **48 passed, 0 failed**.

## Deterministic double-build

`package.sh` twice → byte-identical: `work/pkg1.jar` =
`work/pkg2.jar` = the candidate above (443,874 B /
aa9db99d…). Jar contents verified: 231 entries (0.5.3:
230; +1 = `core/SprinkleMelee.class`); class diff vs the
0.5.3 jar is exactly the host, BuildInfo, SprinkleSettings
(+inners), SprinkleService (+$1) and the new class;
`excel-build.txt` at the jar root carries the 0.5.4 build
ID; `BuildInfo.class` carries the 5371d5 fragment and NO
35bd2b; bundled `plugin.yml` reads version 0.5.4; bundled
`config.yml` carries the elder keys
(`knockback-resistance: -1.0`, `stagger-ticks: 40`).

## Fingerprint verification (release gate)

Layer B homes all carry the new ID, verified in the built
jar: (1) `excel-build.txt` at the jar root; (2) the
fragment-assembled constant in `core/BuildInfo.class`
(5371d5 present, 35bd2b absent); (3) the boot surface line
`Build: EXC-BOSSTRIAL-0.5.4-WRV-20261110-01-5371d5`,
observed in the rig boot below. Layer C semantic markers
carried unchanged; 0.5.4 adds two config keys
(`<chapter>.sprinkle.knockback-resistance`,
`<chapter>.sprinkle.stagger-ticks`) under the standing
naming texture.

## Rig boot verification (desk rig, candidate jar)

A copy of the Item 39 rig (`build-054/rig-054/`, Paper
26.3-235; BossTrial state reset to config-only) with the
candidate jar swapped in and the rig config's elder
sprinkle block set to the LIVE values (Packet A: enabled,
cadence 120, max-alive 10, max-per-fight 60, waves
GUARDIAN×4 / DROWNED×5 / mixed×6) plus the two new keys
(-1.0 / 40). Boot log `work/rig-boot.log` (41,717 B /
sha256 208a26f139faa70cd3b9f1a67c10d231a7e456a34924b35329888230c816dfdf),
clean stop, **SEVERE 0**:
- `Enabling ExcelBossTrial v0.5.4`
- `Build: EXC-BOSSTRIAL-0.5.4-WRV-20261110-01-5371d5`
- `Elite sprinkle: master ON; wither off; elder_guardian
  ARMED (cadence 120s, cap 60) [melee: kb -1.0 stagger 40t];
  warden off; ender_dragon off (phase 2 only)` — the new
  keys parse, sanitize SILENT (no `sprinkle config [...]`
  clamp WARNING anywhere in the boot), and surface on the
  boot line exactly as designed. (Wither reads "off" only
  because the rig config leaves it dark; live arms it.)
- The EliteMobs seam reports unavailable in the rig (no
  EliteMobs plugin in the rig set) — the standing fail-soft;
  the spawn-time half (attribute write + staggered landings
  against real conversions) is therefore NOT rig-proven
  here and is carried as the packet's player proof, the
  same division the 0.5.3 filing used for its fight proofs.

## Live values this build was designed against

Live elder sprinkle (Packet A, Nov 9, 2026; still live
through the 0.5.2 and 0.5.3 boots): enabled true,
cadence-seconds 120, levels [18, 20, 22], max-alive 10,
max-per-fight 60, leash-radius 24.0, waves GUARDIAN×4 ring
/ DROWNED×5 ring / [GUARDIAN, DROWNED]×6 ring. The stagger
therefore spreads live waves at: ×4 → delays 0/13/27/40t,
×5 → 0/10/20/30/40t, ×6 → 0/8/16/24/32/40t.
