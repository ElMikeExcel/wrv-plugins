# Excel rename preparation verification

Status: prepared, not installed. No actual Paper boot or player/client acceptance
is claimed for this rename batch. Six exact original plugin JARs and the MenuJoin
bytecode compatibility change are still blocked. Source builds retain separate
pre-existing repairs and must not be used as narrow live rename replacements.

## Verified locally

- All eight maintained-source modules built on checksum-verified Temurin
  21.0.12.1+1 using the unchanged Java 21 release target.
- 87 Java unit tests passed, with zero failures, errors or skipped tests.
- 17 source identity and offline data-copy tests passed.
- Four existing source repository invariants and eleven shop-generator checks
  passed. The unrelated full-repository aggregate path is deferred to CI.
- 48 artifact-tool tests passed. Negative cases cover altered payloads even with
  updated caller-supplied hashes, duplicate old/new identities, pending update
  JARs, case collisions, conflicting configuration destinations, wrong baselines,
  and intentional refusal of the unimplemented MenuJoin binary transformation.
- Both exact-baseline candidates rebuilt byte-for-byte identically in separate
  calls. Their rename stages change only plugin.yml name/version/provides.
- The final ExcelBedrockMenu JAR was rechecked with the isolated JVM menu probe:
  38 original and 36 final assertions passed. Bukkit, GeyserForms and TeleportMenu
  are test doubles, not the real Paper/Geyser/client integration.
- Independent review compared both candidates to their immediate verified
  inputs and confirmed unchanged executable payloads across the rename stage.

The Bedrock input is the separately verified menu-only patch, whose original live
class/resource inventory and six payload hashes were checked against the server.
The archive packaging of its historical reconstruction differs from the original
live archive. WeekendXP's whole input JAR matches the live server SHA-256.

See [exact candidate and baseline hashes](candidate-status.json), the adjacent
entry-level audits, and the [migration requirements](../../EXCEL-PLUGIN-RENAMES.md).
Qualified-command inventory, safe offline config copying, exact Paper 26.2 build
123 provides/dependency behavior, startup, and existing-player/villager/XP checks
remain pending. A successful unit suite is not deployment authorization.
