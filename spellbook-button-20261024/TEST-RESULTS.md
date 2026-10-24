# Test results (box test server /workspace/sbtn-rig, Oct 24, 2026, CDT)

Test server: Paper 26.3-214, Magic (elmakers) 11.2.5, Geyser 2.12.0, Floodgate 2.2.5,
ExcelMenu 0.1.5, ExcelMenuJoin 1.4.2, ExcelPouch 0.1.0 and the other live Excel plugins,
with these staged jars. The live server was not touched.

| run | time (CDT) | result | log |
|---|---|---|---|
| WandBars unit tests | 1:19 PM | 150 passed, 0 failed | tests/wandbars-unit-tests.log |
| BedrockMenu menu tests | 1:25 PM | 141 checks, ALL PASS | tests/ebm-tests.log |
| Reproducible builds | 1:19 PM | both jars rebuilt with the same sha256 | SHA256SUMS |
| Main phase, pass 1 | 1:07 PM | 80 pass, 0 fail | tests/node-v2-main-4.out |
| Restart phase (full stop/start) | 1:15 PM | 10 pass, 0 fail | tests/node-v2-restart-1.out |
| Rollback phase (0.2.0 + EBM 1.5.17 + old menu, AutoSort out) | 1:16 PM | 3 pass, 0 fail, clean boot | tests/node-v2-rollback-1.out |
| New set restored, clean boot | 1:17 PM | AutoSort, WandBars 0.2.1 and EBM 1.5.18 all enabled | |
| Main phase, pass 2 (check twice, fresh players) | 1:17 PM | 80 pass, 0 fail | tests/node-v2-main-5.out |
| 0.2.1 booted with the LIVE 0.2.0 config.yml | 1:23 PM | Classic default, sneak bars Bedrock only, tip on (built-in defaults) | |

What the main phase covers: the Spellbook (compass in hand, alias, Java menu slot 20,
no-wand hint) and the whole Java tip flow (shown, 2 s gap, count, tips.yml, [OK],
frozen). It covers Java Classic, with the Sneak bars refusal and no wand button in the
Java menu. It covers Bedrock (simulated): tip text and one pop-up per session, Classic
default, the chooser, picking Sneak bars, and the 3-bar wand with closed-bar sneak doing
nothing, press 1->2->3->1, hold = one flip, and a 250 ms debounce. It also covers:
* back to Classic, identical to a never-switched player
* the same account on Java, then back on Bedrock
* Magic load
* the 15-use cap
* AutoSort: /sort no clash, toggle, active.txt, the menu button, off = untouched,
  on = chest, barrel, shulker box and ender chest sorted on open and close, another
  player untouched, second viewer skipped, and 8 plugin menus plus the Magic spell list
  untouched

## Limits (be honest about these)
* **Bedrock was simulated** with `/wandbars simbedrock` on a Java test bot. The real
  Geyser form pop-up and real phone taps or sneak button presses are NOT verified. Check
  them on a real Bedrock device after install (see INSTALL.md proofs).
* **Container opens on the test server go through a test-only plugin,** RigOpen
  (tests/rigopen-rig-only, never shipped). Test-bot block clicks don't reach Paper 26.3
  through ViaVersion on the test server, even with op, facing the block or a different
  protocol. RigOpen calls Player.openInventory on the block's own inventory, so AutoSort
  sees the same InventoryOpen and InventoryClose events as a real click. A real player
  opening a chest after install is still the final proof.
* Trash undo and Leaderboard have no Java window (chat or form), so "not touched" is
  trivially true there.
* "quick_cast=null" (never switched) and "quick_cast=false" (switched back) both mean
  quick cast off. They behave the same, and Magic reports matches=true for both.
* The beginner wand has 1 bar, so Sneak bars has nothing to flip until the student or
  apprentice rank (2 bars) or master (3 bars).
* The device-OS filter (touch only) is NOT used. Mike's final design is Bedrock only with
  an explicit choice, and controller players simply stay on Classic.
