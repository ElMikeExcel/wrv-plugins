# MANIFEST ENTRY DRAFT — ExcelQuartermaster 0.3.1 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelQuartermaster 0.3.1 (console admin) — packet: ~/workspace/quartermaster-031-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/quartermaster-031-build/INSTALL-PACKET.md`
  (build record: `~/workspace/quartermaster-031-build/BUILD-RECORD.md`;
  commission: `~/workspace/console-admin-audit-20261109/AUDIT.md`
  §2/§4).
- **Artifact:** `~/workspace/quartermaster-031-build/ExcelQuartermaster-0.3.1.jar`
  — **79,785 B** — SHA-256
  `a11aa0fcab31544ffeb92c6ab31207412e797bb8d1147eb73d34e4375d0fa2f9`
  (deterministic double-build byte-identical; no in-jar build-ID
  fingerprint exists in this lineage — version + hash identify it).
- **Live-jar gate (what must be live before the swap):**
  `ExcelQuartermaster-0.3.0.jar` — 77,066 B — SHA-256
  `70096fc47b61f09131aa42c1f3a66fbee3c248085d3d39f476338cb01b15765a`
  (held aside at swap as `ExcelQuartermaster-0.3.0.jar.bak-20261110`,
  date adjusted to the install day). An unmatched live jar STOPS
  the packet — lineage question, not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelQuartermaster only. No
  config edit (config.yml byte-identical, no new keys; do not
  overwrite the live config), no data migration (player-tales /
  java-pack-state / issued-wands formats untouched), no other
  plugin change, no permission grants required (console works at
  once; `excelquartermaster.admin` already exists, default op).
  NOT a text live-edit — fully gated.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** one fresh backup for the window + cap-at-6 (name
  prunees; never the Daily newest or the two newest manuals);
  emptiness `list` = 0 twice; fresh hash of the staged 0.3.1 jar
  at the provenance gate.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelQuartermaster v0.3.1`;
  - the four 0.3.0-shaped enable lines (tales/anchor; Bedrock
    presentation; Java parity with the live saved-pack-state
    count; wand issue with the live issued/seal/Magic state);
  - no SEVERE attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `qm status <a real family member or their
  UUID>` prints the one-line status (offers used / remaining,
  pack state, last change) — that line in the log is the live
  proof; `qm status 99999999-9999-9999-9999-999999999999` prints
  the no-record line and creates nothing; `qm status
  NobodyHereXYZ` prints the "Nothing was looked up." rejection;
  `qm` prints both usage lines. No real packreset smoked outside
  a genuine need.
- **What it adds:** ONE read-only console subcommand —
  `/qm status <player>` (offers used + pack state from the
  player's real PackPrompt record, incl. the accepted and
  exhausted terminal wordings and the verbatim last-change
  timestamp), beside the existing console-capable
  `/qm packreset`. Target resolution is packreset's, unchanged
  (online name first, then raw UUID — an offline player is
  addressable by UUID only). Gated by the existing
  `excelquartermaster.admin` (default op), console explicitly
  allowed by the same mechanism packreset already uses. NO new
  mutating op, NO new permission node, NO offline name lookup.
  Commissioned from the Nov 9, 2026 console-admin audit
  (AUDIT.md §2/§4 — ExcelQuartermaster 0.3.0 verdict FAIL,
  packet sized TINY) under Mike's Nov 3, 2026 console-admin
  rule. Player-facing behaviour: none.
- **Proofs:** desk 602/602 (575 carried 0.3.0 checks unchanged
  + 27 new PackStatus checks); deterministic double-build
  byte-identical; rig (minimal Paper 26.2-123, only the 0.3.1
  jar, seeded pack state) clean boot, SEVERE 0, **12/12
  behavior checks typed on the real console stdin**
  (`rig-0.3.1/RIG-RESULTS.md`), including the packreset
  regression and the on-disk proof that status reads create
  and change nothing. Stated limitations: offline targets
  resolve by UUID only (packreset's shape, matched
  deliberately); no player joined (no player surface changed).
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips check,
  menus check — itemized in the install packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelquartermaster-0.3.1-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
