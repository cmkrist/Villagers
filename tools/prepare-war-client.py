#!/usr/bin/env python3
"""Stage a separate war client from verified pack assets, without copying any existing world."""
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import argparse

from runtime_lock import runtime_lock
from runtime_config import ROOT


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--native-regions', action='store_true')
    args = parser.parse_args()
    world = 'NativeFourRegions' if args.native_regions else 'FourBiomesWar'
    kind = 'native-region' if args.native_regions else 'war'
    manifest_name = 'native-regions-client.json' if args.native_regions else 'war-client.json'
    runtime = ROOT / ('runs/native-four-regions-play' if args.native_regions else 'runs/four-biomes-play')
    source = ROOT / 'run'
    if runtime.exists():
        manifest = runtime / manifest_name
        if not manifest.is_file() or json.loads(manifest.read_text()).get('world') != world:
            raise ValueError(f'Refusing to alter an unrecognized existing runtime: {runtime}')
        print(f'Existing {kind} client preserved: {runtime}')
        return
    spec = importlib.util.spec_from_file_location('parallel_preparation', ROOT / 'tools/prepare-parallel.py')
    preparation = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(preparation)
    with runtime_lock(source):
        assets, provenance = preparation.verified_assets(source)
        settings = preparation.settings_files(source)
        runtime.parent.mkdir(parents=True, exist_ok=True)
        staging = Path(tempfile.mkdtemp(prefix='.war-prepare-', dir=runtime.parent))
        try:
            preparation.copy_files(source, staging, assets | settings)
            # The pack provenance is inherited, but no saves, accounts, sessions or server lists are copied.
            (staging / 'pack-provenance.json').write_text(json.dumps(provenance, indent=2)+'\n')
            config = staging / 'config/villagers.json'
            values = json.loads(config.read_text()) if config.exists() else {}
            values.update({'godChildren':False})
            config.parent.mkdir(parents=True,exist_ok=True)
            config.write_text(json.dumps(values,indent=2)+'\n')
            (staging / manifest_name).write_text(json.dumps({'schema':1,'world':world,
                'role':'play','requested_tps':20,'existing_worlds_copied':False},indent=2)+'\n')
            staging.rename(runtime)
        except BaseException:
            shutil.rmtree(staging)
            raise
    print(f'Prepared separate {kind} client: {runtime}')


if __name__ == '__main__':
    main()
