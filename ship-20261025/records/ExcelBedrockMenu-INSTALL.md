# ExcelBedrockMenu 1.5.17 (staged, NOT installed)

Rank: BLOCKER (privacy). The live 1.5.16 shows a real family member's first name to every player as the
example in /childproofmenu help, the plugin.yml usage line and two hint messages.

Provenance: 1.5.15 and 1.5.16 were not on Drive (newest Drive source is 1.5.14). Their source is the box tree
/workspace/childproof/ebm/tree and git branch excelbedrockmenu-1.5.16-childproof (pushed to origin, so the name
is also in that remote branch; cleaning it needs a push, which was not done). The 1.5.16 rebuild from that tree
is class-identical to the installed jar (sha prefix dff55e29), so the tree is the true live source.

## What changes
- The example name becomes the neutral "Alex" in help text, usage, hints, comments and MenuLogicTest.
- Header note in ChildproofMenu. plugin.yml version 1.5.17. Nothing else.
- Only ChildproofMenu.class and plugin.yml differ from the 1.5.16 rebuild.
- No diff file is shipped on purpose (its removed lines would re-expose the name). orig/ holds the old text:
  keep it out of any repo.

## Artifact
- build/ExcelBedrockMenu-1.5.17.jar  sha256 47efb1e9f9315ecf1bf4208040ab62c62680b0d06aae6991fd57388177cc7154  (32325 B, reproducible)
- build/ExcelBedrockMenu-1.5.16-rebuilt.jar (reference only)

## Checks
1. Build + tests: MenuLogicTest ALL PASS on 1.5.16 and 1.5.17 (126 ok lines each; fix-tests.log, orig-tests.log).
   Rig (box rig port 25911): /childproofmenu help as a test player
   - 1.5.16: 4 help lines, real-name example present.
   - 1.5.17: 4 help lines, "Alex" example, no other name.
   Logs: ../tests/BM.log, ../rig/logs-bm-*.log (the log records only true/false, never the name). Rig stopped, plugin removed.
2. Review pass: class and resource comparison against the installed 1.5.16 (above); name scan of fix/ is clean.

## Install record (when Mike approves; not done)
- Move the live ExcelBedrockMenu-1.5.16.jar to ExcelBedrockMenu-1.5.16.jar.bak-<date> (keep it; never overwrite
  an existing .bak). Copy in 1.5.17, verify sha256, restart. No config change. Rollback: restore the .bak jar.
- Also check the server's commands.yml /childproof alias text for the same example name (not visible from the box).
