"""Verify the native collision diagnosis, natural movement, persisted home and exact job bindings."""
from pathlib import Path
import hashlib
import importlib.util
import json
import math
import sys
import zipfile

BASE = Path(__file__).resolve().parent
ROOT = BASE.parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock
def read(path): return json.loads(path.read_text())
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
ID = '231e4c00-19df-4be5-a5ba-5ebc6d29e27f'
oldPins = read(BASE / 'source-before.json')['files']; newPins = read(BASE / 'centering/source-before.json')['files']
assert oldPins == read(BASE / 'inspection/source-before.json')['files']
assert newPins == read(BASE / 'persistence/source-before.json')['files']
for archive, pins in [('probe-source.zip',oldPins),('source-main.zip',newPins)]:
    with zipfile.ZipFile(BASE / archive) as z:
        assert set(z.namelist()) == set(pins)
        for name,digest in pins.items():
            assert hashlib.sha256(z.read(name)).hexdigest() == digest
            if archive == 'source-main.zip': assert sha(ROOT / name) == digest
inspection = read(BASE / 'inspection/native-inspection.json')
assert inspection['onGround'] and not inspection['collisionFree'] and inspection['pose'] == 'STANDING'
assert inspection['homePath']['nodes'] == 1 and not inspection['homePath']['canReach']
intersections = [c for c in inspection['nearby'] if c['intersectsBody']]
assert len(intersections) == 2 and all('smooth_sandstone_slab' in c['state'] and 'type=top' in c['state'] for c in intersections)
beforePosition = inspection['position']; beforeHome = inspection['home']
center = [beforeHome[k]+.5 for k in ['x','y','z']]
report = {'initialPosition':beforePosition,'initialHomeDistance':math.dist(beforePosition,center),
    'nativeRoofIntersections':intersections,'initialNativePathNodes':1,'runs':[]}
for phase, count in [(BASE,258),(BASE/'inspection',258),(BASE/'centering',262),(BASE/'persistence',262)]:
    result = read(phase/'results.json'); actor = next(a for a in result['actors'] if a['uuid'] == ID)
    saved = read(phase/f'{ID}-mind-after.json'); assert saved['home'] == beforeHome
    assert result['exitCode'] == 0 and result['sourceUnchangedDuringRun'] and result['originalWorldUnchanged']
    assert result['population'] == {'original_cohort_loaded_alive':4,'original_cohort_known_dead_or_converted':7,
        'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':5}
    assert result['javaTests'] == {'tests':count,'failures':0,'errors':0,'skipped':0} and result['pythonTests'] == 10
    assert len(result['ownedTerminal']) == 3 and all(not j['sameIdentityAlive'] for j in result['ownedTerminal'])
    log = (phase/'launch.log').read_text()
    assert 'BUILD SUCCESSFUL' in log and 'Saving worlds' in log and 'Animatica animation executor drained and terminated' in log
    if phase in [BASE,BASE/'inspection']: assert actor['position'] == beforePosition
    if phase == BASE/'centering':
        assert actor['survival']['centeringAttempts'] == actor['survival']['centeringSuccesses'] == 1
        assert actor['survival']['centeringFailures'] == 0 and not actor['survival']['centeringActive']
        assert actor['survival']['partialHomeRoutes'] >= 1 and math.dist(actor['position'],center) < 28
    if phase == BASE/'persistence':
        assert actor['survival']['centeringAttempts'] == actor['survival']['centeringSuccesses'] == 0
        assert math.dist(actor['position'],center) < 20 and math.dist(actor['position'],beforePosition) > 25
    report['runs'].append({'phase':phase.name,'handle':result['handle'],'exitCode':0,
        'nativeGameTime':result['nativeGameTime'],'finalSaveGameTime':result['finalSaveGameTime'],
        'position':actor['position'],'homeDistance':math.dist(actor['position'],center),
        'survival':actor['survival'],'meanTickMillis':result['meanTickMillis'],'ownedTerminal':result['ownedTerminal']})
setup = read(BASE/'setup.json'); original = Path(setup['originalRuntime']); runtime = Path(setup['fixtureRuntime'])
spec = importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec); spec.loader.exec_module(parallel)
with runtime_lock(original),runtime_lock(runtime):
    assert parallel.tree_files(original/'saves/VillageTrial') == setup['originalWorldFiles']
    for name in ['lab-command.txt','lab-command.processing.txt']: assert not (runtime/name).exists()
report['sourceWorldFilesUnchanged'] = len(setup['originalWorldFiles'])
report['newFixtureResourcesClockOrActorSetters'] = []
report['bedArrivalAndForestryAccessNotProven'] = True
report['sourceArchiveSHA256'] = sha(BASE/'source-main.zip')
expected040 = '4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]: assert sha(p) == expected040
report['sourceAndDeployed040Unchanged'] = expected040
report['unreleased041dev'] = sha(ROOT/'build/libs/villagers-0.4.1-dev.jar')
print(json.dumps(report,indent=2))
