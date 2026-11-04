# ExcelEliteLadder 0.1.2 - SOURCE

- Plugin: ExcelEliteLadder (ours; "Excel" signature applies - header note in LadderTable.java).
- Version built: 0.1.2 (Nov 1, 2026). Supersedes live 0.1.1.
- Upstream/base: our own source, record = GitHub ElMikeExcel/wrv-plugins PR #70
  (draft, do not merge), branch feature/exceleliteladder-0.1.0 @ d5089101c6b532eb1dd96edb649b3ed58f9871e4,
  folder ExcelEliteLadder/. Fetched file-by-file via the GitHub connector on Nov 1, 2026;
  authenticity cross-checked: tests/fixtures/S5-norm-MobCombatSettings.yml sha256
  01dcf1dc9aea67f23366d28a945b5e75fafdaf0d5abc3dfee56c6a737d29cbe9 matches the PR body.
- Shipped jar (live 0.1.1): ExcelEliteLadder-0.1.1.jar, 31,330 B,
  SHA-256 f7ec4c1ff86487e80a992ce1a982151ec8900ceab482b8ca46c94b42bdf71521,
  MD5 69030edef31240186921bbd17615fb2f - downloaded from the Drive "Minecraft WRV" deposit
  (file id 1qdWZQUSKLZ-Pf4wqJ1ozPllEAatDf-4d, deposited Oct 26, 2026) and MD5-verified;
  preserved at work/deposited-ExcelEliteLadder-0.1.1.jar. PR #70 records the same sha as live.
- BASE PROOF: the unmodified fetched tree (base-0.1.1/) rebuilds BYTE-IDENTICAL to the shipped
  jar on Temurin 25.0.5+8 (~/workspace/.jdk-25.0.5+8, downloaded from Adoptium for this build;
  Steve's original build used the same JDK at /home/box/tools/jdk-25.0.5+8) with compile-only
  deps from ~/workspace/excelshops/build-libs (paper-api, adventure 4.20.0, guava 33.3.1-jre,
  snakeyaml 2.2) + bungeecord-chat/jspecify staged in rig-libraries/. Base suite: 63/63.
  (With the box's Temurin 25.0.4.1 the rebuild differed ONLY in the manifest Created-By line;
  every class and yml was byte-identical - that is why the exact 25.0.5+8 toolchain was used.)
- 0.1.2 output: build-0.1.2/build/ExcelEliteLadder-0.1.2.jar, 31,403 B,
  SHA-256 7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb, deterministic
  (two clean builds identical). Jar-entry diff vs live 0.1.1: LadderTable.class, plugin.yml,
  config.yml only.
- Change scope: LadderTable S4/S5 rows only (see CHANGELOG-0.1.2.md) + version stamps +
  fixtures/tests. No other source file differs from the 0.1.1 tree.
- Tests: plain-JVM suite 77 passed, 0 failed; rig on Paper 26.3-235 + EliteMobs 10.9.12 +
  GuildGate 0.4.0 seeded with the live Nov 1 state (RIG-TEST-RESULTS-0.1.2.md in build-0.1.2/).
- Layout: base-0.1.1/ = pristine fetched tree (+ its rebuild); build-0.1.2/ = the 0.1.2 tree
  with docs and built jar; rig/ = the rig server; work/ = pulled jar + extractions.
