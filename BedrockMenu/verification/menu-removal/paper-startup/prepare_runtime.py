#!/usr/bin/env python3
"""Prepare an isolated check from verified official inputs. Never accepts EULA or starts Paper."""
import argparse,hashlib,json,shutil,subprocess,tarfile,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
EXPECTED_PAPER='7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08'
EXPECTED_JDK='e58fcdcd637b25c03ca84cbbcefc70d11efb8f4b4cbd05decc9f661769d77f94'
EXPECTED_CANDIDATE='fef68e9a06053767fa8223811e4ca2b1294a05daefa0e7bb11346ceba730f276'
def digest(path):
    with open(path,'rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def require(condition,message):
    if not condition:raise RuntimeError(message)
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--paper-jar',required=True,type=Path)
p.add_argument('--jdk-archive',required=True,type=Path)
p.add_argument('--candidate',required=True,type=Path)
a=p.parse_args()
for path,expected in [(a.paper_jar,EXPECTED_PAPER),(a.jdk_archive,EXPECTED_JDK),(a.candidate,EXPECTED_CANDIDATE)]:
    require(path.is_file() and digest(path)==expected,'Missing or mismatched artifact: '+str(path))
for name in ['server','jdk25','compile-libraries','probe/classes']:
    require(not (ROOT/name).exists(),'Use a fresh harness directory; refusing existing '+name)
(ROOT/'server/plugins').mkdir(parents=True)
(ROOT/'probe/classes').mkdir(parents=True)
(ROOT/'evidence').mkdir(exist_ok=True)
with tarfile.open(a.jdk_archive) as t:t.extractall(ROOT/'jdk25',filter='data')
with zipfile.ZipFile(a.paper_jar) as z:
    for line in z.read('META-INF/libraries.list').decode().splitlines():
        checksum,artifact,path=line.split('\t');member='META-INF/libraries/'+path
        if member not in z.namelist():continue
        relative=Path(path);require(not relative.is_absolute() and '..' not in relative.parts,'Unsafe library path')
        data=z.read(member);require(hashlib.sha256(data).hexdigest()==checksum,'Library checksum: '+artifact)
        out=ROOT/'compile-libraries'/relative;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(data)
shutil.copyfile(a.paper_jar,ROOT/'server/paper-26.2-123.jar')
shutil.copyfile(a.candidate,ROOT/'server/plugins/BedrockMenu-1.4.2-menu.1.jar')
shutil.copyfile(ROOT/'server.properties.loopback',ROOT/'server/server.properties')
java_home=ROOT/'jdk25/jdk-25.0.4+7'
cp=':'.join(str(f) for f in (ROOT/'compile-libraries').rglob('*.jar'))
cmd=[str(java_home/'bin/javac'),'--release','21','-cp',cp,'-d',str(ROOT/'probe/classes'),*map(str,(ROOT/'probe/src').rglob('*.java'))]
r=subprocess.run(cmd,text=True,capture_output=True);(ROOT/'evidence/probe-compile.log').write_text(r.stdout+r.stderr);r.check_returncode()
with zipfile.ZipFile(ROOT/'server/plugins/BedrockMenuStartupProbe-TestOnly.jar','w') as z:
    for f in sorted((ROOT/'probe/classes/check').rglob('*.class')):z.write(f,str(f.relative_to(ROOT/'probe/classes')))
    z.write(ROOT/'probe/plugin.yml','plugin.yml')
cmd=[str(java_home/'bin/java'),'-Xverify:all','-cp',str(ROOT/'probe/classes')+':'+str(ROOT/'server/plugins/BedrockMenu-1.4.2-menu.1.jar')+':'+cp,'VerifyRealApi']
r=subprocess.run(cmd,text=True,capture_output=True);(ROOT/'evidence/real-api-linkage.log').write_text(r.stdout+r.stderr);print(r.stdout+r.stderr);r.check_returncode()
print('Prepared. No EULA accepted and no server started. Complete the EULA step before run_startup_check.py.')
