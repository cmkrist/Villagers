# Recovery checkpoint — source verification

Written during the actual007 manual no-new-Java commitment beginning09:40:25UTC,
fixed end09:55UTC unless explicitly released earlier. Only source/Python/docs
used. No Java/Gradle/javap/game/model job, agent or timer was launched during the active
portion. One ordinary build/test ran only after head explicitly released it early.
The immutable active receipt and its raw SHA remain unchanged.

Source adds compact64-request/128KiB schema1 checkpoint storage to the
existing persistence worker; startup restore gates managed ticks until its result,
while a corrupt future checkpoint resumes ordinary AI with recovery disabled.
Only real readable starvation history can supply an ancestor; unresolved records
are retained. Change snapshots coalesce; admission flushes before archive scan;
shutdown flushes remaining data and reports failed or pending writes as failure.
Lookup/native spawn failures now retain requests with backoff rather than losing
them. Disabled godchildren preserve the dormant checkpoint.

Native residency must cover the neighborhood's intersecting chunks before query
or spawn; checks use actual loaded/ticking entity predicates without forcing any
chunks. The public method signatures were confirmed by a Python class-file
reader, not by running javap or JVM code:
[native method surface](native-method-surface.json). This is ordinary-shell static
artifact evidence, not a native run or REA runtime Evidence.

Eight new regressions passed. `tools/test.sh --no-daemon --max-workers=2`
handle6944 exited0 in7sec after explicit early release at09:54:54UTC.
188Java+10Python checks passed, zero failures/errors/skips. Existing third-party
JOML Unsafe runtime warnings remain; no compiler warnings.
[Build](build.log), [verification and exact terminal custody](verification.json).

Observed wrapper753060/start12411860 PPID753049 and daemon753171/start12411914
PPID753060 are absent. Exact build-tag JVM scan empty; no play client, new game,
source worker or timer. Original6 exact identities unchanged. Short-lived test
worker PIDs were not individually sampled and no inherited exemption is claimed.
The active007receipt remains byte-for-byte unchanged; its early release is
recorded separately. No hash/model/unit/engine/successor/grant was invoked here.

Source checks establish checkpoint+death history roundtrip across persistence
close/reopen, not a rendered server/world restart. Native extinction, partial
residency/survivor arrival/failed spawn and full world save/reopen are pending.
No crash-atomic JSON/native entity save claim. See
[design and limits](../../../design/ancestry-recovery.md).
