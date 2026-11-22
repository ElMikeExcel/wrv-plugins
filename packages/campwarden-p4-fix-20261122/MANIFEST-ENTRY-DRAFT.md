## PENDING — Camp Warden P4 door-seal content fix (2026-11-22)
- **Packet:** `~/workspace/camp-warden-fix-20261122/INSTALL-PACKET.md`
- **Candidate:** `~/workspace/camp-warden-fix-20261122/primis_bloodtemple_climb_miniboss_1_p4.yml` — 3,485 B / SHA-256 `831f86d3566bc8ed3968acc256bc32e7afa2240d9609210a711953e67be020b6`; content edit only (P4 onRemove 15 -> 30: Door A restore + Door B open entries verbatim from P1); YAML-validated, census 30/0 by script; basis VERIFY-RECORD.md s.4/6 + D188.
- **Live-file gate (STOP on mismatch):** live `plugins/EliteMobs/custombosses/primis/primis_bloodtemple_climb_miniboss_1_p4.yml` = 2,815 B / `7661989fe8c3870838d54bd43b72609b2aa2ef15c88e70c7dafc20ca60cc1f53` — G1 receipt: `~/workspace/camp-warden-fix-20261122/RECEIPTS.md` (fresh Homebase pull 2026-11-22 01:02 AM CT, MATCH).
- **Scope:** one config-file swap, live .bak kept; both Camp Warden arena doors restore at boss death. No jar/data/world change; seal-while-alive unchanged (boss-alive half accepted, D188 option a).
- **Gates:** marker discipline with re-checks before Shut Down and before Start; BACKUP FIRST + cap-at-6; emptiness `list` = 0 twice.
- **Surfaces:** What's New BOTH surfaces owed in the install pass (entry text drafted in the packet); Guide/tips/menus checked in the pass.
- **Marker on install:** `~/workspace/camp-warden-fix-20261122/INSTALLED.txt`
- **Proof owed after install:** next natural Camp Warden kill restores both doors (player report) or bot proof — until then proof-owed, not proven.
