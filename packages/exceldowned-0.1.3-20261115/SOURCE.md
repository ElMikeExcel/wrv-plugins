# SOURCE — ExcelDowned 0.1.3

Plugin: ExcelDowned 0.1.3 — P-DOWNED-REVIVE-1 fix chain:
(1) totem first — a lethal hit with a totem in either hand is not
intercepted, vanilla consumes the totem, no down; void stays a real
death; (2) window rule — damage while downed eats 0 bleed-out window
by default (damage-eats-window: false), bleed-out is a pure 300 s
wall-clock; (3) Resurrection legs — 'Resurrect' action registered at
onLoad + onEnable, boot self-check WARNING-names any of resurrection /
grouprenewal whose template does not resolve (never fail-open),
ResurrectAction target + fallback both same-world + range-bound,
ranges aligned at 20. Change scope vs 0.1.2 (diff-proven):
DownedListener.java, ExcelDowned.java, MagicBridge.java,
BuildInfo.java changed; FixPolicy.java new; config.yml, plugin.yml,
local-build.sh, DownedTest.java changed; DESIGN-0.1.3.md new.
Build ID: EXC-DOWNED-0.1.3-WRV-20261114-01-d0wn3d.
Tests: 34/34 passed (27 carried + 7 new regression).
Deterministic double-build byte-identical.
Build folder of record: ~/workspace/board-r364/downed-build/
(board run 364). Source tree of record: ExcelDowned-0.1.3/ in that
folder. BUILD-RECORD.md in this package is that build's RESULT.md.
Source tree in this package: src/ (18 .java, ALL files including
PartyProvider.java and TogetherPartyProvider.java — no top-up owed)
+ tests/ (DownedTest.java) + local-build.sh, plugin.yml,
config.yml, DESIGN-0.1.2.md, DESIGN-0.1.3.md.
Sources zip: ExcelDowned-0.1.3-sources.zip — 49,118 B /
SHA-256 3c929d80f07cc60f3bae634c428dcc37cd70f10872f6c1182eaced99c230400e /
MD5 24b22c1bedbd7b4fd399c357caf8139f (24 files: src + tests +
config.yml + plugin.yml + local-build.sh + DESIGN-*.md; the
build-of-record zip name is ExcelDowned-0.1.3-source.zip, identical
bytes — the Drive deposit uses the plural -sources form per the
0.1.4 convention).
Built jar (NOT inside this records package, per house rule):
ExcelDowned-0.1.3.jar — 59,875 B /
SHA-256 31f278bda631fd6dde7cbce1eba267633f35e430489ea29c6437dc92544d9e93 /
MD5 3fa49b0396cb74b08020231007e82522. The Drive deposit carries it;
it is referenced here by hash only.
Base: ExcelDowned 0.1.2 tree. Base gate: the unmodified 0.1.2 tree
rebuilt produced 56,778 B /
2320e812d6c33a21f179b8e64d3f37723f260c97dbefc2eadf73cef97c519396
EXACT (the shipped 0.1.2 jar), 27/27 tests, before any edit.
Install: INSTALLED Nov 15, 2026 by wrv-install-failsafe (12:59 CST
resume window, Install 6 of 13) — see INSTALLED.txt and
INSTALL-PACKET.md in this package.
Status: Installed, not Proven — the D1 / D2 / D3 live proofs are
still owed (see RIG-RESULTS.md). Superseded live by ExcelDowned
0.1.4 on Nov 16, 2026 (wrv-plugins PR #240).
Build record: BUILD-RECORD.md. Packet: INSTALL-PACKET.md.
Tests + rig: RIG-RESULTS.md.

## Drive deposit (Minecraft WRV)

Duplicate check first (2026-11-16): name contains
'ExcelDowned-0.1.3' -> 0 files before upload.
- ExcelDowned-0.1.3.jar -> "Build files" folder
  (1c4f3TZDseHvgWXuN7SAD-PxST9xuVqgI): Drive file
  1Nh5Kbm11bfFss-SocoCKfZtJQXu535hZ, size 59,875,
  MD5 3fa49b0396cb74b08020231007e82522 — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1Nh5Kbm11bfFss-SocoCKfZtJQXu535hZ/view?usp=drivesdk
- ExcelDowned-0.1.3-sources.zip -> "Source" folder
  (1twtEkZWxY3m-93vUJ_5zLuDYdXWBxJjz): Drive file
  1kf5CafTnVSAo9BPuqzZaIgJWHQBYb0-7, size 49,118,
  MD5 24b22c1bedbd7b4fd399c357caf8139f — EXACT;
  download-back MD5 + SHA-256 EXACT.
  Link: https://drive.google.com/file/d/1kf5CafTnVSAo9BPuqzZaIgJWHQBYb0-7/view?usp=drivesdk
- ExcelDowned-0.1.3-SOURCE.md (this file) -> "Source" folder:
  Drive file ID + MD5 are recorded in the publication close-out
  (REPORT.md), since this file cannot carry its own upload ID.
