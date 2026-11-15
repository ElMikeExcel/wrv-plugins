# INSTALL PACKET (DRAFT — G1 gate open, do not file yet)

## PENDING — ExcelAutoSort 0.2.3 (P-AUTOSORT-FILTER-1 filter toggle)
- Lint: 4319 B / max line 88 / non-ASCII 14
- Packet: ~/workspace/board-r362/autosort-filter/INSTALL-PACKET.md
- Receipts: ~/workspace/board-r362/autosort-filter/packet/RECEIPTS.md

## Live-premise receipts (G1)
- Live jar claim: plugins/ExcelAutoSort-0.2.1.jar is live.
  Receipt: OPEN — Homebase relay down this run (3 probes,
  22:10–22:11 CST Nov 14, 2026, all connection-reset at the
  relay); a fresh Homebase pull or a labeled FALLBACK (panel)
  receipt is required before filing. Evidence in RECEIPTS.md.
- Live menu-entry claim: plugins/ExcelMenu/entries/autosort.yml
  is the Nov 12, 2026 toggles-split file (group toggles).
  Receipt: OPEN — same pull as above must confirm it.
- Live-jar gate (STOP on mismatch): the pulled live jar must
  match the lineage in RECEIPTS.md (159,240 B / 48a5c0fe…)
  or the unit re-bases before filing.
- ORDER: this candidate is built on the BB-04 0.2.2 tree and
  contains every BB-04 fix. Install it AFTER BB-04 (0.2.2), or
  instead of it with the supervisor's explicit re-base — never
  before it (a later 0.2.2 install would erase the toggle).

## Candidate
- ~/workspace/board-r362/autosort-filter/packet/ExcelAutoSort-0.2.3.jar
  — 161,791 B / SHA-256 0457557e15c3de52033be300bc7f5bb32991268f3005544d7ec0fe15965b130b
- Build EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07; tests 39/39;
  double-build byte-identical; fingerprint gate PASS.
- Local Paper rig: enables, awake line exact, Done, SEVERE 0
  (RECEIPTS.md). Mirror walk (code-trace): 20/20 anchors PASS
  (~/workspace/board-r362/autosort-filter/WALK-OUTPUT.txt).

## Scope
- (1) A direct Filter ON/OFF toggle button inside the Chest
  filter dialog itself: Java selector slot 3 (LEVER, beside the
  mode row) and the Bedrock main form's first button. OFF
  applies at once and persists, list kept; ON re-arms in the
  mode the chest last filtered with (WHITELIST if it never
  has) through the existing §6.1 confirm gate — the itemized
  reject list and recovery sweep are unchanged. The mode
  buttons / list form, the mode state line, and all mode/list
  behavior are preserved.
- (2) Menu move: plugins/ExcelMenu/entries/autosort.yml is
  replaced by the staged entry (build tree menu/autosort.yml,
  2,381 B / d5795938…): group toggles -> chest_tools, order
  25, order-bedrock 245 — appended after the existing Chest
  Tools doors, nothing renumbered. Every other line of the
  entry is byte-identical to the live Nov 12 file. Applied
  with /excelmenu reload in the same window.
- Config: merge ONLY these keys into the live config.yml —
  messages.filter-toggle-on, messages.filter-toggle-off,
  messages.form-filter-toggle-on, messages.form-filter-toggle-off.
  No other config/data change; player data, filters, buffers
  and entitlements untouched.
- Jar swap only: ExcelAutoSort-0.2.1.jar -> ExcelAutoSort-0.2.3.jar
  (or 0.2.2 -> 0.2.3 when BB-04 has already landed; see ORDER).

## Gates
- Marker discipline with re-checks before Shut Down and before
  Start; BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness list = 0/10 twice.
- Filing waits for the G1 receipts above.

## Boot verify
- "Enabling ExcelAutoSort v0.2.3"; awake line naming build
  EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07; Protect audit sink
  BOUND; /excelmenu reload reports the registry with 0 failed;
  guild LOCKED 4 of 5; SEVERE 0 attributable.
- Marker: ~/workspace/board-r362/autosort-filter/packet/INSTALLED.txt

## Same-pass surfaces
- Owed at filing: Guide knowledge (filter toggle in the dialog;
  Auto-sort switch now on the Chest Tools page), What's New
  BOTH surfaces (website + in-game changelog), tips check,
  menus check. None staged in this unit.
- FEEDBACK-TRACKER row added Nov 14, 2026 (row 63) by the
  build worker.

## Proof owed after install
- Mike (Bedrock): open a chest's filter — the dialog's first
  button is the Filter ON/OFF toggle; OFF lands at once, ON
  re-arms via the confirm; Server Menu shows the Auto-sort
  switch on the Chest Tools page, not under Settings/Toggles.
- Any Java player: selector slot 3 shows the Filter: ON/OFF
  lever beside the mode row and flips the same way.
