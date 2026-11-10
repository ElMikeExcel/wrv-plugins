# ExcelMagicGate 0.3.0 — Rig Verdict (AMENDED build -02)

The unamended build's verdict (demotion seam, step-up, credit feed,
wand lore property seam, auto-upgrade guard — 3 boots, zero SEVERE)
stands for the carried code and is incorporated by reference from
`build-0.3.0/RIG-VERDICT.md`; nothing in the amendment touches those
legs. This file records the AMENDMENT rig: the wand spell-inventory
sync (WandInventorySync), the task's hard gate for state-changing
wand writes.

Rig: Paper 26.2 (port 25599), Magic 11.2.5, ExcelWandBars 0.3.11,
ExcelGuildGate 0.1.0, LuckPerms; stage 4 applied (ceiling master);
amended jar `EXC-MAGICGATE-0.3.0-WRV-20261110-02-d2a310`. Fake
players via the carried FloorRigDriver (unmodified). Earned rows
seeded in earned.yml: RigSyncMaster 2,500 (Master target),
RigSyncAppr 500 (Apprentice target), RigSyncPouch 2,500 (Master
target). Logs: rig/server/boot-amended1.log (boot 1),
boot-amended2.log (boot 2), boot-amended3.log (boot 3, persistence).

## HEADLINE — the inventory sync: PROVEN

### Boot 2 (the load-bearing run; full stage-4 book, 221 spells loaded)

- **RigSyncMaster** (fresh beginner QM wand, Master target):
  stepped beginner → master, then one reconcile granted **46
  spells, verified on the wand** — the missing path spells PLUS
  **archer, bruiser, guardian, revenant, ghast, mount** (the full
  Nov 8 summoning tree) PLUS `tame`. Wand read after: 64 spells,
  path master, 200/35, hotbar 3. No INCOMPLETE line.
- **RigSyncAppr** (Apprentice target): granted 35 spells incl.
  **archer + bruiser** — and NOT guardian, NOT ghast, NOT tame
  (the above-rank negative, live). Wand read: 53 spells, path
  apprentice, 150/25.
- **RigSyncPouch** (Master target): wand parked in the
  pouch-equivalent serialized state BEFORE its first sync; a full
  60 s+ sweep passed with the wand untouched (18 spells, path null,
  reconcile DEFERRED logged). On unpark + activation: stepped
  beginner → master and granted the same 46 spells, verified.
- **Idempotence**: RigSyncMaster re-held after the grant; the next
  sync's status read "in sync (64 path spells held)" and no second
  grant line fired.
- Zero SEVERE, zero sync INCOMPLETE, zero ExcelMagicGate WARN/ERROR
  in boot 2.

### Boot 3 (persistence across restart)

After a clean stop and reboot, RigSyncMaster's bound wand returned
from Magic's OWN saved mage data at master with **64 spells —
archer|2, bruiser|2, guardian, ghast, revenant, mount, tame all
present**. The saved mage file
(`plugins/Magic/data/players/aaaaaaaa-…0001.dat`) was read directly:
the parsed wand block's `spell_inventory` carries all six tree
spells with assigned slots (archer 18, bruiser 25, ghast 34,
guardian 39, mount 48, revenant 55) and the item `custom_data`
representation carries them too — BOTH representations the tame-fix
packet §4 names, written by Magic's own save from the API grants.
(The fake player's Bukkit inventory does not survive a restart —
the carried driver artifact — so the boot-3 reconcile correctly
DEFERRED on the not-live wand; the grants themselves needed no
re-doing. On the live server a returning player's wand is in their
real inventory and the join/activation legs re-check it.)

### Boot 1 (stale-book run — evidence kept, caveat named)

Boot 1 ran against the rig's INHERITED Magic config: the 0.2.0-era
extracted stage templates (142-key book — Magic logged "Loaded 142
spells"), in which the six summoning spells do not exist at all.
Under it, the sync still proved the path-grant mechanism (Master
+41 verified incl. tame; Apprentice +34 with NO tame; pouch
deferral + post-unpark fill +41) and — just as valuably — REFUSED
to count the six nonexistent spells: every sweep logged them
INCOMPLETE by name instead of claiming a grant. The rig was then
repaired (data-folder stage templates re-extracted from the
amended jar; live Magic files replaced with the shipped stage-4
packet) and boots 2–3 ran on the true book. Live consequence, none:
the live server's book is the composed 222-key book where the six
are defined (verified Nov 9–10), and a spell Magic cannot template
is a spell no grant mechanism could land.

### Count note (expected-set authority)

At master, paths.yml TEXT names 65 expected spells; Magic's path
object carries 64. The delta is `disposal`: its book entry is
`enabled: false`, so Magic drops it at path load. The sync reads
the path object (the live authority) and therefore neither grants
nor retries it — matching history (no fixture wand holds disposal).
`wolf` reads as held via the mage's spell set though it sits
outside `spell_inventory` (builtin WolfSpell storage); the held-set
read unions both, so it is never re-granted.

## Carried verdicts (unamended rig, same code)

Demotion seam PROVEN end-to-end (Java + dotted Bedrock name +
pouch-deferred, persisted, WandBars re-read at the shrunken
hotbar); step-up; threshold crossing 99 → 100 live; credit battery;
lore property seam VERIFIED and durable; auto-upgrade guard via
PathUpgradeEvent. Full text: `build-0.3.0/RIG-VERDICT.md`. The
amendment boots re-exercised step-up (three beginner → target
climbs) with zero SEVERE throughout.
