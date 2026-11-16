# INSTALL PACKET - ExcelDowned 0.1.5 (self-check timing fix)
# P-DOWNED-REVIVE-1 Defect-2 template leg, Nov 16, 2026
- Lint: 12971 B / max line 60 / non-ASCII 0
- Status: FILING-READY draft for the supervisor
  (run 427 W14; W13 build + rig record:
  board-r427/downed-015-build/REPORT.md; fresh
  G1 receipts this run). NOT filed by the
  worker; the supervisor files the pointer stub
  (MANIFEST-STUB-DRAFT.md beside this packet).
  Diagnosis of record: board-r427/downed-
  template-diagnosis/DIAGNOSIS.md (timing
  verdict) + downed-template-reverify/REPORT.md
  (W6: WARNs survive on 0.1.4, 3 of 3 boots).
- Packet: ~/workspace/board-r427/downed-015-filing/
  INSTALL-PACKET.md (this file).
- Receipts: ~/workspace/board-r427/downed-015-filing/
  RECEIPTS.md - G1-1..G1-4, Homebase route,
  Nov 16, 2026 08:42-08:43 CST, probe PASS
  attempt 1, no fallback. Its SHA-256 is in
  this folder's SHA256SUMS.

## What this install is
ExcelDowned 0.1.5 fixes ONLY the boot spell-
template self-check (Defect-2 template leg).
0.1.4's check probes Magic's spell registry
synchronously in onEnable, ~7 s before Magic
finishes loading its spells, so it WARNs on
every boot that 'resurrection' and
'grouprenewal' do NOT resolve - a false
positive: both templates exist and load, and
post-load probes resolve them (DIAGNOSIS.md).
0.1.5 defers the check (+200 ticks, retries
at +20 s / +40 s, 3 attempts max), adds the
known-good 'heal' control key (control false
= registry not yet loaded = deferred INFO +
retry, never a template WARN), corrects the
WARN wording (stale grant-NPE claim gone),
and keeps fail-loud: a key absent from the
POPULATED registry WARNs exactly once, naming
that key only. Delta vs 0.1.4: ExcelDowned
.java modified + new TemplateCheckPolicy
.java. The stage5 range rider (DIAGNOSIS S5)
is NOT included (separate call).

## Live-premise receipts (G1) - ALL TAKEN
All receipts in RECEIPTS.md (path above),
Homebase route, Nov 16, 2026. Summary:
- G1-1 live jar ExcelDowned-0.1.4.jar =
  59,998 B / 703d7f9a45b4c0d00f49bfddb1f404
  bd5990601ae37e9b60e5f1937599792339 -
  EXACTLY the 0.1.4 candidate of record and
  the 0.1.5 base gate; lsjson ModTime
  2026-11-16T04:49:58-06:00 (0.1.4 window).
- G1-2 plugins census (17,027 B /
  681287ab...f0477): exactly ONE loadable
  ExcelDowned jar live (0.1.4; three inert
  .bak jars + data folder besides).
- G1-3 live config.yml 1,727 B /
  8f3671e7c27b...6156 - byte-identical to the
  0.1.5 build-tree config; swap ignores it.
- G1-4 latest.log (170,957 B / ebe8c522...
  a504; boot Nov 16 07:48 CST): plugin list +
  Loading/Enabling lines prove live v0.1.4,
  ready line build EXC-DOWNED-0.1.4-WRV-
  20261116-01-d0wn3d; BOTH template WARNs
  still present in this current live boot
  (lines 611-612, count 1 each, old wording);
  Magic "Loaded 221 spells" 7 s after
  Downed's enable; guild OPEN 5 of 5; Done
  present; SEVERE 0.
- Same-day Nov 16 evidence; a later-date
  filing or install re-pulls per G1.

## Candidate
- ~/workspace/board-r427/downed-015-build/
  ExcelDowned-0.1.5.jar - 63,701 B / SHA-256
  50d7514934c7f71eaa4840a3024d3cb0594436a6a04
  09c6c7b2cb21e048b9710
  (verified this run vs the build folder's
  REPORT.md pin and SHA256SUMS line).
- Build: EXC-DOWNED-0.1.5-WRV-20261116-01-d0wn3d;
  base gate PROVEN (untouched 0.1.4 tree
  rebuilt byte-identical to the live jar);
  deterministic double-build byte-identical;
  suite 44/44 (39 carried + 5 new
  TemplateCheckPolicy tests).
- Rig (Homebase mirror, real Magic 11.2.5):
  R1 PASS - cold boot, deferred check logged
  'resolves' for BOTH keys, template WARN 0,
  SEVERE 0. R2 PASS - negative control
  (resurrection removed): final WARN exactly
  once, resurrection only, corrected wording;
  grouprenewal resolved; SEVERE 0. R3 PARTIAL
  - ordering reproduced (enable ~5 s before
  Magic's load) but the deferred-INFO branch
  did not fire (first +10 s attempt found the
  populated registry); pinned by unit suite.
  R4 PARTIAL - post-load probes resolve both
  keys, no skipped-key sync WARNING; BUT
  mirror MagicGate is 0.3.1 (live: 0.3.3) and
  no player joined, so the 0.3.3 join-time
  sync proof rides S7 (Proof owed). Record:
  downed-015-build/REPORT.md + rig-evidence/.

## Live-jar gate (STOP on mismatch)
- Live plugins/ExcelDowned-0.1.4.jar must
  still be 59,998 B / 703d7f9a...3979 at the
  window (G1-1), verified by script:
  python3 ~/workspace/wrv-install-queue/tools/
  verify_hash.py <pulled jar> --size 59998
  --sha256 703d7f9a45b4c0d00f49bfddb1f404bd5
  990601ae37e9b60e5f1937599792339
  -> RESULT: PASS.
  Any mismatch = STOP, re-base. Never swap on
  lineage alone.

## Scope
- Changes: ONE jar swap live -
  plugins/ExcelDowned-0.1.4.jar is renamed to
  ExcelDowned-0.1.4.jar.bak-<date>-pre015 and
  plugins/ExcelDowned-0.1.5.jar is placed.
  Then a restart. Behaviour change is confined
  to the boot self-check (timing, control key,
  WARN wording, above).
- Does NOT change: plugins/ExcelDowned/
  config.yml (G1-3; NO config placement), any
  ExcelDowned data (sessions, records), Magic
  spells.yml / icons.yml, any ExcelMagicGate
  file (incl. stage5 range: 16 - the S5 rider
  is NOT in this packet), any other plugin,
  player data, or any world. The revive path
  is untouched: the 0.1.4 provider fix and
  its behaviour carry over unchanged.

## Gates
- Fully GATED window (jar swap): WINDOW-IN-
  PROGRESS marker at start, deleted at finish,
  re-checked immediately before Shut Down and
  again before Start (a foreign marker under
  3 hours old = coordination abort).
- BACKUP FIRST: one fresh Shockbyte backup
  when the window opens, cap-at-6 (prune
  oldest manuals, name prunees; never the
  Daily Backup's newest copy or the two newest
  manual restore points), verified by script:
  python3 ~/workspace/wrv-install-queue/tools/
  backup_cap_check.py <listing capture>
  -> RESULT: PASS (total <= 6); listing
  retained verbatim in the outcome record.
- Emptiness: list = 0 of 10 twice before the
  swap, each capture verified by script:
  python3 ~/workspace/wrv-install-queue/tools/
  emptiness_check.py <list capture>
  -> RESULT: PASS (online=0).

## Swap steps (in the window, in order)
1. Candidate verify: verify_hash.py on the
   build-folder candidate -> size=63701,
   sha256=50d75149...9710, RESULT: PASS. Also
   sha256sum -c on the build folder's
   SHA256SUMS.
2. Fresh gate pull + verify_hash.py on the
   live jar (gate above) -> PASS, or STOP.
3. Rename live ExcelDowned-0.1.4.jar to
   ExcelDowned-0.1.4.jar.bak-<date>-pre015 via
   Homebase rclone (panel File Manager =
   labeled fallback).
4. Place ExcelDowned-0.1.5.jar into plugins/
   via Homebase rclone (same fallback).
5. Pull-back verify: verify_hash.py on a
   fresh pull of the placed jar -> 63,701 B /
   50d75149...9710 PASS; census: exactly ONE
   active ExcelDowned jar live (0.1.5), the
   0.1.4 file present only as .bak.
6. Restart (Shut Down / Start under marker
   discipline).
7. Boot verify (below). On failure the
   rollback governs - do not declare
   Installed on a failed verify.

## Boot verify (script-named; expected)
- Tools: ~/workspace/wrv-install-queue/tools/
  (boot_log_scan.py, live_pull_check.py,
  verify_hash.py, verify_marker.py) over a
  fresh pull of logs/latest.log after restart.
- boot_log_scan.py <latest.log> -> RESULT:
  PASS with done=present, severe=0, guild
  line read as guild OPEN (the scan's LOCKED
  phrasing does not match the OPEN state; the
  lane confirms by grep below - named here so
  nobody scores it as a failure).
- Line 1: "[ExcelDowned] Loading server plugin
  ExcelDowned v0.1.5" (grep -c = 1); v0.1.4
  loading line absent.
- Line 2: "[ExcelDowned] Enabling ExcelDowned
  v0.1.5" (grep -c = 1) + ready line
  "ExcelDowned 0.1.5 ready" naming build
  EXC-DOWNED-0.1.5-WRV-20261116-01-d0wn3d
  (grep -c = 1; sessions count is live state,
  not a gate value).
- Line 3: "custom Magic action 'Resurrect'
  registered." (grep -c >= 2: load + enable).
- Line 4 - THE WARN LEG (this packet's live
  proof): template WARN count = 0 - grep -c
  "does NOT resolve" over [ExcelDowned] lines
  = 0, and grep -c "grants of this spell will
  NPE" = 0 (old wording gone).
- Line 5: BOTH keys resolve - grep -c for
  "spell template 'resurrection' resolves."
  = 1 AND "spell template 'grouprenewal'
  resolves." = 1 (mirror R1 wording, verbatim
  in downed-015-build/rig-evidence/
  EXCERPTS.txt). The resolves lines log at
  the deferred check (~+10 s after enable),
  AFTER Magic's "Loaded N spells" line -
  let the boot settle before grepping. An
  optional INFO "Magic spell registry not
  yet loaded - spell-template self-check
  deferred (attempt N of 3)" may precede
  them; expected shape, not a failure. A
  final WARN naming a key, or a missing
  resolves line for either key, = FAIL:
  report verbatim, WARN leg stays open.
- Line 6: "[ExcelGuildGate] ExcelGuildGate
  enabled: 5 of 5 seals broken, guild OPEN"
  (grep -c = 1); Done line present; SEVERE 0
  (grep -c over the whole log = 0, the G1-4
  baseline shape).
- Census: config.yml still 1,727 B /
  8f3671e7...6156 (verify_hash.py PASS).
- Marker: ~/workspace/board-r427/
  downed-015-filing/INSTALLED.txt, confirmed
  with verify_marker.py.

## Rollback
- Rename plugins/ExcelDowned-0.1.5.jar out
  (held name), rename the .bak-<date>-pre015
  back to ExcelDowned-0.1.4.jar, restart;
  verify_hash.py on the restored jar =
  59,998 B / 703d7f9a...3979. A rollback
  restores the 2 false-positive WARNs with
  it - the known 0.1.4 shape, not a new fault.
- If boot verify fails in a way the jar
  rollback does not explain, restore the
  window backup per standing discipline and
  STOP.

## Same-pass surfaces (owed at install)
The 0.1.4 pattern governs (run 421 jar window
+ run 422 TEXT surfaces entry, MANIFEST-
ARCHIVE.md): surfaces ride the install pass
as their own legs; the jar window does not
wait on them; none may be left as "staged in
a spec". 0.1.5's player-visible change is
small (false boot WARNs gone; revive
behaviour unchanged). Owed set:
- What's New BOTH surfaces: an in-game
  ExcelWhatsNew changelog record for 0.1.5
  (next free Nov 16 seq assigned by the
  surfaces lane at install time; in-game file
  verified by pulling the live file, never
  assumed from the website pass) AND the
  website What's New entry (generated <li>
  via the site pipeline; Homebase git lane;
  G2 duplicate check first, G3 Pages check
  after merge).
- Guide: ExcelGuide knowledge + intents
  CHECKED for the downed/revive topic - if
  any chunk or intent names the boot template
  WARNs or a template/data mismatch as live
  state, correct it in the same pass (0.1.4
  shape: chunk added, intents unchanged).
  Reload-only live-edits, in-place .bak nets,
  filed as their own TEXT entry sequenced
  with this window.
- Website pair: winkreyesvalley What's New /
  changelog pages carry the same 0.1.5 entry;
  plugin-page version references checked.
- Records: wrv-plugins record package for
  0.1.5 (jar + source deposit to Drive; draft
  record PR via Homebase git, G2 check first;
  record PRs stay OPEN DRAFTS, never merged).
- Broadcaster tips check: ExcelTips /
  AutoBroadcaster downed/revive tips checked
  (no change expected; the check is owed, as
  in the 0.1.4 packet).
- In-game menus check: ExcelMenu +
  ExcelBedrockMenu /downed + /giveup entries
  still resolve after the swap (check only).
Each surface is done in the install pass or
explicitly named as still owed in the outcome
record.

## Proof owed after install
- S7 LIVE REVIVE RETEST, PAIRED IN THE SAME
  POST-INSTALL WINDOW: the still-owed S7
  proof from the 0.1.4 packet (S6-SCOPING.md
  section 6 live half, D2-LIVE-1) runs in the
  same player-session window as this
  install's verification - a real revive with
  real clients, discharged only by the
  tester's own report/screenshot. Player
  class: Marvin or Mike (healer/caster + a
  party member who goes down) - NOT Kailey.
  Protocol (0.1.4 packet, unchanged): party
  confirmed in chat before the down; wand
  carries Resurrection, mana >= 100 confirmed
  before the cast; cast from ~10 blocks;
  every caster-visible message verbatim.
  PASS = revive at 50%, revive record
  written, party DOWN / revived alerts fire.
  Run BOTH doors if the session allows (hub
  /excelparty, EM /em party) - the EM arm
  also discharges the mirror N4 carry-over.
- R4 remainder, read off the SAME session: a
  literal ExcelMagicGate 0.3.3 join-time wand
  sync observation (live carries 0.3.3; the
  mirror ran 0.3.1) - the session log shows
  the joining player's sync with no skipped-
  key WARNING for either key.
- The install boot log (Boot verify Lines
  4-5) is the WARN leg's own live proof and
  does NOT discharge S7: 0.1.5 is Installed
  on the boot verify, Proven only when S7
  lands. Until then the revive path itself
  remains unproven live (owed since 0.1.4).
- Marker on install: ~/workspace/board-r427/
  downed-015-filing/INSTALLED.txt
