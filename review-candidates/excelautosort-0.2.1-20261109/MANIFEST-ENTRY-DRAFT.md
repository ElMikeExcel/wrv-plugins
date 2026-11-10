# MANIFEST ENTRY DRAFT — ExcelAutoSort 0.2.1 (for the board supervisor to file)

Drafted by the desk build lane (Nov 9, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelAutoSort 0.2.1 (admin per-player surface) — packet: ~/workspace/autosort-plugin/build-0.2.1/INSTALL-PACKET.md

- **Packet:** `~/workspace/autosort-plugin/build-0.2.1/INSTALL-PACKET.md`
  (build record: `~/workspace/autosort-plugin/build-0.2.1/BUILD-RECORD.md`;
  spec: SPEC §16 in the build folder).
- **Artifact:** `~/workspace/autosort-plugin/build-0.2.1/ExcelAutoSort-0.2.1.jar`
  — **159,240 B** — SHA-256
  `48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266`
  (deterministic double-build byte-identical; fingerprint gate PASS).
- **Build ID:** `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`
- **Live-jar gate (what must be live before the swap):**
  `ExcelAutoSort-0.2.0.jar` — 154,773 B — SHA-256
  `6164d943cfaeef498e8966978d9c3255d2352ab81ccc0b36875064d4d0013138`
  (0.2.0, installed Nov 7, 2026, 9:07–9:54 AM CST; held aside at
  swap as `ExcelAutoSort-0.2.0.jar.held-20261109-pre021`).
- **Swap shape:** 1-for-1 jar swap + ONE config line merged (the
  `messages.usage` admin line gains `status|revoke|grant`; backup
  `config.yml.bak-20261109-pre021` first). No menu files, no data
  migration — `data/maxorg.yml` dialect unchanged.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manuals); emptiness `list` = 0 twice.
- **Boot-verify lines (all must appear):**
  - awake line: `ExcelAutoSort 0.2.1 awake: sort parity engine on
    (Barrel, Chest, Ender Chest, Shulker Box), filter chests N,
    recovery entries N, protect sink BOUND, gp rights BOUND,
    max-org on (economy BOUND, entitlements N), build
    EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331` — the entitlements
    count must equal the pre-swap live count (carried over, NOT 0
    unless the live count was 0);
  - `ExcelAutoSort: ExcelProtect audit sink BOUND (stream: items)`;
  - plugin count unchanged from pre-swap; SEVERE 0 attributable to
    ExcelAutoSort; guild seal state unchanged (record actual).
- **Console smoke:** `autosort status` prints `ExcelAutoSort 0.2.1
  — build EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`;
  `autosort admin status <known player>` prints that player's
  Auto-sort ON/OFF + Max Organization state. No grant/revoke smoke
  on real accounts.
- **What it adds (SPEC §16):** `/autosort admin status <player>`,
  `/autosort admin revoke <player>` (buy + rent cleared at once,
  persisted), `/autosort admin grant <player> max` (no-charge
  permanent; ExcelCosmetics symmetry) — console-runnable,
  `excelautosort.admin` (op), live-effect, Protect-logged as
  `autosort_maxorg_admin_grant` / `autosort_maxorg_admin_revoke`.
  Commissioned by Mike Nov 9, 2026 (~8:07 PM CST) — 0.2.0's admin
  surface was prune|reload only, violating the Nov 3, 2026 console
  admin rule.
- **Proofs:** unit 35/35 (29 carried + 6 new); rig phase 6
  (admin) 21/21 live on Paper 26.3 — grant/revoke proven live +
  persisted mid-boot, Protect stream verified; full 0.1.0/0.2.0
  regression rig result in BUILD-RECORD.md.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelautosort-0.2.1-20261109/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-PLAN.md there.
