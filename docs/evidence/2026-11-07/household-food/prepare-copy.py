from pathlib import Path
import sys,importlib.util,shutil,json,datetime
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'))
from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
src=ROOT/'runs/family-company/play';dst=ROOT/'runs/household-food/play';out=ROOT/'docs/evidence/2026-11-07/household-food';world=src/'saves/VillageTrial'
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
 rel='villagers/5ad3aad5-3e4b-4eb6-8158-87731868dc03/mind.json';p=copy/rel
 before=json.loads(p.read_text());after=json.loads(p.read_text())
 after['food']={'foodLevel':15,'saturation':0.0,'exhaustion':0.0,'timer':0};after['hunger']=.25
 p.write_text(json.dumps(after,indent=2)+'\n')
 changed=m.tree_files(copy);diff=[name for name in records if records[name]!=changed.get(name)]
 assert diff==[rel] and set(changed)==set(records),diff
 manifest=json.loads((src/'experiment.json').read_text());manifest.update({'prepared_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'runtime':str(dst),'source_snapshot':str(world),'source_world_sha256':digest,'source_world_files':records,'mutable_data_shared':False})
 manifest['notes']=['LABELLED self-supply copy of existing5-resident family fixture. Only copied Fern hunger state15/sat0/exhaustion0/timer0; native inventory/health/age/cooldowns unchanged. Planned time set day for work eligibility; no new supplies/terrain setters. Source family world unchanged; do not merge copied history.']
 (dst/'experiment.json').write_text(json.dumps(manifest,indent=2)+'\n')
 assert m.tree_files(world)==records,'Source changed during copying'
 result={'createdUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'kind':'CONTROLLED COPY; not ordinary survival evidence','originalRuntime':str(src),'fixtureRuntime':str(dst),'originalWorldSHA256':digest,'originalWorldFiles':records,'onlyChangedWorldFile':rel,'beforeMind':before,'fixtureMind':after,'inventoriesHealthTerrainUntouched':True,'noAdditionalResources':True,'requestedObservationTicks':1200,'plannedClockIntervention':'time set day; game time and birth cooldown not reset','originalWorldUnchangedAfterPreparation':True}
 (out/'setup.json').write_text(json.dumps(result,indent=2)+'\n')
 print('Prepared labelled copy; only changed',rel,'of',len(records),'world files. Original world unchanged.')
