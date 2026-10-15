# Native playerdata marker proof

Offline candidate work. The parser and marker protocol do not establish production approval or remove the remaining filesystem/crash/recovery gates.

## Exact supported runtime evidence

The inspected cached server is Paper 26.2 build 123, revision `5001879`. Server JAR SHA-256 is `e26c0cbb690906829c3c98a7d8049fa4175747fb75e38381c20a7996d3ce1acb`. The scoped bytecode inspection is saved at `verification/playerdata/paper-5001879-playerdata.javap.txt`.

- `CraftPlayer.saveData()` calls `CraftServer.getHandle().playerIo.save(getHandle())` and returns void.
- `PlayerDataStorage.save()` returns without writing when `SpigotConfig.disablePlayerDataSaving` is set. It also catches/logs save exceptions. A returned Bukkit call therefore cannot acknowledge persistence.
- `PlayerDataStorage` stores its actual native player directory in `playerDir` and exposes `public File getPlayerDir()`.
- Derive the native directory from the running server's actual `CraftServer.getHandle()` → public inherited `playerIo` → `getPlayerDir()` chain. Resolve the expected player's UUID filename there. Do not infer it from the player's current world/dimension or the legacy `playerdata` layout.
- The previous native synthetic-player fixture actually lives beneath `world/players/data/`. Its compressed NBT contains root `UUID` as an int array of exactly four elements, root `playerGameType` as an int, and root `BukkitValues` as the PDC compound. No adapter path is guessed from this observed directory spelling.

The included resource `src/test/resources/playerdata/native-paper26.2-synthetic-survival.dat` is a prior exact-Paper test's invented native player, not a real user's data. SHA-256: `175a5668b3abd86b12d8580400b28520d91e99855bd515dd1089da6bc5eb5f85`. Its fake unrelated PDC entries are used to prove byte-preserving inspection. Production recovery/eligible UUIDs must remain in private installation configuration, never source/defaults.

## API

`PlayerDataProof.inspect(Path dat, UUID expectedPlayer, String markerKey)` returns an `Inspection` with `status()`, `player()`, `gameMode()`, `marker()` and `reason()`.

- `VERIFIED_UNMARKED_SURVIVAL`: valid full bounded native record, matching root UUID, Survival (`playerGameType=0`) and no own marker key.
- `MARKED`: matching UUID and valid game-mode field, but the own marker is present. This includes active/restored/review values, empty/malformed/unknown-version strings and wrong-typed marker values. Presence is unsafe; a null `marker()` alone never establishes absence.
- `UNKNOWN`: missing, unreadable, truncated, corrupt, oversized, changing, unsupported or identity-mismatched data; missing/invalid mode; or unmarked non-Survival state. UNKNOWN is never proof of an ordinary unaffected player.

Only `excelbuildmode:recovery` in the root `BukkitValues` compound is inspected. No other PDC key is copied, changed, reset or interpreted as entitlement. Nested or similarly named unrelated fields cannot substitute for the authoritative root fields.

`PlayerDataProof.forceVerifiedMarker(Path dat, UUID expectedPlayer, String markerKey, String expectedMarkerOrNull, boolean requireSurvival)` throws on any verification/force error and otherwise returns its inspection. It never writes playerdata. A non-null expected marker must be a canonical `v1:active:<session UUID>`, `v1:restored:<same session UUID>` or `v1:review:<review UUID>` string. A review marker can be acknowledged for an actor-specific hold, but exact matching means it can never satisfy an active/restored session acknowledgment. It always remains MARKED and never proves ordinary unmarked Survival. A null expectation requires actual key absence, including absence of wrong-typed values. Call with `requireSurvival=true` for all entry/restoration/clear acknowledgments; false is an explicit marker-only mode that must not be treated as clean Survival. An actor-specific review hold may use false to preserve the marker even when the observed mode itself is unsafe; it must not change mode or claim inventory recovery to make the proof pass.

## Ordered protocol owned by the coordinator/adapter

1. Prepare immutable Survival recovery and the independent active-session index; publish the actor guard.
2. While still Survival and before any Creative/inventory-clear mutation, set only the own PDC key to `v1:active:<session>` and call native save.
3. Verify the actual native file's UUID, exact active marker and Survival mode; force that same file and its containing directory. Any mismatch/error blocks Creative entry and retains recovery.
4. Restore the saved inventory/XP/mode. Set `v1:restored:<same session>`, call native save, then verify and force that exact restored marker and Survival mode.
5. Complete the journal and independent active index. Only afterward remove the own PDC key, native-save, verify actual absence and Survival, and force again.
6. Release the actor guard only after all required acknowledgments succeed. A failed removal/force must not be silently interpreted as completion.

The marker is not a full inventory hash. Its evidentiary value depends on the adapter setting the new session/state marker only after the associated main-thread mutation, and on serialized native saves. An already persisted marker from another session or phase cannot acknowledge the expected operation. Inspecting native bytes does not validate every game's inventory component or unrelated PDC value.

## Bounded parser and local-filesystem checks

- At most 16 MiB compressed, 32 MiB decompressed, depth 64, and one million declared elements/visited tags. Exceeding a limit produces UNKNOWN or a thrown verification failure, never eviction/truncation of playerdata.
- Strictly one complete gzip member, including deflate end, payload CRC, uncompressed size and optional header CRC. Trailing compressed junk, concatenated members and trailing decoded NBT are rejected.
- All standard NBT tag types are skipped safely. Relevant fields have exact required types. Duplicate compound keys, negative/oversized lengths, malformed UTF and unknown tag types are rejected.
- The target must be a non-symlink regular file with a stable filesystem file identity. Unsupported filesystems fail closed. The verifier opens the saved file read/write solely to make `force(true)` meaningful, without writing any bytes.
- Force reads/validates exact bytes, forces the same open inode and its containing directory, rereads the descriptor and current path, and rejects detected identity/size/mtime/content changes. This requires readable/writable native playerdata and directory-force support.

This remains a point-in-time acknowledgment under the actual filesystem's guarantees. It is not an atomic transaction across native playerdata, plugin journal and independent index; it cannot defeat malicious concurrent filesystem writers or hardware that lies about durability. Call on the server thread without a competing native save. Root journal failure, retention/disk budget and recovery policy belong to the separately reviewed coordinator design. No global join lock is introduced by this class.

## Verification status

`PlayerDataProofTests` covers safe/unsafe classifications, exact UUID/type requirements, every truncated gzip prefix, CRC/length/header corruption, malformed/unknown markers, duplicate/trailing NBT, all tag types, bounds/decompression bomb, replaced-inode/in-place mutation during force, injected force-path failure, exact phase/session matching, removal verification, and the native synthetic fixture.

The parent’s previous constrained build completed successfully with **139 PlayerDataProof assertions passed** before recognized review-marker support was added. The same build passed 77 recovery-safety assertions, 5,283 reward assertions and 5,709 original core assertions. Candidate JAR SHA-256: `ca0afa70894da2a7ea180899ecac714f9f455ee78c765bdd6a6bde0dc4d9fc1c`. This is compilation plus isolated tests and a read/force of the earlier native synthetic fixture, **not a new running Paper server test**. The test main is `com.winkreyes.buildmode.core.PlayerDataProofTests`; its result is saved in `target/playerdata-proof-test-result.txt`.

Disabled native player-data saving and native exceptions swallowed by the void save path still need the targeted exact-Paper extension described in `PLAYERDATA-NATIVE-FIXTURE-PLAN.md`. The per-player fallback hold’s durability across failed metadata and later native rewrites is a separate coordinator-policy review item.

## Current review-marker change

Recognized `v1:review:<UUID>` support and six additional proof assertions are now saved. They require the next aggregate build; the 139-assertion result above does not cover these newer edits. The independent actor-specific review-hold store is owned by the coordinator, not this parser. A native review marker complements that store so a later native quit/save rewrite does not silently erase the prior unsafe classification. Removal and release require the separately reviewed administrator recovery policy, never an inference from the marker text alone.
