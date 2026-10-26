#!/usr/bin/env python3
"""Display + lock pins for ExcelAttunement 0.1.4 (source-level assertions).

0.1.4 (Mike, Oct 24, 2026, after nearly dying with a 0-damage sword in
caster stance) deliberately REVERSES the 0.1.1/0.1.2 "no per-second
action bar" pins: the stance line now carries a live countdown and is
refreshed every second, behind the actionbar / actionbar-live knobs so
the quiet 0.1.2 style is one config line away. The plain-JVM suites
cover the pure core (LockRule, LockFeedback, StanceText); these pins
lock the Bukkit glue:

  (a) tick() refreshes the action bar only behind actionbarLive, and
      sends "Stance clear" (StanceText.CLEAR) on expiry behind actionbar;
  (b) sendActionBar is gated by the actionbar knob and sends
      StanceText.actionBar(stance, remainingSeconds);
  (c) the stance-set chat lines and the stance-fades chat line are
      byte-identical to 0.1.1 (the chat warning stays as it was);
  (d) every lock listener cancels: onHeld, onSwap, onClick, onDrag,
      onPickup each contain setCancelled(true);
  (e) the effect locks (backstops) are intact: onDamageZero still
      setDamage(0.0); MagicBridge.onPreCast still cancels on MARTIAL;
  (f) knobs read with default true: weapon-lock, wand-lock, actionbar,
      actionbar-live, sounds.
"""
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
SRC = ROOT / "src/com/winkreyes/excelattunement"
main = (SRC / "ExcelAttunement.java").read_text()
bridge = (SRC / "MagicBridge.java").read_text()
fails = []


def check(name, ok, detail=""):
    print(("PASS " if ok else "FAIL ") + name + ((" - " + detail) if detail else ""))
    if not ok:
        fails.append(name)


def body(src, sig):
    i = src.index(sig)
    j = src.index("{", i)
    depth = 0
    for k in range(j, len(src)):
        if src[k] == "{":
            depth += 1
        elif src[k] == "}":
            depth -= 1
            if depth == 0:
                return src[j:k + 1]
    raise ValueError(sig)


tick = body(main, "private void tick()")
check("(a) tick() live refresh gated by actionbarLive", "actionbarLive &&" in tick and "sendActionBar(player)" in tick)
check("(a) tick() sends Stance clear on expiry, gated by actionbar", "if (actionbar) player.sendActionBar(Component.text(StanceText.CLEAR" in tick)
sab = body(main, "private void sendActionBar(Player player, boolean bright)")
check("(b) sendActionBar gated by the actionbar knob", "if (!actionbar) return;" in sab)
check("(b) sendActionBar sends the countdown text", "StanceText.actionBar(st, s.remainingSeconds(now))" in sab)
check("(c) martial set chat line unchanged", 'player.sendMessage("§6Martial stance §7- your wand is stilled for " + windowSeconds + "s.");' in main)
check("(c) caster set chat line unchanged", 'player.sendMessage("§bCaster stance §7- your weapons are stilled for " + windowSeconds + "s.");' in main)
check("(c) stance-fades chat line unchanged", 'player.sendMessage("§7Your stance fades - wand and weapon are both free again.");' in tick)
for sig in ["public void onHeld(", "public void onSwap(", "public void onClick(", "public void onDrag(", "public void onPickup("]:
    check("(d) " + sig.split()[-1] + " cancels", "setCancelled(true)" in body(main, sig))
check("(e) caster backstop still zeroes", "event.setDamage(0.0);" in body(main, "public void onDamageZero("))
pre = body(bridge, "public void onPreCast(")
check("(e) martial backstop still cancels the cast", "Stance.MARTIAL" in pre and "event.setCancelled(true);" in pre)
for k in ["weapon-lock", "wand-lock", "actionbar", "actionbar-live", "sounds"]:
    check("(f) knob " + k + " default true", 'getConfig().getBoolean("' + k + '", true)' in main)
print("TOTAL: %d failed" % len(fails))
sys.exit(1 if fails else 0)
