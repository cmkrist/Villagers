# Local exchange of actual goods

Unreleased0.4.1-dev adds a small local market for supported edible food, spare
usable hoes and surplus supported raw wood. A buyer pays with emerald items already in its actual inventory;
the seller receives those same items, including custom components. The buyer
receives an actual seller-owned hoe, raw wood item or bounded food bundle, including wear and components.
Real sneak-right-click emerald gifts can introduce player-held money. No wallet,
currency endowment or production capacity is created by this feature.

The buyer uses its own unmet hunger or missing farming tool to decide whether
to seek trade. A planting-capable buyer with enough owned wood to make a hoe
keeps its existing production route. Search considers at most16 visible managed
adult neighbours within16 blocks and the buyer's home range, every100 ticks.
It rotates local candidates without reading their inventories, private needs or
willingness. Actual item/quantity quotes and payment happen only at mutual native sight, hearing and
three-block contact, within both actors' existing home bounds.

The requester says WANT TRADE FOOD/HOE/WOOD EMERALD in its actual language. The seller
must decode the whole proposal before evaluating its own stock. Its quote names
the actual food/hoe once per item and speaks each emerald in the price; the buyer must decode
the complete quote and the seller must decode YES TRADE before a commit. Missing
words, foreign meanings, inability to pay, capacity and unavailable stock create
no fabricated transaction or refusal penalty. Existing charitable food requests
remain separate; a purchase earns no shared-food relationship credit.
The buyer also checks its own resulting usable ration: a lone carrot/potato that
it must retain for planting cannot masquerade as a meal worth spending its money.
Food quotes start at five gross nutrition: one vanilla bread, two carrots,
five potatoes or five beetroot. The buyer's retained planting root can reduce
the immediately edible portion; this is not a promise of five edible nutrition
after root retention. A quote can grow to at most eight component-identical items
to leave a usable ration. It can combine matching seller slots, preserves each
item's components and may choose a lower-nutrition food when the preferred one
cannot fit. A hoe quote remains one item. The one/two-emerald price covers the
whole stated bundle; there is no partial delivery or extra per-item payment.

Quotes are provisional: one emerald normally, two for a greedy seller. A missing
tool or food at13or below permits a bid up to two; other food bids stop at one.
Food seeking still requires the actor's own unmet hunger and no usable ration.
The higher bid begins in the existing hunger-request range, leaving time for
actual provisioning and travel. It does not require the buyer to reach emergency
hunger before accepting an affordable two-emerald quote.
Both prices are subject to actual held money. Sellers decline in low mood, fear
or a sufficiently poor observed relationship. Buyers may still buy from someone
who refused charity, but decline when afraid. These are bounded authored rules,
not calibrated supply/demand prices or village-wide scarcity knowledge.

A seller retains at least15 edible nutrition after a food sale, excludes raw
wheat from that reserve and retains the farmer's last carrot/potato planting
root. A tool seller retains its actual held working hoe with at least16uses left;
when none is equipped, it retains the most durable usable inventory hoe instead.
Only a different inventory-held hoe with at least16uses left can be offered.
The held tool is never sold. Seller health, food>=13 and fatigue gates protect
its own body. Children, sleep/rest scheduling, water, fire, trading and insufficient
air prevent the operation. Immediate self-care, danger, food delivery, reactions
precede trade. Before another provisioning trip, a hungry buyer with actual
money can attempt a food exchange with a neighbour already in mutual sight,
hearing and three-block reach. This contact-only opportunity checks at most16
nearby candidates every100ticks and shares the existing600tick quote cooldown.
It cannot navigate, read remote stock or bypass seller reserves and consent.
Failed contact quotes leave ongoing provisioning alone; a completed purchase
ends that buyer's interrupted provisioning attempt. This prevents repeated
unreachable farm work from indefinitely hiding a physically available meal.
A funded hungry buyer also gives its bounded food-market journey priority over
starting another provisioning trip or charitable request. Queue waiting, route
failure, missing stock, quote cooldown and body/safety restrictions still yield
to other work. There is no guarantee that a visible neighbour has food for sale.

Approach uses the shared fair path budget and complete loaded/home-bounded paths
of at most128nodes. Queue and physical travel use WorkJourney's separate bounds;
physical stall80ticks, travel400, replan40 and failed-trip backoff400 remain.
Each admitted batch evaluates at most343 cells covering the full three-block
contact radius, pruning by actual reach before world reads. Safe AIR/AIR stands
require sturdy support, native body clearance, mutual hypothetical collider sight
and home bounds. At most three native stand paths are attempted per admitted
batch. A finite snapshot of candidates and its cursor survive the next admission;
three rejected paths do not discard untried stands. Each stand is rechecked
against current terrain, reach and sight before its path query. An accepted path,
exhausted list or interruption ends that search; replanning takes a fresh snapshot.
Rejected and accepted path diagnostics record actual query outcomes and eye
positions. The buyer still rechecks actual mutual sight and reach at contact;
standing near a wall or fence grants no exception to those checks.
Contact attempts back off600ticks per session. No new thread, timer, chunk load,
teleport, terrain edit, container access or remote market is introduced. Frozen
research mode does not trade. Interrupted proposals disappear on unload.

Inventory plans copy both bags, spend only real ingredients, and require complete
capacity on both sides, including slots freed by that very exchange. Differently
marked coins must fit as distinct real stacks. Item stack limits are respected,
including component-defined smaller stack
sizes; detached receipt insertion explicitly splits stacks into legal slots.
Both live inventories and their identities are checked before commit;
a plan is single-use even if balances later
return. Failed plans leave both bags untouched. The commit runs on the server
thread. Each actor retains one bounded diagnostic receipt with actual quantity,
price and before/after goods and money counts, plus a real-event mind
note through the existing coalesced writer. Native inventories preserve money and
goods across clean reload. There is no global crash-atomicity claim across entity
saves, nor a persistent market order, debt or off-screen trade.

The earlier emerald-economy research remains a roadmap: aggregate accounting,
player trade screens, price calibration, durable proposal budgets, repeated
currency circulation, inequality and population-scale simulation remain open.
Implementing this local exchange does not establish that full economy or sustained
survival. Native evidence is recorded separately under
[local-trade-native](../evidence/2026-11-07/local-trade-native/README.md).

## Production after an understood affordable request

When no saleable stock exists, the seller can quote the requested FOOD/HOE kind
and price. Only a completely understood quote and YES TRADE reply, backed by the
buyer's current owned money and bid, can create a production signal. No money is
reserved or paid, and no order or debt is created. A later actual contact quote
must be accepted and paid for the real item. Prices and ability to pay are rechecked.
If a quoted single planting root would remain unusable as the buyer's meal, the
buyer can request usable FOOD instead; it does not pay for that rejected root.
Failed receiving capacity or payment ends that attempt without claiming that
more stock would solve the inventory constraint.

ToolDemand retains one actual customer for at most12000loaded-world ticks.
Only the same customer's later hearing can refresh it; replayed timestamps cannot
restart a fulfilled request, and another customer cannot grow a queue. It clears
on an actual tool sale to that customer or expiry and drops on unload. A fit
producer with a working hoe, the combined food reserve and no spare can pay the
existing native log/plank/stick/hoe recipe chain for one inventory spare. It
retains its working equipment. A spare already held stops further production;
owned intermediates remain when safety, stock, expiry or missing resources stops
work. Material and reach/recipe/output-capacity checks still govern every step.
This grants no trees, blocks, stations or materials.

Customer work and its raw-input buying use a combined reserve: body food must be
at least13, and body food plus actual owned edible nutrition, capped at15, must
total at least28. That is the former minimum13body+15stored energy, while
accounting for an owned ration actually eaten into the body. For example,
15body+15stored and20body+10stored both retain30 actual nutrition. Saturation is
not counted. The personal stored-food target remains15, but nutrition does not
have to stay in the bag to count toward this work gate. The same rule applies
to admission and remaining recipes; no temporary input permit or fictional
energy balance is needed across reload. Working equipment, health, fatigue,
actual heard demand, native recipes and fresh labor intervals remain required.

Food requests use the existing one-customer FoodDemand reserve signal, raising
the edible target15→20. The same actual quote, willingness, budget and language
checks precede a paid-food signal. Ordinary provisioning must earn the extra
food, and an actual sale clears the request. Signal presence is not evidence of
production or delivery. The native circulation fixture and retained failed run
are tracked separately in
[market-production-native](../evidence/2026-11-07/market-production-native/README.md).

The signal retains the heard intent, GIFT or TRADE. An affordable paid proposal
can prepare stock, but cannot authorize charitable delivery or reserve a meal
for a gift. Those paths accept only an explicit understood charitable request.
An actual sale fulfills TRADE; charitable completion/withdrawal applies to GIFT.
The same peer's later actual request can change intent; duplicate timestamps
and other peers cannot replace it. Both intents share the one-customer limit,
five-nutrition surplus, expiry and unload behavior; neither creates a queue,
payment reservation or persistent order. This prevents a gift-willing producer
from silently converting paid preparation into an unpaid handoff. Native paid
and charitable request checks are recorded in
[food-intent-native](../evidence/2026-11-07/food-intent-native/README.md).

A safe producer with a saleable meal can bring it to its one actually heard paid
requester, if that body is loaded, visible and within the normal home/search
range. This uses a separate session-local journey and candidate cursor, sharing
the existing fair planning budget and complete native approach checks. It does
not search customer inventory, reserve money or assume the customer still wants
food. At actual mutual sight/hearing/reach, the normal full quote protocol asks
the buyer to evaluate its current need, bid, money and capacity. The producer
still retains its own reserve and working equipment. Failed offers/trips back
off, and the remembered request still expires or disappears on unload.

Prepared food can prompt this fresh incoming quote during the buyer's outgoing
retry cooldown. The producer's own600tick offer budget bounds it; a purchase
also restarts the buyer's outgoing cooldown. This avoids waiting for a stale
polling deadline after real production has made a meal available. Danger,
rest, player trading and social interruption cancel delivery work; no payment
occurs until actual physical contact and mutual acceptance. An actual paid
delivery earns a sale and purchase, with no charitable-transfer credit.

Prepared hoes now use the same bounded delivery mechanism. Food and tool offers
share one active journey, candidate cursor and outgoing offer cooldown. An active
eligible route stays stable; finished or failed attempts alternate domains when
both have ready goods and a visible known requester. An unavailable requester
does not block the other domain. The buyer's current tool need, money, language,
capacity and consent are rechecked at physical contact; an inquiry is not a sale.

Production-request diagnostics also retain the customer's observed food and
owned money at that actual understood exchange. The strengthened native fixture
samples body food and health every tick and requires the paid meal before food6,
no observed zero-food interval, no starvation damage, retained seller reserves,
conserved currency and clean restart. Evidence for this newer policy is separate:
[early-food-market-native](../evidence/2026-11-07/early-food-market-native/README.md).

Food-bundle payment, retained roots, actual consumption and the contact-first
provisioning opportunity have separate frozen-source native and restart evidence:
[food-bundle-native](../evidence/2026-11-07/food-bundle-native/README.md).

## Raw inputs for heard tool work

A tool producer can seek one supported raw wood item when it has a real,
unexpired understood tool customer, an actual working hoe, no spare for sale,
the combined body/owned-food reserve and a fit body, but lacks the ingredients for
its next wooden-hoe recipe. Food need retains priority. The input demand stops
once actual ingredients exist or the tool request no longer authorizes customer
work. It does not buy speculative stock or reserve another actor's money.

A supplier offers one owned supported log/wood/stem/hyphae item only while
retaining at least one such actual raw item for maintenance. Planks, sticks and
unsupported blocks are not substitutes for that quote. Both inventories use the
existing component-preserving, stale/single-use transaction and capacity checks.
The provisional price remains one emerald normally or two for a greedy seller;
a necessary input can bid up to two. These rules do not establish profitable or
calibrated production prices. A paid input wakes the existing recipe work while
leaving the original tool customer's signal pending. Only the actual completed
hoe sale fulfills that signal. Native recipe availability, ingredients, working
reach and output capacity still govern manufacture.

Missing wood creates no production signal, forestry grant, terrain scan or new
resource authority. This is an inventory market; renewable tree production and
repeated profitable manufacture remain distinct requirements.

WOOD is appended to the concept vocabulary. A villager that actually tries this
new proposal can name a missing concept in its own language, with at most64
invention attempts and no collision with its existing words. Existing words are
preserved. This does not translate the word for its listener: the full actual
proposal/quote must still be understood. Existing conversation and teaching can
spread the new word; migration does not silently give every resident a shared
vocabulary or guarantee immediate market access.

Tool customers remember at most one actual understood supplier response for
12000loaded-world ticks. They prioritize returning to that visible supplier over
ordinary provisioning, while immediate self-care, safety and food buying keep
precedence. Tool acquisition clears the memory, and expiry, clock rewind and
unload discard it. It creates no order, reserved stock or future price guarantee.
An unstocked respondent accepts new tool preparation only with working equipment,
the combined admission food reserve and an apparent owned-input/funding/forestry-task route.
Tool-less neighbours do not become fictitious manufacturers merely by hearing a
request. Any quoted item still needs actual ingredients and a successful later
native recipe/transaction.

Nonbinding food/tool inquiries can now be spoken at actual mutual sight and
configured hearing range, capped at16blocks and both home bounds. Each buyer
has a600tick inquiry budget. The receiver evaluates its own stock only after
fully decoding WANT TRADE, the kind and EMERALD; the customer must understand the
kind/price response and the receiver must decode YES TRADE before remembering a
request. A stocked respondent can acknowledge ready goods; a capable producer
can prepare missing stock. This does not transfer any item, reserve coins, grant
terrain authority or promise future availability. Actual item/quantity quotes,
capacity checks, payment and delivery remain within three-block physical contact.
A fleeing actor declines incoming and outgoing trade; native full-threat survival
and interrupted mid-recipe reload remain separate validation requirements.
