# MANIFEST ENTRY DRAFT — ExcelAimAssist 0.1.5 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelAimAssist 0.1.5 (console admin) — packet: ~/workspace/aimassist-015-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/aimassist-015-build/INSTALL-PACKET.md`
  (build record: `~/workspace/aimassist-015-build/BUILD-RECORD.md`;
  commission: `~/workspace/console-admin-audit-20261109/AUDIT.md`
  §2/§4 + the re-base supplement in that folder).
- **Artifact:** `~/workspace/aimassist-015-build/ExcelAimAssist-0.1.5.jar`
  — **22,745 B** — SHA-256
  `9e5d926b74f0ddaba9b0d9adaaacf9cd41ace56a15a97dbe44a7a28b1fbc7a50`
  (deterministic double-build byte-identical; no in-jar build-ID
  fingerprint exists in this lineage — version + hash identify it).
- **Live-jar gate (what must be live before the swap):**
  `ExcelAimAssist-0.1.4.jar` — 19,401 B — SHA-256
  `7d040d98fc063418b3635c676e8a6f3db5adf093a4e99dec28a8f1c1ac1e6a44`
  (held aside at swap as `ExcelAimAssist-0.1.4.jar.bak-20261110`,
  date adjusted to the install day). An unmatched live jar STOPS
  the packet — lineage question, not a swap.
- **Swap shape:** 1-for-1 jar swap, ExcelAimAssist only. No
  config edit (config.yml byte-identical, no new keys; do not
  overwrite the live config), no data migration (toggles.txt
  format untouched), no other plugin change (NOT
  ExcelBedrockMenu, NOT server_menu.yml — the 0.1.4 menu trio
  stays), no permission grants required (console works at
  once; `excelaimassist.admin` is new and defaults to op).
  NOT a text live-edit — fully gated.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create at
  window start, delete at finish; re-check foreign markers before
  Shut Down and before Start (coordination abort on a foreign
  marker < 3h old).
- **Gates:** one fresh backup for the window + cap-at-6 (name
  prunees; never the Daily newest or the two newest manuals);
  emptiness `list` = 0 twice; fresh hash of the staged 0.1.5 jar
  at the provenance gate.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelAimAssist v0.1.5`;
  - `Successfully registered internal expansion: aimassist [0.1.5]`
    and `Hooked into PlaceholderAPI (%aimassist_status%)`;
  - the 0.1.4-shaped enable line (ON, cone 15.0, range 40.0,
    aim height 0.6, drop compensation ON, target lead ON) with
    the live saved-toggle count;
  - no SEVERE attributable to the swap; plugin count unchanged;
    guildgate LOCKED count unchanged.
- **Console smoke:** `aimassist admin status <a real family
  member BY NAME>` prints the one-line status (state +
  explicit/default basis) — that line in the log is the live
  proof (the rig addressed UUIDs; name resolution is smoked
  here); `aimassist admin status <their UUID>` agrees;
  `aimassist admin set <member> <state they already have>`
  prints the truthful no-op set line (never flip a real
  player's toggle for a smoke); `aimassist admin status
  NobodyHereXYZ` prints the "Nothing was looked up." rejection;
  `aimassist admin` prints both usage lines; `aimassist`
  prints the per-player line (self gate intact).
- **What it adds:** ONE admin subcommand family on the existing
  command — `/aimassist admin status <player>` (effective
  state + explicit/default basis from the player's real
  ToggleStore entry) and `/aimassist admin set <player> on|off`
  (writes through ToggleStore.set and persists toggles.txt at
  once), routed BEFORE the player-only gate and gated in code
  by the NEW `excelaimassist.admin` (default op; console
  explicitly allowed). Target resolution: online name first,
  then raw UUID (offline players by UUID only — no name→UUID
  record exists or is invented). Self-service
  `/aimassist on|off|toggle` and the `excelaimassist.toggle`
  menu node are byte-for-byte unchanged, as are aim behaviour,
  config and the placeholder. Commissioned from the Nov 9,
  2026 console-admin audit (AUDIT.md §2/§4 — ExcelAimAssist
  0.1.3 verdict FAIL, confirmed still FAIL at 0.1.4 by the
  supplement, which re-based the packet number to 0.1.5) under
  Mike's Nov 3, 2026 console-admin rule. Player-facing
  behaviour: none.
- **Proofs:** desk 234/234 (190 carried 0.1.4 checks unchanged
  + 44 new AdminCommands checks); deterministic double-build
  byte-identical; rig (Paper 26.3-231, the 0.1.5 jar + the live
  PlaceholderAPI 2.12.3, seeded toggles) clean boot, SEVERE 0,
  **19/19 behavior checks typed on the real console stdin**
  (`rig-0.1.5/RIG-RESULTS.md`), including the on-disk proof
  that status reads create and change nothing and the console
  self-form gate line verbatim from 0.1.4. Stated limitations:
  offline targets resolve by UUID only; no player joined the
  rig (self-flip by a real client is the one owed player
  proof, itemized in the install packet).
- **Same-pass surfaces owed at install:** Guide check,
  What's New BOTH surfaces (operator note only), tips check,
  menus check — itemized in the install packet.
- **Records:** staged at
  `~/workspace/records-catchup-20261103/packages/excelaimassist-0.1.5-20261110/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
