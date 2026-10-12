#!/usr/bin/env python3
"""Run one explicitly selected boot of the isolated old-to-Excel upgrade test.

Use seed, upgrade, restart in that order. This never downloads, changes production,
edits candidate source, accepts the EULA, or runs a later stage automatically.
Seed requires the approved 0.2.0 JAR and original harness from commit
a92ac702452a684000f860e1667e384d763961e2. Upgrade/restart use the new
ExcelPlayerMap candidate and current harness, retaining the same server folder.
"""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import socket
import subprocess
import zipfile

OLD_SHA = '382e40bdb3eae37d2c5dc6b6344d57749701f4b589bcdfd77919aac2750b9dc9'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def props(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#') and '=' in line)

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def identity(path):
    with zipfile.ZipFile(path) as jar:
        text = jar.read('plugin.yml').decode()
    name = re.search(r'^name:\s*(\S+)\s*$', text, re.M)
    require(name is not None, 'Plugin name missing')
    return name.group(1)

def snapshot(root, destination):
    destination.mkdir(parents=True, exist_ok=False)
    records = {}
    for folder in ['plugins/WRVPlayerMap', 'plugins/ExcelPlayerMap', 'plugins/WRVRuntimeHarness']:
        directory = root / folder
        if not directory.is_dir():
            continue
        for source in sorted(directory.rglob('*')):
            if source.is_file():
                relative = source.relative_to(root)
                target = destination / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, target)
                records[str(relative)] = {'sha256': digest(source), 'bytes': source.stat().st_size}
    world = root / 'wrv-harness-world'
    native_maps = set(world.rglob('map_*.dat'))
    native_maps.update((world / 'data/minecraft/maps').glob('*.dat'))
    for source in sorted(native_maps):
        relative = source.relative_to(root)
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        records[str(relative)] = {'sha256': digest(source), 'bytes': source.stat().st_size}
    (destination / 'files.json').write_text(json.dumps(records, indent=2) + '\n')
    return records

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-dir', required=True, type=Path,
                        help='Existing disposable exact-Paper server bound to 127.0.0.1:25579')
    parser.add_argument('--java-home', required=True, type=Path,
                        help='Eclipse Temurin JDK 25.0.4+7-LTS home')
    parser.add_argument('--annotations-jar', required=True, type=Path,
                        help='Checksum-pinned JetBrains annotations 26.0.2 JAR')
    parser.add_argument('--stage', required=True, choices=['seed', 'upgrade', 'restart'])
    parser.add_argument('--plugin', required=True, type=Path)
    parser.add_argument('--harness-source', required=True, type=Path,
                        help='Folder containing plugin.yml and src/wrv/harness/RuntimeHarness.java')
    args = parser.parse_args()
    ROOT = args.server_dir.resolve()
    JAVA = args.java_home.resolve()
    ANNOTATIONS = args.annotations_jar.resolve()
    EVIDENCE = ROOT / 'upgrade-evidence'
    for command in ['java', 'javac', 'jar']:
        require((JAVA / 'bin' / command).is_file(), 'Missing JDK command: ' + command)
    stage, plugin, source = args.stage, args.plugin.resolve(), args.harness_source.resolve()
    require(plugin.is_file(), 'Supplied candidate does not exist')
    require(identity(plugin) == ('WRVPlayerMap' if stage == 'seed' else 'ExcelPlayerMap'), 'Unexpected plugin identity')
    require('name: WRVRuntimeHarness' in (source / 'plugin.yml').read_text(),
            'Keep harness identity WRVRuntimeHarness so native fixture survives the upgrade')
    config = props(ROOT / 'server.properties')
    for key, expected in {'server-ip':'127.0.0.1','server-port':'25579','online-mode':'true',
                          'white-list':'true','enforce-whitelist':'true','enable-rcon':'false',
                          'enable-query':'false','management-server-enabled':'false',
                          'level-name':'wrv-harness-world'}.items():
        require(config.get(key) == expected, 'Unsafe disposable config: ' + key)
    require((ROOT / 'eula.txt').is_file() and props(ROOT / 'eula.txt').get('eula') == 'true',
            'Existing EULA acceptance missing; this runner never creates acceptance')
    for path, expected in {
        ROOT / 'paper-26.2-123.jar':'7b7b3b43c009103e1971a0576c26f655a7dd9b56a0a2a4438e352c03a7fecd08',
        ROOT / 'libraries/io/papermc/paper/paper-api/26.2.build.123-stable/paper-api-26.2.build.123-stable.jar':'b0f5edbd52e60ab0fe72ab28dfa356c3c412690d68570c2862e5e88d583f5f22',
        ROOT / 'versions/26.2/paper-26.2.jar':'e26c0cbb690906829c3c98a7d8049fa4175747fb75e38381c20a7996d3ce1acb',
        ANNOTATIONS:'2037be378980d3ba9333e97955f3b2cde392aa124d04ca73ce2eee6657199297'
    }.items():
        require(path.is_file() and digest(path) == expected, 'Exact runtime checksum mismatch: ' + str(path))
    version = subprocess.run([str(JAVA/'bin/java'), '-version'], capture_output=True, text=True, check=True).stderr.strip()
    require('Temurin-25.0.4+7' in version and 'build 25.0.4+7-LTS' in version, 'Exact Java runtime required')
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 25579))
    EVIDENCE.mkdir(exist_ok=True)
    stage_dir = EVIDENCE / stage
    require(not stage_dir.exists(), 'Stage already exists; inspect existing evidence before re-running')
    if stage == 'seed':
        require(digest(plugin) == OLD_SHA, 'Old approved candidate checksum mismatch')
        require(not list((ROOT/'plugins').glob('*.jar')), 'Seed requires initially empty plugin directory')
        require(not (ROOT/'plugins/WRVRuntimeHarness/native-restart-item.bin').exists(), 'Existing native fixture would invalidate seed')
        legacy = ROOT / 'plugins/WRVPlayerMap'
        legacy.mkdir(parents=True, exist_ok=True)
        require(not (legacy/'maps.yml').exists(), 'Seed requires no old registry')
        (legacy/'config.yml').write_text('# Disposable upgrade-migration fixture\nupdate-interval-ticks: 37\nmigration-test-value: preserved\n')
    else:
        previous = EVIDENCE / ('seed' if stage == 'upgrade' else 'upgrade') / 'manifest.json'
        require(previous.is_file() and json.loads(previous.read_text()).get('passed'), 'Previous upgrade stage has not passed')
        if stage == 'restart':
            require(digest(plugin) == json.loads(previous.read_text())['plugin_sha256'], 'Restart candidate differs from tested upgrade')
    stage_dir.mkdir()
    shutil.copy2(source/'src/wrv/harness/RuntimeHarness.java', stage_dir/'RuntimeHarness.java')
    shutil.copy2(source/'plugin.yml', stage_dir/'harness-plugin.yml')
    snapshot(ROOT, stage_dir/'before')
    build = stage_dir / 'harness-build'
    build.mkdir()
    classpath = os.pathsep.join(map(str,[*sorted((ROOT/'libraries').rglob('*.jar')),
                                       ANNOTATIONS,ROOT/'versions/26.2/paper-26.2.jar']))
    subprocess.run([str(JAVA/'bin/javac'),'--release','25','-cp',classpath,'-d',str(build),
                    str(source/'src/wrv/harness/RuntimeHarness.java')], check=True)
    shutil.copy2(source/'plugin.yml', build/'plugin.yml')
    harness = stage_dir/'WRVRuntimeHarness.jar'
    subprocess.run([str(JAVA/'bin/jar'),'--create','--file',str(harness),'-C',str(build),'.'],check=True)
    archive = stage_dir/'previous-jars'
    archive.mkdir()
    for old in (ROOT/'plugins').glob('*.jar'):
        require(identity(old) in {'WRVPlayerMap','ExcelPlayerMap','WRVRuntimeHarness'}, 'Unexpected disposable plugin ' + old.name)
        shutil.move(old, archive/old.name)
    shutil.copy2(plugin, ROOT/'plugins'/plugin.name)
    shutil.copy2(harness, ROOT/'plugins/WRVRuntimeHarness.jar')
    log = stage_dir/'console.log'
    manifest = {'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'stage':stage,
                'plugin':str(plugin),'plugin_sha256':digest(plugin),'harness_sha256':digest(harness),
                'harness_source_sha256':digest(source/'src/wrv/harness/RuntimeHarness.java'),
                'harness_descriptor_sha256':digest(source/'plugin.yml'),'java':version,'passed':False}
    (stage_dir/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    with log.open('w') as output:
        process = subprocess.Popen([str(JAVA/'bin/java'),'-Dterminal.jline=false','-Dterminal.ansi=false',
                                    '-XX:ActiveProcessorCount=2','-Xms512M','-Xmx1536M','-jar','paper-26.2-123.jar','--nogui'],
                                   cwd=ROOT,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT,text=True)
        try:
            code = process.wait(timeout=180)
        except subprocess.TimeoutExpired:
            process.stdin.write('stop\n'); process.stdin.flush()
            try:
                process.wait(timeout=45)
            except subprocess.TimeoutExpired:
                process.terminate(); process.wait(timeout=30)
            raise RuntimeError('Isolated stage timed out and server was stopped: '+str(log))
    text = log.read_text()
    lines = [line for line in text.splitlines() if 'WRV_HARNESS_' in line]
    print('\n'.join(lines), flush=True)
    summary = re.search(r'WRV_HARNESS_SUMMARY passed=(\d+) failed=(\d+) assertions=(\d+)', text)
    require(code == 0 and summary and int(summary.group(2)) == 0, 'Harness failed: '+str(log))
    marker = re.search(r'WRV_HARNESS_RESTART (prepared|verified) map=(\d+)',text)
    require(marker and marker.group(1) == ('prepared' if stage=='seed' else 'verified'), 'Expected native restart marker absent')
    native_id = int(marker.group(2))
    after = snapshot(ROOT, stage_dir/'after')
    native = ROOT/'plugins/WRVRuntimeHarness/native-restart-item.bin'
    require(native.is_file(), 'Serialized native map fixture absent')
    if stage != 'seed':
        seed = json.loads((EVIDENCE/'seed/manifest.json').read_text())
        require(native_id == seed['native_map_id'], 'Upgrade changed persisted native map ID')
        require(digest(native) == seed['native_item_sha256'], 'Harness changed serialized old native item')
        seed_files = json.loads((EVIDENCE/'seed/after/files.json').read_text())
        for relative, metadata in seed_files.items():
            if relative.startswith('plugins/WRVPlayerMap/'):
                require((ROOT/relative).is_file() and digest(ROOT/relative)==metadata['sha256'],
                        'Legacy data was altered or removed: '+relative)
        old_config = ROOT/'plugins/WRVPlayerMap/config.yml'
        new_config = ROOT/'plugins/ExcelPlayerMap/config.yml'
        require(new_config.is_file() and new_config.read_bytes()==old_config.read_bytes(), 'Migrated custom config differs from legacy bytes')
    with socket.socket() as sock:
        require(sock.connect_ex(('127.0.0.1',25579)) != 0, 'Listener did not close')
    manifest.update({'passed':True,'returncode':code,'groups':int(summary.group(1)),
                     'assertions':int(summary.group(3)),'native_map_id':native_id,
                     'native_item_sha256':digest(native),'snapshot_file_count':len(after),
                     'listener_closed':True,'completed_utc':datetime.datetime.now(datetime.timezone.utc).isoformat()})
    (stage_dir/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    (stage_dir/'restart.properties').write_text('native-map-id='+str(native_id)+'\nnative-item-sha256='+digest(native)+'\n')
    print(json.dumps(manifest,indent=2))

if __name__ == '__main__':
    main()
