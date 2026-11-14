# LANDING VERIFICATION — EliteMobs 10.9.14 (Worker R341, Nov 14, 2026)

Desk-records verification only (read-only). Sources: this packet folder —
INSTALL-PACKET.md, RECEIPTS.md, INSTALLED.txt, OUTCOME.txt,
SCRIPT-OUTPUT.txt, window-1114/ (gate3/, post/, list captures, logs,
script-blocks-*.txt). Panel / MANIFEST / live server NOT touched.
Note: the failsafe closing phase was still appending files during this
check (script-blocks-counts.txt and OUTCOME.txt landed mid-check;
SCRIPT-OUTPUT.txt grew). All values below are from the final snapshot.

## Checks (name / expected / actual / verdict)

1. LIVE-JAR GATE (gate3 pull vs packet + RECEIPTS G1)
   Expected: 14,303,365 B / 3d796000d1d1073dab3dfebbdeddf2a23b6a2e93a1a02ec8515a6ca6afdb9954
   Actual:   window-1114/gate3/EliteMobs-10.9.10.jar = 14,303,365 B / 3d796000d1d1073dab3dfebbdeddf2a23b6a2e93a1a02ec8515a6ca6afdb9954 (EXACT); script block live-pull-live-jar PASS
   Verdict: PASS

2. G1 CONFIG PULLS, gate3 vs RECEIPTS (5 files)
   Expected: ItemSettings 16,076 B / 3bbfe811…e11729; EconomySettings 9,546 B / ea7ca609…147f5; MobCombatSettings 14,529 B / e3a384d0…dd375; config 8,419 B / 8e4d72bf…ff26; spigot 5,057 B / deb4066e…703cb
   Actual: all five gate3 files byte-identical to the G1 receipts AND to pull-g1/ copies; script blocks live-pull-* x5 all PASS, sizes+hashes EXACT
   Verdict: PASS

3. CANDIDATE JAR (staged)
   Expected: 14,563,211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
   Actual: window-1114/staged-update-EliteMobs.jar = 14,563,211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd (EXACT); block verify-staged-candidate PASS
   Verdict: PASS

4. PLACED JAR vs INSTALLED.txt CLAIM
   Expected (INSTALLED.txt): plugins/EliteMobs-10.9.10.jar = 14563211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
   Actual: window-1114/post/EliteMobs-placed.jar = 14,563,211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd (EXACT); block verify-placed-jar-pullback PASS
   Verdict: PASS

5. BACKUP + CAP-AT-6
   Expected: one fresh backup "EM 10.9.14 pre-install 1114"; total <= 6; prunees named; Daily newest + two newest manuals preserved
   Actual: backups-final-listing.txt shows the new backup (11/14/2026 11:13 AM, 7.7 GB, Manual) and final total 6 (3 manual + 3 Daily, Daily 11/13 newest intact); block backup-cap-final PASS (total=6, prune_needed=0 at final state). Prunee named in OUTCOME.txt only: ONE oldest manual pruned before creation — "pre-wn013-summons019-20261114-0633" (11/14/2026 6:38 AM, 7.5 GB). The prunee does NOT appear in the retained final listing (expected — listing is post-prune).
   Verdict: PASS (prunee evidence = OUTCOME.txt prose; no pre-prune listing retained)

6. EMPTINESS (packet gate 2: list = 0/10 twice, 60 s apart)
   Expected: 0/10, twice, >= 60 s apart
   Actual: SEVEN retained captures, all 0/10 — Essentials-format RCON: list-1 (11:05:57 CST), list-2 (11:09:45 CST, ~3m48s after list-1), list-3-preshutdown (11:21:32), list-4-departure (11:29:53), each 57 B reading "There are 0 out of maximum 10 players online"; vanilla minecraft:list: list-vanilla-test, list-5-vanilla-a (11:32:52 CST), list-6-vanilla-b (11:34:07 CST, ~75 s after a), each 45 B reading "There are 0 of a max of 10 players online". Blocks emptiness-vanilla-a/b PASS.
   FLAG: script-blocks-gate.txt records emptiness-list-1 and emptiness-list-2 as RESULT: FAIL with "online=unparsed" — a parser limitation on the Essentials section-coded wording (disclosed in the SCRIPT-OUTPUT.txt header note), NOT a player-count failure; the raw captures read 0/10 and the vanilla-format pair passes the bundle.
   Verdict: PASS (with parser-artifact FLAG above)

7. BOOT DONE LINE + TIME (vs INSTALLED.txt)
   Expected: boot Done 11:23:58 CST (INSTALLED.txt)
   Actual: window-1114/post/latest-post.log line 784: "[17:23:58] [Server thread/INFO]: Done (23.759s)! For help, type "help"" — log clock is UTC; 17:23:58 UTC = 11:23:58 CST (EXACT match); block boot-log-scan-post PASS (done=present)
   Verdict: PASS

8. SEVERE COUNT, post-boot log
   Expected: 0
   Actual: grep SEVERE over post/latest-post.log = 0 occurrences (independently re-counted); blocks verify-severe-zero PASS in SCRIPT-OUTPUT.txt and script-blocks-counts.txt
   Verdict: PASS

9. VERSION CONFIRMATION POST-BOOT
   Expected: EliteMobs 10.9.14
   Actual: post log line 324 "Enabling EliteMobs v10.9.14" (count 1, block PASS); line 61 "Loading server plugin EliteMobs v10.9.14"; plugin list line shows EliteMobs (10.9.14); window-1114/em-version-post.txt (36 B, section-coded) decodes to "[EliteMobs] version 10.9.14" (the /em version RCON reply)
   Verdict: PASS

10. GUILD SEAL STATE (must still read LOCKED 4 of 5)
    Expected: LOCKED, 4 of 5
    Actual: post log line 343: "ExcelGuildGate enabled: 4 of 5 seals broken, guild LOCKED"; post/guildgate-state.yml byte-identical to gate3 (unlocked: false; broken = ender_dragon, wither, elder_guardian, raid; warden broken: false); block boot-log-scan-post reports guild_locked=4of5 PASS
    Verdict: PASS

11. TUNED KEYS POST-BOOT (packet verify step 2)
    Expected: flatDropRateV3 0.0; enableCurrencyShower false; currencyShowerTierMultiplier 0.5; spigot maxHealth.max 100000000
    Actual: all four read EXACT in post/ pulls (ItemSettings.yml:97, EconomySettings.yml:15/17, spigot.yml:58). Drift gate3->post: EconomySettings, MobCombatSettings, ValidWorlds, spigot byte-identical; ItemSettings differs ONLY in one comment line (line 53, $itemMaterial wording, +31 B, no value change); config.yml gained ONE additive line (autoUpdateTranslations: true, +29 B) — matches OUTCOME.txt step 2 and the packet's additive-defaults allowance
    Verdict: PASS

12. SEALFREEZE + PLAYER DATA + EXPANSIONS (packet verify step 3)
    Expected: SealFreeze ARMED, 11-player all-zero baseline; player_data.db 11 rows zeroed; level store zeroed; M-02 still proposed
    Actual: sealfreeze-state.yml armed: true, byte-identical gate3/post; baseline files byte-identical (11 rows, all fields 0); post log line 454 "ExcelSealFreeze resumed ARMED (baseline of 11 players…)"; player_data.db byte-identical gate3/post (28,672 B / 541cebeb… both), 11 PlayerData rows, sums of CurrencyV2/CurrencyCents/Score/Kills/HighestLevelKilled/QuestsCompleted/DungeonsCompleted/all SkillXP_*/GamblingDebt* all = 0 (Deaths sum 18 = history, exempt per wipe scope); expansions-level-store.txt byte-identical (11 rows 0|0); expansions-release-state.txt byte-identical (M-02|proposed)
    Verdict: PASS

13. WORLDS + VALIDWORLDS (packet verify step 4)
    Expected: em_primis, em_primis_elementals_arena, guild world loaded; ValidWorlds intact; Primis census 17/17
    Actual: post log lines 843/848/853 "Successfully loaded world em_primis / em_primis_elementals_arena / em_adventurers_guild"; blocks verify-world-primis (count 2 — pattern also matches the arena line) and verify-world-arena (count 1) PASS; ValidWorlds.yml byte-identical gate3/post (em_primis + arena true)
    FLAG: the 17/17 Primis content census has NO in-window evidence in the retained records — OUTCOME.txt step 4 states no census counter exists in the boot log and the 17/17 stands carried from the morning S-7 install record.
    Verdict: PASS for world loading; census = MISSING in-window (carried per OUTCOME, flagged)

14. ARCHIVER / LANGUAGE PASS (packet steps 5b/5, verify step 5)
    Expected: archiver count recorded; moved files reviewed as bundled defaults only
    Actual: post log contains NO outdated-file archiver move lines; the only language line is line 271 "Updated (EN) language file. Took 20ms" plus line 804 EliteMobs "Language set to English - using plugin defaults". OUTCOME.txt step 5 asserts ZERO files moved and no new batch folder under plugins/MagmaCore/outdated files/EliteMobs — no directory listing of that folder is retained in window-1114/, so the zero-move claim rests on OUTCOME + absence of move lines in the retained boot log.
    Verdict: PASS by log absence + OUTCOME assertion; independent folder-listing evidence = MISSING (flagged)

15. ABILITY BRIDGE (packet verify step 6)
    Expected: ExcelAbilityBridge enabled, "bridge online" line, no degradation line
    Actual: post log line 868 "EliteMobs class system (ACS) is up - Bedrock ability bridge online: /abilities" (count 1, block verify-bridge-online PASS); no degradation/stand-down line found in the post log
    Verdict: PASS

16. FMM / SPELLCASTER (packet gates 3 + verify step 7)
    Expected: FMM baseline FreeMinecraftModels-2.15.0.jar; post-boot FMM content loaded, no missing-content lock
    Actual: post log: FreeMinecraftModels v2.15.0 loading (line 47) + enabling (line 182), "Loaded 12 custom item definition(s)", "[FMM Shop] Initialized", "Fully initialized!" (line 803); NO spellcaster missing-content lock line anywhere in the post log. OUTCOME.txt step 7 notes /em class info is player-only from console, so the direct class check rides the post-5/5 player proofs.
    Verdict: PASS by log; direct /em class info check = NOT RUNNABLE from console (per OUTCOME, flagged)

17. PLUGIN ENABLE SET (packet verify step 8)
    Expected: ExcelSealFreeze, ExcelExpansions, GuildGate, BossTrial all enabled
    Actual: post log enabling lines present for all four (GuildGate line 340, BossTrial line 366, SealFreeze line 453, Expansions line 469)
    Verdict: PASS

18. ACQUISITION ROUTE
    Expected (packet): primary /em downloadpluginupdate OR Homebase fallback push; record which
    Actual: OUTCOME.txt records PRIMARY route — RCON /em downloadpluginupdate staged plugins/update/EliteMobs-10.9.10.jar (14,563,211 B, 11:17:53 CST), staged pull-back hash EXACT vs candidate, restart applied it, /em applypluginupdate NOT used; outgoing jar preserved on-server as EliteMobs-10.9.10.jar.held-20261114-pre-em10914 (asserted in OUTCOME; the held file itself is not in the retained pulls). Retained staged-update-EliteMobs.jar matches the candidate hash EXACTLY (check 3).
    FLAG: window-1114/em-downloadpluginupdate.txt is 2 B (CRLF only) — the console reply text was NOT retained with content; the route is evidenced by OUTCOME + the staged jar's hash, not by a retained reply.
    Verdict: PASS (with empty-reply FLAG)

19. MARKER DISCIPLINE (packet gate 1)
    Expected: own WINDOW-IN-PROGRESS marker at open; re-check before Shut Down and before Start
    Actual: OUTCOME.txt asserts marker created 11:05:55 CST, re-checked before Shut Down (own marker only) and before Start; lane probe PASS attempt 1 (DESKTOP-IL05DQR), no fallback used. No marker file or probe log is retained in window-1114/ — assertion-only in the desk records.
    Verdict: PASS per OUTCOME assertion; independent artifact = MISSING (flagged)

20. SCRIPT-BUNDLE VERBATIM BLOCK COVERAGE
    Expected: verbatim bundle blocks covering the window's checks, all accounted for
    Actual: SCRIPT-OUTPUT.txt (final) = 21 STEP blocks, 21 RESULT: PASS, 0 FAIL. Per-file block files total 29 blocks: script-blocks-gate.txt 15 (13 PASS, 2 FAIL — the two Essentials-format emptiness parser failures, check 6), script-blocks-counts.txt 8 (8 PASS), script-blocks-boot.txt 3 (3 PASS), script-blocks-post.txt 3 (3 PASS). Distinct STEP names across per-file blocks = 23; SCRIPT-OUTPUT.txt carries 21 of 23 — the 2 not carried are exactly emptiness-list-1/emptiness-list-2 (parser FAILs), whose subject is covered in SCRIPT-OUTPUT by emptiness-vanilla-a/b PASS blocks + the header disclosure note + the retained raw captures.
    Verdict: PASS (coverage complete with the 2 parser-FAIL blocks disclosed, not hidden)

21. INSTALLED.txt INTERNAL CONSISTENCY
    Expected: marker records installed 2026-11-14 11:23 CST, boot Done 11:23:58 CST, window backup "EM 10.9.14 pre-install 1114", lane PC/Homebase with panel for backup + restart only
    Actual: all four claims corroborated — boot Done (check 7), backup name in listing (check 5), placed jar (check 4), lane per OUTCOME (PC lane, panel backup + Shut Down/Start only).
    FLAG (cosmetic): the marker's entry line still begins "entry: PENDING - EliteMobs 10.9.14…" — the failsafe's marker format retains the MANIFEST entry heading; the file's fields record the completed install. Not a state contradiction, flagged for the record.
    Verdict: PASS (with cosmetic FLAG)

## Key evidence files relied on (path / size / SHA-256)

- INSTALL-PACKET.md / 9001 B / a9f862e97fd3c1ec2a05fef7e1cb9476c7bebe55ffe6135baba92e4d10d4d699
- RECEIPTS.md / 4092 B / 90383d41df5647d8fc5d924fcc1220604699d3fd78eee1508c7a1f6ce76d1a01
- INSTALLED.txt / 411 B / 8f2f4afbabcf122e29f855ae2ecdf8add2bab21cefe29b446478c129184a579b
- OUTCOME.txt / 6474 B / 2f0aa7df8d473e1542ef5753e4d817789de5368844b675f206bcf60fd1dd9c4f
- SCRIPT-OUTPUT.txt / 4997 B / 695ff3cbee670b757db66979d95756b455367d632e71e6e74800b2c76b556c78
- window-1114/gate3/EliteMobs-10.9.10.jar / 14303365 B / 3d796000d1d1073dab3dfebbdeddf2a23b6a2e93a1a02ec8515a6ca6afdb9954
- window-1114/staged-update-EliteMobs.jar / 14563211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
- window-1114/post/EliteMobs-placed.jar / 14563211 B / 4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd
- window-1114/post/latest-post.log / 116115 B / 0d3664b4cbbde08c564442ba2c8520924205488587e81dc7e0aee599f3bd73be
- window-1114/gate3/latest-pre.log / 118037 B / 4bcd27e8275d9701aad33499f47e4dfe062614e3a117a4264f88dfb9fd4b5d56
- window-1114/backups-final-listing.txt / 411 B / bd49b443aef4dedced7fb41a74e7e899aa6b61fd3a30d7b0f2579209df16a190
- window-1114/post/player_data.db / 28672 B / 541cebeb2d72db1e316fb9dbb0fd6c8b6b5ef6b1ffca03ecf3c45e1785edbe3e (gate3 copy identical)
- window-1114/post/guildgate-state.yml / 1015 B / 79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0 (gate3 copy identical)
- window-1114/em-version-post.txt / 36 B / da5e1fffbca6c5942adf53e65e8f46a3b4fd083066b77cacaf10156ac52da8b3
- window-1114/script-blocks-gate.txt / 3255 B / 9e77fec394c1fb7024080fc8368bce4750f8b7c201bdcb4ef927707f59f6a99d
- window-1114/script-blocks-counts.txt / 1020 B / 2fa76f0c6333e34781029dde2dd20407d068fc2b5833353fc4de0f77529967a3
- window-1114/script-blocks-boot.txt / 475 B / 21d41ae7f32146fe7a2613dbf1b3ace5dfdf08990969c8e9e6ce5254e702b145
- window-1114/script-blocks-post.txt / 711 B / f633800d4804bb692e816a7aef40559f69abe53ce11fcf503ae9a26d4180b2a3

## Overall verdict

VERDICT: LANDING VERIFIED — PASS WITH FLAGS. Every gate and post-install
check that the retained records can decide is PASS with EXACT size/hash
matches (live jar gate, candidate, placed jar vs INSTALLED.txt claim,
tuned keys, player data, guild LOCKED 4 of 5, boot Done 11:23:58 CST,
SEVERE 0, version 10.9.14 by log + /em version reply, backup + cap-at-6
with prunee named in OUTCOME). No mismatches found. Flags, none of which
contradict the landing: (1) two emptiness script blocks FAIL on a parser
limitation against the Essentials list format — raw captures read 0/10
and the vanilla pair passes; (2) em-downloadpluginupdate.txt retained
empty (2 B) — acquisition proven by staged-jar hash + OUTCOME, not by a
retained console reply; (3) Primis 17/17 census has no in-window record
(carried from the S-7 record per OUTCOME); (4) archiver zero-move and
marker re-checks are OUTCOME-asserted with no independent retained
artifact; (5) INSTALLED.txt entry line still reads "PENDING -" (marker
format, cosmetic). Verification ran while the failsafe closing phase
was still appending files; values above are the final snapshot.
