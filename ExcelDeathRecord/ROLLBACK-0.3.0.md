# ExcelDeathRecord 0.3.0: rollback

Back to 0.2.0: `ExcelDeathRecord-0.2.0.jar`,
SHA-256 `1c7a6ec8f66a229991526023e4a7c0eeff932e0373fdb8eab3d36c369d1be4a0`.
(0.3.0 is `6c7f3f2cdd17cdd9f27223f6f11ad88d9d0fe5f8566675ca3c6fccb4e90aaa50`.)

1. Server empty. Stop it (or use the normal restart rule).
2. In `plugins/`, rename `ExcelDeathRecord-0.3.0.jar` -> `ExcelDeathRecord-0.3.0.jar.bak-<date>`
   (keep it; do not delete).
3. Rename the old jar back: `ExcelDeathRecord-0.2.0.jar.bak-20261024` -> `ExcelDeathRecord-0.2.0.jar`.
   Check its SHA-256 (above).
4. Leave `plugins/ExcelDeathRecord/` as it is. 0.2.0 reads every file 0.3.0
   wrote: it ignores the `lost-and-found` config keys, the `lostfound/` folder
   and `lostfound.csv`, and it sees the "restored by SELF" marks, so an op
   restore of an item a player already brought back is refused without `force`
   (no duplicates). Tested on the test server (boot clean, records read).
5. Start. Console: `ExcelDeathRecord 0.2.0 enabled — ...`. `/lostfound` is gone.
6. Only if the config itself must go back: copy
   `plugins/ExcelDeathRecord/config.yml.bak-pre-0.3.0` over `config.yml`
   (first rename the current one to `config.yml.bak-030-<date>`). Not needed for 0.2.0 to run.

Full restore of the data folder (only if records were damaged, which no test
showed): stop, rename `plugins/ExcelDeathRecord/` to
`plugins/ExcelDeathRecord.bak-030-<date>/`, copy
`plugins/ExcelDeathRecord.bak-20261024-pre030/` back to
`plugins/ExcelDeathRecord/`, start. Losses and recoveries after the install are
then gone from the ledger (they stay in the renamed folder).

If live was still on 0.1.0: same steps with the 0.1.0 jar's .bak. 0.1.0 reads
0.3.0 records (compat check passes) but does NOT know restore marks, so an op
could restore an item a player already brought back. Check
`/deathrecord burned <player>` lines in the 0.3.0 .bak folder before restoring.

Re-install later: put the 0.3.0 jar back the same way. Player state
(free used, cooldown, tip OK) is kept in `lostfound/` and is read again
(tested: 0.3.0 -> 0.2.0 -> 0.3.0 kept everything).
