#!/usr/bin/env python3
"""Offline deterministic build. No network, source generation, activation, or state files."""
import argparse,hashlib,json,os,pathlib,shutil,subprocess,zipfile
P=pathlib.Path; ROOT=P(__file__).resolve().parent
ap=argparse.ArgumentParser();ap.add_argument('--jdk',required=True,type=P);ap.add_argument('--libraries',required=True,type=P);ap.add_argument('--original',required=True,type=P);ap.add_argument('--output',required=True,type=P);a=ap.parse_args()
def require(condition,message):
 if not condition: raise RuntimeError(message)
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(*args):subprocess.run(list(map(str,args)),check=True,env={**os.environ,'TZ':'UTC','LC_ALL':'C','JAVA_TOOL_OPTIONS':'-Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US'})
require(digest(a.original)=='4de62ec8545afbe65b83aeed5a8474b520569e59fd31116dd9f6e4380bc7d580','wrong original JAR')
version=subprocess.check_output([str(a.jdk/'bin/javac'),'-version'],text=True).strip();require(version=='javac 25.0.4','wrong compiler: '+version)
manifest=json.loads((ROOT/'dependencies.json').read_text()); deps=[]
for rel,sha in sorted(manifest.items()):
 p=a.libraries/rel;require(digest(p)==sha,'dependency mismatch: '+rel);deps.append(str(p))
out=a.output.resolve();require(not out.exists(),'output must be a NEW clean directory');out.mkdir(parents=True)
for n in ['classes','tools','tests']:(out/n).mkdir()
cp=os.pathsep.join([str(a.original.resolve())]+deps)
common=['--release','25','-encoding','UTF-8','-g:source,lines','-proc:none']
run(a.jdk/'bin/javac',*common,'-cp',cp,'-d',out/'classes',*sorted(ROOT.glob('src/**/*.java')))
run(a.jdk/'bin/javac',*common,'-cp',cp,'-d',out/'tools',*sorted(ROOT.glob('tools/**/*.java')))
jar=out/'Magic-LEARNED-OFFLINE.jar'
run(a.jdk/'bin/java','-cp',str(out/'tools')+os.pathsep+cp,'PatchLearnedObserver',a.original.resolve(),jar,out/'classes')
newcp=os.pathsep.join([str(jar)]+deps)
run(a.jdk/'bin/javac',*common,'-cp',newcp,'-d',out/'tests',*sorted(ROOT.glob('test/**/*.java')))
run(a.jdk/'bin/java','-cp',str(out/'tests')+os.pathsep+newcp,'LearnedLinkageTest',jar)
run(a.jdk/'bin/java','-cp',str(out/'tests')+os.pathsep+newcp,'LoreBridgeTest')
run(a.jdk/'bin/java','-cp',str(out/'tests')+os.pathsep+newcp,'ContextLookupTest')
run(a.jdk/'bin/java','-cp',str(out/'tests')+os.pathsep+newcp,'CallbackAuthorityTest')
run(a.jdk/'bin/java','-cp',str(out/'tests')+os.pathsep+newcp,'RawWandLoadTest')
expected={'com/elmakers/mine/bukkit/'+p+'.class' for p in ['wand/Wand','spell/BaseSpell','action/CastContext','action/ActionHandler','action/builtin/HealAction','action/builtin/DamageAction','action/builtin/PotionEffectAction']}
with zipfile.ZipFile(a.original) as orig,zipfile.ZipFile(jar) as new:
 require(len(new.namelist())==len(set(new.namelist())),'duplicate entries')
 require(set(orig.namelist())<=set(new.namelist()),'lost native entries')
 changed={n for n in orig.namelist() if orig.read(n)!=new.read(n)};require(changed==expected,'unexpected changed entries: '+repr(changed))
 added=set(new.namelist())-set(orig.namelist());require(bool(added) and all(n.startswith(('wrv/magic/evidence/','wrv/magic/level/')) for n in added),'unexpected added entries')
 require(not any('UsefulSpellProgression' in n or 'PracticeReceiptSink' in n for n in new.namelist()),'obsolete ledger/helper bundled')
 report={'originalSha256':digest(a.original),'outputSha256':digest(jar),'javac':version,'release':25,'changedNativeClasses':sorted(changed),'addedClasses':sorted(added),'linkages':16}
 (out/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS exact seven-class archive comparison; SHA256 '+digest(jar))
