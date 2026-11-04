#!/usr/bin/env bash
# ExcelTogether 0.5.3 (party mesh + the Magic party-repair actions + the 0.5.2 confirmation
# gate): compile, run the FULL suite (inherited 0.3.0 tests, §11A base tests, §11B/§11D mesh
# tests, the 0.5.1 PartyRepairTest, the 0.5.2 RepairGateTest), then assemble the jar
# deterministically.
# Provenance rule (DESIGN §1.2): classes the mesh does not change carry the SHIPPED 0.4.3 bytes
# whenever the recompiled bytes differ, so the jar's unchanged surface is byte-identical to live.
# The jar is built twice and the two SHA-256 hashes must match before it is staged.
# 0.5.1 adds Magic as a COMPILE-TIME-ONLY input (work/magic-11.2.5.jar, hash-pinned below): the
# two action classes extend Magic's action bases. At runtime Magic stays a softdepend - the
# classes are registered reflectively (ReflectMagicLink) and never load when Magic is absent.
set -euo pipefail
D="$(cd "$(dirname "$0")" && pwd)"
JDK=/home/hatch/workspace/.jdk25/bin
L="$HOME/workspace/excelbuildmode-pilot/runtime-server/libraries"
API=$L/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar
CP="$API"
for j in $(find $L/net/kyori $L/org/jspecify $L/com/google $L/net/md-5 $L/org/yaml $L/org/slf4j $L/org/joml $L/it/unimi -name '*.jar' 2>/dev/null); do CP="$CP:$j"; done
JOBS="$HOME/workspace/economy-jars/Jobs.jar:$HOME/workspace/economy-jars/CMILib.jar"
FG=$HOME/workspace/minecraft-server/plugins/floodgate.jar
MMO_UP=$HOME/workspace/excelrewardguard/work/test-server-1.2-providers/plugins/mcMMO.jar
MMO_MINE=$HOME/workspace/backups/pre-guild-20261015/root/plugins/mcMMO-2.3.003-WRV-XP-2.jar
SHIPPED=$D/work/pulled-0.4.3.jar
MAGIC=$D/work/magic-11.2.5.jar
echo "79e49fe32fdc4e25375c0099d2bd10665b9792bfd9b35fab8d64a498ebfc3532  $MAGIC" | sha256sum -c - >/dev/null && echo "Magic build input pinned: 79e49fe3... OK"
O=$D/work/out; rm -rf $O; mkdir -p $O/classes $O/test
# descriptor gate: a plugin.yml/config.yml that does not parse as YAML fails HERE, not at boot
# (the 0.5.0 description's "0.5.0: ..." sentence is why plugin.yml's description is quoted).
python3 -c "
import yaml
d = yaml.safe_load(open('$D/plugin.yml')); assert d['name'] == 'ExcelTogether' and str(d['version']) == '0.5.3', d.get('version')
yaml.safe_load(open('$D/config.yml')); print('descriptors parse: plugin.yml (0.5.3) + config.yml OK')"
$JDK/javac --release 21 -proc:none -nowarn -cp "$CP:$JOBS:$MAGIC" -d $O/classes $(find $D/src -name '*.java')
cp $D/plugin.yml $D/config.yml $O/classes/
echo "== compiled: $(find $O/classes -name '*.class' | wc -l) classes"
# ---- tests ----
$JDK/javac --release 21 -proc:none -nowarn -cp "$O/classes:$CP:$JOBS:$FG" -d $O/test $(find $D/tests -name '*.java')
run() { echo "-- $1"; shift; "$@" || { echo "TEST FAILED: $1"; exit 1; }; }
TC="$O/test:$O/classes:$CP:$JOBS:$FG:$MAGIC"
run PartyTest            $JDK/java -cp "$TC" com.winkreyes.exceltogether.PartyTest
run PartyFeatureTest     $JDK/java -Dsrc=$D/src -Dcfg=$D/config.yml -Dplugin=$D/plugin.yml -Dfgjar=$FG -cp "$TC" com.winkreyes.exceltogether.PartyFeatureTest
run RangeIndicatorTest   $JDK/java -cp "$TC" com.winkreyes.exceltogether.RangeIndicatorTest
run McmmoLinkTest        $JDK/java -Dmcmmojar=$MMO_UP -cp "$TC" com.winkreyes.exceltogether.McmmoLinkTest
run McmmoSyncTest        $JDK/java -cp "$TC" com.winkreyes.exceltogether.McmmoSyncTest
run ShareTest            $JDK/java -cp "$TC" com.winkreyes.exceltogether.ShareTest
run XpShareTest          $JDK/java -Dmcmmojar=$MMO_MINE -cp "$TC" com.winkreyes.exceltogether.XpShareTest
run BaseRulesTest        $JDK/java -cp "$TC" com.winkreyes.exceltogether.BaseRulesTest
run EmSyncTest           $JDK/java -cp "$TC" com.winkreyes.exceltogether.EmSyncTest
run EmLinkMappingTest    $JDK/java -cp "$TC" com.winkreyes.exceltogether.EmLinkMappingTest
run PartyLookupTest      $JDK/java -cp "$TC" com.winkreyes.exceltogether.PartyLookupTest
run PartyRepairTest      $JDK/java -cp "$TC" com.winkreyes.exceltogether.PartyRepairTest
run RepairGateTest       $JDK/java -cp "$TC" com.winkreyes.exceltogether.RepairGateTest
run LeaveSticksTest      $JDK/java -cp "$TC" com.winkreyes.exceltogether.LeaveSticksTest
run AdminCommandTest     $JDK/java -cp "$TC" com.winkreyes.exceltogether.AdminCommandTest
# ---- jar assembly (changed/new from source; design-unchanged from the shipped jar when bytes differ) ----
rm -f "$D/ExcelTogether-0.5.3.jar"   # a failed assembly must never leave a stale jar looking current
python3 - "$O" "$D" <<'PY'
import zipfile, hashlib, os, sys, io
O, D = sys.argv[1], sys.argv[2]
ship = zipfile.ZipFile(os.path.join(D, 'work', 'pulled-0.4.3.jar'))
shipmap = {n: ship.read(n) for n in ship.namelist()}
root = os.path.join(O, 'classes')
CHANGED = ('Controller', 'ExcelTogether', 'EmLink', 'ReflectEmLink', 'EmSync', 'McmmoSync', 'MagicLevelPartyLookup', 'PartyLookup')
def top(rel):
    base = os.path.basename(rel)
    return base[:-6].split('$')[0] if base.endswith('.class') else None
entries, carried, compiled_same, fresh = {}, [], [], []
for dp, _, fn in os.walk(root):
    for f in fn:
        p = os.path.join(dp, f)
        rel = os.path.relpath(p, root)
        data = open(p, 'rb').read()
        t = top(rel)
        if f.endswith('.class') and t not in CHANGED and rel in shipmap:
            if shipmap[rel] == data:
                compiled_same.append(rel)
            else:
                data = shipmap[rel]
                carried.append(rel)
        elif f.endswith('.class'):
            fresh.append(rel)
        entries[rel] = data
def build():
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, 'w', zipfile.ZIP_DEFLATED) as z:
        for n in sorted(entries):
            zi = zipfile.ZipInfo(n, (2026, 11, 1, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, entries[n])
    return buf.getvalue()
b1, b2 = build(), build()
h1, h2 = hashlib.sha256(b1).hexdigest(), hashlib.sha256(b2).hexdigest()
print(f"classes compiled byte-identical to shipped (kept): {len(compiled_same)}")
print(f"classes carried as shipped bytes (design-unchanged, bytes differ): {len(carried)}")
for c in sorted(carried): print("  carried:", c)
print(f"classes from 0.5.3 source (changed + new): {len(fresh)}")
for c in sorted(fresh): print("  source:", c)
assert h1 == h2, "REBUILD NOT REPRODUCIBLE"
open(os.path.join(D, 'ExcelTogether-0.5.3.jar'), 'wb').write(b1)
print(f"ExcelTogether-0.5.3.jar: sha256 {h1} ({len(b1)} bytes) — identical across two builds")
PY
