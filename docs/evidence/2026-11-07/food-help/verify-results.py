"""Recorded native food request/meal, persistence and hungry forestry suspension; no game invocation."""
from pathlib import Path
import datetime,hashlib,json,sys,zipfile
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
checks=[]
def check(ok,note):
 if not ok:raise AssertionError(note)
 checks.append(note)
def read(name):return json.loads((OUT/name).read_text())
def pick(j,prefix):return next(a for a in j['actors'] if a['uuid'].startswith(prefix))
def count(a,item):return sum(int(s.split()[0]) for s in a['inventory'] if s.endswith(' '+item))
def notes(a,text):return sum(text in m.get('note','') for m in a['mind']['memories'])
base=read('results.json');early=read('early-observation.json');reopen=read('persistence/results.json');guard=read('forestry-guard/results.json')
before=pick(early,'231e');after=pick(base,'231e');donorBefore=pick(early,'c7c3');donorAfter=pick(base,'c7c3')
check(before['food']==6 and before['foodHelp']['received']==before['foodHelp']['requests']==0,'Native pre-transfer hungry body, zero authored requests/receipts')
check(count(before,'minecraft:bread')==0 and count(donorBefore,'minecraft:bread')==3,'Native pre-transfer receiver0/donor3bread')
check(after['foodHelp']['requests']==1 and after['foodHelp']['received']==1 and donorAfter['foodHelp']['given']==1,'One authored request and matching actual transfer endpoints')
check(count(donorAfter,'minecraft:bread')==2 and count(after,'minecraft:bread')==0,'Exactly one donor bread spent; receiver ate the ration')
check(after['food']==10 and after['health']==20,'Native meal raised food6→10 after metabolism, health20')
check(all(a['breadCraftedThisLoad']==0 for a in base['actors']),'No bread crafting credited for transferred meal')
check(sum(a['foodHelp']['given'] for a in base['actors'])==sum(a['foodHelp']['received'] for a in base['actors'])==1,'One actual transfer in the recorded loaded society')
check(notes(after,'Received one actual minecraft:bread from c7c3')==1 and notes(after,'Asked visible neighbour c7c3')==1 and notes(after,'Ate owned minecraft:bread')==1,'Receipt/request/meal memories are grounded in real actions')
check(notes(donorAfter,'Gave one actual minecraft:bread to 231e')==1,'Donor remembers actual payment')
r=pick(reopen,'231e');d=pick(reopen,'c7c3')
check(r['food']==10 and d['food']==19 and count(d,'minecraft:bread')==2,'Reopen retains paid nutrition and donor stock')
check(sum(a['foodHelp']['given']+a['foodHelp']['received'] for a in reopen['actors'])==0,'No further handoff in400tick reopen; counters are session-local')
check(notes(r,'Received one actual minecraft:bread from c7c3')==1 and notes(d,'Gave one actual minecraft:bread to 231e')==1,'Transfer history survives actual restart once')
a=pick(guard,'231e');check(a['food']==3 and a['health']==20,'Hungry guard fixture remained alive without body setters')
check(a['forestryStatus']=='waiting for food before wood work' and a['forestryRoute']['batches']==0,'Urgent hunger prevents forestry planning')
check(a['logsGatheredThisLoad']==a['propagulesPlantedThisLoad']==a['leavesClearedThisLoad']==0,'No resource work/material cost while hungry')
check(count(a,'minecraft:mangrove_propagule')==1,'Actual planting reserve retained')
check((OUT/'forestry-guard/sites-before.json').read_bytes()==(OUT/'forestry-guard/sites-after.json').read_bytes(),'Exact finite forestry authority survives hungry suspension')
f=read('forestry-guard/sites-after.json')['forestry'][0]
check(f['id']=='891a2b06-ccd6-4889-9b38-5d85398d87e2' and f['seedTaken'] and len(f['remaining'])==2 and not f['uncleared'],'Same paid seed, completed clearance and quota2')
for name,r in [('first',base),('reopen',reopen),('guard',guard)]:
 check(r['exitCode']==0 and r['originalWorldUnchanged'] and r['sourceUnchangedDuringRun'],name+': terminal0, unchanged source/source-world guards')
 check(r['javaTests']=={'tests':275,'failures':0,'errors':0,'skipped':0} and r['pythonTests']==10,name+': recorded275Java+10Python pass')
 check(r['population']=={'original_cohort_loaded_alive':4,'original_cohort_known_dead_or_converted':7,'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':5},name+': same five living villagers, no restored deaths')
 check(all(not a['sameIdentityAlive'] for a in r['ownedTerminal']),name+': exact launch chain terminal')
for phase,archive in [('.', 'source-local-only.zip'),('persistence','source-main.zip'),('forestry-guard','source-main.zip')]:
 files=read(Path(phase)/'source-before.json')['files']
 with zipfile.ZipFile(OUT/archive) as z:check(all(hashlib.sha256(z.read(n)).hexdigest()==v for n,v in files.items()),phase+': exact archived source cohort')
files=read('persistence/source-before.json')['files'];check(all(hashlib.sha256((ROOT/n).read_bytes()).hexdigest()==v for n,v in files.items()),'Current source matches final native cohort')
# Food changes must not alter metabolism, meal sizing or reproductive rules.
with zipfile.ZipFile(ROOT/'docs/evidence/2026-11-07/forestry/source-main.zip') as old:
 for n in ['src/main/java/local/villagers/mind/Metabolism.java','src/main/java/local/villagers/mind/MealPolicy.java','src/main/java/local/villagers/family/BreedingPolicy.java']:
  check((ROOT/n).read_bytes()==old.read(n),'Preserved baseline rule: '+n)
for run in ['runs/food-help/play','runs/food-help-forestry-guard/play']:
 with runtime_lock(ROOT/run):check(not any((ROOT/run/n).exists() for n in ['lab-command.txt','lab-command.processing.txt','saves/VillageTrial/villagers/work-sites.json.active']),run+': clean shutdown, no pending command')
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:check(hashlib.sha256(p.read_bytes()).hexdigest()==expected,'Preserved deployed0.4.0: '+str(p))
summary={'verifiedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checksPassed':len(checks),'javaTests':275,'pythonTests':10,'nativeVerified':['authored understood food request','one actual bread transferred and eaten','food6→10 after metabolism','nutrition/history/stock persist across restart','hungry forestry suspended without losing seed or authority'],'notYetVerified':['positive planned food-help approach before contact','refused/foreign-language native requests','sustainable replenishment of donated bread','safe native log access/harvest/planting/regrowth','population capacity and tail latency'],'sourceArchiveSHA256':hashlib.sha256((OUT/'source-main.zip').read_bytes()).hexdigest(),'devJarSHA256':hashlib.sha256((ROOT/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),'noDeployment':True}
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
