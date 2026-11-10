#!/usr/bin/env python3
"""Freeze the built mod and mapped compile classes for isolated native acceptance tests."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--server-home',type=Path,required=True,help='Prepared Fabric server with an already accepted EULA')
    parser.add_argument('--artifact-runtime',type=Path,required=True,help='Fresh canonical directory under project/runs')
    args=parser.parse_args()
    root=Path(__file__).resolve().parent.parent
    server=args.server_home.resolve();target=args.artifact_runtime.absolute()
    if target.resolve()!=target or not target.is_relative_to(root/'runs') or target.exists():
        raise ValueError('Artifact must be a fresh canonical directory under project/runs')
    if not re.search(r'eula\s*=\s*true',(server/'eula.txt').read_text()):
        raise ValueError('Prepared server must already have its owner-accepted EULA')
    version=json.loads((root/'build/resources/main/fabric.mod.json').read_text())['version']
    if not re.fullmatch(r'[A-Za-z0-9_.+-]+',version):raise ValueError('Invalid built mod version')
    mod=root/'build/libs'/f'villagers-{version}.jar'
    if not mod.is_file():raise ValueError('Run tools/test.sh first; built mod is missing')
    target.mkdir(parents=True);(target/'mods').mkdir()
    for name in ['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar','libraries','versions']:
        (target/name).symlink_to((server/name).resolve(strict=True))
    shutil.copy2(server/'eula.txt',target)
    apis=list((server/'mods').glob('fabric-api-*.jar'))
    if len(apis)!=1:raise ValueError('Prepared server must contain exactly one pinned Fabric API')
    shutil.copy2(apis[0],target/'mods');shutil.copy2(mod,target/'mods')
    for name,source in [('main-classes','build/classes/java/main'),('main-resources','build/resources/main')]:shutil.copytree(root/source,target/name)
    classes=list((target/'main-classes').rglob('*.class'))
    if not classes:raise ValueError('No compiled classes; retained incomplete artifact')
    with zipfile.ZipFile(target/'mods'/mod.name)as jar:
        for path in classes:
            if jar.read(path.relative_to(target/'main-classes').as_posix())!=path.read_bytes():
                raise ValueError('Build/JAR class mismatch; retained incomplete artifact')
    pins={str(p.relative_to(target)):hashlib.sha256(p.read_bytes()).hexdigest()for p in(target/'mods').glob('*.jar')}
    (target/'artifact-pins.json').write_text(json.dumps(pins,indent=2)+'\n')
    print(f'Frozen {len(classes)} verified main classes and native mod at {target}')


if __name__=='__main__':main()
