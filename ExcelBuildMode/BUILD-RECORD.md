# BUILD RECORD — ExcelBuildMode 1.3.5 (Windows-safe directory sync)

Built Nov 14, 2026 (desk build, FSYNC FIX LANE 2 of 4, backlog
P-FSYNC-1; parent-dispatched). Commission: apply the fix proposed in
`~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md` — the
durable stores' directory fsync (`FileChannel.open(directory)` +
`force()`) throws `AccessDeniedException` under Windows Java NIO and
degrades the plugin on the PC test-server mirror; the live Linux
server is unaffected. **Not installed.** Installs are a later gated
step owned by the supervisor; this lane touched no MANIFEST, made no
live-server contact, and sent nothing. The 1.3.4 build folder was
used read-only and is untouched (its SHA256SUMS re-verified after
this build — see below).

## Source tree — identification and currency

- Tree used: `~/workspace/buildmode-134-build/ExcelBuildMode/`,
  copied (excluding `target/`) into this folder's `ExcelBuildMode/`.
- It is the newest complete BuildMode tree and builds version 1.3.4 —
  the version the diagnosis names as live (ExcelBuildMode 1.3.4) and
  the version the mirror boot degraded.
- Currency chain on record in that folder: SOURCE.md (provenance:
  combined rebuild reuniting the reviewed + console-admin deltas,
  control-build bytecode proof), LIVE-JAR-IDENTIFICATION-20261110.md,
  and INSTALLED.txt — 1.3.4 installed live Nov 10, 2026 with candidate
  SHA-256 `39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3`
  as the panel-exact swap.
- **Base gate (passed before any edit):** the unmodified copied tree
  rebuilt byte-identical to that on-record candidate (99,073 B,
  SHA-256 39d86f3b…), all eight suites green at their recorded totals
  (11,685 assertions). Full detail in TEST-RESULTS.md.

## The change (the whole delta vs the 1.3.4 tree)

Raw unified diff: `fsync-fix.diff` in this folder (12 files). New
helper + four directory-force sites converted; file-level
`channel.force(true)` stays mandatory everywhere; no call-site
signature changes.

- **NEW `core/DirectorySync.java`** — package-private helper.
  `bestEffort(Path)`: opens the directory channel and forces it
  exactly as before, tolerating any `IOException` (the Windows
  `AccessDeniedException` class). Javadoc states the contract: runs
  unchanged where supported (live Linux included); skipped, never
  fatal, where the platform refuses directory channels.
- **`core/RecoveryJournal.java`** — `forceDirectory` body delegates to
  the helper. Covers the constructor durability probe (the raw stack
  trace in the diagnosis) plus begin() ×3 and writeState().
- **`core/ActiveSessionIndex.java`** — its private `force` delegates.
  A fourth durable store the diagnosis never reached: constructed
  immediately after the journal in the same enable block, so the
  journal's failure masked it; its constructor and every save force
  the root directory identically and would have kept the mirror
  degraded on their own.
- **`core/ReviewHoldStore.java`** — `hold()`'s inline post-move
  directory force replaced by the helper call.
- **`core/BuildReturnStore.java`** — `record()`'s inline post-move
  directory force replaced by the helper call.

Version 1.3.4 → 1.3.5 (patch bump), lineage style:

- `plugin.yml`: version `'1.3.5'`; description gains one appended
  sentence, verbatim: "1.3.5 makes the durable stores' directory sync
  best-effort, so storage also works on platforms that refuse
  directory channels (e.g. Windows); the file-level sync is
  unchanged."
- `paper/ExcelBuildMode.java`: the two startup banner strings. These
  are the only log lines changed, and only as version lines always
  change in this lineage — pilot banner: leading token
  "ExcelBuildMode 1.3.5 PILOT:" and one appended fragment, verbatim:
  "1.3.5: durable-store directory sync is best-effort - a platform
  that refuses directory channels (Windows) no longer disables
  storage; the file-level sync is unchanged." Non-pilot banner:
  leading token only ("ExcelBuildMode 1.3.5: cold-start reward
  integration; …"), text otherwise identical.
- Test version pins moved (AdminTests, ReviewedTests — the
  `plugin.yml declares version` checks); `build.sh` jar name and a
  new fixed jar date (2026-11-14T12:00:00Z) + the new suite's run
  line; `docs/PILOT-1.3.5.md` added (design note).
- **No other behavior or message changes.** No player-facing text,
  config, permission, or policy changed.

Attribution note, for the record: two of the three Nov 13 mirror boot
ERRORs (ReviewHoldStore / BuildReturnStore constructor refusals) were
missing-directory refusals — those directories were never provisioned
on the mirror — not fsync failures; the diagnosis's summary sentence
groups them with the fsync cause. The constructors' provision-first
refusal is deliberate and unchanged. This fix removes (a) the
journal/index constructor + save failures, which are the fsync idiom
itself, and (b) the hold/record save failures that would have hit the
two provisioned stores on every save under Windows.

## Swept and deliberately excluded

`PlayerDataProof.forceVerifiedMarker` (line 75) also forces a parent
directory after forcing the playerdata file. It is NOT converted: it
is a fail-closed verification proof, not a store save — its contract
is to fsync the inode and parent directory and then re-verify bytes
and stamps; making its directory force best-effort would silently
loosen a safety proof. On Windows it keeps failing closed. Flagged
here for the supervisor as a separate design call.

## Candidate

- `ExcelBuildMode-1.3.5.jar` — **99,879 B** — SHA-256
  `aa2d35fd4c2fb2a1839c33d2153f96be52c06c02e5e75ec144d27f1911ab8a66`
  — MD5 `e40696b7e4bf910dae26ce61c124f869`
- Deterministic: three full builds (in-place ×2 + clean-room from
  src/build.sh/README/docs) reproduce that hash exactly.
- `ExcelBuildMode-1.3.5-source.zip` — 145,180 B — SHA-256
  `c8be342cca0a51cabcfc2badee125b88d66f1b4e0b3191e22c4101e1483b8c9b`
  (full tree minus `target/`; entry timestamps pinned
  2026-11-14 12:00 UTC).

## Tests

Nine suites green, 11,699 assertions: the eight carried suites at
their exact 1.3.4 totals (only the two version pins moved) + NEW
DirectorySyncTests (14) proving the unguarded idiom throws where a
directory cannot be opened, the helper tolerates exactly that, and
save round-trips through all four stores. Full results:
TEST-RESULTS.md.

## Still owed (not this lane)

- **Rig verification:** mirror boot with 1.3.5, with `review-holds\`
  and `build-return\` provisioned as the live installation provisions
  them — expect zero ExcelBuildMode storage ERRORs and Build entry
  enabled.
- **Install:** supervisor's gated step (packet + MANIFEST + window)
  when the four-lane P-FSYNC-1 sweep is assembled.
