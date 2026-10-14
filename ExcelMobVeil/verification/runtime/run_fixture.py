from pathlib import Path
import subprocess,socket,sys,json,hashlib,time,os
r=Path(__file__).resolve().parent;s=r/'server';mode=sys.argv[1]
jars=list((s/'plugins').glob('ExcelMobVeil*.jar'));assert len(jars)==1
sha=hashlib.sha256(jars[0].read_bytes()).hexdigest()
assert (s/'eula.txt').read_bytes()==Path(os.environ['BASELINE_EULA']).read_bytes()
assert 'server-ip=127.0.0.1' in (s/'server.properties').read_text() and 'server-port=25591' in (s/'server.properties').read_text()
with socket.socket() as sock:sock.bind(('127.0.0.1',25591))
result=s/'verification-result.txt';result.unlink(missing_ok=True);start=time.monotonic()
with (s/f'runtime-{mode}.log').open('w') as log:
 p=subprocess.Popen([str(Path(os.environ['JAVA_HOME'])/'bin/java'),'-Xms256M','-Xmx768M',f'-Dmobveil.probe.mode={mode}','-jar','paper-26.2-123.jar','--nogui'],cwd=s,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
 try:
  deadline=time.monotonic()+(660 if mode=='soak' else 240)
  while p.poll() is None:
   if (r/'STOP_REQUESTED').exists():p.stdin.write('stop\n');p.stdin.flush();p.wait(timeout=30);break
   if time.monotonic()>=deadline:raise subprocess.TimeoutExpired(p.args,deadline-start)
   time.sleep(.5)
  code=p.returncode
 except subprocess.TimeoutExpired:
  p.stdin.write('stop\n');p.stdin.flush()
  try:code=p.wait(timeout=30)
  except subprocess.TimeoutExpired:p.terminate();code=p.wait(timeout=15)
with socket.socket() as sock:sock.bind(('127.0.0.1',25591))
out={'mode':mode,'fresh_execution':True,'exit_code':code,'result':result.read_text().strip() if result.exists() else 'FAIL no result','elapsed_seconds':round(time.monotonic()-start,3),'port_released':True,'candidate_sha256':sha,'server':'Paper 26.2 build 123','new_eula_acceptance':False,'companion_plugins':{x.name:hashlib.sha256(x.read_bytes()).hexdigest() for x in (s/'plugins').glob('ExcelDeathProtection*.jar')}}
(r/f'results-{mode}.json').write_text(json.dumps(out,indent=2));print(json.dumps(out),flush=True)
if code or not out['result'].startswith('PASS'):sys.exit(1)
