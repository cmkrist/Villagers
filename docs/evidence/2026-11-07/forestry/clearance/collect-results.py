from pathlib import Path
import json,sys,importlib.util,shutil,datetime,os,hashlib,xml.etree.ElementTree as E
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
run=ROOT/sys.argv[1];out=ROOT/sys.argv[2];handle=int(sys.argv[3]);setup=json.loads((out/'setup.json').read_text());cap=json.loads((out/'custody-start.json').read_text())
original=Path(setup['originalRuntime'])
with runtime_lock(run),runtime_lock(original):
 records=m.tree_files(original/'saves/VillageTrial');assert records==setup['originalWorldFiles']
 sources=json.loads((out/'source-before.json').read_text())['files'];unchanged=all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==sha for p,sha in sources.items())
 assert unchanged,'Source changed during test'
 e=max((run/'evidence').glob('play-*'),key=lambda p:p.stat().st_mtime_ns);native=json.loads((e/'budget-native.json').read_text())
 for name in ['budget-native.json','budget-end.json']:shutil.copyfile(e/name,out/name)
 pictures=sorted(e.glob('*.png'));shutil.copyfile(pictures[0],out/'native-village.png')
 w=run/'saves/VillageTrial';p=max((w/'villagers/village-evidence').glob('shutdown-*.json'),key=lambda p:int(p.stem.split('-')[-1]));shutil.copyfile(p,out/'census.json');c=json.loads(p.read_text())
 (out/'family').mkdir(exist_ok=True)
 for p in (w/'villagers/family-evidence').glob('*.json'):
  event=json.loads(p.read_text())
  if event.get('gameTime',0)>33104:shutil.copyfile(p,out/'family'/p.name)
 for p in (w/'villagers/village-evidence').glob('*intervention*'):
  if p.is_file():shutil.copyfile(p,out/p.name)
 selected=native['selectedAdults'];children=[]
 for p in (w/'villagers').glob('*/mind.json'):
  mind=json.loads(p.read_text());lineage=mind.get('lineage')
  if lineage and not lineage.get('godChild',False) and {lineage.get('ancestor'),lineage.get('secondParent')}==set(selected):
   children.append(mind);shutil.copyfile(p,out/f'{mind["id"]}-child-mind.json')
 for actor in selected:shutil.copyfile(w/f'villagers/{actor}/mind.json',out/f'{actor}-mind-after.json')
 r={'capturedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'handle':handle,'exitCode':None,'terminalEvidence':'Native budget stop, normal server/chunk save, BUILD SUCCESSFUL and exact captured identities absent. Original exec handle unavailable after context recovery; numeric process exit not recovered.','originalWorldUnchanged':True,'originalWorldSHA256':m.tree_digest(records),'sourceUnchangedDuringRun':unchanged,'nativeGameTime':native['gameTime'],'finalSaveGameTime':c['game_time'],'actors':native['actors'],'newSelectedParentChildren':children,'population':{k:c[k] for k in ['original_cohort_loaded_alive','original_cohort_known_dead_or_converted','original_cohort_unloaded_or_missing','local_loaded_villager_count']},'meanTickMillis':c['mean_server_tick_millis'],'ownedTerminal':[],'originalSix':[],'noAutomatedBuildReleaseTimer':True}
 for a in cap['jobs']:
  try:alive=(Path('/proc')/str(a['pid'])/'stat').read_text().rsplit(')',1)[1].split()[19]==str(a['startTicks'])
  except OSError:alive=False
  assert not alive;r['ownedTerminal'].append({'pid':a['pid'],'startTicks':a['startTicks'],'sameIdentityAlive':alive})
 for pid,start in [(601856,10936249),(601857,10936249),(601946,10936303),(601990,10936325),(602326,10937021),(602372,10937054)]:
  p=Path('/proc')/str(pid)
  try:
   fields=(p/'stat').read_text().rsplit(')',1)[1].split();same=int(fields[19])==start
   record={'pid':pid,'expectedStartTicks':start,'observedStartTicks':int(fields[19]),'status':'same identity running' if same else 'PID reused; not attributed','uid':p.stat().st_uid,'ppid':int(fields[1]),'exe':os.readlink(p/'exe'),'cwd':os.readlink(p/'cwd'),'cgroup':(p/'cgroup').read_text()}
  except FileNotFoundError:record={'pid':pid,'expectedStartTicks':start,'status':'absent; no replacement inferred'}
  r['originalSix'].append(record)
 tests=[E.parse(p).getroot() for p in (ROOT/'build/test-results/test').glob('TEST-*.xml')];r['javaTests']={k:sum(int(x.get(k,'0')) for x in tests) for k in ['tests','failures','errors','skipped']};r['pythonTests']=10
 (out/'results.json').write_text(json.dumps(r,indent=2)+'\n');print({k:r[k] for k in ['exitCode','originalWorldUnchanged','population','meanTickMillis','javaTests']});print('selected parent children',len(children))
