# ExcelTogether one-roster rebuild — Parts 2+3 coupled build (native-pending spare + native-answer completion)

- **Run:** board supervisor run 426, worker W6, Mon Nov 16, 2026 (America/Chicago).
- **Task:** desk/build work only — no installs, no MANIFEST writes, no publications, no
  sends. Build Parts 2+3 of the run-418 EM-door scoping (SCOPING.md §5) as ONE unit on top
  of W5's Part 1 work copy, to supervisor decisions D88–D93 (DECIDE-AND-LOG, ratified this
  run). Work happened ONLY on a copy; the W5 folder and the tree of record were never edited
  (verified below).
- **Start point:** `~/workspace/board-r426/together-rebuild/work/build-0.6.0-r426/`
  (Part 1 built; WIP jar `f6315159…`, suite 1,318/0). Continued in
  `work/build-0.6.0-r426p23/` (copy; copy-time jar hash re-verified `f6315159…`).

## VERDICT (one paragraph)

Parts 2+3 are **built and desk-gate green**: the native-pending spare (Part 2) and the
native-answer completion (Part 3) are implemented in `EmSync.java` exactly to D88–D93,
with the BP-6 fake fix in `EmSyncTest.java`; the full suite passes **1,375 passed,
0 failed** (Part 1 baseline 1,318 + 57 new checks), explicitly including AT-1/AT-1b/AT-1c,
RT-2a/RT-2b, RT-4, the B1 family, and Part 1's NP-1 checks (NP-1f re-pinned — see §4).
The candidate jar is deterministic across two independent `build.sh` runs:
**195,872 B / SHA-256 `6e935f161e2b76c9349b71f73183970beeb8a659a306fb0279075b25f6ebc964`**.
Version stays **0.6.0** (no build-id bump — see §6). Nothing was filed; the remaining
chain is mirror §6 Branch A → desk-gate sign-off → G1 filing (§7).

## 1. BP-1 probe outcome (D88 step 1) — NO inviter-returning pending accessor exists

- **Jar of record probed:** EM **10.9.14** rig jar, Homebase
  `C:\Users\mikeexcel\muse-temp\xpfixrig\EliteMobs-10.9.14.jar` (the board-r418 mirror
  staging path). Verified on the PC before pulling: **14,563,211 B / SHA-256
  `4A6BC4A74E382942E39961E9A25A231DEB46CEA34379D40BC63AE9A9328C44BD`** — exact match to
  the pinned premise (D88's `4A6BC4A7…C44BD`). A copy was pulled to `/tmp` for `javap`
  (scratch only; not part of this deliverable) and its hash re-verified after transfer.
  Full output: `work/probe/javap-10.9.14.txt`.
- **`javap -p com.magmaguy.elitemobs.parties.PartyManager`** — the complete pending
  surface is: the private static field `pendingInvites: Map<UUID, PartyManager$PendingInvite>`;
  package-private `static boolean hasActivePendingInvite(UUID)` (the Part 1 read);
  package-private `openPendingInviteInventory(Player)` / `ignoreInvite(Player)`;
  and PRIVATE helpers `isPendingInviteValid`, `invalidatePendingInvite(UUID)` (returns a
  `PendingInvite` but is private AND destructive — it invalidates the invite it reads),
  `invalidatePendingInvites(Predicate)`, `cleanupExpiredInvites`.
- **`javap -p com.magmaguy.elitemobs.parties.Party`** — no pending members at all
  (id/leader/members only).
- **`PartyManager$PendingInvite`** is a record with `inviterId()` / `partyId()` /
  `expiresAtNanos()` accessors — but **no PartyManager method returns one**. The only
  inviter-revealing route would be reading EM's private static `pendingInvites` FIELD
  reflectively; a field is not an accessor, and D88 step 1 pins the accessor shape
  ("resolve it optionally in ReflectEmLink exactly as Part 1 resolved the boolean" —
  a declared METHOD via `getDeclaredMethod` + `setAccessible`, the shape both mirror
  rigs prove). The field route was therefore NOT taken.
- **Conclusion (D88 step 2 applies):** only the boolean exists → pairing is by
  enumeration, exactly as ratified. `ReflectEmLink` needed no change (byte-identical
  to the Part 1 pin `91f868bc…`).

## 2. Per-decision implementation evidence (D88–D93)

All implementation is in `src/com/winkreyes/exceltogether/EmSync.java`
(→ `1f28f8c01ca2b5d5ae3ceda3625822b13816f7308cce33b08ce9efaeca7ffb4c`); diff saved as
`parts23-emsync.diff` (349 lines). Only two files differ from the W5 copy
(`diff -rq` over `src/` and `tests/`): `EmSync.java` and `EmSyncTest.java`.

- **BP-1 / D88 (pairing by enumeration, poll-atomic):** `computeNativePairingSnapshot()`
  runs at `beginCycle()` and pairs (inviter → target) only when exactly ONE roster-less
  player holds a solo materialised EM party AND exactly one online roster-less target
  (in the inviter's visible-online universe) holds a pending invite with (a) no
  driven-invite ledger entry from anyone and (b) no live hub invite. The snapshot is
  judged on the **cycle-start picture**: an earlier reconcile in the same poll reverting
  one crossed side must not make the remainder pairable mid-poll — the first desk run
  proved this failure live (NP-2d: sequential evaluation spared the second crossed pair
  in the same poll it reverted the first), so pairing was moved into the snapshot;
  `nativePendingTargetFor` only re-checks the inviter's own live picture (solo,
  roster-less, paired target's pending still live) against it. Any ambiguity pairs
  NOTHING and the ordinary revert stands.
- **BP-2 / D89 (completion mechanism):** `driveNativeCompletion(target, inviter)`
  synthesises the hub invite via `PartyManager.invite(inviter, target, now)` and then
  calls the **unmodified** `Controller.acceptFrom(view, inviter)`. No parallel
  native-answer method exists. All accept-time checks/wordings, suppression clearing,
  the mcMMO one-way mirror write and `EmSync.noteJoined` are the door's own code.
- **BP-3 / D90 (origin):** completions land through `acceptFrom`, whose accept tags
  `RosterOrigin.DOOR`. No new enum value; `RosterOrigin.java` and the roster.yml format
  are untouched (byte-identical to the W5 copy). Desk-pinned by NP-2b
  (`originOfPlayer == DOOR`).
- **BP-4 / D91 (observation map):** `nativePendingObs` in EmSync — target →
  (inviter, timestamp), TTL `NATIVE_OBS_MS = ReflectEmLink.INVITE_LEDGER_MS` (60 s).
  Its ONLY write site is `nativePendingSpare` (the Part 2 spare observing a live native
  pair). It is never written from any drive Together makes (driven invites pair through
  the snapshot's ledger exclusion and never reach the spare). Read by the Part 3
  completion (`nativeAnswerCompletion`), the inviter-side spare
  (`nativeCompletionPending`), and the deferred-queue completion leg.
- **BP-5 / D92 (defaults):**
  1. *Foreign/crossed never consumed blindly:* snapshot exclusions (ledger, hub invite)
     + the completion's own crossed re-check (`invitesFor(target)` non-empty → no
     completion), re-checked again on the deferred leg. Desk: NP-2d, NP-2e.
  2. *PARTY_FULL / INVITER_OFFLINE wordings unchanged; refusal → next-poll revert:*
     INVITER_OFFLINE fires verbatim inside `acceptFrom` (NP-2f). PARTY_FULL can refuse
     one step earlier, at the synthesise call (`PartyManager.invite` checks the
     inviter's party size), where `acceptFrom` can never see an invite — there the
     completion speaks acceptFrom's exact PARTY_FULL wordings to both sides and ends
     the attempt identically. Either way the observation is **spent by the attempt**,
     so the formed EM party falls back to the ordinary next-poll revert (B1 shape) —
     the named desk scenario NP-2f (refusal wording + no join + observation spent +
     next-poll revert logged).
  3. *Completion AFTER suppression:* the completion check sits in `one()` after the
     suppression block and after the seed gate, with no popupAnswer-style exception
     (popupAnswer itself still runs first, unchanged — it is hub-invite-keyed and can
     never match a native pair). Desk: NP-2g (suppressed target enforced out, never
     completed).
  4. *Dungeon-busy deferral:* `nativeAnswerCompletion` on a dungeon-busy EM party
     enqueues `Op.Kind.NATIVE_COMPLETE` on the target's roster-less deferred queue
     (expiry rides the existing `deferMaxMs`); `runDeferred` applies it on unfreeze
     only while the observation lives, the pair still stands in EM together, and no
     hub invite has crossed in meanwhile. The deferred REVERT twin gained the same
     two guards AT-1 has (`nativePendingSpare` / `nativeCompletionPending` drop a
     queued revert). Desk: NP-2h.
- **BP-6 / D93 (fake fix):** `EmSyncTest` fake `emLeaveRaw` now removes every
  target-keyed pending entry naming the leaver as inviter (real EM behaviour per
  Mirror Session 2) before the party edit, plus the pinning check NP-2i. A raw
  decline/expiry helper `emPendingGoneRaw` was added for the lifecycle scenarios.

## 3. Files changed (diffs saved in this folder)

| File | Before (W5 copy) | After | Diff |
|---|---|---|---|
| `src/com/winkreyes/exceltogether/EmSync.java` | `053287355639ac…` (record pin) | `1f28f8c01ca2b5d5…` | `parts23-emsync.diff` |
| `tests/com/winkreyes/exceltogether/EmSyncTest.java` | `5340a9cf00af6cf2…` (Part 1) | `4857d740303a18fa…` | `parts23-emsynctest.diff` |

Unchanged and re-verified against the W5 pins: `EmLink.java` (`0507da69…`),
`ReflectEmLink.java` (`91f868bc…`). The W5 folder itself is untouched (its jar still
`f6315159…`, its EmSyncTest still `5340a9cf…`, its EmSync.java still the record pin).

## 4. Test tallies (two full `build.sh` runs, identical)

- **Full suite: 1,375 passed, 0 failed** (Part 1 baseline 1,318 + 57 new).
  Logs: `work/parts23-build-run1.log`, `work/parts23-build-run2.log`.
- **EmSyncTest: 242 passed, 0 failed** (185 at Part 1 + 57).
- Named families, all green in EmSyncTest: **AT-1, AT-1b, AT-1c** (driven spare +
  decline re-arm + ledger-half guard, unchanged); **RT-2a, RT-2b** (+ RT-2c),
  **RT-4** (post-decline native accept still joins nothing and reverts — no
  observation can exist for it); **B1 family** (B1, B3, B4, B5, B8, B9 L1/L3/L4,
  D2 — formed native parties with no pending provenance still revert);
  **NP-1** (Part 1's pending-read checks NP-1a–NP-1e unchanged).
- **NP-1f RE-PINNED:** Part 1 pinned the native materialisation as still-reverting
  ("part 1 alone changes no behaviour"). Parts 2+3 flip exactly that shape by
  design, so NP-1f now pins: spared while the native pending lives, no revert
  logged, pair recorded in the observation map. The re-pin is commented at the
  check site.
- **NP-2 (new, the SCOPING L311–317 desk gate + D92 scenarios), 54 checks:**
  NP-2a spare survives ≥3 polls (nothing created/driven/told); NP-2b native accept
  completes the roster, DOOR origin, both faces agree, observation spent;
  NP-2c expiry re-arms the revert; NP-2c2 native decline re-arms the revert;
  NP-2d crossed pendings pair nothing, complete nothing, both revert;
  NP-2e native pending crossed with a live hub invite is never paired/completed;
  NP-2f hub-refusal fallback (INVITER_OFFLINE wording → next-poll B1-shape revert);
  NP-2g suppressed target never completes; NP-2h dungeon-busy completion defers
  and lands on unfreeze; NP-2i the BP-6 fake pin. (§11C rest checks ride each
  scenario as before.)

## 5. Candidate

- **Path:** `work/build-0.6.0-r426p23/ExcelTogether-0.6.0.jar`
- **Size / SHA-256:** 195,872 B / `6e935f161e2b76c9349b71f73183970beeb8a659a306fb0279075b25f6ebc964`
- **Determinism:** two independent `build.sh` runs produced the identical hash
  (each run also double-builds internally and asserts reproducibility). This
  candidate **supersedes the held 0.6.0 candidate** (`3acf43e1…`) and the Part 1
  WIP jar (`f6315159…`).

## 6. Version / build-id note

**Kept at 0.6.0 — no bump, and no build-id field exists to bump.** `plugin.yml`
still declares `version: 0.6.0` (the build's descriptor gate asserts exactly
that), and the rebuild supersedes the held 0.6.0 candidate before it ever
shipped (live is still 0.5.6 per the D80 rollback record). Recommendation:
keep 0.6.0 for the rebuild packet; the candidate is distinguished by hash
(`6e935f16…`) in every record, as the held candidate was (`3acf43e1…`).

## 7. Remaining chain (NOT done here — no filing was made)

1. **Mirror re-test:** SCOPING.md §6 Branch A (A1–A5) on the Homebase
   test-server mirror against candidate `6e935f16…` — the only gate that
   clears the D80 chain. (A4(iii) expiry and A4(ii) RT-4 shapes are desk-pinned
   by NP-2c/RT-4; the mirror still owes them live.)
2. **Desk-gate sign-off** on this report by the supervisor.
3. **G1 filing:** fresh live pull at filing time (live premise 0.5.6 from
   records), pointer-stub packet per PACKET-TEMPLATE, MANIFEST filing by the
   supervisor — including disposition of the HELD 0.6.0 entry in favour of
   this rebuild. Failsafe installs on the normal emptiness gate; same-pass
   surfaces + AT-4-class player proof owed at install.

## 8. Evidence index

- `work/probe/javap-10.9.14.txt` — the BP-1 probe (jar hash/size header + full
  `javap -p` of PartyManager, Party, PartyManager$PendingInvite).
- `parts23-emsync.diff`, `parts23-emsynctest.diff` — the complete change set
  vs the W5 Part 1 copy.
- `work/parts23-build-run1.log`, `work/parts23-build-run2.log` — two full
  suite runs (1,375/0 each) + the deterministic jar lines.
- `work/build-0.6.0-r426p23/` — the continued build tree (candidate jar at its
  root). Scoping: `~/workspace/board-r418/together-emdoor-scoping/SCOPING.md`
  §4–§6; Part 1 record: `~/workspace/board-r426/together-rebuild/REPORT.md` +
  `BUILD-PLAN.md` (BP-1…BP-6 as ratified by D88–D93).
