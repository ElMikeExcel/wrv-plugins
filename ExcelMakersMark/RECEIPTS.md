# G1 receipts -- ExcelMakersMark 0.1.6 filing (SKELETON, Nov 14, 2026)

Status: FILLED Nov 14, 2026 (board supervisor run 334, from retained pulls in ~/workspace/g1-pulls-bm334/, supervisor re-hash). Skeleton assembled desk-side by board
supervisor run 332. Every row below is filled ONLY from a
same-day live pull at filing time (rclone pull via the PC route;
if the PC route fails its probes -- 3 attempts over ~2 minutes --
a panel File Manager pull labeled FALLBACK (panel)). No live
pull was attempted in run 332 (PC route failing its probes; a
failsafe window in flight). Disk lineage is never a premise.
The live jar filename in particular must come from the pull:
lineage filenames in older records differ.

Packet: ~/workspace/makersmark-fsync-fix-20261114/INSTALL-PACKET.md

| Slot | Live claim | Pull path | Bytes | SHA-256 | Pull timestamp | Route |
|---|---|---|---|---|---|---|
| L1 | live ExcelMakersMark jar (content 0.1.5-lookup-prefilter-OFFLINE per boot list) | plugins/ExcelMakersMark-0.1.1.jar | 208,757 | 5cb6a71771f1bde24eafa5d7ac8ce7a4f046de4da79445012ed6db3ef1104319 | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L2 | exactly-one-jar census, plugins/ listing | plugins-top.txt census: ExcelMakersMark-0.1.1.jar sole loadable (siblings .bak) | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L3 | plugins/ExcelMakersMark/history/history.mmh2 present | plugins/ExcelMakersMark/history/history.mmh2 | 69,216 | a7e4463c99cfd465eacb2e26cf05de9f8ac95d589fb9a786211f5d89871bd745 | 2026-11-14 04:35-04:36 CST | PC route (rclone) |

Filing rule: if any slot cannot be filled, STOP -- do not file.
This file's own size + SHA-256 are cited from the MANIFEST stub
at filing time.
