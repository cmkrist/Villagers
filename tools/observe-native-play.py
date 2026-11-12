#!/usr/bin/env python3
"""Observe the existing primary native save with recorded tick pacing and a bounded health/tick guard."""
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid
import zipfile
from population_audit import compare_population


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--runtime',type=Path,required=True,help='Fresh evidence directory directly under runs/')
    parser.add_argument('--source-snapshot',type=Path,help='Immutable tested source snapshot with source-manifest.json and artifact.json; compile the client from those exact sources')
    parser.add_argument('--ticks',type=int,default=6000)
    parser.add_argument('--min-health',type=int,default=12)
    parser.add_argument('--last-health-recovery',action='store_true',help='With --min-health1, observe actual fed 1HP villagers or stop before their next lethal starvation interval; native health and damage unchanged')
    parser.add_argument('--tps',type=int,choices=(20,40,80),default=20,help='Native tick pacing; all game ticks still run, default normal20TPS')
    parser.add_argument('--simulation-distance',type=int,choices=range(2,33),help='Set the native client simulation radius before world open, bounded by existing render distance')
    parser.add_argument('--observer',nargs=3,type=float,default=(-555,90,-495),metavar=('X','Y','Z'))
    parser.add_argument('--allow-quarry',action='store_true',help='Permit an already-enabled quarry config after its separate gameplay verification')
    parser.add_argument('--enroll-native-illagers',action='store_true',help='Enroll an existing resource-ready native pair after the observer reaches camp; creates no bodies or supplies')
    parser.add_argument('--recruit-native-worker',type=uuid.UUID,help='Adopt one eligible existing camp pillager near this resident after the separately verified copied-world trial')
    parser.add_argument('--care-resume-scope',type=Path,help='Resume an already-enrolled reviewed worker under the frozen startup check; no enrollment permitted')
    parser.add_argument('--startup-worker-proof',type=Path,help='Frozen copied enrollment receipt: verify the exact saved worker before any game tick and resume explicitly')
    parser.add_argument('--reviewed-combat-from',type=Path,help='Continue one recorded encounter only after three preserved copied recovery proofs; known actor stops at8HP')
    parser.add_argument('--recovery-proof',type=Path,action='append',default=[],help='Completed copied recovery runtime; repeat for three independent proofs')
    parser.add_argument('--recovery-jar',type=Path,help='Verified JAR whose survival/controller classes match every recovery proof')
    milestones=parser.add_mutually_exclusive_group()
    milestones.add_argument('--until-stone-pickaxe',action='store_true',help='Save and stop after the existing adventurer actually prepares a stone-tier pickaxe, or at the tick budget')
    milestones.add_argument('--until-iron-ingots',action='store_true',help='Save and stop after the existing adventurer owns three legitimately obtained iron ingots, or at the tick budget')
    milestones.add_argument('--until-iron-pickaxe',action='store_true',help='Save and stop after the existing adventurer actually owns an iron-tier pickaxe')
    args=parser.parse_args()
    if bool(args.reviewed_combat_from)!=bool(args.recovery_jar) or bool(args.reviewed_combat_from)!=bool(args.recovery_proof):
        parser.error('Reviewed combat requires its receipt, verified JAR and three recovery proofs together')
    if args.care_resume_scope and (args.startup_worker_proof or args.recruit_native_worker or args.enroll_native_illagers or not args.last_health_recovery or args.min_health!=1):parser.error('--care-resume-scope requires final-health recovery and an existing worker, separately from enrollment')
    if args.startup_worker_proof and not (args.recruit_native_worker and args.last_health_recovery and args.min_health==1):parser.error('--startup-worker-proof requires worker reference and final-health recovery')
    if args.recruit_native_worker and args.enroll_native_illagers:parser.error('Worker adoption is separate from founder enrollment')
    if args.last_health_recovery and args.min_health!=1:parser.error('--last-health-recovery requires --min-health1')
    if not 100<=args.ticks<=12000 or not 1<=args.min_health<=20 or not all(math.isfinite(n) for n in args.observer):
        parser.error('Ticks must be100..12000, min-health1..20, observer coordinates finite')
    root=Path(__file__).resolve().parents[1];run=root/'runs/native-four-regions-play';world=run/'saves/NativeFourRegions';out=args.runtime.resolve()
    if out.parent!=root/'runs' or out.exists():parser.error('Requires a fresh evidence directory directly under runs/')
    if not (world/'level.dat').is_file() or not (world/'native-four-regions.json').is_file():raise RuntimeError('Existing native primary required; never create a replacement world')
    config=json.loads((run/'config/villagers.json').read_text())
    if config.get('nativeCellEscape',False) or config.get('nativeAdventureQuarry',False) and not args.allow_quarry:raise RuntimeError('Experimental body/terrain work must remain disabled unless separately verified')
    if any('probe' in p.name or 'food-access-audit' in p.name for p in (run/'mods').glob('*.jar')):raise RuntimeError('Primary must not contain probe mods')
    out.mkdir()
    def hashes(folder):
        return {str(p.relative_to(folder)):hashlib.sha256(p.read_bytes()).hexdigest() for p in folder.rglob('*') if p.is_file() and p.name!='session.lock'}
    old=root/'runs/four-biomes-play/saves/FourBiomesWar';old_before=hashes(old)
    (out/'preserved-save-hashes.json').write_text(json.dumps(old_before,indent=2))
    world_before=hashes(world)
    (out/'before-hashes.json').write_text(json.dumps(world_before,indent=2))
    source_root=args.source_snapshot.resolve() if args.source_snapshot else root
    snapshot_manifest=None
    launch_options=[]
    if args.source_snapshot:
        snapshot_manifest=json.loads((source_root/'source-manifest.json').read_text())
        if any(not (source_root/n).is_file() or hashlib.sha256((source_root/n).read_bytes()).hexdigest()!=h for n,h in snapshot_manifest.items()):
            raise ValueError('Tested source snapshot changed')
        artifact=json.loads((source_root/'artifact.json').read_text())
        if artifact.get('failures')!=0 or artifact.get('tests',0)<1 or hashlib.sha256((source_root/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest()!=artifact['jarSha256']:
            raise ValueError('Snapshot needs a passing pinned build')
        pinned_build=run/'.build-pinned'
        init=out/'pinned-client.gradle'
        init.write_text('allprojects { afterEvaluate {\n'
            +'  sourceSets.main.java.setSrcDirs(['+json.dumps(str(source_root/'src/main/java'))+'])\n'
            +'  sourceSets.main.resources.setSrcDirs(['+json.dumps(str(source_root/'src/main/resources'))+'])\n'
            +'  layout.buildDirectory.set(file('+json.dumps(str(pinned_build))+'))\n'
            +'  tasks.named("runClient").configure { doFirst {\n'
            +'    def pinnedJar = new java.util.zip.ZipFile('+json.dumps(str(source_root/'build/libs/villagers-0.4.1-dev.jar'))+')\n'
            +'    try { fileTree(layout.buildDirectory.dir("classes/java/main")).matching { include "**/*.class" }.each { f ->\n'
            +'      def name = layout.buildDirectory.dir("classes/java/main").get().asFile.toPath().relativize(f.toPath()).toString()\n'
            +'      def entry = pinnedJar.getEntry(name)\n'
            +'      if (entry == null || !java.util.Arrays.equals(f.bytes, pinnedJar.getInputStream(entry).readAllBytes())) throw new GradleException("Compiled client class differs from tested artifact: " + name)\n'
            +'    } } finally { pinnedJar.close() }\n'
            +'  } }\n'
            +'} }\n')
        launch_options=['--init-script',str(init)]
        shutil.copy2(source_root/'source-manifest.json',out/'pinned-source-manifest.json')
        shutil.copy2(source_root/'artifact.json',out/'pinned-artifact.json')
    source_hashes=hashes(source_root/'src/main')
    (out/'source-before.json').write_text(json.dumps(source_hashes,indent=2))
    def audit(name):
        subprocess.run([sys.executable,str(root/'tools/audit-save-entities.py'),str(world),'--output',str(out/name)],check=True,stdout=subprocess.DEVNULL)
        return json.loads((out/name).read_text())
    before=audit('bodies-before.json')
    recovery_scope=None
    if args.reviewed_combat_from:
        receipt=args.reviewed_combat_from.resolve()
        if not receipt.is_relative_to(root/'runs'):raise ValueError('Reviewed encounter must be a local actual observation')
        recorded=json.loads((receipt/'result.json').read_text());hit=recorded.get('combatStop',{})
        actor=str(uuid.UUID(hit['actor']));attacker=str(uuid.UUID(hit['attacker']))
        expected=next(a for a in json.loads((receipt/'bodies-after.json').read_text())['bodies'] if a['id']==actor)
        actual=next(a for a in before['bodies'] if a['id']==actor)
        if expected!=actual or actual['health']<16 or not recorded.get('sourceUnchangedDuringRun') or recorded.get('newlyDeadOrMissingIds'):
            raise ValueError('Actual body must still match the reviewed first-hit stop, with at least16HP')
        proofs=sorted(set(p.resolve() for p in args.recovery_proof))
        if len(proofs)<3:raise ValueError('Three independent completed copied recoveries are required')
        prefixes=('local/villagers/VillageSurvival','local/villagers/VillagerController','local/villagers/Session',
            'local/villagers/Villagers$','local/villagers/Villagers.class','local/villagers/mixin/VillagerMixin',
            'local/villagers/RefugeRoutes','local/villagers/NativePathLimits','local/villagers/FloorCentering')
        def controller_hashes(jar):
            with zipfile.ZipFile(jar) as archive:
                return {n:hashlib.sha256(archive.read(n)).hexdigest() for n in archive.namelist() if n.endswith('.class') and n.startswith(prefixes)}
        verified=controller_hashes(args.recovery_jar)
        if len(verified)<6:raise ValueError('Verified controller artifact is incomplete')
        for proof in proofs:
            if not proof.is_relative_to(root/'runs'):raise ValueError('Recovery proof must be a local copied runtime')
            scope=json.loads((proof/'combat-recovery-scope.json').read_text())
            state=json.loads((proof/'native-illager-habitats.json').read_text())
            pop=json.loads((proof/'population-preservation.json').read_text())
            pins=json.loads((proof/'food-diagnostic-copy.json').read_text())
            body=next(a for a in json.loads((proof/'bodies-after.json').read_text())['bodies'] if a['id']==actor)
            if scope['actor']!=actor or scope['attacker']!=attacker or scope['minHealth']!=8 or pins['primaryFileHashes']!=world_before:
                raise ValueError(f'Recovery proof belongs to different physical state: {proof}')
            if state['status']!='passed' or state.get('elapsed',0)<args.ticks or pop['missingOriginalIds'] or pop['newlyDeadOrMissingIds'] or body['health']<actual['health']:
                raise ValueError(f'Recovery proof did not demonstrate retained healthy survival: {proof}')
            if (proof/'native-combat-stop.json').exists() or json.loads((proof/'terminal.json').read_text())['exitCode']!=0:
                raise ValueError(f'Recovery proof stopped early: {proof}')
            if controller_hashes(proof/'mods/villagers-0.4.1-dev.jar')!=verified:raise ValueError(f'Controller differs from the proven artifact: {proof}')
        recovery_scope={'actor':actor,'attacker':attacker,'minHealth':8,'receipt':str(receipt),'proofs':[str(p) for p in proofs],
            'controllerHashes':verified,'scope':'One reviewed encounter only; every native hit retained; all other combat stops at first hit.'}
        (out/'reviewed-combat-scope.json').write_text(json.dumps(recovery_scope,indent=2)+'\n')
    nominated=[a for a in before['bodies'] if 'villagers:adventurer' in a['tags']]
    if len(nominated)!=1 or nominated[0]['health']<args.min_health:raise RuntimeError('One living recorded adventurer above the health guard is required')
    env=os.environ.copy();env.update(VILLAGERS_PLAY_TICKS=str(args.ticks),VILLAGERS_PLAY_MIN_HEALTH=str(args.min_health),VILLAGERS_PLAY_STARVATION_GUARD='1')
    if recovery_scope:env['VILLAGERS_REVIEWED_COMBAT_SCOPE']=str(out/'reviewed-combat-scope.json')
    startup_scope=None
    if args.care_resume_scope:
        startup_scope=json.loads(args.care_resume_scope.resolve().read_text())
        resident=next(a for a in before['bodies'] if a['id']==startup_scope['resident'])
        worker=next(a for a in before['bodies'] if a['id']==startup_scope['worker'])
        if resident['position']!=startup_scope['residentPosition'] or resident['health']!=startup_scope['residentHealth']:
            raise ValueError('Resume scope differs from the current saved resident')
        if not {'villagers:creature_managed','villagers:native_illager_worker'}.issubset(worker['tags']):
            raise ValueError('Frozen resume requires an already-enrolled worker')
        (out/'care-startup-scope.json').write_text(json.dumps(startup_scope,indent=2)+'\n')
        env['VILLAGERS_CARE_STARTUP_SCOPE']=str(out/'care-startup-scope.json')
    if args.startup_worker_proof:
        proof=json.loads(args.startup_worker_proof.resolve().read_text())
        reference=str(args.recruit_native_worker)
        worker=str(uuid.UUID(proof['id']))
        body=next(a for a in before['bodies'] if a['id']==reference)
        actual_worker=next(a for a in before['bodies'] if a['id']==worker)
        if proof.get('reference')!=reference or proof.get('enrolledExisting')!=1 or proof.get('spawnedAdults')!=0 or proof.get('suppliedItems')!=0 or proof['bodyBefore']!=proof['bodyAfter']:
            raise ValueError('Startup requires a conserved copied-worker enrollment proof')
        if actual_worker['health']<=0 or actual_worker.get('noAI') or actual_worker.get('invulnerable'):
            raise ValueError('Startup worker must retain a living native body')
        startup_scope={'schema':1,'resident':reference,'worker':worker,'gameTime':proof['joinedAt'],'workerBody':proof['bodyBefore'],
            'residentHealth':body['health'],'residentPosition':body['position']}
        (out/'care-startup-scope.json').write_text(json.dumps(startup_scope,indent=2)+'\n')
        env['VILLAGERS_CARE_STARTUP_SCOPE']=str(out/'care-startup-scope.json')
    env['VILLAGERS_PLAY_LAST_HEALTH_RECOVERY']='1' if args.last_health_recovery else '0'
    (out/'recovery-observation-scope.json').write_text(json.dumps({'lastHealthRecovery':args.last_health_recovery,'minimumHealth':args.min_health,
        'scope':'Observer boundary only. With explicit min-health1 recovery, a fed 1HP villager may continue; a starving 1HP villager stops at timer60 before the next hit. Native damage, food and health remain unchanged.'},indent=2)+'\n')
    if args.simulation_distance is not None:env['VILLAGERS_SIMULATION_DISTANCE']=str(args.simulation_distance)
    began=time.time();monotonic=time.monotonic();evidence=None;last=0;last_move=0;last_report=0;milestone_requested=False;rate_requested=False;rate_verified=args.tps==20;last_observed_tps=None;observed_rates=set();enrollment_requested=False;observed_simulation=None
    with (out/'client.log').open('w') as log:
        process=subprocess.Popen([str(root/'tools/play-native-regions.sh'),'--no-daemon','--max-workers=2',*launch_options],cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT)
        (out/'process.json').write_text(json.dumps({'pid':process.pid,'started':began,'tickBudget':args.ticks,'minHealth':args.min_health,'observer':args.observer,'nativeTps':args.tps,'initialNativeTps':20,'nativeTpsIsRequestedTarget':True,'starvationGuard':True,'stopOnStonePickaxe':args.until_stone_pickaxe,'stopOnIronIngots':args.until_iron_ingots,'stopOnIronPickaxe':args.until_iron_pickaxe,'codeMode':'Gradle runClient compiled from verified immutable source snapshot' if args.source_snapshot else 'Gradle runClient development classes compiled from recorded src/main sources','sourceRoot':str(source_root),'compiledClassesDirectory':str((pinned_build if args.source_snapshot else run/'.build')/'classes/java/main'),'buildGradleSHA256':hashlib.sha256((root/'build.gradle').read_bytes()).hexdigest(),'enrollNativeIllagers':args.enroll_native_illagers,'config':config},indent=2))
        timeout=False;startup_failure=None
        while process.poll() is None:
            for f in (run/'evidence').glob('play-*/latest.json'):
                if f.stat().st_mtime>=began:evidence=f.parent
            if evidence:
                try:
                    data=json.loads((evidence/'latest.json').read_text());camera=data.get('camera',{})
                    observed_simulation=data.get('server_simulation_distance')
                    last_observed_tps=data.get('native_target_tps');observed_rates.add(last_observed_tps)
                    x,y,z=args.observer
                    if not data.get('screen') and time.time()-last_move>8 and (abs(camera.get('x',0)-x)>25 or abs(camera.get('z',0)-z)>25):
                        subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'gamemode spectator @a',f'tp @a {x} {y} {z}'],check=True)
                        last_move=time.time()
                    if time.time()-last>30:
                        print(json.dumps({'gameTime':data.get('game_time'),'screen':data.get('screen'),'camera':camera,'villagers':[(a.get('name'),a.get('health')) for a in data.get('villagers',[])]}),flush=True);last=time.time()
                    queue_busy=(run/'lab-command.txt').exists() or (run/'lab-command.processing.txt').exists()
                    if data.get('native_target_tps')==args.tps and data.get('training',{}).get('state')=='play':rate_verified=True
                    if startup_scope and not enrollment_requested and not data.get('screen') and not queue_busy:
                        receipt_path=out/'native-care-startup.json'
                        receipt=json.loads(receipt_path.read_text()) if receipt_path.exists() else {}
                        if receipt.get('stage')=='failed':
                            subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'quit'],check=True)
                            startup_failure='Frozen care startup failed; saved shutdown requested'
                        if receipt.get('stage')=='ready':
                            if receipt.get('worker')!=startup_scope['worker'] or receipt.get('gameTime')!=startup_scope['gameTime'] or receipt.get('bodyBefore')!=startup_scope['workerBody'] or receipt.get('bodyBefore')!=receipt.get('bodyAfter'):
                                subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'quit'],check=True)
                                startup_failure='Frozen startup receipt differs from the reviewed body'
                                continue
                            if args.care_resume_scope and (receipt.get('adoption',{}).get('enrolledExisting')!=0 or not receipt.get('adoption',{}).get('alreadyManaged')):
                                subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'quit'],check=True)
                                startup_failure='Continuation must preserve the already-enrolled worker role'
                                continue
                            commands=['villagers play','gamemode spectator @a',f'tp @a {x} {y} {z}',f'tick rate {args.tps}','villagers native-care-resume']
                            subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),*commands],check=True)
                            enrollment_requested=True;rate_requested=True;queue_busy=True
                    if args.recruit_native_worker and not startup_scope and not enrollment_requested and not data.get('screen') and not queue_busy and last_move and time.time()-last_move>=3:
                        if abs(camera.get('x',0)-x)<=25 and abs(camera.get('z',0)-z)<=25:
                            subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),f'villagers native-worker {args.recruit_native_worker}'],check=True)
                            (out/'native-worker-request.json').write_text(json.dumps({'gameTime':data.get('game_time'),'referenceResident':str(args.recruit_native_worker),'scope':'Adopt one existing eligible body; unchanged physical body and stock, no spawned adults or supplied items'},indent=2))
                            enrollment_requested=True;queue_busy=True
                    if args.tps!=20 and not rate_requested and not data.get('screen') and not queue_busy and (not startup_scope or enrollment_requested) and (not args.recruit_native_worker or enrollment_requested):
                        commands=[]
                        initialize_play=data.get('training',{}).get('state')!='play'
                        if initialize_play:
                            # Explicit play initialization prevents its later
                            # startup callback resetting the requested rate.
                            # Restore the observer in the same native batch.
                            commands.extend(['villagers play','gamemode spectator @a'])
                        commands.append(f'tick rate {args.tps}')
                        subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),*commands],check=True)
                        (out/'native-rate-request.json').write_text(json.dumps({'requestedTps':args.tps,'initializedPlayController':initialize_play,'gameTime':data.get('game_time'),'scope':'Native tick pacing only; no clock skip, body, inventory, health, crop or resource setters'},indent=2))
                        rate_requested=True;queue_busy=True
                    if args.enroll_native_illagers and not enrollment_requested and rate_verified and not data.get('screen') and not queue_busy and last_move and time.time()-last_move>=3:
                        if abs(camera.get('x',0)-x)<=25 and abs(camera.get('z',0)-z)<=25:
                            subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'villagers native-illagers'],check=True)
                            (out/'native-enrollment-request.json').write_text(json.dumps({'gameTime':data.get('game_time'),'scope':'Enroll existing locally resource-ready bodies only; no spawned bodies or supplied items'},indent=2))
                            enrollment_requested=True;queue_busy=True
                    if (args.until_stone_pickaxe or args.until_iron_ingots or args.until_iron_pickaxe) and not milestone_requested and not data.get('screen') and not queue_busy:
                        reports=[p for p in (world/'villagers/family-evidence').glob('status-*.json') if p.stat().st_mtime>=began]
                        latest=max(reports,key=lambda p:p.stat().st_mtime) if reports else None
                        state=json.loads(latest.read_text()) if latest else {}
                        hero=next((a for a in state.get('actors',[]) if a.get('uuid')==nominated[0]['id']),{})
                        reached=hero.get('ironPickaxePrepared') is True if args.until_iron_pickaxe else hero.get('ironIngots',0)>=3 if args.until_iron_ingots else hero.get('stonePickaxePrepared') is True
                        if reached:
                            shutil.copy2(latest,out/'milestone-native.json')
                            subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'quit'],check=True)
                            milestone_requested=True
                        elif time.time()-last_report>=30:
                            subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'villagers familyprobe status'],check=True)
                            last_report=time.time()
                except (OSError,ValueError):pass
            if time.monotonic()-monotonic>args.ticks/args.tps+240:
                subprocess.run([sys.executable,str(root/'tools/lab-command.py'),'--run-dir',str(run),'quit'],check=True)
                process.wait(timeout=90);timeout=True;break
            time.sleep(1)
        (out/'terminal.json').write_text(json.dumps({'pid':process.pid,'exitCode':process.returncode,'wallTimeGuard':timeout,'milestoneRequested':milestone_requested,'ended':time.time(),'evidence':str(evidence)},indent=2))
    after=audit('bodies-after.json')
    (out/'compiled-classes-after.json').write_text(json.dumps(hashes((pinned_build if args.source_snapshot else run/'.build')/'classes/java/main'),indent=2))
    source_unchanged=hashes(source_root/'src/main')==source_hashes
    if snapshot_manifest:
        source_unchanged=source_unchanged and all(hashlib.sha256((source_root/n).read_bytes()).hexdigest()==h for n,h in snapshot_manifest.items())
    old_unchanged=hashes(old)==old_before
    original={a['id'] for a in before['bodies']};actual={a['id'] for a in after['bodies']}
    managed=[a for a in after['bodies'] if 'villagers:managed' in a['tags']]
    result={'requestedNativeTps':args.tps,'nativeRateVerifiedInObservation':rate_verified,'lastObservedNativeTps':last_observed_tps,'observedNativeRates':sorted(r for r in observed_rates if isinstance(r,(int,float))),'exitCode':process.returncode,'wallTimeGuard':timeout,'oldSaveUnchanged':old_unchanged,'beforeHumanoids':len(original),'afterHumanoids':len(actual),'missingOriginalIds':sorted(original-actual),'newHumanoidIds':sorted(actual-original),'managedVillagers':len(managed),'minManagedHealth':min(a['health'] for a in managed),'adventurer':next((a for a in after['bodies'] if a['id']==nominated[0]['id']),None)}
    result.update(compare_population(before,after))
    result['combatGuard']='stop after first actual combat hit; normal damage retained'
    if recovery_scope:result['combatGuard']='reviewed actor/attacker only may continue above8HP; all other combat stops at first hit; native damage retained'
    if evidence and (evidence/'budget-native.json').exists():
        budget=json.loads((evidence/'budget-native.json').read_text())
        result['combatStop']=budget.get('combatStop',{})
        result['reviewedCombat']=budget.get('reviewedCombat',{})
    result['stonePickaxeMilestoneRequested']=milestone_requested and args.until_stone_pickaxe
    result['sourceUnchangedDuringRun']=source_unchanged
    result['nativeIllagerEnrollmentRequested']=enrollment_requested and not bool(args.care_resume_scope)
    result['frozenExistingWorkerResume']=bool(args.care_resume_scope)
    result['frozenCareStartupFailure']=startup_failure
    result['frozenCareStartupRequested']=bool(startup_scope)
    if startup_scope:
        receipt_path=out/'native-care-startup.json'
        result['frozenCareStartup']=json.loads(receipt_path.read_text()) if receipt_path.exists() else {}
    result['nativeWorkerReference']=str(args.recruit_native_worker) if args.recruit_native_worker else None
    result['lastHealthRecovery']=args.last_health_recovery
    result['requestedSimulationDistance']=args.simulation_distance
    result['observedSimulationDistance']=observed_simulation
    result['ironIngotsMilestoneRequested']=milestone_requested and args.until_iron_ingots
    result['ironPickaxeMilestoneRequested']=milestone_requested and args.until_iron_pickaxe
    hero=result['adventurer'] or {}
    owned=list(hero.get('inventory',[]))+list(hero.get('equipment',{}).values())
    result['stoneTierPickaxeOwned']=any(item.get('id') in ('minecraft:stone_pickaxe','minecraft:iron_pickaxe','minecraft:diamond_pickaxe','minecraft:netherite_pickaxe') and item.get('count',0)>0 for item in owned)
    result['ironIngotsOwned']=sum(item.get('count',0) for item in owned if item.get('id')=='minecraft:iron_ingot')
    result['ironTierPickaxeOwned']=any(item.get('id') in ('minecraft:iron_pickaxe','minecraft:diamond_pickaxe','minecraft:netherite_pickaxe') and item.get('count',0)>0 for item in owned)
    (out/'result.json').write_text(json.dumps(result,indent=2))
    if evidence:
        for name in ['budget-native.json','budget-end.json','commands.jsonl','observations.jsonl']:
            p=evidence/name
            if p.exists():shutil.copy2(p,out/name)
    print(json.dumps({k:v for k,v in result.items() if k!='adventurer'}),flush=True)
    if startup_scope and result['frozenCareStartup'].get('stage')!='resumed':raise RuntimeError('Frozen care startup did not resume successfully; actual save retained')
    if not rate_verified:raise RuntimeError('Requested native tick rate was never verified in actual observation; actual save retained')
    if args.simulation_distance is not None and observed_simulation!=args.simulation_distance:raise RuntimeError('Requested simulation distance was not observed on the server; actual save retained')
    if process.returncode or timeout or not old_unchanged or original-actual or result['newlyDeadOrMissingIds'] or not source_unchanged:raise RuntimeError(f'Observation requires review; actual save retained without rollback: {out}')
    if result.get('combatStop'):raise RuntimeError(f'Observation stopped after combat damage; actual injuries and save retained: {out}')
    if recovery_scope:
        recovered=next(a for a in after['bodies'] if a['id']==recovery_scope['actor'])
        if not result.get('reviewedCombat') or recovered['health']<=8:raise RuntimeError(f'Reviewed recovery requires inspection; actual save retained: {out}')
    if result['stonePickaxeMilestoneRequested'] and not result['stoneTierPickaxeOwned']:raise RuntimeError('Observed pickaxe milestone was not retained in native saved inventory; actual save preserved')
    if result['ironIngotsMilestoneRequested'] and result['ironIngotsOwned']<3:raise RuntimeError('Observed iron milestone was not retained in native saved inventory; actual save preserved')
    if result['ironPickaxeMilestoneRequested'] and not result['ironTierPickaxeOwned']:raise RuntimeError('Observed iron pickaxe was not retained in native saved inventory; actual save preserved')


if __name__=='__main__':main()
