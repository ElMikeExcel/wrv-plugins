# RECEIPTS — P-SHOP-JAVA-GUI-CHARMS-20261124 (ExcelMenu travel menu charms, TEXT live-edit)

## Live-premise receipts (same-day pulls, Homebase route, Nov 24, 2026)

- Live `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml` — pull:
  `~/workspace/shop-java-charms-fix-20261124/pulls/live-spawnshop_travel_animals.yml`
  — 3,377 B — SHA-256 `b694f26bcd4603a84aedfa126df60465385e2f87887c3540e1eb214cac4aebae`
  — pulled Nov 24, 2026, 22:31:50 UTC (4:31 PM CT), Homebase (PC) route.
  Content matches the Lane B bot-observed Java Travel & Animals window item-for-item
  (saddle 0, name tag 1, leads 2, pearls 3, rockets 4, buildflight 5, balance 6,
  back 7, empty map 8; size 9, dispenser type) — the filing premise is CONFIRMED:
  this ExcelMenu file is the serving layer for the Java category window and it
  carries neither charm.
- Live `plugins/ExcelMenu/menus/spawnshop_potions_protection.yml` (wiring reference
  for the ID 7 Strong Healing Potion special entry — slot 7, clicks
  `[console] fieldmedic buy %player_name%` on left+right) — pulled to
  `~/workspace/shop-java-charms-fix-20261124/pulls/` in the same session.
- Live configs pulled to `~/workspace/shop-java-charms-fix-20261124/rig-pulls/`:
  ExcelEMFixes config.yml SHA-256 `0c694bfa…` (summon-mount section present, both
  charms priced); ExcelShops config.yml SHA-256 `e3714dd3…` (byte-identical to the
  mounts lane's placed config-both; special-products carry mount_charm ID 5 and
  ghast_charm ID 9); ExcelItems config.yml SHA-256 `665fe39f…`; ExcelShops
  spawn-catalog.yml SHA-256 `378421ae…`.
- Live jars pulled (sizes match the live plugin listing exactly): ExcelMenu-2.0.0.jar
  337,628 B / `c1f0f411…`; PlaceholderAPI-2.12.3.jar / `fde03259…`;
  ExcelEMFixes-0.12.0.jar 459,581 B / `e7162029…`. ExcelMenu's reload command is
  `/excelmenu reload` (alias `/dm reload`), read from its plugin.yml.

## Mirror-proof receipts

- Rig: `~/workspace/shop-java-charms-fix-20261124/rig/` — Paper 26.1.2 (fleet-verified
  launcher, SHA-256 `8bb8e8ecd227cebe4a54a1ae78b2ce9894812ff8b13e1438b6c6dfc3ad758af1`),
  live plugin set + live configs, staged menu file placed at
  `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml` (hash = staged `f4499be4…`).
- Boot lines (rig latest.log, Nov 24, 2026 ~22:58 UTC): ExcelMenu self-check
  "loaded 13 menus" incl. spawnshop_travel_animals, no parse error on the staged
  size-18 file; `/shop -> spawnshop` registered; "Summon Mount enabled: Mount Charm
  (registry id 5) at $15000.0 shop / $11250.0 vendor NPC"; "Summon Mount ghast tier
  enabled: Ghast Charm (registry id 9) at $50000.0 shop / $37500.0 vendor NPC; quest
  gate on (fraction 0.5)"; census primis=105 quests; ExcelShops "Mobile shop
  descriptions loaded: 56 products".
- Bot proof: `~/workspace/shop-java-charms-fix-20261124/bot/charmbot.log` — Mineflayer
  bot CharmBot, ALL 10 RESULT lines PASS (see BUILD-RECORD.md for the list).
  RCON balance reads: `bal CharmBot` $15,000 before the buy, $0 after — exactly
  $15,000 charged for the Mount Charm; the Ghast Charm gate refusal charged nothing.
- Known rig artifact (does not affect this proof): ExcelShops enters
  "protection-only mode" on copied rigs (session-proof fence, also seen in the
  mounts lane's boots). The charm charge path is ExcelEMFixes' buy flow, not
  ExcelShops checkout, and it executed end-to-end in the proof.
