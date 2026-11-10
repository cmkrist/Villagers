"""Virtual additions to a read-only saved-block model; NEVER writes Minecraft data."""
from pathlib import Path
import ast, json, sys, importlib.util, collections, math
ROOT=Path('/home/niko/.internal/source/Villagers');RUN=ROOT/'runs/farm-memory-reopen/play';WORLD=RUN/'saves/VillageTrial'
OUT=Path(__file__).resolve().parent/'route-candidate';OUT.mkdir(exist_ok=True)
sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
f=ROOT/'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py';t=ast.parse(f.read_text());t.body=[n for n in t.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.ClassDef))]
ns={'ROOT':ROOT,'WORLD':WORLD,'OUT':OUT,'MAX':4*1024*1024};exec(compile(t,str(f),'exec'),ns)
AIR={'minecraft:air','minecraft:cave_air','minecraft:void_air'}
SOLID={'minecraft:stone','minecraft:andesite','minecraft:diorite','minecraft:granite','minecraft:sand','minecraft:sandstone','minecraft:cut_sandstone','minecraft:smooth_sandstone','minecraft:dirt','minecraft:grass_block','minecraft:terracotta','minecraft:cobblestone','minecraft:oak_planks'}
# Construction order starts beside the existing upper ledge, not on Nell's sole foothold.
placements=[(-256,68,780),(-257,67,780),(-256,68,781),(-256,69,781),(-255,69,781),(-255,70,781),(-255,71,780)]
with runtime_lock(RUN),m.stopped_world(WORLD):
 before=m.tree_files(WORLD);r=ns['Reader']();blocks={}
 for x in range(-265,-219):
  for z in range(758,790):
   for y in range(54,83):blocks[x,y,z]=r.block(x,y,z)['Name']
 def stands():
  return {p for p,b in blocks.items() if b in AIR and blocks.get((p[0],p[1]+1,p[2])) in AIR and blocks.get((p[0],p[1]-1,p[2])) in SOLID}
 def reach(start,drop=1):
  cells=stands();queue=collections.deque([start]);seen={start};prev={}
  while queue:
   x,y,z=queue.popleft()
   for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
    for dy in range(-drop,2):
     q=x+dx,y+dy,z+dz
     if q in cells and q not in seen and (dy<=0 or blocks.get((x,y+2,z)) in AIR):seen.add(q);queue.append(q);prev[q]=(x,y,z)
  return seen,prev
 start=(-257,68,779);home=(-236,72,766)
 initial,_=reach(start);assert len(initial)==1
 stages=[]
 for i,p in enumerate(placements):
  assert blocks[p] in AIR,('not air',p,blocks[p]);x,y,z=p
  adjacent=[q for q in [(x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)] if blocks.get(q) in SOLID]
  assert adjacent,('unattached',p)
  blocks[p]='minecraft:dirt';seen,prev=reach(start)
  stages.append({'index':i,'placement':p,'realDirtCost':1,'existingOrEarlierAttachedNeighbours':adjacent,'reachableApproximate':len(seen)})
 seen,prev=reach(start);end=min(seen,key=lambda p:sum((a-b)**2 for a,b in zip(p,home)))
 route=[end]
 while route[-1]!=start:route.append(prev[route[-1]])
 route.reverse();assert math.dist(end,home)<=2.5,(end,home)
 # Source shortlist is observed saved terrain, not consent or native reachability.
 sources=[]
 for x in range(-263,-222):
  for z in range(701,736):
   for y in range(56,81):
    pos=(x,y,z);b=r.block(*pos)['Name'];d=math.dist(pos,(-227,72,773))
    if b not in {'minecraft:dirt','minecraft:grass_block'} or d>80:continue
    if all(r.block(x,yy,z)['Name'] in AIR for yy in range(y+1,83)) and r.block(x,y-1,z)['Name'] in SOLID:
     sources.append({'position':pos,'kind':b,'homeDistance':d,'support':r.block(x,y-1,z)})
 sources.sort(key=lambda a:a['homeDistance'])
 report={'kind':'READ-ONLY VIRTUAL geometric shortlist, NOT a native path/permission or world edit','runtime':str(RUN),'worldSHA256':m.tree_digest(before),'dirtCost':len(placements),'placements':placements,'stages':stages,'nativeApproachRequired':[-256,70,779],'virtualNellRoute':route,'closestHome':end,'sourceShortlist':sources[:16],'chunks':r.identities,'limitations':['Full-block conservative geometry; ignores native step/path/collision details','Native builder approach, reach/sight, occupancy and construction sequence must be verified','No work grant, wider territory or ownership is implied by saved terrain']}
 (OUT/'report.json').write_text(json.dumps(report,indent=2)+'\n');assert m.tree_files(WORLD)==before
 print('dirt',len(placements),'stages',[(x['placement'],x['reachableApproximate']) for x in stages]);print('Nell route',route);print('source shortlist',sources[:7])
