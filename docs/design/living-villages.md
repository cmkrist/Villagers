# Living villages: design decisions and roadmap

Status refreshed from source and retained evidence, 2026-11-07. Steps 1–5 have
implementations; step 6 has ordinary families with a verified birth and reopen in
an explicitly supplied fixture, but parental bed provisioning remains unfinished.
Steps 7–8 remain proposals. Implementation, unit coverage and native acceptance
are separate claims; the checks below are not all complete. Each step must keep
the existing guarantees:
bounded state, grounded evidence (a villager only learns from things that actually
happened near it), versioned saves that never overwrite unknown formats, and no
claims of personhood.

Economic details are in [economy research](../research/economy.md).
[Project Lead handoff](../HANDOFF.md) records newer production work and runtime
custody. The Lead currently owns source changes; this roadmap refresh resumes
Claude's unfinished documentation without restarting steps already implemented.
Historical schema numbers below describe when fields landed; current mind saves
use schema 7. Do not deploy an older writer over newer worlds.

## Decisions

| Topic | Decision |
|---|---|
| Diversity | Heritable genome. Children inherit genes from both parents, with rare mutation. |
| Traits | Few, bold, categorical traits with visible effects, plus continuous tendencies. |
| Opinions | Villager-to-villager opinions, built like player impressions, changed only by witnessed events. |
| Language | Each village has its own invented language. It changes as it passes between villagers and generations. Players learn to translate it from context. |
| Memories | Structured events. In survival, a player sees a memory only if they shared the experience or were told it; otherwise it shows as `???`. |
| Mood | Needs plus recent memories make a mood, and low mood has visible consequences. |
| Trading | Villagers keep real emerald wallets and may refuse. A villager may approach a player to propose a trade at most once a day (configurable). |
| Breeding | Players may encourage pairs. A *safety* value from spare beds and doors drives the wish for a child. "Paternal instinct" sends a parent (either parent; villagers have no sex) to gather materials and craft a bed when the child has none. |
| Simulation | The village area stays loaded. Outside villages, each villager keeps a configurable chunk radius loaded. |
| Event director | Not now. |

## Roadmap

1. **Foundations (implemented).** Config file, structured memories with witnesses,
   `/villagers memories`, save schema 3.
2. **Genome and traits (implemented).** Genes, expression, inheritance, mutation;
   trait effects on needs and action choice; save schema 4.
3. **Language (implemented; acceptance gaps below).** Per-village lexicons, speech lines, player glossaries, word drift.
4. **Opinions and gossip (implemented; acceptance gaps below).** Villager-to-villager opinions; telling memories and
   opinions in the village language; told memories unlock for listening players.
5. **Mood and consequences (implemented; acceptance gaps below).**
6. **Safety, breeding and parental care (partial).**
7. **Trading (proposal)** per the economy research: wallets, prices, refusals, daily proposals.
8. **Chunk residency (proposal).**

The order follows dependencies. Language must exist before gossip can be "told";
traits feed mood, prices and breeding; residency is last, because it multiplies the
cost of everything before it and needs the profile data from earlier steps.

## Goal and definition of done

**Goal:** every roadmap step is built into the mod: its rules in plain-Java classes
with unit tests, wired into the running game, configurable where agreed, and
documented here. The build must have no compiler warnings and every test must pass.
Anything that can only be confirmed in a running game is listed under *In-game
checks* in that step's section, not claimed as done.

A step is done when all of these hold:

| Step | Done when |
|---|---|
| 3. Language | Each villager has a lexicon, saved. New villagers invent a language or copy a neighbour's. Villagers speak short phrases that nearby players see, with learned words translated. Players learn words from context and can teach them (`/teach`) and list them (`/glossary`). Villagers spread words to each other with occasional mishearing. Mutual intelligibility between villagers is measurable. |
| 4. Opinions and gossip | Villagers hold bounded opinions of other villagers, changed only by witnessed events. While socializing they tell each other memories and opinions. A player who overhears a told memory unlocks it if they understand enough of its words. |
| 5. Mood | A bounded mood from needs, recent memories and traits, with visible consequences and stated reasons. Repeated experience shifts tendencies within bounds. |
| 6. Breeding | A safety value from spare beds behind doors. Willing, safe pairs have children with inherited genome and lexicon, and players can encourage pairs with food. Parental instinct gives a parent a bed-providing goal, through narrow, limited actions. |
| 7. Trading | Emerald wallets with a conserved ledger, prices from needs and traits, refusals with reasons, daily proposals with configurable caps. Villages whose languages are mutually intelligible share prices, and players can ask villagers about rates elsewhere. |
| 8. Chunk residency | Village areas stay loaded while they have adopted villagers. Wanderers keep a configurable radius loaded, under a hard cap. |

## 1. Foundations (implemented)

**Config.** `config/villagers.json` is created with defaults on first launch. An
invalid file is left untouched and the defaults are used, with a logged warning.
Out-of-range values are clamped. Settings are added only when the feature that
reads them lands, so no setting exists that does nothing. The initial setting was:

- `memoryWitnessRadius` (blocks, default 16): how close a player must be, with
  line of sight, to share an experience.

**Structured memories.** Each memory has a stable id, a kind, the game tick, an
optional subject (who did it) and place, a short detail, and the set of players who
know it (at most 8). Kinds: `OUTCOME` (learning result), `COMPLETED` (finished an
action), `CLAIMED_BED`, `RECEIVED_ITEM`, `PLAYER_EVENT`, `SAFETY`, `OPERATOR`, and
`NOTE` (legacy free text). Players within the witness radius with line of sight are
recorded as knowing a memory when it is formed.

**Seeing memories.** `/villagers memories` lists the nearest villager's memories.
Creative and spectator players see everything. Survival and adventure players see
memories they witnessed; the rest show as `???`.

**Schema 3.** Schema 1 and 2 saves upgrade in place. Old memories become `NOTE` or
`OUTCOME` entries that no player knows yet, because nobody can prove they were there.

## 2. Genome and traits (implemented)

Code: `genetics.Genome`, `TraitGene`, `Trait`, `Tendency`; effects in `mind.AgentMind`.

- **Trait genes:** five genes, each with a dominant neutral allele and two recessive
  trait alleles that are opposites. A trait shows only when both copies carry it, so
  two calm parents can have a hot-headed child, and no one can be both opposites. The
  genes and traits are courage (coward / brave), generosity (greedy / generous),
  sociability (gossip / loner), appetite (glutton / frugal) and industry
  (diligent / lazy). Frugal and lazy were added so glutton and diligent have opposites.
- **Tendencies:** boldness, sociability, curiosity, appetite and fertility, each the
  average of 4 additive allele pairs in [0, 1].
- **Inheritance:** one allele of every pair from each parent, chosen independently.
  Each allele mutates with probability 1%: a trait allele switches to another allele,
  and a tendency allele shifts by a Gaussian step (sd 0.15), clamped.
- **Founders:** each trait allele has frequency 0.32 among founders, so each trait
  shows in about 10% of them and about 60% show at least one. Existing villagers get
  a founder genome derived from their UUID whose boldness, sociability and curiosity
  equal their saved personality exactly. Their traits, appetite and fertility are new.

Effects now:

| Trait | Effect |
|---|---|
| Coward | Explores less, and not at all below 60% health. |
| Brave | Explores more, and also at night while above 60% health. |
| Gossip | Socializes much more; gets lonely 30% faster. |
| Loner | Socializes much less; gets lonely at half the rate. |
| Glutton | Gets hungry 30% faster; forages more readily. |
| Frugal | Gets hungry 20% slower. |
| Lazy | Rests much more readily; tires 20% faster. |
| Diligent | Rests less readily; tires 15% slower. |
| Greedy, generous | Affect willingness to share real food after an understood request. Pricing effects remain proposed in step 7. |

Appetite scales hunger from 0.85x to 1.15x and boldness nudges exploring. Fertility
now contributes to ordinary breeding chance in step 6. `/villagers status` shows traits and tendencies.

Step 5 now applies bounded experience shifts to expressed tendencies. Traits
themselves stay fixed for life.

Tests check Mendelian ratios (carriers × carriers give 25% trait children), the
mutation rate, that children average their parents, founder trait frequencies, that
upgrades keep personality, and that trait pairs measurably change the action mix.

## 3. Language (implemented)

Code: `language/`, `Speech`, and the player commands in `VillagerCommands`.
Lexicons landed in mind schema 5; player glossaries have their own schema 1.

- Each villager has a saved lexicon over a fixed concept vocabulary. In mind
  schema 9, its cultural faction is its exact origin biome registry ID. Origin is
  recorded on first managed observation (at birth for ordinary managed children)
  and never changes on travel. Existing saves retain their vocabularies; their
  historical birthplace is unknown, so migration assigns their first observed biome.
  New adult speakers use deterministic biome dialects rather than copying a nearby
  foreign speaker. Faction membership does not itself impose hostility or combat rules.
- Ordinary children inherit only six core concepts (hello, yes, no, want, food,
  danger), choosing words from both parents. Other words develop through encounters;
  children cannot invent missing words merely by trying to say an adult phrase. A developing child's sparse or
  empty vocabulary is never bulk-replaced by an adult dialect. Prior saved child
  vocabularies remain intact.
- Each concept retains at most three competing words. The strongest is spoken;
  sufficiently familiar synonyms are also understood, while conflicting homonyms
  remain ambiguous. Hearing has a 2% mishearing chance. Passive acquisition preserves
  native pronunciation; explicit teaching and successful understood requests can
  strengthen a borrowed word and shift preferences.
- Villager learning stores bounded cross-situational word evidence (256 words,
  eight possible meanings each). Sounds are associated with candidate meanings
  from the observable situation, not per-word private concept labels. At least
  three observations and a 1.5x lead are needed before reinforcement starts;
  usable recognition normally takes four clear encounters. Known words help
  disambiguate the remaining words in a request.
- Conversations exchange greetings and name held objects or nearby visible beds
  and crops. Both participants can speak, each within its own cooldown. Real meals
  can ground food words; situated food requests supply candidate meanings for want
  and food. At most eight nearby managed listeners learn per utterance, within
  16 blocks and the configured hearing radius, with line of sight and while awake.
  Ungrounded gossip does not teach vocabulary; reputation transfer requires at
  least half the spoken words to be decoded correctly. Frozen controls do not
  acquire words. Concepts, contextual cues and phrase structures remain authored;
  this is vocabulary development, not learned grammar or unrestricted conversation.
- Nearby players receive **chat messages**, with known words translated and unknown
  words in grey italics. Speech is not floating text. `hearingRadius` defaults to
  12 blocks; unprompted remarks use `speechCooldownSeconds` (default 45).
- Each player has one global glossary, rather than one per village: at most 256
  words and eight candidate meanings per word. Translation needs at least three
  observations and a leading meaning with at least 1.5 times the runner-up's count.
  Grounded speech and the item held by the listener supply contextual evidence;
  ambiguous homonyms can remain untranslated.
- `/teach <concept> <word>` reinforces a nearby managed villager's vocabulary, with
  a 40-tick lesson cooldown and receptiveness based on the player relationship.
  `/glossary` lists understood words; operator `/villagers language` shows the
  faction, origin, known concepts, learning-evidence count, vocabulary and nearby
  listeners' directional comprehension of the speaker's words.

Lexicon/glossary tests cover bounded learning, copying, drift, translation and
persistence. Language-development tests also cover ambiguity, noun-anchored request
learning, foreign synonyms, biome dialects and partial acquisition across reloads.
These tests do not establish native gameplay learning rates or faction diplomacy. The later [audit](../audit-2026-11-07.md) fixed failed-save retention,
pending-progress reconnects and future-format protection with persistence tests.

**In-game checks remaining:** sustained translation/teaching usability across
players and dialects, reconnects during failed writes, and chat volume at larger
populations. Prompted replies bypass the ordinary speech cooldown; a per-villager
cooldown alone is not a recipient-wide message budget.

## 4. Opinions and gossip (implemented)

Code: `relations/`, `language/Telling`, `Speech` and `mind/AgentMind`.
Opinions and experience landed in mind schema 6.

A villager retains at most 16 acquaintances, each with bounded liking, trust,
fear and witnessed/heard counters. Current direct events are chatting, receiving
shared food, protection and an understood refusal. Theft and arguments remain
ideas, not implemented event types. A full opinion list evicts the least-engaged
acquaintance, using oldest contact to break ties.

Completed chats spread one concept word and may carry gossip. Gossip probability
is 80% for the gossip trait, 10% for loners, and 35% otherwise. Strong opinions
are preferred; hearsay moves liking/trust toward the teller's view by 0.3 times
nonnegative trust in the teller. It does not create a witnessed event. Player
reputation memories can also be passed on.

Told memories unlock for hearing players who understand at least half the words.
Direct player conversation requires trust of at least 0.2 before sharing an untold
memory. Survival memory visibility still distinguishes witnessed/revealed events
from unknown ones. Food requests require local reach, sight and actual language
understanding; sharing moves a real item. Inability to transfer food does not count
as deliberate refusal. Greedy or unhappy speakers may refuse; warm or friendly
speakers can share. Requests have a separate 600-tick budget.

Opinion, gossip and telling rules have unit coverage. See
[families](families.md) for the distinct mutual courtship protocol; a social chat
alone does not authorize birth.

**In-game checks remaining:** end-to-end overheard memory unlocking, reputation
spread, food sharing/refusal and message pacing with multiple listeners. Frozen
mode skips `Speech.converse`, including word spread, gossip and food requests;
these experiments differ socially as well as in learned action values.

## 5. Mood and consequences (implemented)

Code: `mood/Mood`, `mood/Experience`, `mind/AgentMind`, `Speech` and controller labels.
Mood is recomputed each think, not saved. Needs, health, traits and structured
memories from the last 48,000 ticks contribute to a value in [-1, 1]. The three
strongest material reasons are exposed. Bands are miserable below -0.5, unhappy
below -0.15, happy above 0.45, and content otherwise.

Low mood increases rest preference, reduces social preference and can block food
sharing and ordinary reproduction; happiness encourages company. Low spirits are
visible on labels. Trade refusal, arguments and a general wandering-off system
are not implemented mood consequences.

Rescue, harm, chats, refusals and safe exploration shift expressed boldness,
sociability and curiosity by at most ±0.2 each. These experience shifts are saved;
the inherited genome remains unchanged. Unit tests cover mood factors/bands,
bounded experience and persistence.

**In-game checks remaining:** useful visible reasons and behavioral differences
under controlled circumstances, including after reload. Existing family evidence
shows happy eligible actors, but does not validate every mood consequence.

## 6. Safety, breeding and parental care (partial)

Implemented rules and later additions are documented in [ordinary families](families.md).
Birth now requires two matching live spoken agreements, eligible adult bodies,
real breeding-food payments from both parents, kinship/cooldown/population checks,
and an existing available sheltered native HOME bed with a reachable route and
safe spawn space. Local bed/roof/door checks approximate shelter; they do not prove
whole-village safety. Children inherit genes and words from both parents and begin
with fresh action learning. The `breeding` setting can disable births.

[Native family evidence](../evidence/2026-11-07/family-company/README.md) records one
ordinary birth, exact food charges, two-parent heredity and an existing nursery in
an explicitly supplied fixture. Its [persistence check](../evidence/2026-11-07/family-company/persistence/README.md)
records the same child, home and parent cooldowns after reopening. This does not
establish autonomous population growth or a self-sustaining economy. Food supply,
protected paid farm expansion and newer tool work are tracked separately by the
Project Lead; historical evidence must not be attributed to later source versions.

**Remaining parental work:** supplying nursery beds, shearing/obtaining wool,
obtaining wood within explicit authority, crafting and placing a bed, and specific
feeding/escort/defense care for children. Bed crafting must require a crafting
table; missing resources or a station must produce a visible stall reason.

The user accepted limited autonomous construction with real materials and
protection for player structures. Follow [terrain permission](terrain-permission.md)
and the existing construction authority before adding a capability. Generic block
breaking, demolition and chest access are not authorized by the bed-providing goal.
No bed may be conjured or a player's structure guessed safe from its block type.

**In-game checks remaining:** native denial cases, parental provision/care and
renewable reserve output under natural resource limits, plus a new integration
check when bed protection or birth rules change. Preserve supplied fixtures and
failed attempts separately from the original cohort.

## 7. Trading (proposal)

This remains proposed: current trader recruitment is a separate feature, not a
conserved trade economy. As in [economy research](../research/economy.md):
per-villager emerald wallets, trades
that move emeralds rather than create them, prices from needs and traits, reasons for
refusals, and a daily proposal. Settings will include
`tradeProposalsPerVillagerPerDay` (default 1) and `tradeProposalsPerVillagePerDay`
(default 3).

## 8. Chunk residency (proposal)

- The village area stays loaded with chunk tickets while it has adopted villagers.
- Outside a village, each villager keeps `outsideVillageChunkRadius` chunks around it
  loaded (default 1, a 3×3 area; 0 means only its own chunk).
- A hard cap, `maxResidentChunks`, protects the server. When the cap is reached,
  the furthest wanderers are unloaded first and simply wait, without fictional
  off-screen time.
- Villagers perceive the world only through in-game senses. No extra knowledge comes
  from being loaded.

Profiling must establish the cost per resident chunk and per agent before defaults are
fixed. Existing trial-owned tickets do not implement this residency policy.

The config currently exposes `tradeProposalsPerVillagerPerDay` (1),
`tradeProposalsPerVillagePerDay` (3), `outsideVillageChunkRadius` (1) and
`maxResidentChunks` (256), but these are currently stored/clamped placeholders:
source has no runtime consumer for these four accessors. Changing them does not
activate trading or residency. Their presence is not implementation acceptance.
Reconcile them with the config's promise that each setting controls a feature
when implementing steps 7–8.

## Risks

- Scope: every step adds behaviour that must be tested in long visible trials, not just
  unit tests.
- Language may read as noise unless translation progress is fast enough to reward
  attention. Tune with playtests.
- Bed crafting needs a chain of new world-changing capabilities. Each one widens what
  adopted villagers can do to a player's world, so each needs clear limits.
- Chunk residency directly costs server performance, and the village keeps running
  while the player is away.
