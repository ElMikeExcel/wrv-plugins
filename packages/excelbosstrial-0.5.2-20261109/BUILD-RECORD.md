# BUILD RECORD — ExcelBossTrial 0.5.2 (Item 35 combined: Packet B + boss voice)

Built Nov 9, 2026 under the reconciliation of record
(`~/workspace/seal-fight-packet-b-20261109/RECONCILIATION.md`,
25,446 B / sha256 7068a663b7bd24082d53b49a01c8953b58da05f21cfe3248f3bf8aef708920bd).
**Version note:** 0.5.2 supersedes both the built-but-uninstalled
0.5.1 (Packet B) and the designated-but-never-built 0.5.0 (voice
lane). Neither predecessor ever lands; the number gap is the
reconciliation's §6 ruling, not a missing build.

## Candidate jar (FINAL)

- `~/workspace/bosstrial-052-build/ExcelBossTrial-0.5.2.jar` —
  **438,791 B / sha256
  1a866a54840dc510ad78cdbd384b75d49f82a25808a74724a0b4fc1e66f0944b**
- Build ID: **EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a**
  (fresh tag; 0.5.1's a3aa16 never reused).
- ⚠ **Stale-hash flag for the supervisor's close-out:**
  `PACKET-DRAFT.md` and `SOURCE.md` (build folder AND records
  package copies) still cite the superseded dev jar
  (438,778 B / sha256 d470311bce7df625b8306262f154c3eb7d222391d
  48ba1ed06c8bae34ea6c416) in their candidate lines. The dev jar
  was replaced by the final jar above when the rig-run-1 defect
  was fixed (below). Those two candidate lines must be corrected
  to the final jar identity before the install window — the
  window's hash gate will (correctly) STOP on the stale hash.

## Base proof

- Tree copied tree-identical from the built 0.5.1 tree
  (`~/workspace/seal-fight-packet-b-20261109/bosstrial/`), itself
  proven against the landed 0.4.1 (411,565 B / sha256
  f7376dc93f86e866d7e1fa5553d5e66a09e41fdd0ca383682f48d32900
  da20f1).
- Pristine `package.sh` rebuild of the untouched copy is
  byte-identical to the 0.5.1 jar: 422,926 B / sha256
  fff74246500fb13b7e1173ea29b24a7fefd350f4d0256562b93f3d1dc850a652
  (`work/base-pristine.jar`, log `work/base-build.log`), with
  its suite at 0 failures before any 0.5.2 edit.

## Tests

- **27,557 checks / 0 failures / 86 harnesses** (Packet B tally
  method; logs `work/pkg1.log` + `work/pkg2.log`): the 27,468
  carried Packet B checks at 0 failures, plus 89 new voice
  checks — BossVoiceTest 52/0, VoiceBookTest 24/0,
  PluginYmlParseTest +13 (37 total: 0.5.2 version pin + voice
  config-arming pins).

## Deterministic double-build

`package.sh` twice → byte-identical: `work/pkg1.jar` =
`work/pkg2.jar` = the candidate above (438,791 B /
1a866a54…). Jar contents verified: `excel-build.txt` with the
build ID, all Packet B classes, and all voice classes
(BossVoice, VoiceBook, VoiceSettings, DragonChapter$VoiceSink).

## Fingerprint verification (release gate)

All three Layer B homes carry the new ID, verified in the
built jar: (1) `excel-build.txt` at the jar root (build-id
EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a, sku / version 0.5.2
/ channel WRV / build-date 2026-11-09); (2) the
fragment-assembled constant in `core/BuildInfo.class` (contains
the 13447a fragment; the class carries NO a3aa16 — that tag
survives only in a BuildInfo javadoc sentence recording it was
not reused); (3) the boot surface line
`Build: EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a`, observed
in every rig boot log below. Layer C semantic markers carried
unchanged; categories + locations in `FINGERPRINTS.md`.

## Rig-run-1 defect + fix (the double-open)

- **Defect (dev jar, rig run 052, `work/rig-console-052.log`):**
  the dragon **open line fired TWICE** for one battle —
  09:49:57 (battle registration) and again 09:50:07 (the first
  slay hit). Run 052b reproduced it (09:55:32 + 09:55:43).
  Root cause: two open seams were wired for the dragon — the
  chapter's registration open AND the host's generic
  first-attributed-damage open.
- **Fix:** the host open seam now skips ENDER_DRAGON entirely
  (the dragon's chapter owns its open, spoken at battle
  registration; a host open on top would speak the line twice —
  the guard comment ships in `ExcelBossTrial.java`). Change
  landed 10:00; the final jar (pkg1/pkg2, 10:01–10:02) carries
  it. This is also why the final jar differs from the dev jar
  the draft documents were written against (see the stale-hash
  flag above).
- **Rig proof of the fix:** run 052e below — open grep count
  for the whole run = **1**, flip count = **1**.

## Rig proof (local Paper 26.3 rig, final jar in place)

Rig state problem the earlier lane left, and its resolution:
the copied rig's dragon was stuck in vanilla **Phase.DYING**
at partial HP (battle previouslyKilled=false,
respawnPhase=NONE — a live battle dragon in a saved dying
phase), and the RigDriver slay loop cannot damage a DYING
dragon (runs 052–052d: HP frozen at 450/189/149/30). A rig-only
prep plugin (`work/PrepPhase.jar`, source
`work/prepphase-src/`) diagnosed it and rescued it:
`setPhase(CIRCLING)` sticks, crystals 0, HP held at 30
(diagnostic log `work/rig-console-prepdiag.log`). The verdict
run then ran with the dragon's BossTrial battle state wiped,
so the battle registered FRESH in the verdict boot. A second
rig-only probe (`work/VoiceProbe.jar`, source
`work/voiceprobe-src/`) fires the two Bukkit events that are
the only doors to the wither-taunt and warden-boom seams
(WITHER-regain; SONIC_BOOM damage) through the real event
pipeline. Both helpers are rig-only and were removed from the
rig's plugin folder after the runs; the rig was restored to
the Packet B driver set + the original dragon script.

### Run 052e — dragon verdict run (`work/rig-console-052e.log`)

Packet B items re-run unchanged (RECONCILIATION §7.4):

- **Boot clean** ✓: `Enabling ExcelBossTrial v0.5.2`; Build
  line (above); damage-feedback line shows
  `display scale x1000.0 (feedback.display-scale)`; sprinkle
  line shows `ender_dragon ARMED (cadence 180s, cap 40)
  (phase 2 only)`; Boss voice line names all four chapters
  ARMED (10/7/7/8 clips); **SEVERE 0** in the captured log.
- **Registration + open** ✓: `Two-phase trial registered in
  rig-world_the_end: dragon 0425ad63…, phase 1 at 450 HP`
  (10:35:04), and the open voice line fired **exactly once**
  (see fix proof). Caveat, stated plainly: the registration
  fired before the fake player entered the End, so the open
  line's audience read 0 participants; participant delivery
  for the dragon is proven by the flip/beat/verdict lines
  below (1 participant each).
- **Flip → re-light** ✓: `PHASE FLIP in rig-world_the_end:
  … dragon restored to 900 HP`; `Crystal re-light in
  rig-world_the_end: 4 end crystal(s) re-lit on bare towers
  (30 tower(s) found, 0 trial crystal(s) already live,
  ceiling 6)`. Observer (PacketBRigDriver): FLIP OBSERVED at
  tick 175; **crystals after flip+60: 4**, on four distinct
  towers, showingBottom=false.
- **Dragon heals from the re-lit crystals** ✓: two
  `BossTrial boss heal [The Ender Dragon]: +1.0 pool HP
  (ENDER_CRYSTAL)` lines in phase 2 — the re-lit crystals are
  real, not props.
- **Phase-2 milestone waves** ✓: `wave fired (milestone) -
  4 elite(s), 4/40`, then 8/40, then a wave trimmed to 2 at
  the max-alive-10 cap (10/40); observer counted 8
  sprinkle-tagged endermen at flip+200. No wave fired in
  phase 1 (all wave lines are post-flip; note this run's
  phase 1 lasted one hit by design — the pre-wound — while a
  full-length phase-1 absence was Packet B run-8's evidence).
- **Voice seams in this run** ✓: flip line **exactly once**,
  inside the flip sequence beside the re-light line;
  phase-2 beats beat-75, **beat-50 → slug beat50p2** (the
  phase-2 line, not beat50), beat-25; **addwave** line on
  each of the three milestone waves; **verdict** line at the
  death close (10:35:31). The fight's verdict itself read
  DENY_REPEAT_WINDOW — a rig artifact (RigPlayer is inside
  the 24 h repeat window from this rig's earlier kill of the
  same dragon); the death close and its voice line fired.
- Run tail note: the script's trailing second slay kept
  working the re-registered dragon (the Packet B run-8 tail
  shape) when the runner session ended at 10:39:39 and its
  orphaned server was terminated; every verdict item above
  precedes that point (verdict at 10:35:31).

### Phase-1 dragon beats (final jar, run 052c)

`work/rig-console-052c.log` (final jar, restored battle):
beat-75 and beat-50 voice lines fired at the first slay hit
(10:09:21), to 1 participant. Phase-1 **beat-25** was not
separately observed (052c's dragon then stuck at 149 HP in
the DYING artifact; 052e flipped on its first hit) — listed
under NOT RIG-PROVEN below.

### Runs 052f/052g/052h — chapter voice seams (rig-world)

- **Wither taunt** ✓ (052g, clean): after a purge of 6 stale
  rig boss entities, the fresh wither's encounter was opened
  by a real hit; a WITHER-regain probe event produced the
  chat subtitle `"MORE."` (eat #1) AND
  `wrv:bosstrial.wither.eat_01` at the boss to 1 participant.
  A second probe 1 s later (inside the 25 s cooldown)
  produced **no** taunt and **no** voice line — cooldown
  silence proven. Index alignment is rig-visible twice:
  line index 0 "MORE." ↔ eat_01 (052g), and in 052f a natural
  in-fight eat spoke `"More meat for the storm."` (index 8)
  ↔ eat_09; both pairs match the bundled config's
  index-aligned taunts/taunt-sounds lists.
- **Elder open** ✓ (052f/052g/052h): `elder.open` at the
  first attributed hit, to 1 participant.
- **Elder beats** ✓: beat-75 + beat-50 (052g), beat-75 +
  beat-50 + **beat-25** (052h), each at the boss to
  1 participant.
- **Elder verdict** ✓ (052h): pool spent (10,932.8 dealt of
  10,900, 100.3%), fight record verdict=ELDER_PROGRESS 1 of
  3, killer=RigPlayer, and `elder.verdict` fired at the
  close to 1 participant.
- **Warden open** ✓: 052g/052h at the boss to 1 participant.
  (052f's open read 0 participants because that warden had
  fallen 45 blocks and the opening hits were out-of-band —
  the give-up record in 052f shows player-damage 0.0; the
  in-band re-runs are the verdict of record.)
- **Warden boom** ✓ (052f): first SONIC_BOOM probe →
  `wrv:bosstrial.warden.boom` at the boss to 1 participant
  (victim included in audience); a second boom 2 s later
  produced **no** line — the taunt-cooldown rate limit is
  rig-proven.
- **Warden beats** ✓: beat-75 + beat-50 + beat-25 (052g and
  again 052h), to 1 participant.
- **Warden verdict** ✓ (052h): pool spent (7,234.6 of 7,200,
  100.5%), killer=RigPlayer, `warden.verdict` fired at the
  death close to 1 participant. The verdict itself read
  DENY_PRESENCE (champions-standing 0 — the fake player does
  not satisfy the presence rule); the close and its voice
  line fired.
- **Display-scale rendering beyond the boot line** ✓
  (incidental, 052g): the driver's reflective probe read the
  live boss-bar title `Elder Guardian — 2,803,848 /
  10,900,000` — the ×1000 scale applied to a live title
  (pool 10,900 → 10,900,000). This narrows §7.6's owed item:
  title rendering is now rig-observed; action-bar rendering
  was read in the same probe (per-hit text present).

### NOT RIG-PROVEN (explicit — not passed, not closed)

1. **Elder trio relay voice line** (relay spoken WITH its
   broadcast) — RECONCILIATION §7.6 keeps this owed,
   unchanged: it needs three fake-player-opened sibling
   elder encounters on one monument. The 052h elder kill
   advanced a solo monument to progress 1 of 3 with no
   sibling encounter open, so the relay seam correctly did
   not fire. Unit-proven only (TrioRelayTest +
   VoiceBook/VoiceSettings resolution tests).
2. **Elder lastdoor line** — same trio dependency (the
   progress-2 moment); never reached in these runs.
3. **Dragon cadence path** over a sustained 180 s phase-2
   encounter — §7.6, unchanged (the milestone path, same
   service and anchor, is proven in 052e).
4. **Dragon phase-1 beat-25 voice line** — not separately
   observed (see 052c note). Phase-1 beat-75/beat-50 and
   all phase-2 beats are proven.
5. **Warden boom_alt alternation** — only one boom was
   spoken (the rate-limit proof consumed the second event
   silently by design); alternation is unit-proven
   (VoiceBookTest), not rig-observed.
6. **Subtitle identity at non-taunt seams** — the taunt
   seam's chat/voice line pair is rig-observed identical;
   for open/beat/verdict seams the rig surface is the play
   call's event id (a headless rig cannot hear), and text
   identity rests on the unit tests + the DESIGN §5 line
   lists the config was generated from.

Player-proof items (§7.7–7.10: Java/Bedrock hearing the
clips positionally, pack-merge regressions, chapter
re-acceptance, V-1 proper) remain post-install family-session
work, unchanged by this record.

## Install gate (from PACKET-DRAFT.md, unchanged)

The combined window's gate is: live jar = **0.4.1**
(411,565 B / sha256 f7376dc9…) under its live filename, and
**Packet A landed first** (its config state GATE-VERIFYed at
the window). If live has moved off 0.4.1, STOP and
re-reconcile. BACKUP FIRST + cap-at-6, emptiness gate, marker
discipline, and the two frozen pack legs are all as filed in
PACKET-DRAFT.md.

## Records package verification

Package: `~/workspace/records-catchup-20261103/packages/
excelbosstrial-0.5.2-20261109/` — **does NOT verify as
staged** (`sha256sum -c SHA256SUMS`: 134 OK / 29 FAILED /
2 missing). Precise findings:

- The package's top-level **PACKET-DRAFT.md, SOURCE.md and
  FINGERPRINTS.md are byte-identical** to the build-folder
  copies (no refresh needed); **BUILD-RECORD.md was absent**
  and has been added from this record (the 0.5.1 package
  carries its BUILD-RECORD.md at package root).
- **One genuinely stale content file:**
  `src/com/winkreyes/excelbosstrial/ExcelBossTrial.java` —
  package copy sha256 2184d78c1ab9e77171ab452ea96f2a5f56a430
  0a763d6cb640387cc55a73d355 vs final build tree
  f555a73be4006ede6e95319e152626419b38040a535fde37e686028462
  38469c. The package was staged (09:56) before the
  double-open fix landed in the host file (10:00); the
  package therefore documents the PRE-FIX source.
- **28 further failures are stale sums, not stale files:**
  for each, the package file on disk is byte-identical to
  the final build tree; the recorded sum in SHA256SUMS does
  not match it (sums were generated from an intermediate
  tree state).
- **2 phantom entries:** `src/…/core/SprinkleRoller.java`
  and `tests/…/core/SprinkleRollerTest.java` are listed in
  SHA256SUMS but exist in neither the package nor the final
  tree (the roller was refactored into SprinkleCadence
  during the build).
- **Required close-out (supervisor/records lane):** refresh
  the package's ExcelBossTrial.java from the final tree and
  regenerate SHA256SUMS (+ FILE-LIST.txt /
  SOURCE-PROVENANCE.json / the diffs, which derive from the
  same intermediate state) before publication. This record
  lane did not rewrite them: its refresh charter covered the
  four docs only, and the provenance files must be
  regenerated by the records tooling, not hand-edited.

## Rig environment notes for the next driver

- A copied rig can carry a dragon saved in Phase.DYING with
  a live battle (previouslyKilled=false): the slay loop
  cannot hurt it. `work/PrepPhase.jar` (source beside it)
  rescues it in place (setPhase(CIRCLING) + crystal clear +
  pre-wound); a fresh battle registration additionally needs
  the chapter's `dragon/state.yml` worlds entry wiped.
- The rig world accumulates stale boss entities from old
  runs (6 purged before 052g); purge before attributing any
  "first entity of type" probe.
- Warden summons fall off the rig platform: `bosstp` the
  party to the warden before opening hits, or every hit is
  out-of-band (052f's give-up is the specimen).
- Elder/warden verdicts need full ledger pools dealt
  (10,900 / 7,200 at ~43–45 credited per governed hit —
  ~250 / ~165 `bosshitby 45` steps); `bosskill`'s raw
  100,000 is governed down to one capped hit and will not
  finish a fresh boss.
