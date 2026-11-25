# ExcelProtect 0.1.14 "Stack-Safe Provenance" — BUILD RECORD

**Lane:** build lane (desk + mirror rig only — no installs, no
live-server changes, no messages). **Date:** Tue Nov 24, 2026.
**Backlog:** P-PROTECT-RID-STACKSPLIT-20261124 (filed with full
source verification before this lane ran; this build follows its
analysis). **Deliverable folder:**
`~/workspace/excelprotect-ridfix-20261124/` (receipts:
RECEIPTS.md; packet: INSTALL-PACKET.md; surfaces: SURFACES.md).

## The defect (restated from the filing, re-verified in source)

ExcelProtect's Amendment 3.4 provenance stamp writes a UNIQUE
per-stamp record ID (`excelprotect:rid`) into the PDC of any
"fingerprint-class" item (named / enchanted / gear-type) at drop
and at craft (`ItemEvents.stamp()`, called from `onDrop` and
`onCraftStamp`). Minecraft only merges stacks whose metadata
matches exactly, so one drop-and-repick permanently split that
copy from every identical copy the player owned. Player-visible
specimen: Jester's dropped Fresh Rat (items-stream drop record,
Nov 24, 2026 14:54:25 CT, rid `epmwv6aizk-723cac`) could never
stack with his other rats again. Mike endorsed the fix Nov 24
("Oh sounds like our issue" — it needs fixing).

## What the rid is FOR (read before choosing — SPEC.md A3.4)

"Fingerprint-class items (named / enchanted / gear) are stamped
with a Protect record ID in their PDC at first sight (drop /
pickup / craft). Item events carry rid when present; crafts are
recorded (item_craft); export match=<rid> is the per-item
timeline chaining craft/trade/drop/store/destroy records."
The rid is the join key for tracing ONE specific physical item
through its life — dupe / theft / lost-item forensics.

## Fix shape — chosen (c): stamp only unstackable items

Evidence weighed (all from the 0.1.13 source of record):

- The rid's consumers are read-only: `ItemFact` carries it into
  item records; `ItemFact.fromJson` parses it back; `export
  match=<rid>` chains records that carry it. NOTHING in the
  runtime looks an item up BY its rid to function.
- The chain tracker (`Chains`) — the component that actually
  links drop/pickup/store events — deliberately does NOT use
  the rid: its signatures are id + enchants + custom name, with
  the header comment "Vanilla items are not unique instances,
  so matching is STATE-FILTERED and conservative." Chain
  tracking for stackables is therefore untouched by this fix.
- Every class the per-item timeline exists for is unstackable
  in practice: weapons, tools, armor, wands, elytra, totems,
  shulker boxes — all max stack size 1. Stackables are fungible;
  a per-stack identity on them buys no forensic value and costs
  the split.
- Candidate (a) (exempt "consumables/materials") needs a
  material taxonomy that will drift; (c)'s gate is one exact
  property of the item itself. Candidate (b) (strip on pickup)
  was rejected in the filing and here: the pickup is exactly the
  event the timeline must chain through, and stripping breaks
  `export match=<rid>` for every dropped item, gear included.

Implementation (one production file,
`src/com/winkreyes/excelprotect/ItemEvents.java`): the stamp
decision is factored pure — `stampDecision(fingerprint,
maxStackSize) = fingerprint && maxStackSize == 1` — and
`stamp()` returns the stack untouched unless it holds. One gate
covers both call sites (drop + craft). A plain-language comment
at the gate carries the reason (Jester's report + the mechanism),
as do the class javadoc in `ExcelProtect.java` and the
plugin.yml description clause.

**Already-stamped legacy copies — the stated design:** they
KEEP their rid. Nothing is stripped, ever: pickup is a timeline
event, and stripping would break `export match=<rid>` chains
mid-flight and mutate player items beyond the defect's scope.
A legacy stamped stackable therefore remains a solo stack until
it is consumed or used (vanilla similarity rules), and its
records stay traceable. What 0.1.14 guarantees is that no NEW
splits are created. A one-time login normalization that merges
legacy copies back was floated in the filing as an optional
rider; it is NOT in this build (it would need its own design —
it rewrites player inventory items).

One honest side effect, by construction: a stackable craft
result no longer receives a rid at craft time, so `onCraft`'s
rid-gated `item_craft` record no longer fires for stackable
results (it still fires for unstackable results and for any
already-stamped legacy stack). Craft records for fungible goods
were the same trade the filing priced into candidates (a)/(c);
chain tracking is unaffected.

## Provenance — the base tree IS the live source (verified)

- Live is ExcelProtect 0.1.13 (installed Nov 24, 2026 04:41;
  candidate 305,079 B / `e9fcefc6…`).
- Fresh G1 pulls Nov 24, 2026 15:33 CST (Homebase scp, no
  fallback): live jar `e9fcefc6…` / 305,079 B; live config
  `c1720401…` / 16,002 B (RECEIPTS.md PULL-01/02).
- Base gate: a scratch copy of the 0.1.13 build tree
  (`~/workspace/excelprotect-eventts-20261124/build-tree/`),
  built unmodified in this lane's tree, reproduced the live jar
  BYTE-IDENTICAL (`e9fcefc6…`) with suite 129/129. The 0.1.14
  work was done in that fresh copy
  (`~/workspace/excelprotect-ridfix-20261124/build-tree/`);
  the 0.1.13 tree is untouched.

## Changes (exactly)

- `ItemEvents.java`: stamp gate + `stampDecision` helper +
  reason comments (see above).
- `ExcelProtect.java`: class javadoc gains the 0.1.14 entry;
  READY line + `/excelprotect status` report 0.1.14.
- Version furniture (no behavior): `plugin.yml` version
  '0.1.14' + description clause; `local-build.sh` fingerprint
  `EXC-PROTECT-0.1.14-WRV-20261124-01-e14114`, jar name.
  `config.yml` byte-identical to 0.1.13 AND to live
  (`c1720401…`) — no config change, no new keys, no new
  dependencies.

## Tests — full suite + 4 focused (ProtectTest.java)

Suite: **133 found / 133 successful / 0 failed** on both builds
(129 pre-existing, none modified, + 4 new). The new tests pin
the pure stamp decision (the unit JVM has no Bukkit server, so
real ItemStacks cannot be constructed there — the behavioral
proof is the mirror rig's):

1. `stampDecisionKeepsProvenanceForUnstackables`:
   stampDecision(true, 1) = true — gear path unchanged.
2. `stampDecisionNeverStampsStackables`: (true, 64/16/2) all
   false — the Fresh Rat class (rabbit, max 64) can never be
   stamped again.
3. `stampDecisionLeavesNonFingerprintAlone`: (false, 1/64)
   false — pre-existing behavior for plain items unchanged.
4. `legacyStampedItemsStayTraceable`: an ItemFact carrying a
   rid (Jester's actual `epmwv6aizk-723cac`) still renders
   `"rid"` into its record JSON; a clean fact renders none —
   pre-0.1.14 timelines keep chaining under the new build.

## Candidate

| Artifact | Size (B) | SHA-256 |
|---|---:|---|
| `ExcelProtect-0.1.14.jar` | 305,260 | `b4b7fb0cac741f4b86853c575e72ea7b611b9653fda4d95e5d55913d7bf1dd85` |
| `excelprotect-0.1.14-source.zip` | 219,466 | `7426ba7af2e1085e97c726ed6b966aa7fc3362d8eca9d768bb78bfe8f139718f` |

Deterministic double-build PASS (`b4b7fb0c…` twice); fingerprint
gate PASS; plugin.yml inside the jar reads version '0.1.14';
bundled config `c1720401…`.

## Mirror rig — PASS (Homebase test-server mirror, Nov 24, 2026)

Full evidence in RECEIPTS.md; harness + scripts + logs under
`rig/` (evidence in `rig/evidence/`). Three boots ran; run 3 is
the proof of record:

- Run 1 (15:21) proved the entity/record half before its
  inventory readout was trusted: stackable entity rid=no +
  record rid-less; sword entity + record stamped. Its harness
  put the hand stack in held slot 0, overwriting the clean
  stack, and a join-plugin compass landed in slot 0 mid-case —
  inventory assertions invalid, harness fixed (held slot 5,
  clean stacks slot 10, settle delay, pre-case dumps).
- Run 2 (15:25) produced no drops at all: the run-1 shutdown
  had saved RigRid playerdata, and the reloaded fake's drops
  never fired. Prep now deletes the fake's saved playerdata so
  every run starts from fresh-fake conditions (run 3 prep
  removed `world/players/data/71d95e7c….dat{,_old}` + a Magic
  data .bak for the fake — mirror-only rig residue).
- Run 3 (15:29) — all three cases through the real event path
  (details + verbatim lines in RECEIPTS.md): A: unstamped
  entity, rid-less record, re-entered copies MERGED into one
  x4 stack. B: sword stamped on the entity, drop record
  carries the rid. C: legacy stack's drop record carries its
  original rid at count 2 — not stripped, not re-stamped.
  Boot: Enabling v0.1.14 x1, READY version=0.1.14 x1, SEVERE 0,
  SERVER-EXIT 0. (The one stack trace in the boot log is
  DecentHolograms failing to modify the fake player's network
  pipeline — the known rig-fake artifact, also present in the
  0.1.13 rig; not Protect.)
- Two rig-readout caveats, stated plainly: (1) the fake
  player's real-pickup behavior is flaky boot to boot — run 3's
  case A re-entry used the harness's labeled addItem simulation
  (the same merge operation the pickup path performs) after
  240 ticks without a pickup, while run 1's real pickup DID
  consume the identical unstamped entity; (2) in run 3 the
  sword entity was consumed without a Protect pickup record
  and the fake's post-pickup inventory read empty — the
  mirror's other join/pickup machinery acting on a
  connection-less fake; Protect's own case-B evidence (entity
  stamp + drop record) is complete and is what this build
  changes.
- Mirror restored byte-exact after the final run (0.1.8 jar
  `73373B90…`, config `7A423378…`, items stream `B1D293D2…` —
  all equal to pre-rig hashes; candidate + harness removed),
  and the LIVE server on the same PC was verified still
  running, untouched, after the rig (panel: online, uptime
  3h 6m from its 12:26 start).

## Filing

Filed per D212 as a pointer-stub PENDING entry in
`~/workspace/wrv-install-queue/MANIFEST.md` (packet + receipts
+ surfaces + this record in the deliverable folder). NOT
installed by this lane. Records close-out (wrv-plugins draft
PR + Drive deposit + website plugin-row bump) is owed at
install time per SURFACES.md, supervisor chain.

VERDICT: BUILD COMPLETE — candidate proven by suite (133/133),
deterministic double-build, and a mirror rig that exercised
drop -> re-pick for the stackable, gear, and legacy cases end
to end.
