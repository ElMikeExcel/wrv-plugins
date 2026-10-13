# MenuJoin namespace patch: blocked pending exact live bytes

Live JAR: MenuJoin-1.2.0.jar

Verified whole-JAR SHA-256: abc335c402295f458b5beae4ad0972bb8c3c981ed74dca28efe0571d0d1d8178

The current module intentionally refuses all MenuJoin transforms. No MenuJoin JAR has been produced. The recovered source is newer/different and must not be used to reconstruct this artifact.

## Required behavior

A metadata rename changes Plugin.getName(), which changes namespaces produced by NamespacedKey(Plugin, String). Persisted keys must remain exactly:

- menujoin:autojoin_disabled
- menujoin:menu_compass

The narrow intended source-equivalent edit is to use NamespacedKey("menujoin", "autojoin_disabled") and NamespacedKey("menujoin", "menu_compass") in the actual existing onEnable method. Do not alter commands, permissions, PlaceholderAPI identifiers, delays, inventory/drop behavior, other methods, or other classes.

## Implementation once bytes are available

1. Confirm the complete input SHA-256 above. Reject a mismatch. Inspect plugin.yml to confirm MenuJoin 1.2.0 and the actual main-class path.
2. Extract and pin the exact main-class SHA-256. Use javap -c -p -v against those bytes, not source. Confirm there are exactly the two expected construction sites using NamespacedKey(Plugin,String), with key literals autojoin_disabled and menu_compass and the expected destination fields.
3. Use a classfile-aware transform restricted to those two construction sites. Replace the plugin receiver argument with the fixed string "menujoin" and select the (String,String) constructor. Prefer preserving the original constant-pool entries/indices and every unmodified attribute. Adjust only onEnable bytecode, associated offsets/frames/debug ranges, and necessary added constants; no compute/rewrite of unrelated methods.
4. Fail if the opcode sequence, method signatures, count, key literals, class version, stack map layout, exception handlers, or original class hash differ from the reviewed specimen. Do not global-replace "MenuJoin" or namespaces.
5. Compare all classfile structures before/after. Require all other method Code attributes, fields, annotations, bootstrap methods, and existing constants to be unchanged. Audit exact changed offsets/attributes and both class hashes. Require every other JAR payload except plugin.yml to be identical.
6. Run java -Xverify:all with a narrow JVM probe and API stubs to inspect both constructed fields under plugin runtime name ExcelMenuJoin. Assert both old namespaces, toggle state round-trip, and recognition of pre-existing compass keys. These probes only establish bytecode/key behavior, not actual Paper/player behavior.
7. Separately, with exact Paper 26.2 build 123 available, test getPlugin/isPluginEnabled for original and Excel names, dependency resolution through provides, startup order, existing data-folder migration, command ownership, placeholder behavior, and persistent key recognition across restart. Keep this status explicitly unverified until executed.

JDK tools in this executor are available as java com.sun.tools.javac.Main and java com.sun.tools.javap.Main. No actual Paper runtime JAR is currently available locally.
