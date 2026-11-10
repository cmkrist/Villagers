# Civilization stability: bounded peaceful acceptance

Unreleased0.4.1-dev now passes a four-founder peaceful village simulation through
world tick72,681 (over three loaded days), followed by clean restart. All four
original residents remained present, alive and at full health every observed tick.
Food never reached zero; the minimum was5. There were38 paid native crop
harvest/replants,10 cells harvested repeatedly after natural growth,5 actual food
transfers, and produced-food meals consumed by both nonfarmers. No birth occurred.
Breeding was enabled, with six actual beds; this is not a child-survival pass.

The supplied fixture has two generous/frugal farmers with iron hoes and real
wheat/beet seeds, two glutton dependents, four marked bread per resident,48age-zero
mixed crops, water, lighting, two tables and six beds. Common initial language and
a stationary test observer are declared setup. After setup, ordinary controllers
choose all work, travel, social contact, requests, gifts, recipes and meals.
Metabolism remains80base exhaustion/day, growth remains native, and the server
runs80TPS. There is no later food, wood, money, gear, resident replacement,
position/health/need/clock/weather/growth setter or scripted family/food action.

The first final-source observation reached its60,000tick limit with all four
healthy. Its budget-exhaustion result and world remain preserved. A stopped copy
continued without resupply and passed at72,681. Minimum food, histories, harvests,
transfers and original identities carry forward. The continued probe imposes a
96,000total-world-tick ceiling; reopening cannot grant an unlimited new budget.

The final restart verifies every resident's item totals and components, health,
nutrition, held tool and durability at entity load, before autonomous decisions
can legitimately change them. Native inventory compaction is allowed. The first
continuation checker incorrectly demanded identical slots and halted; that log
is retained. Corrected totals matched the old snapshot, and the final restart
uses canonical native item-codec snapshots including components.

The changes also have these separate acceptance results:

-393 Java tests and10 Python checks pass. Two regressions cover rejected family
  navigation clearing an action without completing/training it or crashing labels.
- A four-adult paid economy with only three initial maker bread completed two
  real wood purchases, two manufactured customer tools, farming, income-funded
  meals and clean restart. All five marked coins and native materials conserved;
  every resident remained healthy and the minimum food was9.
- Paid food commission and explicit charitable food production/delivery each
  passed and retained their consequences across restart.
- A separate two-adult renewable-food world passed at84,001ticks with30 native
  harvests, six repeat-harvest cells, produced bread sharing/consumption and exact
  inventory/nutrition restart checks. Its original60,000tick observation and
  stopped-copy continuation remain separate source/artifact-bound records.
- Two earlier four-founder artifacts crashed at the same rejected-family-route
  transition. Minecraft exited0 despite the crash; the missing success result
  correctly failed acceptance. A copy of the actual crashed world then ran the
  fixed controller for1,200ticks with the same four healthy bodies and no setup.

Production changes: sufficiently provisioned heard tool orders can buy inputs and
finish recipes before optional pantry topping-up; food preparation requires
possessed ingredients or an equipped planting route; successful suppliers are
remembered briefly; food requests rotate over visible neighbours instead of
repeatedly selecting the nearest; rejected actions cannot receive completion
credit or cause null-action label crashes. All existing payment, consent, body,
material, permission, loaded-path and inventory guards remain.

[Collected results](results.json), [source pins](source-pins.json) and
[source archive](source.zip) bind the evidence. Each case retains its actual probe,
JAR hashes, logs, reports and PID/start/terminal receipts. The collector independently
checks native class/JAR equality, coin/item exchanges, seed/loot/overflow/durability
conservation and real food transfers. Deliberately stopped fixture prototypes
`runs/stability-civilization` and `runs/stability-civilization-marked` are not
acceptance results; their worlds and exit receipts remain in place.

This establishes a bounded small civilization in a prepared peaceful environment.
It does not establish indefinite resource renewal, hostile natural-village
survival, child-rearing, autonomous houses/roads or population capacity. The food5
night-time minimum is a narrow margin. The primary natural village remains a
separate history with two surviving adults and its retained deaths; it was not
opened or rescued by these fixtures. Next priorities are overnight food buffers,
renewable wood/tool chains, safe natural terrain access and sustained generations.
No installed server, user world or release was upgraded.
