# excelemfixes-0.23.0-20261125 — FINGERPRINTS

| Artifact | Bytes | SHA-256 | MD5 |
|---|---|---|---|
| ExcelEMFixes-0.23.0.jar (installed; NOT in git, hash-referenced here) | 609,952 | 48d0be0943f5faddd090d9b735867ef7f1283c143abed9587fcb843119a89d4b | 8847d410eeaf675dbeffc27093b9223c |
| deploy-config.yml (placed as plugins/ExcelEMFixes/config.yml) | 87,117 | daa7b8bde577447fc417523cfe416f8b7d65074acef3cf40081e0046600cd102 | 6e15449f4516aa8593052dc0ecc9b9f1 |

Gates verified at install (packet INSTALL-PACKET.md + failsafe RUN-RECORD 20261125-2146, fresh on PC): live ExcelEMFixes-0.22.0.jar 596,100 B / 49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04 — PASS; live config 84,251 B / e8b670092ca423e0fd0e5ae7d555ea55729de89dfb3efe592b04de96bbe17086 — PASS; standalone ExcelEMWitherSkulls-0.1.0.jar 14,862 B / 9361dd79f9b0dd639d2b0256bfb866f57d3160ebe718d01a80e6abf8ed present as required — PASS; standalone ExcelWitherSkulls config 1,507 B / a0c5a868fe05d38555fd8b1467b053193fcf82d525f15b1c1ec3a7c81a10da46, values enabled true / target-chance-percent 25.0 / debug-log false — PASS, and diffed identical to the staged module config values.

Outgoing 0.22.0 jar moved to ExcelEMFixes-0.22.0.jar.bak-20261125-pre0230; standalone jar moved to ExcelEMWitherSkulls-0.1.0.jar.bak-20261125-pre0230 (moved out BEFORE the boot — never both jars at a boot); config backup config.yml.bak-20261125-pre0230. plugins/ExcelWitherSkulls/ folder left in place as inert history per packet. Window backup snapshot-11252026.2148. File backups also at D:\muse-servers\tools\failsafe-20261125-2146\backup\.

Tests: 1,972 passed / 0 failed (0.22.0 baseline 1,908 + 64 new: the standalone's SkullMath / SkullSettings / TopUpDecider suites ported verbatim + 5 plugin.yml/config guard checks). Deterministic jar: repeated builds byte-identical. Rig: Phase A (100% leg ELITE 40 → 40 skulls, 38 module-added + 2 vanilla; 0% leg 0 module adds; TAGGER-MISMATCH 0 / MULTI-SKULL 0) + Phase B (module off → 24 modules, ELITE 40 → 0 skulls), boot SEVERE 0 in both phases (detail in BUILD-RECORD.md + RIG-RESULTS.md). Known flake: SkillNudgeMathTest tie check intermittent (2 of 4 runs) — pre-existing, documented in the 0.22.0 record, in a module this fold does not touch.
