# INSTALL PACKET - ExcelDowned 0.1.4 (P-DOWNED-PROVIDER-1)

Lint: 9,501 B / max line 71 / non-ASCII 0
Packet status: FILING-READY (prep by board supervisor run 421,
worker W10, Mon Nov 16, 2026). The supervisor files the MANIFEST
PENDING pointer stub from this packet; this worker filed
nothing and installed nothing.
Decision of record: D84 (decision-log.md) - file 0.1.4 as built
(provider fix only); the perform-hardening defects named in
the CastRig v8 SESSION.md section 5.3 (world-before-distance
ordering in the perform path; missing conjunct logging) do
NOT fold into this packet and ride a later hardening item.

## PENDING - ExcelDowned 0.1.4 (date added: <supervisor fills>)
- Packet: ~/workspace/board-r421/downed-filing/INSTALL-PACKET.md
- Receipts: ~/workspace/board-r421/downed-filing/RECEIPTS.md
  (5,317 B / 5dce07f9f910f687813c2fe4ff52a71ef012215137
  04e62ba0f77b36f94628c4) - all G1 receipts live there.

## Live-premise receipts (G1, all pulled Nov 16, 2026)
Homebase probe PASS attempt 1, 04:35:40 CST (DESKTOP-IL05DQR,
agent 0.2.4); pulls 04:36-04:37 CST via rclone on the PC,
PC hash = local hash on every file; no fallback used.
Full lines in RECEIPTS.md; the claims this packet makes:
- Live active Downed jar is `plugins/ExcelDowned-0.1.3.jar`,
  59,875 B / 31f278bd...93 (RECEIPTS.md receipt 1) - EXACT vs
  the run-417 base gate expectation.
- Live active Together jar is `plugins/ExcelTogether-0.5.6.jar`,
  179,161 B / 1f32f0f7...f4 (receipt 2) - the D80 rollback
  state; the 0.6.0 jar sits held as
  `ExcelTogether-0.6.0.jar.held-20261116-d80rollback`.
- Live `plugins/ExcelDowned/config.yml` is 1,727 B /
  8f3671e7...56 (receipt 3), byte-identical to the candidate
  tree config (diff empty).
- Live `plugins/ExcelGuildGate/state.yml` is 1,080 B /
  8a3bdc65...17 (receipt 4): unlocked: true, 5 of 5 seals
  broken - guild OPEN 5/5.

## Candidate
- Artifact: ~/workspace/board-r417/downed-014/ExcelDowned-0.1.4.jar
- Size: 59,998 B
- SHA-256: 703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60
  e5f1937599792339 (re-verified by this worker Nov 16, 2026)
- Build: EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d
- Build record: ~/workspace/board-r417/downed-014/BUILD-RECORD.md
  (base gate: unmodified 0.1.3 tree rebuilt byte-identical to
  the live jar above; deterministic double-build; suite
  39/39 - 34 carried + 5 new field-shape provider tests;
  negative control: pristine 0.1.3 provider fails exactly the
  3 real-shape tests, 36 pass / 3 fail)
- Fix: ONE production file, `TogetherPartyProvider.java` -
  `membersOf()` reads the `members` FIELD first (public
  getField, then declared-field + setAccessible), keeping the
  method-name list as fallback; fail-soft unchanged,
  UNKNOWN-on-empty NOT loosened. Patch:
  ~/workspace/board-r417/downed-014/TogetherPartyProvider.patch
- Mirror gate: CLEARED. Geometry-true confirmation session
  (run 421 W9, ~/workspace/board-r421/downed-rig-v8/CONFIRM.md):
  caster held at distance 10.0 in-step - M1 provider probe
  PASS (partyMembersOf non-empty, membershipOf = PARTY),
  M2 CAST-1 PASS (wand cast fires, A revived at 50%, revive
  record written, cooldown standing), M3 negative PASS
  (outsider at 10.0 refused on membership alone), perform
  CAST on both routes + fresh handle; tally 37 pass / 4 fail,
  all 4 fails off the cast chain. The earlier Session 2 M2
  FAIL is closed as a rig-geometry fault, not a code fault.
  Mirror pairing note + the N4 EM-door carry-over are under
  Proof owed / Pairing below.

## Live-jar gate (STOP on mismatch)
- At the window, re-pull fresh and require:
  `plugins/ExcelDowned-0.1.3.jar` = 59,875 B /
  31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc
  92544d9e93. Any mismatch stops the swap.
- Pairing check at the window: active Together jar still
  `ExcelTogether-0.5.6.jar` = 179,161 B /
  1f32f0f7bbad69bfd74f496a490b32832112390209b7a476951cc
  07bfd5c16f4. If Together has moved (e.g. 0.6.0 un-held),
  STOP and escalate to the supervisor - the packet was
  premised on the 0.5.6 pairing (see Pairing note in
  RECEIPTS.md; the fix itself is version-independent).

## Scope
- ONE jar swap only: rename the live jar to
  `ExcelDowned-0.1.3.jar.bak-<window date>-pre014` and place
  `plugins/ExcelDowned-0.1.4.jar` (candidate hash above,
  re-hashed fresh at the provenance gate; pull-back must
  hash EXACT to the candidate).
- Config/data UNTOUCHED - verified, not assumed: the build
  record's `diff -rq` shows only `TogetherPartyProvider.java`
  + `BuildInfo.java` differ from the 0.1.3 tree, tree
  config.yml is identical to the 0.1.3 tree config, and the
  live config pulled today is byte-identical to the tree
  config (RECEIPTS.md receipt 3). No config file is placed,
  no `plugins/ExcelDowned/` data (sessions, records) is
  touched, no spells.yml change, no other plugin changes.
- NOT in scope: ExcelSummons' byte-identical defective
  `membersOf()` (S6 section 5.4 - its own ticket/packet,
  ExcelSummons 0.1.14 line); the section 5.3 perform
  hardening (D84); Together 0.6.0 (HELD per D80).

## Gates (installer / failsafe)
- Fully gated jar swap - NOT a text live-edit exemption.
- Marker discipline: WINDOW-IN-PROGRESS.txt at window start,
  deleted at finish; re-check the marker immediately before
  Shut Down and again before Start.
- BACKUP FIRST + cap-at-6: one fresh Shockbyte backup for the
  window; prune oldest manual backups, name prunees; never
  delete the Daily Backup's newest copy or the two newest
  manual restore points.
- Emptiness gate: `list` = 0/10 twice before the swap.

## Boot verify (script-shaped; boot log greps)
- grep "Enabling ExcelDowned v0.1.4" - present exactly once.
- grep "ExcelDowned 0.1.4 ready" AND the fingerprint
  "EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d" - present once
  (the ready line also reports sessions restored, bleed-out
  300s, downed factor 0.25 - values unchanged from 0.1.3).
- grep "custom Magic action 'Resurrect' registered." -
  present (expected twice: load + enable).
- Self-check lines: one per key (resurrection, grouprenewal):
  either "resolves at boot" or a WARNING naming the key. A
  WARNING = the grant-side template mismatch is still live
  for that key (it WARNed on the 0.1.3 install and on the
  mirror): report it verbatim in the install report; do not
  call the grant verified. The mirror's post-load probe
  showed the resurrection template resolving after deferred
  load - that is the expected live shape too.
- grep "ExcelTogether party adapter linked (PartyManager)" -
  expected at the first party probe after boot (the line the
  fixed provider produces when it links); its absence after a
  party forms is a fail signal for the S7 session, not a boot
  blocker.
- SEVERE count attributable to ExcelDowned: 0.
- Guild state unchanged: state.yml still unlocked: true,
  5 of 5 seals broken (compare against receipt 4's hash if
  the file is re-pulled at the window).
- Marker on install: ~/workspace/board-r421/downed-filing/INSTALLED.txt

## Same-pass surfaces (owed at the install pass, staged then)
Revive behaviour is player-facing (party Resurrection revive
works again; party DOWN/revived alerts fire again). The
install pass must stage, in the same pass:
- Guide: knowledge chunk + intent check for the downed/revive
  topic - state that a party member's Resurrection revive
  works as designed (it silently failed before this fix);
  intent added only if the revive question is not already
  routed - check, then stage what is missing.
- What's New BOTH surfaces: a website What's New entry AND
  an in-game ExcelWhatsNew changelog entry for
  ExcelDowned 0.1.4 (registry record + next free Nov 16 seq
  assigned by the surfaces lane at install time).
- Website pair: the winkreyesvalley What's New / changelog
  pages updated with the same entry (pair with the in-game
  surface above).
- Broadcaster tips check: check ExcelTips / broadcaster tips
  for any downed/revive wording - no tip change expected;
  the check itself is owed and named here.
- In-game menus check: no menu definitions change in this
  packet; verify the /downed + /giveup menu entries still
  resolve after the swap (check only).

## Proof owed after install
- S7 LIVE RETEST (S6-SCOPING.md section 6 live half,
  D2-LIVE-1) - a real revive in game with real clients,
  discharged only by the tester's own report: named tester
  class Marvin or Mike (a healer/caster + a party member who
  goes down). Protocol: ExcelTogether party confirmed in chat
  before the down; caster's wand carries Resurrection and
  mana >= 100 confirmed before the cast; cast from ~10
  blocks; every caster-visible message reported verbatim.
  PASS = revive lands at 50%, revive record written, AND the
  party DOWN / revived alert notices fire (the alertParty
  consumer of the same fix). Run BOTH doors if the session
  allows (one hub-door revive via /excelparty, one EM-door
  revive via /em party) - the EM-door arm also discharges the
  mirror N4 carry-over, which the confirmation session did
  not run. If the revive fails while the provider reads
  PARTY, follow the S6 section 6 outcome table (check the
  installed jar hash first; then re-enter scoping at the
  downstream gate) - do not re-open the probe question.
- No other proof is owed by this packet.

*End of packet - run 421 W10 prep. FILING-READY for the
supervisor to file as a MANIFEST PENDING pointer stub
(heading, this packet's path, candidate line, live-jar gate
line, one-sentence scope, marker path, proof-owed line).*
