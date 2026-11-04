# ExcelTogether 0.5.3 — changelog

**Leaving the party now sticks — at every door. And the console can help.**

- FIXED — **the party-leave bug** (reported Nov 3, 2026: Jester_Turtle left the
  party six times in five minutes and was auto-joined back every time; a disband by
  GumGumJetPistol re-formed minutes later). Root cause (BUG-VERDICT.md): the mesh
  only recorded "this player chose to leave" for leaves that started at the
  /excelparty door. Leaves at the EliteMobs door (`/em party leave`) or the mcMMO
  door (`/party` quit / disband) left the other roster copies standing, and the
  reconcilers rebuilt whichever copy had been torn down.
- **Leaving at the EliteMobs door now ends the party for the leaver everywhere** —
  the Together record, the mcMMO party, and the EliteMobs party all drop them, with
  the usual "left the party" messages. Same for a kick or disband done at the mcMMO
  door: it now tears down the EliteMobs face too instead of leaving it to resurrect
  the party. Relogging no longer re-forms a party anyone left or disbanded.
- A disconnect is still NOT a leave: quit mid-party and your EliteMobs face is
  rebuilt when you return, exactly as before (regression-guarded).
- The hold that keeps a removed player from being re-adopted is now keyed to the
  PEOPLE they were removed from, not to the EliteMobs party's internal ID — an
  EliteMobs party re-created around the same players can no longer defeat it.
  Joining a genuinely new group still clears it instantly.
- Config: `mesh.em.door-leave-ends-party` (default true) restores the old
  view-only EliteMobs semantics if ever wanted. No other behavior changes; the
  mesh's idempotence and loop guards are untouched, and 81 of the jar's 90 entries
  are byte-identical to 0.5.2 (see SOURCE.md).
- NEW — **console admin surface** (Mike's standing rule, Nov 3, 2026: "We should
  always have a way to help the players"). `/excelparty admin` now works from the
  server console (and for players with the new `excelparty.admin` permission,
  default op):
  - `admin status <player>` — their party, its leader and members, what mcMMO and
    EliteMobs each show for them, and whether they're held out of auto-rejoin.
  - `admin remove <player>` — removes them with the exact semantics of a voluntary
    leave (mirrors told, no auto-rejoin), announced as an admin removal.
  - `admin disband <player>` — disbands their party with the same cascade.
  In 0.5.2 the console got "Players only." for every /excelparty command — during
  the Nov 3 incident there was no way to free a stuck player from the console.
  Non-admin console use still gets "Players only.", and player commands are
  unchanged.
- Carries everything from 0.5.2 (the Repair Equipment confirmation gate) and
  0.5.1 (the party-repair Magic actions) — neither was ever installed, so this
  jar supersedes both packets; the live gate is the installed 0.5.0 jar.
