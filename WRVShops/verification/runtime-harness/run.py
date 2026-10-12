#!/usr/bin/env python3
"""Compile and run disposable exact-Paper ExcelShops integration checks.

No download, EULA acceptance, live server access, or real economy is performed.
Supply --legacy-plugin to verify the complete original-plugin migration sequence.
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import zipfile

PAPER_SHA256 = '7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08'
ANNOTATIONS_SHA256 = '2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297'
VAULT_SHA256 = '47f6a400a8ab22012e6a320f7bfc63621ce4a246e1c60502ff7fc91d532d2601'
LEGACY_SHA256 = '16de2e1477f561987717412b4c338eda136cd1025fad85e3c692ad29fc663aaa'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tree_hashes(path):
    if path.is_symlink():
        raise RuntimeError('Disposable fixture unexpectedly contains a symlink: ' + str(path))
    if not path.exists():
        return {'.': {'kind': 'absent'}}
    result = {}
    for entry in [path, *sorted(path.rglob('*'))]:
        if entry.is_symlink():
            raise RuntimeError('Disposable fixture unexpectedly contains a symlink: ' + str(entry))
        key = entry.relative_to(path).as_posix()
        result[key] = {'kind': 'directory' if entry.is_dir() else 'file',
                       'sha256': digest(entry) if entry.is_file() else None,
                       'mode': oct(entry.stat().st_mode & 0o777)}
    return result


def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#') and '=' in line)


def verify_port_free():
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 25579))


def stop_child(process):
    if process.poll() is not None:
        return
    try:
        process.stdin.write('stop\n')
        process.stdin.flush()
    except (BrokenPipeError, OSError):
        pass
    try:
        process.wait(timeout=45)
    except subprocess.TimeoutExpired:
        process.terminate()
        try:
            process.wait(timeout=30)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=30)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-dir', required=True, type=Path)
    parser.add_argument('--plugin', required=True, type=Path)
    parser.add_argument('--legacy-plugin', type=Path, help='Exact original WRVShops 0.1.0 candidate; adds clean/dirty/conflict migration checks')
    parser.add_argument('--vault-api', required=True, type=Path)
    parser.add_argument('--annotations-jar', required=True, type=Path)
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'), type=Path)
    parser.add_argument('--boots', default=2, type=int, choices=[1, 2], help='1: first-boot smoke only; 2: full boot/restart/failure-mode suite')
    parser.add_argument('--only-no-vault', action='store_true', help='Debug existing persisted fixture with only the missing-Vault probe')
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    root, plugin, vault, annotations = [p.resolve() for p in (args.server_dir, args.plugin, args.vault_api, args.annotations_jar)]
    if args.java_home is None:
        parser.error('--java-home or JAVA_HOME is required')
    java_home = args.java_home.resolve()
    for command in ['java', 'javac', 'jar']:
        if not (java_home / 'bin' / command).is_file():
            parser.error(f'Missing {command} under {java_home}')
    paper = root / 'paper-26.2-123.jar'
    for path, expected in [(paper, PAPER_SHA256), (vault, VAULT_SHA256), (annotations, ANNOTATIONS_SHA256)]:
        if not path.is_file() or digest(path) != expected:
            parser.error(f'Pinned dependency missing or checksum mismatch: {path.name}')
    if not plugin.is_file():
        parser.error('ExcelShops JAR missing')
    if args.legacy_plugin and (not args.legacy_plugin.is_file() or digest(args.legacy_plugin) != LEGACY_SHA256):
        parser.error('Exact original WRVShops migration baseline missing or checksum mismatch')
    if args.legacy_plugin and (args.boots != 2 or args.only_no_vault):
        parser.error('Migration verification requires the full suite')
    config = properties(root / 'server.properties')
    required = {'server-ip': '127.0.0.1', 'server-port': '25579', 'online-mode': 'true',
                'white-list': 'true', 'enforce-whitelist': 'true', 'enable-rcon': 'false',
                'enable-query': 'false', 'management-server-enabled': 'false', 'level-name': 'wrv-shops-harness-world'}
    for key, expected in required.items():
        if config.get(key) != expected:
            parser.error(f'Isolated config required: {key}={expected}')
    try:
        verify_port_free()
    except OSError:
        parser.error('Isolation port 25579 is already in use; no process will be touched')
    if not (root / 'eula.txt').is_file() or properties(root / 'eula.txt').get('eula') != 'true':
        parser.error('Review and accept the Minecraft EULA before running. The runner never creates acceptance.')
    libraries = sorted((root / 'libraries').rglob('*.jar'))
    if not any(p.name == 'paper-api-26.2.build.123-stable.jar' for p in libraries):
        parser.error('Extract exact official runtime with Paperclip patch-only preparation first')
    plugins = root / 'plugins'
    plugins.mkdir(exist_ok=True)
    allowed = {'WRVShops.jar', 'ExcelShops.jar', 'WRVShopsRuntimeHarness.jar', 'Vault-TestOnly.jar', 'GriefPrevention-TestOnly.jar', 'WRVShopsProtectionHarness.jar'}
    unexpected = [p.name for p in plugins.glob('*.jar') if p.name not in allowed]
    if unexpected:
        parser.error('Use a disposable server without other plugin JARs: ' + ', '.join(unexpected))
    existing_fixture = any((plugins / name).exists() for name in ['WRVShops', 'ExcelShops', 'WRVShopsRuntimeHarness'])
    existing_world = any(root.glob('wrv-shops-harness-world*'))
    if args.legacy_plugin and (existing_fixture or existing_world):
        parser.error('Migration suite requires a fresh disposable fixture; it never overwrites existing shop data or worlds')
    build = root / 'harness-build'
    if build.exists():
        shutil.rmtree(build)
    build.mkdir()
    vault_build = build / 'vault'
    vault_build.mkdir()
    classpath = os.pathsep.join(str(p) for p in [*libraries, annotations, vault, plugin])
    subprocess.run([str(java_home / 'bin/javac'), '--release', '25', '-cp', classpath, '-d', str(vault_build),
                    str(here / 'src/wrv/harness/FakeVault.java')], check=True)
    with zipfile.ZipFile(vault) as source:
        for entry in source.namelist():
            if entry.startswith('net/milkbowl/vault/economy/') and entry.endswith('.class'):
                path = vault_build / entry
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(source.read(entry))
    shutil.copyfile(here / 'vault-plugin.yml', vault_build / 'plugin.yml')
    vault_jar = plugins / 'Vault-TestOnly.jar'
    subprocess.run([str(java_home / 'bin/jar'), '--create', '--file', str(vault_jar), '-C', str(vault_build), '.'], check=True)
    gp_build = build / 'gp'
    gp_build.mkdir()
    subprocess.run([str(java_home / 'bin/javac'), '--release', '25', '-cp', classpath, '-d', str(gp_build),
                    *map(str, sorted((here / 'gp-src').rglob('*.java')))], check=True)
    shutil.copyfile(here / 'gp-plugin.yml', gp_build / 'plugin.yml')
    gp_jar = plugins / 'GriefPrevention-TestOnly.jar'
    subprocess.run([str(java_home / 'bin/jar'), '--create', '--file', str(gp_jar), '-C', str(gp_build), '.'], check=True)
    classpath += os.pathsep + str(gp_jar)
    harness_build = build / 'harness'
    harness_build.mkdir()
    harness_sources = [p for p in sorted((here / 'src').rglob('*.java')) if p.name != 'FakeVault.java']
    subprocess.run([str(java_home / 'bin/javac'), '--release', '25', '-cp', classpath + os.pathsep + str(vault_jar),
                    '-d', str(harness_build), *map(str, harness_sources)], check=True)
    shutil.copyfile(here / 'plugin.yml', harness_build / 'plugin.yml')
    harness = plugins / 'WRVShopsRuntimeHarness.jar'
    subprocess.run([str(java_home / 'bin/jar'), '--create', '--file', str(harness), '-C', str(harness_build), '.'], check=True)
    probe_build = build / 'probe'
    probe_build.mkdir()
    subprocess.run([str(java_home / 'bin/javac'), '--release', '25', '-cp', os.pathsep.join(str(p) for p in [*libraries, annotations]),
                    '-d', str(probe_build), str(here / 'src/wrv/harness/FakePlayer.java'),
                    str(here / 'probe-src/wrv/harness/ProtectionOnlyHarness.java')], check=True)
    shutil.copyfile(here / 'probe-plugin.yml', probe_build / 'plugin.yml')
    probe_jar = build / 'WRVShopsProtectionHarness.jar'
    subprocess.run([str(java_home / 'bin/jar'), '--create', '--file', str(probe_jar), '-C', str(probe_build), '.'], check=True)
    version = subprocess.run([str(java_home / 'bin/java'), '-version'], capture_output=True, text=True, check=True).stderr.strip()
    stages = ['verification-first'] if args.boots == 1 else ['verification-first', 'verification-restart', 'verification-no-economy', 'verification-no-vault']
    if args.only_no_vault:
        stages = ['verification-no-vault']
    if args.legacy_plugin:
        stages = ['verification-legacy-baseline', *stages, 'verification-migration-dirty', 'verification-migration-dirty-restart', 'verification-migration-conflict', 'verification-migration-conflict-restart', 'verification-migration-missing-journal', 'verification-migration-missing-journal-restart']
    baseline = build / 'legacy-baseline'
    legacy_hashes = None
    results = []
    snapshot_roots = ['plugins/WRVShops', 'plugins/WRVShopsRuntimeHarness', 'wrv-shops-harness-world', 'wrv-shops-harness-world_nether', 'wrv-shops-harness-world_the_end']
    for stage in stages:
        if stage in {'verification-migration-dirty', 'verification-migration-conflict', 'verification-migration-missing-journal'}:
            for name in [*snapshot_roots, 'plugins/ExcelShops']:
                target = root / name
                if target.exists():
                    shutil.rmtree(target)
                source = baseline / name
                if source.exists():
                    shutil.copytree(source, target)
            if stage == 'verification-migration-dirty':
                (plugins / 'WRVShops/checkout/active-session').write_text('Disposable synthetic unclean legacy session; no real transactions.\n')
            elif stage == 'verification-migration-missing-journal':
                (plugins / 'WRVShops/checkout/transactions.log').unlink()
            else:
                (plugins / 'ExcelShops').mkdir()
                (plugins / 'ExcelShops/config.yml').write_text('conflicting-target-fixture: preserve-exactly\n')
            legacy_hashes = tree_hashes(plugins / 'WRVShops')
        legacy = stage == 'verification-legacy-baseline'
        target = plugins / ('WRVShops.jar' if legacy else 'ExcelShops.jar')
        old_target = plugins / ('ExcelShops.jar' if legacy else 'WRVShops.jar')
        old_target.unlink(missing_ok=True)
        shutil.copyfile(args.legacy_plugin if legacy else plugin, target)
        conflict = 'migration-conflict' in stage
        dirty = 'migration-dirty' in stage
        missing_journal = 'migration-missing-journal' in stage
        startup_blocked = conflict or missing_journal
        target_before = tree_hashes(plugins / 'ExcelShops') if startup_blocked else None
        manifest = {'utc': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'stage': stage,
                    'plugin_sha256': digest(target), 'plugin_name': 'WRVShops' if legacy else 'ExcelShops',
                    'harness_sha256': digest(harness), 'fake_vault_sha256': digest(vault_jar), 'fake_gp_sha256': digest(gp_jar),
                    'vault_api_sha256': digest(vault), 'paper_sha256': digest(paper), 'java': version,
                    'listen': '127.0.0.1:25579', 'synthetic_players': True, 'real_client': False, 'real_economy': False, 'vault_plugin_present': stage != 'verification-no-vault',
                    'economy_service_present': stage not in {'verification-no-economy', 'verification-no-vault'},
                    'probe_sha256': digest(probe_jar) if stage == 'verification-no-vault' else None}
        no_vault = stage == 'verification-no-vault'
        disabled_vault, disabled_harness = build / 'disabled-Vault.jar', build / 'disabled-Harness.jar'
        active_probe = plugins / 'WRVShopsProtectionHarness.jar'
        if no_vault:
            shutil.move(vault_jar, disabled_vault)
            shutil.move(harness, disabled_harness)
            shutil.copyfile(probe_jar, active_probe)
        log = root / (stage + '-console.log')
        try:
            verify_port_free()
            with log.open('w') as output:
                flags = ['-Dwrv.test.plugin-name=' + ('WRVShops' if legacy else 'ExcelShops')]
                if stage == 'verification-no-economy':
                    flags.append('-Dwrv.test.no-economy=true')
                if dirty or startup_blocked:
                    flags.append('-Dwrv.test.migration-mode=' + ('dirty' if dirty else 'missing-journal' if missing_journal else 'conflict'))
                process = subprocess.Popen([str(java_home / 'bin/java'), *flags, '-Dterminal.jline=false', '-Dterminal.ansi=false',
                                            '-Xms384M', '-Xmx1200M', '-jar', paper.name, '--nogui'], cwd=root,
                                           stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True)
                try:
                    code = process.wait(timeout=240)
                except subprocess.TimeoutExpired:
                    stop_child(process)
                    raise RuntimeError('Timed out; isolated child process stopped. See ' + str(log))
                except BaseException:
                    stop_child(process)
                    raise
        finally:
            if no_vault:
                active_probe.unlink()
                shutil.move(disabled_vault, vault_jar)
                shutil.move(disabled_harness, harness)
        verify_port_free()
        manifest.update({'child_exit_code': code, 'loopback_25579_available_after_run': True})
        lines = [line for line in log.read_text().splitlines() if 'WRV_SHOPS_HARNESS_' in line]
        print('\n'.join(lines), flush=True)
        if code != 0 or not any('WRV_SHOPS_HARNESS_SUMMARY' in line and 'failed=0' in line for line in lines):
            raise RuntimeError('Harness failed; see ' + str(log))
        required_marker = {'verification-no-vault': 'NO_VAULT', 'verification-no-economy': 'PROTECTION_ONLY', 'verification-restart': 'RESTART'}.get(stage)
        if dirty or startup_blocked:
            required_marker = 'MIGRATION_' + ('DIRTY' if dirty else 'MISSING_JOURNAL' if missing_journal else 'CONFLICT')
        if required_marker and not any('WRV_SHOPS_HARNESS_' + required_marker + ' verified' in line for line in lines):
            raise RuntimeError('Required runtime condition not established: ' + required_marker)
        if args.legacy_plugin and not legacy:
            if tree_hashes(plugins / 'WRVShops') != legacy_hashes:
                raise RuntimeError('Legacy data changed during renamed plugin run')
            manifest['legacy_full_folder_hashes_unchanged'] = True
            if not startup_blocked:
                for name in ['migration-fixture.txt', 'nested/opaque-evidence.bin']:
                    if (plugins / 'ExcelShops' / name).read_bytes() != (plugins / 'WRVShops' / name).read_bytes():
                        raise RuntimeError('Whole-folder opaque evidence was not retained: ' + name)
                manifest['opaque_legacy_files_preserved'] = True
            if dirty:
                if (plugins / 'ExcelShops/checkout/transactions.log').read_bytes() != (plugins / 'WRVShops/checkout/transactions.log').read_bytes():
                    raise RuntimeError('Dirty migration appended or dropped transaction evidence')
                if not (plugins / 'ExcelShops/checkout/active-session').exists():
                    raise RuntimeError('Dirty migration silently cleared the recovery marker')
                manifest['dirty_journal_unchanged_and_marker_retained'] = True
            if startup_blocked:
                if tree_hashes(plugins / 'ExcelShops') != target_before:
                    raise RuntimeError('Blocked migration altered or created the target data folder')
                manifest['conflicting_target_unchanged' if conflict else 'missing_legacy_journal_blocks_target_creation'] = True
                if missing_journal:
                    if (plugins / 'WRVShops/checkout/transactions.log').exists():
                        raise RuntimeError('Missing legacy journal was silently recreated')
                    if any((plugins / name).exists() for name in ['.ExcelShops.migration-staging', '.ExcelShops.migration-in-progress']):
                        raise RuntimeError('Incomplete legacy source reached migration staging')
                    manifest['missing_legacy_journal_refused_before_staging'] = True
        if legacy:
            # Only synthetic data created by this run is extended and snapshotted.
            old_data = plugins / 'WRVShops'
            (old_data / 'migration-fixture.txt').write_text('Opaque legacy evidence must survive migration unchanged.\n')
            (old_data / 'nested').mkdir()
            (old_data / 'nested/opaque-evidence.bin').write_bytes(bytes(range(256)))
            legacy_hashes = tree_hashes(old_data)
            for name in snapshot_roots:
                source = root / name
                if source.exists():
                    shutil.copytree(source, baseline / name)
            (root / 'verification-legacy-folder-hashes.json').write_text(json.dumps(legacy_hashes, indent=2) + '\n')
        (root / (stage + '-manifest.json')).write_text(json.dumps(manifest, indent=2) + '\n')
        results.extend(['=== ' + stage, *lines])
    (root / 'verification-results.txt').write_text('\n'.join(results) + '\n')
    (root / 'verification-shutdown.json').write_text(json.dumps({'all_child_processes_waited_to_exit': True, 'loopback_25579_available_after_run': True, 'boots': len(stages), 'live_server_accessed': False, 'real_money_used': False}, indent=2) + '\n')
    print('Isolated checks passed. Evidence in', root)


if __name__ == '__main__':
    main()
