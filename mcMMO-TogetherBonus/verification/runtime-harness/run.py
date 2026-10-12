#!/usr/bin/env python3
"""Compile/run the test-only XP harness on a pre-provisioned disposable exact-Paper server.

No downloads, EULA acceptance, production access, or network configuration changes.
The harness deliberately stops its isolated server after testing. Never install it in production.
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

PAPER_SHA = '7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08'
API_SHA = 'b0f5edbd52e60ab0fe72ab28dfa356c3c412690d68570c2862e5e88d583f5f22'
SERVER_SHA = 'e26c0cbb690906829c3c98a7d8049fa4175747fb75e38381c20a7996d3ce1acb'
CANDIDATE_SHA = '5f6279995c7b30240d2a7e07cb916b6e28b82537826f9ef0f2702b080cf20978'
WEEKEND_SHA = '4452ccd90bea409fca37740d3e4e9967ef6dd56e9280000521eec868cf0254d0'
ANNOTATIONS_SHA = '2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def props(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if line and not line.lstrip().startswith('#') and '=' in line)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-dir', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--annotations-jar', type=Path, required=True)
    args = parser.parse_args()
    root = args.server_dir.resolve()
    java = args.java_home.resolve() / 'bin'
    here = Path(__file__).resolve().parent
    config = props(root / 'server.properties')
    for key, value in {'server-ip': '127.0.0.1', 'server-port': '25581',
                       'online-mode': 'true', 'white-list': 'true', 'enforce-whitelist': 'true',
                       'enable-rcon': 'false', 'enable-query': 'false',
                       'management-server-enabled': 'false', 'level-name': 'xp-harness-world',
                       'pause-when-empty-seconds': '-1'}.items():
        if config.get(key) != value:
            parser.error('Disposable configuration required: ' + key + '=' + value)
    if props(root / 'eula.txt').get('eula') != 'true':
        parser.error('Existing owner EULA acceptance must already be present; this runner never accepts it')
    paper = root / 'paper-26.2-123.jar'
    api = root / 'libraries/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar'
    server = root / 'versions/26.2/paper-26.2.jar'
    plugins = root / 'plugins'
    candidate = plugins / 'mcMMO-2.3.003-WRV-XP-2.jar'
    weekend = plugins / 'WeekendXP-1.0.2.jar'
    for path, expected in [(paper, PAPER_SHA), (api, API_SHA), (server, SERVER_SHA),
                           (candidate, CANDIDATE_SHA), (weekend, WEEKEND_SHA),
                           (args.annotations_jar, ANNOTATIONS_SHA)]:
        if not path.is_file() or sha(path) != expected:
            parser.error('Missing or mismatched exact artifact: ' + str(path))
    allowed = {candidate.name, weekend.name, 'WRVXpRuntimeHarness.jar'}
    if any(p.name not in allowed for p in plugins.glob('*.jar')):
        parser.error('Only the XP pair and this test harness may be installed')
    experience = (plugins / 'mcMMO/experience.yml').read_text()
    if 'EarlyGameBoost:\n    Enabled: false' not in experience:
        parser.error('Set EarlyGameBoost.Enabled=false in this disposable fixture to isolate true zero awards')
    version = subprocess.run([str(java / 'java'), '-version'], capture_output=True, text=True, check=True).stderr.strip()
    if 'Temurin-25.0.4+7' not in version:
        parser.error('Exact Temurin 25.0.4+7 runtime required')
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 25581))
    build = root / 'xp-harness-build'
    build.mkdir(exist_ok=True)
    classpath = os.pathsep.join(str(p) for p in [*sorted((root / 'libraries').rglob('*.jar')),
                                               server, candidate, args.annotations_jar.resolve()])
    subprocess.run([str(java / 'javac'), '--release', '25', '-cp', classpath, '-d', str(build),
                    str(here / 'src/wrv/harness/XpRuntimeHarness.java')], check=True)
    shutil.copyfile(here / 'plugin.yml', build / 'plugin.yml')
    harness = plugins / 'WRVXpRuntimeHarness.jar'
    subprocess.run([str(java / 'jar'), '--create', '--file', str(harness), '-C', str(build), '.'], check=True)
    log = root / 'xp-smoke-console.log'
    with log.open('w') as output:
        process = subprocess.Popen([str(java / 'java'), '-Dwrv.xp.harness=true', '-Dterminal.jline=false',
                                    '-Dterminal.ansi=false', '-XX:ActiveProcessorCount=2', '-Xms512M',
                                    '-Xmx1536M', '-jar', paper.name, '--nogui'], cwd=root,
                                   stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, text=True)
        try:
            code = process.wait(timeout=180)
        except subprocess.TimeoutExpired:
            process.stdin.write('stop\n')
            process.stdin.flush()
            try:
                process.wait(timeout=45)
            except subprocess.TimeoutExpired:
                process.terminate()
                process.wait(timeout=30)
            raise RuntimeError('Runtime test timed out; isolated process stopped')
    text = log.read_text()
    lines = [line for line in text.splitlines() if 'XP_HARNESS_' in line]
    with socket.socket() as probe:
        closed = probe.connect_ex(('127.0.0.1', 25581)) != 0
    passed = code == 0 and closed and any('XP_HARNESS_SUMMARY' in line and 'failed=0' in line for line in lines)
    manifest = {'utc': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'java': version,
                'paper_sha256': sha(paper), 'paper_api_sha256': sha(api), 'patched_server_sha256': sha(server),
                'candidate_sha256': sha(candidate), 'weekendxp_sha256': sha(weekend),
                'harness_sha256': sha(harness), 'listener': '127.0.0.1:25581', 'listener_closed': closed,
                'returncode': code, 'results': lines, 'passed': passed}
    (root / 'xp-smoke-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print('\n'.join(lines))
    if not passed:
        raise RuntimeError('XP harness failed; inspect ' + str(log))


if __name__ == '__main__':
    main()
