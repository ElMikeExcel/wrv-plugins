# ExcelTips integration notes (notes only, no other plugin was edited)

## ExcelMakersMark 0.1.0
Already wired: it softdepends on ExcelTips, asks `hasFeature("makersmark")`,
calls `tip(player, "makersmark")` on `/mark` and signed crafts, and
`acknowledge(player, "makersmark")` on `/mark ok`. The staged `tips.yml` has a
`makersmark` entry (same words as its own config, `needs-plugin: ExcelMakersMark`).
Tested on the test server: tip shows once through ExcelTips, never doubled,
`/mark ok` hides it in ExcelTips too.
Optional later: it could listen for `ExcelTipsAckEvent` (feature "makersmark")
to set its own `tipOk`, so the [OK] click also counts if ExcelTips is removed.

## ExcelTrash 0.2.0
Nothing needed in ExcelTrash (it has no built-in tip). `tips.yml` has:
- `trash` (0.1.0 wording, close deletes) with `unless-plugin: 'ExcelTrash>=0.2.0'`
- `trash-bin` with `needs-plugin: 'ExcelTrash>=0.2.0'`, using the exact words
  from ExcelTrash 0.2.0's TIPS-NOTE.md (red side, OK throws away, Recover last
  trash or /trash undo within 5 minutes). Triggers: `TrashOpenEvent`
  (player-path `getPlayer`), `/trash`, `/bin`, and the `TrashHolder` screen.
So the right tip is on whichever version is installed, with no edit needed
on install day. Tested with 0.2.0 on the test server: one tip per open, old
tip hidden, Java says click and Bedrock says tap.

## ExcelWandBars 0.2.1 (wand tip)
ExcelWandBars 0.2.1 ships its own wand tip (first 15 wand uses, until OK) and
does not call ExcelTips. ExcelTips therefore has NO wand tip (`wandstyle` and
`magic-wand` were removed before release). Tested: using a real Magic wand
shows only the ExcelWandBars tip, and `/tips trigger <player> wandstyle`
finds nothing. If a later ExcelWandBars hands its wand tip over, it should
`ExcelTipsAPI.register(this, "wandstyle", ...)` itself and stop its own
TipGate in the same release. Do not add a `tips.yml` entry for it.

## ExcelWandBars 0.2.2 (Spell Book screen and progress line)
Its `SpellTipHook` registers `spellscreen` with ExcelTips when ExcelTips is
running and then skips its own copy, so there is one tip either way. Nothing
to add to `tips.yml`. Not tested together yet (0.2.2 is still on its own
test rig).

## Lost & Found (ExcelDeathRecord 0.3.0)
Ships its own first-use tip. No `tips.yml` entry, so no duplicate. Optional
hook if it ever wants ExcelTips to show it instead: `register(this,
"lostfound", ...)` plus `tip(player, "lostfound")` on each open, and drop
its own copy in the same release.

## ExcelWandBars note from earlier
The ExcelWandBars staging notes (TIPS-INTEGRATION.md, kept off the repo) describes a
hand-over that ExcelWandBars 0.2.1 did not build. It is out of date; see the
update at its end.

## Any plugin
```java
// softdepend: [ExcelTips]
if (getServer().getPluginManager().isPluginEnabled("ExcelTips")) {
    ExcelTipsAPI.register(this, "myfeature", "My feature", List.of("Java line"), List.of("Bedrock line"), -1);
}
// on each use:
ExcelTipsAPI.tip(player, "myfeature");
```
Reflection works too (see TIPS-INTEGRATION.md) if you do not want a compile
dependency. Rules for tip words: short, kid-friendly, no long dashes, no names,
only real commands, Bedrock text says "tap" not "click", and any seal/boss tip
only says the fight exists and where it is.
