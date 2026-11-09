# DOWNED STATE + RESURRECTION — DESIGN (ExcelDowned, working name — PROVISIONAL)

Commissioned by Mike, Nov 8, 2026, ~1:22 PM CST. His spec, verbatim in the
parent task: lethal damage puts a player into a DOWNED state instead of
death — health frozen, cannot hit, moves very slowly, reduced damage from
mobs, deaggroed, "basically no function because they are dead" — and a new
spell, Resurrection (10-minute cooldown, in and out of combat), casts on a
party member and brings them back at 50% health. "They never lose anything
if you get them in time." A downed player can GIVE UP, dying on the spot,
if they'd rather run back to their stuff. Author credit: Iris / Mike.

## 0. Flags — Mike's calls, built on recommendations

| # | Decision | Built default | Status |
|---|---|---|---|
| F1 | **Bleed-out window** | **300 s (5 min)** | ✅ **CONFIRMED by Mike, Nov 8, 2026, ~4:31 PM CST** (voice call) — long enough for a boss-fight rescue run, short enough that a down is an emergency, not a nap. One config key (`bleed-out-seconds`). |
| F2 | Resurrection mana cost | **100 mana** | ✅ **RULED by Mike, Nov 8, 2026, ~4:31 PM CST** (voice call) — built at 50, raised to 100 pre-install; the 10-min cooldown remains the real gate (Mike's design), and a revive now costs a student-rank mage (100 pool) their whole pool. Applied to the spells packet the same day (see the packet's FILING-RECORD, MANA RULING section). |
| F3 | Resurrection stage unlock | **Stage 4** | ✅ **CONFIRMED by Mike, Nov 8, 2026, ~4:31 PM CST** (voice call) — the marquee healer capstone, arriving with the stage that completes the book (stage-5 book is the stage-4 twin). Alternative if Mike wants it earlier: stage 3, beside Group Renewal. |
| F4 | Plugin name | **ExcelDowned** | Provisional working name. |
| F5 | Downed damage factor | **0.25** | Matches TrainingWheels' Rylie factor; see §3 for how the two compose. |

## 1. Lethal intercept — death does not happen

ExcelDowned listens to `EntityDamageEvent` at HIGHEST. If the victim is a
player, the cause is not VOID, and the event's final damage would take
them to ≤ 0 health, the event is **cancelled** and the player enters
DOWNED instead: no PlayerDeathEvent, no drops, no death record, no
respawn screen. Inventory, XP, location — everything stays exactly where
it was. "They never lose anything if you get them in time" is therefore
structural, not a restore-from-backup promise: the death never ran.

**Downed health representation: health is set to 1.0 (half a heart) and
frozen there.** Justification: (a) 0 is death — unusable; (b) a visible
sliver on the health bar reads instantly as "down but not dead" on both
Java and Bedrock with no custom UI; (c) while DOWNED, every further damage
event is absorbed by the state (§3) and every health-regain event is
cancelled (§5), so the value cannot drift up or down — it is a state
marker, not a resource. Food/saturation are left alone (hunger can't kill
in this ruleset and freezing health makes it moot while downed).

## 2. Downed restrictions — "basically no function"

While DOWNED, the registry (keyed by UUID, not by session) enforces:

- **No dealing damage:** `EntityDamageByEntityEvent` from a downed damager
  is cancelled, including projectile shots whose shooter is downed
  (`ProjectileLaunchEvent` also cancelled at the source).
- **No casting:** a Magic `PreCastEvent` listener cancels every cast by a
  downed mage, no exceptions — including Resurrection (you cannot revive
  yourself; §6 requires a living caster anyway).
- **No world interaction:** block break, block place, item use
  (interact), item drop, hand swap, item pickup, item consume, and
  container opening are all cancelled.
- **Movement very slow:** Slowness V (−75% speed) with hidden particles,
  re-applied by the state task so milk/cure/potions can't clear it, plus
  sprint is forced off. A potion effect is used deliberately: it is
  server-side, identical for Java and Bedrock clients, and needs no
  attribute surgery that other plugins could fight over.
- **Allowed:** chat, commands (so `/giveup` works), being healed at
  (heals land but don't lift the state — §5), and being revived.

## 3. Reduced damage + deaggro while downed

- **Deaggro on entry:** every mob within 32 blocks whose target is the
  player has its target cleared. **Re-target scrub:** `EntityTargetEvent`
  / `EntityTargetLivingEntityEvent` aimed at a downed player are
  cancelled outright, and a 40-tick scrub task re-clears any mob found
  targeting a downed player (covers plugins that set targets directly).
- **Reduced damage:** a downed player cannot lose health (frozen, §1), so
  "reduced damage" is implemented where it can still matter — **time**.
  Damage that reaches a downed player is scaled by **×0.25** and the
  scaled amount **eats the bleed-out window at 1 second per 1 HP**
  (`damage-seconds-per-point: 1.0`). A zombie chewing on a downed player
  (3 HP → 0.75) shortens the rescue window by under a second per hit —
  noticeable in a pile-on, irrelevant from a stray hit, and the deaggro
  rules mean sustained chewing requires the mob to have no other option.
  The scaled amount is recorded per hit (§7).
- **TrainingWheels composition:** TW's 0.25 (Rylie) and this 0.25 are both
  multiplicative scalings of the same event, so they compose to ×0.0625
  regardless of listener order — Rylie downed takes a quarter of a quarter.
  The lethal intercept reads the event's *final* damage, so TW's
  protection also composes at the door: a hit TW already shrank below
  lethal never downs her in the first place.

## 4. Bleed-out, give up, and the exactly-once real death

- **Bleed-out:** at `deadline = downedAt + bleed-out-seconds` the real
  death path runs **exactly once**: state is removed first, a bypass flag
  is set, and health is set to 0, so vanilla fires one genuine
  PlayerDeathEvent — drops, death record, Lost & Found, respawn, all the
  normal consequences, exactly as if the lethal hit had landed.
- **Give up:** `/giveup` is two-step: first use arms it (10 s window,
  explicit chat prompt "type /giveup again to let go"); a second use
  inside the window runs the same exactly-once death path immediately.
  Outside the window the second use simply re-arms. Two-step because a
  fat-fingered death while fumbling for chat is the exact accident this
  command must never cause.
- Exactly-once is a state-machine property, unit-tested: every exit path
  (revive, give-up, bleed-out, external death) consumes the session; a
  second exit attempt finds no session and does nothing.

## 5. Edge cases — ruled

- **Void:** void damage never triggers the downed state — falling into
  the void is a real death, full stop (a frozen 1-HP player hovering in
  the void helps nobody). If a downed player somehow reaches the void
  (knocked off a ledge while downed), the real death path runs.
- **Lava/fire/DoT while downed:** these are damage events like any other:
  scaled ×0.25, they eat the window (lava eats it fast — being downed in
  lava is effectively a short window, which is correct), health stays
  frozen. Fire is not extinguished by the state; rescuers should drag
  aggro, not the body — bodies can't be moved by this design (a ruling:
  no body-dragging in v1; the revive comes to the body).
- **Fall damage while downed:** ignored (event absorbed, no window cost)
  — a prone body takes no new injury from being dropped a short way; the
  lethal intercept already prevents fall-death from standing, and small
  falls while downed are not a second punishment.
- **Healed while downed:** normal heals (spells, potions, regen) are
  *received* — Protect records them (§7) — but the health change is
  cancelled at the event: health stays frozen at 1.0 and the state does
  NOT lift. Only Resurrection revives. This keeps "down" meaningful and
  stops a splash potion from becoming a 10-minute-cooldown bypass.
- **Logout while downed:** the session persists (saved to disk on every
  transition) and the bleed-out runs on **wall clock**. Relog before the
  deadline: state re-applied (health 1.0, slowness, restrictions).
  Deadline passed while offline: the death path runs once, at login —
  drops land at the login spot; not elegant, but honest, and the record
  says bleed_out. Camping a logout to dodge a down therefore gains
  nothing.
- **Server restart while downed:** sessions are reloaded from disk on
  enable; online players are re-marked; deadlines are absolute
  timestamps, so a restart neither extends nor shortens a window.
- **Party wipe / BossTrial:** downs are NOT deaths — BossTrial's ledger
  sees champions still "in" the encounter while downed. Spec for the
  BossTrial lane (phase-2 integration, not built here): poll
  `ExcelDownedAPI.isDowned`; when **every** encounter participant is
  downed AND no standing party member remains in the encounter area,
  close the encounter as a wipe (no-verdict close, like GIVE_UP in the
  0.4.0 record labels); the individual deaths then resolve on their own
  bleed-out/give-up clocks. ExcelDowned itself never touches BossTrial.
- **PvP:** the server is family co-op, but the rules are uniform: a
  player-caused lethal hit downs (doesn't kill), and player damage to a
  downed player eats the window at the same ×0.25. No special-casing.

## 6. Resurrection — the spell

- Key `resurrection`, display **"Resurrection"**, category light,
  attunement class **SUPPORT**, stage 4 unlock (F3), cooldown **600,000
  ms** (Mike's 10 minutes), mana **100** (F2 — built at 50; Mike ruled
  100 on Nov 8, 2026, ~4:31 PM CST), worth 1200, aimed-ally
  targeting shaped like the heal line (range 16, friendly-only).
- **Target rule:** the target must be a DOWNED member of the caster's
  party. Party of record = ExcelTogether/mcMMO mesh, read through the
  fail-soft reflective adapter pattern ExcelSummons established
  (`TogetherPartyProvider` — structural probe, never writes, failure
  answers "unknown", and "unknown" never counts as party for a revive).
- **Revive effect:** target's health set to **50% of max**, state fully
  cleared (restrictions, slowness, nameplate, session consumed), party
  alerted (§8). The revive write is direct (not a regain event), so the
  §5 freeze can't eat it; the exact restored amount is recorded (§7).
- **Cooldown protection (Mike's 10 minutes must never burn on a bad
  target)** — two layers, both built, verdict from the Magic 11.2.5 jar:
  1. **PreCastEvent gate (primary).** `PreCastEvent` is `Cancellable`
     (javap-verified against the shipped jar) and is the API's pre-cast
     hook. ExcelDowned cancels a Resurrection cast when no downed party
     member is within gate range (24) of the caster — a cancelled
     pre-cast means the spell never performs, so no mana and no cooldown
     are spent.
  2. **CastEvent refund (backstop).** If a cast does start but no revive
     happens (aim missed every downed party member), the CastEvent
     listener calls the spell's own `clearCooldown()` (present on the
     API `Spell` interface, javap-verified) and tells the caster why.
     Residual honesty: in that narrow path the 100 mana is spent — only
     the cooldown, the thing Mike's design actually gates on, is
     guaranteed refundable by the API.
- **Delivery mechanism — VERDICT (rig-proven, Nov 8): BOTH paths live.**
  `ActionFactory.registerActionClass` exists in the jar, and ExcelDowned
  registers a `Resurrect` action that performs the revive from the cast
  context's target. The cold-boot worry (registration happens at
  ExcelDowned's enable, after Magic loaded its templates) did NOT
  materialize: the rig boot logged the registration, Magic loaded the
  spell with no unknown-action complaint, the action's `perform`
  revived under a driven context, and a real Magic cast by a fake
  healer revived end-to-end (run 5; full evidence in BUILD-RECORD).
  The CastEvent listener performs the same revive through the same API
  as the guaranteed path, and revive is idempotent in the state
  machine, so the two paths can never double-apply: whichever fires
  first consumes the session; the second finds nobody downed.

## 7. Records — Protect's world

ExcelDowned writes its own JSONL stream in its data folder
(`records/records-YYYY-MM-DD.jsonl`), one line per event, Protect-shaped:

- `downed_enter` {player, uuid, cause, location}
- `damage_while_downed` {player, uuid, source, scaled_amount,
  window_seconds_eaten}
- `revive` {**spell: "resurrection"**, healer, healer_uuid, target,
  target_uuid, amount_restored, effective} — deliberately the same field
  names as Protect 0.1.8's `combat_heal` record, so a revive reads as the
  heal-class fact Mike's Nov 8 standard requires (spell / who / how
  much).
- `give_up`, `bleed_out`, `downed_death_external` {player, uuid}

ExcelDowned also fires real Bukkit events (`DownedEnterEvent`,
`DownedReviveEvent`, `DownedDeathEvent`) and exposes `ExcelDownedAPI`
(isDowned, session snapshot) so Protect and BossTrial can consume live.
**Protect-side ingestion is SPEC ONLY here** (rides the Protect lane): a
`downed` stream in Protect that tails ExcelDowned's JSONL (the G1
translator pattern from the Telemetry retirement) or listens to the
Bukkit events directly; DeathRecord / Lost & Found need NO change —
they see a death only when the real death path runs (give-up/bleed-out),
never for a revived down, because no PlayerDeathEvent ever fired.

## 8. Visibility

- **Downed player:** persistent action-bar line, refreshed by the state
  task: "✝ DOWNED — bleed out in M:SS · /giveup to let go". **Not** the
  boss bar — the bar is the boss's (standing law).
- **Rescuers:** the downed player's nameplate (display + tab list) is
  prefixed "✝ DOWNED " in red for the duration and restored on exit —
  the same nameplate channel the summons health display uses.
- **Party:** chat alert to every party member on down ("✝ <name> is
  DOWN — Resurrection can bring them back (M:SS)") and on revive.

## 9. Files

- Plugin: `build/ExcelDowned/` (source, tests, deterministic build,
  candidate jar `ExcelDowned-0.1.0.jar`).
- Rig: `build/rig/` (desk Paper 26.3 + Magic 11.2.5, NMS fake players —
  the house RigPlayer recipe), results in BUILD-RECORD.md.
- Spell packet: `~/workspace/spell-audit/packet-resurrection-20261108/`,
  sequenced AFTER the healing packet (rebases onto its staged outputs).
