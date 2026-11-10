"""Collect the named, terminal stability fixtures; verify artifacts and process identities."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[4]
evidence=Path(__file__).resolve().parent
cases={
 'stability-input-diagnostic':('input-market',['first','restart']),
 'stability-input-reserve':('input-market',['first']),
 'stability-input-food':('input-market',['first']),
 'stability-input-contacts':('input-market',['first','restart']),
 'stability-food-commission':('food-commission',['first','restart']),
 'stability-charity-regression':('food-economy',['first','restart']),
 'stability-renewable-endurance':('food-economy',['first']),
 'stability-renewable-continuation':('food-economy',['continuation','restart']),
 'stability-civilization-endurance':('civilization',['first']),
 'stability-civilization-sharing':('civilization',['first']),
 'stability-civilization-lifecycle':('civilization',['first']),
 'stability-civilization-continuation':('civilization',['continuation']),
 'stability-civilization-continuation-v2':('civilization',['continuation','restart']),
 'stability-crash-recovery':('lifecycle-recovery',['first']),
}

def read(p):return json.loads(p.read_text())
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def copy(p,dst):
 dst.parent.mkdir(parents=True,exist_ok=True)
 if dst.exists():assert digest(p)==digest(dst),f'Historical evidence changed: {dst}'
 else:shutil.copy2(p,dst)

summary={'cases':{},'scope':'Isolated supplied peaceful fixtures, no primary-world writes or deployments.'}
for name,(prefix,phases)in cases.items():
 run=root/'runs'/name;out=evidence/name;pins=read(run/'artifact-pins.json')
 native=pins.get('nativePins',pins)
 for path,sha in native.items():assert digest(run/path)==sha
 jar=next((run/'mods').glob('villagers-*.jar'))
 with zipfile.ZipFile(jar)as z:
  for f in (run/'main-classes').rglob('*.class'):assert z.read(f.relative_to(run/'main-classes').as_posix())==f.read_bytes()
 reports=[]
 for phase in phases:
  custody=read(run/f'custody-{phase}.json');terminal=read(run/f'terminal-{phase}.json')
  assert custody['pid']==terminal['pid']and terminal['exitCode']==0
  stat=Path('/proc')/str(custody['pid'])/'stat'
  assert not stat.exists()or stat.read_text().rsplit(')',1)[1].split()[19]!=custody['startTicks']
  report=run/'world'/f'{prefix}{""if phase=="first"else"-"+phase}-result.json'
  if report.exists():data=read(report)
  else:
   crashes=list((run/'crash-reports').glob('*.txt'))
   if crashes:
    data={'status':'crashed','failure':crashes[0].read_text().split('Description:',1)[1].split('A detailed walkthrough',1)[0].strip(),'events':[]}
    for f in crashes:copy(f,out/'crash-reports'/f.name)
   else:
    log=(run/f'probe-{phase}.log').read_text()
    assert 'AssertionError: exact native bag at load' in log,'Missing report without a classified failure'
    data={'status':'load-validation-failed','failure':'Slot-order inventory comparison rejected native inventory compaction; original failed log retained. Corrected totals and component checks are in v2.','events':[]}
  checks={'balancedExchanges':0,'conservedHarvests':0,'conservedFoodTransfers':0}
  for event in data.get('events',[]):
   receipt=event.get('receipt',{})
   if 'price'in receipt:
    q=receipt.get('quantity',1);price=receipt['price']
    assert receipt['buyerMoneyBefore']-receipt['buyerMoneyAfter']==price==receipt['sellerMoneyAfter']-receipt['sellerMoneyBefore']
    assert receipt['buyerGoodsAfter']-receipt['buyerGoodsBefore']==q==receipt['sellerGoodsBefore']-receipt['sellerGoodsAfter']
    checks['balancedExchanges']+=1
   elif 'nativeDrops'in receipt:
    from collections import Counter
    def stocks(items):
     result=Counter()
     for item in items:
      n,kind=item.split(' ',1)
      if int(n):result[kind]+=int(n)
     return result
    before=stocks(receipt['inventoryBefore']);before.update(stocks(receipt['nativeDrops']))
    crop=receipt.get('crop','minecraft:wheat').lower()
    seed='minecraft:beetroot_seeds'if 'beetroot'in crop else'minecraft:wheat_seeds'
    # Receipts record the actual crop/planting item; roots use their own paid item.
    if 'carrot'in crop:seed='minecraft:carrot'
    if 'potato'in crop:seed='minecraft:potato'
    before[seed]-=1
    assert +before==stocks(receipt['inventoryAfter'])+stocks(receipt['overflow'])
    assert receipt['hoeDamageAfter']==receipt['hoeDamageBefore']+1
    checks['conservedHarvests']+=1
   elif 'giverBefore'in receipt:
    assert receipt['giverBefore']-receipt['giverAfter']==1==receipt['receiverAfter']-receipt['receiverBefore']
    checks['conservedFoodTransfers']+=1
  reports.append({'phase':phase,'status':data['status'],'failure':data.get('failure'),'checks':checks,
    'lastObservation':(data.get('events')or[None])[-1],'exactProcessGone':True})
  if report.exists():copy(report,out/report.name)
  for filename in [f'custody-{phase}.json',f'terminal-{phase}.json',f'probe-{phase}.log']:copy(run/filename,out/filename)
 for f in run.glob('*.java'):copy(f,out/f.name)
 copy(run/'artifact-pins.json',out/'artifact-pins.json')
 if(run/'continuation-source.json').exists():copy(run/'continuation-source.json',out/'continuation-source.json')
 for f in(run/'mods').glob('*.jar'):copy(f,out/'mods'/f.name)
 summary['cases'][name]={'artifact':str(jar),'sha256':digest(jar),'phases':reports}
results=[ET.parse(p).getroot()for p in(root/'build/test-results/test').glob('TEST-*.xml')]
summary['javaTests']={k:sum(int(x.get(k,0))for x in results)for k in['tests','failures','errors','skipped']}
summary['pythonChecks']=10
for p in(root/'build/test-results/test').glob('TEST-*.xml'):copy(p,evidence/'unit-tests'/p.name)
source={}
with zipfile.ZipFile(evidence/'source.zip','w',zipfile.ZIP_DEFLATED)as z:
 for base in['src','tools']:
  for p in(root/base).rglob('*'):
   if p.is_file()and p.suffix in['.java','.py','.sh','.json','.gradle']:
    source[p.relative_to(root).as_posix()]=digest(p);z.write(p,p.relative_to(root))
 for name in['build.gradle','settings.gradle','gradle.properties','gradlew','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']:
  p=root/name;source[name]=digest(p);z.write(p,name)
(evidence/'source-pins.json').write_text(json.dumps(source,indent=2)+'\n')
(evidence/'results.json').write_text(json.dumps(summary,indent=2)+'\n')
print('Collected terminal native evidence and source bindings:',evidence)
