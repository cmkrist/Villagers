"""Verify rainy-day wake, actual harvest/recipe/meal conservation and retained deaths."""
from pathlib import Path
import datetime,hashlib,importlib.util,json,sys,zipfile
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'tools'))
from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');parallel=importlib.util.module_from_spec(spec);spec.loader.exec_module(parallel)
checks=[]
FERN='5ad3aad5-3e4b-4eb6-8158-87731868dc03';CHILD='589b708d-b768-4a61-a59e-910db084b079';JUNIPER='231e4c00-19df-4be5-a5ba-5ebc6d29e27f'
def read(n):return json.loads((OUT/n).read_text())
def actor(d,id=FERN):return next(a for a in d['actors'] if a['uuid']==id)
def count(a,item):return sum(int(s.split()[0]) for s in a['inventory'] if s.endswith(' minecraft:'+item))
def notes(a):return [m.get('note','') for m in a['mind']['memories']]
def check(ok,note):
    if not ok:raise AssertionError(note)
    checks.append(note)
baseline,early,day,fixed,reopen=[read(n) for n in ['results.json','wake/first-observation.json','wake/day-observation.json','wake/results.json','persistence/results.json']]
bc=read('census.json');a,b,c=actor(baseline),actor(fixed),actor(reopen)
check(bc['is_raining'] and not bc['night'] and a['sleeping'] and a['survival']['lastRefugePlan']['dusk'],
      'Actual baseline: rainy daylight misclassified as dusk keeps Fern asleep')
check(a['adventureActivity']['cropsHarvested']==a['breadCraftedThisLoad']==0,'No baseline food production during 12000-tick observation')
check(early['raining'] and not early['night'] and early['skyDarken']==3 and not actor(early)['sleeping'] and actor(early)['nativeActivity']=='work',
      'Same-world daytime reload wakes Fern in actual rain/skyDarken3 under native WORK schedule')
check(b['adventureActivity']['cropsHarvested']==b['adventureActivity']['replantsFromStock']==4 and b['breadCraftedThisLoad']==1,
      'Actual four harvests/replants and one native bread recipe')
check(count(a,'wheat')==count(b,'wheat')==1 and count(a,'wheat_seeds')==12 and count(b,'wheat_seeds')==19,
      'Starting grain buffer restored: one plus three harvested minus three crafted equals one; real seed income retained')
check(count(a,'beetroot_seeds')==4 and count(b,'beetroot_seeds')==6 and count(b,'beetroot')==count(b,'bread')==0,
      'Paid beetroot replant increases real seed bank; no invented bread or beetroot stock')
check(b['food']==15 and a['food']==9,'Native food rises after actual meals without a nutrition setter')
d=actor(day)
check(any('Crafted 1 bread from 3 wheat at -233, 72, 746' in n for n in notes(d)) and any('Ate owned minecraft:bread' in n for n in notes(d)),
      'Native recipe/table and bread-eating notes captured before short-memory eviction')
check(sum('Harvested real WHEAT' in n for n in notes(d))>=2 and any('Harvested real BEETROOT at -230, 72, 752' in n for n in notes(d)),
      'Two new grains and one naturally regrown beet precede the captured bread meal')
r=d['lastHarvest']
check(r['gameTime']==81937 and r['crop']=='WHEAT' and r['nativeDrops']==['1 minecraft:wheat','3 minecraft:wheat_seeds'] and
      count({'inventory':r['inventoryBefore']},'wheat')==2 and count({'inventory':r['inventoryAfter']},'wheat')==3,
      'Direct third-grain receipt precedes recipe: two new grains plus one held grain, not three newly harvested inputs yet')
check(b['lastHarvest']['crop']=='WHEAT' and b['lastHarvest']['position']=='-226, 72, 753' and
      count({'inventory':b['lastHarvest']['inventoryBefore']},'wheat')==0 and count({'inventory':b['lastHarvest']['inventoryAfter']},'wheat')==1,
      'Later third newly harvested grain replenishes buffer after the actual recipe')
before={tuple(c['position']):c for c in read('after-growth/report.json')['crops']}
after={tuple(c['position']):c for c in read('wake/after-growth/report.json')['crops']}
prior=json.loads((OUT.parent/'farm-regrowth/persistence/after-growth/report.json').read_text())
old={tuple(c['position']):c for c in prior['crops']};pos=(-230,72,752)
check(old[pos]['crop']=={'Name':'minecraft:beetroots'} and before[pos]['crop']=={'Name':'minecraft:beetroots','Properties':{'age':'3'}} and
      after[pos]['crop']['Name']=='minecraft:beetroots' and int(after[pos]['crop'].get('Properties',{}).get('age','0'))<3,
      'Earlier paid beetroot replant naturally matured, was harvested again, and remains replanted')
for p in [(-230,72,751),(-228,72,752),(-226,72,753)]:
    check(after[p]['crop']=={'Name':'minecraft:wheat'},'Native harvested wheat is saved at default age zero: '+str(p))
sunset=read('wake/budget-native.json')
check(not sunset['night'] and actor(sunset)['nativeActivity']=='rest' and b['sleeping'] and b['survival']['intent']=='SHELTER',
      'Native REST schedule still returns to shelter before full darkness')
check(c['sleeping'] and c['nativeActivity']=='rest' and c['inventory']==b['inventory'] and c['breadCraftedThisLoad']==c['adventureActivity']['cropsHarvested']==0,
      'Sunset reopen preserves actual stock/sleep without recipe or harvest replay')
for phase,result,archive,damage in [(Path('.'),baseline,OUT.parent/'refuge-floor/source-backoff.zip',10),
                                   (Path('wake'),fixed,OUT/'source-wake.zip',14),
                                   (Path('persistence'),reopen,OUT/'source-wake.zip',14)]:
    check(read(phase/'terminal.json')['exitCode']==result['exitCode']==0,str(phase)+': original handle directly exited zero')
    check(result['population']=={'original_cohort_loaded_alive':3,'original_cohort_known_dead_or_converted':8,
          'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':3},str(phase)+': three adults remain; no restored offspring')
    census=read(phase/'census.json');life=census['known_actor_lifecycle']
    check(life[JUNIPER]['terminal_game_time']==66151 and life[JUNIPER]['terminal_event']=='death' and
          life[CHILD]['terminal_game_time']==81487 and life[CHILD]['terminal_detail']=='starve',
          str(phase)+': zombie death and later offspring starvation remain authoritative')
    check(CHILD not in {a['uuid'] for a in census['local_loaded_villagers']} and len(result['selectedParentChildren'])==1,
          str(phase)+': legacy child-mind count is a deceased descendant, not a living actor or new birth')
    equipment=next(x['equipment_status'] for x in census['local_loaded_villagers'] if x['uuid']==FERN)
    check(f'iron_hoe x1 damage={damage}/250' in equipment,str(phase)+': actual conserved hoe wear')
    check(len(result['ownedTerminal'])==3 and all(not x['sameIdentityAlive'] for x in result['ownedTerminal']),str(phase)+': exact three-job chain terminal')
    check(result['sourceUnchangedDuringRun'] and result['originalWorldUnchanged'],str(phase)+': pinned source/parent unchanged')
    check(result['javaTests']=={'tests':286,'failures':0,'errors':0,'skipped':0} and result['pythonTests']==10,str(phase)+': recorded full286Java+10Python pass')
    with zipfile.ZipFile(archive) as z:
        check(all(hashlib.sha256(z.read(n)).hexdigest()==h for n,h in read(phase/'source-before.json')['files'].items()),str(phase)+': exact source cohort archived')
check('starved to death' in (OUT/'launch.log').read_text(),'Native server log independently identifies offspring starvation')
nell=read('nell-inspection.json')
check(nell['onGround'] and nell['collisionFree'] and nell['pose']=='STANDING' and not nell['homePath']['canReach'] and nell['homePath']['nodes']==1,
      'Nell has a grounded clear body, not the earlier clipped-start problem')
check(all(not c['intersectsBody'] for c in nell['nearby']) and all(not c['path']['canReach'] for c in nell['localCandidates']),
      'Native local cardinal probes are unreachable without changing the resident or terrain')
terrain=read('terrain-before/report.json')
check(len(terrain['farmDirtOrGrass'])==1 and not any(c['above']['Name']==c['head']['Name']=='minecraft:air' for c in terrain['farmDirtOrGrass']),
      'Bounded farm survey finds no exposed dirt/grass candidate; no ownership or whole-village conclusion inferred')
schedule=read('native-clock-data/villager_schedule.json')
check(any(f['ticks']==12000 and f['value']=='minecraft:rest' for f in schedule['tracks']['minecraft:gameplay/villager_activity']['keyframes']),
      'Loaded native26.3timeline provides sunset rest independently of weather')
with zipfile.ZipFile(OUT/'source-wake.zip') as new,zipfile.ZipFile(OUT.parent/'refuge-floor/source-backoff.zip') as old:
    changed={n for n in read('wake/source-before.json')['files'] if new.read(n)!=old.read(n)}
    check(changed=={'src/main/java/local/villagers/VillageSurvival.java','src/main/java/local/villagers/VillageFamilyProbe.java'},
          'Only refuge timing/daytime wake and diagnostic fields changed; authority, metabolism and recipes unchanged')
check(all(hashlib.sha256((ROOT/n).read_bytes()).hexdigest()==h for n,h in read('wake/source-before.json')['files'].items()),'Current source matches successful wake/food cohort')
setup=read('setup.json')
with runtime_lock(Path(setup['originalRuntime'])),runtime_lock(ROOT/'runs/dawn-food/play'):
    check(parallel.tree_files(Path(setup['originalRuntime'])/'saves/VillageTrial')==setup['originalWorldFiles'],'All286source world files remain unchanged; no history merge')
    check(not any((ROOT/'runs/dawn-food/play'/n).exists() for n in ['lab-command.txt','lab-command.processing.txt','saves/VillageTrial/villagers/work-sites.json.active']),
          'Current continued world stopped with no pending commands or dirty edit marker')
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:
    check(hashlib.sha256(p.read_bytes()).hexdigest()==expected,'Preserved0.4.0: '+str(p))
summary=dict(verifiedUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),checksPassed=len(checks),checks=checks,
             rainyDayWakeVerified=True,nativeSunsetRestVerified=True,paidHarvests=4,newNativeBreadRecipes=1,
             heldGrainBufferRestored=True,sustainableFoodOrOffspringSurvivalProven=False,bothDeathsRetained=True,
             nativeMeanTickMillis=[r['meanTickMillis'] for r in [baseline,fixed,reopen]],
             sourceArchiveSHA256=hashlib.sha256((OUT/'source-wake.zip').read_bytes()).hexdigest(),
             devJarSHA256=hashlib.sha256((ROOT/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),noDeployment=True)
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n');print({k:v for k,v in summary.items() if k!='checks'})
