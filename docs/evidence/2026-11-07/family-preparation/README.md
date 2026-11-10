# Own household reserve preparation — source verification

November 7,2026,09:31UTC. Corrects a circular prerequisite observed in the
[courtship fixture](../courtship-birth/README.md): reserve preparation previously
required a qualifying social encounter, while the invitation path required
already funded food. Full adults could lose stock and never prepare again.

`VillageFamilies.signal` now updates a local food intent from only the actor's
own otherwise-ready state and actual inventory. Preparation precedes invitation,
reacquires after food loss/session reload, and clears funded agreements while
unfunded. It does not create/select a partner. Existing physical provision gates,
real12-unit cost/seed retention, consent, nursery and child cooldowns remain.
No world/entity/inventory setters, free resources, hunger changes or terrain
permissions were added. Repeated readiness uses a native inventory scan without
stack snapshots/payment-plan allocation; actual birth still makes/rechecks plans.
No throughput benchmark is claimed.

`tools/test.sh --no-daemon --max-workers=2` handle66129 exited0 in6sec.
[Build console](build.log), [counts](verification.json).180Java tests and10Python
checks pass, zero failures/errors/skips. Four added regressions use real native
items/containers for stock preparation/reacquisition, suspension, session reload,
and1000 mixed inventories checked against the real payment planner with no
mutation. Existing JOML Unsafe runtime warnings remain; no compiler warning.

This pass launched no game, changed no world/config, and added no agents/timers.
Body/config/cooldown native guards were inspected and preserve existing policy;
the suspension test supplies an ineligible input, not a rendered safety proof.
No ordinary birth, crop access, sustained production or reserve protection pass.

At09:30UTC lead reported readiness withdrawn before the short ordinary build.
No007hold/endpoints/agreement was active. Wrapper730387/start12267385 and
single-use daemon730504/start12267438 were captured live and are absent at the
terminal checkpoint; short test workers were not sampled individually. Exact
build environment-tag scan was empty. Original six identities matched unchanged.
[Full current tuples and fresh readiness](007-readiness-after-test.json).
Readiness at09:31:48 for a later explicitly agreed up-to15min interval is not a
hold, barrier/marker, approval/GO or model grant. No PID exemption is inherited.
