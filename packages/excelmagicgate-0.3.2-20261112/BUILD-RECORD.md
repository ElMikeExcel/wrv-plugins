# ExcelMagicGate 0.3.2 — Build Record

Date: 2026-11-12. This record covers the 0.3.2 build in this folder
(`~/workspace/magicgate-032-build-20261112/build-0.3.2/`), built by
the board supervisor's desk chain. Desk work only: no installs, no
MANIFEST edits, no publishes, no live changes were performed.

Base tree: the shipped ExcelMagicGate 0.3.1 build
(`~/workspace/magicgate-031-build/`), carried whole into this NEW
folder; the 0.3.1 folder was not modified. **Base verification
before any edit (house base discipline):** the 0.3.1 folder's
`sha256sum -c SHA256SUMS` passed on every entry, AND the copied
0.3.1 source tree was rebuilt clean — the rebuild reproduced the
shipped 0.3.1 jar **byte-identically** (SHA-256
`454186c0dd145a57af08757d88b4de92cbd55551c5bdd71bc5677806e9bf42a2`,
243885 B). After copying, the new tree's src/stages/config trees
diffed identical to the base.

## Why this build exists

The P0 "Magic broken" diagnosis (Nov 12, 2026,
`~/workspace/magic-break-diagnosis-20261112/DIAGNOSIS.md`): Magic's
`Wand.buildInventory()` dereferences a null SpellTemplate
(`SpellTemplate.getMode()`) with no guard, so any wand save /
addSpell on a wand whose spell set — or whose grant batch —
contains a key that resolves to no template in Magic's runtime
registry throws an NPE part-way through. §5 Fix 1 is this build:
a small defensive gate in ExcelMagicGate's wand inventory sync
that resolves each grant candidate against Magic's registry
BEFORE addSpell, skips dead keys, and names them — turning the
diagnosis's unnamed dead key into a named WARNING on the next
sync of any affected player.

Context from the supervisor's run-228 pull (same night): all 80
keys on Mike's wand resolve in the live spells.yml and every tier
variant exists, and the pre-sync wand state is unrecoverable — so
no specific live dead key is known today. This build's pre-check
logging is the instrument that names any dead key at sync time on
the next join. (The run-228 pull's RECEIPTS.md was not on disk
when this build ran; the verdict above is as briefed by the
supervisor and is recorded here as briefed, not re-verified.)

## Artifact

- Jar: `ExcelMagicGate-0.3.2.jar` — **247078 B**, SHA-256
  `895cf91dfd28bcca0c493dc2ef69fcc903257001ea1790bb70fcae3b0a81fb68`
- Build ID: `EXC-MAGICGATE-0.3.2-WRV-20261112-01-9c032b`
  (excel-build.txt verified inside the jar; plugin.yml version 0.3.2).
- **Deterministic double-build: PASS** — second clean build
  byte-identical, same SHA-256.
- Stage packets: byte-identical to 0.3.1 (and to 0.2.1). Placement
  `earned.yml`: identical. Shipped `config.yml`: byte-identical to
  0.3.1 (no config change ships in this build).

## Diff summary — Fix-1-only scope (the proof)

Source diff vs the 0.3.1 tree, file by file (verified by `diff -r`
over the full src/tests/stages trees):

- `SpellTemplateGate.java` — NEW pure decision class (no Bukkit
  deps): `partition(keys, resolver)` splits a grant batch into
  grantable/skipped preserving order; the resolver is FAIL-OPEN
  (absent or throwing resolver ⇒ key stays grantable — a broken
  probe never blocks a grant); `skippedWarning(spell, player)`
  renders the pinned WARNING wording; `accounting(...)` renders
  the completion accounting fragment.
- `MagicFloorHook.java` — the only behavioural change:
  - in `syncInventory`, after the missing set is computed, the
    batch is partitioned through the gate with a reflective
    resolver (`spellTemplateResolves`: Magic controller
    `getSpellTemplate(key) != null`; any probe failure returns
    true, fail-open). Each skipped key is logged BY NAME at
    WARNING ("spell 'X' for <player> has no template in Magic —
    skipped, wand data/config mismatch…"); only grantable keys
    proceed to slots/addSpell/placement, and the verify-by-re-read
    judging is unchanged. If nothing grantable remains, the sync
    completes through an early return whose summary carries the
    skipped accounting. At completion, a skipped batch adds a
    WARNING accounting line (`… granted, … unverified, … skipped
    (no template in Magic: …) — skipped keys were not attempted
    and are not counted as granted`) and the same accounting is
    appended to the sync summary (`lastInventorySummary`, surfaced
    by `/magicgate status`).
  - the same gate sits on the `addSpellQuietly` seam as a second
    net: a key with no template is refused (named at WARNING,
    returns false) before addSpell/forceAddSpell can feed it to
    buildInventory. In the sync path the up-front partition means
    this guard does not double-log.
  - javadoc on both methods records the 0.3.2 gate.
- `BuildInfo.java` — version 0.3.2 + build ID rotation only.
- `plugin.yml` — `version: 0.3.2` only (description untouched,
  same as the 0.3.1 rotation practice).
- `excel-build.txt` — build ID + version + date only.
- `build.sh` — default output path/name for this folder + header
  note only.
- Tests (`ExcelMagicGateTest.java`) — build-ID prefix updated to
  0.3.2-01, plus the new `testSpellTemplateGate()` (9 checks).

NOTHING else changed: no rank logic, no thresholds, no reconcile
(rank, demotion, or step-up) behaviour, no credit policy, no
config, no paths, no data format, no stage packets. The wand-name
messages.yml rename from the diagnosis §6 is NOT in this build
(separate config packet, per scope).

## Tests

- **293 checks, 0 failures** (`run-tests.sh`, full output in
  TEST-OUTPUT.txt): all 284 carried 0.3.1 checks + **9 new gate
  checks** — dead keys skipped in order while valid keys stay
  grantable; the WARNING names the key and carries the mismatch
  verdict verbatim; a simulated sync completes with valid keys
  granted + verified held and the dead key never granted;
  accounting counts skipped separately from granted; an all-dead
  batch partitions to zero grantable; throwing and null resolvers
  fail open; empty batch partitions empty.

## Rig verdict (summary — full text in RIG-VERDICT.md)

PROVEN on the rig (Paper 26.3-214 + Magic 11.2.5, stage 4): four
boots, SEVERE 0 and zero `getMode` NPE lines in every boot.
Boot C (the gate proof): a rig-only supplemental-grant config
mismatch (`zzdeadspell`, no template in Magic) on an apprentice
sync — the WARNING names the key, 35 valid spells granted +
verified, the accounting line reports 1 skipped separately, and
later sweeps complete through the all-skipped path with the
accounting visible in `/magicgate status`. Boot B: a mutated copy
of Mike's pulled mage data (bogus key planted in the learned
spells + the wand item's spell_inventory; the original pull never
edited) replayed — step-up beginner → apprentice and a 34-spell
grant, clean; with the recorded finding that an inventory-only
dead entry is dropped by Magic at mage load and never reaches the
sync, so the gate's live seam is the grant-candidate path Boot C
proves. Boot A: Magic's own forceAddSpell refuses a dead key
itself (false, no throw) — dead keys arrive via data/config only.

## Fingerprint (per FINGERPRINT-STANDARD.md)

See FINGERPRINTS.md. Layer A clean. Layer B build ID above.
Layer C: all carried markers + the new natural marker: the
dead-key WARNING + accounting lines in the sync's log voice.
Registry row: NOT written by this build (left to the supervisor's
records leg with the MANIFEST filing, as before).

## Scope compliance

Desk work only, as tasked: no installs, no MANIFEST edits (a
MANIFEST-ENTRY-DRAFT.md is staged for the supervisor to file), no
publishes, no messages sent, no live pulls or live changes. The
records package is staged under
`~/workspace/records-catchup-20261103/packages/excelmagicgate-0.3.2-20261112/`
(and mirrored in this folder's `records/`), unpublished.
