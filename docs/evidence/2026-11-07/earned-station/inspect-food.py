"""Read-only next-task evidence: saved crop ages and already-recorded native equipment. No game/world edits."""
from pathlib import Path
import ast,struct,zlib,hashlib,json,sys,collections,math
ROOT=Path('/home/niko/.internal/source/Villagers');RUN=ROOT/'runs/earned-station/play';OUT=Path(__file__).resolve().parent/'food-gap';OUT.mkdir(exist_ok=True)
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
source=ROOT/'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py';defs=[n for n in ast.parse(source.read_text()).body if isinstance(n,ast.ClassDef)]
ns={'Path':Path,'struct':struct,'zlib':zlib,'hashlib':hashlib,'MAX':4*1024*1024,'ROOT':ROOT,'WORLD':RUN/'saves/VillageTrial','OUT':OUT};exec(compile(ast.Module(body=defs,type_ignores=[]),str(source),'exec'),ns)
with runtime_lock(RUN):
 r=ns['Reader']();crops=[];tables=[];counts=collections.Counter();center=(-226,72,752)
 for x in range(-234,-217):
  for z in range(744,761):
   for y in range(69,76):
    state=r.block(x,y,z);name=state['Name'];counts[name]+=1
    if name=='minecraft:crafting_table':tables.append([x,y,z])
    if name in {'minecraft:wheat','minecraft:beetroots','minecraft:carrots','minecraft:potatoes'}:
     maximum=3 if name=='minecraft:beetroots' else 7
     crops.append({'position':[x,y,z],'state':state,'mature':int(state.get('Properties',{}).get('age','-1'))==maximum})
 census=json.loads((ROOT/'docs/evidence/2026-11-07/earned-station/persistence/census.json').read_text());actors=[]
 native=json.loads((ROOT/'docs/evidence/2026-11-07/earned-station/persistence/budget-native.json').read_text());byid={a['uuid']:a for a in native['actors']}
 for a in census['local_loaded_villagers']:
  actor={k:a.get(k) for k in ['uuid','name','x','y','z','profession','equipment_status','equipment','home','inventory']}
  actor['foodWork']={k:byid[a['uuid']].get(k) for k in ['food','selfFoodGoal','stockWheatTarget','foodStatus','farmJourneyActive','navigationDone','navigationTarget','craftingStatus','breadCraftedThisLoad']}
  actors.append(actor)
 anchors=[{'position':list(p),'state':r.block(*p)} for p in [center,(-227,71,744),(-226,71,744),(-281,75,745)]]
 report={'kind':'Stopped saved crop ages and existing native shutdown census; no movement/harvest action invoked','runtime':str(RUN),'gameTime':census['game_time'],'center':list(center),'radius':8,'verticalRange':[69,75],'crops':crops,'matureCrops':sum(c['mature'] for c in crops),'anchors':anchors,'actors':actors,'chunks':r.identities,'farmlandCells':counts['minecraft:farmland'],'tablesInSurvey':tables}
 (OUT/'report.json').write_text(json.dumps(report,indent=2)+'\n');print('crops',len(crops),'mature',report['matureCrops'],'farmland',report['farmlandCells']);print(crops)
 for a in actors:print(a['uuid'][:4],a['profession'],a['equipment_status'],a['equipment'].get('mainhand'),a['home'])
 print('anchors',anchors);print('tables',tables)
 for a in actors:
  if a['uuid'].startswith(('5ad3','c7c3')):print(a['uuid'][:4],{k:a[k] for k in ['x','y','z','foodWork']})
