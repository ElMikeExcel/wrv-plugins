# MANIFEST ENTRY DRAFT — ExcelShops 1.3.8 (for the board supervisor to file)

Drafted by the desk build lane (board-manager run 190,
Nov 11, 2026). The supervisor verifies and files this as
a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build
lane did NOT edit the MANIFEST, the parent-backlog, or
board state. (No separate INSTALL-PACKET.md was cut for
1.3.8; the build record below carries the packet facts.)

**This entry SUPERSEDES the HELD ExcelShops 1.3.5 packet**
(`~/workspace/shops-135-build/`) — 1.3.5 was built on the
stale 1.3.4 base and must NOT be installed. 1.3.8 is the
same console-admin delta re-based onto the live 1.3.7.

---

## PENDING — ExcelShops 1.3.8 (console admin, re-based on live 1.3.7) — record: ~/workspace/shops138-rebase-20261111/BUILD-RECORD-1.3.8.md

- **Build record:**
  `~/workspace/shops138-rebase-20261111/BUILD-RECORD-1.3.8.md`
  (rig evidence:
  `~/workspace/shops138-rebase-20261111/rig-1.3.8/RIG-RESULTS-1.3.8.md`;
  commission: `~/workspace/console-admin-audit-20261109/AUDIT.md`
  §4, under Mike's Nov 3, 2026 console-admin rule).
- **Artifact:**
  `~/workspace/shops138-rebase-20261111/ExcelShops-1.3.8.jar`
  — **266,417 B** — SHA-256
  `b8a1b8fd9a56f420accdb066a3ae3e9c6989638668cadd2f5497a52117a4adb9`
  (deterministic: two independent builds package
  byte-identical; re-proven by re-packaging both
  surviving class trees to the same hash. Source zip:
  `ExcelShops-1.3.8-source.zip`, 186,824 B, SHA-256
  `78217dadf450dad9b4c4778b01bf23ebe963687a93cfca2f145dfe76e1613c76`.)
- **PROVENANCE CAVEAT — read before filing:** no
  original 1.3.6/1.3.7 source exists on disk (run-183
  sweep, 11 locations). The 1.3.7 base of this build
  was **reconstructed from the live jar** (Vineflower
  decompile, diffed against the 1.3.4 decompile so
  artifacts cancel). Fidelity vs the live jar: 70/99
  classes byte-identical; 18 instruction-identical
  (0 raw javap diff lines); 11 skeleton-equal (same
  ops/constants/calls per method; slot/branch/exception
  -table layout residuals only); all resources
  byte-identical; 412/412 carried tests; 3-boot rig.
  Full accounting in the build record §3. This is a
  stronger-tested base than a blind decompile, but it
  is not the original source — the supervisor surfaces
  this to Mike with the packet, it is not buried.
- **Live-jar gate (what must be live before the swap):**
  the live ExcelShops jar must hash to
  **252,335 B / SHA-256
  `12cf683a3b3125f4d1376ebe180699770ed630fb392a4b2c43f956a06a29988f`**
  (the `1.3.7-analytics-OFFLINE` build, pulled and
  verified Nov 11, 2026; `PULL-RECORD.md` in the build
  folder). Hold it aside at swap as
  `ExcelShops-1.3.7-analytics-OFFLINE.jar.bak-<install date>`.
  An unmatched live jar STOPS the packet — lineage
  question, not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelShops only.
  No config edit (do not overwrite the live data
  folder; config.yml / spawn-catalog.yml / mobile-menus
  are unchanged by this build), no data migration
  (shops.dat, both checkout journals and purchases.csv
  formats untouched — they are the live 1.3.7 formats),
  no other plugin change, no permission grants required
  (console works at once; BOTH admin nodes are declared
  by this jar with default op: `excelshops.admin` for
  /excelshopadmin and `wrvshops.admin` for stats —
  they are distinct nodes, do not conflate them).
  **Session proof stays OFF**: the guard code is
  carried and rig-proven, but the live config is not
  touched by this swap and the shipped default is
  `session-proof-enabled: false`; enabling it live is
  a separate decision for Mike, NOT part of this
  packet. NOT a text live-edit — fully gated.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt`
  — create at window start, delete at finish; re-check
  foreign markers before Shut Down and before Start
  (coordination abort on a foreign marker < 3h old).
- **Gates:** one fresh backup for the window + cap-at-6
  (name prunees; never the Daily newest or the two
  newest manuals); emptiness `list` = 0 twice; fresh
  hash of the staged 1.3.8 jar at the provenance gate.
  (Install-lane routing per Mike's Nov 11, 2026 ruling:
  PC route primary for transfer + hash verification,
  panel fallback immediate if the PC is unreachable.)
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelShops v1.3.8`;
  - `ExcelShops data check: …` (normal outcome; rig
    read EXISTING_TARGET);
  - `ExcelShops purchases available.`;
  - `ExcelShops enabled. Legacy ChestShop confirmation:
    … Economy: EssentialsX Economy`;
  - ExcelMenu `16 menus live, 0 failed` + `/shop ->
    spawnshop` registered;
  - no "entered protection-only mode" line; no SEVERE
    attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `excelshopadmin status <a real
  shop owner>` prints the multi-line status (shops +
  offers, balance, pending) — that block in the log is
  the live proof (the with-shops path the rig could
  not stage); `excelshopadmin status
  99999999-9999-9999-9999-999999999999` prints the
  no-shops block and creates nothing;
  `excelshopadmin status NobodyHereXYZ` prints the
  "Nothing was looked up." rejection; `excelshopadmin
  reload` prints the success line with live catalog
  counts + the audit log line; `excelshop` from
  console still prints the "Use this in game…" gate
  line; **`excelshop stats`** prints the two-line
  sales summary (`Shop sales, last 7 days: …` +
  `Spawn shop … | player shops …`) and
  `excelshop stats abc` prints
  `Usage: /excelshop stats [days 1-3650]` — the 1.3.7
  analytics surface carried by this base.
- **What it adds:** ONE separate console-safe command —
  `/excelshopadmin status <player>` (read-only: shops
  owned with every registered offer + price, the
  player's Vault balance, and the checkout's pending
  state — recovery flag + live-quote count) and
  `/excelshopadmin reload` (config + spawn catalog +
  mobile catalog, all-or-nothing; registry and
  checkout deliberately NOT reloaded), gated by the
  `excelshops.admin` node (default op; checked in
  code — no command-level permission). Target
  resolution: online exact name, then raw UUID, then
  the registry's own owner-name records. Deliberately
  NO money-moving, grant, revoke, delete, or edit op
  (money-adjacent plugin, per the audit's sizing).
  Everything else is the live 1.3.7 behaviour carried
  intact (purchase analytics + `/excelshop stats`,
  session-proof code off by default, mutation fences).
  Player-facing change relative to live: none
  (jar class delta vs the reconstructed 1.3.7: only
  WRVShops and CheckoutService differ, both
  admin-side, plus the 9 new admin classes).
- **Proofs:** desk 430/430 on two independent builds
  (412 carried + 18 AdminCommand); base proof —
  reconstructed tree-1.3.7 passes 412/412 and matches
  the live jar per the fidelity accounting above;
  deterministic double-build byte-identical
  (re-proven from the surviving class trees); rig
  (Paper 26.2-123 + Vault + EssentialsX +
  GriefPrevention + ExcelMenu 0.1.0 + RigCheck)
  **three clean boots, SEVERE 0, RigCheck PARITY OK**,
  all console commands typed on the real console
  stdin — including the full stats replies and a
  two-boot session-proof lifecycle exercise (READY
  envelope observed live mid-session; clean close
  removes envelope + fence; second enable starts
  clean). Stated limitations: with-shops status,
  owner-name resolution, and the non-admin denial are
  harness-pinned, not rig-observed (rig registry
  holds 0 shops; no fake-player driver) — the console
  smoke above is their live proof; crash /
  no-op-recovery session-proof paths not rig-forced
  (guard ships OFF; see build record §6).
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips
  check, menus check — itemized for the install window
  by the supervisor.
- **Records:** this build folder is the staged record
  (jar + source zip + build record + rig evidence);
  records-package deposit, GitHub draft-PR and Drive
  legs are the parent's standard close-out, not this
  lane's.
