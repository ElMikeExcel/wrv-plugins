# ExcelVaults 0.1.1, Oct 26 2026

Draft, staged only. NOT live. Base: ExcelVaults 0.1.0 (draft PR #74, jar sha256
afdf1ec20b6b24575de7179b6c3d9e3ca80a458d950afeb00b6fbb232a2796bf; this branch's 0.1.0 source rebuilds to it).
New jar: ExcelVaults-0.1.1.jar, sha256 a1cec45a6541e0eb2ca3073f81f1eff29e17d79b9bd7ad2e6ce3cdff62503a86
(built twice, identical). Restart needed.

## Why
Mike's parity rule, Oct 26 2026, 12:10 AM CDT: "Rule is to always do onto one as you would do the other.
All our plugins should work both bedrock and java."
In 0.1.0 Bedrock players read "right-click" and "Tap a [Reopen] button", but Bedrock cannot click chat
buttons, and "right-click" means nothing on a phone. Java players read "Tap" for a mouse click.

## What Bedrock players notice (phones, tablets, consoles)
* Tapping a locked vault: the line reads "Sneak + tap it, or type /vaults, to reopen it now for $X."
  (Java keeps its "Sneak + right-click" line and the clickable [Reopen now: $X] button.)
* **New: a plain tap on a locked vault opens the reopen window**, the same one sneak + tap opens:
  "Reopen for $X" / "Cancel". Chat says: "A window opened: tap Reopen to pay, or Cancel. Nothing is
  charged until you tap Reopen." This replaces the [Reopen] chat button they could not press.
  * At most once every 30 seconds per player per vault, so tapping the same vault again does not keep
    popping the window (`bedrock-tap-form-cooldown-seconds: 30`).
  * Only when paying is possible right now (price set, ominous seals open, payments on).
  * Turn it off with `bedrock-tap-opens-form: false` (then Bedrock gets the text line only).
* `/vaults` list window footer: "Tap a vault below to reopen it now. Or sneak + tap a locked vault."
* `/vaults help` on Bedrock (5 lines): "Tap a used vault to see how long is left." and "Sneak + tap it
  (or type /vaults) to pay and reopen it now. On a controller: sneak, then press LT."
* Sneak + tap, paying, refunds, cooldowns, prices and the list are unchanged.

## What Java players notice
* `/vaults` list footer: "Click a [Reopen] button, or sneak + right-click a locked vault." (was "Tap a [Reopen]").
  Done in code when the server config still holds the exact 0.1.0 default line, so the live config
  needs no edit. A custom line is left alone.
* Nothing else changes for Java (same chat button, same sneak + right-click window).

## Config (all optional; the live 0.1.0 config.yml needs no edit)
* `bedrock-tap-opens-form: true`, `bedrock-tap-form-cooldown-seconds: 30`.
* `messages.hint-pay-bedrock`, `messages.list-footer-bedrock`, `messages.bedrock-form-note`,
  `messages.help-bedrock`. Left out, the built-in text above is used (Bukkit config defaults from the jar).
* `/vaults reload` summary adds "(tap opens reopen window)" when the Bedrock tap window is on.

## Code
* New `core/Texts.java`: Bedrock text table, the 0.1.0 Java footer upgrade, `pick()` / `pickLines()`,
  and `TapGate` (per player and vault cooldown, cleared on quit).
* `ExcelVaults.java`: `msgFor(player, key)` picks the Bedrock text for Floodgate players; `hint()` opens the
  confirm window on a Bedrock plain tap (gated); the list form and help use the platform text.
* plugin.yml 0.1.1 (description says sneak + tap on Bedrock); build.sh outputs ExcelVaults-0.1.1.jar.

## Tests
* Unit tests: 112 pass, 0 fail (`test-results-0.1.1/tests-unit-0.1.1.out`; 0.1.1 block added for Texts,
  TapGate and the footer upgrade).
* Box rig (Paper 26.3 + Geyser + Floodgate), Java bot part 1: 56 pass, 0 fail (new: footer says
  "Click a [Reopen]", help uses right-click wording). Part 2 (restart persistence): 8 pass, 1 "fail" that is
  rig data, not a bug: the boot count read 2 cooldowns instead of 1 because a record from the Oct 25
  Bedrock test was still on the rig. `test-results-0.1.1/rig-test.log`.
* Fake Bedrock player through Geyser: 23 pass, 0 fail (`test-results-0.1.1/bedrock-test.log`): plain tap
  shows the Bedrock line, the note, and the "Reopen for $750 / Cancel" window with no charge; a second tap
  within 30 s gives no window; help and the list footer use tap words; sneak + tap, pay and the list still work.

## Install (panel file manager only)
1. Back up `plugins/ExcelVaults-0.1.0.jar` as `ExcelVaults-0.1.0.jar.bak-20261026`.
2. Upload `ExcelVaults-0.1.1.jar` as `ExcelVaults-0.1.1.jar.uploading`, rename it, check sha256 a1cec45a...
3. Remove the 0.1.0 jar from plugins/ (the .bak stays). Restart (0 players, or the full restart rule).
4. Console: `vaults reload` should show the summary with "(tap opens reopen window)".

Rollback: delete the 0.1.1 jar, rename the .bak back to `ExcelVaults-0.1.0.jar`, restart. Records and
config are the same format, so nothing else changes.

## Still to check in game
* A real phone: tap a used vault, the window opens, Cancel charges nothing, Reopen charges the shown price.
* A controller: sneak + LT opens the window.
* A Java player: the [Reopen now: $X] chat button still works and the footer says "Click".
