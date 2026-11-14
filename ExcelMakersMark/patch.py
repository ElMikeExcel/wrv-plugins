# Excel WRV: deterministic surgical patch on the exact shipped
# ExcelMakersMark 0.1.5-lookup-prefilter-OFFLINE jar (same construction as
# the lineage's build.py): swap the recompiled HistoryLedger classes, bump
# the plugin.yml version string, refresh the carried CHANGELOG.md with the
# tree's updated changelog. Every other entry is byte-identical to shipped.
import sys, zipfile, hashlib
from pathlib import Path

root = Path(__file__).parent
base = root / 'inputs' / 'ExcelMakersMark-0.1.5-lookup-prefilter-OFFLINE.jar'
assert hashlib.sha256(base.read_bytes()).hexdigest() == \
    '5cb6a71771f1bde24eafa5d7ac8ce7a4f046de4da79445012ed6db3ef1104319', 'base jar is not the shipped 0.1.5 candidate'
classes_dir = root / 'patch-classes'
tree = root / 'ExcelMakersMark'

with zipfile.ZipFile(base) as z:
    entries = {n: z.read(n) for n in z.namelist() if not n.endswith('/')}
original = dict(entries)

prefix = 'com/winkreyes/makersmark/history/HistoryLedger'
jar_ledger = {n for n in entries if n.startswith(prefix) and n.endswith('.class')}
built_ledger = {str(p.relative_to(classes_dir)) for p in classes_dir.rglob('HistoryLedger*.class')}
assert jar_ledger == built_ledger, f'ledger class set mismatch: jar-only={jar_ledger-built_ledger} built-only={built_ledger-jar_ledger}'
for n in sorted(jar_ledger):
    entries[n] = (classes_dir / n).read_bytes()

old_ver = b'version: 0.1.5-lookup-prefilter-OFFLINE'
new_ver = b'version: 0.1.6-lookup-prefilter-OFFLINE'
assert entries['plugin.yml'].count(old_ver) == 1
entries['plugin.yml'] = entries['plugin.yml'].replace(old_ver, new_ver)
entries['CHANGELOG.md'] = (tree / 'CHANGELOG.md').read_bytes()

out = Path(sys.argv[1])
with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for n, v in sorted(entries.items()):
        i = zipfile.ZipInfo(n, (2026, 11, 14, 0, 0, 0))
        i.compress_type = zipfile.ZIP_DEFLATED
        i.external_attr = 0o644 << 16
        z.writestr(i, v)

changed = sorted(n for n in entries if entries[n] != original[n])
allowed = sorted(list(jar_ledger) + ['plugin.yml', 'CHANGELOG.md'])
assert set(changed) <= set(allowed), f'changed entries outside the allowed set: {changed} vs {allowed}'
must = {prefix + '.class', 'plugin.yml', 'CHANGELOG.md'}
assert must <= set(changed), f'required changes missing: {must - set(changed)}'
# Inner/record classes may compile byte-identical to shipped (the edit
# touches only the outer class's forceDirectory); that is expected, not a
# failure — they are still the freshly compiled bytes from the fixed source.
print('candidate:', out)
print('sha256:', hashlib.sha256(out.read_bytes()).hexdigest())
print('changed entries (%d):' % len(changed))
for n in changed:
    print('  ', n)
