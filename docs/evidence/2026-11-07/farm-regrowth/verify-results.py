"""Verify food provenance, natural growth and the retained overnight death, without launching a client."""
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
FERN = '5ad3aad5-3e4b-4eb6-8158-87731868dc03'
CHILD = '589b708d-b768-4a61-a59e-910db084b079'
JUNIPER = '231e4c00-19df-4be5-a5ba-5ebc6d29e27f'
def read(name): return json.loads((OUT / name).read_text())
def actor(d, who=FERN): return next(a for a in d['actors'] if a['uuid'] == who)
def count(a, item): return sum(int(s.split()[0]) for s in a['inventory'] if s.endswith(' minecraft:' + item))
def notes(a): return [m.get('note', '') for m in a['mind']['memories']]
def check(ok, note):
    if not ok: raise AssertionError(note)
    checks.append(note)
before = json.loads((OUT.parent / 'harvest-bootstrap/persistence/results.json').read_text())
mid, meal, final, reopen = [read(n) for n in ['mid-observation.json', 'later-observation.json', 'results.json', 'persistence/results.json']]
a, b = actor(final), actor(reopen)
check(count(actor(before), 'beetroot') == 2 and count(actor(meal), 'beetroot') == 1 and count(a, 'beetroot') == 0,
      'Earned beetroot stock follows two→one→zero across actual native observations')
check('Ate owned minecraft:beetroot for hunger or healing' in notes(actor(meal)) and
      notes(a).count('Ate owned minecraft:beetroot for hunger or healing') == 2,
      'Two actual earned beetroot meals recorded; no immediate pre-meal food value invented')
check(a['food'] == 19 and b['food'] == 18, 'Final nutrition and ordinary subsequent metabolism recorded')
check(count(actor(mid), 'bread') == 0 and actor(mid)['foodHelp']['given'] == 1 and
      actor(mid, CHILD)['foodHelp']['received'] == 1 and actor(mid, CHILD)['food'] == 13,
      'Separate previous bread donation feeds existing child; not counted as farm income')
check('Gave one actual minecraft:bread to ' + CHILD in notes(actor(mid)) and
      'Received one actual minecraft:bread from ' + FERN in notes(actor(mid, CHILD)),
      'Both native participants retain real bread-transfer evidence')
for phase, r in [(Path('.'), final), (Path('persistence'), reopen)]:
    check(read(phase / 'terminal.json')['exitCode'] == r['exitCode'] == 0, f'{phase}: direct original handle exited zero')
    check(r['population'] == {'original_cohort_loaded_alive': 3, 'original_cohort_known_dead_or_converted': 8,
          'original_cohort_unloaded_or_missing': 0, 'local_loaded_villager_count': 4},
          f'{phase}: one new original-cohort death and four loaded survivors retained')
    check(all(x['adventureActivity']['cropsHarvested'] == x['breadCraftedThisLoad'] == 0 for x in r['actors']),
          f'{phase}: no second harvest or bread manufacture claimed')
    check(count(actor(r), 'wheat_seeds') == 12 and count(actor(r), 'wheat') == 1 and
          count(actor(r), 'beetroot_seeds') == 4 and count(actor(r), 'beetroot') == 0,
          f'{phase}: real remaining seed/grain stock and consumed produce retained')
    census = read(phase / 'census.json')
    check(JUNIPER not in {x['uuid'] for x in census['local_loaded_villagers']}, f'{phase}: Juniper not among living loaded actors')
    life = census['known_actor_lifecycle'][JUNIPER]
    check(life['terminal_event'] == 'death' and life['terminal_detail'] == 'mob' and life['terminal_game_time'] == 66151,
          f'{phase}: authoritative mob-death terminal event survives; stale historic alive field ignored')
    hoe = next(x['equipment_status'] for x in census['local_loaded_villagers'] if x['uuid'] == FERN)
    check('iron_hoe x1 damage=10/250' in hoe, f'{phase}: no repeated harvest wear')
    plots = read(phase / 'after-growth/report.json')['crops']
    check(len(plots) == 8 and all(c['soil'] == {'Name': 'minecraft:farmland', 'Properties': {'moisture': '7'}} for c in plots),
          f'{phase}: eight wet farmland plots retained')
    mature = next(c for c in plots if c['position'] == [-230, 72, 751])
    check(mature['crop'] == {'Name': 'minecraft:wheat', 'Properties': {'age': '7'}}, f'{phase}: naturally mature wheat persists')
    check(all(c['crop'] == {'Name': 'minecraft:beetroots'} for c in plots if c['position'] in [[-230,72,752],[-228,72,751]]),
          f'{phase}: two previous age-zero beetroot replants retained')
    check(all(not p['sameIdentityAlive'] for p in r['ownedTerminal']) and len(r['ownedTerminal']) == 3,
          f'{phase}: exact owned launch chain terminal')
    check(r['sourceUnchangedDuringRun'] and r['originalWorldUnchanged'], f'{phase}: source and isolated parent preserved')
    check(r['javaTests'] == {'tests':286,'failures':0,'errors':0,'skipped':0} and r['pythonTests'] == 10,
          f'{phase}: current unchanged source retains 286 Java/10 Python passing results')
    check({x['id'] for x in r['selectedParentChildren']} == {CHILD}, f'{phase}: existing child only; no birth claimed')
    pins = read(phase / 'source-before.json')['files']
    with zipfile.ZipFile(OUT.parent / 'harvest-bootstrap/source-main.zip') as z:
        check(all(hashlib.sha256(z.read(n)).hexdigest() == h for n,h in pins.items()), f'{phase}: prior archive binds unchanged native source')
    check(all(hashlib.sha256((ROOT/n).read_bytes()).hexdigest() == h for n,h in pins.items()), f'{phase}: current main source unchanged')
    commands = (OUT / phase / 'commands.txt').read_text().splitlines()
    check(commands[0].startswith('villagers familyprobe watch ') and all(c == 'villagers familyprobe status' for c in commands[1:]),
          f'{phase}: only read-only watch/status commands recorded')
check('was slain by Zombie' in (OUT / 'launch.log').read_text(), 'Native server log identifies zombie as Juniper killer')
start = read('before-growth/report.json')
check(next(c['crop'] for c in start['crops'] if c['position'] == [-230,72,751])['Properties']['age'] == '5',
      'Observed ripe wheat grew naturally from baseline age five')
check(len(start['water']) == 6 and all(not c['obstructionsAboveTo100'] for c in start['crops']),
      'Saved baseline has water and clear overhead columns, without native light claim')
setup = read('setup.json')
with runtime_lock(Path(setup['originalRuntime'])), runtime_lock(ROOT / 'runs/farm-regrowth/play'):
    check(parallel.tree_files(Path(setup['originalRuntime']) / 'saves/VillageTrial') == setup['originalWorldFiles'],
          'All 278 source world files unchanged')
    check(not any((ROOT/'runs/farm-regrowth/play'/n).exists() for n in
          ['lab-command.txt','lab-command.processing.txt','saves/VillageTrial/villagers/work-sites.json.active']),
          'Stopped fixture has no pending command or dirty construction marker')
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:
    check(hashlib.sha256(p.read_bytes()).hexdigest() == expected, 'Preserved 0.4.0: '+str(p))
summary = dict(verifiedUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(), checksPassed=len(checks), checks=checks,
               earnedCropMeals=2, newHarvests=0, naturallyMatureWheat=1, newZombieDeaths=1,
               sustainedFoodOrDefenseAcceptance=False, nativeMeanTickMillis=[final['meanTickMillis'],reopen['meanTickMillis']],
               sourceChanged=False, noDeployment=True,
               nextWork='Inspect actual home/refuge route failure before choosing a defense fix; continue from retained death history.')
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
print({k:v for k,v in summary.items() if k!='checks'})
