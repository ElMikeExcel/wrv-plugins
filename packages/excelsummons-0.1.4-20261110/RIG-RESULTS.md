# RIG RESULTS — ExcelSummons 0.1.4 (Nov 10, 2026)

Rig: copy of the 0.1.3 rig server (Paper 26.3-235 + Magic
11.2.5, JDK 25, offline mode, rig world) with the plugin swapped
to ExcelSummons-0.1.4.jar (SHA-256 verified a34d7bdf…cc25a
before boot), the driver swapped to the 0.1.4 SummonsRig
(driver-src in this folder), the rig's Magic modifiers.yml
carrying the FULL reserve template set live will hold (the
0.1.2 packet's seven + this packet's eleven = 18), and the
rig's Magic messages.yml carrying the 0.1.4 messages addition
merged (12 spell sections). Full log: `boot.log` in this
folder. Server reached Done, was exercised by the driver, and
stopped cleanly ("Disabling ExcelSummons v0.1.4"; registry
saved with 8 persistent summons — the surviving pets).

## Boot

- "Enabling ExcelSummons v0.1.4" present.
- Enable line verbatim: "ExcelSummons 0.1.4 enabled (build
  EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19); registry holds 0
  summon(s)."
- SEVERE: 0 in the whole boot + exercise + shutdown.
- Reserve-template warnings ("could not be applied"): **0** —
  with all 18 templates installed, every companion/combat/mount
  modifier the bridge asked for applied (contrast 0.1.3's rig,
  where the missing modifiers addition produced the designed
  fail-soft warnings).

## Behavior exercise — 14/14 PASS, 0 FAIL (RIGOBS tally in boot.log)

Fake players (NMS, the house recipe) SumRigA / SumRigB /
BystanderS; REAL spawned entities registered directly in the
bridge registry with explicit CAST/ADOPTED origins; a
hand-written `plugins/ExcelMagicGate/earned.yml` fixture in the
live file's shape (RigA credited_total 3734, RigB 150, the
bystander absent) read through the plugin's real seam;
modifier state asserted through the real MagicAPI.

| Check | Result |
|---|---|
| rank-seam-reads-earned-yml | PASS — 3734 casts → master, 150 → student, absent row → null |
| rank-caps-fail-lowest | PASS — capForRank: master 5, student 2, null rank → 1 (lowest rung) |
| companion-reserve-modifiers-applied | PASS — 3 wolves (2 cast + 1 adopted; adopted holds too) ride `wrv_summon_reserve_wolf3`; cat rides its bare key; no stale wolf2 |
| dismiss-pets-state | PASS — `summons dismiss pets` as RigA: cast wolf ×2 + cat unregistered; the ADOPTED wolf survives |
| dismiss-pets-entities | PASS — cast pet entities removed; adopted pet entity alive |
| dismiss-pets-releases-reserve | PASS — after release only the adopted wolf's bare `wolf` key (10%) remains held |
| combat-reserve-still-applies | PASS — guardian modifier applied by drift sync alongside the adopted wolf's |
| death-releases-reserve | PASS — guardian killed (real death event): unregistered, its 25% modifier released; adopted wolf's hold remains |
| grandfathered-set-survives | PASS — a student's 6 counted summons (cap 2) all survive repeated sweeps — never trimmed (§3.3) |
| grandfathered-holds-are-exact | PASS — grandfathered holds apply: `wolf5` (50%) + `cat` (5%) on the student's mage |
| grandfathered-new-cast-refused | PASS — new counted cast = TOTAL_CAP while over the rank cap |
| mount-never-total-cap-refused | PASS — fresh mount cast at counted 6 passes the cap check (mount not counted) |
| total-cap-message-shape | PASS — the live refusalMessage renders exactly: "Summon limit reached — your rank (Student) allows 2 summons, and you have 6 out: Wolf, Wolf, Wolf, Wolf, Wolf, Cat. Your summons are pets that can only leave by death." |
| stale-record-kept-reconciliation-unwired | PASS — a record whose entity does not exist SURVIVES sweeps: §3.4 reconciliation deliberately unwired (see BUILD-RECORD) |

## Rig limitations, stated plainly

- Registry records were seeded directly (real spawned
  entities, real registry) rather than produced by Magic
  summon casts: the rig's Magic carries no summons spells
  packet. The paths exercised — registry, caps, reserve sync,
  dismiss, death, sweeps — are the paths this packet changes;
  cast capture is exercised at the policy layer by SummonsTest
  and its only 0.1.4 change is the CAST origin stamp.
- The rank seam was proven against a hand-written earned.yml
  in the live file's exact shape; ExcelMagicGate itself is not
  in the rig (the bridge reads the file, not the plugin — by
  design, the same seam the wand reconcile uses).
- The messages addition was proven to LOAD (Magic booted with
  the merged messages.yml, SEVERE 0, no config errors); the
  per-spell failure texts themselves render only on real cast
  failures, which need the spells packet — that proof is
  §5.3's in-game controlled-cast matrix, still owed post-install.
- The authlib SSL warnings in boot.log are the sandbox's
  offline-mode profile lookups (present in prior rigs),
  unrelated to this packet.
