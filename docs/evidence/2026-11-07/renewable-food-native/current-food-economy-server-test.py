#!/usr/bin/env python3
"""Isolated native food-demand integration fixture. Never opens a society world.

Requires a previously prepared Fabric26.3 server with its owner's accepted EULA.
The fixture is deliberately supplied and selects founder traits; it is not survival evidence.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-home', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--classpath-file', type=Path, required=True)
    parser.add_argument('--renewable', action='store_true', help='Observe native age-zero wheat growth and earned bread sharing; no supplied grain')
    parser.add_argument('--starter-rations', type=int, choices=range(0,7), default=2, help='Declared initial bread per adult in the renewable fixture')
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    server = args.server_home.resolve()
    runtime = args.runtime.absolute()
    if runtime.resolve() != runtime or not runtime.is_relative_to(project / 'runs') or runtime.exists():
        raise ValueError('Use a fresh canonical runtime below project/runs; never replace a saved world')
    if not re.search(r'eula\s*=\s*true', (server / 'eula.txt').read_text()):
        raise ValueError('Prepared server must already have its owner-accepted EULA')
    java = Path(os.environ['JAVA_HOME']) / 'bin'
    runtime.mkdir(parents=True)
    for name in ['fabric-server-launch.jar', 'fabric-server-launcher.properties', 'server.jar', 'libraries', 'versions']:
        (runtime / name).symlink_to(server / name)
    (runtime / 'mods').mkdir(); (runtime / 'config').mkdir()
    for jar in (server / 'mods').glob('fabric-api-*.jar'):
        shutil.copy2(jar, runtime / 'mods' / jar.name)
    version = re.search(r"^version = '([^']+)'", (project / 'build.gradle').read_text(), re.M)[1]
    jar = project / f'build/libs/villagers-{version}.jar'
    shutil.copy2(jar, runtime / 'mods' / jar.name)
    shutil.copy2(server / 'eula.txt', runtime)
    (runtime / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25581\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\ndifficulty=peaceful\npause-when-empty-seconds=-1\n')
    (runtime / 'config/villagers.json').write_text(json.dumps({'baseExhaustionPerDay':80 if args.renewable else 400, 'godChildren':False, 'breeding':False, 'maxResidentChunks':16}))
    # Compile against a detached copy of the compiled main classes, never a changing shared build output.
    shutil.copytree(project / 'build/classes/java/main', runtime / 'main-classes')
    shutil.copytree(project / 'build/resources/main', runtime / 'main-resources')
    cp = args.classpath_file.read_text().replace(str(project / 'build/classes/java/main'), str(runtime / 'main-classes')).replace(str(project / 'build/resources/main'), str(runtime / 'main-resources'))
    classes = runtime / 'probe-classes'; classes.mkdir()
    source = project / 'tools/tests/java/local/villagers/FoodEconomyServerProbe.java'
    shutil.copy2(source, runtime / source.name)
    compiled = subprocess.run([str(java / 'javac'), '-cp', cp, '-d', str(classes), str(runtime / source.name)])
    if compiled.returncode:
        raise RuntimeError(f'Probe compilation failed; retained unstarted fixture at {runtime}')
    with zipfile.ZipFile(runtime / 'mods/food-economy-probe.jar', 'w') as archive:
        archive.writestr('fabric.mod.json', json.dumps({'schemaVersion':1,'id':'food-economy-probe','version':'1','environment':'server','entrypoints':{'main':['local.villagers.FoodEconomyServerProbe']},'depends':{'villagers':'*'}}))
        for entry in classes.rglob('*.class'): archive.write(entry, entry.relative_to(classes))
    pins = {str(p.relative_to(runtime)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (runtime / 'mods').glob('*.jar')}
    (runtime / 'artifact-pins.json').write_text(json.dumps(pins, indent=2)+'\n')
    phases = [('first','food-economy-result.json','passed')]
    if not args.renewable: phases.append(('restart','food-economy-restart-result.json','restart-passed'))
    for phase, report_name, status in phases:
        with (runtime / f'probe-{phase}.log').open('w') as output:
            process = subprocess.Popen([str(java / 'java'), '-Xmx2G', '-Dvillagers.economy.renewable='+str(args.renewable).lower(), '-Dvillagers.economy.startingRations='+str(args.starter_rations), '-jar', 'fabric-server-launch.jar', 'nogui'], cwd=runtime, stdout=output, stderr=subprocess.STDOUT)
            stat = Path(f'/proc/{process.pid}/stat').read_text().rsplit(')',1)[1].split()
            (runtime / f'custody-{phase}.json').write_text(json.dumps({'pid':process.pid,'startTicks':stat[19],'cwd':str(runtime),'launchedAtEpoch':time.time()},indent=2)+'\n')
            print(f'Native fixture {phase}: PID {process.pid}, runtime {runtime}', flush=True)
            try: result = process.wait(timeout=760 if args.renewable else 150)
            except subprocess.TimeoutExpired:
                process.terminate(); process.wait(timeout=30)
                raise RuntimeError(f'Owned fixture timed out; inspect {runtime / ("probe-"+phase+".log")}')
        (runtime / f'terminal-{phase}.json').write_text(json.dumps({'pid':process.pid,'exitCode':result,'finishedAtEpoch':time.time()},indent=2)+'\n')
        report = runtime / 'world' / report_name
        if result or not report.exists() or json.loads(report.read_text())['status'] != status:
            raise RuntimeError(f'Native food economy fixture failed; see {runtime / ("probe-"+phase+".log")}')
        print(f'FOOD ECONOMY {phase} PASS; explicit fixture result: {report}', flush=True)


if __name__ == '__main__':
    main()
