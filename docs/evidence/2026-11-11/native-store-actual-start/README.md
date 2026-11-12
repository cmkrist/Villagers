# Actual native store route starting footing — candidate under test

Older Willow frozen copy at340454: actual feet(-439.2,64.5,-822.7); first pathnode(-440,65,-823) has nativeFloor64, hypothetical centered body collides, next stair node body clear/supported. Ordinary route passes, critical guard fails. This is an inaccurate predicted starting body, not an unsafe later footing. Native route dry and within settlement.

Candidate uses actual body only at index0 with same X/Z and nodeY==ceil(actualFeetY), still requiring native collision-free body and support. Every later footing, bounded route, descent, fluid, real contact and meal conservation guard remains. Regression test covers rounded partial-height start, integer start, later index, other cell/height, NaN.636tests PASS.

Source `/tmp/villagers-worker-v324-store-start-source`; pin `/tmp/villagers-worker-v324-store-start-pinned.jar`, SHAfc2f45d3f13af1b1fd5cbaaf95cb6c0696704e27662016c72932692968dd9e59. Parent exactv324 + verified mine2files + night shelter3files. Only2additional files. The exact NativeFoodStorage starting-footing hunk and its test are now applied to shared root. Not deployed. Shared-root build/test passes 721 tests, zero failures/errors.

Frozen proof PASS: same dry3step/4step store routes now pass both ordinary and critical guards; unreachable target remains rejected. All192 preserved/source unchanged. Frozen native proof `worker-willow-actual-start-frozen-20261111`. All-resident24ktick care pending `worker-v324-store-start-care-v2-20261111`, immutable staged main source (native clock328120; written handoff326749 was stale) `worker-v327-frozen-source-20261111/NativeFourRegions`; full542file hashes matched source before/after copying. No live main use, no save promotion.

Initial care setup failed before launching a server because staged world lacked its companion config/mods directories. Those runtime inputs were copied and hashed; original incomplete runtime retained; fresh v2 runtime used. This is not gameplay failure or a survival result.

The staged-main care v2 run starts at328120, confirmed by native-illager-initial.json; it stopped after1tick on pre-existing Dara-f13a1HP/0food/timer60,emptybag. This is not a healthy baseline or a test of longer route progress. Main had advanced since written HANDOFF. Use known healthy stopped v323copy334748 for the next comparison; never roll main back.

Final healthy old-checkpoint comparison `worker-v324-store-start-healthy-20261111` sourcev323COPY334748 stopped2537ticks on Juniper-e3de1HP/0food at337285. All192original bodies retained/alive, no new/missing/dead IDs; source unchanged. No whole-day survival or extra mining/food-store transaction claim for this candidate. Frozen route correctness is the bounded proof; family bedding priority is also present in v324 parent, so do not claim a strict2file behavior comparison against the older v322 night run. All owned native processes are terminal.
