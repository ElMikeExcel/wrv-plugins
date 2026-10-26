#!/usr/bin/env python3
"""Part A panel check. Usage:
  python3 check.py guard <stage> <file> <downloaded path>   # live copy must equal base/ (= live download 2:57 PM CDT Oct 26)
  python3 check.py after <stage> <file> <downloaded path>   # read-back must equal new/
Prints OK or MISMATCH with both short shas; exit 1 on mismatch. Reads only box files."""
import sys,hashlib,os
mode,st,fn,p=sys.argv[1:5]; H=os.path.dirname(os.path.abspath(__file__))
want=open(f"{H}/{'base' if mode=='guard' else 'new'}/stage{st}/{fn}",'rb').read()
got=open(p,'rb').read()
h=lambda b:hashlib.sha256(b).hexdigest()[:16]
ok=h(got)==h(want)
print(('OK' if ok else 'MISMATCH'),mode,f'stage{st}/{fn}','got',h(got),len(got),'want',h(want),len(want))
sys.exit(0 if ok else 1)
