#!/usr/bin/env python3
"""New controlled culture fixture and syntax-only cohort sweep, using a retained, pinned language build."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import zipfile


def main():
    root=Path(__file__).resolve().parent.parent
    parser=argparse.ArgumentParser()
    parser.add_argument('--runtime-name',default='language-culture-server')
    parser.add_argument('--baseline-runtime',default='language-teaching-v3')
    args=parser.parse_args()
    for name in [args.runtime_name,args.baseline_runtime]:
        if not re.fullmatch(r'language-[a-z0-9-]+',name):raise ValueError('Use isolated language runtime names')
    runtime=root/'runs'/args.runtime_name
    if runtime.exists():raise RuntimeError('Refusing to replace a retained experiment')
    baseline=root/'runs'/args.baseline_runtime
    pins=json.loads((baseline/'artifact-pins.json').read_text())
    for name,expected in pins.items():
        if hashlib.sha256((baseline/name).read_bytes()).hexdigest()!=expected:raise RuntimeError('Baseline artifact pin changed')
    if 'eula=true' not in (baseline/'eula.txt').read_text():raise RuntimeError('Baseline must have accepted EULA')
    java=Path(os.environ['JAVA_HOME'])/'bin'
    runtime.mkdir()
    for name in ['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar','libraries','versions']:
        (runtime/name).symlink_to((baseline/name).resolve())
    shutil.copy2(baseline/'eula.txt',runtime)
    (runtime/'mods').mkdir();(runtime/'config').mkdir()
    for name in ['fabric-api-0.162.0+26.3.jar','villagers-0.4.1-dev.jar']:shutil.copy2(baseline/'mods'/name,runtime/'mods'/name)
    shutil.copytree(baseline/'main-classes',runtime/'main-classes');shutil.copytree(baseline/'main-resources',runtime/'main-resources')
    (runtime/'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25627\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\ndifficulty=peaceful\npause-when-empty-seconds=-1\n')
    (runtime/'config/villagers.json').write_text(json.dumps({'baseExhaustionPerDay':0,'godChildren':False,'breeding':False,'maxResidentChunks':16,'speechCooldownSeconds':5}))
    cp=(root/'build/probe-classpath.txt').read_text().replace(str(root/'build/classes/java/main'),str(runtime/'main-classes')).replace(str(root/'build/resources/main'),str(runtime/'main-resources'))
    classes=runtime/'probe-classes';classes.mkdir()
    sources=[root/'tools/tests/java/local/villagers'/name for name in ['CultureTrialSweep.java','CultureServerProbe.java']]
    for source in sources:shutil.copy2(source,runtime/source.name)
    subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),*[str(runtime/s.name) for s in sources]],check=True)
    with zipfile.ZipFile(runtime/'mods/language-probe.jar','w') as z:
        z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'language-probe','version':'1','environment':'server','entrypoints':{'main':['local.villagers.CultureServerProbe']},'depends':{'villagers':'*'}}))
        for f in classes.rglob('*.class'):z.write(f,f.relative_to(classes))
    with zipfile.ZipFile(baseline/'source.zip') as original,zipfile.ZipFile(runtime/'source.zip','w') as archive:
        for item in original.infolist():archive.writestr(item,original.read(item.filename))
        for source in sources:archive.write(source,source.relative_to(root))
        archive.write(Path(__file__),Path(__file__).relative_to(root))
    (runtime/'artifact-pins.json').write_text(json.dumps({str(f.relative_to(runtime)):hashlib.sha256(f.read_bytes()).hexdigest() for f in (runtime/'mods').glob('*.jar')},indent=2))
    (runtime/'baseline.json').write_text(json.dumps({'runtime':args.baseline_runtime,'productionJarSha256':pins['mods/villagers-0.4.1-dev.jar'],'sourceArchiveSha256':hashlib.sha256((baseline/'source.zip').read_bytes()).hexdigest(),'worldUsage':'Read-only compiled classes, source archive, libraries and mods. Baseline world is never opened.'},indent=2))
    result=runtime/'sweep-result.json'
    with (runtime/'sweep.log').open('w') as log:
        subprocess.run([str(java/'java'),'-cp',str(classes)+os.pathsep+cp,'local.villagers.CultureTrialSweep',str(result)],check=True,stdout=log,stderr=subprocess.STDOUT,timeout=90)
    summary=json.loads(result.read_text())
    if summary['status']!='passed':raise RuntimeError('Culture sweep failed; full results retained')
    print(f"CULTURE SWEEP PASS: {summary['cohorts']} cohorts, {summary['learnerEpisodes']} learner episodes",flush=True)
    for phase,report,status in [('first','culture-result.json','passed'),('restart','culture-restart-result.json','restart-passed'),('frozen','culture-frozen-result.json','frozen-passed')]:
        with (runtime/f'probe-{phase}.log').open('w') as log:
            p=subprocess.Popen([str(java/'java'),'-Xmx2G',*(['-Dvillagers.learning=frozen'] if phase=='frozen' else []),'-jar','fabric-server-launch.jar','nogui'],cwd=runtime,stdout=log,stderr=subprocess.STDOUT)
            stat=Path(f'/proc/{p.pid}/stat').read_text().rsplit(')',1)[1].split()
            (runtime/f'custody-{phase}.json').write_text(json.dumps({'pid':p.pid,'startTicks':stat[19],'cwd':str(runtime),'launchedAtEpoch':time.time()},indent=2))
            print(f'Owned culture fixture {phase} PID{p.pid}; {runtime}',flush=True)
            try:code=p.wait(timeout=100)
            except subprocess.TimeoutExpired:
                p.terminate();p.wait(timeout=30);raise RuntimeError('Owned bounded culture fixture timed out')
        (runtime/f'terminal-{phase}.json').write_text(json.dumps({'exitCode':code,'finishedAtEpoch':time.time()},indent=2))
        result=runtime/'world'/report
        if code or not result.exists() or json.loads(result.read_text())['status']!=status:raise RuntimeError(f'Culture {phase} failed; retained {runtime}')
        print(f'CULTURE {phase} PASS: {result}',flush=True)


if __name__=='__main__':main()
