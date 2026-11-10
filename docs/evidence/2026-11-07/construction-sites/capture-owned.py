from pathlib import Path
import os,json,time,sys
root=Path('/home/niko/.internal/source/Villagers');tag=sys.argv[1];out=Path(sys.argv[2]);procs={}
for p in Path('/proc').iterdir():
 if not p.name.isdigit():continue
 try:
  exe=os.readlink(p/'exe')
  if not exe.endswith('/bin/java'):continue
  raw=(p/'stat').read_text();r=raw[raw.rfind(')')+2:].split();args=(p/'cmdline').read_bytes().decode(errors='replace').split('\0')[:-1]
  procs[int(p.name)]={'pid':int(p.name),'ppid':int(r[1]),'startTicks':r[19],'uid':p.stat().st_uid,'exe':exe,'cwd':os.readlink(p/'cwd'),'cgroup':(p/'cgroup').read_text(),'argv':args,'checkEnvironment':('VILLAGERS_CHECK='+tag+'\0').encode() in (p/'environ').read_bytes()}
 except (OSError,ValueError):pass
owned={i for i,p in procs.items() if p['checkEnvironment'] and p['cwd']==str(root) and 'runClient' in p['argv'] and any(a.endswith('/gradle-wrapper.jar') for a in p['argv'])}
while True:
 add={i for i,p in procs.items() if p['ppid'] in owned}-owned
 if not add:break
 owned.update(add)
receipt={'atUTC':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'basis':'Exact launch environment tag, canonical source cwd and runClient wrapper argv; then observed descendants, not shared-scope attribution.','tag':tag,'jobs':[procs[i] for i in sorted(owned)]}
with out.open('x') as f:json.dump(receipt,f,indent=2);f.write('\n')
for p in receipt['jobs']:print({k:p[k] for k in ['pid','ppid','startTicks','uid','cwd']})
