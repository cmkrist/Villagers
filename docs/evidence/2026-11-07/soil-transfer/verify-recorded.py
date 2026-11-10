"""Verify retained native outcomes and source bindings; no game, build or world writes."""
from pathlib import Path
import hashlib
import json
import zipfile

OUT = Path(__file__).resolve().parent
FERN = '5ad3aad5-3e4b-4eb6-8158-87731868dc03'
JOB = 'e817f6cf-921a-487f-8a22-625b650b34c8'
checks = 0

def check(value, why):
    global checks
    assert value, why
    checks += 1

def read(case, name):
    return json.loads((case / name).read_text())

def actor(case):
    return next(a for a in read(case, 'budget-native.json')['actors'] if a['uuid'] == FERN)

def blocks(case, directory):
    return {tuple(b['position']): b['state']['Name'] for b in read(case, directory + '/report.json')['blocks']}

for case, archive, count in [(OUT, 'source-main.zip', 3), (OUT/'approved', 'source-approved.zip', 2),
                             (OUT/'reopen', 'source-approved.zip', 2)]:
    result = read(case, 'results.json')
    check(result['exitCode'] == 0 and result['originalWorldUnchanged'], 'terminal/original world')
    check(result['sourceUnchangedDuringRun'], 'source changed during native run')
    check(result['population']['local_loaded_villager_count'] == count, 'actual population')
    check(result['population']['original_cohort_unloaded_or_missing'] == 0, 'missing actors')
    check(result['javaTests'] == dict(tests=296, failures=0, errors=0, skipped=0), 'Java checks')
    check(result['pythonTests'] == 10, 'Python checks')
    check(0 < result['meanTickMillis'] < 50, 'bounded trial tick sample')
    check(len(result['ownedTerminal']) == 3 and not any(x['sameIdentityAlive'] for x in result['ownedTerminal']), 'exact terminal custody')
    with zipfile.ZipFile(OUT/archive) as z:
        for name, sha in read(case, 'source-before.json')['files'].items():
            check(hashlib.sha256(z.read(name)).hexdigest() == sha, 'source archive mismatch: ' + name)

protected = actor(OUT)['soilWork']
check(protected['gathered'] == protected['delivered'] == protected['pathQueries'] == 0, 'protected job ran')
sites = read(OUT, 'sites-after.json')
check(len(sites['soilJobs']) == len(sites['protections']) == 1, 'protected authority')
check(sites['soilJobs'][0]['id'] == JOB and not sites['soilJobs'][0]['gathered'], 'protected stage')
b = blocks(OUT, 'protected-blocks')
check(b[-247,65,713] == 'minecraft:grass_block' and b[-232,71,753] == 'minecraft:sand', 'protected terrain')

paid = actor(OUT/'approved')
s = paid['soilWork']; receipt = s['lastReceipt']
check(s['gathered'] == s['delivered'] == 1 and paid['plotsCultivatedThisLoad'] == 1, 'native paid stages')
check(receipt['job'] == JOB and receipt['stage'] == 'delivery', 'exact native job')
check(receipt['nativeDrops'] == ['1 minecraft:sand'], 'actual displaced loot')
check('1 minecraft:dirt' in receipt['inventoryBefore'] and '1 minecraft:dirt' not in receipt['inventoryAfter'], 'real dirt payment')
check('1 minecraft:sand' in receipt['inventoryAfter'], 'retained displaced sand')
check('22 minecraft:wheat_seeds' in receipt['inventoryAfter'] and '21 minecraft:wheat_seeds' in paid['inventory'], 'real planting payment')
for case in [OUT/'approved', OUT/'reopen']:
    b = blocks(case, 'saved-blocks')
    check(b[-247,65,713] == 'minecraft:air', 'source replayed')
    check(b[-232,71,753] == 'minecraft:farmland' and b[-232,72,753] == 'minecraft:wheat', 'paid crop lost')
    sites = read(case, 'sites-after.json')
    check(not sites['soilJobs'] and not sites['farms'] and not sites['protections'], 'authority resurrected')
    lifecycle = read(case, 'census.json')['known_actor_lifecycle']
    for uuid, tick in [('231e4c00-19df-4be5-a5ba-5ebc6d29e27f',66151),
                       ('589b708d-b768-4a61-a59e-910db084b079',81487),
                       ('c7c31152-34a1-4edd-bd84-e48f50f86088',98739)]:
        check(lifecycle[uuid]['terminal_event'] == 'death' and lifecycle[uuid]['terminal_game_time'] == tick, 'actual death lost')
reopened = actor(OUT/'reopen')
check(sorted(x for x in reopened['inventory'] if x != '0 minecraft:air') ==
      sorted(x for x in paid['inventory'] if x != '0 minecraft:air'), 'material replay on reopen')
check(reopened['soilWork']['gathered'] == reopened['soilWork']['delivered'] == reopened['plotsCultivatedThisLoad'] == 0, 'work replay on reopen')
print(f'{checks} recorded outcome/source-binding checks passed; three actual deaths retained.')
