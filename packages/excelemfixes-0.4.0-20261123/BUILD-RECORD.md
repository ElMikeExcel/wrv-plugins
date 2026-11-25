# ExcelEMFixes 0.4.0 — MERGE RECORD (Nov 23, 2026)

The single pack carrying the five completed Nov 22 D195/D198/D199
lanes, merged onto the LIVE 0.3.1 tree by the 0.3.1 merge pattern
(per-module ports into one tree, whole-tree diffs, deterministic
build, full suites + every module rig re-run against the merged
jar). Desk work only — nothing here is installed; the pack is
FILED into the install queue (MANIFEST entries quoted in §7).

**Jar: ExcelEMFixes-0.4.0.jar — 288,362 B — SHA-256
`5d14cc01c05074fc3b51fada5f7aaa2917b2874ae1a6105904e5760ca615e8aa`**
(two builds byte-identical). 14 modules: the nine 0.3.1 modules
unchanged + field-medic, melee-premium, skill-bonus-nudge,
respawn-grace, summon-mount.

---

## 1. What merged, from where

| Module | Lane | Lane jar (dev) | Port shape |
|---|---|---|---|
| summon-mount | ~/workspace/mount-horse-item-20261122/ | 210,719 B, `3c5d0d38…` | 12 added files under `mount/`, registry + plugin.yml + config edits per its MERGE-NOTES.md |
| field-medic | ~/workspace/field-medic-module-20261122/ | 217,063 B, `b9258c11…` | package `fieldmedic/` + test + config section |
| melee-premium | ~/workspace/melee-premium-module-20261122/ | 207,285 B, `89f75762…` | package `meleepremium/` + test + config section |
| skill-bonus-nudge | ~/workspace/skill-nudge-tips-20261122/ | 211,097 B, `b77913d6…` | package `skillnudge/` + 2 tests + config section + THE MERGE EDIT (§2.1) |
| respawn-grace | ~/workspace/respawn-grace-module-20261122/ | 215,826 B, `af6c18ff…` | package `respawngrace/` + test + config section |

Every ported package is **byte-identical to its lane's package**
(`diff -rq` per package: 0 differences, all five). The lanes'
stamped versions (0.3.2 grace, 0.2.0-era mount base, FM/melee/nudge
dev stamps) are all re-stamped 0.4.0 in the five house places:
`ExcelEMFixes.VERSION`, `plugin.yml`, `build.sh`, `README.md`
install line, `rig/build-driver.sh` classpath.

## 2. Whole-tree diff vs the 0.3.1 merged tree

Modified (9): README.md, build.sh, run-tests.sh, src/config.yml,
deploy/config.yml, src/plugin.yml, ExcelEMFixes.java (VERSION
only), ModuleRegistry.java (+5 imports, +5 registry lines, order:
…enderman-resistance, field-medic, melee-premium,
skill-bonus-nudge, respawn-grace, summon-mount), TestMain.java
(+5 suite lines). Added: the five package directories, five test
files, `mount-tests/` (the mount lane's separate suite) +
`run-mount-tests.sh`, libs `ExcelItems-0.1.0.jar` +
`VaultAPI-1.7.jar` (compile-time only). Nothing else. The nine
0.3.1 module packages are byte-identical to 0.3.1.

plugin.yml: `version: 0.4.0`; `softdepend: [EliteMobs,
Geyser-Spigot, Jobs, ExcelItems, Vault]` (the lanes' D199 model,
verbatim); description gains the field-medic + summon-mount
sentences (the lanes' own wording).

### 2.1 The one merge-only code edit — clickable nudge

Per the parent merge note in the nudge lane's RECORD: in
`SkillNudgeModule.check()`, Java players now receive the nudge
with a click event (runs the configured `click-command`, default
`/elitemobs`) + hover text; Bedrock players (via the pack's
`GeyserForms.isBedrockPlayer`) receive the plain message exactly
as before. New config knob `click-command` added to the module's
section in BOTH configs (empty = no click). The module's pure
classes are untouched (unit suites unaffected); the nudge rig was
re-run after the edit (§4). The compiled class in the shipped jar
references `ClickEvent.runCommand` (verified).

## 3. Config assembly

Final deploy/config.yml = live 0.3.1 deploy config
(`74341d9d…`) + the five module sections appended at EOF in
registry order, each extracted verbatim from its lane (deploy
variant where the lane ships one; the field-medic section merged
ONLY from its lane per the brief — its src/deploy sections are
byte-identical anyway). Diff vs 0.3.1: **+277 lines, 0 removed**
(full diff: `receipts-040/config-diff-031-to-040.txt`). Merged
deploy config: 31,870 B, SHA-256
`75f365ea260c5b363b7649f1d19e2423fce064bafb9327fd23e8beb0afc8bb19`.

Section sizes (deploy): field-medic 44, melee-premium 19 (short
deploy form; the shipped src section carries the full comment at
49), skill-bonus-nudge 55 (48 + the 7-line `click-command` block
from §2.1), respawn-grace 80, summon-mount 79.

**WRV values: shipped defaults everywhere** — field-medic
35% / 5 s / 60 s; melee ×1.15; nudge delay 100 ticks,
click-command `/elitemobs`; grace countdown 10 / cap 60 /
window 600; mount price 7500 / npc-price 5625 (= D200's 25%),
combat window 20, loss cooldown 60, horse 0.29 / 0.85 / 24.
No WRV delta exists for any of the five (each lane recorded
"WRV runs the shipped default"; mount's shipped defaults ARE
the WRV values by design). The other four deploy data files are
byte-identical to the 0.3.1 deploy set (hashes match 0.3.1's).

## 4. Proof — all against the merged jar

**Unit suites: 785 passed, 0 failed.** Main suite 735 (524
baseline + 45 field-medic + 33 melee + 34 nudge + 99 grace) via
`run-tests.sh`; summon-mount suite 50 via `run-mount-tests.sh`
(its tests stay a separate suite, as in the lane — they compile
the real ExcelItems core).

**Standard rig** (merged tree's `rig/`, driver expectations
updated to the 14-module set): full boot — 38 results, SEVERE 0,
`BOOT-modules PASS` with all 14 modules; the only FAILs are the
2 pre-existing JobsDummyRig harness fails, identical to the
0.3.1 baseline (BOOT-gate-off's expectation string predates the
module; A-dummy-pays-CONTROL). Ready line: "ExcelEMFixes 0.4.0
ready — modules: [all 14]". modoff boot — 3/3 PASS, enabled
modules=[], /mountcharm gone. (This rig has no ExcelItems, so
the full boot also logged both item modules' stand-down lines —
see the no-ExcelItems row below.)

**Skill-nudge rig** (merged copy `rig-nudge/`, after the §2.1
edit): on **7/7 PASS**, off **5/5 PASS**, SEVERE 0 — identical to
the lane's results; the fake Jester (ARMOR 14 / WANDS 17, `{}`
selections) got exactly one nudge naming 6 claimable, none after
claiming + rejoin.

**Respawn-grace rig** (merged copy `rig-grace/`): **all_pass:
true** — on 24/24, cap 8/8, off 7/7, SEVERE 0 in every boot
(the D201 model end-to-end: ticket → PRELOOT → own-loot
countdown → flourish; attack ends instantly; unrelated pickup
never starts it; world-hop grants nothing).

**Field-medic rig** (merged copy `rig-fieldmedic/`): full —
**13/13 ASSERT PASS** + RESULT PASS (registration id 6 =
field_bandage, **next free id 7**; phase totals 35.0 HP @ pool
100 and 24.5 HP @ pool 70 through CUSTOM regain events, in
combat); off — RESULT PASS (module absent, id 6 unregistered);
noitems — boot Done, SEVERE 0, ready line with all 14. SEVERE 0
in all three boots.

**Summon-mount rig** (merged copy `rig-mount/`): full **27/28**,
modoff **2/3**, noitems **4/4**, SEVERE 0 everywhere. The two
non-passes are the lane driver's registry-definition COUNT
expectations, written when summon-mount was the only item
module: full expected 5 definitions, the merged pack has 6
(4 builtins + mount_charm + field_bandage); modoff expected 4,
the merged boot has 5 (field-medic still on). The identity
checks all pass — BOOT-mint (create(5) = mount_charm) here, and
the FM rig's `definition6=field_bandage nextId=7` with the same
jar — so the registry state is exactly right; the counts are a
harness artifact of merging two item modules, recorded here
rather than silently re-baselined. Everything else passes:
EM NATURAL-vs-CUSTOM A/B under real EliteMobs, both buy venues
($7,500 / $5,625 charged exactly), combat gate, loss cooldown,
death/world-change despawns, stand-down with log line.

**Melee verify probe** (merged copy `rig-melee/`): PASS —
elite melee drop **1.0350** and ledger **1.0350** (0.9 × 1.15,
mechanism C in the merged tree), wand 0.4200/0.4200 untouched,
non-elite in-scope drop 0.2392 (base-scaled), overworld elite
0.9/0.9 (scope gate), SEVERE 0. Numbers identical to the lane's
own probe receipt.

**Food-heal regression rig** (merged copy `rig-foodheal/`,
merged config with food-heal debug on): PASS, SEVERE 0 — the
0.2.1 invariant reproduces with the merged jar: regain-event
totals equal delivered HP to the cent in both phases
(Phase A CUSTOM = 36.00 = Δ36.0; Phase B CUSTOM = 48.60 =
Δ48.6, n=58 — Phase B identical to the digit with the 0.2.1
fixed run; Phase A's effective window ran shorter this boot,
which is why its absolute is 36 not 45 — the module code is
byte-identical to 0.3.1's, per the §2 tree diff).

**No-ExcelItems stand-down (brief item c)**: verified TWICE
with the merged jar — (1) the standard rig's full boot (no
ExcelItems in that rig): both stand-down lines logged verbatim,
pack ready with all other modules live, SEVERE 0; (2) the FM
rig's noitems boot: same two lines, boot Done. The mount rig's
noitems scenario additionally asserts the mount module's
stand-down in-driver (4/4).

## 5. The potion finding (brief items a + filing 3) — READ

The brief's premise — "the potion files' hard coupling is
satisfied because this pack IS the field-medic window" — does
**not** hold against the field-medic build as delivered. Grep of
the lane's source at merge time: the module registers ID 6
`field_bandage` ONLY; there is no potion registration at any ID,
no `fieldmedic buy` command/flow, and no `guildvendor` NPC flow
(the catalog pass's §7 assumed all three). Consequences, all
recorded rather than papered over:

1. **ID re-point done**: the potion's staged special-products
   config now names `excelitems_id: 7` (was 6) — the next free
   ID, proven free by the FM rig (nextId=7 after both item
   modules register). Applied to
   `~/workspace/shop-catalog-pass-20261122/staged/plugins/ExcelShops/config.yml`
   (new SHA-256 `9bd7ee11…`, 2,743 B), its header comment,
   `staged/vendor-npc-listings.yml` (status text; new SHA
   `cd34ed7c…`), and the potions-menu NPC lore ($120 → $112.50,
   the one stale D200 figure; new SHA `22710323…`). Diffs
   regenerated; a note is appended to the catalog RECORD as its
   §10.
2. **Unit 1 (pearl re-bundle) filed as ready** — it never
   depended on the buy flows at list price.
3. **Unit 2 (potion + NPC data) filed with its hard gate
   intact and the blocker stated in the entry**: landing the
   staged config today would break ExcelShops (fail-closed
   parse on unregistered ID 7). Unit 2 waits on a follow-up
   build that registers ID 7 `strong_healing_potion` and
   provides the `fieldmedic buy` + `guildvendor` flows, then
   lands in THAT window. The same `guildvendor` flow is what
   the pearl NPC row and the PlayerMap NPC row wait on.

## 6. Documentation audit (Mike's Nov 23 rule — every module,
## config comments + README)

Audit method: every module's section in the merged
src/config.yml was measured and read (comment lines / words
below), and every README module section was measured and read.
The standard: a clear plain-language explanation of what the
module does — defect/gap → fix — in BOTH places, with shipped
default vs WRV-tuned values stated where they differ.

| Module | Config comments | README |
|---|---|---|
| quest-destinations | ✓ existing (27 lines / 220 w) | ✓ existing (305 w) |
| arcane-bolt | ✓ existing (29 / 236) | ✓ existing (101 w) |
| drop-guide | ✓ existing (13 / 104) | ✓ existing (103 w) |
| progress-tracker | ✓ existing (22 / 163) | ✓ existing (81 w) |
| food-heal | ✓ existing (35 / 287) | ✓ existing (147 w) |
| leash | ✓ existing (23 / 199) | ✓ existing (136 w) |
| seal-restore | ✓ existing (16 / 130) | ✓ existing (114 w) |
| jobs-dummy-gate | ✓ existing (32 / 268) | ✓ existing (253 w) |
| enderman-resistance | ✓ existing (58 / 493) | ✓ existing (267 w) |
| field-medic | ✓ existing — lane's section (37 / 297) | ✓ existing — lane's section (259 w) |
| melee-premium | ✓ existing — lane's section (42 / 331) | ✓ existing — lane's section (266 w) |
| skill-bonus-nudge | ✓ existing — lane's section (45 / 361), + the click-command block WRITTEN AT MERGE | ✓ existing — lane's section (235 w) |
| respawn-grace | ✓ existing — lane's section (59 / 538) | ✓ existing — lane's section (313 w) |
| summon-mount | ✓ existing — lane's section (60 / 495) | ✓ WRITTEN AT MERGE (258 w; the lane owed this section — product framing per its MERGE-NOTES) |

Verdict: no existing module was missing or thin on either
surface — the thinnest two (drop-guide config, progress-tracker
README) were read in full and both explain the gap and the fix
plainly. The only writing the rule required in this merge was
the summon-mount README section and the nudge click-command
config block, both done. Shipped-vs-WRV: the pack's "What Wink
Reyes Valley runs differently" table covers the tuned modules;
all five new modules run shipped defaults (§3), so no new rows.

## 7. Filing

Filed into ~/workspace/wrv-install-queue/MANIFEST.md PENDING
(Nov 23, 2026): (1) the 0.4.0 pack as ONE gated entry (jar +
deploy config; backup-first, verified-empty, restart) with
artifacts, hashes, boot-verify and the per-module proof list;
(2) skill-nudge TEXT entries B1 (ungated) + B2 (enderman
broadcaster line; its 0.3.1 gate is satisfied), each carrying
the PlayerMap-overlap note (same tips/broadcaster files,
appends/different blocks only, gate hashes STOP-on-mismatch,
second to land re-bases); (3) catalog Unit 1 (pearls) as a
ready gated entry with the ExcelMenu path correction, and
Unit 2 under its intact hard gate per §5. Entry A of the nudge
lane rides the pack entry. Backlog P-EM-COMBAT-PACKAGE-20261122
updated (not closed — install + the closing announcement ride
after the failsafe lands the pack).

## 8. Receipts

`receipts-040/`: rig-standard-full-boot.log (ready line +
stand-downs), config-diff-031-to-040.txt. Build-root `receipts/`:
rig-full.json, rig-modoff.json, rig-skillnudge-{on,off}.json,
rig-grace.json, rig-verify-melee.json. Merge-home `receipts/`:
mount rig rig-{full,modoff,noitems}.json. FM rig logs:
build-root/rig-fieldmedic/receipts-merged-{full,off,noitems}.log.
Food-heal: build-root/rig-foodheal/logs/.
