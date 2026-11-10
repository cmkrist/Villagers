# Modern solutions for persistent learning villagers

Research date: 2026-11-06. This document separates published results from project design proposals. Recommendations below are engineering inferences; none of the cited work establishes consciousness, subjective feelings, or personhood.

## Implementation status at the first prototype

The source now contains a small contextual action-value learner with bounded values and experience counts, hunger/fatigue/social drives, stable seeded personality, bounded event memory, and world-scoped UUID persistence. The Fabric controller is being integrated around foraging, rest, socializing, exploration and threat-driven flight. This is a narrower implementation than the architecture proposed below: it does not implement a learned world model, language reasoning, trust relationships, multigeneration genetic evolution, or unrestricted invention of new actions. Its action values estimate observed outcomes; this is not evidence that it has learned long-horizon planning.

`LabClient` adds an opt-in, visible-client development harness behind `-Dvillagers.lab=true`. It creates or reopens the disposable `VillagerLab` flat world at normal difficulty, invokes the lab setup command, sets a spectator camera, and keeps simulation active while the window lacks focus. GPU-framebuffer screenshots are scheduled after 15 seconds, 60 seconds, and then each minute. Captures and structured client-tracked entity observations go to `run/evidence/`. These observations describe what the client tracks, not an omniscient server survival census.

For rendered experiments, atomically deliver a newline-separated `run/lab-command.txt`. The harness consumes it once and accepts `villagers ...`, restricted time/weather commands, `gamemode spectator @a` or `gamemode creative @a`, numeric camera teleport commands such as `tp @a 15 -48 24 145 28`, and `screenshot example-label`. Labels contain only letters, digits, underscores and hyphens. The channel is active only in the named lab world and rejects oversized files and commands outside its allowlist. It cannot execute operating-system commands. Submit a screenshot separately after a world-changing command has rendered to capture the new state. `commands.jsonl` records dispatch; dispatch alone does not assert command success.

The full prototype passed `JAVA_HOME=/home/niko/.internal/minecraft-tools/jdk-25.0.5+8 ./gradlew compileJava --console=plain` against Minecraft 26.3. Rendered gameplay validation, persistence recovery and population capacity must be reported separately after running the actual client. The experiments below remain acceptance criteria until evidence is recorded; compilation and scheduled screenshots are not completed gameplay test results.

## Decision

Build an embodied artificial-life simulation inside Fabric: immediate survival control, homeostatic needs, bounded online learning, persistent autobiographical events, social relationships, and heritable personality. Keep expensive language or world-model inference optional and outside the Minecraft tick loop. Prove survival and measurable adaptation before adding dialogue or genetic search.

This is feasible with ordinary Java on the CPU. The intended many-villager scale is a hypothesis to benchmark, not a result already demonstrated by this project. Learning should change choices among validated capabilities; it should not have to rediscover how to open a door while a zombie kills its owner.

## Evidence and what to borrow

### 1. Homeostatic reinforcement learning: needs can ground reward

Keramati and Gutkin model reward in terms of reducing deviations from physiological targets. Their framework links associative learning with internal state and explains anticipation and competing needs. It offers a principled replacement for a single fixed appetite or arbitrary reward chemistry. It is a normative model with assumptions, not a universal guarantee for partially observed, changing Minecraft worlds. [Original paper, eLife 2014](https://elifesciences.org/articles/04811).

**Project proposal:** track energy, fatigue, perceived danger, social contact, and comfort as bounded values. Food changes energy only after an actual inventory consumption; resting changes fatigue only after the rest action executes. Personality modulates preferences, never removes fatal danger handling. Introduce new needs only when their effect can be observed and tested. Vanilla villagers do not have player hunger: villager energy is a new mod mechanic and must be labelled accordingly.

### 2. Safety shields: constrain exploration

Alshiekh et al. place a shield before or after a learner to prevent actions violating a specified safety model. This separates learning from enforcement. Formal guarantees depend on correct specifications and models; copying the architectural idea does not confer their theorem on our implementation. [Original AAAI 2018 paper](https://ojs.aaai.org/index.php/AAAI/article/view/11797).

**Project proposal:** danger interrupts work/social actions. Exclude known lava, fire, drowning, dangerous falls and unreachable targets from elective choices. Use immediate retreat, fire/water escape, and validated shelter navigation. Detect navigation failure and back off rather than issuing the same target forever. A shield cannot guarantee survival against every mob or unknown terrain. Record every override and its reason to reveal overprotective or ineffective rules.

### 3. Cheap online learning: start small enough to inspect

Thompson sampling provides a well-studied way to balance information gathering and exploiting known outcomes in online choice problems. The tutorial includes bandit and reinforcement-learning settings and explicitly discusses conditions and limitations. [Russo et al., tutorial](https://arxiv.org/abs/1707.02038).

**Project proposal:** use a compact contextual learner over shelter choice, foraging patches, safe routes, and social partners. Tabular Q-learning is also reasonable when delayed effects matter. A contextual bandit alone cannot learn long plans or delayed survival consequences. Start with a small state representation, explicit action completion, observed outcome, visit count, and confidence. Discount stale evidence so a depleted farm or blocked doorway can be relearned. Restrict exploratory choices to the current safe action set.

Do not describe a random personality seed, manually changed utility coefficient, or event log as learned behavior. Learning requires experience to update persistent parameters and a subsequent controlled difference in decisions.

### 4. Reward shaping: avoid paying for endless activity

Ng, Harada and Russell establish conditions under which shaping preserves optimal policies in an MDP: a discounted difference of a state potential is the key form. Arbitrary extra rewards can change the objective and produce pathological incentives. The assumptions matter; approximate state and nonstationary multi-agent environments limit direct guarantees. [Original ICML 1999 paper](https://ai.stanford.edu/~ang/papers/shaping-icml99.pdf).

**Project proposal:** reward verified consumption, successful shelter arrival, and reduction of needs; charge elapsed time and observed damage. Never repeatedly reward merely selecting "eat", approaching a bed, naming a goal, or claiming success. Evaluate reward totals alongside actual health, supplies and deaths: a high score without survival is a bug. Track which action caused each update; threat interruptions must not credit a previous social action for an emergency retreat.

### 5. Voyager: skills and execution feedback, rather than per-tick language

Voyager combines an automatic curriculum, reusable executable skills, and iterative environment/error feedback using GPT-4 black-box queries. Its learning is primarily accumulation and reuse of programs, not online weight training. The paper reports failures from impossible goals, invalid code and mistaken verification, as well as API cost. Its published examples and relative item metrics are not proof of ordinary multiplayer survival at population scale. [Original paper and limitations](https://arxiv.org/html/2305.16291v2), [author repository](https://github.com/MineDojo/Voyager).

**Project proposal:** use a skill catalog with explicit prerequisites, target, timeout, completion test and failure recovery. Reuse successful plans, but obtain outcomes from game state. If a future planner proposes work, map it to typed mod capabilities and validate inventory, recipe and position. Do not execute arbitrary generated Java/JavaScript or allow invented actions to become successful memories.

### 6. Generative Agents: autobiographical retrieval and reflection

Park et al. show a 25-agent social sandbox using observation memories, retrieval by relevance/recency/importance, reflection and planning. Their ablations support these components for perceived believability. The original two-day simulation cost thousands of dollars in token credits and took multiple days; this is historical experiment cost, not a current API price quote. They acknowledge memory errors, limited evaluation duration and robustness concerns. [Original paper, sections 4 and 8.2](https://arxiv.org/html/2304.03442v2).

**Project proposal:** keep a bounded structured event buffer and longer-lived summaries. Store factual events separately from beliefs and inferred summaries, each with source event IDs. Use deterministic salience initially: damage, rescue, food gift, home loss, first success. A villager should remember who helped it because an event occurred. Optional language generates dialogue from those facts; it does not authoritatively change inventories, ownership, needs or history.

### 7. Continual learning: adaptation does not automatically last forever

Dohare et al. demonstrate loss of plasticity in tested continual deep-learning settings and propose continual backpropagation, which reinitializes small numbers of less-used units. This differs from catastrophic forgetting of old examples. Their results do not mean all neural learning fails, or that every long-lived agent needs their algorithm. [Original Nature 2024 paper](https://www.nature.com/articles/s41586-024-07711-7).

**Project proposal:** begin with bounded tabular or small linear learning instead of one deep network per villager. Keep stable identity separate from adaptive competence, and test repeated environmental reversals. If neural policies become justified, compare replay, regularization and controlled feature renewal with explicit retention and adaptation metrics.

### 8. Dreamer 3 and 4: impressive learning, substantial training machinery

DreamerV3 learns world models and improves policies through imagined trajectories. Its Minecraft result uses sparse rewards and no human demonstrations, with an A100 training setup; the paper describes learning the diamond task on one GPU for nine days. That is a research achievement, not evidence that hundreds of agents can each train this way during gameplay. [Original Nature 2025 paper](https://www.nature.com/articles/s41586-025-08744-2).

Dreamer 4 moves training into a world model learned from offline data and demonstrates Minecraft diamonds, with real-time world-model inference on a single GPU. The author page reports improved data efficiency over VPT. It does not establish cheap per-villager training, a Fabric integration, or native compatibility with this modpack. [Author project and paper](https://danijar.com/project/dreamer4/), [paper](https://arxiv.org/abs/2509.24527).

**Project proposal:** reserve learned world models for a later shared research service. Minecraft already exposes exact block/entity state to a mod; pixel-to-keyboard control adds a problem we do not currently need. A shared model could eventually propose plans for many individuals while each retains personal preferences and memories.

### 9. Many-agent Minecraft: feasibility and coordination caveats

Project Sid reports simulations with 10–1000+ agents and a PIANO architecture with concurrent processes, shared state and grounded action awareness. It documents cascading hallucinations and incoherence between speech and actions. Its experiments use particular infrastructure, agent capabilities and evaluation conditions; the headline population is not a benchmark for our local Fabric machine. [Original technical report](https://arxiv.org/html/2411.00114v1).

MineLand explicitly combines large-scale agents, limited senses and physical needs in Minecraft. It is a useful benchmark and architectural comparison rather than a drop-in villager replacement. [Original paper](https://arxiv.org/abs/2403.19267), [author implementation](https://github.com/cocacola-lab/MineLand).

**Project proposal:** bounded perception with line-of-sight where relevant; local relationships rather than all-pairs inference; conversations tied to actual proximity/events. Share expensive spatial indexing and village facts, but preserve each agent's observations and beliefs. Schedule cognition across ticks. Use immutable snapshots for optional asynchronous reasoning and reject stale plans on return.

### 10. Evolution: preserve viable diversity

MAP-Elites searches for high-performing solutions across user-chosen behavioral descriptors instead of keeping only a single winner. It gives a way to preserve different competent strategies, but depends on descriptors, evaluation budget and fitness design. [Mouret and Clune, original paper](https://arxiv.org/abs/1504.04909).

**Project proposal:** separate inherited genome (temperament and bounded learning priors), acquired policy, and personal episodic history. Reproduction can inherit traits with small mutations; it must not copy a parent's autobiography and call that genetic evolution. Evaluate candidate populations across several seeds before adopting trait distributions. Preserve sociable, cautious and exploratory viable lineages. Optimizing survival alone can select villagers that never leave a safe corner; require competence and behavioral diversity as additional measures.

## Proposed runtime architecture

1. **Body/world adapter:** reads real entity state; exposes typed capabilities; preserves Minecraft pathfinding, collision and inventory rules.
2. **Immediate safety:** cheap local checks and interruptible actions; executes independently of slow planning.
3. **Drives and utility:** current needs, temperament, commitments, and available opportunities produce candidate actions.
4. **Personal learner:** adjusts preferences/expected outcomes from completed, failed or interrupted actions.
5. **Skill executor:** owns one current action, validates its target, handles timeout and progress; movement has one authoritative owner.
6. **Memory and relationships:** records significant grounded events, maintains bounded personal knowledge and partner trust.
7. **Persistence:** stable entity UUID, world identity, schema version, traits, needs, learned statistics and event history; atomic snapshots with recovery.
8. **Optional slow cognition:** summarizes experience or proposes typed goals under a shared rate limit. Its unavailability must not stop survival.

Start by retaining the vanilla brain's routine functions while adding a narrow, explicit policy seam. Then replace activities one at a time after equivalent functions exist. Blindly disabling the vanilla brain can remove sleeping, professions, trading, breeding, POI claims and panic handling. Running two independent locomotion systems causes movement contention. A prototype should document precisely which behaviors it owns and which remain vanilla.

Each agent's disk home should live below a world-specific path such as `agents/<world-id>/<entity-uuid>/`. These are persisted data records, not one operating-system process or unrestricted filesystem identity per villager. Use UUID path components rather than names, bounded history rotation, schema migration, and quarantine/recovery for corrupt records. Define death, unload, transfer and world-copy semantics before treating persistence as complete.

## Phased experiments and acceptance gates

These are proposed tests, not completed results. All Minecraft integration validation should run in a visible, rendered Vanilla Perfected client as requested. Small pure-Java checks are supplementary and must not be described as in-game validation.

| Phase | Visible experiment | Primary measure and gate |
| --- | --- | --- |
| 0: compatibility | Launch exact pinned Fabric/modpack; create separate test world; spawn vanilla baseline and instrumented villagers | Confirm mod load, normal rendering/trading/pathfinding, no crash or resource errors |
| 1: survival | Repeated daylight/night transitions, zombie approach, blocked shelter, low supplies, fire/water edge | Deaths and causes, damage, shelter arrival, response latency; compare with vanilla and frozen-learning control |
| 2: actual learning | Two comparable foraging/shelter choices with controlled payoff; then swap which succeeds | Selection probability changes after outcomes, reverses when conditions change; frozen learner does not show equivalent adaptation |
| 3: continuity | Save/reload, chunk unload/reload, restart, duplicate displayed names, corrupt one profile | Same UUID retains traits and learned values; no identity collisions; corrupt data recovers explicitly |
| 4: relationships | One player repeatedly helps, another harms; hold distance and needs constant | Distinct event-grounded partner preference; missing memory ablation removes learned difference |
| 5: load | Same rendered world at 10, 50, 100, then 250 loaded villagers if budget permits | Tick-time percentiles, worst decision age, pathfinding cost, frame time, memory and disk growth |
| 6: evolution | Multiple seeded populations and generations with the same environmental challenges | Survival plus food/shelter competence and behavior diversity; compare no-mutation and no-selection controls |

Use multiple seeds and repeated trials for behavioral claims. Record exact software versions, seed, population, geometry, observation duration, player interventions and action traces. Report sample sizes and uncertainty; a single surviving group is a smoke test. Distinguish loaded active villagers from entities merely stored on disk.

Suggested initial performance hypothesis: cognition at 1–2 Hz per active villager, staggered; immediate survival checks faster; no disk writes or network calls in the decision loop. A provisional total custom-AI budget of 2 ms per server tick at 100 active villagers is a target to measure, not a guaranteed capacity. The game's full server tick must remain within its 50 ms target to maintain 20 TPS. Measure a vanilla/modpack baseline on the same machine and loaded chunks.

## Questions to resolve through playtesting

- Is realistic vulnerability enjoyable, or should young learners receive a temporary village safety net?
- Should villagers ever leave their village, build independently, or change professions without player direction?
- Which reactions should be visible through animation, routine, reputation, names or optional dialogue?
- Does a remembered negative encounter fade, and can repeated help repair trust?
- Are descendants a major gameplay loop, or is individual lifelong development the initial focus?

The first technical milestone should remain narrow: a visible villager makes a grounded choice, learns from its outcome, keeps that change after restart, and survives a controlled threat without waiting for external inference.
