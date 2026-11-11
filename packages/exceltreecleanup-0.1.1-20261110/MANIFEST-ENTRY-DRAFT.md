# MANIFEST ENTRY DRAFT — ExcelTreeCleanup 0.1.1 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelTreeCleanup 0.1.1 (console admin) — packet: ~/workspace/treecleanup-011-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/treecleanup-011-build/INSTALL-PACKET.md`
  (build record: `~/workspace/treecleanup-011-build/BUILD-RECORD.md`;
  commission: `~/workspace/console-admin-audit-20261109/AUDIT.md`
  §4).
- **Artifact:** `~/workspace/treecleanup-011-build/ExcelTreeCleanup-0.1.1.jar`
  — **24,389 B** — SHA-256
  `66e5f4a431fa920fc9f7a9eb582d045f0486b9bc2356555e0d8b422778dc21a7`
  (deterministic double-build byte-identical; no in-jar build-ID
  fingerprint exists in this lineage — version + hash identify it).
- **Live-jar gate (what must be live before the swap):**
  `ExcelTreeCleanup-0.1.0.jar` — 20,515 B — SHA-256
  `0507acefca25ec83858d25ab9dfbd17c47f230fd708939a86b357080bb883423`
  (held aside at swap as `ExcelTreeCleanup-0.1.0.jar.bak-20261110`,
  date adjusted to the install day). An unmatched live jar STOPS
  the packet — lineage question, not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelTreeCleanup only. No
  config edit (config.yml byte-identical, no new keys; do not
  overwrite the live config), no data migration (toggles.txt
  format untouched), no other plugin change, no permission
  grants required (console works at once; `exceltreecleanup.admin`
  is declared by this jar, default op). NOT a text live-edit —
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
  - `Enabling ExcelTreeCleanup v0.1.1`;
  - the 0.1.0-shaped enable line (`ExcelTreeCleanup enabled: ON,
    remnant max 3 logs, flood cap 512, <live count> saved player
    toggle(s), GriefPrevention claim checks ON` — the ON tail is
    required; OFF = claim seam failed, STOP);
  - no SEVERE attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `treecleanup admin status <a real family
  member or their UUID>` prints the one-line status (ON/OFF +
  explicit-vs-default) — that line in the log is the live proof;
  `treecleanup admin status 99999999-9999-9999-9999-999999999999`
  prints the default-ON line and creates nothing;
  `treecleanup admin status NobodyHereXYZ` prints the "Nothing
  was looked up." rejection; `treecleanup admin` prints the
  combined usage line. No real admin set smoked outside a
  genuine need.
- **What it adds:** ONE admin family on the existing command —
  `/treecleanup admin status <player>` (effective toggle state
  + explicit-vs-default, from the player's real ToggleStore
  record) and `/treecleanup admin set <player> on|off` (stores
  an explicit choice through ToggleStore.set, persists
  immediately, audit-logged), routed BEFORE the player-only
  gate so the console can run them, gated by the NEW
  `exceltreecleanup.admin` (default op; checked inside the
  admin branch only — deliberately no command-level permission,
  so every player keeps their own ungated self toggle). Target
  resolution is the house shape (online exact name first, then
  raw UUID — an offline player is addressable by UUID only).
  Commissioned from the Nov 9, 2026 console-admin audit
  (AUDIT.md §4 — ExcelTreeCleanup 0.1.0 verdict FAIL, packet
  sized SMALL, "Same shape as AimAssist") under Mike's Nov 3,
  2026 console-admin rule. Player-facing behaviour: none —
  cleanup logic and the self toggle are byte-for-byte 0.1.0.
- **Proofs:** desk 122/122 (84 carried 0.1.0 checks unchanged
  + 38 new AdminCommand checks); deterministic double-build
  byte-identical; rig (Paper 26.2-123 + GriefPrevention
  16.18.7 + the 0.1.0 TcRigDriver fake players, seeded
  toggles.txt) clean boot, SEVERE 0, **20/20 behavior checks
  typed on the real console stdin**
  (`rig-0.1.1/RIG-RESULTS.md`), including set by UUID and by
  online name, all malformed forms changing nothing, the R6
  self-toggle case re-proven end-to-end, and the on-disk proof
  that status reads create nothing. Stated limitations:
  offline targets resolve by UUID only; the non-admin denial
  is harness-pinned (the rig's fake-player chat is not
  log-visible). Rig run 1 caught an invalid plugin.yml in the
  first jar (unquoted description) — fixed, harness-pinned,
  rebuilt; run 2 is the shipped hash.
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips check,
  menus check — itemized in the install packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/exceltreecleanup-0.1.1-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
