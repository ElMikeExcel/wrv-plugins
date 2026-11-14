# SOURCE - ExcelProtect 0.1.9 (P-PROTECT-I4-1)

Plugin: ExcelProtect 0.1.9 - item-break fix (test T-I4).
Build folder of record: ~/workspace/protect-i4-fix-20261114/
Deliverable jar (NOT inside this records package, per house rule): ExcelProtect-0.1.9.jar - 269208 B / SHA-256 893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626. The jar itself is NOT in the GitHub records package; the Drive deposit carries it (separate leg). It is referenced here by hash only.
Fingerprint: EXC-PROTECT-0.1.9-WRV-20261114-01-0af02b.
Sources zip: ExcelProtect-0.1.9-src.zip - 176360 B / SHA-256 84103696fd4f520f2a6605f6518f425497cc5f0ebc3f9642e1c785abe700a1a4 (59 entries: 50 source/test files + 9 directory entries).
Source tree: src/ (49 files) + tests/ (1 file, ProtectTest.java) in this package, built by local-build.sh (deterministic double-build, identical hash on both builds).
Delta vs 0.1.8: ItemEvents.maxDurabilityOf guards the Damageable max_damage read with hasMaxDamage() + try/catch and falls back to the material max durability (0 when the material has none), so an item break whose meta carries damage but no max_damage component is recorded instead of throwing IllegalStateException. Pure core resolveMaxDurability is unit-pinned without a server.
Tests: 99/99 on 0.1.9; discrimination 99 found / 97 successful / 2 failed on 0.1.8 sources (the 2 new T-I4 regression tests). See TEST-RESULTS.md and BUILD-RECORD.md.
Mirror rig T-I4: PASS - boot Done, READY version=0.1.9, 2 item_break records (incl. the exact crash shape, recorded with material fallback 59), 0 max_damage exceptions, SEVERE 0; evidence in rig-evidence/. Mirror restored byte-exact.
Install: INSTALLED Nov 14, 2026, 12:43 CST (failsafe window; boot Done 12:43:35 CST; backup "ExcelProtect 0.1.9 pre-install 1114"; placed jar pull-back EXACT 269208 B / 89389288...; config untouched 13164 B / 543da9f9...; 88 plugins; SEVERE 0; guild LOCKED 4 of 5) - see INSTALLED.txt / OUTCOME.txt in this package.
Build record: BUILD-RECORD.md. Packet: INSTALL-PACKET.md. G1 receipts: RECEIPTS.md.
