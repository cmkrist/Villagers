# Player impressions: respect that experience can revise

Design checkpoint: November 7, 2026. This is the social update's implementation
contract and visible acceptance plan. Completed validation belongs in a dated
evidence report.

## A revisable first impression

An unfamiliar player starts with **high awe (0.85)**, comparable to a villager's
perception of an iron golem's power, alongside **low confidence (0.05)**. Initial
trust is 0.10 and fear is 0.15. This is an authored, revisable prior, with no
encounter evidence. It can produce attentive looking, respectful
distance, or cautious curiosity. It grants no command authority and implies no
knowledge of a player's motives, private inventory, or future actions. Natural
golems retain vanilla AI; this comparison does not add golem minds or physiology.

Keep four quantities separate:

- **Awe:** perceived capability and salience. A helpful powerful player can remain
  impressive; kindness need not erase perceived power.
- **Trust:** the expectation that this individual helps or refrains from harm.
  Initial reverence is not earned trust.
- **Fear:** anticipated harm or distress. Awe need not imply fear; an attacker
  can raise fear while losing trust.
- **Confidence:** the amount of grounded evidence behind an estimate. An
  unfamiliar player's prior has little evidence, even with above-neutral awe.

Different encounter histories can produce different reactions to the same
player. Needs, commitments, physical affordances and direct danger retain
priority. Attention must not repeatedly interrupt eating, escape, sleep access,
or essential work. These estimates do not imply obedience, worship, loyalty
contracts, consciousness, or personhood.

## Learn from the identified actor and actual outcome

Use the player UUID, villager UUID, world, observation tick, evidence source and
actual result. Display names are presentation only. One player's reputation must
not transfer to another because they wear similar equipment or share a name.

A positive handoff requires a real **useful** item moving from the player's hand
to the villager's equipment/inventory. Count it after transfer succeeds. A full
inventory, rejected item, spectator interaction, cancelled use, or failed reach
check gives no positive evidence. Record the donor decrement and recipient item
change, accounting separately for subsequent consumption/equipping. Food on the
ground has no identified giver without provenance. Do not count both sides of
one interaction or reward repeated failed attempts.

Usefulness is explicit: a previously unowned sword/hoe kind; food for a villager
with hunger at least 0.2 and no existing allowed food; or missing planting material
when a usable hoe is present. A redundant but successful transfer can move an
item without earning trust. Gift evidence also has a 600-game-tick cooldown.

Negative evidence requires accepted native damage attributed to the actual
player, rather than an attempted swing or cancelled attack callback. The current
hook teaches the injured villager only: **witnessed harm is not implemented**.

Witnessed protection is implemented separately. When a player kills a monster,
a living managed villager can credit that player only if the monster was targeting
it or had hurt it within the previous 100 entity ticks. Villager-to-player and
villager-to-monster distances must each be within 12 blocks, with line of sight
to both. The player must be alive and non-spectating. Generic nearby combat or
helping a different resident does not qualify. Protection has a 200-game-tick
cooldown. Its native hook exists; rendered protection validation remains a
separate test from the gift/direct-harm smoke.

Updates remain bounded and revise both priors and learned histories. A gift need
not erase violence immediately. Harmless exposure can support habituation, while
absence of action alone does not prove weakness or benevolence. Confidence is
not another name for friendliness; do not describe a simple bounded estimator as
Bayesian inference unless its implementation supports that interpretation.

## Persistent bounded records

Each world-scoped villager mind holds at most **32 player impressions**, with
finite bounded values and up to **eight recent typed notes** per player. Updating
a record moves it to the retention tail; overflow evicts the least recently
updated entry. Exposure is weak evidence with a 200-game-tick cooldown. Direct
harm and witnessed protection have separate evidence types; there is no witnessed
harm type. Frozen controls retain their checkpoint impressions, and inspecting an
unknown player returns the prior without inventing an encounter. Unloaded time
creates no fictional help or harm.

Schema 2 adds impressions while preserving schema-1 identity, name, temperament,
drives, home, values, visits, learning counters, and memories. Legacy records
start with no invented encounters; apply the prior upon first perception. Keep
corrupt evidence and protect unsupported future schemas. Storage stays at
`<world>/villagers/<villager-UUID>/mind.json`; player UUIDs are keys inside it,
rather than display-name paths or new operating-system homes. Back up a
schema-1 world before migration: the older 0.2.0 JAR does not understand schema 2.

## Inspect without teaching through a belief setter

`/villagers impression` inspects the nearest managed villager's view of its
calling player: resolved UUIDs, awe/trust/fear/confidence, grounded evidence, and
current response. It is an inspection command, not a reward source or setter.

For the file queue's server-source command, resolve the actual connected observer
explicitly and use that observer's position to select the villager. Report the
resolved player UUID/fallback. Do not invent a console identity or silently
select a villager near world spawn. Multiplayer calls use their actual caller;
ambiguous/no-player console calls need an explicit failure or documented choice.

## Fast visible acceptance

Use the dedicated **play** copy at 20 TPS after building the social update. Keep
completed adaptive/frozen comparison saves and evidence untouched. Play starts
from an earlier common snapshot and does not import the adaptive run's newest
learning. Donor inventory setup and damage are explicit social interventions,
separate from the untouched natural-supplies baseline.

1. Record the actual observer UUID, healthy recipient, and a non-recipient control.
   Inspect priors and capture the rendered scene/server audit.
2. Transfer an actual useful allowed item through native handoff logic. Record donor
   decrement, recipient change, exactly one successful handoff event/counter, and
   the same UUID's before/after impression. A rejected attempt must change neither
   inventory nor positive evidence.
3. In the separate play cohort, use one controlled nonlethal native player hit.
   Record whether the victim is the prior gift recipient, accepted health loss,
   real player-source attribution, the victim's changed impression, and no
   non-victim harm update. Never inject harm into the paired comparison worlds.
4. Observe attention/movement at comparable range and need state. Changed labels
   or numbers alone do not establish behavior. Distinguish hurt animation from a
   retained response to the identified player.
5. Save, close cleanly, reopen, and inspect the same UUID pair. Check both legacy
   data retention and schema-2 continuity. A later second real player can test
   identity discrimination; fabricated UUID updates are only unit checks.

An explicitly labelled development probe may invoke the native item-transfer and
player-attributed damage paths in the rendered play copy. It must report those
physical events and outcomes; directly modifying impressions cannot substitute.
The probe's convenience is not evidence that a person performed the interaction.
Test witnessed protection separately with an actual eligible monster kill and
visibility/targeting controls; do not report that capability as covered by a gift
or direct-hit probe.
See [testing.md](../testing.md) for runtime and evidence paths.
