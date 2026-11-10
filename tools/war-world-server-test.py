#!/usr/bin/env python3
"""Create and check a NEW isolated FourBiomesWar world using an existing prepared Fabric server."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-home', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--restart', action='store_true', help='Check the same stopped world again; do not construct a new world')
    parser.add_argument('--resume-observation', action='store_true', help='Continue an interrupted food/family observation from its actual saved world')
    parser.add_argument('--battle', action='store_true', help='Observe autonomous marches, native damage, casualties and creature learning')
    parser.add_argument('--recovery', action='store_true', help='Check retreat recovery control in an isolated native fixture')
    parser.add_argument('--food', action='store_true', help='Observe illager food production and restart in an isolated fixture')
    parser.add_argument('--food-endurance', action='store_true', help='Observe at least one full day, repeat crops and multiple meals; implies --food')
    parser.add_argument('--families', action='store_true', help='Observe ordinary illager births from two empty producers; implies --food')
    parser.add_argument('--family-lifecycle', action='store_true', help='Continue a passed family save until its child matures; requires --restart')
    parser.add_argument('--adventure-prepare', action='store_true', help='Check native owned-wood pickaxe preparation and persistence')
    parser.add_argument('--native-regions', action='store_true', help='Check native four-region terrain and village generation')
    parser.add_argument('--boats', action='store_true', help='Check empty-bag boat preparation, native crossing, at-sea reopen and return with food')
    parser.add_argument('--boat-crops', action='store_true', help='Boat fixture uses mature crops instead of loose food; implies --boats')
    parser.add_argument('--survival-bootstrap', action='store_true', help='Empty-bag local food preparation, native adventure nomination and owned pickaxe')
    parser.add_argument('--stone-progression', action='store_true', help='Continue empty-bag bootstrap through native stone mining and a crafted upgrade')
    parser.add_argument('--immature-field', action='store_true', help='Verify real hoe preparation before the declared crops ripen')
    parser.add_argument('--hand-harvest', action='store_true', help='Verify native harvest/replant without supplied wood or tools')
    parser.add_argument('--server-port', type=int, default=25586)
    args = parser.parse_args()
    if args.hand_harvest:
        if args.restart or args.stone_progression or args.immature_field:
            parser.error('--hand-harvest requires a fresh harvest-only fixture')
        args.survival_bootstrap = True
    if args.immature_field:
        if args.restart or args.stone_progression:
            parser.error('--immature-field requires a fresh preparation-only fixture')
        args.survival_bootstrap = True
    if args.stone_progression:
        args.survival_bootstrap = True
    if args.survival_bootstrap and any((args.native_regions,args.adventure_prepare,args.food,args.food_endurance,args.families,args.family_lifecycle,args.battle,args.recovery,args.resume_observation,args.boats,args.boat_crops)):
        parser.error('--survival-bootstrap is a separate native fixture')
    if args.boat_crops:
        args.boats = True
    if args.boats and any((args.native_regions,args.adventure_prepare,args.food,args.food_endurance,args.families,args.family_lifecycle,args.battle,args.recovery,args.resume_observation)):
        parser.error('--boats is a separate native fixture')
    if args.native_regions and (args.food or args.food_endurance or args.families or args.family_lifecycle or args.battle or args.recovery or args.adventure_prepare or args.resume_observation):
        parser.error('--native-regions is a separate native world fixture')
    if args.adventure_prepare and (args.food or args.food_endurance or args.families or args.family_lifecycle or args.battle or args.recovery or args.resume_observation):
        parser.error('--adventure-prepare is a separate native fixture')
    if args.family_lifecycle:
        if not args.restart:
            parser.error('--family-lifecycle requires --restart')
        args.families = True
    if not 1024 <= args.server_port <= 65535:
        parser.error('--server-port must be1024..65535')
    if args.food_endurance or args.families:
        args.food = True
    if args.resume_observation and (not args.food or args.restart):
        parser.error('--resume-observation requires food/family mode without --restart')
    if args.food and (args.recovery or args.battle):
        parser.error('--food cannot be combined with --recovery or --battle')
    if args.recovery and (args.battle or args.restart):
        parser.error('--recovery requires a fresh fixture without --battle or --restart')
    root = Path(__file__).resolve().parents[1]
    runtime = args.runtime.resolve()
    server = args.server_home.resolve()
    if not runtime.is_relative_to(root / 'runs') or (runtime.exists() != (args.restart or args.resume_observation)):
        raise ValueError('Use a new, nonexistent runtime under runs/')
    if 'eula=true' not in (server / 'eula.txt').read_text():
        raise ValueError('Requires the prepared server owner’s existing accepted EULA')
    java = Path(os.environ['JAVA_HOME']) / 'bin'
    report_name = 'survival-bootstrap' if args.survival_bootstrap else 'boat-travel' if args.boats else 'native-regions' if args.native_regions else 'adventure-prepare' if args.adventure_prepare else 'illager-food' if args.food else 'war-recovery' if args.recovery else 'war-battle' if args.battle else 'war-world'
    probe = 'SurvivalBootstrapServerProbe' if args.survival_bootstrap else 'BoatTravelServerProbe' if args.boats else 'NativeRegionsServerProbe' if args.native_regions else 'AdventurePreparationServerProbe' if args.adventure_prepare else 'IllagerFoodServerProbe' if args.food else 'WarRecoveryServerProbe' if args.recovery else 'WarBattleServerProbe' if args.battle else 'WarWorldServerProbe'
    phase = 'restart-' + str(time.time_ns()) if args.restart else 'first'
    if args.resume_observation:
        world = runtime / 'FourBiomesWar'
        with (world / 'session.lock').open('r+b') as lock:
            fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            prior_result = world / 'illager-food-result.json'
            if prior_result.exists() and json.loads(prior_result.read_text()).get('failure') not in (
                'java.lang.AssertionError: both live bodies present',
                'java.lang.AssertionError: food observation exhausted',
            ):
                raise ValueError('Only interruption, entity-readiness failure or pure observation exhaustion may resume')
            progress = json.loads((world / 'illager-food-progress.json').read_text())
            if progress['minFood'] <= 0 or any(progress[role]['health'] <= 0 for role in ('farmer', 'dependent')):
                raise ValueError('An unhealthy interrupted observation cannot resume as a survival pass')
            phase = 'resume-' + str(time.time_ns())
            archive = runtime / 'observation-archives' / phase
            archive.mkdir(parents=True)
            shutil.copytree(world, archive / 'world')
            shutil.copytree(runtime / 'mods', archive / 'mods')
            for path in runtime.glob('*.json'):
                shutil.copy2(path, archive)
            for path in runtime.glob('probe-*.log'):
                shutil.copy2(path, archive)
            shutil.copy2(world / 'illager-food-progress.json', world / 'illager-food-before-continuation.json')
            if prior_result.exists():
                prior_result.rename(archive / 'illager-food-result.json')
            shutil.copy2(root / 'build/libs/villagers-0.4.1-dev.jar', runtime / 'mods')
    elif args.restart:
        with (runtime / 'FourBiomesWar/session.lock').open('rb') as lock:
            fcntl.lockf(lock, fcntl.LOCK_SH | fcntl.LOCK_NB)
        result = runtime / f'FourBiomesWar/{report_name}-result.json'
        if json.loads(result.read_text())['status'] != 'passed':
            raise ValueError('Restart requires a previously passed world check')
        before = result.with_name(f'{report_name}-before-restart.json')
        if before.exists():
            shutil.copy2(before, runtime / f'prior-{phase}-before-restart.json')
        result.rename(result.with_name(f'{report_name}-before-restart.json'))
    else:
        runtime.mkdir(parents=True)
        for name in ['fabric-server-launch.jar', 'fabric-server-launcher.properties', 'server.jar', 'libraries', 'versions']:
            (runtime / name).symlink_to(server / name)
        (runtime / 'mods').mkdir()
        (runtime / 'config').mkdir()
        shutil.copy2(server / 'eula.txt', runtime)
        for jar in (server / 'mods').glob('fabric-api-*.jar'):
            shutil.copy2(jar, runtime / 'mods' / jar.name)
        shutil.copy2(root / 'build/libs/villagers-0.4.1-dev.jar', runtime / 'mods')
        (runtime / 'config/villagers.json').write_text(json.dumps({'godChildren':False,'breeding':args.families}))
        (runtime / 'server.properties').write_text(
            f'server-ip=127.0.0.1\nserver-port={args.server_port}\nonline-mode=false\n'
            'level-name=FourBiomesWar\nlevel-type=' + ('villagers:native_four_regions' if args.native_regions or args.survival_bootstrap else 'villagers:four_biomes_war') + '\n'
            'level-seed=846206\ngenerate-structures=true\nview-distance=2\nsimulation-distance=2\n'
            'difficulty=normal\npause-when-empty-seconds=-1\nmax-tick-time=180000\n')
    cp = (root / 'build/probe-classpath.txt').read_text()
    classes = runtime / 'probe-classes'
    classes.mkdir(exist_ok=True)
    subprocess.run([str(java / 'javac'), '-cp', cp, '-d', str(classes),
                    str(root / f'tools/tests/java/local/villagers/{probe}.java')], check=True)
    with zipfile.ZipFile(runtime / 'mods/war-world-probe.jar', 'w') as archive:
        archive.writestr('fabric.mod.json', json.dumps({'schemaVersion':1,'id':'war-world-probe','version':'1',
            'environment':'server','entrypoints':{'main':['local.villagers.'+probe]},'depends':{'villagers':'*'}}))
        for path in classes.rglob('*.class'):
            archive.write(path,path.relative_to(classes))
    (runtime / f'artifact-pins-{phase}.json').write_text(json.dumps({p.name:hashlib.sha256(p.read_bytes()).hexdigest()
        for p in (runtime / 'mods').glob('*.jar')},indent=2)+'\n')
    with (runtime / f'probe-{phase}.log').open('w') as output:
        process = subprocess.Popen([str(java / 'java'),'-Xmx3G',
                                    '-Dvillagers.bootstrap.immature=' + str(args.immature_field).lower(),
                                    '-Dvillagers.bootstrap.hand=' + str(args.hand_harvest).lower(),
                                    '-Dvillagers.adventure.stone=' + str(args.stone_progression).lower(),
                                    '-Dvillagers.boats.crops=' + str(args.boat_crops).lower(),
                                    '-Dvillagers.food.endurance=' + str(args.food_endurance).lower(),
                                    '-Dvillagers.food.families=' + str(args.families).lower(),
                                    '-Dvillagers.food.lifecycle=' + str(args.family_lifecycle).lower(),
                                    '-jar','fabric-server-launch.jar','nogui'],
                                   cwd=runtime,stdout=output,stderr=subprocess.STDOUT)
        print(f'Native world check PID {process.pid}: {runtime}',flush=True)
        (runtime / f'process-{phase}.json').write_text(json.dumps({'pid':process.pid,'runtime':str(runtime)}))
        try:
            code = process.wait(timeout=2100 if args.food_endurance else 600 if args.food else 240)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=30)
            raise RuntimeError(f'Owned test exceeded budget: {runtime / ("probe-"+phase+".log")}')
    (runtime / f'terminal-{phase}.json').write_text(json.dumps({'pid':process.pid,'exitCode':code}))
    result = runtime / f'FourBiomesWar/{report_name}-result.json'
    if code or not result.exists() or json.loads(result.read_text())['status'] != 'passed':
        raise RuntimeError(f'World check failed: {runtime / ("probe-"+phase+".log")}')
    print(f'WORLD CHECK PASS: {result}')


if __name__ == '__main__':
    main()
