# ExcelSummons 0.1.8 — TEST RESULTS

Harness: `SummonsTest` via `local-build.sh` (JUnit platform
console, run from the project root). **94 tests found, 94
successful, 0 failed** — in the project build AND again in the
clean-room rebuild from the shipped source zip.

## Carried from 0.1.7 (89)

All 89 of 0.1.7's tests run unchanged except the two re-pins:

- `pluginYmlPins017` → `pluginYmlPins018`: version + fingerprint
  lineage strings only (`version: 0.1.8`, BuildInfo.VERSION
  "0.1.8", BUILD_ID prefix `EXC-SUMMONS-0.1.8-WRV-20261112-01-`);
  every other assertion carried verbatim.
- `configPins017` → `configPins018`: rename only (config.yml is
  byte-identical to 0.1.7, SHA-256 `659d9e2c…c3a9`).

The other 87 — the 0.1.3–0.1.6 bodies (caps, classes, reserve
arithmetic, summons-follow targeting, persistence, admin
policy, 0.1.6 adopted-mana set) and the ten 0.1.7 menu tests
(main page cast-only with list facts, adopted indicators,
clamped-cast hold rendering, Jester menagerie held total,
pagination, Bedrock bodies complete, scope routing, confirm
naming/warning, adopted dismiss policy, empty registry) — are
untouched and green.

## New in 0.1.8 (5)

1. `menuOutCountsDeriveFromCastEntries` — the counts group the
   page's CAST entries by `SummonRecord.baseKey`: mount →
   mounts; wolf / cat / familiar → cast pets; guardian /
   archer|3 / ghast → combat. Tiered keys fold into their base
   (wolf|2 counts as a wolf). Adopted records — including an
   adopted MOUNT — are never counted. Fixture: 1 mount, 4 cast
   pets, 3 combat, total 8 = the cast list size exactly.
2. `menuOutCountsZeroCases` — empty registry: OutCounts all
   zero, line = "Nothing out right now.", body still carries
   the 0.1.7 "No cast summons out right now." closer. Adopted-
   only registry (a menagerie, nothing cast): counts stay zero
   and the line still says nothing is out.
3. `menuOutNowLineFormatsAndPlacement` — singulars at 1,
   plurals elsewhere, ALL THREE groups always print (zeros
   included): "Out now: 1 mount · 0 cast pets · 0 combat
   summons (1 total)"; the mixed fixture reads "Out now:
   1 mount · 1 cast pet · 2 combat summons (4 total)". In the
   Bedrock body the line sits after the registered total and
   before the held line; it never appears on the adopted body.
4. `bedrockFormsHandlerShape018` — structural pin on the
   compiled BedrockForms class: the constant pool references
   the NEW Cumulus (`org.geysermc.cumulus.form.SimpleForm`,
   `org.geysermc.cumulus.form.util.FormBuilder`,
   `resultHandler`, `clickedButtonId`) and Geyser
   (`org.geysermc.geyser.api.GeyserApi`, `connectionByUuid`);
   it does NOT reference the OLD Cumulus
   (`org.geysermc.cumulus.SimpleForm`), `validResultHandler`,
   or `floodgate` anywhere.
5. `bedrockFormsHandlerDeliversEveryIndex018` — BEHAVIORAL:
   loads the REAL Cumulus classes from the staged Geyser jar
   (Geyser-Spigot 2.12.1-b1292, passed by local-build.sh as
   `-Dsummons.test.geyserJar`, in its own URLClassLoader —
   the jars are not on the harness classpath), builds all
   three form shapes through the production
   `BedrockForms.buildForm`, and drives a synthetic response
   through the attached handler for EVERY button position —
   via `FormImpl.callResultHandler` (the dispatch Geyser's
   FormCache uses) AND via the raw-payload entry
   (`SimpleFormDefinition.handleFormResponse` with "<index>"
   / "null"). Main form indices 0–3 (Adopted door included),
   adopted form 0–2 (Back included), confirm form 0–1 (Keep
   included) all reach the callback with their index; a closed
   form reports −1 on both paths.

## The honest limit

The harness proves the handler DELIVERS taps against the real
Cumulus classes; the rig (RIG-RESULTS.md) proves the same
inside a real server with the live Geyser + Floodgate jars. A
real Bedrock client's tap remains Mike's post-install live
proof, as the packet's Acceptance states.

Excel — Wink Reyes Valley (Iris / Mike).
