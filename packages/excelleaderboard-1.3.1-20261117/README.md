# ExcelLeaderboard 1.3.1 — records package (RECONSTRUCTION)

Draft record — do not merge. Board-manager run 489, B11 D1 row 36 (PR-leg-only).

RECONSTRUCTION STATEMENT (explicit, required):
(a) The original ExcelLeaderboard 1.3.1 source was panel-only and is NOT on disk anywhere in the workspace (SOURCE-MISSING). This package is the BB-08 bytecode-verified reconstruction: the 1.3.0 tree + VanillaStatsCache reconstructed from the live jar's bytecode + cache wiring + plugin.yml bumped to 1.3.1 (tree plugin.yml reads version: 1.3.1).
(b) Row version 1.3.1 is the pre-fix live version; the install receipt heading names the successor 1.3.2 (BB-08 back-button fixes, installed Nov 15, 2026) [MANIFEST-ARCHIVE.md:3595], built on this 1.3.1 base. This package documents the 1.3.1 base.
(c) The rebuilt jar in this package (ExcelLeaderboard-1.3.1.jar, 58,497 B, SHA-256 32a21e96c0886fe60940caa624a8d486986bcb2360d51a1bca33eace84fbb54e) is NOT byte-identical to the live jar (57,897 B, SHA-256 3b2c1f1c11faf9e1bdadd77fc25e5f9c905f3eadb2a472d01da6919e978060ee). Equivalence rests on RECEIPTS.md (carried verbatim): 28/33 jar entries byte-identical to live; the 4 differing classes have identical javap -c instruction streams (LineNumberTable only); MANIFEST.MF differs only in JDK patch string; base suite 188 assertions / 0 failures.
Tree SHA256SUMS from BB-08 preserved as TREE-SHA256SUMS (27/27 content entries verified). Package SHA256SUMS generated at assembly covering all package files.
