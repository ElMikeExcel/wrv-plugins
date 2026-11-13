# ExcelSummons 0.1.6 — Adopted pets hold no mana + sync clamp (design summary)

An Excel plugin note for Wink Reyes Valley. November 12, 2026.
Defect of record: the unnumbered item at the end of
`~/workspace/goals/family-minecraft-server-build/hidden_files/parent-backlog.md`
("Summons reserve over-ceiling on re-derivation (Jester report,
Nov 12, 2026)"). Base: the shipped 0.1.5 tree, clean-room
reproduced byte-identical before any edit (see BUILD-RECORD.md).

## The defect

Jester reported Nov 12, 2026 ("why my mana so low", Mana 35 of a
200 Master pool). Live pulls verified the cause: his mage carried
wrv_summon_reserve_wolf5 + cat2 + familiar3 + mount — 5 adopted
wolves ×10% + 2 cats ×5% + 3 familiars ×5% + mount ×10% = **85%
held**. The live registry confirmed the menagerie: 5 wolves +
2 cats + 3 familiars + 1 mount, **all origin=adopted**. The 75%
ceiling + 20% usable floor (live config reserve-ceiling-percent
75, reserve-floor-percent 20) were enforced at PreCast ONLY; the
login/sync re-derivation (`SummonsListener.syncReserve` diffing
`ReservePolicy.desiredModifiers`) applied the raw sum, so the
re-derivation could hold more than any cast would ever be allowed
to add — over the ceiling and under the floor at once.

## Mike's ruling (Nov 12, 2026, ~3:08 PM CST) — the binding spec

"If they are original non summonsed Pets they shouldn't hold
mana. Someone took the time to tame something and or breed
them." Adopted-origin summons contribute **0% reserve,
permanently**. Cast summons keep their rates (combat 10/15/25/
20/25, summoned wolf 10, familiar 5, cat 5, mount 10). The
sync/re-derivation clamp to the 75% ceiling + 20% floor still
applies, to cast sets.

## What changes vs 0.1.5

- **Adopted exemption (`ReservePolicy`).** Every derivation —
  `reserveCounts`, `totalPercent`, `desiredModifiers` — skips
  `origin=adopted` records through one funnel (`holdsReserve`:
  CAST only). Adopted records still count toward the rank count
  cap (CapPolicy untouched) and are still never `dismiss pets`
  targets; they simply never hold mana, on any path: PreCast,
  login sync, drift sweep, or the sweep adoption that registers
  them (which now re-derives knowing the pet itself holds
  nothing).
- **Sync clamp (`ReservePolicy.desiredModifiers`, new
  ceiling/floor overload).** The applied state is built per
  record in registry order: a cast record's hold is included
  only while the running total stays within
  `min(ceiling, 100 − floor)`; a record that would pass the cap
  is skipped — it stays registered and alive, it holds nothing
  — and a smaller later record that still fits is still
  included (skip-and-continue, decided at build time and pinned
  by test). Included per-family counts ride the same
  count-tiered modifier keys as before, so a clamped set's keys
  are a subset of the keys the raw set could produce (no new
  Magic templates needed). The listener passes the LIVE config
  ceiling/floor; the old three-argument form clamps at the
  shipped defaults (75/20).
- **One truth everywhere.** `appliedPercent` (the desired sum)
  is what the PreCast gate reads as "already held" and what
  `/summons list` + the admin list print as the held total;
  `recordHolds` replays the clamp walk per record so a list
  line claims a hold only when that summon actually carries
  one (adopted and clamped-out summons print no mana clause).
  `totalPercent` remains the RAW cast total (unclamped) — the
  registry truth the diagnosis and tests read — and its javadoc
  says so.

## What does not change

Caps, count rules, dismiss scopes, messaging texts, nameplates,
the target engine, lifetimes, the archer guarantee: 0.1.5's,
byte-for-byte where untouched. **No config change** — config.yml
is byte-identical to 0.1.5 (the exemption and the clamp are
code, not configuration), no Magic modifiers/messages merge, no
data-field change: summons-data.yml rows already carry `origin`
(since 0.1.4), so rollback is the plain jar swap in both
directions. Legacy rows with no origin key restore as ADOPTED
(the pre-existing safe default) — under 0.1.6 that also means
they hold nothing, the same safe direction.

## Effect for Jester on install

At his first login sync under 0.1.6, the desired state for his
all-adopted menagerie is EMPTY: wolf5 / cat2 / familiar3 / mount
modifiers are all removed by the sync diff and his full 200 pool
returns. His animals are untouched — registered, alive, and
still counting toward his summon count cap exactly as before.

## Proof shape

SummonsTest carries the spec: the carried 74 tests (the reserve
fixtures re-based to CAST origin — under the ruling, the old
default-origin fixtures ARE adopted records and must hold
nothing) plus 4 new 0.1.6 tests — fixture (a) Jester's exact
all-adopted menagerie → applied 0%; fixture (b) a 90% raw cast
set → clamped at sync to exactly 75% with usable 25% ≥ floor,
the excess summon kept and holding nothing; mixed-origin
same-family separation; and the floor cap binding below a
higher ceiling. The desk rig re-runs the 0.1.5 exercise with
its reserve expectations corrected to the ruling (adopted holds
nothing) plus a new phase G: the menagerie fixture and the
over-ceiling cast clamp proven against real Magic modifier
state on real spawned entities. Jester's post-install proof
(his usable mana back at the full pool) stays the live proof,
named in INSTALL-PACKET.md.
