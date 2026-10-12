#!/usr/bin/env python3
"""Compile and run only a verified disposable official-plugin stack. Never downloads or accepts terms."""
import argparse,hashlib,json,os,socket,subprocess,shutil
from pathlib import Path
PINNED={'paper-26.2-123.jar':'7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08','plugins/ChestShop-3.13-pre-1-build474.jar':'b31a40e9561d399d8b04fada4e863ae28c523618976ff3a3b1a6f16fe3aa8ab2','plugins/EssentialsX-2.22.0.jar':'bda4685105977fca2e209820a9f0ad24275bd103390a03236f38e59bfdac58e6','plugins/GriefPrevention-16.18.7.jar':'45e9907c61222e559ed2e099ffee0ede706a21243a31d7db549baf678708fdf0','plugins/Vault-official-1.7.3.jar':'a6b5ed97f43a5cf5bbaf00a7c8cd23c5afc9bd003f849875af8b36e6cf77d01d'}
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def props(p):return dict(line.split('=',1) for line in p.read_text().splitlines() if '=' in line and not line.startswith('#'))
def port_free():
 with socket.socket() as s:s.bind(('127.0.0.1',25580))
def stop(p):
 if p.poll() is not None:return
 try:p.stdin.write('stop\n');p.stdin.flush()
 except (BrokenPipeError,OSError):pass
 try:p.wait(timeout=35)
 except subprocess.TimeoutExpired:
  p.terminate()
  try:p.wait(timeout=20)
  except subprocess.TimeoutExpired:p.kill();p.wait(timeout=20)
def main():
 a=argparse.ArgumentParser(description=__doc__);a.add_argument('--server-dir',type=Path,required=True);a.add_argument('--java-home',type=Path,required=True);a.add_argument('--baseline',action='store_true');a.add_argument('--label',default='probe');args=a.parse_args();root=args.server_dir.resolve();here=Path(__file__).resolve().parent;java=args.java_home.resolve()
 for name,sha in PINNED.items():
  if digest(root/name)!=sha:raise RuntimeError('Pinned artifact mismatch '+name)
 cfg=props(root/'server.properties')
 for k,v in {'server-ip':'127.0.0.1','server-port':'25580','online-mode':'true','white-list':'true','enforce-whitelist':'true','enable-rcon':'false','enable-query':'false','management-server-enabled':'false','level-name':'excel-legacy-harness-world'}.items():
  if cfg.get(k)!=v:raise RuntimeError('Isolation configuration mismatch '+k)
 if props(root/'eula.txt').get('eula')!='true':raise RuntimeError('Previously accepted EULA file required; no acceptance is created.')
 allowed={Path(n).name for n in PINNED if n.startswith('plugins/')}|{'ExcelShops.jar','ExcelLegacyRuntimeHarness.jar'}
 if any(p.name not in allowed for p in (root/'plugins').glob('*.jar')):raise RuntimeError('Unexpected plugin in disposable fixture')
 if not (root/'dependency-manifest.json').exists():raise RuntimeError('Explicit disposable dependency manifest required')
 port_free();build=root/'harness-build';build.mkdir(exist_ok=True)
 cp=os.pathsep.join(str(p) for p in [*sorted((root/'libraries').rglob('*.jar')),*sorted((root/'plugins').glob('*.jar'))])
 subprocess.run([str(java/'bin/javac'),'--release','25','-cp',cp,'-d',str(build),str(here/'src/wrv/harness/FakePlayer.java'),str(here/'src/wrv/harness/LegacyRuntimeHarness.java')],check=True)
 shutil.copy2(here/'plugin.yml',build/'plugin.yml');subprocess.run([str(java/'bin/jar'),'--create','--file',str(root/'plugins/ExcelLegacyRuntimeHarness.jar'),'-C',str(build),'.'],check=True)
 manifest={'baseline':args.baseline,'artifacts':{p.name:digest(p) for p in (root/'plugins').glob('*.jar')},'synthetic_players':True,'real_client':False,'real_chestshop':True,'real_essentials':True,'real_gp':True,'exact_live_vault':False,'vault_substitute':'official MilkBowl Vault1.7.3','listen':'127.0.0.1:25580'}
 log=root/(args.label+'-console.log');manifest_file=root/(args.label+'-manifest.json')
 with log.open('w') as out:
  command=[str(java/'bin/java'),'-Dterminal.jline=false','-Dterminal.ansi=false','-Xms384M','-Xmx1200M']
  if args.baseline:command+=['-Dlegacy.test.baseline=true']
  command+=['-jar','paper-26.2-123.jar','--nogui'];p=subprocess.Popen(command,cwd=root,stdin=subprocess.PIPE,stdout=out,stderr=subprocess.STDOUT,text=True)
  try:manifest['exit_code']=p.wait(timeout=240)
  except BaseException:stop(p);raise
 port_free();manifest['port_free_after']=True;manifest_file.write_text(json.dumps(manifest,indent=2)+'\n');data=log.read_text();print(data[-9000:]);
 if manifest['exit_code']!=0 or 'LEGACY_HARNESS_SUMMARY' not in data or 'failed=0' not in data:raise RuntimeError('Runtime probe failed; see '+str(log))
if __name__=='__main__':main()
