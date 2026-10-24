# Install: ExcelWandBars 0.2.2 (Spell Book screen) + Magic wands.yml 6 rows

STAGED ONLY. Nothing here is installed. NOT in the Oct 25 2 AM batch unless Mike approves it separately. Do it only when Mike says go, with nobody online
(or in the 2 AM whitelisted window). Box SFTP is DOWN (Oct 24, 2026): use the Shockbyte
panel File Manager (Upload / Download / Rename) and Console for every step.

**Order:** install the approved spellbook batch (ExcelWandBars 0.2.1 + EBM 1.5.18 + menus +
Auto-sort) first or in the same window. 0.2.2 replaces the 0.2.1 jar; it contains all of 0.2.1.

## What goes up (hashes in SHA256SUMS; files are under files/plugins/)
| box file | goes to | replaces |
|---|---|---|
| ExcelWandBars-0.2.2.jar (7d550148...) | plugins/ | plugins/ExcelWandBars-0.2.1.jar (or -0.2.0.jar if the 0.2.1 batch is not in) |
| Magic/wands.yml (289539cd...) | plugins/Magic/wands.yml | live file (see check 2) |
| ExcelMagicGate/stages/stage1/wands.yml (289539cd...) | same path | live 1a9accf2... |
| ExcelMagicGate/stages/stage2/wands.yml (5df72455...) | same path | live 3dc1bf99... |
| ExcelMagicGate/stages/stage3/wands.yml (3ddce12b...) | same path | live a8afb0d8... |
| ExcelMagicGate/stages/stage4/wands.yml (e90958b6...) | same path | live e5165a7e... |
| ExcelMagicGate/stages/stage5/wands.yml (d0b459dd...) | same path | live fb5a8896... |

Keep the live plugins/ExcelWandBars/config.yml as is (new sections use built-in defaults).

## Checks first (stop if any fails)
1. plugins/ has exactly one ExcelWandBars jar (0.2.1 after the spellbook batch, else 0.2.0).
2. Download each live wands.yml (Magic + 5 stages) with the panel and `sha256sum` it on the box.
   * Stage files must match the "replaces" column above (the classic chest set, WRV-150 / noab).
   * plugins/Magic/wands.yml must equal ONE of the 5 stage hashes (the seal stage that is live
     now). Upload the NEW file of that same stage as plugins/Magic/wands.yml (stage1 -> 289539cd...,
     which is the copy in files/plugins/Magic/wands.yml; for another stage use that stage's new file).
   * If any hash differs, STOP: someone changed it. Add the one line by hand instead, under
     `base_wand:` next to `mode: chest`: `  inventory_rows: 6` (two spaces).
3. Optional: `cd files && sha256sum -c ../SHA256SUMS` on the box (all OK).

## Backups (never overwrite one)
Name each `<file>.bak-20261024`; if that exists use `-2`, `-3` (check the listing first).
* plugins/ExcelWandBars-0.2.1.jar -> rename to `ExcelWandBars-0.2.1.jar.bak-20261024` (a .bak name does not load).
* plugins/Magic/wands.yml -> copy to `wands.yml.bak-20261024`.
* plugins/ExcelMagicGate/stages/stageN/wands.yml (N = 1..5) -> copy each to `wands.yml.bak-20261024`.
* plugins/ExcelWandBars/config.yml -> copy to `config.yml.bak-20261024`, leave the original.
Keep the box copies of the downloaded live files too.

## Upload, then restart (jar change needs it). Mike's restart rule
1. Upload the 7 files. Only one ExcelWandBars jar may be loadable.
2. Reason in chat: `say Server restart in 1 minute to add the Spell Book screen for wands`.
3. Wait 1 minute, then count down 10 seconds in chat, then restart.
(The restart also loads the new Magic wands.yml. No separate `magic load` needed. If one is ever run
with players online, Bedrock players rejoin after it; GriefPrevention may make them wait up to
a minute before it lets them back in.)

## Expected boot lines
* `[ExcelWandBars] Enabling ExcelWandBars v0.2.2`
* `[ExcelWandBars] ... spell screen ON ..., progress line ON (min gap 4000 ms, skip during stance true)`
* Wand style and wand tip lines as in 0.2.1. No new ERROR / SEVERE lines for ExcelWandBars or Magic.

## After install: proofs (2 minutes)
* Console `wandbars` shows `spell screen ON`.
* Java: sneak + right click with a wand opens a 6-row spell list, spells in their old places,
  first time shows the Spell Book `[Tip]` with `[OK]`. `/spellbook` starts with `Your rank: ...`.
* Bedrock (real device, still owed): crouch + press and hold with a wand on Classic opens the
  Spell Book form (no barrier chest). Tap a spell -> Use this spell -> action bar `Now using: ...`.
  The tip at the top hides after OK. Wand tip / `/wandstyle` text says tap / press and hold, no "click".
* Sneak bars player on Bedrock: press and hold still opens the spell bar.
* With ExcelTips installed: the Spell Book tip comes from ExcelTips once, never twice
  (`wandbars spelltip <player>` shows `exceltips true`).

## Records after install
Big Brain entry, Route Sheet row, daily log, website wording; wrv-plugins draft PR stays a draft.
