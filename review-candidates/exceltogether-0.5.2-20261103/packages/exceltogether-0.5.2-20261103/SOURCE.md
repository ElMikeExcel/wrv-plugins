# ExcelTogether 0.5.2 — SOURCE / provenance

Build folder: `~/workspace/party-mesh/build-0.5.2/` (the party-mesh project's convention:
build-0.5.1 sits beside it; parent paperwork in `~/workspace/party-mesh/PLACEMENT.md`).

## What 0.5.2 is

0.5.1 (the party mesh + the party-repair Magic actions) plus two of Mike's decisions
(Nov 1, 2026, 8:28–8:29 PM CST):

1. **A confirmation gate in front of Repair Equipment** — "add a confirmation screen pop up
   for this spell to combat using it when not wanted." The first cast repairs NOTHING: it
   prompts the caster with what the spell will do and the LIVE damaged-item count across
   the party (armor + main hand, FULL repair; confirming starts the 4-hour cooldown —
   including when the count is 0, where the prompt says plainly that confirming still
   burns it). Casting again within the window (config, default 10 s) confirms and runs
   the 0.5.1 repair exactly as proven. An unconfirmed cast NEVER starts the cooldown.
2. **Repair Item nerfed** (spells staging only): its cooldown goes 10000 → **1800000**
   (30 minutes); mana stays 200. This half is in `staging/` + INSTALL-PACKET-REPAIREQUIP.md,
   not in the jar.

### Why the gate lives in Together, not in Magic (verified, not assumed)

Magic 11.2.5 has **no spell-cast confirmation facility**: the compile jar
(`work/magic-11.2.5.jar`, sha256 `79e49fe3…`, the same hash-pinned build input as 0.5.1)
was extracted and scanned — every "confirm" hit is a SelectorAction/shop GUI option or
RecallAction's `confirm_marker`; the `confirm: true` in Magic's example spells is a
shop-item option. BaseSpell charges the spell's cooldown the moment a cast runs,
whatever an action returns — so a Magic-side confirm could never satisfy "an arming
cast must never burn the 4-hour cooldown." Therefore:

- The spell's Magic cooldown becomes a **1-second anti-bounce** (spells pass).
- The real 4-hour cooldown is the gate's **persisted lockout**: `RepairGate` (a pure
  arm/confirm/lockout state machine, no Bukkit) + `RepairCooldownStore`
  (`party-repair-cooldowns.yml` in the plugin data folder; written the moment a cast
  confirms and again at disable; read at enable — the lockout survives restarts AND
  relogs). Casts inside the lockout are refused with the remaining time.
- The gate hooks `ExcelPartyRepairAction.start()` AND `perform()` (the verdict is
  computed once per cast and cached, so a confirmed cast is never re-judged mid-flight).
  ARM / LOCKED return `SpellResult.NO_TARGET`, so the child repair never runs. With no
  gate bound (config off), the action falls back to the exact 0.5.1 behavior.
- The prompt rides the cast's own messaging (`CastContext.sendMessage`) — the same
  channel Magic uses for cast feedback on Java and Bedrock alike; no form/GUI path is
  used. The damaged-item count is computed with EquipmentRepair's own repairable test
  over exactly the roster the repair would touch, so the number confirmed against is
  the repair that actually happens.
- All values are config (`party-repair-confirm:` in config.yml): `enabled`,
  `window-seconds: 10`, `cooldown-ms: 14400000`, and the three message strings with
  `{items} {members} {seconds} {cooldown} {remaining}` placeholders. An absent section
  yields exactly those defaults, so the live config needs no edit for the gate to arm.

## Base proof (delta discipline)

The unmodified **0.5.1** tree was rebuilt BEFORE any 0.5.2 edit (in `/tmp/basecheck-051`):

    ExcelTogether-0.5.1.jar: sha256 f2e5b7d3284474c250852dabde708efe3fea2f1dc1109ad6c763e564de2f517f
    (159,821 bytes) — byte-identical to build-0.5.1's shipped candidate, across two builds.

The 0.5.2 tree started as a full copy of build-0.5.1 and the delta was applied to it.
Nothing was ported from anywhere else.

## Candidate

    ExcelTogether-0.5.2.jar
    sha256  cb0cb309d81b5d5dd8400688f4ff83f7d815cb7b675fdb679e985c096bb91d2a
    size    171,337 bytes — build.sh assembles twice and hash-asserts; a third,
            independent build.sh run reproduced the same hash.

## Change scope vs the 0.5.1 candidate jar (entry-by-entry)

- ADDED (6): `RepairGate.class`, `RepairGate$Decision.class`, `RepairGate$Texts.class`,
  `RepairGate$Verdict.class`, `RepairCooldownStore.class`,
  `ExcelPartyRepairAction$1.class` (the gate switch's synthetic class).
- CHANGED (6): `ExcelPartyRepairAction.class` (the gate in start/perform + the count),
  `ExcelRepairItemAction.class` (its wand predicate is now package-visible so the
  gate's count uses the same exclusion — no behavior change),
  `ExcelTogether.class` (gate construction/binding, the awake-line confirm segment,
  lockout persist at disable), `PartyRepairBridge.class` (gate + store bindings),
  `config.yml` (the appended `party-repair-confirm` section + one stale 0.5.1 comment
  corrected), `plugin.yml` (version 0.5.2 + description sentence).
- REMOVED: none. The other **78 shared entries are byte-identical** to the 0.5.1 jar.

## Build inputs

Unchanged from 0.5.1 (JDK 25 at `~/workspace/.jdk25`, `javac --release 21`, the
hash-pinned Magic 11.2.5 compile jar, the hybrid assembly against
`work/pulled-0.4.3.jar`) except: the version pins, and the RepairGateTest run line.
`PartyFeatureTest`'s version pin moved 0.5.1 → 0.5.2 with the bump.

## Companion deliverables in this folder

- `staging/upload-spells-{s2,s3,s4,s5}.yml` — the 0.5.1 staging files plus exactly two
  lines per file (see INSTALL-PACKET-REPAIREQUIP.md for hashes and sequencing).
- `INSTALL-PACKET.md` (jar swap — supersedes the 0.5.1 packet),
  `INSTALL-PACKET-REPAIREQUIP.md` (spells pass, 0.5.2 edition),
  `TEST-REPORT.md` (unit + rig evidence), `CHANGELOG-0.5.2.md`,
  `MANIFEST-ENTRY.md` (the queue-entry draft replacing the 0.5.1 entry).
- `ExcelTogether-0.5.2-src.zip` — src + build.sh + plugin.yml + config.yml.
- `rig/` — the scratch Paper 26.3-235 rig (server clone + PartyRepairRig driver source)
  and the boot logs `boot-A/B/C-console.log` (the gate legs) + `boot-L2/L3-console.log`
  (the fail-soft legs). Rig-only; never shipped.
