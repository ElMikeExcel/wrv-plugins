# PUSH NOTES — ExcelMenu 3.0.0 (parent / supervisor lanes only)

This desk lane made NO GitHub or Drive publication and no
panel or MANIFEST work, per its task limits (staging only —
nothing pushed, uploaded, sent, or published). For the
parent/supervisor lanes:

- **Records package (staged, complete):**
  `~/workspace/records-catchup-20261103/packages/excelmenu-3.0.0-20261113/`
  — SHA256SUMS self-verified (`sha256sum -c SHA256SUMS` all OK
  at staging; see STAGING-REPORT.md). Pattern: mirrors the
  ExcelSummons 0.1.8 records package at
  `~/workspace/records-catchup-20261103/packages/excelsummons-0.1.8-20261112/`
  file-class for file-class, adapted to ExcelMenu's native
  Maven tree (src/main + src/test) exactly as the
  excelmenu-2.0-phase2-20261103 package carries it; the package
  differences are named in STAGING-REPORT.md.
- **The jar is NOT in this package** (house rule). The jar of
  record is
  `~/workspace/excelmenu-30-build-20261113/ExcelMenu-3.0.0.jar`
  (329,425 B, SHA-256
  306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51)
  — take it from there for the Drive leg; do not add it to the
  GitHub record package.
- **G2 duplicate guard — NOT YET RUN for this package:** the
  publishing lane must run
  `~/workspace/workflow-tools/pr-exists.sh` for excelmenu-3.0.0
  before any push, and the close-out record must carry the
  line: "Publication claim: wrv-plugins PR #___ (duplicate
  check run <timestamp>, result ___)" with the result and,
  once the draft exists, the PR number filled in.
- **GitHub leg, when run:** draft record PR in wrv-plugins
  from this package, on branch
  `record/excelmenu-3.0.0-20261113`, left OPEN as a draft,
  never merged (recording work is pre-approved standard
  workflow, Mike Nov 4, 2026).
- **Drive leg:** deposit into the shared Drive Minecraft WRV
  folder: (1) `ExcelMenu-3.0.0.jar` from the build folder
  above (329,425 B, SHA-256
  306607babd772894a9f8cc53eb35bd5514561154d55509ebe1dbb08ce970df51);
  (2) `ExcelMenu-3.0.0-sources.zip` (286,127 B, SHA-256
  792a2ec8c6bd54762ad4f40d592c14f43975f1a04fd3b0fa51f11382c5e382df;
  copy also in this package); (3) `ExcelMenu-3.0.0-SOURCE.md`
  — a copy of this package's SOURCE.md under that name. No
  passwords, tokens, or IPs in anything uploaded — none are
  in this package.
- **Surfaces:** this records close-out does NOT itself land
  the same-pass surfaces. Per INSTALL-PACKET.md / OUTCOME.txt
  they are separate supervisor legs still owed: Guide §5.4
  re-point + rearrange chunk, website pair, What's New BOTH
  surfaces, tips checked (menus = the install packet itself).
  The website PR, when run, may merge with byte-verification
  + the G3 Pages deploy check; the wrv-plugins record PR
  stays an open draft.
- **Routing (Mike, Oct 25, 2026):** Mike awake + available =
  connector route batched (one push_files call); asleep/
  overnight/unknown = browser route, zero dialogs. Never park
  the leg on an untapped card.
- Install state: 3.0.0 is ALREADY INSTALLED live (Nov 13,
  2026, failsafe window from the 07:10 CST scheduled run;
  boot Done (21.582s)!, 93/2/13, SEVERE 0 — INSTALLED.txt /
  OUTCOME.txt in this package). No MANIFEST entry is owed for
  the install; the records + Drive legs, the surfaces above,
  and the post-install proof (Mike's five DESIGN §7 player
  proofs) are what remain.
- The build folder and the pattern packages were NOT modified
  by this staging (read-only sources).
