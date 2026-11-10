# Current-source native world save and reopen

November7,2026,~10:00–10:04UTC, after007 early release. Same unmodified current
source across both launches, pinned MC26.3/full pack, normal20TPS visible play.
Last source suite188Java+10Python pass. No extra food/beds, actor movement,
terrain changes, forced actions/births, time/weather changes, model jobs, agents
or automated timers. Existing six research clients and user services untouched.

A full stopped64MB backup preceded launch:
`runs/archives/current-before-20261107T095939Z/VillageTrial`.
[Backup receipt](backup.json), [source digests](source-sha256.json).
The world was resumed in place and never restored/refilled. Read-only watch,
status, audit, tick query, screenshots and normal quit were the only commands.
The observer starts through the existing harness; no villager was relocated.

## Results

First90346 exited0; second14343 exited0. Both native world saves completed and
Animatica's exact four-worker executor drained/terminated during normal exit;
no post-main watchdog or mind/recovery persistence failure. The second used the
same source digests and normal Gradle compilation checks. All11 original UUID,
genome and home records remained identical. Living actors' age and learning
counters advanced; the six previously dead records stayed unchanged. All remain
schema7. This is native save/reopen continuity, not merely JSON serialization.
[First console](first-launch.log), [second console](second-launch.log),
[full comparisons and custody](results.json).

First shutdown census:5alive/6dead/0missing atgame31020. Second:4alive/7dead/
0missing at31899. **Willow-ca3c naturally starved at31882**, corroborated by native
death logs and saved cause=starve. Its first status had15health/0food; later2health,
and it died shortly before quit. The remaining cohort and all histories were
preserved, with no reset, revival, replacement or hidden supplies. The second
run lasted about44loaded seconds; it was not an instantaneous reopen. Future
acceptance should use an explicit observer tick budget so analysis delays do
not silently lengthen exposure.

Final native mean tick snapshots were3.692ms and3.613ms at20TPS and this small
4–5-survivor cohort. They do not certify larger population throughput, tail
latency, long archives or loaded-world capacity.

The new schema1 recovery checkpoint saved `requests:[]` after first quit,
reopened without blocking managed decisions, and retained identical bytes after
second quit. No unreadable/checkpoint errors or speculative child appeared.
This validates the **empty checkpoint/startup gate**. It does not prove native
nonempty recovery, extinction, partial residency, failed-spawn retry or crash
atomicity. The natural death with other survivors is consistent with retirement
of a stale extinction request, not direct internal queue trace evidence.

## Society behavior remains blocked

Fern's own `familyFoodGoal:true` and `saving food for a child` status were actually
observed in both sessions. Its wheat5/seeds10 and iron hoe (damage5/250) persisted,
with zero harvests in these runs. Body readiness did not become a funded partner
agreement or birth. Nell still stood at exactly(-256.828629,68,779.387396) on the
canyon ledge, with6bread and food20 in both reports. No ordinary birth/new lineage.
[First family report](first-family.json), [second](second-family.json).

The evidence now narrows the production problem: Fern has a real usable hoe,
planting stock and a native remembered farmer job site(-226,72,752), within its
home radius around(-227,72,773). At a sampled final position(-215.787,72,775.257),
the farm lies beyond the local radius8 crop search; provisioning declines and
ordinary exploration resumes. The controller does not travel to that known job
site when local resources are absent. This is a source/evidence inference, not
proof a complete path from that position is available.

A previously labelled operator crafting table was at(-226,71,744), approximately
29blocks from Fern's home center. Current station candidate filtering requires
its center within28; that can reject a station even when an actor could interact
from inside the allowed boundary. The table's **current** existence/path was not
rechecked in this run. Do not call that inferred second blocker a measured
craft failure. Next inspect/implement remembered worksite routing and interaction
reach geometry, with safe route/time/cap bounds, before another reproduction run.
Do not move actors or supply new materials to manufacture the result.

## Rendered and process evidence

Both actual images were viewed: [before save](first-before-save.png),
[after reopen](after-reopen.png). They establish rendered village/terrain and
pack font continuity, not villager intelligence from a distant camera. The Day4
counter includes earlier operator time changes; it is not four unassisted days.
Known resourcepack/model errors remain; full pack compatibility still limited.

All observed wrapper/daemon/game PIDs from both exact tag+parent chains are now
absent; command queue empty and runtime lock free. Original6UID/start/exe/cwd/
cgroup exact identities unchanged. No broad shared-scope ownership, PID
inheritance, current source/game worker, new timer or hash/model approval.
Historical abnormal courtship closure remains unchanged; these later two closes
are separate positive evidence. No further reopen was performed.
