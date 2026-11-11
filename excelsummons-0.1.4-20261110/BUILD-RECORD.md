# BUILD RECORD — ExcelSummons 0.1.4 (Wolf / summons fix)

Built Nov 10, 2026 (supervised desk BUILD unit for the WRV board
supervisor). Design of record:
`~/workspace/wolf-summon-diagnosis-20261110/FIX-PACKET-DESIGN.md`
§§2–4 (context: DIAGNOSIS-INTERIM.md beside it). **Not
installed.** The install packet is `INSTALL-PACKET.md` in this
folder; the MANIFEST entry is drafted in
`MANIFEST-ENTRY-DRAFT.md` for the supervisor to file (this lane
did NOT touch the MANIFEST, the parent-backlog, or board
state). Records package staged in this folder at
`records-package/` (shape mirrors the 0.1.3 package);
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). The
0.1.3 build folder is untouched — its jar still hashes to its
record value (verified at close-out).

**Amendments directed during the build** (supervisor, from
Mike's rulings — both folded into this same packet):
1. **Refusals explain themselves** (Nov 10, 2026, 10:34 AM):
   every failed summon cast tells the player exactly why, with
   the numbers — extended past the bridge to the Magic-side
   summon messages (§4 work below + `messages-addition-0.1.4.yml`).
2. **Rank-scaled summon cap** (Nov 10, 2026, 10:35 AM ruling;
   ladder LOCKED 10:37 AM: **Beginner 1 · Student 2 ·
   Apprentice 3 · Master 5**), SUPERSEDING the design's flat
   cap of 3: the total cap reads the player's EARNED rank from
   MagicGate's earned.yml. Note for the record: the
   supervisor's direction carried the PROPOSAL ladder (Student
   1) because Mike's pick was still pending when it was sent;
   his locked ladder (recorded in MEMORY.md the same morning)
   sets **Student 2**, and the shipped config carries the
   LOCKED values. A future rung change is a config-value edit,
   never a rebuild.

## Base proof

- Authoritative base: the shipped 0.1.3 tree at
  `~/workspace/summons-013-build/` — its BUILD-RECORD/SOURCE
  name `ExcelSummons-0.1.3.jar` (54,707 B, SHA-256
  `d6dc49d08f6a8ddc56b65c9c6ab4e00e496bbfee55088fd9dc407e480861c852`,
  build `EXC-SUMMONS-0.1.3-WRV-20261110-04-c9f312`), re-verified
  on disk Nov 10, 2026.
- DELTA RULE, verified before the change: a clean-room copy of
  the untouched 0.1.3 tree rebuilt ExcelSummons-0.1.3.jar
  **byte-identical** to the shipped jar (d6dc49d0…c852), 53/53
  tests green at the 0.1.3 totals. The 0.1.4 delta was applied
  to a copy of that verified base (0.1.4 therefore includes
  everything 0.1.3 has, incl. the console admin surface).

## Candidate

- `ExcelSummons-0.1.4.jar` — **63,642 B** — SHA-256
  `a34d7bdf54c9017eb3e4290d2fa89c3685e0adb3c9f40eef53f959f6322cc25a`
  — MD5 `5865685388942018f9d4de01e1731d1d`
- Deterministic double-build byte-identical, AND a clean-room
  rebuild and a rebuild from the shipped source zip each
  reproduced the same hash (three independent builds, 69/69
  tests in each).
- Fingerprint gate PASS — build
  **EXC-SUMMONS-0.1.4-WRV-20261110-05-7e3b19**.
- `ExcelSummons-0.1.4-source.zip` — 76,420 B — SHA-256
  `06e40034205ae8f8851bd09befe5353910b348009a3261a384315ccf1f40f391`
  (src/ + tests/ + plugin.yml + config.yml + local-build.sh +
  DESIGN-0.1.4.md + DESIGN-0.1.3.md; timestamps pinned
  2026-11-10).
- Packet companions: `modifiers-addition-0.1.4.yml` (+11
  templates), `messages-addition-0.1.4.yml` (per-spell refusal
  overrides, 12 spell sections).

## What shipped, per design section

Change-scope diff vs the verified 0.1.3 tree — only:
AdminPolicy.java, BuildInfo.java, CapPolicy.java,
ExcelSummons.java, RankCap.java (new), ReservePolicy.java,
SummonRecord.java, SummonRegistry.java, SummonsListener.java,
SummonsTest.java, config.yml, plugin.yml, local-build.sh,
DESIGN-0.1.4.md (new). Everything else byte-for-byte 0.1.3
(diff-proven). No spells.yml value moved (costs, cooldowns,
cap annotations all as the summons packet left them — pinned
in the harness by configPins014 + the design's §5.1 test 15).

### §2 — companion mana reserve (all seven §2.1 sites)

- (a) `ReservePolicy.totalPercent` counts any record whose
  family has a configured percent > 0 (branch filter removed).
- (b) `combatCounts` renamed `reserveCounts`, same widening;
  `desiredModifiers` follows.
- (c) `SummonsListener.onPreCast` reserve gate keys on
  `percentFor(...) > 0` for the cast family, not on COMBAT.
- (d) `sweepTick` drift repair collects owners of any
  percent > 0 record.
- (e) `ExcelSummons.reserveFamilyCaps` takes any branch with
  percent > 0 (this is what makes wolf4/wolf5 exist in
  `allKeys`: the wolf rule's cap is 5).
- (f) `listLineFor` prints `percentFor` for every branch — the
  `Nameplate.listLine` formatter already prints the clause
  whenever hold > 0, one call-site change, as designed.
- (g) config rows added: wolf 10 · familiar 5 · cat 5 ·
  mount 10 (§2.2's justifications are in the config comments).
  Ceiling 75 / floor 20 unchanged; refusal at PreCast spends
  nothing (unchanged refusal shape).
- §2.5 + §3.3 templates: +11 staged (`modifiers-addition-
  0.1.4.yml`): wolf, wolf2, wolf3, **wolf4, wolf5**, familiar,
  familiar2, familiar3, cat, cat2, mount. A harness pin asserts
  `allKeys` output == the 18 shipped templates (7 existing + 11
  new) exactly, and that a 5-wolf set desires exactly `wolf5`.
- §2.3 release paths, verified not re-invented: death
  (onDeath → unregister + sync), expiry (sweep/onEntitiesLoad),
  dismiss (despawn funnel), logout/death for combat
  (despawnCasterCombat), server stop (onDisable) — all already
  end in unregister + re-derive, so widening (a)/(b) widens
  them for free. **Relog is a RE-APPLY path for companions**
  (they persist; onJoin's +60-tick sync re-applies their holds
  from the persisted registry) and a release path for combat —
  stated here as the design requires. Chunk unload releases
  nothing (the entity still exists); the release point stays
  the onEntitiesLoad expiry re-check. Adoption now also syncs
  at registration, so an adopted pet holds from the moment it
  is registered (rig-proven: the adopted wolf's hold is part of
  the wolf3 tier in the rig's first check).
- §2.4 dismiss pets: `AdminPolicy.DismissScope` (MOUNT_ONLY /
  ALL / PETS) alongside the 0.1.3 boolean forms (which delegate,
  byte-identical selections — pinned). Self
  `/summons dismiss pets` + admin
  `/summons admin dismiss <player> pets` route through it; the
  audit line gains the "(cast pets only)" scope phrase.
  `SummonRecord.origin` (CAST at cast capture, ADOPTED at sweep
  adoption) rides the persistence codec; **legacy rows with no
  origin key restore as ADOPTED**. The pets scope selects CAST
  records only — adopted hand-tamed pets count toward the cap
  and hold reserve (one registry, one truth) but are never
  dismiss-deleted; their release paths are death /
  reconciliation only, and the §4 cap message branches on
  exactly that (below).

### §3 — unified cap, as amended to rank-scaled

- `SummonRegistry.countCounted` (+ `countBranches` set
  overload): COMBAT + COMPANION + PACK; MOUNT never counted.
- `CapPolicy` takes the config map `summon-cap-by-rank`
  (Beginner 1 · Student 2 · Apprentice 3 · Master 5 — locked
  values in config with the ruling cited in the comments; a
  missing section falls back to `RankCap.lockedLadder()`).
  `per-caster-combat-cap` retired. Check order per §3.2:
  SPELL_CAP (enforceCap only) → TOTAL_CAP (every managed cast
  incl. wolf, NOT gated on enforceCap; mount exempt) →
  SERVER_CEILING (combat-only, 12, unchanged). Refusal enum:
  TOTAL_CAP added, COMBAT_CAP retired.
- Rank seam: `ExcelSummons.earnedRankOf(UUID)` reads
  `plugins/ExcelMagicGate/earned.yml` (players.<uuid>.
  credited_total) — the same file/record the wand reconcile
  reads — re-read only when its size/mtime stamp changes, and
  derives the rank via `RankCap.rankForCredited`, which mirrors
  ExcelMagicGate 0.3.0 EarnedRank's LOCKED thresholds
  (Student 100 · Apprentice 400 · Master 2,000) verbatim, with
  a harness pin at every boundary. Two honest notes: (1)
  earned.yml carries no seal-ceiling field, so this is the
  EARNED rank exactly as the ruling names it ("read from the
  player's EARNED rank"); (2) any read failure — file missing,
  player absent, unparsable — yields null and **the cap fails
  to the LOWEST configured rung** (supervisor direction),
  pinned in tests and rig-proven.
- §3.3 grandfathering: no login sweep, no trim, anywhere.
  Over-cap sets keep every summon; holds apply from first
  login (rig: a student's 6 counted summons hold wolf5 + cat
  exactly); new counted casts refuse until the counted total
  falls below the player's CURRENT rank cap.

### §4 — refusal messaging (bridge + Magic-side)

Bridge rows (pure builders, harness-pinned strings with live
numbers; the 1,500 ms per-caster throttle is kept, as
designed):

- TOTAL_CAP: "Summon limit reached — your rank (Student)
  allows 2 summons, and you have 2 out: Wolf, Cat. Dismiss one
  (or let a combat summon expire) before summoning another." —
  names rank, limit, count, AND what is currently summoned
  (the 10:34 ruling). Adopted-pets variant: "…Your summons are
  pets that can only leave by death." (rig-verified verbatim).
- SPELL_CAP: "Your Guardian limit is reached — 1 of 1 already
  out." (replaces the raw-key, number-less 0.1.3 text).
- SERVER_CEILING: concept kept, number added: "…right now
  (12). Try again shortly."
- RESERVE_CEILING: "Ghast would hold 25% mana, but your
  summons already hold 70% of 75% allowed. Dismiss a summon
  first."
- RESERVE_FLOOR: "Too much of your mana is already committed
  (toggles and summons) — a Familiar would leave you under 20%
  usable. Free some mana first."

Magic-side (the 10:34 scope addition) —
`messages-addition-0.1.4.yml` stages PER-SPELL overrides in
Magic's messages.yml for the 9 summon families + the 3 tier
keys (archer|2, archer|3, bruiser|2 — staged explicitly
because tier→base message fallback is unverified from config
alone). Mechanism verified from on-disk Magic 11.2.5 config:
`spells.<key>.<message>` overrides `spells.default.<message>`
(messages.defaults.yml itself ships per-spell overrides, e.g.
peek's no_target; the stock live messages.yml carries no
spells section, so the addition creates it). **No default key
is edited; no Magic message key is altered.** Coverage:

| Magic-side refusal | Key(s) | Covered text |
|---|---|---|
| No living target (was SILENT: default no_target '') | no_target, living_entity_required | Names the spell, the living-target rule, and the shared 32-block range |
| Not enough mana | insufficient_resources | Keeps the engine's **$cost** interpolation ("it costs $cost"), adds the reserve explanation + /summons list pointer |
| Cooldown | cooldown | **$time** interpolation kept; the spell is named so a shared wand's cooldown is attributable |
| Generic bucket (incl. WolfSpell's own max_wolves failure, rank/path locks) | insufficient_permission | Rewritten per spell: names the spell's OWN limit (wolf: 5, "including any waiting at home"; familiar 3; cat 2; archer/bruiser 2; guardian/revenant/ghast 1; mount 1) + the not-unlocked possibility, honestly in one line |
| Internal failure | fail | Names the spell; states nothing was spent |

**Known remainders (named, not dropped):**
1. **Usable-mana and held-reserve numbers cannot be
   interpolated in Magic config messages.** Magic's cast-
   failure placeholders verified from messages.defaults.yml
   are $cost / $time / $spell / $player / $target — there is
   no placeholder for the caster's current usable mana or the
   reserve-held percent, so the Magic-side mana text gives the
   cost (via $cost) and points at /summons list for the held
   breakdown. The fully-numeric version of that refusal exists
   where the bridge owns the gate (reserve ceiling/floor
   above); a pure Magic cost failure cannot be made more
   numeric from config.
2. **Which key WolfSpell's internal max_wolves failure maps
   to is UNVERIFIED** (design §1 — needs the §5.3 controlled
   casts or a jar read). The wolf overrides cover BOTH
   candidate keys (insufficient_permission names the 5-wolf
   limit explicitly; fail is also wolf-specific), so whichever
   it maps to, it no longer wears the bare generic line — but
   the packet does not claim to know the mapping.
3. **§5.3's controlled-cast matrix is still owed** (live
   maintenance window, INSTALL-PACKET.md): it convicts the §1
   suspects and renders the per-spell failure texts on real
   casts. This build makes no claim about WolfSpell internals.

### §3.4 — NOT implemented (deliberate, per the design's own gate)

Registry reconciliation (`SummonRegistry.sweepMissing`
wiring) is **NOT shipped**. The design conditions it on a
provably safe loaded-chunk absence test verified in the rig.
No such test exists within this packet's footprint: a
SummonRecord carries a home WORLD but no position, and
`Bukkit.getEntity(uuid) == null` is also the unloaded-chunk
answer, so "chunk loaded, entity absent" cannot be proven for
a persistent pet from the data the registry holds — wiring the
sweep on that evidence would unregister live pets sitting in
unloaded chunks and desync their reserve, the exact failure
§3.4 forbids. The rig PINS the unwired behavior (a record
whose entity does not exist survives sweeps). Stale records
remain cosmetic in /summons list, as in 0.1.3. A future packet
that adds a last-known-position field to the record could
revisit this with a real absence test.

## Tests

- **SummonsTest 69/69** (three builds): 53 carried 0.1.3 tests
  + 16 new. Carried tests changed, each a design-named re-pin:
  `capPolicyRefusesAtThePerCasterCombatCap` → replaced by the
  TOTAL_CAP rank tests (COMBAT_CAP retired, §3.2);
  `capPolicyLeavesPetsAndTheMountOutOfTheCombatMath` →
  replaced by `totalCapCountsAcrossBranches` (companions/pack
  now count; only the mount is out);
  `capPolicyHonorsTheWolfExemption` → `wolfExemptionPinReplaced`
  (both halves asserted, §5.1 test 10);
  `reserveScopeExcludesCompanionsAndTheMount` → DELETED,
  replaced by `reserveCountsCompanionsAndMount` (the Nov 10,
  10:08 AM ruling reversing the 0.1.2 scope, §5.1 test 5);
  `pluginYmlPins013` → `pluginYmlPins014`. All other carried
  tests pass unmodified at their 0.1.3 totals — including the
  0.1.3 admin dismiss semantics, whose boolean selections are
  byte-identical under the new scope delegation.
- **Rig** (Paper 26.3-235 + Magic 11.2.5, `rig-0.1.4/`): clean
  0.1.4 enable, **SEVERE 0**, **reserve-template warnings 0**
  (all 18 templates present), **14/14 behavior checks** —
  rank seam + fail-lowest, companion reserve apply (incl.
  adopted holds), dismiss-pets state/entities/release,
  combat reserve + death release, grandfathered over-cap set
  (survival + exact wolf5 holds + refusal + mount exemption +
  verbatim message), §3.4 unwired pin. Full results:
  `rig-0.1.4/RIG-RESULTS.md`; log: `rig-0.1.4/boot.log`.
- **Sustainability (§5.1 test 14)**: every family's single cast
  from empty passes ceiling + floor at pools 100/150/200; the
  full combat set (70% held) is legal stepwise and is reported
  as the named LOW (a 100-cost wolf is not payable from that
  state until a summon leaves — cooldown-paced, not
  mana-paced, the packet-#1 precedent), not a failure.

## Install-critical differences from 0.1.3

0.1.4 does NOT inherit 0.1.3's "no config edit" property: the
live config must be REPLACED (or hand-merged) — the retired
per-caster-combat-cap key would otherwise linger unused and
the rank map / new reserve rows would be missing (the plugin
falls back to the locked ladder + zero companion percents,
which would silently disable companion reserve). The jar swap
also ships WITH the modifiers +11 and the messages addition
in the same window, and summons-data.yml gains the `origin`
key on its next save (0.1.3/0.1.2 read it back tolerantly —
unknown keys are ignored by the codec's explicit field reads;
rollback is safe). If 0.1.3 was never installed, 0.1.4
supersedes it outright: install 0.1.4 directly over the live
0.1.2 jar. Full steps: INSTALL-PACKET.md.
