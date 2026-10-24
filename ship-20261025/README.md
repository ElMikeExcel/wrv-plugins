# SHIP-20261025: INSTALLED LIVE Sat Oct 24, 2026, 11:37 AM CDT (Paper 26.3)

Record of the combined update. Install route: restart with the reason in chat, 1 minute, 10-second countdown.
Boot: 0 SEVERE. Every replaced file was kept as `.bak-20261025` (no existing .bak overwritten).

| piece | live now | replaced |
|---|---|---|
| ExcelWandBars | 0.2.0 (3bac9054...) NEW: `/wandstyle`, Classic default, Spell bars opt-in | - |
| ExcelTrainingWheels | 0.1.4 (4d9ca503...) lifesteal on kill (+2 HP hostile, +4 HP elite); debug left false as found | 0.1.3 |
| ExcelAttunement | 0.1.6 (441d3f70...) arena locks: glide, fling, hulkleap, lift, blink sealed within r200 of 0,0 in the End while the dragon lives | 0.1.5 |
| ExcelBossTrial | 0.2.7 (7b0392d5...) + staged config (6a1dc981...) | 0.2.6 (live jar sha 191250bd...dd43) |
| ExcelBedrockMenu | 1.5.17 (47efb1e9...) neutral example name in /childproofmenu help | 1.5.16 |
| ExcelMenuJoin | 1.4.2 (877bf195...) compass air tap opens the menu | 1.4.1 |
| ExcelLeaderboard | 1.3.1 (3b2c1f1c...) offline stats cache | 1.3.0 |
| ExcelRaidBuff | 0.1.2-scrubbed (1cee2856...) comments/admin text only | 0.1.2 |
| Magic + ExcelMagicGate stage1-5 spells.yml | Boon active mana 2 -> 1; missile ranks 4/5/5/6/6 | noab/overlay spells |
| Magic + stage1-5 wands.yml | classic set (part B; live had been overlay e97406d0) | overlay |
| /time fix | Essentials disabled-commands `time`; commands.yml `day`/`night` aliases | - |

LIGHTNING SLOT LIVE Sat Oct 24, 2026, 11:53 AM CDT (Mike approved 11:49 AM CDT, original version: Lightning/Smite count
toward the boss share, 3.5 per bolt to the dragon). No-restart file swap of plugins/ExcelMagicGate/stages/stage3-5/spells.yml
(7d43579c / e9e635d2 / 46cc3b14 = lightning-slot/ in the package); backups spells.yml.bak-20261025-L. Live stage is 1, so it
takes effect when Seal 3 falls. ExcelGuide untouched. EliteMobs untouched.

Next restart: NetherNet plan approved (port decision pending).

Contents: MANIFEST.md + SHA256SUMS (package), records/ (per-plugin install records), merge/ (config merges),
evidence/ (rig smoke, BossTrial status 0.2.6 vs 0.2.7, /time check, and the Oct 24 EA 0.1.6 dragon-status restart check),
src/ (sources). Privacy: TW source is the scrubbed tree (same replacements as PR #57, DoorHoldRule shown as
DoorGuardRule; tests 272/272 on the scrubbed tree). BedrockMenu orig/ text is not included. BossTrial source is on PR #60.
