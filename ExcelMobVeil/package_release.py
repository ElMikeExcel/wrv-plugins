#!/usr/bin/env python3
"""Package only a fresh passing artifact. Save the result to Library, then verify its readback."""
import argparse,hashlib,json,pathlib,re,zipfile,xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--runtime-dir',required=True);p.add_argument('--build-log',required=True);p.add_argument('--expected-jar-sha256',required=True);p.add_argument('--output-dir',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parent;runtime=pathlib.Path(a.runtime_dir).resolve();out=pathlib.Path(a.output_dir).resolve();out.mkdir(parents=True,exist_ok=True)
sha=lambda b:hashlib.sha256(b).hexdigest()
jar=root/'target/ExcelMobVeil-0.1.0.jar';jh=sha(jar.read_bytes());assert jh==a.expected_jar_sha256,'Unexpected JAR; reverify before release'
modes=['stacking','edges','normal','purchase','lifecycle','restart','no-economy','review-races','coexist','soak'];runs=[]
for mode in modes:
 r=json.loads((runtime/f'results-{mode}.json').read_text());assert r['candidate_sha256']==jh,(mode,'Different JAR')
 assert r['result'].startswith('PASS') and r['exit_code']==0 and r['port_released'],(mode,'Not a clean passing run')
 assert r.get('new_eula_acceptance') is False,(mode,'Unexpected agreement change')
 runs.append(r)
unit=[]
for f in sorted((root/'target/surefire-reports').glob('TEST-*.xml')):
 t=ET.parse(f).getroot();assert all(int(t.attrib[k])==0 for k in ['failures','errors','skipped'])
 unit.append({k:t.attrib[k] for k in ['name','tests','failures','errors','skipped']})
assert unit and sum(int(t['tests']) for t in unit)>0,'Missing unit evidence'
review=root/'independent-review.md';assert review.exists() and jh in review.read_text(),'Missing artifact-bound independent review'
assert (root/'VERIFICATION.md').exists() and jh in (root/'VERIFICATION.md').read_text(),'Missing artifact-bound verification report'
manifest=[];payload=[]
for f in sorted(root.rglob('*')):
 if not f.is_file():continue
 rel=f.relative_to(root)
 if any(x in rel.parts for x in ['target','.git','durable-copies']):continue
 if f.suffix in {'.zip','.log'}:continue
 b=f.read_bytes();payload.append(('ExcelMobVeil/'+str(rel),b));manifest.append({'path':str(rel),'sha256':sha(b),'bytes':len(b)})
payload.append(('ExcelMobVeil/target/'+jar.name,jar.read_bytes()))
results={'candidate_sha256':jh,'unit_tests':unit,'runtime_modes':runs,'unit_total':sum(int(t['tests']) for t in unit),'runtime_assertions':sum(int(re.search(r'PASS (\d+)',r['result']).group(1)) for r in runs)}
payload.extend([('SOURCE-MANIFEST.json',json.dumps(manifest,indent=2).encode()),('TEST-RESULTS.json',json.dumps(results,indent=2).encode()),('evidence/build-tests.log',pathlib.Path(a.build_log).read_bytes())])
for mode in modes:
 for f in [runtime/f'results-{mode}.json',runtime/'server'/f'assertions-{mode}.txt',runtime/'server'/f'runtime-{mode}.log']:
  assert f.exists(),str(f);payload.append(('evidence/runtime/'+f.name,f.read_bytes()))
for f in sorted((runtime/'src').rglob('*.java')):payload.append(('verification/runtime/'+str(f.relative_to(runtime)),f.read_bytes()))
for name in ['README.md','build_fixture.sh','run_fixture.py','fixture-provenance.json','VERIFICATION-REPORT.md','results-final.json']:
 f=runtime/name
 if f.exists():payload.append(('verification/runtime/'+name,f.read_bytes()))
checksums=''.join(sha(b)+'  '+name+'\n' for name,b in payload)
payload.append(('FILES.sha256',checksums.encode()))
zip_path=out/'ExcelMobVeil-0.1.0-VERIFIED-REBUILD.zip'
with zipfile.ZipFile(zip_path,'w',zipfile.ZIP_DEFLATED) as z:
 for name,b in payload:
  info=zipfile.ZipInfo(name,(2026,10,14,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,b)
with zipfile.ZipFile(zip_path) as z:
 assert z.testzip() is None
 for line in z.read('FILES.sha256').decode().splitlines():
  h,name=line.split('  ',1);assert sha(z.read(name))==h,name
(out/jar.name).write_bytes(jar.read_bytes())
(out/'SHA256SUMS.txt').write_text(jh+'  '+jar.name+'\n'+sha(zip_path.read_bytes())+'  '+zip_path.name+'\n')
(out/'TEST-RESULTS.json').write_text(json.dumps(results,indent=2)+'\n')
print(json.dumps({'jar_sha256':jh,'zip':str(zip_path),'zip_sha256':sha(zip_path.read_bytes()),'zip_bytes':zip_path.stat().st_size,'unit_tests':results['unit_total'],'runtime_assertions':results['runtime_assertions'],'durability_status':'Not ready until saved to Library and downloaded/hash verified'}))
