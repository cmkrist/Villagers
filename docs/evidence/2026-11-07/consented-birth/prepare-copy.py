from pathlib import Path
import sys,importlib.util,shutil,json,datetime
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'))
from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
src=ROOT/'runs/play';dst=ROOT/'runs/consented-birth/play';out=ROOT/'docs/evidence/2026-11-07/consented-birth';world=src/'saves/VillageTrial'
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
 changed=m.tree_files(copy);assert changed==records
 manifest=json.loads((src/'experiment.json').read_text());manifest.update({'prepared_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'runtime':str(dst),'source_snapshot':str(world),'source_world_sha256':digest,'source_world_files':records,'mutable_data_shared':False})
 manifest['notes']=['LABELLED courtship fixture copy. Exact world copy, no mind or entity edits. Planned actual observer gifts:4bread Fern,8bread Dara. Existing spare homes only; no parental movement/health/age/food/mood/action setter. Original world unchanged. Never merge fixture history.']
 (dst/'experiment.json').write_text(json.dumps(manifest,indent=2)+'\n')
 assert m.tree_files(world)==records,'Source changed during copying'
 result={'createdUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'kind':'CONTROLLED COPY; not ordinary survival evidence','originalRuntime':str(src),'fixtureRuntime':str(dst),'originalWorldSHA256':digest,'originalWorldFiles':records,'changedWorldFiles':[],'plannedFoodInputs':{'5ad3aad5-3e4b-4eb6-8158-87731868dc03':4,'c7c31152-34a1-4edd-bd84-e48f50f86088':8},'inventoriesHealthTerrainUntouched':True,'noAdditionalResources':False,'requestedObservationTicks':2400,'originalWorldUnchangedAfterPreparation':True}
 (out/'setup.json').write_text(json.dumps(result,indent=2)+'\n')
 print('Prepared exact labelled courtship copy of',len(records),'world files; no initial setters. Original unchanged.')
