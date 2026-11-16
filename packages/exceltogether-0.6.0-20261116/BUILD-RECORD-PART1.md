# ExcelTogether one-roster rebuild — FIRST unit: scoping verification + build start

- **Run:** board supervisor run 426, worker W5, Mon Nov 16, 2026 (America/Chicago).
- **Task:** desk work only — no installs, no MANIFEST writes, no publications, no
  sends. Verify the run-418 EM-door scoping's 3-part build against Mike's ruling,
  locate the 0.6.0 tree of record, run the base gate, and start the build in a
  work copy if the scoping is build-ready.
- **Ruling under test (Mike, Nov 16, 2026, 7:09 AM CST; MEMORY.md L38):** "Both —
  /em party works as a face of the one party." The one-roster rebuild keeps
  /em party as a working door into the shared roster (3-part build per run-418
  scoping); two independent rosters syncing both ways stays rejected (the Oct 31
  mesh failure). This clears the D80 hold (decision-log.md L558: HOLD of the
  ExcelTogether 0.6.0 gated entry after Mirror Session 2's M4 FAIL).

## VERDICT (one paragraph)

The scoping's Branch A is exactly Mike's ruling, and its 3-part build is
**consistent with the ruling on every part** — each part feeds the ONE roster of
record through a door act, and no part creates, keeps, or syncs a second roster
(§2). The scoping is **build-ready for Part 1 only**: Part 1 (the pending read)
is fully specified, was built in this unit, and passes the full suite (§5).
Parts 2+3 are one coupled unit whose remaining design points the scoping itself
reserves ("Design points the builder must pin, not improvise", SCOPING.md
L303–309) — they are named with recommended resolutions in BUILD-PLAN.md
(BP-1…BP-6) for supervisor DECIDE-AND-LOG ratification, and were **not**
improvised here. Building Part 2 alone (the spare without the completion) is
explicitly not recommended by the scoping (L319–325) and was not done.

## 1. The scoping of record and its 3-part plan (quoted with evidence)

Scoping file: `~/workspace/board-r418/together-emdoor-scoping/SCOPING.md`
(41b5eef1fddf766176ae227b7a8a88bb399a8b8c271a0ab52f0f5320186fada1; its own
SHA256SUMS verifies OK). Root cause it proved: 0.6.0's D74 one-door rebuild
deleted Case B adoption; the mesh reverts a native /em party invite at the
first poll because a native invite satisfies neither half of the AT-1 spare
(no hub invite, no driven-invite ledger entry) and the EmLink interface had
**no pending-invite read at all** (SCOPING.md §2). The open design question it
left (H5, L203) is the one Mike's ruling now answers for Branch A ("EM door
stays a formation door").

The 3-part plan (SCOPING.md §5, L262–267: "the fix is a small design unit (one
packet, Together-only), with three necessary parts — dropping any one leaves a
demonstrable failure"):

1. **Part 1 — a pending read on the mesh leg** (L269–279): "Add a pending-invite
   query to `EmLink` + `ReflectEmLink` (EM's
   `PartyManager.hasActivePendingInvite(UUID)` is proven readable reflectively —
   both rigs already call it; …). The desk fake gains the same read, plus the
   missing raw native-pending state (pending written *without* a ledger entry)."
2. **Part 2 — a native-pending spare** (L280–287): "in the EM-only branch,
   sibling to `drivenInviteMaterialisation`: a roster-less player whose EM party
   is a solo materialisation with a live *native* pending invite behind it is
   spared (debug-log only) while that pending invite lives — bounded by EM's own
   60 s invite timeout, dying with it exactly as the AT-1 spare dies with the hub
   invite. The deferred-queue twin guard gets the same clause."
3. **Part 3 — a native-answer completion** (L288–301): "sibling to
   `popupAnswer`: when a roster-less player is found in an EM party whose
   formation traces to that live (or just-consumed) native pending pair,
   complete the roster join through the same door-accept path popup matching
   uses (`Controller.acceptFrom`-equivalent semantics, accept-time checks
   included, roster origin tagged truthfully as the EM door) instead of
   reverting. This is a *scoped* revival of adoption for pending-backed joins
   only — formed native parties with no pending provenance (B1/B3/B8/D2) and
   post-decline native accepts (RT-4) must keep reverting."

## 2. Ruling-consistency verdict per part

Test applied to each part: (a) does it keep exactly ONE roster of record
(PartyManager / roster.yml) as the only membership truth? (b) does /em party
act as a door/face INTO that roster (an invite/answer surface), never as a
second roster that syncs both ways with the first (the rejected Oct 31 mesh)?

| Part | Verdict | Why |
|---|---|---|
| 1 — pending read | **CONSISTENT** | Pure read-only query on the mesh leg. It creates nothing, drives nothing, syncs nothing; it only makes an existing EM-door act (a native pending invite) visible to the one roster's reconciler. The interface default (false) and the NONE link keep absent-EM behaviour identical to today. |
| 2 — native-pending spare | **CONSISTENT** | A bounded non-action: while a native invite is pending, the reconciler refrains from dismantling the inviter's materialised solo EM party. No roster entry is created, no EM state is written, nothing syncs; the spare dies with EM's own 60 s invite timeout. The roster stays the sole truth — it simply is not yet written, exactly as during a /party invite's pending window today (the AT-1 spare this mirrors). |
| 3 — native-answer completion | **CONSISTENT** | The roster join is completed through the SAME door-accept path the EM popup answer already uses (Controller.acceptFrom semantics, accept-time checks included), i.e. the EM door's answer lands in the one roster as a door act with a truthful origin tag. It is scoped to pending-backed joins only: formed native parties with no pending provenance (B1 family) and post-decline native accepts (RT-4) keep reverting — there is no general EM→roster adoption revival and no EM-roster↔Together-roster two-way sync anywhere in the part. mcMMO remains a one-way mirror throughout (untouched by all three parts). |

**No part implements two-way sync of independent rosters — verified negatively:**
the plan adds (i) a read, (ii) a withholding of a revert, (iii) a completion
through the existing door path. No part writes EM party state from roster state
beyond the materialise-only construction 0.6.0 already has, and no part reads a
formed EM party into the roster except the pending-backed completion, which is
a door answer, not a sync.

**One recorded tension, for the supervisor's decision log (not a blocker):**
D74 Q1 (decision-log.md L517–522) pinned "native EM /em party invite accepted
natively = revert + tell". Mike's Nov 16 ruling adopts the run-418 scoping's
3-part build by reference (MEMORY.md L38), and Part 3 is precisely a scoped
exception to Q1 for pending-backed joins. Q1's protection survives everywhere
else (B1/RT-4 pins stand). The supervisor should log the Q1-supersession scope
when dispatching the Parts 2+3 unit; the ruling itself is Mike's and needs no
further ask.

## 3. Tree of record + live premise

- **0.6.0 source tree of record:** `~/workspace/together-060-build-20261115/build-0.6.0/`
  — identified by the scoping itself (SCOPING.md, Controls read) and confirmed
  here by hash: `src/com/winkreyes/exceltogether/EmSync.java` SHA-256
  `053287355639ac907b725aa496e9ecf7b408efeaaa099d225c7a2ceddbdd59e6` = the AT-1
  fix report's post-fix pin; the tree's jar `ExcelTogether-0.6.0.jar` is
  192,597 B / `3acf43e1f650f6887399d93150eb8e2c50451e1f640067b4b45754aeb58134c8`,
  byte-identical to the staged candidate in
  `~/workspace/board-r418/together-filing/ExcelTogether-0.6.0.jar` (same hash).
  0.5.6 baseline tree beside it: `…/together-060-build-20261115/base-gate-0.5.6/`
  (EmSync.java `9e1e6401494da2cadcad477ad0cbc5c54688e271946f35fb7faa29375115df96`,
  per the scoping).
- **Live premise to cite: live is ExcelTogether 0.5.6** — 179,161 B /
  `1f32f0f7bbad69bfd74f496a490b32832112390209b7a476951cc07bfd5c16f4`. Records
  basis: 0.6.0 was installed Nov 16 ~02:36 and **rolled back in the same
  window** under D80 (board-r418/together-filing/INSTALLED.txt:
  status=ROLLED-BACK 2026-11-16; OUTCOME.txt addendum: pull-back verify EXACT
  179,161 B / 1f32f0f7…, 0.6.0 jar held as
  `plugins/ExcelTogether-0.6.0.jar.held-20261116-d80rollback`, zero player
  impact; MEMORY.md daily summary L310 concurs). This unit cites 0.5.6 **from
  records only**; a fresh G1 live pull is a filing-time step and was not done
  (and must not be replaced by these record hashes at filing).

## 4. Base gates (work copies only — the trees of record were never edited)

Both gates ran in `work/` copies whose `src/` trees were first verified
identical to the trees of record (`diff -r`, clean). Logs:
`base-gate-r426.log`, `pre-edit-060-rebuild.log`.

| Gate | Result | Suite | Jar |
|---|---|---|---|
| 0.5.6 base gate (pristine copy of base-gate-0.5.6) | **PASS — byte-identical** | 981 passed, 0 failed | 179,161 B / `1f32f0f7bbad69bfd74f496a490b32832112390209b7a476951cc07bfd5c16f4` = candidate of record |
| 0.6.0 pre-edit rebuild (unmodified copy of build-0.6.0) | **PASS — byte-identical** | 1,306 passed, 0 failed (EmSyncTest 173) | 192,597 B / `3acf43e1f650f6887399d93150eb8e2c50451e1f640067b4b45754aeb58134c8` = AT-1 candidate of record |

## 5. What was built — Part 1 (the pending read), complete

Work tree: `work/build-0.6.0-r426/` (copy of the tree of record; the record
tree's post-run hashes are unchanged — EmSync.java 05328735…, EmLink.java
8fc26da4…, jar 3acf43e1…). Exactly three files differ from the record tree
(`diff -rq` over src/ and tests/; unified diffs saved as part1-*.diff):

1. `src/com/winkreyes/exceltogether/EmLink.java` (→
   `0507da694d76a88dbe563449bbaa2557c4446be2f89227fdf0bcf37bfd1258b2`): new
   `default boolean hasPendingInvite(UUID target)` — read-only, with the
   contract that false means "no pending read" (none OR unavailable) and must
   never alone license action.
2. `src/com/winkreyes/exceltogether/ReflectEmLink.java` (→
   `91f868bc4248b10aaf29fad7a846c5050cd5c964b69feaef05718a19d4a934f5`): the
   method is resolved **optionally** via `getDeclaredMethod` +
   `setAccessible` — the exact access shape both mirror rigs prove against EM
   10.9.14 — and its absence never disables the link (the link then behaves
   exactly as the pre-Part-1 build). Failures count toward the existing
   failure latch; `pendingReadAvailable()` exposes whether the read resolved.
3. `tests/com/winkreyes/exceltogether/EmSyncTest.java` (→
   `5340a9cf00af6cf2a5850e2ccc3d3eb2ef13d7fb9bb0a14ffae7473f7003382b`): the
   desk fake gains `hasPendingInvite` over its pending slot and the missing
   raw native-pending state (`emInviteRaw` — pending written with NO ledger
   entry, inviter materialised), plus the NP-1 check block (12 checks):
   driven invite reads pending + ledgered (NP-1b); native invite reads
   pending + **no** ledger — the distinction Parts 2/3 key on (NP-1d);
   accept consumes the slot (NP-1c); EmLink.NONE reads false (NP-1a); and
   NP-1f pins the **pre-fix baseline**: with Part 1 alone the native
   materialisation still reverts (the M4 desk shape) — Part 1 changes no
   reconcile behaviour, by design.

**Test tallies (two full build.sh runs, identical):** EmSyncTest **185 passed,
0 failed** (173 baseline + 12 NP-1); full suite **1,318 passed, 0 failed**
(1,306 baseline + 12). Logs: `part1-build-run1.log`, `part1-build-run2.log`.

**WIP jar (NOT a candidate — do not file):**
`work/build-0.6.0-r426/ExcelTogether-0.6.0.jar`, 192,864 B / SHA-256
`f63151595e87d3cdefe53a8012d41653b39f30c04f6f444bd30363ce66f0c680`,
deterministic across both full runs (each run also double-builds internally).
It carries Part 1 only; the EM door still cannot form a party under it
(NP-1f). It exists as build evidence for the next unit to build on.

## 6. What was NOT built, and the exact remaining chain

Parts 2 (spare) and 3 (completion) were not built: they are one coupled unit
(BUILD-PLAN.md BP-4) gated on supervisor-ratified pins BP-1 (target→inviter
pairing), BP-2 (completion mechanism), BP-3 (origin tag), BP-5 (crossed /
full / suppression / freeze defaults), with BP-6 a fake-fidelity fix inside
that unit. Every pin sits inside Mike's ruling; none needs a Mike ask.

Remaining chain, in order:
1. **Supervisor:** DECIDE-AND-LOG entries ratifying BP-1…BP-6 (incl. the D74-Q1
   supersession scope noted in §2) → dispatch Parts 2+3 as one build unit on
   top of this unit's work tree.
2. **Desk gate:** SCOPING.md L311–317 lifecycle scenarios + full suite green
   (AT-1/AT-1b/AT-1c, RT-2a/RT-2b, RT-4, B1 family named).
3. **Mirror re-test:** SCOPING.md §6 Branch A (A1–A5, L365–390) on the
   Homebase test-server mirror — the only gate that clears the D80 chain.
4. **Filing:** fresh G1 live pull at filing time (live premise 0.5.6, §3),
   pointer-stub packet per PACKET-TEMPLATE, MANIFEST filing by the supervisor.
   The HELD 0.6.0 MANIFEST entry is **superseded by this rebuild** — its
   disposition (supersede/close in favour of the rebuild packet) is the
   supervisor's MANIFEST act; this worker made no MANIFEST change.
5. **Failsafe installs** on the normal emptiness gate; same-pass surfaces +
   AT-4-class player proof owed at install, per the original packet pattern.

## 7. Evidence index

- Scoping: `~/workspace/board-r418/together-emdoor-scoping/SCOPING.md`
  (41b5eef1…; §5 L262–325 the plan, §6 L348–390 the re-test).
- Failure evidence: `~/workspace/board-r418/mirror-s2/REPORT.md` §5
  (e6a1d38a…) + `evidence/bootEM.log` (04066a7e…, per the scoping's index).
- Hold + rollback: decision-log.md L558 (D80), L517–522 (D74);
  `~/workspace/board-r418/together-filing/INSTALLED.txt` + `OUTCOME.txt`
  (rollback addendum, live 0.5.6 EXACT).
- Backlog: parent-backlog.md L664 (P-PARTY-REBUILD-1) and L553 (run-419
  update carrying the H5 question Mike has now answered).
- This folder: `base-gate-r426.log`, `pre-edit-060-rebuild.log`,
  `part1-build-run1.log`, `part1-build-run2.log`, `part1-emlink.diff`,
  `part1-reflectemlink.diff`, `part1-emsynctest.diff`, `BUILD-PLAN.md`,
  `work/base-gate-0.5.6-r426/`, `work/build-0.6.0-r426/` — all hashed in
  SHA256SUMS (verified `sha256sum -c`).
