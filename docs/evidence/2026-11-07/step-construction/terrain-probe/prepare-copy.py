"""Exact copy of a stopped society fixture. Read-only observation; no new simulation inputs."""
from pathlib import Path
import datetime
import hashlib
import importlib.util
import json
import shutil
import sys

ROOT = Path('/home/niko/.internal/source/Villagers')
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock
spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec); spec.loader.exec_module(parallel)
source = ROOT / 'runs/step-staging-check/play'
target = ROOT / 'runs/step-terrain-probe/play'
out = Path(__file__).resolve().parent
assert not target.exists(), 'Never reset or overwrite an existing fixture'
with runtime_lock(source), parallel.stopped_world(source / 'saves/VillageTrial'):
    records = parallel.tree_files(source / 'saves/VillageTrial')
    target.mkdir(parents=True)
    for name in ['config', 'defaultconfigs', 'mods', 'resourcepacks']:
        if (source / name).exists(): shutil.copytree(source / name, target / name, symlinks=True)
    for name in ['options.txt', 'pack-provenance.json']:
        if (source / name).exists(): shutil.copy2(source / name, target / name)
    shutil.copytree(source / 'saves/VillageTrial', target / 'saves/VillageTrial', ignore=shutil.ignore_patterns('session.lock'))
    assert parallel.tree_files(target / 'saves/VillageTrial') == records
    manifest = json.loads((source / 'experiment.json').read_text())
    manifest.update({'runtime': str(target), 'source_snapshot': str(source / 'saves/VillageTrial'),
        'source_world_sha256': parallel.tree_digest(records), 'source_world_files': records, 'mutable_data_shared': False,
        'notes': ['Exact stopped-world copy. Read-only harvest/replant observation; NO new actor, food, tool, terrain, clock, RNG or fatigue setters. All earlier fixture interventions remain historical background.']})
    (target / 'experiment.json').write_text(json.dumps(manifest, indent=2) + '\n')
    setup = {'createdUTC': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'kind': 'CONTINUED TWO-ADULT HISTORY; READ-ONLY native builder approach query; NO moveTo/terrain/authority; ALL three deaths retained; no grants or new resources',
        'originalRuntime': str(source), 'fixtureRuntime': str(target), 'originalWorldFiles': records,
        'originalWorldSHA256': parallel.tree_digest(records), 'requestedObservationTicks': 800,
        'plannedOperatorInputs': ['read-only watch/status only; NO new grants, supplies, actor/body/growth/clock setters'],
        'changedWorldFiles': [], 'actorTerrainInventoryClockUnchangedDuringPreparation': True}
    (out / 'setup.json').write_text(json.dumps(setup, indent=2) + '\n')
    files = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in sorted((ROOT / 'src/main').rglob('*')) if p.is_file()}
    files['build.gradle'] = hashlib.sha256((ROOT / 'build.gradle').read_bytes()).hexdigest()
    (out / 'source-before.json').write_text(json.dumps({'files': files}, indent=2) + '\n')
    for actor in ['5ad3aad5-3e4b-4eb6-8158-87731868dc03', 'c7c31152-34a1-4edd-bd84-e48f50f86088']:
        shutil.copyfile(source / f'saves/VillageTrial/villagers/{actor}/mind.json', out / f'{actor}-mind-before.json')
    assert parallel.tree_files(source / 'saves/VillageTrial') == records
print('Copied', len(records), 'files with no simulation inputs.')
