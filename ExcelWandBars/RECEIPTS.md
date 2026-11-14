# G1 receipts -- ExcelWandBars 0.3.18 filing (FILLED Nov 14, 2026, board supervisor run 334)

Status: FILLED from same-day live pulls (PC route, rclone) retained in
~/workspace/g1-pulls-bm334/; every hash below re-computed supervisor-side
from the retained pulled copy. Pull timestamps 2026-11-14 04:35-04:36 CST.

Packet: ~/workspace/wandbars-fsync-fix-20261114/INSTALL-PACKET.md

| Slot | Live claim | Pull path | Bytes | SHA-256 | Pull timestamp | Route |
|---|---|---|---|---|---|---|
| L1 | live ExcelWandBars jar (content 0.3.17 per boot list + packet gate 949e9cd4) | plugins/ExcelWandBars-0.3.4.jar | 553,827 | 949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L2 | exactly-one-jar census, plugins/ listing | plugins-top.txt census: ExcelWandBars-0.3.4.jar sole loadable (all siblings .bak/.held) | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |
| L3 | live Magic version | logs/latest.log: "Enabling Magic v11.2.5-baa3d91" | n/a | n/a | 2026-11-14 04:35-04:36 CST | PC route (rclone) |

Filing rule: if any slot cannot be filled, STOP -- do not file. All slots filled.
