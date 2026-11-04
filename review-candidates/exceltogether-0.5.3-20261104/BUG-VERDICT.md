# BUG VERDICT — ExcelTogether 0.5.2: a voluntary party leave does not stick

> **Addendum (build time, Nov 3, 2026):** the fix's first cut put the EM-door-leave
> detection only in `EmSync.one()`. The B9b scenario then failed intermittently
> (~1 run in 3): when a party-MATE is reconciled before the leaver in the same poll,
> `converge()` step 3 sees the leaver as a "missing" member and drives them back into
> EM through `addOne` before `one(leaver)` ever runs — the same resurrection, one
> funnel over. The shipped fix therefore ALSO guards `addOne` (the single funnel for
> converge, materialise and the deferred queue): a settled member who stepped out at
> the EM door is never driven back in; their leave is honoured instead. After the
> guard, EmSyncTest passes 8/8 runs and a 40-iteration B9b probe passes 40/40.

Report: Nov 3, 2026, 11:26 PM CST (Mike, with Jester_Turtle's screenshot).
Jester, in a party with GumGumJetPistol (Marvin), leaves and is auto-joined back.
Live log (UTC): Jester `/em party leave` ×6+ (05:23–05:28), Marvin `/party DISBAND`
(05:25:51), both relog ~05:27 and 05:28:11–12, re-form line at 05:27:38
("GumGumJetPistol is now in the same party as Jester_Turtle"), ExcelPartyDrops
still `participants=2` at 05:29:56 / 05:30:05.

## Root cause (one sentence)

In the 0.5.x mesh, **"this player chose to leave" is recorded only when the removal
starts at the ExcelTogether door** (`Controller.announce(..., mirror=true)` → leave
drives in both links + `suppress()` in both syncs). Removals that start at the other
two doors of the "one party" — the EliteMobs door and the mcMMO door — record **no
removal intent anywhere**, so each reconciler treats the surviving copies of the
roster as authoritative and rebuilds the copy that was just torn down. Both doors
Jester and Marvin actually used are in that class; that is the whole bug.

## Path 1 — the EliteMobs door is a revolving door (Jester's door, 6+ attempts)

- Code: `EmSync.one()` **Case C** + `EmSync.materialise()` (build-0.5.2
  `src/com/winkreyes/exceltogether/EmSync.java`); semantics per DESIGN §4 L4
  ("leaving EM alone never leaves the party — EM is a view").
- Jester's `/em party leave` changes only EliteMobs state. At the next EmSync poll
  (~2 s) his picture is: EM = NONE, Together = still in the party → Case C →
  `materialise()` drives create/invite/accept and puts him straight back into the
  EM party. Nothing anywhere distinguishes "left at the EM door on purpose" from
  "EM lost him": `partyOf()` reads NONE either way, and no state records a choice.
- EM is the FACE of the one party (its sidebar is what the player watches, and its
  hint line literally reads `/em party leave`), so from the player's seat he
  leaves and is rejoined, every time, for as long as the hub party stands. The
  asymmetry is structural: the mcMMO door has a mirror path for hand quits
  (`McmmoSync` removal branch); the EM door has none.

## Path 2 — the mcMMO disband is undone by the EliteMobs leg (Marvin's door)

- Code: `McmmoSync.one()` (`!mc.real()` branch) → `McmmoSync.quietLeave()` →
  `Controller.announce(ch, ..., mirror=false)`; then `EmSync.one()` **Case B**
  (`caseB()`).
- When the disband lands in mcMMO, McmmoSync dissolves the Together party through
  `quietLeave` — deliberately with `mirror=false` (never push back into the
  origin system). But `mirror=false` also means **no `emLink.leave` drives and no
  `emSync.suppress`**: the EliteMobs party, which nobody touched, still lists both
  players.
- At the next EmSync poll both players are EM-real / Together-null → Case B
  adoption rebuilds the Together party from the EM roster. The trailing
  `mcmmoChanged(adopted, [])` from `caseB` cannot undo it: McmmoSync's
  not-in-mcMMO branch only quiet-leaves players still present in its `mirrored`
  map, and Case-B adoption never marks them there.
- Honest caveat: the pulled log lines do not include mcMMO's own reply to
  `/party DISBAND`, so "disband landed" vs "Marvin was not the mcMMO leader and
  mcMMO refused it" cannot be distinguished from the excerpts. If it was refused,
  Path 1 plus the relog cycle below explain the entire incident on their own
  (the roster of record never changed). Both readings convict the same root
  cause, and the landed variant is broken in code either way — the regression
  tests exercise it directly.

## The relog amplifier (05:27 and 05:28:11–12 relogs; re-form at 05:27:38)

- EliteMobs removes quitters itself — bytecode-verified against the EM jar of
  record: `PartyManager.onPlayerQuit` → `leave(UUID, ...)`; with both players
  out, the EM `Party` object is destroyed.
- Together holds members through the 300 s offline-grace (`PartyManager.onQuit /
  onJoin`), so the hub party survived both relogs; the login watches
  (`Controller.playerJoin` → `emWatch` / `mcmmoWatch`) reconciled on return and
  Case C materialise created a **new** EM party — `Party.getId()` is a
  construction-time `final UUID` (bytecode-verified), so the re-created party
  carries a new key — and re-added both. That landing is the 05:27:38 line.
  ExcelPartyDrops `participants=2` at 05:29:56 / 05:30:05 CST is the Together
  party, never dissolved, still holding both at the end of the window.

## Contributing fragility (hardened in 0.5.3; not needed for the conviction)

EmSync suppression — the one guard that exists — is keyed to the EM `Party` UUID
captured at suppress time (`emSuppressedKey`), and `one()` **clears it whenever
EM shows the player in a party with a different key**, reading that as "a
deliberate fresh join". A party re-created around the *same roster* — exactly
what materialise does after a double relog — carries a new UUID, so if a
Together-door removal's EM leave drive ever failed to land, the re-created twin
would defeat the suppression the same way. 0.5.3 makes suppression roster-based:
it holds while EM lists the player in any party made only of the people they
were removed from (any key), and clears only when EM shows them partyless or in
a party containing someone who was not in that roster.

## Verified NOT the mechanism

- `Controller.leave()` / `announce(mirror=true)` itself is sound: drives both
  links' leaves and suppresses in both syncs (EmSyncTest B5/D5 prove the
  same-key cases). No Together-door (`/excelparty leave`) attempt appears in the
  pulled log lines at all — the two doors used are exactly the two unguarded
  ones.
- `PartyManager` leave/kick/disband semantics are sound (PartyTest).
- The failed console removal ("Players only.") is a separate gap, not this bug:
  `ExcelTogether.onCommand` rejects every non-player sender before any
  subcommand routing. 0.5.3 closes it with the new `admin` branch (Mike's
  standing rule, Nov 3, 2026: every plugin ships console-runnable admin
  commands — "We should always have a way to help the players").

## The 0.5.3 fix (mesh's own terms; idempotence and loop guards intact)

1. **EmSync learns the EM-door leave.** A settled, mirrored, *online* member
   whose EM picture goes NONE while their EM twin survives (same partyKey,
   still listing a party-mate) has left the party: the hub leave runs with the
   full mirror cascade (`announce(mirror=true)` — mcMMO leave drive + both
   suppressions), exactly like a Together-door leave. Guards: the login watch
   is not settled (relogs keep materialising, as designed) and offline players
   are never detected (a quit is not a leave). New config key
   `mesh.em.door-leave-ends-party` (default `true` — Mike's ruling that a
   voluntary leave sticks; `false` restores the 0.5.x view-only semantics).
2. **mcMMO-door removals cascade into the EM leg.** `McmmoSync.quietLeave` now
   calls `Controller.cascadeEmRemoval(change)`: `emLink.leave` drives +
   `EmSync.suppress` for the removed player (and, on a disband, the remaining
   members). DESIGN §2.4 invariant 3 — "Together-originated removals suppress
   in both syncs" — now holds for mcMMO-originated removals too. (Tick order
   already runs the mcMMO poll before the EM poll, so the suppression lands
   before Case B can see the torn-down roster.)
3. **Suppression is roster-keyed** (see fragility above): `EmSync.suppress`
   records the removed-from roster (the `PartyManager.Change` roster, passed
   by `Controller.announce` / `cascadeEmRemoval`); the hold test in `one()` and
   the enforcement test in `converge()` compare rosters, not keys.

Regression evidence: `tests/com/winkreyes/exceltogether/LeaveSticksTest.java` —
scenarios L1 (EM-door leave), L2 (mcMMO disband), L3/L4 (leave → relog → poll,
both doors), L5 (suppression vs key rotation, same roster vs fresh group) —
**fail against the 0.5.2 tree and pass against 0.5.3** (counts in
TEST-RESULTS.txt). One inherited expectation changes with the fix, deliberately:
EmSyncTest **B9b** asserted the 0.5.2 L4 rule (an EM leader who leaves EM only
is driven back in). Under Mike's ruling that a voluntary leave sticks, that leg
now expects the leave to end the party; the test is updated and the change is
called out in CHANGELOG-0.5.3.md.
