# RECEIPTS — EliteMobs 10.9.14 install packet (filed Nov 14, 2026)

## Candidate jar (acquisition, Nov 14, 2026, Homebase)

- EliteMobs 10.9.14, file EliteMobs.jar — 14,563,211 B
  SHA-256 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
  SHA-512 07ec189bb02f872f7a31bf2a7b806ab66c8c3b302057e3460d6e5292d4821909
          0073ee4bb2394ce4794735ba91069fd64bacfa6a4fe6d120729ff0e8f8546e85
  SHA-1   9bb4febfd82911947b0672d35751eb29f7e37517
- Acquired from Modrinth (official distribution channel): the local
  SHA-512 and SHA-1 match the hashes the Modrinth API publishes for
  the 10.9.14 primary file exactly; API size matches (14,563,211 B);
  API metadata: published 2026-11-07, loaders paper/purpur/spigot,
  game versions include 26.3.
- plugin.yml inside the jar: name EliteMobs, version 10.9.14, main
  com.magmaguy.elitemobs.EliteMobs, api-version 1.21.4, softdepend
  includes FreeMinecraftModels.
- The Spigot direct-download route (resource 40090) was tried first
  on Homebase and returned a 5,747 B HTML page (sign-in/Cloudflare
  wall) — not used; no credentials were used anywhere.
- Staged artifact: C:\Users\MikeExcel\muse-tools\em10914\EliteMobs.jar
  on Homebase (the packet's fallback acquisition; primary in-window
  route is /em downloadpluginupdate).

## G1 live-premise pulls (Nov 14, 2026, 10:42:49–10:43:10 CST)

Homebase rclone (shockbyte:) read-only pulls, staged on the PC at
C:\Users\MikeExcel\muse-tools\em10914\g1-live\; the five text files
are also copied into this packet folder under pull-g1/ (the 14 MB jar
is cited by PC path + hash only).

- plugins/EliteMobs-10.9.10.jar (LIVE JAR GATE)
  14,303,365 B / 3d796000d1d1073dab3dfebbdeddf2a23b6a2e93a1a02ec8515
  a6ca6afdb9954
- plugins/EliteMobs/ItemSettings.yml -> pull-g1/ItemSettings.live.yml
  16,076 B / 3bbfe8110a46123bd252c81ed1138e5c497368a17fd6d7afc15d36
  52afe11729 — verified line: flatDropRateV3: 0.0
- plugins/EliteMobs/EconomySettings.yml -> pull-g1/EconomySettings.live.yml
  9,546 B / ea7ca609ec447998b050c4954e87ff497fd288773b4169935db4e67
  823b147f5 — verified lines: enableCurrencyShower: false (line 15);
  currencyShowerTierMultiplier: 0.5 (line 17)
- plugins/EliteMobs/MobCombatSettings.yml -> pull-g1/MobCombatSettings.live.yml
  14,529 B / e3a384d02784dc31f0d091d5dbd89544e58b803de0f04b9bf98da62
  ea2bdd375
- plugins/EliteMobs/config.yml -> pull-g1/config.live.yml
  8,419 B / 8e4d72bfaaa12f9f4d4a04866317eee4077382cccc28fb1b11a8344
  ac319ff26
- spigot.yml -> pull-g1/spigot.live.yml
  5,057 B / deb4066ed063fb56bef783322c102a9f84af3bc5b7dfd2a5fa476ed
  1ccb703cb — verified: settings.attribute.maxHealth.max = 100000000

Note on the historical "TrainingWheels line" tuned-key phrase (Nov 1,
2026): the four pulled EM configs contain no TrainingWheels key
(searched Nov 14, 2026). The packet therefore gates on the three
verified tuned keys above plus whole-boot drift review; the
TrainingWheels integration is Excel-side and untouched by this swap.

## Qualification + rig-check citations

- ~/workspace/em-update-qualification-20261114/REPORT.md
  18,511 B / 06e169ceb4bbbd89eb785126c4c997194b1a14fac917f133275f2
  8ee3e051709 (verdict SAFE TO FILE)
- ~/workspace/em-update-qualification-20261114/RECEIPTS.md
  6,310 B / 55b2337efc9039019862bdebe4b0e10f282aeb62ba450da2d9a62be
  27e693510 (web sources S1–S18)
- ~/workspace/em-update-qualification-20261114/RIG-CHECK.md
  10,044 B / a8bb16f0e908683c173a13551bab1ecfb55a1808575925f72ae8fd
  3bd039551d (pre-filing rig check: PASS)
- javap dumps (ACS surface, old vs 10.9.14):
  ~/workspace/em-update-qualification-20261114/javap/ and
  C:\Users\MikeExcel\muse-tools\em10914\javap-*.txt

## Mirror-fidelity note

The live jar, ItemSettings.yml, config.yml and spigot.yml hashes
above are byte-identical to the PC test-server mirror copies the rig
boot ran against — the Nov 14 rig check was effectively a
live-config boot. The mirror carries no EM world folders and no FMM
jar, so world loading and the FMM version record remain live-window
verifications in the packet (gates 3 and verify 4/8).
