# Ledge construction feasibility — approach rejected

The latest society was inspected without saved-world edits. Nell's position
(-256.83,68,779.39) is an isolated foothold, not a clipped body. A conservative
full-block survey finds only her current standing cell connected by ordinary
one-block steps. The closest eastern foothold is two blocks higher; other nearby
directions have tall walls or deep drops. Original world digests were checked
before and after both saved-block surveys.

`route-candidate/report.json` models seven AIR→DIRT additions. In the completed
virtual layout, a 37-cell walking route reaches within2.5blocks of Nell's bed.
Every candidate addition is attached to an existing or earlier solid block. The
model ignores native path and detailed collision/interaction rules: it is neither
an approved grant nor construction acceptance. No blocks were actually placed.

Candidate construction order:

1. (-256,68,780)
2. (-257,67,780)
3. (-256,68,781)
4. (-256,69,781)
5. (-255,69,781)
6. (-255,70,781)
7. (-255,71,780)

Seven fresh exposed grass sources were found64.07..65.81blocks from Fern's home,
just beyond the old soil job's64block limit. The previously consumed soil source
remains consumed. Source discovery does not imply permission or native reachability;
future work must approve and validate exact real sources. Do not silently widen
the old soil capability or inherit its consumed grant.

A new play-only operator diagnostic, `/villagers constructionprobe route <actor>
<x> <y> <z>`, performs one explicit native path query within64blocks of a current
managed actor. It uses the shared work-path budget, requests no chunks, does not
call moveTo, and grants no work authority. It records nodes and actual navigation
identity before/after. An unavailable shared budget refuses without a query.

The first 200-tick diagnostic run refused at startup because the session/shared
budget was unavailable. It exited0 with no route result. The600tick continuation
then queried Fern at gameTime103735, body(-233.44,72,748.75), goal(-256,70,779).
Result: **canReach=false**,37nodes, all loaded, outside ordinary home range,
navigation path unchanged. It ended at(-256,74,778), four blocks above the intended
foothold. This invalidates the assumed initial builder approach. Bounded/loaded
does not mean complete, permitted or safe. The existing resource path rule remains.

Next investigate complete native approaches to existing stands(-254,69,781),
(-255,73,779) and(-254,73,780), then derive an executable construction order with
native face/reach/occupancy checks. A bridge from(-254,69,781) may need an additional
dirt cell(-255,68,781); this is only a hypothesis. Placing from above can be blocked
by the builder's own supporting floor. Do not infer interaction visibility from
cell distance or build seven blocks instantly to test the finished route.

Both native runs were exact stopped-world copies and preserved all actual deaths
(Juniper66151, offspring81487, Dara98739). Both current adults survived. Latest
primary: **runs/ledge-approach/play, saved104097**, Fernhealth20/food16 and Nellhealth20/
food19. No resources, terrain, body, clock, weather, home or decision setters, new
grants or actors. Ordinary behavior and native growth continued. The installed
0.4.0, user worlds and other clients/services stayed untouched.

300Java+10Python pass, final build53517exit0. Initial83685 failed on a command-builder
parenthesis and was corrected before any native run. Mind8/work-sites6 unchanged;
only ConstructionProbe production source changed from the preceding checkpoint.
Source archive/manifests, process custody, terminal completions and native snapshots
are retained. The200tick case had no screenshot before its normal10second stop;
the collector records that absence instead of restarting for an image. Its initial
10second census caught wrapper+daemon; custody-complete.json captured all3live
identities later. The600tick case captured its complete chain on the initial census.
All sampled exact identities are now terminal. No new workers, service changes,
automatic build/release timers, hash/model grants or resource holds were introduced.
