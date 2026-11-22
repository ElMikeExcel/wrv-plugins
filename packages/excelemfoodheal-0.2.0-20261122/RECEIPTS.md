# RECEIPTS — ExcelEMFoodHeal 0.2.0 (D184 out-of-combat bonus)
# Filed Nov 21, 2026. All live receipts same-day (Nov 21, 2026, America/Chicago).

## G1 live-premise receipts — the live 0.1.0 jar and its live config

- Route: Homebase SSH (config ~/workspace/pc-access/ssh-config.txt,
  host mike-pc), PowerShell via EncodedCommand; files pulled by scp.
  Pull timestamp: 2026-11-21 20:22 CST (02:22 UTC Nov 22).
- Live jar path (PC): D:\muse-servers\wrv\plugins\ExcelEMFoodHeal-0.1.0.jar
  - Located via Get-ChildItem -Filter "*FoodHeal*" under
    D:\muse-servers\wrv\plugins: exactly one jar + the data folder.
  - Live size: 15,564 B.
  - PC-side hash: Get-FileHash -Algorithm SHA256 ->
    8C7F1CECB1E6A911FDB6D8C1156D5CA6765DFECA9EF646E393C5BB46C1A59D05
  - Pulled copy: receipts/ExcelEMFoodHeal-0.1.0.live.jar — 15,564 B /
    SHA-256 8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05
    (matches the PC-side hash AND the 0.1.0 build artifact hash in
    ~/workspace/emfoodheal-build-20261120/SHA256SUMS: the live jar is
    the unmodified 0.1.0 build).
- Live config path (PC): D:\muse-servers\wrv\plugins\ExcelEMFoodHeal\config.yml
  - Live size: 1,301 B. PC mtime: 2026-11-20 18:19:54 (the 0.1.0
    install boot; never edited since).
  - Pulled copy: receipts/config.live.yml — 1,301 B / SHA-256
    cd9428299cf69150046825c08b2b216727080edbe3aa093a308e377c18466079
  - Content verdict: byte-identical to the 0.1.0 bundled default
    (`cmp receipts/config.live.yml` against
    ~/workspace/emfoodheal-build-20261120/src/config.yml -> identical).
    Live values: debug: true; instant-percent 1.0;
    rate-percent-per-second 0.5; saturation-multiplier 0.75;
    min-total-percent 1.0; max-total-percent 10.0;
    combat-window-seconds 20; budget-expiry-seconds 90;
    worlds [em_primis, em_adventurers_guild]; food-saturation {}.
    The two D184 keys do NOT exist in the live config yet — the
    packet's config step appends them (see INSTALL-PACKET.md).

## Candidate receipt

- Staged candidate: ~/workspace/foodheal-ooc-2pct-20261121/ExcelEMFoodHeal-0.2.0.jar
  - 16,948 B / SHA-256
    787198b6859eb91bc3a3e452c03cd3b1cb650c035bb678e5129509e6dd971680
  - Deterministic double-build byte-identical (`cmp` clean).
  - Unit tests 145/145 (137 carried from 0.1.0 + 8 BonusPolicy).
  - Rig: Paper 26.2-123 + real EliteMobs 10.9.14 (pinned jar
    14,563,211 B / SHA-256 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd,
    same pin as 0.1.0): 16/16 RIGRESULT PASS, SEVERE 0,
    evidence rig/runtime.log in this folder.
- Source zip: ExcelEMFoodHeal-0.2.0-source.zip — 18,980 B / SHA-256
  a8cb4420a85a092ee6d8b74605efb23e71b14f6e31fe77b4322c7e8535b77ef5.
- SHA256SUMS in this folder covers the candidate, the source zip,
  the pinned EM jar, and both pulled live files (`sha256sum -c`
  verified clean at filing).

## Config-merge premise (why the packet appends instead of replacing)

- Bukkit's saveDefaultConfig never overwrites an existing
  config.yml, so a jar-only swap would leave the live file without
  the D184 keys; the plugin's in-code defaults (enabled, 1.0) match
  the shipped behavior, but the packet still appends the documented
  key block so the live file states the feature explicitly.
- The append is additive-only: every existing live line stays
  byte-identical (in particular debug: true stays as the live file
  has it). A .bak of the pre-append config is kept by the lane.
