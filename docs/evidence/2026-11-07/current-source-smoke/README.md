# Visible pre-crafting source smoke — November 7, 2026

Bounded isolated play-copy check, 07:35–07:38 UTC. This validates the loaded
pre-crafting artifact, not subsequent parent changes. No source/build/tool files
were edited by the smoke operator. Source hashes captured before launch are in
`source-sha256-before.json`; the exact 116 runtime class/resource hashes are in
`loaded-artifact-sha256.json`. They remained unchanged through reopening.

## Conditions and custody

Runtime `/home/niko/.internal/source/Villagers/runs/play`, role play, adaptive,
normal difficulty, VillageTrial seed 846206, requested/observed 20 TPS. Eleven
existing residents; no replacement villagers, god children, crop growth or terrain
fixtures injected. This inherited play save already contains earlier social-test
interventions; it is not an untouched survival baseline. Observer-only teleport
put the camera near residents for language hearing. The launch harness relocates
the observer to its aerial viewpoint on reopen.

Before first launch, the play launcher lock was available and no process targeted
its directory. The entire stopped save was copied to
`runs/archives/current-source-smoke-before-20261107T0735Z/VillageTrial`.
That directory name is a label; `custody.json` has the actual creation timestamp.
`processes-before.json` records sanitized identities. Original Modrinth 516527,
historical adaptive 602326/frozen 602372 and Buddy 679447 retained the same process
start ticks, executable and working directory after the check (`results.json`).
Their worlds, queues and services were not touched.

First launch `./tools/play.sh`, wrapper→daemon→client
683453→683556→683704, exec handle 75717. Reopen used
`./tools/play.sh -x compileJava -x processResources`, preserving the exact loaded
pre-crafting files while the parent worked independently in default build output.
Second chain 685930→686078→686231, handle 88680. Both consumed explicit `quit`,
logged integrated-server world/chunk saves, returned exit 0, and all six PIDs were
absent afterward. No command batch remained and launcher lock was available.
Terminal records are `launch-1-terminal.json` and `launch-2-terminal.json`.
No further JVM launch is needed or scheduled by this check.

## Passed with direct evidence

- Visible world rendered on both launches. Four screenshots were inspected using
  view_image: aerial `village-001-15s`, ground `current-source-ground`,
  `current-source-before-close`, and `reloaded-current-source-reloaded` PNGs.
  Ground captures show managed villager action/mood labels, native language and
  later the translated word “walk”. Reopen capture shows the village at dusk.
- Minecraft 26.3/Fabric 0.19.5/current Villagers loaded and managed villagers
  acted; no MixinApplyError, InjectionError or InvalidMixinException was logged.
  Required VillagerMixin/VillagerAttributesMixin startup and managed controller
  execution passed this smoke. This is not full pack/resource compatibility.
- All eleven prior schema-2 UUIDs saved as schema 6, retained name, personality,
  home and increasing learned-update counts. All eleven returned after reopening;
  saved language remained present (10/11 lexicons exactly equal at the first
  reopened report; the other continued natural hearing/word updates).
- Live food-state observation: initial report had food level 9 and saturation 5;
  later level remained 9 while saturation became 4 and exhaustion accumulated.
  Food state and age continued across reopening. This verifies active metabolism
  and persistence, not starvation, native food consumption or lethal death.
- Eight rendered speech lines were logged during the first approximately 100s
  session (including fixture gift thanks). Generated `vireka` rendered first as an
  unknown word and later as “walk”. The actual observer UUID
  6609fea5-c92a-359b-acb7-1ab63051aa33 saved seven glossary word records; all seven
  survived reopening. `vireka` hearing count increased 4→5 and the reloaded log
  showed Juniper saying translated “walk”. This validates world reconnect through
  normal leave/save/join, not rapid logout/login within one running server.
- Native positive harvest after the explicitly labelled `villagers testharvest`
  play-only intervention. It supplied exactly one iron hoe and one wheat seed
  through the real handoff to existing Fern-5ad3 (full UUID in native reports).
  Initial probe correctly reported pending travel, not success. Later
  `report-before-close.json` records cropsHarvested=2 and hoe wear 2/250;
  `census-first-final.json` records 2 wheat and 6 wheat seeds in real inventory.
  The immediate probe recorded original age7 and unchanged neighbors/farmland.
  Replanting is part of the native successful harvest operation, but no separate
  post-harvest crop age0 inspection/screenshot was obtained. Do not claim direct
  visual replant acceptance. Harvest counters are session-local and reset on close.
- Native tick query at 07:36:12 UTC reported target 20 TPS, average 4.2ms,
  P95 5.7ms/P99 7.8ms over 100 ticks. This short sample is not a scaling benchmark.

## Limitations and next bounded check

The command file runs server.createCommandSourceStack(), not the actual observer
identity/position. `villagers status` and `villagers language` therefore reported
no managed villager near spawn, despite the camera being close to actors. Both
helper and client allowlists also exclude top-level `/teach` and `/glossary`.
Even allowlisting glossary alone would still lack a player entity. Parent was
notified; correct command-source routing and safe player-command allowlisting
before using this channel as evidence for nearest-player interactions. Successful
native dispatch alone does not establish the intended player-context result.

Prepared resourcepacks are symlinks into immutable shared assets; Minecraft
explicitly ignored them as forbidden symlinks. Existing mod warnings, missing
translation errors and development auth-network errors were logged. Visible mod
startup passed; full pinned resourcepack parity did not. Fix runtime preparation
separately, preserving active comparison clients.

Training correctly refused to grant an acceleration budget to this historical
play copy: state legacy_untracked, no old restart marker, 20 TPS. No accelerated
experiment or live evidence-writer failure injection was attempted. Hunger death,
god children, sustained food consumption, mood behavioral causality, a full
farm→edible-food loop and a controlled broad conversation-volume benchmark remain
pending. Subsequent crafting source was not loaded or validated by this smoke.
Next use the closed play save (with its archived pre-test backup) for the parent's
small conserved wheat→bread slice and a direct post-harvest replant-state check.

`results.json` contains structured continuity, food, glossary, speech and preserved
process checks. First/reloaded reports, native logs, census, command receipts,
images/metadata and before/after mind directories retain raw supporting evidence.
