# ExcelProtect 0.1.3 — SPEC: damage DEALT + healing incidents

**Commissioned by Mike, Nov 5, 2026, 12:25 PM CST, in his words:** "we want it
to track damage to mobs. That way if needed we can investigate cases where
people claim to do too little damage or to low. Also let's us know who's doing
what and can even calculate people's damage contributions with this. While
we're at it we should be able to track players self healing and players
healing to other players too."

0.1.3 completes the combat-evidence triangle 0.1.2 started. 0.1.1 records
per-event combat + per-fight sums; 0.1.2 added coalesced damage TAKEN
incidents (victim = player). 0.1.3 adds the two remaining directions as
coalesced incidents in the same discipline: **damage DEALT** (player → mob)
on the `dealt` stream, and **HEALING** (self + player → player) on the
`healing` stream. Same house JSONL style, same Ledger (one async writer,
daily files, 30-day retention + gzip + aggregates), MONITOR-priority
listeners, map work only on the event thread, IO only on the writer thread.

## §1 The incident discipline (shared with 0.1.2)

Consecutive ticks from the same source arriving within
`combat.damage-incident-gap-ms` (default **2000 ms**, clamp 250–30,000) of
the previous tick merge into ONE incident. An incident closes when the gap
expires (`window`, closed by a 1-second sweep or lazily by the next tick),
when its subject leaves (`quit`), or at plugin shutdown (`shutdown`) —
plus, on the dealt stream, when the victim dies (`victim_death`). Every
record carries the Ledger envelope (`ts`/`ts_ms` Chicago ISO + ms, world,
attacker/target start location) and an incident body with
`start_ts`/`start_ts_ms`/`end_ts_ms`/`duration_ms`.

## §2 Damage dealt — `dealt-YYYY-MM-DD.jsonl`, type `damage_dealt_incident`

**Scope boundary (decided): non-player victims only.** Player-vs-player
damage is already recorded from the victim's angle by 0.1.2's `damage`
stream; recording it again attacker-side would double-count every PvP hit
in contribution sums. The dealt stream is the player → mob evidence base.

**Merge identity:** attacker player + victim (stable per-victim key
`<entity-type-key>:<uuid>`) + damage cause + delivery. Delivery separates
what the hand/cause alone cannot: `hand` (direct), `proj:<projectile-type>`
(a projectile hit belongs to its shooter), `spell:<key>` (a fresh Magic
cast claim explains the hit — see §4).

**Fields:** `player`, `player_uuid`, `cause`, `victim` (entity-type key),
`victim_uuid`, `victim_key`, `victim_name` (only when custom-named),
`projectile` / `spell` (only when that delivery applies), elite attribution
(below), `hit_count`, `total_damage` and `max_single_hit` (**final** damage
— post-armor, post-plugin scaling: the number the game actually applied),
the victim health arc `victim_health_start` (health before the first hit) /
`victim_health_end` / `victim_health_min` / `victim_max_health` (event-time
reads), `died`, `closed`.

**Elite victims:** the same reflective EliteMobs probe 0.1.2 verified
against the real 10.9.12 jar (`EntityTracker.isEliteMob` +
`getEliteMobEntity().getLevel()`), applied to the VICTIM. Encoding matches
0.1.2's: elite → `elite:true` + `elite_level:<n>`; probed non-elite →
`elite:false`; probe cannot answer → `elite_probe:"unavailable"` and no
elite claim either way.

**Death attribution:** when a victim with open incidents dies, ALL
attackers' incidents on it close as `victim_death`; `died:true` lands on
exactly one — the killer's (`EntityDeathEvent.getKiller()`), or the most
recently active attacker's when the game names no killer — and that
incident's health end/min read 0. Contribution questions ("who did what
share of this kill") are answered by summing `total_damage` across the
incidents that closed on that victim.

## §3 Healing — `healing-YYYY-MM-DD.jsonl`, type `healing_incident`

**Scope:** `EntityRegainHealthEvent` where the subject is a PLAYER.

**Merge identity:** target + healer identity + source. Sources:
- `SPELL` — a CUSTOM regain paired to a Magic cast claim (§4): healer is
  the caster (the target themself for a self-cast heal), `spell` carries
  the spell key, `reasons` carries `CUSTOM`.
- A `RegainReason` name (`REGEN`, `SATIATED`, `EATING`, `POTION`, `MAGIC`,
  `MAGIC_REGEN`, …) — the target's own recovery systems, so healer =
  target, `self:true`. **Vanilla limit, stated plainly:**
  `EntityRegainHealthEvent` carries no thrower, so a splash potion thrown
  by another player is indistinguishable from one the target drank — it
  records as self-healing. Spell heals are the attributable player→player
  path; potion assists are not, and the record never guesses.
- `CUSTOM` with no claim and no fresh attribution cache: healer **omitted**
  (unattributed), `self:false`. The report counts these separately.

**Amounts — BOTH are recorded; totals use the event amount.**
`total_healed` sums the events' own amounts (what sources tried to heal —
the same basis CombatCore's per-fight sums use, so the layers agree).
`total_effective` is the cap-adjusted sum: an internal cursor starts at
the first event's health-before and each event contributes at most the
missing health at that moment, clamped at max health; `health_end` is that
cursor. When nothing else heals or hurts the target inside the incident,
`total_effective` EQUALS the real health gained and `health_start →
health_end` is the true arc. Also `max_single_heal`, `event_count`,
`health_start`, `max_health`, `reasons` (distinct RegainReasons merged).

**Relation to CombatCore (0.1.1):** the combat stream's `fight_summary`
carries per-fight `healedDone`/`healedReceived` totals per player —
fight-scoped sums. Healing incidents are the per-engagement detail UNDER
those sums: who healed whom, by what source, in what burst, with cap
loss visible. Neither replaces the other; the capture points are shared
(the same claim feed), not duplicated.

## §4 Claim peek ordering (the one wiring subtlety)

CombatCore CONSUMES a Magic cast claim when its per-event listeners pair
it. The incident watches need the same claim without starving that
consumption, so: CombatCore gains `peekClaim(UUID)` (read-only, same 1 s
freshness), and `ExcelProtect.onEnable` registers **DealtWatch and
HealingWatch BEFORE CombatCore** — their peeks run first, CombatCore then
consumes for its own lines exactly as in 0.1.1/0.1.2. HealingWatch also
caches the last spell attribution per target: after CombatCore consumes
the claim, later events of the same burst (inside the gap) reuse the
cache, so one heal-over-time cast stays ONE incident instead of splitting
into a SPELL incident + an unattributed tail.

## §5 Console surface

Following the 0.1.2 `damage` form exactly (any sender incl. console,
async ledger scan, self-logged to the admin stream):

- `/excelprotect dealt <player> [last-hours]` — total dealt, damage to
  elites, per-victim-type totals, latest incidents (ELITE Lv + names +
  projectile/spell markers + DIED). Admin action `dealt_query`.
- `/excelprotect healing <player> [last-hours]` — self / to-others /
  received-from-others (+ unattributed) totals with effective sums, and
  the latest incidents from all three directions. Admin action
  `healing_query`.

Hours parse/clamp as 0.1.2 (default 24, 0.1–720). Both commands answer
with the OFF message when their stream's toggle is disabled. Raw access
unchanged: `/excelprotect export stream=dealt|healing player=<name> …`.

**Awake lines** (0.1.2 style, appended after the damage line):
`[ExcelProtect] damage dealt: ON gap=2000ms elitemobs=hooked` and
`[ExcelProtect] healing incidents: ON gap=2000ms` (OFF variants name the
config key). READY/status report `version=0.1.3`.

## §6 Config

New toggles under `log.combat`, both default **true** (Mike's standing
goal: track everything): `damage-dealt`, `healing-incidents`. The shed
order gains `dealt, healing` after `damage`, so the new streams shed
before audit/containers under overload and after the 0.1.2 incident
stream. The shipped config documents both; **the live config is NOT
replaced at install** — absent keys default safely to ON.

## §7 Volume discipline

Player→mob hits are the most frequent events Protect records; the
per-(attacker, victim, cause, delivery) merge window is the bound: a
continuous fight against one victim yields ~1 record per engagement, not
per hit. The rig's scripted busy fight (TEST-REPORT) measured 240 hits
at 4/s rotating 6 victims → 6 dealt records (40:1). The Ledger's shed
counters cover both new streams; status shows `dropped-total=0` expected.
