# ExcelWandBars + ExcelTips: how the wand tip hands over

Written Oct 24, 2026 by the ExcelTips executor. Note: the ExcelWandBars work
actually lives in `/workspace/sbtn/wb/` (this folder did not exist). Nothing in
`/workspace/sbtn/wb/` was edited by the ExcelTips work. This is a note for
whoever finishes ExcelWandBars 0.2.1.

## The rule

* ExcelTips present AND its `tips.yml` has an enabled `wandstyle` tip:
  ExcelWandBars hands the wand tip to ExcelTips and does NOT run its own TipGate.
* ExcelTips missing, disabled, or `wandstyle` turned off: ExcelWandBars runs its
  own TipGate exactly as today (first 15 wand uses, Bedrock form once per session).
* Never both. Never a hard dependency.

ExcelTips' `wandstyle` entry is set to the same numbers as TipGate:
`max-shows: 15`, `use-gap-seconds: 2`, combat quiet 10 s (server-wide setting),
Bedrock pop-up only at a calm moment, 5 min between pop-ups.

## Code to drop into ExcelWandBars (reflection, no compile dependency)

```java
// ExcelTips bridge: null-safe, no dependency. Main thread only.
private static java.lang.reflect.Method tipsTip, tipsAck;
private static boolean tipsLooked;

private static Class<?> tipsApi() {
    org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin("ExcelTips");
    if (p == null || !p.isEnabled()) return null;
    try { return Class.forName("com.winkreyes.exceltips.ExcelTipsAPI", true, p.getClass().getClassLoader()); }
    catch (Throwable t) { return null; }
}

/** True when ExcelTips owns the wand tip right now. */
static boolean tipsOwnsWandTip() {
    Class<?> c = tipsApi();
    if (c == null) return false;
    try {
        return (Boolean) c.getMethod("available").invoke(null)
            && (Boolean) c.getMethod("hasFeature", String.class).invoke(null, "wandstyle");
    } catch (Throwable t) { return false; }
}

/** One wand use. ExcelTips decides if a tip shows (15-use cap, combat, calm pop-up). */
static void tipsWandUse(org.bukkit.entity.Player p) {
    Class<?> c = tipsApi(); if (c == null) return;
    try { c.getMethod("tip", org.bukkit.entity.Player.class, String.class).invoke(null, p, "wandstyle"); }
    catch (Throwable ignored) {}
}

/** The player picked a style or typed /wandstyle ok: stop the tip for good. */
static void tipsWandAck(org.bukkit.entity.Player p) {
    Class<?> c = tipsApi(); if (c == null) return;
    try { c.getMethod("acknowledge", org.bukkit.entity.Player.class, String.class).invoke(null, p, "wandstyle"); }
    catch (Throwable ignored) {}
}
```

Where to call it:

1. On each wand use, where TipGate's `onUse(...)` would run:
   ```java
   if (tipsOwnsWandTip()) tipsWandUse(player);
   else { /* existing TipGate path, unchanged */ }
   ```
2. When the player picks a style (form button or `/wandstyle classic|bars`)
   and on `/wandstyle ok`: call `tipsWandAck(player)` AND keep marking TipGate as
   done, so the two stay in step if ExcelTips is later removed.
3. Optional: `softdepend: [ExcelTips]` in ExcelWandBars' plugin.yml (only load
   order; the bridge already copes either way).

Method lookups can be cached; `available()` must still be checked per call
(ExcelTips can be reloaded or removed).

## Words

ExcelTips' `tips.yml` `wandstyle` text is copied from TipGate's constants
(L_INTRO, L_CLASSIC, L_SNEAK, L_PICK_BEDROCK, Java line "Type /wandstyle to see
your controls."). When TipGate wording changes, change `tips.yml` too (or
the other way round). ExcelTips adds its own OK line ("Click [OK] to hide this
tip." on Java, "Tap OK on the pop-up, or type /tips ok, to hide this tip." on
Bedrock); `/wandstyle ok` keeps working through `tipsWandAck`.

Heads-up: L_CLASSIC says "right-click ... left-click" for everyone. Phone and
tablet players read "right-click" as "hold"/"tap". Worth a Bedrock wording
("Classic: hold to pick the next spell, tap to cast.") in both places.

## Test it

On a rig with both jars: `/tips status <player>` shows `wandstyle shown N of 15`
after wand uses; picking a style shows `hidden`. Remove ExcelTips, restart:
TipGate's own tip comes back.
