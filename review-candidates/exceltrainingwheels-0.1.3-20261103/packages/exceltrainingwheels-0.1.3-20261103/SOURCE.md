# ExcelTrainingWheels 0.1.3 — SOURCE

- **Plugin:** ExcelTrainingWheels, version 0.1.3
- **Author:** WinkReyesValley (Excel signature — MikeExcel)
- **Built for:** Paper 26.3 (build 235), Java 21 (`--release 21`), compiled with JDK 25 from `~/workspace/.jdk25`
- **Build script:** `build.sh` in the source tree — manual javac route, house build-libs Paper API jar + kyori 4.20 set; deterministic jar (`--date=2026-11-02T00:00:00-06:00`); two consecutive builds byte-identical.
- **Candidate jar:** `ExcelTrainingWheels-0.1.3.jar`, 39,077 bytes, SHA-256 `81e76017ef14040845c050b908192bc319d08b642cc8ee37041620d5fc7ee189`

## Source tree

`~/workspace/exceltrainingwheels/src-0.1.3/ExcelTrainingWheels/` — deposited to Drive (Source folder) as `ExcelTrainingWheels-0.1.3-src.zip`.

Layout:

- `src/com/winkreyes/exceltrainingwheels/ExcelTrainingWheels.java` — Bukkit glue: shield handler, all compensation seams (EliteMobs ladder division, raid-buff division, BossTrial seam), door hold, siege machinery, `/trainingwheels` command.
- `src/com/winkreyes/exceltrainingwheels/core/` — pure logic, fully unit-tested:
  - `BossDivision.java` — **new in 0.1.3**: which admitted boss-damage shapes divide by BossTrial's pressure scale, and the scale sanitizer.
  - `ShieldRule.java` — the admission predicate for the shield (causes, damager kinds); 0.1.3 adds the skull-explosion arm and the boss-attributed SONIC_BOOM / DRAGON_BREATH admissions.
  - `CompensationTable.java`, `RaidDivision.java`, `ProtectedRegistry.java`, `DoorHoldRule.java`, `CopperDoors.java`, `SiegeBook.java` — carried from 0.1.2 unchanged in behavior.
- `tests/` — plain-Java unit suite (`run-tests.sh`), 280 checks.
- `plugin.yml` — version 0.1.3; `softdepend` gains `ExcelBossTrial`.
- `config.yml` — adds the `compensate.bosstrial` block (default `true`). The bundled `protected.players` entry is the 0.1.2 default (factor 0.5); the LIVE config carries Rylie's factor 0.25 and must not be overwritten — see INSTALL-PACKET.md.

## Base verification

The 0.1.2 tree (`src-0.1.2/`) was rebuilt clean-room before any 0.1.3 edit and matched the shipped `ExcelTrainingWheels-0.1.2.jar` byte-identically (SHA-256 `55af9b383eb7400eb0e3060b9b73aec1369ba3037020c58895547e678b92ff1c`, 34,878 bytes). 0.1.3 is a true delta on the live base, not a reconstruction.

## External contract consumed (not bundled)

- **ExcelBossTrial ≥ 0.3.7** (parallel build): `com.winkreyes.excelbosstrial.api.ExcelBossTrialAPI.outgoingScaleFor(LivingEntity)` → double, the boss's current PRESSURE scale (chapter outgoing × attrition ramp; 1.0 when no encounter is open). Consumed reflectively at runtime — no compile-time dependency, no shading. Contract source: `~/workspace/seal-tuning/build-0.3.7/DESIGN.md` §3.1 and the 0.3.7 scaffold's `ExcelBossTrialAPI.java`. Resolution tries `ExcelBossTrialAPI` first, then `BossTrialAPI` in the same package as a fallback. If the plugin is absent, older, or the call fails, the read is 1.0 and the shield behaves exactly as 0.1.2 (fail-soft, modularity law).
