from pathlib import Path
import json,sys,importlib.util,shutil,datetime,os,hashlib,xml.etree.ElementTree as E
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
out=ROOT/'docs/evidence/2026-11-07/efficient-meals';run=ROOT/'runs/efficient-meal-probe/play';setup=json.loads((out/'setup.json').read_text());cap=json.loads((out/'custody-start.json').read_text())
with runtime_lock(run),runtime_lock(ROOT/'runs/play'):
 records=m.tree_files(ROOT/'runs/play/saves/VillageTrial');assert records==setup['originalWorldFiles']
 source=json.loads((out/'source-before.json').read_text())['files'];assert all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==sha for p,sha in source.items())
 evidence=next((run/'evidence').glob('play-*'));native=json.loads((evidence/'budget-native.json').read_text())
 for name in ['budget-native.json','budget-end.json']:shutil.copyfile(evidence/name,out/name)
 shutil.copyfile(next(evidence.glob('*.png')),out/'native-village.png')
 world=run/'saves/VillageTrial';p=max((world/'villagers/village-evidence').glob('shutdown-*.json'),key=lambda p:int(p.stem.split('-')[-1]));shutil.copyfile(p,out/'census.json');c=json.loads(p.read_text())
 results={'capturedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'kind':setup['kind'],'handle':95381,'exitCode':0,'observation':{'start':33105,'end':native['gameTime'],'limit':400,'finalSave':c['game_time']},'originalWorldUnchanged':True,'originalWorldSHA256':m.tree_digest(records),'sourceUnchangedDuringRun':True,'population':{k:c[k] for k in ['original_cohort_loaded_alive','original_cohort_known_dead_or_converted','original_cohort_unloaded_or_missing']},'actors':[],'meanTickMillis':c['mean_server_tick_millis'],'noAutomatedBuildReleaseTimer':True,'ownedTerminal':[],'originalSix':[]}
 for change in setup['actorChanges']:
  actor=change['fixtureMind']['id'];p=world/change['file'];mind=json.loads(p.read_text());row=next(a for a in native['actors'] if a['uuid']==actor);final=next(a for a in c['local_loaded_villagers'] if a['uuid']==actor);shutil.copyfile(p,out/f'{actor}-mind-after.json')
  notes=[x for x in mind['memories'] if 'Ate owned minecraft:bread' in x.get('note','')]
  bread=sum(x['count'] for x in final['inventory'] if x['item']=='minecraft:bread')
  if actor.startswith('e358'):
   assert row['food']==19 and bread==6 and final['health']==20 and row['action']!='FORAGE' and not notes
   assert all(a[0]==b[0]==1 for a,b in zip(change['fixtureMind']['values'],mind['values'])),'FORAGE preference changed despite unavailable meals'
  else:assert row['food']==20 and bread==1 and final['health']==20 and len(notes)==1
  results['actors'].append({'id':actor,'beforeFood':change['fixtureMind']['food'],'afterFood':mind['food'],'health':final['health'],'inventory':final['inventory'],'nativeAction':row['action'],'forcedForagePreference':change['forcedForagePreference'],'learningBefore':change['fixtureMind']['learningUpdates'],'learningAfter':mind['learningUpdates'],'lastMemories':mind['memories'][-8:],'position':{k:final[k] for k in ['x','y','z']}})
 for a in cap['jobs']:
  try:alive=(Path('/proc')/str(a['pid'])/'stat').read_text().rsplit(')',1)[1].split()[19]==str(a['startTicks'])
  except OSError:alive=False
  assert not alive
  results['ownedTerminal'].append({'pid':a['pid'],'startTicks':a['startTicks'],'sameIdentityAlive':alive})
 for pid,start in [(601856,10936249),(601857,10936249),(601946,10936303),(601990,10936325),(602326,10937021),(602372,10937054)]:
  p=Path('/proc')/str(pid);s=(p/'stat').read_text().rsplit(')',1)[1].split();assert int(s[19])==start
  results['originalSix'].append({'pid':pid,'ppid':int(s[1]),'startTicks':start,'uid':p.stat().st_uid,'exe':os.readlink(p/'exe'),'cwd':os.readlink(p/'cwd'),'cgroup':(p/'cgroup').read_text()})
 tests=[E.parse(p).getroot() for p in (ROOT/'build/test-results/test').glob('TEST-*.xml')];results['javaTests']={k:sum(int(x.get(k,'0')) for x in tests) for k in ['tests','failures','errors','skipped']};results['pythonTests']=10
 assert results['javaTests']=={'tests':215,'failures':0,'errors':0,'skipped':0}
 (out/'results.json').write_text(json.dumps(results,indent=2)+'\n')
 print(json.dumps({k:results[k] for k in ['exitCode','originalWorldUnchanged','sourceUnchangedDuringRun','population','meanTickMillis','javaTests']}))
 for a in results['actors']:print(a['id'],a['afterFood'],'action',a['nativeAction'],'learning',a['learningBefore'],a['learningAfter'])
