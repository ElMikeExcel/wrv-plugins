# INSTALL PACKET - ExcelProtect 0.1.12 (P-PROTECT-EM-1 fix)
Board supervisor run 432, filing prep Nov 16, 2026.
Desk prep only - the FAILSAFE installs, never this worker.
Lint: 8010 B / max line 78 / non-ASCII 0

## Item
P-PROTECT-EM-1 fix: ExcelProtect 0.1.11 EM block-attempt
recorder is blind live because EliteMobs 10.9.14 stops
break/place at BlockDamageEvent / BlockCanBuildEvent
(LOWEST), so the terminal events Protect 0.1.11 listens
to at MONITOR never fire in protected EM worlds.
Diagnosis: ~/workspace/board-r432/protect-em-fix/DIAGNOSIS.md
Build: ~/workspace/board-r432/protect-build/BUILD-RECORD.md
Rig: ~/workspace/board-r432/protect-rig/RIG.md (RIG PASS;
instance leg NOTRUN on mirror, rides live re-proof).

## Packet
- Packet: ~/workspace/board-r432/protect-filing/INSTALL-PACKET.md
- Receipts: ~/workspace/board-r432/protect-filing/RECEIPTS.md
  (3,776 B / c8bc1fcac48191f71357da9b479cf70b4fd51c45e285a63d9b4063cc9d13f518;
  G1 PULL-01..03, run 432, Homebase, no fallback)
- Marker on install:
  ~/workspace/board-r432/protect-filing/INSTALLED.txt

## Live-premise receipts (G1 - RECEIPTS.md in this folder)
Fresh Homebase pulls Nov 16, 2026, 12:03 CST. Probe PASS
attempt 1 (DESKTOP-IL05DQR). STOP on any mismatch at
window time; the failsafe re-pulls fresh at its gate.
- Live jar claim: plugins/ExcelProtect-0.1.11.jar exists
  live at 0.1.11 - receipt PULL-01: 303,587 B /
  ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f,
  pulled 2026-11-16 12:03 CST, Homebase/rclone.
  Match vs expected premise: YES (EXACT).
- Live config claim: plugins/ExcelProtect/config.yml
  log.attempts.enabled is true - receipt PULL-02:
  16,002 B /
  c17204010170fe09d67bfeea348e89f50da071e3526bc834ee1897af69c98a9b,
  pulled 2026-11-16 12:03 CST; grep: enabled: true
  (line 112), coalesce-ms 2000, world-prefix "em_",
  worlds []. Same hash as candidate config - no
  config change is part of this packet.
- Live census claim: exactly one active ExcelProtect
  jar live - receipt PULL-03: rclone lsf plugins/
  2026-11-16 12:03 CST; ExcelProtect-0.1.11.jar is the
  sole loadable jar; 11 inert .held/.bak copies + the
  ExcelProtect/ folder; active-jar count = 1.

## Candidate
- ~/workspace/board-r432/protect-build/ExcelProtect-0.1.12.jar
  - 304,634 B / SHA-256
  6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b;
  build EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12;
  tests 125/125; double-build byte-identical;
  fingerprint gate PASS.
- Source zip (records leg, at install):
  ~/workspace/board-r432/protect-build/excelprotect-0.1.12-source.zip
  - 214,914 B /
  e392691c21de5b2b468af0f875923d711c5021180e7b75228f60214f244de7d2.
- Rig summary: mirror, real EliteMobs 10.9.14 as the
  canceller, passive observer only - guild BREAK via
  damage + PLACE via can_build PASS, Primis both PASS,
  per-player PASS, no-double-count PASS, toggle OFF
  PASS, overworld control PASS; instance NOTRUN (see
  Proof owed). SEVERE 0 all boots.

## Live-jar gate (STOP on mismatch)
- Live file plugins/ExcelProtect-0.1.11.jar must equal
  303,587 B /
  ba10bbcd84de50f199f78b1e09d354ee189f8919ba9732d654ed2a4bcc94260f
  at window time (fresh pull; receipt PULL-01 above,
  never lineage). Any mismatch = STOP, re-base, no swap.

## Scope
- CHANGES: one gated jar swap only -
  plugins/ExcelProtect-0.1.11.jar -> ExcelProtect-0.1.12.jar.
  The outgoing 0.1.11 jar is held aside as
  ExcelProtect-0.1.11.jar.bak-<date>-pre0112 (inert name,
  never co-resident as a loadable jar: at no point may
  two ExcelProtect-*.jar loadable files sit in plugins/).
- NO CHANGE: config.yml (live = candidate, hash above),
  all ExcelProtect data/streams, every other plugin,
  worlds, player data. No new config keys, no new
  dependencies (new handlers are Bukkit/Paper core).

## Gates (failsafe discipline - not waivable)
- Marker discipline: WINDOW-IN-PROGRESS at window start,
  deleted at finish; marker re-checked immediately
  before Shut Down and again before Start; a foreign
  marker under 3 hours old = coordination abort.
- BACKUP FIRST + cap-at-6: one fresh backup when the
  window opens; prune oldest manual backups to keep
  total <= 6, prunees named in the install report;
  never delete the Daily Backup newest copy or the two
  newest manual restore points.
- Emptiness: list = 0/10 twice before the swap, and
  again before Start per failsafe discipline.
- Hash gates: live-jar gate above (fresh pull) +
  placed-jar pull-back must equal the candidate hash.

## Boot verify (by script output - see Verification)
- Enabling line x1: Enabling ExcelProtect v0.1.12
- Attempts line x1: attempts: ON (damage/can-build +
  terminal backstops wording per 0.1.12 boot line)
- Version line x1: READY version=0.1.12
- SEVERE 0 attributable in the boot log.
- Guild state unchanged (OPEN 5 of 5 per last record;
  script-checked at window, not assumed here).

## Verification by script output
Every line names the command/script and expected output;
prose descriptions do not count as verification.
- sha256sum placed plugins/ExcelProtect-0.1.12.jar
  == 6e3cf398...51e4b (candidate); size 304,634.
- sha256sum held-aside .bak-<date>-pre0112
  == ba10bbcd...260f (outgoing 0.1.11); size 303,587.
- plugins listing count: exactly 1 loadable
  ExcelProtect-*.jar (the 0.1.12 jar) after the swap.
- Boot log grep counts: Enabling ExcelProtect v0.1.12
  x1; READY version=0.1.12 x1; attempts ON line x1;
  SEVERE count = 0.
- Attempts-record smoke (script-read of the attempts
  stream after the live re-proof break attempt below):
  exactly ONE new block_attempt record for the tester,
  action=break, via=damage, world=em_adventurers_guild,
  at the attempt position; zero block_change there.
- Config unchanged: sha256sum plugins/ExcelProtect/
  config.yml == c1720401...a9b after install.

## Proof owed after install (live re-proof, DIAGNOSIS 6.4)
Discharged only by the tester's own in-game report /
record read-back, in the install restart window:
- Guild: break + place in em_adventurers_guild
  (alcove ~(214, 87, 230)) - one attempt record each,
  break via=damage, place via=can_build.
- Primis: break + place in em_primis - one record each.
- Instance: break + place inside a real em_id_*
  instance (the mirror NOTRUN leg) - records persist
  after the instance world is deleted.
- Overworld controls stay silent (no attempts records
  with world=world).
- Attempts-toggle flip: log.attempts.enabled off in
  the same window's second restart only if the
  supervisor schedules it (the flip needs a restart -
  no reload command exists); otherwise the mirror
  Tier-3 OFF result stands and the live toggle flip
  remains owed. Do not improvise a second live restart.
Success = live attempt records for all three EM world
classes + controls silent; 0.1.12 is then Proven,
not merely Installed.

## Rollback
- Restore the held-aside 0.1.11 jar: remove the 0.1.12
  jar, rename ExcelProtect-0.1.11.jar.bak-<date>-pre0112
  back to plugins/ExcelProtect-0.1.11.jar, restart under
  the same gates; verify pull-back hash ba10bbcd...260f,
  READY version=0.1.11 x1, SEVERE 0. Config needs no
  rollback (unchanged by this packet).

## Same-pass surfaces
- Surfaces staging is a SIBLING LEG this run (not in
  this packet): Guide knowledge/intents, website pair,
  What's New BOTH surfaces (website + in-game
  changelog), broadcaster tips check, in-game menus
  check - each done in the install pass or explicitly
  named as owed in the item state at install time.
- Records publication owed at install (close-out):
  wrv-plugins draft record PR (stays OPEN DRAFT, never
  merged) + Drive deposit of jar + source zip.
  Publication claim line required per repo (G2):
  duplicate check ~/workspace/workflow-tools/pr-exists.sh
  runs BEFORE any push, at publication time; if an
  existing PR is found, the close-out verifies it
  instead of pushing a new one.
- Baseline citations: none - every live hash above is
  a same-day G1 pull receipt, not a baseline hash.

VERDICT: PACKET READY
