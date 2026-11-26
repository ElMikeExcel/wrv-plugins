# excelemfixes-0.22.0-20261125 — RIG RESULTS (summary; full record in BUILD-RECORD.md)

Rig: Paper 26.3 + EliteMobs 10.9.10 / internal 10.9.14. Packet steps performed on the rig first; migration hashes MATCH ×5. Method note (from BUILD-RECORD): NMS fake players + real CustomQuest objects + real QuestCompleteEvents (the house recipe proven for ExcelEMQuestXP 1.0.1/1.1.0), every assertion read from EliteMobs' own PlayerData / player_data.db.

- Phase A — 33/0: module present (24 modules); no-double-pay for both already-paid fixture players (console backpay paid 0, totals + audit lines unchanged); fresh completion paid exactly 20 → highest + 6 Armor (target=highest); pin set as a non-op player persisted; invalid skill refused; pinned completion paid 20 → Wands + 6 (target=pinned:WANDS); owed backpay paid exactly 80 → pinned Wands + 26 Armor, BACKPAY audit line, ledger write-through on disk, re-run paid 0; menu + bare /questxp dispatched true.
- Phase B — 12/0: restart persistence (totals + pin), pinned routing continues (480 → Wands, +160 Armor), unpin restores HIGHEST (50 → Wands as highest, +16 Armor).
- Phase C — 6/0: toggle off → module absent, /questxp nonexistent, a real fired completion granted nothing, other 23 modules enabled.
- Boot SEVERE 0 in all phases. Rig ready line: "quests in table: 112; total XP across table: 130355; backpay players: 5; backpay already paid: 3" (rig fixture counts; live read 46 paid at install).
