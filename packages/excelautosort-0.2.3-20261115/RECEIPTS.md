# P-AUTOSORT-FILTER-1 / ExcelAutoSort 0.2.3 — RECEIPTS

## G1 live-premise receipt — OPEN (not satisfied)

Claim needing a receipt: live jar is ExcelAutoSort 0.2.1
(plugins/ExcelAutoSort-0.2.1.jar), and the live ExcelMenu entry
plugins/ExcelMenu/entries/autosort.yml is the Nov 12, 2026
toggles-split file this unit's staged entry replaces.

- Homebase probes this run (SSH alias mike-pc, via the runtime
  Tailscale relay): 3 attempts, the lane's retry pattern, all
  failed with "kex_exchange_identification: read: Connection
  reset by peer / Connection reset by 198.19.0.1 port 3128":
  2026-11-14 22:10:10, 22:10:40, 22:11:10 CST.
- Per the Nov 11 alert-clarity rule: this is evidence about the
  relay path only; the PC's own state is unknowable while the
  probes fail. Last successful Homebase probe on record:
  ~4:40 PM CST, Sat Nov 14, 2026 (board-manager run 353).
- The build proceeded on the 0.2.2 candidate tree per the
  commission (do not block the build on the route). Before the
  supervisor files this packet, a fresh Homebase pull or a
  labeled FALLBACK (panel) receipt must confirm BOTH live
  premises above.
- Lineage (NOT G1 evidence, for the re-pull to confirm):
  live jar ExcelAutoSort-0.2.1.jar 159,240 B / SHA-256
  48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266
  (BB-04 RECEIPTS.md); live autosort.yml per menu-toggles-20261112
  OUTCOME.txt: 2,309 B / SHA-256
  6b25beaf3498489bff7dfe671dfa62993f6c44fca69dda3262153d49a41b4976
  (group toggles, order 10, order-bedrock 210).

## Candidate build receipt

- Tree: ~/workspace/board-r362/autosort-filter/build/ExcelAutoSort-0.2.3/
  (copy of the BB-04 0.2.2 candidate tree; BB-04 packet NOT
  mutated — its jar re-hashed this run: 160,197 B / SHA-256
  8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28,
  matching the BB-04 packet exactly.)
- Changed vs 0.2.2 (diff -rq): config.yml, local-build.sh,
  plugin.yml, BuildInfo.java, BedrockFormsHook.java,
  SelectorFlow.java, SelectorGui.java, SelectorSession.java,
  AutoSortTest.java; new: core/FilterToggleCore.java,
  menu/autosort.yml. Nothing else differs.
- Build: bash local-build.sh — JUnit 39 found / 39 successful /
  0 failed (35 pre-existing + 4 new); double-build byte-identical;
  fingerprint gate PASS.
- Build ID: EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07
- Candidate jar: ExcelAutoSort-0.2.3.jar — 161,791 B / SHA-256
  0457557e15c3de52033be300bc7f5bb32991268f3005544d7ec0fe15965b130b
- Source zip: ExcelAutoSort-0.2.3-source.zip — 328,378 B / SHA-256
  0258cbc105bb2a5e2ad580cd8ca4eae0bfe190cbe47d45ee585f49d4ec5cfa7d
- Candidate config.yml: SHA-256
  fde2a51c7a6a19ba7a280885c3d4c0f853bb5ab9f3b98d02435fbfcbdbcc592c
- Staged menu entry menu/autosort.yml: 2,381 B / SHA-256
  d579593873757466c34797fd2c85fe6cca36b9bfa209682bed7a9e324c24f8c8

## Rig receipt (local Paper rig, board-r362 rig/server, port 25654)

- Boot log: ~/workspace/board-r362/autosort-filter/rig/server/boot.log
- "[ExcelAutoSort] Enabling ExcelAutoSort v0.2.3" present.
- Awake line verbatim: "ExcelAutoSort 0.2.3 awake: sort parity
  engine on (Barrel, Chest, Ender Chest, Shulker Box), filter
  chests 0, recovery entries 0, protect sink BOUND, gp rights
  BOUND, max-org on (economy UNBOUND, entitlements 0), build
  EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07"
- "Done (30.984s)" present; SEVERE count in boot.log = 0.
- Rig config carried the 4 new toggle message keys merged in
  (the packet's config-merge scope) — boot clean with them.
- Shutdown note: the piped console "stop" did not take (the
  BB-04 rig's known console quirk); the run was ended by the
  240 s timeout after Done. Boot evidence is unaffected.
- Vault absent on the rig (expected) — same line as 0.2.1/0.2.2.

## Mirror walk receipt (code-trace, scripted)

- Script: ~/workspace/board-r362/autosort-filter/walk-check.sh
- Output: ~/workspace/board-r362/autosort-filter/WALK-OUTPUT.txt
- Result: WALK-TOTAL pass=20 fail=0 (toggle anchors across the
  core, the flow, both renderers, the config keys, and the
  staged menu entry's group/weight lines).
- A live mirror walk (Homebase test-server mirror, Java +
  Bedrock clients) was NOT run: the Homebase relay was down
  this run (see G1). Player proof remains owed after install.
