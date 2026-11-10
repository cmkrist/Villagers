"""Check native farm-memory outcomes and retained source binding, without world writes."""
from pathlib import Path
import hashlib
import json
import zipfile

OUT=Path(__file__).resolve().parent
FERN='5ad3aad5-3e4b-4eb6-8158-87731868dc03'
NELL='e358e923-f9d2-4d5b-a04b-b2531c8f02d9'
checks=0
def check(value,why):
    global checks
    assert value,why
    checks+=1
def read(case,name): return json.loads((case/name).read_text())
def actor(case,uuid): return next(a for a in read(case,'budget-native.json')['actors'] if a['uuid']==uuid)
def materials(a): return sorted(x for x in a['inventory'] if x!='0 minecraft:air')

for case in [OUT,OUT/'reopen']:
    r=read(case,'results.json')
    check(r['exitCode']==0 and r['originalWorldUnchanged'] and r['sourceUnchangedDuringRun'],'terminal/pins')
    check(r['population']==dict(original_cohort_loaded_alive=2,original_cohort_known_dead_or_converted=9,
                               original_cohort_unloaded_or_missing=0,local_loaded_villager_count=2),'actual population')
    check(r['javaTests']==dict(tests=300,failures=0,errors=0,skipped=0) and r['pythonTests']==10,'passing checks')
    check(len(r['ownedTerminal'])==3 and not any(p['sameIdentityAlive'] for p in r['ownedTerminal']),'exact terminal identities')
    check(0<r['meanTickMillis']<50,'small-cohort tick sample')
    with zipfile.ZipFile(OUT/'source-main.zip') as z:
        for name,sha in read(case,'source-before.json')['files'].items():
            check(hashlib.sha256(z.read(name)).hexdigest()==sha,'source binding '+name)
    lifecycle=read(case,'census.json')['known_actor_lifecycle']
    for uuid,tick in [('231e4c00-19df-4be5-a5ba-5ebc6d29e27f',66151),
                      ('589b708d-b768-4a61-a59e-910db084b079',81487),
                      ('c7c31152-34a1-4edd-bd84-e48f50f86088',98739)]:
        check(lifecycle[uuid]['terminal_event']=='death' and lifecycle[uuid]['terminal_game_time']==tick,'history preserved')
before=read(OUT,FERN+'-mind-before.json')
check(not any(e['kind']=='OBSERVED_FARM' for e in before['episodes']),'unearned initial knowledge')
a=actor(OUT,FERN);recall=a['farmRecall'];path=recall['lastPath']
check(recall['trips']==recall['arrivals']==2 and recall['rejected']==0,'actual return trips')
check(path['fromObservation'] and path['canReach'] and path['permitted'] and path['nodes']==9,'complete remembered route')
check(path['target']=='-226, 72, 751','observed patch rather than composter')
check(len(recall['knownFarms'])==2 and not actor(OUT,NELL)['farmRecall']['knownFarms'],'knowledge is personal')
h=a['lastHarvest']
check(h['nativeDrops']==['1 minecraft:wheat','2 minecraft:wheat_seeds'],'actual crop income')
check('21 minecraft:wheat_seeds' in h['inventoryBefore'] and '22 minecraft:wheat_seeds' in h['inventoryAfter'],'real seed payment')
check(h['hoeDamageBefore']==18 and h['hoeDamageAfter']==19 and h['cropAfter'].endswith('[age=0]'),'hoe wear/replant')
b=actor(OUT/'reopen',FERN)
check(b['farmRecall']['knownFarms']==recall['knownFarms'],'persistent actual observations')
check(materials(b)==materials(a),'no replayed material income')
check(b['adventureActivity']['cropsHarvested']==0,'no harvest replay on reopening')
print(f'{checks} recorded outcome/source-binding checks passed; full goal remains active.')
