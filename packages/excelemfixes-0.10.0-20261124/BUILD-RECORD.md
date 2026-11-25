# RECORD — EliteMobs clarity, phase 2 (Nov 24, 2026)

## ADDENDUM — rebase executed (Nov 24, 2026, rebase lane)
The packet's rebase step is DONE. ExcelEMFixes 0.9.0 landed
live Nov 24, 2026 03:08 CT; the clarity-menus module was
ported onto the landed 0.9.0 tree in
build-root-rebased/ (payload byte-identical, hashes in
RECEIPTS.md addendum; the 0.9.0 skill-nudge reword preserved).
Rebuilt candidate: 461,849 B / SHA-256 22224de8… (double-build
byte-identical), amended into INSTALL-PACKET.md, RECEIPTS.md
and the MANIFEST 0.10.0 entry — the entry is now installable
against the live-jar gate 444,409 B / 2d930ac2…. Suites:
1025 passed / 0 failed twice (ClarityMenusTest 66/66; the
tie-break flake did not fire). The owed mirror rig boot ran
Nov 24, 2026 03:20 CT: boot Done (26.088s), SEVERE 0,
"Clarity Menus ready — 5 pages, 10 routes.", 19 modules in
the ready line and `emfixes status`, bot opened /emguide
("EliteMobs, Made Clear") and /emguide skills ("Skills &
Skill Bonuses"); mirror restored exactly (0.7.0 jar +
51,554 B config, stopped) with the live 0.9.0 jar hash
verified unchanged. Rig assets: pc/driver-clarity.ps1,
pc/bot-clarity.js, pc/cleanup-clarity.py, pc/sections-0100.txt,
pc/clarity-section.txt, pc/recon-clarity.ps1. Nothing
installed on live by this lane; no player messaged.
Lane: EM clarity phase 2. Parent brief + two in-flight corrections
(scope redirect ~1:14 AM CT; workflow addendum ~1:16 AM CT).
Nothing installed, no live file edited, no player messaged.

## Scope correction — acknowledged and applied
The original brief put the front-door menus in ExcelMenu. Mike's
correction (Nov 24, 2026, ~1:14 AM CT) moved them INSIDE
ExcelEMFixes as a module; ExcelMenu serves only as the thin
launcher door. This lane's item 1 was rebuilt accordingly before
any ExcelMenu menu work was written (the live ExcelMenu reads
done under the original brief became the door leg + receipts).
Items 2 (F2) and 3 (F10) were unaffected and ran as briefed.
Workflow addendum (~1:16 AM CT) also applied: the module's
documentation carries its reason (config explanation + README),
the upstream step is in the release plan below with the issue
text drafted (UPSTREAM-ISSUE-DRAFT.md, not posted), and this
plan follows the standing loop: player feedback → toggleable
module → documented reason → release → upstream notification
naming the ExcelEMFixes version (MEMORY.md, Nov 24, 2026).

## Coordination with the 0.9.0 glow lane
The glow lane owns ~/workspace/ghast-mount-20261123/build-root.
This lane never edited it. The 0.10.0 build root here
(~/workspace/em-clarity-phase2-20261124/build-root) was seeded
with READ-ONLY copies of that root's src/, libs/, build.sh and
README.md at a moment when the tree was still the pristine
0.8.0 source (plugin.yml version 0.8.0, no glow package present)
plus read-only copies of three deploy data fixtures the pack
test suites read. Final merge/rebase onto the landed 0.9.0 tree
is step 1–5 of the filed packet (INSTALL-PACKET.md, "REBASE
STEP") — it belongs to the filing/install path, amends the
rebuilt jar's hash into the packet, and includes the owed
mirror rig boot. If the glow lane lands 0.9.0 before the parent
files this, nothing here changes: the rebase step is written
against 0.9.0's INSTALLED record, whenever it exists.

## Item 1 — the clarity-menus module (ExcelEMFixes 0.10.0)
Module slug: clarity-menus (recorded choice). Package
excelemfixes.clarity; command /emguide [page]; toggle
modules.clarity-menus.enabled (default on). Built on the pack's
shared Views/MenuRenderer, so Java chest GUI and Bedrock
Cumulus form render identical content; the builder appends
Close to the home page and Back to every other page in code, so
the navigation law cannot be broken by a text edit. All wording
lives in clarity-menus.yml (bundled; data-folder copy overrides
it — the drop-guide pattern), parsed by a validating loader
(exactly one action kind per button; all page/route targets
resolve; home page required).

Menu tree (page id — title — buttons):
- home — "EliteMobs, Made Clear": doors to skills / quests /
  shops / gear + Close (nav).
- skills — "Skills & Skill Bonuses":
  [detail] How to claim skill bonuses (the 4 steps, step 3 =
  click the skill itself; 3-per-skill cap; tiers 10/25/50/75);
  [route skills-screen] Open the EliteMobs menu → runs "em"
  with a hint line (no EM command opens the bonus screen
  directly — verified in the jar; bare /em opens the status
  screen, live config emLeadsToStatusMenu: true);
  [detail] Bonus types in plain words (the audit's five
  glosses); [detail] Where bonuses come from. + Back (nav).
- quests — "Quests":
  [detail] How quests work (accept/track/objectives/turn in,
  10-quest cap); [route my-quests] See my quests → "em quest
  check"; [detail] Careful: the 'Accepted!' line (warning
  worded to stay true whether or not the F1–F9 label fix has
  landed — it teaches [Turn in!] and See-my-quests as the safe
  paths and names the confirmation + cooldown); [detail]
  Finding where to go (/questwhere + /questbeacon);
  [route questwhere] Find my quest → "questwhere". + Back.
- shops — "Shops & Elite Coins":
  [detail] Elite Coins (what/where/spends, vs Excel Bucks);
  [route coin-balance] Check my Elite Coins → "em money
  check"; [detail] Buying: two shops (set vs rolling, arrow
  shop); [detail] The reroll button (worded to stay true
  before and after F3's label lands); [detail] Selling (sell
  NPC only, Confirm Sale, cancel backs out; no /em command
  exists for the sell shop — verified: only an admin
  open-for-others variant); [route shop-set] → "em shop
  custom"; [route shop-rolling] → "em shop dynamic". + Back.
- gear — "Gear Tools": one [detail] + one [route] per tool —
  Repair ("em repair"; elite item + elite scrap), Enchant
  ("em enchant"; elite book onto elite item), Elite Scrolls
  ("em scroll"; converts a normal item into an elite item),
  Unbind ("em unbind"; Unbind Scroll removes the soulbound
  lock). Purposes grounded in the live menu files. + Back.
Every route command was verified against the running jar's
command classes and plugin.yml permissions (all default-true
for players; QuestCheckCommand declares no class permission —
framework default; the post-install player proof verifies it).
Dynamic state (e.g. a live unclaimed-bonus count in the menu)
does NOT exist in the pack today without new code paths into
EM player data; per the brief the menus stay static-clear and
no dynamic piece is specced — the skill-bonus-nudge (0.9.0)
already carries the count to the player in chat, so a menu-side
count is a duplicate, not a gap. (Judgment recorded here in
place of a follow-on module request.)

ExcelMenu door leg (rides the same packet, same window — the
el_questwhere precedent): staged-excelmenu/el_emguide.yml
(new entries file; order 90 / order-bedrock 80, last on both)
+ staged-excelmenu/elitemobs_menu.yml (full replacement; the
only change is the elite slot list gaining slot 9; gated on
the live layout hash in RECEIPTS.md).

Build + proof:
- ExcelEMFixes-0.10.0.jar: 440,539 B, SHA-256
  00fd21cb5ff4b91c6277797ecc5ec8f7ebc6765e923e7c4e38786144deab77e7;
  two builds byte-identical.
- Pack suites: 1000 passed / 1 failed — the failure is
  "Jester-shaped: armor named first on the tie", the documented
  pre-existing skill-nudge tie-break flake (fires on the
  pristine tree; module untouched by this lane). New
  ClarityMenusTest: 66/66 (structure, chrome law, route-command
  table, canonical-term checks, loader rejection cases).
- PROOF LEVEL (stated honestly): config lint + unit suites +
  view-model golden comparison (the module builds the same
  Views, under the same structural invariants the suite asserts
  for the known-good drop-guide menus). A mirror rig boot was
  NOT run: the only rig is inside the running glow lane's build
  root (coordination rule), and a from-scratch rig was not
  stood up for a module whose rendering path is the pack's
  shared, already-proven renderer. The rig boot is owed and is
  written into the packet as rebase step 4, before install.

## Item 2 — F2 structural fix attempt
Verdict: CANNOT be split within EM's quest-screen format; the
label fix (already staged in the F1–F9 packet) + the
clarity-menus warning is the ceiling. Full evidence in
F2-AMENDMENT.md, including the phase-2 discovery that EM
10.9.14 already intercepts abandoning with a second-click
confirmation (live Quests.yml questAbandonConfirmationText +
the jar's separate leaveconfirmed command), which lowers F2's
severity. Command neutralization considered and rejected, with
reasons. No new snippet staged. Decision logged as D217.

## Item 3 — F10 chat messages
Outcome: KEYS VERIFIED, overrides staged (3). The messages
live in EM's generated config files, not the translations
layer (unused here: 14 zero-byte ymls, no CSVs, English =
plugin defaults). Full inventory + judgments in F10-REPORT.md:
quest/currency/shop/skill messages are overwhelmingly clear
(quoted); three genuinely unclear keys in Quests.yml got exact
line-level overrides (staged/Quests.snippet.yml) —
questPrerequisitesMissingMessage (why + next step),
noQuestDestinationFound (/questwhere pointer),
chatTrackingHover (ungarbled). No custom_language.yml
overrides: that vehicle is a legacy no-op on this version
(decision D217). Filed as its own TEXT packet.

## Filings (D212)
- MANIFEST: "## PENDING — ExcelEMFixes 0.10.0 (clarity-menus
  module: /emguide front door) (added Nov 24, 2026)" —
  packet INSTALL-PACKET.md (6470 B / max line 78 /
  non-ASCII 22).
- MANIFEST: "## PENDING — EliteMobs quest message clarity
  (F10) (added Nov 24, 2026)" — packet INSTALL-PACKET-F10.md
  (2367 B / max line 64 / non-ASCII 7).
- Surfaces staged same-pass: SURFACES.md (What's New website
  + in-game texts, website help card; announcement waits for
  install; Guide hibernated — owed).
- Decision log: D217 (F2 ceiling + F10 vehicle). D215 left
  free for the glow lane; D216 was the audit's.

## Release plan for 0.10.0 (the standing loop)
1. Glow lane lands 0.9.0 (its own window + record).
2. Rebase per the packet (port module, rebuild, suites,
   mirror rig boot), amend the candidate hash into the packet.
3. Install at the first verified-empty moment (gated window,
   backup-first, jar .bak rollback written in the packet);
   ExcelMenu door + layout land in the same window.
4. Close-out surfaces from SURFACES.md (What's New both,
   website card); player proofs: Jester (Java) + Marvin
   (Bedrock) — proof-owed until their reports land.
5. UPSTREAM: post UPSTREAM-ISSUE-DRAFT.md to MagmaGuy/EliteMobs
   as a GitHub issue in Mike's voice, naming ExcelEMFixes
   0.10.0 — drafted here, posted at release time through the
   parent after Mike's review (standing rule: drafts get
   Mike's eyes first). This closes the loop for this feedback:
   Jester's report → clarity audit F1–F9 + wording packet →
   clarity-menus module → documented reason (config + README)
   → WRV release → upstream notice naming the version.

## Blocked / not done, stated plainly
- No mirror rig boot (see proof level) — owed at rebase.
- No dynamic unclaimed-count in the menus — judged a duplicate
  of the 0.9.0 nudge, not specced (above).
- The F2 stronger form (separate [Abandon] element in the NPC
  quest-screen templates) is upstream-only; carried as a
  suggestion in the draft issue, not a local fix.
- /em quest check's permission is framework-default (no
  class-declared permission in the jar) — the route is staged
  on that evidence; the post-install player proof is where a
  permission surprise would surface, and the fallback (drop
  that one route button via the text file) needs no rebuild.
