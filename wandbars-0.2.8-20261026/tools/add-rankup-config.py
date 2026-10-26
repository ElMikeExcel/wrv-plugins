#!/usr/bin/env python3
"""Add the 0.2.8 rank-up sections to a downloaded live plugins/ExcelWandBars/config.yml.
Usage: add-rankup-config.py LIVE_IN.yml OUT.yml
Appends `rank-up-casting:` and `rank-names:` exactly as in src/config.yml (rank-up ON, broadcast ON,
800/1600/1800, no seal cap, Master name). Refuses if either top-level key already exists.
Everything already in the live file is kept byte for byte (append only)."""
import sys, re, os
src = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'config.yml')
live = open(sys.argv[1], encoding='utf-8').read()
for k in ('rank-up-casting', 'rank-names'):
    if re.search(r'(?m)^' + k + r':', live):
        sys.exit('REFUSE: live config already has ' + k + ': (edit by hand)')
s = open(src, encoding='utf-8').read()
def block(key):
    m = re.search(r'(?ms)^' + key + r':.*?(?=^\S)', s + '\nEND:')
    return m.group(0).rstrip() + '\n'
# keep the comment lines right above each key
def with_comments(key):
    lines = s.split('\n'); i = next(n for n, l in enumerate(lines) if l.startswith(key + ':'))
    j = i
    while j > 0 and lines[j - 1].startswith('#'): j -= 1
    return '\n'.join(lines[j:i]) + ('\n' if j < i else '') + block(key)
add = '\n# ---- added for ExcelWandBars 0.2.8 (rank-up by casting), Oct 26, 2026 ----\n' + with_comments('rank-up-casting') + '\n' + with_comments('rank-names')
out = live if live.endswith('\n') else live + '\n'
out += add
import yaml
d = yaml.safe_load(out)
assert d['rank-up-casting']['enabled'] is True and d['rank-up-casting']['announce']['broadcast'] is True
assert d['rank-up-casting']['sp-to-reach'] == {'student': 800, 'apprentice': 1600, 'master': 1800}
assert d['rank-up-casting']['max-rank-by-stage'] == {} and d['rank-names']['master'] == 'Master'
assert '\u2014' not in add
open(sys.argv[2], 'w', encoding='utf-8').write(out)
print('OK: appended', len(add.splitlines()), 'lines; YAML parses; rank-up ON, broadcast ON, 800/1600/1800, no seal cap, Master')
