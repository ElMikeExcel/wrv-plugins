#!/usr/bin/env python3
"""Compile the disposable harness and run two isolated exact-Paper boots.

No downloads, server setup, EULA acceptance, or production changes are performed.
The supplied folder must be a disposable, already unpacked Paper test server.
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

PAPER_SHA256 = '7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08'
ANNOTATIONS_SHA256 = '2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def properties(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#') and '=' in line)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-dir', required=True, type=Path)
    parser.add_argument('--plugin', required=True, type=Path)
    parser.add_argument('--annotations-jar', required=True, type=Path)
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'), type=Path)
    args = parser.parse_args()
    here = Path(__file__).resolve().parent
    root, plugin, annotations = args.server_dir.resolve(), args.plugin.resolve(), args.annotations_jar.resolve()
    if args.java_home is None:
        parser.error('--java-home or JAVA_HOME is required (Java 25)')
    java_home = args.java_home.resolve()
    for command in ['java', 'javac', 'jar']:
        if not (java_home / 'bin' / command).is_file():
            parser.error(f'Missing {command} under {java_home}')
    paper = root / 'paper-26.2-123.jar'
    if not paper.is_file() or digest(paper) != PAPER_SHA256:
        parser.error('Exact official paper-26.2-123.jar checksum mismatch or missing')
    if not annotations.is_file() or digest(annotations) != ANNOTATIONS_SHA256:
        parser.error('JetBrains annotations 26.0.2 checksum mismatch or missing')
    if not plugin.is_file():
        parser.error('Plugin JAR missing')
    config = properties(root / 'server.properties')
    required = {'server-ip': '127.0.0.1', 'server-port': '25578', 'online-mode': 'true',
                'white-list': 'true', 'enforce-whitelist': 'true', 'enable-rcon': 'false',
                'enable-query': 'false', 'level-name': 'wrv-harness-world'}
    for key, expected in required.items():
        if config.get(key) != expected:
            parser.error(f'Isolated config required: {key}={expected}')
    if not (root / 'eula.txt').is_file() or properties(root / 'eula.txt').get('eula') != 'true':
        parser.error('EULA is not accepted in this disposable server. Review the Minecraft EULA yourself first.')
    libraries = sorted((root / 'libraries').rglob('*.jar'))
    if not any(p.name == 'paper-api-26.2.build.123-stable.jar' for p in libraries):
        parser.error('Run official Paperclip patch-only setup first to extract exact runtime libraries')
    plugins = root / 'plugins'
    plugins.mkdir(exist_ok=True)
    unexpected = [p.name for p in plugins.glob('*.jar') if p.name not in {'WRVPlayerMap-0.1.0.jar', 'WRVRuntimeHarness.jar'}]
    if unexpected:
        parser.error('Use a disposable test folder with only this prototype/harness: ' + ', '.join(unexpected))
    build = root / 'harness-build'
    build.mkdir(exist_ok=True)
    classpath = os.pathsep.join(str(p) for p in [*libraries, annotations])
    subprocess.run([str(java_home / 'bin/javac'), '--release', '25', '-cp', classpath,
                    '-d', str(build), str(here / 'src/wrv/harness/RuntimeHarness.java')], check=True)
    shutil.copyfile(here / 'plugin.yml', build / 'plugin.yml')
    harness = plugins / 'WRVRuntimeHarness.jar'
    subprocess.run([str(java_home / 'bin/jar'), '--create', '--file', str(harness), '-C', str(build), '.'], check=True)
    target = plugins / 'WRVPlayerMap-0.1.0.jar'
    if target.resolve() != plugin:
        shutil.copyfile(plugin, target)
    version = subprocess.run([str(java_home / 'bin/java'), '-version'], capture_output=True, text=True, check=True).stderr.strip()
    for stage in ['verification-first', 'verification-restart']:
        manifest = {'utc': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'stage': stage,
                    'plugin_sha256': digest(target), 'harness_sha256': digest(harness),
                    'paper_sha256': digest(paper), 'java': version}
        (root / (stage + '-manifest.json')).write_text(json.dumps(manifest, indent=2) + '\n')
        log = root / (stage + '-console.log')
        with log.open('w') as output:
            process = subprocess.Popen([str(java_home / 'bin/java'), '-Dterminal.jline=false', '-Dterminal.ansi=false',
                                        '-Xms512M', '-Xmx1536M', '-jar', paper.name, '--nogui'],
                                       cwd=root, stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True)
            try:
                code = process.wait(timeout=180)
            except subprocess.TimeoutExpired:
                process.stdin.write('stop\n'); process.stdin.flush()
                try:
                    process.wait(timeout=45)
                except subprocess.TimeoutExpired:
                    process.terminate(); process.wait(timeout=30)
                raise RuntimeError('Timed out; isolated server stopped. See ' + str(log))
        lines = [line for line in log.read_text().splitlines() if 'WRV_HARNESS_' in line]
        print('\n'.join(lines))
        if code != 0 or not any('WRV_HARNESS_SUMMARY' in line and 'failed=0' in line for line in lines):
            raise RuntimeError('Harness failed; see ' + str(log))
        if stage == 'verification-restart' and not any('WRV_HARNESS_RESTART verified' in line for line in lines):
            raise RuntimeError('Restart did not establish persisted ID restoration')
    print('Both isolated boots passed. Evidence is in', root)

if __name__ == '__main__':
    main()
