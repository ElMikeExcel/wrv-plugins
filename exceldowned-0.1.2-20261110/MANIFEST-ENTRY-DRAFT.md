# MANIFEST ENTRY DRAFT — ExcelDowned 0.1.2 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelDowned 0.1.2 (console admin surface) — packet: ~/workspace/downed-012-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/downed-012-build/INSTALL-PACKET.md`
  (build record: `~/workspace/downed-012-build/BUILD-RECORD.md`;
  design: `DESIGN-0.1.2.md` in the build tree / source zip).
- **Artifact:** `~/workspace/downed-012-build/ExcelDowned-0.1.2.jar`
  — **56,778 B** — SHA-256
  `2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396`
  — build `EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d`
  (deterministic double-build byte-identical; clean-room and
  source-zip rebuilds reproduce the same hash).
- **Live-jar gate (what must be live before the swap):**
  `ExcelDowned-0.1.1.jar` — 47,199 B — SHA-256
  `331e3b103ef07da6f81455f212d56c849916e1105232d0bb0b5bd472ae871d2f`
  (0.1.1, installed Nov 8, 2026; held aside at swap as
  `ExcelDowned-0.1.1.jar.bak-20261110`).
- **Swap shape:** 1-for-1 jar swap. No config edit (shipped
  config byte-identical to 0.1.1), no data migration, no spells
  change, no permission grants required (console works at once;
  `exceldowned.admin` already exists, default op).
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0 twice.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelDowned v0.1.2`;
  - `ExcelDowned: custom Magic action 'Resurrect' registered.`;
  - ready line `ExcelDowned 0.1.2 ready — build EXC-DOWNED-0.1.2-WRV-20261110-01-d0wn3d; sessions restored: N; bleed-out 300s; downed factor 0.25`;
  - no startup-problem line; plugin count unchanged; SEVERE 0
    attributable to ExcelDowned; guildgate LOCKED count unchanged.
- **Console smoke:** `downed revive NobodyHereXYZ` and
  `downed clear NobodyHereXYZ` print the unknown-name line;
  `downed revive` prints the usage line; `giveup` still prints
  "Only a player can give up." (admin-branch-only allowance). No
  real revive/clear smoked outside the player proof.
- **What it adds:** `/downed revive <player>` (force-resolve an
  online downed session without death, through the existing
  Resurrection revive path, recorded as `admin_revive` with the
  operator actor) + `/downed clear <player>` (remove a downed
  session outright, online or offline-durable; no health
  restored, no death run) — console-runnable, gated by the
  existing `exceldowned.admin` (default op). Commissioned from
  the Nov 9, 2026 console-admin audit (AUDIT.md §4) under Mike's
  Nov 3, 2026 console-admin rule — 0.1.1 was a FAIL (console
  could inspect but not fix a stuck downed player). Downed is
  SECOND in the audit's suggested build order (after BuildMode
  1.3.3).
- **Proofs:** DownedTest 27/27 (17 carried at 0.1.1 totals + 10
  new AdminPolicy tests incl. acts exercised on a real session
  registry); rig (Paper 26.3-235 + Magic 11.2.5) clean boot,
  SEVERE 0, 17/17 behavior checks through the real console
  sender (`rig-0.1.2/RIG-RESULTS.md`). Post-install player
  proof: console revive at 50% on a downed volunteer, console
  clear leaving the volunteer at half a heart and healable,
  Resurrection-cast + /giveup-menu regression pair.
- **Same-pass surfaces owed at install:** Guide check, What's New
  BOTH surfaces, tips check, menus check — itemized in the install
  packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/exceldowned-0.1.2-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
