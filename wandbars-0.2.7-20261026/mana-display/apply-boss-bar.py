#!/usr/bin/env python3
"""Option B1 (staged, not applied anywhere live): add a mana boss bar to base_wand in a Magic wands.yml.
Usage: python3 apply-boss-bar.py IN.yml OUT.yml
Text edit only (keeps comments): inserts the block right after the 'action_bar:' block of base_wand.
Refuses if base_wand is missing, already has boss_bar, or the action_bar block is not where expected."""
import sys, re
src, dst = sys.argv[1], sys.argv[2]
t = open(src, encoding='utf-8').read()
m = re.search(r'(?m)^base_wand:\n', t)
if not m: sys.exit('no base_wand: block')
end = re.search(r'(?m)^\S', t[m.end():])
body = t[m.end(): m.end() + (end.start() if end else len(t) - m.end())]
if re.search(r'(?m)^  boss_bar:', body): sys.exit('base_wand already has boss_bar')
a = re.search(r'(?m)^  action_bar:\n(?:    .*\n)+', body)
if not a: sys.exit('base_wand action_bar block not found')
block = (
"  # WRV mana number (staged Oct 26 2026, option B1): a thin bar at the top of the screen,\n"
"  # \"Mana 42\" (the bar fill shows how full you are), shown only while the wand is held. Same on Java and Bedrock (Geyser shows\n"
"  # Java boss bars). It does not use the action bar, so it never fights the stance, range,\n"
"  # page or spell lines. Reload with: magic load (no restart).\n"
"  boss_bar:\n"
"    mode: mana\n"
"    bar_title: \"Mana @mana\"\n"
"    bar_color: blue\n"
"    bar_style: solid\n")
pos = m.end() + a.end()
open(dst, 'w', encoding='utf-8').write(t[:pos] + block + t[pos:])
print('ok', dst)
