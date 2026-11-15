# INSTALL PACKET (DRAFT — NOT FILE-READY) — ExcelMenu 3.0.2 (BB-01 Back fixes)

- Unit: BB-01 — ExcelMenu Back fixes, 15 rows (FIX-1 6 + FIX-2 9),
  board-manager run 354. Source split:
  ~/workspace/back-button-audit-20261113/FIXBATCH-SPLIT-R348.md §7.
- Lint: 6600 B / max line 78 / non-ASCII 24
- Status: DRAFT. Do NOT file to MANIFEST.md until the G1 receipt
  owed in RECEIPTS.md R1 lands (Homebase pull or FALLBACK (panel)
  receipt) and the live-jar gate below is filled from it.

## Live-premise receipts (G1)

- Live ExcelMenu jar claim — receipt: OWED. No same-day live pull
  exists (Homebase probes failed 4x on 2026-11-14; panel fallback
  needs a live-browser leg). Full probe record: RECEIPTS.md R1 in
  this packet folder (5552 B / SHA-256
  3f8914e33bf1182322a7a2142f318875085944b6727c732837054daff717a369).
- Live layouts/commands_menu.yml claim — receipt: OWED, same pull.
- Premise the build stands on (UNVERIFIED live): live is the 3.0.1
  lineage the audit read (AUDIT.md line 17). If the owed pull shows
  live has moved, STOP, re-base onto the live tree, re-check the
  15 rows, and re-issue this packet (split §3/§7 stop condition).

## Candidate

- ~/workspace/back-button-wave1-r354/BB-01/ExcelMenu-3.0.2.jar —
  337069 B / SHA-256
  0dacc1364851f716aaefd67aa138b259f2eca34ca6cf51e3aeb14228c72e7359;
  build BB-01/build-tree/build.sh; tests 174/174 (baseline 169);
  rig: local Paper boot clean, SEVERE 0 (RECEIPTS.md R3/R5).
- Staged layout: BB-01/packet/staged-layouts/commands_menu.yml —
  3707 B / SHA-256
  0005e5dcd6e40bfa8c6b6c78c920db1dd1a916c6ca75651452090841175466a9.
  Placed over plugins/ExcelMenu/layouts/commands_menu.yml at
  install (live file kept as .bak). Jar extraction is fill-missing
  only, so the Java commands Back item ships ONLY in this file.

## Live-jar gate (STOP on mismatch)

- Live file plugins/ExcelMenu-3.0.1.jar (expected name per audit
  lineage; CONFIRM at the owed pull) = size/hash FROM THE G1
  RECEIPT — intentionally blank in this draft. The audited-tree
  jar (330214 B / 5bb1c1a3...) in RECEIPTS.md R2 is disk lineage
  and must NOT be used as this gate.

## Scope — what changes (15 rows)

FIX-1 (Back lands on the INVOKING screen):
1. Teleport confirmation gate, Java + Bedrock (2): Cancel item /
   Cancel button and prompt close now return to the invoking menu
   (tracked per player by MenuService; default Server Menu). The
   gate is answered "no" first — a Back tap never teleports.
   Java chest Cancel lore now reads "Back to the menu".
2. Teleport direction choice step 2, Java + Bedrock (2): Back
   returns to the step-1 picker. Java: Back item at slot 4.
   Bedrock: the choice becomes a SimpleForm (a ModalForm holds
   only the two direction buttons) — same labels, same order,
   Back appended last.
3. Childproof confirm, Bedrock (1): Cancel and form close return
   to the player picker; nothing is changed by either.
4. Skills & Perks website follow-on form, Bedrock (1): buttons
   become Close + Back; Back returns to the Skills & Perks form.

FIX-2 (Back lands on the ExcelMenu Server Menu, opened through
MenuService — the /menu opener; the shared FIX-2 contract):
5. Server Commands root, Java (1): layout page 1 gains a fixed
   Back item at slot 49 (ARROW, "Back to Server Menu",
   [openguimenu] server_menu) — ships in the staged layout file.
6. Server Commands root picker, Bedrock (1): the grouped picker
   gains a Back button last (FormModel ServerMenuTarget) for every
   grouped layout that is not server_menu itself.
7. Teleport picker step 1, Java + Bedrock (2): Back appended —
   Bedrock form button last; Java chest reserves its last slot
   for a Back item (head capacity shrinks by one at 54 players).
8. Childproof player picker, Bedrock + Java (2): Bedrock form
   gains Back last; the Java chat list gains a clickable
   "[Back to Server Menu]" line (runs /menu).
9. Skills & Perks bespoke form, Bedrock (1): buttons become
   My mcMMO stats / Skill Trees on the Website / Close / Back.
10. Server Website form, Bedrock (1): buttons become Close + Back.
11. Ask the Guide text box, Bedrock (1): a CustomForm has no
    button surface, so closing the box now re-opens the Server
    Menu (the audit's "closing reopens the parent" Yes pattern).
    Send behaviour unchanged.

What does NOT change: no other screen, no entry/layout besides
commands_menu page 1 chrome, no config, no player data, no
settled wording (all existing labels kept; Back is appended,
never substituted). Server Menu root keeps no Back (absolute
root, N/A rows untouched). Form close stays "cancelled" on every
screen except the two confirm gates and the Ask box, where the
audit rows name close as part of the defect.

## Gates

- Fully gated install (jar + one data-file placement): marker
  discipline with re-checks before Shut Down and before Start;
  BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest
  or the two newest manuals); emptiness list = 0 twice.
- Provenance: candidate hash re-verified at install; live-jar
  gate above checked against the fresh G1 pull.

## Boot verify

- Enable line: "ExcelMenu 3.0.2 enabled" with 83 entries, 2
  registry menus, 12 definition menus (counts from the rig boot;
  re-check live, entry count may differ if live entries moved).
- Verification by script output: grep the boot log for
  "ExcelMenu 3.0.2 enabled" (expect 1); grep -c SEVERE (expect 0
  attributable); after placement, sha256sum the placed
  layouts/commands_menu.yml (expect the staged hash above).
- Guild LOCKED count and seal state untouched (read-only check).
- Marker: BB-01/build-tree/INSTALLED.txt (written on install).

## Same-pass surfaces

- Guide knowledge: no new mechanic to teach (Back buttons are
  self-evident); checked — no Guide edit owed.
- What's New, website + in-game changelog: one line each, drafted
  here — "Every menu now has a Back button — teleport, childproof,
  Skills & Perks, the website page, Ask the Guide and Server
  Commands all lead back where you came from." Filed with install.
- Broadcaster tips: checked — no tip references these screens'
  exits; no change.
- In-game menus check: this unit IS the menus change (above).

## Proof owed after install

- Mirror walk on the PC test-server mirror (owed since the desk
  pass — Homebase route was down; RECEIPTS.md R6): every touched
  flow on Java + Bedrock, Back out level by level; teleport and
  childproof confirms must show the invoking screen, never a
  teleport/childproof side effect.
- Mike's spot taps (batch acceptance end, split §2/§7).
