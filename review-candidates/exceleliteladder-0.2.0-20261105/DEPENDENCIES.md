# Dependencies

This plugin is the project's own code (Wink Reyes Valley). It has no
upstream plugin source dependency: it is compiled against the Paper API
only, as recorded in the build folder's SOURCE.md. The primary compile
input is hash-pinned in build.sh and in DEPENDENCIES.sha256:

- Paper API 26.3 build 231 (`paper-api-26.3.build.231-stable.jar`,
  SHA-256 `f0df9e7f…`) plus its runtime library set from this VM's pinned
  trees (build.sh assembles the classpath from
  `~/workspace/excelbuildmode-pilot/runtime-server/libraries`). SnakeYAML
  2.6 from that set also drives the migration equality suite.
- Toolchain: JDK 25 (Temurin 25.0.4.1, `~/workspace/.jdk25`),
  `javac --release 21`.

ExcelEliteLadder acts on the upstream EliteMobs plugin's
MobCombatSettings.yml at the file level and watches ExcelGuildGate seal
state at runtime (both unmodified, softdepend, reflective API read —
unchanged from 0.1.x); rig boots ran the real EliteMobs 10.9.12 and
ExcelGuildGate 0.4.0 jars. No upstream source is included; dependency
binaries are NOT included in this record (hashes in DEPENDENCIES.sha256).

The 0.2.0 product jar (ExcelEliteLadder-0.2.0.jar, SHA-256
781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0) is
deposited in Drive 'Minecraft WRV' > Build files (see
SOURCE-PROVENANCE.json). It is not included in this text record.
