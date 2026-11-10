# Off-thread extinction ancestry

Current-source performance pass, November7,2026. This affects optional god-child
recovery after starvation, not ordinary two-parent breeding.

The old extinction path synchronously loaded every historical mind into a list
on the server thread. The new path captures immutable current state, requests a
streaming best-candidate reduction on the existing persistence worker, and checks
completion during later server ticks without waiting. It retains a champion
rather than the full historical population. Age, learning updates and UUID keep
the previous deterministic ranking. Historical files from another dimension,
unreadable files and unsupported schemas are skipped without quarantine/writes.

Current loaded identity IDs mask their older disk records, even when their new
home is outside the village. Queued immutable snapshots likewise override disk.
A request-time overlay is detached from later queue mutations. Only primitive
neighborhood data, IDs and immutable snapshots cross the worker boundary; live
levels/entities/minds are inspected on the server thread only.

Nearby extinction triggers coalesce by the existing radius64 neighborhood.
There is one in-flight ancestry lookup and at most64 admitted pending village
recoveries. An overloaded backlog logs an admission failure; death records remain
saved and no speculative fallback is invented. Requests above capacity are not
silently claimed recovered. This cap is not a many-village capacity benchmark.
A new starvation may refresh a coalesced trigger; living villagers cancel stale
recovery. Survivors are checked again immediately before spawning. If the target
chunk unloaded during lookup, completion waits for natural residency rather than
forcing it loaded. Ordinary spawn remains native and vulnerable.

## Verification and remaining limits

Six regressions exercise champion ranking/tie-breaks, changed-home masking,
corrupt/future-format preservation, neighborhood/dimension boundaries, a blocked
IO worker that does not block the caller, queued/current overlays, request-time
immutability and shutdown rejection without caller-thread IO fallback. These are
not rendered extinction/birth evidence or an archive-scale benchmark.

The historical scan still takes IO time proportional to the readable archive
and currently shares the existing persistence worker. It can delay that worker's
loads/saves; it no longer blocks ordinary tick acquisition on disk or retains the
whole archive in memory. A maintained ancestry index may be useful after measured
archive workloads justify it. The remaining blocking census API is diagnostic,
not called by the extinction tick path.

Current source adds a separate schema1 `villagers/recoveries.json`
checkpoint with at most64 compact dimension/position/deceased-UUID references,
not duplicated minds. It is loaded on the existing worker before managed
metabolism/decisions begin. Corrupt/unsupported checkpoint files disable only
divine recovery for that session; ordinary AI resumes and the file is protected.
Missing/corrupt/future death histories retain the request without inventing a
founder. Disabling godchildren leaves an existing checkpoint dormant/untouched.

Checkpoint updates coalesce in one immutable pending reference. The existing
save worker writes minds then checkpoint, including before a historical ancestry
scan and at successful graceful shutdown. Failed writes remain pending and
shutdown returns false, including on a repeated close. Native spawn/lookup
failures keep requests with bounded backoff. A successful checkpoint/reopen can
resume pending lookup using existing death history; champion selection is
recomputed. Missing dimensions/history remain retained for later diagnosis.

The implementation also requires native loaded-and-ticking entities across all chunks
intersecting the64-block neighborhood before ancestry admission/spawn. At most81
chunk predicates per request per40 ticks; no chunk is forced loaded. Seeing a
living villager can still retire a stale request immediately. This prevents
unloaded villagers or early chunk startup from being mistaken for extinction.
These work bounds are not a native capacity/semantics acceptance result.

After explicit007early release, build6944 exited0 in7seconds:188Java and10Python
checks passed, zero failures/errors/skips. Eight added regressions cover compact
roundtrip/capacity, unknown/corrupt/oversized/link protection, coalesced nonblocking
queue+graceful reopen, retained unreadable death history, checkpoint and mind-write
failure reporting, ancestry checkpoint ordering and stopped-executor refusal.
[Verification and custody](../evidence/2026-11-07/recovery-checkpoint/README.md).
No native game was launched in this source pass.

This is not an atomic transaction with native world/entity saves. Crash/power
loss and the interval between entity spawn, journal update and native entity save
still require separate reconciliation/acceptance. A failed/timed-out shutdown
claims no successful checkpoint. No historical transient request is reconstructed
without a saved checkpoint. Rendered extinction, survivor arrival, native failed
spawn, partial residency, and successful full world save/reopen remain pending.

Next live checks: starvation-triggered recovery, no recovery for other death
causes, survivor arrival while lookup is pending, natural chunk departure/return,
and clean save/reopen. Keep operator setups and divine recovery interventions
separate from natural-supplies survival comparisons.


## Subsequent native empty-checkpoint acceptance

Two current-source visible world saves and a same-world reopen exited0. The
new empty schema1 checkpoint survived byte-for-byte and managed age/learning
advanced after restoration; no native save/checkpoint failure or spurious child.
One additional natural starvation with other survivors was recorded and kept,
final4alive/7dead/0missing. No nonempty ticket was carried across a native restart,
so that acceptance, extinction/partial residency/failed spawn and crash atomicity
remain open. [Exact evidence](../evidence/2026-11-07/world-reopen-current/README.md).
