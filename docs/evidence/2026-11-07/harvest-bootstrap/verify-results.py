"""Verify recorded native crop conservation and persistence; never launch or mutate a world."""
from pathlib import Path
import datetime, hashlib, importlib.util, json, sys, zipfile

ROOT = Path(__file__).resolve().parents[4]
OUT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock
spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec)
spec.loader.exec_module(parallel)
checks = []

def read(name):
    return json.loads((OUT / name).read_text())

def check(ok, note):
    if not ok:
        raise AssertionError(note)
    checks.append(note)

def fern(d):
    return next(a for a in d['actors'] if a['uuid'] == '5ad3aad5-3e4b-4eb6-8158-87731868dc03')

def count(a, item):
    return sum(int(s.split()[0]) for s in a['inventory'] if s.endswith(' ' + item))

def crop_map(d):
    return {tuple(c['position']): c['state'] for c in d['crops']}

early = fern(read('early-observation.json'))
produced, reopened = read('results.json'), read('persistence/results.json')
a, b = fern(produced), fern(reopened)
check(count(early, 'minecraft:beetroot') == count(early, 'minecraft:beetroot_seeds') == 0,
      'Native pre-harvest observation has no beetroot or matching seeds')
check(early['adventureActivity']['cropsHarvested'] == 0, 'Early observation precedes successful harvests')
check(count(a, 'minecraft:beetroot') == 2 and count(a, 'minecraft:beetroot_seeds') == 4,
      'Native harvest creates exactly two held beetroots and four retained seeds')
check(all(count(a, item) == count(early, item) for item in
          ['minecraft:wheat_seeds', 'minecraft:wheat', 'minecraft:bread']),
      'Original wheat, wheat seeds and bread retained by producer')
activity = a['adventureActivity']
check((activity['cropsHarvested'], activity['replantsFromHarvest'], activity['replantsFromStock']) == (2, 1, 1),
      'Two successful harvests with one real harvest-funded and one stock-funded replant')
check(early['selfFoodDeficit'] == 10 and a['selfFoodDeficit'] == 8 and early['food'] == a['food'] == 20,
      'Edible held reserves improve by two; no harvested-meal consumption claimed')
r = a['lastHarvest']
check(r['gameTime'] == 54594 and r['position'] == '-230, 72, 752' and r['crop'] == 'BEETROOT',
      'Exact retained second native harvest')
check(r['plantingSource'] == 'owned stock' and r['nativeDrops'] == ['1 minecraft:beetroot', '4 minecraft:beetroot_seeds'],
      'Actual second loot and payment source recorded')
check(count({'inventory': r['inventoryBefore']}, 'minecraft:beetroot_seeds') == 1 and
      count({'inventory': r['inventoryAfter']}, 'minecraft:beetroot_seeds') == 4,
      'Second replant conserves one owned seed plus four actual drops')
check(r['inventoryAfter'] == a['inventory'] and r['overflow'] == [], 'Actual final bag matches receipt; no overflow')
check(r['hoeDamageBefore'] == 9 and r['hoeDamageAfter'] == 10 and not r['hoeBroke'] and
      r['cropAfter'] == 'Block{minecraft:beetroots}[age=0]', 'Native wear and age-zero replant recorded')
parent = json.loads((OUT.parent / 'earned-station/food-gap/report.json').read_text())
before = crop_map(parent)
for phase in [Path('.'), Path('persistence')]:
    result = read(phase / 'results.json')
    report = read(phase / 'food-gap/report.json')
    saved = crop_map(report)
    check(report['farmlandCells'] == 8 and len(saved) == 8 and report['matureCrops'] == 0,
          f'{phase}: eight intact farmland/crops and no ripe crop remaining')
    for pos in [(-230, 72, 752), (-228, 72, 751)]:
        check(before[pos] == {'Name': 'minecraft:beetroots', 'Properties': {'age': '3'}} and
              saved[pos] == {'Name': 'minecraft:beetroots'},
              f'{phase}: mature beetroot becomes saved default-age-zero crop at {pos}')
    wheat = [p for p, state in before.items() if state['Name'] == 'minecraft:wheat']
    check(len(wheat) == 6 and all(saved[p]['Name'] == 'minecraft:wheat' and
          int(before[p]['Properties']['age']) <= int(saved[p]['Properties']['age']) < 7 for p in wheat),
          f'{phase}: six immature wheat retained; natural growth allowed')
    check(next(x['state']['Name'] for x in report['anchors'] if x['position'] == [-281, 75, 745]) == 'minecraft:crafting_table',
          f'{phase}: earned workstation persists')
    check(result['population'] == {'original_cohort_loaded_alive': 4,
          'original_cohort_known_dead_or_converted': 7, 'original_cohort_unloaded_or_missing': 0,
          'local_loaded_villager_count': 5}, f'{phase}: preserved population and death history')
    check(len(result['newSelectedParentChildren']) == 1 and
          result['newSelectedParentChildren'][0]['id'] == '589b708d-b768-4a61-a59e-910db084b079',
          f'{phase}: legacy collector child is the already existing ordinary child')
    check(result['sourceUnchangedDuringRun'] and result['originalWorldUnchanged'], f'{phase}: recorded source/parent unchanged')
    check(result['javaTests'] == {'tests': 286, 'failures': 0, 'errors': 0, 'skipped': 0} and
          result['pythonTests'] == 10, f'{phase}: recorded 286 Java and 10 Python checks pass')
    check(len(result['ownedTerminal']) == 3 and all(not p['sameIdentityAlive'] for p in result['ownedTerminal']),
          f'{phase}: exact owned launch chain terminal')
    log = (OUT / phase / 'launch.log').read_text()
    check('BUILD SUCCESSFUL' in log and 'Stopping worker threads' in log, f'{phase}: native shutdown and successful Gradle terminal log')
    check((OUT / phase / 'commands.txt').read_text().splitlines() ==
          ['villagers familyprobe watch 5ad3aad5-3e4b-4eb6-8158-87731868dc03 c7c31152-34a1-4edd-bd84-e48f50f86088 -269 75 733'],
          f'{phase}: recorded command queue only installs read-only watch')
    pins = read(phase / 'source-before.json')['files']
    with zipfile.ZipFile(OUT / 'source-main.zip') as z:
        check(all(hashlib.sha256(z.read(n)).hexdigest() == h for n, h in pins.items()), f'{phase}: archive matches native source pins')
    check(all(hashlib.sha256((ROOT / n).read_bytes()).hexdigest() == h for n, h in pins.items()),
          f'{phase}: current main source matches native cohort')
check(b['inventory'] == a['inventory'] and b['food'] == 20 and b['selfFoodDeficit'] == 8,
      'Actual earned bag/nutrition reserve retained on restart')
check(b['adventureActivity']['cropsHarvested'] == 0 and b['lastHarvest'] == {},
      'Restart has no new harvest; diagnostic state resets normally')
for case, damage in [(OUT.parent / 'earned-station/persistence', 8), (OUT, 10), (OUT / 'persistence', 10)]:
    c = json.loads((case / 'census.json').read_text())
    actor = next(x for x in c['local_loaded_villagers'] if x['uuid'] == a['uuid'])
    check(f'iron_hoe x1 damage={damage}/250' in actor['equipment_status'], f'{case.name}: native hoe damage {damage}')
setup = read('setup.json')
with runtime_lock(Path(setup['originalRuntime'])), runtime_lock(ROOT / 'runs/harvest-bootstrap/play'):
    check(parallel.tree_files(Path(setup['originalRuntime']) / 'saves/VillageTrial') == setup['originalWorldFiles'],
          'All 270 parent world files still match original manifest')
    check(not any((ROOT / 'runs/harvest-bootstrap/play' / n).exists() for n in
          ['lab-command.txt', 'lab-command.processing.txt', 'saves/VillageTrial/villagers/work-sites.json.active']),
          'Native fixture stopped with no pending command or dirty edit marker')
expected = '4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT / 'build/libs/villagers-0.4.0.jar', Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:
    check(hashlib.sha256(p.read_bytes()).hexdigest() == expected, 'Preserved deployed 0.4.0: ' + str(p))
summary = {'verifiedUTC': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'checksPassed': len(checks),
           'checks': checks, 'javaTests': 286, 'pythonTests': 10, 'nativeHarvestFundedReplantVerified': True,
           'restartRetainedYieldAndWear': True, 'farmMealConsumptionOrSustainableYieldProven': False,
           'persistenceTerminalBasis': 'Normal shutdown/BUILD SUCCESSFUL log and exact chain terminal; original handle output truncated. Collector exitCode is not independent evidence.',
           'sourceArchiveSHA256': hashlib.sha256((OUT / 'source-main.zip').read_bytes()).hexdigest(),
           'devJarSHA256': hashlib.sha256((ROOT / 'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),
           'noDeployment': True}
(OUT / 'verification.json').write_text(json.dumps(summary, indent=2) + '\n')
print({k: v for k, v in summary.items() if k != 'checks'})
