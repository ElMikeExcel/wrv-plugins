# PUSH NOTES — ExcelSkillsPanel 0.2.2 (parent / supervisor lanes only)

This desk lane made NO GitHub or Drive publication and no
panel or MANIFEST work, per its task limits (staging only —
nothing pushed, uploaded, sent, or published). For the
parent/supervisor lanes:

- **Records package (staged, complete):**
  `~/workspace/records-catchup-20261103/packages/excelskillspanel-0.2.2-20261113/`
  — SHA256SUMS self-verified (`sha256sum -c SHA256SUMS` all OK
  at staging; see PACKAGE-NOTES.md). Pattern: mirrors the
  ExcelSummons 0.1.8 records package at
  `~/workspace/records-catchup-20261103/packages/excelsummons-0.1.8-20261112/`,
  with the package-level differences named in
  PACKAGE-NOTES.md (no sources zip shipped by this build, no
  config.yml — the plugin ships none, DIAGNOSIS.md carried,
  base-gate + raw rig tree stay in the build folder).
- **The jar is NOT in this package** (house rule for this
  package). The jar of record is
  `~/workspace/skillspanel-022-build-20261113/ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar`
  (62,142 B, SHA-256 374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e)
  — take it from there for the Drive leg; do not add it to the
  GitHub record package.
- **G2 duplicate guard — NOT RUN by this lane:** the supervisor
  runs `~/workspace/workflow-tools/pr-exists.sh` for this
  package BEFORE any push (exit 3 = an open/draft PR already
  exists — do not push a new one; verify the existing PR
  instead). The close-out record carries the line:
  "Publication claim: wrv-plugins PR #___ (duplicate check run
  <timestamp>, result ___)" with the PR number filled in once
  the draft exists.
- **GitHub leg, when run:** draft record PR in wrv-plugins
  from this package, on branch
  `record/excelskillspanel-0.2.2-20261113`, left OPEN as a draft,
  never merged (recording work is pre-approved standard
  workflow, Mike Nov 4, 2026).
- **Drive leg:** deposit into the shared Drive Minecraft WRV
  folder: (1) `ExcelSkillsPanel-0.2.2.jar` from the build folder
  above (62,142 B, SHA-256
  374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e);
  (2) `ExcelSkillsPanel-0.2.2-SOURCE.md` — a copy of this
  package's SOURCE.md under that name. There is NO sources zip
  for 0.2.2 (none was shipped by the build); the src tree
  travels in the PR. No passwords, tokens, or IPs in anything
  uploaded — none are in this package.
- **Surfaces:** the 0.2.2 install DOES owe same-pass surfaces
  (supervisor legs, not this records leg): Guide skills-panel
  chunk (rank-from-carried-wand + credited-casts wording),
  website pair, What's New BOTH surfaces (website + in-game
  changelog entry for 0.2.2), broadcaster tips check, in-game
  menus check. (The copied INSTALL-PACKET.md retains its
  original same-pass-surfaces text as the install record of its
  window.)
- **Routing (Mike, Oct 25, 2026):** Mike awake + available =
  connector route batched (one push_files call); asleep/
  overnight/unknown = browser route, zero dialogs. Never park
  the leg on an untapped card.
- Install state: 0.2.2 is ALREADY INSTALLED live (Nov 13, 2026,
  failsafe window scheduled 15:03 CST; boot 3:48:09 PM CST —
  INSTALLED.txt / OUTCOME.txt in this package). No MANIFEST
  entry is owed for the install; the records + Drive legs, the
  same-pass surfaces, and the post-install proof (Mike's live
  `/skillpanel` check holding food, Java + Bedrock) are what
  remain.
- The build folder was NOT modified by this staging (read-only
  sources; the build's own SHA256SUMS re-verified 6/6 in place).
