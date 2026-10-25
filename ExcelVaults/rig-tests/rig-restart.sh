#!/bin/bash
# Restart ONLY the ExcelVaults rig (java whose cwd is /workspace/excelvaults-rig). Never touches other rigs.
R=/workspace/excelvaults-rig
P=""; for p in $(pgrep -f '^java .*paper.jar'); do [ "$(readlink /proc/$p/cwd)" = "$R" ] && P=$p; done
if [ -n "$P" ]; then
  python3 "$(dirname "$0")/rcon.py" save-all stop >/dev/null 2>&1
  for i in $(seq 1 60); do kill -0 $P 2>/dev/null || break; sleep 1; done
  kill -0 $P 2>/dev/null && { echo "rig $P did not stop"; exit 1; }
fi
cp "$(dirname "$0")/../ExcelVaults-0.1.0.jar" $R/plugins/
rm -f $R/boot.log; $R/start2.sh; sleep 2
for i in $(seq 1 90); do grep -aq 'Done (' $R/boot.log 2>/dev/null && break; sleep 1; done
pgrep -f '^java .*paper.jar' | while read p; do [ "$(readlink /proc/$p/cwd)" = "$R" ] && echo "rig java pid $p"; done
grep -a 'ExcelVaults enabled' $R/logs/latest.log | sed 's/^.*INFO\]: //'
