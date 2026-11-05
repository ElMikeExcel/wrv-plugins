#!/usr/bin/env bash
# ExcelLandmarks 0.1.0: compile, run the logic suite (rent clock, Protect
# attribution, tap routing, menu model, core), then assemble the jar
# deterministically. The jar is assembled twice inside the script and the
# SHA-256 hashes must match; run the whole script twice for the cross-run
# reproducibility check (see TEST-REPORT.md).
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"
JDK=/home/hatch/workspace/.jdk25/bin
L="$HOME/workspace/excelbuildmode-pilot/runtime-server/libraries"
API=$HOME/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar
CP="$API"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
VAULT=$HOME/workspace/excelprotect/src/ExcelProtect/libs/Vault-2.0.0.jar
GP=$HOME/workspace/excelprotect/src/ExcelProtect/libs/GriefPrevention-16.18.7.jar
FG=$HOME/workspace/minecraft-server/plugins/floodgate.jar
echo "8b2ce431c88741fcae6d4de6032ab2ab0cca6aa86e8af375bdfe654321822a68  $VAULT" | sha256sum -c - >/dev/null && echo "Vault build input pinned: 8b2ce431... OK"
echo "f0df9e7faccd38fabcdf4d2e6a243649cabc11f0a19fec100b43d7357369fb22  $API" | sha256sum -c - >/dev/null && echo "Paper API build input pinned: f0df9e7f... OK"
echo "45e9907c61222e559ed2e099ffee0ede706a21243a31d7db549baf678708fdf0  $GP" | sha256sum -c - >/dev/null && echo "GriefPrevention build input pinned: 45e9907c... OK"
echo "21570aff9ce17d6983928e8552777760e1ede5050026b04c686b0ae112e6fd7e  $FG" | sha256sum -c - >/dev/null && echo "Floodgate build input pinned: 21570aff... OK"
O=$D/work/out; rm -rf $O; mkdir -p $O/classes $O/test
# descriptor gate: plugin.yml / config.yml that do not parse fail HERE, not at boot
python3 -c "
import yaml
d = yaml.safe_load(open('$D/src/plugin.yml')); assert d['name'] == 'ExcelLandmarks' and str(d['version']) == '0.1.0', d.get('version')
yaml.safe_load(open('$D/src/config.yml'))
yaml.safe_load(open('$D/menu-entry/landmarks.yml'))
print('descriptors parse: plugin.yml (0.1.0) + config.yml + menu entry OK')"
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$VAULT:$GP:$FG" -d $O/classes $(find $D/src -name '*.java')
cp $D/src/plugin.yml $D/src/config.yml $O/classes/
echo "== compiled: $(find $O/classes -name '*.class' | wc -l) classes"
# ---- tests ----
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$VAULT:$GP:$FG" -d $O/test $(find $D/tests -name '*.java')
run() { echo "-- $1"; shift; "$@" || { echo "TEST FAILED"; exit 1; }; }
TC="$O/test:$O/classes:$CP:$VAULT:$GP:$FG"
run RentClockTest     $JDK/java -cp "$TC" com.winkreyes.excellandmarks.RentClockTest
run AttributionTest   $JDK/java -cp "$TC" com.winkreyes.excellandmarks.AttributionTest
run TapRouterTest     $JDK/java -cp "$TC" com.winkreyes.excellandmarks.TapRouterTest
run SpotMenuModelTest $JDK/java -cp "$TC" com.winkreyes.excellandmarks.SpotMenuModelTest
run CoreTest          $JDK/java -cp "$TC" com.winkreyes.excellandmarks.CoreTest
# ---- deterministic jar assembly (built twice, hash-asserted) ----
rm -f "$D/ExcelLandmarks-0.1.0.jar"
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
            zi = zipfile.ZipInfo(n, (2026, 11, 5, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, entries[n])
    return buf.getvalue()
b1, b2 = build(), build()
h1, h2 = hashlib.sha256(b1).hexdigest(), hashlib.sha256(b2).hexdigest()
assert h1 == h2, "REBUILD NOT REPRODUCIBLE"
open(os.path.join(D, 'ExcelLandmarks-0.1.0.jar'), 'wb').write(b1)
print(f"ExcelLandmarks-0.1.0.jar: sha256 {h1} ({len(b1)} bytes) - identical across two assemblies")
PY
