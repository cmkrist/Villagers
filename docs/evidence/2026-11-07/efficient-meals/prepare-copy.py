from pathlib import Path
import sys,importlib.util,shutil,json,datetime
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'))
from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
src=ROOT/'runs/play';dst=ROOT/'runs/efficient-meal-probe/play';out=ROOT/'docs/evidence/2026-11-07/efficient-meals';world=src/'saves/VillageTrial'
assert not dst.exists(),'Separate fixture already exists; do not reset it'
with runtime_lock(src),m.stopped_world(world):
 records=m.tree_files(world);digest=m.tree_digest(records)
 dst.mkdir(parents=True)
 for name in ('config','defaultconfigs','mods','resourcepacks'):
  s=src/name
  if s.exists():shutil.copytree(s,dst/name,symlinks=True)
 for name in ('pack-provenance.json','options.txt'):
  if (src/name).exists():shutil.copy2(src/name,dst/name)
 copy=dst/'saves/VillageTrial';shutil.copytree(world,copy,ignore=shutil.ignore_patterns('session.lock'))
 assert m.tree_files(copy)==records,'Copy is not an exact baseline'
 changes=[]
 for actor,food,forceForage in [('5ad3aad5-3e4b-4eb6-8158-87731868dc03',15,False),('e358e923-f9d2-4d5b-a04b-b2531c8f02d9',19,True)]:
  rel=f'villagers/{actor}/mind.json';p=copy/rel
  before=json.loads(p.read_text());after=json.loads(p.read_text())
  after['food']={'foodLevel':food,'saturation':0.0,'exhaustion':0.0,'timer':0};after['hunger']=(20-food)/20
  if forceForage:
   for row in after['values']:row[0]=1.0
  p.write_text(json.dumps(after,indent=2)+'\n')
  changes.append({'file':rel,'beforeMind':before,'fixtureMind':after,'forcedForagePreference':forceForage})
 changed=m.tree_files(copy);diff=[name for name in records if records[name]!=changed.get(name)]
 assert set(diff)=={c['file'] for c in changes} and set(changed)==set(records),diff
 manifest=json.loads((src/'experiment.json').read_text());manifest.update({'prepared_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'runtime':str(dst),'source_snapshot':str(world),'source_world_sha256':digest,'source_world_files':records,'mutable_data_shared':False})
 manifest['notes']=['LABELLED efficient-meal copy. Fern food15; Nell food19 plus learned FORAGE value1 in every context; saturation/exhaustion/timer0 and redundant hunger updated. No health/inventory/position/terrain setter. Original world unchanged. Do not merge fixture history.']
 (dst/'experiment.json').write_text(json.dumps(manifest,indent=2)+'\n')
 assert m.tree_files(world)==records,'Source changed during copying'
 result={'createdUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'kind':'CONTROLLED COPY; not ordinary survival evidence','originalRuntime':str(src),'fixtureRuntime':str(dst),'originalWorldSHA256':digest,'originalWorldFiles':records,'changedWorldFiles':diff,'actorChanges':changes,'inventoriesHealthTerrainUntouched':True,'noAdditionalResources':True,'requestedObservationTicks':400,'originalWorldUnchangedAfterPreparation':True}
 (out/'setup.json').write_text(json.dumps(result,indent=2)+'\n')
 print('Prepared labelled copy;',len(diff),'of',len(records),'world files changed. Original world unchanged.')
