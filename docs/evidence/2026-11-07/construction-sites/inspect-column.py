from pathlib import Path
import ast,struct,zlib,hashlib,sys,json
ROOT=Path('/home/niko/.internal/source/Villagers');sys.path.insert(0,str(ROOT/'tools'));from runtime_lock import runtime_lock
runtime=ROOT/sys.argv[1];out=ROOT/sys.argv[2];raw=out/'terrain';raw.mkdir(exist_ok=True)
p=ROOT/'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py';tree=ast.parse(p.read_text());defs=[n for n in tree.body if isinstance(n,ast.ClassDef)]
ns={'Path':Path,'struct':struct,'zlib':zlib,'hashlib':hashlib,'MAX':4*1024*1024,'ROOT':ROOT,'WORLD':runtime/'saves/VillageTrial','OUT':raw};exec(compile(ast.Module(body=defs,type_ignores=[]),str(p),'exec'),ns)
with runtime_lock(runtime):
 r=ns['Reader']();blocks=[{'position':[-228,y,752],'state':r.block(-228,y,752)} for y in range(70,76)];report={'blocks':blocks,'chunks':r.identities};(out/'plot.json').write_text(json.dumps(report,indent=2)+'\n');print(blocks)
