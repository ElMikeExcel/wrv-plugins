# ExcelProtect 0.1.9 (P-PROTECT-I4-1) - RECEIPTS (G1 live-premise receipts)

Worker run 340 - Sat Nov 14, 2026. Filing prep only; no filing, no
install, no live file changed by this prep.

## Homebase probe

- Invocation: ssh -F ~/.ssh/config mike-pc (Tailscale relay path).
- First combined probe command failed on the remote shell's bare
  `hostname` invocation (Windows quirk), not on the connection.
- Direct probes immediately after: `echo PROBE_OK` PASS and
  `cmd /c hostname` returned DESKTOP-IL05DQR, both exit 0, at
  2026-11-14 ~11:11 CST. Route UP on the retry pattern; the PC's own
  state was never in doubt and no fallback was used or needed.
- Last successful probe: 2026-11-14 11:16-11:17 CST (surfaces pulls).

## Receipt 1 - live jar (the gate premise)

- Remote: shockbyte:1. Michael's Server/plugins/ExcelProtect-0.1.8.jar
  (rclone copyto on Homebase to the PC staging folder, then scp to
  the desk; hashed on both sides, identical).
- Local: ~/workspace/protect-i4-fix-20261114/live-pull/live-018.jar
- Size: 268,894 B
- SHA-256: 73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a
- Pulled: 2026-11-14 11:13 CST (PC clock 11:13:31 -06:00)
- Matches the task's expected gate EXACTLY. Gate holds.

## Receipt 2 - live config (premise, re-based this run)

- Remote: shockbyte:1. Michael's Server/plugins/ExcelProtect/config.yml
  (same rclone + scp path as Receipt 1).
- Local: ~/workspace/protect-i4-fix-20261114/live-pull/live-config.yml
- Size: 13,164 B
- SHA-256: 543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51
- Pulled: 2026-11-14 11:13 CST.
- RE-BASE NOTE: the task brief expected the pre-refresh config
  (7,991 B / 7a423378...). That premise is stale: the ExcelProtect
  config-refresh entry LANDED today (MANIFEST-ARCHIVE record: failsafe
  run 10:33 CST, boot 10:53 CST) and placed exactly these bytes
  (13,164 B / 543da9f9...). The fresh pull therefore shows the
  refreshed config, byte-identical to the shipped config text and to
  the config.yml desk copy in this build folder. This packet makes NO
  gate on the config (the swap never touches it); the value is
  recorded so the window can prove config-untouched after the swap.

## Receipt 3 - exactly-one-jar census of live plugins/

- Method: rclone ls "shockbyte:1. Michael's Server/plugins"
  --max-depth 1, 2026-11-14 11:13 CST: 383 top-level entries.
- ExcelProtect lines: 9 total. Exactly ONE active jar:
  ExcelProtect-0.1.8.jar (268,894 B). The other 8 are inert:
  0.1.0-0.1.5 as .jar.held-*, 0.1.6 as .jar.bak-20261106-pre017,
  0.1.7 as .jar.bak-20261108-prebosspair. No 0.1.9 jar live.

## Receipt 4 - live boot baseline (plugins/logs/latest.log pull)

- Remote: shockbyte:1. Michael's Server/logs/latest.log (rclone +
  scp, same window as Receipts 1-2).
- Local: ~/workspace/protect-i4-fix-20261114/live-pull/latest.log
- Size: 119,254 B
- SHA-256: cfb6cc29c5a6f668d602f488269262aea94f1e9734d5847f7dcbb1fdac344f0c
- Boot shown is the config-refresh boot (Done (23.156s)!):
  Initialized 88 plugins / Bukkit plugins (88);
  ExcelProtect READY version=0.1.8, retention-days=30;
  ExcelGuildGate: 4 of 5 seals broken, guild LOCKED;
  SEVERE count 0; "max_damage" count 0 (verify_counts, below).

## Receipt 5 - surfaces pulls (same Homebase route, 11:16-11:17 CST)

Local copies in ~/workspace/protect-i4-fix-20261114/live-pull/:
- knowledge-live.yml (plugins/ExcelGuide/knowledge.yml) 645,669 B /
  e640d00b78b93d7c16a0274ae0099f2651b522eea4ae9c81699aadc2dc0234c2
- intents-live.yml (plugins/ExcelGuide/intents.yml) 103,006 B /
  2006081fc70fb6df04995ea5e657637523048ce4582a3fccd413868d84a69c56
- changelog-live.yml (plugins/ExcelWhatsNew/changelog.yml) 92,810 B /
  3292df8e4f38769e877939505051bec74ad6f48f268ddd3afe600285009f88ec
- tips-live.yml (plugins/ExcelTips/tips.yml) 18,286 B /
  5ef8bc6f4c53676dd0ff73b921fc2c8db3a576df0137d2a5519783629bf7ebeb
- menu-lostfound-live.yml (plugins/ExcelMenu/entries/lostfound.yml)
  859 B / b35169eb3ac86235ab09f485c1480493bba62c54a21736953f732b82cb5bcbde
- menu-yilostfound-live.yml (entries/yi_lostfound.yml) 586 B /
  f0ae483adf9a0aa3af1ddd6d8c47f1c0a5ab10b22a3df321f8aacdd880058c44
Counts behind the packet's surfaces verdicts (verify_counts.py):
knowledge ExcelProtect x7 (1 version claim: the All-plugins row
"ExcelProtect Created by us 0.1.8 installed November 8, 2026"; 1 dated
historical news mention "(ExcelProtect 0.1.8)"; rest version-free
"logged to ExcelProtect"); intents ExcelProtect x2, version claims 0;
changelog ExcelProtect x2, version claims 0; tips ExcelProtect x0;
menu lostfound entries ExcelProtect x0, version claims 0 (the one
"0.1.8" string in yi_lostfound.yml is an ExcelMenu provenance
comment). Website desk copies: website-after-fifth-seal-20261114
SECTION.md + BRIEF.md carry 0 ExcelProtect mentions; site-ai
ai-knowledge.json carries the same mirrored knowledge row (1 x
"ExcelProtect 0.1.8"), discharged by the Guide knowledge leg.

## Desk-side script verification (this run, verbatim blocks)

STEP: live-jar-gate
EXPECTED: size=268894 sha256=73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a
ACTUAL: size=268894 sha256=73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a
RESULT: PASS

STEP: live-config-premise
EXPECTED: size=13164 sha256=543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51
ACTUAL: size=13164 sha256=543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51
RESULT: PASS

STEP: candidate
EXPECTED: size=269208 sha256=893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626
ACTUAL: size=269208 sha256=893892889318ae2d5ffc08846a55b92d0b83bab4140d9a6f46d9f91f4cf6b626
RESULT: PASS

STEP: g1-live-jar
EXPECTED: path=plugins/ExcelProtect-0.1.8.jar size=268894 sha256=73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a
ACTUAL: path=plugins/ExcelProtect-0.1.8.jar size=268894 sha256=73373b90f78f7789575535b0d2de59847011e52868309dfee7d480908e99719a lane=rclone pulled_at=2026-11-14T17:13:57Z
RESULT: PASS

STEP: g1-live-config
EXPECTED: path=plugins/ExcelProtect/config.yml size=13164 sha256=543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51
ACTUAL: path=plugins/ExcelProtect/config.yml size=13164 sha256=543da9f9cc2a4410a8d4747d531816bb96e580fb754db63fe10fe68558de1f51 lane=rclone pulled_at=2026-11-14T17:13:59Z
RESULT: PASS

STEP: current-boot-baseline
EXPECTED: done=present severe=0 guild_locked=4of5
ACTUAL: done=present severe=0 guild_locked=4of5
RESULT: PASS

STEP: severe-count
EXPECTED: count=0 pattern=SEVERE mode=occurrences
ACTUAL: count=0
RESULT: PASS

STEP: maxdmg-count
EXPECTED: count=0 pattern=max_damage mode=occurrences
ACTUAL: count=0
RESULT: PASS

Scripts: ~/workspace/wrv-install-queue/tools/ (verify_hash.py,
live_pull_check.py, boot_log_scan.py, verify_counts.py), run
2026-11-14 ~11:14-11:20 CST against the pulled bytes above.
