# BB-01 RESULT — ExcelMenu Back fixes (board-manager run 354, unit BB-01)

Worker outcome: BUILD COMPLETE (15/15 rows implemented, tests + local
boot rig green). PACKET IS A DRAFT — the G1 live-premise pull is
BLOCKED (Homebase route down; panel fallback needs a live-browser
leg), so the packet must not be filed until the supervisor completes
that pull. Mirror walk likewise owed (same route). Details below;
all evidence is structured, paths relative to
~/workspace/back-button-wave1-r354/ unless absolute.

## 1. G1 live-premise receipt — BLOCKED, exact blocker + evidence

- Route tried: Homebase SSH (mike-pc via Tailscale proxy), probe
  pattern 3 attempts over ~2 minutes plus 2 later retries.
- Probes (America/Chicago): 2026-11-14 16:53, 16:54, 16:55, 17:13,
  ~17:17 — ALL failed identically:
  `kex_exchange_identification: read: Connection reset by peer` /
  `Connection reset by 198.19.0.1 port 3128`.
- Per the Nov 11 alert-clarity rule: my connection path to Homebase
  failed its probes; the PC's own state is unknowable while they
  fail. No claim is made that the PC is down.
- FALLBACK (panel): a valid G1 fallback exists on paper (Shockbyte
  panel File Manager download, labeled FALLBACK (panel)) but it
  needs a live-browser leg a desk worker cannot run. OWED by the
  supervisor/failsafe lane before filing/install.
- Baseline check: nightly-pull-20261114 records its pull did NOT
  run; newest baseline (2026-11-12) expired 2026-11-14 03:22 CST.
  No live hash is cited anywhere in this unit (G4 respected).
- Consequence: the build stands on the audited 3.0.1 tree (the
  split's source of record). The packet draft carries the live-jar
  gate BLANK on purpose — it must be filled from the owed pull,
  and if live has moved past 3.0.1 the unit re-bases first
  (split §3/§7 stop condition). Full record: BB-01/packet/RECEIPTS.md
  R1/R2 (5552 B / SHA-256
  3f8914e33bf1182322a7a2142f318875085944b6727c732837054daff717a369).
  Audited-tree jar (disk lineage, NOT a live premise): 330214 B /
  5bb1c1a3c45ce4b54e75a5e5b6e5d03b50f06c5708afd3904c3cf85e031b2939.

## 2. Build — ExcelMenu 3.0.2, all 15 rows

- Candidate: BB-01/ExcelMenu-3.0.2.jar — 337069 B / SHA-256
  0dacc1364851f716aaefd67aa138b259f2eca34ca6cf51e3aeb14228c72e7359
- Source: BB-01/build-tree/ (copy of the audited 3.0.1 tree +
  BB-01 changes; build.sh, version bumped 3.0.1 -> 3.0.2 in
  build.sh + plugin.yml).
- Shared contract: every FIX-2 Back lands on the ExcelMenu Server
  Menu through MenuService.openMenu(player, "server_menu") — the
  opener /menu itself uses — wired as one shared opener in
  ExcelMenu.onEnable (+ FormService.setServerMenuOpener). Settled
  labels were never substituted; Back is appended LAST everywhere.

Row map (audit row -> implementation):
- FIX-1 teleport confirm, Java+Bedrock (2): ConfirmService takes a
  returnMenuId (MenuService tracks each player's last non-confirm
  menu; default server_menu). Bedrock: Cancel button and form
  close reopen it. Java: Cancel item (slot 5) and ESC-close with
  ticket still pending (new gate.isPending) reopen it. Gate is
  answered/cancelled first — Back never teleports. Cancel lore:
  "&7Back to the menu — stay where I am" (ConfirmTargets).
- FIX-1 teleport step-2 choice, Java+Bedrock (2): Back returns to
  the step-1 picker. Java chest: Back item slot 4 (ARROW).
  Bedrock: ModalForm (2 buttons max — both are the directions)
  converted to SimpleForm, same labels/order + Back last.
- FIX-1 childproof confirm, Bedrock (1): Cancel and form close
  reopen the player picker (ChildproofScreen.confirm).
- FIX-1 Skills & Perks website follow-on, Bedrock (1): buttons
  Close + Back (SkillsPerksCore.WEBSITE_FORM_BUTTONS); Back
  reopens the Skills & Perks form.
- FIX-2 Server Commands root, Java (1): bundled layouts/
  commands_menu.yml page 1 gains fixed item slot 49 (ARROW,
  "&7&lBack to Server Menu", action [openguimenu] server_menu).
  Ships as a STAGED LAYOUT FILE (jar extraction is fill-missing
  only): BB-01/packet/staged-layouts/commands_menu.yml — 3707 B /
  0005e5dcd6e40bfa8c6b6c78c920db1dd1a916c6ca75651452090841175466a9.
- FIX-2 Server Commands root picker, Bedrock (1): FormModel gains
  Target.ServerMenuTarget + a picker overload flag; FormService
  sets it for every GROUPED layout whose id is not server_menu.
  Group forms keep Back->picker, so the chain is group -> picker
  -> Server Menu. Server Menu picker itself unchanged (root).
- FIX-2 teleport step-1 picker, Java+Bedrock (2): Bedrock form
  appends "Back" after the names. Java chest reserves its last
  slot for a Back item (size rounds up for heads+1; heads cap at
  backSlot, so at 54 players one head slot is traded for Back).
- FIX-2 childproof picker, Bedrock+Java (2): Bedrock form appends
  "Back". Java chat list gains a clickable "[Back to Server
  Menu]" line (runCommand /menu; ChildproofCore.JAVA_BACK_LABEL).
- FIX-2 Skills & Perks bespoke form, Bedrock (1): BUTTONS become
  [My mcMMO stats, Skill Trees on the Website, Close, Back];
  Back index handled in SkillsPerksScreen.
- FIX-2 Server Website form, Bedrock (1): buttons become
  [Close, Back] (WebsiteCore.FORM_BUTTONS); Back opens Server Menu.
- FIX-2 Ask the Guide box, Bedrock (1): CustomForm has no button
  surface, so onClosedOrInvalid now re-opens the Server Menu —
  the audit's own "closing reopens the parent" Yes pattern. Send
  path unchanged.
- Out of scope kept out: Server Menu root (N/A), Chest
  Organization / pins / Maker's Mark (Unverified), Basics Menu
  (not a defect), teleport request answer prompt (not in BB-01's
  15 — it is a standalone prompt with no parent screen).

## 3. Test output summary (script-named)

- Command: `BB-01/build-tree/build.sh` (compile + jar + JUnit).
- Result: `OK (174 tests)` — baseline 3.0.1: `OK (169 tests)`
  (menu-colors-20261113/excelmenu-301-build/build-301-*.log).
  Delta +5 test methods, 0 failures, 0 errors. Compile warnings:
  1 pre-existing deprecation (ActionDispatcher, also in 3.0.1).
- New/changed pins: FormModelTest.pickerWithServerMenuBackEndsWithBack
  (picker = 9 groups + Back last, ServerMenuTarget);
  ChestModelTest.commandsMenuPagesCarryEveryRow (page-1 slots
  39 -> 40; slot 49 FixedItem name/action pinned);
  SkillsPerksCoreTest.backButtonsAppendedLast;
  ChildproofCoreTest.backNavigationLabels;
  TeleportCoreTest.backNavigationLabels;
  TeleportConfirmationGateTest.isPendingTracksTheTicketLifecycle;
  ConfirmTargetsTest.settledButtonLabels (CANCEL_LORE).
- These tests run the PRODUCTION layouts/entries through
  ChestModel/FormModel — the render-shape walk of every registry
  screen in scope (commands_menu Java page 1 + Bedrock picker).

## 4. Boot rig (local Paper; mirror substitute evidence)

- Rig: local Paper 26.3, plugins = candidate + PlaceholderAPI
  only; log kept at BB-01/rig-evidence/rig-latest.log (35193 B /
  636f36fee4612bde4a3dd88ad25e463794640507170f2ef8e62c94b58897bcdf).
- Script output: "ExcelMenu layouts: 2 live: commands_menu,
  server_menu"; "self-check: loaded 12 menus"; "ExcelMenu 3.0.2
  enabled ... 83 entries, 2 registry menus, 12 definition menus
  live."; "Done (92.648s)!"; `grep -c SEVERE` = 0.
- Extracted rig layouts/commands_menu.yml contains the slot-49
  Back item (grep evidence in RECEIPTS.md R5).
- NOT done: the PC test-server mirror walk (Java + Bedrock,
  in-game, Back out level by level) — blocked by the same
  Homebase route failure as §1; owed before install, and Mike's
  spot taps remain the acceptance end after install. Bukkit
  click/form handlers are covered by code + core pins only.

## 5. Packet draft

- Path: BB-01/packet/INSTALL-PACKET.md (DRAFT — NOT FILE-READY,
  NOT written to MANIFEST.md; supervisor is sole MANIFEST writer).
- Lint (P3 script, python byte/line/non-ASCII count):
  6600 B / max line 78 / non-ASCII 24 — inside targets
  (<= ~12 KB, lines < ~300).
- Receipts: BB-01/packet/RECEIPTS.md, cited in the packet by
  path + hash (above). Packet carries: candidate + staged-layout
  hashes, blank live-jar gate (owed), scope per row, full gates,
  boot-verify script lines, same-pass surfaces state (Guide
  checked/no edit; What's New line drafted for both surfaces;
  tips checked/no change; menus = this unit), proof-owed lines.

## 6. SHA256SUMS verification

- File: BB-01/SHA256SUMS (5 entries: candidate jar, packet,
  receipts, staged layout, rig log).
- Command: `sha256sum -c SHA256SUMS` in BB-01/ — result: 5/5 OK
  (ExcelMenu-3.0.2.jar: OK; packet/INSTALL-PACKET.md: OK;
  packet/RECEIPTS.md: OK; packet/staged-layouts/commands_menu.yml:
  OK; rig-evidence/rig-latest.log: OK).

## 7. What the supervisor does next

1. When the Homebase route recovers (or via a panel browser leg):
   pull live plugins/ExcelMenu*.jar + live layouts/
   commands_menu.yml; record path/size/SHA-256/timestamp as the
   G1 receipt (FALLBACK (panel) label if the panel is used) and
   fill the packet's live-jar gate. If live != 3.0.1 lineage,
   send BB-01 back for re-base before filing.
2. Run the mirror walk (§4) on the test-server mirror.
3. File the packet as a pointer stub in MANIFEST.md; the
   failsafe installs (jar + staged layout placement, fully gated).

No purchases, no sends, no installs, no live data touched or
deleted by this unit. MANIFEST.md was not written.
