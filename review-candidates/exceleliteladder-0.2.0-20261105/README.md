# ExcelEliteLadder 0.2.0 — source record

Plugin: ExcelEliteLadder  
Version: 0.2.0  
Record assembled: Nov 5, 2026 (records close-out pass)  
Local build folder: `eliteladder-config-build`

## What this is

The 2.0 change (Mike's approval: 2.0 decision sheet item 3, Nov 5, 2026 —
"Yes to all 2.0"): the mob ladder table leaves the jar for
`plugins/ExcelEliteLadder/ladder.yml`, so a ladder retune no longer needs a
jar cycle. The accepted trade, in Mike's terms: the config loader is strict
enough to be nearly as typo-proof as the hardcoded table was — strict
validation (types, ranges, monotonicity, no missing/unknown keys), ANY
defect refuses the whole table with a loud SEVERE-free WARN naming the
defect, and the last-good table keeps running (at first boot the fallback
is the built-in copy of the same table). The ladder VALUES are unchanged:
the shipped `ladder.yml` equals the 0.1.2 hardcoded table value-for-value
(migration equality suite, exact double equality on 5 steps × 6 keys), and
a fresh 0.2.0 install behaves identically to 0.1.2. New console surface:
`/eliteladder reload`; `/eliteladder status` and the enable line report
the table source (config vs built-in default). Full detail:
CHANGELOG-0.2.0.md.

## Built artifact (not included in this record — text only)

- Jar: `ExcelEliteLadder-0.2.0.jar` — 40,475 bytes, SHA-256 `781eea2fe99269fb1400b217f5026f29bcd03dc919501cc84b28aa56e083afd0`
  (deposited in Drive 'Minecraft WRV' > Build files; deterministic build —
  build.sh assembles twice and byte-compares).

## Install / live status

Installed live Nov 5, 2026, 6:04–6:20 AM CST by wrv-install-failsafe
(backup "EliteLadder pre-0.2.0 20261105" first, cap-at-6 prune of one
oldest manual; boot Done 19.854s; enable line reports "ladder table:
ladder.yml (config) — default generated on first boot"; ladder.yml
generated in the data folder; `eliteladder status` + `eliteladder reload`
verified live; plugins 78; guild LOCKED 3 of 5; TPS 20.0; SEVERE 0 —
INSTALLED.txt). Supersedes 0.1.2 (recorded in packages/
exceleliteladder-0.1.2-20261103). Player-facing change: none. Player
proofs owed: none.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build
  outputs excluded, see FILE-LIST.txt). The base-proof rebuilt 0.1.2 jar
  is likewise excluded; its script `base-proof/build-base.sh` is included.
- `plugin.yml`, `config.yml`, `ladder.yml` — as shipped in this version
- `TEST-REPORT.md` — the build lane's full test report; `TEST-RESULTS.txt`
  — the close-out summary
- `BUILD-RECORD.md`, `INSTALL-PACKET.md` (as installed), `INSTALLED.txt`
  (the install record), `MANIFEST-ENTRY.md` (the filed queue entry)
- `rig/evidence/` — the three Paper 26.3-235 rig boot logs
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — compile inputs, hash-pinned
- `FUNCTIONAL-CHANGE.diff` — change vs the 0.1.2 record tree
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
