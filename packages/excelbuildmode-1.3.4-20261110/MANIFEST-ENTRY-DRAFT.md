# MANIFEST ENTRY DRAFT — ExcelBuildMode 1.3.4 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`, REPLACING the HELD
BuildMode 1.3.3 entry (the HELD history stays recorded; the
console-admin 1.3.3 jar is superseded by this build and must never
be filed or installed). The build lane did NOT edit the MANIFEST,
the parent-backlog, or board state.

---

## PENDING — ExcelBuildMode 1.3.4 (combined rebuild: reviewed + console admin) — packet: ~/workspace/buildmode-134-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/buildmode-134-build/INSTALL-PACKET.md`
  (build record: `~/workspace/buildmode-134-build/BUILD-RECORD.md`;
  recovery record: `~/workspace/buildmode-134-build/REVIEWED-DELTA-RECOVERY.md`;
  designs: `docs/PILOT-1.3.3.md` + `docs/PILOT-1.3.4.md` in the
  build tree / source zip).
- **Artifact:** `~/workspace/buildmode-134-build/ExcelBuildMode-1.3.4.jar`
  — **99,073 B** — SHA-256
  `39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3`
  (deterministic double-build byte-identical; source zip rebuilds
  the same hash; the recovered reviewed half is control-proven
  bytecode-identical to the live reviewed build's delta).
- **Live-jar gate (what must be live before the swap):**
  `ExcelBuildMode-1.3.3.jar` — the October REVIEWED build —
  90,044 B — SHA-256
  `3662dcb73ca08ac1677e4257f64eae489ab9486e484ecc447ea2779cedc3ee1c`
  (live since the Oct 23–25, 2026 batch; held aside at swap as
  `ExcelBuildMode-1.3.3.jar.bak-20261110-reviewed`). STOP if the
  live jar is anything else — in particular NOT the genuine 1.3.2
  (26d356b4…, present only as a non-loadable .bak) and NOT the
  superseded console-admin 1.3.3 jar (fa640544…).
- **Swap shape:** 1-for-1 jar swap. No config edit, no data
  migration, no permission changes (the live `excelbuildmode.reviewed`
  grant carries under the same node name; console works at once;
  `excelbuildmode.admin` defaults to op).
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0 twice;
  nobody in Build Mode at swap.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelBuildMode v1.3.4`;
  - banner `ExcelBuildMode 1.3.4 PILOT:` including BOTH
    `1.3.3: console admin - /buildmode admin status <player> and /buildmode admin off <player> (excelbuildmode.admin, console allowed)`
    and `1.3.4: combined rebuild - the reviewed-builder exemption rides alongside the console admin surface`;
  - no startup-problem line; plugin count unchanged; SEVERE 0
    attributable to ExcelBuildMode; guildgate LOCKED 0 of 5.
- **Console smoke:** `buildmode admin status NobodyHereXYZ` prints
  the unknown-name line; `buildmode admin` prints the usage line;
  `buildmode status` still prints the player-path console rejection
  (admin-branch-only allowance). No real restore smoked outside the
  player proofs.
- **What it adds vs live:** `/buildmode admin status <player>` +
  `/buildmode admin off <player>` — console-runnable,
  `excelbuildmode.admin` (default op); off forces a clean Survival
  restore through the existing recovery path. The reviewed-builder
  exemption (`excelbuildmode.reviewed`, default false) is PRESERVED
  from the live build, recovered + regression-pinned — it is not
  new behavior. Commissioned: console-admin audit (Nov 9, 2026,
  AUDIT.md §4) under Mike's Nov 3, 2026 console-admin rule (1.3.2
  was the audit's support-critical FAIL) + decision D5 (Nov 10,
  2026) resolving the 1.3.3 version collision.
- **Proofs:** eight suites, 11,685 assertions (six carried at
  1.3.2 totals + AdminTests 82 + ReviewedTests 25 new, both truth
  tables); recovery control-build zero normalized-javap diff on
  both recovered classes + plugin.yml byte-identical; rig
  (Paper 26.2-123 + WorldEdit 7.4.5) clean boot, SEVERE 0, 7/7
  console checks (`rig-1.3.4/RIG-RESULTS.md`). Post-install player
  proofs: A — reviewed pair (Marvin enters with standing WorldEdit,
  exits clean; non-reviewed counter-check if an account is
  available); B — console admin rescue (status in Build Mode,
  console admin off restore verified in game, on/off + relog
  regression pair).
- **Same-pass surfaces owed at install:** Guide check, What's New
  BOTH surfaces, tips check, menus check — itemized in the install
  packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelbuildmode-1.3.4-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
