# MANIFEST ENTRY DRAFT — ExcelPouch 0.1.1 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelPouch 0.1.1 (console admin) — packet: ~/workspace/pouch-011-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/pouch-011-build/INSTALL-PACKET.md`
  (build record: `~/workspace/pouch-011-build/BUILD-RECORD.md`;
  brief + design: `~/workspace/pouch-consoleadmin-20261110/`).
- **Artifact:** `~/workspace/pouch-011-build/ExcelPouch-0.1.1.jar`
  — **45,109 B** — SHA-256
  `b4cdfa2057733a06fe39607bd018c571d8bb0a9cfbce06d753cc0e6cfff3f9c4`
  (deterministic double-build byte-identical; no in-jar build-ID
  fingerprint exists in this lineage — version + hash identify it).
- **Live-jar gate (what must be live before the swap):**
  `ExcelPouch-0.1.0.jar` — 28,574 B — SHA-256
  `ace560bfd315f3dd9b3ebc54562fb45a5ca82a5bae1750ec521293122a69d211`
  (held aside at swap as `ExcelPouch-0.1.0.jar.bak-20261110`).
  An unmatched live jar STOPS the packet — lineage question,
  not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelPouch only. No config
  edit (no new config keys; do not overwrite the live config),
  no data migration (existing data/*.yml format-compatible both
  directions), no other plugin change (PouchQuery untouched —
  ExcelMenuJoin needs no bump), no permission grants required
  (console works at once; `excelpouch.admin` is new, default op).
  `data/rescue/` is created by the plugin on first confirmed
  clear, not by the packet. NOT a text live-edit — fully gated.
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
  - `Enabling ExcelPouch v0.1.1`;
  - enable line naming the pocket count + allowlist + wand key;
  - no SEVERE attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `pouch admin status` prints the global
  trio incl. "PouchQuery service: registered"; `pouch admin
  status NobodyHereXYZ` prints the never-played rejection;
  `pouch admin` prints help; `pouch` still prints "Only players
  can use the Utility Pouch." Live proof (brief §6.3): one
  console `pouch admin status` on a real family member read back
  in the log. No real return/clear smoked outside a genuine
  rescue need.
- **What it adds:** the console-admin set — `admin status`
  (global + per-player, online or offline, read-only with
  non-retaining reads), `admin return` (online only,
  full-inventory safe), `admin clear … confirm` (the D10 stack:
  confirm gate → close-GUI-first + post-sync revalidation →
  verified verbatim rescue copy → delete via setPocket → audit
  line; works offline; sealed pockets clearable, broken stores
  refuse), `admin reload` — gated by the new `excelpouch.admin`
  (default op), console explicitly allowed. NO grant op, NO
  offline-return destination, NO broken-store reset/recovery op
  (design-D3 flagged in brief §5 — the delete-the-quarantine
  form is a hard-line park for Mike), NO rescue pruning.
  Commissioned from the Nov 9, 2026 console-admin audit
  (AUDIT.md §2/§4 — ExcelPouch 0.1.0 verdict FAIL) under Mike's
  Nov 3, 2026 console-admin rule. Player-facing behaviour:
  none.
- **Proofs:** desk 95/95 (72 carried regression checks unchanged
  + 23 new AdminSelector checks); deterministic double-build
  byte-identical; rig (Paper 26.2 + Magic 11.2.5 + ExcelMenuJoin
  1.4.0) clean boot, SEVERE 0, **44/44 behavior checks through
  the real console sender** (`rig-0.1.1/RIG-RESULTS.md`),
  including the design §4.6 duplicate-wand worked example, the
  GUI-open race for return and clear, and both D10 negative
  tests (unwritable rescue dir refuses; post-close-sync pocket
  change aborts). One recorded deviation: the §3 step-2
  revalidation compares the §4.1 item identity, not raw bytes
  (Magic refreshes volatile wand fields in the GUI round-trip;
  a different item still aborts) — BUILD-RECORD.md.
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips check,
  menus check — itemized in the install packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelpouch-0.1.1-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
