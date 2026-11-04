# ExcelBossTrial 0.3.6 — CHANGELOG

Release date: built 2026-11-02 (pending install). Base: ExcelBossTrial
0.3.5 (FROZEN candidate jar, 350,952 bytes, SHA-256
`3364c152202eb864c16240dc48efc68241a5de7ab4ef25419247867443bf5dc0`),
rebuilt byte-identical from its tree before any change — see
`base-verify/BASE-PROOF.md` in the build folder.

0.3.6 is the **FEEL PATCH** (Mike, Nov 2, 2026, approving the feel
audit's option (e)): seal fights finally SHOW their numbers. The
damage governor never flattened real damage — a netherite sword
always hit the elder's pool 2.75x harder than a wooden one — but it
flattened PERCEPTION to zero: every hit in every seal fight moves
the vanilla boss bar less than one pixel, so no player could see
their gear working. 0.3.6 changes no fight math at all. It displays
the fight the ledger already judges: the boss bar carries the true
pool numbers, and every hitter sees their own damage totals as
they land. Fight length, pools, caps, ramp, reflect, verdicts,
rewards: identical, proven by a dedicated regression suite that
runs the same hit sequence with the display observing and without
and demands byte-identical ledgers.

## What players see

- **A numbered boss bar on every seal fight.** Title:
  `Elder Guardian — 8,431 / 10,900` — the true remaining pool and
  the true total, US-grouped, updated on every recorded hit. A
  wooden sword takes 4 off the number; a Sharpness V netherite
  sword takes 11; a crit 16.5; a Power V bow about 24. Gear tiers
  are visible again.
- **The Warden gets a bar.** Vanilla gives the Warden no boss bar
  at all; BossTrial now supplies its own, with the same true
  numbers.
- **Wither and dragon fights keep ONE bar** — BossTrial's
  numbered one. The vanilla wither/dragon bars show only a
  fraction of the same fight (by intake design the fraction is
  the ledger's), so they are hidden while the encounter is open
  and restored when it closes. Elder guardians have no vanilla
  bar (verified against the Paper 26.3 jar: no boss-event
  machinery exists for them), so nothing changes hands there.
- **Your damage, as you deal it.** After each of your hits, the
  action bar shows `16.5 hit · 31.5 total damage` — your last
  governed hit and your fight total. The total IS the fight
  table's number: the same per-player dealt the verdict record
  and ExcelProtect's fight summary carry. A hit the share band
  refuses (wrong height/range) raises the pool total on the bar
  but never inflates your personal total — the display follows
  the ledger, never invents.
- **The dragon's bar is priced per phase.** The title total is
  the current phase's pool (phase 1, then phase 2 at the flip) —
  the same pricing the chapter's own governor judges by.
- **Opt out any time:** `/bosstrial feedback off` removes you
  from the boss bar audience and stops your action-bar totals;
  `/bosstrial feedback on` restores both. Your choice persists
  across restarts (state.yml). Other players' bars are
  unaffected. Chat stays clean either way — feedback is screen
  furniture only, never chat.

## The display rules (why it can't lie, and can't break a fight)

- The display reads ledger getters ONLY (`totalDamage`,
  `maxHealth`, `dealtBy`) after the record calls have run. It
  never writes to the ledger, the governor, or any fight state.
- The ledger-equality regression (FeedbackNoDamageChangeTest)
  runs a scripted sequence — plain hits, a crit, a per-event-cap
  trim, a same-tick explosion-pool starve, a banded exclusion,
  taken-side hits including a TrainingWheels-shaped quarter and
  the 0.3.5 reflect value at multiplier 0.0 — with the feedback
  observer computing after every record, without it, and with
  feedback configured fully OFF. All three ledgers come out
  identical in every judged field.
- Fail-soft: if the display path ever throws, it logs once,
  removes its bars, restores the vanilla bars, and disables
  itself for the rest of the boot. The fight cannot notice.
- The 0.3.4 FightTableEvent → ExcelProtect path and the 0.3.5
  elder reflect fix are untouched (jar-level diff: the emitter
  classes are byte-identical to 0.3.5).

## Configuration (all new, all adjustable)

New `feedback:` block in config.yml with the settled defaults:

- `pool-numbers: true` — the numbered boss bar. False = no
  BossTrial bar at all (vanilla bars stay as vanilla).
- `damage-total: actionbar` — per-player totals: `actionbar`
  or `off`.
- `title-format: "{name} — {remaining} / {total}"`
- `damage-total-format: "{last} hit · {total} total damage"`
- `viewer-range: 64` — who sees the bar: fight participants
  always, plus online players within this range of the boss.
- `damage-total-min-interval-ms: 500` — action-bar throttle.
- `title-tick-ms: 1000` — the reconcile sweep cadence (also the
  dragon phase-flip pickup bound).

Per-chapter overrides: each chapter section accepts its own
`feedback:` block with `pool-numbers` and/or `damage-total`;
each key overrides the global independently (commented examples
ship in the config). A junk value anywhere degrades to the
settled default — a typo can never silently switch feedback
off, and never crashes a boot. Both switches are read untyped:
in YAML an unquoted `off` parses as a boolean, and the plugin
honors that shape as off rather than letting a typed read
resurrect the display (rig finding F4, fixed before ship).

State: `state.yml` gains one additive key, `feedback-opt-outs`
(the opted-out player UUIDs). Old state files load unchanged;
broken rows in the list are skipped, never fatal.

## Deliberately NOT in 0.3.6

- **Floating damage numbers (TextDisplay entities)** stay out:
  they ship only when a rig with Geyser aboard verifies a
  Bedrock client renders them (king rule: tested and proven
  before it ships). The title + action-bar surfaces are the
  settled 0.3.6 feedback; numbers over the boss's head are a
  later, separately-proven addition if Mike wants them.
- No fight-math option from the audit's menu (vulnerability
  windows, crit bypass, ledger amplification) — Mike chose
  feedback only; durations change by exactly zero.

## Tests

Full suite green: 27,068 checks (0.3.5's 26,949 inherited plus
119 new), 0 failures, including FeedbackFormatTest (25),
FeedbackSettingsTest (35), FeedbackTotalsTest (20),
FeedbackOptOutsTest (14), FeedbackNoDamageChangeTest (16), and
the CommandAccess matrix extension (+9: `/bosstrial feedback`
is a player's own-screen toggle, allowed for players and ops
alike, never a stranger path into admin verbs). Built twice from
clean copies: byte-identical jars. Rig-proven on Paper 26.3-235
(see the build folder's TEST-REPORT.md): exact title decrements
at the duel pool 10,900 (wood 4 / Sharp V 11 / crit 16.5 / bow
23.8), the warden's supplied bar, wither vanilla-bar suppression
holding past 60 s and a chunk excursion and restoring at close,
the dragon title switching pools at the phase flip, opt-out
removing audience + action bar and restoring both, the kill
switch booting dark with an identical verdict path, and a
deliberately broken feedback config booting clean on sanitized
defaults.
