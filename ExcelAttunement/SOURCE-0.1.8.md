# ExcelAttunement 0.1.8 (Fix A) — SOURCE / base proof

Built Nov 1, 2026 (CST) by the build agent for the parent (Iris). Build +
test + packet only — nothing installed, no live contact.

## The defect (from the investigation)

`~/workspace/wandbars-ux-build/STANCE-SPELL-LOCK-INVESTIGATION.md`: ExcelAttunement's
`Weapons.kindOfProjectile` classified ANY player-launched arrow as BOW
(martial) unless a crossbow was in hand, so a wand-cast Magic **Arrow**
spell hit set MARTIAL stance ("fighter stance", wand stilled 30 s,
refreshed by the fight itself). Jester hit it live Nov 1, 2026, the night
the Arrow fix first let the spell land. Fix A (§8 of the investigation):
judge an arrow by the hand that loosed it — BOW in hand → BOW, CROSSBOW
in hand → CROSSBOW, anything else → NONE (never recorded in the
ProjectileTracker). TRIDENT unchanged.

## HARD GATE 1 — version reconciliation: PASSED (recovery, not reconstruction)

The deposited source of record in `~/workspace/excelattunement/` stops at
0.1.2 (jar `52d38c53…`); the live jar is 0.1.7 (Steve-era, Oct 26, 2026).
The 0.1.7 code was recovered from two independent deposits and proven to
be the live code before any fix work:

1. **The live jar itself** — Google Drive folder "2026-10-26 live custom
   jars backup" (the Oct 26 jar-backup pass recorded in the shared
   assistants file), file `ExcelAttunement-0.1.7.jar`, 70,788 B.
   Downloaded copy: `recovered-0.1.7/ExcelAttunement-0.1.7.jar`.
   SHA-256 `4b5d8837eca4039e0d3f63a33fdd849a94e245718cbe769c51725c0d71665dcc`
   — exactly the hash in the Oct 26 install record ("plugins/ExcelAttunement-0.1.7.jar
   sha256 4b5d8837…", server readback matched), and the Drive folder's own
   SHA256SUMS agrees.
2. **The full 0.1.7 source tree** — GitHub `ElMikeExcel/wrv-plugins`,
   draft PR #79, branch `feature/excelattunement-0.1.7-scroll-fix`
   @ 22969e16c3 (the record PR for the live install). Cloned to
   `gh-0.1.7/`. Its `ExcelAttunement/SHA256SUMS-0.1.7.txt` names the same
   jar hash `4b5d8837…`, the 0.1.6 rollback jar `441d3f70…` (matches the
   install record), a 0.1.6→0.1.7 patch, and a source zip.
3. **Rebuild proof (decisive):** the recovered tree, built with its own
   `build.sh` on this box (JDK 25.0.4.1), produces a jar whose **every
   class file and resource is byte-identical to the live 0.1.7 jar**
   (41 entries compared individually). The only differing entry is
   `META-INF/MANIFEST.MF`, whose `Created-By` line names the build JDK:
   the live jar was built with JDK 25.0.5, this box has 25.0.4.1 — a
   2-byte jar-size difference (70,790 vs 70,788) in the manifest stamp
   only. Two rebuilds on this box are byte-identical to each other
   (`ecea9eb0…`). The recovered tree IS the 0.1.7 code.
4. **Defect confirmed in the shipped bytes:** `javap` of the recovered
   live jar's `Weapons.kindOfProjectile` shows the exact fallback —
   ARROW/SPECTRAL_ARROW → `kindOfItemName(hand)`; CROSSBOW → CROSSBOW;
   otherwise an unconditional BOW return. `onLaunch(ProjectileLaunchEvent)`
   and `resolveAttack(EntityDamageByEntityEvent)` are present with the
   shapes the investigation cited from 0.1.2.
5. **What 0.1.3–0.1.7 added over 0.1.2** (class-inventory diff, 0.1.2 jar
   → 0.1.7 jar: 15 → 32 classes): ArenaLock set (arena locks — movement
   spells sealed near a live Ender Dragon), WorldWard/WardRules/WardWorlds/WardNotice
   (guild ward — Magic silent in EliteMobs worlds), HotbarSkip/LockRule/LockFeedback/StanceText
   (0.1.4+ item locks + the 0.1.7 stance-scroll hop), MagicBridge$InFlight,
   ExcelAttunement$1. Of the 15 shared classes, 5 differ (ExcelAttunement,
   $Attack, MagicBridge, AttunementState, AttunementState$1) and 10 are
   byte-identical — **including `Weapons.class`, `WeaponKind.class`,
   `DamageRule.class` and `ProjectileTracker.class`**: the defective
   classifier in live 0.1.7 is byte-for-byte the 0.1.2 code the
   investigation read. Per-version color from the record: 0.1.3 = Magic
   heal-kit spells classed SUPPORT (PR #58); 0.1.4 = hotbar item locks
   (the scroll trap); 0.1.7 = the stance-scroll hop fix (PR #79,
   CHANGELOG-0.1.7.md in the recovered tree). The fix base carries all of
   it because the base is the proven 0.1.7 tree itself.

## HARD GATE 2 — spell table override: undisturbed

The live spell table is the data-folder override
(`plugins/ExcelAttunement/spell-classes.txt`, 106 rows, installed Oct 31,
2026). In the 0.1.7 source, `ExcelAttunement.loadSpellTable()` (line ~627)
reads `new File(getDataFolder(), "spell-classes.txt")` first and logs
"Using spell table override: …", falling back to the jar-bundled table
only when no override exists. Fix A touches none of that code
(`ExcelAttunement.class` is byte-identical between the 0.1.7 and 0.1.8
jars — see the scope proof), the jar-bundled `spell-classes.txt` and
`config.yml` are byte-identical between the jars, and the rig boot with
the live 106-row override in the data folder printed both
"Using spell table override: plugins/ExcelAttunement/spell-classes.txt"
and "spell table 106 spells". Nothing to regenerate for this fix.

## The 0.1.8 change (scope-proven)

Work tree: `work/ExcelAttunement/` (the recovered 0.1.7 tree plus):

- `src/com/winkreyes/excelattunement/core/Weapons.java` — Fix A in
  `kindOfProjectile` only: BOW hand → BOW, CROSSBOW hand → CROSSBOW, any
  other hand (wand/stick/empty/null) → NONE, with the reasoning comment.
- `plugin.yml` — version 0.1.7 → 0.1.8 (the only plugin.yml change).
- `tests/RangedMartialTest.java` — 12 new checks (see TEST-REPORT.md).
- `tests/WardTest.java` — the plugin.yml version pin moved 0.1.7 → 0.1.8
  (same practice as the 0.1.7 build).
- `build.sh` — version comment, default OUT name, jar date 2026-11-01.

**Jar scope proof** (entry-by-entry SHA-256, 0.1.8 jar vs the real live
0.1.7 jar): 36 file entries in both; exactly 3 differ —
`core/Weapons.class` (the fix; bytecode verified: BOW→BOW, CROSSBOW→CROSSBOW,
else NONE), `plugin.yml` (version line only), `META-INF/MANIFEST.MF`
(the JDK Created-By stamp described above; the 0.1.8 jar is built on this
box, so it carries 25.0.4.1 like the base rebuild — cosmetic, and the
same stamp the base rebuild carries). Everything else — all stance,
ward, arena-lock, tracker, classifier classes, config.yml, spell-classes.txt —
is byte-identical to live.

## Artifacts (this folder)

- `jars/ExcelAttunement-0.1.8.jar` + `jars/ExcelAttunement-0.1.8-repro.jar`
  — two builds, byte-identical: SHA-256
  `971292ede865b0a0ae6fd436ce90d48fa3a96e58cc73d26f8e152b2e4822262a`, 70,796 B.
- `source/ExcelAttunement-0.1.8-src.zip` — the 0.1.8 work tree (src, tests,
  tools, build.sh, run-tests.sh, plugin.yml, config.yml, spell-classes.txt),
  SHA-256 `e4c3992745d30ad1c9036764ea79d91440dbcaaddff8f3ad29c0e0097f0afbad`.
- `recovered-0.1.7/` — the Drive-recovered live 0.1.7 jar + SHA256SUMS +
  the two local rebuilds (`ecea9eb0…`).
- `gh-0.1.7/` — the PR #79 clone (0.1.7 source of record).
- `work/ExcelAttunement/` — the 0.1.8 build tree.
- `rig/` — RigDriver2 source + the preserved rig logs
  (`rig-run1-latest.log`, `rig-run3-latest.log`; run 2 was the
  aim-confounded run, also preserved).

Deposit recommendation for the parent: the recovered 0.1.7 tree should
also be filed as the Attunement source of record next to the 0.1.2
deposit in `~/workspace/excelattunement/` (it closes the "deposited
source is 0.1.2, live is 0.1.7" drift caveat), and the 0.1.8 source zip
deposited with the jar per the standing deposit rule.
