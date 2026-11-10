"""Verify actual interior refuge paths, bounded retry work, and separate retained histories."""
from pathlib import Path
import datetime, hashlib, importlib.util, json, sys, zipfile
ROOT=Path(__file__).resolve().parents[4]
OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'tools'))
from runtime_lock import runtime_lock
spec=importlib.util.spec_from_file_location('parallel',ROOT/'tools/prepare-parallel.py')
parallel=importlib.util.module_from_spec(spec);spec.loader.exec_module(parallel)
checks=[]
def read(name):return json.loads((OUT/name).read_text())
def actor(d,prefix='231e'):return next(a for a in d['actors'] if a['uuid'].startswith(prefix))
def check(ok,note):
    if not ok:raise AssertionError(note)
    checks.append(note)
baseline=read('../refuge-diagnosis/results.json')
initial,night,backoff,final=[read(n/'results.json') for n in [Path('.'),Path('persistence'),Path('backoff'),Path('final-acceptance')]]
probe=read('../refuge-diagnosis/idle-inspection.json')
check(probe['collisionFree'] and not probe['homePath']['canReach'] and probe['homePath']['nodes']==15,
      'Native collision-free start has a real unreachable 15-node bed path')
check(probe['homePath']['target']=='-269, 76, 733' and probe['homePath']['positions'][-1]=='-269, 75, 731',
      'Actual bed target is above saved bed and last node lies outside north wall')
check(actor(baseline)['sleeping'] and actor(baseline)['survival']['lastRefugePlan']['inside'],
      'Diagnostic-only baseline also shelters; no causal survival improvement inferred')
for label,r in [('initial',initial),('final-source',final)]:
    a=actor(r);s=a['survival'];route=s['lastFloorRoute']
    check(s['floorRoutes']>0 and route['pathCanReach'] and 1<=route['pathNodes']<=128,
          label+': actual complete floor route assigned under native navigation')
    check(route['stand']==route['pathEnd'] and route['bed']=='-269, 75, 733',label+': exact standing goal reached by native path for unchanged home')
    check(a['sleeping'] and s['intent']=='SHELTER' and s['lastRefugePlan']['inside'] and s['lastRefugePlan']['covered'],
          label+': actual arrival/sleep/coverage independently follows path assignment')
    check(s['routeStalls']==0 and s['partialHomeRoutes']==0,label+': no refuge stall or partial-route relaxation')
check(actor(initial)['survival']['lastFloorRoute']['pathNodes']==34 and
      actor(initial)['survival']['lastFloorRoute']['stand']=='-270, 75, 734','Retained first native 34-node interior route')
check(actor(night)['sleeping'] and actor(night)['health']>0 and night['nativeGameTime']>66151,
      'Comparator retains living sleeping Juniper after former separate-branch death tick')
n0,n1=actor(night,'e358'),actor(backoff,'e358')
check(n0['position']==n1['position'] and n0['survival']['lastRefugePlan']['pathNodes']==n1['survival']['lastRefugePlan']['pathNodes']==1,
      'Retry-cost observations concern the same unmoved stuck position and one-node bed path')
s0,s1=n0['survival'],n1['survival']
check(s1['refugePathQueries']*s0['homePathPlans']<s0['refugePathQueries']*s1['homePathPlans'],
      'Final backoff reduces native home-query work per plan; not a wall-time speedup claim')
check(s1['refugeStandInspections']==150 and s1['homePathPlans']==10 and s1['refugePathQueries']==14,
      'Native backoff case: 150 candidate checks and 14 queries across 10 plans')
population={'original_cohort_loaded_alive':4,'original_cohort_known_dead_or_converted':7,
            'original_cohort_unloaded_or_missing':0,'local_loaded_villager_count':5}
phases=[(Path('../refuge-diagnosis'),baseline,'source-diagnostic.zip'),(Path('.'),initial,'source-main.zip'),
        (Path('persistence'),night,'../source-main.zip'),(Path('backoff'),backoff,'../source-backoff.zip'),
        (Path('final-acceptance'),final,'../source-backoff.zip')]
for phase,r,archive in phases:
    check(read(phase/'terminal.json')['exitCode']==r['exitCode']==0,str(phase)+': direct original handle exited zero')
    check(r['population']==population,str(phase)+': comparator cohort/history retained')
    check(r['javaTests']=={'tests':286,'failures':0,'errors':0,'skipped':0} and r['pythonTests']==10,
          str(phase)+': recorded 286 Java/10 Python pass')
    check(r['sourceUnchangedDuringRun'] and r['originalWorldUnchanged'],str(phase)+': source and parent unchanged during run')
    pins=read(phase/'source-before.json')['files']
    with zipfile.ZipFile(OUT/phase/archive) as z:
        check(all(hashlib.sha256(z.read(n)).hexdigest()==h for n,h in pins.items()),str(phase)+': exact source archive binding')
    if phase==Path('backoff'):
        check(not r['ownedTerminal'] and read('backoff/custody-gap.json')['noRelaunchToRepairMissingCensus'],
              'Short backoff census gap explicit; no fabricated exact tuples or vacuous terminal claim')
    else:
        check(len(r['ownedTerminal'])==3 and all(not x['sameIdentityAlive'] for x in r['ownedTerminal']),
              str(phase)+': captured exact three-job chain terminal')
    if phase!=Path('../refuge-diagnosis'):
        check(all(a['survival']['refugePathQueries']<=3*a['survival']['homePathPlans'] and
                  a['survival']['refugeStandInspections']<=75*a['survival']['homePathPlans'] for a in r['actors']),
              str(phase)+': actual home-query and candidate bounds retained')
check(read('../refuge-diagnosis/setup.json')['originalWorldFiles']==read('setup.json')['originalWorldFiles']==
      read('final-acceptance/setup.json')['originalWorldFiles'],'All independent comparisons start from identical 278 saved files')
pins=read('final-acceptance/source-before.json')['files']
check(all(hashlib.sha256((ROOT/n).read_bytes()).hexdigest()==h for n,h in pins.items()),'Current main source matches final positive native cohort')
with zipfile.ZipFile(OUT/'source-backoff.zip') as finalZip,zipfile.ZipFile(OUT.parent/'refuge-diagnosis/source-diagnostic.zip') as oldZip:
    for name in ['src/main/java/local/villagers/survival/SurvivalPolicy.java','src/main/java/local/villagers/TraderRecruitment.java']:
        check(finalZip.read(name)==oldZip.read(name),'Unchanged partial-return or recruitment authority: '+name)
retained=read('retained-death-world.json');parent=read('setup.json')
with runtime_lock(Path(retained['runtime'])),runtime_lock(Path(parent['originalRuntime'])):
    check(parallel.tree_files(Path(retained['runtime'])/'saves/VillageTrial')==retained['files'],
          'All 286 retained-death world files remain unchanged; no resurrection/merge')
    check(parallel.tree_files(Path(parent['originalRuntime'])/'saves/VillageTrial')==parent['originalWorldFiles'],
          'All 278 pre-death source files remain unchanged')
for runtime in ['runs/refuge-diagnosis/play','runs/refuge-floor/play','runs/refuge-final/play']:
    with runtime_lock(ROOT/runtime):
        check(not any((ROOT/runtime/n).exists() for n in ['lab-command.txt','lab-command.processing.txt','saves/VillageTrial/villagers/work-sites.json.active']),
              runtime+': stopped and no pending command/dirty construction marker')
expected='4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0'
for p in [ROOT/'build/libs/villagers-0.4.0.jar',Path('/home/cmkrist/Games/VanillaPerfectedCompanion/mods/villagers-0.4.0.jar')]:
    check(hashlib.sha256(p.read_bytes()).hexdigest()==expected,'Preserved deployed 0.4.0: '+str(p))
summary=dict(verifiedUTC=datetime.datetime.now(datetime.timezone.utc).isoformat(),checksPassed=len(checks),checks=checks,
             actualFloorGoalArrival=True,finalSourcePositiveNativeVerified=True,partialPolicyUnchanged=True,
             retainedDeathHistoryUnchanged=True,generalDefenseOrSurvivalRateProven=False,
             nativeMeanTickMillis=[r['meanTickMillis'] for _,r,_ in phases],
             shortBackoffExactProcessCensusMissing=True,
             finalSourceArchiveSHA256=hashlib.sha256((OUT/'source-backoff.zip').read_bytes()).hexdigest(),
             devJarSHA256=hashlib.sha256((ROOT/'build/libs/villagers-0.4.1-dev.jar').read_bytes()).hexdigest(),noDeployment=True)
(OUT/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
print({k:v for k,v in summary.items() if k!='checks'})
