#!/usr/bin/env python3
"""Disposable Paper test only. Never creates EULA acceptance or touches live servers.
Run only after the existing server/eula.txt acceptance is explicitly authorized.
"""
from pathlib import Path
import datetime,hashlib,json,shutil,socket,subprocess,urllib.request

ROOT=Path(__file__).resolve().parent
SERVER=ROOT/'server'
JAVA=ROOT/'jdk25/jdk-25.0.4+7/bin/java'
PAPER=SERVER/'paper-26.2-123.jar'
CANDIDATE=SERVER/'plugins/BedrockMenu-1.4.2-menu.1.jar'
EXPECTED_PAPER='7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08'
EXPECTED_CANDIDATE='fef68e9a06053767fa8223811e4ca2b1294a05daefa0e7bb11346ceba730f276'
PORT=25581

def digest(path):
    with open(path,'rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def port_free():
    with socket.socket() as s:s.bind(('127.0.0.1',PORT))

def props(path):
    return dict(line.split('=',1) for line in path.read_text().splitlines() if line and not line.startswith('#') and '=' in line)

def stop_child(p):
    if p.poll() is not None:return
    try:p.stdin.write('stop\n');p.stdin.flush()
    except (BrokenPipeError,OSError):pass
    try:p.wait(timeout=45)
    except subprocess.TimeoutExpired:
        p.terminate()
        try:p.wait(timeout=15)
        except subprocess.TimeoutExpired:p.kill();p.wait(timeout=15)

assert digest(PAPER)==EXPECTED_PAPER,'Paper hash mismatch'
assert digest(CANDIDATE)==EXPECTED_CANDIDATE,'Candidate hash mismatch'
assert {p.name for p in (SERVER/'plugins').glob('*.jar')}=={'BedrockMenu-1.4.2-menu.1.jar','BedrockMenuStartupProbe-TestOnly.jar'},'Unexpected test plugin'
assert (SERVER/'eula.txt').exists() and props(SERVER/'eula.txt').get('eula')=='true','STOP: authorized existing EULA acceptance required; script never supplies it'
eula_hash=digest(SERVER/'eula.txt')
config=props(SERVER/'server.properties')
for key,value in {'server-ip':'127.0.0.1','server-port':str(PORT),'online-mode':'true','white-list':'true','enforce-whitelist':'true','enable-rcon':'false','enable-query':'false','management-server-enabled':'false','level-name':'bedrock-menu-isolated-test-world'}.items():
    assert config.get(key)==value,f'Unsafe or unexpected property {key}'
port_free()

# The official Paperclip archive records the exact Mojang source and hash.
import zipfile
with zipfile.ZipFile(PAPER) as z:
    expected_mojang,url,filename=z.read('META-INF/download-context').decode().strip().split('\t')
mojang=SERVER/'cache'/filename
mojang.parent.mkdir(exist_ok=True)
if not mojang.exists():
    with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'BedrockMenu-compatibility-test/1.0'}),timeout=90) as src,open(mojang,'wb') as dst:
        shutil.copyfileobj(src,dst)
assert digest(mojang)==expected_mojang,'Mojang source checksum mismatch'
with open(ROOT/'evidence/paperclip-patch-only.log','w') as log:
    subprocess.run([str(JAVA),'-Dpaperclip.patchonly=true','-jar',str(PAPER)],cwd=SERVER,stdout=log,stderr=subprocess.STDOUT,timeout=180,check=True)

summary={'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'candidate_sha256':EXPECTED_CANDIDATE,'paper_sha256':EXPECTED_PAPER,'mojang_sha256':expected_mojang,'java':subprocess.check_output([str(JAVA),'-version'],stderr=subprocess.STDOUT,text=True),'listen':'127.0.0.1:'+str(PORT),'real_paper_server':True,'actual_geyser':False,'real_client':False,'real_essentials':False,'boots':[]}
for number in (1,2):
    port_free()
    results=SERVER/'bedrock-startup-results.txt'
    if results.exists():results.rename(ROOT/'evidence'/f'previous-results-before-boot-{number}.txt')
    logpath=ROOT/'evidence'/f'boot-{number}.log'
    p=None
    try:
        with open(logpath,'w') as log:
            p=subprocess.Popen([str(JAVA),'-XX:ActiveProcessorCount=2','-Xms256M','-Xmx1024M','-jar',str(PAPER),'--nogui'],cwd=SERVER,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
            p.wait(timeout=240)
    finally:
        if p is not None:stop_child(p)
    text=logpath.read_text()
    assert p.returncode==0,f'Boot {number} exit {p.returncode}'
    assert 'Done (' in text,f'Boot {number} did not finish actual startup'
    assert results.exists(),f'Boot {number} did not produce probe results'
    checktext=results.read_text();assert 'RESULT PASS 28 checks' in checktext and 'FAIL' not in checktext,checktext
    assert '[BedrockMenu] Enabling BedrockMenu v1.4.2-menu.1' in text,'Missing candidate enable log'
    assert '[BedrockMenu] Disabling BedrockMenu v1.4.2-menu.1' in text,'Missing candidate disable log'
    assert digest(CANDIDATE)==EXPECTED_CANDIDATE,'Candidate changed during runtime'
    assert digest(SERVER/'eula.txt')==eula_hash,'Existing EULA file changed'
    port_free()
    shutil.copyfile(results,ROOT/'evidence'/f'boot-{number}-results.txt')
    summary['boots'].append({'boot':number,'exit_code':p.returncode,'checks':28,'results':'PASS','loopback_port_free_after':True})
    (ROOT/'evidence/summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print('PASS actual Paper boot',number,'28 checks, clean exit, loopback port free',flush=True)
summary['assertions']=56;summary['eula_file_unchanged']=True
(ROOT/'evidence/summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print('PASS 2 actual Paper startups, 56 checks, unchanged exact candidate',flush=True)
