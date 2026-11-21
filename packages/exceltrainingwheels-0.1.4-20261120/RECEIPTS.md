# RECEIPTS — ExcelTrainingWheels 0.1.4 (D158 shield widening)

## Live-premise receipts (G1) — pulled Nov 20, 2026, live check this morning

Source folder: ~/workspace/trainingwheels-livecheck-20261120/ (pull/ + em-files.txt),
pulled 2026-11-20 ~16:14-16:17 UTC (10:14-10:17 AM CST), same day as this build.

- pull/latest.log — 249,869 B — SHA-256 e85ed89a87dae9dc447030ca7f12f322c2eaea2abdf5e64f2b3519df1a8eb076
  Boot lines in that log (verbatim):
  - "[10:56:52] [ExcelTrainingWheels] Loading server plugin ExcelTrainingWheels v0.1.3"
  - "[10:57:00] [ExcelTrainingWheels] Enabling ExcelTrainingWheels v0.1.3"
  - "[10:57:00] [ExcelTrainingWheels] ExcelTrainingWheels enabled: shield ON (1 protected), door hold ON, iron siege ON (threshold 60s, cap 8), door tiers ON (copper 45s)."
  - "[10:57:00] [ExcelTrainingWheels] shield compensation: seal 5, elite factor 0.1064 (dealt x2.35, base factor 0.25, source: GuildGate)"
  - Server plugin list in the same log: "EliteMobs (10.9.14)", "ExcelTrainingWheels (0.1.3)".
- pull/logs-list.txt — 10,438 B — SHA-256 f2a1e0d79cb24073e57b33bd1e7f287fb4ba5d975156ef5dfe1234c680759fad
- Rotated logs pulled with it: 2026-11-20-1.log.gz (4,668 B), 2026-11-20-2.log.gz (41,568 B),
  2026-11-20-3.log.gz (35,168 B), 2026-11-20-4.log.gz (30,617 B). Zero TW WARN/ERROR and zero
  SHIELD lines across latest.log and all four rotated logs (the gate-failure fingerprint).
- em-files.txt — 283,192 B — SHA-256 08bcf31ad9c72277ebc43421e3be606230a9a140c3fc840418ead09a9ab757d0
  EliteMobs content census: custombosses/primis/primis_crazed_rabbit.yml (RABBIT, level 1,
  damageMultiplier 0.2), primis_crazed_rabbit_large.yml (level 3, x0.4),
  primis_crazed_rabbit_farmstead.yml (level 5, x0.1), mobproperties/elite_killer_bunny.yml
  (baseDamageV2 12.0).
- Live config values (as read by the Nov 20 live check from the live server, same day):
  enabled: true, debug: true, shield.default-factor: 0.25, compensation enabled,
  assume-seal: 0, excluded-worlds: [], dealt-multipliers {0: 1.60, 1: 1.75, 2: 1.90,
  3: 2.05, 4: 2.35, 5: 2.35}; protected entry "00000000-0000-0000-0009-01f05d0d739d":
  {factor: 0.25, name: ".RylieDSunflower"}. The load-bearing values (base factor 0.25,
  seal 5, dealt x2.35, 1 protected) are corroborated by the boot lines above.
- Live jar identity: file name plugins/ExcelTrainingWheels-0.1.7.jar; running code is
  internal version 0.1.3 (boot line above). The live jar bytes were NOT pulled this
  morning, so no live-jar hash is claimed here; the packet's gate is filename +
  internal version, verified by the installer's own fresh pull at install time.

## Build receipts

- P0 base verification: unmodified copy of the 0.1.3 source of record
  (~/workspace/exceltrainingwheels/src-0.1.3/ExcelTrainingWheels/) ran 280/280 unit
  checks and rebuilt byte-identical to the 0.1.3 candidate jar (39,077 B,
  SHA-256 81e76017ef14040845c050b908192bc319d08b642cc8ee37041620d5fc7ee189).
- Unit tests after the fix: 303/303 (246 carried unmodified + 57 in the rewritten
  ShieldRuleTest). Command: src-0.1.4/ExcelTrainingWheels/tests/run_tests.sh.
- Deterministic double-build (src-0.1.4/ExcelTrainingWheels/build.sh, two clean runs):
  both 41,632 B, SHA-256 d60416f3de432bc2c8ac618b425510565681d3a8b29222f222fd20bec165abd0,
  cmp byte-identical. Internal plugin.yml version 0.1.4 == jar filename version.
- Rig (Paper 26.3-235 + EliteMobs 10.9.8 + TW 0.1.4, rig/RIG-RUN3-runtime.log):
  runner status done-marker, exit 0, SEVERE 0. RIGRESULT lines: BOOT PASS;
  R1F PASS (elite killer bunny, EM PersistentTagger tag, 4.7 -> prot 0.5 / vet 4.7,
  ratio 0.10638297872340426 = 0.25/2.35); R1Fb PASS (0.8 -> prot 0.08510638297872342);
  R2 PASS (plain rabbit 4.0 -> 1.0); R3 PASS (killer bunny 8.0 -> 2.0);
  R4 PASS (zombie control 8.0 -> 2.0); R5 PASS (witch MAGIC 6.0 -> 1.5);
  R6 PASS (unattributed POISON 1.0 -> 0.25); R7 PASS (FALL 10.0 -> 10.0 unscaled,
  no SHIELD line); R8 PASS (tamed wolf 8.0 -> 2.0).
  SHIELD lines from that log (verbatim):
  - "SHIELD .RylieDSunflower: ENTITY_ATTACK from RABBIT 4.7 -> 0.5 (factor 0.10638297872340426, elite true, raid x1.0, boss x1.0 NON_BOSS, seals 5)"
  - "SHIELD .RylieDSunflower: ENTITY_ATTACK from RABBIT 0.8 -> 0.08510638297872342 (factor 0.10638297872340426, elite true, raid x1.0, boss x1.0 NON_BOSS, seals 5)"
  - "SHIELD .RylieDSunflower: ENTITY_ATTACK from RABBIT 4.0 -> 1.0 (factor 0.25, elite false, raid x1.0, boss x1.0 NON_BOSS, seals 5)"
  - "SHIELD .RylieDSunflower: MAGIC from WITCH 6.0 -> 1.5 (factor 0.25, elite false, raid x1.0, boss x1.0 NON_BOSS, seals 5)"
  - "SHIELD .RylieDSunflower: POISON (no damager) 1.0 -> 0.25 (factor 0.25, seals 5)"
  - No SHIELD line for FALL anywhere in the run log.
  Rig limit (stated plainly): a full EliteMobs CustomBossEntity spawn was NOT
  achieved on the rig — the rig's EliteMobs custom-boss registry was empty
  (reflective dump: "EMREGISTRY size=0"), so neither the rig fixture modeled on
  primis_crazed_rabbit.yml nor EliteMobs' own test_boss.yml could spawn. The elite
  case was proven against a real rabbit-base entity carrying EliteMobs' real elite
  tag (PersistentTagger.tagElite, isEliteEntity=true — the exact call TW's seam
  reads), through the real Bukkit event pipeline. The genuine-boss link is covered
  by the post-install proof owed below.
