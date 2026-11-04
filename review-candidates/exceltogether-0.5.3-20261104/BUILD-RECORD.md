# ExcelTogether 0.5.3 — BUILD RECORD

- **Built:** Nov 3, 2026, late evening CST (rig boot 11:59 PM CST), by Iris
  (subagent session for the party-leave fix; commissioned by Mike Nov 3, 2026,
  11:37 PM CST after the Jester_Turtle / GumGumJetPistol incident).
- **Tree:** `~/workspace/party-mesh/build-0.5.3/` — a full copy of build-0.5.2 with
  the delta applied. Base proof first: the pristine 0.5.2 tree rebuilt in
  `/tmp/basecheck-052` to `cb0cb309d81b5d5dd8400688f4ff83f7d815cb7b675fdb679e985c096bb91d2a`
  (171,337 B), byte-identical to the shipped 0.5.2 candidate, suite 826/826.
- **Candidate:** `ExcelTogether-0.5.3.jar`, sha256
  `494073dcabf05ad0637846e21095faea0cef46046f09a7d50d0987f19f43f9f5`, 176,042 B.
  build.sh assembles the jar twice and hash-asserts; a second full build.sh run
  reproduced the identical hash.
- **Unit suite:** 890 checks, 0 failures (828 inherited — EmSyncTest's B9b block
  rewritten to the 0.5.3 verdict, net +2 — plus LeaveSticksTest 33 and
  AdminCommandTest 29, both new). Full counts in TEST-RESULTS.txt.
- **Order-sensitivity hardening:** EmSyncTest failed intermittently (1 run in 3)
  during the build — a real race in the fix's first cut (converge's addOne funnel
  could resurrect a door-leaver before their own reconcile). Fixed by guarding the
  funnel; after the fix: EmSyncTest 8/8 runs green, LeaveSticksTest 8/8,
  AdminCommandTest 3/3, and a 40-iteration B9b probe 40/40.
- **Rig:** Paper 26.3-235 local rig (`rig/`, PartyAdminRig driver): all 6 verdicts
  PASS, awake line reads `ExcelTogether 0.5.3 enabled: …`, console replies verbatim
  in `rig/boot-admin-console.log`, SEVERE 0, no WARN/ERROR attributable to
  ExcelTogether. The rig has no mcMMO/EliteMobs, so the mesh legs were OFF there —
  mesh semantics are proven by the unit fakes; the rig proves the real Bukkit
  command path (console sender accepted for admin, "Players only." retained for
  non-admin console use).
- **Status at close-out:** BUILT + TESTED + PACKETED. NOT installed — the install
  is the failsafe's job via the PENDING entry in
  `~/workspace/wrv-install-queue/MANIFEST.md` (packet: INSTALL-PACKET.md; live gate
  = the live 0.5.0 jar at `f4490814…`, since neither 0.5.1, 0.5.2 nor 0.5.3 is live
  yet; this packet supersedes the 0.5.2 packet).
