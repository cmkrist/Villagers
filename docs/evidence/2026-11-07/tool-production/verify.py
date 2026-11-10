"""Read-only assertions across the retained production/reopen evidence and source fixture."""
from pathlib import Path
import hashlib
import importlib.util
import json
import sys

BASE = Path(__file__).resolve().parent
ROOT = BASE.parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock

def read(path): return json.loads(path.read_text())
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
DARA = 'c7c31152-34a1-4edd-bd84-e48f50f86088'

before = read(BASE / f'supply-before-{DARA}.json')
after = read(BASE / f'supply-after-{DARA}.json')
assert before['handBefore'] == '0 minecraft:air' and after['given'] == 2
assert before['inventoryBefore'] == ['4 minecraft:bread'] + ['0 minecraft:air'] * 7
assert after['inventoryAfter'] == ['4 minecraft:bread', '1 minecraft:oak_log',
    '1 minecraft:wheat_seeds'] + ['0 minecraft:air'] * 5
observed = read(BASE / 'production-observed.json')
actor = next(a for a in observed['actors'] if a['uuid'] == DARA)
assert actor['nativeAge'] > 0 and not actor['selfFoodGoal'] and not actor['familyFoodGoal']
assert actor['woodRecipesThisLoad'] == 3 and actor['hoesCraftedThisLoad'] == 1
assert actor['inventory'][0] == '4 minecraft:bread'
assert '1 minecraft:wheat_seeds' in actor['inventory'] and '2 minecraft:stick' in actor['inventory']

report = {'supplyGameTime': before['gameTime'], 'suppliedMaterials': before['inputs'],
    'productionObservedAt': observed['gameTime'], 'independentOfFoodAndBirthGoals': True, 'runs': []}
for phase, recipes, hoes in [(BASE, 3, 1), (BASE / 'persistence', 0, 0)]:
    result = read(phase / 'results.json')
    actor = next(a for a in result['actors'] if a['uuid'] == DARA)
    census = read(phase / 'census.json')
    saved = next(a for a in census['local_loaded_villagers'] if a['uuid'] == DARA)
    assert actor['woodRecipesThisLoad'] == recipes and actor['hoesCraftedThisLoad'] == hoes
    held = saved['equipment']['mainhand']
    assert held == {'item': 'minecraft:wooden_hoe', 'count': 1}
    assert 'damage=0/59' in saved['equipment_status']
    assert sorted(s for s in actor['inventory'] if not s.startswith('0 ')) == [
        '1 minecraft:wheat_seeds', '2 minecraft:stick', '3 minecraft:bread']
    assert result['exitCode'] == 0 and result['sourceUnchangedDuringRun'] and result['originalWorldUnchanged']
    assert result['population'] == {'original_cohort_loaded_alive': 4,
        'original_cohort_known_dead_or_converted': 7, 'original_cohort_unloaded_or_missing': 0,
        'local_loaded_villager_count': 5}
    assert result['javaTests'] == {'tests': 244, 'failures': 0, 'errors': 0, 'skipped': 0}
    assert result['pythonTests'] == 10
    assert len(result['ownedTerminal']) == 3 and all(not a['sameIdentityAlive'] for a in result['ownedTerminal'])
    for name, digest in read(phase / 'source-before.json')['files'].items(): assert sha(ROOT / name) == digest, name
    log = (phase / 'launch.log').read_text()
    assert 'BUILD SUCCESSFUL' in log and 'Saving worlds' in log
    assert 'Animatica animation executor drained and terminated' in log
    report['runs'].append({'phase': phase.name, 'handle': result['handle'], 'exitCode': 0,
        'nativeGameTime': result['nativeGameTime'], 'finalSaveGameTime': result['finalSaveGameTime'],
        'recipesThisLoad': recipes, 'hoesThisLoad': hoes, 'hoeDamage': 0, 'seed': 1, 'sticks': 2,
        'bread': 3, 'meanTickMillis': result['meanTickMillis'], 'sourcePinsMatch': True,
        'ownedTerminal': result['ownedTerminal']})

spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec); spec.loader.exec_module(parallel)
setup = read(BASE / 'setup.json'); original = Path(setup['originalRuntime']); runtime = Path(setup['fixtureRuntime'])
with runtime_lock(original), runtime_lock(runtime):
    assert parallel.tree_files(original / 'saves/VillageTrial') == setup['originalWorldFiles']
    for name in ['lab-command.txt', 'lab-command.processing.txt']: assert not (runtime / name).exists()
    report['sourceWorldFilesUnchanged'] = len(setup['originalWorldFiles'])
    report['noPendingCommands'] = True
expected = '4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for path in [ROOT / 'build/libs/villagers-0.4.0.jar', Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:
    assert sha(path) == expected
report['sourceAndDeployed040Unchanged'] = expected
report['unreleased041dev'] = sha(ROOT / 'build/libs/villagers-0.4.1-dev.jar')
print(json.dumps(report, indent=2))
