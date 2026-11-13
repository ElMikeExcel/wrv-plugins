# TEST RESULTS — ExcelSummons 0.1.7 (Nov 12, 2026)

## Unit / logic harness (SummonsTest, via local-build.sh)

**89/89 tests successful, 0 failed** — reproduced identically in
the in-tree double-build and in a clean-room rebuild from the
shipped source zip (both jars byte-identical, SHA-256
2fc1a226…d56).

Composition: the **78 carried 0.1.6 tests**, of which exactly 2
changed, each a version re-pin in the established pattern:

- `pluginYmlPins016` → `pluginYmlPins017`: version 0.1.6 → 0.1.7,
  build-id lineage prefix `EXC-SUMMONS-0.1.7-WRV-20261112-01-`,
  plus one new assertion (the usage line names the menu form,
  `list|menu|dismiss`). Every other assertion carried verbatim.
- `configPins016` → `configPins017`: rename only — config.yml is
  byte-identical to 0.1.6 (the menu is code, not config; no menu
  title setting was needed), every assertion carried verbatim.

Plus **11 new 0.1.7 tests**, all on the pure menu layer:

- `menuMainPageShowsCastOnlyWithListFacts` — mixed fixture (2 cast
  combat + 2 adopted): the main page carries CAST only; each
  entry's line equals `Nameplate.listLine` over listLineFor's
  exact inputs (byte-for-byte); an entity with no HP text renders
  "not loaded"; held total 35 (guardian 25 + archer 10).
- `menuAdoptedSubPageCarriesTheIndicators` — adopted entries
  render ONLY in the sub-page model/body; every adopted row
  carries the ADOPTED badge + "holds no mana"; adopted hold = 0
  with no mana clause in its line; the main page shows the
  "Adopted pets (2)" count button and never an adopted line.
- `menuAppliedHoldRenderingSkipsClampedCast` — the 0.1.6 clamp at
  the menu layer: guardian 25 + ghast 25 + revenant 20 applied,
  bruiser 15 skipped by the ceiling (shows hold 0, claims none),
  the cast cat behind it still holds 5; total exactly 75.
- `menuHeldTotalIgnoresAdoptedMenagerie` — Jester's fixture at
  the model layer: 11 adopted → held 0%, cast page empty, button
  reads "Adopted pets (11)", no "Mana held" line (mirrors the
  list's omission at 0).
- `menuPaginationSplitsBothPages` — 50 cast + 47 adopted:
  PAGE_SIZE 45; cast pages 45 + 5, adopted pages 45 + 2; page
  indices clamp both directions; the pages tile the full list
  with nothing dropped or repeated.
- `menuBedrockBodiesAreComplete` — 60 cast + 12 adopted: the main
  body contains EVERY cast line and no adopted line; the adopted
  body contains EVERY adopted line.
- `menuDismissScopeRoutingMatchesTheCommands` — exactly 3 scope
  buttons in command order (MOUNT_ONLY / PETS / ALL);
  scopeForButton(3) is null (that index is the adopted door, not
  a dismiss); resultMessage texts equal the command's own
  ("Nothing to dismiss." / "Dismissed N summon(s).").
- `menuAdoptedDismissConfirmNamesThePetAndWarns` — per-pet button
  label ("Dismiss Wolf"); the confirm body names the specific
  pet, says it is the real tamed animal, warns "permanently" +
  "cannot be re-summoned"; the keep label is "No — keep my pet".
- `adoptedDismissGateIsNarrowAndAudited` — AdoptedDismissPolicy:
  adopted + owner passes; CAST refused; another owner's pet
  refused; nulls refused; the audit line names the player and
  the pet's spell key ("Summons adopted dismiss by …").
- `commandScopesStillNeverSelectAdopted` — AdminPolicy
  .dismissTargets (byte-identical to 0.1.6) over a mixed fixture:
  NO scope selects an adopted pet (adopted records exist only on
  the wolf/cat families; PETS is origin-checked, MOUNT_ONLY/ALL
  select mount/combat branches where adopted pets never sit);
  PETS → cast wolf only; ALL → cast guardian + cast mount;
  MOUNT_ONLY → cast mount. The menu model's only scope buttons
  remain the three command scopes — no bulk adopted control.
- `menuEmptyRegistryRendersClean` — zero records: counts 0, one
  empty page, "Adopted pets (0)", the empty-state body lines.

## What the harness does not prove (rig + live proof own these)

GUI/form mechanics, the real dismiss paths, and the audit line
in a running server are the rig's (RIG-RESULTS.md, 22/22). A real
Bedrock client's form interaction is NOT proven anywhere in this
packet — it is the post-install acceptance (Mike's compass proof).
