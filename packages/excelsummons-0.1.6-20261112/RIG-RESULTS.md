# RIG RESULTS — ExcelSummons 0.1.6 (Nov 12, 2026)

Rig: copy of the 0.1.5 rig server (Paper 26.3-235 + Magic
11.2.5, JDK 25, offline mode, rig world) with the plugin swapped
to ExcelSummons-0.1.6.jar (SHA-256 verified 08da4f10…f70f1 before
boot), the driver updated for 0.1.6 (driver-src in this folder:
phases A–D reserve expectations corrected to Mike's Nov 12, 2026
ruling + new phase G), the registry data file cleared for a
clean start, and the rig's Magic config carried from the 0.1.5
rig (the full 18 reserve templates + the 0.1.4 messages
addition — 0.1.6 changes no Magic config). Full log: `boot.log`
in this folder. Server reached Done, was exercised by the
driver, and stopped cleanly ("Disabling ExcelSummons v0.1.6";
registry saved with 20 persistent summons — the surviving pets
+ the §3.4 stale record + phase G's persistent survivors,
exactly the expected closing shape).

## Run note (transparency)

The FIRST 0.1.6 rig run tallied 20 PASS / 1 FAIL:
`death-releases-reserve` failed because the driver's phase-C
expectations still carried the 0.1.5 shape in two places (the
first editing pass missed phase C's single-line step form, so
the check still expected the adopted wolf's bare key to remain
held — the exact behavior 0.1.6 removes; the plugin's behavior
in that run was the NEW one). The driver was corrected, the rig
server was reset to its pristine start state (fresh copy, data
file cleared, jars re-verified), and the FULL run was repeated.
**Every number below is from that clean second run.**

## Boot

- "Enabling ExcelSummons v0.1.6" present.
- Enable line verbatim: "ExcelSummons 0.1.6 enabled (build
  EXC-SUMMONS-0.1.6-WRV-20261112-07-d1607a); registry holds 0
  summon(s)."
- SEVERE: 0 in the whole boot + exercise + shutdown.
- Reserve-template warnings ("could not be applied"): **0**.
- Archer-equipment soft-fail lines ("archer equipment skipped"
  / "did not verify"): **0**.

## Behavior exercise — 21/21 PASS, 0 FAIL (RIGOBS tally in boot.log)

Phases A–E are the 0.1.5 exercise (fake players SumRigA /
SumRigB / BystanderS; real spawned entities registered directly
in the bridge registry with explicit CAST/ADOPTED origins; the
earned.yml fixture read through the plugin's real seam;
modifier state asserted through the real MagicAPI). Setup +
phase E + phase F pass at their carried verdicts:
rank-seam-reads-earned-yml, rank-caps-fail-lowest,
dismiss-pets-state, dismiss-pets-entities,
grandfathered-set-survives, grandfathered-new-cast-refused,
mount-never-total-cap-refused, total-cap-message-shape,
stale-record-kept-reconciliation-unwired, and phase F's five
archer-equipment checks (base / tier-2 / tier-3 equipment on
real spawned skeletons through the bridge's own
registration-path method, the archer-only guard, the non-living
soft-fail).

The reserve checks, corrected to the Nov 12, 2026 ruling:

| Check | Result |
|---|---|
| companion-reserve-modifiers-applied | PASS — 2 CAST wolves + cast cat hold (wolf2 + cat bare key); the ADOPTED wolf adds nothing (no wolf3 — under 0.1.5 this state rode wolf3) |
| dismiss-pets-releases-reserve | PASS — after dismiss pets, NOTHING remains held: the surviving adopted wolf holds no mana (under 0.1.5 its bare wolf key stayed) |
| combat-reserve-still-applies | PASS — a CAST guardian's modifier applies by drift sync; the adopted wolf beside it holds nothing |
| death-releases-reserve | PASS — guardian death releases its 25%; no reserve of any kind remains |
| grandfathered-adopted-holds-nothing | PASS — RigB's over-cap grandfathered set is ALL adopted (5 wolves + cat): it holds NOTHING (under 0.1.5 this same state applied wolf5 50% + cat 5%), and every summon survives untracked-down — nothing trimmed |

Phase G (new, 0.1.6) — the two regression fixtures against real
Magic modifier state, applied by the bridge's own drift sync
(the same re-derivation path a login takes):

| Check | Result |
|---|---|
| adopted-menagerie-applies-no-reserve | PASS — Jester's fixture on the bystander: 5 wolves + 2 cats + 3 familiars + 1 mount, ALL adopted (11 registered); appliedPercent reads 0 through the plugin's live config and NO wolf5 / cat2 / familiar3 / mount modifier is on the mage (0.1.5 applied 85% here) |
| cast-set-clamped-at-sync-to-ceiling | PASS — RigB cast set guardian 25 + ghast 25 + revenant 20 + bruiser 15 + cat 5 (raw totalPercent 90): the sync applied exactly 75 — guardian / ghast / revenant / cat modifiers present, bruiser's ABSENT; the excess summon stays registered, holding nothing |

## Rig limitations, stated plainly

- Registry records were seeded directly (real spawned entities,
  real registry) rather than produced by Magic summon casts —
  the paths exercised (registry, caps, reserve sync incl. the
  new clamp, dismiss, death, sweeps, equipment guarantee) are
  the paths this packet changes. The sync path proven here is
  the drift/login re-derivation itself (`syncReserve` via the
  sweep), not a simulated one.
- No live Magic cast and no real player login were exercised:
  the rig's Magic carries no summons spells packet (the same
  limitation prior rigs state). The post-install live proof
  stays Jester's: his next login clearing all four adopted
  modifiers and returning his full 200 pool.
- The rank seam was proven against a hand-written earned.yml in
  the live file's exact shape; ExcelMagicGate itself is not in
  the rig (the bridge reads the file, not the plugin — by
  design).
- The authlib SSL warnings in boot.log are the sandbox's
  offline-mode profile lookups (present in prior rigs),
  unrelated to this packet.
