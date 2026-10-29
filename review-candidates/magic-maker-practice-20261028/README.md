# Combined Magic practice and Maker history source review

Unmerged draft-source export of the approved offline review candidate. Not an install package. Spell practice remains disabled in the packaged review configuration; legacy wand SP routing is retained. The fixed MagicGate applied-stage contract activates only currently applied spell families, without introducing seal-revoke downgrades. The adjustable early-spell balance and generated definitions are included. Existing native gate/rank prerequisites remain relevant.

## Layout
- `magic/coordinator/src`: current ExcelWandBars coordinator and production bootstrap.
- `magic/src`: effective-event/practice retention support.
- `magic/first-observation-core/src`: durable first-observed rank floor.
- `magic/native-source`: native Magic bridge sources and seven-class bytecode patch tool.
- `maker/source`: Maker history source, packaged defaults, plugin descriptor, tests and offline build wrapper.
- `magic/proposed-practice-config/automatic-stage-legacy-review.yml`: exact disabled proposed configuration.
- `early-magic`: adjustable balance, validation/generation code, generated definitions. Stage baseline configurations are intentionally not included.

## Build and reproducibility limits
This is not a self-contained clean rebuild claim. Provide lawful existing dependencies yourself; no dependency binaries are published here.

Native patch build requires the exact original Magic JAR SHA256 `4de62ec8545afbe65b83aeed5a8474b520569e59fd31116dd9f6e4380bc7d580`, Java 25.0.4, matching Paper dependency libraries and the dependency manifest expected by its build script. The dependency hash manifest and native test sources are included from the exact reviewed build inputs, correcting their omission from the combined archive. PACKAGING-ADDITIONS.json records these exact additions. The native patch source tree now contains every project-local input referenced by build.py; external JDK, original JAR and dependency library files must still be supplied. The native source export was rebuilt successfully with matching approved artifact hash; see VALIDATION.md.

The coordinator offline candidate was built against the patched Magic API, existing coordinator dependency classes and Paper libraries. Two unchanged Geyser source files require Geyser/Cloudburst dependencies unavailable to that clean source rebuild; the tested candidate preserved their baseline classes byte-for-byte. Some other original dependency classes were likewise preserved. Those binaries are excluded from this public export. A dependency-complete source-only rebuild remains outstanding.

Maker's `build-offline.sh` accepts explicit Paper, PlaceholderAPI and JUnit paths plus EXTRA_CP for shared Magic/coordinator interfaces. It intentionally emits an experimental review name. Generated native spell definitions can be validated using the accompanying Python code; generation and generation tests additionally require separate approved stage reference inputs and PyYAML. No live stage inputs are included. `MAGIC_STAGE_REFERENCE_DIR` supplies the local test reference directory.

The original candidate passed isolated startup/stage transition, missing-family fail-closed and default-OFF checks. Native patch and Maker sanitized sources were rebuilt and tested; the full coordinator dependency-complete clean rebuild remains unavailable. Nothing was deployed. Personal-source sanitization changes are enumerated in SANITIZATION.md; do not claim binary identity between a rebuild of this copy and the tested artifact. Full real combat, actual progression and Java/Bedrock acceptance remain separate gates.

SOURCE-MANIFEST.json records each exact exported source/default hash and any sanitization. SCAN-REVIEW.json records the local pattern scan, not a guarantee of exhaustive secret detection. No public publication, live configuration, player data, credentials, server access details or binary artifacts are included.
