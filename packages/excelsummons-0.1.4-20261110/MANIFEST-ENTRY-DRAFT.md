# MANIFEST ENTRY DRAFT — ExcelSummons 0.1.4 (for the board supervisor to file)

Drafted by the desk build lane (Nov 10, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.
NOTE: filing this entry SUPERSEDES the pending ExcelSummons
0.1.3 entry if that entry is still PENDING (0.1.4 contains the
whole 0.1.3 delta and installs directly over the live 0.1.2
jar); if 0.1.3 installs first, 0.1.4 installs over it. The
supervisor marks the 0.1.3 entry SUPERSEDED in that case.

---

## PENDING — ExcelSummons 0.1.4 (Wolf / summons fix: companion reserve, rank-scaled cap, dismiss pets, refusal messaging) — packet: ~/workspace/summons-014-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/summons-014-build/INSTALL-PACKET.md`
  (build record: `~/workspace/summons-014-build/BUILD-RECORD.md`;
  design: FIX-PACKET-DESIGN §§2–4 in
  `~/workspace/wolf-summon-diagnosis-20261110/`, as amended by
  Mike's Nov 10, 2026 rulings 10:08 / 10:34 / 10:35 / 10:37 AM;
  tree summary: `DESIGN-0.1.4.md` in the build tree / source zip).
- **Artifacts (ship together, one window):**
  - `~/workspace/summons-014-build/ExcelSummons-0.1.4.jar` —
    **63,642 B** — SHA-256
    `a34d7bdf54c9017eb3e4290d2fa89c3685e0adb3c9f40eef53f959f6322cc25a`
    — build `EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19`
    (double-build byte-identical; clean-room + source-zip
    rebuilds reproduce the hash).
  - `~/workspace/summons-014-build/modifiers-addition-0.1.4.yml`
    — +11 reserve templates appended to Magic's modifiers.yml.
  - `~/workspace/summons-014-build/messages-addition-0.1.4.yml`
    — per-spell refusal overrides merged into Magic's
    messages.yml (new `spells:` section; no default key edited).
  - config: REPLACE the live ExcelSummons config with the
    shipped one (or hand-merge: + `summon-cap-by-rank`, −
    `per-caster-combat-cap`, + reserve rows wolf/familiar/cat/
    mount). NOT a no-config swap (contrast 0.1.3).
- **Live-jar gate:** live is either ExcelSummons-0.1.3.jar
  (54,707 B, SHA-256 d6dc49d0…c852) or ExcelSummons-0.1.2.jar
  (48,944 B, SHA-256 064e0ba5…2473) — verify which before the
  swap; STOP if neither hashes.
- **Precondition note:** plugins/ExcelMagicGate/earned.yml is
  the rank seam's source; if MagicGate 0.3.0 is not yet live,
  summon caps fail to Beginner 1 until it is — the install
  report must name that state if so.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create
  at window start, delete at finish; re-check foreign markers
  before Shut Down and before Start.
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the
  Daily newest or the two newest manuals); emptiness `list` = 0
  twice.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelSummons v0.1.4`;
  - enable line `ExcelSummons 0.1.4 enabled (build EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19); registry holds N summon(s).`;
  - NO "could not be applied" reserve warnings (missing
    template = the modifiers step was missed); SEVERE 0
    attributable; guildgate LOCKED count unchanged; plugin
    count unchanged.
- **What it adds/changes:** companions + mount hold mana
  reserve (wolf 10 / familiar 5 / cat 5 / mount 10; one 75%
  ceiling, 20% floor, every branch); the summon cap is ONE
  counted total (combat + companion + pack; mount uncounted)
  scaled by EARNED rank (Beginner 1 · Student 2 · Apprentice 3
  · Master 5, from MagicGate earned.yml; unreadable rank fails
  to 1); over-cap sets grandfathered, never trimmed;
  `/summons dismiss pets` + admin pets scope release CAST pets
  only (adopted hand-tamed pets protected by the new origin
  field); every bridge refusal names its reason with numbers
  (rank, limit, count, current summons, percents); Magic-side
  summon failures get per-spell specific texts (target, mana
  $cost, cooldown $time, own-limit). §3.4 registry
  reconciliation deliberately NOT wired (record reasoning in
  BUILD-RECORD).
- **Proofs:** SummonsTest 69/69 (three builds, byte-identical);
  rig (Paper 26.3-235 + Magic 11.2.5) clean boot, SEVERE 0,
  reserve warnings 0, 14/14 behavior checks
  (`rig-0.1.4/RIG-RESULTS.md`). Post-install: the §5.3
  controlled-cast matrix in the install packet (convicts the
  Wolf diagnosis §1 suspects; renders the Magic-side failure
  texts on real casts).
- **Same-pass surfaces owed at install:** Guide, What's New
  BOTH surfaces, tips check, menus check — itemized in the
  install packet.
- **Records:** staged at
  `~/workspace/summons-014-build/records-package/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
