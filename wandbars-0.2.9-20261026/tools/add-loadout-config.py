#!/usr/bin/env python3
"""Add the 0.2.9 `bar-loadout:` section to a downloaded live plugins/ExcelWandBars/config.yml.
Usage: add-loadout-config.py LIVE_IN.yml OUT.yml [--off]
Appends tools/bar-loadout-block.yml (enabled true, 21 slots, new spell lines on, max 3).
--off writes enabled: false (0.2.8 behaviour, every spell on the bar in pages).
Refuses if bar-loadout: already exists. Everything already in the live file is kept byte for byte (append only)."""
import sys, re, os
here = os.path.dirname(os.path.abspath(__file__))
live = open(sys.argv[1], encoding='utf-8').read()
if re.search(r'(?m)^bar-loadout:', live):
    sys.exit('REFUSE: live config already has bar-loadout: (edit by hand)')
block = open(os.path.join(here, 'bar-loadout-block.yml'), encoding='utf-8').read()
off = '--off' in sys.argv[3:]
if off:
    block = block.replace('  enabled: true\n', '  enabled: false\n', 1)
add = '\n# ---- added for ExcelWandBars 0.2.9 (own spell bar), Oct 26, 2026 ----' + block
out = (live if live.endswith('\n') else live + '\n') + add
import yaml
d = yaml.safe_load(out)
b = d['bar-loadout']
assert b == {'enabled': not off, 'slots': 21, 'new-spell-lines': True, 'max-new-spell-lines': 3}, b
assert '\u2014' not in add and '\u2013' not in add
assert out.startswith(live)
open(sys.argv[2], 'w', encoding='utf-8').write(out)
print('OK: appended', len(add.splitlines()), 'lines; YAML parses; bar-loadout enabled', not off, ', 21 slots, lines on, max 3')
