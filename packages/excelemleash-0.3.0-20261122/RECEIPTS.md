# RECEIPTS — ExcelEMLeash 0.3.0 packet (D183)

## G1 live-premise receipts (pulled fresh this run, Homebase scp route)

- Live jar claim: `D:\muse-servers\wrv\plugins\ExcelEMLeash-0.2.0.jar`
  is the live ExcelEMLeash jar.
  Receipt: `receipts/live-ExcelEMLeash-0.2.0.jar` in this folder —
  7,356 B / SHA-256
  `7f72e67259552f1a5d188ad0fcfc1fe7ae95e8a675c8b274f1c7ba96be6fe838`,
  pulled Nov 21, 2026, 21:31 CT (scp from Homebase, live path
  above). Matches the 0.2.0 shipped identity exactly.
- Live config claim: `D:\muse-servers\wrv\plugins\ExcelEMLeash\config.yml`
  is the 0.2.0 config (`leash-removal.enabled: true`, `debug: true`).
  Receipt: `receipts/live-config.yml` in this folder — 988 B /
  SHA-256
  `1b44b7df6626a2bb198d991e75ad392d3f9fdc942ccf610912b0b0b967f5a84a`,
  pulled in the same pass.

## Candidate identity

- `ExcelEMLeash-0.3.0.jar` (this folder): 13,655 B / SHA-256
  `2b04e90f651767047279ec1a53181916bac0e43cd6371f919c6675986356033e`.
  Deterministic: two consecutive builds byte-identical.
- `config.yml` (this folder): 1,819 B / SHA-256
  `65d0f4b2863f8b4ff211c6d51073823184c0a332d4dc8e2e0ab9af108fcb2df5`.
- `SHA256SUMS` in this folder carries both hashes.

## Proof evidence

- Harness: 31/31 PASS (14 LeashRemovalPolicy + 17 ReturnPolicy),
  `work/run-tests.sh`.
- Rig: full suite 10/10 PASS, SEVERE 0 — preserved log
  `rig-runtime-final.log` in this folder (run of Nov 21, 2026,
  ~21:24–21:31 CT). Cases: REG, R1, R2, R3, FAKEPLAYER, A-pull96,
  B-midhit, C-camp, D-death, D-worldleave.
- Build narrative + the rig-found key bug and its fix:
  `BUILD-RECORD.md` in this folder.
