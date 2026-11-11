# MANIFEST ENTRY DRAFT — ExcelSummons 0.1.5 (for the board supervisor to file)

Drafted by the desk build lane (Nov 11, 2026). The supervisor
verifies and files this as a PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md`. The build lane did NOT
edit the MANIFEST, the parent-backlog, or board state.

---

## PENDING — ExcelSummons 0.1.5 (archer equipment guarantee: bow + tier helmet set/verified bridge-side at registration) — packet: ~/workspace/summons-015-build/INSTALL-PACKET.md

- **Packet:** `~/workspace/summons-015-build/INSTALL-PACKET.md`
  (build record: `~/workspace/summons-015-build/BUILD-RECORD.md`;
  diagnosis: `~/workspace/goals/family-minecraft-server-build/hidden_files/archer-bow-diagnosis-20261110.md`
  — layer-1 desk check DONE Nov 10, 2026: spells config CORRECT
  at every tier, Magic's SpawnEntity layer does not apply
  item_in_hand on spawn; this packet is the layer-2 bridge-side
  guarantee; tree summary: `DESIGN-0.1.5.md` in the build tree /
  source zip).
- **Artifacts (jar only, one window):**
  - `~/workspace/summons-015-build/ExcelSummons-0.1.5.jar` —
    **65,253 B** — SHA-256
    `5cdf16a0b4178adc7e264320996f6d7f350cb14343dfdc59ae965a0bd619f61e`
    — build `EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315`
    (double-build byte-identical; clean-room + source-zip
    rebuilds reproduce the hash).
  - No config swap (the shipped config.yml is byte-identical
    to the installed 0.1.4 config — do NOT replace the live
    one), no Magic modifiers/messages merge, no data-field
    change.
- **Live-jar gate:** live is ExcelSummons-0.1.4.jar (63,642 B,
  SHA-256 a34d7bdf…25a, installed Nov 11, 2026 ~2 AM) — verify
  before the swap; STOP if it does not hash.
- **Marker path (hand window):**
  `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` — create
  at window start, delete at finish; re-check foreign markers
  before Shut Down and before Start.
- **Gates:** BACKUP FIRST + cap-at-6 (name prunees; never the
  Daily newest or the two newest manuals); emptiness `list` = 0
  twice.
- **Boot-verify lines (all must appear):**
  - `Enabling ExcelSummons v0.1.5`;
  - enable line `ExcelSummons 0.1.5 enabled (build EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315); registry holds N summon(s).`;
  - NO "could not be applied" reserve warnings; SEVERE 0
    attributable; guildgate LOCKED count unchanged; plugin
    count unchanged. ("archer equipment skipped" / "did not
    verify" lines fire only per cast as soft-fail notes — none
    expected; report any occurrence.)
- **What it adds/changes:** when the bridge registers a CAST
  archer, it now sets the skeleton's bow (every tier) + tier
  helmet (iron base, chainmail tiers 2/3) and verifies both by
  read-back — soft-fail only, in applyHarness's shape; a failure
  never breaks registration or the cast. The tier comes from the
  cast-time spell key (exact; no record field added — the
  guarantee's limit is that it applies at the registration
  moment only, which covers every archer: combat summons never
  persist across a restart). Everything else — caps, reserve,
  dismiss, messaging — is unchanged 0.1.4 behavior; no
  spells.yml value touched anywhere in this packet.
- **Proofs:** SummonsTest 74/74 (three builds, byte-identical);
  rig (Paper 26.3-235 + Magic 11.2.5) clean boot, SEVERE 0,
  reserve warnings 0, archer soft-fail lines 0, 19/19 behavior
  checks (`rig-0.1.5/RIG-RESULTS.md` — the 14 carried 0.1.4
  checks + phase F's 5 archer-equipment checks). Post-install:
  Jester's in-game archer cast (the diagnosis's named player
  proof) — bow + helmet seen, archer shooting.
- **Same-pass surfaces owed at install:** Guide, What's New
  BOTH surfaces, tips check, menus check — itemized in the
  install packet.
- **Records:** staged at
  `~/workspace/summons-015-build/records-package/` and
  `~/workspace/records-catchup-20261103/packages/excelsummons-0.1.5-20261111/`
  (SHA256SUMS-verified); GitHub draft-PR + Drive legs are the
  parent's, per PUSH-NOTES.md in the build folder.
