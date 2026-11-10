"""Verify recorded earned-log→planks→table item→placed table, protection and clean restart."""
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
def plot(name):return {tuple(x['position']):x['state'] for x in read(name)['blocks']}
protected=read('results.json');early=read('approved/early-observation.json');built=read('approved/results.json');reopen=read('persistence/results.json')
a=actor(protected);b=actor(built);c=actor(reopen);e=actor(early)
check(a['stationRecipesThisLoad']==a['tablesBuiltThisLoad']==0,'Protection blocked all table crafting/placement')
check(count(a,'minecraft:mangrove_log')==2 and count(a,'minecraft:mangrove_roots')==1,'Protected earned materials untouched')
sites=read('sites-after.json');check(len(sites['stations'])==len(sites['protections'])==1,'One finite table grant and overriding protection retained')
check(sites['stations'][0]['target']=={'dimension':'minecraft:overworld','x':-281,'y':75,'z':745},'Exact native table site')
check(e['stationRecipesThisLoad']==1 and e['tablesBuiltThisLoad']==0 and count(e,'minecraft:mangrove_planks')==4 and count(e,'minecraft:mangrove_log')==1,'Captured actual intermediate four planks from one earned log')
check(b['stationRecipesThisLoad']==2 and b['tablesBuiltThisLoad']==1,'Native two recipe steps then one paid placement')
for k in ['minecraft:mangrove_planks','minecraft:crafting_table','minecraft:wheat','minecraft:wheat_seeds']:
 check(count(b,k)==0,'No leftover or supplied builder input/output: '+k)
check(count(b,'minecraft:mangrove_log')==1 and count(b,'minecraft:mangrove_roots')==1,'Exactly one earned log spent; root and other log retained')
check(b['food']==10 and b['health']==20,'Builder survives unchanged nutrition/health')
notes=[m.get('note','') for m in b['mind']['memories']]
earlyNotes=[m.get('note','') for m in e['mind']['memories']]
check(any('Crafted 4 minecraft:mangrove_planks' in n for n in earlyNotes) and any('Crafted 1 minecraft:crafting_table' in n for n in notes),'Native recipe memories captured at their respective times; short ring may evict earlier steps')
pre=plot('plot.json');post=plot('approved/plot.json');after=plot('persistence/plot.json');pos=(-281,75,745)
check(pre[pos]['Name']=='minecraft:air' and post[pos]['Name']=='minecraft:crafting_table','Raw native AIR→CRAFTING_TABLE at authorized target')
check(all(pre[p]==post[p] for p in pre if p!=pos),'Selected support/access/headroom/paid propagule unchanged')
check(post==after,'Placed table and selected native terrain survive restart')
check(c['stationRecipesThisLoad']==c['tablesBuiltThisLoad']==0 and count(c,'minecraft:mangrove_log')==1 and count(c,'minecraft:mangrove_roots')==1,'No recipe or placement replay on restart')
check(not read('approved/sites-after.json')['stations'] and not read('persistence/sites-after.json')['stations'],'Completed table grant retired and stays retired')
check(not read('approved/sites-after.json')['protections'],'Only explicit protection removal activated existing job')
check((OUT/'approved/commands.txt').read_text().splitlines()[0]=='villagers site remove '+sites['protections'][0]['id'],'Correct protection UUID removed, no replacement job/materials')
for phase,r in [('.',protected),('approved',built),('persistence',reopen)]:
 check(r['exitCode']==0 and r['originalWorldUnchanged'] and r['sourceUnchangedDuringRun'],phase+': terminal0 and unchanged parent world/source')
 check(r['javaTests']=={'tests':280,'failures':0,'errors':0,'skipped':0} and r['pythonTests']==10,phase+': recorded280Java+10Python pass')
 check(r['population']=={'original_cohort_loaded_alive':4,'original_cohort_known_dead_or_converted':7,'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':5},phase+': unchanged living society/death history')
 check(all(not p['sameIdentityAlive'] for p in r['ownedTerminal']),phase+': exact captured launch chain terminal')
 archive='source-protected.zip' if phase=='.' else 'source-main.zip'
 with zipfile.ZipFile(OUT/archive) as z:check(all(hashlib.sha256(z.read(n)).hexdigest()==h for n,h in read(Path(phase)/'source-before.json')['files'].items()),phase+': exact archived source cohort')
check(all(hashlib.sha256((ROOT/n).read_bytes()).hexdigest()==h for n,h in read('approved/source-before.json')['files'].items()),'Current source matches final native cohort')
with runtime_lock(ROOT/'runs/earned-station/play'):
 check(not any((ROOT/'runs/earned-station/play'/n).exists() for n in ['lab-command.txt','lab-command.processing.txt','saves/VillageTrial/villagers/work-sites.json.active']),'Clean shutdown/no pending command')
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:check(hashlib.sha256(p.read_bytes()).hexdigest()==expected,'Preserved0.4.0: '+str(p))
food=read('food-gap/report.json');check(len(food['crops'])==8 and food['matureCrops']==2 and food['farmlandCells']==8,'Next-task saved evidence: two mature beetroot crops, six immature wheat')
summary={'verifiedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checksPassed':len(checks),'javaTests':280,'pythonTests':10,'nativeEarnedLogToPaidTableVerified':True,'restartNoReplay':True,'noDeployment':True,'sourceArchiveSHA256':hashlib.sha256((OUT/'source-main.zip').read_bytes()).hexdigest(),'devJarSHA256':hashlib.sha256((ROOT/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),'nextWork':['mature edible-crop fallback when requested wheat is unavailable','native actual food harvest/replant/refill','natural tree regrowth and second income cycle','broader buildings/parent care/defense/population performance']}
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
