# RECEIPTS — ExcelCalmBars 0.1.0 live premises

All pulls Nov 24, 2026, ~6:35 PM CST (2026-11-25 00:35 UTC) from live
`D:\muse-servers\wrv\plugins` via the Homebase SSH route
(~/workspace/server-gui-20261121/work/ssh-pc.sh). Local copies in
~/workspace/combat-calm-bars-20261124/live-pull/.

## Absence premise (the live-jar gate)

- Claim: no ExcelCalmBars jar or folder exists on live.
- Receipt: `plugins-listing-clean.txt` — positive listing of the live
  plugins folder, 608 lines, 21,004 B, SHA-256
  2cb77ac01a5fbbb55de30fdd0d42e888b27614bdc356711aa889c29c2cd12c0a;
  `grep -ci calmbars` over it = 0. Live jars present in the same
  listing: EliteMobs-10.9.10.jar, Jobs.jar, mcMMO-2.3.003-WRV-XP-2-
  ExcelShare-1.jar, ExcelDeathProtection-0.2.2.jar, CMILib.jar,
  ExcelItems-0.1.0.jar, Vault-2.0.0.jar.

## Live configs the bar patterns were derived from

- EliteMobs skills.yml (live): 3,059 B, SHA-256
  6142cdb87ec9126049956ad72c453d95337accf4667339bfc093fabb31ab5235 —
  `showXPBar: true`; skillBarTitleFormat
  '&6$skillName &7Lv.$level$xpText'; skillLevelUpTitleFormat
  '&e&l✦ $skillName LEVEL UP! &7→ &eLv.$level &e&l✦'.
- mcMMO experience.yml (live): 20,908 B, SHA-256
  aba562a24694e9b0675e9a72f0dc5c7b6cbb0905db2da32803958ccd105542ef —
  `Experience_Bars.Enable: true`.
- Jobs generalConfig.yml values (BossBar Enabled, DefaultState
  Batched, Timer 5) and the Jobs live jar receipt (1,077,157 B,
  SHA-256 ab0c1605…, v5.2.6.3) are held by the companion trim lane:
  ~/workspace/jobs-bossbar-timer-20261124/ (the trim itself is
  INSTALLED as a TEXT live-edit, Nov 24, 2026).

## Live jars pulled for pattern/bytecode derivation

- mcMMO-live.jar: 3,711,102 B, SHA-256
  5821026cecf5c5484fad4a5a10b9a01b9f2c7171fcdb17004072ada675efe2d6
- ExcelDeathProtection-live.jar: 62,054 B, SHA-256
  aefc05d1b44e125aad3a994f14e6fe90456aff6e146200b2b884784cbf2b7561
  (0.2.2 — its refreshPresentation bytecode is the (d) evidence that
  the standing bar belongs to the BASIC tier; premium shows a potion
  effect instead.)
- CMILib-live.jar: 1,060,388 B, SHA-256
  908f2b85a71cb10d8f00a528faa8ce46018074cbba79862888adda15111f1c17
- ExcelItems-live.jar: 20,214 B, SHA-256
  85dbb9ea001bfe020aa33cb2db37228e3e8b21a4645a91b026c2cef6dc58685d

## Server-version premise

- Live runs Paper 26.3. The bridge's reflection targets were verified
  field-for-field with javap against the 26.3 server jar AND the
  26.1.2 jar (identical ClientboundBossEventPacket shape); the
  behavioral rig ran 26.1.2 (see BUILD-RECORD.md for why).
