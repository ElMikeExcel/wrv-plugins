# MANIFEST ENTRY DRAFT — ExcelChunks 0.1.1 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelChunks 0.1.1 (console admin) — packet: ~/workspace/chunks-011-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/chunks-011-build/INSTALL-PACKET.md`
  (build record: `~/workspace/chunks-011-build/BUILD-RECORD.md`;
  commission: `~/workspace/console-admin-audit-20261109/AUDIT.md`
  §4).
- **Artifact:** `~/workspace/chunks-011-build/ExcelChunks-0.1.1.jar`
  — **8,784 B** — SHA-256
  `8d9b47e3dfaa453322915fd216ddcf4820f15c3c33e6418f76a7cc7c19b33877`
  (deterministic double-build byte-identical; no in-jar build-ID
  fingerprint exists in this lineage — version + hash identify it).
- **Live-jar gate (what must be live before the swap):**
  `ExcelChunks-0.1.0.jar` — 7,150 B — SHA-256
  `cf4f08c835ba4ec1314b0b12460367e98d6f857dd4bd37ad16e6faaee6ce2948`
  (held aside at swap as `ExcelChunks-0.1.0.jar.bak-20261110`,
  date adjusted to the install day). An unmatched live jar STOPS
  the packet — lineage question, not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelChunks only. No
  config edit (the plugin has no config file), no data
  migration, no other plugin change, no permission grants
  required (console works at once; `excelchunks.admin` is
  declared by this jar, default op). NOT a text live-edit —
  fully gated.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** one fresh backup for the window + cap-at-6 (name
  prunees; never the Daily newest or the two newest manuals);
  emptiness `list` = 0 twice; fresh hash of the staged 0.1.1 jar
  at the provenance gate.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelChunks v0.1.1`;
  - the 0.1.0-shaped enable line (`ExcelChunks enabled: /chunks
    view resync ON (item radius 96 blocks, cooldown 10s)`);
  - no SEVERE attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `chunks status` prints the three status
  lines (enabled + version; the resync constants; the
  no-config/permissions line) — those lines in the log are the
  live proof; bare `chunks` prints the 0.1.0 console refusal
  line verbatim.
- **What it adds:** ONE global status on the existing command —
  `/chunks status` (enabled/config state: version, item
  radius 96, cooldown 10s, hide/show delay 2 ticks, the
  permission nodes), routed BEFORE the player-only gate so the
  console can run it, gated by the NEW `excelchunks.admin`
  (default op; checked inside the status branch only — the
  command-level `excelchunks.use` is unchanged, so every
  player keeps their own resync). Commissioned from the Nov 9,
  2026 console-admin audit (AUDIT.md §4 — ExcelChunks 0.1.0
  verdict FAIL, packet sized TINY: no per-player op, because
  there is no persistent per-player state) under Mike's
  Nov 3, 2026 console-admin rule. Player-facing behaviour:
  none — the resync logic is byte-for-byte 0.1.0.
- **Proofs:** base proof — the unmodified 0.1.0 tree rebuilds
  the live jar byte-identically; desk 89/89 (60 carried 0.1.0
  checks unchanged + 29 new StatusCommand checks);
  deterministic double-build byte-identical; rig (Paper
  26.2-123 + Geyser-Spigot 2.11.3 + floodgate 2.2.5) clean
  boot, SEVERE 0, **6/6 behavior checks typed on the real
  console stdin** (`rig-0.1.1/RIG-RESULTS.md`), including all
  three status lines verbatim and the unchanged console
  refusal. Stated limitation: the non-admin denial is
  harness-pinned (the rig has no player sender).
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips check,
  menus check — itemized in the install packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelchunks-0.1.1-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
