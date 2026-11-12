#!/usr/bin/env python3
"""Bounded isolated native language fixture; never opens an existing society world."""
import hashlib
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import zipfile


def main():
    root=Path(__file__).resolve().parent.parent
    parser=argparse.ArgumentParser();parser.add_argument('--runtime-name',default='language-partnerships-server')
    args=parser.parse_args()
    if not __import__('re').fullmatch(r'language-[a-z0-9-]+',args.runtime_name):raise ValueError('Use an isolated language runtime name')
    runtime=root/'runs'/args.runtime_name
    if runtime.exists():
        raise RuntimeError('Refusing to replace a retained fixture')
    server=root/'runs/recruitment-server'
    if 'eula=true' not in (server/'eula.txt').read_text():
        raise RuntimeError('Prepared server must have its owner-accepted EULA')
    java=Path(os.environ['JAVA_HOME'])/'bin'
    runtime.mkdir()
    for name in ['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar','libraries','versions']:
        (runtime/name).symlink_to(server/name)
    shutil.copy2(server/'eula.txt',runtime)
    (runtime/'mods').mkdir();(runtime/'config').mkdir()
    for jar in (server/'mods').glob('fabric-api-*.jar'):shutil.copy2(jar,runtime/'mods'/jar.name)
    shutil.copy2(root/'build/libs/villagers-0.4.1-dev.jar',runtime/'mods')
    (runtime/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25622\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\ndifficulty=peaceful\npause-when-empty-seconds=-1\n')
    (runtime/'config/villagers.json').write_text(json.dumps({'baseExhaustionPerDay':0,'godChildren':False,'breeding':False,'maxResidentChunks':16,'speechCooldownSeconds':5}))
    shutil.copytree(root/'build/classes/java/main',runtime/'main-classes');shutil.copytree(root/'build/resources/main',runtime/'main-resources')
    cp=(root/'build/probe-classpath.txt').read_text().replace(str(root/'build/classes/java/main'),str(runtime/'main-classes')).replace(str(root/'build/resources/main'),str(runtime/'main-resources'))
    source=root/'tools/tests/java/local/villagers/PartnershipServerProbe.java'
    shutil.copy2(source,runtime/source.name)
    classes=runtime/'probe-classes';classes.mkdir()
    subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),str(runtime/source.name)],check=True)
    with zipfile.ZipFile(runtime/'mods/language-probe.jar','w') as z:
        z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'language-probe','version':'1','environment':'server','entrypoints':{'main':['local.villagers.PartnershipServerProbe']},'depends':{'villagers':'*'}}))
        for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
    with zipfile.ZipFile(runtime/'source.zip','w') as z:
        for base in ['src/main/java','src/test/java']:
            for f in (root/base).rglob('*.java'):z.write(f,f.relative_to(root))
        z.write(source,source.relative_to(root))
    (runtime/'artifact-pins.json').write_text(json.dumps({str(f.relative_to(runtime)):hashlib.sha256(f.read_bytes()).hexdigest() for f in (runtime/'mods').glob('*.jar')},indent=2))
    for phase,report,status in [('first','partnership-result.json','passed'),('frozen','partnership-frozen-result.json','frozen-passed'),('family','partnership-family-result.json','family-passed'),('restart','partnership-restart-result.json','restart-passed'),('separated','partnership-separated-result.json','separated-passed')]:
        config=json.loads((runtime/'config/villagers.json').read_text());config['breeding']=phase=='family';(runtime/'config/villagers.json').write_text(json.dumps(config))
        with (runtime/f'probe-{phase}.log').open('w') as log:
            p=subprocess.Popen([str(java/'java'),'-Xmx2G',*(['-Dvillagers.learning=frozen'] if phase=='frozen' else []),'-jar','fabric-server-launch.jar','nogui'],cwd=runtime,stdout=log,stderr=subprocess.STDOUT)
            stat=Path(f'/proc/{p.pid}/stat').read_text().rsplit(')',1)[1].split()
            (runtime/f'custody-{phase}.json').write_text(json.dumps({'pid':p.pid,'startTicks':stat[19],'cwd':str(runtime),'launchedAtEpoch':time.time()},indent=2))
            print(f'Owned language fixture {phase} PID{p.pid}; {runtime}',flush=True)
            try:code=p.wait(timeout=100)
            except subprocess.TimeoutExpired:
                p.terminate();p.wait(timeout=30);raise RuntimeError('Bounded owned language fixture timed out')
        (runtime/f'terminal-{phase}.json').write_text(json.dumps({'exitCode':code,'finishedAtEpoch':time.time()},indent=2))
        result=runtime/'world'/report
        if code or not result.exists() or json.loads(result.read_text())['status']!=status:
            raise RuntimeError(f'Language fixture {phase} failed; retained {runtime}')
        print(f'LANGUAGE {phase} PASS: {result}',flush=True)


if __name__=='__main__':main()
