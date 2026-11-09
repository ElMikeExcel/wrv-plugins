# PACKET — ExcelMagicGate stage-template refresh (parent-backlog Item 41)

**Built:** Nov 9, 2026, desk build lane (board-supervisor supervised).
**DESK ONLY** — no panel access, no installs, no MANIFEST / parent-backlog /
board-manager-state / daily-log edits. The original tree
`~/workspace/magicgate-floor-build/` was read and copied, never modified
(verified after the build: its stage spells still hash to the stale-template
values below).
**Spec:** `~/workspace/magicgate-template-check-20261109/VERDICT.md` §5.2.
**Packet folder:** `~/workspace/magicgate-template-refresh-20261109/`
(refreshed source tree in `ExcelMagicGate/`, jar candidate at packet root).

## 1. Scope

Exactly VERDICT §5.2 (a)–(d): the jar/source stage templates are brought up
to the Nov 8 composed-install finals so any future extraction, data-folder
reset, or fresh install is safe. **No Java code changes** — the only source
edits are the version/build-ID stamp (0.2.0 → 0.2.1, see §4) and the two
test assertions that encode the refreshed packet values (§5).

## 2. File changes (before = copied 0.2.0 tree, byte-identical to the original)

### spells.yml — REPLACED byte-for-byte with the composed-install finals
Source: `~/workspace/spell-audit/packet-resurrection-20261108/staged-filed/`

| File | Before | After |
|---|---|---|
| stage2/spells.yml | 49,239 B / `2bc1a64858e710dde102d6d456a45695bb514ed906e9335895aa621e6824afb0` | **90,312 B / `8bb39f04513df0ee43d29de42268fd8f769f3e776af113ecd2dc17775b121921`** ✓ matches task/VERDICT value |
| stage3/spells.yml | 97,209 B / `01037ece9914c67a81a1d52d1d8bef9e22b077b9cfd838be5cf6f0f8b2cc68ca` | **146,756 B / `70c568b2f83503118e9d5093813306f3454081fb363e8dfc22dada10893b5d11`** ✓ |
| stage4/spells.yml | 107,725 B / `36ea9c878e6de8191a2d9d2988ee7e207520e7257111006dbf6f58383185f64d` | **189,295 B / `32f1f91a217f197ea2f523ece6bf55655ce325eeff7dc1f8a71701b21044b291`** ✓ |
| stage5/spells.yml | 150,186 B / `d6e245fbb8678c64c5382b4caa14e157a6b4d79014d5df77bf2784fb7abbfd46` | **189,295 B / `d34112ba181af482d6faf8d58b4e2cd53dff10a9969f76b4fd06c194960cb355`** ✓ |

**stage0 / stage1 spells.yml: UNCHANGED** (28,704 B /
`565aa5b9d648d908ef0fca57a4d8f91a04b7c8fe22076a1ae4314e60df1d580b` each) —
pre-book files, left per VERDICT §5.2a ("stage0/stage1 spells are pre-book
files, leave unless a packet says otherwise"; no packet says otherwise).

### grants.txt — refreshed (VERDICT §5.2b), existing format preserved exactly
Every added line is the file's own format
(`lp group default permission set magic.cast.<spell> true`), grouped in one
comment-headed "Template refresh Nov 9, 2026" block per file, placed at the
end of the DEFAULT-group section (stage4: before the BUILDER block, which is
untouched). Header count comments were updated to the new totals (stage5's
header also corrected: it claimed "102 path spells"; the file actually
carried 100).

| File | Before | After | Grants |
|---|---|---|---|
| stage2/grants.txt | 2,704 B / `d9b68796a2c525b6910668b1fde32b4c2f173fdb21141b74aaf610eb3c28635d` | 3,400 B / `99586ce677087e10fab7b4f87bdf97507b7e662da454a1050667e6993e328f30` | 33 → **40** (+7) |
| stage3/grants.txt | 4,144 B / `b60a90cf55625123aad9bfecd006d11355cba1fe827d30a3a254167cbf28dd12` | 4,939 B / `db81bd1a7e9521ea03f2fefbf6328b1f9120a947a4d5bfd974b2a77513889cb6` | 59 → **68** (+9) |
| stage4/grants.txt | 5,020 B / `f90db94ef281b5a62400b006ccdcf54597c35524ae857c7d12ef41b4fc80f7ee` | 6,267 B / `181501745b669d6eaaa0c1d15023407b269de1c0b893af071719b7a145672af2` | 67 → **82** (default 59 → 74, +15; builder 8 unchanged) |
| stage5/grants.txt | 6,768 B / `75237989bc60086d3baeacc6c85399cc70ddb00e534d52a8ad4ca7cbe7458bb8` | 7,947 B / `5ef5a6485948ab4e1cb76d19e2ff432b31f8fe6dd7d22ffa7167490bcb0b80e2` | 100 → **115** (+15) |

**What was added, per stage (cumulative by the packets' own placements):**
- **Stage 2 (+7):** `mend`, `flashheal`, `greaterheal`, `ward` (healer kit —
  present in the stage-2 book; healing packet §2: "the healer release
  (stage 2) already carries … Heal/Flash/Greater/Mend"), `archer` (summons
  packet: stage 2), `wrv_spark`, `wrv_frostbite` (WRV customs, in the
  stage-2 book).
- **Stage 3 (+9):** the stage-2 set + `bruiser` (summons packet: stage 3),
  `grouprenewal` (healing packet §2: "Placement: stage 3").
- **Stage 4 (+15):** the stage-3 set + `guardian`, `revenant`, `ghast`,
  `mount` (summons packet, re-tiered to stage 4 by Mike's correction),
  `layonhands` (healing packet §3: add to "the MagicGate stage-4 grant
  set"), `resurrection` (resurrection packet §1/§2: stage-4 unlock,
  stage-4 grant set).
- **Stage 5 (+15):** the same cumulative 15 — `resurrection`, `layonhands`,
  `mend`, `flashheal`, `greaterheal`, `grouprenewal`, `ward`, `archer`,
  `bruiser`, `guardian`, `revenant`, `ghast`, `mount`, `wrv_spark`,
  `wrv_frostbite` — exactly the set VERDICT §5.2b names. `stormshackle`
  tiers `|2`–`|4` need **no** new line: rank variants share the existing
  `magic.cast.stormshackle` node, already granted at every stage.

**What each stage deliberately does NOT carry (grants-completeness check):**
the refreshed spells books contain *definitions* for spells whose unlock
sits higher (e.g. stage2's book defines `admission`, `arrow`, `eat_food`;
stage3's defines `boomerang`, `throwsword`, `reflect`; stage4's book is
key-identical to stage5's). Definitions are not unlocks — the unlock record
in this design is paths + wands + grants, which agree exactly at every
stage (stage2 paths/wands 33 = old grants 33; stage3 59 = 59; stage4
59 paths + 8 masons-wand = 67; stage5 100 = 100 — all verified this run).
No Nov 8 packet moved any of those spells' unlock stage, so they are NOT
granted below their designed stage: stage2 does not grant the arrow family
(stage 3) or the stage-5 ladder spells in its book; stage3 does not grant
the stage-4 set or stage-5 ladder spells in its book; stage4 does not
grant the 33 stage-5 path spells (they unlock at stage 5; the summons
packet's "everything gets released on stage four" correction applied only
to `revenant`/`ghast`/`mount`, which ARE granted at stage 4 above).
Stage0 (0 grants, dormant by design) and stage1 (13) are unchanged.

### paths.yml / wands.yml — VERDICT: NO EDIT (verified, not assumed)
Diffed stage5 `paths.yml` and `wands.yml` spell lists against the
live-final (stage5) spell list: paths carry 100 spells, wands the same 100
(the old grants set, exactly). **No Nov 8 packet places any new spell on a
path or a wand** — the spells packets are spells.yml-only deltas, and the
two packets that name a learnability mechanism name *grants* as that
mechanism (healing §3 layonhands, resurrection §1.3). The new spells'
`|rank` tiers gate on path *rank* + cast counts (e.g. grouprenewal|2
"student path, 50 casts"), not on path membership. Adding them to
paths/wands would redesign the ladder (rank placement, `spell_count`,
wand lists) on no packet's authority. **Verdict: paths.yml and wands.yml
stay byte-identical to 0.2.0 at every stage; grants are the placement the
packets designed.**

## 3. Jar candidate

- **File:** `ExcelMagicGate-0.2.1.jar` (packet root)
- **Size / SHA-256:** 192,747 B / `7110bfdcff48ef1bb6f68ff779276eb25a1118680ccaddbe6dc629fdc8121c5a`
- **Deterministic double-build: PASS** — two clean builds byte-identical
  (same hash), via the copied tree's `build.sh` (original method: JDK 25
  javac, `--release 21`, pinned zip timestamps, sorted entries).
- **Embedded-template verification: PASS** — the built jar's
  `stages/stage{2,3,4,5}/spells.yml` hash to the four finals in §2 and its
  `stages/stage{2,3,4,5}/grants.txt` hash to the refreshed grants; jar
  `plugin.yml` reads version 0.2.1 and `excel-build.txt` carries the new
  build ID.
- **Version stamp: bumped to 0.2.1** (the tree's versioning supports it:
  `plugin.yml` version, `excel-build.txt`, `BuildInfo.VERSION`).
  New build ID: **`EXC-MAGICGATE-0.2.1-WRV-20261109-01-d34112`** (suffix =
  stage-5 final hash prefix). Stamped in `plugin.yml` (version),
  `excel-build.txt`, `BuildInfo.java`, and the copied `build.sh` header /
  default output path (now this packet folder — the copy no longer points
  at the original folder).

## 4. Tests + harness

**Unit suite** (`ExcelMagicGate/run-tests.sh` in the copied tree; output in
`test-output.txt`): **passed=158, failed=0** — same 158 checks as 0.2.0.
Two assertions were updated in the copy to encode the refreshed packet
(they assert packet *values*, so a packet refresh must move them; both
edits are commented in the test source):
1. grant counts `{0, 13, 33, 59, 67, 100}` → `{0, 13, 40, 68, 82, 115}`;
2. build-ID prefix `EXC-MAGICGATE-0.2.0-WRV-20261108-01-` →
   `EXC-MAGICGATE-0.2.1-WRV-20261109-01-`.
No other test was touched. (Stage4's builder-group pre-command assertion
still passes: the builder block is untouched.)

**Spell logic harness** (`~/workspace/spell-audit/logic-tests/spell_logic_tests.py`,
full output in `harness-stage{2,3,4,5}.txt`):

| Template | Findings | CRITICAL | HIGH | Check 8 | Check 10 | Check 12 | Check 13 |
|---|---|---|---|---|---|---|---|
| stage2 | 123 | **0** | 5 | 0 | 0 | 0 | 0 |
| stage3 | 225 | **0** | 5 | 0 | 0 | 0 | 0 |
| stage4 | 278 | **0** | 6 | 0 | 0 | 0 | 0 |
| stage5 | 278 | **0** | 6 | 0 | 0 | 0 | 0 |

**Gate (VERDICT §5.2c): CRITICAL 0 and checks 8/10/12/13 = 0 on stage5 —
PASS** (and on every stage). Finding counts match the resurrection
packet's filed harness numbers exactly (123 / 225 / 278 / 278); the HIGHs
are the known check-1 sustainability set the VERDICT records for the
live-final book.

## 5. Notes the installer must carry (VERDICT §5.2d + supersession)

- **A jar refresh alone changes NOTHING live.** `extractStages()` copies
  jar templates into the data folder *only if the target does not already
  exist* (missing-only). The live server's data-folder stage files are
  untouched by a jar swap; this packet's purpose is future extraction /
  reset / fresh-install safety only. If the server data-folder files ever
  need refreshing, that is a separate explicit file placement, never a
  side effect of this jar swap.
- **Supersession:** the 0.2.0 BUILD-RECORD claim "Stage packets
  byte-identical to 0.1.0 — PASS (42/42)" is **now false by design** for
  this build: **8 of the 42** packet files differ — spells.yml stage2–5
  (4 files) and grants.txt stage2–5 (4 files); the other 34 — all
  stage0/1 files and every classes/config/messages/paths/wands file —
  remain byte-identical to 0.1.0. The records package
  for 0.2.1 must carry this supersession in writing; the 0.2.0 record
  itself is not edited by this lane.
- **Not on the Nov 14 critical path.** The Nov 14 stage-5 flip reads the
  server data-folder files, not this jar (VERDICT §0/§3). The pre-flip
  gate stands on its own: fresh panel hash of
  `plugins/ExcelMagicGate/stages/stage5/spells.yml` must read
  189,295 B / `d34112ba…` before the Warden attempt.

## 6. Install instructions (parent hand window — jar swap only)

1. **Panel coordination:** create
   `~/workspace/wrv-install-queue/WINDOW-IN-PROGRESS.txt` at window start,
   delete at finish; check first for a foreign marker / in-flight failsafe.
2. **Live-jar gate (STOP if mismatch):** the live jar must read
   **ExcelMagicGate-0.2.0.jar — 173,699 B / SHA-256
   `2e576bf4055905ac1b76b5550e3c5efe6f88df8fe1b2a4c7f222a30a96fcfa4f`**
   (panel-verified at the 0.2.0 install, Nov 8–9). Any other size/hash =
   stop, re-verify before touching anything.
3. **BACKUP FIRST + cap-at-6:** take a fresh Shockbyte backup before the
   swap; before creating it, prune the OLDEST manual backups (named in the
   install report) until the post-creation total is ≤ 6; never delete the
   Daily Backup's newest copy or the two newest manual restore points.
4. **Emptiness gate:** swap only with the server verified empty
   (player list = 0, twice).
5. **Swap:** rename the outgoing jar to a `.bak` name (0.2.0 pattern:
   `ExcelMagicGate-0.2.0.jar.bak-<date>-pre021`), place
   `ExcelMagicGate-0.2.1.jar` (192,747 B / `7110bfdc…`), panel-verify its
   size + hash after placement. **No config merge** — 0.2.1 changes no
   config (the 0.2.0 floor-reconcile section is already live); no
   data-folder file placements in this window.
6. **Boot-verify lines (all required):**
   - `Enabling ExcelMagicGate v0.2.1` with build line
     `EXC-MAGICGATE-0.2.1-WRV-20261109-01-d34112`;
   - **`stage 4 of 5`** (applied stage unchanged; seals 4 of 5 — no apply
     may fire on this boot);
   - **the floor line** — `/magicgate status` reads
     `Stage floor: master (pool 200, regen 35/s) | Floor reconcile: ON`
     (same wording as the 0.2.0 boot, `INSTALLED.txt` Nov 9);
   - SEVERE 0; watch sweeps firing.
   Any apply firing, stage change, or missing floor line = rollback to
   the `.bak` jar and report.
7. **Close-out (same pass, parent):** records package + Drive deposit +
   draft record PR for 0.2.1 carrying the §5 supersession note; MANIFEST
   INSTALLED conversion. No player-facing surfaces change (no new
   behavior — templates only), so Guide/website/What's New need no edit;
   state that explicitly in the install report rather than skipping it.

## 7. Failures / gaps

None. Every step completed: spells hashes exact (4/4), grants refreshed
(40/68/82/115), paths/wands verdict recorded, jar built deterministically,
unit suite 158/0, harness gate PASS on all four templates, SHA256SUMS
verified with `sha256sum -c` (see file). No rig run was performed — this
packet changes no code paths, and the 0.2.0 rig verdict stands for the
unchanged code; a boot proof happens at the install window (§6.6).
