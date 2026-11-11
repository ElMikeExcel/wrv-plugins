# BUILD RECORD — ExcelBuildMode 1.3.4 (combined rebuild: reviewed + console admin)

Built Nov 10, 2026 (desk build lane for the WRV board supervisor;
decision D5, board-manager run 144). Commission: the Oct "reviewed"
build is LIVE with no surviving source, and the Nov 10 console-admin
build (also numbered 1.3.3) would regress it — 1.3.4 reunites both
deltas at a clean version number. **Not installed.** The install
packet is `INSTALL-PACKET.md` in this folder; the MANIFEST entry is
drafted in `MANIFEST-ENTRY-DRAFT.md` for the supervisor to file
(this lane did NOT touch the MANIFEST, the parent-backlog, or board
state). Records staged at
`~/workspace/records-catchup-20261103/packages/excelbuildmode-1.3.4-20261110/`;
GitHub/Drive legs are the parent's (see `PUSH-NOTES.md`). Neither
the 1.3.3 build folder nor the live 1.3.2 pilot tree was modified.

## The recovery (the heart of this build)

Full record: `REVIEWED-DELTA-RECOVERY.md` in this folder, with raw
disassembly + normalized diffs in `recovery-evidence/`.

- Inputs, hash-verified in this run: preserved reviewed jar
  `~/workspace/requal/menujoin143/preserved/ExcelBuildMode-1.3.3.jar`
  (90,044 B, SHA-256 3662dcb73ca08ac1677e4257f64eae489ab9486e484ecc447ea2779cedc3ee1c)
  and genuine 1.3.2
  `~/workspace/excelbuildmode-pilot/ExcelBuildMode-1.3.2.jar`
  (89,718 B, SHA-256 26d356b4c6266200a30ee891fec4727ba11456491264dfb76df9c8615d600116).
- Byte diff of the two jars (48 entries each, identical file sets):
  only `plugin.yml`, `paper/BuildPolicy.class`, and
  `paper/ExcelBuildMode.class` differ — every other class,
  config.yml, and the manifest are byte-identical.
- Normalized `javap -c -p` disassembly pins the whole delta:
  `BuildPolicy.REVIEWED_PERMISSION` ("excelbuildmode.reviewed"),
  `BuildPolicy.refusesOutsideWorldEdit(w, r, h) = w && !r && h`
  wired into `enter()`'s outside-WorldEdit refusal, and
  `BuildPolicy.probesResidualWorldEdit(w, r) = w && !r` wired into
  `removeAttachment()`'s exit probe; plugin.yml gains version 1.3.3,
  the reviewed description sentence, and the reviewed permission
  block (default false). The reviewed build's banner strings are the
  plain 1.3.2 ones (unchanged).
- **Control-build proof:** the recovered delta alone, applied to the
  genuine 1.3.2 source tree (`excelbuildmode-pilot/work/ExcelBuildMode-1.0`),
  reproduces the preserved jar's plugin.yml BYTE-IDENTICAL, and its
  compiled BuildPolicy + ExcelBuildMode classes match the preserved
  jar's classes with ZERO normalized-javap diff (both control diff
  files are 0 bytes, in `recovery-evidence/`). The reconstruction is
  bytecode-faithful, not inferred.

## Base for the combined build

The console-admin tree `~/workspace/buildmode-133-build/ExcelBuildMode/`
copied to `~/workspace/buildmode-134-build/ExcelBuildMode/` (the
1.3.3 build folder itself untouched). Change-scope diff of the 1.3.4
tree vs that base — only: `paper/BuildPolicy.java` (recovered
reviewed helpers + constant), `paper/ExcelBuildMode.java` (the two
recovered call sites + the 1.3.4 banner), `plugin.yml` (version
1.3.4, reviewed block, combined description), `AdminTests.java`
(version pin 1.3.3 → 1.3.4 only), `ReviewedTests.java` (new),
`build.sh` (ReviewedTests run + jar name/date), `docs/PILOT-1.3.4.md`
(new). config.yml and every other file are byte-for-byte the base's
(diff-proven).

## Candidate

- `ExcelBuildMode-1.3.4.jar` — **99,073 B** — SHA-256
  `39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3`
  — MD5 `f175678bbae3eb6ae55416ea260c1ed0`
- Deterministic double-build byte-identical (clean-room copy of
  src + build.sh), AND a rebuild from the shipped source zip
  reproduced the same hash a third time.
- `ExcelBuildMode-1.3.4-source.zip` — 141,842 B — SHA-256
  `da252ddce47aab7bc497a20bf69d6f8488c77aaeaa37c89f1180113818a548dc`
  (src/ + build.sh + README + docs incl. PILOT-1.3.4.md; timestamps
  pinned 2026-11-10 18:00 UTC).

## What shipped (the whole change vs live)

Against the LIVE reviewed 1.3.3, 1.3.4 adds exactly the console
admin surface of `docs/PILOT-1.3.3.md` (AdminPolicy, `/buildmode
admin status|off`, `excelbuildmode.admin` default op, gated tab
completion, the audit log line) and changes nothing else: the
reviewed exemption is preserved instruction-for-instruction
(control-proven), and every 1.3.2 behavior rides unchanged.
Against the console-admin 1.3.3 build, 1.3.4 adds exactly the
recovered reviewed delta. The jar's plugin.yml carries BOTH
permissions: `excelbuildmode.reviewed` (default false) and
`excelbuildmode.admin` (default op), plus `excelbuildmode.use` /
`excelbuildmode.bigpaste` unchanged.

## Tests

- **Eight suites green, 11,685 assertions** via `bash build.sh`:
  CoreTests 5,710; RewardTests 5,283; RecoverySafetyTests 84;
  PlayerDataProofTests 145; PilotGateTests 338; BuildReturnTests 18
  (all carried, unmodified, at their 1.3.2 totals); AdminTests 82
  (carried; version pin moved to 1.3.4) + **ReviewedTests 25 (new)**:
  the full entry truth table (refusal fires for a non-reviewed
  holder with standing WorldEdit; never for a reviewed holder; never
  with temporary WorldEdit off), the full exit truth table (probe
  runs for non-reviewed; skipped for reviewed), plugin.yml pins
  (version 1.3.4; reviewed registered, default false, preserved
  description wording; admin still registered, default op), and the
  probe-set pins (reviewed node is not a WorldEdit probe node and is
  never session-attached, so holding it trips neither probe itself).
- **Rig** (Paper 26.2-123 + WorldEdit 7.4.5, `rig-1.3.4/`): clean
  1.3.4 enable + full banner (both delta lines), **SEVERE 0**, clean
  stop, and 7/7 console checks re-run verbatim from the 1.3.3 rig.
  Full results: `rig-1.3.4/RIG-RESULTS.md`; log: `rig-1.3.4/boot.log`.
  Rig limitation, stated plainly: no real client exists in the rig,
  so the reviewed entry/exit behavior and a live force-restore run
  only in the install packet's post-install player proofs; the
  harness truth tables + the control-build bytecode proof carry
  those legs at the desk.
