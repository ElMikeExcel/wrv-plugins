#!/usr/bin/env bash
# ExcelBrainGames 0.2.0 (Phase 1 framework + Word Search + Memory Match; Phase 2 adds Sudoku):
# compile, run the full logic suite (WordSearch / Memory / Ledger) and the
# generator property tests against the shipped config + word lists, then
# assemble the jar deterministically. The jar is assembled twice inside the
# script and the SHA-256 hashes must match; run the whole script twice for
# the cross-run reproducibility check (see TEST-REPORT.md).
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"
JDK=/home/hatch/workspace/.jdk25/bin
L="$HOME/workspace/excelbuildmode-pilot/runtime-server/libraries"
API=$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar
CP="$API"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
VAULT=$HOME/workspace/excelprotect/src/ExcelProtect/libs/Vault-2.0.0.jar
echo "8b2ce431c88741fcae6d4de6032ab2ab0cca6aa86e8af375bdfe654321822a68  $VAULT" | sha256sum -c - >/dev/null && echo "Vault build input pinned: 8b2ce431... OK"
echo "f0df9e7faccd38fabcdf4d2e6a243649cabc11f0a19fec100b43d7357369fb22  $API" | sha256sum -c - >/dev/null && echo "Paper API build input pinned: f0df9e7f... OK"
O=$D/work/out; rm -rf $O; mkdir -p $O/classes $O/test
# descriptor gate: plugin.yml / config.yml / word lists that do not parse fail HERE, not at boot
python3 -c "
import yaml, glob
d = yaml.safe_load(open('$D/plugin.yml')); assert d['name'] == 'ExcelBrainGames' and str(d['version']) == '0.2.0', d.get('version')
yaml.safe_load(open('$D/config.yml'))
for f in glob.glob('$D/wordlists/**/*.yml', recursive=True): yaml.safe_load(open(f))
print('descriptors parse: plugin.yml (0.2.0) + config.yml + word lists OK')"
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$VAULT" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
cp -r $D/wordlists $O/classes/
echo "== compiled: $(find $O/classes -name '*.class' | wc -l) classes"
# ---- tests ----
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$VAULT" -d $O/test $(find $D/tests -name '*.java')
run() { echo "-- $1"; shift; "$@" || { echo "TEST FAILED: $1"; exit 1; }; }
TC="$O/test:$O/classes:$CP:$VAULT"
run WordSearchTest $JDK/java -cp "$TC" com.winkreyes.excelbraingames.WordSearchTest
run MemoryTest     $JDK/java -cp "$TC" com.winkreyes.excelbraingames.MemoryTest
run SudokuTest     $JDK/java -cp "$TC" com.winkreyes.excelbraingames.SudokuTest
run SudokuProperty $JDK/java -cp "$TC" com.winkreyes.excelbraingames.SudokuPropertyTest "$D/config.yml"
run LedgerTest     $JDK/java -cp "$TC" com.winkreyes.excelbraingames.LedgerTest
run PropertyTest   $JDK/java -Dbuild="$D" -cp "$TC" com.winkreyes.excelbraingames.PropertyTest
# ---- deterministic jar assembly (built twice, hash-asserted) ----
rm -f "$D/ExcelBrainGames-0.2.0.jar"
python3 - "$O" "$D" <<'PY'
import zipfile, hashlib, os, sys, io
O, D = sys.argv[1], sys.argv[2]
root = os.path.join(O, 'classes')
entries = {}
for dp, _, fn in os.walk(root):
    for f in fn:
        p = os.path.join(dp, f)
        rel = os.path.relpath(p, root)
        entries[rel] = open(p, 'rb').read()
def build():
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as z:
        for n in sorted(entries):
            zi = zipfile.ZipInfo(n, (2026, 11, 4, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, entries[n])
    return buf.getvalue()
b1, b2 = build(), build()
h1, h2 = hashlib.sha256(b1).hexdigest(), hashlib.sha256(b2).hexdigest()
assert h1 == h2, "REBUILD NOT REPRODUCIBLE"
open(os.path.join(D, 'ExcelBrainGames-0.2.0.jar'), 'wb').write(b1)
print(f"ExcelBrainGames-0.2.0.jar: sha256 {h1} ({len(b1)} bytes) - identical across two assemblies")
PY
