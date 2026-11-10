#!/usr/bin/env python3
"""Test actual hoe wear, granted wood gathering, replacement and resumed harvest in a fresh isolated server.

Uses a frozen previously tested artifact runtime, never the changing shared build.
The explicitly supplied fixture is not a renewable-forestry or general survival claim.
"""
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


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifact-runtime',type=Path,required=True)
    parser.add_argument('--runtime',type=Path,required=True)
    parser.add_argument('--classpath-file',type=Path,required=True)
    parser.add_argument('--full-storage',action='store_true',help='Separate native no-space denial fixture; no replacement or restart phase')
    parser.add_argument('--partial-restart',action='store_true',help='Save after tool replacement, resume outstanding log/planting, then verify a second restart')
    parser.add_argument('--local-trade',action='store_true',help='Run the separate finite-currency local trade integration probe')
    parser.add_argument('--market-production',action='store_true',help='Run request-driven manufacturing, farming and returned-currency integration')
    parser.add_argument('--food-bundle',choices=['carrot','beetroot'],help='Separate ordinary-AI bulk food payment/meal fixture')
    parser.add_argument('--food-commission',action='store_true',help='Isolate a real paid request to a gift-willing producer')
    parser.add_argument('--food-sharing',action='store_true',help='Run the existing explicit charitable request/production/delivery probe against a frozen artifact')
    parser.add_argument('--input-market',action='store_true',help='Observe repeated paid raw inputs, customer tool orders and income-funded meals')
    parser.add_argument('--maker-rations',type=int,choices=[3,4],default=4,help='Declared initial maker bread in the input-market fixture')
    parser.add_argument('--civilization',action='store_true',help='Four-founder two-day peaceful village endurance and restart')
    parser.add_argument('--server-port',type=int,default=25582,help='Loopback-only port for this isolated fixture')
    parser.add_argument('--civilization-continue-from',type=Path,help='Copy only a stopped civilization fixture that exhausted observation without another failure')
    args=parser.parse_args()
    if args.civilization_continue_from and not args.civilization:parser.error('--civilization-continue-from requires --civilization')
    if not 1024<=args.server_port<=65535:parser.error('--server-port must be1024..65535')
    if args.civilization and any([args.input_market,args.food_sharing,args.food_commission,args.food_bundle,args.local_trade,args.market_production,args.full_storage,args.partial_restart]):parser.error('Choose one fixture')
    if args.maker_rations!=4 and not args.input_market:parser.error('--maker-rations requires --input-market')
    if args.full_storage and args.partial_restart:parser.error('Choose one fixture variant')
    if (args.local_trade or args.market_production or args.food_bundle) and (args.full_storage or args.partial_restart):parser.error('Market tests are separate fixtures')
    if args.local_trade and args.market_production:parser.error('Choose one market fixture')
    if args.food_bundle and (args.local_trade or args.market_production):parser.error('Choose one market fixture')
    if args.food_commission and (args.food_bundle or args.local_trade or args.market_production or args.full_storage or args.partial_restart):parser.error('Choose one fixture')
    if args.food_sharing and (args.food_commission or args.food_bundle or args.local_trade or args.market_production or args.full_storage or args.partial_restart):parser.error('Choose one fixture')
    if args.input_market and (args.food_sharing or args.food_commission or args.food_bundle or args.local_trade or args.market_production or args.full_storage or args.partial_restart):parser.error('Choose one fixture')
    project=Path(__file__).resolve().parent.parent
    source=args.artifact_runtime.absolute(); runtime=args.runtime.absolute()
    for path in (source,runtime):
        if path.resolve()!=path or not path.is_relative_to(project/'runs'):
            raise ValueError('Use canonical isolated runtimes below project/runs')
    if runtime.exists(): raise ValueError('Runtime must be fresh; saved fixtures are never replaced')
    pins=json.loads((source/'artifact-pins.json').read_text())
    if not all(digest(source/name)==value for name,value in pins.items()):
        raise ValueError('Frozen source artifact no longer matches its pins')
    if not re.search(r'eula\s*=\s*true',(source/'eula.txt').read_text()):
        raise ValueError('Source must have an already owner-accepted EULA')
    java=Path(os.environ['JAVA_HOME'])/'bin'
    runtime.mkdir(parents=True); (runtime/'mods').mkdir(); (runtime/'config').mkdir()
    for name in ['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar','libraries','versions']:
        (runtime/name).symlink_to((source/name).resolve())
    shutil.copy2(source/'eula.txt',runtime)
    for jar in (source/'mods').glob('*.jar'):
        if jar.name.startswith(('fabric-api-','villagers-')): shutil.copy2(jar,runtime/'mods'/jar.name)
    (runtime/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={args.server_port}\nonline-mode=false\nlevel-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\ndifficulty=peaceful\npause-when-empty-seconds=-1\n')
    (runtime/'config/villagers.json').write_text(json.dumps({'baseExhaustionPerDay':400 if args.local_trade or args.market_production or args.food_bundle or args.food_commission or args.food_sharing or args.input_market else 80,'godChildren':False,'breeding':args.civilization,'maxResidentChunks':16}))
    if args.civilization_continue_from:
        import fcntl
        parent=args.civilization_continue_from.resolve()
        if not parent.is_relative_to(project/'runs'):raise ValueError('Continuation parent must be an isolated runtime under runs')
        previous=json.loads((parent/'world/civilization-result.json').read_text())
        if previous.get('failure')!='java.lang.AssertionError: civilization endurance budget exhausted':raise ValueError('Only observation exhaustion can continue; never erase deaths or other failures')
        with (parent/'world/session.lock').open('rb') as lock:
            fcntl.lockf(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
            shutil.copytree(parent/'world',runtime/'world')
        shutil.copy2(parent/'config/villagers.json',runtime/'config/villagers.json')
        (runtime/'continuation-source.json').write_text(json.dumps({'source':str(parent),'reportSHA256':digest(parent/'world/civilization-result.json')},indent=2)+'\n')
    for name in ['main-classes','main-resources']:shutil.copytree(source/name,runtime/name)
    class_count=0
    with zipfile.ZipFile(next((runtime/'mods').glob('villagers-*.jar'))) as native:
        for entry in (runtime/'main-classes').rglob('*.class'):
            if native.read(entry.relative_to(runtime/'main-classes').as_posix())!=entry.read_bytes():
                raise ValueError('Detached compile classes must exactly match the frozen native mod')
            class_count+=1
    cp=args.classpath_file.read_text()
    for original,replacement in [(project/'build/classes/java/main',runtime/'main-classes'),(project/'build/resources/main',runtime/'main-resources')]:
        cp=cp.replace(str(original),str(replacement))
    classes=runtime/'probe-classes';classes.mkdir()
    probe_name='CivilizationServerProbe' if args.civilization else 'InputMarketServerProbe' if args.input_market else 'FoodEconomyServerProbe' if args.food_sharing else 'FoodCommissionServerProbe' if args.food_commission else 'FoodBundleServerProbe' if args.food_bundle else 'MarketProductionServerProbe' if args.market_production else 'LocalTradeServerProbe' if args.local_trade else 'ToolRenewalServerProbe'
    probe=project/f'tools/tests/java/local/villagers/{probe_name}.java'
    shutil.copy2(probe,runtime/probe.name)
    compiled=subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),str(runtime/probe.name)])
    if compiled.returncode:raise RuntimeError(f'Probe compilation failed; unstarted fixture preserved at {runtime}')
    with zipfile.ZipFile(runtime/'mods/tool-renewal-probe.jar','w') as archive:
        archive.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'tool-renewal-probe','version':'1','environment':'server','entrypoints':{'main':['local.villagers.'+probe_name]},'depends':{'villagers':'*'}}))
        for entry in classes.rglob('*.class'):archive.write(entry,entry.relative_to(classes))
    bindings={'frozenArtifactRuntime':str(source),'sourcePins':pins,'fixtureSourceSHA256':digest(runtime/probe.name),'nativeClassBindingsChecked':class_count,
              'nativePins':{str(p.relative_to(runtime)):digest(p) for p in (runtime/'mods').glob('*.jar')},
              'scope':'Declared supplied native fixture. No primary worlds, current shared source writes, recipe forcing or post-setup resident/terrain setters.'}
    (runtime/'artifact-pins.json').write_text(json.dumps(bindings,indent=2)+'\n')
    phases=[('first','tool-renewal-result.json','passed','tool-renewal-result.json')]
    if not args.full_storage:phases.append(('restart','tool-renewal-restart-result.json','restart-passed','tool-renewal-result.json'))
    if args.partial_restart:
        phases=[('first','tool-renewal-result.json','partial-passed','tool-renewal-result.json'),
                ('resume','tool-renewal-resume-result.json','passed','tool-renewal-result.json'),
                ('restart','tool-renewal-restart-result.json','restart-passed','tool-renewal-resume-result.json')]
    if args.local_trade or args.market_production:phases=[('first','local-trade-result.json','passed','local-trade-result.json'),('restart','local-trade-restart-result.json','restart-passed','local-trade-result.json')]
    if args.food_bundle:phases=[('first','food-bundle-result.json','passed','food-bundle-result.json'),('restart','food-bundle-restart-result.json','restart-passed','food-bundle-result.json')]
    if args.food_commission:phases=[('first','food-commission-result.json','passed','food-commission-result.json'),('restart','food-commission-restart-result.json','restart-passed','food-commission-result.json')]
    if args.food_sharing:phases=[('first','food-economy-result.json','passed','food-economy-result.json'),('restart','food-economy-restart-result.json','restart-passed','food-economy-result.json')]
    if args.input_market:phases=[('first','input-market-result.json','passed','input-market-result.json'),('restart','input-market-restart-result.json','restart-passed','input-market-result.json')]
    if args.civilization:phases=[('first','civilization-result.json','passed','civilization-result.json'),('restart','civilization-restart-result.json','restart-passed','civilization-result.json')]
    if args.civilization_continue_from:phases=[('continuation','civilization-continuation-result.json','passed','civilization-result.json'),('restart','civilization-restart-result.json','restart-passed','civilization-continuation-result.json')]
    for phase,report_name,status,restart_from in phases:
        with (runtime/f'probe-{phase}.log').open('w') as output:
            process=subprocess.Popen([str(java/'java'),'-Xmx2G','-Dvillagers.civilization.restartFrom='+restart_from,'-Dvillagers.civilization.continue='+str(bool(args.civilization_continue_from) and phase!='restart').lower(),'-Dvillagers.input.makerRations='+str(args.maker_rations),'-Dvillagers.bundle.kind='+str(args.food_bundle or 'carrot'),'-Dvillagers.toolRenewal.fullStorage='+str(args.full_storage).lower(),'-Dvillagers.toolRenewal.partialRestart='+str(args.partial_restart).lower(),'-Dvillagers.toolRenewal.restartFrom='+restart_from,'-jar','fabric-server-launch.jar','nogui'],cwd=runtime,stdout=output,stderr=subprocess.STDOUT)
            stat=Path(f'/proc/{process.pid}/stat').read_text().rsplit(')',1)[1].split()
            (runtime/f'custody-{phase}.json').write_text(json.dumps({'pid':process.pid,'startTicks':stat[19],'cwd':str(runtime),'launchedAtEpoch':time.time()},indent=2)+'\n')
            print(f'Tool renewal {phase}: PID {process.pid}, runtime {runtime}',flush=True)
            try:result=process.wait(timeout=1300 if args.civilization else 300 if args.input_market else 240 if args.market_production else 210 if args.local_trade else 160)
            except subprocess.TimeoutExpired:
                process.terminate();result=process.wait(timeout=30)
                (runtime/f'terminal-{phase}.json').write_text(json.dumps({'pid':process.pid,'exitCode':result,'timedOut':True,'finishedAtEpoch':time.time()},indent=2)+'\n')
                raise RuntimeError(f'Owned fixture timed out; retained at {runtime}')
        (runtime/f'terminal-{phase}.json').write_text(json.dumps({'pid':process.pid,'exitCode':result,'finishedAtEpoch':time.time()},indent=2)+'\n')
        report=runtime/'world'/report_name
        if result or not report.exists() or json.loads(report.read_text())['status']!=status:
            raise RuntimeError(f'Native tool renewal failed; see {runtime/f"probe-{phase}.log"}')
        print(f'TOOL RENEWAL {phase} PASS: {report}',flush=True)


if __name__=='__main__':main()
