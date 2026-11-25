# BUILD RECORD — P-SHOP-JAVA-GUI-CHARMS-20261124
## Mount Charm + Ghast Charm missing from the Java /shop GUI — ExcelMenu menu fix

- Lane date: Nov 24, 2026. Status: **FILED** as a TEXT (live-edit exemption) PENDING
  entry in `~/workspace/wrv-install-queue/MANIFEST.md`; packet in `packet/`.
  The file was NOT placed live by this lane (per task).

## Premise check (filing's account vs live bytes)
- Live `plugins/ExcelMenu/menus/spawnshop_travel_animals.yml` pulled via the Homebase
  route: 3,377 B, SHA-256 `b694f26bcd4603a84aedfa126df60465385e2f87887c3540e1eb214cac4aebae`
  (`pulls/live-spawnshop_travel_animals.yml`, Nov 24, 2026, 22:31:50 UTC).
- Its contents match the Lane B bot-observed Java Travel & Animals window
  item-for-item (saddle 0, name tag 1, leads ×2 slot 2, pearls ×16 slot 3,
  rockets ×16 slot 4, Build Flight Pass slot 5 via `[console] buildfly buy
  %player_name%`, balance slot 6, back slot 7, Empty Map slot 8) — **premise
  CONFIRMED, no discrepancy**: Java category windows render from ExcelMenu's menu
  files, and the Nov 24 mounts install (DeluxeMenus + MobileShop surfaces) never
  updated this one.
- Wiring reference: the live potions menu's ID 7 Strong Healing Potion entry
  (slot 7; lore with price line, `%vault_eco_balance_2dp%` balance line,
  guild-vendor line, "Tap to review 1. No charge yet."; clicks left+right
  `[console] fieldmedic buy %player_name%`). The charm entries mirror it exactly,
  routing through the ExcelEMFixes buy commands (`mountcharm buy` /
  `ghastcharm buy %player_name%`) so review/confirm and the D204 quest gate —
  enforced inside the buy path — apply unchanged.

## Staged file
- `staged/spawnshop_travel_animals.yml` (+ identical copy in `packet/`):
  4,993 B, SHA-256 `f4499be4b4de64afa9c961bc23822460f2f63bd12e058b7987490d8eafc4433f`.
- Changes vs live (diff-verified, nothing else): header comment; `size: 9` → `18`
  and the `type: dispenser` line removed — the live file is a FULL 9-slot
  dispenser, so no free slots existed; chest-by-omission at size 18 is the proven
  pattern of live `spawnshop_cosmetics_colors.yml` (size 36). Slots 0–8
  byte-identical.
- **Slot 9 — mount_charm**: ECHO_SHARD, name/lore from the mounts lane's staged
  DeluxeMenus block (incl. "Price: $15,000" + guild-vendor $11,250 line),
  clicks left+right `[console] mountcharm buy %player_name%`.
- **Slot 10 — ghast_charm**: GHAST_TEAR, $50,000 lore + $37,500 vendor line +
  quest-gate lore lines, clicks left+right `[console] ghastcharm buy %player_name%`.
- YAML parses; slots unique.

## Mirror proof (Paper 26.1.2 rig, live plugin set + live configs, staged file placed)
- Boot: ExcelMenu self-check "loaded 13 menus" incl. spawnshop_travel_animals with
  NO parse error on the staged size-18 file; `/shop -> spawnshop` registered;
  Summon Mount lines for ID 5 ($15,000/$11,250) and ID 9 ($50,000/$37,500, quest
  gate on, fraction 0.5), census primis=105 quests; ExcelShops "56 products".
- Bot proof (`bot/charmbot.log`, Mineflayer CharmBot) — **10/10 RESULT PASS**:
  MOUNT_LISTED (slot 9, echo_shard), MOUNT_PRICE ($15,000 in lore), GHAST_LISTED
  (slot 10, ghast_tear), GHAST_PRICE ($50,000), GHAST_GATE_LORE, MOUNT_REVIEW
  ("/mountcharm confirm within 30s"), MOUNT_BOUGHT ("Mount Charm bought for
  $15,000"), MOUNT_ITEM_ID5 (delivered echo_shard's custom_data carries
  `excelitems:id` = 5), GHAST_REFUSAL ("finish at least half the quests… 0 of 105
  quests in primis — 53 needed"), GHAST_NO_ITEM.
- Money exactness (RCON): `bal CharmBot` $15,000 before → $0 after the Mount buy —
  exactly $15,000 charged; the Ghast refusal charged nothing.
- Honest rig notes: (1) ExcelShops enters "protection-only mode" on the copied rig
  (known session-proof fence artifact, also in the mounts lane's boots) — it does
  not touch this proof because the charge path is ExcelEMFixes' buy flow, which
  executed end-to-end. (2) Version detour: the bot's minecraft-data has no data
  for protocol 26.2/26.3, so the rig ran on the fleet-verified Paper 26.1.2
  launcher (SHA-256 `8bb8e8ec…`); ExcelMenu 3.0.3 enabled cleanly there. The menu
  file format is version-independent YAML; the live server's ExcelMenu build is
  the same jar pulled from live. (3) The bot's first run failed on name extraction
  (26.x items carry `components`, nbt is null) — extractor fixed to read
  `customName`/`lore`/`custom_data` from components; that failure and fix are
  visible in `bot/` (charmbot.log run 1 vs run 2).

## Filing
- MANIFEST stub: "PENDING (TEXT — LIVE-EDIT EXEMPTION) — ExcelMenu Travel &
  Animals menu: Mount Charm + Ghast Charm listings (P-SHOP-JAVA-GUI-CHARMS-20261124)".
- Packet: `packet/INSTALL-PACKET.md` (lint 5184 B / max line 110 / non-ASCII 20),
  `packet/RECEIPTS.md`, `packet/spawnshop_travel_animals.yml`, `packet/SHA256SUMS`.
- Gates for the executing lane: live-file hash gate `b694f26b…` (STOP on mismatch),
  in-place `.bak-charms-20261124` before placement, placed-hash verify,
  `excelmenu reload`, reload log-line verify, Iris post-place visibility check.
- PC-side litter from the pull route (harmless, on the PC): `D:\muse-servers\charms-rig-pull.zip`,
  `D:\muse-servers\make-rig-zip.ps1`.
