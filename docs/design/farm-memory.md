# Remembering observed farms

Unreleased 0.4.1-dev, mind schema 8. A villager with farming tools can remember a
crop patch it actually sees while performing its existing local crop search.
Seeing immature crops also counts; remembering a patch does not reveal whether
its crops are ripe later, promise a meal, or grant permission to edit it.

The existing cached radius-eight/y±three search nominates one nearby crop on real
farmland. A native sight ray, eye distance of at most eight blocks, current home
bounds, and crop/soil protection checks must pass before recording knowledge.
This adds no wider terrain scan or chunk requests. A villager learns no farm
locations merely from another villager's saved state, parentage or a global map.

Each mind retains at most eight `OBSERVED_FARM` episodes in its existing bounded
64-episode store. Observations within four horizontal blocks and two vertical
blocks of a remembered patch are merged. New distant patches evict the oldest
farm observation. Observation itself changes no food, mood reward, learned action
value or reproductive state. Old mind schemas migrate with no invented farms;
schema 7 cannot carry the new episode kind. Older binaries protect schema 8 as an
unsupported future version. Back up worlds before testing a newer source build.

When local supplies cannot meet a food goal, an observed patch takes precedence
over the villager's native assigned composter. An active trip retains its target
so discovering a nearer patch cannot restart its deadline. Knowledge is filtered
by dimension, current loaded state, explicit protection, normal home bounds and
32-block trip distance. No remote crop-state inspection selects ripe targets.
Within eight blocks, clear sight of a missing crop removes that particular memory;
this bounded recheck runs at most once per 100 ticks, over at most eight records.

Travel reuses the existing 400-tick deadline, 80-tick physical stall detection,
400-tick failure backoff, 40-tick path interval and shared two planning batches per
dimension per second. Complete native paths must have 1..128 loaded nodes, all
inside the ordinary home range. Actual native movement must start successfully.
Survival and trading interrupt as before. No territory expansion, teleport,
partial resource path or movement-exhaustion discount is introduced.

Arrival triggers another ordinary crop/food scan. Gathering still uses the actual
hoe, native drops, replanting payment, reach/sight, capacity and protection rules.
An observed patch can be revisited without an existing seed if native loot can
fund the replant; the commit must still prove that payment. The composter fallback
keeps its existing planting-stock requirement. This is remembered access, not new
food or guaranteed sustainable yield. Knowledge sharing between villagers, food
distribution, unreachable terrain and sufficient population-level production
remain further society work. Native evidence is recorded separately in
`docs/evidence/2026-11-07/farm-memory/`.
