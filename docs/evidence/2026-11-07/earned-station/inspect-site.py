"""Read stopped native blocks around the approved station; never mutate the saved world."""
from pathlib import Path
import ast,struct,zlib,hashlib,sys,json
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
run=ROOT/sys.argv[1];out=ROOT/sys.argv[2];raw=out/'terrain';raw.mkdir(exist_ok=True)
p=ROOT/'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py';tree=ast.parse(p.read_text());defs=[n for n in tree.body if isinstance(n,ast.ClassDef)]
ns={'Path':Path,'struct':struct,'zlib':zlib,'hashlib':hashlib,'MAX':4*1024*1024,'ROOT':ROOT,'WORLD':run/'saves/VillageTrial','OUT':raw};exec(compile(ast.Module(body=defs,type_ignores=[]),str(p),'exec'),ns)
positions=[(-281,75,745),(-281,74,745),(-281,76,745),(-269,72,723),(-269,71,723)]
for dx,dz in [(-1,0),(1,0),(0,-1),(0,1)]:positions.extend([(-281+dx,y,745+dz) for y in [74,75,76]])
with runtime_lock(run):
 r=ns['Reader']();blocks=[{'position':list(p),'state':r.block(*p)} for p in positions];report={'blocks':blocks,'chunks':r.identities};(out/'plot.json').write_text(json.dumps(report,indent=2)+'\n');print(blocks[:5])
