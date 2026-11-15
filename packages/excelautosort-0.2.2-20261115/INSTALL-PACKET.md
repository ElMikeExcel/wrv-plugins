# INSTALL PACKET (DRAFT — G1 gate open, do not file yet)

## PENDING — ExcelAutoSort 0.2.2 (BB-04 back-button fixes)
- Lint: 3680 B / max line 88 / non-ASCII 10
- Packet: ~/workspace/back-button-wave1-r354/BB-04/packet/INSTALL-PACKET.md
- Receipts: ~/workspace/back-button-wave1-r354/BB-04/packet/RECEIPTS.md

## Live-premise receipts (G1)
- Live jar claim: plugins/ExcelAutoSort-0.2.1.jar is live.
  Receipt: OPEN — Homebase relay down all run (7 probe attempts,
  16:51–17:04 CST Nov 14, 2026, all connection-reset at the
  relay); FALLBACK (panel) receipt or a fresh Homebase pull is
  required before filing. Full evidence in RECEIPTS.md.
- Live-jar gate (STOP on mismatch): the pulled live jar must
  match the lineage in RECEIPTS.md (159,240 B / 48a5c0fe…) or
  the unit re-bases before filing.

## Candidate
- ~/workspace/back-button-wave1-r354/BB-04/packet/ExcelAutoSort-0.2.2.jar
  — 160,197 B / SHA-256 8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28
- Build EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2; tests 35/35;
  double-build byte-identical; fingerprint gate PASS.
- Local Paper rig: enables, awake line exact, Done, SEVERE 0
  (RECEIPTS.md). Mirror walk (code-trace): 25/25 anchors PASS
  over the 12 rows (BB-04/rig/WALK-OUTPUT.txt).

## Scope
- 12 back-button audit rows (BB-04): FIX-1 filter recovery box
  Java + Bedrock (Back to the Selector / main form when opened
  from it; to the Server Menu when opened standalone); FIX-2
  chest-filter Selector (Java slot 49) + main form (Bedrock),
  Max Organization Unlock Java (slot 45) + Bedrock form — Back
  lands on the ExcelMenu Server Menu via performCommand("menu"),
  the opener ExcelShops already uses; no ExcelMenu files edited.
- FIX-3 relabels (6 rows): config key confirm-not-yet
  "Not yet" -> "Back" (Selector confirm Java + Bedrock modal,
  MaxOrg purchase confirm Java + Bedrock modal — handlers
  unchanged); MaxOrg Network Java slot 46 and Bedrock network
  button relabelled "Max Organization" -> "Back" (handlers
  unchanged, still open the Unlock screen).
- Bedrock main form also gains the Recovery box button the Java
  Selector already had (needed so the Bedrock FIX-1 entry path
  exists in-form); button indices shift after index 1.
- Config: merge ONLY these keys into the live config.yml —
  confirm-not-yet -> "&eBack"; add back-button: "&eBack";
  add menu-open-failed line. No other config/data change;
  player data, filters, buffers and entitlements untouched.
- Jar swap only: ExcelAutoSort-0.2.1.jar -> ExcelAutoSort-0.2.2.jar.

## Gates
- Marker discipline with re-checks before Shut Down and before
  Start; BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness list = 0/10 twice.
- Filing waits for BB-01's packet to exist (Wave-1 order, split
  section 6) and for the G1 receipt above.

## Boot verify
- "Enabling ExcelAutoSort v0.2.2"; awake line naming build
  EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2; Protect audit sink
  BOUND; guild LOCKED 4 of 5; SEVERE 0 attributable.
- Marker: ~/workspace/back-button-wave1-r354/BB-04/packet/INSTALLED.txt

## Same-pass surfaces
- Owed at filing: Guide knowledge (chest filter / Max Org Back
  behaviour), What's New BOTH surfaces (website + in-game
  changelog), tips check, menus check. None staged in this unit.

## Proof owed after install
- Mike's spot taps (backlog acceptance): Java + Bedrock —
  Selector Back to Server Menu; recovery Back from both entry
  paths; Unlock Back; both confirm screens read "Back";
  Network "Back" returns to Unlock. Plus a live mirror walk if
  the supervisor requires it before filing.
