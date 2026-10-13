#!/usr/bin/env python3
"""Run paired-confirmation QA on a pre-provisioned disposable server. Never touches a live server."""
import argparse,hashlib,json,os,socket,subprocess,shutil,zipfile,io,re
from pathlib import Path
from run import PINNED,digest,props,port_free,stop

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('--server-dir',required=True,type=Path);parser.add_argument('--java-home',required=True,type=Path)
 parser.add_argument('--plugin',required=True,type=Path);parser.add_argument('--plugin-sha256',required=True);parser.add_argument('--guard',required=True,type=Path);parser.add_argument('--guard-sha256',required=True)
 parser.add_argument('--original-chestshop',required=True,type=Path)
 parser.add_argument('--mode',choices=['full','restart','missing-excel','no-guard','mismatch','recovery','missing-chestshop-recovery','partial-failure'],default='full');parser.add_argument('--label',required=True)
 args=parser.parse_args();root=args.server_dir.resolve();here=Path(__file__).resolve().parent;java=args.java_home.resolve();plugins=root/'plugins'
 for name,sha in PINNED.items():
  if 'ChestShop' not in name and digest(root/name)!=sha:raise RuntimeError('Pinned dependency mismatch '+name)
 if digest(root/'annotations-26.0.2.jar')!='2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297':raise RuntimeError('Annotations checksum mismatch')
 if digest(args.original_chestshop)!=PINNED['plugins/ChestShop-3.13-pre-1-build474.jar']:raise RuntimeError('Original ChestShop hash mismatch')
 if digest(args.plugin)!=args.plugin_sha256:raise RuntimeError('Supplied ExcelShops hash mismatch')
 if digest(args.guard)!=args.guard_sha256:raise RuntimeError('Supplied guard hash mismatch')
 cfg=props(root/'server.properties')
 for key,value in {'server-ip':'127.0.0.1','server-port':'25580','online-mode':'true','white-list':'true','enforce-whitelist':'true','enable-rcon':'false','enable-query':'false','management-server-enabled':'false','level-name':'excel-legacy-harness-world'}.items():
  if cfg.get(key)!=value:raise RuntimeError('Isolation configuration mismatch '+key)
 if props(root/'eula.txt').get('eula')!='true':raise RuntimeError('Previously accepted EULA file required')
 if not (root/'dependency-manifest.json').exists():raise RuntimeError('Disposable manifest required')
 port_free();config=plugins/'ExcelShops/config.yml';config.parent.mkdir(exist_ok=True);config.write_text('require-grief-prevention: true\nquote-seconds: 5\nbedrock-forms: false\n');allowed={Path(p).name for p in PINNED if p.startswith('plugins/')}|{'ExcelShops.jar','ExcelLegacyRuntimeHarness.jar'}
 if any(p.name not in allowed for p in plugins.glob('*.jar')):raise RuntimeError('Unexpected plugin in disposable server')
 chest=plugins/'ChestShop-3.13-pre-1-build474.jar'
 shutil.copy2(args.original_chestshop if args.mode=='no-guard' else args.guard,chest)
 if args.mode=='mismatch':
  with zipfile.ZipFile(chest) as source:entries={n:source.read(n) for n in source.namelist()}
  entries['plugin.yml']=entries['plugin.yml'].replace(b'+confirmation.1',b'+confirmation.mismatch-test')
  gate='com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class'
  if b'chestshop474-confirmation.1' not in entries[gate]:raise RuntimeError('Expected guard contract string missing')
  entries[gate]=entries[gate].replace(b'chestshop474-confirmation.1',b'chestshop474-confirmation.9')
  with zipfile.ZipFile(chest,'w',zipfile.ZIP_DEFLATED) as target:
   for name,data in entries.items():target.writestr(name,data)
 if args.mode=='missing-excel':
  if (plugins/'ExcelShops.jar').exists():shutil.move(plugins/'ExcelShops.jar',root/'last-disabled-ExcelShops.jar')
 else:shutil.copy2(args.plugin,plugins/'ExcelShops.jar')
 if args.mode in ('recovery','missing-chestshop-recovery'):
  marker=plugins/'ExcelShops/legacy-checkout/active-session';marker.parent.mkdir(parents=True,exist_ok=True)
  if not marker.exists():marker.write_text('Synthetic interrupted disposable game-trade session. Keep paused.\n')
 if args.mode=='missing-chestshop-recovery':
  shutil.move(chest,root/'disabled-ChestShop.jar')
  native_marker=plugins/'ExcelShops/checkout/active-session'
  if native_marker.exists():shutil.move(native_marker,root/(args.label+'-native-marker-preserved.evidence'))
 build=root/'full-harness-build';build.mkdir(exist_ok=True)
 cp=os.pathsep.join(str(p) for p in [root/'annotations-26.0.2.jar',args.original_chestshop,*sorted((root/'libraries').rglob('*.jar')),*sorted(plugins.glob('*.jar'))])
 subprocess.run([str(java/'bin/javac'),'--release','25','-cp',cp,'-d',str(build),*map(str,sorted((here/'src').rglob('*.java')))],check=True)
 (build/'plugin.yml').write_text('name: ExcelLegacyRuntimeHarness\nversion: 0.0.0-test-only\nmain: wrv.harness.FullLegacyRuntimeHarness\napi-version: \'1.21\'\ndepend: [Vault, Essentials, GriefPrevention, ChestShop]\nsoftdepend: [ExcelShops]\ndescription: NEVER DEPLOY. Disposable synthetic game-plugin integration fixture.\n')
 if args.mode=='missing-chestshop-recovery':
  metadata=(build/'plugin.yml').read_text().replace('wrv.harness.FullLegacyRuntimeHarness','wrv.harness.LegacyRecoveryProbe').replace('depend: [Vault, Essentials, GriefPrevention, ChestShop]','depend: [Vault, Essentials, GriefPrevention, ExcelShops]');(build/'plugin.yml').write_text(metadata)
 subprocess.run([str(java/'bin/jar'),'--create','--file',str(plugins/'ExcelLegacyRuntimeHarness.jar'),'-C',str(build),'.'],check=True)
 recovery_paths=[plugins/'ExcelShops/legacy-checkout/transactions.log',plugins/'ExcelShops/legacy-checkout/active-session']
 recovery_before={p.name:digest(p) for p in recovery_paths if p.exists()}
 manifest={'mode':args.mode,'legacy_evidence_before':recovery_before,'native_marker_isolated_for_independent_legacy_probe':args.mode=='missing-chestshop-recovery','harness_sources':{p.relative_to(here).as_posix():digest(p) for p in sorted((here/'src').rglob('*.java'))},'source_plugin_sha256':digest(args.plugin),'source_guard_sha256':digest(args.guard),'artifacts':{p.name:digest(p) for p in plugins.glob('*.jar')},'synthetic_players':True,'real_network_client':False,'real_chestshop':args.mode!='missing-chestshop-recovery','real_essentials':True,'real_gp':True,'exact_live_vault':False,'vault_substitute':'official MilkBowl Vault1.7.3','listen':'127.0.0.1:25580'}
 log=root/(args.label+'-console.log')
 with log.open('w') as out:
  command=[str(java/'bin/java'),'-Dlegacy.test.mode='+args.mode,'-Dterminal.jline=false','-Dterminal.ansi=false','-Xms384M','-Xmx1200M','-jar','paper-26.2-123.jar','--nogui'];process=subprocess.Popen(command,cwd=root,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True)
  try:manifest['exit_code']=process.wait(timeout=360)
  except BaseException:stop(process);raise
 port_free();manifest['legacy_evidence_after']={p.name:digest(p) for p in recovery_paths if p.exists()};manifest['port_free_after']=True;(root/(args.label+'-manifest.json')).write_text(json.dumps(manifest,indent=2)+'\n')
 result=plugins/'ExcelLegacyRuntimeHarness'/(args.mode+'-results.txt')
 if result.exists():shutil.copy2(result,root/(args.label+'-results.txt'))
 data=log.read_text();summary=re.findall(r'LEGACY_FULL_SUMMARY passed=(\d+) assertions=(\d+) failed=(\d+)',data)
 print('\n'.join(line for line in data.splitlines() if 'LEGACY_FULL_FAIL' in line or 'LEGACY_FULL_SUMMARY' in line or 'Legacy ChestShop trading paused' in line or 'Legacy trade outcome needs' in line))
 if args.mode in ('recovery','missing-chestshop-recovery') and manifest['legacy_evidence_after']!=recovery_before:raise RuntimeError('Recovery evidence unexpectedly changed')
 if manifest['exit_code']!=0 or not summary or summary[-1][2]!='0':raise RuntimeError('Paired runtime failed; see '+str(log))
if __name__=='__main__':main()
