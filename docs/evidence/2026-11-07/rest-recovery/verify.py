"""Read-only native recovery, persistence, bounded planning and custody verification."""
from pathlib import Path
import hashlib
import importlib.util
import json
import sys
import zipfile

BASE = Path(__file__).resolve().parent
ROOT = BASE.parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock
def read(path): return json.loads(path.read_text())
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
JUNIPER = '231e4c00-19df-4be5-a5ba-5ebc6d29e27f'
before = read(BASE / f'{JUNIPER}-mind-before.json')
assert before['fatigue'] == 1
pins = read(BASE / 'source-before.json')['files']
assert pins == read(BASE / 'persistence/source-before.json')['files']
with zipfile.ZipFile(BASE / 'source-main.zip') as archive:
    for name, digest in pins.items():
        assert sha(ROOT / name) == digest and hashlib.sha256(archive.read(name)).hexdigest() == digest
report = {'startingFatigue': before['fatigue'], 'newSimulationInputs': [], 'runs': []}
for phase, bouts, plans, rejected in [(BASE, 5, 30, 2), (BASE / 'persistence', 1, 10, 1)]:
    result = read(phase / 'results.json'); actor = next(a for a in result['actors'] if a['uuid'] == JUNIPER)
    assert actor['groundRestsThisLoad'] == bouts and actor['bedRestsThisLoad'] == 0
    assert actor['restPathRejections'] == rejected
    assert actor['survival']['plans'] == actor['survival']['homePathPlans'] == plans
    assert actor['survival']['partialHomeRoutes'] == 0
    assert actor['position'] == [-234.2069023807201, 73.0, 766.6999999880791]
    assert 0 <= actor['fatigue'] < .5 and actor['health'] == 20 and not actor['sleeping']
    assert all(s == '0 minecraft:air' for s in actor['inventory'])
    saved = read(phase / f'{JUNIPER}-mind-after.json')
    assert saved['home'] == before['home']
    assert abs(saved['fatigue'] - actor['fatigue']) < .01
    assert result['exitCode'] == 0 and result['originalWorldUnchanged'] and result['sourceUnchangedDuringRun']
    assert result['population'] == {'original_cohort_loaded_alive': 4, 'original_cohort_known_dead_or_converted': 7,
        'original_cohort_unloaded_or_missing': 0, 'local_loaded_villager_count': 5}
    assert result['javaTests'] == {'tests': 258, 'failures': 0, 'errors': 0, 'skipped': 0} and result['pythonTests'] == 10
    log = (phase / 'launch.log').read_text()
    assert 'BUILD SUCCESSFUL' in log and 'Saving worlds' in log and 'Animatica animation executor drained and terminated' in log
    if phase == BASE:
        assert len(result['ownedTerminal']) == 3 and all(not x['sameIdentityAlive'] for x in result['ownedTerminal'])
    else:
        assert result['ownedTerminal'] == [] and read(phase / 'custody-start.json')['jobs'] == []
    report['runs'].append({'phase': phase.name, 'handle': result['handle'], 'exitCode': 0,
        'nativeGameTime': result['nativeGameTime'], 'finalSaveGameTime': result['finalSaveGameTime'],
        'fatigue': actor['fatigue'], 'groundRests': bouts, 'homePlans': plans, 'restPathRejections': rejected,
        'partialHomeRoutes': 0, 'position': actor['position'], 'meanTickMillis': result['meanTickMillis'],
        'sampledOwnedTerminal': result['ownedTerminal']})
first = read(BASE / 'persistence/reopen-first-observation.json')
start = next(a for a in first['actors'] if a['uuid'] == JUNIPER)
persisted = read(BASE / 'persistence/juniper-mind-before.json')
assert start['groundRestsThisLoad'] == 0 and abs(start['fatigue'] - persisted['fatigue']) < .01
report['reopenFirstFatigue'] = start['fatigue']
report['reopenGameTime'] = first['gameTime']
report['partialReturnAndHomeArrivalUnproven'] = True
report['lateReopenCustodySample'] = 'No PID inferred; exit0,normal save,free runtime lock,no tagged client establish terminal state.'
setup = read(BASE / 'setup.json'); original = Path(setup['originalRuntime']); runtime = Path(setup['fixtureRuntime'])
spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec); spec.loader.exec_module(parallel)
with runtime_lock(original), runtime_lock(runtime):
    assert parallel.tree_files(original / 'saves/VillageTrial') == setup['originalWorldFiles']
    for name in ['lab-command.txt', 'lab-command.processing.txt']: assert not (runtime / name).exists()
report['sourceWorldFilesUnchanged'] = len(setup['originalWorldFiles'])
report['sourceArchiveSHA256'] = sha(BASE / 'source-main.zip')
expected = '4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for path in [ROOT / 'build/libs/villagers-0.4.0.jar', Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]: assert sha(path) == expected
report['sourceAndDeployed040Unchanged'] = expected
report['unreleased041dev'] = sha(ROOT / 'build/libs/villagers-0.4.1-dev.jar')
print(json.dumps(report, indent=2))
