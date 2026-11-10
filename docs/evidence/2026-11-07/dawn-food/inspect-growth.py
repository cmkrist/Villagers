"""Read saved crop/soil/overhead conditions without loading or changing a world."""
from pathlib import Path
import ast, hashlib, json, struct, sys, zlib

ROOT = Path(__file__).resolve().parents[4]
BASE = Path(__file__).resolve().parent
OUT = BASE / (sys.argv[1] if len(sys.argv) > 1 else 'after-growth')
OUT.mkdir(exist_ok=True)
RUN = ROOT / 'runs/dawn-food/play'
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock
source = ROOT / 'docs/evidence/2026-11-07/farm-capacity/inspect-saved-farm.py'
classes = [n for n in ast.parse(source.read_text()).body if isinstance(n, ast.ClassDef)]
ns = dict(Path=Path, struct=struct, zlib=zlib, hashlib=hashlib, MAX=4*1024*1024,
          ROOT=ROOT, WORLD=RUN / 'saves/VillageTrial', OUT=OUT)
exec(compile(ast.Module(body=classes, type_ignores=[]), str(source), 'exec'), ns)
baseline = json.loads((BASE.parent / 'farm-regrowth/persistence/after-growth/report.json').read_text())
with runtime_lock(RUN):
    reader = ns['Reader']()
    crops = []
    for previous in baseline['crops']:
        x, y, z = previous['position']
        state = reader.block(x, y, z)
        crops.append(dict(position=[x, y, z], crop=state, soil=reader.block(x, y-1, z),
                          obstructionsAboveTo100=[dict(position=[x, h, z], state=reader.block(x, h, z))
                          for h in range(y+1, 101) if reader.block(x, h, z)['Name'] not in
                          ['minecraft:air', 'minecraft:cave_air', 'minecraft:void_air']]))
    water = []
    for x in range(-234, -221):
        for z in range(747, 758):
            for y in [71, 72]:
                state = reader.block(x, y, z)
                if state['Name'] == 'minecraft:water' or state.get('Properties', {}).get('waterlogged') == 'true':
                    water.append(dict(position=[x, y, z], state=state))
    census = json.loads((BASE / 'census.json').read_text())
    report = dict(kind='Read-only saved growth substrate/overhead survey; no native light assertion',
                  gameTime=census['game_time'], crops=crops, water=water, chunks=reader.identities)
    (OUT / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
print('Saved crop ages:', [(c['position'], c['crop']) for c in crops])
print('Moisture:', [c['soil'].get('Properties', {}).get('moisture', '0') for c in crops])
