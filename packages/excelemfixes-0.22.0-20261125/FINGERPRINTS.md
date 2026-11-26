# excelemfixes-0.22.0-20261125 — FINGERPRINTS

| Artifact | Bytes | SHA-256 | MD5 |
|---|---|---|---|
| ExcelEMFixes-0.22.0.jar (installed; NOT in git, hash-referenced here) | 596,100 | 49eae192df49fe029c7528e76769552d4faacc9bfedb3ce76a716e213aca9b04 | 9f30dbdf88ca92a12c6b8e55c0174570 |
| deploy-config.yml (placed as plugins/ExcelEMFixes/config.yml) | 84,251 | e8b670092ca423e0fd0e5ae7d555ea55729de89dfb3efe592b04de96bbe17086 | fb9bfe5903cf3c4218d9e0b0ea078bf5 |

Gates verified at install (packet INSTALL-PACKET.md + failsafe RUN-RECORD 20261125-2042): live ExcelEMFixes-0.21.0.jar 552,041 B / 4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a — PASS; live config 81,482 B / 949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f — PASS; standalone ExcelEMQuestXP-1.1.0.jar 50,603 B / cab1574471c50ace591bc12603afbd42272c06aec8c4cc78a3b660ae112b670e present as required — PASS.

Migration at install: live plugins/ExcelEMQuestXP/ state copied to plugins/ExcelEMFixes/quest-xp/ with per-file SHA-256 MATCH (quest-xp.yml 821e7103…, backpay.yml 24274c55…, backpay-paid.yml 47cab75d…, grants.log b564329b…); targets.yml absent live (no player had ever set a pin — expected absence, proven from grants.log + boot line). Post-boot ledger hash unchanged: backpay-paid.yml 47cab75d… / 5,407 B.

Outgoing 0.21.0 jar moved to ExcelEMFixes-0.21.0.jar.bak-20261125-pre0220; standalone jar moved to ExcelEMQuestXP-1.1.0.jar.bak-20261125-pre0220; config backup config.yml.bak-20261125-pre0220. Window backup snapshot-11252026.2045.

Tests: 1,908 passed / 0 failed (0.21.0 baseline 1,843 + 65 new QuestXpModuleTest checks). Rig: Phase A 33/0, Phase B 12/0, Phase C 6/0, boot SEVERE 0 in all phases (detail in BUILD-RECORD.md + RIG-RESULTS.md).
