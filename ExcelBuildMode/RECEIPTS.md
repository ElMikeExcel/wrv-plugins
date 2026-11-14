# G1 receipts -- ExcelBuildMode 1.3.5 filing (SKELETON, Nov 14, 2026)

Status: FILLED Nov 14, 2026 (board supervisor run 334, from retained pulls in ~/workspace/g1-pulls-bm334/, supervisor re-hash). Skeleton assembled desk-side by board
supervisor run 332. Every row below is filled ONLY from a
same-day live pull at filing time (rclone pull via the PC route;
if the PC route fails its probes -- 3 attempts over ~2 minutes --
a panel File Manager pull labeled FALLBACK (panel)). No live
pull was attempted in run 332 (PC route failing its probes; a
failsafe window in flight). Disk lineage is never a premise.

Packet: ~/workspace/buildmode-fsync-fix-20261114/INSTALL-PACKET.md

| Slot | Live claim | Pull path | Bytes | SHA-256 | Pull timestamp | Route |
|---|---|---|---|---|---|---|
| L1 | live ExcelBuildMode jar | plugins/ExcelBuildMode-1.3.4.jar | 99,073 | 39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3 | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L2 | exactly-one-jar census, plugins/ listing | plugins-top.txt census: ExcelBuildMode-1.3.4.jar sole loadable (all siblings .bak) | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L3 | plugins/ExcelBuildMode/ dirs review-holds + build-return + recovery present | buildmode-top.txt: active-index/, build-return/, recovery/, review-holds/ all present | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |

Filing rule: if any slot cannot be filled, STOP -- do not file.
This file's own size + SHA-256 are cited from the MANIFEST stub
at filing time.
