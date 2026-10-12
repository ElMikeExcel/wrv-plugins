#!/usr/bin/env python3
"""Build deterministic guarded third-party binary and corresponding-source ZIP."""
from pathlib import Path
import hashlib
import difflib
import io
import json
import re
import tarfile
import zipfile

BASE = Path(__file__).resolve().parent
ORIGINAL_SHA = 'b31a40e9561d399d8b04fada4e863ae28c523618976ff3a3b1a6f16fe3aa8ab2'
SOURCE_SHA = '00f15e49bedd11c8300dd4ffaee6a783b92b26af39f5ba00a7683814c7564501'
REVISION = 'dde7ee2155303714c2d5054deeccef4618e3939c'
STAMP = (2026, 10, 12, 3, 0, 0)


def verified(path, expected):
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != expected:
        raise SystemExit(f'Checksum mismatch: {path.name}')
    return data


def write_zip(path, files):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(files.items()):
            info = zipfile.ZipInfo(name, STAMP)
            info.create_system = 3
            info.external_attr = (0o100644 << 16)
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)


def main():
    original = verified(BASE / '.cache/ChestShop-3.13-pre-1-build474.jar', ORIGINAL_SHA)
    source = verified(BASE / '.cache/upstream-source.tar.gz', SOURCE_SHA)
    classes = BASE / 'target/classes'
    if not (classes / 'com/Acrobot/ChestShop/Confirmation/ConfirmationGate.class').is_file():
        raise SystemExit('Run bootstrap.sh and the Maven build first')
    for source_file in (BASE / 'src').rglob('*.java'):
        compiled = classes / source_file.relative_to(BASE / 'src').with_suffix('.class')
        if not compiled.is_file() or compiled.stat().st_mtime_ns < source_file.stat().st_mtime_ns:
            raise SystemExit(f'Uncompiled source change: {source_file.relative_to(BASE)}. Rebuild first.')
    with zipfile.ZipFile(io.BytesIO(original)) as archive:
        files = {entry.filename: archive.read(entry) for entry in archive.infolist() if not entry.is_dir()}
    # Repacking invalidates signatures. Refuse a newly signed upstream rather than strip signatures silently.
    if any(n.upper().startswith('META-INF/') and n.upper().endswith(('.SF', '.RSA', '.DSA')) for n in files):
        raise SystemExit('Unexpected signed upstream JAR; needs review')
    for path in sorted(classes.rglob('*.class')):
        files[path.relative_to(classes).as_posix()] = path.read_bytes()
    original_metadata = files['plugin.yml'].decode()
    if "version: '3.13-pre-1 (build 474)'" not in original_metadata:
        raise SystemExit('Unexpected upstream plugin version')
    files['plugin.yml'] = original_metadata.replace("version: '3.13-pre-1 (build 474)'", "version: '3.13-pre-1 (build 474)+confirmation.1'").encode()
    for name in ('NOTICE', 'COPYING.LESSER'):
        files['META-INF/confirmation-guard/' + name] = (BASE / name).read_bytes()
    manifest = {
        'build_id': 'chestshop474-confirmation.1', 'api_version': 1,
        'upstream_sha256': ORIGINAL_SHA, 'upstream_revision': REVISION,
        'upstream_source_sha256': SOURCE_SHA,
        'overlay_sources': {p.relative_to(BASE).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                            for p in sorted((BASE / 'src').rglob('*.java'))},
        'deployment': 'Replace the original ChestShop JAR, retain all ChestShop data; pair with ExcelShops.',
    }
    files['META-INF/confirmation-guard/build.json'] = (json.dumps(manifest, indent=2, sort_keys=True) + '\n').encode()
    out = BASE / 'target/ChestShop-3.13-pre-1-build474-confirmation.1.jar'
    write_zip(out, files)

    sources = {}
    with tarfile.open(fileobj=io.BytesIO(source), mode='r:gz') as archive:
        for member in archive.getmembers():
            if not member.isfile():
                continue
            relative = Path(member.name).relative_to('ChestShop-3-' + REVISION)
            if '..' in relative.parts:
                raise SystemExit('Unsafe upstream archive member')
            sources['ChestShop-upstream/' + relative.as_posix()] = archive.extractfile(member).read()
    # Keep a directly applicable patch in addition to the full modified sources.
    patch = []
    for changed in sorted((BASE / 'src').rglob('*.java')):
        relative = 'plugin/src/main/java/' + changed.relative_to(BASE / 'src').as_posix()
        original_text = sources.get('ChestShop-upstream/' + relative, b'').decode()
        patch.extend(difflib.unified_diff(original_text.splitlines(keepends=True),
                     changed.read_text().splitlines(keepends=True),
                     fromfile='a/' + relative if original_text else '/dev/null', tofile='b/' + relative))
    sources['ChestShop-confirmation.patch'] = ''.join(patch).encode()
    (BASE / 'target/ChestShop-confirmation.patch').write_bytes(sources['ChestShop-confirmation.patch'])
    # Complete corresponding upstream source plus modified overlay, tests and build scripts.
    for path in sorted(BASE.rglob('*')):
        if not path.is_file() or any(part in ('.cache', 'target', '__pycache__') for part in path.relative_to(BASE).parts):
            continue
        sources['ChestShop-ConfirmationGuard/' + path.relative_to(BASE).as_posix()] = path.read_bytes()
    parent_pom = (BASE.parent / 'pom.xml').read_bytes()
    sources['parent-pom.xml'] = parent_pom
    sources['pom.xml'] = re.sub(rb'<modules>.*?</modules>', b'<modules><module>ChestShop-ConfirmationGuard</module></modules>', parent_pom, flags=re.S)
    sources['BUILDING-GUARD.txt'] = b'With JDK 21+, chmod +x mvnw ChestShop-ConfirmationGuard/bootstrap.sh, then run ./ChestShop-ConfirmationGuard/bootstrap.sh, ./mvnw -f ChestShop-ConfirmationGuard/pom.xml clean verify, and python3 ChestShop-ConfirmationGuard/package.py. The other repository modules are not required to rebuild this guard.\n'
    sources['mvnw'] = (BASE.parent / 'mvnw').read_bytes()
    source_out = BASE / 'target/ChestShop-3.13-pre-1-build474-confirmation.1-sources.zip'
    write_zip(source_out, sources)
    result = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (out, source_out)}
    (BASE / 'target/guarded-artifacts.json').write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    print(json.dumps(result, indent=2, sort_keys=True))


if __name__ == '__main__':
    main()
