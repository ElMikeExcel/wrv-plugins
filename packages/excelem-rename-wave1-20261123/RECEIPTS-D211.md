# RECEIPTS-D211 - SealAlerts companion build evidence (Nov 23, 2026)

Companion to packets/RECEIPTS.md (wave-1 receipts, sha256 abae3d3a...).
All live evidence below was pulled fresh on Nov 23, 2026 via the
Homebase SSH route (background only, zero console input).

## Fresh live census + pulls (Nov 23, 2026 18:24:06 -06:00, PC-side)

Census script: pulls/census-d211.ps1, run on the PC; output file
pulls/d211-census.txt, sha256
b351d39def7bb14def084d37c378dc93ec51fca989af3c0b7145cf94ad157b1a.
Jars pulled to pulls/live-jars-d211/ and hash-verified locally against
the PC-computed Get-FileHash values (exact match both jars):

- plugins/ExcelSealAlerts-0.1.2-runtime-diagnostic-OFFLINE.jar =
  43,426 B / b3f1bb5470d3fcf79653b05b822b33b70e21d028f0d6a46750da853bf5cf8345
  (identical to the staging record's audit census value - no drift).
- plugins/ExcelSealLog-1.0.0.jar = 8,420 B /
  e75de2643cf0e5f1e34902574f2bc3732144356ae6609dae1c15b231b61eb0be
  (identical to the wave-1 census of 18:07 CT the same day - no drift).
- FOLDER ExcelSealAlerts present, 6 files; config.yml 2,233 B /
  434ca6319fc1b7a9d08ceaea7eaefb871ee1efbc12429bc599c15f14e6952875
  (the deployed config carrying the webhook - the swap must not touch
  this folder), data.yml 177 B, plus 4 archive/held-aside files.
- FOLDER ExcelSealLog ABSENT; FOLDER ExcelEMSealLog ABSENT (stateless,
  as staged).

Live jar internals (from the pulled bytes): plugin.yml version
'0.1.2-runtime-diagnostic-OFFLINE', softdepend [ExcelSealLog,
ExcelGuildGate, ExcelBossTrial]; constant pools confirm the coupling
sites in the live build: ExcelSealAlerts.class holds "ExcelSealLog" in
the TAPPED list, SealParser.class holds "ExcelSealLog" as its dispatch
key. 16 jar entries, sorted, no manifest.

## The coupling has THREE functional sites (source-verified)

Source: ~/workspace/sealalerts-fix/source-0.1.2 (provenance snapshot of
the live build). The by-name reference to SealLog lives at:
1. ExcelSealAlerts.java:33 - TAPPED = List.of("ExcelSealLog", ...).
2. plugin.yml:9 - softdepend: [ExcelSealLog, ...].
3. SealParser.java:76 - accept() dispatch: "ExcelSealLog".equals(plugin)
   routes tapped lines to the sealLog rules. The tap handler passes
   p.getName() (ExcelSealAlerts.java:112), so a TAPPED-only fix would
   attach the tap yet silently drop every SealLog fight line at the
   parser - the boot proof cannot see it. All three sites are the one
   logical change (SealAlerts' by-name reference to SealLog) and move
   together. Remaining "ExcelSealLog" occurrences in the 0.1.3 source
   are comments only (javadoc), verified by diff + grep.

## Candidate build (deterministic)

Build tree: sealalerts-build/ (source-0.1.3/ is a copy of source-0.1.2
with exactly 4 changed lines: TAPPED entry, SealParser dispatch key,
softdepend entry, version '0.1.2-runtime-diagnostic-OFFLINE' -> '0.1.3';
diff -r against source-0.1.2 shows nothing else). GuildGate/BossTrial
strings untouched everywhere. The hardcoded webhook wire identity
("ExcelSealAlerts 0.1.0 (server plugin)") is deliberately preserved,
per the source's own comment.
Build: sealalerts-build/build.sh - JDK ~/workspace/.jdk25, javac
--release 21 -proc:none, classpath = rig paper-api 26.3.build.256 +
excelshops build-libs set; packaging = sorted entries, fixed date
2026-11-23, no manifest (the live jar's own recipe shape).
Two independent clean builds byte-identical:
ExcelSealAlerts-0.1.3.jar = 43,153 B / sha256
26c2bb34b89b7f7baf48cde5c98b7bc260eb8da70249967aaa9f4346fc985159.
Entry set identical to the live jar (16 entries). Candidate constant
pools: ExcelSealAlerts.class TAPPED = [ExcelEMSealLog, ExcelGuildGate,
ExcelBossTrial]; SealParser.class dispatch key = ExcelEMSealLog; no
standalone "ExcelSealLog" constant remains in either class.
Candidate staged at builds/ExcelSealAlerts-0.1.3.jar (SHA256SUMS.txt
updated).

## Parser dispatch check (harness, not shipped)

sealalerts-build/harness/.../DispatchCheck.java, run against the
candidate classes - ALL PASS:
- T1 accept("ExcelEMSealLog", FIGHT_START line) -> FIGHT_START event.
- T2 accept("ExcelEMSealLog", BOSS_DEATH line) -> FIGHT_END event.
- T3 accept("ExcelSealLog", FIGHT_START line) -> no events (old name
  no longer routed, as designed).
- T4 accept("ExcelGuildGate", "Seal broken: ..." line) -> SEAL_BROKE
  event (GuildGate dispatch unchanged).

## Rig boots (staging rig: Paper 26.3 build 256 + live EliteMobs jar)

- boot-D1-sealalerts013-emseallog.log - NEW SealAlerts 0.1.3 + renamed
  ExcelEMSealLog: Done (33.941s), SEVERE 0, ERROR 0.
  "[SealAlerts] READY version=0.1.3 taps=[ExcelEMSealLog]";
  "sealalerts status" -> "Watching: [ExcelEMSealLog], open fights: []".
  server_started outbox event: not_watching=[ExcelGuildGate,
  ExcelBossTrial] - no SealLog name missing (GuildGate/BossTrial are
  absent from the rig itself, so plugins_ok reads false in every rig
  boot including the C1 control; the pair signal is the not_watching
  list).
- boot-D2-sealalerts013-oldseallog.log - NEW SealAlerts 0.1.3 + OLD-name
  ExcelSealLog (mismatch control): READY version=0.1.3 taps=[],
  "Watching: []", not_watching=[ExcelEMSealLog, ExcelGuildGate,
  ExcelBossTrial], SEVERE 0. The new build cannot tap the old name:
  the SealAlerts swap and the SealLog rename are one atomic unit.
- Controls from staging (same rig): boot-C1 (old SealAlerts + old
  SealLog) taps=[ExcelSealLog], not_watching without SealLog;
  boot-C2 (old SealAlerts + renamed SealLog) taps=[], not_watching
  includes "ExcelSealLog". Both mismatch directions proven silent
  (SEVERE 0, no error) - the window must land both halves together
  or neither.
- Rig left in the D1 target set (ExcelEMSealLog + SealAlerts 0.1.3).
