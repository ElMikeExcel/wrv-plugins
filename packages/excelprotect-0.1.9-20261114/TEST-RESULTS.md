# TEST RESULTS - ExcelProtect 0.1.9 (P-PROTECT-I4-1, test T-I4)

Source of record: BUILD-RECORD.md in this package (desk build + Homebase mirror rig leg, Nov 14, 2026). Live server was never touched by testing; all rig work ran on the Homebase test mirror.

## Base gate
- 0.1.8 sources rebuilt byte-identical to the live-premise jar: ExcelProtect-0.1.8.jar, SHA-256 73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a (fingerprint EXC-PROTECT-0.1.8-WRV-20261108-01-a018d8).
- Base suite on 0.1.8 sources: 97 found / 97 successful / 0 failed.

## Candidate suite
- ExcelProtect-0.1.9.jar: 269208 B, SHA-256 893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626 (fingerprint EXC-PROTECT-0.1.9-WRV-20261114-01-0af02b).
- Two independent builds (A and B) produced the identical hash - deterministic.
- Suite on 0.1.9: 99 found / 99 successful / 0 failed (both builds).

## Discrimination
- The 0.1.9 suite run against the 0.1.8 sources: 99 found / 97 successful / 2 failed.
- The 2 failures are the new T-I4 regression tests (itemBreakWithDamageButNoMaxDamageComponent, itemBreakMaxDurabilityResolutionMatrix), failing on 0.1.8 because ItemEvents.maxDurabilityOf is absent - on 0.1.8 the handler reads getMaxDamage() guarded only by hasDamageValue() and throws IllegalStateException. Both pass on 0.1.9. The tests pin the defect.

## Mirror rig (T-I4) - PASS
- Boot 2026-11-14 11:02:56-11:03:53 CST on the Homebase mirror (JDK 25, paper-26.3-245); server Done (20.161s); rig printed RIGOBS DONE mode=probe and self-shut-down.
- READY line: [ExcelProtect] READY version=0.1.9 dir=plugins\ExcelProtect gp=on vault=on mcmmo=on we=on retention-days=30
- item_break records in items-2026-11-14.jsonl: 2 (file grew 2359 B / 13 lines pre-rig to 3317 B / 15 lines post-rig): (1) synthetic T-I4 probe break - wooden_pickaxe, damage 58, max_durability 59 (material fallback; meta carried damage with no max_damage component - the exact 0.1.8 crash shape, now recorded); (2) wear probe break - leather_helmet, damage 55, max_durability 55.
- "We don't have max_damage" exception lines in boot log: 0. SEVERE lines: 0.
- Evidence: rig-evidence/boot-i4.log (105289 B / 537d598a165c85ad9e0b76a8068ca1a4b13b7286276c2b92bd9c783b54165992), rig-evidence/items-2026-11-14.jsonl (3317 B / acbe9014576c546bb00126d3fcb7087bcff0595f47b78675b3a88e5d3ea0a9e2).
- Mirror restored from snapshot byte-exact: 11218 files / 3525741824 B; Protect jar back to 0.1.8.

T-I4 verdict: PASS.
