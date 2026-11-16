# SOURCE — ExcelDowned 0.1.4

Plugin: ExcelDowned 0.1.4 — provider fix (P-DOWNED-PROVIDER-1):
TogetherPartyProvider.membersOf() reads the `members` FIELD first
(public getField, then declared-field + setAccessible), keeping the
method-name list as fallback; fail-soft unchanged, UNKNOWN-on-empty
not loosened. ONE production file changes vs 0.1.3;
BuildInfo.java fingerprint fragments only otherwise; config.yml
byte-identical to 0.1.3.
Build ID: EXC-DOWNED-0.1.4-WRV-20261116-01-d0wn3d.
Tests: 39/39 passed; negative control 36 pass / 3 fail on the
pristine 0.1.3 provider (exact). Deterministic double-build
byte-identical.
Build folder of record: ~/workspace/board-r417/downed-014/
(board run 417, worker W7). Source tree of record:
work/build-0.1.4/ in that folder.
Source tree in this package: src/ (18 .java, ALL files including
PartyProvider.java and TogetherPartyProvider.java — no top-up owed)
+ tests/ (DownedTest.java) + local-build.sh, plugin.yml,
config.yml, DESIGN-0.1.2.md, DESIGN-0.1.3.md, DESIGN-0.1.4.md.
Sources zip: ExcelDowned-0.1.4-sources.zip — 54,754 B /
SHA-256 ba5b6c08a8c7dc50a88763a7180272240f1821eb1d788f248c03e709172f6db0 /
MD5 16c7a78a92453ec5a66fe1b0b429e3c6 (25 files: src + tests +
config.yml + plugin.yml + local-build.sh + DESIGN-*.md).
Built jar (NOT inside this records package, per house rule):
ExcelDowned-0.1.4.jar — 59,998 B /
SHA-256 703d7f9a45b4c0d00f49bfddb1f404bd5990601ae37e9b60e5f1937599792339 /
MD5 128cf1b78101d56baecb6b3a1dcc7104. The Drive deposit carries it;
it is referenced here by hash only.
Base: ExcelDowned 0.1.3 tree. Base gate: the unmodified 0.1.3 tree
rebuilt produced 59,875 B /
31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93
EXACT (the live 0.1.3 jar), 34/34 tests, before any edit.
Install: INSTALLED Nov 16, 2026 by wrv-install-failsafe (04:43 CST
window) — see INSTALLED.txt and INSTALL-PACKET.md in this package.
Status: Installed, not Proven — the S7 live retest is still owed
(see RIG-RESULTS.md).
Build record: BUILD-RECORD.md. Packet: INSTALL-PACKET.md.
Tests + rig: RIG-RESULTS.md. Fix diff: TogetherPartyProvider.patch.

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-16): name contains
'ExcelDowned-0.1.4' -> 0 files before upload.
- ExcelDowned-0.1.4.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1NNUZe0NKiu7mrtuOx-UMneHv4Z2ulV9d, size 59,998,
  MD5 128cf1b78101d56baecb6b3a1dcc7104 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1NNUZe0NKiu7mrtuOx-UMneHv4Z2ulV9d/view?usp=drivesdk
- ExcelDowned-0.1.4-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1YHW6dm4Fk1a7uTf9mpOgI4C3kEht2iaa, size 54,754,
  MD5 16c7a78a92453ec5a66fe1b0b429e3c6 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1YHW6dm4Fk1a7uTf9mpOgI4C3kEht2iaa/view?usp=drivesdk
- ExcelDowned-0.1.4-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication close-out
  (PUBLISH.md), since this file cannot carry its own upload ID.
