"""Exact stopped construction-world continuation copy; no entity or terrain edits."""
from pathlib import Path
import datetime
import hashlib
import importlib.util
import json
import shutil
import sys

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT / 'tools'))
from runtime_lock import runtime_lock
spec = importlib.util.spec_from_file_location('parallel', ROOT / 'tools/prepare-parallel.py')
parallel = importlib.util.module_from_spec(spec)
spec.loader.exec_module(parallel)
source = ROOT / 'runs/construction-sites/play'
target = ROOT / 'runs/station-production/play'
out = Path(__file__).resolve().parent
assert not target.exists(), 'Never overwrite/reset an existing fixture'
with runtime_lock(source), parallel.stopped_world(source / 'saves/VillageTrial'):
    records = parallel.tree_files(source / 'saves/VillageTrial')
    target.mkdir(parents=True)
    for name in ['config', 'defaultconfigs', 'mods', 'resourcepacks']:
        if (source / name).exists():
            shutil.copytree(source / name, target / name, symlinks=True)
    for name in ['options.txt', 'pack-provenance.json']:
        if (source / name).exists(): shutil.copy2(source / name, target / name)
    shutil.copytree(source / 'saves/VillageTrial', target / 'saves/VillageTrial',
                    ignore=shutil.ignore_patterns('session.lock'))
    assert parallel.tree_files(target / 'saves/VillageTrial') == records
    manifest = json.loads((source / 'experiment.json').read_text())
    manifest.update({'runtime': str(target), 'source_snapshot': str(source / 'saves/VillageTrial'),
        'source_world_sha256': parallel.tree_digest(records), 'source_world_files': records,
        'mutable_data_shared': False, 'notes': ['LABELLED one-log/one-seed handoff and daylight input; no production or actor/terrain setters. Prior fixture inputs retained.']})
    (target / 'experiment.json').write_text(json.dumps(manifest, indent=2) + '\n')
    setup = {'createdUTC': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'kind': 'CONTROLLED COPY; material handoff, not autonomous resource acquisition',
        'originalRuntime': str(source), 'fixtureRuntime': str(target), 'originalWorldFiles': records,
        'originalWorldSHA256': parallel.tree_digest(records), 'changedWorldFiles': [],
        'requestedObservationTicks': 600,
        'plannedOperatorInputs': ['time set day', 'observer-only teleport within handoff reach',
            '2 oak logs and 1 wheat seed to Dara through ordinary item handoff', 'withdraw prior operator table to observer; finite station approval and same-state revocation probe; regrant with explicit protection', 'read-only family watch'],
        'actorTerrainUntouchedDuringPreparation': True}
    (out / 'setup.json').write_text(json.dumps(setup, indent=2) + '\n')
    files = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in sorted((ROOT / 'src/main').rglob('*')) if p.is_file()}
    files['build.gradle'] = hashlib.sha256((ROOT / 'build.gradle').read_bytes()).hexdigest()
    (out / 'source-before.json').write_text(json.dumps({'files': files}, indent=2) + '\n')
    assert parallel.tree_files(source / 'saves/VillageTrial') == records
print('Exact world copy:', len(records), 'files; no source world/actor/terrain writes.')
