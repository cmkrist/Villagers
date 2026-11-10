# Authored food request, real meal and hungry forestry suspension

This historical checkpoint binds275tests. Later [forestry-access work](../forestry-access/README.md)
uses this same fed history to verify actual wood income and paid planting.
Current source pins differ; recorded food-transfer evidence is unchanged.

Native verification: Juniper requested food from Dara,Dara handed over one actual
bread,and Juniper ate it. Food rose6→10 after metabolism while Dara's bread3→2.
Nutrition/stock/history survived restart. A second fixture's hungry forester
retained actual seed1 and the exact unfinished grant,with zero forestry planning.
Current275Java+10Python pass; verification.json records41evidence assertions.
See [rules](../../../design/food-help.md).

## Source and inputs

Source0.4.1-dev remains unreleased. FoodHelp adds a bounded visible-neighbor goal;
Speech.requestFood reuses existing phrase decoding,willingness and real handoff.
No peer inventory lookup chooses a candidate. Actual gifts are counted/remembered
only after inventory transfer. Foresters yield at food<=6; food-work eligibility
and metabolism remain unchanged. Last status-only correction prevents a recovered
villager from displaying the old hungry forestry status.

runs/food-help/play was an exact244-file copy of stopped runs/home-route/play.
Only read-only familyprobe watch/status commands were issued. No food,actor
movement,terrain,clock,health,hunger/fatigue,age,RNG or dialogue/transfer action
setter. Existing historical food/tool/station/nursery fixture inputs remain; this
is a distribution test,not renewable production or the user's default world.
The original source world remained unchanged throughout both native runs.

## Native evidence

50239exit0,1200ticks:cutoff49997/final50000. Exact early report proves Juniper food6,
no usable ration,requests0/received0,and Dara bread3. At cutoff,Juniper requests1/
received1,Dara given1/bread2,Juniper food10/health20/no held bread. Every actor
breadCraftedThisLoad=0. Matching memories record the actual bread receipt,the
request to that same neighbor,and eating the owned bread. Donor records payment.
This was an authored request,not an operator-invoked chat or transfer.

58593exit0,400tick continuation:cutoff50401/final50405. Juniper food10,Dara food19/
bread2,and the single transfer memories persist. All session given/received
counters0; no further handoff during this run. Request cooldowns are session-local;
this is not proof of a persisted request cooldown. Same4original survivors+
existing child,7old deaths,0missing throughout. No new child for the watched pair;
the existing child remains present. Native mean2.960275/3.9071ms for five actors
are snapshots,not scale/tail-latency acceptance.

23918exit0,400ticks:cutoff52710/final52713. A separate257-file copy of stopped
runs/forestry-refresh/play at runs/food-help-forestry-guard/play,again read-only
watch and no added simulation inputs. Source world unchanged. Juniper stays
food3/health20,seed1. The exact sites-before/after bytes match: same grant
891a2b06-ccd6-4889-9b38-5d85398d87e2,seedTaken=true,quota2,uncleared[]. Forestry
status waiting for food,batches0,logs/leaves/plant0. No neighbor in visible home
range was found; this test does not claim a rescue of an isolated forester.
Mean3.68211ms/five actors is a short snapshot.

Each source-before pin is archived: source-local-only.zip for the first successful
run; source-main.zip for final status/persistence/guard. Prior forestry273source
pins remain historical and legitimately differ. Build-final.log and build-status.log
pass275Java+10Python;build.log covers the earlier same-count pre-instrumentation
cohort. Baseline Metabolism,MealPolicy and BreedingPolicy bytes are unchanged.
No new schema,model job,worker,timer,service or deployment.

## Limits and custody

Native proof covers an authored understood request,real transfer/meal,persistence,
and urgent hunger suspending a real forestry grant. A successful planned approach
before contact was not independently captured; do not infer it from zero path
rejections. Foreign-language/refusal/native full-capacity cases retain pure/source
coverage and need separate native acceptance. Long-term donor reserves,isolated
residents,adequate crop yield and sustainable replenishment remain open.

Source food-help tuples905219/14667248,905320/14667305,905464/14667802;
persistence907100/14693038,907202/14693096,907326/14693431;
guard908159/14698394,908260/14698452,908407/14698961. All exact identities terminal,
no pending commands. Original frozen3identities remain exact;historical adaptive3
remain absent,no cause/replacement inferred. No original client/service touched.
No active resource hold or inference/hash grant;005/006/007 evidence untouched.

Source and installed0.4.0 JAR SHA remains
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
Current dev JAR/source archive hashes are in verification.json. No deployment.
Next: actual safe log-access diagnosis,renewable farm output and fuller food aid,
with real materials and all player protections retained.
