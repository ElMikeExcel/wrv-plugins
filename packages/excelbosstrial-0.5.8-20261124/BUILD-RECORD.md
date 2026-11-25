# BUILD RECORD — ExcelBossTrial 0.5.8 (grantclaim emloot form)

Built Nov 24, 2026. Lane: ~/workspace/bosstrial-emloot-20261124/.
Unblocks Mike's Nov 24, 2026 standing rule — item returns go
through the bossrewards chest — for genuine EliteMobs items
(the dagger lane's diagnosis: the 0.5.7.1 build could credit
only vanilla materials and BossTrial seal sets; EM's own
`em loot give` needs the recipient online).

## Base gate (house pattern — PASSED before any edit)

- Source of record: ~/workspace/bosstrial0571-drive/
  (ExcelBossTrial-0.5.7.1-src.zip + jar 472,617 B / sha256
  94cf076da9cbb489fa4ded38cd9b7599b4d72e36724f88017e0540ef7b01f899,
  byte-identical to the live jar file ExcelBossTrial-0.4.1.jar
  per the dagger lane's Nov 24 verification).
- The unmodified extracted tree, packaged with JDK 25
  (~/workspace/.jdk25) and the classpath of record
  (~/workspace/board-r425/bb18-fix-build/work/classpath.txt),
  reproduced the 0.5.7.1 jar BYTE-IDENTICALLY (472,617 B /
  94cf076d…) with the suite at 2,865 checks / 103 harnesses,
  0 failures — exactly the SOURCE.md figures.

## The change (only this)

New grantclaim form: `/bosstrial grantclaim <player> emloot
<loot-file> [count]` (count optional, default 1, bound
1..4096 as the other forms). Four production paths:

1. `core/GrantClaimPlan.java` — pure half: `isEmLootForm`
   (mode word), `parseEmLoot` (bare file name — no path
   separators; count rules identical to the material form),
   `EmLootPlan` / `EmLootResult` / `EmLootError`. Stack
   sizing reuses the existing `split()`.
2. `emloot/EmLootGateway.java` — NEW. The reflective EM seam,
   in EliteSprinkleGateway's shape (EM classloader, seam-dead
   logging, never throws). Construction is EM's own give
   path, bytecode-verified against the shipped EM jar
   (commands/admin/LootCommand.give):
   `CustomItem.getCustomItem(filename)` (null = unknown
   loot), then `generateDefaultsItemStack(player, false,
   null)`. The recipient is passed when online (the exact
   call `em loot give` makes); offline, null is passed —
   EM's constructor null-guards its one player read on this
   path (a permission check), and the authored name, lore,
   enchants, attributes and item identity all come from the
   item's own config.
3. `ExcelBossTrial.java` — dispatch + `handleGrantClaimEmLoot`
   + `refuseEmLoot`. Player resolution, config toggle
   (commands.grantclaim-enabled), audit logging and the
   recipient chat notice are the existing forms' own code
   paths. ALL stacks are built before the store write, and
   the write is the existing `TeamClaims.grantClaim` — the
   ledger stays single-writer, checksum-valid, and the claim
   presents/delivers like an earned batch. Refusals (EM
   unavailable / unknown loot / bad count / bad file word /
   mid-build failure) store nothing. Usage strings extended
   with the new form (command handler + plugin.yml).
4. Stamps: plugin.yml version 0.5.8 + the 0.5.8 description
   sentence; excel-build.txt; core/BuildInfo.java — build ID
   EXC-BOSSTRIAL-0.5.8-WRV-20261124-01-2c5e05 in all three
   homes from the start (the 0.5.7 lesson).

No config change (no new keys; the form rides the existing
grantclaim toggle). No data/state-format change. Every
existing form's grant/refusal semantics are untouched.

## Tests

- Full suite: 2,890 checks / 104 harnesses, 0 failures
  (base 2,865 / 103; delta = the new 24-check
  GrantClaimEmLootPlanTest + 1 PluginYmlParseTest 0.5.8
  sentence check; BuildInfoStampTest passes on the new
  stamp).
- Deterministic: package.sh run twice — both jars
  byte-identical, 481,720 B / sha256
  119ecbad189376e12f235c6c9c2a381033579ce0dfbc0497c4e3aa91c89f995b.
- Jar-entry diff vs the 0.5.7.1 jar: only ExcelBossTrial
  (+ inner classes), core/BuildInfo, core/GrantClaimPlan
  (+ 3 new nested classes), the new emloot package,
  plugin.yml, excel-build.txt.

## Rig proof (scratch rig, FULL level — join + claim)

Own rig in this folder (Paper 26.1.2, EliteMobs 10.9.14 —
the live jar — candidate BossTrial; the shared Homebase
mirror was NOT used; live never touched):

1. Boot with EM: BossTrial enabling line reads Build:
   EXC-BOSSTRIAL-0.5.8-WRV-20261124-01-2c5e05, SEVERE 0.
   (Rig seeding note: a fresh EM registers loose files at
   customitems/ top level; the dagger file was seeded there.
   On live the Primis package registration covers the file —
   the registered key is the bare filename either way.)
2. Console, recipient OFFLINE (never joined):
   `bosstrial grantclaim RigTester emloot
   primis_ice_village_alanus_dagger.yml` → Granted. Refusals
   verified in the same boot: unknown file → UNKNOWN_LOOT;
   count `abc` / `0` → COUNT; `../evil.yml` → LOOT_FILE;
   nothing stored by any refusal. Count form:
   `grantclaim RigTester2 emloot … 2` → one claim, 2 stacks.
   Material form unchanged (`STONE 3` granted).
3. Ledger read-back (server stopped; team-rewards.dat
   parsed, SHA-256 checksum OK): RigTester holds EXACTLY
   ONE claim, state OWED, ONE unit; its paper-item-v1
   payload gzip-decompresses to the full EM item NBT —
   minecraft:stone_sword, custom_name "Alanus' Dagger",
   authored lore lines, attribute modifiers
   elitemobs:elite_damage / elitemobs:elite_speed, and
   custom_data PDC elitemobs:custom_item_id =
   primis_ice_village_alanus_dagger.yml (+ elitemobsitem,
   customlore, elitelevel 5, itemvalue). All three dagger
   payloads in the ledger are byte-identical.
4. Bot join (Mineflayer, offline account RigTester):
   BossTrial auto-presented the claims chest at join; after
   a bot crash + rejoin, /bossrewards showed the claim entry
   ("Reward from console-grant:20261124-171042-1"); click →
   "Reward claimed." and the dagger in the bot inventory
   (stone_sword, custom name "Alanus' Dagger"). Ledger
   re-parse: the claim flipped OWED → DELIVERED; the other
   test claims stayed OWED.
5. Control comparison: with the bot online and op'd, EM's
   own `/em loot give RigTester
   primis_ice_village_alanus_dagger.yml` delivered a second
   dagger ("[EliteMobs] Successfully gave RigTester item
   Alanus' Dagger"). Playerdata NBT comparison of the two
   swords: identical id, count, attribute modifiers,
   custom_name, tooltip display, and every identity PDC —
   with ONE explained difference (below).
6. Boot without EliteMobs: BossTrial enables clean;
   emloot refuses EM_UNAVAILABLE ("EliteMobs is not
   available on this server; nothing was stored."); the
   material form still grants.

### The one difference: soulbind on offline builds

EM applies its Soulbind enchantment during construction
only when a player object is present (SoulbindEnchantment.
addEnchantment(stack, player); EM also re-binds on pickup).
So the EM-given control carries elitemobs:soulbind =
recipient UUID, elitemobs:prestige = 0 and the lore line
"Soulbound to RigTester", while the OFFLINE-built claim
item carries neither and reads "Not Soulbound!". Every
other field — including the unique-identity PDC
elitemobs:custom_item_id — is identical, and when the
recipient is ONLINE at grant time this form passes them to
EM and the build is EM's own give, soulbind included.
EM exposes no offline binding route (every construction
and binding entry point takes a live Player). For the
Jester dagger credit specifically: running the one console
command while Jester is online yields the fully-bound
build; crediting offline yields a genuine but unbound
dagger. The parent/Mike picks; the form supports both.

## Alert-hook caveat (found during read-back — parent item)

The jester-dagger-alert hook's credit guard searches the
Base64-decoded claim payload bytes for "Alanus" — but
paper-item-v1 payloads are GZIP-compressed ItemStack bytes
(verified on this rig: every payload, vanilla and EM
alike), so the plain-text search can never match a real
claim. As built, the hook would stay silent even after a
genuine dagger credit lands. One-line class of fix:
gzip-decompress the decoded payload before searching.
Flagged here and in the dagger RECORD.md; the hook is the
parent's to repair — this lane did not touch it.
RESOLVED Nov 24, 2026: the guard now gzip-decompresses
payloads before searching; verified 9/9 on real-shape
synthetic ledgers plus a read-only live-ledger check
(NONE, correct for today). See the dagger RECORD.md §8.

## Candidate

- ~/workspace/bosstrial-emloot-20261124/ExcelBossTrial-0.5.8.jar
  — 481,720 B / sha256
  119ecbad189376e12f235c6c9c2a381033579ce0dfbc0497c4e3aa91c89f995b
- Source tree of the build: bosstrial/ in this folder
  (+ ExcelBossTrial-0.5.8-src.zip record copy).
- Install packet: INSTALL-PACKET.md in this folder;
  filed PENDING in the MANIFEST Nov 24, 2026.
