#!/usr/bin/env python3
"""Inspect food/boat routes or rehearse native boating in a full-pack COPY of a stopped world."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-world', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--server-home', type=Path, required=True)
    parser.add_argument('--port', type=int, default=25595)
    parser.add_argument('--boats', action='store_true', help='Inspect the actual grassland boat routes instead of desert food access')
    parser.add_argument('--expand-copy-border', action='store_true', help='Open exploration in a --boats or --sustain diagnostic COPY')
    parser.add_argument('--farm', action='store_true', help='Observe natural crop growth and island food production in a copy')
    parser.add_argument('--night-meal', action='store_true', help='Verify the saved starving grain owner can prepare a night meal')
    parser.add_argument('--farm-meal', action='store_true', help='Continue a saved farm harvester until actually fed or critically injured')
    parser.add_argument('--grain-meal', action='store_true', help='Observe saved hungry grain owners physically craft and eat a meal')
    parser.add_argument('--sustain', action='store_true', help='Observe a longer native island food window, including repeat yields and births')
    parser.add_argument('--sustain-ticks', type=int, default=14000, help='Game ticks for --sustain (1000..48000)')
    parser.add_argument('--tps', type=int, choices=(20,40,80), default=20, help='Wall-time simulation rate for --sustain; every game rule still runs each tick')
    parser.add_argument('--water-exit', action='store_true', help='Compare native shore routes before/after a copied-world border expansion')
    parser.add_argument('--water-recovery', action='store_true', help='Verify the saved swimmer can reach shore and real food')
    parser.add_argument('--rehearse', action='store_true', help='Run bounded normal AI in the diagnostic COPY')
    parser.add_argument('--all-residents', action='store_true', help='Also load the existing western residents during the desert food rehearsal')
    parser.add_argument('--healing', action='store_true', help='Follow saved injured desert residents until naturally recovered')
    parser.add_argument('--illagers', action='store_true', help='Inspect existing native illagers, habitats and settlement resources while frozen')
    parser.add_argument('--illager-ticks',type=int,default=8000,help='Bounded normal-AI window for --illagers --rehearse (1000..48000)')
    args = parser.parse_args()
    if not 1000<=args.illager_ticks<=48000 or args.illager_ticks!=8000 and not (args.illagers and args.rehearse):
        parser.error('--illager-ticks requires --illagers --rehearse and must be1000..48000')
    if args.illagers and (args.boats or args.farm or args.sustain or args.water_exit or args.water_recovery or args.night_meal):
        parser.error('--illagers is a separate frozen habitat audit')
    if args.healing and (not args.rehearse or args.boats or args.farm or args.farm_meal or args.grain_meal or args.sustain or args.night_meal or args.water_recovery or args.water_exit):
        parser.error('--healing requires the default desert --rehearse observation')
    if args.all_residents and (not args.rehearse or args.boats or args.farm or args.farm_meal or args.grain_meal or args.sustain or args.night_meal or args.water_recovery or args.water_exit):
        parser.error('--all-residents requires the default desert --rehearse observation')
    if args.expand_copy_border and not (args.boats and not args.rehearse or args.sustain and args.rehearse):
        parser.error('--expand-copy-border requires frozen --boats or --sustain --rehearse')
    if args.grain_meal:
        if args.farm_meal or args.sustain:
            parser.error('--grain-meal is a separate saved-grain recovery observation')
        args.farm_meal = True
    if args.farm_meal:
        args.farm = True
    if args.sustain:
        if args.farm_meal:
            parser.error('--sustain cannot be combined with --farm-meal')
        args.farm = True
    if not 1000<=args.sustain_ticks<=48000 or not args.sustain and (args.sustain_ticks!=14000 or args.tps!=20 and not (args.healing or args.illagers)):
        parser.error('--sustain-ticks must be1000..48000; nondefault window/rate requires --sustain')
    if args.water_recovery and (not args.rehearse or args.boats or args.farm or args.night_meal or args.water_exit):
        parser.error('--water-recovery requires --rehearse as a separate recovery observation')
    if args.water_exit and (args.rehearse or args.boats or args.farm or args.night_meal):
        parser.error('--water-exit is a separate spatial diagnostic')
    if args.farm and (args.boats or not args.rehearse):
        parser.error('--farm requires --rehearse and cannot be combined with --boats')
    if args.night_meal and (args.boats or args.farm or not args.rehearse):
        parser.error('--night-meal requires --rehearse without --boats/--farm')
    root = Path(__file__).resolve().parents[1]
    source = args.source_world.resolve()
    runtime = args.runtime.resolve()
    server = args.server_home.resolve()
    game = source.parent.parent if source.parent.name == 'saves' else source.parent
    if not runtime.is_relative_to(root / 'runs') or runtime.exists() or source == runtime / source.name:
        raise ValueError('Requires a fresh diagnostic runtime under runs/')
    if 'eula=true' not in (server / 'eula.txt').read_text():
        raise ValueError('Requires the existing prepared server acceptance')
    with (game / '.villagers-client.lock').open('a+b') as client_lock, (source / 'session.lock').open('rb') as world_lock:
        fcntl.flock(client_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        fcntl.lockf(world_lock, fcntl.LOCK_SH | fcntl.LOCK_NB)
        runtime.mkdir(parents=True)
        shutil.copytree(source, runtime / source.name)
        shutil.copytree(game / 'config', runtime / 'config')
        shutil.copytree(game / 'mods', runtime / 'mods')
        pins = {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in source.rglob('*') if p.is_file() and p.name != 'session.lock'}
    for name in ['fabric-server-launch.jar', 'fabric-server-launcher.properties', 'server.jar', 'libraries', 'versions']:
        (runtime / name).symlink_to(server / name)
    shutil.copy2(server / 'eula.txt', runtime)
    for jar in (server / 'mods').glob('fabric-api-*.jar'):
        shutil.copy2(jar, runtime / 'mods' / jar.name)
    shutil.copy2(root / 'build/libs/villagers-0.4.1-dev.jar', runtime / 'mods')
    if args.rehearse and not args.boats:
        config_file = runtime / 'config/villagers.json'
        values = json.loads(config_file.read_text())
        values['hayForaging'] = True
        config_file.write_text(json.dumps(values, indent=2)+'\n')
    (runtime / 'food-diagnostic-copy.json').write_text(json.dumps({'source': str(source), 'world': source.name,
        'primaryFileHashes': pins, 'purpose': 'Bounded normal-AI rehearsal; no primary-world growth evidence.' if args.rehearse
        else 'Frozen spatial/path inspection only; no survival or growth evidence.'}, indent=2))
    (runtime / 'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=false\n'
        f'level-name={source.name}\nview-distance=2\nsimulation-distance=2\ndifficulty=normal\n'
        'pause-when-empty-seconds=-1\nmax-tick-time=180000\n')
    java = Path(os.environ['JAVA_HOME']) / 'bin'
    classes = runtime / 'probe-classes'
    classes.mkdir()
    probe = 'NativeIllagerHabitatProbe' if args.illagers else 'NativeWaterExitProbe' if args.water_exit or args.water_recovery else 'NightGrainMealProbe' if args.night_meal else 'NativeFarmProductionProbe' if args.farm else 'NativeBoatAccessProbe' if args.boats else 'FoodAccessRehearsalProbe' if args.rehearse else 'FoodAccessAuditProbe'
    subprocess.run([str(java / 'javac'), '-cp', (root / 'build/probe-classpath.txt').read_text(), '-d', str(classes),
                    str(root / f'tools/tests/java/local/villagers/{probe}.java')], check=True)
    with zipfile.ZipFile(runtime / 'mods/food-access-audit.jar', 'w') as jar:
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'food-access-audit', 'version': '1',
            'environment': 'server', 'entrypoints': {'main': [f'local.villagers.{probe}']}, 'depends': {'villagers': '*'}}))
        for p in classes.rglob('*.class'):
            jar.write(p, p.relative_to(classes))
    (runtime / 'artifact-pins.json').write_text(json.dumps({p.name: hashlib.sha256(p.read_bytes()).hexdigest()
        for p in (runtime / 'mods').glob('*.jar')}, indent=2))
    with (runtime / 'audit.log').open('w') as log:
        process = subprocess.Popen([str(java / 'java'), '-Xmx4G', f'-Dvillagers.boats.rehearse={str(args.rehearse).lower()}', f'-Dvillagers.audit.expandedBorder={str(args.expand_copy_border).lower()}', f'-Dvillagers.food.allResidents={str(args.all_residents).lower()}', f'-Dvillagers.food.healing={str(args.healing).lower()}', f'-Dvillagers.farm.meal={str(args.farm_meal).lower()}', f'-Dvillagers.farm.grain={str(args.grain_meal).lower()}', f'-Dvillagers.farm.sustain={str(args.sustain).lower()}', f'-Dvillagers.farm.windowTicks={args.sustain_ticks}', f'-Dvillagers.farm.tps={args.tps}', f'-Dvillagers.illagers.windowTicks={args.illager_ticks}', f'-Dvillagers.water.recovery={str(args.water_recovery).lower()}', '-jar', 'fabric-server-launch.jar', 'nogui'],
                                   cwd=runtime, stdout=log, stderr=subprocess.STDOUT)
        (runtime / 'process.json').write_text(json.dumps({'pid': process.pid}))
        print(f'Copied-world {"rehearsal" if args.rehearse else "frozen audit"} PID {process.pid}: {runtime}', flush=True)
        try:
            code = process.wait(timeout=max(840,args.illager_ticks/args.tps+120) if args.illagers else max(840,args.sustain_ticks/args.tps+120) if args.sustain else 840 if args.farm or args.healing or args.illagers else 360 if args.rehearse else 240)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=30)
            raise RuntimeError('Owned copied-world audit exceeded its observation budget')
    (runtime / 'terminal.json').write_text(json.dumps({'pid': process.pid, 'exitCode': code}))
    after = {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in source.rglob('*') if p.is_file() and p.name != 'session.lock'}
    if after != pins:
        raise RuntimeError('Primary source files changed during the copied-world audit')
    result = runtime / ('native-illager-habitats.json' if args.illagers else 'native-water-exit.json' if args.water_exit or args.water_recovery else 'night-grain-meal.json' if args.night_meal else 'native-farm-production.json' if args.farm else 'native-boat-access.json' if args.boats else 'food-access-rehearsal.json' if args.rehearse else 'food-access-audit.json')
    if code or not result.exists() or json.loads(result.read_text())['status'] != 'passed':
        raise RuntimeError(f'Copied-world audit failed: {runtime / "audit.log"}')
    print(f'COPIED-WORLD {"REHEARSAL" if args.rehearse else "FROZEN AUDIT"} PASS; primary unchanged: {result}')


if __name__ == '__main__':
    main()
