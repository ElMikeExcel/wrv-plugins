#!/usr/bin/env python3
"""Build a hash-indexed checkpoint, or verify bytes downloaded from durable Library."""
import argparse,hashlib,json,pathlib,zipfile
p=argparse.ArgumentParser();s=p.add_subparsers(dest='operation',required=True)
a=s.add_parser('snapshot');a.add_argument('output');a.add_argument('--label',required=True);a.add_argument('--include-jar',action='store_true')
a=s.add_parser('verify-download');a.add_argument('file');a.add_argument('expected_sha256');a.add_argument('--library-id',required=True);a.add_argument('--receipt',required=True)
a=p.parse_args();root=pathlib.Path(__file__).resolve().parent
sha=lambda b:hashlib.sha256(b).hexdigest()
if a.operation=='snapshot':
    output=pathlib.Path(a.output).resolve();output.parent.mkdir(parents=True,exist_ok=True)
    records=[];payload=[]
    for path in sorted(root.rglob('*')):
        if not path.is_file() or path==output:continue
        rel=path.relative_to(root)
        if '.git' in rel.parts or 'target' in rel.parts or 'durable-copies' in rel.parts:continue
        if path.suffix in {'.zip','.log'}:continue
        data=path.read_bytes();payload.append((str(pathlib.Path('ExcelMobVeil')/rel),data));records.append({'path':str(rel),'sha256':sha(data),'bytes':len(data)})
    if a.include_jar:
        path=root/'target/ExcelMobVeil-0.1.0.jar';data=path.read_bytes();payload.append(('ExcelMobVeil/target/'+path.name,data));records.append({'path':'target/'+path.name,'sha256':sha(data),'bytes':len(data)})
    payload.append(('CHECKPOINT.json',json.dumps({'label':a.label,'files':records,'warning':'A checkpoint is not deployment approval. Use only the final release receipt and verified downloaded bytes.'},indent=2).encode()))
    with zipfile.ZipFile(output,'w',zipfile.ZIP_DEFLATED) as z:
        for name,data in payload:z.writestr(name,data)
    with zipfile.ZipFile(output) as z:assert z.testzip() is None
    print(json.dumps({'path':str(output),'sha256':sha(output.read_bytes()),'bytes':output.stat().st_size,'files':len(payload)}))
else:
    path=pathlib.Path(a.file);actual=sha(path.read_bytes())
    if actual!=a.expected_sha256:raise SystemExit('HASH MISMATCH: stop publication/installation')
    receipt={'library_file_id':a.library_id,'local_download':str(path.resolve()),'expected_sha256':a.expected_sha256,'downloaded_sha256':actual,'verified':True}
    pathlib.Path(a.receipt).write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt))
