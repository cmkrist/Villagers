# Observed farm knowledge and return trips

Unreleased 0.4.1-dev, mind schema8. The previous goal turn implemented one paid
soil expansion; this pass adds a practical use for persistent spatial knowledge.
The existing source searched for crops only within eight blocks and travelled
farther only to the actor's native assigned composter. It did not retain crop
patches it had actually seen elsewhere. This is a confirmed source gap, not a
claim that it was the sole cause of Dara's starvation.

An exact 313-file copy of the latest two-adult society at 100878 continued for
2000 native ticks. No food, tool, actor, terrain, weather, clock, body, home or
decision setters were used; operator inputs were read-only watch/status. A second
exact 318-file copy continued for 400 ticks to verify reopening. Parent worlds
remained unchanged. The installed 0.4.0, user world and other clients/services
were preserved. Older supplied fixture inputs remain historical background.

Fern learned wheat patches at (-226,72,751), tick101494, and (-230,72,753), tick102814,
through the cached local crop scan and actual sight checks. Her recorded mind had
no farm observations before this run. Nell learned none: actors did not receive
shared/global/parental knowledge. Native counters show **two observed-farm trips,
two arrivals and zero route rejections**. The retained last path at102494 targets
(-226,72,751), is native complete/permitted and has9nodes. The new target differs
from Fern's assigned composter at(-226,72,752). Memory notes corroborate both arrivals.

Fern also harvested/replanted wheat at(-228,72,753), tick102004. Actual native drops
were1wheat+2seeds; paying1owned seed changed21→22seeds, wheat0→1, ironhoe18→19wear.
The crop was replanted age0; no overflow was lost. This is one actual harvest in
the same run, not a causal survival comparison or proof of adequate yield.

The run exited0, saved102883,2alive/9originaldead/0missing, mean server tick3.181ms.
Reopen exited0, saved103286, same population, mean3.966ms. It completed another
observed-farm return and arrival on a complete permitted5node path. The two farm episodes
kept their actual identities/positions/observation ticks, and all three real deaths
remained terminal: Juniper66151, offspring81487, Dara98739. No recipe/income was
replayed. The dead offspring is outside the original cohort count. Nell remains
on her inaccessible clear ledge; food distribution, safe access and long-term
population survival remain open.

The implementation uses at most8farm observations inside the existing64episode
limit, a four-block horizontal/two-block vertical merge neighborhood, and no extra
terrain scan. One sight ray is added for a local scan's nominated patch. Missing
crop memories are rechecked at most once100ticks over8records and removed only
after a nearby clear view. Observation gives no food, action-value reward or mood
bonus. Readiness and physical work rules retain their separate roles.

Trips retain ordinary28home bounds, loaded1..128node complete native paths, shared
2planning batches/dimension/second,40tick replans,400tick deadline,80tick stall and
400tick failure backoff. A nearer observation cannot reset an active target's
deadline. This is remembered access, not wider roaming or new edit permission.
Protected crop/soil remains unavailable. A remembered patch is not remotely
queried for ripeness. Schema1..7 gains no invented farms; schema7 with the new
episode kind is rejected, and older binaries protect future schema8 files.

300Java+10Python pass (final build1667exit0; initial45451exit0). Four new tests cover
save/reload without food/learning/family changes, memory bounds/merging, invalid
observations, witness visibility and schema migration/refusal. Existing future-
schema file protection and family persistence checks pass at schema8. These tests
do not establish unseen farm discovery, communication between villagers, native
forgotten-crop behavior or larger-population throughput.

`source-main.zip` and both source manifests bind the tested source. Exact live
wrapper→daemon→game custody and actual original handle completion are retained in
each phase; all captured identities are terminal. No worker, release/build timer,
resource hold, model job or deployment was introduced. Third-party resourcepack
and Realms startup errors remain; this is not a full pack compatibility pass.
See [rules](../../../design/farm-memory.md) and run `verify-recorded.py` here for
checks against the retained outcomes and source archive.
