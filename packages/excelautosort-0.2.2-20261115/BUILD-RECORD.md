# BB-04 RESULT — ExcelAutoSort Back fixes + relabels (run 354)

Unit: BB-04, 12 rows (FIX-1 2 / FIX-2 4 / FIX-3 6), per
FIXBATCH-SPLIT-R348.md section 3 G1, BB-04 row, section 6.
Status: BUILD COMPLETE + locally rig-verified. Packet is a
DRAFT — G1 live-premise receipt OPEN (blocker below), so the
packet is not file-ready and MANIFEST.md was not written
(per brief). Filing also waits for BB-01 (split section 6).

## G1 live-premise receipt — BLOCKED

- Homebase probes: 8 attempts, all failed, exit 255,
  "kex_exchange_identification: read: Connection reset by peer /
  Connection reset by 198.19.0.1 port 3128" — round 1 (retry
  pattern, 3 attempts over ~2 min) 16:51:04 / 16:51:53 /
  16:52:42 CST; round 2 16:54:05; round 3 17:03:01 / 17:03:42 /
  17:04:22; final 17:06:25 CST, Sat Nov 14, 2026.
- This is a relay-path failure; the PC's own state is
  unknowable while the probes fail (Nov 11 alert-clarity rule).
  Last successful Homebase probe on record: ~4:40 PM CST
  Nov 14, 2026 (board-manager run 353).
- FALLBACK (panel) receipt: not taken — the panel route needs
  a live-browser session this desk worker cannot operate.
  Parent/supervisor lane must take the labeled FALLBACK
  (panel) receipt (panel download of the live
  ExcelAutoSort jar + local SHA-256) or re-run the Homebase
  pull when the relay recovers, before filing.
- Lineage only (NOT G1 evidence): build-0.2.1/INSTALLED.txt
  records live = ExcelAutoSort-0.2.1.jar, 159,240 B / SHA-256
  48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266.
  The 3 edited GUI files are byte-identical between the
  audited src/ tree and the build-0.2.1 tree (diff -rq).

## Candidate (structured evidence)

- Jar: ~/workspace/back-button-wave1-r354/BB-04/packet/ExcelAutoSort-0.2.2.jar
  — 160,197 B / SHA-256 8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28
- Build tree: ~/workspace/back-button-wave1-r354/BB-04/build/ExcelAutoSort-0.2.2/
  (base: autosort-plugin/build-0.2.1; 8 files differ — see
  packet/RECEIPTS.md)
- Build ID: EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2
- Tests: JUnit 35/35 pass. Double-build byte-identical.
  Fingerprint gate PASS.
- Source zip: packet/ExcelAutoSort-0.2.2-source.zip —
  100,207 B / SHA-256 c2ff6c453264c810e5fc340ade2fac0a32a940f30b958fa0427565733ca288fe
- Version test updated 0.2.1 -> 0.2.2 (the one test failure on
  the first build run; fixed and rebuilt green).

## What changed (per row)

- FIX-1 Java recovery: slot 53 is now Back (was Done) — to the
  Selector when opened from it (new Holder.returnSession,
  clickSelector 52 passes the session), else to the Server
  Menu. FIX-1 Bedrock recovery: Back button before Done — to
  the main form when opened from it, else to the Server Menu.
- FIX-2: Java Selector slot 49 Back + Bedrock main-form Back
  button; Java MaxOrg Unlock slot 45 Back + Bedrock unlock
  form Back action. All land on the ExcelMenu Server Menu via
  performCommand("menu") (the opener ExcelShops already uses),
  with a "menu could not open" fallback line. No ExcelMenu
  files edited. Done buttons kept alongside Back.
- FIX-2 support change: Bedrock main form gains the Recovery
  box button (with waiting count) that the Java Selector
  already had — without it the Bedrock FIX-1 entry path does
  not exist in-form. Main-form button indices after index 1
  shift (2 recovery, 3 Back, 4 Done).
- FIX-3 (6 rows, handlers unchanged): config confirm-not-yet
  "Not yet" -> "Back" covers Selector confirm (Java slot 23,
  Bedrock modal button 2) and MaxOrg purchase confirm (Java
  slot 15, Bedrock modal button 2). MaxOrg Network Java
  slot 46 and Bedrock network button relabelled from
  "Max Organization" to "Back" via the new back-button key
  (the shared maxorg-info-title key still titles the info
  papers on the Unlock/Confirm screens — untouched).
- Config delta for install: confirm-not-yet -> "&eBack"; add
  back-button: "&eBack"; add menu-open-failed line. Merge
  only; no other config/data change.

## Mirror walk

- Code-trace walk, scripted: BB-04/rig/walk-check.sh ->
  BB-04/rig/WALK-OUTPUT.txt — WALK-TOTAL pass=25 fail=0
  (25 render/handler/landing anchors covering all 12 rows).
- Local Paper rig (BB-04/rig/server, boot.log): candidate
  enables; awake line verbatim with the 0.2.2 build ID;
  Protect sink BOUND; Done (86.512s); SEVERE 0. Console
  smoke unusable on this rig (Paper-internal NPE in
  net.minecraft.commands, no plugin frames — in RECEIPTS.md).
- NOT run: live mirror walk on the Homebase test-server
  mirror (Java + Bedrock clients) — Homebase relay down all
  run. Packet names it + Mike's spot taps as proof owed.

## Packet draft

- ~/workspace/back-button-wave1-r354/BB-04/packet/INSTALL-PACKET.md
  — lint 3680 B / max line 88 / non-ASCII 10; SHA-256
  aa424a619a44e9e8485fc9f05ae22709885f000b1c6fbc08e7a9e99a4fdd7677
- Receipts: packet/RECEIPTS.md; sums: packet/SHA256SUMS.
- MANIFEST.md: NOT written (per brief). No installs, no
  purchases, no sends, no deletions performed.

## Blockers (exact)

1. G1 receipt open: Homebase relay down (8 failed probes,
   evidence above); panel fallback requires a live-browser
   lane this worker does not have. Unblocks the moment either
   route produces the live-jar pull (path/size/SHA-256/time).
2. Live mirror walk not run (same outage); local rig + code
   walk stand in, packet keeps the proof owed.
3. Filing waits for BB-01's packet (split section 6) — by
   design, not a defect.
