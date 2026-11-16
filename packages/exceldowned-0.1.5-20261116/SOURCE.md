# SOURCE — ExcelDowned 0.1.5

Plugin: ExcelDowned 0.1.5 — self-check timing fix: the spell-template
self-check no longer runs synchronously in onEnable. It is deferred
(+200 ticks) with bounded retries (+20 s, +40 s; 3 attempts max) via
the new pure policy class TemplateCheckPolicy.java, probes the `heal`
control key alongside `resurrection` / `grouprenewal` (control false
= registry not yet loaded = defer + retry, never a template WARN),
and the stale "grants of this spell will NPE" wording is corrected.
ONE production file modified (ExcelDowned.java) plus ONE new class
vs 0.1.4; everything else is version knobs (plugin.yml 0.1.5,
BuildInfo fragment, local-build.sh), tests, and DESIGN-0.1.5.md.
Build ID: EXC-DOWNED-0.1.5-WRV-20261116-01-d0wn3d.
Tests: 44/44 passed. Deterministic double-build byte-identical.
Build folder of record: ~/workspace/board-r427/downed-015-build/
(board run 427). Source tree of record: work/build-0.1.5/ in that
folder.
Source tree in this package: src/ (19 .java, ALL files including the
new TemplateCheckPolicy.java) + tests/ (DownedTest.java) +
local-build.sh, plugin.yml, config.yml, DESIGN-0.1.2.md,
DESIGN-0.1.3.md, DESIGN-0.1.4.md, DESIGN-0.1.5.md.
Sources zip: ExcelDowned-0.1.5-sources.zip — 58,395 B /
SHA-256 1924d44601e1030973ade58af8b8ff99249c408073084503cc76574fcaa3079e /
MD5 d616532ac46ccf56e049079abf2bb5f0 (27 files: src + tests +
config.yml + plugin.yml + local-build.sh + DESIGN-*.md).
Built jar (NOT inside this records package, per house rule):
ExcelDowned-0.1.5.jar — 63,701 B /
SHA-256 50d7514934c7f71eaa4840a3024d3cb0594436a6a0409c6c7b2cb21e048b9710 /
MD5 df6794b3c4e33d6168d259774e90d581. The Drive deposit carries it;
it is referenced here by hash only.
Base: ExcelDowned 0.1.4 tree. Base gate: the unmodified 0.1.4 tree
rebuilt produced 59,998 B /
703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60e5f1937599792339
EXACT (the installed 0.1.4 jar), 39/39 tests, before any edit.
Install: INSTALLED Nov 16, 2026 by wrv-install-failsafe (window
marker 09:23:40 CST, boot Done 09:39:53 CST) — see INSTALLED.txt and
INSTALL-PACKET.md in this package. Boot verify: template WARNs 0,
both keys resolve, SEVERE 0.
Status: Installed, not Proven — the S7 live retest is still owed
(see RIG-RESULTS.md).
Build record: BUILD-RECORD.md. Packet: INSTALL-PACKET.md.
Tests + rig: RIG-RESULTS.md. Fix diff: ExcelDowned-0.1.5.diff.

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-16): name contains
'ExcelDowned-0.1.5' -> 0 files before upload.
- ExcelDowned-0.1.5.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1b8Sz20h8Np920mYOQSF-NDxCsuBp1eOL, size 63,701,
  MD5 df6794b3c4e33d6168d259774e90d581 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1b8Sz20h8Np920mYOQSF-NDxCsuBp1eOL/view?usp=drivesdk
- ExcelDowned-0.1.5-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1e3LLXPinhAtw1xp8RLPyTebVSAcVITJA, size 58,395,
  MD5 d616532ac46ccf56e049079abf2bb5f0 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1e3LLXPinhAtw1xp8RLPyTebVSAcVITJA/view?usp=drivesdk
- ExcelDowned-0.1.5-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication close-out
  (REPORT.md), since this file cannot carry its own upload ID.
