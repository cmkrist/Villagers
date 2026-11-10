# Quiet rest recovery — unreleased0.4.1-dev

Juniper recovered from fatigue1.0 to0.4334375 through five actual quiet-ground rest
bouts,then retained that recovery across restart and completed one further bout
to0.325. No new food,tools,terrain,clock,actor movement,hunger,fatigue,age or action
inputs were applied. Juniper's home route is still unresolved; this is recovery
evidence,not arrival at home or access to the forest.

## Why this preceded forestry

The stopped station-production world was surveyed read-only. The bounded region
contains119near-home log columns,all mangrove,within Juniper's home range and none
within the other adults' inspected ranges.64nearby hanging propagules include
mature seed material. [Forestry boundary](../../../design/forestry.md),raw chunks
and survey reports under `../forestry/terrain-survey/` preserve that evidence.
Block type does not prove ownership or native reachability.

Juniper stood at(-234.206902,73,766.7),home(-269,75,733),fatigue1 and repeatedly
failed REST. The old rest code allowed ground rest only when home was invalid;
a valid unreachable bed caused endless failed travel. Refuge planning's NORMAL
intent also bypassed its40tick retry deadline. The new behavior restores existing
ground rest after a failed/occupied bed route,requires real80tick quiet intervals
and bounds retries. It does not move a resident or widen ordinary home territory.

## Native inputs and results

`prepare-copy.py` copied the stopped station-production fixture exactly (221files)
into `runs/rest-recovery/play`. Source world stayed byte-identical. All previous
fixture supplies/interventions remain labelled historical background. The ONLY
queued command was read-only family observation selecting Juniper/Dara; later
status requests are also read-only. No method invoked rest or chose an action.

Handle8238/exit0:1200ticks43963→45163/final save45169. Native fatigue0.4334375,
groundRests5,bedRests0,restPathRejections2. Survival plans/homePathPlans30,
partialHomeRoutes0. Body position unchanged,health20,inventoryempty,homeunchanged.
The intermediate `recovery-observed.json` already showed five bouts at45062.

Read-only reopen87520/exit0:400ticks45170→45570/final save45576. The initial watch
report at45185 had no loaded actors; it is not a fatigue observation. The first
loaded Juniper report at45207 shows fatigue0.43375 and groundRests0,matching the
persisted state before further rest. The report filename `birth-transition` is
the existing observer's state-change label,not evidence of a new birth. End state
fatigue0.325,groundRests1,bedRests0,restPathRejections1,plans/homePathPlans10,
partialHomeRoutes0. Same home/body position. All5live villagers,7historical deaths,
0missing original UUIDs retained; the earlier ordinary child remains alive.

Mean native tick4.566835ms/5.026459ms at20TPS/five actors are snapshots,not scale or
tail-latency proof. Refuge planning honors its deadline; the observed30/10plans
match the1200/400tick windows. No actual partial route was taken,so returning home
and the positive partial-path branch remain unverified natively. The pure guards
are tested. Juniper has still not demonstrated a route to the mangrove resources.

## Source and validation

RestProgress requires80consecutive quiet/safe ticks. Walking,unsafe footing,another
work gap,location changes,clock rollback and replay cannot earn immediate credit.
Existing0.12fatigue recovery and normal accumulation rates are unchanged. Ground
checks use actual onGround/body/fluid/fire/air and damaging footing states. Bed
ownership/sight/native sleep stay enforced. WorkJourney gives finite stall/deadline/
backoff; paths share existing work admission. Survival remains above elective rest.

Calm partial returns require an already-displaced resident,2..128native nodes,
at least1block closer endpoint,loaded nodes and a finite detour bound. Ordinary
home range stays28. Existing danger path selection is preserved. No schema,IO,
thread,external timer or terrain permission added. [Rules](../../../design/rest-recovery.md).

Build59885exit0:258Java+10Python tests,zero failures/errors/skips. Seven new tests
cover consecutive rest,movement,unsafe interruption,work gaps,bed changes,rollback,
replay/invalid observations and finite return-path guards. Native source pins and
`source-main.zip` retained. `verify.py` asserts fatigue,counters,unchanged positions/
home,planning bounds,persistence,population,source221-file manifest and artifact
preservation. Source/deployed0.4.0 hash is unchanged;0.4.1-dev remains unreleased.

## Process custody and limits

First chain865756/14026938,865893/14027006,866152/14027647 was sampled live with
exact metadata and is terminal. The short reopen ended before its late custody
sample; jobs[] supplies no PID. No PID/exception was inferred. Its handle exit0,
normal save/Animatica drain,free runtime lock and absence of a tagged live client
establish terminal status. No repeat run was launched merely to obtain tuples.

Historical adaptive601856/601946/602326 remain absent,no cause or replacement
inferred. Frozen601857/601990/602372 retain their exact identities in latest results.
No other client/world/service was stopped,paused,restarted or changed. No owned
active transient job,automated build/release timer,new worker,hold,model grant or
inherited exception. Failed read-only report selection initially picked an older/
empty observation; no simulation changed. The verifier now uses the first loaded
Juniper observation after the prior final save.

Next: inspect native route topology around Juniper and the owned bed,then implement
finite protected mangrove resource/propagule jobs with actual loot and planting.
Do not substitute supplied logs,teleports or a wider home range for that access.
