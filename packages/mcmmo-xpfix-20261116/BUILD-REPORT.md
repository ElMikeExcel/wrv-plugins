# mcMMO XP Fix — Build Report (Nov 15, 2026)

**Task:** Implement MECHANICS.md §5 Option 1 in a copy of the WRV mcMMO fork —
training dummies and Magic summons grant zero mcMMO XP of any kind.
**Status: BUILT + FULL TEST SUITE GREEN.** Desk work only: nothing was installed,
no live server was touched, no player was contacted. The revert is a separate
task and is not part of this build.

## Deliverables

| Item | Value |
|---|---|
| Built jar | `~/workspace/mcmmo-xpfix-build-20261115/mcMMO.jar` |
| Jar size | 3,718,764 bytes |
| Jar SHA-256 | `cf61938c430cb0770355ba70af7f36c516fd97340a063443a4253ee6c3813e05` |
| Patched source | `~/workspace/mcmmo-xpfix-build-20261115/repo/` (copy of the fork) |
| Full diff | `~/workspace/mcmmo-xpfix-build-20261115/xpfix.diff` (487 lines) and Appendix A below |
| Pristine fork | `~/workspace/mcmmo-edit/repo` — untouched (verified: no new files, its CombatUtils.java unmodified) |

## What changed (diff summary)

Three files: one modified, two new. No config, no plugin.yml, no database code,
no public API signature changes.

### 1. `src/main/java/com/gmail/nossr50/util/skills/CombatUtils.java` — 3 hunks

- **`calculatePveBaseXP` (the choke point):** returns `0` immediately when
  `NoXpEntityRecognition.isNoXpEntity(target)` is true. Every weapon skill
  (Swords/Spears/Axes/Maces/Unarmed/Tridents/Archery/Crossbows) and
  Taming-as-target prices through this method, and `processCombatXP` already
  skips scheduling the award task when the priced XP is 0 — so a recognised
  target pays nothing, per hit, forever, regardless of its HP pool.
- **`processTamingCombat` (pet branch):** the `processCombatXP(..., TAMING, 3)`
  call is skipped when the attacking wolf is a recognised ExcelSummons summon
  (`NoXpEntityRecognition.isSummon(wolf)`). The summon's Taming abilities
  (FastFoodService, Pummel, Sharpened Claws, Gore damage boosts) still run —
  only the owner's XP gain is removed.
- **`processCombatAttack` (Acrobatics direction):** at the top of the
  `target instanceof Player` branch, if the attacker root
  (`painSourceRoot` — EntityListener already resolves projectiles to their
  shooter, so a summon skeleton's arrow counts as the summon) is a recognised
  no-XP entity, the method returns early. The defender therefore earns no
  Acrobatics dodge XP from a dummy's or summon's attacks. Side effect, by the
  prescribed shape: Swords counter-attack processing is also skipped against
  recognised attackers (they are practice/owned entities; counter-attacking
  them was never a designed interaction).

### 2. `src/main/java/com/gmail/nossr50/util/skills/NoXpEntityRecognition.java` — NEW (293 lines)

The single recognition helper all three hunks consult. See "Recognition paths"
below.

### 3. `src/test/java/com/gmail/nossr50/util/skills/NoXpEntityRecognitionTest.java` — NEW (16 tests)

Filename-matcher cases (the five guild dummies match; `damage_test_dummy.yml`,
other bosses, wrong extensions do not), one test per PDC stamp, the unstamped
control, and null-safety.

## Recognition paths (all positive-ID, all fail-open)

`isNoXpEntity(entity)` ORs four checks, cheapest first:

1. **Persistent-data stamps** (`entity.getPersistentDataContainer().has(key)`,
   any type):
   - `exceltrainingdummy:training` — our ExcelTrainingDummy. It already pays 0
     by construction (virtual pool, damage zeroed before mcMMO's health-delta
     task measures it); the stamp is defence in depth.
   - `excelrewardguard:denied_v1` — RewardGuard's denial stamp
     (`ProvenanceStore.java:11` in the RewardGuard 1.6 source).
   - `magic:magicspawned` and `magic:magicmob` — Magic's own stamps (see
     "Magic SpawnEntity coverage" below).
2. **ExcelSummons registry (reflective):** plugin-presence check
   (`Bukkit.getPluginManager().getPlugin("ExcelSummons")`, must be enabled),
   then `Class.forName("com.winkreyes.excelsummons.ExcelSummonsAPI")` →
   `isSummon(Entity)` invoked per call. No compile-time dependency; the fork
   builds and runs identically without ExcelSummons. Method handle is cached
   after first resolution; a stale handle (plugin reloaded under a new
   classloader) is dropped and re-resolved, paying normally for that hit.
3. **EliteMobs training dummies (reflective):** same presence gate for
   "EliteMobs", then the ExcelProtect EliteProbe chain —
   `EntityTracker.isEliteMob(entity)` and `getEliteMobEntity(entity)` — plus,
   only for a confirmed elite, `EliteEntity.isCustomBossEntity()` →
   `CustomBossEntity.getCustomBossesConfigFields()` →
   `CustomConfigFields.getFilename()`. XP is zeroed **only** when the filename
   positively matches the guild dummy set: basename (directory prefix and case
   ignored) starts with `training_dummy_lv` and ends with `.yml`
   (lv1/25/50/75/100). A natural elite or any other custom boss answers false
   at the `isCustomBossEntity`/filename step and **pays full XP** (control
   case preserved). The tracker methods were verified by javap against the
   EliteMobs 10.9.12 jar in the workspace (the same jar EliteProbe's pattern
   was verified against); the boss-side methods (`isCustomBossEntity`,
   `getCustomBossesConfigFields`, `getFilename`) were read from that jar's
   `EliteEntity` / `CustomBossEntity` / `CustomConfigFields` class listings.
   Live runs EliteMobs 10.9.14 — rig fixtures R1/R2 below verify the chain
   against 10.9.14 on the mirror before this jar is installed anywhere.

**Fail-open behaviour (every path):** plugin absent or disabled → not
recognised; API class/method unresolvable while the plugin is present → probe
marked broken, one warning logged via `mcMMO.p.getLogger()`, entity pays
normally; any exception during a probe → that call pays normally. Plugin
absence is never cached, so a late-enabling plugin is picked up. Nothing in
this class can throw into the combat path (all probe bodies are
`catch (Throwable)`-guarded), and per-hit cost for an ordinary mob is four
PDC `has` lookups plus two plugin-presence map lookups; reflective invokes
happen only for entities the cheap checks or the elite gate pass through to.

## Magic SpawnEntity coverage — stamp FOUND

The Magic 11.2.5 jar in the workspace (`~/workspace/magic-scratch/downloads/
Magic-11.2.5.jar`, the live build) **does** stamp its mobs. Evidence
(javap/bytecode inspection):

- `com.elmakers.mine.bukkit.magic.MagicMetaKeys` defines
  `MAGIC_SPAWNED` = MetaKey\<Boolean\>("magicspawned") and
  `MAGIC_MOB` = MetaKey\<String\>("magicmob").
- `com.elmakers.mine.bukkit.utility.platform.base_v1_20_5.
  PersistentEntityMetadataUtils.getKey(...)` builds
  `new NamespacedKey(plugin, metaKey.getName())` with the Magic plugin as
  owner and stores booleans as PDC BYTEs — i.e. the PDC keys are
  **`magic:magicspawned`** (BYTE) and **`magic:magicmob`** (STRING).
- `com.elmakers.mine.bukkit.entity.EntityData` reads `MAGIC_SPAWNED` into its
  `magicSpawned` field and writes `MAGIC_MOB` when applying mob data;
  `SpawnEntityAction` writes Magic meta keys on its products (its NO_DROPS
  write is directly visible in bytecode).

Both keys are in the recognition set, so Magic-spawned mobs are covered
**whether or not ExcelSummons adopts them** — the residual gap MECHANICS.md
flagged for unmanaged Magic spawns is closed at the stamp level. One honest
caveat: bytecode proves the stamping machinery and EntityData's use of it;
that a given live SpawnEntity product carries the stamp on the running build
is a rig check (fixture R7 below), not something provable from the jar alone.
If R7 surprises us, the fallback is the MECHANICS.md adoption-stamping route
in ExcelSummons/MagicGate — the fork patch would not need to change.

## Not covered by this patch (flagged, per MECHANICS.md §5)

- **Vanilla XP orbs and Jobs payouts on KILLING a summon/dummy.** Both are
  kill-triggered and live outside mcMMO; this patch governs mcMMO XP only.
  (ExcelTrainingDummy already zeroes its own dropped XP; EM's guild dummies
  have all loot/XP drops off in their boss files. A live summon killed by a
  player still drops vanilla XP for its type, as any mob does.)
- **Magic SP (`earns:`)** — per-cast progression inside Magic, by design;
  hitting or being hit is not an input to it.
- Party SHARED_PVE shares derived from a recognised target's gains: with the
  base priced at 0 there is no gain to share, so this is covered transitively.

## Compile & test results

Environment: Temurin JDK 17.0.20.1 + Maven 3.9.9 (house toolchain,
`~/workspace/.jdk/env.sh`). The fork's dependency set only resolves through
the house file:// mirror settings
(`-s ~/workspace/mcmmo-edit/build-tools/settings.xml`, local repo
`~/workspace/m2-repo`) — plain offline mode against `~/.m2` fails on missing
plugin artifacts before reaching the code; that is a pre-existing environment
property, identical for the pristine fork, and is recorded here so the next
builder doesn't re-diagnose it.

- `mvn -o -Dmaven.repo.local=~/workspace/.m2/repository compile -DskipTests`
  (workspace repo): **BUILD SUCCESS** (only pre-existing deprecation warnings;
  none from the new/changed files).
- Targeted: `mvn test -Dtest='NoXpEntityRecognitionTest,CombatUtilsTest'` —
  **52 tests, 0 failures** (16 new recognition tests + 36 CombatUtils,
  including the pre-existing `CalculatePveBaseXp` suite).
- Full suite: `mvn test` — **1,926 tests run, 0 failures, 0 errors, 0
  skipped** (pristine-fork baseline 1,910 + 16 new).
- Package: `mvn package -DskipTests` — **BUILD SUCCESS**; shaded
  `target/mcMMO.jar` copied to the deliverable path above; the jar contains
  `com/gmail/nossr50/util/skills/NoXpEntityRecognition.class` (verified via
  `unzip -l`). No database code was touched, so the AGENTS.md Docker/SQL
  suites are not implicated.

## RIG-PLAN — Homebase test-server mirror fixtures

Mirror: the PC test server (Paper 26.3 + the live plugin set — EliteMobs
**10.9.14**, Magic 11.2.5, ExcelSummons 0.1.13, ExcelTrainingDummy 1.0.1,
ExcelRewardGuard, FreeMinecraftModels) with **only** the mcMMO jar swapped
for this build. Method for every fixture: record the test player's
`/mcstats` (or mcMMO debug XP values) before and after a fixed hit count
(e.g. 50 sword hits), and assert the delta. A fixture passes only if BOTH the
XP assertion and the behaviour assertion hold.

| # | Fixture | XP assertion | Behaviour assertion |
|---|---|---|---|
| R1 | EM training dummy **Lv.100** at the guild training grounds (husk, 2,293,760 HP) | Swords/Spears/Axes/Maces/Unarmed/Archery deltas all **0** after 50 hits each | Dummy still stands, takes damage, HP decreases; EM DPS display (if any) unaffected |
| R2 | EM training dummy **Lv.1** (same row) | All weapon deltas **0** | As R1; also confirms the filename match covers the whole `training_dummy_lv*` set, not just lv100 |
| R3 | **ExcelTrainingDummy** (each tier: Practice/Dungeon/Seal) | All weapon deltas **0** | Dummy DPS ledger still records the hits (its listener reads final damage independently); session/report flow unchanged |
| R4 | **ExcelSummons as target** — one of each summon type (familiar, archer, bruiser, guardian, revenant, ghast, wolf) summoned by a second player, hit by the test player | All weapon deltas **0** per type | Summon still takes damage, fights, dies, and despawns on its duration as before |
| R5 | **ExcelSummons as pet** — test player's own wolf summon attacking a vanilla zombie | **Taming delta 0** from the summon's hits | Summon still deals damage and its Taming abilities (Gore etc., when unlocked) still modify damage |
| R6 | **Magic SpawnEntity mob NOT adopted by ExcelSummons** (e.g. a raw Magic `familiar`-class spawn via a test wand/spell with Summons adoption disabled or an unmanaged Magic mob) | All weapon deltas **0** (proves the `magic:magicspawned`/`magic:magicmob` stamp path on the live build) | Mob behaves/despawns per Magic's own rules |
| R7 | **Control — regular EliteMobs elite** (a natural elite zombie/husk in the overworld, NOT a custom boss) | Weapon XP **still pays** at the normal per-damage rate | Elite behaves normally; this is the fail-open control for the EM probe |
| R8 | **Control — regular EM custom boss** (any non-dummy customboss file, e.g. a Primis boss) | Weapon XP **still pays** | Boss fight mechanics unchanged; proves the filename match doesn't overreach |
| R9 | **Control — vanilla zombie** (no plugins involved) | Weapon XP **still pays** at the normal rate | — |
| R10 | **Acrobatics direction** — a bruiser/archer summon attacks the test player repeatedly (dodge-eligible hits) | **Acrobatics delta 0** from those attacks | Player still takes the damage (no god-mode side effect); repeat with a vanilla zombie attacker as control: Acrobatics pays as before |
| R11 | **ExcelSummons wolf as pet vs a recognised target** (summon wolf attacking the Lv.100 dummy) | Taming delta **0** (both directions blocked) | Fight proceeds; dummy HP decreases |
| R12 | **RewardGuard-denied entity** (if a fixture can be produced via RG's farm-denial path) | Weapon deltas **0** | RG behaviour otherwise unchanged |

Pass criteria: R1–R6, R10–R12 zero-XP assertions all hold AND R7–R9 controls
pay normally AND no SEVERE/new warnings in the mirror log (one allowed
exception: none expected — probe warnings would mean an API shape drifted on
10.9.14 and must be diagnosed before install). Any control failure is a
stop: the patch does not ship until fail-open is re-proven.

---

## ADDENDUM — Origin-aware Taming skip + damage-test-dummy recognition (Nov 15, 2026, late)

Two changes landed on top of the build above, in the same build copy. The
Deliverables table at the top of this report describes the SUPERSEDED first
build; the current deliverable is:

| Item | Value |
|---|---|
| Built jar | `~/workspace/mcmmo-xpfix-build-20261115/mcMMO.jar` |
| Jar size | **3,719,269 bytes** (was 3,718,764; +505) |
| Jar SHA-256 | **`ef15578a5906192a1ab6eff2036fa487a2ab6426f10aa64a9b93dba28ca075b3`** (was `cf61938c…`) |
| Diff vs previous build | `~/workspace/mcmmo-xpfix-build-20261115/xpfix-origin-addendum.diff` (406 lines; previous build's sources snapshotted in `prev-build-src/`) |
| Full diff vs pristine fork | `~/workspace/mcmmo-xpfix-build-20261115/xpfix.diff` regenerated (783 lines, exactly 5 files: CombatUtils + NoXpEntityRecognition + its test + 2 new test-only stubs) |

### Change 1 — the pet-branch Taming skip is now ORIGIN-AWARE

**Mike's ruling (Nov 15, 2026): cast summons pay NO Taming XP; adopted pets
STILL DO.** The first build skipped the Taming award for any wolf answering
`ExcelSummonsAPI.isSummon(...)` — but that registry is origin-blind, and
ExcelSummons' adoption sweep auto-registers every hand-tamed wolf, so
ordinary pet dogs would have stopped paying Taming XP too.

**How origin is read (the signal exists — no mechanism was invented):**
ExcelSummons has tracked origin since 0.1.4 in its summon record model.
`SummonRecord` carries an `Origin` enum — **CAST** (a Magic cast the bridge
captured at cast time; registered in SummonsListener's cast-capture path) vs
**ADOPTED** (a hand-tamed vanilla pet the sweep merely registered) — with an
`origin()` accessor, exposed to consumers through the public facade
`ExcelSummonsAPI.getRecord(Entity)`. It is the same origin ExcelSummons' own
rank cap ("cap counts CAST summons only") and dismiss rules read, and its
persistence codec restores a record with no origin key as ADOPTED. There is
NO PDC stamp for origin (ExcelSummons writes no persistent-data stamps at
all) — the API record is the only channel, and it is sufficient. Verified
against the shipped ExcelSummons **0.1.13** jar
(`~/workspace/board-r394/summons-mana-build/packet/ExcelSummons-0.1.13.jar`)
by javap — `getRecord(Entity)`, `SummonRecord.origin()`, and the
`SummonRecord$Origin` enum with CAST/ADOPTED are all present — and against
its 0.1.13 sources beside it.

**Implementation (reflective, same EliteProbe pattern as the rest of the
class):** `NoXpEntityRecognition` gains `isCastSummon(Entity)` — it resolves
`ExcelSummonsAPI.getRecord(Entity)` reflectively (its own cached handle and
probe state, same stale-handle/one-warning discipline as `isSummon`), reads
`origin()` and the enum's `name()` from the live objects per call, and
answers true only when the name is exactly `"CAST"` (the pure decision is
factored as `isCastOriginName`, unit-tested directly). `CombatUtils`'
pet branch now consults `isCastSummon(wolf)` instead of `isSummon(wolf)`;
nothing else in CombatUtils changed.

**Fail-open posture preserved, deliberately:** unregistered wolf → pays;
ADOPTED record → pays; a record whose origin reads as anything but CAST →
pays; plugin absent/disabled → pays; `getRecord` unresolvable → probe
marked broken, one warning, pays; any probe exception → handle dropped,
pays. Only a positively-identified cast summon is blocked. One honest edge:
origin is ExcelSummons' canonical record — a cast wolf that somehow escaped
cast capture and was later swept would read ADOPTED and pay; that is the
same record the rank cap trusts, and the fail direction matches the ruling
(only positively-cast pets are blocked).

**Scope discipline:** ONLY the pet branch narrowed. Target-side recognition
(`isNoXpEntity`: stamps, summons of either origin as targets, EM dummies)
and the Acrobatics branch are byte-for-byte unchanged — a summon as a
*target* still grants no weapon XP regardless of origin, per Mike's standing
rule that summons shouldn't give XP of any kind; the ruling's carve-out is
only about an owner earning Taming XP *from* an adopted pet's fighting.

### Change 2 — `damage_test_dummy.yml` now matches the dummy matcher

Under the same standing rule ("training dummy … shouldn't give you XP of
any kind"), the interaction audit found a second dummy-class file in the
live EliteMobs tree: `custombosses/damage_test_dummy.yml` (Lv.50,
healthMultiplier 1000, ai:false; currently unspawnable — no spawn location,
referenced nowhere). The first build's matcher did NOT match it (its own
test listed it as a non-match). `isTrainingDummyFilename` now also matches
that exact basename alongside the `training_dummy_lv*.yml` prefix rule; the
matcher test row is flipped to a match and a directory-prefixed row added.

### Tests

Two test-only stub classes (`src/test/java/com/winkreyes/excelsummons/
ExcelSummonsAPI.java` + `SummonRecord.java`) mirror the real API's class
names and signatures so the new tests drive the genuine reflective path
against a seeded in-memory registry; they are test-classpath only and are
NOT in the shaded jar (verified: no `com/winkreyes/**` entries in the jar).

- Targeted: `mvn test -Dtest='NoXpEntityRecognitionTest,CombatUtilsTest'` —
  **62 tests, 0 failures** (was 52). Recognition class is now **26 tests**
  (was 16): TrainingDummyFilename 11 (10 rows + null; damage_test row
  flipped, +1 prefixed row), Stamps 5, NullSafety 1, and the new
  **CastSummonOrigin 9** — cast wolf → `isCastSummon` true (the exact
  predicate CombatUtils consults before paying, i.e. 0 Taming XP); adopted
  wolf → registered but NOT cast (pays); unregistered wolf → pays;
  plugin-absent → fails open (pays); origin-name matcher rows
  (CAST/ADOPTED/case/garbage) + null.
- Full suite: `mvn test` — **1,936 tests run, 0 failures, 0 errors, 0
  skipped** (previous build 1,926 + 10 new).
- Package: `mvn package -DskipTests` — BUILD SUCCESS; the shaded jar
  contains the updated `NoXpEntityRecognition.class`, and javap on the jar
  confirms `isCastSummon(Entity)` and `isCastOriginName(String)` are in it.
  No database code touched, so the Docker/SQL suites remain unimplicated.

### Rig follow-up (for whoever runs the RIG-PLAN above)

Fixture **R5 is unchanged** (a CAST wolf summon as pet must still show
Taming delta 0). Add one control before install: **R5b — an ADOPTED wolf**
(a hand-tamed wolf the ExcelSummons sweep has registered) fighting as a pet
against a vanilla zombie must show a **normal, non-zero Taming delta** —
that is the behaviour this addendum restores, and it is the half a rig pass
can prove end-to-end against the real ExcelSummons 0.1.13 registry.

## Appendix A — Full diff against the pristine fork

Also saved as `~/workspace/mcmmo-xpfix-build-20261115/xpfix.diff`
(`diff -ruN --exclude=target` over the two `src/` trees).

```diff
diff -ruN '--exclude=target' mcmmo-edit/repo/src/main/java/com/gmail/nossr50/util/skills/CombatUtils.java mcmmo-xpfix-build-20261115/repo/src/main/java/com/gmail/nossr50/util/skills/CombatUtils.java
--- mcmmo-edit/repo/src/main/java/com/gmail/nossr50/util/skills/CombatUtils.java	2026-10-09 00:41:45.368868232 +0000
+++ mcmmo-xpfix-build-20261115/repo/src/main/java/com/gmail/nossr50/util/skills/CombatUtils.java	2026-11-16 04:13:57.656876376 +0000
@@ -564,7 +564,12 @@
             }
 
             event.setDamage(boostedDamage);
-            processCombatXP(mmoPlayer, target, PrimarySkillType.TAMING, 3);
+
+            // WRV: a registered summon fighting as a pet earns its owner no
+            // Taming XP; the summon's abilities above still apply.
+            if (!NoXpEntityRecognition.isSummon(wolf)) {
+                processCombatXP(mmoPlayer, target, PrimarySkillType.TAMING, 3);
+            }
         }
 
     }
@@ -690,6 +695,13 @@
         }
 
         if (target instanceof Player player) {
+            // WRV: an attack from a recognised no-XP entity (training dummy,
+            // summon) grants the defender nothing either — no Acrobatics
+            // dodge XP and no counter-attack processing.
+            if (NoXpEntityRecognition.isNoXpEntity(painSourceRoot)) {
+                return;
+            }
+
             if (ExperienceConfig.getInstance().isNPCInteractionPrevented()) {
                 if (Misc.isNPCEntityExcludingVillagers(target)) {
                     return;
@@ -1123,6 +1135,14 @@
     }
 
     private static double calculatePveBaseXP(@NotNull LivingEntity target) {
+        // WRV: recognised no-XP entities (training dummies, managed summons,
+        // RewardGuard-denied entities) never pay combat XP. Combat XP here is
+        // priced per point of damage with no per-kill cap, so a plugin-made
+        // mega-HP practice target would otherwise pay out indefinitely.
+        if (NoXpEntityRecognition.isNoXpEntity(target)) {
+            return 0;
+        }
+
         double baseXP = 0;
         final EntityType type = target.getType();
 
diff -ruN '--exclude=target' mcmmo-edit/repo/src/main/java/com/gmail/nossr50/util/skills/NoXpEntityRecognition.java mcmmo-xpfix-build-20261115/repo/src/main/java/com/gmail/nossr50/util/skills/NoXpEntityRecognition.java
--- mcmmo-edit/repo/src/main/java/com/gmail/nossr50/util/skills/NoXpEntityRecognition.java	1970-01-01 00:00:00.000000000 +0000
+++ mcmmo-xpfix-build-20261115/repo/src/main/java/com/gmail/nossr50/util/skills/NoXpEntityRecognition.java	2026-11-16 04:13:53.249876394 +0000
@@ -0,0 +1,293 @@
+package com.gmail.nossr50.util.skills;
+
+import java.lang.reflect.Method;
+import java.util.Locale;
+import org.bukkit.Bukkit;
+import org.bukkit.NamespacedKey;
+import org.bukkit.entity.Entity;
+import org.bukkit.persistence.PersistentDataContainer;
+import org.bukkit.plugin.Plugin;
+import org.jetbrains.annotations.Nullable;
+import org.jetbrains.annotations.VisibleForTesting;
+
+/**
+ * WRV house patch: recognises entities that must never be a source of mcMMO XP
+ * in either direction — training dummies and managed summons.
+ *
+ * <p>mcMMO pays weapon XP per point of damage dealt, settled per hit, with no
+ * per-kill or per-entity cap. That is safe against vanilla-sized health pools,
+ * but a plugin-supplied mega-HP practice target (an EliteMobs training dummy
+ * with millions of HP) pays out indefinitely. Recognition here is the choke
+ * point {@link CombatUtils} consults before pricing a target, before paying
+ * Taming XP for a pet's hit, and before letting a defender earn Acrobatics XP
+ * from an attack.
+ *
+ * <p>Every check is a POSITIVE identification and every failure mode is OPEN:
+ * if a partner plugin is absent, its API cannot be resolved, or a probe
+ * throws, the entity is treated as an ordinary mob and pays normally. A
+ * regular EliteMobs elite, a vanilla mob, or anything unrecognised is never
+ * blocked by this class.
+ *
+ * <p>Partner plugins are reached reflectively only (the ExcelProtect
+ * EliteProbe pattern); this fork compiles and runs identically without them.
+ */
+public final class NoXpEntityRecognition {
+
+    // Persistent-data stamps written by the owning plugins.
+    private static final NamespacedKey EXCEL_TRAINING_DUMMY_KEY =
+            new NamespacedKey("exceltrainingdummy", "training");
+    private static final NamespacedKey REWARD_GUARD_DENIED_KEY =
+            new NamespacedKey("excelrewardguard", "denied_v1");
+    // Magic (com.elmakers) stamps every mob it spawns or manages: MAGIC_SPAWNED
+    // is a BYTE boolean, MAGIC_MOB a STRING mob key, both under the "magic"
+    // namespace (verified against the Magic 11.2.5 jar, MagicMetaKeys +
+    // PersistentEntityMetadataUtils).
+    private static final NamespacedKey MAGIC_SPAWNED_KEY =
+            new NamespacedKey("magic", "magicspawned");
+    private static final NamespacedKey MAGIC_MOB_KEY =
+            new NamespacedKey("magic", "magicmob");
+
+    private static final String EXCEL_SUMMONS_PLUGIN = "ExcelSummons";
+    private static final String EXCEL_SUMMONS_API =
+            "com.winkreyes.excelsummons.ExcelSummonsAPI";
+    private static final String ELITE_MOBS_PLUGIN = "EliteMobs";
+    private static final String ELITE_MOBS_TRACKER =
+            "com.magmaguy.elitemobs.entitytracker.EntityTracker";
+
+    // Probe states: 0 = untried, 1 = hooked, 2 = API broken (fail open, logged once).
+    // Plugin absence is never cached: it is re-checked cheaply on every call.
+    private static volatile int summonsProbeState = 0;
+    private static volatile Method summonsIsSummonMethod;
+    private static volatile int eliteProbeState = 0;
+    private static volatile Method eliteIsEliteMobMethod;
+    private static volatile Method eliteGetEliteMobEntityMethod;
+
+    private NoXpEntityRecognition() {
+    }
+
+    /**
+     * Whether the entity is positively recognised as a no-XP entity: one of our
+     * training dummies, a RewardGuard-denied entity, a Magic-spawned or
+     * Magic-managed mob, an ExcelSummons summon, or an EliteMobs training
+     * dummy.
+     *
+     * @param entity the entity to check, may be null
+     * @return true only on a positive identification; false (pay normally) in
+     *         every other case, including probe failure
+     */
+    public static boolean isNoXpEntity(@Nullable Entity entity) {
+        if (entity == null) {
+            return false;
+        }
+
+        if (hasNoXpStamp(entity)) {
+            return true;
+        }
+
+        if (isSummon(entity)) {
+            return true;
+        }
+
+        return isEliteMobsTrainingDummy(entity);
+    }
+
+    /**
+     * Whether the entity is a registered ExcelSummons summon, via the plugin's
+     * own API reached reflectively.
+     *
+     * @param entity the entity to check, may be null
+     * @return true only when ExcelSummons is present and its registry claims
+     *         the entity; false in every other case
+     */
+    public static boolean isSummon(@Nullable Entity entity) {
+        if (entity == null || !pluginEnabled(EXCEL_SUMMONS_PLUGIN)) {
+            return false;
+        }
+
+        final Method method = resolveSummonsMethod();
+        if (method == null) {
+            return false;
+        }
+
+        try {
+            return Boolean.TRUE.equals(method.invoke(null, entity));
+        } catch (Throwable t) {
+            // A stale handle (plugin reloaded under a new classloader) must not
+            // break combat: drop it, re-resolve next time, pay normally now.
+            summonsIsSummonMethod = null;
+            summonsProbeState = 0;
+            return false;
+        }
+    }
+
+    private static boolean hasNoXpStamp(@Nullable Entity entity) {
+        if (entity == null) {
+            return false;
+        }
+
+        try {
+            final PersistentDataContainer container =
+                    entity.getPersistentDataContainer();
+            return container.has(EXCEL_TRAINING_DUMMY_KEY)
+                    || container.has(REWARD_GUARD_DENIED_KEY)
+                    || container.has(MAGIC_SPAWNED_KEY)
+                    || container.has(MAGIC_MOB_KEY);
+        } catch (Throwable t) {
+            return false;
+        }
+    }
+
+    private static boolean isEliteMobsTrainingDummy(@Nullable Entity entity) {
+        if (entity == null || !pluginEnabled(ELITE_MOBS_PLUGIN)) {
+            return false;
+        }
+
+        if (!resolveEliteTrackerMethods()) {
+            return false;
+        }
+
+        try {
+            if (!Boolean.TRUE.equals(eliteIsEliteMobMethod.invoke(null, entity))) {
+                return false;
+            }
+
+            final Object eliteEntity =
+                    eliteGetEliteMobEntityMethod.invoke(null, entity);
+            if (eliteEntity == null) {
+                return false;
+            }
+
+            // isCustomBossEntity lives on the EliteEntity base class; natural
+            // elites answer false here and keep paying full XP (control case).
+            final Method isCustomBoss = eliteEntity.getClass()
+                    .getMethod("isCustomBossEntity");
+            if (!Boolean.TRUE.equals(isCustomBoss.invoke(eliteEntity))) {
+                return false;
+            }
+
+            final Method getConfigFields = eliteEntity.getClass()
+                    .getMethod("getCustomBossesConfigFields");
+            final Object configFields = getConfigFields.invoke(eliteEntity);
+            if (configFields == null) {
+                return false;
+            }
+
+            final Method getFilename = configFields.getClass()
+                    .getMethod("getFilename");
+            final Object filename = getFilename.invoke(configFields);
+            return filename instanceof String name
+                    && isTrainingDummyFilename(name);
+        } catch (Throwable t) {
+            // Any mismatch between this probe and the installed EliteMobs
+            // build fails open: the entity pays normally.
+            return false;
+        }
+    }
+
+    /**
+     * Whether a boss filename names one of the Adventurers Guild training
+     * dummies (training_dummy_lv1/25/50/75/100.yml). Directory prefixes and
+     * case are ignored; every other boss file answers false.
+     *
+     * @param filename the boss filename from EliteMobs, may be null
+     * @return true only for the training-dummy set
+     */
+    @VisibleForTesting
+    static boolean isTrainingDummyFilename(@Nullable String filename) {
+        if (filename == null) {
+            return false;
+        }
+
+        String base = filename.replace('\\', '/');
+        final int slash = base.lastIndexOf('/');
+        if (slash >= 0) {
+            base = base.substring(slash + 1);
+        }
+
+        final String lower = base.toLowerCase(Locale.ROOT);
+        return lower.startsWith("training_dummy_lv") && lower.endsWith(".yml");
+    }
+
+    private static boolean pluginEnabled(@Nullable String pluginName) {
+        if (pluginName == null) {
+            return false;
+        }
+
+        try {
+            if (Bukkit.getPluginManager() == null) {
+                return false;
+            }
+            final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
+            return plugin != null && plugin.isEnabled();
+        } catch (Throwable t) {
+            return false;
+        }
+    }
+
+    private static @Nullable Method resolveSummonsMethod() {
+        if (summonsProbeState == 2) {
+            return null;
+        }
+        if (summonsProbeState == 1) {
+            return summonsIsSummonMethod;
+        }
+
+        synchronized (NoXpEntityRecognition.class) {
+            if (summonsProbeState != 0) {
+                return summonsProbeState == 1 ? summonsIsSummonMethod : null;
+            }
+            try {
+                final Class<?> api = Class.forName(EXCEL_SUMMONS_API);
+                summonsIsSummonMethod = api.getMethod("isSummon", Entity.class);
+                summonsProbeState = 1;
+                return summonsIsSummonMethod;
+            } catch (Throwable t) {
+                summonsProbeState = 2;
+                logProbeFailure(EXCEL_SUMMONS_PLUGIN, t);
+                return null;
+            }
+        }
+    }
+
+    private static boolean resolveEliteTrackerMethods() {
+        if (eliteProbeState == 2) {
+            return false;
+        }
+        if (eliteProbeState == 1) {
+            return true;
+        }
+
+        synchronized (NoXpEntityRecognition.class) {
+            if (eliteProbeState != 0) {
+                return eliteProbeState == 1;
+            }
+            try {
+                final Class<?> tracker = Class.forName(ELITE_MOBS_TRACKER);
+                eliteIsEliteMobMethod =
+                        tracker.getMethod("isEliteMob", Entity.class);
+                eliteGetEliteMobEntityMethod =
+                        tracker.getMethod("getEliteMobEntity", Entity.class);
+                eliteProbeState = 1;
+                return true;
+            } catch (Throwable t) {
+                eliteProbeState = 2;
+                logProbeFailure(ELITE_MOBS_PLUGIN, t);
+                return false;
+            }
+        }
+    }
+
+    private static void logProbeFailure(String pluginName, Throwable cause) {
+        try {
+            if (com.gmail.nossr50.mcMMO.p != null) {
+                com.gmail.nossr50.mcMMO.p.getLogger().warning(
+                        "No-XP recognition: " + pluginName
+                                + " is present but its API could not be resolved ("
+                                + cause.getClass().getSimpleName()
+                                + "); affected entities will pay XP normally.");
+            }
+        } catch (Throwable ignored) {
+            // Logging must never break combat.
+        }
+    }
+}
diff -ruN '--exclude=target' mcmmo-edit/repo/src/test/java/com/gmail/nossr50/util/skills/NoXpEntityRecognitionTest.java mcmmo-xpfix-build-20261115/repo/src/test/java/com/gmail/nossr50/util/skills/NoXpEntityRecognitionTest.java
--- mcmmo-edit/repo/src/test/java/com/gmail/nossr50/util/skills/NoXpEntityRecognitionTest.java	1970-01-01 00:00:00.000000000 +0000
+++ mcmmo-xpfix-build-20261115/repo/src/test/java/com/gmail/nossr50/util/skills/NoXpEntityRecognitionTest.java	2026-11-16 04:14:09.380876330 +0000
@@ -0,0 +1,140 @@
+package com.gmail.nossr50.util.skills;
+
+import static java.util.logging.Logger.getLogger;
+import static org.assertj.core.api.Assertions.assertThat;
+import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.ArgumentMatchers.eq;
+import static org.mockito.Mockito.when;
+
+import com.gmail.nossr50.MMOTestEnvironment;
+import org.bukkit.NamespacedKey;
+import org.bukkit.entity.LivingEntity;
+import org.bukkit.persistence.PersistentDataContainer;
+import org.junit.jupiter.api.AfterEach;
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.Nested;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.params.ParameterizedTest;
+import org.junit.jupiter.params.provider.CsvSource;
+import org.mockito.Mockito;
+
+class NoXpEntityRecognitionTest extends MMOTestEnvironment {
+    private static final java.util.logging.Logger logger = getLogger(
+            NoXpEntityRecognitionTest.class.getName());
+
+    @BeforeEach
+    void setUp() {
+        mockBaseEnvironment(logger);
+    }
+
+    @AfterEach
+    void tearDown() {
+        cleanUpStaticMocks();
+    }
+
+    @Nested
+    class TrainingDummyFilename {
+
+        @ParameterizedTest
+        @CsvSource({
+                "training_dummy_lv1.yml, true",
+                "training_dummy_lv25.yml, true",
+                "training_dummy_lv100.yml, true",
+                "adventurers_guild/training_dummy_lv50.yml, true",
+                "TRAINING_DUMMY_LV75.YML, true",
+                "damage_test_dummy.yml, false",
+                "training_dummy.yml, false",
+                "some_boss.yml, false",
+                "training_dummy_lv100.txt, false",
+        })
+        void filenameShouldMatchOnlyTheGuildTrainingDummySet(String filename,
+                boolean expected) {
+            // When - the filename is checked
+            final boolean recognised =
+                    NoXpEntityRecognition.isTrainingDummyFilename(filename);
+
+            // Then - only the training_dummy_lv* boss files match
+            assertThat(recognised).isEqualTo(expected);
+        }
+
+        @Test
+        void filenameShouldRejectNull() {
+            assertThat(NoXpEntityRecognition.isTrainingDummyFilename(null))
+                    .isFalse();
+        }
+    }
+
+    @Nested
+    class Stamps {
+
+        private LivingEntity entity;
+        private PersistentDataContainer container;
+
+        @BeforeEach
+        void setUpEntity() {
+            entity = Mockito.mock(LivingEntity.class);
+            container = Mockito.mock(PersistentDataContainer.class);
+            when(entity.getPersistentDataContainer()).thenReturn(container);
+            when(container.has(any(NamespacedKey.class))).thenReturn(false);
+        }
+
+        @Test
+        void excelTrainingDummyStampShouldBeRecognised() {
+            // Given - an entity stamped by ExcelTrainingDummy
+            when(container.has(eq(new NamespacedKey("exceltrainingdummy",
+                    "training")))).thenReturn(true);
+
+            // Then - it is a no-XP entity
+            assertThat(NoXpEntityRecognition.isNoXpEntity(entity)).isTrue();
+        }
+
+        @Test
+        void rewardGuardDeniedStampShouldBeRecognised() {
+            // Given - an entity denied by ExcelRewardGuard
+            when(container.has(eq(new NamespacedKey("excelrewardguard",
+                    "denied_v1")))).thenReturn(true);
+
+            // Then - it is a no-XP entity
+            assertThat(NoXpEntityRecognition.isNoXpEntity(entity)).isTrue();
+        }
+
+        @Test
+        void magicSpawnedStampShouldBeRecognised() {
+            // Given - an entity Magic stamped as spawned
+            when(container.has(eq(new NamespacedKey("magic", "magicspawned"))))
+                    .thenReturn(true);
+
+            // Then - it is a no-XP entity
+            assertThat(NoXpEntityRecognition.isNoXpEntity(entity)).isTrue();
+        }
+
+        @Test
+        void magicMobStampShouldBeRecognised() {
+            // Given - an entity Magic stamped as one of its mobs
+            when(container.has(eq(new NamespacedKey("magic", "magicmob"))))
+                    .thenReturn(true);
+
+            // Then - it is a no-XP entity
+            assertThat(NoXpEntityRecognition.isNoXpEntity(entity)).isTrue();
+        }
+
+        @Test
+        void unstampedEntityShouldNotBeRecognised() {
+            // Given - an ordinary mob with no stamps and no partner plugins
+            // answering for it (control case: a regular mob still pays XP)
+
+            // Then - it is not a no-XP entity
+            assertThat(NoXpEntityRecognition.isNoXpEntity(entity)).isFalse();
+        }
+    }
+
+    @Nested
+    class NullSafety {
+
+        @Test
+        void nullEntityShouldNotBeRecognised() {
+            assertThat(NoXpEntityRecognition.isNoXpEntity(null)).isFalse();
+            assertThat(NoXpEntityRecognition.isSummon(null)).isFalse();
+        }
+    }
+}
```

---

## ADDENDUM 2 — mcMMO XP pays ZERO in all EliteMobs worlds (Nov 15, 2026, late)

Mike's ruling (Nov 15, 2026, after the interaction audit): **turn mcMMO XP
off in all EliteMobs worlds**, keeping everything the current build already
does for the overworld seams. Rationale: the audit covered today's 83 EM
packages, but the 57 gated ExcelExpansions packs release over coming months
and each would need its own per-mob audit; a world-level rule covers every
future pack the day it releases. EM content still pays EM's own rewards,
Jobs money, and vanilla XP orbs — only mcMMO XP is zeroed, and only while
the gaining player stands in an EM world.

The deliverables table and first addendum describe the SUPERSEDED build;
the current deliverable is:

| Item | Value |
|---|---|
| Built jar | `~/workspace/mcmmo-xpfix-build-20261115/mcMMO.jar` |
| Jar size | **3,721,966 bytes** (was 3,719,269; +2,697) |
| Jar SHA-256 | **`404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501`** (was `ef15578a…`) |
| Diff vs previous build | `~/workspace/mcmmo-xpfix-build-20261115/xpfix-world-addendum.diff` (846 lines; previous build's changed-file sources snapshotted in `prev-build-src-ef15578a/`) |
| Full diff vs pristine fork | `~/workspace/mcmmo-xpfix-build-20261115/xpfix.diff` regenerated (1,636 lines, exactly 12 files) |
| Pristine fork | `~/workspace/mcmmo-edit/repo` — still untouched (verified: no `NoXpWorld` references anywhere under its `src/`) |

### How EM worlds are identified (evidence, all read-only, Nov 15, 2026)

Three independent live sources agree:

1. **Content-package census.** Every `worldName:` across all 83 live EM
   package files (fresh Homebase pull, `em-gating-packet-20261115/
   gate-pull-1602/`): **75 unique world names — 74 start with `em_`, and
   the single exception is `GoblinKingdom_vf`.**
2. **Boot log** (current live boot): `[EliteMobs] Loading world em_primis !`
   / `Creating world …` / `Successfully loaded world …` — EM loads its
   worlds under exactly those names. (Only the 3 ungated worlds load at
   boot right now; the other 72 base names belong to gated packs and load
   when their chapters release — the same rules catch them on arrival.)
3. **ExcelProtect player streams** (actual gameplay, Nov 2–15): players'
   recorded world names include the instanced copies —
   `em_id_primis_wolf_den_1`, `em_id_primis_lighthouse_keeper_1`,
   `em_id_primis_gladius_statue_1` **and** `em_id_primis_gladius_statue_2`.
   **An instanced copy is the base world name + `_<instance number>`**, so
   instances inherit their base world's classification.

**Classification table — every live world name:**

| World name(s) | Class | Rule that catches it |
|---|---|---|
| `em_adventurers_guild`, `em_beasts_sanctuary`, `em_dark_spire`, `em_diamond_arena`, `em_fireworks`, `em_hallosseum`, `em_id_binder_of_worlds`, `em_id_bloodtemple`, `em_id_bone_monastery`, `em_id_craftenmines_lab`, `em_id_enchantment_challenge_1` … `em_id_enchantment_challenge_20` (all twenty), `em_id_frost_palace`, `em_id_oasis_pyramid`, `em_id_primis_air_temple`, `em_id_primis_counselor`, `em_id_primis_gladius`, `em_id_primis_gladius_statue`, `em_id_primis_ice_elemental`, `em_id_primis_lighthouse_keeper`, `em_id_primis_rat_cellar`, `em_id_primis_sea_monster`, `em_id_primis_sewers`, `em_id_primis_storm_temple`, `em_id_primis_terravilla`, `em_id_primis_trial_of_ice`, `em_id_primis_wolf_den`, `em_id_the_bridge`, `em_id_the_cave`, `em_id_the_city`, `em_id_the_climb`, `em_id_the_deep_mines`, `em_id_the_hallowed_haunt`, `em_id_the_mines`, `em_id_the_nether_bell`, `em_id_the_nether_wastes`, `em_id_the_palace`, `em_id_the_quarry`, `em_invasion`, `em_iron_league_arena`, `em_knight_castle`, `em_north_pole`, `em_oasis`, `em_primis`, `em_primis_elementals_arena`, `em_sewer_maze`, `em_steamworks_lair`, `em_the_airship`, `em_the_catacombs`, `em_the_colosseum`, `em_the_dark_cathedral`, `em_the_pirate_ship`, `em_the_ruins`, `em_the_vampire_manor`, `em_under_grove`, `em_yggdrasil` (74 names) | **EM — pays 0** | Built-in prefix rule: name starts with `em_` (case-insensitive) |
| Any of the `em_id_*` names + `_<number>` (observed: `em_id_primis_wolf_den_1`, `em_id_primis_lighthouse_keeper_1`, `em_id_primis_gladius_statue_1`, `em_id_primis_gladius_statue_2`) | **EM — pays 0** | Same prefix rule; the instance suffix never escapes it |
| `GoblinKingdom_vf` (and `GoblinKingdom_vf_<number>`, should it ever instance) | **EM — pays 0** | Built-in exact-name exception (+ its `name_` instance form), case-insensitive |
| `world`, `world_nether`, `world_the_end` | **NOT EM — pays normally** | No rule matches; the fail posture is pay-normally for anything unmatched |

**Matcher** (`util/skills/NoXpWorldRecognition.java`, new): lowercased
name → `em_` prefix → Goblin Kingdom exact/instance forms → config extras
(exact names set, then prefix list). Self-contained: no dependency on
EliteMobs, ExcelAttunement, or any other plugin (the Attunement ward
maintains its own registry for Magic; this fork rule deliberately needs
nothing but the world name, so it cannot fail when a partner plugin is
absent — and any failure to read the config extras degrades to the
built-in rules, never to blocking the overworld).

**Config extras** (the future-proofing leg): config.yml gains a `WRV`
section — `WRV.No_XP_Worlds.Extra_Names` and `WRV.No_XP_Worlds.Extra_Prefixes`
(both ship empty). `GeneralConfig.getNoXpWorldNames()` /
`getNoXpWorldPrefixes()` resolve them lazily (powerLevelCap pattern:
resolved once, reset by `loadKeys()`), lowercased/trimmed/blank-free.
BukkitConfig copies the new keys into existing installs' config.yml at
startup, and the getters fall back to empty when the section is absent —
so a future pack with an off-pattern world name is a config edit, never
a rebuild.

### Choke points (two gates in `McMMOPlayer`, both keyed on the gaining
player's current world)

1. **`beginUnsharedXpGain`** — early return in an EM world. This is the
   pipeline gate: every normal gain (combat per-damage via
   `AwardCombatXpTask`, pet/Taming, Acrobatics, gathering, fishing,
   PASSIVE gains, `SkillManager` paths) funnels through it, so it stops
   the player's own award, the PartyTogetherBonus, and the gain's
   party-level XP contribution in one place. Party XP shares
   (`ShareHandler`, EQUAL mode) are distributed by calling this same
   method once per member — **a member standing in an EM world receives
   no share**, proven by test. The leak direction is closed too: share
   recipients must be near the earner and `Misc.isNear` is same-world
   (the fork's own `PartyTogetherBonusTest.noBonusWhenMemberInOtherWorld`
   proves the semantics), so an EM-world gain can only ever be offered to
   members in that same world, each gated in turn.
2. **`applyXpGain`** — early return in an EM world. This is the final
   funnel: `EventUtils.handleXpGainEvent` is the only place profile XP
   is written for an online player, its only caller is `applyXpGain`,
   and every path to it passes through here — including the exact-amount
   paths that bypass the pipeline entirely (ExperienceAPI gain overloads,
   `/addxp`, `McMMOPlayer.addXp`, `ExperienceAPI.addXpFromBlockBySkill`).
   A deliberate API/command award to a player standing in an EM world
   therefore also pays 0, per the ruling's blanket wording.

**Paths that remain, stated precisely:**
- `ExperienceAPI.addOfflineXP` writes XP directly to an **offline**
  player's stored profile. An offline player is in no world, so no gain
  can occur "while in an EM world" through it; unchanged by design.
- ExperienceAPI **level-setting** methods (`addLevel`/`setLevel` family)
  manipulate levels, not XP gains; they are admin/API tools and are not
  gain channels. Unchanged.
- **Party level XP** (the party's own progression pool, `Party.applyXpGain`)
  from an *overworld* earner still accrues while a member stands in an EM
  world — it is not player skill XP, and the member's own skill gains are
  all zeroed for the duration. An EM-world earner contributes none (gate 1).
- RewardGuard's seal-step multiplier composes inside the pipeline
  downstream of these gates and is unaffected in the overworld; in EM
  worlds there is no gain left for it to shape, which is the ruling.

### Tests

- **Targeted:** `NoXpWorldRecognitionTest` (30) +
  `McMMOPlayerNoXpWorldTest` (7) + `GeneralConfigNoXpWorldsTest` (3) —
  **40 new tests, all green.** Highlights: the full matcher census rows
  (every EM shape incl. instance copies and the Goblin exception, and the
  near-miss negatives `em`, `emx_world`, `some_em_world`,
  `goblinkingdom_vfx`); config extras by name and by prefix; pipeline
  gains → 0 across Swords/Mining/Acrobatics/Taming in the guild hub and
  in an instanced copy, paying normally in the overworld; the
  exact-amount funnel → 0 in `em_primis`, normal in the overworld; EQUAL
  party share → member in `em_primis` receives 0 while the overworld
  earner keeps their 50, and the overworld-member control receives the
  full 50; the shipped config.yml carries the WRV keys empty and still
  validates.
- **Full suite:** `mvn test` — **1,976 tests run, 0 failures, 0 errors,
  0 skipped** (previous build 1,936 + 40 new). The pre-existing
  recognition, CombatUtils, Together-bonus, and AddXp suites all still
  pass — the gates fail open for the harness's world-less player mock
  exactly as they do for any unmatched world in production.
- **Package:** `mvn package -DskipTests` — BUILD SUCCESS; the shaded jar
  contains `com/gmail/nossr50/util/skills/NoXpWorldRecognition.class`
  (verified via `unzip -l`) and its bundled config.yml carries the
  `WRV.No_XP_Worlds` section (verified via `unzip -p`). No database code
  touched, so the Docker/SQL suites remain unimplicated.

### Rig follow-up (additions to the RIG-PLAN above; run with R1–R12 + R5b)

| # | Fixture | XP assertion | Behaviour assertion |
|---|---|---|---|
| R13 | **Normal EM elite inside an EM world** — a natural elite (NOT a training dummy) in `em_primis` | All weapon deltas **0** — this is the fixture that proves the world rule reaches mobs the filename recognition deliberately still pays for in the overworld (R7's control must still pay there) | Elite fights/dies normally; EM loot and quest progress unaffected |
| R14 | **Control — vanilla zombie in the overworld** (re-run of R9 against this jar) | Weapon XP **still pays** at the normal rate | — |
| R15 | **Instanced copy** — enter an instanced Primis dungeon (mirror assigns e.g. `em_id_primis_wolf_den_1`), hit ordinary dungeon mobs | All weapon deltas **0** for the whole run | Dungeon completes normally; EM rewards/Jobs money/vanilla XP still pay (they are not mcMMO XP) |
| R16 | **Goblin Kingdom** — enter `GoblinKingdom_vf` (if the mirror carries the pack), hit any mob | All weapon deltas **0** — proves the one non-`em_` exception end-to-end | World loads/plays normally |
| R17 | **Party share + command funnel in an EM world** — (a) two test players in an EQUAL-share party, both in `em_primis`: earner hits a normal elite; (b) `/addxp` (or an ExperienceAPI award) given to a player standing in `em_primis`; repeat both in the overworld as controls | (a) earner **and** member deltas **0** in the EM world, both pay in the overworld control; (b) **0** in the EM world, pays in the overworld | Party otherwise functions (chat, item share untouched) |
| R18 | **Config extras** — on the mirror only: create/join any non-EM test world, add its name to `WRV.No_XP_Worlds.Extra_Names`, restart, gain; then remove the entry, restart, gain again | **0** while listed; **pays** once removed — proves a future off-pattern pack is a config edit, not a rebuild | Server boots clean both times; config.yml keeps the WRV section |

**Watch item for a future pass (not this build):** EternalTD is a separate
MagmaGuy plugin, not EliteMobs — if its hub world ever lands under a name
outside these rules and the house wants the same treatment there, it is a
one-line `Extra_Names` config edit by design.
