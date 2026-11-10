"""Observe the actual stopped crash checkpoint with the fixed artifact, in a fresh copy."""
from pathlib import Path
import fcntl,hashlib,json,os,shutil,subprocess,time,zipfile
root=Path(__file__).resolve().parents[4]
source=root/'runs/stability-civilization-endurance'
runtime=root/'runs/stability-crash-recovery'
artifact=root/'runs/stability-artifact-lifecycle'
assert not runtime.exists()
assert json.loads((source/'terminal-first.json').read_text())['exitCode']==0
with(source/'world/session.lock').open('rb')as lock:
 fcntl.lockf(lock,fcntl.LOCK_SH|fcntl.LOCK_NB)
 shutil.copytree(source/'world',runtime/'world')
for name in['fabric-server-launch.jar','fabric-server-launcher.properties','server.jar','libraries','versions']:(runtime/name).symlink_to((artifact/name).resolve())
for name in['main-classes','main-resources','mods']:shutil.copytree(artifact/name,runtime/name)
shutil.copy2(artifact/'eula.txt',runtime)
shutil.copytree(source/'config',runtime/'config')
shutil.copy2(source/'server.properties',runtime/'server.properties')
p=runtime/'server.properties';p.write_text(p.read_text().replace('server-port=25582','server-port=25583'))
probe=root/'tools/tests/java/local/villagers/LifecycleRecoveryServerProbe.java';shutil.copy2(probe,runtime/probe.name)
cp=Path('/tmp/villagers-probe-classpath').read_text().replace(str(root/'build/classes/java/main'),str(runtime/'main-classes')).replace(str(root/'build/resources/main'),str(runtime/'main-resources'))
java=Path(os.environ['JAVA_HOME'])/'bin';classes=runtime/'probe-classes';classes.mkdir()
subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),str(runtime/probe.name)],check=True)
with zipfile.ZipFile(runtime/'mods/recovery-probe.jar','w')as z:
 z.writestr('fabric.mod.json',json.dumps({'schemaVersion':1,'id':'recovery-probe','version':'1','environment':'server','entrypoints':{'main':['local.villagers.LifecycleRecoveryServerProbe']},'depends':{'villagers':'*'}}))
 for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes))
pins={str(p.relative_to(runtime)):hashlib.sha256(p.read_bytes()).hexdigest()for p in(runtime/'mods').glob('*.jar')}
(runtime/'artifact-pins.json').write_text(json.dumps(pins,indent=2))
with(runtime/'probe-first.log').open('w')as output:
 process=subprocess.Popen([str(java/'java'),'-Xmx2G','-jar','fabric-server-launch.jar','nogui'],cwd=runtime,stdout=output,stderr=subprocess.STDOUT)
 stat=Path(f'/proc/{process.pid}/stat').read_text().rsplit(')',1)[1].split()
 (runtime/'custody-first.json').write_text(json.dumps({'pid':process.pid,'startTicks':stat[19],'cwd':str(runtime)},indent=2))
 print('Owned crash recovery PID',process.pid,flush=True)
 try:code=process.wait(timeout=60)
 except subprocess.TimeoutExpired:process.terminate();code=process.wait(timeout=30)
(runtime/'terminal-first.json').write_text(json.dumps({'pid':process.pid,'exitCode':code,'finishedAtEpoch':time.time()},indent=2))
report=runtime/'world/lifecycle-recovery-result.json'
assert code==0 and report.exists()and json.loads(report.read_text())['status']=='passed'
print('Actual crashed checkpoint recovery PASS:',report,flush=True)
