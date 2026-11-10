"""Read-only saved-block shortlist, NOT native collision/path authority."""
from pathlib import Path
import ast, json, sys, importlib.util, collections
ROOT=Path('/home/niko/.internal/source/Villagers')
RUN=ROOT/'runs/farm-memory-reopen/play'; WORLD=RUN/'saves/VillageTrial'
OUT=Path(__file__).resolve().parent/'survey'; OUT.mkdir(exist_ok=True)
sys.path.insert(0,str(ROOT/'tools'))
from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
p=ROOT/'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py'
t=ast.parse(p.read_text());t.body=[n for n in t.body if isinstance(n,(ast.Import,ast.ImportFrom,ast.ClassDef))]
ns={'ROOT':ROOT,'WORLD':WORLD,'OUT':OUT,'MAX':4*1024*1024};exec(compile(t,str(p),'exec'),ns)
with runtime_lock(RUN),m.stopped_world(WORLD):
 before=m.tree_files(WORLD);r=ns['Reader']();states={};stands=set();counts=collections.Counter()
 for x in range(-270,-233):
  for z in range(762,793):
   for y in range(54,83):
    b=r.block(x,y,z);states[x,y,z]=b;counts[b['Name']]+=1
 air={'minecraft:air','minecraft:cave_air','minecraft:void_air'}
 # Conservative full-block approximation; native collision verification is still required.
 solid={'minecraft:stone','minecraft:andesite','minecraft:diorite','minecraft:granite','minecraft:sand','minecraft:sandstone','minecraft:cut_sandstone','minecraft:smooth_sandstone','minecraft:dirt','minecraft:grass_block','minecraft:terracotta','minecraft:red_sandstone','minecraft:cobblestone'}
 for x,y,z in states:
  if y>=82 or y<=54:continue
  if states[x,y,z]['Name'] in air and states.get((x,y+1,z),{}).get('Name') in air and states.get((x,y-1,z),{}).get('Name') in solid:stands.add((x,y,z))
 start=(-257,68,779);assert start in stands
 def neighbours(p):
  x,y,z=p
  for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
   for dy in [0,1,-1]:
    q=(x+dx,y+dy,z+dz)
    if q in stands and (dy<=0 or states.get((x,y+2,z),{}).get('Name') in air):yield q
 queue=collections.deque([start]);reach={start};prev={}
 while queue:
  p=queue.popleft()
  for q in neighbours(p):
   if q not in reach:reach.add(q);queue.append(q);prev[q]=p
 close=min(reach,key=lambda p:(p[0]+236)**2+(p[1]-72)**2+(p[2]-766)**2)
 route=[];q=close
 while q!=start:route.append(q);q=prev[q]
 route.append(start);route.reverse()
 report={'kind':'READ-ONLY full-block approximation; no grant, native collision/path result, or world edit', 'runtime':str(RUN),'worldSHA256':m.tree_digest(before),'start':start,'bounds':[-270,-234,54,82,762,792],'counts':dict(counts),'standCells':sorted(stands),'reachableApproximate':sorted(reach),'closestHomeApproximate':close,'routeApproximate':route,'chunks':r.identities,'localBlocks':[{'pos':p,**s} for p,s in states.items() if -261<=p[0]<=-251 and 64<=p[1]<=74 and 774<=p[2]<=784]}
 (OUT/'report.json').write_text(json.dumps(report,indent=2)+'\n');assert m.tree_files(WORLD)==before
 print('stand/reachable',len(stands),len(reach),'closestHome',close,'route',route)
 print('unique blocks',counts)
 print('Local stand heights, x=-263..-250, rows z=773..785; slash means multiple floors')
 for z in range(773,786):
  print(z,' '.join('/'.join(str(y) for xx,y,zz in sorted(stands) if xx==x and zz==z) or '.' for x in range(-263,-249)))
