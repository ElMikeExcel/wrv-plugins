# BUILD RECORD — ExcelBossTrial 0.5.6 (cash taper + seal cycle + per-player first-break extras)

Built Nov 11, 2026 (desk lane, board supervisor dispatch).
Commission: Mike, Nov 11, 2026 — "Build it now, install after
Saturday." Live BossTrial is 0.5.5 (in
`ExcelBossTrial-0.4.1.jar`) and stays untouched; the Warden
attempt Sat Nov 14, 2026 runs on 0.5.5. Desk only: no panel
access, no installs, no MANIFEST edits (a draft entry is
staged beside this record), no sends.

## Candidate jar (FINAL)

- `~/workspace/bosstrial-056-build/ExcelBossTrial-0.5.6.jar`
  — **469,839 B / sha256
  5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c**
- Build ID: **EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56**
  (fresh tag; 0.5.5's 887e6f, 0.5.4's 5371d5, 0.5.3's 35bd2b,
  0.5.2's 13447a and 0.5.1's a3aa16 never reused).

## Base proof

- **True 0.5.5 source tree: `~/workspace/bosstrial-055-build/bosstrial/`**
  — verified from its BUILD-RECORD (0.5.5 record, build ID
  …-887e6f) and proven in this lane: a pristine `package.sh`
  rebuild of the untouched copy is byte-identical to the
  shipped 0.5.5 jar (455,307 B / eade52aa…, `work/base-pristine.jar`,
  log `work/base-build.log`), suite green (2,588 checks /
  92 harnesses by the house tally) before any 0.5.6 edit.
- `~/workspace/excelbosstrial/` was checked and rejected as
  the base: its lineage tops out at 0.2.x builds.

## Scope, as built

**1. CASH TAPER (Mike's law Nov 10; scope confirmed Nov 11).**
New pure core `core/CashTaper.java` is the single price table
for every cash path: per-player per-seal LIFETIME counts —
Warden clear #1 $10,000; every other seal's #1 $5,000; #2
$5,000; #3+ $2,000. Counts never reset across cycles (the
cycle boundary re-keys cycle books only; the lifetime ladder
in `clearCounts` is never cleared). Host pricing reads the
count `recordRepeatClear` just incremented (increment-then-
price), per present champion, on: the repeat layer, the
Warden first-break layer (a first-timer prices exactly the
0.5.5 D6 total of $10,000), a NEW first-break cash payment
for non-Warden CREDIT kills (0.5.5 paid cash only on repeats
and the Warden first break; the taper's clear-#1 price now
applies to every seal's first break), and the raid repeat
path (priced from GuildGate's per-winner counts after the
clear). CASH-ONLY: mcMMO XP (5,000 SWORDS), MagicLevel XP
(250), jackpot +2 rolls (Underdog scaling unchanged) and the
war chest pay IN FULL at every count — pinned by
CashTaperTest against the settled specs/config defaults.

**2. SEAL-CYCLE MECHANISM (reset law Nov 10 + D7; design
`~/workspace/sealcycle-20261110/DESIGN-AND-BUILDSCOPE.md`).**
This jar carries the BossTrial half of the design:
- `core/SealCycle.java` — the cycle mirror: lifetime broken
  set (history, never cleared — a broken seal stays broken),
  cycle count, the `guildOpened` latch (set once at the
  first 5/5, first timestamp kept forever; `eliteDoorOpen()`
  reads the latch — stage 5 / the Elite door stays open),
  cycle archive + increment ONLY at 5/5.
- Boundary handling (design C-4): the host credits its mirror
  on every CREDIT (and on the raid break event, the raid
  being the fifth seal BossTrial does not judge); at a
  completion, `handleCycleBoundary()` clears the credited-
  seal fallback + repeat windows and reopens per-cycle clear
  counts. A reflective `SealCycleEvent` listener (the raid
  adapter's fail-soft shape) re-syncs from a cycle-built
  GuildGate when one is present; with live GuildGate 0.4.0 it
  logs inert and BossTrial's own credit paths carry the
  mirror. **The GuildGate half of the design (SealState
  latch, cycle-scoped API, the event itself) is a companion
  build, NOT in this jar** — the packet says so plainly.
- Elder partial-progress expiry (law (a), design §3.3):
  `MonumentTracker` stamps `countedAtMs` on every counted
  elder; the window opens at the FIRST counted kill and does
  not slide; `expirePartialTrio` flips a stale 1/3 or 2/3 to
  a due respawn (complete trios refuse — generation turnover
  owns them). Config `elder-guardian.progress-expiry-hours`
  (default 24, 0 = never). Enforcement at all three design
  points: before counting a new elder, in maintenance, and
  at boot. Pre-timestamp counted elders migrate to a fresh
  window at boot (design §3.5). The progress broadcast gains
  the window's remaining time (E-5), and expiry broadcasts
  `messages.elder-progress-expired` once per expiry.

**3. PER-PLAYER FIRST-BREAK EXTRAS (Mike, Nov 11).** New
`core/FirstBreakBook.java`: extras pay the first time THAT
PLAYER personally breaks the seal — once ever per player per
seal, in any cycle; the book is never cleared by a cycle.
Host wiring: on every CREDIT (non-progress) kill, present
champions are marked in the book and the state flushes
BEFORE any extra moves. The base (non-reforged) signature
distribution pays only marked-fresh champions on a CREDIT
kill (the reforged repeat signature is a repeat-slate row
and pays the roster as before); the Warden trophy + EM kit
pay only personally-fresh champions — including on a later
cycle's break, where the Warden layer now also runs for a
personally-fresh party (cash taper-priced, accelerants/war
chest in full). Party/server one-time elements stay single
historical events: the "Sealbreakers" title and headline pay
only on the guild-once fresh break (`guildOnceFreshThisKill`),
and the guild-hall plaque record remains the 0.5.5
once-flag + FIRST-BREAK log line. The Raid Village Standard
exception is untouched: `raidBreakPay` notes the personal
first break for the record, gates nothing on it, and the
Standard pays exactly as before. (One scope note for
review: raid FIRST-break cash is not added — the raid
companion SPEC of record pays the Standard on a break and
cash on repeats; the taper prices the raid repeat path,
where GuildGate supplies the counts.)

**4. COUNTER SEEDING from the record** (`core/CounterSeeding.java`,
settled table listed here for review):

| Player | Seal | Seeded lifetime count | Consequence |
|---|---|---|---|
| Marvin | elder_guardian | 2 | next Elder clear is #3 → $2,000 |
| Jester | elder_guardian | 2 | next Elder clear is #3 → $2,000 |
| — (nobody) | wither | NO SEED | Nov 7 break was a manual grant; first PAID clear pays #1 |
| — (nobody) | ender_dragon | NO SEED | parties' clear #1 already stands in the paid book |
| — (nobody) | raid | NO SEED | clear #1 stands in GuildGate's book |

Seeds apply from state.yml's `counter-seeds` section
(UUID-keyed, written at install from the live usercache /
state — names are never resolved by the plugin, so a seed
can never land on a name-hash UUID) under a MAX merge:
a seed raises a count to at least the seeded value, never
lowers the book, and is idempotent.

## Tests

Full suite via `package.sh` (both builds): **0 failures —
2,765 checks / 98 harnesses**, vs the base proof's 2,588 / 92
with the same tally (`work/tally.py`). Delta +177 / +6 =
CashTaperTest 42 + SealCycleTest 58 + FirstBreakBookTest 18
+ CounterSeedingTest 13 + MonumentExpiryTest 29 +
SimulatedClearPayoutTest 14 (all new) + PluginYmlParseTest
+3 (60 total). Detail: `TEST-RESULTS.txt`.

## Deterministic double-build

`package.sh` twice → byte-identical: `work/pkg1.jar` =
`work/pkg2.jar` = the candidate (469,839 B / 5cd9f76b…).
Jar contents: 241 entries (0.5.5: 234; +7 = `CashTaper`,
`CounterSeeding` + its `Seed` inner, `FirstBreakBook`,
`SealCycle` + its `CreditResult` inner, and the host's new
anonymous cycle-listener class); host / MonumentTracker /
BuildInfo changed in place under existing names.

## Fingerprint verification (release gate)

Layer B homes all carry the new ID, verified in the built
jar: (1) `excel-build.txt` at the jar root; (2) the
fragment-assembled constant in `core/BuildInfo.class`
(3c9e56 present, 887e6f absent — string scan); (3) the boot
surface line `Build: EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`,
observed in the rig boot below.

## Rig behavior proof (desk rig, candidate jar)

A copy of the 0.5.5 desk rig (`rig-056/`, Paper 26.3-235,
live GuildGate 0.4.0 jar included) with the candidate jar
swapped in and two seeded state facts: a `counter-seeds`
entry (RigPlayer → elder_guardian 2) and a stale partial
trio (one COUNTED elder stamped 25 h old, one TRACKED
sibling). Two boots were run; boot 1's log was overwritten
by boot 2's, and what each proved is stated exactly:

- Boot 1 (log overwritten): `Counter seed applied:
  elder_guardian / 14d09c01-… now stands at lifetime clear
  #2` at load; the merged count persists in the rig's
  state.yml `repeat-clears` (still there after boot 2).
- Boot 2 (`work/rig-boot.log`, 53,165 B / sha256
  bb82ee2e4c336713273b6ab7f13258f8ceb042920bee4967826ba6cafb57fcbd),
  **SEVERE 0**: `Enabling ExcelBossTrial v0.5.6`; the taper,
  cycle, expiry and first-break boot lines exact (cycle
  listener inert against GuildGate 0.4.0, as designed); and
  the **full law-(a) loop on the real server**: the boot
  sweep logged `BossTrial elder progress EXPIRED: monument
  monument-1's partial trio (1 counted elder(s)) passed the
  24.0h window — progress returns to 0 of 3 and the elders
  respawn`, after which the maintenance respawn path
  respawned the flipped elder at its home — its state
  record now stands TRACKED under a NEW UUID
  (beb528c2-…) at the same home position. The rig's standing
  RigDriver warden probes ran with no BossTrial error; the
  only BossTrial WARN class is the standing EliteMobs-seam
  fail-soft, same as the 0.5.5 rig; remaining stack traces
  are offline-environment noise (Mojang services).
- Payout paths: the desk rig still carries no verdict-
  driving script (the 0.5.5 scope limit stands), so simulated
  clears at counts 1/2/3+ are proven by
  `SimulatedClearPayoutTest`, which replays the host's exact
  increment-then-price sequence through `CashTaper`,
  including the seeded Elder pair ($2,000 next clear) and a
  mixed veteran/newcomer party pricing per player. The
  Nov 14 Warden fight runs on 0.5.5 and is not this build's
  proof; 0.5.6's first live clear is.

## Design deviations

None against the commissioned scope. Two boundaries stated
plainly: (1) the GuildGate half of the seal-cycle design is
a companion build (above); BossTrial's mirror + listener are
built so the pair lands as one packet when GuildGate's cycle
build exists. (2) Raid first-break cash was not added (scope
note in §3 above); if Mike wants the taper's #1 price on a
raid break too, it is a one-method change in `raidBreakPay`.
