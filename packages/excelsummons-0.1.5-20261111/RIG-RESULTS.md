# RIG RESULTS — ExcelSummons 0.1.5 (Nov 11, 2026)

Rig: copy of the 0.1.4 rig server (Paper 26.3-235 + Magic
11.2.5, JDK 25, offline mode, rig world) with the plugin swapped
to ExcelSummons-0.1.5.jar (SHA-256 verified 5cdf16a0…f61e before
boot), the driver extended with phase F (driver-src in this
folder), the registry data file cleared for a clean start, and
the rig's Magic config carried from the 0.1.4 rig (the full 18
reserve templates + the 0.1.4 messages addition — 0.1.5 changes
no Magic config). Full log: `boot.log` in this folder. Server
reached Done, was exercised by the driver, and stopped cleanly
("Disabling ExcelSummons v0.1.5"; registry saved with 8
persistent summons — the surviving pets + the §3.4 stale
record, exactly the 0.1.4 rig's closing shape).

## Boot

- "Enabling ExcelSummons v0.1.5" present.
- Enable line verbatim: "ExcelSummons 0.1.5 enabled (build
  EXC-SUMMONS-0.1.5-WRV-20261111-06-c7d315); registry holds 0
  summon(s)."
- SEVERE: 0 in the whole boot + exercise + shutdown.
- Reserve-template warnings ("could not be applied"): **0**.
- Archer-equipment soft-fail lines ("archer equipment skipped"
  / "did not verify"): **0** — every guarantee applied and
  verified on the first pass.

## Behavior exercise — 19/19 PASS, 0 FAIL (RIGOBS tally in boot.log)

Phases A–E are the 0.1.4 exercise carried unchanged (fake
players SumRigA / SumRigB / BystanderS; real spawned entities
registered directly in the bridge registry with explicit
CAST/ADOPTED origins; the earned.yml fixture read through the
plugin's real seam; modifier state asserted through the real
MagicAPI) — all 14 checks PASS at their 0.1.4 verdicts:
rank-seam-reads-earned-yml, rank-caps-fail-lowest,
companion-reserve-modifiers-applied, dismiss-pets-state,
dismiss-pets-entities, dismiss-pets-releases-reserve,
combat-reserve-still-applies, death-releases-reserve,
grandfathered-set-survives, grandfathered-holds-are-exact,
grandfathered-new-cast-refused, mount-never-total-cap-refused,
total-cap-message-shape, stale-record-kept-reconciliation-unwired.

Phase F (new, 0.1.5) — the archer equipment guarantee. Real
skeletons spawned, registered as CAST archers, their equipment
CLEARED first to reproduce Magic's bowless spawn, then geared
through `SummonsListener.applyArcherEquipment` — the exact
method `onCast` calls at registration, with the cast-time tier
key in hand:

| Check | Result |
|---|---|
| archer-base-tier-equipment | PASS — base-tier CAST archer: BOW in main hand + IRON_HELMET after the guarantee (record registered) |
| archer-tier2-equipment | PASS — archer\|2: BOW + CHAINMAIL_HELMET |
| archer-tier3-equipment | PASS — archer\|3: BOW + CHAINMAIL_HELMET |
| archer-guarantee-archer-only | PASS — a bruiser husk run through the same call keeps empty hands/head: the guarantee fires for the archer family only |
| archer-guarantee-soft-fail-nonliving | PASS — the guarantee on a non-living entity (a dropped item) is a clean no-op: no exception, entity untouched |

## Rig limitations, stated plainly

- The guarantee was exercised through the listener's public
  registration-path method on real spawned entities, not through
  a live Magic cast: the rig's Magic carries no summons spells
  packet (the same limitation the 0.1.4 rig states). The one
  line in `onCast` that invokes the method is covered by the
  harness at the spec layer (ArcherEquipment) and by the method's
  own read-back verification here. The in-game cast proof stays
  Jester's post-install archer cast, per the diagnosis.
- Registry records were seeded directly (real spawned entities,
  real registry) rather than produced by Magic summon casts —
  the paths exercised (registry, caps, reserve sync, dismiss,
  death, sweeps, equipment guarantee) are the paths this packet
  changes.
- The rank seam was proven against a hand-written earned.yml in
  the live file's exact shape; ExcelMagicGate itself is not in
  the rig (the bridge reads the file, not the plugin — by
  design).
- The authlib SSL warnings in boot.log are the sandbox's
  offline-mode profile lookups (present in prior rigs),
  unrelated to this packet.
