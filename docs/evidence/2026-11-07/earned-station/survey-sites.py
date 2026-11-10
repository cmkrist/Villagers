"""Read stopped earned-wood world only; shortlist air sites, never infer ownership or native path validity."""
from pathlib import Path
import ast,struct,zlib,hashlib,json,sys,math
ROOT=Path('/home/niko/.internal/source/Villagers');RUN=ROOT/'runs/forestry-root/play';OUT=Path(__file__).resolve().parent/'site-survey';OUT.mkdir(exist_ok=True)
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
source=ROOT/'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py';tree=ast.parse(source.read_text());defs=[n for n in tree.body if isinstance(n,ast.ClassDef)]
ns={'Path':Path,'struct':struct,'zlib':zlib,'hashlib':hashlib,'MAX':4*1024*1024,'ROOT':ROOT,'WORLD':RUN/'saves/VillageTrial','OUT':OUT};exec(compile(ast.Module(body=defs,type_ignores=[]),str(source),'exec'),ns)
solid={'minecraft:mud','minecraft:dirt','minecraft:grass_block','minecraft:stone','minecraft:sand','minecraft:cobblestone','minecraft:coarse_dirt','minecraft:granite','minecraft:andesite','minecraft:diorite'}
with runtime_lock(RUN):
 r=ns['Reader']();body=(-282.22633201407996,75,745.6282591472983);home=(-269,75,733);candidates=[]
 def air(x,y,z):return r.block(x,y,z)['Name']=='minecraft:air'
 def clear(x,y,z):return air(x,y,z) and air(x,y+1,z) and r.block(x,y-1,z)['Name'] in solid
 for x in range(-289,-275):
  for z in range(739,753):
   for y in range(69,79):
    if sum((a-b)**2 for a,b in zip((x+.5,y+.5,z+.5),home))>28**2 or not clear(x,y,z):continue
    access=[(x+dx,y,z+dz) for dx,dz in [(-1,0),(1,0),(0,-1),(0,1)] if clear(x+dx,y,z+dz)]
    if len(access)<2:continue
    nearby=[r.block(xx,yy,zz)['Name'] for xx in range(x-2,x+3) for yy in range(y-1,y+2) for zz in range(z-2,z+3)]
    if any('_bed' in n or '_door' in n or n in {'minecraft:chest','minecraft:barrel','minecraft:crafting_table','minecraft:furnace'} for n in nearby):continue
    candidates.append({'position':[x,y,z],'support':r.block(x,y-1,z),'access':access,'distanceToRecordedBody':math.dist((x+.5,y,z+.5),body)})
 candidates.sort(key=lambda p:p['distanceToRecordedBody']);report={'sourceRuntime':str(RUN),'kind':'Stopped read-only shortlist; not native safe-site/path/protection acceptance','recordedActor':body,'home':home,'candidates':candidates,'chunks':r.identities}
 (OUT/'candidates.json').write_text(json.dumps(report,indent=2)+'\n');print('candidates',len(candidates));print(json.dumps(candidates[:8],indent=2))
