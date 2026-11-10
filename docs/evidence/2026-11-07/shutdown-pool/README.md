# Pinned Animatica executor shutdown — reproduced and fixed

November 7, 2026. This investigation followed the post-main client watchdog in
[courtship evidence](../courtship-birth/README.md). It did not reopen or tick a
world, alter the surviving cohort, replace mod jars, or change user services.

## Findings and evidence boundary

A Python constant-string inventory of 37,969 classes in the 77 isolated mod JARs
and nested JARs found 21 candidate executor references. This was a bounded
candidate search, not complete Java call-flow analysis. See
[search scope and candidates](executor-string-candidates.json).

REA's connected tools were absent. Its installed CLI doctor reported unknown
launcher registration; no configuration or engine was changed. The CLI's archive
inspection succeeded: [complete REA Evidence](animatica-rea.json), ID
`ev_3e4513b0812701e028ced768e4ed7bac27505b2c52c7314f22f032b4562f17f3`,
subject SHA256 `c7df115a20895fe121009937bf0a1d3b8704ea6a5bfc38160f7e50552592d53c`.
That establishes artifact/member identity, not execution. Focused JDK25 `javap`
output is separate ordinary-shell evidence, not REA decompilation.

[AnimatedTexture bytecode](animatica-AnimatedTexture.javap.txt) shows the pinned
`0.6.2+26.3` class creates `EXECUTORS = Executors.newFixedThreadPool(4)` without a
custom factory. Native texture ticks submit pixel work to it. Its instance close
frees image resources but does not close the static executor. A class-constant
search found no other Animatica class referring to that field or a shutdown call.
This alone made a candidate; it did not prove historical pool-number ownership.

The new explicit play-only title diagnostic then observed the exact public
`AnimatedTexture.EXECUTORS` field. Before any factory probe, it already had four
workers and 492 completed tasks, with zero active/queued work. Its factory
allocated one unstarted NEW thread named `pool-4-thread-5`, non-daemon. The four
live `pool-4-thread-1..4` workers were non-daemon and WAITING. No task was submitted
to this executor by the probe. The pinned initializer only creates an empty pool;
its field read cannot explain those already existing workers/completed tasks.
[Runtime observation](baseline-executor.json) therefore identifies this run's
pool4 as Animatica's executor. The old courtship dump lacks this field observation;
it is strongly consistent with the same defect, rather than independent proof
of that older run's allocation provenance.

The inspected [baseline title image](baseline-title.png) establishes a real
rendered client with the selected pack. Normal stop was requested at09:19:01UTC.
The original post-main watchdog reproduced at09:19:17; handle50880 exit1,
game248. [Console](baseline-launch.log), [crash](crash-2026-11-07_04.19.17-client.txt).
The probe reported worldLoaded=false, and all154 saved-world files retained size
and mtime. No integrated world was started by the harness.

## Scoped fix and matching visible check

`compat.AnimaticaShutdown` registers a client stopping callback only when Fabric
reports Animatica version exactly `0.6.2+26.3`. It reads only the exact known static
field and checks its four-worker ThreadPoolExecutor layout. It calls shutdown
and waits at most two seconds for accepted work to drain before texture teardown.
It does not enumerate/kill workers, call shutdownNow, change daemon flags, close
other executors, force VM exit, or run when absent/another version. The selected
Fabric lifecycle bytecode places CLIENT_STOPPING in exitWorldAndClose after its
stop log: [local lifecycle evidence](fabric-client-lifecycle.javap.txt).

Three regressions use real non-daemon pools to check complete accepted-task
drain, timeout without interrupting in-flight work, and caller interrupt
preservation while shutdown still begins. The final [build](fix-build.log)
passed176Java and10Python tests with no failures/errors/skips. Existing JOML
Unsafe runtime warnings remain; there were no compiler warnings.

The matching fixed title run again observed the same four-worker executor and
492 completed tasks: [observation](fixed-executor.json), [inspected image](fixed-title.png).
At09:22:44UTC normal stop drained/terminated all four workers. Handle29190 exited0;
runClient reported BUILD SUCCESSFUL, with no watchdog crash. Both runs retained
29 matching resource ERROR lines selected by the report's respackopts/item-model
filter; this change does not resolve resource compatibility. No worlds were
loaded and all154 world-file metadata remained unchanged.
[Fixed console](fixed-launch.log), [verification summary](verification.json).

## Custody, coordination and remaining checks

Baseline wrapper725629/start12194097, daemon725734/start12194163 and
game725924/start12194556 were observed live and are absent after terminal exit.
Fixed wrapper727762/start12216462 and daemon727866/start12216520 were observed
live and are absent. The fixed game finished before second process sampling;
no exact game PID is invented. A fresh runtime live-client scan was empty and
the launcher lock free. Short unit-build PIDs were not captured. Full available
metadata and explicit limits are in the custody JSONs. No broad PID inheritance.

Original six UID/start/exe/cwd/cgroup identities matched at both checkpoints.
User Modrinth516527 was unchanged; prior external Buddy707822 had exited
naturally, and no replacement service identity was claimed. No client/service
was stopped except these two explicitly launched title diagnostics. No owned
build/release timers or new agents were created.

For007, lead reported ready at09:23:57UTC for a LATER explicitly agreed interval
up to15min with no NEW owned Java/build/game/model jobs. That readiness is not a
hold, endpoint agreement, barrier, hash approval or grant activation. See the
unchanged [fresh readiness receipt](007-readiness-after-fix.json).

This passes rendered title startup/quit for the pinned pack with the fix. A full
world save/reopen with the compatibility callback remains unverified, as do
missing/future-version mod runtime cases and active animation-work shutdown in
native graphics. The pure task tests do not replace those checks. Ordinary birth,
food economy, hay gathering and the canyon remain separate pending tasks.


Later [current-source native world save/reopen](../world-reopen-current/README.md)
passed two ordinary full-world saves/quit with the exact idle Animatica pool
terminated. This closes that happy-path gap; busy native animation work at quit
and optional-mod absence/future-version runtimes remain unverified.
