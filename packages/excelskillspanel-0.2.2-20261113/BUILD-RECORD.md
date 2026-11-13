# ExcelSkillsPanel 0.2.2 — BUILD RECORD (re-dispatch lane)

**Work folder:** `~/workspace/skillspanel-022-build-20261113/`
**Date:** Nov 13, 2026 (America/Chicago). **Lane:** re-dispatch build lane,
dispatched by the board supervisor under its supervision rule (re-dispatch
once with the failure context included).
**Items:** 12 (remove the "Class: Mage" line) + 13 (panel header must show
the ACTIVE/HELD wand's values). **Diagnosis:** `DIAGNOSIS.md` in this folder.

## 1. What the prior lane left behind (unverified on arrival — trust nothing on disk)

- `ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar` + `-repro.jar`, 61,245 B,
  built 13:11 CST — **STALE, discarded as evidence.** Source files were
  edited AFTER that build (`MagicApiReads.java` 13:39, `MagicLines.java`
  13:40, `MagicHeaderProvider.java` 13:41, `PanelTest.java` 13:41 CST),
  after the rig run (13:19 CST). The prior lane stopped on a cron timeout
  with "fixes implemented in source, NOT yet built or rig-proven."
- `rig/phase-a-results.txt` (13:19 CST): SUMMARY pass=64 fail=7 — the
  seven failures this record resolves in §2.
- The source as left behind **did not compile its own tests**: the 0.2.2
  `PanelTest` calls `MagicApiReads.wrvDisplayName(...)` /
  `wrvNextKey(...)`, which were package-private — 13 compile errors on
  the first rebuild attempt (§3, fix F0).

## 2. The 7-failure resolution (the prior run's 64/7 → this run's 71/0)

The 7 failures were 3 distinct mismatches, each in the HELD scenario's
`mana-panel-chain` check plus the NOT-HELD and TWO-WAND twins
(`-next-master`, `-mana-150-25`, `-header-lines` × 2 scenarios, plus the
held `mana-panel-chain`):

### 2a. "Next rank: Wizard" — the CODE was already fixed; the JAR was stale (expectation CORRECT, code fix unbuilt)

- **Evidence that "Wizard" is real:** Magic 11.2.5's own
  `defaults/messages.defaults.yml` (inside
  `~/workspace/magic-scratch/downloads/Magic-11.2.5.jar`), line 1034:
  "Survival Progression: &6Beginner&f, &6Student&f, &6Apprentice&f,
  &6Wizard &7(&fAKA &6Master&7)". The same file's mana-progression note
  (line 733) names the path keys "student,apprentice,master". So
  `Wand.getNextPath()` on an Apprentice wand correctly returns the master
  path — whose **display name in Magic is "Wizard"**. "Wizard" is a real
  Magic rank name, not a wrong lookup.
- **Why the panel must still show "Master":** the WRV ladder has exactly
  four ranks (MagicGate `RankFloor`: beginner → student → apprentice →
  master), and MagicGate's own wand lore writes "Next: Master — 785 to
  go". The panel matches the wand lore and the WRV ladder, never Magic's
  chain name.
- **Resolution:** NO new code change — the WRV-order fix
  (`MagicApiReads.WRV_RANKS`, `wrvDisplayName`, `wrvNextKey`; rank/next
  never from `getNextPath()`) was already in the source the prior lane
  left behind. The 13:19 rig ran the **stale 13:11 jar**, built before
  that fix landed in source. Rebuilding from current source resolved it:
  the new rig reads `next=Master` in all three scenarios. The rig
  expectation ("next rank must stay Master") was CORRECT throughout and
  is unchanged.

### 2b. "Mana: 154 / 150" — the RIG EXPECTATION was wrong (the panel's numbers were right)

- **What the panel showed:** current mana 154.0, frame max 150, regen 25.
  The rig expected 150 / 150 / 25 (the wand lore's static frame).
- **Evidence the expectation was wrong — Magic's own live numbers:**
  - The 13:19 run's own HELD chain: `mage.getMana()=154.0,
    effMax=154, effRegen=25`. Magic itself holds the player's mana at
    **154, above the frame 150**. The wand's mana regenerates (25/s)
    toward the EFFECTIVE pool, not the frame.
  - The wand lore's "Mana: 150" is the STATIC frame; the live mana bar
    in-game is the effective pool. A panel showing "150 / 150" while the
    player actually has 154 mana would understate the real pool — and
    "154 / 150" (current over max) is internally inconsistent.
  - Formula check: frame 150 × (1 + 3.2095%) = 154.8 → truncated 154,
    exactly `mage.getEffectiveManaMax()` in the held state. The
    provider's `MagicLines.effectiveManaMax` formula and Magic's own
    effective max AGREE.
- **What the panel must show (and now shows):** the EFFECTIVE pool —
  live current mana / effective max, regen likewise: **"Mana: 154 / 154,
  Mana regen: 25"** at rig level 136 (boost 3.2095%). The Boosts line
  already explains the +3.21%; this is not double-counting.
- **Mike's pin is preserved:** at Mike's Magic Level 26 (boost 0.5943%),
  the same formula gives (int)(150 × 1.005943) = **150** — "150 / 150",
  exactly the wand lore. DIAGNOSIS.md §5's "wand's own frame" wording was
  written against Mike's level-26 numbers, where frame == effective; the
  level-136 fixture is what distinguishes them.
- **Resolution:** corrected the RIG EXPECTATION (driver), not the panel
  code — the provider's effective-pool display (frame → effective max +
  current clamped to it) was already in source. The driver now derives
  the expected display from the live boost placeholder through the
  provider's own formula and asserts the header carries it.

### 2c. The stale-reference trap (why the first re-run still failed 2/71)

- The first rebuild + re-run (phase A/B: 69 pass, 2 fail) still failed
  the two NOT-HELD mana checks — because the corrected driver used
  `mage.getMana()` / `mage.getEffectiveManaMax()` as the live-mana
  reference, and **with no wand active those reads fall back to the
  CLASS frame**: not-held mage mana 51.0, effMax 51 (beginner frame 50 ×
  1.0321 — DIAGNOSIS.md §3, proven live by the rig's own NO-WAND chain
  INFO line: `mana=51.0 effMax=51 effRegen=5`). The panel was right
  (154.0/150/25 from the carried wand); the reference was the class
  fallback.
- **Resolution (driver, second correction):** the live-mana reference is
  now a FRESH `controller.getIfWand(...)` wrapper on the slot-0 Renewal
  wand item (slot 0 in every scenario) — the wand itself, read in the
  same tick. The `getEffectiveManaMax()` cross-check now applies ONLY
  while a wand is active (held / two-wand states, where it reads 154);
  in the not-held state it is skipped as the class fallback, with the
  reason recorded in the check message. Decisive evidence line from the
  final run: `not-held PANEL chain: ... refWandMana=154.0 effMax=154
  effRegen=25 mageMana=51.0 mageEffMax=51` — wand 154, mage 51, panel
  shows the wand.
- The HELD `mana-panel-chain` check was likewise corrected earlier in
  this run: its reference was the stale bound-wand object captured at
  rig step 2, still carrying the .dat file's STORED mana (150.0), while
  the live mana had regenerated to 154.0. It now compares against the
  mage's live mana (valid while the wand is active) and the wand frame
  for max/regen.

### 2d. Failure accounting (7 → 0)

| Failure (13:19 run) | Cause | Fix | Where |
|---|---|---|---|
| `mana-panel-chain` (held) | stale bound-wand reference (stored 150.0) vs live mana 154.0 | driver: reference = mage live mana (wand active) | `rig/driver-src/panelrig/PanelRig.java` |
| `not-held-next-master` + twin | stale jar predated the WRV-order code fix | rebuild from current source (fix already in source) | — (build) |
| `two-wand-next-master` | same | same | — (build) |
| `not-held-mana-150-25` + twin | expectation pinned the static frame for a live quantity | driver: expectation = effective pool 154/154 | driver |
| `two-wand-mana-150-25` | same | same | driver |
| `not-held-header-lines` + twin | same two mismatches inside the composed header | driver: expected header lines = effective pool | driver |
| `two-wand-header-lines` | same | same | driver |

Plus, found during this run (not in the 7): the source as left behind did
not compile its tests (F0, §3).

## 3. Build

### F0 — compile fix (this lane, before any build)
`PanelTest` (0.2.2) calls `MagicApiReads.wrvDisplayName(...)` and
`wrvNextKey(...)`, which were package-private → 13 javac errors on the
first rebuild attempt. Fix: made both methods `public` in
`MagicApiReads.java` (pure functions; the tests pin their contract).
No behaviour change.

### Base gate (0.2.1 rebuilds byte-identical to live) — PASS
- `base-gate/local-build.sh` run Nov 13, 2026 ~13:53 CST.
- Rebuilt `ExcelSkillsPanel-0.2.1.jar` = **57,652 B /
  `ce430d1f0166d7557e181ceae85ca3fe6a84285adaeef92787140a6493bbb45c`** —
  byte-identical to the live 0.2.1 jar (DIAGNOSIS.md live hash
  `ce430d1f…`; double-build byte-identical; fingerprint gate PASS).
- Base-gate JUnit: **33/33 tests pass.**

### 0.2.2 build — PASS
- `ExcelSkillsPanel/local-build.sh` run Nov 13, 2026 ~13:56 CST
  (after F0).
- JUnit: **38/38 tests pass** (4 containers, 0 failed).
- Double-build byte-identical; fingerprint gate PASS.
- **Candidate:** `ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar` — **62,142 B /
  SHA-256 `374ad117f2cd1676f3a68dff9ad2b3180e7d575e4457149d91bbe5e28665563e`**
  (repro jar identical hash).
- Build ID: `EXC-SKILLSPANEL-0.2.2-WRV-20261113-01-c7d2e9`
  (`excel-build.txt` + `BuildInfo` fragments verified in-jar).
- `plugin.yml` in-jar: version 0.2.2, main
  `com.winkreyes.excelskillspanel.ExcelSkillsPanel`.
- Changes vs 0.2.1 (source diff): `BuildInfo` (version), `MagicLines`
  (no Class line; credited-casts label; effective-mana helpers),
  `MagicApiReads` (carried-wand resolution; WRV rank order; F0
  visibility), `MagicGateReads` (new — MagicGate ledger + lore fallback),
  `MagicHeaderProvider` (effective-pool composition; credited casts),
  `MagicLevelStoreReads` (totalCasts removed), `PanelTest` (0.2.2 cases).

## 4. Rig results — ALL PHASES PASS

Real Paper 26.3 + Magic 11.2.5, fixture = Mike's real mage data
(pull-bm228, id/name rewritten to rig player RigCaster
`33333333-4444-5555-6666-777777777777`), seeded ExcelMagicLevel record
(total-xp 5000, total-casts 57 DECOY), seeded MagicGate ledger row
(credited_total 1,215).

- **Phase A** (no Geyser/Floodgate): **SUMMARY pass=71 fail=0**
  (`rig/phase-a-results.txt`, 16,717 B).
- **Phase B** (+ live-line Geyser-Spigot 2.12.1-b1292 + Floodgate):
  **SUMMARY pass=71 fail=0** (`rig/phase-b-results.txt`, 16,716 B).
- Driver rebuilt for this run: `rig/PanelRig.jar` (20,399 B /
  `52fa620c…`).

Key proven lines (phase A; phase B identical):

- HELD: `PANEL chain: rankName=Apprentice rankKey=apprentice
  next=Master mana=154.0 manaMax=150 manaRegen=25`; header
  `[Rank: Apprentice, Next rank: Master, Mana: 154 / 154, Mana regen: 25,
  Boosts: mana pool +3.21%, mana regen +3.21%, cast cost -1.28%,
  Credited casts: 1,215, Guild seals: 0 of 5, … 2% — 1 / 48 XP]`.
- NOT-HELD (food held, Renewal wand in inventory — the live failure
  mode): same readings; `active wand while food held: null` (Magic
  clears the cache — the 0.2.1 panel fell back to Beginner here);
  header identical to held.
- TWO-WAND (beginner + Renewal carried, Renewal held): same readings;
  `active wand with two wands carried: student`.
- No Class line on any surface (header, detail, Bedrock body, chat);
  no "Total casts" label anywhere; "Credited casts: 1,215" from the
  MagicGate ledger (the 57 decoy never appears); casts lore fallback
  parses 1,215.
- Bedrock: body carries the corrected header; gold-filled bars (§6);
  `bedrock-send-honest-false` (form send for a Java fake player
  answers false, chat fallback applies). Phase B additionally boots
  with the live Geyser/Floodgate jars present.
- Java: command opens the chest, skill click opens detail, Back
  returns, Magic header click opens the Magic detail; gold bars in
  main + detail.
- mcMMO rows verified against direct ExperienceAPI reads (Mining 25,
  120/1520 XP, power 28); all 19 rows; perk ladders + descriptions
  verified live (Item 10 sweep intact).

## 5. What 0.2.2 changes (for the packet)

- **Item 12:** no "Class: Mage" line on any surface — the field is gone
  from the DTO end-to-end (`MagicData`, `MagicApiReads`, provider).
- **Item 13:** rank / next rank / mana / regen come from the SAME
  resolved CARRIED wand (held → cached-active-if-carried → offhand →
  inventory scan, highest path wins → bound wands → class path last,
  only when the player owns no wand). Next rank from the WRV order
  (Apprentice → Master, never Magic's "Wizard"). Mana displayed as the
  effective pool (live current / effective max, regen effective); the
  Boosts line still carries the percentages. Casts from MagicGate's
  ledger, labelled "Credited casts"; the panel NEVER falls back to
  ExcelMagicLevel's `totalCasts` again (if MagicGate cannot answer, the
  line is omitted).
- **Unchanged:** Magic Level / XP bar / total XP, Boosts line, seals,
  mcMMO rows, perk ladders, Bedrock % handling, gold bars. No config of
  its own (only reads ExcelMagicLevel's live `config.yml` reward table
  for the regen/cost fractions); no data files of its own to preserve.
  Pure jar swap: outgoing `ExcelSkillsPanel-0.2.1.jar` → incoming
  `ExcelSkillsPanel-0.2.2.jar`; plugin count unchanged (84).

## 6. Files

- Candidate: `ExcelSkillsPanel/ExcelSkillsPanel-0.2.2.jar` (+ `-repro.jar`)
- `SHA256SUMS` (6/6 verified with `sha256sum -c`)
- Rig: `rig/PanelRig.jar`, `rig/phase-a-results.txt`,
  `rig/phase-b-results.txt`, `rig/boot-a.log`, `rig/boot-b.log`,
  `rig/driver-src/panelrig/PanelRig.java` (corrected driver),
  `rig/run-rig.sh`
- `INSTALL-PACKET.md`, `MANIFEST-ENTRY-DRAFT.md` (this folder)
- Base gate: `base-gate/` (0.2.1 source + rebuilt jar, hash `ce430d1f…`)

## 7. Owes / notes for the supervisor

- **G1 live pulls are NOT in this folder** (desk lane does no panel
  pulls): the supervisor must run them at filing — listed explicitly in
  `INSTALL-PACKET.md` §G1 (live 0.2.1 jar hash/size, live plugin
  versions, exactly-one-live-jar check). The packet MUST NOT be filed
  on the carried 03:54-window premise alone; the installing window
  re-pulls fresh and STOPs on mismatch.
- **Player proof owed after install (Mike, named tester):** open
  `/skillpanel` while holding food (wand NOT in hand) on Java + Bedrock
  and confirm: Rank Apprentice, Next rank Master, Mana 150 / 150,
  regen 25, Credited casts 1,215, no Class line. (At his level 26 the
  effective pool IS 150/150.) A second-wand live check is NOT owed by
  this packet — the two-wand rig fixture is a guard only; Mike disputes
  carrying two wands and the registry question is a separate supervisor
  check (fresh playerdata pull).
- **Same-pass surfaces owed (supervisor legs, not this packet):** Guide
  skills-panel chunk (rank-from-carried-wand + credited-casts wording),
  website pair, What's New BOTH surfaces (website + in-game changelog
  entry for 0.2.2), broadcaster tips check, in-game menus check.
  Record publication (wrv-plugins draft PR) is a supervisor leg — G2
  duplicate check before any push.
- No installs, MANIFEST edits, panel access, purchases, or messages were
  performed by this lane. Desk work only.
