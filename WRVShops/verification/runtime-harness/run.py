#!/usr/bin/env python3
"""Compile and run disposable exact-Paper WRVShops integration checks.

No download, EULA acceptance, live server access, or real economy is performed.
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

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#') and '=' in line)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-dir', required=True, type=Path)
    parser.add_argument('--plugin', required=True, type=Path)
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
        parser.error('WRVShops JAR missing')
    config = properties(root / 'server.properties')
    required = {'server-ip': '127.0.0.1', 'server-port': '25579', 'online-mode': 'true',
                'white-list': 'true', 'enforce-whitelist': 'true', 'enable-rcon': 'false',
                'enable-query': 'false', 'management-server-enabled': 'false', 'level-name': 'wrv-shops-harness-world'}
    for key, expected in required.items():
        if config.get(key) != expected:
            parser.error(f'Isolated config required: {key}={expected}')
    with socket.socket() as probe:
        try:
            probe.bind(('127.0.0.1', int(config['server-port'])))
        except OSError:
            parser.error('Isolation port 25579 is already in use; no process will be touched')
    if not (root / 'eula.txt').is_file() or properties(root / 'eula.txt').get('eula') != 'true':
        parser.error('Review and accept the Minecraft EULA before running. The runner never creates acceptance.')
    libraries = sorted((root / 'libraries').rglob('*.jar'))
    if not any(p.name == 'paper-api-26.2.build.123-stable.jar' for p in libraries):
        parser.error('Extract exact official runtime with Paperclip patch-only preparation first')
    plugins = root / 'plugins'
    plugins.mkdir(exist_ok=True)
    allowed = {'WRVShops.jar', 'WRVShopsRuntimeHarness.jar', 'Vault-TestOnly.jar', 'GriefPrevention-TestOnly.jar', 'WRVShopsProtectionHarness.jar'}
    unexpected = [p.name for p in plugins.glob('*.jar') if p.name not in allowed]
    if unexpected:
        parser.error('Use a disposable server without other plugin JARs: ' + ', '.join(unexpected))
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
    target = plugins / 'WRVShops.jar'
    if target.resolve() != plugin:
        shutil.copyfile(plugin, target)
    version = subprocess.run([str(java_home / 'bin/java'), '-version'], capture_output=True, text=True, check=True).stderr.strip()
    stages = ['verification-first'] if args.boots == 1 else ['verification-first', 'verification-restart', 'verification-no-economy', 'verification-no-vault']
    if args.only_no_vault:
        stages = ['verification-no-vault']
    for stage in stages:
        manifest = {'utc': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'stage': stage,
                    'plugin_sha256': digest(target), 'harness_sha256': digest(harness), 'fake_vault_sha256': digest(vault_jar), 'fake_gp_sha256': digest(gp_jar),
                    'vault_api_sha256': digest(vault), 'paper_sha256': digest(paper), 'java': version,
                    'listen': '127.0.0.1:25579', 'synthetic_players': True, 'real_client': False, 'real_economy': False, 'vault_plugin_present': stage != 'verification-no-vault',
                    'economy_service_present': stage not in {'verification-no-economy', 'verification-no-vault'},
                    'probe_sha256': digest(probe_jar) if stage == 'verification-no-vault' else None}
        (root / (stage + '-manifest.json')).write_text(json.dumps(manifest, indent=2) + '\n')
        no_vault = stage == 'verification-no-vault'
        disabled_vault, disabled_harness = build / 'disabled-Vault.jar', build / 'disabled-Harness.jar'
        active_probe = plugins / 'WRVShopsProtectionHarness.jar'
        if no_vault:
            shutil.move(vault_jar, disabled_vault)
            shutil.move(harness, disabled_harness)
            shutil.copyfile(probe_jar, active_probe)
        log = root / (stage + '-console.log')
        try:
            with log.open('w') as output:
                flags = ['-Dwrv.test.no-economy=true'] if stage == 'verification-no-economy' else []
                process = subprocess.Popen([str(java_home / 'bin/java'), *flags, '-Dterminal.jline=false', '-Dterminal.ansi=false',
                                            '-Xms384M', '-Xmx1200M', '-jar', paper.name, '--nogui'], cwd=root,
                                           stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True)
                try:
                    code = process.wait(timeout=240)
                except subprocess.TimeoutExpired:
                    process.stdin.write('stop\n'); process.stdin.flush()
                    try:
                        process.wait(timeout=45)
                    except subprocess.TimeoutExpired:
                        process.terminate(); process.wait(timeout=30)
                    raise RuntimeError('Timed out; isolated child process stopped. See ' + str(log))
        finally:
            if no_vault:
                active_probe.unlink()
                shutil.move(disabled_vault, vault_jar)
                shutil.move(disabled_harness, harness)
        lines = [line for line in log.read_text().splitlines() if 'WRV_SHOPS_HARNESS_' in line]
        print('\n'.join(lines), flush=True)
        if code != 0 or not any('WRV_SHOPS_HARNESS_SUMMARY' in line and 'failed=0' in line for line in lines):
            raise RuntimeError('Harness failed; see ' + str(log))
        if stage == 'verification-no-vault' and not any('WRV_SHOPS_HARNESS_NO_VAULT verified' in line for line in lines):
            raise RuntimeError('Missing Vault plugin/API protection-only startup was not established')
        if stage == 'verification-no-economy' and not any('WRV_SHOPS_HARNESS_PROTECTION_ONLY verified' in line for line in lines):
            raise RuntimeError('Protection-only startup was not established')
        if stage == 'verification-restart' and not any('WRV_SHOPS_HARNESS_RESTART verified' in line for line in lines):
            raise RuntimeError('Restart persistence was not established')
    print('Isolated checks passed. Evidence in', root)

if __name__ == '__main__':
    main()
