# ExcelProtect 0.1.12 — records package staging notes

Staged Nov 16, 2026 (worker W4, board supervisor run 438).
Published as an OPEN DRAFT record PR in wrv-plugins at
`packages/excelprotect-0.1.12-20261116/` — never merged.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-16T20:08:20Z, result NO MATCH for
'excelprotect-0.1.12'; also verified NO MATCH by run 438 W2
for 'excelprotect-0.1.12' / 'protect-0.1.12') — PR number
filled in at publish time in the publish record, not here.
Recording work pre-approved (Mike, Nov 4, 2026).

## Package

- File count: 70 files total on disk, including this
  RECORDS-STAGING.md and SHA256SUMS.
- SHA256SUMS: 69 entries — every file in the package
  except SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the
  publish record (W4-PROTECT-RECORDS.md) for the verified
  counts (local + Homebase).
- Jar: NOT included in the package (fleet precedent:
  ExcelProtect 0.1.11 PR #236 carries the source zip, not
  the jar; Together 0.6.0 PR #244 and BossTrial 0.5.7.1
  PR #245 likewise record jar identity only). Candidate
  jar of record: ExcelProtect-0.1.12.jar — 304,634 B /
  SHA-256 6e3cf3981f38f92d3489bdde60b21f1c44ab202c26a55b70b57f9b5200d51e4b
  / MD5 6b7a75035272f53523a1f2fe224a16c0, build
  EXC-PROTECT-0.1.12-WRV-20261116-01-0d1a12.
  INSTALLED Nov 16, 2026, 13:33 CST (see INSTALLED.txt +
  OUTCOME.txt; placed pull-back == candidate, 19 PASS /
  0 FAIL, SEVERE 0). The source zip IS in the package as
  ExcelProtect-0.1.12-src.zip (214,914 B / SHA-256
  e392691c21de5b2b468af0f875923d711c5021180e7b75228f60214f244de7d2),
  mirroring the 0.1.11 package shape.

## What 0.1.12 fixes

P-PROTECT-EM-1: ExcelProtect 0.1.11's EM block-attempt
recorder captured NOTHING in live EliteMobs worlds,
because EliteMobs stops break/place at BlockDamageEvent /
BlockCanBuildEvent (LOWEST), so the terminal
BlockBreakEvent / BlockPlaceEvent that 0.1.11 listened to
at MONITOR never fire there. 0.1.12 adds two early-stage
MONITOR observation points in BlockAttemptWatch
(onDamage records cancelled damage as break via "damage";
onCanBuild records denied builds as place via "can_build",
attempted type from the event material), keeps the
terminal handlers as backstops, adds the additive `via`
field on attempt records, and excludes `via` from the
coalescer key so one physical attempt yields exactly ONE
record (first via wins). Config unchanged (byte-identical
to 0.1.11). Full design: DIAGNOSIS.md in this package.

- Tests: full suite 125 passed / 0 failed (117
  pre-existing + 8 new pinning tests). Deterministic
  double-build byte-identical. Base gate: 0.1.11 rebuilt
  byte-identical before the change.
- Mirror rig: RIG PASS with real EliteMobs as the
  canceller (guild + Primis break/place each recorded
  exactly once; toggle OFF records nothing while EM still
  blocks) — see RIG.md. The em_id_* instance leg was
  NOTRUN on the mirror and rides the live re-proof.

## What was copied from where

Template: the ExcelProtect 0.1.11 records package
(`packages/excelprotect-0.1.11-20261115/`, PR #236) —
same flattened layout: source tree at package root.

- Source tree (64 files): copied verbatim from
  ~/workspace/board-r432/protect-build/build-tree/ —
  src/ (60 .java), tests/ (1 file), plugin.yml,
  config.yml, local-build.sh. The build tree's libs/
  dependency jars and the candidate jar are NOT in the
  package (fleet precedent); the same 64 files plus the
  directory entries are exactly the content of the
  deposited source zip.
- ExcelProtect-0.1.12-src.zip =
  ~/workspace/board-r432/protect-build/excelprotect-0.1.12-source.zip
  (byte-identical copy, renamed to the 0.1.11 pattern).
- BUILD-RECORD.md = board-r432/protect-build/BUILD-RECORD.md.
- DIAGNOSIS.md = board-r432/protect-em-fix/DIAGNOSIS.md.
- RIG.md = board-r432/protect-rig/RIG.md.
- Install evidence: INSTALL-PACKET.md, INSTALLED.txt,
  OUTCOME.txt, RECEIPTS.md = board-r432/protect-filing/
  (failsafe install Nov 16, 2026, 13:33 CST window;
  placed jar pull-back EXACT).
- SHA256SUMS was generated locally for this package.
  The build folder's own SHA256SUMS (9 entries) verified
  9/9 OK at staging time.

## Drive deposit

- ExcelProtect-0.1.12.jar -> Build files folder:
  Drive file 1UVkQItm479InTmwkE0ck5WZ82Td3-YbZ,
  304,634 B, MD5 6b7a75035272f53523a1f2fe224a16c0 EXACT;
  download-back SHA-256 6e3cf398...51e4b EXACT.
- ExcelProtect-0.1.12-src.zip -> Source folder:
  Drive file 1q4X7yqCz7QIF99enG_7sJDHq1J9-C4h2,
  214,914 B, MD5 aa3388f0ed8f4b0eab2e96129d7f47a5 EXACT;
  download-back SHA-256 e392691c...7d2 EXACT.

## Proof still owed

Live re-proof: guild break/place, Primis break/place, a
real em_id_* instance break/place, overworld controls
silent, attempts-toggle flip (needs its own restart
window if scheduled). Until it lands, ExcelProtect
0.1.12 is Installed, not Proven.
