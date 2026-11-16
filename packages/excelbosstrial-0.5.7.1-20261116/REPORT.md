# BB-18 stamp-only build — BossTrial 0.5.7.1 (board supervisor run 428, worker W9)

- Run: 428 (W9 — BossTrial 0.5.7.1 stamp-only build). Date: Mon
  Nov 16, 2026 (America/Chicago).
- Scope: build only. No installs, no MANIFEST writes, no
  publications, no edits to the tree of record (all work on a
  copy, §2). Live server and panel untouched.
- Premise: ExcelBossTrial 0.5.7 is Installed (Nov 16, 2026,
  07:48 CST failsafe window) but its boot `Build:` line prints
  the stale 0.5.6 BuildInfo stamp — the 0.5.7 build updated
  `plugin.yml` and `excel-build.txt` and missed
  `BuildInfo.java`, and no harness guarded the split (scoping
  of record: `~/workspace/board-r428/bb18-stamp-scope/REPORT.md`,
  Option A). This build is the stamp-only correction under a
  FRESH version, **0.5.7.1** (supervisor decision, run 428:
  0.5.7 already shipped / is of record in PR #242 / Drive /
  live and is never reissued), with the §4.4 prevention harness
  built into this same build.
- What's New: **checked — no entry** (supervisor decision, run
  428). A cosmetic boot-log stamp correction with no
  player-facing behaviour change needs no player entry.

## 1. Candidate jar (FINAL)

- `~/workspace/board-r428/bb18-stamp-build/ExcelBossTrial-0.5.7.1.jar`
  — **472,617 B / SHA-256
  `94cf076da9cbb489fa4ded38cd9b7599b4d72e36724f88017e0540ef7b01f899`**
- Deterministic double-build: `package.sh` run twice on the
  fixed tree — both jars byte-identical at `94cf076d…`
  (`cmp` clean; second jar `work/candidate-0571-second.jar`).
- Version inside the jar: `plugin.yml` reads
  `version: 0.5.7.1`; `excel-build.txt` reads build-id
  `EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`, version
  0.5.7.1, build-date 2026-11-16.
- Jar-entry diff vs the 0.5.7 candidate (472,525 B /
  `4b737c58…`): 244 → 244 entries, none added or removed;
  content-changed entries are exactly:
  - `com/winkreyes/excelbosstrial/core/BuildInfo.class` —
    **present in the diff, as the fingerprint rule requires**
    (its absence would have been a stop);
  - `com/winkreyes/excelbosstrial/ExcelBossTrial.class` — its
    ONLY changed printable string is the inlined boot-line
    constant: old `Build: EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`
    → new `Build: EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`
    (javac inlines the compile-time `BuildInfo.BUILD_ID`
    constant into the one referencing class and folds the
    `"Build: " +` concatenation; string-set diff of the class
    is exactly that one string, size delta +2 B = the build
    ID's length delta 42 → 44). This is the stamp reaching
    the actual failing surface, not an extra change. (The
    scoping report's Option A item 4 expected only the three
    stamp homes in the diff; the host class necessarily joins
    them for this reason — recorded here so the reading is
    explicit, per the fingerprint-check rule.)
  - `plugin.yml`, `excel-build.txt`.
  Nothing else changed.

## 2. Base and work area

- Tree of record (never edited):
  `~/workspace/board-r425/bb18-fix-build/work/bosstrial-057/`
  (the 0.5.7 tree that was compiled for the installed jar).
- Work copy: `~/workspace/board-r428/bb18-stamp-build/work/bosstrial-0571/`
  (verbatim copy; `diff -r` against the tree of record clean
  before any edit).
- **BASE GATE (house pattern — PASSED before any edit):**
  `package.sh` on the unmodified copy produced a jar
  **byte-identical to the installed 0.5.7 candidate** —
  472,525 B / SHA-256
  `4b737c584dae9d9d6ebcffe85dc0899fbc6918d06f7c3818b9e8a5bd9bfc1e01`;
  suite 2,856 checks / 102 harnesses, 0 failures — exactly
  the 0.5.7 build's tally. Log: `work/base-gate.log`; jar:
  `work/base-gate.jar`.

## 3. The change

Full unified diff: `BuildInfoStamp-0.5.7.1.diff` in this
folder (6 paths). Production sources changed: **1**
(`BuildInfo.java` only).

| Path | Change |
|---|---|
| `src/com/winkreyes/excelbosstrial/core/BuildInfo.java` | **The fix:** `PART_B` `"0.5.6-WRV-"` → `"0.5.7.1-WRV-"`, `PART_C` `"20261111-01-3c9e56"` → `"20261116-01-471aa6"`, `VERSION` `"0.5.6"` → `"0.5.7.1"`, javadoc build-ID paragraph rewritten to match (fresh tag `471aa6`, minted Nov 16, 2026, seq 01; never reuses `bb18` or `3c9e56`) |
| `plugin.yml` | `version: 0.5.7.1` + one appended description sentence ("0.5.7.1 corrects the BuildInfo build stamp …"), per the 0.5.7 precedent |
| `excel-build.txt` | build-id / version bumped to the new ID; build-date 2026-11-16 |
| `tests/com/winkreyes/excelbosstrial/core/PluginYmlParseTest.java` | version pin 0.5.7 → 0.5.7.1 + one added 0.5.7.1 description-sentence check |
| `tests/com/winkreyes/excelbosstrial/core/BuildInfoStampTest.java` | **NEW** — the prevention harness (§4) |
| `SOURCE.md` | appended 0.5.7.1 section (base, change, tests, install pointer), per the 0.5.7 precedent |

New build identity: `EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`
(fresh 6-hex tag `471aa6`, generated for this build).

No config, no data, no state-format change; no gameplay
behaviour changes from 0.5.7; Bedrock unaffected (no UI
touched).

## 4. Prevention harness — BuildInfoStampTest (NEW)

`tests/com/winkreyes/excelbosstrial/core/BuildInfoStampTest.java`
— plain-JVM main-method harness in the house style, so
`build-test.sh` runs it on every build. It asserts the
three-home agreement (§4.4 of the scoping report):

- (a) `BuildInfo.VERSION` == the `version:` parsed from
  `plugin.yml`;
- (b) `BuildInfo.BUILD_ID` == the `build-id:` parsed from
  `excel-build.txt`;
- (c) `excel-build.txt`'s `version:` == `plugin.yml`'s version
  (and == `BuildInfo.VERSION`);
- (d) prefix agreement: `BUILD_ID` starts with
  `"EXC-BOSSTRIAL-" + VERSION + "-WRV-"` and with
  `"EXC-BOSSTRIAL-" + <plugin.yml version> + "-WRV-"` (fleet
  precedent: ExcelMagicGate's `BUILD_ID` prefix assertion).

8 checks total (2 file-presence + 6 agreement).

**Both runs recorded:**

- **Pre-edit tree (must FAIL — it did):** harness compiled
  against the unmodified 0.5.7 copy and run from its root:
  `BuildInfoStampTest: 4 passed, 4 failed`, exit 1. Failures,
  verbatim (log: `work/pre-edit-stamp-test.log`):
  - `FAIL: BuildInfo.VERSION equals plugin.yml version (BuildInfo=0.5.6, plugin.yml=0.5.7)`
  - `FAIL: BuildInfo.VERSION equals excel-build.txt version (BuildInfo=0.5.6, excel-build.txt=0.5.7)`
  - `FAIL: BuildInfo.BUILD_ID equals excel-build.txt build-id (BuildInfo=EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56, excel-build.txt=EXC-BOSSTRIAL-0.5.7-WRV-20261116-01-bb18)`
  - `FAIL: BuildInfo.BUILD_ID prefix agrees with plugin.yml version`
- **Fixed tree (must PASS — it does):** in the full suite,
  `BuildInfoStampTest: 8 passed, 0 failed` (both builds).

## 5. Three-home fingerprint (mandatory section)

All three homes of the candidate jar agree on one identity:

| Home | Value | File / entry | Size / SHA-256 |
|---|---|---|---|
| `BuildInfo.java` (source) | `PART_A "EXC-BOSSTRIAL-"`, `PART_B "0.5.7.1-WRV-"`, `PART_C "20261116-01-471aa6"`, `BUILD_ID = EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`, `VERSION = "0.5.7.1"` | `src/com/winkreyes/excelbosstrial/core/BuildInfo.java` | 1,598 B / `f7697db34414389d60e1e524565502340c8fc19055f7c81e68f20c9bae0473d4` |
| `BuildInfo.class` (in jar) | strings: `EXC-BOSSTRIAL-`, `0.5.7.1-WRV-`, `20261116-01-471aa6`, assembled `EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`, `0.5.7.1` — all present; stale `0.5.6-WRV-`, `20261111-01-3c9e56`, the assembled 0.5.6 ID, raw `0.5.6`, `3c9e56` and `bb18` — all **absent** | `com/winkreyes/excelbosstrial/core/BuildInfo.class` | 520 B / `f6c6754fe022181d8fdf3ea9056242c5608817e856754c95b616e3fbbbd4421a` |
| `plugin.yml` (in jar == tree) | `version: 0.5.7.1` | `plugin.yml` | 15,667 B / `54038fae85010bd90838cb20d716bfbf62917f645838055c06ed78cdab646fa6` |
| `excel-build.txt` (in jar == tree) | `build-id: EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6`, `version: 0.5.7.1`, `build-date: 2026-11-16` | `excel-build.txt` | 128 B / `e45aef333c968c9460d105210277033fa33ad7cf3f58b0c75df83ca466a515f8` |

`javap -constants` on the built `BuildInfo.class` confirms
`PART_B = "0.5.7.1-WRV-"`, `PART_C = "20261116-01-471aa6"`,
`BUILD_ID = "EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6"`,
`VERSION = "0.5.7.1"`.

## 6. Compile and full suite

Build method: the tree's own `package.sh` → `build-test.sh`
(JDK 25 at `~/workspace/.jdk25`, `--release 21`, dependency
classpath per the 0.5.7 build's `work/classpath.txt`, copied
to `work/classpath.txt`). Logs: `work/build-0571.log`,
`work/build-0571-second.log`.

Full-suite tally (house `tally.py` over each log):
**2,865 checks / 103 harnesses, 0 failures** — base
2,856 / 102, delta +9 checks / +1 harness = the new
`BuildInfoStampTest` (8) + the one added
`PluginYmlParseTest` sentence check (+1; that harness now
62 passed, 0 failed). Identical tally on both builds.

## 7. Desk rig boot (Homebase test-server mirror)

The mirror was **free** when checked (Mon Nov 16, 2026
~09:13 CST, via `ssh -F ~/.ssh/config mike-pc` only: no Java
process, ports 25566/25568 free), so the rig ran. Full
evidence: `RIG-EVIDENCE.txt` in this folder; full boot log:
`work/rig-boot.log`.

- Mirror's own BossTrial jar (`ExcelBossTrial-0.5.6.jar`,
  471,888 B / `005bd005…`) was copied to
  `ExcelBossTrial-0.5.6.jar.bak-rig0571-20261116` (hash
  verified identical), moved aside, and the candidate placed
  as the sole BossTrial jar; placed hash on the PC verified
  `94CF076D…` / 472,617 B before boot.
- Boot via the pc-agent launch route (`start-testserver.cmd`,
  the S0-proven route); server reached `Done (22.497s)`.
- Boot lines of record:
  - `[ExcelBossTrial] Loading server plugin ExcelBossTrial v0.5.7.1`
  - `[ExcelBossTrial] Enabling ExcelBossTrial v0.5.7.1` — count **1**
  - `[ExcelBossTrial] Build: EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6` — count **1**
  - old build ID `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56` — count **0**
  - `SEVERE` in the whole boot log — count **0**
- Teardown: RCON `stop` (list showed 0 players), `SERVER-EXIT 0`,
  no Java process and ports free after; candidate jar removed
  and the mirror's original jar restored byte-exact
  (`005bd005…` / 471,888 B, verified after restore). Mirror
  left OFF, as found. (The boot rewrote the mirror's own
  BossTrial data-folder bookkeeping, as any mirror boot does;
  no live state was touched.)

## 8. Remaining chain (for the supervisor)

1. **Filing (supervisor):** one pointer-stub MANIFEST entry
   per PACKET-TEMPLATE.md for `ExcelBossTrial-0.5.7.1.jar` —
   472,617 B / `94cf076d…` — with a G1 same-day live pull of
   the live jar (expected at the retained live filename
   `plugins/ExcelBossTrial-0.4.1.jar`, 472,525 B /
   `4b737c58…`) before filing. Jar swap only; rollback =
   restore the 0.5.7 jar. **Not filed by this worker.**
2. **Failsafe install** under the standing gates
   (backup-first, cap-at-6, emptiness gate). Boot-verify
   against the NEW ID: `Build:
   EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6` count 1,
   `Enabling ExcelBossTrial v0.5.7.1` count 1, SEVERE 0,
   pull-back EXACT — the greps this rig already proved on
   the mirror.
3. **Records:** a NEW draft package for 0.5.7.1 (PR #242's
   0.5.7 package is not amended). **Surfaces:** What's New
   checked-no-entry (§header); Guide / tips / menus
   checked-no-change (no player-facing behaviour change) —
   named here so the filing pass can carry them.
4. **Proof owed after install:** none new — the stamp proof
   is the install's own boot-log grep; the BB-18 Back proofs
   remain owed on their own track, unaffected.

## 9. Deliverables in this folder

- `ExcelBossTrial-0.5.7.1.jar` — the candidate
  (472,617 B / `94cf076d…`)
- `BUILD-RECORD.md` — this record
- `BuildInfoStamp-0.5.7.1.diff` — unified source diff vs the
  0.5.7 tree of record (6 paths, §3)
- `RIG-EVIDENCE.txt` — mirror rig boot evidence (§7)
- `SHA256SUMS` — hashes over the above, verified with
  `sha256sum -c`
- `work/` — the full 0.5.7.1 work tree
  (`work/bosstrial-0571/`), build logs (`base-gate.log`,
  `build-0571.log`, `build-0571-second.log`), the base-gate
  jar, the second-build jar, the pre-edit stamp-test log,
  the rig boot log, and the dependency classpath used
