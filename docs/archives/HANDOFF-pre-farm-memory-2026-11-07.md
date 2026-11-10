# Villagers Project Lead — focused continuation

Updated 2026-11-07 after native soil acquisition, transport, paid replacement,
planting and reopen. Full earlier checkpoint: archives/HANDOFF-pre-soil-transfer-2026-11-07.md.
Read this and evidence/2026-11-07/soil-transfer/README.md; do not repeat old audits.

## Full continuing goal

Lead Villagers toward a performant Minecraft society that crafts, builds, has
children, protects its village, and develops interesting histories, language,
relationships and adaptation. Keep usage low; carry this full goal and a sharp
specific next task into fresh contexts. Source audit and existing-thread audit
are done. The broad goal remains ACTIVE; never mark it complete because one
bounded feature passed. No new workers/timers for resource coordination; finished
audit_domain, audit_tools_trials and visible_current_source stay finished.

## Current source and validation

Canonical /home/niko/.internal/source/Villagers. Most source/docs/tools untracked,
README modified. NEVER reset/clean. Source0.4.1-dev UNRELEASED. No commit, PR,
release, deployment or service action by Lead. Installed0.4.0 remains unchanged;
artifact hashes in soil-transfer/artifact-pins.json. MC26.3,Loader0.19.5,
FabricAPI0.162.0+26.3,JDK25.0.5+8,Gradle9.8.1. Mind schema7, work-sites schema6.
296Java+10Python PASS, build66017exit0 (soil-transfer/build-approved.log).
399recorded outcome/source-binding checks PASS via soil-transfer/verify-recorded.py.
Normal checks: tools/test.sh --no-daemon --max-workers=2. No unchanged suite rerun.
No source edits after that passing build; later changes were docs/evidence only.

User chose limited autonomous building with real materials and protection for
player structures. Do not ask again. Read design/terrain-permission.md and
soil-transfer.md. No blanket demolition, mining, chest looting or block-type
ownership guesses. Generic BREAK/PLACE remain disabled. No free items, restored
deaths or clock/weather/growth/body setters to make trials pass.

## Current primary history — TWO adults, three actual deaths retained

Continue runs/soil-transfer-reopen/play, stopped100878. Fern5ad3 and Nelle358 live.
Dara c7c3 starved98739 during the approved soil phase, entering it with food0,
health9. Juniper231e died to a zombie66151; offspring589b died of starvation81487
(already adult then). Preserve ALL three deaths. Original cohort2alive/9dead/0missing
excludes the additional descendant death. Loaded2. Stale lifecycle alive/health
fields and a child mind file do not override terminal events/actual loaded census.

Never promote older protected98067 (3live), dawn-food86062 (3live), farm-regrowth69646
(4live), or pre-death5live branches to erase deaths or restore stock. They are
read-only historical comparators. Default user runs/play remains a separate
unchanged history, last independently4originalalive/7dead/0missing/nochild33104.
Never merge same-UUID fixture histories into each other or into the user world.

## Latest implemented and native behavior

Arid ordinary-home ground had no bare dirt; real exposed soil exists just beyond
normal28block work bounds. Schema6 adds max4finite soil jobs reserving farm slots
(totalfarms+soil<=8). Explicit operator grant: /villagers site soil sx sy sz tx ty tz.
Both endpoints loaded/nearplayer, bare exposed source DIRT/GRASS, supported wet sand
target. Approval is exact-cell consent, not ownership inference. Source/support/
headroom+target roles watched; protection either end blocks all; outside standard
writes retire jobs; dirty shutdown revokes edit grants while keeping protection.
Raw chunk editors bypass hooks; no global save/crash atomicity claim.

SoilWork uses actual native dirt loot, bare hand, full inventory snapshot/CAS and
one-use receipt; destination consumes1heldDIRT and retains1nativeSAND. At40workticks
recheck body/authority/sight/reach/world/bag. Paid destination becomes ONE existing
finite farm-cell grant; normal real seed+hoe costs apply. No resource replay.
Explicit assigned trip: source<=64fromhome,target<=28, corridor8/max72 ONLY under
live1200tickclaim/healthyday/food>6. Ordinary28home and other path validators stay
unchanged. Complete loaded1..128node routes,3queries/batch(shared2batches/dim/sec),
75stands,40path/100scan/80stall/400backoff bounds. No partial resource routes.

Native job e817f6cf-921a-487f-8a22-625b650b34c8: source(-247,65,713),target(-232,71,753).
Protected phase37405EXIT0 saved98067: sourcegrass/targetsand unchanged,0soilqueries/
income. Only protection efde2bfa-96ae-4441-98fd-0546afcd3d3f was removed afterward.
Approved58481EXIT0 saved100473: Fern1gather/1deliver,18queries,paid1DIRT/retained1SAND,
22→21wheatseeds,1newplot,sourceAIR,targetmoistFARMLAND+WHEAT,grantsretired.
Reopen69857EXIT0 saved100878: sameblocks/materials,no new grants/replayedwork.
Native save compacts bag slots; compare material amounts, not identical slot order.
Sample mean ticks3.35/3.14/3.78ms, not a larger-population benchmark.
Source-main.zip binds protected cohort; source-approved.zip binds later safe-footing/
stale-trip reset/native-block-event improvements, approved+reopen. Both retained.

Current Fernhealth20/food16,21wheatseeds,7beetseeds,1sand,ironhoe re-equipped.
Nellhealth20/food20,1bread,still on clear low ledge with unusable1nodebedpath; she is
not clipped. Ninefarmcells now exist, but adequate output/distribution is unproven.
Operator assignment remains required; land discovery/general buildings unfinished.

## Sharp next task: actual food access and survival

Do source/saved-evidence diagnosis first, then one bounded meaningful improvement
and native continuation of the TWO-adult primary. Dara's starvation is permanent;
a historical pre-death comparator may diagnose behavior but never replace primary.
FoodHelp searches visible peers<=16 with ordinary home/path/reach/language/willingness
checks; Dara's last protected snapshot had0requests/no visible neighbour. FarmWorksite
travels only to its native remembered composter; Dara's(-251,75,736) differs from
productive farm(-226,72,752). These are leads, not a proven single cause. Do not
blindly widen all ranges or hand out bread. Consider bounded observed food-location
memory/communication, food access, larger earned yield and real-material paths for
Nell. Inspect existing code before choosing; no new worker needed.

Retain working native harvest/regrowth, true grain→bread/eating, voluntary food
gifts, rest/wake and family guards. Native clock differs from gameTime by94578;
NEVER use gameTime%24000. Use native activity REST for shelter; rain is not dusk.
Actualoffspringbirth36379 consumed3bread/12nutrition EACH parent, mutualconsent,
spareHOME,genes+27words,save/reopen; offspring is now dead. Natural sustainable
population growth, parent care, defense and buildings remain ongoing full goal.

## Resource custody and preserved clients

All new bounded chains terminal, exact live captures+closure in soil-transfer:
protected1041592/16125230,1041696/16125290,1041878/16125818;
approved1048066/16214971,1048182/16215031,1048339/16215558;
reopen1050536/16232942,1050637/16233002,1050791/16233527.
No active transient owned test/build/game/model job, pending command, automated
build/release timer or newworker. Original frozen daemon601990/10936325 and game
602372/10937054 remain exact/running untouched. Originaladaptive601856/10936249,
601946/10936303,602326/10937021 and frozenwrapper601857/10936249 absent externally;
no cause/replacement/ownership inferred. Preserve original clients/worlds,Buddy,
Niko,Modrinth. Shared scopes do not attribute unrelated Java. Newtuples inherit NO
exceptions.005closed/spent,006expiredunused,007earlyreleased09:54:54; no hold/hash/
unit/engine/successor/grant001/inference permission. Historical evidence immutable.
Head received terminal custody, no reply/review/new work requested:
<t3-thread://v2/d7fe5e09-2b47-40d7-9375-c3592cdc6d75/2a6b199a-0bbd-4884-887d-119d63e29b97>.
Read named threads before answering about them. No periodic resource polling.

For native launch, capture exactowned chain in SAME orchestration after10s with
/tmp/villagers-capture-owned.py TAG OUT (O_EXCL). Recover confirmed originalhandle;
NEVER relaunch merely for lost observation. Keep tests finite. No new permissions
for deployment/services are implied. Carry this FULL goal plus a sharper next-task
handoff on the next context change, and leave broad goal ACTIVE.
