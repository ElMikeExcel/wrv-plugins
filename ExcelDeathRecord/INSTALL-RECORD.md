# ExcelDeathRecord 0.2.0: install record

Jar: ExcelDeathRecord-0.2.0.jar
SHA-256: 1c7a6ec8f66a229991526023e4a7c0eeff932e0373fdb8eab3d36c369d1be4a0
Built with ./build.sh (JDK 25, --release 21, against Paper API 26.2 build 133). Deterministic: rebuilds are byte-identical.
Status: built and tested on a scratch Paper 26.2 server. Not installed yet.

## Install (empty server)
1. In plugins/, rename ExcelDeathRecord-0.1.0.jar to ExcelDeathRecord-0.1.0.jar.bak-<date>.
   Only one ExcelDeathRecord jar may be loadable.
2. Upload ExcelDeathRecord-0.2.0.jar and check the SHA-256. Keep plugins/ExcelDeathRecord/ as it is.
3. Restart. The console must show:
   ExcelDeathRecord 0.2.0 enabled — death ledger + Burn Ledger watching; restore ready (menu + typed), bedrock-forms=ready, display-zone=America/Chicago, log=restores.csv

## Restore one item to another player (the recipient must be online)
    deathrecord <player>                         -> note the death number n
    deathrecord <player> <n>                     -> note the item number k
    deathrecord restore <player> <n> <k> to <recipient>
or in game as an op: /deathrecord restore <player>, then follow the menu.

## Rollback
Delete the 0.2.0 jar, restore the .bak, and restart. 0.1.0 reads records that 0.2.0 marked (see run-compat.sh).

## Tests
- ./run-tests.sh: 192 passed, 0 failed (selection, argument parsing, all-or-nothing slot planning, equipment matching, CSV, restore marks, paging, typed hints, XP math, and the 0.1.0 suites).
- ./run-compat.sh <0.1.0 source>: 0.1.0 reads 0.2.0 death and burn records with the marks ignored. PASS.
- Live (Paper 26.2, fake players):
  - The dead player had an enchanted netherite shovel (Efficiency V, Mending, Unbreaking III). It was restored to another player by typed command and by the Java chest menu, with the enchantments intact.
  - A second attempt was refused. force gave it again and the mark showed "2 times".
  - A full inventory was refused with the exact message, and nothing was given or marked.
  - Restore all to the same player put the helmet and shield back into their slots.
  - XP was restored (level 30).
  - A Burn Ledger restore worked.
  - Paging worked: page 2 numbering continues from page 1.
  - An Essentials nickname resolved as the target.
  - restores.csv rows were written.
