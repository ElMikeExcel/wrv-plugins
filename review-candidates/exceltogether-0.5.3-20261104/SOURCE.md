# ExcelTogether 0.5.3 — SOURCE / provenance

Build folder: `~/workspace/party-mesh/build-0.5.3/` (the party-mesh project's convention:
build-0.5.2 sits beside it; parent paperwork in `~/workspace/party-mesh/PLACEMENT.md`).

## What 0.5.3 is

0.5.2 plus the fix for the Nov 3, 2026 party-leave bug and the console admin surface
Mike's new standing rule requires. Full mechanism verdict: `BUG-VERDICT.md` in this
folder. In one paragraph: in the 0.5.x mesh, "this player chose to leave" was recorded
only when the removal started at the ExcelTogether door. Removals starting at the
EliteMobs door (`/em party leave`) or the mcMMO door (`/party` quit/kick/disband)
recorded no removal intent anywhere, so each reconciler treated the surviving roster
copies as authoritative and rebuilt the torn-down copy — Jester_Turtle left six times
in five minutes and was auto-joined back every time, and a hand disband re-formed at
05:27:38 UTC after two relogs.

### The fix (three parts, all in the mesh's own terms)

1. **The EM door is a real door** (`EmSync`): a settled, mirrored, ONLINE member whose
   EM picture goes NONE while their EM twin survives without them (same partyKey,
   still listing a party-mate) has LEFT the party — the hub leave runs with the full
   mirror cascade (`Controller.announce(mirror=true)`), exactly like a Together-door
   leave. The check runs in `EmSync.one()` AND inside `addOne()` — the single funnel
   every drive back into EM passes through (converge, materialise, the deferred
   queue) — because a party-mate reconciled earlier in the same poll would otherwise
   drive the leaver back in as a "missing" member first (found by the B9b probe at
   build time; see BUG-VERDICT.md's addendum). Guards keep 0.5.2 semantics everywhere
   else: offline players are never detected (a quit is not a leave — EM drops
   quitters itself and materialise-on-login rebuilds their face, proven by regression
   scenario L6), and the login watch must settle first (the watch now ends on a
   settled picture instead of at first sight, so a relog is never mistaken for a
   leave). New config key `mesh.em.door-leave-ends-party`, default `true`; `false`
   restores the 0.5.x view-only semantics. This supersedes DESIGN §4 L4 for
   deliberate leaves; L4's protection of leader-reform and materialise is untouched.
2. **mcMMO-door removals cascade into the EM leg** (`McmmoSync.quietLeave` →
   `Controller.cascadeEmRemoval`): the mcMMO leg is still never pushed back into
   (mirror=false stands — no feedback loop), but the removal now also drives the
   EliteMobs leaves and records the removal in EmSync, so Case B adoption cannot
   resurrect the party from the untouched EM roster. `Controller.tick()` already
   runs the mcMMO poll before the EM poll, so the suppression lands first.
3. **Suppression is roster-keyed** (`EmSync.suppress(id, roster)`): the hold compares
   ROSTERS, not the EM partyKey. A removed player stays held while EM lists them in
   any party made only of the people they were removed from — including a twin
   re-created under a fresh Party UUID (EliteMobs `Party.getId()` is a
   construction-time final UUID, bytecode-verified during Phase 1) — and clears only
   when EM shows them partyless or grouped with someone new. `Controller.announce`
   passes the `PartyManager.Change` roster (removed + remaining) as the hint.

### The console admin surface (Mike's rule, Nov 3, 2026)

"We should always have a way to help the players." `/excelparty admin` is routed in
`ExcelTogether.onCommand` BEFORE the player-only check — the 0.5.2 failure was the
command rejecting non-player senders with "Players only." (proven live Nov 3,
11:32 PM CST, during the incident). Console always allowed; a player needs the new
permission `excelparty.admin` (default op, declared in plugin.yml). Logic lives in
`Controller.adminCommand` (unit-testable without Bukkit):

- `admin status <player>` — the Together party (size, leader, members, offline
  marks), what each mirror currently shows (mcMMO party name/leader/size, EliteMobs
  leader/size), and the suppression state in both syncs.
- `admin remove <player>` — removes them through the SAME semantics as a voluntary
  leave: `PartyManager.leave` + `announce(mirror=true)` (both links' leave drives +
  suppression in both syncs), the removed player told "You were removed from the
  party by an admin.", the rest told the same in announcement form.
- `admin disband <player>` — disbands that player's party: every member leaves via
  the same primitive, then one round of mirror drives + suppressions for all, each
  member told "Your party was disbanded by an admin."

Name resolution (`NameMatch`) covers online players plus everyone in a Together
party, so an offline member inside the grace window is still reachable. Tab
completion offers `admin` (+ subcommands + player names) to console/permitted
senders only.

## Base proof (delta discipline)

The unmodified **0.5.2** tree was rebuilt BEFORE any 0.5.3 edit (in
`/tmp/basecheck-052`):

    ExcelTogether-0.5.2.jar: sha256 cb0cb309d81b5d5dd8400688f4ff83f7d815cb7b675fdb679e985c096bb91d2a
    (171,337 bytes) — byte-identical to build-0.5.2's shipped candidate; full suite
    826 checks, 0 failures in that tree.

The 0.5.3 tree started as a full copy of build-0.5.2 and the delta was applied to
it. The regression suite (`LeaveSticksTest`) was run against that pristine 0.5.2
build first: **19 passed, 14 failed** — every convicted path reproduces on 0.5.2
(the EM-door leave, the mcMMO disband, both relog variants, the partyKey-rotation
suppression defeat), and the disconnect-is-not-a-leave guard passes on both
versions. Nothing was ported from anywhere else.

## Candidate

    ExcelTogether-0.5.3.jar
    sha256  494073dcabf05ad0637846e21095faea0cef46046f09a7d50d0987f19f43f9f5
    size    176,042 bytes — build.sh assembles twice and hash-asserts; a second,
            independent build.sh run reproduced the same hash.

## Change scope vs the 0.5.2 candidate jar (entry-by-entry)

- ADDED: none. REMOVED: none.
- CHANGED (9 of 90 entries; the other **81 are byte-identical** to 0.5.2):
  `Controller.class` + `Controller$1.class` (the admin branch, cascadeEmRemoval,
  the roster hint in announce, the player-directory hook),
  `EmSync.class` + `EmSync$Op.class` + `EmSync$Op$Kind.class` (the EM-door leave,
  the addOne funnel guard, roster suppression, the watch-settle change),
  `McmmoSync.class` (the quietLeave cascade call),
  `ExcelTogether.class` (admin routing before the player-only check, tab
  completion, directory + door-leave config wiring, the 0.5.3 awake line),
  `config.yml` (the `mesh.em.door-leave-ends-party` key + comment),
  `plugin.yml` (version 0.5.3, the `excelparty.admin` permission, description +
  usage sentences).
- Assembly note (the provenance rule bites here): build.sh's CHANGED tuple gained
  `'McmmoSync'` in 0.5.3. Without it, the hybrid assembly would have silently
  carried the shipped 0.4.3 McmmoSync bytes (which differ from a recompile) and
  dropped the cascade fix from the jar while every unit test still passed against
  the compiled classes. The tuple in `build-0.5.3/build.sh` is the record.
- One inherited test changed expectation: EmSyncTest's B9b block (the §4 L4 leg)
  asserted the convicted behavior (record unchanged, leaver driven back in). It now
  asserts the 0.5.3 verdict — the EM-door leave ends the membership, the rest of the
  party stands under the next leader, EM keeps its promoted leader — and is labelled
  as rewritten in the test source. `PartyFeatureTest`'s version pin moved
  0.5.2 → 0.5.3 with the bump. No other inherited test was touched.

## Build inputs

Unchanged from 0.5.2 (JDK 25 at `~/workspace/.jdk25`, `javac --release 21`, the
hash-pinned Magic 11.2.5 compile jar `79e49fe3…`, the hybrid assembly against
`work/pulled-0.4.3.jar`: 43 entries compiled byte-identical to shipped and kept,
13 carried as shipped bytes, 32 from 0.5.3 source) except: the version pins, the
CHANGED tuple above, and the two new test run lines (LeaveSticksTest,
AdminCommandTest).

## Companion deliverables in this folder

- `BUG-VERDICT.md` — the Phase-1 mechanism conviction (code paths cited) + the
  build-time addendum on the addOne funnel race.
- `BUILD-RECORD.md`, `TEST-RESULTS.txt` (unit + rig evidence),
  `CHANGELOG-0.5.3.md`, `INSTALL-PACKET.md`, `MANIFEST-ENTRY.md` (the filed queue
  entry), `COVERAGE.md` (the other Excel plugins' console-admin coverage note).
- `ExcelTogether-0.5.3-src.zip` — src + tests + build.sh + plugin.yml + config.yml.
- `rig/` — the scratch Paper 26.3-235 rig (server clone + PartyAdminRig driver
  source) and the boot log `boot-admin-console.log`. Rig-only; never shipped.
