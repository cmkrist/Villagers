from pathlib import Path
from collections import Counter
import json,hashlib,shutil,zipfile,datetime,xml.etree.ElementTree as ET
root=Path.cwd(); evidence=root/'docs/evidence/2026-11-07/food-intent-native'
def load(p):return json.loads(p.read_text())
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def stocks(values):
 out=Counter()
 for x in values:
  n,item=x.split(' ',1)
  if int(n):out[item]+=int(n)
 return out
def collect(name,kind,status,group):
 p=root/'runs'/name;out=evidence/group/name;out.mkdir(parents=True,exist_ok=True)
 phases=['first','restart'] if status=='passed' else ['first'];reports=[];checks=Counter()
 pins=load(p/'artifact-pins.json')
 for file,sha in pins['nativePins'].items():assert digest(p/file)==sha
 with zipfile.ZipFile(next((p/'mods').glob('villagers-*.jar'))) as jar:
  classes=list((p/'main-classes').rglob('*.class'))
  for f in classes:assert jar.read(str(f.relative_to(p/'main-classes')))==f.read_bytes()
 assert len(classes)==pins['nativeClassBindingsChecked']
 for phase in phases:
  custody=load(p/f'custody-{phase}.json');terminal=load(p/f'terminal-{phase}.json');assert terminal['exitCode']==0 and terminal['pid']==custody['pid']
  proc=Path('/proc')/str(custody['pid'])/'stat'
  assert not proc.exists() or proc.read_text().rsplit(')',1)[1].split()[19]!=custody['startTicks']
  report=load(p/'world'/f'{kind}{"-restart" if phase=="restart" else ""}-result.json');assert report['status']==('restart-passed' if phase=='restart' else status)
  for event in report.get('events',[]):
   r=event.get('receipt')
   if not r:continue
   if 'price' in r:
    q=r.get('quantity',1);price=r['price']
    assert r['buyerMoneyBefore']-r['buyerMoneyAfter']==price==r['sellerMoneyAfter']-r['sellerMoneyBefore']
    assert r['buyerGoodsAfter']-r['buyerGoodsBefore']==q==r['sellerGoodsBefore']-r['sellerGoodsAfter']
    checks['balancedExchanges']+=1
   elif 'nativeDrops' in r:
    before=stocks(r['inventoryBefore']);drops=stocks(r['nativeDrops']);after=stocks(r['inventoryAfter']);overflow=stocks(r['overflow'])
    before.update(drops);before['minecraft:wheat_seeds']-=1
    assert +before==after+overflow and r['hoeDamageAfter']==r['hoeDamageBefore']+1
    checks['paidNativeHarvests']+=1
  reports.append({'phase':phase,'status':report['status'],'failure':report.get('failure'),'lastObservation':report.get('events',[None])[-1],'custody':custody,'terminal':terminal,'exactProcessGone':True})
 for f in p.iterdir():
  if f.is_file() and (f.suffix in ['.json','.java'] or f.name.startswith('probe-') and f.suffix=='.log'):shutil.copy2(f,out/f.name)
 for f in (p/'world').glob('*result.json'):shutil.copy2(f,out/f.name)
 shutil.copytree(p/'mods',out/'mods',dirs_exist_ok=True)
 if (p/'world/villagers').exists():shutil.copytree(p/'world/villagers',out/'villagers',dirs_exist_ok=True)
 return {'runtime':str(p),'nativeClassBindingsChecked':len(classes),'pins':pins,'checks':dict(checks),'phases':reports}
cases=[('food-commission-prior-source','food-commission','failed','retained-failure'),('food-commission-server','food-commission','passed','prior-source'),('food-intent-sharing-regression','food-economy','passed','prior-source'),('food-intent-production-regression','local-trade','failed','retained-failure'),('food-intent-production-regression-v2','local-trade','failed','retained-failure'),('food-commission-server-v3','food-commission','passed','accepted'),('food-intent-sharing-regression-v3','food-economy','passed','accepted'),('food-intent-production-regression-v3','local-trade','passed','accepted')]
results=[collect(*args) for args in cases]
pins=load(evidence/'source-pins-v3.json');current={str(f.relative_to(root/'src')):digest(f) for f in sorted((root/'src').rglob('*')) if f.is_file()}
xml=list((root/'runs/food-intent-build-v3/.build/test-results/test').glob('TEST-*.xml'));tests={k:sum(int(ET.parse(f).getroot().get(k,0)) for f in xml) for k in ['tests','failures','errors']}
assert tests=={'tests':376,'failures':0,'errors':0}
assert results[0]['pins']['fixtureSourceSHA256']==results[1]['pins']['fixtureSourceSHA256']==results[5]['pins']['fixtureSourceSHA256']
assert 'paid request alone cannot authorize' in results[0]['phases'][0]['failure']
paid=results[5]['phases'][0]['lastObservation'];assert paid['giftBoundaryChecked'] and paid['seller']['wheat']+3*paid['seller']['crafted']==6 and paid['seller']['money']==1 and paid['buyer']['money']==0
assert paid['seller']['giftsGiven']==paid['buyer']['giftsReceived']==0
sharing=results[6]['phases'][0]['lastObservation'];assert sharing['foodDeliveries']==1 and sharing['producer']['wheat']+3*sharing['producer']['crafted']==18
for file in ['tools/tool-renewal-server-test.py','tools/tests/java/local/villagers/FoodCommissionServerProbe.java','tools/tests/java/local/villagers/FoodEconomyServerProbe.java','tools/tests/java/local/villagers/MarketProductionServerProbe.java']:
 shutil.copy2(root/file,evidence/Path(file).name)
summary={'capturedUTC':datetime.datetime.now(datetime.timezone.utc).isoformat(),'status':'Prior source reproduces unauthorized paid-request gift; both intermediate late-food failures retained. Final source passes paid commission, explicit charity, productive circulation and all three clean restarts','finalSourceDrift':pins!=current,'finalSourceChangedPaths':sorted(k for k in pins.keys()|current.keys() if pins.get(k)!=current.get(k)),'javaTests':tests,'pythonChecks':10,'sameProbeSourceAcrossPriorAndCurrentPaidFixture':True,'runs':results}
(evidence/'results.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps({'sourceDrift':summary['finalSourceDrift'],'tests':tests,'runs':[(r['runtime'],r['checks']) for r in results]},indent=2))
