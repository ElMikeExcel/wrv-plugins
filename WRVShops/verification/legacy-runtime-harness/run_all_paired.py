#!/usr/bin/env python3
"""Run the exact supplied paired game-plugin artifacts through full and lifecycle QA."""
import argparse,json,re,subprocess,sys,hashlib
from pathlib import Path

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 for name in ['server-dir','java-home','plugin','guard','original-chestshop']:parser.add_argument('--'+name,type=Path,required=True)
 for name in ['plugin-sha256','guard-sha256','prefix']:parser.add_argument('--'+name,required=True)
 parser.add_argument("--resume",action="store_true")
 args=parser.parse_args();here=Path(__file__).resolve().parent;root=args.server_dir.resolve()
 sequence=[('full','full'),('restart','clean-restart'),('missing-excel','missing-excel'),('no-guard','unmodified-chestshop'),('mismatch','mismatched-chestshop'),('partial-failure','injected-item-failure'),('recovery','recovery-first'),('recovery','recovery-restart'),('missing-chestshop-recovery','absent-chestshop-recovery'),('missing-chestshop-recovery','absent-chestshop-recovery-restart')]
 records=[]
 if args.resume:
  saved=json.loads((root/(args.prefix+'-aggregate.json')).read_text());records=saved['runs']
  for index,record in enumerate(records):
   expected=args.prefix+'-'+sequence[index][1]
   if record['label']!=expected or record['failures']:raise RuntimeError('Cannot resume invalid prior sequence')
   manifest=json.loads((root/record['manifest']).read_text())
   if manifest['source_plugin_sha256']!=args.plugin_sha256 or manifest['source_guard_sha256']!=args.guard_sha256 or manifest['exit_code']!=0:raise RuntimeError('Prior accepted artifact mismatch')
 for mode,name in sequence[len(records):]:
  label=args.prefix+'-'+name
  command=[sys.executable,str(here/'run_paired.py'),'--mode',mode,'--label',label]
  for key in ['server_dir','java_home','plugin','plugin_sha256','guard','guard_sha256','original_chestshop']:command+=['--'+key.replace('_','-'),str(getattr(args,key))]
  print('START '+label,flush=True);subprocess.run(command,check=True)
  result=(root/(label+'-results.txt')).read_text();match=re.search(r'SUMMARY passed=(\d+) assertions=(\d+) failed=(\d+)',result)
  if not match or int(match[3]):raise RuntimeError('Invalid result for '+label)
  records.append({'label':label,'mode':mode,'groups':int(match[1]),'assertions':int(match[2]),'failures':int(match[3]),'manifest':label+'-manifest.json','results':label+'-results.txt'})
  (root/(args.prefix+'-aggregate.json')).write_text(json.dumps({'complete':False,'runs':records},indent=2)+'\n')
 summary={'complete':True,'excelshops_sha256':args.plugin_sha256,'guard_sha256':args.guard_sha256,'full_server_boots':len(records),'groups':sum(r['groups'] for r in records),'assertions':sum(r['assertions'] for r in records),'failures':sum(r['failures'] for r in records),'runs':records,'limits':['Synthetic players, not real clients','Official Vault1.7.3 is a substitute for unidentified live Vault2.0.0','No live server access or installation']}
 (root/(args.prefix+'-aggregate.json')).write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
if __name__=='__main__':main()
