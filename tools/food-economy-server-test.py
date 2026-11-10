#!/usr/bin/env python3
"""Isolated native food-economy fixtures. Never opens a society world.

Requires a previously prepared Fabric26.3 server with its owner's accepted EULA.
Fixtures declare infrastructure, starting supplies and founder traits. Renewable
mode observes bounded peaceful food production, not full-threat survival.
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
    parser.add_argument('--verify-restart', action='store_true', help='Also verify saved renewable inventories and nutrition without replay')
    parser.add_argument('--minimum-ticks', type=int, default=0, help='Keep observing renewable survival after the first completed food loop')
    parser.add_argument('--continue-from', type=Path, help='Copy a stopped exhausted renewable fixture; preserve source world and result')
    parser.add_argument('--renewable', action='store_true', help='Observe native age-zero wheat growth and earned bread sharing; no supplied grain')
    parser.add_argument('--investment', action='store_true', help='Verify ordinary stocked adults plant protected/approved bare ground with real seeds')
    parser.add_argument('--cultivate', action='store_true', help='Renewable fixture starts with bare operator-approved plots; controller pays all planting costs')
    parser.add_argument('--observation-ticks', type=int, default=48000, help='Renewable observation bound in100..96000 actual ticks')
    parser.add_argument('--starter-rations', type=int, choices=range(0,7), default=2, help='Declared initial bread per adult in the renewable fixture')
    args = parser.parse_args()
    if args.continue_from and not args.renewable: parser.error('--continue-from requires --renewable')
    if args.renewable and args.investment: parser.error('Choose one native fixture mode')
    if args.cultivate and not args.renewable: parser.error('--cultivate requires --renewable')
    if not 100 <= args.observation_ticks <= 96000: parser.error('--observation-ticks must be100..96000')
    if not 0 <= args.minimum_ticks <= args.observation_ticks: parser.error('--minimum-ticks must fit the observation bound')
    project = Path(__file__).resolve().parent.parent
    server = args.server_home.resolve()
    runtime = args.runtime.absolute()
    if runtime.resolve() != runtime or not runtime.is_relative_to(project / 'runs') or runtime.exists():
        raise ValueError('Use a fresh canonical runtime below project/runs; never replace a saved world')
    if not re.search(r'eula\s*=\s*true', (server / 'eula.txt').read_text()):
        raise ValueError('Prepared server must already have its owner-accepted EULA')
    java = Path(os.environ['JAVA_HOME']) / 'bin'
    if args.continue_from:
        source_runtime=args.continue_from.resolve()
        if not source_runtime.is_relative_to(project / 'runs'):
            raise ValueError('Continuation source must be an isolated runtime below runs')
        report=json.loads((source_runtime / 'world/food-economy-result.json').read_text())
        if report.get('failure') != 'java.lang.AssertionError: Native economy observation budget exhausted':
            raise ValueError('Only observation exhaustion can continue; never reset a death')
        # A stopped server releases session.lock; do not copy a live world.
        import fcntl
        lock=(source_runtime / 'world/session.lock').open('rb')
        fcntl.lockf(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
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
    (runtime / 'config/villagers.json').write_text(json.dumps({'baseExhaustionPerDay':80 if args.renewable or args.investment else 400, 'godChildren':False, 'breeding':False, 'maxResidentChunks':16}))
    if args.continue_from:
        shutil.copytree(source_runtime / 'world',runtime / 'world')
        shutil.copy2(source_runtime / 'config/villagers.json',runtime / 'config/villagers.json')
        lock.close()
        (runtime / 'continuation-source.json').write_text(json.dumps({'source':str(source_runtime),'previousResultSHA256':hashlib.sha256((source_runtime / 'world/food-economy-result.json').read_bytes()).hexdigest()},indent=2)+'\n')
    # Compile against a detached copy of the compiled main classes, never a changing shared build output.
    shutil.copytree(project / 'build/classes/java/main', runtime / 'main-classes')
    shutil.copytree(project / 'build/resources/main', runtime / 'main-resources')
    cp = args.classpath_file.read_text().replace(str(project / 'build/classes/java/main'), str(runtime / 'main-classes')).replace(str(project / 'build/resources/main'), str(runtime / 'main-resources'))
    classes = runtime / 'probe-classes'; classes.mkdir()
    probe = 'FarmInvestmentServerProbe' if args.investment else 'FoodEconomyServerProbe'
    source = project / f'tools/tests/java/local/villagers/{probe}.java'
    shutil.copy2(source, runtime / source.name)
    compiled = subprocess.run([str(java / 'javac'), '-cp', cp, '-d', str(classes), str(runtime / source.name)])
    if compiled.returncode:
        raise RuntimeError(f'Probe compilation failed; retained unstarted fixture at {runtime}')
    with zipfile.ZipFile(runtime / 'mods/food-economy-probe.jar', 'w') as archive:
        archive.writestr('fabric.mod.json', json.dumps({'schemaVersion':1,'id':'food-economy-probe','version':'1','environment':'server','entrypoints':{'main':['local.villagers.'+probe]},'depends':{'villagers':'*'}}))
        for entry in classes.rglob('*.class'): archive.write(entry, entry.relative_to(classes))
    pins = {str(p.relative_to(runtime)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (runtime / 'mods').glob('*.jar')}
    (runtime / 'artifact-pins.json').write_text(json.dumps(pins, indent=2)+'\n')
    phases = [('first','food-economy-result.json','passed')]
    if args.continue_from: phases=[('continuation','food-economy-continuation-result.json','passed')]
    if not args.renewable or args.verify_restart: phases.append(('restart','food-economy-restart-result.json','restart-passed'))
    for phase, report_name, status in phases:
        with (runtime / f'probe-{phase}.log').open('w') as output:
            process = subprocess.Popen([str(java / 'java'), '-Xmx2G', '-Dvillagers.economy.minimumTicks='+str(args.minimum_ticks), '-Dvillagers.economy.continue='+str(bool(args.continue_from) and phase!='restart').lower(), '-Dvillagers.economy.restartFrom='+('food-economy-continuation-result.json' if args.continue_from and phase=='restart' else 'food-economy-result.json'), '-Dvillagers.economy.renewable='+str(args.renewable).lower(), '-Dvillagers.economy.cultivate='+str(args.cultivate).lower(), '-Dvillagers.economy.observationTicks='+str(args.observation_ticks), '-Dvillagers.economy.startingRations='+str(args.starter_rations), '-jar', 'fabric-server-launch.jar', 'nogui'], cwd=runtime, stdout=output, stderr=subprocess.STDOUT)
            stat = Path(f'/proc/{process.pid}/stat').read_text().rsplit(')',1)[1].split()
            (runtime / f'custody-{phase}.json').write_text(json.dumps({'pid':process.pid,'startTicks':stat[19],'cwd':str(runtime),'launchedAtEpoch':time.time()},indent=2)+'\n')
            print(f'Native fixture {phase}: PID {process.pid}, runtime {runtime}', flush=True)
            try: result = process.wait(timeout=args.observation_ticks/80+90 if args.renewable else 150)
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
