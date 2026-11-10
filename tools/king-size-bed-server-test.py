#!/usr/bin/env python3
"""Run an isolated King Size Bed placement and courtship acceptance fixture.

Usage: JAVA_HOME=<JDK25> python3 tools/king-size-bed-server-test.py --server-home <Fabric26.3 server>
Requires an already prepared server and its owner's accepted eula.txt. Never touches that server's world.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-home', type=Path, required=True)
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    server = args.server_home.resolve()
    java_home = Path(os.environ['JAVA_HOME'])
    if not re.search(r'eula\s*=\s*true', (server / 'eula.txt').read_text()):
        raise SystemExit('Use a prepared server with an accepted EULA.')
    runtime = Path(tempfile.mkdtemp(prefix='king-bed-test-', dir=project / 'runs'))
    classpath = runtime / 'classpath.txt'
    init = runtime / 'probe.gradle'
    init.write_text('''allprojects {
        afterEvaluate {
            tasks.register('king-bedProbeClasspath') {
                doLast { file(System.getenv('VILLAGERS_PROBE_CLASSPATH')).text = sourceSets.main.runtimeClasspath.asPath }
            }
        }
    }''')
    env = dict(os.environ, VILLAGERS_PROBE_CLASSPATH=str(classpath))
    env.pop('VILLAGERS_RUN_DIR', None)
    subprocess.run([str(project / 'gradlew'), '-I', str(init), 'build', 'king-bedProbeClasspath'], cwd=project, env=env, check=True)
    version = re.search(r"^version = '([^']+)'", (project / 'build.gradle').read_text(), re.M)[1]
    for name in ['fabric-server-launch.jar', 'fabric-server-launcher.properties', 'server.jar', 'libraries', 'versions']:
        (runtime / name).symlink_to(server / name)
    (runtime / 'mods').mkdir()
    (runtime / 'config').mkdir()
    for jar in (server / 'mods').glob('fabric-api-*.jar'):
        shutil.copy2(jar, runtime / 'mods' / jar.name)
    shutil.copy2(project / f'build/libs/villagers-{version}.jar', runtime / 'mods')
    shutil.copy2(server / 'eula.txt', runtime)
    (runtime / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25592\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\ndifficulty=peaceful\n')
    (runtime / 'config/villagers.json').write_text(json.dumps({'traderRecruitmentChancePercent': 100, 'godChildren': False, 'breeding': True}))
    classes = runtime / 'probe-classes'
    classes.mkdir()
    subprocess.run([str(java_home / 'bin/javac'), '-cp', classpath.read_text(), '-d', str(classes), str(project / 'tools/tests/java/local/villagers/KingSizeBedServerProbe.java')], check=True)
    with zipfile.ZipFile(runtime / 'mods/king-bed-probe.jar', 'w') as archive:
        archive.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'king-bed-probe', 'version': '1', 'environment': 'server', 'entrypoints': {'main': ['local.villagers.KingSizeBedServerProbe']}, 'depends': {'villagers': '*'}}))
        for entry in classes.rglob('*.class'):
            archive.write(entry, entry.relative_to(classes))
    for phase, marker in [('first', 'KING BED PASS:')]:
        log = runtime / f'probe-{phase}.log'
        with log.open('w') as output:
            process = subprocess.Popen([str(java_home / 'bin/java'), '-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui'], cwd=runtime, stdout=output, stderr=subprocess.STDOUT)
            try:
                result = process.wait(timeout=90)
            except subprocess.TimeoutExpired:
                process.terminate()
                process.wait(timeout=30)
                raise RuntimeError(f'Server probe timed out; see {log}')
        contents = log.read_text()
        if result or marker not in contents or 'KING BED FAILED' in contents:
            raise RuntimeError(f'Server probe failed; see {log}\n' + contents[-6000:])
        print(next(line for line in contents.splitlines() if marker in line))
    print(f'Acceptance logs: {runtime}')


if __name__ == '__main__':
    main()
