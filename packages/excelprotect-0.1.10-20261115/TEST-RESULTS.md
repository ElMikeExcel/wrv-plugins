# TEST RESULTS - ExcelProtect 0.1.10 (P-PROTECT-WATCH-1, Audit Watch)

Source of record: BUILD-RECORD.md in this package (desk build, board run 364, worker W11) + the build folder evidence logs. Live server was never touched by testing; rig work ran locally.

## Base gate
- 0.1.9 sources rebuilt byte-identical to the tree-of-record jar: ExcelProtect-0.1.9.jar, 269208 B, SHA-256 893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626.
- Base suite on 0.1.9 sources: 99 found / 99 successful / 0 failed.
- Evidence: build folder evidence/base-gate-build.log (953be9cdfe3b7a0c3ec070d663657902737e359f64dc675c6227d21369fe3fdb).

## Candidate suite
- ExcelProtect-0.1.10.jar: 295195 B, SHA-256 dd78718e789492865aec621dda6f5ccefdcce57a4df95b0b4bc360c15de1be61 (fingerprint EXC-PROTECT-0.1.10-WRV-20261114-01-0d1a10).
- Two independent builds (A and B) produced the identical hash - deterministic. Evidence: build folder evidence/build-0110-a.log (f84a314669a87ae92473cc1db5e14d8a8014e26c281ff8f5fd0ba8383a17bced), evidence/build-0110-b.log (fdd2eae0cf7e9301c85a1a76d0fb20e3abd297698041ef1979280f440625fdb5), evidence/candidate-a.jar (byte-identical to the candidate).
- Suite on 0.1.10: 107 found / 107 successful / 0 failed (both builds; 99 base + 8 new audit-watch cases: auditReserveAdoptedHoldsNothing, auditReserveClampWalkSkipsOverCapAndFitsSmallerLater, auditReserveFloorBindsBelowCeiling, auditReservePercentFoldsTiersAndUnits, auditLogRoutesPerPlayer, auditReportBuildsPerPlayerStory, auditWatchReasonMapping, gameplayEventCarriesKindAndFields).

## Local rig boot - PASS
- Paper 26.3 (pre-patched rig directory) + Magic 11.2.5 + ExcelSummons 0.1.9 + candidate 0.1.10: boot Done (40.115s), SEVERE 0, "audit watch: Magic CastEvent + PreCastEvent feeds hooked" and "audit watch: ON dir=plugins/ExcelProtect/audit per-player daily files magic=on summons=on", READY version=0.1.10, audit/ folder created, /excelprotect status answered version=0.1.10, clean disable.
- Evidence: rig-evidence/rig-boot.log (33022 B / 86b913bc87486f6b8616ac7a730f174e66c1886bb814fd11e6d022fbb20bc1cf).
- NOT proven by this rig (no client in the VM): a real in-game cast / summon / dismiss producing audit lines end to end - that proof was written into the packet as the post-install player proof owed (Mike).

## Install verification (failsafe window, Nov 15, 2026)
- SCRIPT-OUTPUT.txt in this package: 24 blocks, all RESULT: PASS (backup cap, emptiness x4, G1 live pulls, candidate jar+config, placed + bak pull-backs, boot log scan, Enabling x1, audit watch ON x1, READY x1, SEVERE 0, guild state unchanged, INSTALLED marker).
