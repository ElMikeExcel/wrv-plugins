# Mirror Branch A — ExcelTogether 0.6.0 one-roster rebuild (Parts 2+3 candidate)

- **Run:** board supervisor run 427, worker W2, Mon Nov 16, 2026 (America/Chicago).
- **Task:** execute the mirror Branch A session (steps A1–A5) for the run-426
  one-roster rebuild candidate, on the Homebase test-server mirror only.
  No installs, no MANIFEST writes, no panel contact, live server never touched.
- **A1–A5 definition of record:** `~/workspace/board-r418/together-emdoor-scoping/SCOPING.md`
  §6, "Branch A (fix built) — all must pass", L365–390 in the run-426 citation
  (quoted in §1 below). Also carried in `~/workspace/board-r426/together-rebuild/BUILD-PLAN.md`
  (final section) and `~/workspace/board-r426/together-parts23/REPORT.md` §7 item 1.
- **Ruling under test (Mike, Nov 16, 2026, 07:09 CST):** "Both — /em party works
  as a face of the one party."

## VERDICT: BRANCH A GREEN — A1 PASS, A2 PASS, A3 PASS, A4 PASS (i+ii+iii), A5 PASS

| Step | Verdict | Boot | SEVERE | `reverted a non-door` lines |
|---|---|---|---|---|
| A1 native formation | **PASS** | NA2 | 0 | 0 |
| A2 native accept completes | **PASS** | NA2 | 0 | 0 |
| A3 rest | **PASS** | NA2 | 0 | 0 |
| A4(i) B1 formed-native revert | **PASS** | NB | 0 | 2 (one per member — the expected revert) |
| A4(ii) RT-4 decline→native | **PASS** | NC | 0 | 1 (inviter materialisation after decline — AT-1b shape) |
| A4(iii) pending-expiry revert | **PASS** | ND | 0 | 1 (at exactly +60 s) |
| A5 AT-1 regression control | **PASS** | NE | 0 | 0 |

Boot gate: **SEVERE 0 in all six boots** (NA1, NA2, NB, NC, ND, NE).
Every boot's read-back: `VERSIONS ExcelTogether=0.6.0 EliteMobs=10.9.14
mcMMO=2.3.003-WRV-XP-2-ExcelShare-1 ExcelDowned=0.1.4`.

**Gate consequence (per SCOPING.md §6):** Branch A passes on every criterion,
so this mirror gate **clears the D80 chain's mirror leg** for the rebuild
candidate `6e935f16…`. Remaining chain (not this worker): desk-gate sign-off,
fresh G1 live pull at filing time, pointer-stub packet + MANIFEST filing by
the supervisor (including disposition of the HELD/superseded old 0.6.0 entry —
the old candidate `3acf43e1…` was never staged in this session), failsafe
install on the emptiness gate.

## 1. The A1–A5 criteria (SCOPING.md §6, Branch A, quoted)

1. **A1 native formation:** B `/em party invite A` (real path only, no
   reflective fallback in the scored leg). At every second +1…+6: zero
   `reverted a non-door` lines for either player; `emPending(A)=true` at
   +3 s and +6 s; B's EM party still real.
2. **A2 native accept completes:** A `/em party accept` (real path). Within
   2 polls: Together `partyOf` shows {A, B} for both, EM `getParty` shows
   the same pair, Downed provider `membershipOf(B,A)=PARTY` and
   `partyMembersOf(B)` non-empty.
3. **A3 rest:** two further polls drive nothing and revert nothing (the
   §11C rest discipline).
4. **A4 negatives still hold:** (i) a formed native EM party with no pending
   provenance (B1 shape, reflective raw joins) is still reverted; (ii)
   hub-invite → door decline → native EM accept (RT-4 shape) still joins
   nothing and is reverted; (iii) a native pending invite left unanswered
   expires and the inviter's materialised party is reverted on the next
   poll after expiry.
5. **A5 regression control:** Session 1 mode I (AT-1) re-run on the same
   candidate still PASS.

## 2. Candidate + premise verification (before any boot)

- **Candidate:** `ExcelTogether-0.6.0.jar`, **195,872 B / SHA-256
  `6e935f161e2b76c9349b71f73183970beeb8a659a306fb0279075b25f6ebc964`** —
  verified three times: (a) VM `sha256sum` of
  `~/workspace/board-r426/together-parts23/work/build-0.6.0-r426p23/ExcelTogether-0.6.0.jar`
  = the run-426 W6 record (REPORT.md §5 + its SHA256SUMS, which verify);
  (b) PC staging hash after upload (`6E935F16…C964`, 195,872 B);
  (c) on-mirror hash at staging (stage-r427.ps1 output, same values).
  This is the Parts 2+3 rebuild candidate. The superseded held candidate
  (`3acf43e1…`) was never staged.
- **EM premise:** rig jar of record, EM **10.9.14**, 14,563,211 B /
  `4A6BC4A74E382942E39961E9A25A231DEB46CEA34379D40BC63AE9A9328C44BD`
  from PC staging `muse-temp\xpfixrig` — on-mirror hash matched at staging.
- **Homebase probe: PASS on attempt 1**, 07:47 CST —
  `ssh -F ~/.ssh/config mike-pc hostname` returned `DESKTOP-IL05DQR`, exit 0
  (the `-F` flag used on every SSH/SCP call in this session). No fallback
  used. Three later SSH sessions dropped with broken pipes (boots NA2, NE,
  ND launches — the S1/S2 pattern); each detached boot completed on the PC
  and its log was recovered on re-probe — relay-path evidence only, PC
  never implicated.
- **Pre-run mirror census = current left state:** Together 0.5.3
  (176,042 B / `494073DC…F9F5`), EliteMobs file `EliteMobs-10.9.10.jar`
  (10.9.12; `3D796000…B995`), Downed **0.1.4** (59,998 B / `703D7F9A…339` —
  the mirror's current left state, newer than S2's 0.1.2), JAVA NONE.
- **Harness:** `TogetherRig427` (driver-src/ beside this report; built
  12,972 B / `14339C34967F776929104250C97E33AB1F689811EA72F0A0EB5DA17A84CE5668`),
  derived from the r418 `TogetherRig416` pattern (fake players, real
  chat-packet path via `handleChatCommand` as the primary medium, EM
  reflective reads; Downed provider probed reflectively, CastRigS2 shape).
  Modes: NA = A1+A2+A3, NB = A4(i), NC = A4(ii), ND = A4(iii), NE = A5
  (Session 1 mode I shape with fresh identities). Fresh rig identities
  every boot (millis suffix), per the S1 Q-artefact lesson.
- **First-boot seed confound — found, named, and re-run (the NA1 boot).**
  The mirror's ExcelTogether data folder held **no roster.yml** at this
  session's first boot, so Together enabled with the first-boot seed armed
  (`no roster.yml yet (first boot) — the roster seeds EM-first at login`).
  Boot NA1 (RigA/B22188) scored driver-PASS on A1/A2/A3 with 0 revert
  lines, but its log carries at 07:50:58: `ExcelTogether first-boot seed:
  seeded a party of 2 from EliteMobs (leader RigB22188) with origin
  LOGIN_REBUILD` — i.e. NA1's roster completion was performed by the
  **seed**, not by Part 3's native-answer completion, and NA1's A2 is
  therefore **not scored** (log retained as
  `evidence/bootNA1-seedconfounded.log`). NA1's A1 evidence is unaffected
  (during the pending window the seed seeded nothing and nothing reverted),
  but the scored A1/A2/A3 boot is **NA2**, run after NA1 had persisted
  roster.yml: NA2 enabled with `Roster of record: restored 1 party(ies)
  from roster.yml` — seed NOT armed, the S1/S2 premise. All later boots
  share that premise.

## 3. A1 + A2 + A3 — PASS (boot NA2, evidence/bootNA2.log; RigA6897 = target A, RigB6897 = inviter B)

- **A1 (invite 08:02:13, real chat path only, no reflective fallback):**
  at **every** second +1…+6: `emPending(A)=true`,
  `em(B)=party{leader=RigB6897, members=[RigB6897]}` — the native pending
  invite and the inviter's materialised solo party survived ≥3 EmSync
  polls untouched. Whole-boot `reverted a non-door` count: **0** (in
  Mirror Session 2 this exact shape died at +1 s). Driver:
  `A1-NATIVE-FORMATION PASS`.
- **A2 (A `/em party accept` via real chat path, 08:02:21):** at +6 s and
  +10 s, all faces agree — Together `partyOf` = {RigB6897, RigA6897} for
  both players, EM `getParty` = the same pair, mcMMO mirror = the same
  pair; Downed provider `membershipOf(B,A)=PARTY`,
  `partyMembersOf(B)=[6d5b6317-…]` (A's UUID, non-empty). Roster origin
  read-back: **`origin(player)=DOOR lastOrigin=DOOR`** — the completion
  landed through the door-accept path exactly as D90/NP-2b pin it (and
  unlike NA1's seed-driven LOGIN_REBUILD). Driver:
  `A2-NATIVE-ACCEPT-COMPLETES PASS`.
- **A3 (rest):** two further polls (+5 s window after the A2 score):
  hub and EM state byte-identical to the A2 baseline, no drives, no
  reverts (whole-boot revert count still 0). Driver: `A3-REST PASS`.

## 4. A4 negatives — PASS (boots NB, NC, ND)

- **A4(i) B1 (boot NB, RigC/D56846):** formed native EM party staged by
  reflective raw joins in one tick (create+invite+accept SUCCESS;
  `em(C)=party{leader=RigC56846, members=[RigC56846, RigD56846]}` — no
  pending provenance at any poll). At +10 s: EM null and Together null
  for both; **2 revert lines** (one per member). Driver:
  `A4I-B1-FORMED-NATIVE-REVERTED PASS`. The scoped completion did not
  revive general adoption.
- **A4(ii) RT-4 (boot NC, RigE/F33906):** hub `/party invite` staged
  (`hubInvites=1`, `emPending(F)=true`); F declined at the door —
  `hubInvites=0`, `emPending(F)=false`; native EM accept then returned
  **NO_PENDING_INVITE**; at +7 s both players null on EM, Together and
  mcMMO faces. **1 revert line** — the inviter's driven materialisation
  reverting after the decline (the AT-1b shape, expected). Driver:
  `A4II-RT4-DECLINE-THEN-NATIVE PASS`.
- **A4(iii) expiry (boot ND, RigG/H50093):** native invite 08:09:35
  (real path); at +3 s `emPending(H)=true`, `em(G)` materialised solo —
  spared. The revert fired at **08:10:35, exactly +60 s** (EM's invite
  timeout): `reverted a non-door EliteMobs party change for RigG50093`.
  At +75 s: `emPending(H)=false`, `em(G)=party=null`, hub null. The
  spare is bounded by EM's own timeout, as designed. Driver:
  `A4III-PENDING-EXPIRY-REVERT PASS`.

## 5. A5 regression control — PASS (boot NE, evidence/bootNE.log; RigI/J18309)

Session 1 mode I shape with fresh identities: I `/party invite J` via the
real chat-packet path — at every second +1…+6 `hubInvites=1`,
`emPending(J)=true`, inviter's driven solo EM party intact (≥3 polls);
EM popup accept (the r416/S1 equivalence, `PartyManager.accept`) returned
**SUCCESS**; Together, EM and mcMMO all read {RigI18309, RigJ18309},
origin DOOR. Whole-boot revert lines: **0**. Driver:
`A5-R3-REALPATH-INVITE PASS` + `A5-AT1-POPUP-ACCEPT-REALPATH PASS`.
The Parts 2+3 rebuild did not reopen the driven-invite (AT-1) defect it
neighbours.

## 6. Mirror left-state (cleanup, 08:11 CST)

`scripts/cleanup-r427.ps1`: driver jar + data folder + candidate 0.6.0 jar
+ EM 10.9.14 jar removed; `ExcelTogether-0.5.3.jar` and
`EliteMobs-10.9.10.jar` renamed back; ExcelTogether / EliteMobs / mcMMO /
ExcelDowned data folders robocopy-restored from `wrv-testserver\r427-backup`.
**Final jar census vs pre-run census: IDENTICAL**
(`evidence/census-before.txt` = `evidence/census-after-cleanup.txt`).
Restored hashes: Together `494073DC…F9F5`, EliteMobs `3D796000…B995`.
Final check: **JAVA NONE** — mirror stopped, exactly as found. PC staging
left in place per house pattern: `muse-temp\together-rig427` +
`wrv-testserver\r427-backup` + `boot-r427-*.log` copies. No scheduled
tasks created; debug Chrome never touched.

## 7. Evidence index (this folder)

- `evidence/bootNA2.log` (116,595 B) — the scored A1/A2/A3 boot.
- `evidence/bootNA1-seedconfounded.log` (118,156 B) — first boot; A2
  completion performed by the first-boot seed (LOGIN_REBUILD), NOT scored
  for A2; retained so the confound is auditable.
- `evidence/bootNB.log` (114,222 B) — A4(i). `evidence/bootNC.log`
  (114,450 B) — A4(ii). `evidence/bootND.log` (114,877 B) — A4(iii).
  `evidence/bootNE.log` (115,385 B) — A5.
- `evidence/census-before.txt` / `census-staged.txt` /
  `census-after-cleanup.txt` — mirror jar censuses (before = after).
- `driver-src/TogetherRig427.java` + `plugin.yml` — the driver as built
  (one post-build import fix, recorded in SESSION-LOG.md).
- `scripts/` — stage/build/run/cleanup PowerShell as run.
- `SESSION-LOG.md` — the timed session record, incl. the failed
  fire-and-forget NA2 launch attempt (boot.log truncated, java never
  started, no state changed) and its recovery.
- `SHA256SUMS` — this folder's deliverables, verified with `sha256sum -c`.

**Bottom line for the supervisor:** Mirror Branch A is GREEN on the
Parts 2+3 rebuild candidate `6e935f16…` — the EM door now forms the one
party natively (A1/A2, origin DOOR, Downed provider PARTY), the rest
discipline holds (A3), every protection negative still holds (A4 i–iii),
and the AT-1 driven-invite control is unregressed (A5). The one trap in
the session — a first-boot-seed completion masquerading as an A2 pass —
was caught from the boot log and re-run under the correct premise; the
scored boot is NA2. Next in the chain: desk-gate sign-off → G1 filing.
