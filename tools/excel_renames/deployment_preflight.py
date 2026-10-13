#!/usr/bin/env python3
"""Read-only deployment preflight for an exact local plugins-directory snapshot.

Plan JSON: {"schema": 1, "plugins": [{"old_name": "NoPhantoms",
"baseline_sha256": "...", "candidate_path": "/.../ExcelNoPhantoms-1.0.0-excel.1.jar",
"candidate_sha256": "..."}, ... eight entries ...]}.

This does not stop/start a server, upload, rename, remove, or copy anything.
"""
import argparse
import json
from pathlib import Path
from rename_plugins import VERSIONS, Refusal, descriptor, read_regular, require, safe_archive, sha256, validate_hash, transform_bytes
from menujoin_namespace import patch_menujoin


def tree_receipt(root):
    require(root.is_dir() and not root.is_symlink(), f'Unsafe config directory: {root}')
    result = []
    folded = set()
    for path in sorted(root.rglob('*')):
        rel = path.relative_to(root).as_posix()
        require(not path.is_symlink(), f'Symlink in config directory: {path}')
        require(path.is_dir() or path.is_file(), f'Special file in config directory: {path}')
        require(rel.casefold() not in folded, f'Case collision in config directory: {path}')
        folded.add(rel.casefold())
        if path.is_file():
            data = path.read_bytes()
            result.append({'path': rel, 'type': 'file', 'size': len(data), 'sha256': sha256(data)})
        else:
            result.append({'path': rel, 'type': 'directory'})
    return result


def preflight(plugins_dir, plan):
    directory = Path(plugins_dir)
    require(directory.is_dir() and not directory.is_symlink(), 'Need a local nonsymlink plugins directory snapshot')
    require(isinstance(plan, dict) and plan.get('schema') == 1 and isinstance(plan.get('plugins'), list), 'Bad plan schema')
    items = plan['plugins']
    require(len(items) == 8 and {x.get('old_name') for x in items} == set(VERSIONS), 'Plan must cover each of the exact eight plugins once')
    inventories = []
    update_paths = [directory / 'update'] + [Path(x) for x in plan.get('additional_update_directories', [])]
    for update in update_paths:
        require(not update.is_symlink(), f'Symlink update directory: {update}')
        if update.exists():
            require(update.is_dir(), f'Invalid update directory: {update}')
            pending = list(update.rglob('*'))
            require(all(not p.is_symlink() for p in pending), f'Symlink within update directory: {update}')
            require(not any(p.is_file() and p.suffix.casefold() == '.jar' for p in pending), f'Pending update JARs can replace candidates at startup: {update}')
    paths = list(directory.iterdir())
    require(len({p.name.casefold() for p in paths}) == len(paths), 'Case-colliding plugins directory entries')
    require(all(not p.is_symlink() for p in paths), 'Symlink in plugins directory requires separate review')
    jar_scan_paths = list(paths)
    for extra in plan.get('additional_plugin_directories', []):
        extra = Path(extra)
        require(extra.is_dir() and not extra.is_symlink(), f'Unsafe additional plugin directory: {extra}')
        require(extra.resolve() != directory.resolve(), 'Repeated plugin scan directory')
        children = list(extra.iterdir())
        require(all(not p.is_symlink() for p in children), f'Symlink in additional plugin directory: {extra}')
        require(len({p.name.casefold() for p in children}) == len(children), f'Case-colliding additional plugin directory entries: {extra}')
        jar_scan_paths.extend(children)
    for path in sorted(jar_scan_paths):
        if not path.name.lower().endswith('.jar'):
            continue
        data = read_regular(path)
        _, _, payloads = safe_archive(data)
        _, meta, _ = descriptor(payloads['plugin.yml'])
        claims = [meta['name']] + meta.get('provides', [])
        inventories.append({'path': path, 'sha256': sha256(data), 'meta': meta, 'claims': claims, 'bytes': data})
    actions = []
    content_checks = []
    for item in items:
        old = item['old_name']; new = 'Excel' + old
        validate_hash(item.get('baseline_sha256'))
        validate_hash(item.get('candidate_sha256'))
        related = [x for x in inventories if any(c.casefold() in (old.casefold(), new.casefold()) for c in x['claims']) or any(resembles(x['path'].name, n) for n in (old, new))]
        require(len(related) == 1, f'{old}: expected exactly one old JAR and no old/new duplicates; found {[str(x["path"]) for x in related]}')
        baseline = related[0]
        require(baseline['path'].resolve().parent == directory.resolve(), f'{old}: baseline found outside primary plugins directory; config-root mapping is not authorized')
        is_original_bedrock = old == 'BedrockMenu' and baseline['meta']['version'] == '1.4.2'
        baseline_version = '1.4.2' if is_original_bedrock else VERSIONS[old]
        require(baseline['meta']['name'] == old and baseline['meta']['version'] == baseline_version, f'{old}: unexpected live descriptor')
        if is_original_bedrock:
            require(baseline['sha256'] == '68eb426b83f827a9fbf92f83cac4f4922f9904ba3aafc2dc9c1a0f27667fc841', 'BedrockMenu: original live JAR must match separately verified provenance')
        require(baseline['sha256'] == item['baseline_sha256'], f'{old}: live baseline hash changed')
        candidate = Path(item['candidate_path'])
        data = read_regular(candidate)
        require(sha256(data) == item['candidate_sha256'], f'{old}: candidate hash mismatch')
        _, _, payloads = safe_archive(data)
        _, meta, _ = descriptor(payloads['plugin.yml'])
        require(meta['name'] == new and meta['version'] == VERSIONS[old] + '-excel.1', f'{old}: candidate descriptor mismatch')
        require(old in meta.get('provides', []), f'{old}: compatibility provides missing')
        expected_meta = dict(baseline['meta'], name=new, version=VERSIONS[old] + '-excel.1', provides=baseline['meta'].get('provides', []) + [old])
        if is_original_bedrock:
            require(item['candidate_sha256'] == 'b84f9fc0f118c330a9e8c9fd807660db6016fedd834a241a6242b1b4b26a0b33', 'BedrockMenu: candidate does not match verified menu-removal plus rename chain')
            expected_meta['description'] = '/menu that works on Bedrock via native Geyser forms. Removes the redundant Jobs & Money menu entry.'
        require(meta == expected_meta, f'{old}: candidate descriptor changes beyond allowed fields')
        require(candidate.name == f'{new}-{meta["version"]}.jar', f'{old}: candidate filename mismatch')
        require(candidate.resolve().parent != directory.resolve(), f'{old}: candidate is already inside live directory')
        targets = [p for p in paths if p.name.casefold() == new.casefold() or p.name.casefold() == candidate.name.casefold()]
        require(not targets, f'{old}: target JAR/config destination already exists: {targets}')
        legacy = directory / old
        near = [p for p in paths if p.name.casefold() == old.casefold()]
        require(not near or near == [legacy], f'{old}: non-exact legacy config case')
        config = tree_receipt(legacy) if legacy.exists() else None
        content_checks.append((old, baseline, data, is_original_bedrock))
        actions.append({
            'old_name': old, 'new_name': new,
            'includes_prior_menu_removal': is_original_bedrock,
            'baseline_path': str(baseline['path']), 'baseline_sha256': baseline['sha256'],
            'candidate_path': str(candidate), 'candidate_sha256': item['candidate_sha256'],
            'install_destination': str(directory / candidate.name),
            'config_source': str(legacy) if config is not None else None,
            'config_destination': str(directory / new) if config is not None else None,
            'config_snapshot': config,
            'required_operations': ['Stop server and confirm fully stopped', 'Back up original JAR and entire original data directory outside plugins', 'Recheck all hashes and target absence immediately before mutation', 'Remove old JAR from plugins and install the single candidate', 'Copy entire old data directory to new name only if present, verify every file hash, and retain original unchanged', 'Start server and verify runtime identity, dependencies, commands and persistence'],
        })
    for old, baseline, candidate_data, is_original_bedrock in content_checks:
        if is_original_bedrock:
            # Both complete JAR hashes were pinned above to the separately reviewed lineage.
            continue
        expected_bytes, _ = transform_bytes(baseline['bytes'], baseline['sha256'], old, patch_menujoin if old == 'MenuJoin' else None)
        require(candidate_data == expected_bytes, f'{old}: candidate payload/archive differs from reproducible approved transformation')
    return {'schema': 1, 'status': 'offline_snapshot_preflight_passed', 'plugins_directory': str(directory), 'actions': actions,
            'safety': 'Read-only check; does not authorize deployment. Snapshot must be current. Stop server and recheck before changing files. Keep exact baseline artifacts and configuration snapshots for rollback.',
            'unverified': ['Actual Paper provides dependency/name lookup behavior', 'Live runtime/gameplay', 'Server stopped state and remote filesystem snapshot freshness', 'Custom update/plugin scan directories omitted from the plan are not observable; include every server-configured directory']}


def resembles(filename, name):
    low, prefix = filename.casefold(), name.casefold()
    return low == prefix + '.jar' or low.startswith(prefix + '-') and low.endswith('.jar')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--plugins-dir', required=True, type=Path)
    p.add_argument('--plan', required=True, type=Path)
    args = p.parse_args()
    try:
        plan = json.loads(args.plan.read_text())
        print(json.dumps(preflight(args.plugins_dir, plan), indent=2, sort_keys=True))
    except (Refusal, OSError, ValueError, KeyError, TypeError) as e:
        p.exit(2, f'REFUSED: {e}\n')

if __name__ == '__main__':
    main()
