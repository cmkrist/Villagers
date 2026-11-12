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
import sys
import zipfile
from mind_schema_audit import audit_jar_minds
from population_audit import compare_population


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-world', type=Path, required=True)
    parser.add_argument('--runtime', type=Path, required=True)
    parser.add_argument('--server-home', type=Path, required=True)
    parser.add_argument('--production-jar',type=Path,help='Use an explicitly verified production JAR instead of the shared build output')
    parser.add_argument('--port', type=int, default=25595)
    parser.add_argument('--boats', action='store_true', help='Inspect the actual grassland boat routes instead of desert food access')
    parser.add_argument('--boat-ticks',type=int,default=2400,help='Bounded normal boat AI window (1000..48000); longer windows also observe stone expeditions')
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
    parser.add_argument('--all-residents', action='store_true', help='Load the saved managed residents in every region during the copied rehearsal')
    parser.add_argument('--focus-resident',help='Inspect or rehearse one existing saved resident by UUID, with all saved residents loaded')
    parser.add_argument('--focus-ticks',type=int,default=8000,help='Focused resident observation budget,100..48000 ticks')
    parser.add_argument('--focus-cycle',action='store_true',help='Observe the full focused window instead of stopping after the first meal')
    parser.add_argument('--healing', action='store_true', help='Follow saved injured desert residents until naturally recovered')
    parser.add_argument('--food-cycle',action='store_true',help='Observe the saved desert residents for a full 24000-tick food cycle, including work after recovery')
    parser.add_argument('--illagers', action='store_true', help='Inspect existing native illagers, habitats and settlement resources while frozen')
    parser.add_argument('--illager-min-health', type=int, choices=range(1,21), default=4, help='Copied illager rehearsal stop boundary; lower values permit testing recovery of already injured saved bodies without changing damage')
    parser.add_argument('--care-startup-scope',type=Path,help='Verify and enroll the exact reviewed existing worker while frozen before this COPY resumes')
    parser.add_argument('--last-health-recovery', action='store_true', help='COPY only: permit recovery at1HP, stopping before the next lethal starvation interval')
    parser.add_argument('--enroll-camp-copy',action='store_true',help='In a frozen illager COPY, enroll only an existing resource-ready pair before route inspection')
    parser.add_argument('--recruit-worker-copy',metavar='RESIDENT_UUID',help='In a frozen illager COPY, activate one existing local worker near this resident, with no supplied resources')
    parser.add_argument('--combat-recovery-from',type=Path,help='COPY only: replay one recorded actual encounter, stopping the known actor at8HP and any other combat at first hit')
    parser.add_argument('--cell-escape', action='store_true', help='Opt in to experimental real cell-door breaking in an illager rehearsal COPY')
    parser.add_argument('--camp-passages',action='store_true',help='Opt in to a bounded real entrance to an inaccessible free native bunk in an illager rehearsal COPY')
    parser.add_argument('--camp-meals',action='store_true',help='Opt in to heard captive requests and real local food deliveries in an illager rehearsal COPY')
    parser.add_argument('--camp-farms',action='store_true',help='Opt in to real seed gathering, hoe preparation and native camp crop planting in a rehearsal COPY')
    parser.add_argument('--quarry',action='store_true',help='Opt in to a bounded real native quarry in a boat rehearsal COPY')
    milestones=parser.add_mutually_exclusive_group()
    milestones.add_argument('--until-iron-ingots',action='store_true',help='Observe until the adventurer owns three legitimately obtained iron ingots')
    milestones.add_argument('--until-iron-armor',action='store_true',help='Observe until the existing adventurer wears a full earned iron-tier armor set')
    milestones.add_argument('--until-iron-pickaxe',action='store_true',help='Observe until the adventurer actually owns an iron-tier pickaxe')
    milestones.add_argument('--until-stone-pickaxe',action='store_true',help='Observe beyond the first three stone drops until a real pickaxe is crafted or the boat tick budget ends')
    parser.add_argument('--illager-ticks',type=int,default=8000,help='Bounded normal-AI window for --illagers --rehearse (1000..48000)')
    args = parser.parse_args()
    if args.care_startup_scope and not (args.illagers and args.rehearse and args.all_residents and args.illager_min_health==1 and args.last_health_recovery):
        parser.error('--care-startup-scope requires all-resident illager rehearsal with min-health1 and last-health-recovery')
    if args.last_health_recovery and not (args.illagers and args.rehearse and args.illager_min_health == 1):
        parser.error('--last-health-recovery requires --illagers --rehearse --illager-min-health 1')
    if args.illager_min_health != 4 and not (args.illagers and args.rehearse):
        parser.error('--illager-min-health requires --illagers --rehearse')
    if args.combat_recovery_from and not (args.illagers and args.rehearse and args.all_residents):
        parser.error('--combat-recovery-from requires --illagers --rehearse --all-residents')
    if args.recruit_worker_copy:
        import uuid
        args.recruit_worker_copy=str(uuid.UUID(args.recruit_worker_copy))
        if not args.illagers or args.rehearse or args.enroll_camp_copy:parser.error('--recruit-worker-copy requires a separate frozen --illagers audit')
        args.all_residents=True
    if args.enroll_camp_copy:
        if not args.illagers or args.rehearse:parser.error('--enroll-camp-copy requires a frozen --illagers audit')
        args.all_residents=True
    if args.focus_cycle and not (args.focus_resident and args.rehearse):parser.error('--focus-cycle requires --focus-resident --rehearse')
    if not 100<=args.focus_ticks<=48000 or args.focus_ticks!=8000 and not args.focus_resident:
        parser.error('--focus-ticks requires --focus-resident and must be100..48000')
    if args.focus_resident:
        import uuid
        args.focus_resident=str(uuid.UUID(args.focus_resident))
        if any((args.boats,args.illagers,args.farm,args.night_meal,args.water_exit,args.water_recovery,args.food_cycle)):
            parser.error('--focus-resident is a separate resident meal audit')
        args.all_residents=True
    if args.until_iron_armor and (not args.boats or not args.rehearse):
        parser.error('--until-iron-armor requires --boats --rehearse')
    if args.until_iron_pickaxe and (not args.boats or not args.rehearse):
        parser.error('--until-iron-pickaxe requires --boats --rehearse')
    if args.until_iron_ingots and (not args.boats or not args.rehearse or args.until_stone_pickaxe):
        parser.error('--until-iron-ingots requires --boats --rehearse and is separate from --until-stone-pickaxe')
    if args.camp_meals and not (args.rehearse and (args.illagers or args.focus_resident or args.boats and args.all_residents)):
        parser.error('--camp-meals requires an illager, focused resident or all-resident boat --rehearse observation')
    if args.camp_farms and not (args.illagers and args.rehearse):parser.error('--camp-farms requires --illagers --rehearse')
    if args.food_cycle and (not args.rehearse or any((args.healing,args.boats,args.farm,args.farm_meal,args.grain_meal,args.sustain,args.night_meal,args.water_recovery,args.water_exit,args.illagers))):
        parser.error('--food-cycle requires a separate default desert --rehearse observation')
    if args.cell_escape and not (args.illagers and args.rehearse):
        parser.error('--cell-escape requires --illagers --rehearse')
    if args.camp_passages and not ((args.illagers or args.focus_resident) and args.rehearse):
        parser.error('--camp-passages requires an illager or focused resident --rehearse observation')
    if args.quarry and not (args.rehearse and (args.boats or args.illagers)):
        parser.error('--quarry requires boat or all-resident illager --rehearse observation')
    if args.until_stone_pickaxe and not (args.boats and args.rehearse):
        parser.error('--until-stone-pickaxe requires --boats --rehearse')
    if not 1000<=args.boat_ticks<=48000 or args.boat_ticks!=2400 and not (args.boats and args.rehearse):
        parser.error('--boat-ticks requires --boats --rehearse and must be1000..48000')
    if not 1000<=args.illager_ticks<=48000 or args.illager_ticks!=8000 and not (args.illagers and args.rehearse):
        parser.error('--illager-ticks requires --illagers --rehearse and must be1000..48000')
    if args.illagers and (args.boats or args.farm or args.sustain or args.water_exit or args.water_recovery or args.night_meal):
        parser.error('--illagers is a separate frozen habitat audit')
    if args.healing and (not args.rehearse or args.boats or args.farm or args.farm_meal or args.grain_meal or args.sustain or args.night_meal or args.water_recovery or args.water_exit):
        parser.error('--healing requires the default desert --rehearse observation')
    if args.all_residents and (not (args.rehearse or args.focus_resident or args.enroll_camp_copy or args.recruit_worker_copy) or args.farm or args.farm_meal or args.grain_meal or args.sustain or args.night_meal or args.water_recovery or args.water_exit):
        parser.error('--all-residents requires a desert, boat or illager --rehearse observation')
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
    if not 1000<=args.sustain_ticks<=48000 or not args.sustain and (args.sustain_ticks!=14000 or args.tps!=20 and not (args.healing or args.food_cycle or args.illagers or args.boats and args.rehearse)):
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
    production_jar=args.production_jar.resolve() if args.production_jar else root / 'build/libs/villagers-0.4.1-dev.jar'
    java = Path(os.environ['JAVA_HOME']) / 'bin'
    mind_compatibility = audit_jar_minds(source, production_jar, java / 'javap')
    game = source.parent.parent if source.parent.name == 'saves' else source.parent
    if not runtime.is_relative_to(root / 'runs') or runtime.exists() or source == runtime / source.name:
        raise ValueError('Requires a fresh diagnostic runtime under runs/')
    if 'eula=true' not in (server / 'eula.txt').read_text():
        raise ValueError('Requires the existing prepared server acceptance')
    with (game / '.villagers-client.lock').open('a+b') as client_lock, (source / 'session.lock').open('rb') as world_lock:
        fcntl.flock(client_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        fcntl.lockf(world_lock, fcntl.LOCK_SH | fcntl.LOCK_NB)
        runtime.mkdir(parents=True)
        (runtime / 'mind-schema-compatibility.json').write_text(json.dumps(mind_compatibility, indent=2)+'\n')
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
    shutil.copy2(production_jar, runtime / 'mods/villagers-0.4.1-dev.jar')
    # Recheck the copied inputs too: a shared build output may have changed
    # after preflight. The receipt describes exactly what the server will use.
    mind_compatibility = audit_jar_minds(runtime / source.name,
        runtime / 'mods/villagers-0.4.1-dev.jar', java / 'javap')
    (runtime / 'mind-schema-compatibility.json').write_text(json.dumps(mind_compatibility, indent=2)+'\n')
    if args.rehearse:
        config_file = runtime / 'config/villagers.json'
        values = json.loads(config_file.read_text())
        if not args.boats:
            values['hayForaging'] = True
        values['nativeCellEscape'] = args.cell_escape
        values['nativeCampPassages'] = args.camp_passages
        values['nativeCampMeals'] = args.camp_meals
        values['nativeCampFarming'] = args.camp_farms
        values['nativeAdventureQuarry'] = args.quarry
        config_file.write_text(json.dumps(values, indent=2)+'\n')
    (runtime / 'food-diagnostic-copy.json').write_text(json.dumps({'source': str(source), 'world': source.name,
        'primaryFileHashes': pins, 'purpose': 'Bounded normal-AI rehearsal; no primary-world growth evidence.' if args.rehearse
        else 'Frozen spatial/path inspection only; no survival or growth evidence.'}, indent=2))
    (runtime / 'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={args.port}\nonline-mode=false\n'
        f'level-name={source.name}\nview-distance=2\nsimulation-distance=2\ndifficulty=normal\n'
        'pause-when-empty-seconds=-1\nmax-tick-time=180000\n')
    subprocess.run([sys.executable,str(root/'tools/audit-save-entities.py'),str(runtime/source.name),
                    '--output',str(runtime/'bodies-before.json')],check=True,stdout=subprocess.DEVNULL)
    startup_options=[]
    if args.care_startup_scope:
        scope_path=runtime/'care-startup-scope.json'
        shutil.copy2(args.care_startup_scope.resolve(),scope_path)
        startup_options=['-Dvillagers.lab=true','-Dvillagers.trial=native-regions',f'-Dvillagers.lab.maxPlayTicks={args.illager_ticks}','-Dvillagers.lab.minHealth=1','-Dvillagers.lab.lastHealthRecovery=true',f'-Dvillagers.lab.careStartupScope={scope_path}']
    if args.combat_recovery_from:
        import uuid
        receipt_dir=args.combat_recovery_from.resolve()
        if not receipt_dir.is_relative_to(root/'runs'):raise ValueError('Recovery requires an actual local observation receipt')
        prior=json.loads((receipt_dir/'result.json').read_text())
        hit=prior.get('combatStop',{})
        actor=str(uuid.UUID(hit['actor']));attacker=str(uuid.UUID(hit['attacker']))
        expected=next(a for a in json.loads((receipt_dir/'bodies-after.json').read_text())['bodies'] if a['id']==actor)
        actual=next(a for a in json.loads((runtime/'bodies-before.json').read_text())['bodies'] if a['id']==actor)
        if not prior.get('sourceUnchangedDuringRun') or prior.get('newlyDeadOrMissingIds') or expected!=actual or actual['health']<16:
            raise ValueError('Recovery requires the unchanged saved body from a reviewed actual first-hit stop, at least16HP')
        (runtime/'combat-recovery-scope.json').write_text(json.dumps({'actor':actor,'attacker':attacker,'minHealth':8,
            'receipt':str(receipt_dir),'recordedHit':hit,'scope':'Copied encounter only. Native damage retained; every other combat still stops at first hit; no primary or body changes.'},indent=2)+'\n')
    if args.all_residents:
        shutil.copy2(runtime/'bodies-before.json',runtime/'managed-residents-before.json')
    classes = runtime / 'probe-classes'
    classes.mkdir()
    probe = 'ResidentMealProbe' if args.focus_resident else 'NativeIllagerHabitatProbe' if args.illagers else 'NativeWaterExitProbe' if args.water_exit or args.water_recovery else 'NightGrainMealProbe' if args.night_meal else 'NativeFarmProductionProbe' if args.farm else 'NativeBoatAccessProbe' if args.boats else 'FoodAccessRehearsalProbe' if args.rehearse else 'FoodAccessAuditProbe'
    # Compile against the exact copied mod artifact, never mutable Gradle class
    # directories that an independent subsequent build can replace mid-compile.
    probe_classpath = (root / 'build/probe-classpath.txt').read_text()
    for mutable in ('build/classes/java/main','build/resources/main'):
        probe_classpath = probe_classpath.replace(str(root / mutable),str(runtime / 'mods/villagers-0.4.1-dev.jar'))
    subprocess.run([str(java / 'javac'), '-cp', probe_classpath, '-d', str(classes),
                    str(root / f'tools/tests/java/local/villagers/{probe}.java'),
                    str(root / 'tools/tests/java/local/villagers/ProbeResidents.java')], check=True)
    with zipfile.ZipFile(runtime / 'mods/food-access-audit.jar', 'w') as jar:
        jar.writestr('fabric.mod.json', json.dumps({'schemaVersion': 1, 'id': 'food-access-audit', 'version': '1',
            'environment': 'server', 'entrypoints': {'main': [f'local.villagers.{probe}']}, 'depends': {'villagers': '*'}}))
        for p in classes.rglob('*.class'):
            jar.write(p, p.relative_to(classes))
    (runtime / 'artifact-pins.json').write_text(json.dumps({p.name: hashlib.sha256(p.read_bytes()).hexdigest()
        for p in (runtime / 'mods').glob('*.jar')}, indent=2))
    with (runtime / 'audit.log').open('w') as log:
        if args.illagers and args.rehearse:
            (runtime / 'observation-guard.json').write_text(json.dumps({'minimumHealth': args.illager_min_health, 'lastHealthRecovery': args.last_health_recovery,
                'scope': 'Copied rehearsal stop boundary only; native damage and saved health unchanged.'}, indent=2) + '\n')
        process = subprocess.Popen([str(java / 'java'), '-Xmx4G', *startup_options, f'-Dvillagers.illagers.lastHealthRecovery={str(args.last_health_recovery).lower()}', f'-Dvillagers.illagers.minHealth={args.illager_min_health}', f'-Dvillagers.illagers.enrollFrozen={str(args.enroll_camp_copy).lower()}', f'-Dvillagers.illagers.recruitWorker={args.recruit_worker_copy or ""}', f'-Dvillagers.food.focusCycle={str(args.focus_cycle).lower()}', f'-Dvillagers.food.focusTicks={args.focus_ticks}', f'-Dvillagers.food.focus={args.focus_resident}', f'-Dvillagers.boats.untilIronArmor={str(args.until_iron_armor).lower()}', f'-Dvillagers.boats.untilIronPickaxe={str(args.until_iron_pickaxe).lower()}', f'-Dvillagers.boats.untilIronIngots={str(args.until_iron_ingots).lower()}', f'-Dvillagers.boats.untilStonePickaxe={str(args.until_stone_pickaxe).lower()}', f'-Dvillagers.boats.rehearse={str(args.rehearse).lower()}', f'-Dvillagers.audit.expandedBorder={str(args.expand_copy_border).lower()}', f'-Dvillagers.food.allResidents={str(args.all_residents).lower()}', f'-Dvillagers.food.healing={str(args.healing).lower()}', f'-Dvillagers.food.cycle={str(args.food_cycle).lower()}', f'-Dvillagers.farm.meal={str(args.farm_meal).lower()}', f'-Dvillagers.farm.grain={str(args.grain_meal).lower()}', f'-Dvillagers.farm.sustain={str(args.sustain).lower()}', f'-Dvillagers.farm.windowTicks={args.sustain_ticks}', f'-Dvillagers.farm.tps={args.tps}', f'-Dvillagers.illagers.windowTicks={args.illager_ticks}', f'-Dvillagers.boats.windowTicks={args.boat_ticks}', f'-Dvillagers.water.recovery={str(args.water_recovery).lower()}', '-jar', 'fabric-server-launch.jar', 'nogui'],
                                   cwd=runtime, stdout=log, stderr=subprocess.STDOUT)
        (runtime / 'process.json').write_text(json.dumps({'pid': process.pid}))
        print(f'Copied-world {"rehearsal" if args.rehearse else "frozen audit"} PID {process.pid}: {runtime}', flush=True)
        try:
            code = process.wait(timeout=max(840,args.focus_ticks/args.tps+120) if args.focus_resident else max(840,24000/args.tps+120) if args.food_cycle else max(840,args.boat_ticks/args.tps+120) if args.boats and args.rehearse else max(840,args.illager_ticks/args.tps+120) if args.illagers else max(840,args.sustain_ticks/args.tps+120) if args.sustain else 840 if args.farm or args.healing or args.illagers else 360 if args.rehearse else 240)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=30)
            raise RuntimeError('Owned copied-world audit exceeded its observation budget')
    (runtime / 'terminal.json').write_text(json.dumps({'pid': process.pid, 'exitCode': code}))
    subprocess.run([sys.executable,str(root/'tools/audit-save-entities.py'),str(runtime/source.name),
                    '--output',str(runtime/'bodies-after.json')],check=True,stdout=subprocess.DEVNULL)
    preservation=compare_population(json.loads((runtime/'bodies-before.json').read_text()),json.loads((runtime/'bodies-after.json').read_text()))
    (runtime/'population-preservation.json').write_text(json.dumps(preservation,indent=2)+'\n')
    if preservation['missingOriginalIds'] or preservation['newlyDeadOrMissingIds']:
        raise RuntimeError(f'Copied-world population preservation failed; actual saved world retained: {runtime / "population-preservation.json"}')
    after = {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest()
             for p in source.rglob('*') if p.is_file() and p.name != 'session.lock'}
    if after != pins:
        raise RuntimeError('Primary source files changed during the copied-world audit')
    if 'its mind file has an unsupported schema or could not be quarantined' in (runtime/'audit.log').read_text():
        raise RuntimeError('Copied-world mind restoration failed; fallback minds invalidate survival and progression evidence')
    if (runtime/'native-combat-stop.json').exists():
        raise RuntimeError(f'Copied-world combat observation stopped after the first hit: {runtime / "native-combat-stop.json"}')
    result = runtime / ('resident-meal.json' if args.focus_resident else 'native-illager-habitats.json' if args.illagers else 'native-water-exit.json' if args.water_exit or args.water_recovery else 'night-grain-meal.json' if args.night_meal else 'native-farm-production.json' if args.farm else 'native-boat-access.json' if args.boats else 'food-access-rehearsal.json' if args.rehearse else 'food-access-audit.json')
    if code or not result.exists() or json.loads(result.read_text())['status'] != 'passed':
        raise RuntimeError(f'Copied-world audit failed: {runtime / "audit.log"}')
    print(f'COPIED-WORLD {"REHEARSAL" if args.rehearse else "FROZEN AUDIT"} PASS; primary unchanged: {result}')


if __name__ == '__main__':
    main()
