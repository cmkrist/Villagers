"""Read-only checks of retained construction evidence and the stopped fixture."""
from pathlib import Path
import hashlib
import importlib.util
import json
import re
import sys

BASE = Path(__file__).resolve().parent
ROOT = BASE.parents[3]
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock

def read(path):
    return json.loads(path.read_text())

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

FERN = '5ad3aad5-3e4b-4eb6-8158-87731868dc03'
phases = [BASE, BASE / 'approved', BASE / 'persistence']
report = {'runs': []}
for phase, seeds, wear, plots in zip(phases, [12, 11, 11], [6, 7, 7], [0, 1, 0]):
    result = read(phase / 'results.json')
    actor = next(a for a in result['actors'] if a['uuid'] == FERN)
    census = read(phase / 'census.json')
    saved = next(a for a in census['local_loaded_villagers'] if a['uuid'] == FERN)
    assert actor['inventory'][0] == f'{seeds} minecraft:wheat_seeds'
    assert actor['inventory'][1] == '2 minecraft:bread'
    assert actor['plotsCultivatedThisLoad'] == plots
    damage = int(re.search(r'damage=(\d+)/250', saved['equipment_status'])[1])
    assert damage == wear
    assert result['exitCode'] == 0 and result['sourceUnchangedDuringRun']
    assert result['originalWorldUnchanged']
    assert result['population'] == {
        'original_cohort_loaded_alive': 4,
        'original_cohort_known_dead_or_converted': 7,
        'original_cohort_unloaded_or_missing': 0,
        'local_loaded_villager_count': 5,
    }
    assert result['javaTests'] == {'tests': 237, 'failures': 0, 'errors': 0, 'skipped': 0}
    assert result['pythonTests'] == 10
    for name, expected in read(phase / 'source-before.json')['files'].items():
        assert digest(ROOT / name) == expected, name
    assert all(not item['sameIdentityAlive'] for item in result['ownedTerminal'])
    log = (phase / 'launch.log').read_text()
    assert 'BUILD SUCCESSFUL' in log and 'Saving worlds' in log
    assert 'Animatica animation executor drained and terminated' in log
    report['runs'].append({'phase': phase.name, 'handle': result['handle'],
        'exitCode': 0, 'wheatSeeds': seeds, 'hoeDamage': damage,
        'plotsThisLoad': plots, 'finalSaveGameTime': result['finalSaveGameTime'],
        'sourcePinsMatch': True, 'sampledOwnedJobsTerminal': result['ownedTerminal']})

probe = read(BASE / 'native-revocation/outside-touch-38349.json')
assert probe['authorizedBefore'] and not probe['authorizedAfter'] and probe['blockUnchanged']
protected = read(BASE / 'sites-after-protected.json')
assert len(protected['farms']) == len(protected['protections']) == 1
assert protected == read(BASE / 'approved/sites-before.json')
empty = {'schemaVersion': 1, 'farms': [], 'protections': []}
assert read(BASE / 'approved/sites-after.json') == empty
assert read(BASE / 'persistence/sites-before.json') == empty
assert read(BASE / 'persistence/census-before.json')['game_time'] == 39738
for phase in phases[1:]:
    blocks = read(phase / 'plot.json')['blocks']
    assert blocks[1]['position'] == [-228, 71, 752]
    assert blocks[1]['state']['Name'] == 'minecraft:farmland'
    assert blocks[2]['state']['Name'] == 'minecraft:wheat'
    assert all(b['state']['Name'] == 'minecraft:air' for b in blocks[3:])

spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec)
spec.loader.exec_module(parallel)
setup = read(BASE / 'setup.json')
original = Path(setup['originalRuntime'])
runtime = Path(setup['fixtureRuntime'])
with runtime_lock(runtime), runtime_lock(original):
    assert parallel.tree_files(original / 'saves/VillageTrial') == setup['originalWorldFiles']
    sites = runtime / 'saves/VillageTrial/villagers/work-sites.json'
    assert read(sites) == empty
    assert not sites.with_name(sites.name + '.active').exists()
    report['cleanSiteMetadata'] = empty
    report['activeMarkerAbsent'] = True
    report['originalWorldFilesUnchanged'] = len(setup['originalWorldFiles'])

expected_040 = '4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
artifacts = [ROOT / 'build/libs/villagers-0.4.0.jar',
    Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]
for artifact in artifacts:
    assert digest(artifact) == expected_040
report['preserved040Artifacts'] = {str(p): expected_040 for p in artifacts}
report['developmentArtifact'] = {'path': 'build/libs/villagers-0.4.1-dev.jar',
    'sha256': digest(ROOT / 'build/libs/villagers-0.4.1-dev.jar'), 'released': False}
report['outsideWriteRevocation'] = probe
report['approvedRunCustodyLimit'] = 'Live PID sample missed; no PID inferred. Exit0/save/free lock verified.'
print(json.dumps(report, indent=2))
