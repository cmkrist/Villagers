# Survival trial: failed, three of eight original villagers remain

Recorded November6,2026 (local), November7 UTC. The user observed game day6; the fixture has also had manual time changes, so the evidence chart uses elapsed time since the latest rendered client run instead.

Five distinct original villagers have explicit server-side zombie death records: Moss, Pip-4b46, Juniper, Pip-dda5 and Hazel. These are confirmed deaths, not merely unloaded entities. The remaining server-side identities are Fern, Rowan and Sage; sampled health is20,14 and20 respectively. Original mind files are retained as records, not living entities or an automatic resurrection mechanism.

The movement/doorway regression check and twelve unit tests still passed their narrower checks. They did not establish survival. This longer rendered trial **fails the project survival objective**. It has no matched vanilla/frozen-learning cohort and cannot measure improvement over those baselines.

## What the evidence establishes

- Damage, death and absence are grounded in logs and observations rather than inferred from labels.
- The killed villagers were labelled afraid/fleeing, so an authored fear response can trigger while still failing to protect its owner.
- Random flight is not reliable refuge seeking. The controller selects an away point; it does not prioritize a verified protected household or retain a safe escape plan.
- The open courtyard and cabins with open entrances are not predator-proof shelter. Resting in a bed is not sufficient protection.
- Exploration can carry a villager far from home. Navigation failures/timeouts do not currently build a useful target-specific avoidance memory or a robust return-home plan.
- Eating only reduces the authored hunger drive; it does not provide a health recovery mechanic, and the fixture has no renewable food economy.

These implementation gaps explain why survival is unproven and suggest the next controlled experiments; they are not an exhaustive causal diagnosis of every individual death.

## Next implementation and experiment

Prioritize reachable shelter and nightfall return, explicit door/gate use, risk-aware food search with a home range, and escape-path progress/failure handling. Test each change in a separate visible cohort before repeating a multi-day trial against matched vanilla/frozen-learning controls. Keep this failed trial and its surviving identities intact. Do not silently replenish, respawn, grant invulnerability or replace its result with a fresh eight-villager cohort.

Artifacts: `result.json`, `death-log.txt`, `server-report.json`, and `population.html`. The original rendered client remains active; this status check started no new build, game or model jobs.
