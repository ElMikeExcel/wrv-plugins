#!/usr/bin/env bash
# ExcelBrainGames 0.3.3: Sudoku visual box view for Bedrock. The board
# is the SAME on both platforms; the presentation keys off the VIEWER's
# platform (Floodgate softdep): Bedrock viewers get a box picker (one
# entry per box, finished boxes marked) and one box at a time as a true
# visual square on the 5-wide phone stream, a context line (full row +
# full column) on a cell tap, and a 3x3 digit-tile picker; unknown
# platform -> the Java full-board view. Java presentation, the
# generator, the checker, tiers, givens, payouts, mistake rules, the
# $1,500/day cap, the packs, Word Search and every other game are
# unchanged, and the save format gains NO new key. Compiles, generates
# + validates the tile packs (twice, zips must be byte-identical — the
# packs are NOT touched by this build), runs the full carried suite
# plus SudokuBoxViewTest, verifies the build fingerprint, then
# assembles the jar deterministically (twice inside, hash-asserted;
# run the whole script twice for the cross-run check — see
# TEST-REPORT.md).
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"
JDK=/home/hatch/workspace/.jdk25/bin
L="$HOME/workspace/excelbuildmode-pilot/runtime-server/libraries"
API=$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar
CP="$API"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
VAULT=$HOME/workspace/excelprotect/src/ExcelProtect/libs/Vault-2.0.0.jar
FG=$D/libs/floodgate.jar
echo "8b2ce431c88741fcae6d4de6032ab2ab0cca6aa86e8af375bdfe654321822a68  $VAULT" | sha256sum -c - >/dev/null && echo "Vault build input pinned: 8b2ce431... OK"
echo "f0df9e7faccd38fabcdf4d2e6a243649cabc11f0a19fec100b43d7357369fb22  $API" | sha256sum -c - >/dev/null && echo "Paper API build input pinned: f0df9e7f... OK"
echo "21570aff9ce17d6983928e8552777760e1ede5050026b04c686b0ae112e6fd7e  $FG" | sha256sum -c - >/dev/null && echo "Floodgate build input pinned: 21570aff... OK"
O=$D/work/out; rm -rf $O; mkdir -p $O/classes $O/test
# descriptor gate: plugin.yml / config.yml / word lists that do not parse fail HERE, not at boot
python3 -c "
import yaml, glob
d = yaml.safe_load(open('$D/plugin.yml')); assert d['name'] == 'ExcelBrainGames' and str(d['version']) == '0.3.3', d.get('version')
assert d.get('softdepend') == ['floodgate'], d.get('softdepend')
c = yaml.safe_load(open('$D/config.yml'))
bt = c['games']['wordsearch'].get('bedrock-tiers') or {}
assert set(bt) == {'kid', 'standard', 'challenge'}, sorted(bt)
for tid, s in bt.items(): assert s['cols'] == 5 and s['rows'] * 5 <= 50, (tid, s)
for f in glob.glob('$D/wordlists/**/*.yml', recursive=True): yaml.safe_load(open(f))
print('descriptors parse: plugin.yml (0.3.3, softdepend floodgate) + config.yml (bedrock-tiers 5-wide) + word lists OK')"
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$VAULT:$FG" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
cp -r $D/wordlists $O/classes/
echo "== compiled: $(find $O/classes -name '*.class' | wc -l) classes"
# ---- tile packs: generate twice (zips must be byte-identical), then validate ----
python3 $D/packs/generate_tiles.py
J1=$(sha256sum $D/packs/dist/braingames-tiles-java.zip | cut -d' ' -f1)
B1=$(sha256sum $D/packs/dist/braingames-tiles-bedrock.mcpack | cut -d' ' -f1)
python3 $D/packs/generate_tiles.py
J2=$(sha256sum $D/packs/dist/braingames-tiles-java.zip | cut -d' ' -f1)
B2=$(sha256sum $D/packs/dist/braingames-tiles-bedrock.mcpack | cut -d' ' -f1)
[ "$J1" = "$J2" ] && [ "$B1" = "$B2" ] && echo "packs reproducible: java $J1 / bedrock $B1" || { echo "PACK REBUILD NOT REPRODUCIBLE"; exit 1; }
python3 $D/packs/validate_packs.py
# ---- tests ----
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$VAULT" -d $O/test $(find $D/tests -name '*.java')
run() { echo "-- $1"; shift; "$@" || { echo "TEST FAILED: $1"; exit 1; }; }
TC="$O/test:$O/classes:$CP:$VAULT"
run WordSearchTest $JDK/java -cp "$TC" com.winkreyes.excelbraingames.WordSearchTest
run MemoryTest     $JDK/java -cp "$TC" com.winkreyes.excelbraingames.MemoryTest
run SudokuTest     $JDK/java -cp "$TC" com.winkreyes.excelbraingames.SudokuTest
run SudokuProperty $JDK/java -cp "$TC" com.winkreyes.excelbraingames.SudokuPropertyTest "$D/config.yml"
run TextTest       $JDK/java -cp "$TC" com.winkreyes.excelbraingames.TextTest
run TextProperty   $JDK/java -cp "$TC" com.winkreyes.excelbraingames.TextPropertyTest "$D/wordlists"
run LedgerTest     $JDK/java -cp "$TC" com.winkreyes.excelbraingames.LedgerTest
run PropertyTest   $JDK/java -Dbuild="$D" -cp "$TC" com.winkreyes.excelbraingames.PropertyTest
run GeometryTest   $JDK/java -cp "$TC" com.winkreyes.excelbraingames.GeometryTest
run SudokuBoxViewTest $JDK/java -cp "$TC" com.winkreyes.excelbraingames.SudokuBoxViewTest
run BedrockPropertyTest $JDK/java -Dbuild="$D" -cp "$TC" com.winkreyes.excelbraingames.BedrockPropertyTest
run TileTest       $JDK/java -Dtiles="$D/packs/dist/tiles-manifest.json" -cp "$TC" com.winkreyes.excelbraingames.TileTest
# ---- build fingerprint (house standard, Layer B): the id's hash6 is the
# first 6 hex of the SHA-256 over the sorted per-file source hashes (the
# carrier file, core/BuildInfo.java, excluded); the BuildInfo constant
# must carry it, and excel-build.txt at the jar root must repeat it ----
SUFFIX=$(cd $D/src && find . -name '*.java' ! -path './com/winkreyes/excelbraingames/core/BuildInfo.java' | sort | xargs sha256sum | sha256sum | cut -c1-6)
BUILD_ID=$(grep 'BUILD_ID =' $D/src/com/winkreyes/excelbraingames/core/BuildInfo.java | grep -o 'EXC-BRAINGAMES-[0-9A-Za-z.-]*' | head -1)
[ -n "$BUILD_ID" ] || { echo "FINGERPRINT: no build id in BuildInfo.java"; exit 1; }
[ "${BUILD_ID##*-}" = "$SUFFIX" ] || { echo "FINGERPRINT MISMATCH: BuildInfo carries $BUILD_ID but sources hash to suffix $SUFFIX"; exit 1; }
printf '%s\n' "$BUILD_ID" > $O/classes/excel-build.txt
echo "fingerprint gate: $BUILD_ID == excel-build.txt == recomputed source hash OK"
# ---- deterministic jar assembly (built twice, hash-asserted) ----
rm -f "$D/ExcelBrainGames-0.3.3.jar"
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
open(os.path.join(D, 'ExcelBrainGames-0.3.3.jar'), 'wb').write(b1)
print(f"ExcelBrainGames-0.3.3.jar: sha256 {h1} ({len(b1)} bytes) - identical across two assemblies")
PY
