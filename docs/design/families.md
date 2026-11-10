# Ordinary families — first implementation slice

Current source, November7,2026. This is separate from divine starvation recovery.
Plain rules, persistence and integration compile/tests exist. No visible ordinary
birth or nursery-restart acceptance has yet been established.

## Rules

A completed social encounter can give two nearby adopted adults an opportunity
to have a child. Both must be awake, not trading, dry, not burning, healthy
(at least90%), fed (food14+), rested (fatigue below0.6), and not in low mood.
Neither can dislike the other or have been recently attacked. Direct parents,
children, siblings and other pairs sharing a recorded parent are excluded.
Only immediate stored parentage is available; complete extended-family detection
is not implemented. Daytime and absence of visible nearby monsters are additional
safety conditions. These checks do not prove the area is safe from every hazard.

Each parent pays12 actual breeding-food units: bread counts4, carrots/potatoes/
beetroot count1. Wheat and unrelated items cannot pay. The farmer's final carrot
or potato is kept as planting stock. Detached plans are checked against current
inventories before committing; failed admission does not charge parents. A child
starts with a full food bar, funded by this reproduction cost, and learns to
survive normally. There are no free adult replacements or hidden hunger setters.

A nursery must be an available HOME point of interest within16 blocks: a complete
matching head/foot bed, empty and with two air blocks above each half, roof over
both halves, and a nearby wooden door. At most8 POI candidates and3 paths are
considered. The door proximity/roof test is a local shelter approximation, not
proof of a completely sealed building or invulnerability. Minecraft's POI ticket
is reserved before spawning and installed as the child's home. Existing bed
claims are respected; no bed is spawned by ordinary breeding.

The pair's fertility, mutual regard, and available nursery count affect a bounded
chance (maximum25% per eligible attempt). Each participant has at most one attempt
per200 game ticks. Expensive family searches are limited to one per dimension
per40 ticks. A local radius32 population of64 living villagers, including vanilla
residents and children, prevents further births in that neighborhood. This is
not a world population cap or a proven load capacity. No extra chunks are loaded.

Parents receive native age cooldowns and a persisted24000-tick next-birth deadline.
The baby is a real native baby, spawned in checked local space, with inherited
biparental genes and a first lexicon copied from both parents with existing word
drift. Personal action values start fresh. Structured birth episodes identify
the child and its parents; mood can reflect the actual birth. Status reports the
last family decision and next allowed birth tick.

## Persistence

Mind schema7 adds a saved next-birth tick and two-parent lineage. The existing
`ancestor`/`ancestorName` fields retain their legacy divine-ancestor meaning for
god children; for ordinary children they identify the first parent, with explicit
`secondParent`/`secondParentName`. `godChild` distinguishes the types. Founders
still have no lineage. Schema5/6 god children upgrade without inventing a second
parent; missing cooldowns default to0. Future schemas remain protected from
old writers. Older releases cannot read new schema7 saves; keep world backups.

## Evidence and remaining work

Regression tests cover physical/relationship/cooldown willingness, food costs and
seed retention, bounded chance, kinship exclusion, both parents and inherited
words, fresh action learning, parentage/cooldown round-trip, schema6 god-child
migration, and counter saturation. Current source compiles with the real engine.
These are not proof of in-game birth or conserved nursery occupancy.

Next: one visible isolated play fixture with two eligible adults, explicitly
supplied food and a real spare sheltered bed. Observe the ordinary controller,
then verify one baby UUID, two charged inventories, inherited genome/lexicon,
reserved home, positive parent cooldowns and persistence through reopening. Also
exercise no-spare-bed/no-food cases without creating a child. Label operator
materials and environmental interventions; do not call it natural population
growth or a complete parental-care system.

Open economy work includes crafting surplus bread for family reserves, renewable
tool replacement, and parent bed provisioning. Parents do not yet gather wood,
shear wool, build nursery beds, feed/escort children, or protect them specifically.
The strict spare-bed requirement currently blocks births when a village is full.
The agreed larger roadmap (bed provision, conserved trade, bounded residency)
remains active in living-villages.md; this slice does not complete step6.

## Household food provisioning

Otherwise willing adults prepare one parent's real food reserve from their own
state before advertising courtship. A qualifying social encounter can also set
this goal. Lost stock after eating/sharing, or a reloaded session with insufficient
stock, reacquires preparation without requiring another partner encounter. This
is an authored goal, distinct from the elective action learner. It only takes control
while that parent has at least14food (the breeding-body floor), is healthy,
rested, dry, not sleeping or in low mood,
and it is daytime after the survival layer has declined movement ownership.
It uses the existing real food pickup, harvest/replant and native bread-crafting
operations, retaining the resulting food in inventory instead of eating it.

The goal stops once actual inventory can pay one parent's breeding cost. It
attempts only when relevant resources are known, times out blocked routes, and
backs off before trying again. Ordinary needs, trading, danger and rest retain
priority. Superseded elective actions receive no outcome-learning credit. The
intent is transient and re-established from the actor's own willingness and
actual inventory after a restart; real stock remains in native entity saves. This closes the
hungry-only craft-and-immediately-eat limitation, but a complete naturally
renewable family economy still needs live evidence and tool/bed provisioning.

## First visible family observation

The isolated schema7 fixture check supplied two adults real food and one spare
sheltered HOME POI. They remained fed/HAPPY with reserves but did not complete a
qualifying mutual encounter during the bounded window. No ordinary birth occurred;
charges, child inheritance/home/cooldowns and reopen remain unverified. Evidence:
[evidence note](../evidence/2026-11-07/ordinary-birth/README.md).

This identifies partner coordination as a behavioral bottleneck. Next use grounded
local readiness communication or remembered mutually expressed intent, with
expiry/recipient bounds and normal social movement. Do not read private minds
omnisciently or force a test-selected pair to converse to manufacture acceptance.

## Grounded courtship implementation

Ready adults can now make a local spoken family offer. It uses their own needs,
food reserves and cooldown, discloses public family identity for kinship checks,
and requires a nearby listener to independently decide whether to reply. All
three actually spoken intent/reply words must match the receiver's own lexicon;
no private peer inventory or learned values are read to select a mate. A reply
must actually be spoken and understood before reciprocal agreements are created.

Offers respect ambient speech cooldown and last at most1200 ticks. Direct
replies have a separate600-tick budget, avoiding synchronized speech cooldowns
that could prevent acceptance forever. Each dimension processes at most one
offer attempt per20 ticks and at most8 local hearers per offer. Hearing radius is
bounded by the configured radius and16 blocks, with line of sight. One current
agreement cannot be replaced by a third offer. Each actor remembers at most one
partner; intent disappears on unload/restart and expires rather than becoming a
permanent binding. This creates no schema change or persistent partner ledger.

An eligible actor can pursue an agreed meeting through the ordinary SOCIALIZE
executor while that known partner is locally visible, managed, in range and
within the home boundary. Other social candidates retain their old range and
relationship preferences. The authored meeting goal is not credited to the
contextual action learner. Survival, trading, physical needs, player avoidance,
readiness and the original real birth/food/nursery checks remain in force.
Agreements clear after a birth or lost readiness. An invitation is not a birth.

Five pure regressions cover locality/visibility/readiness/understanding, clock
rollback/expiry, unseen partners, third-offer rejection and declared kinship.
The full161-test suite passed. Courtship has NOT yet been observed in a visible
client; ordinary birth and all birth/restart acceptance remain pending.

## Courtship live result

The first current-source courtship run observed an audible readiness offer but
no reply, agreement or birth. The originally supplied adults were outside the
12-block hearing range: Nell was physically stranded on a steep canyon ledge,
while Fern had naturally shared away its saved breeding reserve. This does not
isolate the courtship logic as the cause of nonreproduction. Six unsupplied
residents with low starting food died of native starvation; their histories were
preserved. See [courtship-birth evidence](../evidence/2026-11-07/courtship-birth/README.md).

Do not force a meeting, lower the real food cost, teleport the stranded actor,
restore the dead cohort or treat unit tests as a birth pass. The next simulation
priority is feasible food production/access and reserves, then a new bounded
reproduction check with real prerequisites. A native watchdog shutdown failure
and remaining pinned-resource errors also need attribution before release.


The next source food slice adds an optional finite hay-to-grain fallback shared
by ordinary forage and the existing family provisioning intent. It is off by
default and does not change birth costs or invent a family goal for actors who
have not established one. See [food bootstrap](food-bootstrap.md). Live access,
renewable production and reserve-sharing acceptance remain pending.


## Preparation before courtship — source correction

The visible reserve loss exposed a circular prerequisite: a full villager could
only start stocking after a willing social encounter, while local invitations
required already stocked food. Current source establishes the existing food goal
from an adult's own enabled/daytime/physical/mood/cooldown readiness and inventory.
It stops at one parent's12 breeding-food units. Actual collecting requires at least14food and the existing
health/rest/safety/capability/resource checks.
Preparing does not require, select or reveal a partner and does not reserve a
nursery. Funding stock alone neither creates consent nor permits birth.

This also reacquires the goal after eating/sharing or a session reload. It does
not protect stock from later sharing, create ingredients, find unavailable tools,
solve access, or establish renewable supply. Those remain economy tasks.

Repeated readiness checks now scan native inventory without copying ItemStacks
or allocating a payment plan. Detached payment snapshots and callback rechecks
remain on the actual birth path. Four regressions cover real wheat-to-bread
preparation, food loss/reacquisition and cleared consent, suspension, session
reload and1000 mixed native-inventory comparisons against the payment planner.
The full180Java+10Python checks passed. This is source acceptance; no new live
family/production/birth observation was made.
[Build, verification and custody](../evidence/2026-11-07/family-preparation/README.md).


## Own-intent native observation

A later same-source save/reopen observed Fern's active saving-food intent in
both sessions, with real wheat5/seeds10 and an equipped usable iron hoe. It did
not produce new bread or a birth. Its remembered farmer job site lies beyond
the local8block search from its sampled location; the source does not route back
to that known worksite. Table filtering at the28home boundary is a further
unverified access hypothesis. Nell remains stranded. Willow naturally starved,
final4alive/7dead/0missing; no cohort reset. See
[world reopen evidence](../evidence/2026-11-07/world-reopen-current/README.md).


## Remembered farm and bounded stock planning

Current source can send a farmer with at least14food, otherwise eligible for provisioning, to
its own existing native composter job memory when local supplies are absent.
It needs real planting stock/hoe, loaded same-dimension site, permitted home path,
physical progress and a bounded trip/backoff. It knows no unseen crop yield and
creates no field or items. Stock planning stops extra wheat collection once raw
inputs cover the missing part of one parent's12-unit reserve; payable roots stay
eligible with seed retention. Raw wheat is never actual birth payment.

Table candidates can extend one interaction reach beyond the home center bound,
but actual body/stand/path stay within28 and real sight/eye reach applies. A
bounded legal stand search replaces walking onto the table. Shared route budget
and stalled-target backoff bound work. No general terrain edit is enabled.

A bounded1200tick native run observed Fern returning toward the remembered farm,
one real harvest/replant/hoe wear and2bread crafts, inventory wheat5→0/seeds10→12/
bread0→2. Earlier supplied hoe/table remain labelled inputs. Its2bread still
cannot fund3; no partner consent or ordinary birth.4alive/7dead unchanged.
[Exact production evidence and limits](../evidence/2026-11-07/remembered-farm/README.md).


## First native ordinary family — latest acceptance

Current source now requires both matching live agreements at consideration/birth
and after spawn callbacks. Own funded readiness can motivate a bounded approach
to visible adult company, without reading peer food/needs or conferring consent.
Company uses the shared path budget and home limits. Offers need an audience and
a dedicated finite budget; company preempts incomplete unrelated elective work.

Two failed supplied-copy attempts are preserved. Continuing the second copy with
no further food or actor setters produced Fern-589b at36379, parents Fern/Dara,
ordinary generation1/godChild=false. Each paid3realbread/12breeding units; matching
pre-birth agreements and both60379cooldowns are recorded. The baby inherited
parent genes/27words, claimed existing nursery(-242,72,759), and survived normal
save/reopen with stable identity/genome/lineage/home and advancing age. This is
native birth acceptance in a labelled4/8bread fixture, not autonomous surplus,
parental care or sustainable population.218Java+10Python pass.
[Complete native evidence](../evidence/2026-11-07/family-company/README.md).

User has now selected limited autonomous construction with real materials and
protection for player structures. [Accepted terrain direction](terrain-permission.md).
Site/protection/resource mechanisms remain to implement; no blanket terrain grant.
