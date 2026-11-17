from pathlib import Path
import subprocess,shutil,socket,time,json
r=Path('/workspace/shared/wand-diagnostic');b=Path('/workspace/scratch/9e01236aa418/leaderboard-runtime-prep');s=b/'server'
assert (s/'eula.txt').read_text().strip()=='eula=true'
props=(s/'server.properties').read_bytes();assert b'server-ip=127.0.0.1' in props
for port in (25589,25590,25591):
 with socket.socket() as sock:sock.bind(('127.0.0.1',port))
backup=b/'plugins-before-wand-diagnostic';assert not backup.exists()
shutil.move(s/'plugins',backup);(s/'plugins').mkdir()
shutil.copy2('/workspace/shared/wand-fix/deps/Geyser-Spigot-2.12.0-b1269.jar',s/'plugins/Geyser-Spigot.jar')
shutil.copy2('/workspace/shared/wand-fix/deps/Magic-11.2.5.jar',s/'plugins/Magic.jar')
shutil.copy2('/workspace/shared/wand-followup/ExcelWandBars-0.3.2-gesture-candidate.jar',s/'plugins/ExcelWandBars.jar')
shutil.copy2(r/'WRVWandDiagnostic-0.1.0-offline.jar',s/'plugins/WRVWandDiagnostic.jar');shutil.copy2(r/'probe/DiagnosticProbe.jar',s/'plugins/DiagnosticProbe.jar')
(s/'plugins/Geyser-Spigot').mkdir();shutil.copy2('/workspace/shared/bedrock-readiness/generated-plugins/Geyser-Spigot/config.yml',s/'plugins/Geyser-Spigot/config.yml')
(s/'server.properties').write_bytes(props.replace(b'level-name=leaderboard-isolated-world',b'level-name=wand-diagnostic-synthetic-world'))
p=None
try:
 with (r/'smoke.log').open('w') as log:
  p=subprocess.Popen([str(b/'jdk/bin/java'),'-Xms256M','-Xmx1024M','-jar','paper-26.3-214.jar','--nogui'],cwd=s,stdin=subprocess.PIPE,stdout=log,stderr=subprocess.STDOUT,text=True)
  try:code=p.wait(timeout=180)
  except subprocess.TimeoutExpired:
   p.stdin.write('stop\n');p.stdin.flush()
   try:code=p.wait(timeout=30)
   except subprocess.TimeoutExpired:p.terminate();code=p.wait(timeout=15)
  txt=(r/'smoke.log').read_text();(r/'smoke-result.json').write_text(json.dumps({'exit_code':code,'booted':'Done (' in txt,'probe_pass':'DIAGNOSTIC_PROBE_PASS' in txt,'scope':'synthetic loopback fixture; no real player'},indent=2))
finally:
 if p and p.poll() is None:p.kill();p.wait()
 shutil.move(s/'plugins',r/'generated-plugins');shutil.move(backup,s/'plugins');(s/'server.properties').write_bytes(props)
 print('Fixture restored; diagnostic process stopped')
print((r/'smoke-result.json').read_text())
