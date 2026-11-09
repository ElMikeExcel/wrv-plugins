# ExcelBossTrial 0.5.2 — FINGERPRINTS (categories + locations only)

Per `~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md` §2: this
file names marker CATEGORIES and where they live, never a raw secret
value beyond what the shipped jar itself prints in plain sight.
Passive only: no phone-home, no telemetry, no sabotage.

- **Layer B — build ID (carried from 0.5.1, rotated):**
  - File marker: `excel-build.txt` at the jar root (bundled by
    `package.sh`), carrying sku / version / channel / date / build ID.
  - Code marker: the build ID assembled from string fragments in
    `core/BuildInfo.java` (no single literal in source).
  - Surface marker: the boot log line `Build: <build-id>` printed by
    `ExcelBossTrial.onEnable`.
  - Build ID (public — it prints in every server log):
    `EXC-BOSSTRIAL-0.5.2-WRV-20261109-01-13447a`.
  - Rotation verified for this build: the tag is FRESH (13447a);
    0.5.1's tag (a3aa16) is carried by NO 0.5.2 marker home and
    does not appear in the built jar (it survives only in a
    BuildInfo javadoc sentence recording that it was not
    reused). All three homes were verified in the built jar as
    a release gate (see BUILD-RECORD.md).
- **Layer C — semantic markers (carried from 0.5.1, none vanished):**
  plugin name/namespace (`excelbosstrial`), PDC key namespace
  (including the 0.5.1 re-light/sprinkle/relay keys), boss-bar keys,
  config key naming, and message voice remain the standing authorship
  texture. 0.5.2 ADDS, under the same naming texture: the voice
  config keys (`<chapter>.taunt-sounds`, `<chapter>.voice.*`), the
  `wrv:bosstrial.<boss>.<slug>` sound-event naming in the two frozen
  resource packs, and the `BossTrial voice [...]` log-line shape.
- **Rotation rule:** the build-ID sequence/tag segment rotates per
  build; the semantic markers persist version to version. A vanished
  marker in a future build is a flagged defect needing a named reason
  + replacement in the same pass.
