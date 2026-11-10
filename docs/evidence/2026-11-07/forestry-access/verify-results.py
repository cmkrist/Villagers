"""Native standing diagnosis, real root/log income, paid planting and exact outside-write revocation."""
from pathlib import Path
import datetime,hashlib,json,sys,zipfile
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
checks=[]
def read(name):return json.loads((OUT/name).read_text())
def check(ok,note):
 if not ok:raise AssertionError(note)
 checks.append(note)
def actor(j):return next(a for a in j['actors'] if a['uuid']=='231e4c00-19df-4be5-a5ba-5ebc6d29e27f')
def count(a,item):return sum(int(s.split()[0]) for s in a['inventory'] if s.endswith(' '+item))
def blocks(name):return {tuple(x['position']):x['state'] for x in read(name)['blocks']}
probe=read('inspection.json');base=read('root-trial/results.json');reopen=read('root-trial/persistence/results.json');revoked=read('root-revocation/results.json')
check(probe['pose']=='STANDING' and abs(probe['eyeOffset']-1.62)<.001,'Native standing eye height is normal')
for t in probe['targets'][:2]:
 check(sum(t['counts'].values())==100 and not t['accepted'],'Exact100 production candidates rejected for original log '+t['target'])
 reference=t['referencePlantStand'];check(reference['failure']=='sight blocked' and reference['hitBlock']=='-270, 74, 722' and 'mangrove_roots' in reference['hitState'],'Native dry root occludes log from valid mud stand')
check(len(probe['targets'][2]['accepted'])==10,'Native plant has admitted stands before root clearing')
a=actor(base)
for key,value in [('propagulesGatheredThisLoad',1),('leavesClearedThisLoad',3),('rootsClearedThisLoad',1),('logsGatheredThisLoad',2),('propagulesPlantedThisLoad',1)]:check(a[key]==value,'Native completed stage: '+key)
check(count(a,'minecraft:mangrove_log')==2 and count(a,'minecraft:mangrove_roots')==1 and count(a,'minecraft:mangrove_propagule')==0,'Actual root/log inventory and one paid seed')
check(a['health']==20 and a['food']==10 and a['foodHelp']['received']==0,'Previously earned meal powered work; no further gift in forestry trial')
check(not read('root-trial/sites-after.json')['forestry'],'Completed finite job retired')
raw=blocks('root-trial/plot.json');plant=raw[(-269,72,723)]
check(plant['Name']=='minecraft:mangrove_propagule' and plant['Properties']['hanging']=='false' and plant['Properties']['waterlogged']=='false','Native planted propagule, not hanging seed or invented item')
check(raw[(-269,71,723)]['Name']=='minecraft:mud','Captured native soil retained')
removed=[(-269,74,721),(-270,73,723),(-269,74,723),(-270,74,723),(-270,74,722),(-271,74,722),(-271,75,722)]
check(all(raw[p]['Name']=='minecraft:air' for p in removed),'Raw chunks match exact seed/3leaf/root/2log removals')
check(all(raw[p]['Name']=='minecraft:mangrove_log' for p in [(-270,76,722),(-270,77,722)]),'Former upper logs untouched')
b=actor(reopen)
check(count(b,'minecraft:mangrove_log')==2 and count(b,'minecraft:mangrove_roots')==1 and count(b,'minecraft:mangrove_propagule')==0,'Reopen preserves earned inventory despite native slot compaction')
check(all(b[k]==0 for k in ['propagulesGatheredThisLoad','leavesClearedThisLoad','rootsClearedThisLoad','logsGatheredThisLoad','propagulesPlantedThisLoad']),'No resource/plant replay across restart')
check(not read('root-trial/persistence/sites-after.json')['forestry'],'Reopen retains completed authority retirement')
check(blocks('root-trial/persistence/plot.json')==raw,'Selected native terrain and paid plant persist unchanged')
hooks=[json.loads(p.read_text()) for p in (OUT/'root-revocation/native-hook').glob('*.json')]
check(len(hooks)==1 and hooks[0]['authorizedBefore'] and not hooks[0]['authorizedAfter'] and hooks[0]['blockUnchanged'] and hooks[0]['soil']=='-270, 74, 722','Direct same-state native root write revokes fresh root authority')
check(not read('root-revocation/sites-after.json')['forestry'],'Outside root write retires whole separate job')
check(blocks('root-revocation/plot.json')[(-270,74,722)]['Name']=='minecraft:mangrove_roots','No physical root cut by the revocation probe')
for phase,r,tests in [('.',read('results.json'),275),('root-trial',base,278),('root-trial/persistence',reopen,278),('root-revocation',revoked,278)]:
 check(r['exitCode']==0 and r['originalWorldUnchanged'] and r['sourceUnchangedDuringRun'],phase+': terminal0, original source-world/source pins unchanged')
 check(r['javaTests']=={'tests':tests,'failures':0,'errors':0,'skipped':0} and r['pythonTests']==10,phase+': recorded check suite passes')
 check(r['population']=={'original_cohort_loaded_alive':4,'original_cohort_known_dead_or_converted':7,'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':5},phase+': same living society/death history')
 check(all(not x['sameIdentityAlive'] for x in r['ownedTerminal']),phase+': exact captured launch chain terminal')
 archive='source-probe.zip' if phase=='.' else 'source-main.zip'
 with zipfile.ZipFile(OUT/archive) as z:check(all(hashlib.sha256(z.read(n)).hexdigest()==v for n,v in read(Path(phase)/'source-before.json')['files'].items()),phase+': archived exact source cohort')
files=read('root-trial/source-before.json')['files'];check(all(hashlib.sha256((ROOT/n).read_bytes()).hexdigest()==v for n,v in files.items()),'Current source matches accepted root-cycle cohort')
for run in ['runs/forestry-access/play','runs/forestry-root/play']:
 with runtime_lock(ROOT/run):check(not any((ROOT/run/n).exists() for n in ['lab-command.txt','lab-command.processing.txt','saves/VillageTrial/villagers/work-sites.json.active']),run+': clean shutdown/no pending command')
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:check(hashlib.sha256(p.read_bytes()).hexdigest()==expected,'Preserved0.4.0: '+str(p))
summary={'verifiedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checksPassed':len(checks),'javaTests':278,'pythonTests':10,'nativeHarvestAndPaidPlantVerified':True,'nativeNaturalRegrowthVerified':False,'noDeployment':True,'sourceArchiveSHA256':hashlib.sha256((OUT/'source-main.zip').read_bytes()).hexdigest(),'devJarSHA256':hashlib.sha256((ROOT/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),'remainingWork':['natural tree growth and next harvest cycle','earned-wood integration into crafting/building','adequate renewable food output','native broad protection/physics/growth cases','population capacity and tail latency']}
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
