# excelemfixes-0.21.0-20261125 — FINGERPRINTS

| Artifact | Bytes | SHA-256 | MD5 |
|---|---|---|---|
| ExcelEMFixes-0.21.0.jar (installed; NOT in git, hash-referenced here) | 552,041 | 4ecf2709ee41f08b2517184621c2b349ec2dbb21aaca5834d3b36e407fd5700a | c21d6fe6e88d019d99058d5e0fd58f3c |
| deploy-config.yml (placed as plugins/ExcelEMFixes/config.yml) | 81,482 | 949ff6d2423938eb93158cac667ae15a7f9186e4567bbab0a6298cd88342dc0f | 7f0616ee2f962871ec626963461905b4 |

Gates verified at install (INSTALLED.txt): live ExcelEMFixes-0.20.0.jar 543,319 B / a4999341b2238d2f26a827a411e0e2d19cdba24ee29daa64e21cc5b685697cd2 — PASS; live config 80,048 B / 40f3ed445c43ec3dff32bbc51677fe1144d7ea580d8f39075242df53adf8839f — PASS.

Outgoing 0.20.0 jar moved to ExcelEMFixes-0.20.0.jar.bak-20261125-pre0210; config backup config.yml.bak-20261125-pre0210. Window backup snapshot-11252026.1916.

Tests: 1,843 passed / 0 failed on clean runs of the final tree. One intermittent single failure can appear — the documented pre-existing skill-nudge tie-break flake ("armor named first on the tie", salted Map.of iteration in untouched code), reproduced on the pristine 0.20.0 tree as well, unrelated to this build's change.
