# D211 Filing Record - SealAlerts companion build + wave-1 packet completion

**Status:** COMPLETE Nov 23, 2026 - SealAlerts 0.1.3 built + proven,
packet rewritten to 5 changes, MANIFEST stub FILED. Nothing installed.
**Authority:** D211 (decision log, Nov 23, 2026 ~6:21 PM CT) - option (a):
a one-string ExcelSealAlerts build rides the wave-1 window so SealLog can
rename in wave 1; the SealAlerts swap and the SealLog rename land in the
SAME window as one atomic unit.
**Lane rules:** no installs, no sends/posts, no purchases, no deletions.
Together + RaidBuff untouched.

## Plan

1. Build ExcelSealAlerts 0.1.3 from ~/workspace/sealalerts-fix/source-0.1.2
   with the SealLog name reference moved: TAPPED entry + plugin.yml
   softdepend "ExcelSealLog" -> "ExcelEMSealLog"; version bump 0.1.2 ->
   0.1.3. GuildGate / BossTrial entries unchanged. Deterministic build
   (build twice, byte-compare).
2. Parser-dispatch check (see the coupling note below) + rig boots:
   D1 = new SealAlerts + renamed ExcelEMSealLog (expect READY
   taps=[ExcelEMSealLog], SEVERE 0); the old-name control is the staging
   C1/C2 pair plus a noted D2 statement in the packet.
3. Rewrite packets/INSTALL-PACKET.md to cover all 5 changes with the
   same-window atomicity requirement in the header; fresh live-premise
   receipts for the SealAlerts jar + SealLog jar (Homebase SSH pull).
4. File the PENDING pointer stub in the MANIFEST.

## Coupling note (found at source read, before building)

The name "ExcelSealLog" is a functional reference in THREE places, not
two: (1) ExcelSealAlerts.java:33 TAPPED list; (2) plugin.yml softdepend;
(3) SealParser.java:76 - accept() dispatches on the tapped plugin's
Bukkit name: `"ExcelSealLog".equals(plugin)` routes lines to the sealLog
parser. The tap handler passes p.getName() (ExcelSealAlerts.java:112),
so after the rename the parser would receive "ExcelEMSealLog" and, if
site (3) were left unchanged, silently drop every SealLog fight line
while the boot proof (taps + plugins_ok) still reads healthy - exactly
the silent degradation D211 exists to prevent. All three sites are the
same single logical change (SealAlerts' by-name reference to SealLog)
and all three move together; GuildGate/BossTrial strings are untouched
everywhere. This note is the deviation record against the task brief's
"TAPPED + softdepend" wording; the parent report flags it explicitly.

## Evidence

### Fresh live premises (Homebase SSH, Nov 23, 2026 18:24:06 -06:00)

Census pulls/census-d211.ps1 -> pulls/d211-census.txt (sha256
b351d39d...); jars in pulls/live-jars-d211/, PC hashes cross-verified
locally (exact match):
- ExcelSealAlerts-0.1.2-runtime-diagnostic-OFFLINE.jar 43,426 B /
  b3f1bb5470d3fcf79653b05b822b33b70e21d028f0d6a46750da853bf5cf8345
  (no drift vs the staging census).
- ExcelSealLog-1.0.0.jar 8,420 B / e75de2643cf0e5f1e34902574f2bc3732144356
  ae6609dae1c15b231b61eb0be (no drift vs the 18:07 CT wave-1 census).
- ExcelSealAlerts folder: 6 files, config.yml 2,233 B / 434ca631...
  (deployed webhook config - swap leaves the folder untouched).
- ExcelSealLog + ExcelEMSealLog folders ABSENT live.
Live jar internals re-verified from the pulled bytes: plugin.yml
version '0.1.2-runtime-diagnostic-OFFLINE', softdepend as expected;
"ExcelSealLog" constants present in BOTH ExcelSealAlerts.class
(TAPPED) and SealParser.class (dispatch) - the coupling sites exist
in the live build exactly as in the source snapshot.

### Build (sealalerts-build/)

source-0.1.3/ = source-0.1.2 copy + exactly 4 changed lines (diff -r
verified): TAPPED entry, SealParser dispatch key, plugin.yml
softdepend entry - all "ExcelSealLog" -> "ExcelEMSealLog" - plus
version '0.1.2-runtime-diagnostic-OFFLINE' -> '0.1.3'. GuildGate /
BossTrial strings untouched; remaining "ExcelSealLog" occurrences
are comments only. The hardcoded webhook wire identity string is
preserved per the source's own instruction.
build.sh: JDK ~/workspace/.jdk25, javac --release 21 -proc:none,
paper-api 26.3.build.256 classpath; packaging sorted entries /
fixed date 2026-11-23 / no manifest (the live jar's recipe shape).
Two independent clean builds BYTE-IDENTICAL:
**ExcelSealAlerts-0.1.3.jar = 43,153 B / sha256
26c2bb34b89b7f7baf48cde5c98b7bc260eb8da70249967aaa9f4346fc985159**,
16 entries (entry set identical to the live jar); constant pools
show ExcelEMSealLog at both sites, no standalone "ExcelSealLog"
constant left. Staged at builds/ExcelSealAlerts-0.1.3.jar;
builds/SHA256SUMS.txt updated (5 candidates).

### Parser dispatch harness (sealalerts-build/harness/, not shipped)

ALL PASS against the candidate classes: T1 "ExcelEMSealLog"
FIGHT_START -> FIGHT_START event; T2 BOSS_DEATH -> FIGHT_END event;
T3 old name "ExcelSealLog" -> no events; T4 GuildGate "Seal broken:"
-> SEAL_BROKE event (unchanged). This is the proof that the third
site mattered: with a TAPPED-only fix, T1/T2 produce nothing while
every boot signal reads healthy.

### Rig boots (staging rig, Paper 26.3 build 256 + live EM jar)

- D1 (boot-D1-sealalerts013-emseallog.log): NEW SealAlerts 0.1.3 +
  ExcelEMSealLog candidate. Done (33.941s), SEVERE 0, ERROR 0.
  "[SealAlerts] READY version=0.1.3 taps=[ExcelEMSealLog]";
  sealalerts status "Watching: [ExcelEMSealLog], open fights: []".
  server_started outbox event: not_watching=[ExcelGuildGate,
  ExcelBossTrial] - no SealLog name missing. (plugins_ok=false in
  every rig boot incl. the C1 control: GuildGate/BossTrial are not
  in the rig; on live all three exist and the packet's proof-owed
  checks the first real event for plugins_ok=true.)
- D2 (boot-D2-sealalerts013-oldseallog.log): NEW SealAlerts 0.1.3 +
  OLD-name ExcelSealLog. READY version=0.1.3 taps=[], "Watching: []",
  not_watching=[ExcelEMSealLog, ExcelGuildGate, ExcelBossTrial],
  SEVERE 0. => The new build cannot tap the old name: the SealAlerts
  swap + SealLog rename MUST land in the same window as one atomic
  unit (packet header carries this; controls C2 + D2 prove both
  mismatch directions fail silently).
- Rig restored to the D1 target set afterwards.

### Packet + filing

- packets/INSTALL-PACKET.md rewritten: 5 changes (4 renames +
  SealAlerts swap), ATOMICITY REQUIREMENT section in the header,
  all 5 live-jar gates + absence gates + the untouched-config gate,
  boot-verify lines incl. the three-name SealAlerts READY/status,
  surfaces updated (SealLog website 1/1 now in scope; SealAlerts
  name unchanged so no website change for change 5), D211 resolution
  section replacing the blocker section, Wave-3 TAPPED note for
  GuildGate/BossTrial carried in the packet.
  Final lint: 11,682 B / max line 139 / non-ASCII 0 (recounted after
  the header lint line was filled; header cites the pre-fill count
  11,611 per house convention).
- packets/RECEIPTS-D211.md (5,708 B / sha256 0ff32b1fb89f65d1fc559e6
  ed4ed6cd865b47284c3fa65f07857ebb89104467f) holds the D211 receipts;
  packets/RECEIPTS.md unchanged (abae3d3a...).
- MANIFEST: PENDING pointer stub filed after the TrainingWheels
  D208 stub - heading "## PENDING - ExcelEM rename wave 1: 4 renames
  + ExcelSealAlerts 0.1.3 companion (D163/D211) (Nov 23, 2026)".
- STAGING-RECORD.md: D211 addendum appended (blocker resolved,
  packet state updated).

### Deviation flagged for the parent

The brief said "EXACTLY one functional change: the TAPPED entry and
the matching softdepend entry." The source shows the SealLog name is
a functional reference at a THIRD site (SealParser.accept dispatch,
SealParser.java:76, fed by p.getName() from the tap handler). All
three sites are the same single logical change - SealAlerts' by-name
reference to SealLog - and leaving the third unchanged would have
filed a build that passes every boot check while silently dropping
100% of SealLog fight lines (harness T1/T2 are the counterfactual
proof). I moved all three and documented it here, in
RECEIPTS-D211.md, and in the packet's D211 section. If the parent
wants the literal two-site build instead, it is a one-line revert +
rebuild away - but it should not be installed.
