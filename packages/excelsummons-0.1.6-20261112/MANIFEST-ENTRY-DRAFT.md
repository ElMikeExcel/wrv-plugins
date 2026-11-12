# MANIFEST ENTRY DRAFT — ExcelSummons 0.1.6 (for the board supervisor to file)

Drafted by the desk build lane (Nov 12, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`, completing the G1
live-premise receipts first (see the placeholder below). The
build lane did NOT edit the MANIFEST, the parent-backlog, or
board state.

---

## PENDING — ExcelSummons 0.1.6 (adopted pets hold no mana + sync re-derivation clamped to the 75% ceiling / 20% floor) — packet: ~/workspace/summons-016-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/summons-016-build/INSTALL-PACKET.md`
  (build record: `~/workspace/summons-016-build/BUILD-RECORD.md`;
  defect of record: the unnumbered item at the end of
  `~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
  — Jester's Nov 12, 2026 report: his all-adopted menagerie held
  85% of a 200 pool because the ceiling/floor bound PreCast
  only; binding spec: Mike's ruling Nov 12, 2026, ~3:08 PM CST —
  adopted pets hold NO mana; tree summary: `DESIGN-0.1.6.md` in
  the build tree / source zip).
- **Artifacts (jar only, one window):**
  - `~/workspace/summons-016-build/ExcelSummons-0.1.6.jar` —
    **66,209 B** — SHA-256
    `08da4f10a5154b28187f2b000535abb0154498b751efa19c0a3518e4e04f70f1`
    — build `EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a`
    (double-build byte-identical; clean-room + source-zip
    rebuilds reproduce the hash).
  - No config swap (the shipped config.yml is byte-identical
    to the installed 0.1.5 config — do NOT replace the live
    one), no Magic modifiers/messages merge (the clamped key
    set is a subset of the existing 18 templates), no
    data-field change.
- **Live-jar gate:** live is ExcelSummons-0.1.5.jar (65,253 B,
  SHA-256 5cdf16a0…f61e, installed Nov 11, 2026) — verify
  before the swap; STOP if it does not hash.
- **Live-premise receipts (G1) — SUPERVISOR TO COMPLETE AT
  FILING:** same-day live pulls (path, size, SHA-256,
  timestamp; rclone, or labeled FALLBACK (panel) receipts) for:
  (1) the live ExcelSummons jar; (2) the live ExcelSummons
  config.yml (reserve-ceiling-percent / reserve-floor-percent
  values); (3) the live summons-data.yml (origin census —
  Jester's menagerie rows). The build lane asserts no live
  hashes; this field is a labeled placeholder until the
  supervisor fills it.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create
  at window start, delete at finish; re-check foreign markers
  before Shut Down and before Start.
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the
  Daily newest or the two newest manuals); emptiness `list` = 0
  twice.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelSummons v0.1.6`;
  - enable line `ExcelSummons 0.1.6 enabled (build EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a); registry holds N summon(s).`;
  - NO "could not be applied" reserve warnings; SEVERE 0
    attributable; guildgate LOCKED count unchanged; plugin
    count unchanged. (Expected, not a failure: first login
    syncs remove pre-0.1.6 adopted-pet reserve modifiers —
    that is the fix landing.)
- **What it adds/changes:** adopted-origin records now
  contribute 0% mana reserve on every path (PreCast, login
  sync, drift sweep, list lines) — Mike's ruling: original
  tamed/bred pets hold no mana; cast summons keep their rates
  (combat 10/15/25/20/25, wolf 10, familiar 5, cat 5, mount
  10) and adopted pets still count toward the summon count
  cap. The sync re-derivation is clamped to the live ceiling
  (75) + floor (20): holds apply per record in registry order,
  a record that would pass the cap holds nothing (the summon
  stays), and the PreCast gate + list totals now quote the
  applied (clamped) total. Everything else — caps, dismiss,
  messaging, the archer guarantee — is unchanged 0.1.5
  behavior; no config or spells value touched.
- **Proofs:** SummonsTest 78/78 (three builds, byte-identical;
  incl. fixture (a) Jester's all-adopted menagerie → applied
  0% and fixture (b) a 90% raw cast set → clamped at sync to
  exactly 75%, usable ≥ 20%); rig (Paper 26.3-235 + Magic
  11.2.5) clean boot, SEVERE 0, reserve warnings 0, archer
  soft-fail lines 0, 21/21 behavior checks
  (`rig-0.1.6/RIG-RESULTS.md` — the 19 carried checks with
  A–D reserve expectations corrected to the ruling + phase G's
  menagerie + clamp checks against real Magic modifier state).
  Post-install: Jester's login proof — full 200 pool back,
  menagerie untouched (the defect's named player proof).
- **Same-pass surfaces owed at install:** Guide, What's New
  BOTH surfaces, tips check, menus check — itemized in the
  install packet.
- **Records:** staged at
  `~/workspace/summons-016-build/records-package/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
  FEEDBACK-TRACKER row for this bug owed at close-out
  (tester = Jester).
