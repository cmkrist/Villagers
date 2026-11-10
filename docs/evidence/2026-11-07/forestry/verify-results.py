"""Verify recorded native outcomes and current source/artifact custody, without running Minecraft."""
from pathlib import Path
import datetime,hashlib,json,zipfile,sys
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
checks=[]
def check(ok,note):
    if not ok:raise AssertionError(note)
    checks.append(note)
def read(p):return json.loads((OUT/p).read_text())
def actor(r):return next(a for a in r['actors'] if a['uuid']=='231e4c00-19df-4be5-a5ba-5ebc6d29e27f')
def count(a,item):return sum(int(s.split()[0]) for s in a['inventory'] if s.endswith(' '+item))
phases=['.','approved','lower-logs','clearance','leaf-refresh','leaf-refresh/approved','leaf-refresh/route-candidates','leaf-refresh/revocation']
results={p:read(Path(p)/'results.json') for p in phases}
for p,r in results.items():
    check(r['originalWorldUnchanged'] and r['sourceUnchangedDuringRun'],f'{p}: source/copy guard recorded')
    check(r['population']=={'original_cohort_loaded_alive':4,'original_cohort_known_dead_or_converted':7,'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':5},f'{p}: five loaded residents, no restored deaths')
    check(all(not j['sameIdentityAlive'] for j in r['ownedTerminal']),f'{p}: exact launch chain terminal')
    check(r['exitCode']==(None if p=='clearance' else 0),f'{p}: honest exit provenance')
    a=actor(r);check(a['logsGatheredThisLoad']==0 and a['propagulesPlantedThisLoad']==0,f'{p}: no native log/plant success claim')
    check(count(a,'minecraft:mangrove_log')==0,f'{p}: no invented log income')
check(actor(results['.'])['propagulesGatheredThisLoad']==0,'Protection blocked initial seed harvest')
check(actor(results['approved'])['propagulesGatheredThisLoad']==1,'First continued fixture gathered one real seed')
check(actor(results['leaf-refresh'])['propagulesGatheredThisLoad']==1,'Independent copied fixture gathered one real seed')
check(actor(results['clearance'])['leavesClearedThisLoad']==1,'Pre-fix clearance stopped after one leaf')
check(actor(results['clearance'])['food']==0 and actor(results['clearance'])['health']==7,'Starvation observation retained, not hidden or repaired')
a=actor(results['leaf-refresh/approved']);check(a['leavesClearedThisLoad']==3 and count(a,'minecraft:mangrove_propagule')==1,'Native three-leaf clearance retained actual propagule')
a=actor(results['leaf-refresh/route-candidates']);check(a['forestryRoute']['standCandidates']==0 and a['forestryRoute']['batches']==3,'Current native blocker is zero admitted stands')
check(count(a,'minecraft:mangrove_propagule')==1 and a['health']==20,'Clean restart retains seed and living actor')
sites=read('leaf-refresh/route-candidates/sites-after.json');f=sites['forestry'][0]
check(sites['schemaVersion']==4 and f['seedTaken'] and len(f['remaining'])==2 and not f['uncleared'],'Paid seed/three cleared leaves persist; log quota remains2')
blocks={tuple(b['position']):b['state'] for b in read('leaf-refresh/route-candidates/plot.json')['blocks']}
check(all(blocks[p]['Name']=='minecraft:air' for p in [(-269,74,721),(-270,73,723),(-269,74,723),(-270,74,723),(-269,72,723)]),'Raw native chunks: seed/three leaves absent, plant cell still AIR')
check(all(blocks[p]['Name']=='minecraft:mangrove_log' for p in [(-271,74,722),(-271,75,722),(-270,76,722),(-270,77,722)]),'Raw native chunks: assigned and former logs untouched')
hooks=[json.loads(p.read_text()) for p in (OUT/'leaf-refresh/revocation/native-hook').glob('*.json')]
check(len(hooks)==1 and hooks[0]['authorizedBefore'] and not hooks[0]['authorizedAfter'] and hooks[0]['blockUnchanged'] and hooks[0]['soil']=='-269, 75, 721','Direct native same-state leaf write still revokes')
check(not read('leaf-refresh/revocation/sites-after.json')['forestry'],'Outside leaf write retires whole copied job')
for phase,archive in [('clearance','clearance/source-main.zip'),('leaf-refresh/approved','leaf-refresh/approved/source-main.zip'),('leaf-refresh/route-candidates','source-main.zip')]:
    with zipfile.ZipFile(OUT/archive) as z:
        for name,digest in read(Path(phase)/'source-before.json')['files'].items():check(hashlib.sha256(z.read(name)).hexdigest()==digest,f'{phase}: archived {name}')
for name,digest in read('leaf-refresh/route-candidates/source-before.json')['files'].items():check(hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest,'Current source matches accepted route-candidate cohort: '+name)
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:check(hashlib.sha256(p.read_bytes()).hexdigest()==expected,'Preserved0.4.0: '+str(p))
for run in ['runs/forestry/play','runs/forestry-refresh/play','runs/forestry-leaf-revocation/play']:
    with runtime_lock(ROOT/run):
        check(not (ROOT/run/'saves/VillageTrial/villagers/work-sites.json.active').exists(),run+': clean control metadata shutdown')
        check(not any((ROOT/run/name).exists() for name in ['lab-command.txt','lab-command.processing.txt']),run+': no pending command')
summary={'verifiedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checksPassed':len(checks),'javaTests':results['leaf-refresh/route-candidates']['javaTests'],'pythonTests':10,'nativeEndToEndLogPlantCycleVerified':False,'nativeVerified':['real wild propagule acquisition','three explicitly approved leaf cuts','retained seed and partial authority across restart','direct same-state leaf write revokes'],'remainingBlockers':['zero admitted standing positions for selected logs','food resilience/optional work priority','native log harvest and paid planting','natural regrowth and subsequent cycle'],'sourceArchiveSHA256':hashlib.sha256((OUT/'source-main.zip').read_bytes()).hexdigest(),'devJarSHA256':hashlib.sha256((ROOT/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),'noDeployment':True}
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
