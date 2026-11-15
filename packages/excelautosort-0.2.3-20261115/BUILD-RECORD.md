# P-AUTOSORT-FILTER-1 — build worker RESULT (board-manager run 362)

VERDICT: BUILT. ExcelAutoSort 0.2.3 candidate + staged menu entry
+ packet draft, all evidence below. Nothing filed in MANIFEST,
nothing installed, no live file touched.

## Candidate (structured evidence)

- Jar: ~/workspace/board-r362/autosort-filter/packet/ExcelAutoSort-0.2.3.jar
  — 161,791 B / SHA-256
  0457557e15c3de52033be300bc7f5bb32991268f3005544d7ec0fe15965b130b
- Build ID: EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07
- Tests: 39 found / 39 successful / 0 failed (35 pre-existing +
  4 new: filterToggleFlipsAndRestoresLastActiveMode,
  filterToggleMemorySurvivesOffAndFollowsTheLatestMode,
  filterToggleWordsLiveInConfig,
  menuToggleEntryLivesInChestToolsNotSettings)
- Double-build: byte-identical (build script cmp PASS);
  fingerprint gate PASS (excel-build.txt + BuildInfo fragments)
- Source zip: packet/ExcelAutoSort-0.2.3-source.zip — 328,378 B /
  SHA-256 0258cbc105bb2a5e2ad580cd8ca4eae0bfe190cbe47d45ee585f49d4ec5cfa7d
- Build tree: ~/workspace/board-r362/autosort-filter/build/ExcelAutoSort-0.2.3/
- Rig (local Paper, BB-04 rig pattern, port 25654): PASS —
  "Enabling ExcelAutoSort v0.2.3"; awake line exact incl. build
  ID; "Done (30.984s)"; SEVERE count 0. Log:
  rig/server/boot.log. (Piped console stop did not take — the
  BB-04 rig's known quirk; run ended by timeout after Done.)
- Code-trace walk: WALK-TOTAL pass=20 fail=0
  (walk-check.sh / WALK-OUTPUT.txt in this folder)
- Packet draft: ~/workspace/board-r362/autosort-filter/INSTALL-PACKET.md
  — Lint: 4319 B / max line 88 / non-ASCII 14 (targets met:
  <= ~12 KB, lines < 300, minimal non-ASCII)
- Receipts: packet/RECEIPTS.md (build, rig, walk, G1 sections);
  packet/SHA256SUMS

## Ownership verification (verify-first, from source)

- The Chest filter dialog is owned by ExcelAutoSort — VERIFIED:
  gui/SelectorGui.java (Java selector; title key gui-selector-title
  "Chest filter"), bedrock/BedrockFormsHook.java (Bedrock main
  form; state text "Filter mode now: ..." built in stateText()),
  engine/SelectorFlow.java (all mode/list semantics),
  core/Mode.java, core/FilterCore.java, store/ChestRecord.java.
- The Bedrock premise matches the 0.2.2 source exactly: the main
  form's content is text-only state and its buttons were
  list/hand/recovery/back/done — no toggle. (Live is 0.2.1; its
  BedrockFormsHook is the same shape — records-catchup copy at
  ~/workspace/records-catchup-20261103/packages/excelautosort-0.2.1-20261109/.)
- The Java selector already had mode buttons at slots 0/1/2
  (OFF / WHITELIST / BLACKLIST) but no single ON/OFF toggle;
  slot 3 was empty. The toggle now occupies slot 3.
- Menu toggle ownership — VERIFIED: the only on/off toggle entry
  in the ExcelMenu registry owned by ExcelAutoSort is entry id
  "autosort" (requires-plugin: ExcelAutoSort, action
  "[player] autosort", permission excelautosort.toggle,
  status-placeholder %autosort_status% — served by ExcelAutoSort's
  own hook/AutoSortExpansion.java). It is the per-player
  auto-sort switch, NOT a per-chest filter control.

## Premise corrections (report per verify-first)

1. There is NO per-chest filter on/off entry anywhere in the
   menu system: the per-chest filter mode lives only inside the
   Selector dialog. The menu entry Mike's report points at can
   only be the "autosort" toggle entry above. The move was
   applied to that entry.
2. "Settings (Details Settings page)": no filter or auto-sort
   control exists inside the Details Settings door/definition
   in any source checked (details_settings entry is ExcelDetails'
   combat-meter door, group settings). The autosort entry's
   group in the newest live-shaped set (menu-toggles-20261112
   staged + installed, OUTCOME.txt registry reload 94/0) is
   "toggles" — the Toggles band that sits in the Settings row /
   directly after the Settings pages (Makers Mark 205, Details
   220 Bedrock weights around it) — and "settings" in the older
   ExcelMenu 3.0.1 bundled snapshot. Under either, it was NOT
   with the chest tools; the staged move puts it in
   chest_tools ("Chest Tools", server_menu page 2 — the chest
   organization section holding chestfilter / filterrecover /
   maxorg), order 25, order-bedrock 245, appended after maxorg
   (20 / 240) with no live entry renumbered and no slot/weight
   collision (chest_tools: 8 slots, 3 doors today).
3. "Chest Organization" is not a group id in any menu source;
   the chest-organization section's id is chest_tools. The
   staged entry uses that id.

## What was built

- NEW core/FilterToggleCore.java — pure toggle decision:
  isOn(mode); toggleTarget(current, lastActive) (filtering ->
  OFF; OFF -> lastActive, else WHITELIST); remember(landed,
  previous) (OFF never erases the memory); stateWord.
- engine/SelectorFlow.java — toggleFilter(s) routes through the
  existing requestMode: OFF applies at once and persists (record
  mode OFF, list kept — the existing §5.1 pause semantics);
  ON goes through the existing §6.1 confirm gate with its
  itemized rejects + recovery sweep. open() seeds the session
  memory from the record; confirm() updates it.
- gui/SelectorSession.java — new field lastActiveMode.
- gui/SelectorGui.java — slot 3 LEVER toggle button
  (config keys messages.filter-toggle-on / -off), click case 3
  -> flow.toggleFilter; mode row and state readout unchanged.
- bedrock/BedrockFormsHook.java — toggle is the main form's
  FIRST button (config keys messages.form-filter-toggle-on /
  -off); handler routes via flow.toggleFilter (NEEDS_CONFIRM ->
  the existing confirm modal, which returns to the main form);
  remaining buttons shift by one (list 1, hand 2, recovery 3,
  back 4, done 5). stateText() mode line unchanged.
- config.yml — ONLY 4 new message keys added (the toggle words).
- plugin.yml / BuildInfo.java / local-build.sh — 0.2.2 -> 0.2.3,
  build ID above.
- menu/autosort.yml (build tree) — the staged ExcelMenu entry
  described in correction 2; every line except group/order/
  order-bedrock/header is identical to the live Nov 12 entry.
- Diff vs the 0.2.2 tree (diff -rq): exactly the files above
  plus tests. Nothing else differs.

## BB-04 non-mutation proof

- BB-04 packet jar re-hashed this run: 160,197 B / SHA-256
  8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28
  — identical to the BB-04 packet's recorded hash. The 0.2.3
  tree is a copy; the filed BB-04 packet was never written.
- ORDER WARNING for the supervisor (also in the packet draft):
  0.2.3 is built ON the 0.2.2 tree and contains all BB-04
  fixes. It must install AFTER BB-04 or instead of it — never
  before (a later 0.2.2 install would erase the toggle).

## G1 / Homebase

- Homebase probes this run: 3 attempts (22:10:10, 22:10:40,
  22:11:10 CST, Nov 14, 2026), all "Connection reset by
  198.19.0.1 port 3128" at the relay. Per the alert-clarity
  rule this is evidence about my relay path only; the PC's own
  state is unknowable while they fail. Last successful probe
  on record ~4:40 PM CST Nov 14, 2026 (run 353).
- Per the commission, the build proceeded on the 0.2.2
  candidate tree. G1 remains OPEN in the packet draft: the
  supervisor must take a fresh Homebase pull or a labeled
  FALLBACK (panel) receipt for BOTH live premises (live jar
  0.2.1; live autosort.yml = Nov 12 toggles file) before
  filing. Full detail in packet/RECEIPTS.md.

## FEEDBACK-TRACKER

- FOUND: ~/workspace/goals/family-minecraft-server-build/files/FEEDBACK-TRACKER.xlsx
  (sheet "Feedback Tracker"). Row 63 appended for this item
  (feature / version 0.2.3 / testing ask / Mike's Nov 14 report
  as the received feedback / status BUILT — install + proof
  owed). Pre-edit backup copy:
  ~/workspace/board-r362/autosort-filter/FEEDBACK-TRACKER.pre-r362.xlsx

## Not done / owed (supervisor chain)

- MANIFEST filing (after G1 receipts land) — supervisor.
- Same-pass surfaces at filing: Guide, What's New BOTH,
  tips check, menus check — named as owed in the packet draft.
- Live mirror walk (Homebase test-server mirror) — not run,
  relay down; player proof after install stands in the packet.
- Persistence proof shape: the OFF toggle landing persists via
  the pre-existing requestMode(OFF) record write (store save),
  unit-covered at the decision layer (FilterToggleCore tests)
  and by the unchanged 35-test suite over FilterCore/Selector
  model; an in-game persistence tap is part of the post-install
  player proof owed (Mike, per the packet).
