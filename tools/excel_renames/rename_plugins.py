#!/usr/bin/env python3
"""Offline, hash-pinned runtime-name changes. Never deploys or modifies inputs."""
import argparse
import copy
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import stat
import zipfile
import yaml

VERSIONS = {
    'BedrockMenu': '1.4.2-menu.1', 'MenuJoin': '1.2.0', 'WeekendXP': '1.0.2',
    'NoPhantoms': '1.0.0', 'RestartCountdown': '1.1.0', 'Leaderboard': '1.0.0',
    'WealthBoard': '1.3.0', 'ShopVillager': '1.0.0',
}
MAX_ARCHIVE = 64 * 1024 * 1024
MAX_ENTRY = 32 * 1024 * 1024
MAX_TOTAL = 128 * 1024 * 1024

class Refusal(ValueError):
    pass

def require(condition, message):
    if not condition:
        raise Refusal(message)

def sha256(data):
    return hashlib.sha256(data).hexdigest()

class UniqueLoader(yaml.SafeLoader):
    pass

def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        require(isinstance(key_node, yaml.ScalarNode) and key_node.tag == 'tag:yaml.org,2002:str', 'Non-string YAML key')
        key = loader.construct_object(key_node, deep=deep)
        require(key not in result, f'Duplicate YAML key: {key}')
        result[key] = loader.construct_object(value_node, deep=deep)
    return result
UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)

def descriptor(data):
    require(len(data) < 1024 * 1024, 'Oversized plugin.yml')
    try:
        text = data.decode('utf-8')
        require(not text.startswith('\ufeff'), 'BOM descriptor is unsupported')
        for token in yaml.scan(text):
            require(not isinstance(token, (yaml.AliasToken, yaml.AnchorToken, yaml.TagToken)), 'YAML aliases/anchors/tags are unsupported')
        obj = yaml.load(text, Loader=UniqueLoader)
        node = yaml.compose(text, Loader=UniqueLoader)
    except (UnicodeError, yaml.YAMLError) as e:
        raise Refusal(f'Invalid plugin.yml: {e}') from e
    require(isinstance(obj, dict) and isinstance(node, yaml.MappingNode), 'plugin.yml must be one mapping')
    for key in ('name', 'version', 'main'):
        require(isinstance(obj.get(key), str) and obj[key], f'Missing/string-only descriptor key: {key}')
    require(re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.-]*', obj['name']), 'Unsafe plugin name')
    require(re.fullmatch(r'[A-Za-z0-9_$]+(?:\.[A-Za-z0-9_$]+)+', obj['main']), 'Unsafe main class')
    if 'provides' in obj:
        require(isinstance(obj['provides'], list) and all(isinstance(x, str) and re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.-]*', x) for x in obj['provides']), 'provides must be a string list')
        require(len({x.casefold() for x in obj['provides']}) == len(obj['provides']), 'Duplicate provides aliases')
    return text, obj, node

def safe_archive(data):
    require(len(data) <= MAX_ARCHIVE, 'Oversized archive')
    try:
        z = zipfile.ZipFile(io.BytesIO(data))
        infos = z.infolist()
        require(infos and len(infos) <= 10000, 'Empty or excessive archive entries')
        names, folded, total, payloads = set(), set(), 0, {}
        for i in infos:
            name = i.filename
            require(i.orig_filename == name and '\x00' not in i.orig_filename, 'NUL/truncated ZIP path')
            path = PurePosixPath(name)
            require(name and '\\' not in name and '\x00' not in name and ':' not in name and not path.is_absolute(), f'Unsafe ZIP path: {name!r}')
            parts = name.rstrip('/').split('/')
            require(all(p not in ('', '.', '..') for p in parts), f'Unsafe ZIP path: {name!r}')
            require(name not in names and name.casefold() not in folded, f'Duplicate/case-colliding ZIP entry: {name}')
            names.add(name); folded.add(name.casefold())
            require(not i.flag_bits & 1, 'Encrypted ZIP entry')
            mode = i.external_attr >> 16
            require(stat.S_IFMT(mode) in (0, stat.S_IFREG, stat.S_IFDIR), f'Special ZIP entry: {name}')
            require(i.compress_type in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED), f'Unexpected compression: {name}')
            require(i.file_size <= MAX_ENTRY, f'Oversized entry: {name}')
            total += i.file_size
            require(total <= MAX_TOTAL, 'Excessive expanded archive size')
            upper = name.upper()
            require(not (upper.startswith('META-INF/') and (upper.endswith(('.SF', '.RSA', '.DSA', '.EC')) or upper.startswith('META-INF/SIG-'))), 'Signed JAR requires separate review')
            require(upper not in ('META-INF/INDEX.LIST', 'PAPER-PLUGIN.YML', 'BUNGEE.YML', 'VELOCITY-PLUGIN.JSON') and not upper.startswith('META-INF/VERSIONS/'), 'Unexpected loader/index/multi-release metadata')
            payloads[name] = z.read(i)
            require(not i.is_dir() or payloads[name] == b'', 'Nonempty ZIP directory')
        require('plugin.yml' in names, 'Missing exact plugin.yml')
        manifest = payloads.get('META-INF/MANIFEST.MF', b'')
        require(not re.search(rb'(?im)^[-A-Za-z0-9_]*Digest[-A-Za-z0-9_]*\s*:', manifest), 'Manifest digest/signature metadata requires separate review')
        return z, infos, payloads
    except (zipfile.BadZipFile, NotImplementedError, RuntimeError) as e:
        raise Refusal(f'Invalid ZIP: {e}') from e

def rewrite_descriptor(data, old_name):
    require(old_name in VERSIONS, 'Plugin is outside the exact eight-plugin scope')
    text, before, root = descriptor(data)
    new_name, new_version = 'Excel' + old_name, VERSIONS[old_name] + '-excel.1'
    require(before['name'] == old_name, f'Expected runtime name {old_name}, got {before["name"]}')
    require(before['version'] == VERSIONS[old_name], f'Unexpected baseline version {before["version"]}')
    aliases = before.get('provides', [])
    require(not any(x.casefold() in (old_name.casefold(), new_name.casefold()) for x in aliases), 'Old/new compatibility alias already present')
    spans = {key.value: value for key, value in root.value}
    edits = []
    for key, val in (('name', new_name), ('version', new_version)):
        node = spans[key]
        require(isinstance(node, yaml.ScalarNode) and node.style not in ('|', '>'), f'Unexpected {key} syntax')
        edits.append((node.start_mark.index, node.end_mark.index, val))
    if 'provides' in spans:
        node = spans['provides']
        key_node = next(k for k, v in root.value if k.value == 'provides')
        original = text[key_node.start_mark.index:node.end_mark.index]
        ending = '\r\n' if original.endswith('\r\n') else '\n' if original.endswith('\n') else ''
        edits.append((key_node.start_mark.index, node.end_mark.index, 'provides: [' + ', '.join(aliases + [old_name]) + ']' + ending))
    for a, b, replacement in sorted(edits, reverse=True):
        text = text[:a] + replacement + text[b:]
    if 'provides' not in spans:
        newline = '\r\n' if '\r\n' in text else '\n'
        text = text.rstrip('\r\n') + newline + 'provides: [' + old_name + ']' + newline
    result = text.encode('utf-8')
    _, after, _ = descriptor(result)
    expected = dict(before, name=new_name, version=new_version, provides=aliases + [old_name])
    require(after == expected, 'Descriptor changed outside name/version/provides')
    return result, before, after

def read_regular(path):
    path = Path(path)
    require(not path.is_symlink() and path.is_file(), f'Not a regular nonsymlink input: {path}')
    require(path.stat().st_size <= MAX_ARCHIVE, 'Oversized input')
    return path.read_bytes()

def validate_hash(value):
    require(isinstance(value, str) and re.fullmatch('[0-9a-f]{64}', value), 'Exact lowercase SHA-256 required')

def transform_bytes(data, expected_sha256, old_name, menujoin_patcher=None):
    validate_hash(expected_sha256)
    require(sha256(data) == expected_sha256, 'Input SHA-256 mismatch')
    z, infos, payloads = safe_archive(data)
    changed_yml, before, after = rewrite_descriptor(payloads['plugin.yml'], old_name)
    main_entry = before['main'].replace('.', '/') + '.class'
    require(main_entry in payloads, 'Declared main class missing')
    replacements = {'plugin.yml': changed_yml}
    patch_report = None
    if old_name == 'MenuJoin':
        require(menujoin_patcher is not None, 'MenuJoin must include separately verified fixed-namespace bytecode patch')
        replacement, patch_report = menujoin_patcher(payloads[main_entry])
        require(replacement != payloads[main_entry], 'MenuJoin namespace patch did not change class')
        replacements[main_entry] = replacement
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w') as target:
        target.comment = z.comment
        for info in infos:
            target.writestr(copy.copy(info), replacements.get(info.filename, payloads[info.filename]))
    candidate = output.getvalue()
    _, out_infos, out_payloads = safe_archive(candidate)
    require([x.filename for x in out_infos] == [x.filename for x in infos], 'Archive entries/order changed')
    changed = [n for n in payloads if payloads[n] != out_payloads[n]]
    require(set(changed) == set(replacements), 'Unexpected payload changes')
    for i, oi in zip(infos, out_infos):
        for key in ('date_time', 'compress_type', 'comment', 'extra', 'create_system', 'create_version', 'extract_version', 'internal_attr', 'external_attr'):
            require(getattr(i, key) == getattr(oi, key), f'Unexpected ZIP metadata change: {i.filename}/{key}')
    audit = {
        'schema': 1, 'old_name': old_name, 'new_name': after['name'],
        'old_version': before['version'], 'new_version': after['version'],
        'input_sha256': expected_sha256, 'output_sha256': sha256(candidate),
        'output_filename': f'{after["name"]}-{after["version"]}.jar',
        'changed_entries': changed, 'unchanged_entry_count': len(infos) - len(changed),
        'descriptor_before': before, 'descriptor_after': after,
        'entries': [{ 'name': n, 'input_sha256': sha256(payloads[n]), 'output_sha256': sha256(out_payloads[n]), 'input_size': len(payloads[n]), 'output_size': len(out_payloads[n]) } for n in payloads],
        'menujoin_namespace_patch': patch_report,
        'runtime_verification': 'Not performed by this offline transformer; Paper provides resolution and live gameplay remain separate checks.',
    }
    return candidate, audit

def write_exclusive(path, data):
    path = Path(path)
    require(not path.exists() and not path.is_symlink(), f'Refusing to overwrite output: {path}')
    with open(path, 'xb') as f:
        f.write(data)

def transform_file(input_path, expected_sha256, old_name, output_dir, menujoin_patcher=None):
    data = read_regular(input_path)
    candidate, audit = transform_bytes(data, expected_sha256, old_name, menujoin_patcher)
    output_dir = Path(output_dir)
    require(output_dir.is_dir() and not output_dir.is_symlink(), 'Output directory must exist and be nonsymlink')
    output = output_dir / audit['output_filename']
    audit_path = output.with_suffix('.audit.json')
    require(not output.exists() and not audit_path.exists(), 'Output/audit already exists')
    # A second check makes accidental source mutation during the read explicit.
    require(sha256(read_regular(input_path)) == expected_sha256, 'Baseline changed during transformation')
    write_exclusive(output, candidate)
    write_exclusive(audit_path, (json.dumps(audit, indent=2, sort_keys=True) + '\n').encode())
    require(sha256(read_regular(output)) == audit['output_sha256'], 'Written output failed hash check')
    require(sha256(read_regular(input_path)) == expected_sha256, 'Baseline changed after transformation')
    return output, audit

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--input', required=True, type=Path)
    p.add_argument('--sha256', required=True)
    p.add_argument('--old-name', required=True, choices=VERSIONS)
    p.add_argument('--output-dir', required=True, type=Path)
    args = p.parse_args()
    try:
        patcher = None
        if args.old_name == 'MenuJoin':
            from menujoin_namespace import patch_menujoin
            patcher = patch_menujoin
        output, audit = transform_file(args.input, args.sha256, args.old_name, args.output_dir, patcher)
        print(json.dumps({'output': str(output), 'sha256': audit['output_sha256'], 'changed_entries': audit['changed_entries']}, indent=2))
    except (Refusal, OSError) as e:
        p.exit(2, f'REFUSED: {e}\n')

if __name__ == '__main__':
    main()
