# RIG-REPORT — mcMMO XP-fix rig proof (Nov 15–16, 2026)

**Verdict: RIG GREEN** for the final combined jar, with two named caveats
(R6 finding; the NOTRUN cluster). Details below.

| Build | Size | SHA-256 | Role |
|---|---|---|---|
| Original patch | 3,718,764 B | `cf61938c430cb0770355ba70af7f36c516fd97340a063443a4253ee6c3813e05` | Superseded before rig completion (scope rulings) |
| Scoped build (addendum 1) | 3,719,269 B | `ef15578a5906192a1ab6eff2036fa487a2ab6426f10aa64a9b93dba28ca075b3` | Staged evidence run |
| **Final combined jar (INSTALL CANDIDATE)** | **3,721,966 B** | **`404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501`** | **Full matrix + verdict run** |

## How the rig worked

- Venue: a disposable **clone** of the Homebase test-server mirror at
  `C:\Users\MikeExcel\wrv-testserver-xpfix\server\` (ports 25567/25512/25624).
  The original mirror was under active contention from a sibling rig lane
  all evening (it renamed this rig's jar mid-run), so the clone kept both
  lanes out of each other's way. The clone was deleted after the runs; the
  original mirror was restored (see Restoration). **No live-server access,
  no player contact, no live RCON at any point.**
- The clone's plugin set was staged to live versions for fixture fidelity:
  EliteMobs 10.9.14, ExcelSummons 0.1.13 (+ live config), ExcelTrainingDummy
  1.0.1, live mcMMO config. One substitution to note: ExcelTogether was
  **0.5.6 (live)** in the clone; the mirror had a 0.6.0-WIP build from the
  sibling lane, which was the prime suspect in one clone anomaly and was
  swapped out before the proof runs.
- Harness: a purpose-built plugin (`rig/XpFixRig.java`, final rig build
  SHA-256 `2C35C92C…`) driving two NMS fake players (RigAttacker/RigOwner,
  plus fresh RigAttackerB/RigOwnerB profiles in the supplemental run).
  Measurement = mcMMO (level, rawXP) snapshot pairs around scripted damage;
  paid = level up or raw up at the same level; zero = exact equality.
  Damage events were counted per (attacker, target) pair, so every verdict
  below carries its landed-hit count — a "zero" with zero landed hits is
  called out as vacuous wherever it occurs.

## Results — final combined jar `404025cc…` (full run: pass 14, fail 2, notrun 14)

| # | Fixture | Result | Evidence |
|---|---|---|---|
| R1 | EM training dummy Lv.100 → 0 XP | **NOTRUN** | Custom bosses never materialise on the mirror (see Environment limits). Covered another way: the dummy's home world is `em_adventurers_guild`, an `em_` world — R15/R16/R17 prove the world rule zeroes all gains there regardless of the filename matcher; the matcher itself is unit-tested (build report, 1,976 tests). |
| R2 | EM training dummy Lv.1 → 0 XP | **NOTRUN** | Same as R1. |
| R3 | ExcelTrainingDummy → 0 | **PASS** | Swords delta exactly 0 with the dummy still present; zero in both baseline and patched modes by construction (virtual HP pool — mcMMO measures no health delta). Also PASS on ef15578a and in baseline runs. |
| R4 | ExcelSummons types as target → 0 (×7) | **NOTRUN** | Cast summons cannot be produced headlessly (see Environment limits). The registry-recognition path they would exercise is the same one proven live by R12 (stamp path) and R5b (registry origin read), and is unit-tested. |
| R5 | Cast-summon wolf as pet → owner Taming 0 | **NOTRUN** | Same production limit as R4. The CAST-origin skip is unit-tested against the real `ExcelSummonsAPI.getRecord` shapes; the dangerous direction (adopted pets wrongly zeroed) is disproven in-rig by R5b/R5c on both patched builds. |
| R5b | **Adopted pet wolf → owner Taming PAYS** | **PASS** | Wolf adopted by the real 0.1.13 sweep; origin read back through the live API as `origin=ADOPTED`; 7 wolf hits on a 500-HP zombie; owner Taming lvl23/raw1224.0 → lvl25/raw214.69 (paid). Also PASS on ef15578a (lvl0 → lvl2). |
| R5c | Plain tamed wolf control → pays | **PASS** | 12 hits; Taming lvl44 → lvl46; `isSummonAtRead=true` (the sweep had adopted it — consistent with the origin rule paying adopted pets). Also PASS on ef15578a. |
| R6 | Magic `mmob` mob (unadopted) → 0 if stamped | **FAIL — finding** | Paid on the final jar (Swords raw 516.57 → 1030.14, 7 hits), as it did in baseline (+559 raw). Probe evidence: an `mmob`-spawned zombie carries **no Magic PDC stamps at all** (only RewardGuard origin + CoreProtect keys), so the patch's magic-stamp recognition has nothing to recognise. See Finding 1. |
| R7 | Natural EM elite control → pays | **FAIL (harness artifact)** | The ambient elite was teleported to the pen and EM/Paper invalidated it (`teleport=true valid=false health=5.02` probe) — 0 damage events are possible on an invalid entity. Supplemental fight-in-place attempt: no ambient elite with health ≥ 30 existed (they sit near-dead underground). The pays-normally direction for elites rests on: probe strings show natural elites match **no** recognition rule (customBoss=false, no filename, no stamps → fail-open pays by construction), plus R9/R14, R10b, R17a-control and R5b all paying normally on the final jar. |
| R8 | Non-dummy EM custom boss control → pays | **NOTRUN** | Custom-boss materialisation limit (same as R1). |
| R9 | Vanilla zombie control → pays | **PASS** | Swords lvl4/raw3.0 → lvl4/raw516.57, 7 landed hits. |
| R10 | Acrobatics vs summon attacker → 0 | **NOTRUN** | Cascade from R4/R5 (no cast summon to attack with). Acrobatics branch shares the recognition helper proven by R12; control R10b passes. |
| R10b | Acrobatics vs zombie control → pays | **PASS** | 28 zombie hits on the player; Acrobatics raw 0.0 → 9354.54 (paid). (An earlier 3-hit sample on ef15578a read zero — dodge-roll variance; the 28-hit sample is decisive.) |
| R11 | Summon wolf vs Lv.100 dummy | **NOTRUN** | Both inputs unavailable (cast production + boss materialisation). |
| R12 | RewardGuard-denied-stamped zombie → 0 | **PASS** | Rig applied RG 1.6/1.7's exact stamp keys/types to a plain zombie: Swords delta exactly 0 on the final jar (and on ef15578a). In baseline the same stamped zombie **paid** — the patch's stamp recognition is what zeroes it. |
| R13 | Normal elite inside `em_primis` → 0 | **PASS (vacuous hits)** | A natural elite was moved into `em_primis` valid (`teleport=true valid=true nowWorld=em_primis`), but landedHits=0 — cross-world-teleported EM elites stop producing damage events (same invalidation family as R7). The zero is therefore not independent proof; the world rule **in `em_primis` itself** is proven with real hits by R17a's earner leg (7 hits, delta 0). |
| R14 | Overworld zombie control (addendum-2 re-run of R9) | **PASS** | Same fixture/evidence as R9, recorded under the R14 label against this jar. |
| R15 | Instanced-copy world `em_id_primis_wolf_den_1` → 0 | **PASS** | 7 landed hits on an ordinary zombie inside the instanced-shape world; Swords delta exactly 0. |
| R16 | `GoblinKingdom_vf` → 0 | **PASS** | 7 landed hits; delta exactly 0 — the one non-`em_` exception works end-to-end. |
| R17a | Party share in EM world | **PASS (earner leg)** | Party formed via PartyAPI (`inSameParty=true`). In `em_primis`: earner delta 0 **and** member delta 0 across 7 landed hits. Overworld control: earner paid (lvl4 → lvl5). Caveat: the member received 0 in the overworld control too — EQUAL sharing did not flow in-rig (share unlock/mode), so the member-zero is not independent proof; the EQUAL-share gate itself is proven by the build's unit tests (member in `em_primis` receives 0 while the overworld earner keeps their share). |
| R17b | ExperienceAPI funnel in EM world | **PASS** | `ExperienceAPI.addXP(atk, "Swords", 1000)` standing in `em_primis`: delta exactly 0. Same award in the overworld: paid (lvl5 → lvl6). The `applyXpGain` gate works end-to-end. |
| R18 | Config extras (`WRV.No_XP_Worlds.Extra_Names`) | **PASS (both legs)** | World `rigtest_plain` listed: 7 hits → delta 0. Entry removed + restart: 7 hits → paid (raw 616.0 → 1067.92). A future off-pattern pack is a config edit, not a rebuild. (First unlisted attempt failed because mcMMO re-serialises the list in block YAML style at boot, so the flow-style removal regex missed it; the retry edited the block form and verified the line before booting.) |
| R19 | `damage_test_dummy.yml` boss → 0 (matcher fixture) | **NOTRUN** | Boss materialisation limit. The file is in the dummy matcher (addendum 1) and unit-tested; the boss is currently unspawnable in production too (no spawn location). |

Baseline contrast (unpatched fork, clone v3 run + original-mirror v2 run):
R9 paid, R3 zero (by construction), R6 paid (+559 raw), R12 stamped zombie
**paid**, R10b paid, tamed/adopted wolves paid — i.e. everything the patch
zeroes was verified to pay without it, and everything it must keep paying
was verified to pay with it.

Staged-build note: the ef15578a run reproduced the same PASS set
(R9/R14, R3, R12, R5b, R5c) before the final jar existed, so the scoped
Taming behaviour was proven on both patched builds.

## Finding 1 — R6: Magic-spawned mobs carry no Magic PDC stamps (mmob path)

The patch recognises Magic mobs by the `magic:magicspawned` /
`magic:magicmob` PDC stamps. On the live plugin stack, a mob spawned by
Magic's own `mmob spawn` command carries **neither** stamp (probe shows
only `excelrewardguard:origin_v1` and CoreProtect keys), so it pays mcMMO
XP in the overworld exactly as it did before the patch — **no regression,
but no coverage either** on that path. Exposure analysis:

- `mmob` is an admin/console command; no player can reach it.
- Every player-reachable Magic mob spawn in WRV's spell config routes
  through ExcelSummons (adopted/registered → recognised, R5b-proven
  registry), and WRV's `spells.yml` has no non-summon SpawnEntity spells.
- Inside EM worlds, the world rule zeroes any such gain anyway (R15–R17).

Recommendation: accept for this install; if Magic-mob coverage is ever
wanted in the overworld, it needs a different recognition signal (e.g.
Magic's mob registry queried reflectively), not a patch tweak.

## Environment limits (why the NOTRUNs are NOTRUN)

1. **EM custom bosses do not materialise headlessly on the mirror.**
   Four independent spawn routes were tried per boss
   (`CustomBossEntity.createCustomBossEntity(...).spawn(...)`,
   `SpawnRegionalBoss`, console `em spawn <file> <level>`,
   `materializePendingSpawns` — pending count read back as 0), across
   three boots and two builds. Every route reports success and no entity
   ever exists afterwards (elite census shows only ambient natural
   elites). Creating the `em_adventurers_guild` world fresh did not make
   EM populate its dummy row either. This blocks R1, R2, R8, R19 (and
   R11's dummy half). It is a mirror/headless limitation, not a patch
   behaviour: on the live server these bosses are persistent regional
   content placed by EM itself.
2. **Wand-cast summons cannot be produced by fake players.** 30+ cast
   attempts across three routes (Magic API `MageSpell.cast()`, player
   `/cast`, console `magic cast`) and two generations of fake-player
   profiles: the API cast returns false for wandless fake players, and a
   per-tick appearance watch (200 ticks, snapshot before cast) never saw
   any entity appear. The two apparent "finds" during the evening were
   pre-existing registry entities standing near the pen, not cast
   products. This blocks R4, R5, R10 (and R11's wolf half). Adopted-pet
   fixtures (R5b/R5c) were unaffected — the adoption sweep works on
   ordinary tamed wolves.
3. **Cross-chunk/cross-world teleports invalidate EM-tracked elites**
   (probe: `teleport=true valid=false`), which hollowed out R7 and the
   hit-count of R13. Documented here so a future rig fights elites in
   place or spawns them in place instead of moving them.

## Restoration

- The **clone** tree (`C:\Users\MikeExcel\wrv-testserver-xpfix\`) was
  deleted in full after evidence harvest.
- The **original mirror** was restored to its pre-rig state and verified
  by inventory: `EliteMobs-10.9.10.jar` (14,303,365 B) and
  `ExcelSummons-0.1.5.jar` (65,253 B) back under their original names;
  the mcMMO jar was never swapped on the original mirror and remains the
  original 3,695,180 B build; `TowersRig-0.1.0.jar` present exactly once;
  zero XpFixRig / ExcelTrainingDummy leftovers in any extension; the
  EliteMobs, ExcelSummons and mcMMO data folders were restored from the
  pre-rig backup (`xpfixrig-backup-v2`). The sibling lane's own files
  were not touched. **Mirror is OFF** (no java process).
- Note: one tool result during the restore phase was flagged by the
  runtime's injection check. The restore was therefore executed as
  discrete, individually verified steps taken from this task's own plan
  rather than by running the pre-written restore script wholesale; the
  end state was verified by inventory as above.

## Evidence files (this folder, `rig/`)

- `rig-results-patched-final.txt` — full matrix, final jar `404025cc…`
- `rig-results-patched-ef15578a.txt` — full matrix, scoped build
- `rig-results-minifix-final.txt` — supplemental run (fresh profiles,
  guild-world attempt, fight-in-place attempt)
- `rig-results-r18-listed.txt` / `rig-results-r18-unlisted.txt` — R18 legs
- `rig-results-baseline-v2.txt`, `rig-results-baseline-clone-v3.txt`,
  `rig-results-baseline-v1-dressrehearsal.txt` — unpatched contrast runs
- `boot-ef15578a.log`, `boot-r18-unlisted.log` — representative boot logs
- `XpFixRig.java` — the rig harness source (all versions' fixtures)

## Verdict

**RIG GREEN** — the final combined jar
`404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501` is
safe to file for gated install.

Every fixture that could be executed on the mirror passed, including all
three of Mike's rulings proven end-to-end: the world rule (R15, R16,
R17a earner leg, R17b — real hits, exact-zero deltas), the scoped Taming
rule (R5b adopted pays / R5c control pays, against the real Summons
0.1.13 registry), and the recognition choke point (R12). All controls
that must keep paying do (R9/R14, R10b, R17a/R17b overworld legs, R18
unlisted leg). The two non-passes are a pre-existing Magic stamping gap
on an admin-only path (R6, no regression, exposure nil for players) and
a harness artifact (R7). The NOTRUN cluster traces to two headless-mirror
limits (EM boss materialisation, wand-cast production), each with
unit-test or adjacent-rig coverage, and the patch's fail-open design
means an unrecognised entity pays normally — the only entity class that
must never pay, the guild training dummy, sits in an `em_` world where
the proven world rule zeroes it regardless of the filename matcher.
