# Native owned-meal boundary and grounded food exchange

## Controlled native copy, not ordinary survival

Prepared a new independent runtime `runs/meal-probe/play` from the stopped
`runs/play` world. All168 baseline world files matched before the fixture change.
Only copied Fern's mind.json changed: food0, saturation0, exhaustion0, timer79,
and redundant hunger1. Native health20, position, inventory, terrain and all
other minds were copied unchanged. Fern already owned two bread; no food or
other resources were supplied. [Exact setup and baseline manifest](setup.json),
[retained preparation script](prepare-copy.py). Do not rerun against the existing
fixture or merge any fixture history back into the original village.

This is an explicit hunger setter **in a separate controlled copy**. It cannot
establish ordinary village survival, production sustainability, birth or recovery.
The original world remained stopped. Its full file manifest matched before copying,
after preparation and after the native run under runtime exclusion.

## Native result

Handle76199 exited0. Existing observer requested normal quit at33105→33505,
400loaded ticks; final save33508 includes three further shutdown ticks.
[Launch log](launch.log), [native boundary snapshot](budget-native.json),
[saved census](census.json), [results and exact custody](results.json).

Copied Fern consumed the two real bread: native inventory2→0, food0→10,
saturation0→10; health remained20. Two separate authored meal memories retain
learning index499, context-1/reward0. Subsequent completed EXPLORE attempts have
reward0, and its native position changed. This corroborates actual owned-food
consumption, no initial starvation hit and continued ordinary movement without
crediting those meals as exploration nutrition. With final food10, ordinary
regeneration could not hide an initial health loss. No fixture healing was used.

Four live/seven dead/zero missing remains unchanged in the copy; all original
histories were retained. The original village still has its two bread and original
food20; **its** hunger was not advanced or reset. [Saved copied mind](fern-mind-after.json).
The native screenshot was viewed and shows the village overview, not close-up
proof of eating: [rendered view](native-village.png).

World saves completed; Animatica's exact four idle workers drained and terminated.
Observed wrapper776669/start12839174, daemon776770/start12839233 and
game776928/start12839748 are all absent at the terminal checkpoint. These were
observed descendant identities, not inferred from shared cgroup membership.
The original six exact identities remain unchanged; all metadata is in results.
No new client remains, no automated build/release timer, no external observer,
model/hash/grant or coordination hold. 007 remains released and immutable.

Native mean tick3.87146ms at20TPS/four survivors is only a small-cohort snapshot.
Resourcepack/model warnings remain. Native injury-specific eating/regeneration,
waking/danger, planting-root retention, exact cooldown spacing and comprehensive
learning attribution still require focused acceptance. This run preceded the
food-sharing source change below; it is not native acceptance of that change.

## Next source change: requests must be heard and understood

The previous conversation path ignored the actual returned WANT FOOD utterance,
and could share food or impose REFUSED even when no request was spoken or understood.
Speech now requires a complete actual WANT FOOD decoded using the donor's own
lexicon. Partial words, silence, foreign words, reversed meaning and missing
vocabulary produce neither a transfer nor a refusal penalty. The requester uses
its own hunger and a separate600tick request budget; no peer hunger/inventory scan
selects a donor. Request budget is transient across restart, not a persisted claim.

Both actors must be alive, awake, not trading, in the same level, within3blocks
and configured hearing distance, with mutual sight. The actual giveFood operation
rechecks those constraints and managed identity before changing inventories. It
retains the previous real one-item transfer and planting-stock rule. Willingness
still uses donor traits, mood, own hunger and its own opinion of the requester.
No extra reserve is invented or automatically protected for hypothetical children.

A willing actor that lacks transferable food, or whose recipient lacks capacity,
does not count as deliberately refusing. An unwilling actor must actually speak
NO and the requester must understand it before REFUSED changes its opinion.
An actual visible handoff still earns SHARED_FOOD. Frozen controls retain their
existing no-conversation-learning behavior. No new world search, path request,
thread, timer, disk write or persisted schema was added.

Build32584 exited0:209Java+10Python pass, zero failures/errors/skips, no compiler
warnings. [Build](build-sharing.log). Six added regressions cover own-need/request
budget, silent/partial/foreign language, missing vocabulary, wrong decoded meaning,
understood refusal and physical/hearing/sight boundaries. Third-party JOML Unsafe
runtime warnings remain. No tagged build processes remain.

Native food-sharing acceptance remains pending. This change retains chat-triggered
requests; it is not proactive village aid, food-seeking navigation or canyon rescue.
Next food-economy work should also examine early elective meals: the existing
forage action can still eat bread with only one food point missing, even though
the new survival reflex waits until its nutrition fits. Do not claim global meal
efficiency or adequate farm throughput from the current checks.
