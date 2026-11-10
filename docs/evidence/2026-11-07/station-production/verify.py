"""Read-only verification of costs, denied/paid builds, native saves and source bindings."""
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
DARA = 'c7c31152-34a1-4edd-bd84-e48f50f86088'
phases = [BASE, BASE / 'approved', BASE / 'job-range', BASE / 'persistence']
old = read(BASE / 'source-before.json')['files']
current = read(BASE / 'job-range/source-before.json')['files']
assert old == read(BASE / 'approved/source-before.json')['files']
assert current == read(BASE / 'persistence/source-before.json')['files']
assert {n for n in old if old[n] != current[n]} == {'src/main/java/local/villagers/ConstructionSites.java'}
assert sha(BASE / 'before-job-range.ConstructionSites.java') == old['src/main/java/local/villagers/ConstructionSites.java']
with zipfile.ZipFile(BASE / 'source-main.zip') as archive:
    assert set(archive.namelist()) == set(current)
    for name, digest in current.items():
        assert sha(ROOT / name) == digest
        assert hashlib.sha256(archive.read(name)).hexdigest() == digest

gift = read(BASE / f'wood-evidence/supply-after-{DARA}.json')
assert gift['given'] == 3 and gift['handBefore'] == '0 minecraft:air'
assert gift['inventoryBefore'] == ['4 minecraft:bread'] + ['0 minecraft:air'] * 7
assert gift['inventoryAfter'] == ['4 minecraft:bread', '2 minecraft:oak_log', '1 minecraft:wheat_seeds'] + ['0 minecraft:air'] * 5
withdraw = read(BASE / 'station-evidence/withdraw-after.json')
assert withdraw == {'oldPositionAir': True, 'observerSlot9': '1 minecraft:crafting_table'}
probe = read(next((BASE / 'construction-evidence').glob('outside-station-touch-*.json')))
assert probe['authorizedBefore'] and not probe['authorizedAfter'] and probe['blockUnchanged']
blocked = read(BASE / 'sites-after.json')
assert len(blocked['stations']) == len(blocked['protections']) == 1 and blocked['schemaVersion'] == 2
assert blocked == read(BASE / 'approved/sites-before.json')
allowed = read(BASE / 'approved/sites-after.json')
assert allowed['stations'] == blocked['stations'] and not allowed['protections']
assert allowed == read(BASE / 'job-range/sites-before.json')
empty = {'schemaVersion': 2, 'farms': [], 'protections': [], 'stations': []}
assert read(BASE / 'job-range/sites-after.json') == read(BASE / 'persistence/sites-before.json') == read(BASE / 'persistence/sites-after.json') == empty

report = {'realInputs': gift['inputs'], 'outsideWriteRevocation': probe, 'runs': []}
for phase, stationRecipes, built, toolRecipes, hoes, logs in zip(phases, [0,0,2,0], [0,0,1,0], [0,0,3,0], [0,0,1,0], [2,2,0,0]):
    result = read(phase / 'results.json'); actor = next(a for a in result['actors'] if a['uuid'] == DARA)
    assert (actor['stationRecipesThisLoad'], actor['tablesBuiltThisLoad'], actor['woodRecipesThisLoad'], actor['hoesCraftedThisLoad']) == (stationRecipes,built,toolRecipes,hoes)
    assert '1 minecraft:wheat_seeds' in actor['inventory']
    assert sum(int(s.split()[0]) for s in actor['inventory'] if s.endswith('minecraft:oak_log')) == logs
    census = read(phase / 'census.json'); saved = next(a for a in census['local_loaded_villagers'] if a['uuid'] == DARA)
    if phase in phases[2:]:
        assert '2 minecraft:stick' in actor['inventory'] and '3 minecraft:bread' in actor['inventory']
        assert saved['equipment']['mainhand'] == {'item': 'minecraft:wooden_hoe', 'count': 1}
        assert 'damage=0/59' in saved['equipment_status']
    plot = read(phase / 'plot.json')['blocks']
    assert plot[0]['state']['Name'] == 'minecraft:air'
    assert plot[1]['state']['Name'] == 'minecraft:sand' and plot[3]['state']['Name'] == 'minecraft:air'
    assert plot[2]['state']['Name'] == ('minecraft:crafting_table' if phase in phases[2:] else 'minecraft:air')
    assert result['exitCode'] == 0 and result['sourceUnchangedDuringRun'] and result['originalWorldUnchanged']
    assert result['population'] == {'original_cohort_loaded_alive': 4, 'original_cohort_known_dead_or_converted': 7,
        'original_cohort_unloaded_or_missing': 0, 'local_loaded_villager_count': 5}
    assert result['javaTests'] == {'tests': 251, 'failures': 0, 'errors': 0, 'skipped': 0} and result['pythonTests'] == 10
    assert len(result['ownedTerminal']) == 3 and all(not a['sameIdentityAlive'] for a in result['ownedTerminal'])
    log = (phase / 'launch.log').read_text()
    assert 'BUILD SUCCESSFUL' in log and 'Saving worlds' in log and 'Animatica animation executor drained and terminated' in log
    report['runs'].append({'phase': phase.name, 'handle': result['handle'], 'exitCode': 0,
        'nativeGameTime': result['nativeGameTime'], 'finalSaveGameTime': result['finalSaveGameTime'],
        'stationRecipes': stationRecipes, 'tablesBuilt': built, 'toolRecipes': toolRecipes, 'hoes': hoes,
        'meanTickMillis': result['meanTickMillis'], 'ownedTerminal': result['ownedTerminal']})

setup = read(BASE / 'setup.json'); original = Path(setup['originalRuntime']); runtime = Path(setup['fixtureRuntime'])
spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec); spec.loader.exec_module(parallel)
with runtime_lock(original), runtime_lock(runtime):
    assert parallel.tree_files(original / 'saves/VillageTrial') == setup['originalWorldFiles']
    sites = runtime / 'saves/VillageTrial/villagers/work-sites.json'
    assert read(sites) == empty and not sites.with_name(sites.name + '.active').exists()
    for name in ['lab-command.txt', 'lab-command.processing.txt']: assert not (runtime / name).exists()
report['sourceWorldFilesUnchanged'] = len(setup['originalWorldFiles'])
report['sourceArchiveSHA256'] = sha(BASE / 'source-main.zip')
expected040 = '4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for path in [ROOT / 'build/libs/villagers-0.4.0.jar', Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]: assert sha(path) == expected040
report['sourceAndDeployed040Unchanged'] = expected040
report['unreleased041dev'] = sha(ROOT / 'build/libs/villagers-0.4.1-dev.jar')
print(json.dumps(report, indent=2))
