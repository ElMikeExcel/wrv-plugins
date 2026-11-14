# G1 receipts -- ExcelShops 1.3.9 filing (SKELETON, Nov 14, 2026)

Status: FILLED Nov 14, 2026 (board supervisor run 334, from retained pulls in ~/workspace/g1-pulls-bm334/, supervisor re-hash). Skeleton assembled desk-side by board
supervisor run 332. Every row below is filled ONLY from a
same-day live pull at filing time (rclone pull via the PC route;
if the PC route fails its probes -- 3 attempts over ~2 minutes --
a panel File Manager pull labeled FALLBACK (panel)). No live
pull was attempted in run 332 (PC route failing its probes; a
failsafe window in flight). Disk lineage is never a premise.

Packet: ~/workspace/shops-fsync-fix-20261114/INSTALL-PACKET.md

| Slot | Live claim | Pull path | Bytes | SHA-256 | Pull timestamp | Route |
|---|---|---|---|---|---|---|
| L1 | live ExcelShops jar | plugins/ExcelShops-1.3.8.jar | 266,417 | b8a1b8fd9a56f420accdb066a3ae3e9c6989638668cadd2f5497a52117a4adb9 | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L2 | exactly-one-jar census, plugins/ listing | plugins-top.txt census: ExcelShops-1.3.8.jar sole loadable (siblings .bak/.disabled/.rollback) | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L3 | live enable version line in latest.log | logs/latest.log: "[ExcelShops] Enabling ExcelShops v1.3.8" | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |

Filing rule: if any slot cannot be filled, STOP -- do not file.
This file's own size + SHA-256 are cited from the MANIFEST stub
at filing time.
